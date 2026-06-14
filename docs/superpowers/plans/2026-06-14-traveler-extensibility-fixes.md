# Traveler Extensibility Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the highest-risk extensibility bottlenecks found in the Traveler architecture audit.

**Architecture:** Add extension points where future features currently require editing central classes. Keep behavior unchanged by extracting existing logic behind small interfaces and preserving current defaults.

**Tech Stack:** Java 21, Gradle, JUnit, JFR.

---

### Task 1: Surface Graph Connection Providers

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceConnectionProvider.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTraversalContext.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/navigation/AdjacentSurfaceConnectionProvider.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/navigation/DropSurfaceConnectionProvider.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/navigation/JumpSurfaceConnectionProvider.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTraversalGraph.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTraversalGraphSettings.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/surface/SurfaceTraversalGraphTest.java`

- [ ] **Step 1: Write failing tests**

Add tests that prove settings can accept a custom provider and that default providers still include walk, drop, and jump behavior.

- [ ] **Step 2: Run targeted tests**

Run: `./gradlew.bat :core:test --tests dev.traveler.core.world.surface.SurfaceTraversalGraphTest`

- [ ] **Step 3: Implement provider API**

Move the existing adjacent/drop/jump connection generation behind providers. Keep `SurfaceTraversalGraph` responsible for state, caches, and context methods only.

- [ ] **Step 4: Verify**

Run targeted surface graph tests, then `./gradlew.bat check`.

- [ ] **Step 5: Profile**

Run the smoothing JFR workload and inspect hot methods.

- [ ] **Step 6: Commit**

Commit: `refactor(path): make surface connections pluggable`

### Task 2: Route Search Pipeline Composition

**Files:**
- Create focused route search components if needed.
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/RouteSearchServiceTest.java`

- [ ] **Step 1: Write failing tests**

Add tests proving route search can receive a custom surface pathfinder or smoothing stage without changing `RouteSearchService`.

- [ ] **Step 2: Implement composition**

Extract graph creation, pathfinding, smoothing, and route conversion behind constructor-injected collaborators while preserving `standardClient()` behavior.

- [ ] **Step 3: Verify and commit**

Run route tests, full `check`, JFR if route hot path changed, then commit.

### Task 3: Minecraft Block Behavior Resolver Chain

**Files:**
- Create resolver interface/classes under `modules/mc/1_21_11/common/.../adapter/world` or `adapter/block`.
- Modify: `MinecraftSurfaceBlockAdapter.java`
- Test: common adapter tests.

- [ ] **Step 1: Write failing tests**

Prove a custom resolver can map a Minecraft block state to a custom core behavior before fallback logic.

- [ ] **Step 2: Implement resolver chain**

Move slab/stair/full/air/fluid detection out of the adapter into ordered resolvers.

- [ ] **Step 3: Verify and commit**

Run common tests and full `check`.
