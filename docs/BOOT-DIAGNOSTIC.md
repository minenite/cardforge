# Fail-soft boot survey

Diagnostic run on branch `diagnostic/fail-soft-survey` with
`defaultRequire: 0` in both mixin configs. **Not a proposed implementation** —
the purpose was to find out how far startup can get and what actually breaks.

## Result: startup reaches the server tick loop

```
[Cardboard]  Loading Libraries...
[Cardboard|Libraries]  Loaded 33 libraries. Paper-API 26.2.build.110-stable; Adventure 5.2.0
[Cardboard]  Scanning Paper-API for events... Found: 467
[Cardboard]  scanning plugins for events... Found: 0
[Bukkit]     Loading Paper PluginInitializerManager..
[Cardforge]  Cardforge on NeoForge 26.2.0.52-beta (Minecraft 26.2)
[minecraft]  Found new data pack file/bukkit, loading it automatically
[minecraft]  Starting minecraft server version 26.2
[minecraft]  Loading properties
[minecraft]  Starting Minecraft server on *:25565
```

Every Cardboard subsystem that runs before world load completed: the runtime
library loader, Paper API event scan (467 events), the Paper plugin
initializer, the Bukkit datapack (created by MainMixin and picked up by
vanilla), and the Bukkit logger.

**Failed injections during this run: 0.** With fail-soft enabled, no mixin
reported a failed injection check before the server reached its tick loop.
The 107-target overlap with NeoForge's patches is therefore an upper bound on
future work, not a count of current breakage — most patched classes are not
patched in the methods Cardboard injects into.

## What actually blocked startup

Four class-loading conflicts, all from bundling libraries the platform already
provides. Each produced `LinkageError: loader constraint violation` or
`IncompatibleClassChangeError`, and each was fixed by excluding the package
from the bundle:

| Library | Symptom |
|---|---|
| slf4j, log4j | loader constraint violation on `org.slf4j.Logger` |
| ASM | loader constraint violation on `org.objectweb.asm.tree.ClassNode` |
| maven-artifact | loader constraint violation on `ArtifactVersion` |
| commons-lang3 | `IncompatibleClassChangeError` on `MutableObject` |

Also required: Cardboard's five `META-INF/services` entries, and moving the
libraries Loom used to nest via `include()` into the bundled configuration.

## The remaining blocker is not ours

```
java.lang.NoClassDefFoundError: io/netty/channel/kqueue/KQueueSocketChannel
  Caused by: IllegalStateException: Only supported on OSX/BSD
```

NeoForge 26.2.0.52-beta selects netty's **kqueue** transport on Linux, which
only works on macOS/BSD. Verified by control: **the same server fails
identically with Cardforge removed from `mods/`.** Removing the kqueue jars
does not help either — something references `KQueueSocketChannel` directly, so
it becomes a hard `ClassNotFoundException`.

This is a platform bug in the only 26.2 NeoForge builds published so far (all
`-beta`, 26.2.0.47 through .52). It is unrelated to this port.

## Conclusion

Full Bukkit/plugin/server startup looks reachable. Nothing in Cardboard's
Bukkit layer blocked it — the port ran until NeoForge's own networking failed
to initialise on Linux.

Next steps, in order:
1. Retest on a NeoForge build where the netty transport bug is fixed, or on
   macOS where kqueue is valid.
2. Restore `defaultRequire: 1` and fix injections individually as they surface.
   This survey suggests far fewer than the 107-target upper bound.
3. Load a plugin and confirm the Bukkit plugin lifecycle.
