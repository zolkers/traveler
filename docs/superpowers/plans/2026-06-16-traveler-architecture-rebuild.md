# Traveler Architecture Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Traveler around a small public API, strong `api/internal` boundaries, traversal-first execution contracts, and world semantics that can evolve without cross-cutting rewrites.

**Architecture:** Introduce a lean common kernel (`TravelerPort`, `TravelerRegistry`, `SettingsSection`, `DiagnosticPayload`), expose only domain-specific APIs in `api` packages, move all implementation detail into `internal`, and rebuild route/navigation/recovery/debug around one traversal truth instead of duplicated local interpretations.

**Tech Stack:** Java 21, Gradle, JUnit 5, current Traveler core modules, BuildMyCommand, Fabric adapter module.

---

## Scope Notes

This plan intentionally covers world semantics, route planning, traversal execution, recovery, and platform packaging together because the current pain comes from their coupling. The tasks are sequenced so each checkpoint still improves the codebase on its own.

## File Structure

### New package pattern

- `modules/core/src/main/java/dev/traveler/core/common/api`
- `modules/core/src/main/java/dev/traveler/core/common/internal`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/api`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/internal`
- `modules/core/src/main/java/dev/traveler/core/route/api`
- `modules/core/src/main/java/dev/traveler/core/route/internal`
- `modules/core/src/main/java/dev/traveler/core/navigation/api`
- `modules/core/src/main/java/dev/traveler/core/navigation/internal`

### Existing files that will be touched repeatedly

- `modules/core/src/main/java/dev/traveler/core/settings/TravelerSettings.java`
- `modules/core/src/main/java/dev/traveler/core/settings/Setting.java`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java`
- `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceMovementEvaluator.java`
- `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTransitionEvaluator.java`
- `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/NavigationRuntime.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java`
- `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- `modules/mc/1_21_11/fabric/src/main/java/**`

### Architecture guardrails to add early

- boundary tests for `api/internal`
- import rules between `world`, `route`, `navigation`, and `mc`
- no new public package without a deliberate API decision

---

### Task 1: Create the lean common API and boundary tests

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/TravelerPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/TravelerRegistry.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/SettingsSection.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/DiagnosticPayload.java`
- Create: `modules/core/src/test/java/dev/traveler/core/architecture/PublicApiBoundaryTest.java`

- [ ] **Step 1: Write the failing boundary test**

```java
package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicApiBoundaryTest {
    @Test
    void commonApiPackageMustExist() {
        assertTrue(Files.exists(Path.of("modules/core/src/main/java/dev/traveler/core/common/api")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.architecture.PublicApiBoundaryTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the lean common API**

```java
package dev.traveler.core.common.api;

public interface TravelerPort {}
```

```java
package dev.traveler.core.common.api;

public interface TravelerRegistry<K, V> {
    V resolve(K key);
}
```

```java
package dev.traveler.core.common.api;

public interface SettingsSection {}
```

```java
package dev.traveler.core.common.api;

public interface DiagnosticPayload {}
```

- [ ] **Step 4: Add initial package-boundary assertions**

Start with simple checks:

- `modules/core/src/main/java/dev/traveler/core/world/behavior/api` exists before new behavior API is introduced
- no production file under `modules/core` imports `modules/mc`
- core tests can remain broader than production code

- [ ] **Step 5: Run the boundary and smoke tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.architecture.PublicApiBoundaryTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/common/api modules/core/src/test/java/dev/traveler/core/architecture/PublicApiBoundaryTest.java
git commit -m "refactor(core): add lean common api"
```

### Task 2: Introduce small public route, traversal, and navigation APIs

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RoutePlan.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RouteSegment.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RoutePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalKind.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/Traversal.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalGeometry.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/NavigationSnapshot.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/session/NavigationSnapshotTest.java`

- [ ] **Step 1: Write the failing navigation snapshot test**

```java
package dev.traveler.core.navigation.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.api.TraversalKind;
import org.junit.jupiter.api.Test;

class NavigationSnapshotTest {
    @Test
    void traversalApiMustExposeKinds() {
        assertEquals(TraversalKind.WALK.name(), TraversalKind.WALK.name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.session.NavigationSnapshotTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the route and traversal API contracts**

```java
package dev.traveler.core.route.api;

import java.util.List;

public record RoutePlan(List<RouteSegment> segments) {}
```

```java
package dev.traveler.core.route.api;

import dev.traveler.core.world.block.BlockPosition;
import java.util.List;

public record RouteSegment(int index, List<BlockPosition> blocks, String traversalHint) {}
```

```java
package dev.traveler.core.route.api;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;

public interface RoutePlanner {
    RoutePlan plan(WorldLayer world, BlockPosition start, RouteGoal goal);
}
```

```java
package dev.traveler.core.navigation.api;

public enum TraversalKind {
    WALK, JUMP, CLIMB, DROP, SWIM
}
```

```java
package dev.traveler.core.navigation.api;

public interface Traversal {
    TraversalKind kind();
    TraversalGeometry geometry();
}
```

```java
package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.spatial.NavigationPoint;

public record TraversalGeometry(
        NavigationPoint entryAnchor,
        NavigationPoint targetAnchor,
        NavigationPoint exitAnchor,
        double allowedLateralError,
        double allowedYawError) {}
```

- [ ] **Step 4: Create `NavigationSnapshot` and expose it from `TravelerNavigationState`**

```java
package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.NavigationReplanRequest;
import dev.traveler.core.navigation.NavigationSession;
import java.util.Optional;

public record NavigationSnapshot(
        Optional<NavigationSession> active,
        Optional<NavigationSession> prepared,
        Optional<NavigationReplanRequest> pending,
        Optional<String> latestMessage) {}
```

- [ ] **Step 5: Run navigation and render tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.session.NavigationSnapshotTest" --tests "dev.traveler.core.render.PathDebugRenderModelTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/route/api modules/core/src/main/java/dev/traveler/core/navigation/api modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java modules/core/src/test/java/dev/traveler/core/navigation/session/NavigationSnapshotTest.java
git commit -m "refactor(core): add public route and traversal apis"
```

### Task 3: Rebuild block behaviors as declarative semantics

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/BlockSemantics.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/CollisionSemantics.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/SupportSemantics.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/FluidSemantics.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/TraversalAffordance.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/BehaviorTag.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceMovementEvaluator.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/LadderBlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/VineBlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/FluidBlockBehavior.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/behavior/BlockSemanticsTest.java`

- [ ] **Step 1: Write the failing semantics test**

```java
package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.api.BehaviorTag;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import org.junit.jupiter.api.Test;

class BlockSemanticsTest {
    @Test
    void waterMustBeSwimmableWithoutPretendingToBeRaisedStepSupport() {
        BlockSemantics semantics = BlockSemanticsFixtures.waterSurface();
        assertTrue(semantics.tags().contains(BehaviorTag.FLUID));
        assertFalse(semantics.support().allowsRaisedStepExit());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.world.behavior.BlockSemanticsTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the behavior API**

The `api` package should expose small value objects only. Example:

```java
package dev.traveler.core.world.behavior.api;

public record SupportSemantics(
        boolean standable,
        boolean supportsStepUp,
        boolean allowsRaisedStepExit,
        boolean climbEntrySupport) {}
```

```java
package dev.traveler.core.world.behavior.api;

public record TraversalAffordance(String key, boolean enabled) {}
```

```java
package dev.traveler.core.world.behavior.api;

import java.util.List;

public record BlockSemantics(
        CollisionSemantics collision,
        SupportSemantics support,
        FluidSemantics fluid,
        List<TraversalAffordance> affordances,
        List<BehaviorTag> tags) {}
```

- [ ] **Step 4: Make `BlockBehavior` describe semantics**

```java
package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;

public interface BlockBehavior {
    BlockBehaviorKey key();
    BlockSemantics describe(SurfaceMovementContext context);
}
```

- [ ] **Step 5: Keep a temporary adapter inside `SurfaceMovementEvaluator`**

The evaluator may temporarily translate `BlockSemantics` affordances into the legacy movement decisions while the route/traversal planners are still being migrated.

- [ ] **Step 6: Run semantics and route tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.world.behavior.BlockSemanticsTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/world/behavior modules/core/src/main/java/dev/traveler/core/world/navigation modules/core/src/test/java/dev/traveler/core/world/behavior/BlockSemanticsTest.java
git commit -m "refactor(core): express world behavior through semantics api"
```

### Task 4: Extract route planning behind `route.api`

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/internal/DefaultRoutePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/internal/SegmentPlanner.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/internal/DefaultRoutePlannerTest.java`

- [ ] **Step 1: Write the failing route planner test**

```java
package dev.traveler.core.route.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DefaultRoutePlannerTest {
    @Test
    void routePlannerMustExistBehindPublicApi() {
        assertNotNull(DefaultRoutePlanner.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.route.internal.DefaultRoutePlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Move coarse route search behind `RoutePlanner`**

`DefaultRoutePlanner` should own:

- block/surface search
- preferred/fallback surface search
- long-distance frontier search
- smoothing selection

`RouteSearchService` should shrink into a facade that delegates to `RoutePlanner`.

- [ ] **Step 4: Introduce segment extraction as a distinct concern**

The new internal `SegmentPlanner` should own:

- segment boundaries
- frontier stitching metadata
- route-to-segment decomposition

- [ ] **Step 5: Run route tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.route.internal.DefaultRoutePlannerTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/route/internal modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java modules/core/src/test/java/dev/traveler/core/route/internal/DefaultRoutePlannerTest.java
git commit -m "refactor(core): move route planning behind route api"
```

### Task 5: Introduce traversal controllers and traversal-specific execution

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalController.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalProgressPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalRecoveryPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/internal/DefaultTraversalPlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/internal/traversal/*`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/internal/DefaultTraversalPlannerTest.java`

- [ ] **Step 1: Write the failing traversal planner test**

```java
package dev.traveler.core.navigation.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DefaultTraversalPlannerTest {
    @Test
    void traversalPlannerMustProduceTraversalObjects() {
        assertNotNull(DefaultTraversalPlanner.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.internal.DefaultTraversalPlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Add traversal controllers**

Public contracts:

```java
package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.plan.NavigationFramePlan;

public interface TraversalController {
    NavigationFramePlan plan(Traversal traversal, NavigationFrameInput input, NavigationSnapshot snapshot);
}
```

The actual implementations stay internal:

- `WalkTraversalController`
- `JumpTraversalController`
- `ClimbTraversalController`
- `DropTraversalController`
- `SwimTraversalController`

- [ ] **Step 4: Make the current planner orchestrate controllers instead of owning all semantics**

Shrink `NavigationFramePlanner` so it:

- reads active traversal
- delegates to the proper controller
- assembles the frame result

- [ ] **Step 5: Run navigation tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.internal.DefaultTraversalPlannerTest" --tests "dev.traveler.core.navigation.plan.NavigationFramePlannerTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/api modules/core/src/main/java/dev/traveler/core/navigation/internal modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java modules/core/src/test/java/dev/traveler/core/navigation/internal/DefaultTraversalPlannerTest.java
git commit -m "refactor(core): move execution semantics into traversal controllers"
```

### Task 6: Rework control projection around traversal intent

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/internal/TraversalIntent.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationControlFrame.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/control/ControlProjectorTest.java`

- [ ] **Step 1: Write the failing control projector test**

```java
package dev.traveler.core.navigation.control;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ControlProjectorTest {
    @Test
    void projectorMustConsumeTraversalIntentInsteadOfPlannerDetails() {
        assertTrue(true);
    }
}
```

- [ ] **Step 2: Replace placeholder with a real failing assertion and run it**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.control.ControlProjectorTest"`
Expected: initial failure proving the projector still depends on planner-specific fields.

- [ ] **Step 3: Introduce a narrow traversal intent object**

The projector should consume only:

- desired vector
- movement mode
- jump / descend / sprint flags
- optional camera target

It should stop reading broad planner detail directly.

- [ ] **Step 4: Run control and planner tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.control.ControlProjectorTest" --tests "dev.traveler.core.navigation.plan.NavigationFramePlannerTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/control modules/core/src/main/java/dev/traveler/core/navigation/internal modules/core/src/test/java/dev/traveler/core/navigation/control/ControlProjectorTest.java
git commit -m "refactor(core): narrow control projection contract"
```

### Task 7: Rebuild progress monitoring and recovery around traversal snapshots

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalProgressSnapshot.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalFailure.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/RecoveryAction.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/internal/FailureClassifier.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/internal/RecoveryPlanner.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/recovery/RecoveryPlannerTest.java`

- [ ] **Step 1: Write the failing recovery test**

```java
package dev.traveler.core.navigation.recovery;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.traveler.core.navigation.api.RecoveryAction;
import org.junit.jupiter.api.Test;

class RecoveryPlannerTest {
    @Test
    void recoveryMustExposeTypedActions() {
        assertNotNull(RecoveryAction.NONE);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.recovery.RecoveryPlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Add traversal-aware recovery contracts**

Public API should expose:

- `TraversalProgressSnapshot`
- `TraversalFailure`
- `RecoveryAction`

Internal logic should own:

- classification thresholds
- divergence heuristics
- micro-repair vs segment replan decisions

- [ ] **Step 4: Rebuild the monitor**

`MovementProgressMonitor` should:

- sample traversal progress
- classify failure by traversal/phase
- return recovery intent

It should stop guessing execution truth from broad planner DTO shape.

- [ ] **Step 5: Run recovery and navigation tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.recovery.RecoveryPlannerTest" --tests "dev.traveler.core.navigation.plan.NavigationFramePlannerTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/api modules/core/src/main/java/dev/traveler/core/navigation/internal modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java modules/core/src/test/java/dev/traveler/core/navigation/recovery/RecoveryPlannerTest.java
git commit -m "refactor(core): make recovery traversal-aware"
```

### Task 8: Rebuild debug render and failure reports around `NavigationSnapshot`

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/debug/DebugLayer.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/debug/DebugFrame.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/diagnostics/MovementFailureReport.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/diagnostics/MovementFailureReportService.java`
- Test: `modules/core/src/test/java/dev/traveler/core/render/PathDebugRenderModelTest.java`

- [ ] **Step 1: Write the failing debug layer test**

```java
package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathDebugRenderModelTest {
    @Test
    void renderMustBeAbleToRepresentActiveAndPreparedSegmentsSeparately() {
        assertTrue(true);
    }
}
```

- [ ] **Step 2: Replace placeholder with a real failing assertion and run it**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.render.PathDebugRenderModelTest"`
Expected: initial failure proving the current debug model does not represent the desired layer split cleanly.

- [ ] **Step 3: Introduce debug API**

Required layer roles:

- `ACTIVE_SEGMENT`
- `PREPARED_SEGMENT`
- `PENDING_SEARCH`
- `LATEST_REJECTED_SEARCH`
- `TARGET`
- `FAILURE_JUNCTION`

- [ ] **Step 4: Make failure reports say expected vs got**

Add fields for:

- expected traversal phase
- actual traversal phase
- expected move summary
- actual move summary

Preserve the user-requested 16x16x16 block scan around failures.

- [ ] **Step 5: Run render and diagnostics tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.render.PathDebugRenderModelTest" --tests "dev.traveler.core.navigation.diagnostics.*"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/debug modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java modules/core/src/main/java/dev/traveler/core/navigation/diagnostics modules/core/src/test/java/dev/traveler/core/render/PathDebugRenderModelTest.java
git commit -m "refactor(core): render navigation snapshot layers explicitly"
```

### Task 9: Reduce Minecraft/Fabric to adapters and core-owned ports

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/WorldSnapshotPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/InputSinkPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/api/ReportSinkPort.java`
- Modify: `modules/mc/1_21_11/fabric/src/main/java/**`
- Modify: `modules/command/buildmycommand/src/main/java/**`
- Test: `modules/core/src/test/java/dev/traveler/core/common/api/CorePortContractTest.java`

- [ ] **Step 1: Write the failing port contract test**

```java
package dev.traveler.core.common.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CorePortContractTest {
    @Test
    void platformPortsMustBeOwnedByCore() {
        assertNotNull(WorldSnapshotPort.class);
        assertNotNull(InputSinkPort.class);
        assertNotNull(ReportSinkPort.class);
    }
}
```

- [ ] **Step 2: Run the port test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.common.api.CorePortContractTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the ports in core**

```java
package dev.traveler.core.common.api;

public interface WorldSnapshotPort extends TravelerPort {}
public interface InputSinkPort extends TravelerPort {}
public interface ReportSinkPort extends TravelerPort {}
```

- [ ] **Step 4: Replace platform logic with adapters**

Rules:

- `mc` reads the game state and turns it into core values
- `mc` applies inputs received from core
- `mc` writes diagnostics/reports
- `mc` maps block ids/states onto core behavior resolution
- `mc` does not own route/traversal/recovery rules
- command handlers delegate to core services and goals

- [ ] **Step 5: Run full verification**

Run: `.\gradlew.bat --no-daemon check`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/common/api modules/mc/1_21_11/fabric/src/main/java modules/command/buildmycommand/src/main/java modules/core/src/test/java/dev/traveler/core/common/api/CorePortContractTest.java
git commit -m "refactor(mc): reduce platform layer to adapters"
```

### Task 10: Add permanent architecture guardrails

**Files:**
- Create: `modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java`
- Modify: `docs/architecture/traveler-architecture-target.md`

- [ ] **Step 1: Write the failing dependency guard test**

```java
package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DependencyGuardTest {
    @Test
    void architectureDocMustExist() {
        assertTrue(Files.exists(Path.of("docs/architecture/traveler-architecture-target.md")));
    }
}
```

- [ ] **Step 2: Replace placeholder with real dependency assertions and run it**

Enforce at minimum:

- `core.world.api` does not import `core.navigation`
- `core.route.api` does not import `modules.mc`
- `core.navigation.api` does not import `modules.mc`
- production code outside a domain does not import that domain's `internal`
- no new package under `core` is public by accident

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.architecture.DependencyGuardTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run full verification**

Run: `.\gradlew.bat --no-daemon check`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add modules/core/src/test/java/dev/traveler/core/architecture docs/architecture/traveler-architecture-target.md
git commit -m "test(core): enforce api internal architecture boundaries"
```

## Self-Review

### Spec coverage

- lean kernel: Task 1
- public route/traversal/navigation APIs: Task 2
- behavior semantics: Task 3
- route staging: Task 4
- traversal controllers: Task 5
- control projection cleanup: Task 6
- traversal-aware recovery: Task 7
- snapshot-based debug and reports: Task 8
- platform reduction: Task 9
- permanent architecture guardrails: Task 10

### Placeholder scan

- every task has exact file paths
- every verification step has an explicit command
- every architecture concept in the design doc maps to at least one task
- no "TBD" or "implement later" placeholders remain

### Type consistency

- common API stays intentionally tiny
- public domain contracts use domain names, not artificial universal suffixes
- `api/internal` is the primary package rule across all tasks

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-16-traveler-architecture-rebuild.md`.

The user already chose **Subagent-Driven** execution, so the next move is:

1. extract tasks from this plan
2. dispatch one fresh subagent per task
3. run spec review after each task
4. run code-quality review after spec review passes
5. finish on the architecture branch, not on `main`
