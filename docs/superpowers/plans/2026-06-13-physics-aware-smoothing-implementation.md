# Physics-Aware Smoothing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make path smoothing validate direct surface shortcuts against the player's physical body footprint instead of a center-only line.

**Architecture:** Keep `PathSmoother` generic and unchanged. Strengthen the `SurfaceLineOfWalk` implementation in `core` so the smoothing predicate uses `MovementProfile` dimensions, body clearance, support continuity, and movement capabilities. Minecraft modules continue to provide world/block adapters only.

**Tech Stack:** Java 21, JUnit, existing core surface navigation classes.

---

### Task 1: Red Tests For Player Physics In Smoothing

**Files:**
- Modify: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceLineOfWalkTest.java`

- [x] Add a failing test where a smooth line is rejected because the player's body width clips a side wall even though center cells are walkable.
- [x] Keep vertical shortcut coverage through existing smoothing policy and command tests.
- [x] Run `./gradlew.bat :core:test --tests dev.traveler.core.world.navigation.SurfaceLineOfWalkTest`.

### Task 2: Surface Footprint Clearance

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceLineOfWalk.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTraversalGraph.java`
- Optional create: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceBodyFootprint.java`

- [x] Introduce a small body footprint helper using `MovementProfile.dimensions().width()`.
- [x] Check every footprint subcell at each smoothing sample for support, same-floor clearance, and body clearance.
- [x] Preserve existing constructors by adapting `MovementCapabilities` through `MovementProfiles.defaultPlayerWith(...)`.
- [x] Run the targeted surface line tests.

### Task 3: Wire Search Service To MovementProfile

**Files:**
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerPathSearchService.java`
- Modify tests if constructor expectations change.

- [x] Prefer `MovementProfile` when constructing surface smoothing.
- [x] Keep existing pathfinding capabilities behavior intact.
- [x] Run `./gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest`.

### Task 4: Verification And Commit

**Files:**
- No new production files beyond task 2.

- [x] Run `./gradlew.bat check`.
- [x] Run a quick JFR or profile workload for surface smoothing if available.
- [x] Inspect `git diff --check` and `git status --short`.
- [ ] Commit with `fix(core): validate smoothed paths against player physics`.
