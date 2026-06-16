# Traveler Target Architecture

## Purpose

Traveler already has strong execution instincts, but the codebase is hard to evolve because the same movement is reinterpreted by too many layers. Jump, climb, swim, smoothing, recovery, debug render, and the platform bridge do not all speak the same language. This document defines the new shared language.

## Current Failure Pattern

Today the codebase leaks responsibility across layers:

- [RouteSearchService](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java) owns route search, smoothing, preferred/fallback surface selection, long-distance fallback, and diagnostics assembly.
- [NavigationFramePlanner](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java) mixes route progress, action choice, jump alignment, steering choice, timing, camera targeting, climb direction, and speed choice.
- [MovementProgressMonitor](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java) evaluates progress from abstractions that are only partially aligned with the traversal being executed.
- [ControlProjector](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java) converts movement vectors into `ZQSD`-style inputs without a first-class traversal contract.
- [BlockBehavior](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java) mixes world semantics with path smoothing and local movement decisions.
- [TravelerNavigationState](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/TravelerNavigationState.java) stores active/prepared/pending path state, but that state is not yet the single source of truth for debug layers and recovery.

The result is a mathematical gap:

1. Route logic thinks in block/surface transitions.
2. Traversal execution thinks in continuous positions and phases.
3. Input projection thinks in camera-relative axes.
4. Recovery thinks in deltas and timeouts.
5. Debug render shows whichever intermediate artifact is convenient.

When those layers disagree, simple tuning becomes expensive.

## Codebase Audit Anchors

### Model families that already exist but have no common roots

These are the biggest semantic families currently floating without shared contracts:

1. **Spatial/world-position models**
   - [BlockPosition](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/block/BlockPosition.java)
   - [NavigationPoint](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/spatial/NavigationPoint.java)
   - [SurfaceNode](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/surface/SurfaceNode.java)
   - [RenderVertex](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/RenderVertex.java)

   They are all public carriers of “where is something in the world?”, but they do not share even a minimal spatial contract.

2. **Path and segment models**
   - [RoutePath](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RoutePath.java)
   - [RouteStep](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteStep.java)
   - [NavigationPath](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/follow/NavigationPath.java)
   - [NavigationSegmentIntent](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/follow/NavigationSegmentIntent.java)

   The route planner and the route follower are describing the same journey in two unrelated vocabularies.

3. **Movement/traversal intent models**
   - [MovementAction](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/behavior/decision/MovementAction.java)
   - [LocomotionAction](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/locomotion/LocomotionAction.java)
   - [ActionIntent](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/ActionIntent.java)
   - [LocomotionPlan](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/locomotion/LocomotionPlan.java)

   These are all forms of “what movement are we executing?”, but they are not bound to one root family.

4. **Render models**
   - [DebugRenderFrame](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/DebugRenderFrame.java)
   - [DebugLine](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/DebugLine.java)
   - [DebugBox](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/DebugBox.java)

   Even inside rendering, Traveler is modeling “frame made of primitives” without a shared root contract.

5. **Settings sections**
   - [RouteSearchSettings](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteSearchSettings.java)
   - [LongDistanceRouteSettings](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/longdistance/LongDistanceRouteSettings.java)
   - [PathFollowSettings](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/follow/PathFollowSettings.java)
   - [PathSteeringSettings](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/steering/PathSteeringSettings.java)
   - [MovementHealthSettings](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementHealthSettings.java)

   They already behave like configuration sections, but they are not modeled as one configuration family.

### Coupling hotspots that justify the rebuild

1. **Planner choke point**
   - [NavigationFramePlanner](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java)
   - [MovementActionPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementActionPolicy.java)
   - [JumpTraversalController](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/JumpTraversalController.java)
   - [MovementVectorPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/MovementVectorPolicy.java)

   These files currently share responsibility for movement interpretation, which is why jump/climb/swim tuning becomes cross-cutting.

2. **Overloaded cross-layer frame DTOs**
   - [NavigationFramePlan](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlan.java)
   - [NavigationControlFrame](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/NavigationControlFrame.java)
   - [ControlProjector](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java)
   - [RouteMovementHealthProbe](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/RouteMovementHealthProbe.java)

   These objects are carrying planner, projector, monitor, and debug meaning all at once.

3. **Route generation fused with execution targets**
   - [RouteSearchService](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java)
   - [DefaultSurfaceRouteStepProvider](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/step/DefaultSurfaceRouteStepProvider.java)
   - [ClimbSurfaceRouteStepProvider](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/step/ClimbSurfaceRouteStepProvider.java)
   - [SurfaceSmoothingPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceSmoothingPolicy.java)

   Smoothing and runtime action targets are too closely tied to route construction.

4. **Recovery re-derives movement semantics**
   - [MovementProgressMonitor](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java)
   - [JumpMovementHealthPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/JumpMovementHealthPolicy.java)
   - [ClimbMovementHealthPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/ClimbMovementHealthPolicy.java)
   - [SwimMovementHealthPolicy](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/SwimMovementHealthPolicy.java)

   Recovery is currently forced to guess execution truth from partial planner artifacts.

5. **Debug/render tightly coupled to navigation internals**
   - [PathDebugRenderModel](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java)
   - [MovementFailureReport](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/diagnostics/MovementFailureReport.java)

   Debug is not merely observing the pipeline; it depends on the current internal shape of the pipeline.

## Architecture Laws

These laws are intentionally strict:

1. One movement meaning, one contract.
2. No feature may redefine route, traversal, control, progress, or failure semantics locally.
3. Behaviors describe the world; controllers drive the player.
4. Recovery reacts to typed traversal progress, never to anonymous motion alone.
5. Debug render reads navigation state, not ad hoc intermediate results.
6. `mc`/`fabric` adapts data and inputs only; it never becomes a second core.

## Layer Model

```text
common
  -> root models, contracts, registries, settings, diagnostics
world
  -> behavior semantics, collision/support/fluid/climb affordances
route
  -> route goals, route plans, segment plans, long-distance strategy
navigation
  -> traversal plans, frame plans, input projection, progress, recovery, debug
platform
  -> Minecraft/Fabric adapters, command bridge, reports, rendering hooks
```

Dependency rules:

- `common` has no dependency on feature layers.
- `world` depends only on `common`.
- `route` depends on `common` + `world`.
- `navigation` depends on `common` + `world` + `route`.
- `platform` depends on everything else, but no core package depends on `platform`.

## Root Contracts

Every public model implements a common parent. No more public records that float without a family.

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

Core logic contracts:

```java
package dev.traveler.core.common.contract;

import dev.traveler.core.common.model.TravelerModel;

public interface TravelerPolicy<I extends TravelerModel, O extends TravelerModel> {
    O apply(I input);
}

public interface TravelerPort {}
```

Registry contract:

```java
package dev.traveler.core.common.registry;

public interface TravelerRegistry<K, V> {
    V resolve(K key);
}
```

## Shared Domain Models

The key is not “more models.” The key is “fewer meanings.”

### Route Family

- `RouteGoalModel`
- `RoutePlanModel`
- `RouteSegmentModel`
- `RouteFailureModel`
- `LongDistancePlanModel`

### Traversal Family

- `TraversalModel`
- `TraversalGeometryModel`
- `TraversalPhaseModel`
- `TraversalIntentModel`
- `TraversalProgressModel`
- `TraversalFailureModel`
- `TraversalRecoveryModel`

### Session / Debug Family

- `NavigationSessionModel`
- `NavigationPipelineStateModel`
- `DebugLayerModel`
- `DebugFrameModel`
- `FailureReportModel`

### Settings Family

- `TravelerSettingsModel`
- `RouteSettingsModel`
- `TraversalSettingsModel`
- `RecoverySettingsModel`
- `RenderSettingsModel`

## Behaviors: World Semantics, Not Micro-AI

Behaviors are fundamental, but they must be disciplined.

Bad behavior design:

- behavior chooses when to jump
- behavior owns recovery
- behavior rewrites smoothing rules on the fly
- behavior manipulates input booleans directly

Good behavior design:

- behavior describes what a block or local structure allows
- route/traversal layers consume those affordances

Behavior contract:

```java
package dev.traveler.core.world.behavior.api;

import dev.traveler.core.world.behavior.context.BlockBehaviorContext;
import dev.traveler.core.world.behavior.model.BlockSemanticsModel;

public interface BlockBehavior {
    BlockSemanticsModel describe(BlockBehaviorContext context);
}
```

Block semantics:

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

Example affordances:

- `WALK_SUPPORT`
- `STEP_UP`
- `DROP_EDGE`
- `CLIMB_UP`
- `CLIMB_DOWN`
- `SWIM_SURFACE`
- `SWIM_SUBMERGED`
- `BLOCKED`
- `HAZARD`

This lets water explicitly expose “swimmable” without pretending to be a step-up support.

## Planner Decomposition

The current planner stack needs to become a pipeline.

```text
Goal -> RoutePlanner -> RoutePlanModel
RoutePlanModel -> SegmentPlanner -> RouteSegmentModel
RouteSegmentModel -> TraversalPlanner -> TraversalModel
TraversalModel -> FramePlanner -> TraversalIntentModel
TraversalIntentModel -> InputProjector -> ControlFrameModel
```

Responsibilities:

- `RoutePlanner`: finds the coarse route and handles long-distance segmentation.
- `SegmentPlanner`: turns route steps into segment-level boundaries and frontier policy.
- `TraversalPlanner`: emits precise `WALK`, `JUMP`, `CLIMB`, `DROP`, `SWIM` traversals with geometry.
- `FramePlanner`: decides what the current frame should do inside the active traversal.
- `InputProjector`: translates traversal intent into camera + input booleans, nothing more.

## Traversal Is the Missing Contract

The most important model in the rebuild is `TraversalModel`.

```java
public record TraversalModel(
        TraversalType type,
        NavigationAnchorModel entryAnchor,
        TraversalGeometryModel geometry,
        AlignmentWindowModel alignmentWindow,
        SupportWindowModel supportWindow,
        InputProfileKey inputProfile,
        ProgressMetricKey progressMetric,
        FailurePolicyKey failurePolicy)
        implements TravelerDecisionModel {}
```

That one model closes the gap between:

- evaluation
- steering
- input projection
- progress monitoring
- recovery
- debug rendering

### Example: Jump

`TraversalModel` for jump carries:

- entry block / takeoff anchor
- allowed yaw band
- allowed lateral error
- landing zone
- airborne corridor
- commit conditions
- expected progress metric

Then:

- traversal planner validates the jump
- frame planner aligns and commits it
- projector knows which axes are legal during jump phases
- progress monitor knows the difference between setup, commit, airborne, and landing
- debug render shows the actual jump geometry, not a guessed line

### Example: Climb

Climb traversal carries:

- climb surface anchor
- climb face / column
- climb direction (`UP`, `DOWN`, `LEVEL`)
- entry window
- exit window
- smoothing preservation rule

## Recovery Is Not a Global Panic Button

Recovery becomes typed and phase-aware.

```text
TraversalProgressSnapshot
-> FailureClassifier
-> FailureKind
-> RecoveryPlanner
-> RecoveryAction
```

Failure kinds must be explicit:

- `NO_PROGRESS`
- `PATH_DIVERGENCE`
- `ACTION_SETUP_TIMEOUT`
- `TRAVERSAL_ABORTED`
- `SEGMENT_STITCH_FAILURE`
- `WORLD_STATE_INVALIDATED`
- `UNLOADED_FRONTIER`

Recovery actions must also be explicit:

- `NONE`
- `MICRO_REPAIR`
- `REBUILD_TRAVERSAL`
- `REPLAN_SEGMENT`
- `REPLAN_ROUTE`
- `STOP_WITH_REPORT`

This is better than the current pattern where unrelated problems collapse into the same “stuck” branch.

## Debug Render Reads Pipeline State

Debug rendering must come from `NavigationPipelineStateModel`, not from whichever raw path result arrived most recently.

Required layers:

- `ACTIVE_SEGMENT`
- `PREPARED_SEGMENT`
- `PENDING_SEARCH`
- `LATEST_REJECTED_SEARCH`
- `TARGET`
- `FAILURE_JUNCTION`

This is what keeps old/new segments visible at the same time without lying about what the bot is actually following.

## Settings Become a Real Domain

`Setting<T>` and `TravelerSettings` are a good start, but settings must map to the rebuilt domains:

- `RouteSettingsModel`
- `TraversalExecutionSettingsModel`
- `ProgressMonitorSettingsModel`
- `RecoverySettingsModel`
- `DebugRenderSettingsModel`
- `BehaviorSettingsModel`

Each policy receives only the settings model it needs.

## Target Package Map

```text
modules/core/src/main/java/dev/traveler/core/common/model
modules/core/src/main/java/dev/traveler/core/common/contract
modules/core/src/main/java/dev/traveler/core/common/registry
modules/core/src/main/java/dev/traveler/core/common/settings
modules/core/src/main/java/dev/traveler/core/common/diagnostic

modules/core/src/main/java/dev/traveler/core/world/behavior/api
modules/core/src/main/java/dev/traveler/core/world/behavior/model
modules/core/src/main/java/dev/traveler/core/world/behavior/context
modules/core/src/main/java/dev/traveler/core/world/behavior/registry
modules/core/src/main/java/dev/traveler/core/world/behavior/standard
modules/core/src/main/java/dev/traveler/core/world/geometry

modules/core/src/main/java/dev/traveler/core/route/api
modules/core/src/main/java/dev/traveler/core/route/goal
modules/core/src/main/java/dev/traveler/core/route/search
modules/core/src/main/java/dev/traveler/core/route/segment
modules/core/src/main/java/dev/traveler/core/route/longdistance

modules/core/src/main/java/dev/traveler/core/navigation/api
modules/core/src/main/java/dev/traveler/core/navigation/session
modules/core/src/main/java/dev/traveler/core/navigation/traversal
modules/core/src/main/java/dev/traveler/core/navigation/frame
modules/core/src/main/java/dev/traveler/core/navigation/control
modules/core/src/main/java/dev/traveler/core/navigation/progress
modules/core/src/main/java/dev/traveler/core/navigation/recovery
modules/core/src/main/java/dev/traveler/core/navigation/debug

modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/adapter
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/input
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/render
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/report
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/command
```

## Why This Is Better

This structure is better because:

1. one feature no longer redefines the laws of another feature
2. models have families, so navigation concepts become searchable and teachable
3. behavior semantics become reusable across route, traversal, and diagnostics
4. recovery becomes explainable instead of heuristic soup
5. platform upgrades become easier because `mc` is only a bridge
6. tuning jump/climb/swim/smoothing becomes local policy work instead of cross-cutting surgery
7. beginners can add a behavior or traversal by filling a known set of contracts

## Migration Principle

We do not freeze development for a “big bang rewrite.” The migration path is:

1. introduce the contracts beside the current code
2. migrate one pipeline stage at a time
3. keep compatibility shims only as short-lived adapters
4. delete legacy abstractions as soon as their replacements are wired and tested

That is how we turn the current mod into a framework without losing the execution quality it already has.
