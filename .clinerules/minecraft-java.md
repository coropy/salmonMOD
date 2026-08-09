---
paths:
  - "src/**/*.java"
  - "build.gradle"
  - "gradle.properties"
  - "settings.gradle"
---

# Minecraft 26.2 and Fabric Java rules

## API verification

- Resolve exact types and signatures from generated sources or the configured dependency sources before using web search.
- Common deobfuscated JAR: `%USERPROFILE%\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common-deobf\26.2\minecraft-common-deobf-26.2.jar`
- Client deobfuscated JAR: `%USERPROFILE%\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly-deobf\26.2\minecraft-clientonly-deobf-26.2.jar`
- If local evidence is insufficient, use official Fabric/Minecraft sources or migration documentation for version `26.2`; do not copy syntax from older releases without verification.

## Known 26.2 conventions

- Use `CustomPacketPayload` with Fabric play networking; keep codecs, registration, senders, and receivers aligned.
- Prefer `Identifier.fromNamespaceAndPath()` or `Identifier.parse()`.
- Commands use the three-argument `CommandRegistrationCallback` registration shape.
- Use `ServerPlayerEvents.AFTER_RESPAWN` for the existing respawn/dimension resynchronization flow; do not invent unavailable events.
- Serialize `Direction` through its string name when no direct codec is available.
- Treat `BlockPos` as immutable.
- Rendering uses the current `RenderPipeline`/`StagedVertexBuffer` path present in this repository. Verify current client source before changing draw submission or camera APIs.

## Verification

- Common/server Java only: `./gradlew.bat compileJava`
- Any client or networking change: `./gradlew.bat compileJava compileClientJava`
- Logic with tests: add `./gradlew.bat test`
- Registration/resource coupling or broad changes: use `./gradlew.bat build`

Do not run `genSources` routinely; use it only if required sources are absent or stale.
