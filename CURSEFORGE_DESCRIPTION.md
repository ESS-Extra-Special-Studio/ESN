# CurseForge copy — ExtraSpecialNetwork (ESN)

Same text as Desktop `Minecraft/ExtraSpecialNetwork/CURSEFORGE_DESCRIPTION.md`.

## Short summary

Shared networking fabric for Extra Special Studio mods—channels, packets, and fan-out with no lobby, wave, or gameplay types of its own.

## Main page

ExtraSpecialNetwork

Shared multiplayer networking for Extra Special Studio mods—packet channels, targeted delivery, and group fan-out so each feature mod does not reinvent the same glue.

ExtraSpecialNetwork (ESN) is a library mod. It does not add gameplay, items, screens, lobbies, waves, or a “main feature” by itself. Other Extra Special Studio mods (and third-party mods that opt in) depend on it for a thin shared network fabric so sync and fan-out stay consistent across the stack.

What it does:

ESN ships a small Esn* API: EsnChannels to register a channel per consumer mod, EsnChannel to register packet codecs and handlers, and sendToServer / sendTo(player) / sendTo(players) for client↔server and group delivery. It has zero lobby, wave, airdrop, HUD, or feature types—those stay in ESL (sessions) and in the consuming mod. ESN has zero GUI or render code.

How it works:

Dependent mods register their own packets on an ESN channel and send them to one player or many. Session membership and ready-checks live in ExtraSpecialLib (ESL); ESN only moves bytes to the right players. ESN does not open menus, decide game rules, or own encounter meaning.

How to use it:

Players: If a mod’s page says ExtraSpecialNetwork / ESN is required (for example multiplayer airdrop lobbies), add the same Minecraft + loader (Forge / NeoForge) build the author tested with. You will not get a new menu from ESN alone—it is there because another mod needs multiplayer sync.
Authors (Extra Special Studio / third-party): Declare ESN as a dependency when you need shared packets or fan-out. Use EsnChannels / EsnChannel for transport only. Put lobby/session logic in ESL (or your own domain code); put gameplay meaning in your mod. Match loader and game version with the mods that require it (Forge and NeoForge variants are separate files).

Important:

ESN is a dependency, not a standalone content pack. Pair it with ExtraSpecialLib when you need lobbies or wave sessions. Match loader and Minecraft version to the consuming mod. If something fails to load, check the mod that depends on ESN first, then confirm ESN version and loader match the pack. License: All Rights Reserved (studio library).
