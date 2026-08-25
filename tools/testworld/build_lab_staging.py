#!/usr/bin/env python3
"""Create a new AE All Pattern test-lab datapack staging directory.

This intentionally writes no region or level NBT. Minecraft must load the new
world and execute aeallpattern_test:build so the game owns every world write.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


PACK_FORMAT = 48
NAMESPACE = "aeallpattern_test"


def sign(x: int, y: int, z: int, *lines: str, color: str = "dark_purple") -> str:
    messages = []
    for line in (*lines, "", "", "", "")[:4]:
        component = json.dumps({"text": line, "color": color}, ensure_ascii=False, separators=(",", ":"))
        messages.append("'" + component.replace("'", "\\'") + "'")
    return (
        f"setblock {x} {y} {z} minecraft:oak_sign[rotation=8]"
        f"{{front_text:{{messages:[{','.join(messages)}]}}}} replace"
    )


def barrel(commands: list[str], x: int, y: int, z: int, items: list[tuple[str, int]]) -> None:
    commands.append(f"setblock {x} {y} {z} minecraft:barrel[facing=up] replace")
    for slot, (item, count) in enumerate(items):
        commands.append(f"item replace block {x} {y} {z} container.{slot} with {item} {count}")


def build_commands() -> list[str]:
    commands = [
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule keepInventory true",
        "gamerule spawnRadius 0",
        "gamerule doMobSpawning false",
        "time set day",
        "weather clear",
        "kill @e[type=minecraft:item,distance=..96]",
        "fill -24 5 -18 24 18 24 minecraft:air replace",
        "fill -24 4 -18 24 4 24 minecraft:smooth_stone replace",
        "fill -22 4 -16 22 4 -10 minecraft:purple_concrete replace",
        "fill -22 4 -8 22 4 2 minecraft:light_gray_concrete replace",
        "fill -22 4 4 22 4 10 minecraft:orange_concrete replace",
        "fill -22 4 12 22 4 22 minecraft:cyan_concrete replace",
        "setworldspawn 0 5 -14",
        "forceload add -24 -18 24 24",
        sign(0, 5, -15, "AE All Pattern", "0.1.0 Test Lab", "Creative + Peaceful", "Read guide file"),
        sign(-16, 5, -11, "PURPLE ZONE", "AE2 core", "Linker + CPU", "Supplies nearby"),
        sign(-16, 5, 3, "ORANGE ZONE", "Vanilla furnaces", "Fuel included", "AE auto-return"),
        sign(-16, 5, 11, "CYAN ZONE", "Mekanism", "Singles + factories", "Creative power"),
        sign(14, 5, -7, "DIAGNOSTICS", "/aeallpattern status", "/aeallpattern perf", "/reload"),
        sign(14, 5, -3, "STRESS PACK", "1000 smelting", "recipe files", "reload + perf"),
        # AE2 core and a compact 2x2 crafting CPU.
        "setblock 0 5 -5 ae2:controller replace",
        "setblock -1 5 -5 ae2:creative_energy_cell replace",
        "setblock 1 5 -5 ae2:drive replace",
        "data merge block 1 5 -5 {inv:{item0:{id:\"ae2:item_storage_cell_64k\",count:1}}}",
        "setblock 0 6 -5 ae2:cable_bus{cable:{id:\"ae2:fluix_glass_cable\"},north:{id:\"ae2:crafting_terminal\"}} replace",
        "setblock 1 6 -5 ae2:cable_bus{cable:{id:\"ae2:fluix_glass_cable\"},north:{id:\"ae2:pattern_encoding_terminal\"}} replace",
        "setblock 0 5 -4 aeallpattern:pattern_linker replace",
        "setblock 0 5 -6 ae2:1k_crafting_storage replace",
        "setblock 1 5 -6 ae2:crafting_unit replace",
        "setblock 0 6 -6 ae2:crafting_unit replace",
        "setblock 1 6 -6 ae2:crafting_monitor replace",
        "setblock 2 5 -5 ae2:molecular_assembler replace",
        "setblock 2 5 -6 ae2:interface replace",
        "setblock -2 5 -5 ae2:pattern_provider replace",
        sign(0, 7, -7, "AE CORE", "2 terminals ready", "Linker uses channel", "CPU + 64k drive"),
        sign(-7, 5, -7, "STEP 1", "Take binder", "Right-click linker", "No sneak"),
        sign(7, 5, -7, "STEP 2", "Sneak-right-click", "a machine input", "within 64 blocks"),
    ]

    barrel(commands, -5, 5, -5, [
        ("aeallpattern:pattern_binder", 1),
        ("aeallpattern:pattern_linker", 8),
        ("ae2:fluix_glass_cable", 64),
        ("ae2:crafting_terminal", 4),
        ("ae2:pattern_encoding_terminal", 2),
        ("ae2:import_bus", 16),
        ("ae2:export_bus", 16),
        ("ae2:storage_bus", 16),
        ("ae2:certus_quartz_wrench", 1),
        ("ae2:blank_pattern", 64),
        ("ae2:item_storage_cell_64k", 1),
        ("ae2:controller", 8),
        ("ae2:creative_energy_cell", 8),
    ])

    # Vanilla stations. Hopper below each machine demonstrates deterministic output extraction.
    vanilla = [
        (-12, "minecraft:furnace", "FURNACE"),
        (-6, "minecraft:blast_furnace", "BLAST FURNACE"),
        (0, "minecraft:smoker", "SMOKER"),
    ]
    for x, block, label in vanilla:
        commands.extend([
            f"setblock {x} 6 7 {block}[facing=south] replace",
            f"setblock {x} 5 7 minecraft:smooth_stone replace",
            f"item replace block {x} 6 7 container.1 with minecraft:coal 64",
            sign(x, 7, 6, label, "Bind any face", "Fuel preloaded", "Auto-return to ME"),
        ])
    barrel(commands, 6, 5, 7, [
        ("minecraft:raw_iron", 64),
        ("minecraft:raw_gold", 64),
        ("minecraft:raw_copper", 64),
        ("minecraft:coal", 64),
        ("minecraft:oak_log", 64),
        ("minecraft:beef", 64),
        ("minecraft:potato", 64),
        ("minecraft:cobblestone", 64),
    ])

    # Mekanism single machines and matching basic factories, each on creative power.
    mekanism_rows = [
        (-12, "mekanism:energized_smelter", "SMELTER"),
        (-6, "mekanism:crusher", "CRUSHER"),
        (0, "mekanism:enrichment_chamber", "ENRICHER"),
    ]
    for x, block, label in mekanism_rows:
        commands.extend([
            f"setblock {x} 5 15 mekanism:creative_energy_cube replace",
            f"data merge block {x} 5 15 {{energy_containers:[{{container:0b,stored:9223372036854775807L}}]}}",
            f"setblock {x} 6 15 {block} replace",
            f"data merge block {x} 6 15 {{energy_containers:[{{container:0b,stored:9223372036854775807L}}]}}",
            sign(x, 7, 14, label, "Precharged", "Bind any face", "Auto-return to ME"),
        ])
    factories = [
        (-12, "mekanism:basic_smelting_factory", "SMELT FACTORY"),
        (-6, "mekanism:basic_crushing_factory", "CRUSH FACTORY"),
        (0, "mekanism:basic_enriching_factory", "ENRICH FACTORY"),
    ]
    for x, block, label in factories:
        commands.extend([
            f"setblock {x} 5 20 mekanism:creative_energy_cube replace",
            f"data merge block {x} 5 20 {{energy_containers:[{{container:0b,stored:9223372036854775807L}}]}}",
            f"setblock {x} 6 20 {block} replace",
            f"data merge block {x} 6 20 {{energy_containers:[{{container:0b,stored:9223372036854775807L}}]}}",
            sign(x, 7, 19, label, "Precharged", "Bind any face", "Auto-return to ME"),
        ])
    barrel(commands, 6, 5, 15, [
        ("minecraft:raw_iron", 64),
        ("minecraft:raw_gold", 64),
        ("minecraft:cobblestone", 64),
        ("minecraft:quartz", 64),
        ("minecraft:redstone", 64),
        ("minecraft:diamond", 64),
        ("mekanism:raw_osmium", 64),
        ("mekanism:raw_tin", 64),
        ("mekanism:raw_lead", 64),
        ("mekanism:configurator", 1),
    ])
    commands.extend([
        sign(13, 5, 12, "CHECKLIST", "Bind + purple box", "Craft + return", "Restart + reload"),
        "setblock 14 5 2 minecraft:command_block{Command:\"aeallpattern perf\",auto:0b} replace",
        "setblock 14 6 2 minecraft:stone_button[face=floor] replace",
        "setblock 18 5 2 minecraft:command_block{Command:\"reload\",auto:0b} replace",
        "setblock 18 6 2 minecraft:stone_button[face=floor] replace",
        sign(14, 5, 1, "PERF BUTTON", "Runs diagnostics", "Output in chat/log"),
        sign(18, 5, 1, "RELOAD BUTTON", "Reload datapacks", "Catalog generation", "must increment"),
        "spawnpoint @a 0 5 -14",
        "gamemode creative @a",
        "effect give @a minecraft:night_vision infinite 0 true",
        "tellraw @a {\"text\":\"AE All Pattern 0.1.0 test lab generated. Read AE_ALL_PATTERN_TEST_GUIDE.md in the save folder.\",\"color\":\"light_purple\"}",
    ])
    return commands


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", type=Path, required=True, help="new, non-existing world directory")
    args = parser.parse_args()
    world = args.world.resolve()
    if world.exists():
        raise SystemExit(f"refusing to modify existing path: {world}")

    pack = world / "datapacks" / "aeallpattern_test"
    write_json(pack / "pack.mcmeta", {
        "pack": {
            "pack_format": PACK_FORMAT,
            "description": "AE All Pattern 0.1.0 deterministic test lab",
        }
    })
    function_path = pack / "data" / NAMESPACE / "function" / "build.mcfunction"
    function_path.parent.mkdir(parents=True, exist_ok=True)
    function_path.write_text("\n".join(build_commands()) + "\n", encoding="utf-8")

    recipe_dir = pack / "data" / NAMESPACE / "recipe"
    for index in range(1000):
        write_json(recipe_dir / f"stress_{index:04d}.json", {
            "type": "minecraft:smelting",
            "category": "misc",
            "ingredient": {"item": "minecraft:cobblestone"},
            "result": {"id": "minecraft:stone", "count": 1},
            "experience": 0.0,
            "cookingtime": 20,
        })
    write_json(recipe_dir / "same_output_from_stone_bricks.json", {
        "type": "minecraft:smelting",
        "category": "misc",
        "ingredient": {"item": "minecraft:stone_bricks"},
        "result": {"id": "minecraft:stone", "count": 1},
        "experience": 0.0,
        "cookingtime": 40,
    })

    guide = """# AE All Pattern 0.1.0 Test Lab

Generated for Minecraft 1.21.1 / NeoForge 21.1.219 / AE2 19.2.17 / Mekanism 10.7.19.

## First use

1. Open the world in Creative mode and walk to the purple AE2 zone.
2. Take the All Pattern Binder from the supply barrel.
3. Right-click the powered All Pattern Linker without sneaking.
4. Sneak-right-click any face of a supported machine. The adapter prefers that face and safely finds a valid item input when needed.
5. Confirm the purple outline and inspect craftable items in an AE2 crafting terminal.
6. The linker continuously drains every stack exposed by the bound machine's output capability into the same ME network.

## Stations

- Purple: powered AE2 controller, linker, installed 64k storage cell, ready-to-use crafting and pattern encoding terminals, compact crafting CPU, assembler, interface, and supplies.
- Orange: furnace, blast furnace, smoker, preloaded fuels, inputs, and unobstructed outputs for Linker auto-return.
- Cyan: energized smelter, crusher, enrichment chamber, their basic factories, precharged power, and materials.
- Gray diagnostics: `/aeallpattern status`, `/aeallpattern perf`, and `/reload` buttons.

## Required checks

- Binding survives save/restart and disappears on unbind.
- Missing channel/power stops publication.
- Replacing a machine safely invalidates its route.
- Blocked input keeps ownership in the linker; unbinding or breaking it recovers queued material.
- A full ME drive leaves completed output in the machine; recovery retries after storage becomes available.
- `/reload` increases recipe generation once and does not create duplicate virtual patterns.
- The 1000 duplicate stress recipes load but collapse safely through deterministic filtering.

KubeJS is not bundled. Use the included datapack reload probe for this dependency matrix.
"""
    (world / "AE_ALL_PATTERN_TEST_GUIDE.md").write_text(guide, encoding="utf-8")
    write_json(world / "lab-plan.json", {
        "schema": 1,
        "world": world.name,
        "minecraft": "1.21.1",
        "mod_version": "0.1.0",
        "function": f"{NAMESPACE}:build",
        "stress_recipes": 1000,
        "writes_region": False,
    })
    print(f"Created safe staging directory: {world}")
    print(f"Next: start Minecraft server and run `function {NAMESPACE}:build`")


if __name__ == "__main__":
    main()
