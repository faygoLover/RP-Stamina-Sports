"""Модели, состояния блоков и лут тренажёров RP Stamina Sports (коробки на ванильных текстурах).

Правила (находки 71–72):
- координаты элементов — только целые пиксели (иначе текстура ложится мимо сетки);
- UV каждой грани считаются от координат и сдвигаются внутрь текстуры 0–16 — части модели, выходящие за блок
  (стойки выше 16, детали предмета), не берут куски соседних текстур атласа.

    python tools/gen_models.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "rpsports")
D = os.path.join(ROOT, "data")

IRON = "minecraft:block/iron_block"
BLACK = "minecraft:block/black_concrete"
GRAY = "minecraft:block/gray_concrete"
WOOL_BLACK = "minecraft:block/black_wool"
RED = "minecraft:block/red_wool"
BLUE = "minecraft:block/blue_wool"
ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _fit(a, b):
    """Отрезок UV — внутрь 0–16 (не длиннее 16)."""
    size = min(16, b - a)
    a = a % 16
    if a + size > 16:
        a = 16 - size
    return a, a + size


def _uv(face, fr, to):
    x1, y1, z1 = fr
    x2, y2, z2 = to
    u = {"north": (16 - x2, 16 - x1), "south": (x1, x2), "west": (z1, z2), "east": (16 - z2, 16 - z1),
         "up": (x1, x2), "down": (x1, x2)}[face]
    v = {"up": (z1, z2), "down": (16 - z2, 16 - z1)}.get(face, (16 - y2, 16 - y1))
    ua, ub = _fit(*u)
    va, vb = _fit(*v)
    return [ua, va, ub, vb]


def box(fr, to, tex):
    for c in list(fr) + list(to):
        if c != int(c):
            raise ValueError(f"не целый пиксель: {fr} {to}")
    fr = [int(c) for c in fr]
    to = [int(c) for c in to]
    faces = {f: {"texture": "#" + tex, "uv": _uv(f, fr, to)} for f in ("north", "south", "east", "west", "up", "down")}
    return {"from": fr, "to": to, "faces": faces}


def shifted(elems, dx=0, dy=0):
    out = []
    for e in elems:
        tex = next(iter(e["faces"].values()))["texture"][1:]
        out.append(box([e["from"][0] + dx, e["from"][1] + dy, e["from"][2]],
                       [e["to"][0] + dx, e["to"][1] + dy, e["to"][2]], tex))
    return out


def disp(scale, dy=0):
    return {"gui": {"rotation": [30, 225, 0], "translation": [0, dy, 0], "scale": [scale] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [scale * 0.7] * 3},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [scale * 0.7] * 3}}


def loot(name, part_prop):
    cond = [{"condition": "minecraft:survives_explosion"}]
    if part_prop:
        cond.append({"condition": "minecraft:block_state_property", "block": f"rpsports:{name}", "properties": {"part": "0"}})
    w(os.path.join(D, "rpsports", "loot_tables", "blocks", f"{name}.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"rpsports:{name}"}], "conditions": cond}]})


def multi(name, parts, tex, item_elems, item_disp):
    """Многоблочный тренажёр: parts[i] — элементы части i (модель «на север»)."""
    for i, elems in enumerate(parts):
        w(os.path.join(A, "models", "block", f"{name}_{i}.json"),
          {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex, "elements": elems})
    variants = {}
    for f, y in ROT:
        for i in range(6):  # свойство part общее 0…5; лишние — на модель основания
            m = {"model": f"rpsports:block/{name}_{i if i < len(parts) else 0}"}
            if y:
                m["y"] = y
            variants[f"facing={f},part={i}"] = m
    w(os.path.join(A, "blockstates", f"{name}.json"), {"variants": variants})
    w(os.path.join(A, "models", "item", f"{name}.json"),
      {"parent": "minecraft:block/block", "textures": tex, "elements": item_elems, "display": item_disp})
    loot(name, True)


# ---- Скамья 2×1: голова у x = 0, ноги — во второй блок (+x). Стойки по краям (z 0…1, 15…16) с крюками к ногам.
bench_tex = {"frame": IRON, "pad": WOOL_BLACK, "particle": IRON}
bench_head = [box([2, 6, 4], [16, 8, 12], "pad"), box([4, 0, 6], [6, 6, 10], "frame"),
              box([12, 0, 0], [13, 18, 1], "frame"), box([12, 0, 15], [13, 18, 16], "frame"),
              box([13, 16, 0], [15, 17, 1], "frame"), box([13, 16, 15], [15, 17, 16], "frame"),
              box([12, 1, 1], [13, 2, 15], "frame")]
bench_foot = [box([0, 6, 4], [13, 8, 12], "pad"), box([9, 0, 6], [11, 6, 10], "frame")]
multi("bench", [bench_head, bench_foot], bench_tex, shifted(bench_head, -8) + shifted(bench_foot, 8), disp(0.42))

# ---- Турник 2×3: стойки у внешних краёв, перекладина 2 px на высоте 2 + 6…7 px (≈ 2,4 блока).
pull_tex = {"frame": IRON, "bar": GRAY, "particle": IRON}
post_l = box([1, 0, 6], [3, 16, 10], "frame")
post_r = box([13, 0, 6], [15, 16, 10], "frame")
top_l = [box([1, 0, 6], [3, 8, 10], "frame"), box([3, 6, 7], [16, 7, 9], "bar")]
top_r = [box([13, 0, 6], [15, 8, 10], "frame"), box([0, 6, 7], [13, 7, 9], "bar")]
pull_item = [box([2, -16, 6], [4, 24, 10], "frame"), box([12, -16, 6], [14, 24, 10], "frame"),
             box([4, 21, 7], [12, 22, 9], "bar")]
multi("pullup_bar", [[post_l], [post_r], [post_l], [post_r], top_l, top_r], pull_tex, pull_item, disp(0.38, 1))

# ---- Груша 1×2: висит в верхних двух третях.
bag_tex = {"bag": RED, "rod": GRAY, "particle": RED}
bag_low = [box([4, 11, 4], [12, 16, 12], "bag")]
bag_high = [box([4, 0, 4], [12, 12, 12], "bag"), box([5, 12, 5], [11, 13, 11], "bag"), box([7, 13, 7], [9, 16, 9], "rod")]
multi("punching_bag", [bag_low, bag_high], bag_tex, shifted(bag_low, 0, -8) + shifted(bag_high, 0, 8), disp(0.45))

# ---- Дорожка 2×1: рама, лента, поручень на дальнем конце второй части.
tr_tex = {"frame": IRON, "belt": BLACK, "particle": IRON}
tr_back = [box([0, 0, 2], [16, 2, 14], "frame"), box([0, 2, 3], [16, 3, 13], "belt")]
tr_front = [box([0, 0, 2], [14, 2, 14], "frame"), box([0, 2, 3], [13, 3, 13], "belt"),
            box([13, 0, 2], [15, 18, 4], "frame"), box([13, 0, 12], [15, 18, 14], "frame"),
            box([13, 16, 4], [15, 18, 12], "frame")]
multi("treadmill", [tr_back, tr_front], tr_tex, shifted(tr_back, -8) + shifted(tr_front, 8), disp(0.42))

# ---- Коврик 1×1 (решение Алекса — чтобы застелить площадку).
mat = [box([0, 0, 0], [16, 1, 16], "mat")]
w(os.path.join(A, "models", "block", "gym_mat.json"), {"parent": "minecraft:block/block", "textures": {"mat": BLUE, "particle": BLUE}, "elements": mat})
w(os.path.join(A, "models", "item", "gym_mat.json"), {"parent": "rpsports:block/gym_mat"})
w(os.path.join(A, "blockstates", "gym_mat.json"), {"variants": {f"facing={f}": {"model": "rpsports:block/gym_mat", **({"y": y} if y else {})} for f, y in ROT}})
loot("gym_mat", False)

# ---- Блины для штанги.
ITEM = {"gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}}
for name, r in (("2_5", 3), ("5", 4), ("10", 5), ("20", 6)):
    w(os.path.join(A, "models", "item", f"plate_{name}.json"), {"parent": "minecraft:block/block",
      "textures": {"plate": BLACK, "hole": GRAY, "particle": BLACK},
      "elements": [box([8 - r, 8 - r, 7], [8 + r, 8 + r, 9], "plate"), box([7, 7, 6], [9, 9, 10], "hole")], "display": ITEM})

# ---- Гантели: блины 5/2/1 кг (2 px толщиной) набором на каждую сторону, хват между ними, гайки, концы грифа.
PLATE_R = {5: 4, 2: 3, 1: 2}


def side_plates(kg):
    per = round(kg / 2)
    out = []
    for p in (5, 2, 1):
        while per >= p:
            out.append(p)
            per -= p
    return out or [1]


for kg in (2, 5, 8, 12, 16, 20):
    elems = [box([5, 7, 7], [11, 9, 9], "grip")]
    left, right = 5, 11
    for p in side_plates(kg):  # крупные — ближе к хвату
        r = PLATE_R[p]
        elems.append(box([left - 2, 8 - r, 8 - r], [left, 8 + r, 8 + r], "plate"))
        elems.append(box([right, 8 - r, 8 - r], [right + 2, 8 + r, 8 + r], "plate"))
        left -= 2
        right += 2
    elems += [box([left - 1, 6, 6], [left, 10, 10], "nut"), box([right, 6, 6], [right + 1, 10, 10], "nut"),
              box([left - 2, 7, 7], [left - 1, 9, 9], "grip"), box([right + 1, 7, 7], [right + 2, 9, 9], "grip")]
    w(os.path.join(A, "models", "item", f"dumbbell_{kg}.json"), {"parent": "minecraft:block/block",
      "textures": {"grip": GRAY, "plate": BLACK, "nut": IRON, "particle": BLACK}, "elements": elems, "display": ITEM})

w(os.path.join(D, "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
  {"replace": False, "values": ["rpsports:bench", "rpsports:pullup_bar", "rpsports:treadmill"]})
print("ok")
