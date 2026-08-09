---
paths:
  - "src/main/resources/**"
---

# Minecraft resource rules

- Keep asset ids under the `salmon` namespace and match Java registry ids exactly.
- Item definitions belong in `assets/salmon/items/`; block geometry belongs in `assets/salmon/models/block/`; block states belong in `assets/salmon/blockstates/`.
- Keep `ja_jp.json` and `en_us.json` synchronized for user-visible keys.
- When adding blocks, verify the relevant block/item model, blockstate, loot table, and mining/paint tags as applicable.
- Use the component-oriented item definition format already present in this 26.2 project; verify a neighboring working resource instead of recalling an older format.
- Run `./gradlew.bat processResources` after resource changes and `./gradlew.bat build` when Java registration also changed.
