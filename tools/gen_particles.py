#!/usr/bin/env python3
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/gesturalcombat/textures/particle"
DEF = ROOT / "src/main/resources/assets/gesturalcombat/particles"
SIZE = 64


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
    generate_ring()
    generate_burst()
    generate_trail()
    print("particles written to", TEX)


if __name__ == "__main__":
    main()
