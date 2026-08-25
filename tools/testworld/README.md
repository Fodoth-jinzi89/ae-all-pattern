# Test-world tooling

`generate.py` emits the deterministic manual test-lab manifest and never edits a Minecraft world. Automated in-world coverage is provided separately by the generated `empty.nbt` GameTest structure and `CoreGameTests`.

`build_lab_staging.py` creates a new staging directory containing the test datapack, guide, and 1001 reload/stress recipes. It refuses an existing target and never writes region or level NBT. Minecraft must own those writes:

```bash
python3 tools/testworld/build_lab_staging.py --world run/aeallpattern_test_lab
./gradlew runServer
# In the server console after startup:
function aeallpattern_test:build
save-all flush
stop
```

The generated lab contains a powered AE2 core, linker, crafting CPU, installed 64k cell, ready crafting/encoding terminals, pre-fueled vanilla cooking stations, Mekanism single machines and factories, labeled diagnostics, materials, and deterministic recipe stress data. Machine adapters find a valid input side, and every stack exposed by a bound machine's output capability returns to the same ME network automatically.

Direct save editing is intentionally out of scope: the game-owned GameTest path avoids unsafe region mutation. Any future offline generator must implement the backup, dry-run, empty-area scan, temporary-copy validation, version-specific NBT, and post-write assertions documented in `docs/testing/test-world-generation.md`.

Manifest-only example:

```bash
python3 tools/testworld/generate.py --output build/testworld/lab-plan.json
```
