from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "generated" / "gui_samples"
W, H = 183, 255
SCALE = 3


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        r"C:\Windows\Fonts\malgunbd.ttf" if bold else r"C:\Windows\Fonts\malgun.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


FONT_5 = font(5)
FONT_6 = font(6)
FONT_7 = font(7)
FONT_8_B = font(8, bold=True)
FONT_10_B = font(10, bold=True)


def rect(d: ImageDraw.ImageDraw, box, fill, outline=None, width: int = 1):
    d.rectangle(box, fill=fill, outline=outline, width=width)


def line(d: ImageDraw.ImageDraw, points, fill, width: int = 1):
    d.line(points, fill=fill, width=width)


def centered_text(d: ImageDraw.ImageDraw, box, text: str, fill, fnt):
    x1, y1, x2, y2 = box
    bbox = d.textbbox((0, 0), text, font=fnt)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = x1 + ((x2 - x1 + 1) - tw) // 2
    y = y1 + ((y2 - y1 + 1) - th) // 2 - 1
    d.text((x, y), text, font=fnt, fill=fill)


def pixel_sword(d: ImageDraw.ImageDraw, cx: int, cy: int, flip: bool = False):
    sx = -1 if flip else 1
    steel = rgba("bcefff")
    edge = rgba("1a4f73")
    dark = rgba("081827")
    gold = rgba("d4a34a")
    # blade diagonal
    for i in range(15):
        x = cx + sx * i
        y = cy - i
        rect(d, (min(x, x + sx), y, max(x, x + sx), y + 1), steel)
        rect(d, (x - sx, y + 1, x - sx, y + 1), edge)
    tip_x1 = cx + sx * 15
    tip_x2 = cx + sx * 17
    rect(d, (min(tip_x1, tip_x2), cy - 16, max(tip_x1, tip_x2), cy - 14), rgba("e9ffff"))
    # guard and handle
    rect(d, (cx - 3, cy + 1, cx + 3, cy + 3), edge)
    rect(d, (cx - 2, cy + 4, cx + 2, cy + 8), dark)
    rect(d, (cx - 1, cy + 8, cx + 1, cy + 10), gold)


def draw_outer_frame(d: ImageDraw.ImageDraw):
    # A 6-row large chest uses the upper 126 px for the container background.
    # The lower player-inventory area must stay fully transparent.
    chest_h = 126
    left, top, right, bottom = 3, 3, W - 4, chest_h - 1
    border = 10

    def blend(a: str, b: str, t: float) -> tuple[int, int, int, int]:
        ar, ag, ab, _ = rgba(a)
        br, bg, bb, _ = rgba(b)
        return (
            int(ar + (br - ar) * t),
            int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t),
            255,
        )

    def horizontal_bar(y1: int, y2: int, top_color: str, bottom_color: str):
        for y in range(y1, y2 + 1):
            t = (y - y1) / max(1, y2 - y1)
            line(d, [(left, y), (right, y)], blend(top_color, bottom_color, t))

    def vertical_bar(x1: int, x2: int, left_color: str, right_color: str):
        for x in range(x1, x2 + 1):
            t = (x - x1) / max(1, x2 - x1)
            line(d, [(x, top + border), (x, bottom - border)], blend(left_color, right_color, t))

    # Plain blue-steel plate frame only. No rivets, corner plates, icons, text, or inventory frame.
    horizontal_bar(top, top + border - 1, "5e8797", "111c24")
    horizontal_bar(bottom - border + 1, bottom, "1b2b34", "05090d")
    vertical_bar(left, left + border - 1, "547a8c", "101922")
    vertical_bar(right - border + 1, right, "1b2b34", "05090d")

    # Thin bevel lines keep the metal readable without adding decorative parts.
    rect(d, (left, top, right, bottom), None, rgba("020508"), 1)
    rect(d, (left + 1, top + 1, right - 1, bottom - 1), None, rgba("83b8ca"), 1)
    rect(d, (left + border, top + border, right - border, bottom - border), None, rgba("071017"), 1)

    # The center remains transparent for later title/button work.


def draw_frame(d: ImageDraw.ImageDraw):
    draw_outer_frame(d)


def draw_lobby_scene(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    rect(d, box, rgba("07121c"))
    # sky/snow slopes
    d.polygon([(x1, y2), (x1 + 24, y1 + 18), (x1 + 52, y2)], fill=rgba("d8f5ff"))
    d.polygon([(x1 + 20, y2), (x1 + 48, y1 + 13), (x2, y2)], fill=rgba("9cd4ef"))
    d.polygon([(x1 + 58, y2), (x1 + 74, y1 + 10), (x2, y2)], fill=rgba("f2ffff"))
    # lodge
    rect(d, (x1 + 56, y1 + 18, x1 + 78, y2 - 5), rgba("5a351d"), rgba("0a0805"))
    d.polygon([(x1 + 51, y1 + 19), (x1 + 67, y1 + 5), (x1 + 83, y1 + 19)], fill=rgba("dceaf0"))
    rect(d, (x1 + 64, y1 + 28, x1 + 69, y2 - 5), rgba("f7c858"))
    rect(d, (x1 + 72, y2 - 14, x2, y2), rgba("2cb9dd"))


def draw_survival_scene(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    rect(d, box, rgba("06170d"))
    d.polygon([(x1, y2), (x1 + 28, y1 + 22), (x1 + 64, y2)], fill=rgba("385f28"))
    d.polygon([(x1 + 36, y2), (x1 + 67, y1 + 15), (x2, y2)], fill=rgba("275224"))
    # mine entrance
    rect(d, (x1 + 54, y1 + 20, x1 + 82, y2 - 5), rgba("3b2a1b"), rgba("0e0b07"))
    d.polygon([(x1 + 48, y1 + 20), (x1 + 68, y1 + 4), (x1 + 88, y1 + 20)], fill=rgba("34434a"))
    rect(d, (x1 + 4, y2 - 12, x2, y2), rgba("2f7b37"))
    for i in range(0, 50, 7):
        line(d, [(x1 + 20 + i, y2), (x1 + 20 + i, y2 - 8)], rgba("b2d068"))


def draw_newworld_scene(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    rect(d, box, rgba("051926"))
    # sea and ruins
    rect(d, (x1, y1 + 31, x2, y2), rgba("10556b"))
    d.polygon([(x1 + 47, y1 + 34), (x1 + 66, y1 + 7), (x1 + 88, y1 + 34)], fill=rgba("31525b"))
    rect(d, (x1 + 20, y1 + 22, x1 + 48, y2 - 8), rgba("736345"), rgba("1a150e"))
    rect(d, (x1 + 27, y1 + 11, x1 + 38, y1 + 22), rgba("998b64"))
    d.polygon([(x1 + 62, y1 + 18), (x1 + 62, y2 - 7), (x1 + 86, y2 - 7)], fill=rgba("e6f4f1"))
    rect(d, (x1 + 72, y2 - 12, x2, y2), rgba("d39735"))


def draw_icon_panel(d: ImageDraw.ImageDraw, box, kind: str):
    x1, y1, x2, y2 = box
    rect(d, box, rgba("061019"), rgba("e8fbff"), 1)
    rect(d, (x1 + 1, y1 + 1, x2 - 1, y2 - 1), rgba("0c1a22"), rgba("1b3a4a"))
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if kind == "snow":
        for dx, dy in [(0, 1), (1, 0), (1, 1), (1, -1)]:
            line(d, [(cx - dx * 10, cy - dy * 10), (cx + dx * 10, cy + dy * 10)], rgba("e9ffff"), 2)
        rect(d, (cx - 2, cy - 2, cx + 2, cy + 2), rgba("7fe9ff"))
    elif kind == "pick":
        line(d, [(cx - 7, cy + 10), (cx + 6, cy - 6)], rgba("cfa14a"), 3)
        line(d, [(cx - 11, cy - 6), (cx + 10, cy - 11)], rgba("cadce2"), 3)
        line(d, [(cx - 6, cy - 12), (cx + 13, cy - 3)], rgba("5a6e75"), 2)
    else:
        # map + compass
        d.polygon([(cx - 12, cy - 8), (cx - 2, cy - 12), (cx + 10, cy - 5), (cx + 6, cy + 10), (cx - 13, cy + 8)], fill=rgba("d7c59b"), outline=rgba("4d3921"))
        line(d, [(cx - 6, cy - 4), (cx + 5, cy + 4)], rgba("2d6173"))
        d.polygon([(cx + 5, cy - 12), (cx + 10, cy), (cx + 3, cy + 9), (cx - 3, cy)], fill=rgba("cfefff"), outline=rgba("10283b"))


def draw_card(d: ImageDraw.ImageDraw, y: int, accent: str, kind: str, scene):
    x1, x2 = 23, 160
    y1, y2 = y, y + 43
    accent_rgba = rgba(accent)
    rect(d, (x1 - 2, y1 - 2, x2 + 2, y2 + 2), rgba("10171b"), rgba("010406"), 2)
    rect(d, (x1 - 1, y1 - 1, x2 + 1, y2 + 1), None, accent_rgba, 1)
    rect(d, (x1, y1, x2, y2), rgba("121a1f"), rgba("dffaff"), 1)
    draw_icon_panel(d, (x1 + 4, y1 + 4, x1 + 37, y2 - 4), kind)
    scene(d, (x1 + 42, y1 + 5, x2 - 5, y2 - 5))
    # dark title strip and arrow area, text intentionally omitted for plugin/localization flexibility.
    rect(d, (x1 + 44, y1 + 8, x1 + 93, y1 + 17), rgba("d7ecf0", 190))
    rect(d, (x1 + 46, y2 - 13, x1 + 128, y2 - 8), rgba("74dfff", 185))
    d.polygon([(x2 - 16, y2 - 18), (x2 - 6, y2 - 12), (x2 - 16, y2 - 6)], fill=rgba("f0fff8"))


def draw_inventory(d: ImageDraw.ImageDraw):
    rect(d, (22, 207, 160, 249), rgba("141719"), rgba("070a0c"), 2)
    rect(d, (27, 213, 155, 242), rgba("222526"), rgba("3d4243"))
    for row_y in (216, 232):
        for col in range(9):
            x = 29 + col * 14
            rect(d, (x, row_y, x + 11, row_y + 11), rgba("313333"), rgba("070909"))
            line(d, [(x + 1, row_y + 1), (x + 10, row_y + 1)], rgba("555b5b"))
            line(d, [(x + 1, row_y + 10), (x + 10, row_y + 10)], rgba("171919"))
    # navigation buttons
    for x, direction in ((5, "left"), (166, "right")):
        rect(d, (x, 216, x + 14, 239), rgba("36454b"), rgba("10181c"), 2)
        rect(d, (x + 4, 221, x + 10, 233), rgba("6c8792"), rgba("bff6ff"))
        if direction == "left":
            d.polygon([(x + 5, 227), (x + 11, 221), (x + 11, 233)], fill=rgba("d8fbff"))
        else:
            d.polygon([(x + 11, 227), (x + 5, 221), (x + 5, 233)], fill=rgba("d8fbff"))


def draw_lamps(d: ImageDraw.ImageDraw):
    for x in (8, 171):
        rect(d, (x, 174, x + 6, 194), rgba("1b1209"), rgba("050505"))
        rect(d, (x + 1, 178, x + 5, 188), rgba("ffc85a"), rgba("7a491e"))
        rect(d, (x + 2, 176, x + 4, 178), rgba("ffef9f"))


def draw_gui() -> Image.Image:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_frame(d)
    draw_lamps(d)
    draw_card(d, 54, "6fd6ff", "snow", draw_lobby_scene)
    draw_card(d, 105, "72e68b", "pick", draw_survival_scene)
    draw_card(d, 156, "f1b64d", "map", draw_newworld_scene)
    draw_inventory(d)
    # corner swords for expedition identity
    pixel_sword(d, 20, 31, False)
    pixel_sword(d, 163, 31, True)
    return img


def draw_frame_only() -> Image.Image:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_outer_frame(d)
    return img


def draw_clickmap(base: Image.Image) -> Image.Image:
    img = base.copy()
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    slot_x = [5 + i * 18 for i in range(10)]
    slot_y = [14 + i * 18 for i in range(12)]
    for x in slot_x:
        line(d, [(x, 0), (x, H)], rgba("ff0000", 210), 1)
    for y in slot_y:
        line(d, [(0, y), (W, y)], rgba("ff0000", 210), 1)
    # recommended click zones
    for box, color in [
        ((23, 54, 160, 97), "6fd6ff"),
        ((23, 105, 160, 148), "72e68b"),
        ((23, 156, 160, 199), "f1b64d"),
    ]:
        rect(d, box, None, rgba(color, 230), 2)
    return Image.alpha_composite(img, overlay)


def save_preview(img: Image.Image, path: Path):
    img.resize((img.width * SCALE, img.height * SCALE), Image.Resampling.NEAREST).save(path)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    frame = draw_frame_only()

    frame_path = OUT / "expedition_frame_blue_steel_183x255.png"
    preview_path = OUT / "expedition_frame_blue_steel_183x255_preview3x.png"

    frame.save(frame_path)
    save_preview(frame, preview_path)

    for path in [frame_path, preview_path]:
        print(path)


if __name__ == "__main__":
    main()
