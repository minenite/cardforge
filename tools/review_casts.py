#!/usr/bin/env python3
"""
Semantic review of the bridge casts and @Invoker calls introduced by the port.

Casting through Object means javac cannot check the receiver, so a mis-extracted
receiver compiles cleanly and only fails at runtime with ClassCastException.
This flags casts whose receiver is unlikely to be an instance of the bridge's
target type.
"""
import pathlib
import re
import sys

# bridge interface -> the Minecraft type it is meant to be applied to
TARGETS = {
    "EntityBridge": "entity",
    "ServerPlayerBridge": "player",
    "LevelBridge": "level/world",
    "ServerLevelBridge": "level/world",
    "MinecraftServerBridge": "server",
    "ServerLoginPacketListenerImplBridge": "login listener",
    "ItemStackBridge": "item stack",
    "IngredientBridge": "ingredient",
}
# receivers that are Bukkit-side objects: casting these to an NMS bridge is wrong
BUKKIT_HINT = re.compile(r'\b(Craft[A-Z]\w*|bukkit\w*|\w*Bukkit)\b')
CAST = re.compile(r'\(\(([\w.]*?)(\w+Bridge|IMixin\w+|ILevelSettings)\)\s*\(Object\)\s*([^)]{1,80}?)\)\s*\.\s*([\w$]+)\s*\(')

suspicious, total = [], 0
for p in sorted(pathlib.Path("src/main/java").rglob("*.java")):
    for n, line in enumerate(p.read_text().split("\n"), 1):
        for m in CAST.finditer(line):
            total += 1
            iface, recv, meth = m.group(2), m.group(3).strip(), m.group(4)
            why = None
            if BUKKIT_HINT.search(recv) and iface != "ItemStackBridge":
                why = "receiver looks like a Bukkit object"
            elif recv.endswith("Bridge") or "Bridge)" in recv:
                why = "receiver already a bridge cast"
            elif recv in ("", "this") and "/mixin/" not in str(p):
                why = "'this' outside a mixin"
            if why:
                suspicious.append((str(p).replace("src/main/java/", ""), n, iface, recv, meth, why))

print(f"bridge/invoker cast sites scanned: {total}")
print(f"flagged for manual review: {len(suspicious)}\n")
for f, n, iface, recv, meth, why in suspicious:
    print(f"  {f}:{n}\n    (({iface}) (Object) {recv}).{meth}()   <- {why}")
sys.exit(0)
