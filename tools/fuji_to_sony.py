"""Adapts Fujifilm film-simulation recipes (scraped by scrape_filmsim.py) to Sony alpha 7R III picture profiles.

Usage: python fuji_to_sony.py recipes_raw.json ../data/fuji-adaptions.json

The mapping is a heuristic written from the meaning of each parameter, NOT measured on a camera.
Every generated recipe says so in its notes. The rules are documented in docs/FUJI-ADAPTION.md.
"""
import json
import re
import sys

STARTER_COUNT = 8  # recipes in data/recipes.json; adaptions continue the order after them

# --- film simulation -> base picture profile -------------------------------------------------------
# gamma, colorMode, saturation, colorPhase, depth {R,G,B,C,M,Y}
BASES = {
    "provia": ("Movie", "Movie", 0, 0, dict(R=0, G=0, B=0, C=0, M=0, Y=0)),
    "velvia": ("Still", "Still", 12, 0, dict(R=2, G=3, B=2, C=2, M=1, Y=2)),
    "astia": ("Cine2", "Movie", -4, 1, dict(R=0, G=-1, B=-1, C=-1, M=-1, Y=-1)),
    "classic chrome": ("Cine1", "Pro", -6, 0, dict(R=0, G=-3, B=-2, C=-2, M=-1, Y=-1)),
    "classic neg": ("Still", "Cinema", 4, -2, dict(R=1, G=-3, B=2, C=1, M=0, Y=-1)),
    "nostalgic": ("Cine2", "Cinema", 2, 2, dict(R=1, G=-2, B=-2, C=-1, M=1, Y=2)),
    "eterna": ("Cine2", "Cinema", -8, -1, dict(R=0, G=-2, B=-1, C=-1, M=-1, Y=-1)),
    "reala": ("Still", "Movie", 0, 0, dict(R=1, G=0, B=0, C=0, M=0, Y=0)),
    "pro neg. std": ("Movie", "Pro", -3, 0, dict(R=0, G=0, B=0, C=0, M=0, Y=0)),
    "pro neg. hi": ("Still", "Pro", 1, 0, dict(R=0, G=0, B=0, C=0, M=0, Y=0)),
    "acros": ("Still", "Black & White", None, None, None),
}

WB_PRESET_K = {"daylight": 5500, "lightday": 5500, "shade": 7000, "cloudy": 6000, "incandescent": 3000,
               "fluorescent": 4200, "underwater": 5500}

RANGES = {"Highlight": (-2, 4), "Shadow": (-2, 4), "Color": (-4, 4), "Sharpness": (-4, 4),
          "Noise Red.": (-4, 4), "Clarity": (-5, 5)}


def rnd(x):
    """Round half away from zero."""
    return int(x + 0.5) if x >= 0 else -int(-x + 0.5)


def quarter(x):
    return rnd(x * 4) / 4


def clamp(x, lo, hi):
    return max(lo, min(hi, x))


def slug(s):
    import unicodedata
    s = unicodedata.normalize("NFD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")[:40] or "rezept"


def base_for(sim):
    s = sim.lower()
    for key in ("classic chrome", "classic neg", "nostalgic", "pro neg. std", "pro neg. hi", "acros",
                "provia", "velvia", "astia", "eterna", "reala"):
        if key in s:
            return key, BASES[key]
    return None, None


def parse_wb(text):
    """-> (kelvin or None, red shift, blue shift, label for the note)."""
    t = text.strip()
    red = blue = 0
    m = re.search(r"([+-]?\d+)\s*(?:\(to [+-]?\d+\))?\s*Red", t)
    if m:
        red = int(m.group(1))
    m = re.search(r"([+-]?\d+)\s*Blue", t)
    if m:
        blue = int(m.group(1))
    head = t.split(",")[0].strip().lower()
    kelvin = None
    km = re.match(r"^k?(\d{4})k?$", head)
    if km:
        kelvin = clamp(rnd(int(km.group(1)) / 100) * 100, 2500, 9900)
    else:
        for name, k in WB_PRESET_K.items():
            if head.startswith(name):
                kelvin = k
                break
    return kelvin, red, blue


def num(s):
    return float(s.replace("+", ""))


def adapt(r, order):
    key, base = base_for(r["sim"])
    if base is None:
        return None, "unknown film simulation: " + r["sim"]
    gamma, color_mode, base_sat, base_phase, base_depth = base
    adj = r["adj"]
    try:
        v = {k: num(adj[k]) for k in RANGES}
    except (KeyError, ValueError):
        # recipes without Highlight/Shadow (older bodies) default to 0, anything else invalid is skipped
        v = {}
        for k in RANGES:
            if k in adj:
                try:
                    v[k] = num(adj[k])
                except ValueError:
                    return None, "invalid " + k
            else:
                v[k] = 0.0
    for k, (lo, hi) in RANGES.items():
        if not lo <= v[k] <= hi:
            return None, f"{k} out of range: {v[k]}"

    rows = r["rows"]
    dr = r["dr"] if r["dr"] in ("DR100", "DR200", "DR400") else "DR100"
    hi_t, sh_t = v["Highlight"], v["Shadow"]

    # tone: Shadow -> black level / black gamma, DR + Highlight -> knee
    black_level = clamp(-3 * rnd(sh_t), -15, 15)
    bg_level = clamp(-rnd(sh_t), -7, 7)
    base_point = {"DR100": 100, "DR200": 92.5, "DR400": 85}[dr]
    slope = {"DR100": 0, "DR200": 1, "DR400": 2}[dr]
    if dr == "DR100" and rnd(hi_t) == 0:
        knee = {"mode": "Auto", "point": "", "slope": ""}
    else:
        point = clamp(round((base_point + 2.5 * rnd(hi_t)) / 2.5) * 2.5, 75, 105)
        knee = {"mode": "Manuell", "point": point, "slope": slope}

    # colour
    mono = key == "acros"
    sat = "" if mono else clamp(base_sat + 4 * rnd(v["Color"]), -32, 32)
    phase = "" if mono else base_phase
    cce = rows.get("Color Chrome Effect", "-").lower()
    ccb = rows.get("Color Chrome FX Blue", "-").lower()
    depth = {}
    for ch in "RGBCMY":
        if mono:
            depth[ch] = ""
            continue
        d = base_depth[ch]
        if ch != "B":
            d += 2 if cce == "strong" else 1 if cce == "weak" else 0
        else:
            d += 3 if ccb == "strong" else 1 if ccb == "weak" else 0
        depth[ch] = clamp(d, -7, 7)

    # detail
    detail = {"level": clamp(rnd(1.5 * v["Sharpness"]), -7, 7), "mode": "Auto", "vh": "", "bw": "",
              "limit": "", "crisp": "", "hl": ""}

    # white balance: Fuji red/blue shift -> amber/blue and green/magenta
    kelvin, red, blue = parse_wb(rows.get("White Balance", "Auto"))
    ab = clamp(quarter((red - blue) / 2 * 7 / 9), -7, 7)
    gm = clamp(quarter(-(red + blue) / 2 * 7 / 9), -7, 7)
    auto_wb = kelvin is None
    wb_k = 5500 if auto_wb else kelvin

    # descriptive traits
    warm = ab + (wb_k - 5500) / 700
    contrast = sh_t + hi_t
    adjectives = []
    if mono:
        adjectives.append("Schwarzweiß")
        if contrast >= 3:
            adjectives.append("kontrastreich")
        elif contrast <= -2:
            adjectives.append("weich")
    else:
        if warm > 2.5:
            adjectives.append("warm")
        elif warm < -1.5:
            adjectives.append("kühl")
        total_sat = base_sat + 4 * rnd(v["Color"])
        if total_sat >= 12:
            adjectives.append("kräftig")
        elif total_sat <= -8:
            adjectives.append("gedämpft")
        if contrast >= 3:
            adjectives.append("kontrastreich")
        elif contrast <= -2:
            adjectives.append("weich")
    sim_name = r["sim"].replace("Nostalgic Neg.", "Nostalgic Negative")
    look_adj = ", ".join(a.capitalize() if i == 0 else a for i, a in enumerate(adjectives)) or "Ausgewogen"
    look = f"{look_adj} – angelehnt an Fujis {sim_name}."

    handle = r["handle"]
    who = f"von {handle}" if handle else "(Ersteller siehe Quellseite)"
    original = [f"{r['sim']}", r["dr"] or "DR100"]
    for k in ("Highlight", "Shadow", "Color", "Sharpness", "Noise Red.", "Clarity"):
        if k in adj:
            original.append(f"{k} {adj[k]}")
    for k in ("Grain Effect", "Color Chrome Effect", "Color Chrome FX Blue", "White Balance", "ISO", "Exposure Comp."):
        if k in rows:
            original.append(f"{k} {rows[k]}")
    notes = [
        f"Adaption eines Fujifilm-Rezepts {who}. Die Werte sind eine Näherung nach docs/FUJI-ADAPTION.md "
        "und nicht an der α7R III geprüft.",
        "Original: " + ", ".join(original) + ".",
        "Nicht übertragbar: Korn, Clarity und Rauschreduzierung (bei Sony getrennt vom Bildprofil einzustellen).",
    ]
    if auto_wb:
        notes.append("Der Weißabgleich ist bei Fuji auf Auto oder Custom gestellt; 5500 K ist nur ein Startwert.")
    if mono and any(w in r["sim"] for w in ("Red", "Ye", "Green", "(R)", "(Ye)", "(G)")):
        notes.append("Der Filter der Acros-Variante hat bei Sony keine Entsprechung.")

    tags = ["Fuji-Adaption", sim_name] + [a for a in adjectives if a != "Schwarzweiß"] + (["Schwarzweiß"] if mono else [])
    recipe = {
        "id": "", "order": order, "name": r["title"], "look": look, "tags": tags,
        "notes": "\n".join(notes), "source": f"Fuji-Adaption · {handle + ' · ' if handle else ''}filmsimrecipes.com", "sourceUrl": r["url"],
        "pp": {
            "blackLevel": black_level, "gamma": gamma,
            "blackGamma": {"range": "Middle", "level": bg_level},
            "knee": knee, "colorMode": color_mode, "saturation": sat, "colorPhase": phase,
            "depth": depth, "detail": detail,
        },
        "wb": {"kelvin": wb_k, "ab": ab, "gm": gm},
    }
    return recipe, None


def main():
    raw = json.load(open(sys.argv[1], encoding="utf-8"))
    out, skipped, ids, names = [], [], set(), {}
    for r in raw:
        if r["locked"] or not r["sim"] or "�" in r["title"]:
            skipped.append((r["title"], "locked/unknown/garbled"))
            continue
        rec, why = adapt(r, STARTER_COUNT + len(out) + 1)
        if rec is None:
            skipped.append((r["title"], why))
            continue
        if r["title"] in names:  # duplicate title -> add creator handle
            rec["name"] = f"{r['title']} ({r['handle'] or 'Variante'})"
        names[r["title"]] = True
        base_id = "fx-" + slug(rec["name"])
        rid, n = base_id, 2
        while rid in ids:
            rid, n = f"{base_id}-{n}", n + 1
        ids.add(rid)
        rec["id"] = rid
        out.append(rec)
    doc = {
        "schemaVersion": 1,
        "camera": "Sony ILCE-7RM3 (α7R III)",
        "notes": "Adaptionen von Fujifilm-Rezepten (Quelle: filmsimrecipes.com, Rezepte der genannten Ersteller). "
                 "Heuristische Übersetzung, nicht an der Kamera geprüft. Look-Texte sind eigene Formulierungen.",
        "recipes": out,
        "slots": {},
    }
    with open(sys.argv[2], "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, indent=1)
    print(len(out), "recipes written;", len(skipped), "skipped")
    for t, why in skipped:
        print("  skipped:", t, "-", why)


if __name__ == "__main__":
    main()
