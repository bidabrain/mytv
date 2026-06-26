#!/usr/bin/env python3
"""从 icon.png 抠出红色圆角 CCTV 标志，生成 Android 各密度图标资源。

- 自适应前景：抠出的白色标志（透明底）
- 自适应背景：取自图标的红色（纯色）
- 传统方形/圆形图标：红色圆角方块（四角透明）/ 圆形遮罩
- TV banner：红底居中白标志
"""
import os
from collections import deque
from PIL import Image, ImageDraw, ImageChops, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "icon.png")
RES = os.path.join(ROOT, "app", "src", "main", "res")

img = Image.open(SRC).convert("RGB")
W, H = img.size
px = img.load()

WHITE_THR = 185   # 方块/四角检测：判为"白底"
LOGO_THR = 240    # 标志提取：仅纯白笔画，滤掉红方块内边缘抗锯齿杂圈

def is_white(rgb):
    r, g, b = rgb
    return r >= WHITE_THR and g >= WHITE_THR and b >= WHITE_THR

# 1) 白像素掩码
white = [[is_white(px[x, y]) for x in range(W)] for y in range(H)]

# 2) 从四角洪水填充，标出"外部白色四角"
outside = [[False] * W for _ in range(H)]
dq = deque()
for sx, sy in [(0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)]:
    if white[sy][sx] and not outside[sy][sx]:
        outside[sy][sx] = True
        dq.append((sx, sy))
while dq:
    x, y = dq.popleft()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < W and 0 <= ny < H and white[ny][nx] and not outside[ny][nx]:
            outside[ny][nx] = True
            dq.append((nx, ny))

# 3) logo 白 = 白且非外部四角；红方块区域 = 非外部四角
logo_white = Image.new("L", (W, H), 0)   # 标志白色 alpha
square = Image.new("L", (W, H), 0)        # 圆角方块区域 alpha
lw = logo_white.load()
sq = square.load()
red_pixels = []
for y in range(H):
    row_out = outside[y]
    row_white = white[y]
    for x in range(W):
        if not row_out[x]:
            sq[x, y] = 255
            r, g, b = px[x, y]
            if r >= LOGO_THR and g >= LOGO_THR and b >= LOGO_THR:
                lw[x, y] = 255
            elif not row_white[x]:
                red_pixels.append(px[x, y])

# 4) 红色 = 红方块内非白像素的中位数
red_pixels.sort(key=lambda c: (c[0], c[1], c[2]))
red = red_pixels[len(red_pixels) // 2]
RED_HEX = "#%02X%02X%02X" % red
print("sampled red:", red, RED_HEX)
print("logo white coverage:", sum(sum(1 for x in range(W) if lw[x, y]) for y in range(H)))

# 抠出的标志（白色 + alpha），裁到外接框
white_rgba = Image.new("RGBA", (W, H), (255, 255, 255, 0))
white_rgba.putalpha(logo_white)
bbox = logo_white.getbbox()
logo = white_rgba.crop(bbox)

# 红色圆角方块（四角透明）：原图 RGB + square alpha
square_rgba = img.convert("RGBA")
square_rgba.putalpha(square)
sq_bbox = square.getbbox()
square_logo = square_rgba.crop(sq_bbox)

# ---- 输出辅助 ----
def ensure(d):
    os.makedirs(d, exist_ok=True)

def paste_centered(canvas_size, sprite, scale):
    """把 sprite 等比缩放到 canvas 的 scale 比例，居中贴到透明画布。"""
    cw = ch = canvas_size
    sw, sh = sprite.size
    target = int(canvas_size * scale)
    ratio = min(target / sw, target / sh)
    nw, nh = max(1, int(sw * ratio)), max(1, int(sh * ratio))
    s = sprite.resize((nw, nh), Image.LANCZOS)
    canvas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    canvas.paste(s, ((cw - nw) // 2, (ch - nh) // 2), s)
    return canvas

def circle_mask(im):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, im.size[0] - 1, im.size[1] - 1), fill=255)
    out = im.copy()
    a = out.getchannel("A")
    from PIL import ImageChops
    out.putalpha(ImageChops.multiply(a, m))
    return out

# 密度 -> 倍率（基准 dpi 160）
DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# 1) 自适应前景：108dp，标志占内部 ~62%（满足 66dp 安全区）
for name, m in DENS.items():
    size = round(108 * m)
    fg = paste_centered(size, logo, 0.62)
    d = os.path.join(RES, f"mipmap-{name}")
    ensure(d)
    fg.save(os.path.join(d, "ic_launcher_foreground.png"))

# 2) 传统方形/圆形图标：48dp 满铺
for name, m in DENS.items():
    size = round(48 * m)
    sqr = square_logo.resize((size, size), Image.LANCZOS)
    d = os.path.join(RES, f"mipmap-{name}")
    ensure(d)
    sqr.save(os.path.join(d, "ic_launcher.png"))
    circle_mask(sqr).save(os.path.join(d, "ic_launcher_round.png"))

# 3) TV banner：320x180（xhdpi 基准），红底居中标志
for name, m in DENS.items():
    bw, bh = round(320 * m), round(180 * m)
    banner = Image.new("RGBA", (bw, bh), red + (255,))
    target_h = int(bh * 0.7)
    ratio = target_h / logo.size[1]
    nw, nh = int(logo.size[0] * ratio), target_h
    s = logo.resize((nw, nh), Image.LANCZOS)
    banner.paste(s, ((bw - nw) // 2, (bh - nh) // 2), s)
    d = os.path.join(RES, f"drawable-{name}")
    ensure(d)
    banner.convert("RGB").save(os.path.join(d, "banner.png"))

print("done. RED_HEX=", RED_HEX)
