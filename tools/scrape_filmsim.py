"""Reads public recipe pages of filmsimrecipes.com into a JSON file (data only; nothing is executed)."""
import html
import json
import re
import sys
import time
import urllib.request

OUT_DIR = sys.argv[1]
UA = {"User-Agent": "Mozilla/5.0 (personal recipe import; polite, 1 req/sec)"}


def get(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", "ignore")


def text(s):
    return html.unescape(re.sub(r"<[^>]+>", "", s)).strip()


sitemap = get("https://filmsimrecipes.com/portfolio-sitemap.xml")
urls = sorted(set(re.findall(r"https://filmsimrecipes\.com/project/[a-z0-9\-]+/", sitemap)))
print(len(urls), "urls")
out = []
for i, u in enumerate(urls):
    try:
        h = get(u)
    except Exception as e:  # noqa: BLE001
        print("ERR", u, e)
        continue
    title = text(re.search(r"<title>(.*?)</title>", h, re.S).group(1)) if "<title>" in h else ""
    locked = bool(re.search(r'class="prolock[^"]*locked', h))
    m = re.search(r'class="bl">Film simulation</div><div class="bv">(.*?)</div>', h)
    sim = text(m.group(1)) if m else ""
    dr = re.search(r'<div class="dr"><div class="k">(.*?)</div>', h)
    rows = {text(a): text(b) for a, b in re.findall(r'<div class="row"><dt>(.*?)</dt><dd>(.*?)</dd></div>', h)}
    adj = {text(t): text(n) for n, t in re.findall(r'<div class="ac"><div class="n">(.*?)</div><div class="t">(.*?)</div></div>', h)}
    maker = re.search(r'class="mn"><a href="([^"]+)">(.*?)</a>', h)
    handle = re.search(r'class="mh">(.*?)</div>', h)
    tags = [text(t) for t in re.findall(r'class="ctag[^"]*"[^>]*>(.*?)</', h)]
    cam = re.search(r'Fully compatible with ([A-Za-z0-9\- ]+)', h)
    out.append({
        "url": u, "title": title.split("—")[0].strip(), "locked": locked, "sim": sim,
        "dr": text(dr.group(1)) if dr else "", "rows": rows, "adj": adj,
        "maker": text(maker.group(2)) if maker else "", "handle": text(handle.group(1)) if handle else "",
        "camera": cam.group(1).strip() if cam else "",
    })
    time.sleep(0.8)
    if i % 20 == 0:
        print(i, u, flush=True)
with open(OUT_DIR + "/recipes_raw.json", "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
print("done", len(out), "locked:", sum(1 for r in out if r["locked"]))
