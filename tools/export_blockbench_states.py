#!/usr/bin/env python3
"""Package Blockbench-authored geometry/paint into bucket-aware Minecraft states.

Does not design geometry or paint textures: the .bbmodel and atlas in Blockbench
are the design sources. Animated frames scroll the painted fluid tiles only.
"""
import copy
import json
from pathlib import Path
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/cobbleworks'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def export():
    source = json.loads((ROOT / 'art/blockbench/cobble_generator.json').read_text())
    atlas = Image.open(ASSETS / 'textures/block/cobbleworks_atlas.png').convert('RGBA')
    for fluid, tile in [('water', 6), ('lava', 7)]:
        x, y = tile % 4 * 32, tile // 4 * 32
        painted = atlas.crop((x, y, x + 32, y + 32))
        frames = Image.new('RGBA', (32, 128))
        for frame in range(4):
            frames.paste(ImageChops.offset(painted, 0, frame * 2), (0, frame * 32))
        path = ASSETS / f'textures/block/bb_{fluid}_flow.png'
        frames.save(path)
        write(Path(str(path) + '.mcmeta'), {'animation': {'frametime': 5, 'interpolate': True}})
    variants = {}
    for phase in ['waiting', 'running', 'paused', 'full']:
        for water in [False, True]:
            for lava in [False, True]:
                model = copy.deepcopy(source)
                model.pop('groups', None)
                model['parent'] = 'minecraft:block/block'
                model['textures']['particle'] = 'cobbleworks:block/cobbleworks_atlas'
                model['display'] = {
                    'gui': {'rotation': [30, 225, 0], 'scale': [.6, .6, .6]},
                    'ground': {'translation': [0, 3, 0], 'scale': [.25, .25, .25]},
                    'fixed': {'rotation': [0, 180, 0], 'scale': [.5, .5, .5]},
                }
                for e in model['elements']:
                    if e['name'].endswith('sight glass'):
                        fluid = 'water' if e['name'].startswith('Water') else 'lava'
                        filled = water if fluid == 'water' else lava
                        tile = (6 if fluid == 'water' else 7) if filled else (8 if fluid == 'water' else 9)
                        for face in e['faces'].values():
                            if filled and phase == 'running':
                                model['textures'][fluid] = f'cobbleworks:block/bb_{fluid}_flow'
                                face.update(texture='#' + fluid, uv=[0, 0, 16, 16])
                            else:
                                u, v = tile % 4 * 4, tile // 4 * 4
                                face.update(texture='#0', uv=[u, v, u + 4, v + 4])
                    elif e['name'] == 'Status indicator':
                        tile = {'waiting': 13, 'running': 10, 'paused': 12, 'full': 11}[phase]
                        u, v = tile % 4 * 4, tile // 4 * 4
                        for face in e['faces'].values():
                            face['uv'] = [u, v, u + 4, v + 4]
                name = f'cobble_generator_{phase}_w{int(water)}_l{int(lava)}'
                write(ASSETS / f'models/block/{name}.json', model)
                for facing, angle in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
                    key = f'facing={facing},lava={str(lava).lower()},phase={phase},water={str(water).lower()}'
                    variants[key] = {'model': f'cobbleworks:block/{name}', 'y': angle}
    write(ASSETS / 'blockstates/cobble_generator.json', {'variants': variants})
    write(ASSETS / 'items/cobble_generator.json', {'model': {
        'type': 'minecraft:model', 'model': 'cobbleworks:block/cobble_generator_waiting_w0_l0'}})
    write(ASSETS / 'models/item/cobble_generator.json', {'parent': 'cobbleworks:block/cobble_generator_waiting_w0_l0'})
    elements = source['elements']
    overlaps = []
    for i, a in enumerate(elements):
        for b in elements[i + 1:]:
            if all(min(a['to'][k], b['to'][k]) - max(a['from'][k], b['from'][k]) > 1e-5 for k in range(3)):
                overlaps.append((a['name'], b['name']))
    assert not overlaps, f'Intersecting model cuboids: {overlaps}'
    assert all(0 <= v <= 16 for e in elements for v in e['from'] + e['to'])
    assert len(variants) == 64
    print(f'Blockbench export: {len(elements)} cuboids, zero volume intersections, 64 bucket/facing/phase variants')


if __name__ == '__main__':
    export()
