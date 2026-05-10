#!/usr/bin/env python3
"""Fetch OSM node coordinates from Overpass and write valid GeoJSON for app assets."""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from typing import Dict, Iterable, List

# Усі node id з актуального експорту зупинок маршруту 101 (без координат у файлі)
NODE_IDS = [
    419826895,
    431331643,
    431331644,
    431333747,
    431333748,
    442483786,
    749139303,
    750591487,
    750592068,
    824428742,
    1151031850,
    1151031852,
    1502808738,
    1804121120,
    1866166172,
    2454354748,
    2475064313,
    2475064316,
    2475064318,
    2475064324,
    2475064329,
    2475064333,
    2475064336,
    2475106369,
    2475106370,
    2475106371,
    2475106372,
    2475106373,
    2475106374,
    2475106375,
    2475106378,
    2475106379,
    2475106380,
    2475106382,
    2475106384,
    2475106387,
    2475106390,
    2475106395,
    2475106399,
    2475106405,
    2477214515,
    3901062719,
    3954179666,
    4372082915,
    4372082916,
    4372082917,
    4372082918,
    4372091321,
    4372091322,
    4437803215,
    4871394821,
    4928418121,
    4993578724,
    9040085419,
    10032365686,
    10032408426,
    10032408431,
]

OUT_PATH = "app/src/main/assets/map/stops_101_route.geojson"

# Кілька інстансів: один великий запит часто дає 504; дрібні батчі надійніші.
OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
]

HEADERS = {
    "Content-Type": "application/x-www-form-urlencoded",
    "Accept": "*/*",
            "User-Agent": "RoutePulse/1.0 (stops geojson build; contact=local)",
}

CHUNK_SIZE = 12
QUERY_TIMEOUT_SEC = 55
HTTP_TIMEOUT_SEC = 65
MAX_RETRIES = 3


def _chunks(ids: List[int], size: int) -> Iterable[List[int]]:
    for i in range(0, len(ids), size):
        yield ids[i : i + size]


def overpass_fetch(endpoint: str, node_ids: List[int]) -> Dict:
    inner = "".join(f"node({i});" for i in node_ids)
    query = (
        f"[out:json][timeout:{QUERY_TIMEOUT_SEC}];\n({inner});\nout body;\n"
    )
    req = urllib.request.Request(
        endpoint,
        data=query.encode("utf-8"),
        headers=HEADERS,
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT_SEC) as resp:
        return json.load(resp)


def fetch_all_nodes(node_ids: List[int]) -> Dict[int, dict]:
    by_id: Dict[int, dict] = {}
    for chunk in _chunks(node_ids, CHUNK_SIZE):
        ok = False
        last_err: BaseException | None = None
        for endpoint in OVERPASS_ENDPOINTS:
            for attempt in range(MAX_RETRIES):
                try:
                    data = overpass_fetch(endpoint, chunk)
                    for el in data.get("elements", []):
                        if (
                            el.get("type") == "node"
                            and "lat" in el
                            and "lon" in el
                        ):
                            by_id[el["id"]] = el
                    ok = True
                    break
                except (
                    urllib.error.HTTPError,
                    urllib.error.URLError,
                    TimeoutError,
                    OSError,
                    json.JSONDecodeError,
                ) as e:
                    last_err = e
                    time.sleep(2.0 * (attempt + 1))
            if ok:
                break
        if not ok:
            raise RuntimeError(
                f"Overpass failed for chunk starting {chunk[0]}: {last_err}"
            ) from last_err
        time.sleep(1.0)
    return by_id


def main() -> None:
    by_id = fetch_all_nodes(NODE_IDS)

    features = []
    missing = []
    for nid in NODE_IDS:
        el = by_id.get(nid)
        if not el:
            missing.append(nid)
            continue
        tags = el.get("tags") or {}
        name_uk = (tags.get("name:uk") or "").strip()
        name = (tags.get("name") or "").strip()
        title = name_uk or name or tags.get("official_name") or ""
        if not title:
            title = f"Зупинка (node {nid})"
        props = {"name": tags.get("name"), "name:uk": tags.get("name:uk"), "osm_id": nid}
        lon, lat = float(el["lon"]), float(el["lat"])
        features.append(
            {
                "type": "Feature",
                "properties": props,
                "geometry": {"type": "Point", "coordinates": [lon, lat]},
            }
        )

    fc = {
        "type": "FeatureCollection",
        "generator": "RoutePulse tools/build_stops_101_geojson.py + Overpass API",
        "copyright": "Data © OpenStreetMap contributors, ODbL",
        "features": features,
    }
    if missing:
        fc["meta_missing_nodes"] = missing

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(fc, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"Wrote {len(features)} features to {OUT_PATH}")
    if missing:
        print("WARNING missing nodes:", missing)


if __name__ == "__main__":
    main()
