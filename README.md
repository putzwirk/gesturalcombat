# Gestural Combat

Draw mouse gestures while holding attack to unleash weapon techniques.
Forge 1.20.1. Requires Better Combat and Player Animator.

## Gestures (hold the weapon's mouse button and move the mouse)

Main-hand weapon gestures use LMB, off-hand weapon gestures use RMB. Hold a
sword in each hand and LMB-drag for main-hand techniques, RMB-drag for
off-hand techniques. Sword plus shield: LMB for sword, RMB for shield.
A bow casts its circle gesture from whichever hand holds it; an off-hand bow
draws automatically while RMB is held and the gesture tracks once the draw is
fully maxed, while a main-hand bow is drawn with RMB and gestured with LMB.

| Symbol | Shape | Sword | Axe | Spear | Tool |
| --- | --- | --- | --- | --- | --- |
| — | horizontal line | Horizontal Slash | Wide Cleave | Spear Sweep | Trench (mines) |
| ↑ | upward stroke | Rising Cut | Rend | Lunge | Pulverize |
| ↓ | downward stroke | Executioner's Drop | Cleave | Skyfall | Shatter |
| ○ | circle | Whirlwind | Cyclone | Tempest | Quake |
| ⚡ | zigzag | Flurry | Earthsplitter | Impale | Rockslide |

All gestures are available from the start. XP is earned per cast and per kill;
level-ups are announced in chat.

## Controls

- A quick click (no drag) still performs a normal attack, so Better Combat
  combos remain usable with every weapon.
- Once the mouse moves far enough to start a stroke, attacks are suppressed
  for the duration of the drag and the technique is cast on release, so a
  gesture never doubles as a click attack. Block mining with tools is
  unaffected as long as the click does not turn into a drag.
- Multi-hit techniques (Flurry, Impale, Rockslide, Whirlwind, Cyclone,
  Tempest) play one Better Combat swing per hit, alternating left/right
  variants, timed to the damage pulses.
- The bottom strip shows the five gestures for the held weapon, tinted by
  weapon family, with cooldown wipe, cast pulse and lock shake. No text.

## Modded weapons

Weapon family is resolved from Better Combat's weapon attributes when present
(SimplySwords and other BC-supported weapons work automatically), falling back
to vanilla item classes (sword/axe/trident/pickaxe/shovel/hoe) and finally to
registry-name matching for modded weapons that ship no Better Combat attributes.

## Particles and trails

Gesture casts play archetype-specific particles instead of the vanilla sweep
particle — piercing sword streaks for horizontal, upward and downward strikes,
spinning arcs for circles — and leave a pixelated ribbon that is swept directly
from the rendered blade each frame. See
`PARTICLES_AND_TRAILS.md`; the trail can be disabled with `weaponTrails` in the
common config.

## Dev environment notes

Better Combat 1.9.0 and Player Animator 1.0.2-rc1 are SRG-compiled Forge jars.
ModDevGradle's official-mapped dev runtime cannot load them as-is: their
prebuilt refmaps lack field entries, so `@Shadow` fields fail to resolve.
`run/mods` therefore contains dev-only copies produced by
`/tmp/opencode/remap` (tiny-remapper SRG->official using the project's
intermediateToNamed.srg, refmap field-entries patch, ASM rename of remaining
SRG shadow fields to official names). Production (real instances) uses the
unmodified jars, where SRG names resolve natively. Do not ship the dev copies.
`mmt cp <instance>` deploys only this mod's jar.
