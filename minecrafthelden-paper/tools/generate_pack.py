from pathlib import Path
from PIL import Image
import base64, io, json, zipfile, shutil

ROOT = Path(__file__).resolve().parents[1]
work = ROOT / 'build' / 'pack'
if work.exists(): shutil.rmtree(work)
(work / 'assets/minecrafthelden/font').mkdir(parents=True)
(work / 'assets/minecrafthelden/textures/font').mkdir(parents=True)

filled_b64 = 'iVBORw0KGgoAAAANSUhEUgAAAAsAAAALCAYAAACprHcmAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA2ElEQVQYlW2RMU7DQBRE3ziREDdBipQjpOEwFG5oqKGOuAS+QGS5odorbJEiLZQWZQSSQ5ZJsfISE6aav3ozq68v2wBIygawrb8zQDWCaloUIuoTklzvnX2I0yKF6M8f++1wdDfY9d6TWU/PBjwHYLHk45jYegbAw1UifM+APLO6BWCeqyu25SNKCGCXft8rgPoaXg9caJfgPQE3i7JpLn/ZWH1yN9jdYK+/bPXJalpnzBkugaa1QsxgiGWxwoymBB7XOXR3PwEv4BL4B7SNxgueS5LHq53rBLkow+6VDkKrAAAADmVYSWZNTQAqAAAACAAAAAAAAADSU5MAAAAASUVORK5CYII='
empty_b64 = 'iVBORw0KGgoAAAANSUhEUgAAAAsAAAALCAYAAACprHcmAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAt0lEQVQYlW2RQcqFMAyEv/x234XZewbd9RQ9qwfxJBZBXArJWzzqX8srBDqZCckk4u4AiMj3A7i79BggVGHOuXI/sbtLqMSyLAzDgIgAkFLCzLjv+ykItTqEwHEciAgpJfZ9B2Acx6fDH92rHn7h0CZVFYBSCqqKu2Nm/+LW+TzPXNeFqlJKIcbItm2s64q7S+hXNU0TZoaZvYQA0s5UNxNj5DzPl/CZtQ3Ac87+pd6c9O7bI/T5D3ILiXCgQjswAAAADmVYSWZNTQAqAAAACAAAAAAAAADSU5MAAAAASUVORK5CYII='
filled = Image.open(io.BytesIO(base64.b64decode(filled_b64))).convert('RGBA').crop((0,0,9,9))
empty = Image.open(io.BytesIO(base64.b64decode(empty_b64))).convert('RGBA').crop((0,0,9,9))
canvas = Image.new('RGBA', (25, 36), (0,0,0,0))
positions = [0,3,11]
for lives in range(4):
    y = lives * 9
    for idx, x in enumerate(positions):
        is_filled = idx >= 3 - lives
        canvas.alpha_composite(filled if is_filled else empty, (x,y))
canvas.save(work / 'assets/minecrafthelden/textures/font/lives.png', optimize=True)

font = {"providers": [{"type":"bitmap","file":"minecrafthelden:font/lives.png","ascent":8,"height":9,"chars":["\\ue000","\\ue001","\\ue002","\\ue003"]}]}
(work / 'assets/minecrafthelden/font/lives.json').write_text(json.dumps(font, ensure_ascii=False, separators=(',',':')), encoding='utf-8')
pack = {"pack":{"pack_format":75,"description":"Original Minecraft Helden HUD für Paper 1.21.11"}}
(work / 'pack.mcmeta').write_text(json.dumps(pack, ensure_ascii=False, separators=(',',':')), encoding='utf-8')

out = ROOT / 'build' / 'minecrafthelden-pack.zip'
out.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(work.rglob('*')):
        if p.is_file(): z.write(p, p.relative_to(work).as_posix())
print(out)
