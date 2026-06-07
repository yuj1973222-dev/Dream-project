from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "generated" / "leeseolhud_compass"
LOGICAL_WIDTH = 480
LOGICAL_HEIGHT = 34
SCALE = 4
WIDTH = LOGICAL_WIDTH * SCALE
HEIGHT = LOGICAL_HEIGHT * SCALE
RENDER_HEIGHT = LOGICAL_HEIGHT
BASE_CODEPOINT = 0xE340
STEP_DEGREES = 5
LABEL_STEP_DEGREES = 15
GLYPH_SEGMENTS = 16


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        Path("C:/Windows/Fonts/segoeui.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
        Path("C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


FONT_SMALL = font(9 * SCALE)
FONT_CARDINAL = font(10 * SCALE, bold=True)


def label(degrees: int) -> str:
    return {
        0: "N",
        45: "NE",
        90: "E",
        135: "SE",
        180: "S",
        225: "SW",
        270: "W",
        315: "NW",
    }.get(degrees % 360, str(degrees % 360))


def fade_alpha(x: int) -> int:
    margin = 52 * SCALE
    if margin <= x <= WIDTH - margin:
        return 255
    if x < margin:
        return int(70 + 185 * max(0, x) / margin)
    return int(70 + 185 * max(0, WIDTH - x) / margin)


def faded(fill: tuple[int, int, int], x: int, boost: int = 0) -> tuple[int, int, int, int]:
    return (*fill, max(0, min(255, fade_alpha(x) + boost)))


def draw_centered(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fnt, fill):
    bbox = draw.textbbox((0, 0), text, font=fnt)
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    draw.text(
        (xy[0] - width / 2 - bbox[0], xy[1] - height / 2 - bbox[1]),
        text,
        font=fnt,
        fill=fill,
    )


def force_fixed_glyph_advance(image: Image.Image) -> None:
    # Minecraft bitmap fonts can infer each glyph's advance from visible pixels.
    # Near-transparent anchors keep all compass segments equal-width without being
    # visible in normal play.
    cell_width = WIDTH // GLYPH_SEGMENTS
    pixels = image.load()
    for segment in range(GLYPH_SEGMENTS):
        left = segment * cell_width
        right = left + cell_width - 1
        pixels[left, HEIGHT - 1] = (0, 0, 0, 1)
        pixels[right, HEIGHT - 1] = (0, 0, 0, 1)


def make_image(center_degrees: int) -> Image.Image:
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    center_x = (LOGICAL_WIDTH // 2 - 9) * SCALE
    baseline_y = 10 * SCALE
    px_per_degree = 3.85 * SCALE
    silver = (248, 250, 252)
    minor_silver = (190, 195, 200)

    for delta in range(-60, 61, STEP_DEGREES):
        degree = (center_degrees + delta) % 360
        x = int(round(center_x + delta * px_per_degree))
        if x < 8 * SCALE or x > WIDTH - 8 * SCALE:
            continue

        cardinal = degree in (0, 90, 180, 270)
        diagonal = degree in (45, 135, 225, 315)
        labeled = degree % LABEL_STEP_DEGREES == 0
        tick_height = (9 if cardinal else 8 if diagonal else 7 if labeled else 4) * SCALE
        tick_fill = faded(silver if labeled else minor_silver, x, 0 if labeled else -10)
        draw.line((x, baseline_y, x, baseline_y - tick_height), fill=tick_fill, width=SCALE)

        if labeled:
            text = label(degree)
            label_font = FONT_CARDINAL if text in {"N", "NE", "E", "SE", "S", "SW", "W", "NW"} else FONT_SMALL
            draw_centered(draw, (x, 24 * SCALE), text, label_font, faded((250, 252, 255), x))

    # Draw the horizontal rule last so every tick visibly connects to the same baseline.
    for x in range(0, WIDTH):
        draw.line((x, baseline_y, x + 1, baseline_y), fill=faded(silver, x, 0), width=SCALE)

    # Center marker and selected heading.
    draw.polygon(
        [
            (center_x, 0),
            (center_x - 6 * SCALE, 9 * SCALE),
            (center_x + 6 * SCALE, 9 * SCALE),
        ],
        fill=(255, 255, 255, 255),
    )
    draw.line((center_x, 9 * SCALE, center_x, baseline_y + 2 * SCALE), fill=(255, 255, 255, 255), width=SCALE)

    force_fixed_glyph_advance(image)

    return image


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for old_file in OUT.glob("compass_*.png"):
        old_file.unlink()
    providers = []
    for index, degrees in enumerate(range(0, 360, STEP_DEGREES)):
        file_name = f"compass_{degrees:03d}.png"
        make_image(degrees).save(OUT / file_name)
        providers.append(
            {
                "type": "bitmap",
                "file": f"leeseolhud:gui/compass/{file_name}",
                "ascent": 10,
                "height": RENDER_HEIGHT,
                "chars": [
                    "".join(
                        chr(BASE_CODEPOINT + index * GLYPH_SEGMENTS + segment)
                        for segment in range(GLYPH_SEGMENTS)
                    )
                ],
            }
        )
    (OUT / "README.txt").write_text(
        "LeeSeolHUD compass glyphs. Codepoints: U+E340..U+E7BF. "
        "Each image represents 0..355 degrees in 5-degree steps. "
        "Labels are rendered every 15 degrees. "
        "Each heading uses 16 glyph segments to force stable glyph advance while using 4x source resolution.\n",
        encoding="utf-8",
    )
    print(f"generated {len(providers)} compass images in {OUT}")


if __name__ == "__main__":
    main()
