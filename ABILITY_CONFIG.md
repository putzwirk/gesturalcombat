# Ability & weapon configuration

Everything about a gesture ability — its trigger symbol, hitbox shape, damage,
knockback, cooldown, hit count — lives in JSON, not code. Two files drive it:

| File | Purpose |
| --- | --- |
| `data/gesturalcombat/abilities.json` | per-technique overrides (symbol, hitbox, damage, cooldown) |
| `data/gesturalcombat/weapons.json` | per-item / per-tag weapon profiles (archetype + stat multipliers) |

Both ship inside the jar with the tuned defaults. A datapack (or `kubejs`
style resource pack, or the world's `datapacks/` folder) with the same paths
replaces them. Run `/reload` after editing — the server re-reads both files and
pushes the result to every connected client.

Turn on `debugHitboxes` in the common config to draw the resolved hitbox in
world with end-rod particles while testing.

## abilities.json

```json
{
  "techniques": {
    "sword_slash": {
      "gesture": "LINE_H",
      "archetype": "sword",
      "hitbox": {
        "shape": "ARC",
        "range": 2.8,
        "angle": 110,
        "width": 1.0,
        "height": 1.6,
        "vertical_offset": 0.0,
        "forward_offset": 0.0,
        "max_targets": 16,
        "line_of_sight": false
      },
      "damage_multiplier": 1.0,
      "knockback": 0.25,
      "lift": 0.0,
      "cooldown_ticks": 10,
      "hits": 1,
      "hit_interval": 0,
      "windup_ticks": 0
    }
  }
}
```

Every field is optional. Omitting a key keeps the code default, so a file with
`{"techniques": {"sword_slash": {"damage_multiplier": 1.3}}}` only changes that
one number.

- `gesture` — the symbol that casts it: `LINE_H`, `LINE_V_UP`, `LINE_V_DOWN`,
  `CIRCLE`, `ZIGZAG`, `V`, `CARET`. An array assigns several symbols:
  `"gesture": ["CIRCLE", "V"]`. A symbol can only map to one technique per
  archetype; the last assignment in the file wins.
- `archetype` — moves the technique to another weapon family (`sword`, `axe`,
  `spear`, `shield`, `bow`).
- `damage_multiplier` — multiplies the player's attack damage (after mastery and
  weapon-profile multipliers).
- `cooldown_ticks` — base cooldown before config/mastery/attack-speed scaling.

### Hitbox shapes

| Shape | Geometry | Reads it well for |
| --- | --- | --- |
| `ARC` | horizontal fan in front of the player: `range` deep, `angle` wide, `height` as a vertical window centred on the eye | slashes, sweeps, cleaves |
| `VERTICAL_ARC` | vertical fan in the plane you are facing: `angle` is the vertical sweep around your crosshair pitch, `width` is the lateral thickness | rising cuts, overhead chops, slams |
| `BOX` | swept box along the look vector: `range` deep, `width` wide, `height` tall | thrusts, stabs, pierces |
| `SPHERE` | radius around the player's torso | spins, whirlwinds, ground slams |

`ARC` and `VERTICAL_ARC` both follow your **yaw** (camera facing); `VERTICAL_ARC` additionally follows your **pitch**, so looking down sweeps the ground and looking up sweeps overhead. `BOX` follows the full look vector (yaw + pitch).

Common knobs:

- `range` — reach in blocks (still multiplied by mastery range passives and the
  weapon profile's `reach_multiplier`).
- `angle` — total fan width in degrees (`360` = full circle) for `ARC`, total
  vertical sweep for `VERTICAL_ARC`. Ignored by `BOX` and `SPHERE`.
- `width` — lateral thickness. Only meaningful for `VERTICAL_ARC` (keep it
  narrow, ~0.8-1.1: the blade is thin sideways) and `BOX`.
- `height` — vertical size. For `ARC` it is the vertical window around eye
  height (mobs above/below are ignored, so an arc can't hit through a floor);
  for `VERTICAL_ARC` it is a minimum vertical reach, so a tall slash still
  catches mobs at your feet or above your head.
- `vertical_offset` — shift the region up/down from eye height.
- `forward_offset` — start the region this many blocks in front (stabs that
  should not hit an enemy pressed against your chest).
- `max_targets` — hard cap on entities hit per activation, nearest first.
- `line_of_sight` — reject targets behind blocks (on by default for `BOX`).

The query is box-aware: it tests the entity's full bounding box (all edges) and
not just its centre, then sorts by distance and truncates at `max_targets`.

### Recipes

**Wider axe cleave, more damage, same animation:**

```json
{"techniques": {"axe_cleave": {"hitbox": {"shape": "ARC", "angle": 140, "range": 3.0}, "damage_multiplier": 1.35}}}
```

**Sword execute becomes a true overhead vertical cleave** (thin sideways,
wide vertically):

```json
{"techniques": {"sword_execute": {"hitbox": {"shape": "VERTICAL_ARC", "range": 2.8, "angle": 120, "width": 0.9, "height": 2.8}}}}
```

**Spear thrust that cannot hit through walls, and leaves a gap in front:**

```json
{"techniques": {"spear_thrust": {"hitbox": {"shape": "BOX", "range": 4.8, "width": 1.6, "height": 1.2, "forward_offset": 0.2, "line_of_sight": true}}}}
```

**Swap symbols — whirl on `V`, guard on `CIRCLE`:**

```json
{"techniques": {"sword_whirl": {"gesture": "V"}, "sword_guard": {"gesture": "CIRCLE"}}}
```

## weapons.json

Profiles make *any* modded weapon usable without touching code. A profile is
matched by item id, by glob, or by item tag, in file order — first match wins.

```json
{
  "weapons": [
    {
      "items": ["simplyswords:watcher_claymore"],
      "archetype": "sword",
      "damage_multiplier": 1.15,
      "reach_multiplier": 1.1,
      "reach": 4.2,
      "cooldown_multiplier": 0.9,
      "animations": [
        "bettercombat:two_handed_slash_horizontal_left",
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_slam"
      ],
      "techniques": {
        "sword_slash": {"damage_multiplier": 1.2},
        "sword_whirl": {"hitbox": {"shape": "SPHERE", "range": 3.5}}
      }
    },
    {
      "items": ["simplyswords:*greatsword*", "#minecraft:axes"],
      "archetype": "axe",
      "reach_multiplier": 1.05
    }
  ]
}
```

- `items` — exact ids (`mod:item`), globs (`mod:*scythe*`, `*:rapier`), or tags
  (`#mod:tag`).
- `archetype` — weapon family override, checked before Better Combat's category
  and before name heuristics.
- `damage_multiplier` / `reach_multiplier` / `cooldown_multiplier` — apply to
  every ability cast with that item.
- `reach` — optional **absolute** reach in blocks. When set it replaces
  `reach_multiplier` for every ability hitbox, so `4.2` means every technique
  with this weapon reaches exactly 4.2 blocks (before the mastery range
  passive). Useful when a weapon should feel long without needing ratios.
- `animations` — Better Combat animation ids played by gesture casts with this
  weapon. Each id is classified by name (`spin`/`whirl`, `slam`, `rising`,
  `stab`/`throw`, `vertical`, `slash`/`swipe`/`cross`) and matched to the
  gesture, preferring the weapon's own handedness. Missing ids are skipped.
  See `categories` below for the shared table.

`weapons.json` also has a top-level `categories` object mapping a Better Combat
category to an animation pool. It is used for any weapon that declares that
category — from this mod or any other — but only when the pool contains
animations more specific than the generic `bettercombat:` set:

```json
"categories": {
  "chakram": [
    "simplyswords:one_handed_chakram_throw_left",
    "simplyswords:chakram_spin",
    "simplyswords:chakram_spin_long_grounded"
  ]
}
```

Resolution order per gesture: the weapon's `animations`, then its category
pool, then the archetype default. Kinds map to gestures as follows —
`CIRCLE` → spin, `LINE_H`/`ZIGZAG` → horizontal slash (or stab/throw when the
pool has no slash), `LINE_V_UP` → rising or stab/throw, `LINE_V_DOWN` → slam or
vertical slash. A gesture with no matching kind falls back to the built-in
archetype animation.
- `techniques` — per-technique overrides in exactly the same schema as
  `abilities.json`, merged on top of the global technique config.
- `specials` — gesture-bound signature moves for that specific weapon:

```json
"specials": [
  {
    "gesture": "CIRCLE",
    "cooldown_ticks": 160,
    "damage_multiplier": 1.4,
    "knockback": 0.7,
    "lift": 0.2,
    "hitbox": {"shape": "SPHERE", "range": 4.0, "max_targets": 10},
    "target_effects": [{"id": "minecraft:fire", "duration": 100, "amplifier": 0}],
    "self_effects": [{"id": "minecraft:strength", "duration": 100, "amplifier": 0}],
    "heal": 0.0,
    "sound": "minecraft:item.firecharge.use",
    "particles": "flame"
  }
]
```

Casting the matching gesture with that weapon runs the special instead of the
archetype technique, with its own per-weapon cooldown key. `damage_multiplier`
stacks on top of the normal ability damage (attack damage × mastery × weapon
profile × global config), `hitbox` is scaled by the weapon's reach and the
mastery range passive, and `cooldown_ticks` is scaled by the mastery cooldown
passive, the global config and the attack-speed factor. The resulting cooldown
is shown on that weapon's HUD icon. See `SIMPLY_SWORDS.md` for the full Simply
Swords mapping.

## How Better Combat's own compat works (and why Simply Swords works here for free)

Better Combat keys weapon data by **item id**: `data/<modid>/weapon_attributes/<item_path>.json`
is the profile for `<modid>:<item_path>`. Each file is resolved through a
`parent` chain (`data/bettercombat/weapon_attributes/sword.json` etc.), where
parent values are overridden by the child, then the merged
`WeaponAttributes(attack_range, category, attacks[], …)` is registered in
`WeaponRegistry.registrations` and synced to clients as gzip-compressed JSON.

Each attack entry carries its own `hitbox`
(`HORIZONTAL_PLANE` / `VERTICAL_PLANE` / `FORWARD_BOX`), `angle`,
`damage_multiplier`, `upswing`, `animation`, and swing sound — that is how BC
lets any weapon look and behave correctly. Items without a profile get
`WeaponAttributesFallback`, which infers from vanilla attribute modifiers
(attack damage / attack speed), and stacks can carry per-stack attributes in
NBT (`ItemStackNBTWeaponAttributes`).

Simply Swords ships one `weapon_attributes` file per weapon (plus
`BetterCombatCompat` on the mod side), so BC already knows its claymores are
`claymore`, its glaives are `glaive`, and so on. This mod reads that resolved
`category` through `compat/BetterCombatBridge`, maps the family onto its own
archetype, and only needs a `weapons.json` entry when you want to change
Gestural Combat's numbers for a specific item.
