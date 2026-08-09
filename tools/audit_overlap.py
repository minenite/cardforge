#!/usr/bin/env python3
"""Audit Cardboard's mixin targets against NeoForge's patches to Minecraft.

A mixin applying cleanly proves only that the bytecode still has a matching
shape. It says nothing about whether the code underneath still *means* the same
thing. NeoForge patches Minecraft heavily: it redirects call sites to its own
hooks, neutralises vanilla branches in place (`if (false && ...)`), delegates
methods to replacement subsystems, and adds early-outs that run before an
injected @At("HEAD"). Any of those can leave a Cardboard hook applying happily
while firing at the wrong time, on the wrong path, or never.

So this cross-references two things:

  - which Minecraft classes Cardboard mixes into, and at which methods;
  - which of those classes NeoForge patches, and which methods those patch
    hunks actually touch.

Where both touch the same method, the mixin is reported for manual review,
ranked by how dangerous the injection type is if the semantics moved:
@Overwrite and @Redirect replace behaviour outright, cancellable injections
decide whether vanilla runs at all, and @Modify* rewrite values in flight.

Usage: audit_overlap.py <cardforge-src-root> <neoforge-repo> [--all]
Exit status is always 0: this is a report, not a gate.
"""
import collections
import pathlib
import re
import sys

MIXIN_TARGET = re.compile(r'@Mixin\s*\(\s*(?:value\s*=\s*)?\{?\s*([A-Za-z0-9_.]+)\.class')
INJECTION = re.compile(r'@(Inject|Redirect|Overwrite|ModifyVariable|ModifyArg|ModifyArgs|ModifyConstant|ModifyReturnValue|WrapOperation|WrapWithCondition|Accessor|Invoker)\b')
METHOD_ARG = re.compile(r'method\s*=\s*(?:\{\s*)?"([^"]+)"')
TARGET_ARG = re.compile(r'target\s*=\s*"([^"]+)"')
CANCELLABLE = re.compile(r'cancellable\s*=\s*true')

# How much damage a silently-moved semantic does, worst first.
SEVERITY = {
    'Overwrite': 4,
    'Redirect': 4,
    'WrapOperation': 4,
    'WrapWithCondition': 3,
    'ModifyVariable': 3,
    'ModifyArg': 3,
    'ModifyArgs': 3,
    'ModifyConstant': 3,
    'ModifyReturnValue': 3,
    'Inject': 2,
    'Accessor': 1,
    'Invoker': 1,
}

# Patch lines that mean NeoForge did more than move code around.
REPLACEMENT_MARKERS = [
    (re.compile(r'if\s*\(\s*false\s*&&'), 'vanilla branch neutralised (`if (false && ...)`)'),
    (re.compile(r'net\.neoforged\.neoforge\.common\.CommonHooks'), 'call routed through CommonHooks'),
    (re.compile(r'net\.neoforged\.neoforge\.event\.EventHooks'), 'call routed through EventHooks'),
    (re.compile(r'IShearable'), 'shearing moved to IShearable'),
    (re.compile(r'net\.neoforged\.neoforge\.capabilities'), 'capabilities subsystem'),
    (re.compile(r'@Deprecated|// Neo: '), 'annotated Neo change'),
]




def _strip_comments(text):
    """Removes block and line comments, preserving line count and string literals.

    The annotation scan was counting @Overwrite inside commented-out code -
    PlayerDataStorage reported three overwrites and has none live - which both
    inflates the review queue and, worse, invites the conclusion that a bucket was
    examined when the entries in it were never real.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"' or c == "'":
            quote = c
            out.append(c)
            i += 1
            while i < n:
                out.append(text[i])
                if text[i] == '\\':
                    i += 2
                    if i - 1 < n:
                        out.append(text[i - 1])
                    continue
                if text[i] == quote:
                    i += 1
                    break
                i += 1
            continue
        if text.startswith('//', i):
            while i < n and text[i] != '\n':
                i += 1
            continue
        if text.startswith('/*', i):
            end = text.find('*/', i + 2)
            end = n if end == -1 else end + 2
            # keep newlines so line numbers still line up
            out.append('\n' * text.count('\n', i, end))
            i = end
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def _annotations(text):
    """Yields (name, argument-text) for each annotation, with balanced parens.

    The previous pattern stopped the argument text at the first ';' or '{'. Mixin
    targets are JVM descriptors - @At(target = "Lnet/minecraft/world/Foo;bar()V")
    - so every injection carrying one failed to match at all, and the file looked
    as though it contained no injections. That silently emptied the audit's
    largest bucket.
    """
    for m in re.finditer(r'@(\w+)\s*\(', text):
        depth = 0
        i = m.end() - 1
        in_string = False
        while i < len(text):
            ch = text[i]
            if in_string:
                if ch == '\\':
                    i += 2
                    continue
                if ch == '"':
                    in_string = False
            elif ch == '"':
                in_string = True
            elif ch == '(':
                depth += 1
            elif ch == ')':
                depth -= 1
                if depth == 0:
                    yield m.group(1), text[m.end():i]
                    break
            i += 1


def mixin_targets(src_root):
    """Maps Minecraft class name -> list of (mixin file, injections)."""
    out = collections.defaultdict(list)
    for path in sorted(pathlib.Path(src_root).rglob('*.java')):
        text = _strip_comments(path.read_text(errors='replace'))
        m = MIXIN_TARGET.search(text)
        if not m:
            continue
        target = m.group(1)
        # Resolve a simple name through the file's own imports.
        if '.' not in target:
            imp = re.search(r'^import\s+((?:net\.minecraft|org\.bukkit|io\.papermc)[\w.]*\.%s);' % re.escape(target),
                            text, re.M)
            target = imp.group(1) if imp else target
        if not target.startswith('net.minecraft'):
            continue

        injections = []
        for block in _annotations(text):
            kind = block[0]
            if kind not in SEVERITY:
                continue
            args = block[1]
            methods = METHOD_ARG.findall(args)
            targets = TARGET_ARG.findall(args)
            injections.append({
                'kind': kind,
                'methods': methods,
                'targets': targets,
                'cancellable': bool(CANCELLABLE.search(args)),
            })
        # Bare @Overwrite carries no arguments, so pick up the method name after it.
        for ow in re.finditer(r'@Overwrite\b[^(]', text):
            tail = text[ow.end():ow.end() + 400]
            name = re.search(r'\b(\w+)\s*\(', tail)
            injections.append({
                'kind': 'Overwrite',
                'methods': [name.group(1)] if name else [],
                'targets': [],
                'cancellable': False,
            })
        out[target].append((path, injections))
    return out


def patched_methods(patch_path):
    """Method names appearing in a patch's changed lines, plus any markers hit."""
    methods = set()
    markers = set()
    for line in patch_path.read_text(errors='replace').splitlines():
        if not line or line[0] not in '+-@':
            continue
        if line.startswith('@@'):
            # Hunk headers carry the enclosing declaration - sometimes. Just as
            # often the declaration is on a context line a line or two below, and
            # reading only the @@ line missed handleSetCarriedItem, where an
            # @Overwrite was discarding NeoForge's hotbar-switch events. The
            # context lines are scanned below, so nothing more is needed here
            # beyond not stopping at the header.
            for name in re.findall(r'\b(\w+)\s*\(', line):
                methods.add(name)
            continue
        for pattern, label in REPLACEMENT_MARKERS:
            if pattern.search(line):
                markers.add(label)
        for name in re.findall(r'\b(\w+)\s*\(', line[1:]):
            methods.add(name)
    # Context lines too: a patch that inserts into the middle of a method shows the
    # signature as unchanged context, and that method is every bit as patched as one
    # whose signature line moved.
    for line in patch_path.read_text(errors='replace').splitlines():
        if line[:1] in (' ', '+', '-') and DECL.search(line):
            for name in re.findall(r'\b(\w+)\s*\(', line):
                methods.add(name)
    return methods, markers


DECL = re.compile(r'\b(?:public|protected|private)\b[^;]*\(')


# Two shapes. A value-returning delegate is `return wider(...)`; a void one is a
# bare call. Matching only the first missed ItemStack#hurtAndBreak, which is void
# and whose narrow form NeoForge reduced to `this.hurtAndBreak(amount, level,
# (LivingEntity) player, onBreak);` - a real delegate that silently stopped a
# Bukkit event firing, found by hand because this tool did not report it.
DELEGATE = re.compile(r'^\+\s*(?:return\s+)?(?:this\.)?(\w+)\(')


def delegating_overloads(patch_path):
    """Method names NeoForge turned into a delegate to a wider overload.

    The pattern is: keep the old signature, make its whole body `return
    wider(...)`, and move the real code into the new one. Every internal call
    site then moves to the wide overload. A mixin still targeting the narrow
    signature applies cleanly - the method exists - and silently stops firing on
    the path that matters. That is what happened to openMenu and emptyContents.
    """
    names = set()
    for line in patch_path.read_text(errors='replace').splitlines():
        m = DELEGATE.match(line)
        if m:
            names.add(m.group(1))
    return names


INTERMEDIARY = re.compile(r'method\s*=\s*"(method_\d+)"')


def intermediary_injections(src_root):
    """Live injections still targeting a Fabric intermediary name.

    Cardboard came from Fabric, where method_NNNNN is a real name. Under Mojang
    mappings it matches nothing, so the injection can never bind - and because
    Mixin only resolves a config's targets when the class is first loaded, strict
    mode does not catch it at boot. It crashes the server whenever something
    first needs that class, which can be hours in. One of these took the server
    down during chunk generation when a ram behaviour loaded.

    Comments are stripped first, since most surviving intermediary names in this
    codebase are inside disabled code and harmless.
    """
    hits = []
    for path in sorted(pathlib.Path(src_root).rglob('*.java')):
        text = _strip_comments(path.read_text(errors='replace'))
        text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
        text = re.sub(r'//[^\n]*', '', text)
        for m in INTERMEDIARY.finditer(text):
            hits.append((path, m.group(1)))
    return hits


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 0
    src_root = sys.argv[1]
    neoforge = pathlib.Path(sys.argv[2])
    show_all = '--all' in sys.argv

    patch_root = neoforge / 'patches' / 'net' / 'minecraft'
    if not patch_root.is_dir():
        print(f'no patches under {patch_root}')
        return 0

    stale = intermediary_injections(src_root)
    if stale:
        print('!! Live injections targeting Fabric intermediary names.')
        print('   These cannot bind under Mojang mappings and crash the server')
        print('   when the class is first loaded, not at boot.\n')
        for path, name in stale:
            print(f'   {path.name}: {name}')
        print()

    targets = mixin_targets(src_root)
    print(f'Cardboard mixes into {len(targets)} Minecraft classes.\n')

    overlapping = []
    for cls, entries in targets.items():
        rel = cls[len('net.minecraft.'):].replace('.', '/') + '.java.patch'
        patch = patch_root / rel
        if not patch.is_file():
            continue
        methods, markers = patched_methods(patch)
        overlapping.append((cls, entries, methods, markers, patch))

    print(f'NeoForge patches {len(overlapping)} of them. These are the overlap.\n')

    findings = []
    for cls, entries, patched, markers, patch in overlapping:
        for path, injections in entries:
            for inj in injections:
                names = set()
                for m in inj['methods']:
                    names.add(m.split('(')[0])
                for t in inj['targets']:
                    mm = re.search(r';(\w+)\(', t)
                    if mm:
                        names.add(mm.group(1))
                collide = names & patched
                if not collide and not show_all:
                    continue
                sev = SEVERITY[inj['kind']]
                if inj['cancellable']:
                    sev += 1
                if markers:
                    sev += 1
                findings.append({
                    'severity': sev,
                    'cls': cls,
                    'mixin': path.name,
                    'kind': inj['kind'],
                    'cancellable': inj['cancellable'],
                    'methods': sorted(collide) or sorted(names),
                    'markers': sorted(markers),
                })

    # Separate, higher-signal check: a descriptor-less injection into a method
    # NeoForge turned into a delegate is firing on the wrong overload.
    delegate_hits = []
    for cls, entries, patched, markers, patch in overlapping:
        delegates = delegating_overloads(patch)
        if not delegates:
            continue
        for path, injections in entries:
            for inj in injections:
                for raw in inj['methods']:
                    name = raw.split('(')[0]
                    if name in delegates and '(' not in raw:
                        delegate_hits.append((cls, path.name, inj['kind'], name))

    if delegate_hits:
        print('!! Descriptor-less injections into methods NeoForge made delegates.')
        print('   These apply cleanly and then fire on the wrong overload.\n')
        for cls, mixin, kind, name in sorted(set(delegate_hits)):
            print(f'   @{kind} {cls.split(".")[-1]}#{name}  ({mixin})')
        print()

    findings.sort(key=lambda f: (-f['severity'], f['cls'], f['mixin']))
    print(f'{len(findings)} injection(s) land on a method NeoForge also patched.\n')

    for f in findings:
        flags = []
        if f['cancellable']:
            flags.append('cancellable')
        if f['markers']:
            flags.extend(f['markers'])
        print(f"[{f['severity']}] @{f['kind']:<16} {f['cls'].split('.')[-1]}#{','.join(f['methods'])}")
        print(f"      {f['mixin']}")
        if flags:
            print(f"      ! {'; '.join(flags)}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
