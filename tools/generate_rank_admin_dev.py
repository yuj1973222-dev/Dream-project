from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_DIR = ROOT / "betterranks_edit" / "contents" / "betterranks" / "textures"
PREVIEW_DIR = ROOT / "assets" / "generated" / "rank_previews"


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


PIXEL_FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "I": ["111", "010", "010", "010", "010", "010", "111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "V": ["10001", "10001", "10001", "10001", "01010", "01010", "00100"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
}


def pixel_text_size(text: str) -> tuple[int, int]:
    width = 0
    for i, ch in enumerate(text):
        width += len(PIXEL_FONT[ch][0])
        if i < len(text) - 1:
            width += 1
    return width, 7


def draw_pixel_text(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill, shade):
    x, y = xy
    cursor = x
    for ch in text:
        glyph = PIXEL_FONT[ch]
        # shadow first
        for row, line_bits in enumerate(glyph):
            for col, bit in enumerate(line_bits):
                if bit == "1":
                    draw.point((cursor + col + 1, y + row + 1), fill=shade)
        for row, line_bits in enumerate(glyph):
            for col, bit in enumerate(line_bits):
                if bit == "1":
                    draw.point((cursor + col, y + row), fill=fill)
        cursor += len(glyph[0]) + 1


def centered_text(draw: ImageDraw.ImageDraw, box, text: str, fill):
    x1, y1, x2, y2 = box
    tw, th = pixel_text_size(text)
    x = x1 + ((x2 - x1 + 1) - tw) // 2
    y = y1 + ((y2 - y1 + 1) - th) // 2
    draw_pixel_text(draw, (x, y), text, fill, rgba("05070a", 230))


def badge(width: int, label: str, theme: str) -> Image.Image:
    img = Image.new("RGBA", (width, 9), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    if theme == "admin":
        border = rgba("3a0c12")
        accent = rgba("d94652")
        text = rgba("fff0f0")
        shine = rgba("ffd6d6")
    elif theme == "dev":
        border = rgba("0a2738")
        accent = rgba("42d8ff")
        text = rgba("eafcff")
        shine = rgba("c8f8ff")
    else:
        border = rgba("30363c")
        accent = rgba("9ea9b3")
        text = rgba("f1f4f6")
        shine = rgba("ffffff")

    # Metal shell, matching the compact BetterRanks rank style.
    d.rectangle((0, 1, width - 1, 7), fill=rgba("090d11"))
    d.rectangle((1, 0, width - 2, 8), fill=rgba("151a1f"))
    d.rectangle((1, 1, width - 2, 7), outline=border)
    d.line((2, 1, width - 3, 1), fill=rgba("59636d"))
    d.line((2, 7, width - 3, 7), fill=rgba("05070a"))

    # Left bevel and accent shine.
    d.rectangle((2, 2, 5, 6), fill=rgba("222b34"))
    d.point((2, 2), fill=shine)
    d.line((5, 2, width - 6, 2), fill=rgba("2c333b"))
    d.line((5, 3, width - 6, 3), fill=rgba("22282f"))
    d.line((5, 5, width - 6, 5), fill=rgba("11161c"))
    d.line((5, 6, width - 6, 6), fill=rgba("0c1015"))

    # Thin colored core line.
    d.line((2, 0, width - 3, 0), fill=accent)
    d.point((width - 2, 1), fill=accent)
    d.point((1, 7), fill=accent)

    centered_text(d, (1, 0, width - 2, 8), label, text)
    return img


def save(name: str, img: Image.Image):
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    texture_path = TEXTURE_DIR / f"{name}.png"
    preview_path = PREVIEW_DIR / f"{name}_preview12x.png"

    img.save(texture_path)
    img.resize((img.width * 12, img.height * 12), Image.Resampling.NEAREST).save(preview_path)
    print(texture_path)
    print(preview_path)


def main():
    save("rank_admin_metal", badge(34, "ADMIN", "admin"))
    save("rank_dev_metal", badge(24, "DEV", "dev"))
    save("player", badge(41, "PLAYER", "player"))


if __name__ == "__main__":
    main()
