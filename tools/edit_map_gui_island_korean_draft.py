from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "assets" / "generated" / "gui_samples"
SRC = OUT_DIR / "map_gui_original_2026-06-07_04.27.24.png"
OUT = OUT_DIR / "map_gui_island_korean_draft.png"
PREVIEW = OUT_DIR / "map_gui_island_korean_draft_preview2x.png"


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def font(size: int, bold: bool = False):
    candidates = [
        r"C:\Windows\Fonts\malgunbd.ttf" if bold else r"C:\Windows\Fonts\malgun.ttf",
        r"C:\Windows\Fonts\gulim.ttc",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


FONT_TITLE = font(15, True)


def rect(d: ImageDraw.ImageDraw, box, fill=None, outline=None, width: int = 1):
    d.rectangle(box, fill=fill, outline=outline, width=width)


def line(d: ImageDraw.ImageDraw, points, fill, width: int = 1):
    d.line(points, fill=fill, width=width)


def is_yellow_markup(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, a = pixel
    return a > 0 and r > 170 and g > 135 and b < 95


def remove_yellow_markup(img: Image.Image, box: tuple[int, int, int, int]):
    # Remove only the user's yellow markup pixels. Avoid broad repainting so the
    # source pixel-art background survives.
    px = img.load()
    src = img.copy().load()
    x1, y1, x2, y2 = box
    w, h = img.size
    for y in range(max(0, y1), min(h, y2 + 1)):
        for x in range(max(0, x1), min(w, x2 + 1)):
            if not is_yellow_markup(src[x, y]):
                continue
            samples = []
            for radius in (3, 6, 10, 14):
                samples.clear()
                for yy in range(max(0, y - radius), min(h, y + radius + 1)):
                    for xx in range(max(0, x - radius), min(w, x + radius + 1)):
                        p = src[xx, yy]
                        if not is_yellow_markup(p):
                            samples.append(p)
                if samples:
                    break
            if samples:
                samples.sort(key=lambda p: p[0] + p[1] + p[2])
                px[x, y] = samples[len(samples) // 2]


def centered_text(d: ImageDraw.ImageDraw, box, text: str, fill, fnt, stroke=None):
    x1, y1, x2, y2 = box
    bbox = d.textbbox((0, 0), text, font=fnt, stroke_width=1 if stroke else 0)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = x1 + ((x2 - x1 + 1) - tw) // 2
    y = y1 + ((y2 - y1 + 1) - th) // 2 - 1
    d.text((x, y), text, font=fnt, fill=fill, stroke_width=1 if stroke else 0, stroke_fill=stroke)


def draw_island(d: ImageDraw.ImageDraw, cx: int, cy: int, scale: int = 1):
    # Small floating island marker: grass top, dirt underside, water rim,
    # matching the existing blue/green map-marker palette.
    s = scale
    outline = rgba("06202a")
    dark = rgba("0a3941")
    grass = rgba("8fd957")
    grass_hi = rgba("ccf28c")
    dirt = rgba("8a6a37")
    dirt_dark = rgba("3f2d19")
    water = rgba("89eaff")

    # Shadow/rim.
    d.ellipse((cx - 16*s, cy - 12*s, cx + 16*s, cy + 12*s), fill=rgba("06202a", 230))
    d.ellipse((cx - 14*s, cy - 11*s, cx + 14*s, cy + 10*s), fill=water)
    d.ellipse((cx - 11*s, cy - 8*s, cx + 11*s, cy + 7*s), fill=dark)

    # Island body.
    d.polygon(
        [
            (cx - 10*s, cy - 2*s),
            (cx + 10*s, cy - 2*s),
            (cx + 7*s, cy + 8*s),
            (cx + 2*s, cy + 12*s),
            (cx - 5*s, cy + 9*s),
            (cx - 10*s, cy + 4*s),
        ],
        fill=dirt,
        outline=outline,
    )
    d.polygon(
        [
            (cx - 11*s, cy - 8*s),
            (cx + 10*s, cy - 8*s),
            (cx + 13*s, cy - 3*s),
            (cx + 8*s, cy + 1*s),
            (cx - 8*s, cy + 1*s),
            (cx - 13*s, cy - 3*s),
        ],
        fill=grass,
        outline=outline,
    )
    line(d, [(cx - 8*s, cy - 6*s), (cx + 6*s, cy - 6*s)], grass_hi, max(1, s))
    line(d, [(cx - 7*s, cy + 4*s), (cx - 2*s, cy + 7*s)], dirt_dark, max(1, s))
    line(d, [(cx + 4*s, cy + 3*s), (cx + 2*s, cy + 8*s)], dirt_dark, max(1, s))

    # Tiny landmark on top.
    rect(d, (cx - 2*s, cy - 16*s, cx + 2*s, cy - 8*s), rgba("244f35"), outline)
    d.polygon(
        [(cx - 5*s, cy - 8*s), (cx, cy - 13*s), (cx + 5*s, cy - 8*s)],
        fill=rgba("f3e27c"),
        outline=outline,
    )


def repaint_title(img: Image.Image):
    d = ImageDraw.Draw(img)
    # Title plaque interior. Keep the surrounding gray metal frame intact.
    rect(d, (43, 5, 300, 30), rgba("bdbdbd"))
    rect(d, (46, 7, 297, 27), rgba("d2d2d2"))
    line(d, [(46, 28), (297, 28)], rgba("8b8b8b"))
    centered_text(d, (43, 3, 300, 30), "지도", rgba("323232"), FONT_TITLE, stroke=rgba("eeeeee"))


def main():
    img = Image.open(SRC).convert("RGBA")
    d = ImageDraw.Draw(img)

    # Remove yellow annotation underline and rough circle marks first.
    remove_yellow_markup(img, (150, 24, 196, 31))
    for box in [(145, 45, 196, 83), (72, 91, 128, 130), (225, 92, 281, 133)]:
        remove_yellow_markup(img, box)

    repaint_title(img)

    # Repaint each marked position as island-like map markers.
    draw_island(d, 171, 64, 1)
    draw_island(d, 101, 112, 1)
    draw_island(d, 253, 113, 1)

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    img.resize((img.width * 2, img.height * 2), Image.Resampling.NEAREST).save(PREVIEW)
    print(OUT)
    print(PREVIEW)


if __name__ == "__main__":
    main()
