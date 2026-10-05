#!/usr/bin/env python3
"""Original Cobbleworks pixel textures, multipart models and matching publication artwork.

Run with Python + Pillow. Deterministic, no borrowed mod assets or external API required.
"""
import json
import math
import random
import gzip
import struct
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/cobbleworks'
TEX = ASSETS / 'textures/block'
CF = ROOT / 'curseforge'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def texture(name, color, style='metal', animated=False):
    frames = 4 if animated else 1
    im = Image.new('RGB', (32, 32 * frames))
    rng = random.Random(name)
    for f in range(frames):
        tile = Image.new('RGB', (32, 32)); d = ImageDraw.Draw(tile)
        for y in range(32):
            for x in range(32):
                n = rng.randrange(-7, 8)
                c = tuple(max(0, min(255, c + n)) for c in color)
                d.point((x, y), c)
        if style == 'metal':
            d.rectangle((0, 0, 31, 31), outline=(17, 24, 32))
            d.line((1, 1, 30, 1), fill=tuple(min(255, c + 26) for c in color))
            d.line((1, 1, 1, 30), fill=tuple(min(255, c + 20) for c in color))
            d.line((30, 2, 30, 30), fill=tuple(max(0, c - 16) for c in color))
            for x, y in [(3, 3), (28, 3), (3, 28), (28, 28)]:
                d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=(21, 29, 38))
                d.point((x, y), fill=(183, 169, 142))
        elif style == 'vent':
            for y in range(3, 30, 5):
                d.rectangle((3, y, 28, y + 2), fill=(10, 18, 25))
                d.line((4, y + 3, 27, y + 3), fill=(78, 94, 106))
        elif style == 'stone':
            for row in range(4):
                for col in range(4):
                    x, y = col * 8 + (row % 2) * 3 - 3, row * 8
                    shade = rng.randrange(78, 122)
                    d.rounded_rectangle((x, y, x + 7, y + 7), radius=1,
                                        fill=(shade, shade + 6, shade + 10), outline=(42, 53, 62))
                    d.line((x + 1, y + 1, x + 5, y + 1), fill=(146, 155, 160))
        elif style == 'fluid':
            for y in range(32):
                for x in range(32):
                    shimmer = math.sin(x * .45 + (y + f * 8) * .3) * 15 + math.cos(y * .4 - f) * 10
                    d.point((x, y), tuple(max(0, min(255, int(c + shimmer))) for c in color))
            for j in range(5):
                x, y = 3 + j * 6, (5 + j * 7 - f * 4) % 32
                d.rectangle((x, y, x + 1, min(31, y + 3)), fill=tuple(min(255, c + 65) for c in color))
            d.line((1, 0, 1, 31), fill=tuple(min(255, c + 60) for c in color))
        elif style == 'belt':
            for y in range(2, 32, 5): d.line((1, y, 30, y), fill=(82, 100, 108))
        im.paste(tile, (0, f * 32))
    TEX.mkdir(parents=True, exist_ok=True)
    im.save(TEX / f'{name}.png')
    if animated:
        write(TEX / f'{name}.png.mcmeta', {'animation': {'frametime': 4, 'interpolate': True}})


def element(a, b, tex, name):
    return {'name': name, 'from': a, 'to': b,
            'faces': {side: {'texture': '#' + tex} for side in ['north', 'south', 'east', 'west', 'up', 'down']}}


def machine(phase):
    e = []
    def box(a, b, tex, label): e.append(element(a, b, tex, label))
    box([0, 0, 0], [16, 1, 16], 'dark', 'Foot plate')
    box([0, 1, 0], [16, 2, 16], 'copper', 'Copper foundation')
    box([0, 2, 0], [16, 3, 16], 'metal', 'Upper foundation')
    box([1, 3, 2], [15, 7, 15], 'metal', 'Chassis')
    box([1, 3.5, 1.95], [5, 6.5, 2], 'vent', 'Front intake grille')
    box([11, 3.5, 1.95], [15, 6.5, 2], 'vent', 'Front exhaust grille')
    for x, fluid, label in [(1, 'water', 'Water'), (11, 'lava', 'Lava')]:
        box([x, 7, 3], [x + 4, 8, 13], 'copper', label + ' bottom collar')
        box([x, 15, 3], [x + 4, 16, 13], 'copper', label + ' cap')
        box([x + .5, 8, 3.25], [x + 3.5, 15, 12.75], fluid, label + ' glazed reservoir')
        for px in [x, x + 3.5]:
            for pz in [3, 12.5]: box([px, 8, pz], [px + .5, 15, pz + .5], 'metal', label + ' structural rail')
        box([x, 11.5, 3], [x + 4, 12, 3.25], 'copper', label + ' gauge collar')
        box([x + 1, 15.95, 6], [x + 3, 16, 10], 'dark', label + ' hatch inset')
    box([5, 7, 11], [11, 14, 15], 'vent', 'Rear compressor')
    box([5, 13, 11], [11, 14, 15], 'copper', 'Compressor crown')
    box([5, 7, 4], [11, 8, 11], 'dark', 'Reaction bed')
    box([5.5, 8, 5], [10.5, 10.7, 10.5], 'stone', 'Exposed cobblestone core')
    box([5, 8, 4], [5.5, 11, 11], 'copper', 'Reaction guard left')
    box([10.5, 8, 4], [11, 11, 11], 'copper', 'Reaction guard right')
    box([5, 7, 0], [11, 8, 4], 'metal', 'Output chute')
    box([5.5, 8, .5], [10.5, 8.25, 4], 'belt', 'Conveyor surface')
    box([5, 8, 0], [5.5, 9, 4], 'copper', 'Chute rail left')
    box([10.5, 8, 0], [11, 9, 4], 'copper', 'Chute rail right')
    box([6.5, 6, 1.9], [9.5, 6.6, 2], 'indicator', 'Status indicator')
    textures = {n: 'cobbleworks:block/' + n for n in ['dark', 'metal', 'copper', 'vent', 'stone', 'belt']}
    textures['water'] = 'cobbleworks:block/' + ('water_running' if phase == 'running' else 'water' if phase == 'full' else 'water_dim')
    textures['lava'] = 'cobbleworks:block/' + ('lava_running' if phase == 'running' else 'lava' if phase == 'full' else 'lava_dim')
    textures['indicator'] = 'cobbleworks:block/indicator_' + phase
    textures['particle'] = textures['metal']
    return {'parent': 'minecraft:block/block', 'ambientocclusion': True, 'textures': textures,
            'display': {
                'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.6, .6, .6]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.25, .25, .25]},
                'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [.5, .5, .5]}}, 'elements': e}


LANG = {
    'block.cobbleworks.cobble_generator': 'Cobbleworks Generator',
    'cobbleworks.menu.subtitle': 'WATER + LAVA / ENDLESS STONE',
    'cobbleworks.menu.water': 'WATER', 'cobbleworks.menu.lava': 'LAVA', 'cobbleworks.menu.output': 'OUTPUT',
    'cobbleworks.menu.status.waiting': 'Waiting for water and lava buckets',
    'cobbleworks.menu.status.running': 'Generating cobblestone',
    'cobbleworks.menu.status.paused': 'Paused — controls or redstone',
    'cobbleworks.menu.status.full': 'Output full — make room to continue',
    'cobbleworks.menu.power.on': 'Running', 'cobbleworks.menu.power.off': 'Paused',
    'cobbleworks.menu.power_hint': 'Toggle production. Buckets and stored output stay safe.',
    'cobbleworks.menu.status.paused': 'Paused \u2014 needs an active redstone signal',
    'cobbleworks.menu.power_hint': 'Toggle production. Buckets and stored output stay safe. A redstone signal is always required to generate.',
    'cobbleworks.menu.progress_hint': 'Cycle: %s / %s ticks',
    'cobbleworks.menu.footer': 'Reusable buckets. No fuel. No waste.',
    'cobbleworks.configuration.title': 'Cobbleworks Settings',
    'cobbleworks.configuration.generator': 'Generator',
    'cobbleworks.configuration.generator.button': 'Generator',
    'cobbleworks.configuration.generator.tooltip': 'Server-controlled production settings',
    'cobbleworks.configuration.enabled': 'Enable Production',
    'cobbleworks.configuration.enabled.tooltip': 'Disable generation without locking the inventory',
    'cobbleworks.configuration.cycle_ticks': 'Cycle Duration (ticks)',
    'cobbleworks.configuration.cycle_ticks.tooltip': '20 ticks = one second; default 40 ticks',
    'cobbleworks.configuration.output_per_cycle': 'Cobble per Cycle',
    'cobbleworks.configuration.output_per_cycle.tooltip': 'Batch size from 1 to 64; default 1',
    'cobbleworks.configuration.section.cobbleworks.server.toml': 'Cobbleworks (Server)',
    'cobbleworks.configuration.section.cobbleworks.server.toml.title': 'Cobbleworks Server Settings',
}


def font(size, bold=False):
    return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans' + ('-Bold' if bold else '') + '.ttf', size)


def machine_art(size=700):
    """Composite the actual Blockbench MCP viewport render."""
    preview = Image.open(CF / 'machine-preview.png').convert('RGBA')
    preview = preview.crop(preview.getbbox())
    preview.thumbnail((size, size), Image.Resampling.LANCZOS)
    canvas = Image.new('RGBA', (size, size))
    canvas.alpha_composite(preview, ((size - preview.width) // 2, (size - preview.height) // 2))
    return canvas


def artwork():
    CF.mkdir(parents=True, exist_ok=True)
    # High-resolution antialiasing keeps marketing art clean without blurring game textures.
    im = Image.new('RGB', (800,800), (12,21,32)); d = ImageDraw.Draw(im)
    for r in range(330,120,-24): d.ellipse((400-r,400-r,400+r,400+r),outline=(23,43,58),width=2)
    d.arc((94,94,706,706),135,270,fill=(63,193,220),width=8)
    d.arc((94,94,706,706),-45,90,fill=(243,154,74),width=8)
    art=machine_art(670); im.paste(art,(65,60),art)
    d.text((400,683),'COBBLEWORKS',font=font(41,True),anchor='mt',fill=(230,230,221))
    logo=im.resize((400,400),Image.Resampling.LANCZOS)
    logo.save(CF/'logo.png'); logo.save(RES/'logo.png')
    banner=Image.new('RGB',(1600,500),(12,21,32)); d=ImageDraw.Draw(banner)
    for x in range(0,1600,40): d.line((x,0,x,500),fill=(18,32,46))
    for y in range(0,500,40): d.line((0,y,1600,y),fill=(18,32,46))
    for x in range(1600):
        mix=x/1599
        d.line((x,0,x,5),fill=(int(63+(243-63)*mix),int(193+(154-193)*mix),int(220+(74-220)*mix)))
    d.rounded_rectangle((60,67,193,98),radius=14,fill=(27,52,65))
    d.text((126,82),'NEOFORGE',font=font(15,True),anchor='mm',fill=(108,208,222))
    d.text((60,123),'COBBLEWORKS',font=font(82,True),fill=(238,237,228))
    d.text((64,230),'Two buckets. Endless possibilities.',font=font(29),fill=(159,181,193))
    for i,(text,color) in enumerate([('ANIMATED WORKSHOP',(73,200,223)),('HOPPER READY',(210,166,121)),('REDSTONE CONTROL',(248,163,89))]):
        x=64+i*260
        d.ellipse((x,324,x+9,333),fill=color)
        d.text((x+18,316),text,font=font(17,True),fill=(179,197,207))
    d.text((64,430),'MINECRAFT 26.1.2  /  JAVA 25',font=font(19),fill=(105,136,154))
    art=machine_art(610); banner.paste(art,(970,-72),art)
    banner.save(CF/'banner.png')
    # Keep the original MCP viewport render as the source, never overwrite it.


def main():
    for n,c,s in [('metal',(56,72,88),'metal'),('dark',(23,34,45),'metal'),('copper',(177,113,68),'metal'),
                  ('vent',(52,65,78),'vent'),('stone',(98,106,116),'stone'),('belt',(28,39,51),'belt'),
                  ('water',(24,135,175),'fluid'),('lava',(220,95,25),'fluid'),
                  ('water_dim',(24,58,75),'fluid'),('lava_dim',(76,48,31),'fluid')]: texture(n,c,s)
    texture('water_running',(28,154,195),'fluid',True)
    texture('lava_running',(235,119,30),'fluid',True)
    for phase,color in [('waiting',(39,108,143)),('running',(73,195,135)),('paused',(84,98,112)),('full',(239,163,59))]:
        texture('indicator_'+phase,color,'metal')
        write(ASSETS/f'models/block/cobble_generator_{phase}.json',machine(phase))
    write(ASSETS/'blockstates/cobble_generator.json',{'variants': {
        f'facing={facing},phase={phase}': {'model':f'cobbleworks:block/cobble_generator_{phase}','y':rot}
        for facing,rot in [('north',0),('east',90),('south',180),('west',270)]
        for phase in ['waiting','running','paused','full']}})
    write(ASSETS/'items/cobble_generator.json',{'model':{'type':'minecraft:model','model':'cobbleworks:block/cobble_generator_waiting'}})
    write(ASSETS/'models/item/cobble_generator.json',{'parent':'cobbleworks:block/cobble_generator_waiting'})
    write(ASSETS/'lang/en_us.json',LANG)
    write(RES/'data/cobbleworks/recipe/cobble_generator.json',{
        'type':'minecraft:crafting_shaped','category':'redstone','pattern':['IGI','CPC','IRI'],
        'key':{'I':'minecraft:iron_ingot','G':'minecraft:glass','C':'minecraft:cobblestone','P':'minecraft:piston','R':'minecraft:redstone'},
        'result':{'id':'cobbleworks:cobble_generator','count':1}})
    write(RES/'data/cobbleworks/loot_table/blocks/cobble_generator.json',{
        'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'cobbleworks:cobble_generator'}],
                                        'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(RES/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['cobbleworks:cobble_generator']})
    write(RES/'data/cobbleworks/advancement/recipes/redstone/cobble_generator.json',{
        'parent':'minecraft:recipes/root',
        'criteria':{'has_iron':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'minecraft:iron_ingot'}]}},
                    'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'cobbleworks:cobble_generator'}}},
        'requirements':[['has_iron','has_the_recipe']], 'rewards':{'recipes':['cobbleworks:cobble_generator']}})
    # Original empty 5x5x5 GameTest fixture, in vanilla compressed NBT format.
    def nbt_name(name):
        b=name.encode(); return struct.pack('>H', len(b))+b
    data=b'\x0a\x00\x00'
    data+=b'\x03'+nbt_name('DataVersion')+struct.pack('>i',4790)
    data+=b'\x09'+nbt_name('size')+b'\x03'+struct.pack('>iiii',3,5,5,5)
    data+=b'\x09'+nbt_name('palette')+b'\x0a'+struct.pack('>i',1)+b'\x08'+nbt_name('Name')+nbt_name('minecraft:air')+b'\x00'
    for name in ['blocks','entities']: data+=b'\x09'+nbt_name(name)+b'\x0a'+struct.pack('>i',0)
    data+=b'\x00'
    fixture=RES/'data/cobbleworks/structure/empty.nbt'
    fixture.parent.mkdir(parents=True,exist_ok=True); fixture.write_bytes(gzip.compress(data,mtime=0))
    artwork()
    from export_blockbench_states import export
    export()
    print('Generated 16 original textures, 4 detailed models, 16 facing/state variants, recipes/loot/lang and icon/banner.')


if __name__ == '__main__': main()
