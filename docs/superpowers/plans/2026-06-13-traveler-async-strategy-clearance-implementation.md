# Traveler Async Strategy Clearance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move expensive path searches off the client frame path, make movement inputs strategically use strafe/backpedal, and make surface paths prefer safer clearance around blocks.

**Architecture:** Keep `core` responsible for reusable job, steering, and clearance primitives. Minecraft common/fabric captures world data and applies results on the client thread; no worker should read a live Minecraft `BlockGetter`. Navigation continues to consume immutable `NavigationPath` and debug state remains synchronized.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit, Fabric client events, existing Traveler command framework.

---

### Task 1: Async Path Job Framework

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/job/PathJob.java`
- Create: `modules/core/src/main/java/dev/traveler/core/job/PathJobExecutor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/job/PathJobHandle.java`
- Create: `modules/core/src/main/java/dev/traveler/core/job/PathJobResult.java`
- Create: `modules/core/src/main/java/dev/traveler/core/job/PathJobState.java`
- Test: `modules/core/src/test/java/dev/traveler/core/job/PathJobExecutorTest.java`

- [x] Write failing tests that verify a job starts as queued, completes on a `Traveler-Pathfinder` worker thread, stores a result, and can cancel a queued job.
- [x] Implement immutable job metadata, handle state transitions, single-thread executor, cancellation, and shutdown.
- [x] Run `./gradlew.bat :core:test --tests dev.traveler.core.job.PathJobExecutorTest`.
- [x] Commit `feat(core): add async path job executor`.

### Task 2: Immutable Minecraft Search Snapshot

**Files:**
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/adapter/world/ImmutableMinecraftWorldSnapshot.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerPathSearchService.java`
- Test: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModuleTest.java`

- [x] Write failing tests that verify snapshot-backed searches do not call the live world after capture.
- [x] Capture all blocks inside the existing search bounds on the client thread into an immutable map of `SurfaceBlock` values.
- [x] Route worker searches through the immutable snapshot and keep direct block fallback behavior for tests without a world.
- [x] Run `./gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest`.
- [x] Commit `feat(mc): capture immutable path search snapshots`.

### Task 3: Async Commands And Navigation Completion

**Files:**
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModule.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/PathTravelerCommandFeature.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/NavigateTravelerCommandFeature.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerPathJobService.java`
- Modify: `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/event/FabricEventBootstrap.java`
- Test: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModuleTest.java`

- [x] Write failing tests that `/traveler path block ...` returns a queued message immediately and later updates `PathfinderDebugState`.
- [x] Add a job service that cancels previous jobs by purpose, enqueues worker search, and drains completed results on the client tick/render callback.
- [x] Make `/traveler navigate block ...` start navigation only after a successful async result is drained.
- [x] Ensure failed/cancelled jobs produce chat/debug messages without starting navigation.
- [x] Run `./gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest`.
- [x] Commit `feat(mc): run path commands asynchronously`.

### Task 4: Strategic Movement Modes

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementVectorPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementVectorSettings.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/plan/MovementVectorPolicyTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/control/ControlProjectorTest.java`

- [x] Write failing tests for `FORWARD_ARC` behavior: medium angle should request `DIRECT` with meaningful strafe, not pure forward.
- [x] Write failing tests for `STRAFE_TURN`: wide angle should strafe while the camera catches up instead of waiting.
- [x] Write failing tests for `SIDESTEP_RECENTER`: outside corridor should choose lateral correction without special actions.
- [x] Implement mode selection based on forward dot, side dot, desired distance, lateral correction, and action setup.
- [x] Run `./gradlew.bat :core:test --tests dev.traveler.core.navigation.plan.* --tests dev.traveler.core.navigation.control.*`.
- [x] Commit `feat(core): add strategic movement projection`.

### Task 5: Clearance-Aware Surface Paths

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceClearanceScorer.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceLineOfWalk.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceLineOfWalkSettings.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceSmoothingPolicy.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceClearanceScorerTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceLineOfWalkTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceSmoothingPolicyTest.java`

- [x] Write failing tests for a corner block near the right side of a planned line: smoothing must reject the shortcut.
- [x] Write failing tests that nodes near diagonal/corner blocked cells receive higher cost than open nodes.
- [x] Make `SurfaceClearanceScorer` footprint-aware across cardinals and diagonals.
- [x] Add a smoothing safety margin so the line-of-walk checks a capsule wider than exact player width.
- [x] Preserve angular landmarks when a sharp turn is near blocked body space.
- [x] Run `./gradlew.bat :core:test --tests dev.traveler.core.world.navigation.*`.
- [x] Commit `fix(core): prefer comfortable surface clearance`.

### Task 6: Anti-Wall Steering And Debug Signals

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/steering/PathSteeringController.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/steering/PathSteeringSettings.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/debug/DebugTextFormatter.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/steering/PathSteeringControllerTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/debug/DebugTextFormatterTest.java`

- [x] Write failing tests for short-range anti-wall correction when lateral error or clearance risk is high.
- [x] Add debug text fields for phase, mode, special action allowed, lateral error, and clearance warning when available.
- [x] Tint risky debug nodes differently if render model already exposes per-node material; otherwise include risk in chat/debug text only.
- [x] Run `./gradlew.bat :core:test --tests dev.traveler.core.navigation.* --tests dev.traveler.core.debug.* --tests dev.traveler.core.render.*`.
- [x] Commit `feat(core): expose anti-wall navigation debug`.

### Task 7: Final Verification

**Files:**
- No production edits expected.

- [x] Run `./gradlew.bat check`.
- [x] Run the surface smoothing JFR workload and inspect hot methods.
- [x] Run `git status --short` and `git log --oneline -6`.
- [x] If all checks pass, report remaining known risk: async snapshot size and worker cancellation behavior should be tuned with real in-game traces.

**Verification notes:**
- `./gradlew.bat check` passed after async commands, strategic movement, clearance, anti-wall debug, and jump transition fixes.
- JFR smooth workload: `found=25 searches=25 mode=smooth millis=5076 blockReads=1074185`.
- Hot methods after micro-optimization are expected footprint/cache costs: `SurfaceTraversalGraph.collidesWithFootprintColumn`, `SurfaceBlockCache.cached`, and A* map access. The artificial `isOwnSupportBlock` hotspot was removed.
