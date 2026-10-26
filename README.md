# Gestural Combat

Swinging a sword by spamming click gets old fast, so this mod makes you draw your attacks instead. Inspired by Halloween Doodle game :D

Mod requires **[Better Combat](https://www.curseforge.com/minecraft/mc-mods/better-combat-by-daedelus)** and **[playerAnimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator)**, also fully compatible with SimplySwords.

## How it feels

It's a bit like a fighting game where the buttons are mouse strokes. Straight lines, circles, zigzags, little V and ^ shapes. Takes a few minutes to get used to, then it starts to flow and you stop thinking about the shapes at all.

Swords, axes, spears, shields and bows all play differently, and a lot of weapon types have their own tricks on top of that. A katana is not a claymore, a rapier is not a dagger. Light weapons are quick and slippery, heavy ones make you commit to every swing and hit like a truck.

Everything is unlocked from the start. No skill trees, no grinding. Pick up a weapon and try things.

## Simply Swords

This was the main thing I tested with, so Simply Swords has the most love here. Pretty much every weapon from it is set up and works out of the box.

I tried hard not to make one weapon type the obvious best pick. Every type has its own pace and strengths, quick ones deal more over time, heavy ones trade that for burst and reach, and I kept adjusting until nothing felt silly in either direction. The weapons still keep their own stats too, so a better weapon is still a better weapon, it's not all flattened out.

The special weapons also get something extra. A lot of the signature ones have a unique move tied to one of the gestures, with a long cooldown so it feels like an event and not something you spam. They're not meant to replace the normal moves, just to give those weapons their own personality.

Balance is never perfect, so if something feels too strong or too weak, tell me. Or change it yourself, it's all adjustable (see below).

## Modded weapons

Better Combat compat data is enough for the mod to recognise a weapon and give it something basic to do, but to make a weapon feel right (proper moves, reach, animations) it needs a small datapack entry.

Simply Swords is fully set up already. Born in Chaos and a few other mods have partial coverage. For anything else you'll probably want to write a profile, which is just a JSON file per mod. The format is explained in the docs on the source page. If you make one for a mod and want to share it, send it over and I'll add it.

## Config

Not much to configure. There are damage and cooldown multipliers if you want to make everything harder or easier, and the rest are debug toggles if you faced a bug.

## About how it was made

I built this with an AI agent doing the coding while I directed it, played it constantly and tuned everything by feel (like, 200+ overall prompts). Expect rough edges, and report bugs on github, please!:D

MIT license.

## Current mod state:

*   bow and crossbow abilities are broken
*   Some weapons are unfinished (have basic combos) and will be changed later to be cooler
*   Weapon balance is fine already but can be changed in the future
*   pvp is untested, but i really wanna see how it looks.
