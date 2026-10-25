#!/usr/bin/env python3
import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "putzwirk" / "gesturalcombat"
RES = ROOT / "src" / "main" / "resources" / "data" / "gesturalcombat"

SLOT_ORDER = ["LINE_H", "LINE_V_UP", "LINE_V_DOWN", "CIRCLE", "ZIGZAG", "V", "CARET"]
HORIZON = 1200

BASE_DAMAGE = {
    "soul_knife": 4.0,
    "sickle": 5.0,
    "rapier": 5.0,
    "katana": 5.5,
    "sword": 6.5,
    "chakram": 5.0,
    "trident": 7.0,
    "spear": 6.0,
    "battlestaff": 6.5,
    "twinblade": 6.5,
    "warglaive": 6.5,
    "scythe": 8.0,
    "claymore": 8.0,
    "glaive": 8.0,
    "halberd": 8.0,
    "greataxe": 9.0,
    "greathammer": 10.0,
    "heavy_axe": 9.0,
    "mace": 9.0,
    "axe": 9.0,
}


def read(path):
    return path.read_text(encoding="utf-8")


def balanced(text, open_index):
    depth = 0
    quote = None
    for i in range(open_index, len(text)):
        ch = text[i]
        if quote:
            if ch == "\\":
                continue
            if ch == quote:
                quote = None
            continue
        if ch in "\"'":
            quote = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[open_index + 1:i], i + 1
    raise ValueError("unbalanced parentheses")


def split_args(text):
    args = []
    depth = 0
    quote = None
    current = []
    for ch in text:
        if quote:
            current.append(ch)
            if ch == quote:
                quote = None
            continue
        if ch in "\"'":
            quote = ch
            current.append(ch)
        elif ch in "([{":
            depth += 1
            current.append(ch)
        elif ch in ")]}":
            depth -= 1
            current.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if current:
        args.append("".join(current).strip())
    return args


def enum_name(token):
    match = re.fullmatch(r"(?:\w+\.)?(\w+)", token.strip())
    return match.group(1) if match else None


def literal(token):
    token = token.strip()
    if token in ("null", ""):
        return None
    try:
        return float(token)
    except ValueError:
        return None


def unquote(token):
    match = re.fullmatch(r'"([^"]*)"', token.strip())
    return match.group(1) if match else None


def effect_parameters():
    params = {}
    for path in sorted((JAVA / "technique" / "effect").glob("*.java")):
        text = read(path)
        for match in re.finditer(r"public " + re.escape(path.stem) + r"\(", text):
            inner, _ = balanced(text, match.end() - 1)
            names = []
            for arg in split_args(inner):
                tokens = re.findall(r"\w+", arg)
                if tokens:
                    names.append(tokens[-1])
            if len(names) > len(params.get(path.stem, [])):
                params[path.stem] = names
    return params


def parse_techniques(effect_params):
    text = read(JAVA / "technique" / "Techniques.java")
    out = []
    for match in re.finditer(r"new Technique\(", text):
        inner, _ = balanced(text, match.end() - 1)
        args = split_args(inner)
        if len(args) < 8:
            continue
        tech = {
            "id": unquote(args[0]),
            "archetype": enum_name(args[1]),
            "shape": enum_name(args[2]),
            "level": int(literal(args[3]) or 1),
            "cooldown": int(literal(args[4]) or 0),
            "xp": int(literal(args[6]) or 0),
            "family": unquote(args[8]) if len(args) > 8 else None,
            "multiplier": 1.0,
            "hits": 1,
            "interval": 0,
            "windup": 0,
            "knockback": 0.0,
            "lift": 0.0,
            "pin": 0,
            "aoe": 1,
        }
        effect = args[7]
        effect_match = re.match(r"new (\w+)\((.*)\)\s*$", effect, re.S)
        if effect_match:
            effect_class = effect_match.group(1)
            effect_args = split_args(effect_match.group(2))
            names = effect_params.get(effect_class, [])
            for index, value in enumerate(effect_args):
                name = names[index] if index < len(names) else ""
                number = literal(value)
                if number is None:
                    continue
                low = name.lower()
                if "damagemultiplier" in low or low == "damage":
                    tech["multiplier"] = number
                elif low == "hits":
                    tech["hits"] = max(1, int(number))
                elif "interval" in low:
                    tech["interval"] = max(0, int(number))
                elif "windup" in low:
                    tech["windup"] = max(0, int(number))
                elif "knockback" in low:
                    tech["knockback"] = number
                elif low == "lift":
                    tech["lift"] = number
            pins = [int(v) for v in re.findall(r"HitStun\.pin\([^,]+,\s*(\d+)", effect)]
            tech["pin"] = max(pins) if pins else 0
        out.append(tech)
    return out


def parse_abilities(techniques):
    data = json.loads(read(RES / "abilities.json"))["techniques"]
    by_id = {tech["id"]: tech for tech in techniques}
    for tech_id, entry in data.items():
        tech = by_id.get(tech_id)
        if tech is None:
            continue
        if entry.get("gesture"):
            tech["shape"] = entry["gesture"].upper()
            if isinstance(tech["shape"], str) and tech["shape"] not in SLOT_ORDER:
                tech["shape"] = None
        if entry.get("damage_multiplier") is not None:
            tech["multiplier"] = float(entry["damage_multiplier"])
        if entry.get("hits") is not None:
            tech["hits"] = max(1, int(entry["hits"]))
        if entry.get("hit_interval") is not None:
            tech["interval"] = max(0, int(entry["hit_interval"]))
        if entry.get("windup_ticks") is not None:
            tech["windup"] = max(0, int(entry["windup_ticks"]))
        if entry.get("knockback") is not None:
            tech["knockback"] = float(entry["knockback"])
        if entry.get("lift") is not None:
            tech["lift"] = float(entry["lift"])
        if entry.get("cooldown_ticks") is not None:
            tech["cooldown"] = int(entry["cooldown_ticks"])
        hitbox = entry.get("hitbox") or {}
        if hitbox.get("max_targets"):
            tech["aoe"] = int(hitbox["max_targets"])
    return techniques


def parse_weapon_rules():
    text = read(JAVA / "weapon" / "WeaponRules.java")

    def group(name):
        match = re.search(r"Set<String>\s+" + name + r"\s*=\s*Set\.of\(([^)]*)\)", text, re.S)
        return set(re.findall(r'"([^"]+)"', match.group(1))) if match else set()

    tuning = {}
    for family, damage, cooldown, stun, reach in re.findall(
            r'Map\.entry\("([a-z_]+)",\s*new Tuning\(([0-9.]+),\s*([0-9.]+),\s*(\d+),\s*([0-9.]+)\)\)', text):
        tuning[family] = {"damage": float(damage), "cooldown": float(cooldown),
                          "stun": int(stun), "reach": float(reach)}

    default_match = re.search(r"DEFAULT\s*=\s*new Tuning\(([0-9.]+),\s*([0-9.]+),\s*(\d+),\s*([0-9.]+)\)", text)
    default = ({"damage": float(default_match.group(1)), "cooldown": float(default_match.group(2)),
                "stun": int(default_match.group(3)), "reach": float(default_match.group(4))}
               if default_match else {"damage": 1.0, "cooldown": 1.0, "stun": 4, "reach": 1.0})

    locked = re.search(r"HEAVY_LOCKED\s*=\s*java\.util\.EnumSet\.of\(([^)]*)\)", text, re.S)
    locked_shapes = set(re.findall(r"GestureShape\.(\w+)", locked.group(1))) if locked else set()

    stun_text = read(JAVA / "technique" / "HitStun.java")
    immunity = re.search(r"IMMUNITY_TICKS\s*=\s*(\d+)", stun_text)
    chain_match = re.search(r"MAX_CHAIN_TICKS\s*=\s*(\d+)", stun_text)

    return {
        "heavy": group("HEAVY"),
        "light": group("LIGHT"),
        "area": group("AREA"),
        "heavy_locked": locked_shapes,
        "immunity": int(immunity.group(1)) if immunity else 0,
        "chain": int(chain_match.group(1)) if chain_match else 999,
        "tuning": tuning,
        "default": default,
    }


def family_class(family, rules):
    if family and family in rules["heavy"]:
        return "heavy"
    if family and family in rules["light"]:
        return "light"
    if family and family in rules["area"]:
        return "area"
    return "normal"


def tuning_for(family):
    return RULES["tuning"].get(family, RULES["default"])


def weapon_profiles():
    data = json.loads(read(RES / "weapons.json"))["weapons"]
    profiles = {}
    for entry in data:
        family = entry.get("family")
        if not family:
            continue
        profile = profiles.setdefault(family, {
            "archetype": entry.get("archetype"),
            "damage": [],
            "cooldown": [],
            "reach": [],
            "specials": [],
        })
        profile["damage"].append(entry.get("damage_multiplier", 1.0))
        profile["cooldown"].append(entry.get("cooldown_multiplier", 1.0))
        profile["reach"].append(entry.get("reach_multiplier", 1.0))
        if profile["specials"]:
            continue
        for special in entry.get("specials", []):
            gesture = (special.get("gesture") or "").upper()
            if gesture and gesture not in profile["specials"]:
                profile["specials"].append({
                    "id": family + "_" + gesture.lower(),
                    "shape": gesture,
                    "cooldown": int(special.get("cooldown_ticks", 40)),
                    "multiplier": float(special.get("damage_multiplier", 1.0)),
                    "hits": 1,
                    "interval": 0,
                    "windup": 0,
                    "pin": 0,
                })
    return profiles


def build_kits(techniques, profiles):
    by_family = {}
    by_archetype = {}
    for tech in techniques:
        if tech["family"]:
            by_family.setdefault(tech["family"], {})[tech["shape"]] = tech
        elif tech["shape"]:
            by_archetype.setdefault(tech["archetype"], {}).setdefault(tech["shape"], tech)
    kits = {}
    for family, profile in profiles.items():
        archetype = (profile["archetype"] or "").upper()
        family_map = by_family.get(family, {})
        archetype_map = by_archetype.get(archetype, {})
        if family == "sword":
            slots = dict(archetype_map)
            slots.update(family_map)
        else:
            slots = dict(family_map) if family_map else dict(archetype_map)
        if not family_map:
            for special in profile.get("specials", []):
                slots[special["shape"]] = special
        kits[family] = {
            "family": family,
            "archetype": archetype,
            "slots": slots,
            "damage_multiplier": sum(profile["damage"]) / len(profile["damage"]),
            "cooldown_multiplier": sum(profile["cooldown"]) / len(profile["cooldown"]),
            "reach_multiplier": sum(profile["reach"]) / len(profile["reach"]),
            "weight": family_class(family, RULES),
        }
        blocked = set(RULES["heavy_locked"]) if kits[family]["weight"] == "heavy" else set()
        if family == "chakram":
            blocked.add("LINE_V_DOWN")
        kits[family]["slots"] = {shape: tech for shape, tech in kits[family]["slots"].items() if shape not in blocked}
    for archetype, slots in by_archetype.items():
        if archetype in ("SWORD", "SHIELD", "BOW"):
            continue
        kits["(no family) " + archetype.lower()] = {
            "family": None,
            "archetype": archetype,
            "slots": dict(slots),
            "damage_multiplier": 1.0,
            "cooldown_multiplier": 1.0,
            "reach_multiplier": 1.0,
            "weight": "normal",
            "tuning_family": archetype.lower(),
        }
    return kits


def simulate(kit, cast_ticks=4):
    weight = kit["weight"]
    tuning = tuning_for(kit.get("tuning_family") or kit["family"])
    stun_ticks = tuning["stun"]
    damage_factor = tuning["damage"]
    cooldown_factor = tuning["cooldown"]
    base = kit["damage_multiplier"] * damage_factor * BASE_DAMAGE.get(kit["family"] or kit["archetype"].lower(), 6.0)
    heavy = weight == "heavy"
    slots = [(shape, kit["slots"][shape]) for shape in SLOT_ORDER if shape in kit["slots"]]

    ready = {tech["id"]: 0 for _, tech in slots}
    busy_until = 0
    cast_lock = 0
    cursor = 0
    total_damage = 0.0
    casts = 0
    schedule = []
    per_tech = {tech["id"]: {"damage": 0.0, "casts": 0} for _, tech in slots}
    stun_end = 0
    immune_until = 0
    chain_start = 0
    stunned_ticks = 0
    gap_run = 0
    longest_gap = 0
    gaps = []
    immune = RULES.get("immunity", 0)
    max_chain = RULES.get("chain", 999)

    for tick in range(HORIZON):
        still = [entry for entry in schedule if entry[0] > tick]
        for hit_tick, damage, tech in schedule:
            if hit_tick > tick:
                continue
            total_damage += damage
            per_tech[tech["id"]]["damage"] += damage
            pin = max(stun_ticks, tech.get("pin", 0))
            hit_tick = max(hit_tick, tick)
            if hit_tick > stun_end:
                if stun_end > 0:
                    immune_until = max(immune_until, stun_end + immune)
                if hit_tick < immune_until:
                    continue
                chain_start = hit_tick
                stun_end = hit_tick + pin
                continue
            capped = min(max(stun_end, hit_tick + pin), chain_start + max_chain)
            if capped <= hit_tick:
                immune_until = max(immune_until, hit_tick + immune)
                continue
            stun_end = max(stun_end, capped)
        schedule = still
        if tick < stun_end:
            stunned_ticks += 1
            if gap_run:
                gaps.append(gap_run)
                longest_gap = max(longest_gap, gap_run)
                gap_run = 0
        else:
            gap_run += 1
        if tick < busy_until or tick < cast_lock:
            continue
        ordered = slots[cursor:] + slots[:cursor]
        for offset, (_, tech) in enumerate(ordered):
            if tech["id"] in ready and ready[tech["id"]] > tick:
                continue
            cooldown = max(4, round(tech["cooldown"] * cooldown_factor * kit["cooldown_multiplier"]))
            ready[tech["id"]] = tick + cooldown
            hits = tech["hits"]
            per_hit = base * tech["multiplier"] / hits
            first = tick + tech["windup"]
            for index in range(hits):
                schedule.append((first + index * tech["interval"], per_hit, tech))
            per_tech[tech["id"]]["casts"] += 1
            casts += 1
            cast_lock = tick + cast_ticks
            if heavy:
                busy_until = tick + tech["windup"] + (hits - 1) * tech["interval"] + 12
            cursor = (cursor + offset + 1) % len(slots)
            break

    if gap_run:
        gaps.append(gap_run)
        longest_gap = max(longest_gap, gap_run)

    seconds = HORIZON / 20.0
    return {
        "family": kit["family"] or kit["archetype"].lower(),
        "weight": weight,
        "archetype": kit["archetype"].lower(),
        "slots": len(slots),
        "dps": total_damage / seconds,
        "damage": total_damage,
        "casts": casts,
        "stun": min(100.0, 100.0 * stunned_ticks / HORIZON),
        "gap": longest_gap / 20.0,
        "median_gap": (sorted(gaps)[len(gaps) // 2] / 20.0) if gaps else 0.0,
        "short_gaps": (100.0 * sum(1 for length in gaps if length < immune) / len(gaps)) if gaps else 0.0,
        "aoe": max((tech.get("aoe", 1) for _, tech in slots), default=1),
        "reach": tuning["reach"] * kit.get("reach_multiplier", 1.0),
        "burst": sum(base * tech["multiplier"] for _, tech in slots),
        "kit_slots": slots,
        "per_tech": per_tech,
        "stun_ticks": stun_ticks,
    }


def render(rows, show_techs):
    header = (f"{'weapon':<18}{'class':<8}{'arch':<7}{'slots':>6}{'reachx':>7}{'casts/m':>9}"
              f"{'burst':>8}{'dmg/s':>9}{'stun%':>8}{'gap':>7}{'med':>7}{'<imm':>7}")
    lines = [header, "-" * len(header)]
    for row in rows:
        lines.append(
            f"{row['family']:<18}{row['weight']:<8}{row['archetype']:<7}{row['slots']:>6}"
            f"{row['reach']:>6.2f}x{row['casts'] * 60.0 / (HORIZON / 20.0):>9.1f}{row['burst']:>8.2f}"
            f"{row['dps']:>9.2f}{row['stun']:>7.1f}%{row['gap']:>6.2f}s{row['median_gap']:>6.2f}s"
            f"{row['short_gaps']:>6.0f}%"
        )
    if show_techs:
        lines.append("")
        tech_header = (f"{'weapon':<14}{'technique':<20}{'slot':<14}{'cd':>5}{'x':>7}{'hits':>6}"
                       f"{'stun':>6}{'dmg/s':>8}")
        lines.append(tech_header)
        lines.append("-" * len(tech_header))
        for row in rows:
            for shape, tech in row["kit_slots"]:
                entry = row["per_tech"].get(tech["id"])
                if not entry or not entry["casts"]:
                    continue
                lines.append(
                    f"{row['family']:<14}{tech['id']:<20}{shape:<14}{tech['cooldown']:>5}"
                    f"{tech['multiplier']:>7.2f}{tech['hits']:>6}{max(row['stun_ticks'], tech.get('pin', 0)):>6}"
                    f"{entry['damage'] / (HORIZON / 20.0):>8.2f}"
                )
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="DPS and stunlock analysis for gesturalcombat weapon kits")
    parser.add_argument("--techs", action="store_true", help="print per-technique breakdown")
    parser.add_argument("--sort", default="dps", choices=["dps", "stun", "name"])
    parser.add_argument("--cast-ticks", type=int, default=4, help="ticks spent drawing each gesture")
    parser.add_argument("--json", action="store_true", help="emit machine-readable results")
    args = parser.parse_args()

    global RULES
    RULES = parse_weapon_rules()
    techniques = parse_abilities(parse_techniques(effect_parameters()))
    kits = build_kits(techniques, weapon_profiles())
    rows = [simulate(kit, args.cast_ticks) for kit in kits.values() if kit["slots"]]
    if args.sort == "stun":
        rows.sort(key=lambda row: -row["stun"])
    elif args.sort == "name":
        rows.sort(key=lambda row: row["family"])
    else:
        rows.sort(key=lambda row: -row["dps"])
    if args.json:
        print(json.dumps({row["family"]: {key: value for key, value in row.items() if key != "per_tech"}
                          for row in rows}, indent=2, sort_keys=True))
        return
    print(f"horizon {HORIZON / 20.0:.0f}s, damage in units of player attack damage, "
          f"cast every {args.cast_ticks} ticks as soon as cooldown allows")
    print("burst = damage of casting every slot once, stun% = share of time the target is stunned, "
          "gap = longest window the target can act")
    print(render(rows, args.techs))


if __name__ == "__main__":
    main()
