# Gestural Combat

Forge and Fabric 1.20.1. Hold attack, draw a shape with the mouse, and the weapon does a
technique instead of a normal swing. Needs Better Combat and Player Animator.

Heads up: this mod was made with AI. I directed an AI agent through it, played
everything in game and tuned the numbers myself, but nobody has read the code
line by line the way a hand-written mod gets read. It works and it's fun, and
it may still have rough edges I haven't hit yet.

## Playing

LMB draws main-hand gestures, RMB draws off-hand ones. Clicking normally still
swings, so Better Combat combos keep working - the gesture only starts once the
mouse has moved far enough that it can't be a plain click, and then no click
attack fires.

The shapes are the ones you'd expect: a straight line, a stroke up, a stroke
down, a circle, a Z, a V and a ^. Circles and Zs are the multi-hit moves, V is a
guard and ^ is a step. Everything is unlocked from the first cast - there is no
leveling or skill tree, though the code still carries the fields for one.

Swords get a cut, rising cut, executioner's drop, whirlwind, flurry, a guard on
V and a skirmish step on ^. Axes get wide cleave, rend, cleave, cyclone, quake,
a brace on V and a step on ^. Spears and tridents get sweep, lunge, skyfall,
tempest and impale, and tridents take a ward on V - they have no step, so their
^ slot is empty. Warglaives, glaives and halberds trade the step for reach and a
deeper slam. Anything that isn't a weapon or a shield - a pickaxe, a shovel, a
bare hand - gets nothing, and clicking works as normal.

Bows cast a fire arrow with a circle: it burns on the way, ignites what it hits
and drops a short-lived patch of fire on impact. Crossbows answer the same
circle differently - one heavy bolt that punches through a whole line of mobs,
knocks them all back and then needs a long reload before it fires again.

The strip along the bottom of the screen is the held weapon's kit: cooldown
wipes, a pulse when something fires, a shake when the slot is locked, and no
text at all.

## How it feels

Heavy weapons commit. Claymore, greataxe, greathammer, heavy axe, mace, glaive
and halberd all have a longer recovery after every move, and their circle, V and
^ slots are locked - you get the big committed swings and nothing else. Light
and one-handed weapons are faster to recover and step further, quick weapons
like daggers and rapiers further still.

Downward attacks with a heavy weapon crater the ground. The blocks under the
impact lift, tilt and hang in the air for a moment before dropping back, dirt
and stone spray out in a ring, and anything standing close gets shoved. The
terrain itself is never modified, so a crash can't leave holes in your world.

If you're in the air when you cast it - two blocks of clearance or more - you
dive instead of swinging where you stand, slam on landing, and lose only a
fraction of the usual fall damage. Off a cliff that's the difference between a
crater and a death screen.

Anything stunned for more than a quarter second gets dizzy stars spinning over
its head, so you can tell a real stagger from a flinch without reading numbers.

## Two weapons

Hold the same family in both hands and the pair starts working together:
cooldowns drop slightly, and casting the same gesture from the other hand within
half a second gets you a free echoed swing. Two-handed weapons never pair.

The V guard becomes a two-weapon brace as well. Swords cross both blades and
ignore everything that hits you for a second. Tridents level both and answer a
blocked hit with a short lunge and an impale. Axes brace wide, bank a stack for
every hit they eat, then dump it as a spin.

Beyond that: chakrams throw and recall (the Z saw parks on each hit, knocks the
target back and catches up again), shields bash things away without stunning
them, and bows fire their own way.

## Config

`config/gesturalcombat-common.toml` has the damage and cooldown multipliers,
`weaponTrails` for the swing ribbon, and `debugHitboxes` / `debugCasts` if you
want to see what the server thinks you just did. `debugWeapons` turns on the
weapon self-check: on every load it walks every weapon in the pack and reports
which gestures each one actually has, then tells you which ones are broken —
a slot whose move never resolves, a slot with no icon of its own, a weapon with
two slots drawn from the same icon, or a cast that was swung with a mob in range
and hit nothing.

## Disabling a player

`/gesturalcombat disable <player>` turns the mod off for that player on the
server. Their client stops drawing the HUD, stops reading gestures, and stops
holding back vanilla clicks, and the server refuses their casts, so they play
with plain Better Combat attacks. `/gesturalcombat enable <player>` puts them
back and `/gesturalcombat disabled` lists who is off. The list lives in
`config/gesturalcombat-disabled.json` and survives restarts.

## Modded weapons

Each supported mod has its own file under `data/gesturalcombat/weapons/`
(vanilla, Simply Swords, Born in Chaos), so adding another one is a single JSON
file rather than an edit to one giant table. See `ABILITY_CONFIG.md`.

A profile pins everything the moveset depends on: `family`, `archetype`,
`two_handed`, `animations`, the gesture allow-list and the tuning multipliers.
Better Combat and Simply Swords are only read for the items they have that we
have not profiled yet, so a dependency update cannot silently re-balance or
re-animate a weapon we already know.

## Version parity

Every port must produce exactly the same moveset as the original 1.20.1 build.
The rules that keep that true:

- Animation choice is a pure function of our own data. It never consults the
  animation registry, so it cannot change with load order, thread, or a
  dependency's asset pack. A pinned animation that is missing is reported, not
  replaced by a different one.
- Family, archetype and two-handedness come from the profile, not from Better
  Combat's live attributes.
- The pools in `00-categories.json` are ours, keyed by our family name.
- Better Combat 2.x scales the wind-up differently from 1.9 for the same
  arguments, so the length handed to it is normalised back to the 1.9 timing.

To compare two builds, set `debugWeapons` and run `/gesturalcombat audit` on
each one. Every weapon logs a `parity:` line with its family, handedness and the
exact animation sequence per gesture; diff the two dumps and fix every line that
is not identical. Ids ending in `!` are pinned animations that failed to
register.

## Dev notes

The project is a multiloader build: `common/` holds every technique, weapon rule
and client effect, while `fabric/` and `forge/` only wire loader APIs (registry,
networking, events, config directory) into it. Compile both with `./gradlew build`;
the jars land in `fabric/build/libs` and `forge/build/libs` as
`gesturalcombat-<loader>-1.20.1-<version>.jar`.

Better Combat 1.9.0 and Player Animator 1.0.2-rc1 are SRG-compiled Forge jars
that ModDevGradle's official-mapped dev runtime can't load as-is, because their
prebuilt refmaps lack field entries and `@Shadow` fields fail to resolve.
`run/mods` holds dev-only copies produced by a tiny-remapper SRG->official pass;
real instances use the untouched jars, where SRG resolves natively. Don't ship
the dev copies - deploying only ever copies this mod's own jar.
