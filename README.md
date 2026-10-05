# Cobbleworks

Minecraft **26.1.2**, NeoForge **26.1.2.108**, Java **25**.

A bucket-powered infinite cobblestone generator with a custom animated inventory,
automatic transport output and an original Blockbench model.

## Machine behavior

- Slot 0: one reusable water bucket. Slot 1: one reusable lava bucket.
- Slots 2–10: extraction-only cobblestone output, up to 576 items.
- Default production: one cobble per 40 ticks; server settings allow 1–64 per cycle.
- Top accepts buckets; sides accept buckets and extract output; bottom extracts output.
- Unsided NeoForge capability exposes output only. Bucket extraction is manual only.
- **Production always requires an active redstone signal.** No signal means paused.
- Output is always pushed (up to eight items every eight ticks) into a connected
  inventory/transport below; no toggle. Full output pauses generation without
  discarding a batch. Comparator reports output fill.
- Inventory changes immediately synchronize the two visible reservoir blockstates.
- Compact 176x206 panel: animated reservoir columns above their bucket slots, a linked
  progress bar, one power toggle and a small top-right status indicator
  (waiting/running/paused/full).
- Contents, progress and the power toggle persist. Breaking drops contents separately.
- No energy/fluid capability, offline production, chunk loader or claimed universal
  compatibility with every third-party pipe mod.

## Build and test

```sh
./gradlew buildAndCollect --offline --console=plain
./gradlew runGameTestServer --offline --console=plain
./gradlew runClient --offline --console=plain
```

`check` executes dependency-free policy assertions. The explicit GameTest run
registers real-server integration tests for production, buckets, capacity, mandatory
redstone, transactions, save/reload, hoppers, automatic transport, drops and reservoir
blockstates. Test registration is disabled in ordinary game sessions.

## Artwork workflow

Open `art/blockbench/cobble_generator.bbmodel` in Blockbench. Geometry and the
128px atlas were created/painted via Blockbench MCP. Export Java geometry to
`art/blockbench/cobble_generator.json` and save the painted atlas to
`src/main/resources/assets/cobbleworks/textures/block/cobbleworks_atlas.png`.

```sh
python3 tools/export_blockbench_states.py
```

This packages the authored model into 64 facing/phase/bucket combinations and
derives scrolling animation frames from the painted fluid tiles. It also checks
for intersecting cuboids. Do not regenerate machine geometry from placeholder art.

CurseForge copy and publication artwork are in `curseforge/`. No upload is performed.
