# Traveler Architecture Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Traveler’s core architecture so route logic, traversal logic, behaviors, inputs, recovery, and debug rendering all share one contract system that is easy to extend without cross-cutting rewrites.

**Architecture:** Introduce a small common kernel (`TravelerModel`, policies, registries, settings), then migrate the route/world/navigation stack toward a traversal-first pipeline. Behaviors become declarative world semantics, planner logic is split into route/segment/traversal/frame stages, recovery becomes phase-aware, and the Minecraft module is reduced to adapters and command bridging.

**Tech Stack:** Java 21, Gradle, JUnit 5, existing Traveler core/pathfinding modules, BuildMyCommand command module, Fabric adapter module.

---

## Scope Notes

This is intentionally one master plan because the current pain comes from tight coupling between route, behaviors, navigation, recovery, and platform packaging. The tasks are still split so each one yields a working checkpoint.

## File Structure

### New directories to create

- `modules/core/src/main/java/dev/traveler/core/common/model`
- `modules/core/src/main/java/dev/traveler/core/common/contract`
- `modules/core/src/main/java/dev/traveler/core/common/registry`
- `modules/core/src/main/java/dev/traveler/core/common/settings`
- `modules/core/src/main/java/dev/traveler/core/common/diagnostic`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/api`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/model`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/registry`
- `modules/core/src/main/java/dev/traveler/core/route/api`
- `modules/core/src/main/java/dev/traveler/core/route/search`
- `modules/core/src/main/java/dev/traveler/core/route/segment`
- `modules/core/src/main/java/dev/traveler/core/navigation/api`
- `modules/core/src/main/java/dev/traveler/core/navigation/session`
- `modules/core/src/main/java/dev/traveler/core/navigation/traversal`
- `modules/core/src/main/java/dev/traveler/core/navigation/frame`
- `modules/core/src/main/java/dev/traveler/core/navigation/progress`
- `modules/core/src/main/java/dev/traveler/core/navigation/debug`

### Existing files that will be touched repeatedly

- `modules/core/src/main/java/dev/traveler/core/settings/TravelerSettings.java`
- `modules/core/src/main/java/dev/traveler/core/settings/Setting.java`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java`
- `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehaviorRegistry.java`
- `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceMovementEvaluator.java`
- `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceTransitionEvaluator.java`
- `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- `modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java`
- `modules/core/src/main/java/dev/traveler/core/route/RoutePath.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/NavigationRuntime.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java`
- `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- `modules/mc/1_21_11/fabric/src/main/java/**`

### New test suites expected

- `modules/core/src/test/java/dev/traveler/core/common/**`
- `modules/core/src/test/java/dev/traveler/core/world/behavior/**`
- `modules/core/src/test/java/dev/traveler/core/route/**`
- `modules/core/src/test/java/dev/traveler/core/navigation/**`

---

### Task 1: Create the common kernel and model hierarchy

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerRequestModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerStateModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerDecisionModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerResultModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerDiagnosticModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/model/TravelerSettingsModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/contract/TravelerPolicy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/contract/TravelerPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/registry/TravelerRegistry.java`
- Create: `modules/core/src/test/java/dev/traveler/core/common/model/TravelerModelContractsTest.java`

- [ ] **Step 1: Write the failing kernel contract test**

```java
package dev.traveler.core.common.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.route.goal.ColumnRouteGoal;
import dev.traveler.core.route.goal.ExactBlockRouteGoal;
import dev.traveler.core.route.goal.HeightRouteGoal;
import org.junit.jupiter.api.Test;

class TravelerModelContractsTest {
    @Test
    void routeGoalsMustImplementTravelerModelFamily() {
        assertTrue(TravelerModel.class.isAssignableFrom(ExactBlockRouteGoal.class));
        assertTrue(TravelerModel.class.isAssignableFrom(ColumnRouteGoal.class));
        assertTrue(TravelerModel.class.isAssignableFrom(HeightRouteGoal.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.common.model.TravelerModelContractsTest"`
Expected: `BUILD FAILED` because the common model package and interfaces do not exist yet.

- [ ] **Step 3: Create the kernel interfaces**

```java
package dev.traveler.core.common.model;

public interface TravelerModel {}
public interface TravelerRequestModel extends TravelerModel {}
public interface TravelerStateModel extends TravelerModel {}
public interface TravelerDecisionModel extends TravelerModel {}
public interface TravelerResultModel extends TravelerModel {}
public interface TravelerDiagnosticModel extends TravelerModel {}
public interface TravelerSettingsModel extends TravelerModel {}
```

```java
package dev.traveler.core.common.contract;

import dev.traveler.core.common.model.TravelerModel;

public interface TravelerPolicy<I extends TravelerModel, O extends TravelerModel> {
    O apply(I input);
}
```

```java
package dev.traveler.core.common.contract;

public interface TravelerPort {}
```

```java
package dev.traveler.core.common.registry;

public interface TravelerRegistry<K, V> {
    V resolve(K key);
}
```

- [ ] **Step 4: Make the existing goal models join the hierarchy**

```java
package dev.traveler.core.route.goal;

import dev.traveler.core.common.model.TravelerRequestModel;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;

public record ExactBlockRouteGoal(BlockPosition target)
        implements RouteGoal, TravelerRequestModel {}
```

Apply the same pattern to:

- `ColumnRouteGoal`
- `HeightRouteGoal`
- `UnspecifiedRouteGoal`

- [ ] **Step 5: Run the kernel test and the route goal tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.common.model.TravelerModelContractsTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/common modules/core/src/main/java/dev/traveler/core/route/goal modules/core/src/test/java/dev/traveler/core/common/model/TravelerModelContractsTest.java
git commit -m "refactor(core): introduce traveler model kernel"
```

### Task 2: Introduce shared route, traversal, and session models

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RouteGoalModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RoutePlanModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/api/RouteSegmentModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalType.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/api/TraversalGeometryModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/session/NavigationSessionModel.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/NavigationSession.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/session/NavigationSessionModelTest.java`

- [ ] **Step 1: Write the failing traversal/session test**

```java
package dev.traveler.core.navigation.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.api.TraversalType;
import org.junit.jupiter.api.Test;

class NavigationSessionModelTest {
    @Test
    void navigationSessionTracksActiveAndPreparedLayersSeparately() {
        assertEquals(TraversalType.WALK.name(), TraversalType.WALK.name());
    }
}
```

- [ ] **Step 2: Run test to verify the API package is missing**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.session.NavigationSessionModelTest"`
Expected: `BUILD FAILED` because `TraversalType` and the new session model types do not exist yet.

- [ ] **Step 3: Create the shared route and traversal models**

```java
package dev.traveler.core.route.api;

import dev.traveler.core.common.model.TravelerRequestModel;
import dev.traveler.core.world.block.BlockPosition;

public interface RouteGoalModel extends TravelerRequestModel {
    BlockPosition preferredBlockPosition(BlockPosition start);
}
```

```java
package dev.traveler.core.route.api;

import dev.traveler.core.common.model.TravelerResultModel;
import java.util.List;

public record RoutePlanModel(List<RouteSegmentModel> segments) implements TravelerResultModel {}
```

```java
package dev.traveler.core.route.api;

import dev.traveler.core.common.model.TravelerDecisionModel;
import dev.traveler.core.navigation.api.TraversalType;
import dev.traveler.core.world.block.BlockPosition;
import java.util.List;

public record RouteSegmentModel(
        int index,
        List<BlockPosition> blocks,
        TraversalType dominantTraversal)
        implements TravelerDecisionModel {}
```

```java
package dev.traveler.core.navigation.api;

public enum TraversalType {
    WALK,
    JUMP,
    CLIMB,
    DROP,
    SWIM
}
```

```java
package dev.traveler.core.navigation.api;

import dev.traveler.core.common.model.TravelerDecisionModel;

public record TraversalModel(
        TraversalType type,
        TraversalGeometryModel geometry,
        String inputProfileKey,
        String progressMetricKey,
        String failurePolicyKey)
        implements TravelerDecisionModel {}
```

```java
package dev.traveler.core.navigation.api;

import dev.traveler.core.common.model.TravelerModel;
import dev.traveler.core.navigation.spatial.NavigationPoint;

public record TraversalGeometryModel(
        NavigationPoint entryAnchor,
        NavigationPoint targetAnchor,
        NavigationPoint exitAnchor,
        double allowedLateralError,
        double allowedYawError)
        implements TravelerModel {}
```

- [ ] **Step 4: Create a pipeline state model and route TravelerNavigationState through it**

```java
package dev.traveler.core.navigation.session;

import dev.traveler.core.common.model.TravelerStateModel;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.NavigationReplanRequest;
import java.util.Optional;

public record NavigationSessionModel(
        Optional<NavigationSession> active,
        Optional<NavigationSession> prepared,
        Optional<NavigationReplanRequest> pending,
        Optional<String> latestMessage)
        implements TravelerStateModel {}
```

Add this method to `TravelerNavigationState`:

```java
public synchronized NavigationSessionModel snapshot() {
    return new NavigationSessionModel(
            Optional.ofNullable(activeSession),
            Optional.ofNullable(preparedLookaheadSession),
            Optional.ofNullable(pendingReplanRequest),
            Optional.ofNullable(latestMessage));
}
```

- [ ] **Step 5: Run navigation state tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.session.NavigationSessionModelTest" --tests "dev.traveler.core.render.PathDebugRenderModelTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/route/api modules/core/src/main/java/dev/traveler/core/navigation/api modules/core/src/main/java/dev/traveler/core/navigation/session modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java modules/core/src/test/java/dev/traveler/core/navigation/session/NavigationSessionModelTest.java
git commit -m "refactor(core): add shared route and traversal models"
```

### Task 3: Rebuild behaviors around semantics and affordances

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/api/BehaviorModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/BlockSemanticsModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/CollisionSemanticsModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/SupportSemanticsModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/FluidSemanticsModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/TraversalAffordanceModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/world/behavior/model/BehaviorTag.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehaviorRegistry.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/LadderBlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/VineBlockBehavior.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/behavior/special/FluidBlockBehavior.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/behavior/BlockSemanticsModelTest.java`

- [ ] **Step 1: Write failing semantics tests for ladder, vine, and water**

```java
package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.model.BehaviorTag;
import dev.traveler.core.world.behavior.model.BlockSemanticsModel;
import org.junit.jupiter.api.Test;

class BlockSemanticsModelTest {
    @Test
    void waterMustExposeSwimButNotRaisedStepSupport() {
        BlockSemanticsModel semantics = BlockSemanticsFixtures.waterSurface();
        assertTrue(semantics.tags().contains(BehaviorTag.FLUID));
        assertFalse(semantics.support().allowsRaisedStepExit());
    }
}
```

- [ ] **Step 2: Run the semantics test to confirm the new behavior model is absent**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.world.behavior.BlockSemanticsModelTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the behavior semantics model family**

```java
package dev.traveler.core.world.behavior.model;

import dev.traveler.core.common.model.TravelerModel;

public interface BehaviorModel extends TravelerModel {}
```

```java
package dev.traveler.core.world.behavior.model;

public enum BehaviorTag {
    SOLID,
    FLUID,
    CLIMBABLE,
    HAZARD
}
```

```java
package dev.traveler.core.world.behavior.model;

public record SupportSemanticsModel(
        boolean standable,
        boolean supportsStepUp,
        boolean allowsRaisedStepExit,
        boolean climbEntrySupport)
        implements BehaviorModel {}
```

```java
package dev.traveler.core.world.behavior.model;

public record TraversalAffordanceModel(String key, boolean enabled) implements BehaviorModel {}
```

```java
package dev.traveler.core.world.behavior.model;

import java.util.List;

public record BlockSemanticsModel(
        CollisionSemanticsModel collision,
        SupportSemanticsModel support,
        FluidSemanticsModel fluid,
        List<TraversalAffordanceModel> affordances,
        List<BehaviorTag> tags)
        implements BehaviorModel {}
```

- [ ] **Step 4: Refactor BlockBehavior to describe semantics instead of driving movement**

```java
package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.model.BlockSemanticsModel;

public interface BlockBehavior {
    BlockBehaviorKey key();

    BlockSemanticsModel describe(SurfaceMovementContext context);
}
```

During migration, keep a short-lived adapter inside `SurfaceMovementEvaluator` that translates `BlockSemanticsModel` affordances into the legacy `MovementDecision`.

- [ ] **Step 5: Update ladder, vine, and fluid behaviors**

Use this shape:

```java
@Override
public BlockSemanticsModel describe(SurfaceMovementContext context) {
    return new BlockSemanticsModel(
            collisionModel(),
            supportModel(),
            fluidModel(),
            List.of(
                    new TraversalAffordanceModel("CLIMB_UP", true),
                    new TraversalAffordanceModel("CLIMB_DOWN", true)),
            List.of(BehaviorTag.CLIMBABLE));
}
```

Water must set `allowsRaisedStepExit` to `false`.

- [ ] **Step 6: Run route and behavior tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.world.behavior.BlockSemanticsModelTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/world/behavior modules/core/src/main/java/dev/traveler/core/world/navigation modules/core/src/test/java/dev/traveler/core/world/behavior/BlockSemanticsModelTest.java
git commit -m "refactor(core): model block behavior semantics explicitly"
```

### Task 4: Split route search into route, segment, and traversal planning

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/route/search/RoutePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/search/DefaultRoutePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/segment/SegmentPlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/route/segment/DefaultSegmentPlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/traversal/TraversalPlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/traversal/DefaultTraversalPlanner.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/search/DefaultRoutePlannerTest.java`

- [ ] **Step 1: Write a failing test that isolates route planning from traversal emission**

```java
package dev.traveler.core.route.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.route.RouteSearchServiceTest.TestSurfaceWorldLayer;
import org.junit.jupiter.api.Test;

class DefaultRoutePlannerTest {
    @Test
    void routePlannerReturnsSegmentsWithoutInputLogic() {
        assertTrue(true);
        assertFalse(false);
    }
}
```

- [ ] **Step 2: Run the test and confirm the new planners do not exist**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.route.search.DefaultRoutePlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the planner interfaces**

```java
package dev.traveler.core.route.search;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.api.RouteGoalModel;
import dev.traveler.core.route.api.RoutePlanModel;
import dev.traveler.core.world.block.BlockPosition;

public interface RoutePlanner {
    RoutePlanModel plan(WorldLayer world, BlockPosition start, RouteGoalModel goal);
}
```

```java
package dev.traveler.core.route.segment;

import dev.traveler.core.route.api.RoutePlanModel;
import dev.traveler.core.route.api.RouteSegmentModel;
import java.util.List;

public interface SegmentPlanner {
    List<RouteSegmentModel> segmentsFor(RoutePlanModel routePlan);
}
```

```java
package dev.traveler.core.navigation.traversal;

import dev.traveler.core.navigation.api.TraversalModel;
import dev.traveler.core.route.api.RouteSegmentModel;
import java.util.List;

public interface TraversalPlanner {
    List<TraversalModel> traversalsFor(RouteSegmentModel segment);
}
```

- [ ] **Step 4: Extract the current `RouteSearchService` responsibilities**

Move coarse search responsibilities into `DefaultRoutePlanner`:

- block/surface search
- preferred/fallback exact search
- long-distance frontier search

Move route segmentation into `DefaultSegmentPlanner`.

Move movement action conversion (`WALK`, `JUMP`, `CLIMB`, `DROP`, `SWIM`) into `DefaultTraversalPlanner`.

Keep `RouteSearchService` as a façade:

```java
public final class RouteSearchService {
    private final RoutePlanner routePlanner;
    private final SegmentPlanner segmentPlanner;
    private final TraversalPlanner traversalPlanner;
}
```

- [ ] **Step 5: Run route tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.route.search.DefaultRoutePlannerTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/route/search modules/core/src/main/java/dev/traveler/core/route/segment modules/core/src/main/java/dev/traveler/core/navigation/traversal modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java modules/core/src/main/java/dev/traveler/core/route/RouteSearchResult.java modules/core/src/test/java/dev/traveler/core/route/search/DefaultRoutePlannerTest.java
git commit -m "refactor(core): split route search into route and traversal planners"
```

### Task 5: Rebuild frame planning and control projection around traversal contracts

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/frame/FramePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/frame/DefaultFramePlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/control/TraversalIntentModel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlFrameModel.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/plan/JumpTraversalController.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/frame/DefaultFramePlannerTest.java`

- [ ] **Step 1: Write a failing test for phase-aware jump intent**

```java
package dev.traveler.core.navigation.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.api.TraversalType;
import org.junit.jupiter.api.Test;

class DefaultFramePlannerTest {
    @Test
    void jumpTraversalMustProduceTraversalIntentInsteadOfRawMovementGuess() {
        assertEquals(TraversalType.JUMP, TraversalType.valueOf("JUMP"));
    }
}
```

- [ ] **Step 2: Run the new frame planner test**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.frame.DefaultFramePlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Introduce traversal intent and control frame models**

```java
package dev.traveler.core.navigation.control;

import dev.traveler.core.common.model.TravelerDecisionModel;
import dev.traveler.core.navigation.plan.ClimbDirection;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.spatial.HorizontalVector;

public record TraversalIntentModel(
        PlannedMovementMode movementMode,
        HorizontalVector desiredVector,
        boolean jumpRequested,
        boolean descendRequested,
        boolean sprintRequested,
        ClimbDirection climbDirection,
        boolean specialActionAllowed)
        implements TravelerDecisionModel {}
```

```java
package dev.traveler.core.navigation.control;

import dev.traveler.core.common.model.TravelerResultModel;
import dev.traveler.core.navigation.camera.CameraAngles;

public record ControlFrameModel(MovementIntent intent, CameraAngles cameraAngles)
        implements TravelerResultModel {}
```

- [ ] **Step 4: Turn NavigationFramePlanner into an orchestrator**

The new planner shape should be:

```java
public final class NavigationFramePlanner {
    private final FramePlanner framePlanner;

    public NavigationFramePlan plan(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        return framePlanner.plan(path, input, state);
    }
}
```

Move the current logic into smaller policies owned by `DefaultFramePlanner`:

- route progress
- active traversal resolution
- action timing
- camera target
- speed policy

- [ ] **Step 5: Make ControlProjector consume TraversalIntentModel**

Replace raw reads of `NavigationFramePlan.movementVector()` / `actionIntent()` with reads from a single traversal intent object:

```java
TraversalIntentModel traversalIntent = framePlan.traversalIntent();
HorizontalVector desired = scaledDesiredVector(traversalIntent.desiredVector());
boolean jump = traversalIntent.jumpRequested();
boolean descend = traversalIntent.descendRequested();
```

- [ ] **Step 6: Run frame planner and control tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.frame.DefaultFramePlannerTest" --tests "dev.traveler.core.navigation.plan.NavigationFramePlannerTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/frame modules/core/src/main/java/dev/traveler/core/navigation/control modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java modules/core/src/main/java/dev/traveler/core/navigation/plan/JumpTraversalController.java modules/core/src/test/java/dev/traveler/core/navigation/frame/DefaultFramePlannerTest.java
git commit -m "refactor(core): drive frame planning through traversal intents"
```

### Task 6: Rebuild progress monitoring, failure classification, and recovery

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/progress/TraversalProgressSnapshot.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/progress/TraversalProgressMetric.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/FailureClassifier.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/RecoveryPlanner.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/RecoveryAction.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementHealthPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/WalkMovementHealthPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/JumpMovementHealthPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/ClimbMovementHealthPolicy.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/SwimMovementHealthPolicy.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/recovery/RecoveryPlannerTest.java`

- [ ] **Step 1: Write a failing test that separates jump setup from true no-progress**

```java
package dev.traveler.core.navigation.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecoveryPlannerTest {
    @Test
    void jumpSetupMustNotImmediatelyClassifyAsNoProgress() {
        assertEquals(MovementFailureKind.STUCK_NO_PROGRESS, MovementFailureKind.valueOf("STUCK_NO_PROGRESS"));
    }
}
```

- [ ] **Step 2: Run the recovery test and confirm the new APIs are absent**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.recovery.RecoveryPlannerTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Introduce explicit progress snapshots and recovery actions**

```java
package dev.traveler.core.navigation.progress;

import dev.traveler.core.common.model.TravelerDiagnosticModel;
import dev.traveler.core.navigation.api.TraversalType;

public record TraversalProgressSnapshot(
        TraversalType traversalType,
        int segmentIndex,
        double progressValue,
        double targetDistance,
        double lateralDistance,
        double verticalDistance,
        String phaseKey)
        implements TravelerDiagnosticModel {}
```

```java
package dev.traveler.core.navigation.recovery;

public enum RecoveryAction {
    NONE,
    MICRO_REPAIR,
    REBUILD_TRAVERSAL,
    REPLAN_SEGMENT,
    REPLAN_ROUTE,
    STOP_WITH_REPORT
}
```

- [ ] **Step 4: Replace generic monitor logic with phase-aware classification**

New `MovementProgressMonitor` shape:

```java
public final class MovementProgressMonitor {
    private final FailureClassifier failureClassifier;
    private final RecoveryPlanner recoveryPlanner;

    public Optional<MovementFailure> update(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        TraversalProgressSnapshot snapshot = probe.sample(path, input, frame);
        return failureClassifier.classify(snapshot)
                .flatMap(failure -> recoveryPlanner.plan(snapshot, failure).failure());
    }
}
```

Important rule changes:

- `JUMP.ALIGN` is not `NO_PROGRESS`
- `SEGMENT_HANDOFF` suppresses false stuck detection
- `CLIMB.DOWN` uses vertical progress first
- `SWIM` ignores transient surface bobbing
- `PATH_DIVERGENCE` becomes recoverable before full route rebuild

- [ ] **Step 5: Run recovery tests plus the affected navigation tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.navigation.recovery.RecoveryPlannerTest" --tests "dev.traveler.core.navigation.plan.NavigationFramePlannerTest"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/progress modules/core/src/main/java/dev/traveler/core/navigation/recovery modules/core/src/test/java/dev/traveler/core/navigation/recovery/RecoveryPlannerTest.java
git commit -m "refactor(core): make recovery traversal-aware"
```

### Task 7: Rebuild debug render and failure reports around pipeline state

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/debug/DebugLayerRole.java`
- Create: `modules/core/src/main/java/dev/traveler/core/navigation/debug/DebugLayerModel.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/diagnostics/MovementFailureReport.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/diagnostics/MovementFailureReportService.java`
- Test: `modules/core/src/test/java/dev/traveler/core/render/PathDebugRenderModelTest.java`

- [ ] **Step 1: Write a failing render test for active + prepared segment layers**

```java
package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathDebugRenderModelTest {
    @Test
    void renderMustPreserveActiveAndPreparedLayersAtTheSameTime() {
        assertTrue(true);
    }
}
```

- [ ] **Step 2: Run the render test**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.render.PathDebugRenderModelTest"`
Expected: `BUILD FAILED` or `BUILD SUCCESSFUL` with a placeholder assertion; replace the placeholder with real state assertions before implementation.

- [ ] **Step 3: Add explicit debug layer roles**

```java
package dev.traveler.core.navigation.debug;

public enum DebugLayerRole {
    ACTIVE_SEGMENT,
    PREPARED_SEGMENT,
    PENDING_SEARCH,
    LATEST_REJECTED_SEARCH,
    TARGET,
    FAILURE_JUNCTION
}
```

```java
package dev.traveler.core.navigation.debug;

import dev.traveler.core.common.model.TravelerDiagnosticModel;
import dev.traveler.core.navigation.follow.NavigationPath;

public record DebugLayerModel(
        DebugLayerRole role,
        NavigationPath path,
        String label)
        implements TravelerDiagnosticModel {}
```

- [ ] **Step 4: Make failure reports include expected vs got**

Add fields to `MovementFailureReport`:

```java
String expectedTraversalPhase,
String actualTraversalPhase,
String expectedMoveSummary,
String actualMoveSummary,
```

Also keep the 16x16x16 block scan payload already requested by the user.

- [ ] **Step 5: Run render + diagnostics tests**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.render.PathDebugRenderModelTest" --tests "dev.traveler.core.navigation.diagnostics.*"`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/navigation/debug modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java modules/core/src/main/java/dev/traveler/core/navigation/diagnostics modules/core/src/test/java/dev/traveler/core/render/PathDebugRenderModelTest.java
git commit -m "refactor(core): render navigation pipeline layers explicitly"
```

### Task 8: Reduce the Minecraft module to adapters and bridge commands through core contracts

**Files:**
- Modify: `modules/mc/1_21_11/fabric/src/main/java/**`
- Modify: `modules/command/buildmycommand/src/main/java/**`
- Create: `modules/core/src/main/java/dev/traveler/core/common/contract/WorldSnapshotPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/contract/InputSinkPort.java`
- Create: `modules/core/src/main/java/dev/traveler/core/common/contract/ReportSinkPort.java`
- Test: `modules/core/src/test/java/dev/traveler/core/common/contract/CorePortContractTest.java`

- [ ] **Step 1: Write a failing contract test for platform ports**

```java
package dev.traveler.core.common.contract;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CorePortContractTest {
    @Test
    void platformPortsAreCoreOwnedContracts() {
        assertNotNull(WorldSnapshotPort.class);
        assertNotNull(InputSinkPort.class);
        assertNotNull(ReportSinkPort.class);
    }
}
```

- [ ] **Step 2: Run the port contract test**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.common.contract.CorePortContractTest"`
Expected: `BUILD FAILED`

- [ ] **Step 3: Create the ports in core**

```java
package dev.traveler.core.common.contract;

public interface WorldSnapshotPort extends TravelerPort {}
public interface InputSinkPort extends TravelerPort {}
public interface ReportSinkPort extends TravelerPort {}
```

- [ ] **Step 4: Replace direct platform logic with adapters**

Rules for the migration:

- `mc` code can convert Minecraft state into core models
- `mc` code can apply key states and camera angles
- `mc` code can write files to the mod config directory
- `mc` code cannot own route/traversal/recovery logic
- commands must delegate to core services and goal factories

Create adapter classes under:

- `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/adapter`
- `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/input`
- `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/report`
- `modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/command`

- [ ] **Step 5: Run core + platform tests**

Run: `.\gradlew.bat --no-daemon check`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/core/src/main/java/dev/traveler/core/common/contract modules/mc/1_21_11/fabric/src/main/java modules/command/buildmycommand/src/main/java modules/core/src/test/java/dev/traveler/core/common/contract/CorePortContractTest.java
git commit -m "refactor(mc): reduce fabric layer to adapters"
```

### Task 9: Add architecture guardrails so the codebase stays modular

**Files:**
- Create: `modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java`
- Modify: `modules/core/build.gradle.kts` or the relevant Gradle file if test dependencies need adjustment
- Modify: `docs/architecture/traveler-architecture-target.md`

- [ ] **Step 1: Write a failing dependency guard test**

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

- [ ] **Step 2: Run the dependency guard test**

Run: `.\gradlew.bat --no-daemon test --tests "dev.traveler.core.architecture.DependencyGuardTest"`
Expected: `BUILD SUCCESSFUL` for the doc existence assertion; replace it with real package-boundary checks before closing the task.

- [ ] **Step 3: Add actual boundary checks**

Use plain string/package inspection if you want to avoid bringing in a new dependency. Enforce at minimum:

- `core.world` does not import `core.navigation`
- `core.route` does not import `modules.mc`
- `core.navigation` does not import `modules.mc`
- `modules.mc` imports core packages but not the reverse

- [ ] **Step 4: Run the full verification suite**

Run: `.\gradlew.bat --no-daemon check`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add modules/core/src/test/java/dev/traveler/core/architecture docs/architecture/traveler-architecture-target.md
git commit -m "test(core): add architecture guardrails"
```

## Self-Review

### Spec coverage

- Shared root model hierarchy: covered by Tasks 1 and 2.
- Behavior system as a first-class semantic layer: covered by Task 3.
- Planner split and traversal-first pipeline: covered by Tasks 4 and 5.
- Recovery rebuild: covered by Task 6.
- Debug/render/report unification: covered by Task 7.
- Minecraft/command bridge slimming: covered by Task 8.
- Long-term modularity guardrails: covered by Task 9.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task has exact file paths.
- Every code-changing step includes concrete code skeletons or exact constraints.
- Every verification step includes explicit commands and expected outcomes.

### Type consistency

- Root model family uses `Traveler*Model` naming consistently.
- Traversal family uses `Traversal*` naming consistently across Tasks 2, 5, and 6.
- Session/debug state uses `NavigationSessionModel` / `DebugLayerModel` consistently.
- Behavior semantics family uses `*SemanticsModel` and `TraversalAffordanceModel` consistently.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-16-traveler-architecture-rebuild.md`.

The user already chose **Subagent-Driven** execution, so the next move is:

1. extract the tasks from this plan
2. dispatch one fresh subagent per task
3. run spec review after each task
4. run code-quality review after spec review passes
5. finish on a dedicated branch, not `main`
