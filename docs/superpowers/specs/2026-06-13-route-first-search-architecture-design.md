# Route-First Search Architecture Design

Date: 2026-06-13

## Goal

Make Traveler path search produce a semantic route in `core` instead of letting the Minecraft command module own search, smoothing, and navigation conversion.

The route must preserve the movement decision for every traversed segment so navigation does not re-infer jumps, drops, stairs, slabs, or normal walking from raw height deltas.

## Decisions

- `core` owns route search, route diagnostics, route smoothing output, and movement actions.
- `mc/1_21_11/common` owns Minecraft snapshot capture, command messaging, debug state updates, and async job wiring only.
- `SurfaceTraversalGraph` remains the low-level graph for now, but movement decision evaluation is centralized so graph expansion and route building cannot disagree.
- `NavigationPath` may carry per-segment route actions. Point-only paths remain supported for direct/block fallback.
- Chat/debug output must expose why a path failed or what semantic movement was planned.
- Production code keeps max two nesting levels through guard clauses and small policy objects.

## Package Convention

```text
dev.traveler.core.route
  RoutePath
  RouteStep
  RouteSearchResult
  RouteSearchDiagnostics
  RouteSearchFailureReason
  RouteSearchService
  RouteSearchSettings

dev.traveler.core.world.navigation
  low-level surface graph, line-of-walk, smoothing, scoring, movement evaluation helpers

dev.traveler.mc.v1_21_11.common.command
  command route, snapshot capture orchestration, result presentation
```

The route package is intentionally above world navigation. It is the stable contract consumed by commands, debug render, and navigation.

## Expected Behavior

- A surface path returns both the display/debug path and a `RoutePath`.
- Each `RouteStep` contains `from`, `to`, `MovementAction`, and cost.
- Drops are represented as `DROP`, not as generic walk.
- Jumps and larger vertical moves remain preserved through smoothing.
- Special block behaviors stay in `core.world.behavior.special`; Minecraft only classifies block state into core `SurfaceBlock` data.
- If a target is passable but unreachable, diagnostics must separate `target passability` from route failure reason.

## Quality Gates

- Unit tests cover route step ordering, actions, diagnostics, and navigation conversion.
- Architecture tests prevent command code from importing A*, graph, smoother, or surface traversal graph directly.
- `:core:test` and targeted `:mc_1_21_11_common:test` must pass before final `check`.

