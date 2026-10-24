#!/usr/bin/env python3
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/gesturalcombat/textures/particle"
DEF = ROOT / "src/main/resources/assets/gesturalcombat/particles"
SIZE = 64

ARCS = {
    "slash45": (55.0, 6),
    "slash90": (100.0, 6),
    "slash180": (165.0, 6),
    "slash270": (250.0, 7),
    "slash360": (350.0, 7),
}


def blank():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(image, x, y, alpha):
    x = int(x)
    y = int(y)
    if 0 <= x < SIZE and 0 <= y < SIZE and alpha > 0:
        current = image.getpixel((x, y))
        if alpha > current[3]:
            image.putpixel((x, y), (255, 255, 255, alpha))


def write_def(name, frames):
    entries = ", ".join(f'"gesturalcombat:{name}_{index}"' for index in range(1, frames + 1))
    (DEF / f"{name}.json").write_text('{\n  "textures": [\n    ' + entries.replace(", ", ",\n    ") + "\n  ]\n}\n")


def arc_frame(span_degrees, progress, seed):
    rng = random.Random(seed)
    image = blank()
    alpha_scale = (1.0 - progress * 0.55)
    radius = 12.0 + 20.0 * progress
    span = math.radians(span_degrees * (0.45 + 0.55 * progress))
    start = -span * 0.5
    steps = max(10, int(span * radius * 2.2))
    for step in range(steps):
        u = step / steps
        angle = start + span * u
        taper = math.sin(math.pi * min(1.0, max(0.0, u))) ** 0.55
        if taper <= 0.05:
            continue
        thickness = (1.4 + 4.2 * taper) * (0.65 + 0.35 * progress)
        base_alpha = int(235 * taper * alpha_scale)
        for layer in range(int(thickness) + 1):
            r = radius - layer
            x = int(round(SIZE / 2 + math.cos(angle) * r))
            y = int(round(SIZE / 2 + math.sin(angle) * r))
            fade = int(base_alpha * (1.0 - layer / (thickness + 1.0)))
            if layer == 0:
                fade = min(255, fade + 40)
            if rng.random() < 0.12:
                fade = max(0, fade - 70)
            put(image, x, y, fade)
    inner = radius * 0.7
    for step in range(steps // 2):
        u = step / max(1, steps // 2)
        angle = start + span * u
        taper = math.sin(math.pi * min(1.0, max(0.0, u))) ** 0.9
        x = int(round(SIZE / 2 + math.cos(angle) * inner))
        y = int(round(SIZE / 2 + math.sin(angle) * inner))
        put(image, x, y, int(90 * taper * alpha_scale))
    return image


def stab_frame(progress, seed):
    rng = random.Random(seed)
    image = blank()
    alpha_scale = 1.0 - progress * 0.55
    length = 26 + 32 * progress
    top = int(round(SIZE / 2 - length * 0.5))
    bottom = int(round(SIZE / 2 + length * 0.5))
    for y in range(top, bottom):
        u = (y - top) / max(1.0, length)
        edge = math.sin(math.pi * min(1.0, u)) ** 0.35
        width = (0.6 + 2.6 * (1.0 - u) ** 1.6) * (0.7 + 0.3 * progress)
        alpha = int(240 * edge * alpha_scale)
        for offset in range(int(width) + 1):
            fade = int(alpha * (1.0 - offset / (width + 1.5)))
            if offset == 0:
                fade = min(255, fade + 60)
            x = SIZE // 2 + offset
            put(image, x, y, fade)
            if offset > 0:
                put(image, SIZE // 2 - offset, y, fade // 2)
        if rng.random() < 0.08:
            put(image, SIZE // 2, y, max(0, alpha - 80))
    tip_y = bottom
    for spread in range(1, 4):
        put(image, SIZE // 2 - spread, tip_y - spread, int(90 * alpha_scale))
        put(image, SIZE // 2 + spread, tip_y - spread, int(90 * alpha_scale))
    return image


def generate_arcs():
    for name, (span, frames) in ARCS.items():
        for index in range(frames):
            progress = index / (frames - 1)
            arc_frame(span, progress, hash((name, index)) & 0xFFFF).save(TEX / f"{name}_{index + 1}.png")
        write_def(name, frames)


def generate_stab():
    frames = 6
    for index in range(frames):
        progress = index / (frames - 1)
        stab_frame(progress, 900 + index).save(TEX / f"stab_{index + 1}.png")
    write_def("stab", frames)


def ring_frame(radius, alpha, gaps):
    image = blank()
    steps = int(radius * 6.2) + 8
    for step in range(steps):
        angle = step / steps * math.tau
        if any(start <= step < start + length for start, length in gaps):
            continue
        x = int(round(SIZE / 2 + math.cos(angle) * radius))
        y = int(round(SIZE / 2 + math.sin(angle) * radius))
        put(image, x, y, alpha)
        put(image, x, y - 1, alpha // 2)
        put(image, x + 1, y, alpha // 3)
    return image


def generate_ring():
    rng = random.Random(7)
    frames = 7
    for index in range(frames):
        t = index / (frames - 1)
        radius = 4 + 27 * math.sqrt(t)
        alpha = int(255 * (1.0 - t) ** 1.3)
        gaps = [(rng.randrange(0, 40), rng.randrange(1, 3)) for _ in range(5)]
        ring_frame(radius, alpha, gaps).save(TEX / f"ring_{index + 1}.png")
    write_def("ring", frames)


def generate_burst():
    rng = random.Random(11)
    frames = 6
    spikes = 13
    for index in range(frames):
        t = index / (frames - 1)
        alpha = int(255 * (1.0 - t) ** 1.5)
        image = blank()
        for spike in range(spikes):
            angle = spike / spikes * math.tau + rng.uniform(-0.12, 0.12)
            length = (10 + 24 * math.sqrt(t)) * (0.7 + rng.random() * 0.6)
            for step in range(int(length)):
                fade = int(alpha * (1.0 - step / max(1.0, length)))
                x = int(round(SIZE / 2 + math.cos(angle) * step))
                y = int(round(SIZE / 2 + math.sin(angle) * step))
                put(image, x, y, fade)
                if step % 3 == 0:
                    put(image, x, y + 1, max(0, fade // 2))
        image.save(TEX / f"burst_{index + 1}.png")
    write_def("burst", frames)


def generate_bolt():
    rng = random.Random(23)
    frames = 5
    for index in range(frames):
        t = index / (frames - 1)
        alpha = int(255 * (1.0 - t) ** 1.2)
        image = blank()
        x = SIZE // 2
        for y in range(4, SIZE - 4, 2):
            x += rng.randrange(-2, 3)
            put(image, x, y, alpha)
            put(image, x + 1, y, alpha // 2)
            if y % 8 == 0:
                branch = rng.choice((-1, 1))
                for step in range(1, 5):
                    put(image, x + branch * step, y + step, max(0, alpha - step * 30))
        image.save(TEX / f"bolt_{index + 1}.png")
    write_def("bolt", frames)


def generate_trail():
    image = blank()
    for y in range(SIZE):
        along = 1.0 - y / (SIZE - 1)
        along = round(along * 3) / 3
        for x in range(SIZE):
            across = math.sin(math.pi * (x + 0.5) / SIZE) ** 0.7
            alpha = int(255 * across * along)
            alpha = (alpha // 64) * 64
            image.putpixel((x, y), (255, 255, 255, alpha))
    image.save(TEX / "trail.png")


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    DEF.mkdir(parents=True, exist_ok=True)
    generate_arcs()
    generate_stab()
    generate_ring()
    generate_burst()
    generate_bolt()
    generate_trail()
    print("particles written to", TEX)


if __name__ == "__main__":
    main()
