#!/usr/bin/env python3
"""
Rewrite calls that relied on Fabric Loom's interface injection.

Cardboard declares loom:injected_interfaces in fabric.mod.json, which grafts
bridge interfaces onto six Minecraft classes at compile time. NeoForge has no
equivalent, so every such call needs an explicit cast.

Driven by javac output so only real call sites are touched, and repeated until
the error count stops falling.
"""
import collections
import json
import pathlib
import re
import subprocess
import sys

LOG = "/tmp/bridge_pass.log"
BRIDGES = json.load(open("/tmp/bridge_methods.json"))


def compile_once():
    subprocess.run(["./gradlew", "compileJava", "--no-daemon", "--console=plain"],
                   stdout=open(LOG, "w"), stderr=subprocess.STDOUT)
    txt = pathlib.Path(LOG).read_text()
    m = re.search(r"^(\d+) errors?$", txt, re.M)
    return (int(m.group(1)) if m else 0), txt


def collect(txt):
    """file -> {(line, method)} for unresolved bridge methods."""
    sites = collections.defaultdict(set)
    lines = txt.split("\n")
    for i, l in enumerate(lines):
        m = re.match(r"(/[^:]*src/main/java/[^:]+):(\d+): error:", l)
        if not m:
            continue
        for j in range(i + 1, min(i + 6, len(lines))):
            s = re.search(r"symbol:\s+method (\w+)[\(\s]", lines[j])
            if s:
                if s.group(1) in BRIDGES:
                    sites[m.group(1)].add((int(m.group(2)), s.group(1)))
                break
    return sites


# Type::method  -- a method reference cannot take a cast, so it becomes a lambda
REF = r"([\w.$]+)::(%s)\b"


def receiver_span(line, dot):
    """Walk back from the '.' before a call to find the whole receiver expression."""
    i = dot - 1
    while i >= 0 and line[i].isspace():
        i -= 1
    if i < 0:
        return None
    if line[i] == ")":
        depth = 0
        while i >= 0:
            if line[i] == ")":
                depth += 1
            elif line[i] == "(":
                depth -= 1
                if depth == 0:
                    break
            i -= 1
        if i < 0:
            return None
        # include a preceding identifier chain, e.g. this.getHandle()
        j = i - 1
        while j >= 0 and (line[j].isalnum() or line[j] in "_$."):
            j -= 1
        return j + 1
    j = i
    while j >= 0 and (line[j].isalnum() or line[j] in "_$."):
        j -= 1
    return j + 1


def cast_call(line, meth, iface):
    """Insert an explicit cast around the receiver of the first .meth( on the line."""
    needle = "." + meth + "("
    dot = line.find(needle)
    while dot != -1:
        start = receiver_span(line, dot)
        if start is not None and start < dot:
            recv = line[start:dot]
            if "((" + iface not in recv:
                return (line[:start] + "((" + iface + ") (Object) " + recv + ")"
                        + line[dot:])
        dot = line.find(needle, dot + 1)
    return line


def rewrite(sites):
    n = 0
    for f, entries in sites.items():
        p = pathlib.Path(f)
        src = p.read_text().split("\n")
        for ln, meth in sorted(entries, reverse=True):
            i = ln - 1
            if i >= len(src):
                continue
            iface = BRIDGES[meth]
            line = src[i]
            new = re.sub(REF % re.escape(meth),
                         lambda m: f"x -> (({iface}) (Object) x).{meth}()", line, count=1)
            if new == line:
                new = cast_call(line, meth, iface)
            if new != line:
                src[i] = new
                n += 1
        p.write_text("\n".join(src))
    return n


def main():
    prev = None
    for rnd in range(1, 9):
        count, txt = compile_once()
        print(f"round {rnd}: {count} errors")
        if count == 0:
            print("compiles clean")
            return 0
        if prev is not None and count >= prev:
            print("no further progress from bridge rewriting")
            return 1
        prev = count
        sites = collect(txt)
        if not sites:
            print("no remaining bridge-method errors")
            return 1
        print(f"  rewriting {sum(len(v) for v in sites.values())} sites in {len(sites)} files")
        if rewrite(sites) == 0:
            print("  nothing matched; stopping")
            return 1
    return 1


if __name__ == "__main__":
    sys.exit(main())
