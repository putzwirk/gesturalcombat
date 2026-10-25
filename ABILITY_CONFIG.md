# Ability & weapon configuration

This mod's combat numbers are data, not code. Two JSON files hold them, both
shipped inside the jar at these paths:

```
data/gesturalcombat/abilities.json     # one config per technique (the mod's moves)
data/gesturalcombat/weapons.json       # one profile per weapon item / tag / glob
```

Override either file from a datapack with the same path and the mod reads yours
instead. That works for a world datapack (`<world>/datapacks/<pack>/data/...`,
`pack_format` 15) and for a mod that ships the same file. Edit, then run
`/reload`: the server re-reads both files and pushes the result to every
connected client (the client never reads its own copy of the numbers, so
multiplayer stays in sync).

You do **not** need to recompile anything to rebalance a technique.

Two config flags help while you work: `debugHitboxes` draws the resolved hitbox
in world, `debugCasts` logs every cast with its id, family and shape plus how
many targets each arc actually caught.

---

## abilities.json

### Shape of the file

```json
{
  "techniques": {
    "sword_slash": {
      "gesture": "LINE_H",
      "damage_multiplier": 1.0,
      "knockback": 0.25,
      "cooldown_ticks": 10
    },
    "axe_cleave": {
      "hitbox": {
        "shape": "ARC",
        "range": 3.0,
        "angle": 140,
        "height": 1.8,
        "max_targets": 12
      }
    }
  }
}
```

Each key is a **technique id** (see `/reload` log line or the ids in
`Techniques.java`; `debugCasts` prints them as you play). Every field is
optional — anything you leave out keeps the value the code was written with, so
a one-line file that only bumps `damage_multiplier` is a valid override.

Unknown technique ids are ignored rather than rejected, so a typo costs you
nothing but a silent no-op. Check the log: `Loaded ability configs: N techniques
configured, M weapon profiles` tells you the files parsed at all, and
`debugCasts` tells you which id fired.

### Technique fields

| Key | Type | Unit | What it does |
| --- | --- | --- | --- |
| `gesture` | string or array | — | Symbol that casts the move: `LINE_H`, `LINE_V_UP`, `LINE_V_DOWN`, `CIRCLE`, `ZIGZAG`, `V`, `CARET`. |
| `archetype` | string | — | Moves the technique to another class: `sword`, `axe`, `spear`, `shield`, `bow`. |
| `hitbox` | object | — | Replaces the default hit region; see below. |
| `damage_multiplier` | number | × | Multiplies the player's attack damage for this move. |
| `knockback` | number | blocks/tick | Push strength. **Below 0.3 this stops being a shove and becomes a stun** (see the note at the bottom). |
| `lift` | number | blocks/tick | Upward push. Any `lift` above 0 also forces a real shove instead of a stun. |
| `self_knockback` | number | blocks/tick | Push applied to the caster (lunge/retreat moves). |
| `cooldown_ticks` | int | ticks (20 = 1 s) | Base cooldown before all multipliers. |
| `hits` | int | count | Number of damage pulses. `1` = a single hit. |
| `hit_interval` | int | ticks | Gap between pulses for multi-hit moves. |
| `hit_interval_end` | int | ticks | Gap for the *last* pulse; pulses ramp linearly from `hit_interval` to this. Use it for accelerating combos. |
| `windup_ticks` | int | ticks | Delay before the first hit resolves. Also offsets the animation in the client. |

`gesture` is only about dispatch. The *first* entry in an array becomes the
technique's own symbol; extra entries additionally bind that symbol to the same
technique for family-less (archetype-level) moves. A technique that belongs to a
weapon family (`claymore`, `sickle`, `sai`, …) keeps its family identity — you
can move its symbol, but you can't hand it to a different family this way.

### Multi-hit timing

With `hits: 3`, `hit_interval: 5`, `hit_interval_end: 2`, `windup_ticks: 4`,
the pulses land at tick 4, 9 and 14 from the cast, and the client plays a swing
for each. Damage is divided evenly (`total / hits`) unless the effect itself
handles ramping, so raising `hits` on its own does not raise total damage — it
splits the same damage into more, smaller hits with more stun and more animation.
To make a move actually stronger, raise `damage_multiplier`.

The same delay array drives the whoosh sounds and the number of Better Combat
swings, which is why `hits` and the animation always match.

### Hitbox shapes

| `shape` | Geometry | Good for |
| --- | --- | --- |
| `ARC` | Horizontal fan in front of the player: `range` deep, `angle` wide, `height` tall as a window around eye level | slashes, sweeps, cleaves |
| `VERTICAL_ARC` | Vertical fan in the facing plane: `angle` is the up/down sweep, `width` the sideways thickness | overhead chops, rising cuts, slams |
| `BOX` | Box swept along the look vector: `range` deep, `width` wide, `height` tall | thrusts, stabs, pierces |
| `SPHERE` | Sphere around the player's torso | spins, whirlwinds, ground slams |

`ARC` and `VERTICAL_ARC` follow your yaw only; `VERTICAL_ARC` also follows pitch,
so looking down sweeps the floor and looking up sweeps overhead. `BOX` follows
the full look direction.

| Key | Default | Notes |
| --- | --- | --- |
| `range` | shape-dependent | Reach in blocks, still multiplied by the weapon's reach. |
| `angle` | 110–360 | Total fan width in degrees; ignored by `BOX` and `SPHERE`. |
| `width` | 1.0 | Sideways thickness. Keep thin (~0.8–1.1) for vertical arcs and boxes. |
| `height` | 1.4–3.0 | Vertical size. For `ARC` this is the window around eye height, so an arc cannot hit through a floor. |
| `vertical_offset` | 0.0 | Shift the whole region up/down from eye height. |
| `forward_offset` | 0.0 | Start the region this far in front, for stabs that shouldn't hit something pressed against your chest. |
| `max_targets` | 6–16 | Hard cap per pulse, nearest first. |
| `line_of_sight` | shape default | Reject targets behind blocks. Moves that build a `BOX` for a thrust set it on; arcs leave it off. |

The query tests the target's whole bounding box, not its centre point, then
sorts by distance and truncates at `max_targets`.

### Recipes

**Make the axe cleave wider and stronger, same animation:**

```json
{"techniques": {"axe_cleave": {"hitbox": {"shape": "ARC", "angle": 140, "range": 3.0}, "damage_multiplier": 1.35}}}
```

**Turn the sword finisher into a true overhead cleave** (thin sideways, tall
vertically, follows your pitch):

```json
{"techniques": {"sword_execute": {"hitbox": {"shape": "VERTICAL_ARC", "range": 2.8, "angle": 120, "width": 0.9, "height": 2.8}}}}
```

**A spear thrust that respects walls and leaves a gap at your chest:**

```json
{"techniques": {"spear_thrust": {"hitbox": {"shape": "BOX", "range": 4.8, "width": 1.6, "height": 1.2, "forward_offset": 0.2, "line_of_sight": true}}}}
```

**Swap two symbols:**

```json
{"techniques": {"sword_whirl": {"gesture": "V"}, "sword_guard": {"gesture": "CIRCLE"}}}
```

**A slower, heavier finisher with an extra pulse:**

```json
{"techniques": {"katana_execute": {"damage_multiplier": 2.1, "hits": 3, "hit_interval": 4, "hit_interval_end": 8, "cooldown_ticks": 70}}}
```

---

## weapons.json

Profiles let any modded weapon use the mod properly without touching code.
Profiles are tested in file order and the **first match wins**.

```json
{
  "categories": {
    "chakram": [
      "simplyswords:one_handed_chakram_throw_left",
      "simplyswords:chakram_spin"
    ]
  },
  "weapons": [
    {
      "items": ["simplyswords:watcher_claymore"],
      "archetype": "sword",
      "family": "claymore",
      "damage_multiplier": 1.15,
      "reach_multiplier": 1.1,
      "cooldown_multiplier": 0.9,
      "animations": [
        "bettercombat:two_handed_slash_horizontal_left",
        "bettercombat:two_handed_slam"
      ],
      "techniques": {
        "claymore_sweep": {"damage_multiplier": 1.2}
      },
      "specials": [
        {
          "gesture": "CIRCLE",
          "cooldown_ticks": 160,
          "damage_multiplier": 1.4,
          "knockback": 0.7,
          "hitbox": {"shape": "SPHERE", "range": 4.0, "max_targets": 10}
        }
      ]
    },
    {
      "items": ["*:*greatsword*", "#minecraft:axes"],
      "archetype": "axe"
    }
  ]
}
```

### Matching

`items` entries can be:

- an exact id: `simplyswords:iron_sai`
- a glob: `*:*scythe*`, `simplyswords:*claymore*`, `*:rapier`
- a tag: `#minecraft:axes`

A profile only applies to whatever matched; nothing is inferred from a partial
match beyond the glob.

### Profile fields

| Key | Default | What it does |
| --- | --- | --- |
| `archetype` | inferred | Forces the weapon class (`sword`/`axe`/`spear`/`shield`/`bow`). Checked before Better Combat and before name heuristics. |
| `family` | inferred | Forces the mod's weapon family, which selects the kit (`claymore`, `sickle`, `katana`, …). Set this if the item's id doesn't contain a recognisable word. |
| `damage_multiplier` | 1.0 | Scales every cast's damage with this item. |
| `reach_multiplier` | 1.0 | Scales every cast's hitbox range. |
| `cooldown_multiplier` | 1.0 | Scales every cast's cooldown. |
| `reach` | unset | **Absolute** reach in blocks. When set it replaces `reach_multiplier` for every ability. Leave it out to inherit the weapon's Better Combat `attack_range`. |
| `animations` | empty | Better Combat animation ids this weapon should use; see below. |
| `techniques` | empty | Per-technique overrides with exactly the same schema as `abilities.json`, layered on top of the global values. |
| `specials` | empty | Signature moves for this weapon only; see below. |

### Animations

There are three sources, checked in this order:

1. the profile's own `animations` list,
2. the shared pool for the weapon's Better Combat category (top-level
   `categories`),
3. the built-in archetype animation.

Ids are classified by name — `spin`/`whirl`, `slam`/`smash`/`overhead`,
`rising`/`upper`, `stab`/`thrust`/`throw`, `vertical`, `slash`/`swipe`/`cross` —
then matched to the gesture: `CIRCLE` wants a spin, `LINE_H` and `ZIGZAG` want
horizontal slashes (a stab or throw is used if the pool has no slash), `LINE_V_UP`
wants a rising cut or stab, `LINE_V_DOWN` wants a slam or vertical slash. When a
gesture has no matching kind in the pool, the archetype default plays instead.
Ids that don't resolve in the animation registry are skipped silently, so a
typo'd animation id just means you get the default swing.

Multi-hit techniques repeat the ordered pool, so a three-entry pool gives you
three different swings per three-hit move.

### Specials

A special replaces the normal technique for one gesture on one weapon. It has
its own cooldown key (so it doesn't share the archetype's cooldown) and does not
need a `Techniques.java` entry.

| Key | Default | Notes |
| --- | --- | --- |
| `gesture` | required | Symbol that triggers it. |
| `cooldown_ticks` | 120 | Minimum 20; still scaled by config and attack speed. |
| `damage_multiplier` | 1.0 | Stacks on top of the normal ability damage. |
| `knockback` | 0.4 | Push on hit. |
| `lift` | 0.0 | Upward push. |
| `heal` | 0.0 | Flat health restored to the caster on cast. |
| `sound` | none | Sound event id played on cast, e.g. `minecraft:item.firecharge.use`. Unknown ids are ignored. |
| `particles` | none | One of `flame`, `soul`, `soul_fire_flame`, `crit`, `explosion`, `electric_spark`, `snowflake`, `enchanted_hit`, `witch`, `end_rod`. |
| `hitbox` | `SPHERE` 3.2 / 360° / max 10 | Same keys as technique hitboxes. |
| `target_effects` | empty | `[{"id": "minecraft:fire", "duration": 100, "amplifier": 0}]`, applied to everything hit. |
| `self_effects` | empty | Same shape, applied to the caster. |

Special hits always pin targets briefly (stun ticks, or the technique's hit
interval minus two, whichever is longer), so a signature move reads as a heavy
blow even when its `knockback` is small.

---

## How the numbers actually combine

Damage for one pulse:

```
attack_damage (player attribute)
  × profile.damage_multiplier
  × family weight factor      (0.67-1.36, per family, see WeaponRules.TUNING)
  × global damageMultiplier   (config)
  × technique damage_multiplier (abilities.json, or the effect's own default)
  ÷ hits
```

Reach for one pulse:

```
profile.reach (absolute, if set, × family reach factor)
  else hitbox.range × profile.reach_multiplier × family reach factor
```

Cooldown:

```
base cooldown_ticks
  × global cooldownMultiplier (config)
  × attack-speed factor  (= reference_speed / your weapon speed, clamped to 1.0 max, i.e. fast weapons never get slower)
  × profile.cooldown_multiplier
  × family cooldown factor
  × 0.92 if both hands hold the same non-heavy family
```

Family weight and cooldown factors live in `weapon/WeaponRules.java` (`TUNING`),
not in JSON. That table is what makes a claymore hit harder and recover slower
than a sword while both use the same ability numbers.

Because damage is computed from the player's attack attribute, a technique is
automatically strong with a strong weapon and weak unarmed. `damage_multiplier`
is a relative knob, not a flat damage number.

---

## Gotchas worth knowing

- **`knockback` under 0.3 is a stun, not a shove.** With no `lift`, small
  knockback values pin the target for the weapon's stun ticks instead of pushing
  it. If a move unexpectedly freezes mobs, check that number. Giving it any
  `lift` forces a real shove.
- **Ticks, not seconds.** 20 ticks = 1 second.
- **Hits split damage.** More hits on the same multiplier means the same total
  damage in smaller pieces.
- **First match wins** in `weapons.json`, so put specific profiles above globs
  and tags.
- **Families own their kits.** A family technique can be re-tuned or moved to
  another symbol, but a family that defines its own move for a symbol
  (`sickle` + `CARET`) shadows the archetype move for that symbol.
- **The server owns the numbers in multiplayer.** It parses the files and syncs
  the result; clients only fall back to their own bundled copy before the first
  sync, for HUD previews. If a player sees old numbers, the server needs to
  `/reload` (or they need to reconnect).
- **Datapacks need `pack_format` 15** for 1.20.1 or the pack won't load at all.

---

## Contributing: adding a technique

Data covers re-tuning existing moves. A *new* move needs a `TechniqueEffect`
implementation:

1. Write an effect in `technique/effect/` (or reuse one) — it receives the
   player, level, held stack and technique, resolves its own hitbox and damage.
2. Register it in `Techniques.java`:
   `add(new Technique(id, archetype, shape, level, cooldown, nodeId, xp, effect, family))`.
   `level`, `nodeId` and `xp` are unused leftovers from the removed progression
   system; pass `1`, `null`, `0`.
3. Add an `abilities.json` entry so it becomes tunable without a recompile.
4. Map an animation in `client/anim/AnimationCatalog.java` if the default
   archetype swing isn't right.
5. Give it a HUD icon alias in `client/hud/AbilityIcons.java` (falls back to the
   archetype icon).
6. Run `python3 tools/balance_sim.py` and check `--dual` too. It reads
   `Techniques.java` plus `abilities.json` and reports damage per second, stun
   share and the longest window the target has to act, per family. Numbers
   outside the weight-class bands are almost always a cooldown/damage mismatch
   rather than a hitbox problem.
