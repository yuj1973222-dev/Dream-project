from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "generated" / "gui_samples"

GUI_W, GUI_H = 183, 255
SLOT_X, SLOT_Y = 5, 14
SLOT = 18
PANEL_COLS, PANEL_ROWS = 3, 2
PANEL_W, PANEL_H = SLOT * PANEL_COLS, SLOT * PANEL_ROWS


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def blend(a: str, b: str, t: float, alpha: int = 255) -> tuple[int, int, int, int]:
    ar, ag, ab, _ = rgba(a)
    br, bg, bb, _ = rgba(b)
    return (
        int(ar + (br - ar) * t),
        int(ag + (bg - ag) * t),
        int(ab + (bb - ab) * t),
        alpha,
    )


def rect(d: ImageDraw.ImageDraw, box, fill, outline=None, width: int = 1):
    d.rectangle(box, fill=fill, outline=outline, width=width)


def line(d: ImageDraw.ImageDraw, points, fill, width: int = 1):
    d.line(points, fill=fill, width=width)


def draw_button_panel() -> Image.Image:
    img = Image.new("RGBA", (PANEL_W, PANEL_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # BetterRanks-style compact metal shell, scaled up for a 3x2-slot GUI button.
    rect(d, (1, 1, PANEL_W - 2, PANEL_H - 2), rgba("06090d"), rgba("020304"))
    rect(d, (2, 0, PANEL_W - 3, PANEL_H - 1), rgba("141a20"), rgba("41535d"))
    rect(d, (3, 2, PANEL_W - 4, PANEL_H - 3), rgba("1d252c"), rgba("070a0e"))

    for y in range(4, PANEL_H - 4):
        t = (y - 4) / max(1, PANEL_H - 9)
        line(d, [(5, y), (PANEL_W - 6, y)], blend("34424a", "0d1217", t))

    # Polished upper highlight and lower shadow.
    line(d, [(4, 2), (PANEL_W - 5, 2)], rgba("8fa8b6"))
    line(d, [(5, 3), (PANEL_W - 6, 3)], rgba("526c7a"))
    line(d, [(4, PANEL_H - 4), (PANEL_W - 5, PANEL_H - 4)], rgba("020405"))
    line(d, [(5, PANEL_H - 5), (PANEL_W - 6, PANEL_H - 5)], rgba("0a0d10"))

    # Subtle blue steel core line.
    line(d, [(5, 5), (PANEL_W - 6, 5)], rgba("5ad9ff"))
    line(d, [(5, PANEL_H - 7), (PANEL_W - 6, PANEL_H - 7)], rgba("0a4f6a"))

    # Inner button face, intentionally blank for later text/icon work.
    rect(d, (8, 8, PANEL_W - 9, PANEL_H - 9), None, rgba("516673"))
    rect(d, (9, 9, PANEL_W - 10, PANEL_H - 10), None, rgba("10171d"))

    return img


def draw_slot_debug(base: Image.Image) -> Image.Image:
    img = base.copy()
    d = ImageDraw.Draw(img)

    for col in range(9):
        x = SLOT_X + col * SLOT
        line(d, [(x, 0), (x, 126)], rgba("ff0000", 190))
    for row in range(6):
        y = SLOT_Y + row * SLOT
        line(d, [(0, y), (GUI_W, y)], rgba("ff0000", 190))

    x1, y1 = SLOT_X, SLOT_Y
    x2, y2 = x1 + PANEL_W - 1, y1 + PANEL_H - 1
    rect(d, (x1, y1, x2, y2), None, rgba("00ff7a", 230), 2)
    return img


def save_preview(img: Image.Image, path: Path, scale: int):
    img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST).save(path)


def main():
    OUT.mkdir(parents=True, exist_ok=True)

    panel = draw_button_panel()
    gui = Image.new("RGBA", (GUI_W, GUI_H), (0, 0, 0, 0))
    gui.alpha_composite(panel, (SLOT_X, SLOT_Y))
    debug = draw_slot_debug(gui)

    panel_path = OUT / "expedition_button_panel_slots_0_1_2_9_10_11_54x36.png"
    gui_path = OUT / "expedition_button_panel_slots_0_1_2_9_10_11_183x255.png"
    preview_path = OUT / "expedition_button_panel_slots_0_1_2_9_10_11_preview6x.png"
    debug_path = OUT / "expedition_button_panel_slots_0_1_2_9_10_11_slot_debug_preview6x.png"

    panel.save(panel_path)
    gui.save(gui_path)
    save_preview(gui, preview_path, 6)
    save_preview(debug, debug_path, 6)

    for path in [panel_path, gui_path, preview_path, debug_path]:
        print(path)


if __name__ == "__main__":
    main()
