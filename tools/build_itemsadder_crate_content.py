#!/usr/bin/env python3
import argparse
import base64
import json
import math
import shutil
import zipfile
from pathlib import Path


def find_entry(zip_file, suffix):
    matches = [name for name in zip_file.namelist() if name.replace("\\", "/").endswith(suffix)]
    if not matches:
        raise FileNotFoundError(suffix)
    return matches[0]


def walk_outliner(nodes, groups_by_uuid, parent_names=None):
    parent_names = parent_names or []
    element_parents = {}
    for node in nodes:
        if isinstance(node, str):
            element_parents[node] = list(parent_names)
            continue
        if not isinstance(node, dict):
            continue
        group = groups_by_uuid.get(node.get("uuid"), {})
        name = str(group.get("name", "")).lower()
        names = parent_names + ([name] if name else [])
        for child in node.get("children", []):
            if isinstance(child, str):
                element_parents[child] = list(names)
            elif isinstance(child, dict):
                element_parents.update(walk_outliner([child], groups_by_uuid, names))
    return element_parents


def is_degenerate(element, epsilon=0.02):
    frm = element.get("from", [0, 0, 0])
    to = element.get("to", [0, 0, 0])
    return any(abs(float(to[i]) - float(frm[i])) <= epsilon for i in range(3))


def face_to_json(face):
    data = {
        "uv": [round(float(v), 4) for v in face.get("uv", [0, 0, 16, 16])],
        "texture": f"#{face.get('texture', 0)}",
    }
    if "rotation" in face:
        data["rotation"] = int(face["rotation"])
    return data


def build_model(bbmodel):
    groups_by_uuid = {group.get("uuid"): group for group in bbmodel.get("groups", [])}
    parents = walk_outliner(bbmodel.get("outliner", []), groups_by_uuid)

    kept = []
    skipped = []
    for element in bbmodel.get("elements", []):
        uuid = element.get("uuid")
        names = parents.get(uuid, [])
        if "vfx" in names:
            skipped.append((uuid, "vfx-group"))
            continue
        if is_degenerate(element):
            skipped.append((uuid, "degenerate-plane"))
            continue
        kept.append(element)

    if not kept:
        raise ValueError("no model elements left after filtering")

    xs = []
    ys = []
    zs = []
    for element in kept:
        frm = [float(v) for v in element["from"]]
        to = [float(v) for v in element["to"]]
        xs.extend([frm[0], to[0]])
        ys.extend([frm[1], to[1]])
        zs.extend([frm[2], to[2]])

    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)
    min_z, max_z = min(zs), max(zs)
    range_x = max_x - min_x
    range_y = max_y - min_y
    range_z = max_z - min_z

    # Keep the Blockbench proportions, but fit the widest axis safely inside one block.
    scale = min(1.0, 15.5 / max(range_x, range_y, range_z))
    center_x = (min_x + max_x) / 2.0
    center_z = (min_z + max_z) / 2.0
    y_offset = 0.2

    def transform(point):
        x, y, z = [float(v) for v in point]
        return [
            round((x - center_x) * scale + 8.0, 4),
            round((y - min_y) * scale + y_offset, 4),
            round((z - center_z) * scale + 8.0, 4),
        ]

    elements = []
    for element in kept:
        model_element = {
            "from": transform(element["from"]),
            "to": transform(element["to"]),
            "faces": {},
        }
        rotation = element.get("rotation")
        if rotation:
            nonzero = [(axis, float(value)) for axis, value in zip(("x", "y", "z"), rotation) if abs(float(value)) > 0.001]
            if len(nonzero) == 1:
                axis, angle = nonzero[0]
                model_element["rotation"] = {
                    "origin": transform(element.get("origin", [8, 8, 8])),
                    "axis": axis,
                    "angle": round(angle, 4),
                    "rescale": True,
                }
        for direction, face in element.get("faces", {}).items():
            if face:
                model_element["faces"][direction] = face_to_json(face)
        elements.append(model_element)

    textures = bbmodel.get("textures", [])
    texture_size = [
        int(bbmodel.get("resolution", {}).get("width", textures[0].get("width", 64) if textures else 64)),
        int(bbmodel.get("resolution", {}).get("height", textures[0].get("height", 64) if textures else 64)),
    ]
    model = {
        "credit": "Converted from Crates and Stuff common_crate.bbmodel for LeeSeol Network",
        "texture_size": texture_size,
        "textures": {
            "0": "crates_and_stuff:block/common_crate_texture",
            "particle": "crates_and_stuff:block/common_crate_texture",
        },
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.35, 0.35, 0.35]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]},
        },
    }
    return model, skipped, scale


def extract_texture(bbmodel, destination):
    textures = bbmodel.get("textures", [])
    if not textures:
        raise ValueError("bbmodel has no textures")
    source = textures[0].get("source", "")
    if not source.startswith("data:image/png;base64,"):
        raise ValueError("texture is not embedded png base64")
    destination.write_bytes(base64.b64decode(source.split(",", 1)[1]))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--zip", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    if args.out.exists():
        shutil.rmtree(args.out)

    with zipfile.ZipFile(args.zip) as zf:
        entry = find_entry(zf, "BBModels/common_crate.bbmodel")
        bbmodel = json.loads(zf.read(entry).decode("utf-8"))

    content = args.out / "crates_and_stuff"
    configs = content / "configs"
    models = content / "models" / "block"
    textures = content / "textures" / "block"
    configs.mkdir(parents=True)
    models.mkdir(parents=True)
    textures.mkdir(parents=True)

    model, skipped, scale = build_model(bbmodel)
    (models / "common_crate.json").write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")
    extract_texture(bbmodel, textures / "common_crate_texture.png")

    blocks_yml = """info:
  namespace: crates_and_stuff

items:
  common_crate:
    display_name: Common Crate
    permission: crates_and_stuff.common_crate
    resource:
      material: PAPER
      generate: false
      model_path: block/common_crate
    specific_properties:
      block:
        placed_model:
          type: REAL_NOTE
          break_particles: ITEM
        hardness: 2
        drop_when_mined: true
"""
    (configs / "blocks.yml").write_text(blocks_yml, encoding="utf-8")

    summary = {
        "model": "common_crate",
        "elements_kept": len(model["elements"]),
        "elements_skipped": len(skipped),
        "skipped_reasons": skipped,
        "scale": scale,
        "output": str(content),
    }
    (args.out / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
