# Traveler Navigation Architecture

Status: Draft for review
Date: 2026-06-13
Scope: `modules/core` navigation, pathfinding, movement planning, render debug contracts, and Minecraft adapters.

## Purpose

Traveler does not use MVC for navigation. MVC is built around user interface state, while a pathfinder is a decision pipeline. Traveler uses a strict **World -> Route -> Plan -> Control -> Adapter** architecture.

This document is normative. New pathfinding, navigation, movement, camera, input, render-debug, and Minecraft adapter work must follow these rules.

## Core Principle

Only one layer may decide behavior for a navigation frame.

Lower layers describe what exists or what is possible. Higher projection layers convert an already-decided plan into outputs. If a class changes behavior based on path action, camera state, input state, and player state at the same time, that class is in the wrong layer.

## Canonical Pipeline

```text
World Model -> Route Plan -> Navigation Plan -> Control Projection -> Runtime Adapter
```

Debug rendering observes the pipeline. It never changes decisions.

## Layers

### 1. World Model

Responsibility:
- Describe blocks, shapes, fluids, body clearance, surface height, collision, and movement feasibility.
- Model special blocks through dedicated behavior classes, not numeric penalties alone.
- Answer questions such as "can the player move from A to B?" and "what action is required?"

Allowed concepts:
- `BlockPosition`
- `SurfaceNode`
- `SurfaceBlock`
- `MovementCapabilities`
- block behavior classes such as slab, stair, fluid, full block
- collision and clearance scoring

Forbidden concepts:
- camera yaw or pitch
- keyboard inputs
- render state
- navigation phase timing
- Minecraft loader APIs

### 2. Route Plan

Responsibility:
- Run graph search.
- Smooth and score paths.
- Preserve every semantic action landmark.
- Produce a route that describes steps, not just points.

A route step must carry enough intent for navigation:
- source and target position
- movement action: walk, step up, jump, drop, swim, special block transition
- surface/body clearance score when available
- required preservation flag when smoothing must not remove it

Smoothing rule:
- Smoothing may remove geometric noise.
- Smoothing must not remove action boundaries.
- Smoothing must not turn a jump, step, drop, stair transition, fluid transition, or clearance-sensitive passage into an unlabelled straight segment.

### 3. Navigation Plan

Responsibility:
- Be the single source of truth for frame behavior.
- Convert route + agent state into a `NavigationFramePlan`.
- Own navigation phases and policies.

Navigation phases:
- `APPROACH`: move toward the next relevant route step.
- `ALIGN`: preserve momentum while camera/body alignment catches up.
- `EXECUTE_ACTION`: perform jump, step, drop, swim, stair transition, or other semantic movement.
- `RECOVER`: handle blocked, overshot, or unstable movement.
- `ARRIVE`: decelerate and finish.

The Navigation Plan owns:
- action timing
- jump hold windows
- recovery decision
- camera target choice
- movement target choice
- speed intent
- line holding intent
- strafe/back/forward intent at the semantic level
- route progress decisions

The Navigation Plan must produce one immutable frame output:

```text
NavigationFramePlan
  phase
  routeStep
  movementTarget
  movementVector
  cameraTarget
  actionIntent
  speedIntent
  toleranceProfile
  debugSignals
```

Until this type exists, existing classes may continue to return smaller records, but every refactor should move toward this single output.

### 4. Control Projection

Responsibility:
- Convert `NavigationFramePlan` into camera angles and keyboard state.
- Apply smoothing and hysteresis to outputs.
- Never decide path behavior.

Allowed decisions:
- key press hysteresis
- camera angular velocity limits
- deadzones
- output clamping

Forbidden decisions:
- choosing when to jump
- choosing whether a node is skipped
- choosing whether a path is valid
- choosing a recovery phase
- changing camera target because of a block type

If projection needs a special-case rule, the rule belongs in Navigation Plan as a named policy.

### 5. Runtime Adapter

Responsibility:
- Read Minecraft state.
- Convert Minecraft state to core agent state.
- Apply control outputs to Minecraft.
- Register Fabric/NeoForge events and commands.

Forbidden concepts:
- pathfinding heuristics
- navigation phases
- special block movement rules
- route smoothing
- scoring

Adapters may contain Minecraft imports. Core must never contain Minecraft, Fabric, NeoForge, Mixin, Brigadier, or BuildMyCommand imports.

### 6. Debug Rendering

Responsibility:
- Render the latest route, current phase, action nodes, target, clearance hints, and control vectors.
- Make behavior visible without changing it.

Rendering must show the Navigation Plan, not reconstruct behavior from raw path nodes.

## Package Convention

Use packages by responsibility, not by symptom.

Required direction:

```text
dev.traveler.core.world
  -> dev.traveler.core.path / dev.traveler.core.world.navigation
  -> dev.traveler.core.navigation.route
  -> dev.traveler.core.navigation.plan
  -> dev.traveler.core.navigation.control
  -> dev.traveler.core.navigation.runtime
```

Existing packages may be migrated gradually:
- `navigation.follow` should become route progress and route cursor code.
- `navigation.steering` should become movement-vector planning under Navigation Plan.
- `navigation.locomotion` should become action timing policy under Navigation Plan.
- `navigation.input` should become control projection only.
- `navigation.camera` should become camera target policy plus camera output projection, separated by package.

Minecraft packages remain adapters:

```text
dev.traveler.mc.v<version>.common.adapter
dev.traveler.mc.v<version>.common.command
dev.traveler.mc.v<version>.fabric
dev.traveler.mc.v<version>.neoforge
```

`common` may depend on Minecraft mappings for shared version logic. Loader-specific code belongs only in `fabric` or `neoforge`.

## Policy Convention

Every behavior-changing rule must be a named policy.

Examples:
- `ActionTimingPolicy`
- `CameraTargetPolicy`
- `MovementVectorPolicy`
- `LineHoldingPolicy`
- `RecoveryPolicy`
- `RouteProgressPolicy`
- `SmoothingPreservationPolicy`

Policies must:
- live in Navigation Plan or Route Plan, depending on ownership
- be pure or mostly pure
- expose inputs and outputs through records
- have focused tests
- avoid hidden mutable state unless the state is explicitly part of the policy contract

Policies must not:
- call Minecraft APIs
- set keyboard state
- render
- inspect command state
- mutate route data in place

## Data Ownership Rules

World Model owns feasibility.

Route Plan owns route shape and semantic route steps.

Navigation Plan owns current phase and frame-level behavior.

Control Projection owns output smoothing, key hysteresis, and camera velocity limits.

Runtime Adapter owns IO with Minecraft.

Debug Rendering owns visual representation only.

## Anti-Spaghetti Rules

These are hard rules:

- Do not add behavior rules directly to adapters.
- Do not add behavior rules directly to renderers.
- Do not add behavior rules directly to input projection.
- Do not add behavior rules directly to camera angle smoothing.
- Do not make smoothing decide movement action.
- Do not make graph search know about keyboard inputs.
- Do not make route progress depend on key state.
- Do not use one-off boolean flags when a phase or policy would be clearer.
- Do not exceed two nesting levels in production code.
- Do not duplicate behavior checks across camera, input, follow, and locomotion.

If a fix seems to require touching camera, input, locomotion, and follow at once, stop and move the rule into Navigation Plan.

## Adding A New Movement Behavior

Use this sequence:

1. Add or update World Model behavior if Minecraft physics changed.
2. Add route semantics so the path records the action.
3. Add or update a Navigation Plan policy that chooses when to execute it.
4. Add Control Projection tests if the output keys or camera smoothing change.
5. Add adapter code only if Minecraft IO needs a new mapping.
6. Add debug rendering only to display the behavior.

The same behavior must not be rediscovered independently by multiple layers.

## Testing Rules

Every behavior rule needs tests at the layer that owns it.

Required test categories:
- World Model: special blocks, collision, body clearance, movement feasibility.
- Route Plan: A*, smoothing, semantic action preservation, clearance scoring.
- Navigation Plan: phase transitions, route progress, action timing, recovery.
- Control Projection: input hysteresis, camera smoothing, key mapping.
- Runtime Adapter: Minecraft state conversion and loader registration boundaries.
- Architecture: forbidden imports and package direction.

Regression tests must be written before fixes.

## Profiling Rules

Performance-sensitive changes must be profiled with JFR or an equivalent profiler.

Profile these surfaces:
- surface graph search
- smoothing
- navigation frame planning
- control projection
- render debug generation

Hotspots in Route Plan may justify caches. Hotspots in Control Projection are suspicious because projection should be cheap.

## Migration Direction

The current implementation is allowed to remain temporarily, but new work should migrate toward this shape:

```text
NavigationController
  delegates to NavigationFramePlanner

NavigationFramePlanner
  owns phase selection and calls named policies

ControlProjector
  turns NavigationFramePlan into MovementIntent and CameraAngles

MinecraftClientNavigationAdapter
  applies only final outputs
```

The target end state is that `NavigationController` is orchestration only and contains no behavior rules.

## Review Checklist

Before merging any navigation change, answer these questions:

- Which layer owns this behavior?
- Is the rule represented by a named policy?
- Is there exactly one source of truth for the decision?
- Can the code be tested without Minecraft?
- Does this change preserve route action landmarks?
- Does projection only project?
- Does the adapter only adapt?
- Are package dependencies pointing in the canonical direction?
- Is production nesting depth at most two?
- Did a profiler run if this can affect path search, smoothing, or per-frame navigation?

If any answer is no, redesign before coding.
