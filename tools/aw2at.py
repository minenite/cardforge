#!/usr/bin/env python3
"""
Convert a Fabric access widener into a NeoForge access transformer.

Fabric AW                                  NeoForge AT
  accessible class  a/b/C                    public a.b.C
  extendable class  a/b/C                    public a.b.C          (also de-finals)
  accessible method a/b/C name (desc)        public a.b.C name(desc)
  extendable method a/b/C name (desc)        protected a.b.C name(desc)
  accessible field  a/b/C name desc          public a.b.C name
  mutable    field  a/b/C name desc          public-f a.b.C name

Entries with no AT equivalent are reported rather than silently dropped.
"""
import sys
from collections import OrderedDict


def convert(lines):
    out, skipped = OrderedDict(), []
    for n, raw in enumerate(lines, 1):
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        if parts[0] in ("accessWidener", "classTweaker"):
            continue
        if len(parts) < 3:
            skipped.append((n, raw.strip(), "unparseable"))
            continue

        access, kind, target = parts[0], parts[1], parts[2].replace("/", ".")

        if kind == "class":
            if access in ("accessible", "extendable"):
                out.setdefault(f"public {target}", None)
            else:
                skipped.append((n, raw.strip(), f"class access '{access}'"))
        elif kind == "method":
            if len(parts) < 5:
                skipped.append((n, raw.strip(), "method missing name/desc"))
                continue
            name, desc = parts[3], parts[4]
            vis = "public" if access == "accessible" else "protected"
            out.setdefault(f"{vis} {target} {name}{desc}", None)
        elif kind == "field":
            if len(parts) < 4:
                skipped.append((n, raw.strip(), "field missing name"))
                continue
            name = parts[3]
            # mutable strips final; accessible only widens visibility
            vis = "public-f" if access == "mutable" else "public"
            key = f"{vis} {target} {name}"
            # a field that is both accessible and mutable should end up public-f
            if vis == "public-f":
                out.pop(f"public {target} {name}", None)
                out.setdefault(key, None)
            elif f"public-f {target} {name}" not in out:
                out.setdefault(key, None)
        else:
            skipped.append((n, raw.strip(), f"unsupported kind '{kind}'"))
    return list(out.keys()), skipped


def main():
    if len(sys.argv) < 2:
        print("usage: aw2at.py <accesswidener> [out.cfg]", file=sys.stderr)
        return 2
    with open(sys.argv[1]) as fh:
        entries, skipped = convert(fh)

    header = [
        "# Generated from Cardboard's Fabric access widener by tools/aw2at.py",
        "# Do not edit by hand; regenerate instead.",
        "",
    ]
    text = "\n".join(header + entries) + "\n"

    if len(sys.argv) > 2:
        with open(sys.argv[2], "w") as fh:
            fh.write(text)
        print(f"wrote {len(entries)} AT entries -> {sys.argv[2]}")
    else:
        sys.stdout.write(text)

    if skipped:
        print(f"\n{len(skipped)} entr{'y' if len(skipped)==1 else 'ies'} need manual handling:",
              file=sys.stderr)
        for n, raw, why in skipped:
            print(f"  line {n}: {why}\n    {raw}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
