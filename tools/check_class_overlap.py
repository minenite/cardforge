#!/usr/bin/env python3
"""Fail if the CardForge jar ships any class NeoForge already provides.

CardForge is loaded by the TRANSFORMER class loader while NeoForge's own
libraries sit on the app loader. Any class present in both is not merely
wasted space: as soon as one appears in a signature crossing the two loaders,
the JVM raises

    LinkageError: loader constraint violation ... have different Class objects

which is how bundling maven-artifact broke mod construction via
IModInfo#getVersion. That failure is invisible until runtime and easy to
reintroduce whenever a new dependency is bundled, so it is checked here.

Usage: check_class_overlap.py <cardforge.jar> <neoforge-server-dir>
Exits non-zero and prints the offending classes if any overlap exists.
"""
import pathlib
import sys
import zipfile


def classes_in(jar):
    try:
        with zipfile.ZipFile(jar) as z:
            return {n for n in z.namelist() if n.endswith(".class")}
    except zipfile.BadZipFile:
        return set()


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2

    jar = pathlib.Path(sys.argv[1])
    server = pathlib.Path(sys.argv[2])
    if not jar.is_file():
        print(f"no such jar: {jar}")
        return 2

    ours = classes_in(jar)
    if not ours:
        print(f"no classes found in {jar}")
        return 2

    # The patched Minecraft jar is deliberately overlapped by our Mixins, and
    # the mods directory contains our own jar; neither is a library conflict.
    overlaps = {}
    for lib in sorted((server / "libraries").rglob("*.jar")):
        if "minecraft-server-patched" in lib.name:
            continue
        shared = ours & classes_in(lib)
        if shared:
            overlaps[lib] = shared

    if not overlaps:
        print(f"OK: {len(ours)} classes, none shadow a NeoForge library")
        return 0

    total = sum(len(v) for v in overlaps.values())
    print(f"FAIL: {total} class(es) shadow NeoForge libraries\n")
    for lib, shared in overlaps.items():
        rel = lib.relative_to(server)
        print(f"  {rel}  ({len(shared)})")
        for name in sorted(shared)[:5]:
            print(f"      {name}")
        if len(shared) > 5:
            print(f"      ... and {len(shared) - 5} more")
    return 1


if __name__ == "__main__":
    sys.exit(main())
