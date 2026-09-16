# Changelog

## 1.0.2

Fixed:
CurseForge file relations no longer incorrectly list ESC and ESL as required. ESN only needs Minecraft and the loader.

## 1.0.1

Changed:
No transport changes. Public API stays `EsnChannels` / `EsnChannel` (sendToServer, sendTo, fan-out). Lobby sessions stay in ESL; gameplay stays in the consuming mod.

## 1.0.0

Added:
Shared network fabric API (`EsnChannels` / `EsnChannel`) for consumer mods: register packets, sendToServer, sendTo a player, fan-out to many players.
Forge 1.20.1 SimpleChannel backend and NeoForge 1.21.1 PayloadRegistrar twin with the same public `Esn*` surface.
No lobby, wave, airdrop, or HUD types inside ESN — transport only.
