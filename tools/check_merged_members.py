#!/usr/bin/env python3
"""Finds mixin members that silently displace a method on the target class.

A mixin does not need an injection annotation to change behaviour. Mixin merges
plain methods, fields and interface implementations into the target, so a file
carrying nothing but @Mixin can supply load-bearing code -
RegistryLookup_DelegateMixin is exactly that, and it provides the whole
implementation of getValueForCopying.

That makes "has no injection annotation" useless as a safety argument, and an
earlier pass of the overlap audit used it as one. The question worth asking is
narrower and answerable: does a merged member share a name with a method that
already exists on the target? If so it may be displacing it with no @Overwrite
to declare the intent.

Checked against the compiled class in the shipped server jar rather than the
patch text, because the patch only shows what NeoForge changed, not what the
class actually has.

Usage: check_merged_members.py <src-root> <minecraft-server-patched.jar>
"""
import pathlib
import re
import subprocess
import sys
import tempfile
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import audit_overlap as A

MIXIN_TARGET = re.compile(r'@Mixin\s*\(\s*(?:value\s*=\s*)?([\w.]+)\.class')
MEMBER = re.compile(
    r'((?:@\w+(?:\([^)]*\))?\s*)*)'
    r'\b(?:public|protected|private|default|static|final|\s)+'
    r'[\w.<>\[\],? ]+\s+(\w+)\s*\(([^;{}]*)\)\s*\{')
# Bridge methods are deliberately namespaced so they cannot collide.
PREFIXES = ('cardboard', 'cb', 'spigot', 'bukkit', 'fabric', 'paper')


def target_methods(jar, cls, workdir):
    path = cls.replace('.', '/') + '.class'
    try:
        jar.getinfo(path)
    except KeyError:
        return None
    jar.extract(path, workdir)
    try:
        out = subprocess.run(['javap', '-p', '-classpath', workdir, cls],
                             capture_output=True, text=True, timeout=60).stdout
    except Exception:
        return None
    # name -> set of parameter counts. Name alone cannot tell an overload from an
    # override, and the mixins here are full of deliberate overloads: Paper's
    # hurtAndBreak(..., boolean force) sits alongside the vanilla one on purpose.
    methods = {}
    for name, params in re.findall(r'\b(\w+)\s*\(([^)]*)\)', out):
        args = [a for a in params.split(',') if a.strip()]
        methods.setdefault(name, set()).add(len(args))
    return methods


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    src_root = pathlib.Path(sys.argv[1])
    jar = zipfile.ZipFile(sys.argv[2])
    workdir = tempfile.mkdtemp()

    findings = []
    checked = 0
    for path in sorted(src_root.rglob('*.java')):
        text = A._strip_comments(path.read_text(errors='replace'))
        match = MIXIN_TARGET.search(text)
        if not match:
            continue
        cls = match.group(1)
        if '.' not in cls:
            imp = re.search(r'^import\s+([\w.]*\.%s);' % re.escape(cls), text, re.M)
            if not imp:
                continue
            cls = imp.group(1)
        if not cls.startswith('net.minecraft'):
            continue
        existing = target_methods(jar, cls, workdir)
        if existing is None:
            continue
        checked += 1
        for m in MEMBER.finditer(text):
            anns, name, params = m.group(1), m.group(2), m.group(3)
            # Only the class body itself merges into the target. A method declared
            # inside an anonymous class - new CommandSource() { ... } - belongs to
            # that inner type, but looks identical to the regex.
            depth = text.count('{', 0, m.start()) - text.count('}', 0, m.start())
            if depth != 1:
                continue
            # Injection handlers are callbacks, not merged members, and @Unique is
            # an explicit promise that the name is the mixin's own. Neither can
            # displace a target method, so neither belongs in this report.
            if any(a in anns for a in ('Shadow', 'Overwrite', 'Inject', 'Redirect',
                                       'Modify', 'Wrap', 'Unique', 'Accessor',
                                       'Invoker', 'Intrinsic')):
                continue
            if name.startswith(PREFIXES) or '$' in name:
                continue
            arity = len([a for a in params.split(',') if a.strip()])
            if arity in existing.get(name, ()):
                findings.append((path.name, cls.split('.')[-1], f'{name}/{arity}'))

    print(f'Checked {checked} mixins against the compiled target classes.\n')
    if not findings:
        print('No merged member shares a name AND arity with a target method.')
        return 0
    print(f'{len(findings)} merged member(s) share a name and arity with a target method:\n')
    for f, cls, name in findings:
        print(f'  {f:46} {cls}#{name}()')
    return 1


if __name__ == '__main__':
    sys.exit(main())
