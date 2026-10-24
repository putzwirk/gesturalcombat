#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INVENTORY = ROOT / "simplyswords_inventory.json"
OUT = ROOT / "src/main/resources/data/gesturalcombat/weapons.json"

BASE_REACH = {"sword": 2.5, "axe": 2.9, "spear": 3.5}
MIN_FACTOR = 0.70
MAX_FACTOR = 1.80

CATEGORY = {
    "sword": "sword", "sickle": "sword", "katana": "sword", "rapier": "sword",
    "soul_knife": "sword", "twinblade": "sword", "battlestaff": "sword", "chakram": "sword",
    "claymore": "sword", "warglaive": "spear", "glaive": "spear", "halberd": "spear",
    "trident": "spear", "spear": "spear", "scythe": "axe", "greathammer": "axe",
    "mace": "axe", "greataxe": "axe", "heavy_axe": "axe",
}


def hitbox(gesture):
    if gesture == "CIRCLE":
        return {"shape": "SPHERE", "range": 4.0, "max_targets": 10}
    if gesture == "LINE_V_DOWN":
        return {"shape": "VERTICAL_ARC", "range": 3.2, "angle": 120, "width": 1.0, "height": 2.8, "max_targets": 8}
    if gesture == "LINE_V_UP":
        return {"shape": "ARC", "range": 3.1, "angle": 100, "height": 3.4, "vertical_offset": 0.2, "max_targets": 8}
    if gesture == "LINE_H":
        return {"shape": "ARC", "range": 3.3, "angle": 130, "height": 3.4, "vertical_offset": 0.2, "max_targets": 8}
    if gesture == "ZIGZAG":
        return {"shape": "ARC", "range": 3.0, "angle": 110, "height": 3.4, "vertical_offset": 0.2, "max_targets": 6}
    if gesture == "V":
        return {"shape": "ARC", "range": 2.8, "angle": 90, "height": 3.2, "vertical_offset": 0.2, "max_targets": 6}
    return {"shape": "BOX", "range": 3.6, "width": 1.6, "height": 2.0, "max_targets": 4}


def family(text):
    t = (text or "").lower()
    if any(k in t for k in ["fire", "flame", "ember", "burn", "brimstone", "inferno", "pyre", "cinder"]):
        return "fire"
    if any(k in t for k in ["frost", "chill", "freeze", "frozen", "ice", "glacial"]):
        return "frost"
    if any(k in t for k in ["storm", "lightning", "thunder", "tempest", "galeforce", "cyclone", "vortex", "wind"]):
        return "storm"
    if any(k in t for k in ["soul", "lich", "anguish", "necro", "wraith", "spirit"]):
        return "soul"
    if any(k in t for k in ["poison", "plague", "toxic", "venom", "blight"]):
        return "poison"
    if any(k in t for k in ["levitat", "astral", "starcall", "celestial", "gravity"]):
        return "astral"
    if any(k in t for k in ["bleed", "blood", "crimson", "vampiric", "lifesteal", "leech", "devour"]):
        return "blood"
    if any(k in t for k in ["hunt", "omen", "shadow", "void", "rift", "enigma"]):
        return "shadow"
    return "impact"


SPECS = {
    "fire": ({"id": "minecraft:fire", "duration": 100, "amplifier": 0}, "flame", "minecraft:item.firecharge.use"),
    "frost": ({"id": "minecraft:slowness", "duration": 120, "amplifier": 1}, "snowflake", "minecraft:block.glass.break"),
    "storm": ({"id": "minecraft:glowing", "duration": 80, "amplifier": 0}, "electric_spark", "minecraft:item.trident.thunder"),
    "soul": ({"id": "minecraft:wither", "duration": 80, "amplifier": 0}, "soul", "minecraft:entity.wither.spawn"),
    "poison": ({"id": "minecraft:poison", "duration": 120, "amplifier": 0}, "witch", "minecraft:entity.spider.ambient"),
    "astral": ({"id": "minecraft:levitation", "duration": 40, "amplifier": 0}, "end_rod", "minecraft:block.amethyst_block.chime"),
    "blood": ({"id": "minecraft:wither", "duration": 60, "amplifier": 0}, "crit", "minecraft:entity.player.attack_crit"),
    "shadow": ({"id": "minecraft:blindness", "duration": 80, "amplifier": 0}, "enchanted_hit", "minecraft:entity.enderman.teleport"),
    "impact": ({"id": "minecraft:glowing", "duration": 60, "amplifier": 0}, "crit", "minecraft:entity.player.attack_strong"),
}
SELF = {
    "blood": [{"id": "minecraft:regeneration", "duration": 100, "amplifier": 0}],
    "storm": [{"id": "minecraft:speed", "duration": 100, "amplifier": 0}],
    "soul": [{"id": "minecraft:absorption", "duration": 120, "amplifier": 0}],
    "fire": [{"id": "minecraft:strength", "duration": 100, "amplifier": 0}],
    "frost": [{"id": "minecraft:resistance", "duration": 100, "amplifier": 0}],
    "shadow": [{"id": "minecraft:invisibility", "duration": 80, "amplifier": 0}],
}
CATEGORY_BASE = {
    "anchor": ["bettercombat:two_handed_slam_heavy"],
    "axe": ["bettercombat:one_handed_slash_horizontal_left", "bettercombat:one_handed_slash_horizontal_right"],
    "battlestaff": [
        "bettercombat:two_handed_stab_left",
        "bettercombat:two_handed_stab_right",
        "bettercombat:two_handed_slash_horizontal_left",
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_spin",
        "bettercombat:two_handed_slam",
    ],
    "claw": ["bettercombat:one_handed_punch", "bettercombat:one_handed_swipe_horizontal_right"],
    "claymore": [
        "bettercombat:two_handed_stab_left",
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_slam",
    ],
    "coral_blade": [
        "bettercombat:one_handed_slash_horizontal_left",
        "bettercombat:one_handed_slash_horizontal_right",
        "bettercombat:one_handed_uppercut_right",
    ],
    "dagger": [
        "bettercombat:dual_handed_stab",
        "bettercombat:one_handed_slash_horizontal_left",
        "bettercombat:one_handed_slash_horizontal_right",
    ],
    "double_axe": ["bettercombat:two_handed_slash_horizontal_right", "bettercombat:two_handed_spin"],
    "fist": ["bettercombat:one_handed_punch"],
    "glaive": [
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_slash_vertical_left",
        "bettercombat:two_handed_slash_vertical_right",
    ],
    "halberd": ["bettercombat:two_handed_stab_right", "bettercombat:two_handed_slash_vertical_left"],
    "hammer": ["bettercombat:two_handed_slam"],
    "heavy_axe": ["bettercombat:two_handed_slash_horizontal_right", "bettercombat:two_handed_spin"],
    "katana": [
        "bettercombat:two_handed_slash_horizontal_left",
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_slash_vertical_left",
        "bettercombat:two_handed_slash_vertical_right",
    ],
    "lance": ["bettercombat:one_handed_stab", "bettercombat:one_handed_stab_mounted"],
    "mace": [
        "bettercombat:one_handed_slash_horizontal_left",
        "bettercombat:one_handed_slash_horizontal_right",
        "bettercombat:one_handed_slam",
    ],
    "pickaxe": ["bettercombat:one_handed_slam"],
    "rapier": ["bettercombat:one_handed_stab", "bettercombat:one_handed_slash_horizontal_left"],
    "scythe": ["bettercombat:two_handed_slash_horizontal_left", "bettercombat:two_handed_slash_horizontal_right"],
    "sickle": [
        "bettercombat:dual_handed_slash_cross",
        "bettercombat:dual_handed_slash_uncross",
        "bettercombat:one_handed_slash_horizontal_left",
        "bettercombat:one_handed_slash_horizontal_right",
    ],
    "soul_knife": ["bettercombat:one_handed_stab"],
    "spear": ["bettercombat:two_handed_stab_right"],
    "staff": [
        "bettercombat:two_handed_slash_horizontal_left",
        "bettercombat:two_handed_slash_horizontal_right",
        "bettercombat:two_handed_slam",
    ],
    "sword": [
        "bettercombat:one_handed_stab",
        "bettercombat:one_handed_slash_horizontal_left",
        "bettercombat:one_handed_slash_horizontal_right",
    ],
    "trident": ["bettercombat:one_handed_stab"],
    "twin_blade": [
        "bettercombat:one_handed_slash_switch_blade_left",
        "bettercombat:one_handed_slash_switch_blade_right",
    ],
    "wand": ["bettercombat:one_handed_stab"],
}

CATEGORY_EXCLUDE = {("scythe", "simplyswords:one_handed_chakram_throw_left")}

DAMAGE = {"CIRCLE": 1.35, "LINE_V_DOWN": 1.55, "LINE_V_UP": 1.25, "LINE_H": 1.20,
          "ZIGZAG": 1.30, "V": 1.00, "CARET": 1.15}
KNOCK = {"CIRCLE": 0.7, "LINE_V_DOWN": 0.9, "LINE_V_UP": 0.35, "LINE_H": 0.5,
         "ZIGZAG": 0.4, "V": 0.2, "CARET": 0.6}

TUNED = {
    "simplyswords:twisted_blade": [
        {
            "gesture": "ZIGZAG",
            "cooldown_ticks": 120,
            "damage_multiplier": 1.45,
            "knockback": 1.2,
            "lift": 0.25,
            "hitbox": {"shape": "SPHERE", "range": 3.8, "max_targets": 8},
            "target_effects": [{"id": "minecraft:glowing", "duration": 80, "amplifier": 0}],
            "self_effects": [
                {"id": "minecraft:speed", "duration": 140, "amplifier": 1},
                {"id": "minecraft:strength", "duration": 120, "amplifier": 0},
            ],
            "heal": 0.0,
            "sound": "minecraft:entity.player.attack_crit",
            "particles": "crit",
        }
    ],
}


def build_categories(inventory):
    pools = {}
    for entry in inventory["weapons"]:
        category = entry.get("category")
        if not category:
            continue
        pool = pools.setdefault(category, [])
        for animation in entry.get("attack_animations") or []:
            if (category, animation) in CATEGORY_EXCLUDE:
                continue
            if animation not in pool:
                pool.append(animation)
    for category, extra in CATEGORY_BASE.items():
        pool = pools.setdefault(category, [])
        for animation in extra:
            if animation not in pool:
                pool.append(animation)
    for category, pool in pools.items():
        pool.sort(key=lambda animation: 0 if animation.startswith("simplyswords:") else 1)
    return pools


def build():
    inventory = json.loads(INVENTORY.read_text())
    weapons = []
    for entry in inventory["weapons"]:
        category = entry.get("category")
        archetype = CATEGORY.get(category, "sword")
        base = BASE_REACH[archetype]
        reach = entry.get("attack_range") or base
        profile = {
            "items": [entry["id"]],
            "archetype": archetype,
            "damage_multiplier": 1.0,
            "reach_multiplier": round(max(MIN_FACTOR, min(MAX_FACTOR, reach / base)), 2),
            "cooldown_multiplier": 1.0,
        }
        animations = [animation for animation in (entry.get("attack_animations") or [])
                      if category == "chakram" or "chakram" not in animation]
        if animations:
            profile["animations"] = animations
        if entry["id"] in TUNED:
            profile["specials"] = TUNED[entry["id"]]
        elif entry.get("arsenal_role") == "signature" and entry.get("special") and entry.get("suggested_gesture"):
            gesture = entry["suggested_gesture"]
            fam = family(entry["special"] + " " + entry["name"])
            effect, particles, sound = SPECS[fam]
            profile["specials"] = [{
                "gesture": gesture,
                "cooldown_ticks": 180 if gesture in ("CIRCLE", "LINE_V_DOWN") else 140,
                "damage_multiplier": round(DAMAGE[gesture] + 0.05, 2),
                "knockback": KNOCK[gesture],
                "lift": 0.35 if fam == "astral" else 0.1,
                "hitbox": hitbox(gesture),
                "target_effects": [effect],
                "self_effects": SELF.get(fam, []),
                "heal": 4.0 if fam == "blood" else 0.0,
                "sound": sound,
                "particles": particles,
            }]
        weapons.append(profile)
    categories = build_categories(inventory)
    OUT.write_text(json.dumps({"categories": categories, "weapons": weapons}, indent=2) + "\n")
    specials = sum(len(w.get("specials", [])) for w in weapons)
    animated = sum(1 for w in weapons if "animations" in w)
    print(f"{len(weapons)} weapons, {specials} specials, {animated} with animations, {len(categories)} categories")


if __name__ == "__main__":
    build()
