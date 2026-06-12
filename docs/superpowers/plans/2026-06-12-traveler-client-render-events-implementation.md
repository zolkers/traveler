# Traveler Client Render Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the next client-side foundation: a loader-neutral Traveler event bus plus a Fabric world-render debug path renderer that visualizes the latest path without moving the player.

**Architecture:** `common` owns Minecraft-version client concepts that are shared by loaders: event dispatch contracts, debug render snapshots, and render command preparation. `fabric` owns only Fabric callbacks and the concrete world-render bridge. `core` stays untouched and pure.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Minecraft 1.21.11 Mojmap through Loom, Fabric API world-render callbacks, JUnit 6.1.0, Checkstyle nesting max 2.

---

## File Structure

- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEvent.java`
  - Small synchronous event list with deterministic listener order.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEventListener.java`
  - Functional listener contract.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEventSubscription.java`
  - Closeable registration handle.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerClientEvents.java`
  - Shared client event registry.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/ClientTickEvent.java`
  - Client tick payload.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/WorldRenderEvent.java`
  - World-render payload with partial tick and camera position.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/ColorRgba.java`
  - Immutable color value with channel validation.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/RenderVertex.java`
  - World-space vertex.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/DebugLine.java`
  - One colored 3D line segment.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/DebugRenderFrame.java`
  - Immutable list of lines for a frame.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/PathDebugRenderModel.java`
  - Converts `PathfinderDebugSnapshot` into centered block-to-block line commands.
- Modify `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/debug/PathfinderDebugState.java`
  - Add `snapshot()` read helper for render code.
- Modify `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/FabricEventBootstrap.java`
  - Register Fabric client tick and world-render callbacks.
- Create `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/FabricPathDebugRenderer.java`
  - Converts common debug lines into Fabric/Minecraft render calls.
- Modify `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/TravelerFabricClientMod.java`
  - Wire `PathDebugRenderModel` into event bootstrap.

## Task 1: Common Event Bus

**Files:**
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEvent.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEventListener.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerEventSubscription.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/TravelerClientEvents.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/ClientTickEvent.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/event/WorldRenderEvent.java`
- Test: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/event/TravelerEventTest.java`

- [ ] **Step 1: Write failing tests**

Test listener order, unsubscribe behavior, snapshot-safe dispatch, and null rejection.

- [ ] **Step 2: Run RED**

Run: `.\gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.event.TravelerEventTest`

Expected: fail because event classes do not exist.

- [ ] **Step 3: Implement minimal event bus**

Use `CopyOnWriteArrayList`, listener priorities, and `AutoCloseable` subscriptions. Keep dispatch synchronous and max nesting depth <= 2.

- [ ] **Step 4: Run GREEN**

Run the same targeted test until it passes, then run `.\gradlew.bat :mc_1_21_11_common:check`.

- [ ] **Step 5: Commit**

Commit message: `feat(mc-common): add traveler client event bus`

## Task 2: Common Debug Render Model

**Files:**
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/ColorRgba.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/RenderVertex.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/DebugLine.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/DebugRenderFrame.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/render/PathDebugRenderModel.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/debug/PathfinderDebugState.java`
- Test: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/render/PathDebugRenderModelTest.java`

- [ ] **Step 1: Write failing tests**

Test that empty/no-result snapshots produce no lines, a 3-node path produces 2 centered colored line segments, and invalid color channels fail.

- [ ] **Step 2: Run RED**

Run: `.\gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.render.PathDebugRenderModelTest`

Expected: fail because render classes do not exist.

- [ ] **Step 3: Implement render model**

Keep the render model immutable and common-loader-neutral. It may import `BlockPosition` and common debug classes, but must not import Fabric.

- [ ] **Step 4: Run GREEN**

Run the targeted test and `.\gradlew.bat :mc_1_21_11_common:check`.

- [ ] **Step 5: Commit**

Commit message: `feat(mc-common): add path debug render model`

## Task 3: Fabric World Render Bridge

**Files:**
- Create: `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/FabricPathDebugRenderer.java`
- Modify: `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/FabricEventBootstrap.java`
- Modify: `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/TravelerFabricClientMod.java`
- Test: `modules/mc/1_21_11/fabric/src/test/java/dev/traveler/mc/v1_21_11/fabric/FabricEventBootstrapTest.java`

- [ ] **Step 1: Write failing tests**

Test that Fabric bootstrap emits Traveler tick events and delegates render events to `FabricPathDebugRenderer`.

- [ ] **Step 2: Run RED**

Run: `.\gradlew.bat :mc_1_21_11_fabric:test --tests dev.traveler.mc.v1_21_11.fabric.FabricEventBootstrapTest`

Expected: fail because bootstrap is still a placeholder.

- [ ] **Step 3: Implement Fabric bridge**

Register `ClientTickEvents.END_CLIENT_TICK` and `WorldRenderEvents.END_MAIN`. The concrete renderer should use Minecraft's line rendering path with the `RenderTypes.lines()` shader pipeline exposed by Mojmap.

- [ ] **Step 4: Run GREEN**

Run the targeted Fabric test and `.\gradlew.bat :mc_1_21_11_fabric:build`.

- [ ] **Step 5: Commit**

Commit message: `feat(fabric): render debug paths in world`

## Task 4: Final Verification

**Files:**
- Modify only if verification exposes a real issue.

- [ ] **Step 1: Run full checks**

Run: `.\gradlew.bat clean check`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Fabric build**

Run: `.\gradlew.bat :mc_1_21_11_fabric:build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Review architecture guards**

Ensure `core` remains unchanged or pure, `common` has no Fabric/NeoForge imports, and `fabric` only bootstraps common render/event logic.

- [ ] **Step 4: Commit verification fixes if needed**

Commit only concrete fixes found by verification.
