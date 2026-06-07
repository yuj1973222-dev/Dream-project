from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "generated" / "gui_samples"
W, H = 183, 255
PREVIEW_SCALE = 4


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


FONT_6 = font(6, False)
FONT_8_B = font(8, True)
FONT_10_B = font(10, True)


def rect(d: ImageDraw.ImageDraw, box, fill=None, outline=None, width: int = 1):
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


def blend(c1: str, c2: str, t: float, alpha: int = 255) -> tuple[int, int, int, int]:
    a = rgba(c1)
    b = rgba(c2)
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
        alpha,
    )


def gradient_rect(d: ImageDraw.ImageDraw, box, top: str, bottom: str, outline=None):
    x1, y1, x2, y2 = box
    for y in range(y1, y2 + 1):
        t = (y - y1) / max(1, y2 - y1)
        line(d, [(x1, y), (x2, y)], blend(top, bottom, t))
    if outline:
        rect(d, box, None, outline)


def draw_outer_shell(d: ImageDraw.ImageDraw):
    # 183x255 is the ItemsAdder-style large chest GUI canvas used by the examples.
    # The whole menu is drawn because this is a visual concept, not a clickmap.
    rect(d, (5, 12, 177, 247), rgba("14181b"), rgba("050709"), 2)
    rect(d, (8, 15, 174, 244), rgba("2c3439"), rgba("73848c"), 1)
    rect(d, (12, 35, 170, 206), rgba("0b1116"), rgba("030506"), 1)

    # Side steel rails echo the Pixie menu frame, but use expedition blue steel.
    for x1, x2 in ((9, 18), (165, 174)):
        gradient_rect(d, (x1, 19, x2, 238), "38444c", "12191f", rgba("030506"))
        line(d, [(x1 + 1, 20), (x1 + 1, 237)], rgba("7fb9cf"))
        line(d, [(x2 - 1, 20), (x2 - 1, 237)], rgba("081016"))
        for y in (28, 64, 100, 136, 172, 218):
            rect(d, (x1 + 2, y, x2 - 2, y + 7), rgba("8c6a3f"), rgba("2a1c10"))

    # Bottom inventory frame.
    gradient_rect(d, (23, 211, 159, 241), "2a2e30", "151819", rgba("030506"))
    for row_y in (216, 231):
        for col in range(9):
            x = 28 + col * 14
            rect(d, (x, row_y, x + 11, row_y + 11), rgba("343637"), rgba("070808"))
            line(d, [(x + 1, row_y + 1), (x + 10, row_y + 1)], rgba("5d6262"))
            line(d, [(x + 1, row_y + 10), (x + 10, row_y + 10)], rgba("161818"))


def draw_title(d: ImageDraw.ImageDraw):
    gradient_rect(d, (28, 6, 155, 27), "d7e0e2", "71777b", rgba("111315"))
    rect(d, (31, 9, 152, 24), rgba("1a1d20"), rgba("030405"))
    centered_text(d, (31, 7, 152, 20), "EXPEDITION MENU", rgba("f3f7f8"), FONT_10_B)
    centered_text(d, (31, 18, 152, 26), "서버 이동", rgba("9fefff"), FONT_6)

    # Small banner tabs.
    for x in (36, 134):
        gradient_rect(d, (x, 27, x + 12, 49), "134d72", "081d31", rgba("03101c"))
        d.polygon([(x, 49), (x + 6, 57), (x + 12, 49)], fill=rgba("08213a"))
        compass(d, x + 6, 38, 6)


def compass(d: ImageDraw.ImageDraw, cx: int, cy: int, r: int):
    line(d, [(cx, cy - r), (cx, cy + r)], rgba("f4fbff"))
    line(d, [(cx - r, cy), (cx + r, cy)], rgba("f4fbff"))
    d.polygon([(cx, cy - r - 2), (cx + 2, cy), (cx, cy + 2), (cx - 2, cy)], fill=rgba("8ee7ff"))


def draw_sword(d: ImageDraw.ImageDraw, cx: int, cy: int, flip: bool):
    sx = -1 if flip else 1
    for i in range(17):
        x = cx + sx * i
        y = cy - i
        rect(d, (min(x, x + sx), y, max(x, x + sx), y + 1), rgba("bff7ff"))
        rect(d, (x - sx, y + 1, x - sx, y + 1), rgba("235a78"))
    rect(d, (cx - 4, cy + 2, cx + 4, cy + 4), rgba("2d5263"), rgba("0a1820"))
    rect(d, (cx - 1, cy + 5, cx + 1, cy + 11), rgba("79542b"))
    rect(d, (cx - 2, cy + 10, cx + 2, cy + 13), rgba("d7b15d"))


def draw_scene_lobby(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    gradient_rect(d, box, "14283a", "071017")
    d.polygon([(x1, y2), (x1 + 22, y1 + 16), (x1 + 49, y2)], fill=rgba("eefcff"))
    d.polygon([(x1 + 18, y2), (x1 + 46, y1 + 11), (x2, y2)], fill=rgba("92d2ec"))
    rect(d, (x1 + 58, y1 + 19, x1 + 78, y2 - 5), rgba("55341e"), rgba("130b06"))
    d.polygon([(x1 + 52, y1 + 20), (x1 + 68, y1 + 7), (x1 + 84, y1 + 20)], fill=rgba("edf9ff"))
    rect(d, (x1 + 64, y1 + 25, x1 + 69, y2 - 5), rgba("ffc95c"))


def draw_scene_survival(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    gradient_rect(d, box, "0b2918", "06110b")
    d.polygon([(x1, y2), (x1 + 32, y1 + 19), (x1 + 70, y2)], fill=rgba("3d7134"))
    d.polygon([(x1 + 42, y2), (x1 + 69, y1 + 12), (x2, y2)], fill=rgba("275d2d"))
    rect(d, (x1 + 54, y1 + 19, x1 + 82, y2 - 5), rgba("3d2b1b"), rgba("0e0804"))
    d.polygon([(x1 + 48, y1 + 20), (x1 + 68, y1 + 5), (x1 + 88, y1 + 20)], fill=rgba("58676c"))
    for i in range(0, 50, 8):
        line(d, [(x1 + 20 + i, y2), (x1 + 20 + i, y2 - 8)], rgba("c3d778"))


def draw_scene_newworld(d: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    gradient_rect(d, box, "082635", "06131c")
    rect(d, (x1, y1 + 28, x2, y2), rgba("10566d"))
    d.polygon([(x1 + 52, y1 + 31), (x1 + 69, y1 + 8), (x1 + 91, y1 + 31)], fill=rgba("4d6164"))
    rect(d, (x1 + 20, y1 + 23, x1 + 45, y2 - 7), rgba("746545"), rgba("1b150e"))
    rect(d, (x1 + 28, y1 + 12, x1 + 38, y1 + 23), rgba("a59566"))
    d.polygon([(x1 + 62, y1 + 18), (x1 + 62, y2 - 7), (x1 + 86, y2 - 7)], fill=rgba("effdfb"))


def draw_panel(d: ImageDraw.ImageDraw, y: int, accent: str, label: str, subtitle: str, icon: str, scene_fn):
    x1, x2 = 25, 158
    y1, y2 = y, y + 42
    rect(d, (x1 - 2, y1 - 2, x2 + 2, y2 + 2), rgba("06090b"), rgba("010203"), 2)
    rect(d, (x1 - 1, y1 - 1, x2 + 1, y2 + 1), None, rgba(accent), 1)
    gradient_rect(d, (x1, y1, x2, y2), "172027", "0a0f14", rgba("d7eef4"))
    draw_icon(d, (x1 + 4, y1 + 4, x1 + 34, y2 - 4), icon, accent)
    scene_fn(d, (x1 + 39, y1 + 5, x2 - 5, y2 - 5))

    # Readable Korean labels are part of this sample; click logic remains slot-based.
    rect(d, (x1 + 42, y1 + 8, x1 + 96, y1 + 20), rgba("071015", 215))
    d.text((x1 + 45, y1 + 6), label, font=FONT_10_B, fill=rgba(accent))
    rect(d, (x1 + 42, y2 - 15, x1 + 130, y2 - 7), rgba("071015", 205))
    d.text((x1 + 45, y2 - 17), subtitle, font=FONT_6, fill=rgba("f2f5f2"))
    d.polygon([(x2 - 16, y2 - 18), (x2 - 6, y2 - 12), (x2 - 16, y2 - 6)], fill=rgba("f3fff8"))


def draw_icon(d: ImageDraw.ImageDraw, box, kind: str, accent: str):
    rect(d, box, rgba("081018"), rgba("dffaff"))
    x1, y1, x2, y2 = box
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if kind == "snow":
        for dx, dy in ((0, 1), (1, 0), (1, 1), (1, -1)):
            line(d, [(cx - dx * 10, cy - dy * 10), (cx + dx * 10, cy + dy * 10)], rgba("eaffff"), 2)
        rect(d, (cx - 2, cy - 2, cx + 2, cy + 2), rgba(accent))
    elif kind == "pick":
        line(d, [(cx - 6, cy + 10), (cx + 7, cy - 6)], rgba("cfa24a"), 3)
        line(d, [(cx - 11, cy - 5), (cx + 11, cy - 10)], rgba("d9edf2"), 3)
        line(d, [(cx - 7, cy - 12), (cx + 13, cy - 3)], rgba("5c7179"), 2)
    else:
        d.polygon([(cx - 12, cy - 8), (cx - 2, cy - 12), (cx + 10, cy - 5), (cx + 6, cy + 10), (cx - 13, cy + 8)], fill=rgba("d4c397"), outline=rgba("4a3822"))
        d.polygon([(cx + 4, cy - 12), (cx + 10, cy), (cx + 3, cy + 10), (cx - 3, cy)], fill=rgba("d9fbff"), outline=rgba("11314a"))


def draw_nav_buttons(d: ImageDraw.ImageDraw):
    for x, flip in ((7, False), (162, True)):
        gradient_rect(d, (x, 218, x + 13, 239), "4d6470", "1b252b", rgba("071016"))
        if flip:
            d.polygon([(x + 9, 228), (x + 4, 222), (x + 4, 234)], fill=rgba("d8fbff"))
        else:
            d.polygon([(x + 4, 228), (x + 9, 222), (x + 9, 234)], fill=rgba("d8fbff"))


def draw_gui() -> Image.Image:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_outer_shell(d)
    draw_title(d)
    draw_sword(d, 25, 43, False)
    draw_sword(d, 158, 43, True)
    draw_panel(d, 59, "7fdcff", "로비", "튜토리얼 · 상점 · 이동", "snow", draw_scene_lobby)
    draw_panel(d, 108, "82e58b", "야생", "채집 · 농사 · 탐험", "pick", draw_scene_survival)
    draw_panel(d, 157, "f0c35c", "신세계", "신규 탐험 지역", "map", draw_scene_newworld)
    draw_nav_buttons(d)
    return img


def draw_clickmap(base: Image.Image) -> Image.Image:
    img = base.copy()
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)

    # 9x6 chest grid reference. Slots are 18x18 in the GUI coordinate system.
    for x in [5 + i * 18 for i in range(10)]:
        line(d, [(x, 0), (x, H)], rgba("ff3030", 180))
    for y in [14 + i * 18 for i in range(12)]:
        line(d, [(0, y), (W, y)], rgba("ff3030", 180))

    for box, color in (
        ((25, 59, 158, 101), "7fdcff"),
        ((25, 108, 158, 150), "82e58b"),
        ((25, 157, 158, 199), "f0c35c"),
    ):
        rect(d, box, None, rgba(color, 230), 2)
    return Image.alpha_composite(img, overlay)


def save_preview(img: Image.Image, path: Path):
    img.resize((img.width * PREVIEW_SCALE, img.height * PREVIEW_SCALE), Image.Resampling.NEAREST).save(path)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    gui = draw_gui()
    clickmap = draw_clickmap(gui)

    gui_path = OUT / "expedition_learned_gui_183x255.png"
    preview_path = OUT / "expedition_learned_gui_183x255_preview4x.png"
    clickmap_path = OUT / "expedition_learned_gui_183x255_clickmap.png"
    clickmap_preview_path = OUT / "expedition_learned_gui_183x255_clickmap_preview4x.png"

    gui.save(gui_path)
    save_preview(gui, preview_path)
    clickmap.save(clickmap_path)
    save_preview(clickmap, clickmap_preview_path)

    for path in (gui_path, preview_path, clickmap_path, clickmap_preview_path):
        print(path)


if __name__ == "__main__":
    main()
