from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "assets" / "generated" / "gui_samples"
OUT = OUT_DIR / "map_gui_pixel_copy_183x255.png"
PREVIEW = OUT_DIR / "map_gui_pixel_copy_183x255_preview4x.png"
CLICKMAP = OUT_DIR / "map_gui_pixel_copy_183x255_clickmap.png"
CLICKMAP_PREVIEW = OUT_DIR / "map_gui_pixel_copy_183x255_clickmap_preview4x.png"
W, H = 183, 255


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
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


FONT_TITLE = font(9, False)
FONT_KO = font(10, True)
FONT_NUM = font(10, True)
FONT_LABEL = font(8, False)


def rect(d: ImageDraw.ImageDraw, box, fill=None, outline=None, width: int = 1):
    d.rectangle(box, fill=fill, outline=outline, width=width)


def line(d: ImageDraw.ImageDraw, pts, fill, width: int = 1):
    d.line(pts, fill=fill, width=width)


def text_center(d: ImageDraw.ImageDraw, box, text: str, fill, fnt, stroke=None):
    x1, y1, x2, y2 = box
    sw = 1 if stroke else 0
    bb = d.textbbox((0, 0), text, font=fnt, stroke_width=sw)
    tw = bb[2] - bb[0]
    th = bb[3] - bb[1]
    x = x1 + ((x2 - x1 + 1) - tw) // 2
    y = y1 + ((y2 - y1 + 1) - th) // 2 - 1
    d.text((x, y), text, font=fnt, fill=fill, stroke_width=sw, stroke_fill=stroke)


def gradient_rect(d: ImageDraw.ImageDraw, box, c1: str, c2: str, outline=None):
    x1, y1, x2, y2 = box
    a = rgba(c1)
    b = rgba(c2)
    for y in range(y1, y2 + 1):
        t = (y - y1) / max(1, y2 - y1)
        col = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))
        line(d, [(x1, y), (x2, y)], col)
    if outline:
        rect(d, box, None, outline)


def draw_title(d: ImageDraw.ImageDraw):
    # Large white-gray title plate matching the reference.
    rect(d, (21, 1, 161, 17), rgba("111111"))
    rect(d, (23, 2, 159, 15), rgba("e4e4e4"), rgba("6b6b6b"))
    rect(d, (25, 4, 157, 13), rgba("cfcfcf"))
    text_center(d, (23, 1, 159, 16), "지도", rgba("151515"), FONT_TITLE)
    rect(d, (18, 16, 164, 18), rgba("151515"))


def draw_frame(d: ImageDraw.ImageDraw):
    # Outer body and inventory area.
    rect(d, (0, 0, W - 1, H - 1), rgba("d4d4d4"), rgba("101010"))
    rect(d, (1, 18, W - 2, H - 2), rgba("d8d8d8"))

    # Main map panel.
    rect(d, (5, 31, 179, 145), rgba("05080d"), rgba("05080d"))
    rect(d, (7, 33, 177, 143), rgba("25344b"), rgba("020305"))
    rect(d, (9, 35, 165, 141), rgba("0e7bdd"), rgba("020305"))

    # Right scroll rail copied as visual-only element.
    rect(d, (166, 35, 177, 141), rgba("33425f"), rgba("111725"))
    rect(d, (167, 36, 176, 62), rgba("536178"))
    text_center(d, (167, 39, 176, 58), "×", rgba("f5f5f7"), FONT_NUM)
    rect(d, (168, 69, 175, 132), rgba("3a4b6a"), rgba("1b2435"))
    rect(d, (170, 96, 173, 126), rgba("6f85a4"), rgba("2e3c58"))
    d.polygon([(172, 64), (168, 67), (175, 67)], fill=rgba("f3f3f3"))
    d.polygon([(172, 138), (168, 134), (175, 134)], fill=rgba("f3f3f3"))


def draw_inventory(d: ImageDraw.ImageDraw):
    d.text((7, 151), "보관함", font=FONT_LABEL, fill=rgba("444444"))
    for row, y in enumerate([164, 182, 200, 229]):
        for col in range(9):
            x = 7 + col * 19
            rect(d, (x, y, x + 17, y + 16), rgba("8d8d8b"), rgba("2c2c2c"))
            line(d, [(x + 1, y + 1), (x + 16, y + 1)], rgba("ffffff"))
            line(d, [(x + 1, y + 1), (x + 1, y + 15)], rgba("ffffff"))
            line(d, [(x + 16, y + 2), (x + 16, y + 15)], rgba("3d3d3d"))
            line(d, [(x + 2, y + 15), (x + 16, y + 15)], rgba("3d3d3d"))


def draw_map_background(d: ImageDraw.ImageDraw):
    # Blue orbital map background.
    gradient_rect(d, (9, 35, 165, 141), "1694e4", "0358bd")
    # Planet arcs and moon shapes.
    d.ellipse((-18, 20, 62, 89), fill=rgba("1583df"))
    d.ellipse((-8, 23, 70, 91), fill=rgba("0b64c3"))
    d.pieslice((34, 20, 170, 124), 205, 342, fill=rgba("3f8bde"))
    d.pieslice((45, 21, 165, 122), 205, 342, fill=rgba("075abd"))
    d.arc((52, 55, 158, 151), 205, 335, fill=rgba("9fdcff"), width=3)
    d.ellipse((67, 92, 112, 133), fill=rgba("eaf8ff"), outline=rgba("11141d"), width=2)
    d.pieslice((73, 83, 118, 126), 292, 55, fill=rgba("242433"))
    d.ellipse((74, 90, 106, 121), fill=rgba("9adfff"))
    d.ellipse((75, 91, 94, 111), fill=rgba("c7f4ff"))
    # Dots/stars.
    for x, y, c in [
        (13, 50, "a9f4ff"), (24, 68, "74dfff"), (49, 60, "051627"),
        (58, 67, "051627"), (101, 97, "07131d"), (116, 97, "07131d"),
        (132, 69, "95dcff"), (147, 103, "c9f8ff"), (35, 83, "a8edff"),
        (55, 131, "d7ffff"), (20, 113, "f5ffff"), (117, 119, "74dfff"),
    ]:
        rect(d, (x, y, x + 1, y + 1), rgba(c))
    d.ellipse((12, 76, 18, 82), fill=rgba("6ccaff"), outline=rgba("050608"))
    d.ellipse((20, 50, 28, 58), fill=rgba("b9f1ff"), outline=rgba("06090d"))
    d.ellipse((130, 97, 140, 107), fill=rgba("9fe5ff"), outline=rgba("06090d"))


def draw_left_character(d: ImageDraw.ImageDraw):
    # Simplified copy of the left explorer and telescope.
    rect(d, (18, 105, 34, 132), rgba("214928"), rgba("090909"))
    rect(d, (20, 97, 32, 108), rgba("f7d28d"), rgba("1a1008"))
    rect(d, (18, 92, 34, 99), rgba("f4c33b"), rgba("1c1207"))
    rect(d, (17, 89, 35, 93), rgba("245122"), rgba("111111"))
    line(d, [(21, 107), (16, 124)], rgba("f0c58b"), 2)
    line(d, [(31, 107), (38, 122)], rgba("f0c58b"), 2)
    rect(d, (42, 96, 56, 109), rgba("9caabd"), rgba("070707"))
    d.ellipse((38, 91, 58, 111), fill=rgba("526171"), outline=rgba("0a0a0a"), width=2)
    d.ellipse((43, 96, 55, 107), fill=rgba("111820"))
    line(d, [(47, 110), (42, 134)], rgba("161a1d"), 2)
    line(d, [(50, 110), (57, 134)], rgba("161a1d"), 2)


def draw_right_character(d: ImageDraw.ImageDraw):
    # Simplified copy of the right explorer.
    rect(d, (134, 110, 151, 132), rgba("1f4928"), rgba("090909"))
    rect(d, (136, 100, 148, 112), rgba("f1c493"), rgba("1a1008"))
    rect(d, (132, 91, 154, 101), rgba("9ec5cf"), rgba("21242a"))
    rect(d, (135, 94, 151, 102), rgba("fed36a"))
    d.ellipse((124, 89, 137, 102), fill=rgba("9fe5ff"), outline=rgba("06090d"), width=2)
    line(d, [(136, 112), (123, 102)], rgba("f1c493"), 2)
    line(d, [(148, 112), (156, 122)], rgba("f1c493"), 2)
    d.ellipse((132, 116, 147, 136), fill=rgba("3090c8"), outline=rgba("031018"))


def draw_button(d: ImageDraw.ImageDraw):
    # Central blue button with Korean text.
    rect(d, (62, 105, 122, 127), rgba("1b1e29"), rgba("020202"), 2)
    rect(d, (65, 108, 119, 124), rgba("1b7bbf"), rgba("0d3e68"))
    gradient_rect(d, (66, 109, 118, 123), "63e1f2", "18a8d7")
    line(d, [(68, 110), (116, 110)], rgba("d3ffff"))
    text_center(d, (65, 106, 119, 125), "생성", rgba("ffffff"), FONT_KO, stroke=rgba("1a3849"))


def draw_island(d: ImageDraw.ImageDraw, cx: int, cy: int, number: str):
    # Floating island copied as a compact dot sprite.
    d.ellipse((cx - 20, cy - 7, cx + 20, cy + 17), fill=rgba("126cad"))
    d.ellipse((cx - 17, cy - 6, cx + 17, cy + 14), fill=rgba("64d3ff"))
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 10), fill=rgba("171a20"))
    d.ellipse((cx - 13, cy - 14, cx + 13, cy + 8), fill=rgba("9b6d3b"))
    d.polygon(
        [(cx - 14, cy - 8), (cx + 13, cy - 8), (cx + 10, cy + 3), (cx + 2, cy + 10), (cx - 8, cy + 6)],
        fill=rgba("754628"),
        outline=rgba("160d08"),
    )
    d.ellipse((cx - 13, cy - 19, cx + 13, cy - 5), fill=rgba("12311d"), outline=rgba("07100a"))
    d.ellipse((cx - 11, cy - 20, cx + 11, cy - 7), fill=rgba("57bb3d"))
    d.ellipse((cx - 8, cy - 18, cx + 8, cy - 9), fill=rgba("85e75b"))
    # Tiny trees/rocks.
    for ox in (-8, -3, 7):
        rect(d, (cx + ox, cy - 19, cx + ox + 2, cy - 12), rgba("144b23"))
        d.polygon(
            [(cx + ox - 3, cy - 16), (cx + ox + 1, cy - 23), (cx + ox + 5, cy - 16)],
            fill=rgba("1f8d3b"),
            outline=rgba("06280e"),
        )
    d.ellipse((cx + 6, cy - 10, cx + 11, cy - 6), fill=rgba("c6d9cd"), outline=rgba("3c4a40"))
    d.ellipse((cx - 13, cy + 7, cx - 4, cy + 12), fill=rgba("48b7fa"))
    d.ellipse((cx + 3, cy + 7, cx + 13, cy + 12), fill=rgba("48b7fa"))

    # Number placard.
    rect(d, (cx - 8, cy - 31, cx + 9, cy - 10), rgba("f6f6f6"), rgba("111111"), 2)
    text_center(d, (cx - 7, cy - 30, cx + 8, cy - 11), number, rgba("222222"), FONT_NUM)


def draw_clickmap(base: Image.Image) -> Image.Image:
    img = base.copy()
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    for x in [5 + i * 18 for i in range(10)]:
        line(d, [(x, 0), (x, H)], rgba("ff2020", 170))
    for y in [14 + i * 18 for i in range(12)]:
        line(d, [(0, y), (W, y)], rgba("ff2020", 170))
    for box in [(113, 58, 153, 97), (36, 58, 76, 97), (74, 27, 114, 66), (62, 105, 122, 127)]:
        rect(d, box, None, rgba("00ff7f", 220), 2)
    return Image.alpha_composite(img, overlay)


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_frame(d)
    draw_map_background(d)
    draw_left_character(d)
    draw_right_character(d)
    draw_island(d, 129, 76, "1")
    draw_island(d, 54, 76, "2")
    draw_island(d, 91, 43, "3")
    draw_button(d)
    draw_title(d)
    draw_inventory(d)

    img.save(OUT)
    img.resize((W * 4, H * 4), Image.Resampling.NEAREST).save(PREVIEW)
    clickmap = draw_clickmap(img)
    clickmap.save(CLICKMAP)
    clickmap.resize((W * 4, H * 4), Image.Resampling.NEAREST).save(CLICKMAP_PREVIEW)
    print(OUT)
    print(PREVIEW)
    print(CLICKMAP)
    print(CLICKMAP_PREVIEW)


if __name__ == "__main__":
    main()
