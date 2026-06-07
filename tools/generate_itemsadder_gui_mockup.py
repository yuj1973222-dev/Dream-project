from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path
import math
import random


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "previews" / "expedition_itemsadder_gui_mockup.png"

W, H = 1600, 900
GUI_W, GUI_H = 900, 760
GUI_X, GUI_Y = (W - GUI_W) // 2, 45


def font(size, bold=False):
    candidates = [
        "C:/Windows/Fonts/NotoSansKR-VF.ttf",
        "C:/Windows/Fonts/malgunbd.ttf" if bold else "C:/Windows/Fonts/malgun.ttf",
        "C:/Windows/Fonts/NanumGothic.ttf",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


FONT_TITLE = font(42, True)
FONT_SUB = font(24, True)
FONT_BIG = font(50, True)
FONT_DESC = font(25, True)


def rect(draw, xy, fill, outline=None, width=1):
    draw.rectangle(xy, fill=fill, outline=outline, width=width)


def text_center(draw, xy, text, fill, fnt, shadow=True):
    x1, y1, x2, y2 = xy
    box = draw.textbbox((0, 0), text, font=fnt)
    tw, th = box[2] - box[0], box[3] - box[1]
    x = x1 + (x2 - x1 - tw) // 2
    y = y1 + (y2 - y1 - th) // 2 - 2
    if shadow:
        draw.text((x + 3, y + 3), text, font=fnt, fill=(0, 0, 0, 170))
    draw.text((x, y), text, font=fnt, fill=fill)


def draw_pixel_forest(draw):
    # Dark in-game-like forest background, intentionally subdued.
    sky = [(10, 24, 28), (17, 35, 38), (15, 28, 27)]
    for y in range(H):
        t = y / H
        r = int(sky[0][0] * (1 - t) + sky[2][0] * t)
        g = int(sky[0][1] * (1 - t) + sky[2][1] * t)
        b = int(sky[0][2] * (1 - t) + sky[2][2] * t)
        draw.line((0, y, W, y), fill=(r, g, b))

    random.seed(7)
    for _ in range(34):
        x = random.randint(-80, W)
        trunk_w = random.randint(18, 42)
        trunk_h = random.randint(350, 780)
        y = H - trunk_h + random.randint(-80, 120)
        rect(draw, (x, y, x + trunk_w, H), (28, 18, 12))
        for _ in range(random.randint(4, 8)):
            lx = x + random.randint(-90, 70)
            ly = y + random.randint(-80, 260)
            lw = random.randint(80, 190)
            lh = random.randint(45, 115)
            rect(draw, (lx, ly, lx + lw, ly + lh), random.choice([(18, 45, 28), (22, 58, 35), (14, 37, 26)]))

    # Dark overlay like Minecraft inventory background dim.
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 95))
    return overlay


def draw_slot_grid(draw, x, y):
    slot = 54
    gap = 5
    for row in range(3):
        for col in range(9):
            sx = x + col * (slot + gap)
            sy = y + row * (slot + gap)
            rect(draw, (sx, sy, sx + slot, sy + slot), (38, 38, 35), (112, 112, 103), 3)
            rect(draw, (sx + 4, sy + 4, sx + slot - 4, sy + slot - 4), (27, 27, 25), (65, 65, 60), 1)


def draw_badge(draw, cx, cy, theme):
    rect(draw, (cx - 48, cy - 48, cx + 48, cy + 48), (33, 36, 34), (168, 145, 96), 4)
    rect(draw, (cx - 38, cy - 38, cx + 38, cy + 38), theme["badge"], (45, 45, 42), 3)
    icon = theme["icon"]
    if icon == "snow":
        for a in range(0, 180, 45):
            rad = math.radians(a)
            draw.line((cx - math.cos(rad) * 26, cy - math.sin(rad) * 26,
                       cx + math.cos(rad) * 26, cy + math.sin(rad) * 26), fill=(230, 245, 255), width=5)
        draw.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), fill=(230, 245, 255))
    elif icon == "pick":
        draw.line((cx - 22, cy + 24, cx + 18, cy - 18), fill=(218, 180, 92), width=8)
        draw.line((cx - 8, cy - 12, cx + 30, cy - 20), fill=(220, 228, 210), width=8)
        draw.line((cx - 10, cy - 8, cx + 5, cy - 28), fill=(220, 228, 210), width=8)
        draw.line((cx + 4, cy + 18, cx + 26, cy + 32), fill=(210, 190, 75), width=4)
    else:
        draw.line((cx - 20, cy + 26, cx + 24, cy - 18), fill=(230, 214, 150), width=6)
        draw.polygon([(cx + 25, cy - 22), (cx + 8, cy - 16), (cx + 18, cy - 5)], fill=(238, 238, 218))
        draw.arc((cx - 30, cy - 30, cx + 30, cy + 30), 15, 340, fill=(230, 214, 150), width=4)


def draw_button_scene(draw, xy, theme):
    x1, y1, x2, y2 = xy
    # Pixel scene background.
    rect(draw, xy, theme["bg"], None)
    for i in range(16):
        px = x1 + i * ((x2 - x1) // 15)
        shade = int(30 + i * 3)
        draw.line((px, y1, px, y2), fill=tuple(min(255, c + shade // 5) for c in theme["bg"]), width=16)
    # Distant hills / trees / structures.
    for i in range(7):
        bx = x1 + 250 + i * 82
        by = y2 - 26 - (i % 3) * 13
        if theme["name"] == "lobby":
            draw.polygon([(bx - 50, by), (bx, by - 48), (bx + 55, by)], fill=(148, 171, 184))
            draw.polygon([(bx - 40, by), (bx, by - 36), (bx + 44, by)], fill=(235, 242, 245))
        elif theme["name"] == "wild":
            rect(draw, (bx - 26, by - 54, bx + 24, by), (35, 74, 39))
            rect(draw, (bx - 6, by - 26, bx + 5, by + 8), (64, 42, 25))
        else:
            draw.polygon([(bx - 40, by), (bx + 10, by - 55), (bx + 62, by)], fill=(45, 85, 83))
            rect(draw, (bx - 20, by - 35, bx + 20, by), (95, 89, 67))

    if theme["name"] == "lobby":
        rect(draw, (x1 + 430, y1 + 38, x1 + 575, y2 - 24), (81, 61, 42), (230, 230, 220), 3)
        draw.polygon([(x1 + 410, y1 + 42), (x1 + 505, y1 + 2), (x1 + 595, y1 + 42)], fill=(226, 236, 242))
        rect(draw, (x1 + 466, y1 + 78, x1 + 494, y1 + 105), (238, 191, 78))
        rect(draw, (x1 + 516, y1 + 78, x1 + 544, y1 + 105), (238, 191, 78))
    elif theme["name"] == "wild":
        rect(draw, (x1 + 392, y1 + 64, x1 + 530, y2 - 22), (54, 43, 29), (92, 92, 74), 3)
        draw.polygon([(x1 + 370, y1 + 67), (x1 + 465, y1 + 20), (x1 + 550, y1 + 67)], fill=(53, 58, 47))
        for k in range(5):
            rect(draw, (x1 + 600 + k * 26, y2 - 45, x1 + 613 + k * 26, y2 - 18), (213, 171, 62))
    else:
        for k in range(4):
            rect(draw, (x1 + 405 + k * 34, y1 + 46 - k * 6, x1 + 426 + k * 34, y2 - 28), (109, 101, 75))
        draw.polygon([(x1 + 600, y2 - 30), (x1 + 720, y2 - 30), (x1 + 676, y2 - 64)], fill=(48, 40, 32))
        draw.line((x1 + 676, y2 - 64, x1 + 676, y1 + 32), fill=(188, 170, 105), width=4)


def draw_world_button(draw, x, y, w, h, title, subtitle, theme):
    shadow = (x + 8, y + 8, x + w + 8, y + h + 8)
    rect(draw, shadow, (8, 8, 8, 110))
    rect(draw, (x, y, x + w, y + h), (26, 28, 27), theme["outline"], 5)
    rect(draw, (x + 8, y + 8, x + w - 8, y + h - 8), (17, 18, 18), theme["inner"], 3)
    scene_box = (x + 170, y + 15, x + w - 16, y + h - 15)
    draw_button_scene(draw, scene_box, theme)
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rectangle(scene_box, fill=(0, 0, 0, 60))
    base.alpha_composite(overlay)
    draw_badge(draw, x + 82, y + h // 2, theme)
    draw.text((x + 160, y + 30), title, font=FONT_BIG, fill=theme["title"], stroke_width=3, stroke_fill=(0, 0, 0))
    draw.text((x + 162, y + 92), subtitle, font=FONT_DESC, fill=(242, 238, 220), stroke_width=2, stroke_fill=(0, 0, 0))


base = Image.new("RGBA", (W, H), (0, 0, 0, 255))
draw = ImageDraw.Draw(base)
overlay = draw_pixel_forest(draw)
base.alpha_composite(overlay)
draw = ImageDraw.Draw(base)

# Main frame, close to a large chest GUI texture.
rect(draw, (GUI_X, GUI_Y, GUI_X + GUI_W, GUI_Y + GUI_H), (34, 34, 32), (142, 137, 124), 5)
rect(draw, (GUI_X + 16, GUI_Y + 16, GUI_X + GUI_W - 16, GUI_Y + GUI_H - 16), (21, 22, 22), (66, 65, 61), 3)

# Side rails.
for sx in (GUI_X - 38, GUI_X + GUI_W + 6):
    rect(draw, (sx, GUI_Y + 38, sx + 32, GUI_Y + GUI_H - 38), (73, 48, 31), (148, 134, 110), 4)
    for yy in (GUI_Y + 34, GUI_Y + GUI_H - 82):
        rect(draw, (sx - 7, yy, sx + 39, yy + 42), (54, 55, 53), (170, 165, 145), 3)

# Header.
header = (GUI_X + 145, GUI_Y + 28, GUI_X + GUI_W - 145, GUI_Y + 118)
rect(draw, header, (31, 30, 29), (142, 137, 124), 4)
rect(draw, (header[0] + 8, header[1] + 8, header[2] - 8, header[3] - 8), (13, 13, 13), (71, 70, 66), 3)
text_center(draw, (header[0], header[1] + 6, header[2], header[1] + 58), "EXPEDITION MENU", (246, 244, 235), FONT_TITLE)
text_center(draw, (header[0], header[1] + 56, header[2], header[3] - 4), "◆ 서버 이동 ◆", (218, 214, 199), FONT_SUB)

# Banners.
for bx in (GUI_X + 60, GUI_X + GUI_W - 130):
    rect(draw, (bx, GUI_Y + 20, bx + 70, GUI_Y + 118), (18, 58, 94), (105, 121, 132), 4)
    draw.polygon([(bx, GUI_Y + 118), (bx + 35, GUI_Y + 145), (bx + 70, GUI_Y + 118)], fill=(18, 58, 94))
    draw_badge(draw, bx + 35, GUI_Y + 72, {"badge": (31, 65, 92), "icon": "compass"})

themes = [
    {"name": "lobby", "bg": (45, 76, 94), "outline": (128, 184, 231), "inner": (186, 221, 248), "badge": (44, 83, 125), "icon": "snow", "title": (135, 214, 255)},
    {"name": "wild", "bg": (31, 78, 38), "outline": (97, 188, 94), "inner": (133, 205, 121), "badge": (56, 93, 44), "icon": "pick", "title": (133, 235, 133)},
    {"name": "new", "bg": (28, 80, 80), "outline": (219, 164, 51), "inner": (234, 183, 70), "badge": (70, 61, 36), "icon": "compass", "title": (255, 198, 84)},
]

button_x = GUI_X + 66
button_y = GUI_Y + 145
button_w = GUI_W - 132
button_h = 118
draw_world_button(draw, button_x, button_y, button_w, button_h, "로비", "튜토리얼 · 상점 · 이동", themes[0])
draw_world_button(draw, button_x, button_y + 132, button_w, button_h, "야생", "광질 · 농사 · 탐험", themes[1])
draw_world_button(draw, button_x, button_y + 264, button_w, button_h, "신세계", "신규 탐험 지역", themes[2])

# Inventory grid panel.
grid_x = GUI_X + 150
grid_y = GUI_Y + 610
rect(draw, (GUI_X + 100, GUI_Y + 572, GUI_X + GUI_W - 100, GUI_Y + GUI_H - 34), (42, 42, 39), (118, 114, 104), 4)
draw_slot_grid(draw, grid_x, grid_y)

# Navigation arrows.
for side, ax in (("left", GUI_X + 22), ("right", GUI_X + GUI_W - 82)):
    ay = GUI_Y + 610
    rect(draw, (ax, ay, ax + 60, ay + 60), (55, 55, 52), (164, 158, 140), 4)
    if side == "left":
        draw.polygon([(ax + 39, ay + 16), (ax + 17, ay + 30), (ax + 39, ay + 44)], fill=(228, 226, 213))
    else:
        draw.polygon([(ax + 21, ay + 16), (ax + 43, ay + 30), (ax + 21, ay + 44)], fill=(228, 226, 213))

# A single chest item placeholder in first grid slot to show real slot area.
rect(draw, (grid_x + 8, grid_y + 8, grid_x + 42, grid_y + 42), (133, 83, 29), (221, 161, 71), 3)
rect(draw, (grid_x + 10, grid_y + 10, grid_x + 40, grid_y + 24), (169, 108, 38), None)
draw.line((grid_x + 8, grid_y + 25, grid_x + 42, grid_y + 25), fill=(60, 42, 24), width=3)

OUT.parent.mkdir(parents=True, exist_ok=True)
base = base.convert("RGB")
base.save(OUT, quality=95)
print(OUT)
