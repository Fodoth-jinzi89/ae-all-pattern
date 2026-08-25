# Test-world tooling

`generate.py` is currently a safe planning tool. It emits the intended test-lab manifest and never edits a Minecraft world.

The real generator will be added after the linker block, binding schema, and adapters stabilize. It must implement the backup, dry-run, empty-area scan, temporary-copy validation, version-specific NBT, and post-write assertions documented in `docs/testing/test-world-generation.md`.

Example:

```bash
python3 tools/testworld/generate.py --output build/testworld/lab-plan.json
```
