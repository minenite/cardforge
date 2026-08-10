# Running CardForge behind a proxy

CardForge implements **Velocity modern forwarding** natively. This document is
the configuration that works, and the traps that cost real time to find.

Verified on 2026-08-10 against Velocity 3.5.1 with two NeoForge 26.2 backends:
login, server switching with `/server`, and chat, with a real Mojang account
through the proxy.

## Why not BungeeCord legacy forwarding

Legacy forwarding smuggles the player's address, UUID and skin through the
handshake hostname, separated by NUL bytes. A mod loader marks that same field,
so on a modded server the two collide and the handshake is ambiguous. CardForge
still tolerates loader markers there, but modern forwarding is the supported
path and the one to use.

Modern forwarding leaves the hostname alone. During login the backend asks the
proxy who the player is over the `velocity:player_info` channel, and the proxy
answers with an HMAC-SHA256 signature over the forwarded identity. The shared
secret authenticates the exchange, so — unlike a plain offline-mode server — a
backend that is reachable directly cannot be impersonated.

## Configuration

### Proxy — `velocity.toml`

```toml
player-info-forwarding-mode = "MODERN"
forwarding-secret-file = "forwarding.secret"
online-mode = true

[servers]
lobby = "127.0.0.1:25566"
warz1 = "127.0.0.1:25567"
try = ["lobby"]
```

### Each backend — `spigot.yml`

```yaml
settings:
  velocity:
    enabled: true
    secret: <exact contents of forwarding.secret>
  bungeecord: false
```

### Each backend — `server.properties`

```properties
online-mode=false
enforce-secure-profile=false
server-port=<unique per backend>
```

`online-mode=false` is required and is not a security hole here: the forwarding
secret is what authenticates players, and a backend with forwarding enabled
refuses any login that does not carry valid forwarding data. Do not expose the
backend ports publicly.

The secret must match **exactly**. A mismatch fails HMAC verification and
presents as `Invalid proxy forwarding data.` — which reads like a protocol bug
and is not one.

## Traps

These are all things that look like a CardForge defect and are not.

### `enforce-secure-profile=false` does not stop the profile-key kick

The advice you will find everywhere is to set this and move on. It is necessary
but not sufficient, because it only decides whether a key is **required** —
`handleChatSessionUpdate` validates any key that *is* sent, without consulting
the property. A backend behind a proxy is offline-mode and never speaks to the
session server, so it cannot validate it, and the player is kicked seconds after
joining:

```
Failed to validate profile key: Invalid signature for profile public key.
```

CardForge handles this by not installing a signing session for proxied players.

### Chat signing is given up for proxied players

Following from the above: chat messages are verified against the player's key,
so accepting a key the backend cannot verify only moves the failure one step
later, to `Chat had an invalid signature. Please try reconnecting.`

CardForge leaves proxied players on vanilla's unsigned-message path. The
consequence is real and worth knowing:

- Chat from proxied players is **not signed end to end**.
- Clients show no secure-chat indicator.
- Chat reports cannot be attributed by signature.

This is inherent to proxying — the proxy terminates the authenticated session,
so no backend can produce a signature the client's key covers. It is exactly
what Velocity's `MODERN_LAZY_SESSION` forwarding version exists to express.
Unproxied servers enforce signing exactly as before.

### A proxy cannot decode NeoForge's command tree

Brigadier argument types cross the wire as numeric registry ids, and a proxy
keeps a hardcoded table of the vanilla ones. NeoForge registers two of its own —
`neoforge:enum` and `neoforge:modid` — so the first command tree a NeoForge
server sends kills the connection *after* a successful login:

```
CorruptedFrameException: Error decoding AvailableCommandsPacket
Caused by: IllegalArgumentException: Argument type identifier 58 unknown.
```

This is not mod-specific and not a CardForge bug: any NeoForge server hits it,
and Velocity 3.5.1 and 4.0.0 fail identically. CardForge writes those arguments
as `brigadier:string` whenever a proxy is configured. The cost is that
client-side tab completion for those arguments falls back to free text — the
commands still work, and the server still parses them with the real type,
because only the wire representation changes.

### Skins, and the profile the login finishes with

A proxied player showing the default skin means the login completed with an
offline-mode profile rather than the forwarded one. The forwarded profile is the
only source of the player's `textures` property, so losing it loses the skin and
gives the player a name-hashed UUID - which also means plugin data keyed by UUID
is stored under the wrong identity.

Two things caused this, and both are fixed; they are recorded because the
symptom is intermittent and easy to misread:

- Vanilla's offline branch runs during `handleHello` and starts verification
  with a profile built from the name alone. If the server tick beats the proxy's
  answer, that stand-in finishes the login. Whether it does is a race, so the
  skin appears on one server and not the next.
- `initUUID`, inherited from the legacy Spigot path, rebuilds the profile from
  the name later, dropping every property.

The trace settles it in one line: `props=1 keys=[textures]` on both the
`verified:` and `finishing login with` lines means the skin arrived and was used.
`props=0` means it was not.

### Forwarding version

CardForge requests version 4, `MODERN_LAZY_SESSION`. Velocity's
`findForwardingVersion` caps the request at 4 and then, for a client at 1.19.3
or newer, answers 4 if 4 was requested and 1 otherwise — so requesting 2 or 3
gets a modern client the same payload as requesting 1. Version 4 writes no key
fields, so the payload layout matches version 1; what it changes is which side
is responsible for the chat session.

## Diagnosing a failure

Both Minecraft's logger and `System.out` can silently discard messages from the
network threads if the server's log4j appender throws, and a failing appender
truncates `debug.log` at arbitrary points — which reads exactly like a
connection dying and is not. Do not trust `debug.log` truncation as evidence.

Start the backend with:

```
-Dcardforge.proxy.trace=true
```

and CardForge writes the login exchange straight to `cardforge-proxy.log` in the
server directory, bypassing the logger entirely. A healthy login:

```
handshake HEAD: intent=LOGIN proto=776 host=localhost
handleHello HEAD: name=<player> velocityModern=true secretLen=12
sent forwarding request, txn=241418224
answer received: txn=241418224 expected=241418224 payload=RetainedQueryAnswerPayload
verified: name=<player> uuid=<real uuid> addr=<player's real address>
```

Read it in order — each line rules out a layer:

| Symptom | Meaning |
|---|---|
| No lines at all | Nothing reached the backend. Check the proxy is actually alive — a Velocity that has logged `Shutting down the proxy` can still hold its port and accept connections it will never serve. |
| Stops after `handshake` | The backend was reached but login did not start. |
| Stops after `sent forwarding request` | The proxy never answered. Usually `player-info-forwarding-mode` is not `MODERN`. |
| `REJECTED: ...` | The answer arrived but did not verify. Usually mismatched secrets. |
| `verified:` then a kick | Login succeeded; the fault is later — the command tree or chat session traps above. |
| `verified: ... props=0` | The proxy sent no skin data. Check the proxy is in online mode. |
| `finishing login with ... props=0` after a good `verified:` | The forwarded profile was replaced between verification and login. |

Check the forwarded UUID against the account's real one
(`https://api.mojang.com/users/profiles/minecraft/<name>`). If it matches, the
forwarding path is correct and the problem is downstream of login.
