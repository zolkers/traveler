# Navigation Architecture Realignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Realign Traveler client navigation with `docs/architecture/traveler-navigation-architecture.md`.

**Architecture:** Move frame behavior into `dev.traveler.core.navigation.plan`, keep key/camera output mapping in `dev.traveler.core.navigation.control`, and make `NavigationController` orchestration-only. Existing route, steering, and locomotion pieces may be reused only when they serve a single layer.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit, Checkstyle, JaCoCo, Fabric client adapter.

---

### Task 1: Planner Contracts

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationPhase.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/PlannedMovementMode.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/ActionIntent.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementVectorIntent.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/SpeedIntent.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/ToleranceProfile.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlan.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/plan/NavigationFramePlanTest.java`

- [ ] Write record validation tests for immutable frame plan fields.
- [ ] Run `./gradlew.bat :core:test --tests dev.traveler.core.navigation.plan.NavigationFramePlanTest` and confirm the test fails before implementation.
- [ ] Implement records/enums with validation and no Minecraft dependencies.
- [ ] Re-run the targeted test and commit.

### Task 2: Navigation Planner

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/RouteProgressPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementActionPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementVectorPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/ActionTimingPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/CameraTargetPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/plan/NavigationFramePlannerTest.java`

- [ ] Write tests proving the planner owns phases, jump timing, camera target choice, route progress, turn-strafe, and recovery.
- [ ] Run the targeted planner tests and confirm red failures.
- [ ] Implement the policies using existing route, steering, and locomotion value objects where appropriate.
- [ ] Re-run planner tests and commit.

### Task 3: Control Projection

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjectionFrame.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjectionSettings.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/control/ControlProjectorTest.java`

- [ ] Write tests proving projection maps an existing frame plan to keys/camera without deciding jump, recovery, or route progress.
- [ ] Run targeted control tests and confirm red failures.
- [ ] Implement projection, camera smoothing, input hysteresis, and yaw shortest-path safety.
- [ ] Re-run control tests and commit.

### Task 4: Controller And Adapter Realignment

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationController.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationControlFrame.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationControllerState.java`
- Modify: `modules/core/src/test/java/dev/traveler/core/navigation/NavigationControllerTest.java`
- Modify if compile requires it: `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/v1_21_11/fabric/navigation/MinecraftClientNavigationAdapter.java`

- [ ] Update controller tests to assert orchestration behavior and expose the frame plan.
- [ ] Run controller tests and confirm red failures.
- [ ] Rewire `NavigationController` to call `NavigationFramePlanner` then `ControlProjector`.
- [ ] Ensure the Minecraft adapter only applies `NavigationControlFrame` outputs.
- [ ] Re-run targeted tests and commit.

### Task 5: Cleanup And Verification

**Files:**
- Remove or deprecate unused old behavior classes only after `rg` proves no production use.
- Modify architecture tests if package direction needs explicit enforcement.

- [ ] Run `rg` checks for old duplicated decision classes.
- [ ] Run `./gradlew.bat :core:test`.
- [ ] Run `./gradlew.bat check`.
- [ ] Run a quick JFR/profile workload for path search and navigation hotspots where available.
- [ ] Commit the cleanup.
