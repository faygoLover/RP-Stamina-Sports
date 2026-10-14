"""Тестовый мир для проверки в игре: Суперплоский, без построек, мобов, погоды и смены суток, творческий, читы.

Берёт level.dat любого суперплоского мира из run/saves как образец (там уже верные данные Forge), правит настройки и
кладёт новый мир без чанков — земля сгенерируется заново.

    python scripts/make_test_world.py [образец] [имя]      # по умолчанию: test → "RP Sports test"
"""
import os
import shutil
import sys

import nbtlib
from nbtlib.tag import Byte, Int, Long, String, List

SAVES = os.path.join(os.path.dirname(__file__), "..", "run", "saves")
src = sys.argv[1] if len(sys.argv) > 1 else "test"
name = sys.argv[2] if len(sys.argv) > 2 else "RP Sports test"

out = os.path.join(SAVES, name)
if os.path.exists(out):
    shutil.rmtree(out)
os.makedirs(out)

f = nbtlib.load(os.path.join(SAVES, src, "level.dat"))
d = f["Data"]
d["LevelName"] = String(name)
d["allowCommands"] = Byte(1)
d["GameType"] = Int(1)
d["DayTime"] = Long(6000)
d["raining"] = Byte(0)
d["thundering"] = Byte(0)
d["rainTime"] = Int(0)
d["thunderTime"] = Int(0)
d["clearWeatherTime"] = Int(1_000_000)
d.pop("Player", None)

wg = d["WorldGenSettings"]
wg["generate_features"] = Byte(0)  # постройки
gen = wg["dimensions"]["minecraft:overworld"]["generator"]
if gen["type"] != "minecraft:flat":
    sys.exit("Образец должен быть суперплоским миром")
gen["settings"]["structure_overrides"] = List[String]([])
gen["settings"]["features"] = Byte(0)
gen["settings"]["lakes"] = Byte(0)

rules = d["GameRules"]
for rule in ("doDaylightCycle", "doWeatherCycle", "doMobSpawning", "doPatrolSpawning", "doTraderSpawning",
             "doInsomnia", "doWardenSpawning"):
    rules[rule] = String("false")

f.save(os.path.join(out, "level.dat"))
print("Мир готов:", os.path.abspath(out))
