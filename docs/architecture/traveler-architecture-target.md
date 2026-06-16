# Traveler Target Architecture

## Purpose

Traveler already does many things well at runtime, but the codebase is hard to evolve because one movement is described differently by route search, traversal execution, input projection, recovery, and debug rendering. This document defines the corrected target architecture: small public APIs, explicit pipeline stages, strong world semantics, and strict `api/internal` boundaries.

## Diagnosis

The problem is not "too few abstractions." The problem is "too many overlapping meanings."

Today:

- [RouteSearchService](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java) mixes pathfinding, smoothing, progress fallback, and diagnostics assembly.
- [NavigationFramePlanner](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java) mixes route progress, action choice, jump/climb steering, timing, camera targeting, speed, and debug detail.
- [MovementProgressMonitor](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementProgressMonitor.java) re-derives execution truth from planner artifacts instead of consuming a stable traversal contract.
- [ControlProjector](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/control/ControlProjector.java) translates vectors into inputs without a first-class traversal profile.
- [BlockBehavior](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/behavior/BlockBehavior.java) still carries movement-evaluation assumptions that belong higher in the stack.
- [PathDebugRenderModel](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/PathDebugRenderModel.java) is forced to understand internal pipeline details.

That creates a mathematical gap:

1. route logic thinks in blocks and surfaces
2. traversal logic thinks in movement phases and constraints
3. input logic thinks in camera-relative axes
4. recovery logic thinks in thresholds and time windows
5. render logic thinks in whatever intermediate objects it can see

When these disagree, tuning one detail becomes cross-cutting surgery.

## Audit Anchors

### Model families that exist but are not aligned

1. **Spatial values**
   - [BlockPosition](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/block/BlockPosition.java)
   - [NavigationPoint](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/spatial/NavigationPoint.java)
   - [SurfaceNode](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/surface/SurfaceNode.java)
   - [RenderVertex](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/render/RenderVertex.java)

2. **Path/segment values**
   - [RoutePath](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RoutePath.java)
   - [RouteStep](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/route/RouteStep.java)
   - [NavigationPath](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/follow/NavigationPath.java)
   - [NavigationSegmentIntent](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/follow/NavigationSegmentIntent.java)

3. **Movement intent values**
   - [MovementAction](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/world/behavior/decision/MovementAction.java)
   - [LocomotionAction](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/locomotion/LocomotionAction.java)
   - [ActionIntent](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/plan/ActionIntent.java)
   - [LocomotionPlan](/C:/Users/vriegert/traveler/modules/core/src/main/java/dev/traveler/core/navigation/locomotion/LocomotionPlan.java)

These families matter, but they do **not** need a universal parent tree. They need clear ownership and stable boundaries.

### Coupling hotspots that justify the redesign

1. `NavigationFramePlanner` is the current policy choke point.
2. `NavigationFramePlan` and `NavigationControlFrame` are overloaded cross-layer carriers.
3. Route generation, smoothing, and execution targets are too tightly fused.
4. Recovery duplicates movement semantics instead of consuming them.
5. Debug/render and failure reports are wired into live navigation internals.

## The 10/10 Correction

The first rebuild direction was close, but too abstract. A great Java architecture here is **not** "every public record implements `TravelerModel`." A great Java architecture is:

- a tiny common kernel
- small, explicit public APIs
- plain immutable values where possible
- `api/internal` as the main boundary
- domain-specific contracts instead of universal generic families

This is closer to how strong Java projects age well.

## Architecture Laws

1. Core defines the laws; features attach to the laws.
2. Behaviors describe the world; they never drive inputs or recovery.
3. Route planning, traversal planning, frame planning, recovery, and debug all share the same traversal truth.
4. `mc`/`fabric` is a bridge, never a second core.
5. Public API is intentionally small.
6. Internal code is free to move as long as API contracts stay stable.

## Layer Model

```text
common
  -> tiny shared interfaces and utilities
world
  -> block semantics, affordances, geometry
route
  -> route goals, route search, long-distance routing
navigation
  -> traversal planning, frame planning, control projection, progress, recovery, debug
platform
  -> Minecraft/Fabric adapters, commands, reports, render hooks
```

Dependency rules:

- `common` depends on nothing else
- `world` depends on `common`
- `route` depends on `common` + `world`
- `navigation` depends on `common` + `world` + `route`
- `platform` depends on all core APIs, but core never depends on `platform`

## Package Strategy: `api` and `internal`

This is the most important structural rule.

```text
dev.traveler.core.<domain>.api
dev.traveler.core.<domain>.internal
```

Rules:

- everything in `api` is intentionally consumable outside the domain
- everything in `internal` is implementation detail
- cross-domain production code depends on `api`, not `internal`
- tests may inspect `internal`; production callers should not

This gives us real modularity without overbuilding a shared type system.

## Lean Common Kernel

The common layer should contain only contracts that create real leverage:

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

That is enough. We do **not** force request/state/result/decision marker hierarchies across the whole project.

## Public API Surfaces

### World API

- `BlockBehavior`
- `BehaviorResolver`
- `BlockSemantics`
- `TraversalAffordance`
- `SupportSemantics`
- `FluidSemantics`
- `CollisionSemantics`

### Route API

- `RouteGoal`
- `RoutePlanner`
- `RoutePlan`
- `RouteSegment`
- `LongDistancePlanner`

### Navigation / Traversal API

- `TraversalKind`
- `Traversal`
- `TraversalGeometry`
- `TraversalPlanner`
- `TraversalController`
- `TraversalProgressPolicy`
- `TraversalRecoveryPolicy`
- `NavigationSession`
- `NavigationSnapshot`

### Debug / Diagnostics API

- `DebugLayer`
- `DebugFrame`
- `FailureReport`

## Value Object Discipline

We keep plain immutable values plain.

Examples:

- `BlockPosition`
- `NavigationPoint`
- `SurfaceNode`
- `RenderVertex`

These are related values, but they are not the same abstraction. They stay separate unless there is a real shared behavior to extract. Similar shape is not enough reason to force inheritance.

## Behaviors: World Semantics, Not Micro-AI

Behaviors are fundamental, but they must stay declarative.

Bad behavior design:

- decides when to jump
- owns recovery rules
- rewrites smoothing directly
- manipulates input booleans

Good behavior design:

- describes support
- describes collision
- describes fluid rules
- exposes traversal affordances
- exposes local hazards or restrictions

Behavior contract:

```java
package dev.traveler.core.world.behavior.api;

import dev.traveler.core.world.behavior.context.BlockBehaviorContext;

public interface BlockBehavior {
    BlockSemantics describe(BlockBehaviorContext context);
}
```

Semantics shape:

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

This is the right place to encode "water is swimmable but not a raised step support."

## Planner Decomposition

The pipeline becomes explicit:

```text
Goal -> RoutePlanner -> RoutePlan
RoutePlan -> SegmentPlanner -> RouteSegment
RouteSegment -> TraversalPlanner -> Traversal
Traversal -> FramePlanner -> TraversalIntent
TraversalIntent -> InputProjector -> ControlFrame
```

Responsibilities:

- `RoutePlanner`: coarse route and long-distance strategy
- `SegmentPlanner`: segment boundaries and segment stitching rules
- `TraversalPlanner`: concrete `WALK`, `JUMP`, `CLIMB`, `DROP`, `SWIM` traversals
- `FramePlanner`: phase-aware per-frame intent for the active traversal
- `InputProjector`: translates intent into inputs and camera goals

## Traversal Is the Shared Truth

The most important contract in the system is `Traversal`.

```java
package dev.traveler.core.navigation.api;

public interface Traversal {
    TraversalKind kind();
    TraversalGeometry geometry();
    TraversalController controller();
    TraversalProgressPolicy progressPolicy();
    TraversalRecoveryPolicy recoveryPolicy();
}
```

This is what closes the gap between route logic, execution, monitoring, recovery, and debug.

### Jump traversal carries

- entry/takeoff anchor
- lateral alignment window
- allowed yaw band
- commit conditions
- landing zone
- airborne corridor
- progress policy
- recovery policy

### Climb traversal carries

- climb column / face
- direction (`UP`, `DOWN`, `LEVEL`)
- entry window
- exit window
- preserved nodes / no-smoothing constraints
- climb progress policy
- climb recovery policy

### Swim traversal carries

- surface preference
- submerged allowance
- fluid exit rules
- swim corridor tolerance
- swim progress policy

## Recovery Is Typed, Not Global

Recovery should consume traversal-aware progress snapshots:

```text
TraversalProgressSnapshot
-> FailureClassifier
-> TraversalFailure
-> RecoveryPlanner
-> RecoveryAction
```

Failure kinds:

- `NO_PROGRESS`
- `PATH_DIVERGENCE`
- `ACTION_SETUP_TIMEOUT`
- `TRAVERSAL_ABORTED`
- `SEGMENT_STITCH_FAILURE`
- `WORLD_STATE_INVALIDATED`
- `UNLOADED_FRONTIER`

Recovery actions:

- `NONE`
- `MICRO_REPAIR`
- `REBUILD_TRAVERSAL`
- `REPLAN_SEGMENT`
- `REPLAN_ROUTE`
- `STOP_WITH_REPORT`

This is far better than today's implicit "everything eventually becomes stuck or divergence."

## Debug Render Reads Navigation Snapshot

Debug must read stable snapshot state, not planner internals.

Required layers:

- `ACTIVE_SEGMENT`
- `PREPARED_SEGMENT`
- `PENDING_SEARCH`
- `LATEST_REJECTED_SEARCH`
- `TARGET`
- `FAILURE_JUNCTION`

That is how we keep old/new segments visible without lying about what is actually active.

## Settings Become a Real Domain

`Setting<T>` and `TravelerSettings` stay, but settings sections become small domain units implementing `SettingsSection`.

Examples:

- `RouteSearchSettings`
- `LongDistanceRouteSettings`
- `TraversalExecutionSettings`
- `PathFollowSettings`
- `MovementHealthSettings`
- `DebugRenderSettings`

Each policy receives the smallest settings section it needs.

## Target Package Map

```text
modules/core/src/main/java/dev/traveler/core/common/api
modules/core/src/main/java/dev/traveler/core/common/internal
modules/core/src/main/java/dev/traveler/core/common/settings

modules/core/src/main/java/dev/traveler/core/world/behavior/api
modules/core/src/main/java/dev/traveler/core/world/behavior/internal
modules/core/src/main/java/dev/traveler/core/world/behavior/context
modules/core/src/main/java/dev/traveler/core/world/geometry

modules/core/src/main/java/dev/traveler/core/route/api
modules/core/src/main/java/dev/traveler/core/route/internal
modules/core/src/main/java/dev/traveler/core/route/goal
modules/core/src/main/java/dev/traveler/core/route/longdistance

modules/core/src/main/java/dev/traveler/core/navigation/api
modules/core/src/main/java/dev/traveler/core/navigation/internal
modules/core/src/main/java/dev/traveler/core/navigation/control
modules/core/src/main/java/dev/traveler/core/navigation/recovery
modules/core/src/main/java/dev/traveler/core/navigation/debug

modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/adapter
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/input
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/render
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/report
modules/mc/1_21_11/fabric/src/main/java/dev/traveler/mc/command
```

## What We Explicitly Avoid

To keep this architecture strong, Traveler should avoid:

- a universal "everything is a `TravelerModel`" tree
- giant manager classes
- public exposure of internal planner DTOs
- behavior classes that secretly drive navigation
- debug code that depends on internal planner structure
- platform modules that reimplement core logic

## Why This Is Better

This version is better because:

1. the public API is small enough to learn
2. internal refactors become cheaper
3. behaviors become reusable semantics instead of mixed-control objects
4. jump/climb/swim tuning becomes local traversal work
5. recovery becomes explainable and traversal-aware
6. the Minecraft module becomes easier to swap and upgrade
7. beginners can add a feature by implementing a few contracts, not by editing five unrelated subsystems

## Migration Principle

We do not do a big-bang rewrite. We migrate by stages:

1. introduce `api/internal` boundaries
2. introduce the lean common kernel
3. extract behavior semantics API
4. extract route/traversal/navigation APIs
5. move current logic behind those APIs
6. delete legacy cross-layer carriers as soon as their replacements are proven

That is how Traveler becomes a framework without losing the runtime quality it already has.
