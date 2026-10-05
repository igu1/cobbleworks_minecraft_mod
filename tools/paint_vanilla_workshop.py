"""Paint original 32-pixel material tiles for the redesigned workshop atlas.

Each tile is scaled with nearest-neighbour sampling to preserve chunky pixels.
The tile positions retain the bucket/phase contract of export_blockbench_states.
"""
from pathlib import Path
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'art/blockbench/workshop_atlas.png'


def paint():
    rng = random.Random(74)
    atlas = Image.new('RGBA', (256, 256))
    palettes = [
        ['#686b63', '#797c72', '#85887e', '#61665d'],
        ['#b66d46', '#ce8658', '#dd9a68', '#a9603d'],
        ['#939b92', '#b6bcb0', '#c8cabc', '#818a82'],
        ['#8f653b', '#a77946', '#b68850', '#78522f'],
        ['#777c70', '#969b88', '#adb09b', '#60675f'],
        ['#afa68a', '#c6bc9e', '#d4cbb0', '#918b74'],
        ['#255cba', '#307bd3', '#4193e0', '#6ab5e9'],
        ['#e66516', '#fb9126', '#ffc14a', '#be4313'],
        ['#283d4c', '#344c59', '#43626b', '#1b303c'],
        ['#473631', '#594238', '#75513d', '#352923'],
        ['#457e3c', '#64a34e', '#91c75a', '#335a2f'],
        ['#9e6b28', '#d39a3a', '#efbd59', '#704f27'],
        ['#633c30', '#8b5140', '#b87554', '#4a3029'],
        ['#305662', '#477785', '#6e9da3', '#263e4b'],
        ['#2e332e', '#484e44', '#555d51', '#242923'],
        ['#262c27', '#323a32', '#404b3e', '#1d241e'],
    ]
    for index, palette in enumerate(palettes):
        tile = Image.new('RGBA', (16, 16), palette[0])
        d = ImageDraw.Draw(tile)
        for _ in range(48):
            x, y = rng.randrange(16), rng.randrange(16)
            d.rectangle((x,y,min(15,x+rng.randrange(1,4)),min(15,y+rng.randrange(1,3))), fill=rng.choice(palette))
        if index in [0,4]:
            # Staggered blocks with irregular hand-cut joints, not fine noise.
            for y, seams in [(0,[3,11]),(5,[7,15]),(10,[2,10]),(15,[6,14])]:
                d.line((0,y,15,y),fill='#454d43')
                if y<15: d.line((0,y+1,15,y+1),fill=palette[2])
                for x in seams: d.line((x,y,x,y+4),fill='#454d43')
        elif index in [1,2]:
            d.rectangle((0,0,15,15),outline=palette[3])
            d.line((1,1,14,1),fill=palette[2])
            d.line((1,1,1,14),fill=palette[1])
            for x,y in [(2,2),(12,2),(2,12),(12,12)]:
                d.rectangle((x,y,x+1,y+1),fill=palette[2])
        elif index in [3,5]:
            for y in [0,5,10,15]: d.line((0,y,15,y),fill=palette[3])
            d.line((3,2,10,2),fill=palette[2])
            d.line((6,7,14,7),fill=palette[3])
        elif index in [6,7]:
            for y in [3,8,13]:
                for x in range(0,16,5):
                    d.line((x,y,min(15,x+3),y),fill=palette[2])
        elif index in [8,9]:
            d.rectangle((0,0,15,15),outline=palette[3])
            d.line((2,2,2,12),fill=palette[2])
        elif index in [10,11,12,13]:
            d.rectangle((0,0,15,15),fill='#30382f')
            d.rectangle((3,3,12,12),fill=palette[3])
            d.rectangle((4,4,11,11),fill=palette[1])
            d.rectangle((5,5,10,7),fill=palette[2])
        elif index == 14:
            for y in [2,6,10,14]:
                d.rectangle((2,y,13,y+1),fill='#131d18')
                if y<14: d.line((2,y+2,13,y+2),fill=palette[2])
        # Preserve the broad readable forms, adding restrained 32px detailing.
        tile = tile.resize((32,32), Image.Resampling.NEAREST)
        d = ImageDraw.Draw(tile)
        for _ in range(36):
            x,y = rng.randrange(2,30),rng.randrange(2,30)
            base = tile.getpixel((x,y))
            delta = rng.choice([-7,7])
            color = tuple(max(0,min(255,c+delta)) for c in base[:3])+(255,)
            d.point((x,y),fill=color)
        if index in [0,4]:
            # Medium-sized stone blocks: enough joints to read at gameplay
            # distance, with a single-pixel bevel rather than noisy cracks.
            d.rectangle((0,0,31,31),fill=palette[0])
            for row,y in enumerate(range(0,32,8)):
                for x in range(-8 if row%2 else 0,32,12):
                    color = palette[rng.randrange(1,3)]
                    d.rectangle((x+1,y+1,x+11,min(31,y+7)),fill=color)
                    d.line((x+1,y+1,x+10,y+1),fill=palette[2])
                    d.line((x,y,x,y+7),fill=palette[3])
                    d.line((x,y,min(31,x+12),y),fill='#4f574b')
                    if x>=0:
                        d.line((x+3,y+5,x+6,y+5),fill=palette[0])
        elif index in [1,2]:
            d.rectangle((0,0,31,31),outline=palette[3])
            d.line((1,1,30,1),fill=palette[2])
            for x,y in [(4,4),(26,4),(4,26),(26,26)]:
                d.rectangle((x-1,y-1,x+2,y+2),fill=palette[3])
                d.rectangle((x,y,x+1,y+1),fill=palette[2])
            for y in [8,24]:
                d.line((2,y,29,y),fill=palette[3])
                d.line((2,y+1,29,y+1),fill=palette[1])
        elif index in [3,5]:
            for y in [3,13,23]:
                d.line((4,y,18,y),fill=palette[1])
                d.line((10,y+2,28,y+2),fill=palette[3])
        elif index in [6,7]:
            for x,y in [(7,9),(17,17),(5,24),(23,5)]:
                d.line((x,y,x+4,y),fill=palette[2])
                d.point((x+5,y+1),fill=palette[1])
        elif index == 14:
            d.rectangle((5,7,26,24),fill='#252e26')
            for y in [9,13,17,21]:
                d.rectangle((7,y,24,y+1),fill='#121c16')
                d.line((7,y+2,24,y+2),fill=palette[2])
        atlas.paste(tile.resize((64,64),Image.Resampling.NEAREST),((index%4)*64,(index//4)*64))
    atlas.save(TARGET)
    print(TARGET)


if __name__ == '__main__':
    paint()
