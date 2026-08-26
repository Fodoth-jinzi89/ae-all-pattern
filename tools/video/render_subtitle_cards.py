#!/usr/bin/env python3
"""Render transparent Chinese subtitle cards for FFmpeg overlay."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


FONT = Path("/System/Library/Fonts/Hiragino Sans GB.ttc")
WIDTH = 1920
HEIGHT = 180
FONT_SIZE = 72
STROKE = 6


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    data = json.loads(args.manifest.read_text(encoding="utf-8"))
    font = ImageFont.truetype(str(FONT), FONT_SIZE)

    for index, event in enumerate(data["lines"]):
        text = event["text"]
        image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        left, top, right, bottom = draw.textbbox((0, 0), text, font=font, stroke_width=STROKE)
        x = (WIDTH - (right - left)) / 2 - left
        y = (HEIGHT - (bottom - top)) / 2 - top
        draw.text(
            (x, y),
            text,
            font=font,
            fill=(255, 255, 255, 255),
            stroke_width=STROKE,
            stroke_fill=(0, 0, 0, 255),
        )
        image.save(args.output / f"{index:02d}.png")


if __name__ == "__main__":
    main()
