#!/usr/bin/env python3
"""
Завантажити з Overpass API геометрію relation bus і зупинки для київських маршрутів.

Той самий движок, що й https://overpass-turbo.eu/ (не браузер, а POST до /api/interpreter).

Формат виходу сумісний із BusRouteGeoJsonParser та TripRouteAssetLoader.
Після оновлення зупинок запустіть tools/annotate_stop_schedule_geojson.py, щоб записати planned_time у GeoJSON.
Дані: © OpenStreetMap contributors, ODbL.
"""
from __future__ import annotations

import json
import re
import sys
import time
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Set, Tuple

ROOT = Path(__file__).resolve().parents[1]
ASSETS_MAP = ROOT / "app" / "src" / "main" / "assets" / "map"

# Популярні маршрути Києва (узгоджено з типовими лініями «Київпастранс» у OSM).
# «3» у межах bbox часто відсутній як «Київський автобус» — замінено на «11».
ROUTE_REFS: Sequence[str] = ("11", "7", "18", "24", "50", "55", "62", "114", "115")

# Південь, захід, північ, схід — охоплення Києва та околиць.
KYIV_BBOX = (50.25, 30.25, 50.72, 30.95)

OVERPASS_ENDPOINTS = (
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
)

HEADERS = {
    "Content-Type": "application/x-www-form-urlencoded",
    "Accept": "*/*",
    "User-Agent": "CityApp-KyivBusFetch/1.0 (+local dev)",
}

NETWORK_EXPECTED = "Київський автобус"


def overpass_post(query: str, timeout_http: int = 200) -> Dict[str, Any]:
    data = query.encode("utf-8")
    last_err: Optional[BaseException] = None
    for endpoint in OVERPASS_ENDPOINTS:
        for attempt in range(3):
            try:
                req = urllib.request.Request(
                    endpoint, data=data, headers=HEADERS, method="POST"
                )
                with urllib.request.urlopen(req, timeout=timeout_http) as resp:
                    return json.load(resp)
            except (
                urllib.error.HTTPError,
                urllib.error.URLError,
                TimeoutError,
                OSError,
                json.JSONDecodeError,
            ) as e:
                last_err = e
                time.sleep(2.0 * (attempt + 1))
        time.sleep(3.0)
    raise RuntimeError(f"Overpass failed: {last_err}") from last_err


def find_relation_ids() -> Dict[str, int]:
    """Один variant на ref — найменший id серед relations «Київський автобус»."""
    refs_regex = "^(" + "|".join(re.escape(r) for r in ROUTE_REFS) + ")$"
    south, west, north, east = KYIV_BBOX
    query = f"""[out:json][timeout:120];
rel["route"="bus"]["ref"~"{refs_regex}"]({south},{west},{north},{east});
out tags;
"""
    data = overpass_post(query)
    by_ref: Dict[str, List[int]] = defaultdict(list)
    for el in data.get("elements", []):
        if el.get("type") != "relation":
            continue
        tags = el.get("tags") or {}
        if tags.get("network") != NETWORK_EXPECTED:
            continue
        ref = (tags.get("ref") or "").strip()
        if ref in ROUTE_REFS:
            by_ref[ref].append(int(el["id"]))
    chosen: Dict[str, int] = {}
    for ref in ROUTE_REFS:
        ids = sorted(by_ref.get(ref, []))
        if not ids:
            print(f"WARN: немає relation для ref={ref}", file=sys.stderr)
            continue
        if len(ids) > 1:
            print(
                f"INFO: ref={ref}: кількість напрямків={len(ids)}, беремо relation id={ids[0]}",
                file=sys.stderr,
            )
        chosen[ref] = ids[0]
    return chosen


def fetch_relation_geom(rel_id: int) -> Dict[str, Any]:
    query = f"""[out:json][timeout:180];
rel({rel_id});
out geom;
"""
    return overpass_post(query, timeout_http=220)


def relation_to_route_feature(elements: List[Dict[str, Any]], rel_id: int) -> Optional[Dict[str, Any]]:
    rel = next((e for e in elements if e.get("type") == "relation" and e.get("id") == rel_id), None)
    if not rel:
        return None
    tags = dict(rel.get("tags") or {})
    tags["type"] = "route"

    lines: List[List[List[float]]] = []
    for m in rel.get("members") or []:
        if m.get("type") != "way":
            continue
        geom = m.get("geometry")
        if not geom:
            continue
        chunk: List[List[float]] = []
        for pt in geom:
            lon = pt.get("lon")
            lat = pt.get("lat")
            if lon is None or lat is None:
                continue
            chunk.append([float(lon), float(lat)])
        if len(chunk) >= 2:
            lines.append(chunk)

    if not lines:
        return None

    if len(lines) == 1:
        geom_obj: Dict[str, Any] = {"type": "LineString", "coordinates": lines[0]}
    else:
        geom_obj = {"type": "MultiLineString", "coordinates": lines}

    return {"type": "Feature", "properties": tags, "geometry": geom_obj}


def _member_is_pt_stop_role(role_lc: str) -> bool:
    return "platform" in role_lc or role_lc.startswith("stop") or role_lc == "stop"


def extract_stop_candidates(rel_element: Dict[str, Any]) -> Tuple[List[int], Set[int]]:
    """У порядку members: PT v2 (platform/stop*) або legacy node без ролі."""
    ordered: List[int] = []
    pt_marked: Set[int] = set()
    seen: Set[int] = set()
    for m in rel_element.get("members") or []:
        if m.get("type") != "node":
            continue
        role_lc = (m.get("role") or "").lower()
        include = _member_is_pt_stop_role(role_lc) or role_lc == ""
        if not include:
            continue
        nid = int(m["ref"])
        if nid in seen:
            continue
        seen.add(nid)
        ordered.append(nid)
        if _member_is_pt_stop_role(role_lc):
            pt_marked.add(nid)
    return ordered, pt_marked


def node_tags_imply_stop(tags: Dict[str, Any]) -> bool:
    pt = (tags.get("public_transport") or "").lower()
    if pt in ("platform", "stop_position", "station"):
        return True
    if tags.get("highway") == "bus_stop":
        return True
    return False


def fetch_nodes_body(node_ids: Sequence[int]) -> Dict[int, Dict[str, Any]]:
    if not node_ids:
        return {}
    by_id: Dict[int, Dict[str, Any]] = {}
    chunk_size = 45
    for start in range(0, len(node_ids), chunk_size):
        chunk = node_ids[start : start + chunk_size]
        inner = "".join(f"node({i});" for i in chunk)
        query = f"[out:json][timeout:120];\n({inner});\nout body;\n"
        data = overpass_post(query)
        for el in data.get("elements", []):
            if el.get("type") == "node" and "lat" in el and "lon" in el:
                by_id[int(el["id"])] = el
        time.sleep(0.6)
    return by_id


def stops_geojson_from_ordered_nodes(
    node_ids: Sequence[int],
    nodes_by_id: Dict[int, Dict[str, Any]],
    pt_role_nodes: Set[int],
) -> Dict[str, Any]:
    features: List[Dict[str, Any]] = []
    seen_geom: Set[Tuple[str, str]] = set()
    idx = 0
    for nid in node_ids:
        el = nodes_by_id.get(nid)
        if not el:
            continue
        tags = el.get("tags") or {}
        if nid not in pt_role_nodes and not node_tags_imply_stop(tags):
            continue
        lat = float(el["lat"])
        lon = float(el["lon"])
        key = (f"{lat:.5f}", f"{lon:.5f}")
        if key in seen_geom:
            continue
        seen_geom.add(key)
        idx += 1
        name_uk = (tags.get("name:uk") or "").strip()
        name = (tags.get("name") or "").strip()
        title = name_uk or name or (tags.get("official_name") or "").strip()
        if not title:
            title = f"Зупинка (node {nid})"
        props = {
            "name": tags.get("name"),
            "name:uk": tags.get("name:uk"),
            "osm_id": nid,
            "order_hint": idx,
        }
        features.append(
            {
                "type": "Feature",
                "properties": props,
                "geometry": {"type": "Point", "coordinates": [lon, lat]},
            }
        )
    return {
        "type": "FeatureCollection",
        "generator": "CityApp tools/fetch_kyiv_bus_routes.py + Overpass API",
        "copyright": "Data © OpenStreetMap contributors, ODbL",
        "features": features,
    }


def write_route_asset(ref: str, feature: Dict[str, Any]) -> None:
    path = ASSETS_MAP / f"{ref}_route.geojson"
    fc = {
        "type": "FeatureCollection",
        "generator": "CityApp tools/fetch_kyiv_bus_routes.py + Overpass API",
        "copyright": "The data included in this document is from www.openstreetmap.org. "
        "The data is made available under ODbL.",
        "features": [feature],
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(fc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_stops_asset(ref: str, fc: Dict[str, Any]) -> None:
    path = ASSETS_MAP / f"stops_{ref}_route.geojson"
    path.write_text(json.dumps(fc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def subtitle_from_tags(tags: Dict[str, Any]) -> str:
    fr = (tags.get("from") or "").strip()
    to = (tags.get("to") or "").strip()
    if fr and to:
        return f"{fr} — {to}"
    return (tags.get("name:uk") or tags.get("name") or "").strip()


def main() -> None:
    ASSETS_MAP.mkdir(parents=True, exist_ok=True)
    id_by_ref = find_relation_ids()
    if not id_by_ref:
        print("Не знайдено жодного маршруту за bbox + network.", file=sys.stderr)
        sys.exit(1)

    manifest: Dict[str, Any] = {"network": NETWORK_EXPECTED, "bbox": KYIV_BBOX, "routes": []}

    for ref in ROUTE_REFS:
        if ref not in id_by_ref:
            continue
        rel_id = id_by_ref[ref]
        print(f"Fetching route {ref} (relation {rel_id})...")
        pack = fetch_relation_geom(rel_id)
        els = pack.get("elements", [])
        rel_el = next((e for e in els if e.get("type") == "relation" and e.get("id") == rel_id), None)
        feat = relation_to_route_feature(els, rel_id)
        if not feat:
            print(f"WARN: немає геометрії для ref={ref}", file=sys.stderr)
            continue
        tags = feat.get("properties") or {}
        write_route_asset(ref, feat)

        node_ids: List[int] = []
        pt_marked: Set[int] = set()
        if rel_el:
            node_ids, pt_marked = extract_stop_candidates(rel_el)
        if not node_ids:
            print(f"WARN: немає candidate node-зупинок у relation для ref={ref}", file=sys.stderr)
        nodes_map = fetch_nodes_body(node_ids)
        stops_fc = stops_geojson_from_ordered_nodes(node_ids, nodes_map, pt_marked)
        write_stops_asset(ref, stops_fc)

        manifest["routes"].append(
            {
                "ref": ref,
                "relation_id": rel_id,
                "subtitle": subtitle_from_tags(tags),
                "from": tags.get("from"),
                "to": tags.get("to"),
                "name_uk": tags.get("name:uk"),
                "stops_written": len(stops_fc["features"]),
            }
        )
        print(f"  OK: liniya + {len(stops_fc['features'])} zupynok -> map/{ref}_route.geojson")
        time.sleep(2.0)

    summary_path = ASSETS_MAP / "kyiv_bus_fetch_manifest.json"
    summary_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Manifest:", summary_path)


if __name__ == "__main__":
    main()
