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
        match = re.search(r"Set<String>\s+" + name + r"\s*=\s*Set\.of\(([^)]*)\)", text)
        return set(re.findall(r'"([^"]+)"', match.group(1))) if match else set()

    def factor_values(method, order):
        body = re.search(r"public static double " + method + r"\([^)]*\)\s*\{(.*?)\n    \}", text, re.S).group(1)
        values = []
        for statement in re.findall(r"return\s+([^;]+);", body):
            values.extend(float(v) for v in re.findall(r"[0-9]+\.[0-9]+", statement))
        return dict(zip(order, values))

    locked = re.search(r"HEAVY_LOCKED\s*=\s*java\.util\.EnumSet\.of\(([^)]*)\)", text, re.S)
    locked_shapes = set(re.findall(r"GestureShape\.(\w+)", locked.group(1))) if locked else set()

    stun_body = re.search(r"public static int hitStunTicks\([^)]*\)\s*\{(.*?)\n    \}", text, re.S).group(1)
    stun_light = int(re.search(r"LIGHT\.contains\(value\)\)\s*\{\s*return\s+(\d+)", stun_body).group(1))
    stun_heavy = int(re.search(r"HEAVY\.contains\(value\)\)\s*\{\s*return\s+(\d+)", stun_body).group(1))
    stun_area, stun_normal = (int(v) for v in re.search(r"AREA\.contains\(value\)\s*\?\s*(\d+)\s*:\s*(\d+)", stun_body).groups())

    return {
        "heavy": group("HEAVY"),
        "light": group("LIGHT"),
        "area": group("AREA"),
        "damage_factor": factor_values("damageFactor", ["heavy", "area", "light", "normal"]),
        "cooldown_factor": factor_values("cooldownFactor", ["light", "heavy", "area", "normal"]),
        "heavy_locked": locked_shapes,
        "stun": {
            "light": stun_light,
            "heavy": stun_heavy,
            "area": stun_area,
            "normal": stun_normal,
        },
    }


def family_class(family, rules):
    if family and family in rules["heavy"]:
        return "heavy"
    if family and family in rules["light"]:
        return "light"
    if family and family in rules["area"]:
        return "area"
    return "normal"


def factor(values, weight):
    return values[weight]


def weapon_profiles():
    data = json.loads(read(RES / "weapons.json"))["weapons"]
    profiles = {}
    for entry in data:
        family = entry.get("family")
        if not family:
            continue
        profile = profiles.setdefault(family, {
            "archetype": entry.get("archetype"),
            "damage_multiplier": entry.get("damage_multiplier", 1.0),
            "cooldown_multiplier": entry.get("cooldown_multiplier", 1.0),
            "reach_multiplier": entry.get("reach_multiplier", 1.0),
            "specials": [],
        })
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
            "damage_multiplier": profile["damage_multiplier"],
            "cooldown_multiplier": profile["cooldown_multiplier"],
            "reach_multiplier": profile["reach_multiplier"],
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
        }
    return kits


def simulate(kit, cast_ticks=4):
    weight = kit["weight"]
    stun_ticks = RULES["stun"][weight]
    damage_factor = factor(RULES["damage_factor"], weight)
    cooldown_factor = factor(RULES["cooldown_factor"], weight)
    base = kit["damage_multiplier"] * damage_factor
    heavy = weight == "heavy"
    slots = [(shape, kit["slots"][shape]) for shape in SLOT_ORDER if shape in kit["slots"]]

    ready = {tech["id"]: 0 for _, tech in slots}
    busy_until = 0
    cast_lock = 0
    cursor = 0
    total_damage = 0.0
    casts = 0
    stun_intervals = []
    schedule = []
    per_tech = {tech["id"]: {"damage": 0.0, "casts": 0} for _, tech in slots}

    for tick in range(HORIZON):
        still = [entry for entry in schedule if entry[0] > tick]
        for hit_tick, damage, tech in schedule:
            if hit_tick > tick:
                continue
            total_damage += damage
            per_tech[tech["id"]]["damage"] += damage
            pin = max(stun_ticks, tech.get("pin", 0))
            stun_intervals.append((hit_tick, hit_tick + pin))
        schedule = still
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

    merged = []
    for start, end in sorted(stun_intervals):
        start = max(0, start)
        end = min(end, HORIZON)
        if end <= start:
            continue
        if merged and start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    stunned = sum(end - start for start, end in merged)
    gaps = []
    cursor = 0
    for start, end in merged:
        if start > cursor:
            gaps.append(start - cursor)
        cursor = max(cursor, end)
    if cursor < HORIZON:
        gaps.append(HORIZON - cursor)
    longest_gap = max(gaps) if gaps else 0

    seconds = HORIZON / 20.0
    return {
        "family": kit["family"] or kit["archetype"].lower(),
        "weight": weight,
        "archetype": kit["archetype"].lower(),
        "slots": len(slots),
        "dps": total_damage / seconds,
        "damage": total_damage,
        "casts": casts,
        "stun": min(100.0, 100.0 * stunned / HORIZON),
        "gap": longest_gap / 20.0,
        "aoe": max((tech.get("aoe", 1) for _, tech in slots), default=1),
        "reach": kit.get("reach_multiplier", 1.0),
        "burst": sum(base * tech["multiplier"] for _, tech in slots),
        "kit_slots": slots,
        "per_tech": per_tech,
        "stun_ticks": stun_ticks,
    }


def render(rows, show_techs):
    header = (f"{'weapon':<18}{'class':<8}{'arch':<7}{'slots':>6}{'reachx':>7}{'casts/m':>9}"
              f"{'burst':>8}{'dmg/s':>9}{'stun%':>8}{'gap':>8}")
    lines = [header, "-" * len(header)]
    for row in rows:
        lines.append(
            f"{row['family']:<18}{row['weight']:<8}{row['archetype']:<7}{row['slots']:>6}"
            f"{row['reach']:>6.2f}x{row['casts'] * 60.0 / (HORIZON / 20.0):>9.1f}{row['burst']:>8.2f}"
            f"{row['dps']:>9.2f}{row['stun']:>7.1f}%{row['gap']:>7.2f}s"
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
