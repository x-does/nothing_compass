#!/usr/bin/env python3
"""Generate original Play icon and feature artwork; never substitute for real screenshots."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

out = Path(__file__).resolve().parents[1] / 'store' / 'artwork'
out.mkdir(parents=True, exist_ok=True)
bg, accent, white, muted = '#101113', '#e9ff89', '#f4f2ec', '#a9adb5'
font = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'

icon = Image.new('RGB', (512, 512), bg)
d = ImageDraw.Draw(icon)
d.polygon([(256, 110), (367, 337), (256, 297), (145, 337)], fill=accent)
d.rounded_rectangle((239, 329, 273, 402), radius=8, fill=accent)
icon.save(out / 'play-icon-512.png')

feature = Image.new('RGB', (1024, 500), bg)
d = ImageDraw.Draw(feature)
d.text((60, 88), 'GLYPH NORTH', font=ImageFont.truetype(font, 19), fill=accent)
d.text((56, 145), 'A little time.', font=ImageFont.truetype(font, 51), fill=white)
d.text((56, 212), 'A little direction.', font=ImageFont.truetype(font, 51), fill=white)
d.text((60, 316), 'Shake for north. Flip back to time.', font=ImageFont.truetype(font, 22), fill=muted)
# Original dot-matrix arrow illustration.
for y in range(13):
    for x in range(13):
        dx, dy = x - 6, y - 6
        lit = (abs(dx) <= .65 and -3.8 <= dy <= 4) or (-5 <= dy <= -1.5 and abs(dx) <= (dy + 5) * .9 + .35)
        cx, cy = 690 + x * 20, 112 + y * 20
        d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=accent if lit else '#303339')
feature.save(out / 'feature-graphic-1024x500.png')
print(out)
