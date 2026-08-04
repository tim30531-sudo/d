from pathlib import Path
import base64
import gzip

root = Path(__file__).resolve().parent
data = ''.join(path.read_text(encoding='utf-8').strip() for path in sorted(root.glob('source.part*')))
source = gzip.decompress(base64.b64decode(data))
out = root.parent / 'src/main/java/de/tim30531/minecrafthelden/MinecraftHeldenPlugin.java'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(source)
print(out)
