# Navigation Debug Overlay Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add dense Traveler navigation debug visibility through core snapshots, rendered overlays, and chat commands/messages.

**Architecture:** Core owns the debug data model and render primitives. `NavigationRuntime` writes snapshots after each produced control frame. Minecraft/Fabric only adapts the snapshot to chat and rendering.

**Tech Stack:** Java 21, Gradle, JUnit, Fabric client command/event adapters.

---

### Task 1: Core Navigation Debug Snapshot

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/debug/NavigationDebugSnapshot.java`
- Create: `modules/core/src/main/java/dev/traveler/core/debug/DebugTextFormatter.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/debug/PathfinderDebugState.java`
- Test: `modules/core/src/test/java/dev/traveler/core/debug/NavigationDebugSnapshotTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/debug/PathfinderDebugStateTest.java`

- [ ] Write tests for copying the latest navigation frame and producing chat/debug text.
- [ ] Run targeted tests and confirm red failure.
- [ ] Implement immutable navigation debug snapshot and debug state storage.
- [ ] Re-run tests and commit.

### Task 2: Runtime Debug Emission

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationRuntime.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/NavigationRuntimeTest.java`

- [ ] Write tests proving runtime stores frame snapshots and clears runtime debug on release.
- [ ] Run targeted tests and confirm red failure.
- [ ] Inject `PathfinderDebugState` into runtime and update it after control frames.
- [ ] Re-run tests and commit.

### Task 3: Render Overlay

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- Test: `modules/core/src/test/java/dev/traveler/core/render/PathDebugRenderModelTest.java`

- [ ] Write tests proving navigation snapshots add movement, target, and camera debug lines plus target boxes.
- [ ] Run targeted tests and confirm red failure.
- [ ] Add overlay render primitives from core snapshot data.
- [ ] Re-run tests and commit.

### Task 4: Chat Commands And Prints

**Files:**
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/DebugTravelerCommandFeature.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModule.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/NavigateTravelerCommandFeature.java`
- Modify if needed: Fabric source adapter tests.

- [ ] Write tests for `/traveler debug status`, `/traveler debug clear`, and navigation start/stop feedback text.
- [ ] Run targeted command tests and confirm red failure.
- [ ] Register debug command feature and ensure chat output includes current debug status.
- [ ] Re-run tests and commit.

### Task 5: Verification

- [ ] Run `./gradlew.bat check`.
- [ ] Scan for duplicate debug formatting or Minecraft imports in core.
- [ ] Commit final formatting if needed.
