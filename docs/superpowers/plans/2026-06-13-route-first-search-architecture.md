# Route-First Search Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development for each code task. Use review checkpoints after each task if subagents are available and authorized.

**Goal:** Move path search semantics from Minecraft common into core route services, preserve per-segment movement actions, and make Minecraft common a thin adapter.

**Architecture:** `core.route` produces `RouteSearchResult`; `core.world.navigation` evaluates movement decisions once; `NavigationPath` can carry route actions; `mc/common` formats and applies results.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit, Checkstyle, JaCoCo, Fabric client adapter.

---

### Task 1: Documentation Checkpoint

**Files:**
- Create: `docs/superpowers/specs/2026-06-13-route-first-search-architecture-design.md`
- Create: `docs/superpowers/plans/2026-06-13-route-first-search-architecture.md`

- [ ] Commit the spec and plan with a docs conventional commit.

### Task 2: Route Model

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteStep.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/RoutePath.java`
- Create: `modules/core/src/test/java/dev/traveler/core/route/RoutePathTest.java`

- [ ] Write failing tests for ordered points, actions, cost, and validation.
- [ ] Implement immutable route records.
- [ ] Run targeted core route tests and commit.

### Task 3: Shared Movement Decisions

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/decision/MovementAction.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/decision/MovementDecision.java`
- Create or modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceMovementEvaluator.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTraversalGraph.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceMovementEvaluatorTest.java`

- [ ] Write failing tests proving safe drops are semantic `DROP` and graph/evaluator agree.
- [ ] Centralize movement decision evaluation.
- [ ] Run targeted surface navigation tests and commit.

### Task 4: Route Search Service

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchSettings.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchDiagnostics.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchFailureReason.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/RouteSearchServiceTest.java`

- [ ] Write failing tests for found route, no start surface, no goal surface, and direct fallback.
- [ ] Move A*, surface smoothing, block fallback, and diagnostics into core.
- [ ] Preserve existing client search bounds and capabilities.
- [ ] Run targeted core route tests and commit.

### Task 5: Minecraft Adapter Rewire

**Files:**
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerPathSearchService.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerPathSearchResult.java`
- Modify relevant command/debug tests.

- [ ] Write or update tests proving command search delegates to `RouteSearchService`.
- [ ] Remove direct A*, graph, smoother, and surface traversal imports from command code.
- [ ] Preserve async snapshot capture and chat/debug updates.
- [ ] Run targeted common tests and commit.

### Task 6: Navigation Route Actions

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/follow/NavigationPath.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementActionPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- Test: relevant navigation planner tests.

- [ ] Write failing tests proving route actions drive jump/drop plans.
- [ ] Thread per-segment actions through planner.
- [ ] Keep point-only navigation path compatibility.
- [ ] Run targeted navigation tests and commit.

### Task 7: Guards, Profiling, Cleanup

**Files:**
- Modify architecture tests under `modules/*/src/test`.
- Remove dead code only after `rg` proves no production usage.

- [ ] Add architecture guard for `mc/common/command` forbidden search internals.
- [ ] Run duplication/dead-code scans with `rg`.
- [ ] Run `./gradlew.bat :core:test`, `./gradlew.bat :mc_1_21_11_common:test`, and `./gradlew.bat check`.
- [ ] Run a quick JFR/flamegraph workload for path search.
- [ ] Commit cleanup.

