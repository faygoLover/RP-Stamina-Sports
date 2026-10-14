"""Связанные моды из сборки автора — для общей проверки семьи (-Pwith_pack=true). В git не попадают (libs/pack).

Берёт из сборки DEPARTMENT_s5_client моды, с которыми работают моды семьи RP, и всё, что им обязательно нужно;
Zone Artifacts — свежий из соседней папки мода. Старое содержимое libs/pack удаляется.

    python scripts/copy_pack.py
"""
import os
import re
import shutil
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DST = os.path.join(HERE, "..", "libs", "pack")
PACK = os.path.join(os.environ.get("APPDATA", ""), "ElyPrismLauncher", "instances", "DEPARTMENT_s5_client", "minecraft", "mods")
ZONE = os.path.join(HERE, "..", "..", "faygolover's Zone Artifacts", "build", "libs")

# Моды, с которыми связаны моды семьи (проверки isLoaded и совместимость), плюс мебель госпиталя Medicine.
WANT = [
    "curios", "tacz", "zerocontact", "taa", "voicechat", "legendarysurvivaloverhaul", "patchouli", "carryon", "cuffed",
    "sanitydim", "betterthirdperson", "farmersdelight", "vinery", "brewery", "candlelight", "herbalbrews",
    "butchery", "health_and_disease", "industrialhellscape", "moa_decor_science", "multibeds", "refurbished_furniture",
]
# Уже есть в libs (ставятся отдельно).
SKIP = {"playeranimator", "bendylib"}


def mods_toml(path):
    try:
        with zipfile.ZipFile(path) as z:
            return z.read("META-INF/mods.toml").decode("utf-8", "replace")
    except Exception:
        return None


def parse(text):
    head = re.split(r"\[\[dependencies", text)[0]
    own = re.findall(r'^\s*modId\s*=\s*"([^"]+)"', head, re.M)
    deps = set()
    for blk in re.findall(r"\[\[dependencies[^\]]*\]\](.*?)(?=\[\[|\Z)", text, re.S):
        m = re.search(r'modId\s*=\s*"([^"]+)"', blk)
        man = re.search(r"mandatory\s*=\s*(true|false)", blk)
        typ = re.search(r'type\s*=\s*"([^"]+)"', blk)
        if m and ((man and man.group(1) == "true") or (typ and typ.group(1).lower() == "required")):
            deps.add(m.group(1))
    return own, deps - set(own) - {"forge", "minecraft"}


index = {}
for f in sorted(os.listdir(PACK)):
    if not f.endswith(".jar"):
        continue
    t = mods_toml(os.path.join(PACK, f))
    if t is None:
        continue
    own, deps = parse(t)
    for o in own:
        index[o] = (f, deps)

need, queue = [], list(WANT)
while queue:
    mid = queue.pop(0)
    if mid in need or mid in SKIP or mid.startswith("rp"):
        continue
    if mid not in index:
        print("нет в сборке:", mid)
        continue
    need.append(mid)
    queue.extend(sorted(index[mid][1]))

shutil.rmtree(DST, ignore_errors=True)
os.makedirs(DST)
jars = {}
for mid in need:
    jars.setdefault(index[mid][0], mid)
for jar, mid in sorted(jars.items(), key=lambda x: x[1]):
    shutil.copyfile(os.path.join(PACK, jar), os.path.join(DST, f"{mid}-1.jar"))
    print(f"libs/pack/{mid}-1.jar <- {jar}")

zone = sorted((f for f in os.listdir(ZONE) if f.startswith("fl_zone_arts-") and f.endswith(".jar")),
              key=lambda s: [int(x) for x in re.findall(r"\d+", s)])
if zone:
    shutil.copyfile(os.path.join(ZONE, zone[-1]), os.path.join(DST, "fl_zone_arts-1.jar"))
    print("libs/pack/fl_zone_arts-1.jar <-", zone[-1])
