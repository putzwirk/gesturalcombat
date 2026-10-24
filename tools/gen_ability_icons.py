#!/usr/bin/env python3
import json
import math
from pathlib import Path

from PIL import Image

W = H = 16
OUT_DIR = Path("src/main/resources/assets/gesturalcombat/textures/gui/abilities")

OUTLINE = (0x1A, 0x1A, 0x20, 0xFF)
ACCENT = (0xFF, 0xFF, 0xFF, 0xFF)

PALETTES = {
    "sword": ((0x56, 0x5F, 0x75), (0x96, 0xA3, 0xB5), (0xE2, 0xE8, 0xF0)),
    "axe": ((0x68, 0x42, 0x23), (0xB5, 0x7E, 0x49), (0xF0, 0xCD, 0xA0)),
    "spear": ((0x6B, 0x5A, 0x20), (0xC6, 0xA7, 0x4A), (0xF7, 0xE8, 0xB2)),
    "shield": ((0x2E, 0x5E, 0x34), (0x5E, 0xA3, 0x60), (0xAA, 0xE8, 0xA8)),
    "bow": ((0x63, 0x3F, 0x1E), (0xB0, 0x7A, 0x48), (0xF4, 0xD6, 0xA0)),
}

ABILITIES = {
    "sword_slash": ("sword", "LINE_H", None),
    "sword_rising": ("sword", "LINE_V_UP", None),
    "sword_whirl": ("sword", "CIRCLE", None),
    "sword_flurry": ("sword", "ZIGZAG", None),
    "sword_execute": ("sword", "LINE_V_DOWN", "impact"),
    "sword_guard": ("sword", "V", "bar"),
    "sword_skirmish": ("sword", "CARET", "speed"),
    "axe_cleave": ("axe", "LINE_V_DOWN", "impact"),
    "axe_wide": ("axe", "LINE_H", "double"),
    "axe_rend": ("axe", "LINE_V_UP", None),
    "axe_cyclone": ("axe", "CIRCLE", None),
    "axe_quake": ("axe", "ZIGZAG", None),
    "spear_thrust": ("spear", "LINE_V_UP", None),
    "spear_sweep": ("spear", "LINE_H", "double"),
    "spear_impale": ("spear", "ZIGZAG", None),
    "spear_tempest": ("spear", "CIRCLE", None),
    "spear_skyfall": ("spear", "LINE_V_DOWN", "impact"),
    "shield_bash": ("shield", "LINE_H", None),
    "shield_guard": ("shield", "LINE_V_DOWN", "bar"),
    "shield_charge": ("shield", "LINE_V_UP", "speed"),
    "shield_throw": ("shield", "CIRCLE", None),
    "bow_fire": ("bow", "CIRCLE", None),
}


def pdist(px, py, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    length = dx * dx + dy * dy
    if length < 1e-9:
        return math.hypot(px - x0, py - y0)
    t = max(0.0, min(1.0, ((px - x0) * dx + (py - y0) * dy) / length))
    return math.hypot(px - (x0 + dx * t), py - (y0 + dy * t))


def seg(cells, x0, y0, x1, y1, r=0.95):
    for y in range(H):
        for x in range(W):
            if pdist(x + 0.5, y + 0.5, x0, y0, x1, y1) <= r:
                cells.add((x, y))


def thin(cells, x0, y0, x1, y1):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        if 0 <= x0 < W and 0 <= y0 < H:
            cells.add((x0, y0))
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def poly(cells, points):
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    for y in range(int(math.floor(min(ys))), int(math.ceil(max(ys))) + 1):
        for x in range(int(math.floor(min(xs))), int(math.ceil(max(xs))) + 1):
            if in_poly(x + 0.5, y + 0.5, points):
                if 0 <= x < W and 0 <= y < H:
                    cells.add((x, y))


def in_poly(px, py, points):
    inside = False
    j = len(points) - 1
    for i in range(len(points)):
        xi, yi = points[i]
        xj, yj = points[j]
        if (yi > py) != (yj > py):
            if px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                inside = not inside
        j = i
    return inside


def arc(cells, cx, cy, r, a0, a1, thick=0.95, step=2.0):
    angle = a0
    prev = None
    while angle <= a1 + 1e-6:
        rad = math.radians(angle)
        point = (cx + math.cos(rad) * r, cy + math.sin(rad) * r)
        if prev is not None:
            seg(cells, prev[0], prev[1], point[0], point[1], thick)
        prev = point
        angle += step


def bar(cells, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < H:
                cells.add((x, y))


def bar(cells, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < H:
                cells.add((x, y))


def arrow_h(cells, tip_x, y0, y1, direction=1):
    spans = ((y0 - 2, y1 + 2), (y0 - 1, y1 + 1), (y0, y1), (y0, y1))
    for offset, (a, b) in enumerate(spans):
        x = tip_x - direction * (3 - offset)
        for y in range(a, b + 1):
            if 0 <= x < W and 0 <= y < H:
                cells.add((x, y))


def arrow_v(cells, tip_y, x0, x1, direction=1):
    spans = ((x0 - 2, x1 + 2), (x0 - 1, x1 + 1), (x0, x1), (x0, x1))
    for offset, (a, b) in enumerate(spans):
        y = tip_y - direction * (3 - offset)
        for x in range(a, b + 1):
            if 0 <= x < W and 0 <= y < H:
                cells.add((x, y))


def arrow(cells, tip, direction, length=4.0, half=2.4):
    dx, dy = direction
    norm = math.hypot(dx, dy) or 1.0
    dx, dy = dx / norm, dy / norm
    base = (tip[0] - dx * length, tip[1] - dy * length)
    perp = (-dy, dx)
    poly(cells, [tip, (base[0] + perp[0] * half, base[1] + perp[1] * half),
                 (base[0] - perp[0] * half, base[1] - perp[1] * half)])
    seg(cells, base[0], base[1], tip[0] - dx * 1.2, tip[1] - dy * 1.2, 0.95)


def build_stroke(shape, kind=None):
    stroke = set()
    cue = set()
    accent = set()
    if shape == "LINE_H":
        if kind == "double":
            bar(stroke, 2, 6, 11, 8)
            arrow_h(stroke, 14, 6, 8)
        else:
            bar(stroke, 2, 7, 11, 8)
            arrow_h(stroke, 14, 7, 8)
    elif shape == "LINE_V_UP":
        bar(stroke, 7, 7, 8, 13)
        arrow_v(stroke, 3, 7, 8, -1)
    elif shape == "LINE_V_DOWN":
        bar(stroke, 7, 2, 8, 9)
        arrow_v(stroke, 13, 7, 8)
    elif shape == "CIRCLE":
        arc(stroke, 8.0, 8.0, 4.6, 0.0, 360.0, 0.75, 2.0)
    elif shape == "ZIGZAG":
        bar(stroke, 2, 3, 12, 4)
        seg(stroke, 12.0, 4.5, 3.0, 10.5, 0.75)
        bar(stroke, 3, 10, 13, 11)
    elif shape == "V":
        seg(stroke, 2.0, 5.0, 8.0, 13.0, 0.75)
        seg(stroke, 8.0, 13.0, 14.0, 5.0, 0.75)
    elif shape == "CARET":
        seg(stroke, 2.0, 13.0, 8.0, 5.0, 0.75)
        seg(stroke, 8.0, 5.0, 14.0, 13.0, 0.75)
    return stroke, cue, accent


def add_cue(shape, cue, accent, kind):
    if kind is None:
        return
    if kind == "spiral":
        accent.add((8, 7))
        accent.add((9, 8))
    elif kind == "bar":
        thin(cue, 4, 8, 11, 8)
    elif kind == "impact":
        if shape == "LINE_V_DOWN":
            accent.add((5, 11))
            accent.add((10, 11))
        elif shape == "LINE_V_UP":
            accent.add((5, 5))
            accent.add((10, 5))
        else:
            accent.add((5, 6))
            accent.add((10, 6))
    elif kind == "burst" and shape == "LINE_H":
        accent.add((15, 6))
        accent.add((15, 7))
        accent.add((15, 9))


def connected(cells):
    if not cells:
        return False
    start = next(iter(cells))
    seen = {start}
    stack = [start]
    while stack:
        x, y = stack.pop()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) in cells and (nx, ny) not in seen:
                seen.add((nx, ny))
                stack.append((nx, ny))
    return len(seen) == len(cells)


def shade(cells, palette):
    dark, mid, light = palette
    pixels = {}
    for x, y in cells:
        above = (x, y - 1) in cells
        below = (x, y + 1) in cells
        if not above:
            pixels[(x, y)] = light
        elif not below:
            pixels[(x, y)] = dark
        else:
            pixels[(x, y)] = mid
    return pixels


def center(image):
    px = image.load()
    xs = [x for y in range(H) for x in range(W) if px[x, y][3] > 0]
    ys = [y for y in range(H) for x in range(W) if px[x, y][3] > 0]
    if not xs:
        return image
    dx = int(math.floor((W - 1 - (min(xs) + max(xs))) / 2.0 + 0.5))
    dy = int(math.floor((H - 1 - (min(ys) + max(ys))) / 2.0 + 0.5))
    if dx == 0 and dy == 0:
        return image
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    target = out.load()
    for y in range(H):
        for x in range(W):
            if px[x, y][3] > 0:
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H:
                    target[nx, ny] = px[x, y]
    return out


def render(technique, family, shape, cue_kind):
    stroke, cue, accent = build_stroke(shape, cue_kind)
    add_cue(shape, cue, accent, cue_kind)
    body = stroke
    palette = PALETTES[family]

    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    put = image.load()

    outline = set()
    for x, y in body:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H and (nx, ny) not in body:
                    outline.add((nx, ny))

    for x, y in outline:
        put[x, y] = OUTLINE
    for (x, y), color in shade(body, palette).items():
        put[x, y] = (*color, 0xFF)
    for x, y in cue:
        if 0 <= x < W and 0 <= y < H and (x, y) not in body and (x, y) not in outline:
            put[x, y] = (*palette[2], 0xFF)
    for x, y in accent:
        if 0 <= x < W and 0 <= y < H and (x, y) not in body and (x, y) not in outline:
            put[x, y] = ACCENT

    image = center(image)
    return image, stroke, cue, accent


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    report = {}
    for technique, (family, shape, cue_kind) in ABILITIES.items():
        image, stroke, cue, accent = render(technique, family, shape, cue_kind)
        image.save(OUT_DIR / f"{technique}.png")
        pixels = sum(1 for y in range(H) for x in range(W) if image.getpixel((x, y))[3] > 0)
        xs = [x for (x, y) in stroke]
        ys = [y for (x, y) in stroke]
        report[technique] = {
            "stroke_connected": connected(stroke),
            "cue_connected": connected(cue) if cue else None,
            "stroke_pixels": len(stroke),
            "total_pixels": pixels,
            "bbox": [min(xs), min(ys), max(xs), max(ys)],
        }
    print(json.dumps(report, indent=1))
    failures = [k for k, v in report.items() if not v["stroke_connected"]]
    print("disconnected strokes:", failures)


if __name__ == "__main__":
    main()
