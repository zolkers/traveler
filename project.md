# Traveler Project Summary

## Vision

Traveler is a client-side Minecraft pathfinder and navigation framework. The goal is not only to find a valid path, but to produce a path that is comfortable to execute, debuggable, extensible, and ready for multiple Minecraft loaders.

The project is Fabric-first for now, Mojang mappings only, Java 21, and Gradle Kotlin DSL. NeoForge is intentionally kept out of the current runtime milestone.

## Module Architecture

### `:core` - `modules/core`

The core module owns the reusable Traveler engine:

- generic graph pathfinding and A* search;
- route search and surface traversal;
- movement models, traversal rules, block behavior contracts, and smoothing;
- navigation intent, steering/control models, and debug data models;
- event, command, render, and async job abstractions;
- tests for pathfinding, smoothing, architecture, and movement behavior.

`core` must stay pure Java. It must not import Minecraft, Fabric, NeoForge, Brigadier, Mixin, or BuildMyCommand.

### `:mc_1_21_11_common` - `modules/mc/1_21_11/common`

The common Minecraft module adapts Minecraft 1.21.11 concepts to core contracts:

- world snapshots and block classifiers;
- Minecraft block behavior resolution;
- command use cases and BuildMyCommand declarations;
- debug state shared by loader-specific bootstraps;
- client-side route/path job coordination.

Minecraft imports are allowed here when adapting MC data to core, but Fabric and NeoForge imports are not.

### `:mc_1_21_11_fabric` - `modules/mc/1_21_11/fabric`

The Fabric module is loader glue only:

- client entrypoint;
- Fabric event registration;
- Fabric command registration;
- Fabric render integration;
- Fabric-side navigation tick/render hooks.

It should not own pathfinding rules, smoothing logic, or block behavior policy.

## Core Conventions

- Production code nesting depth is max 2. Prefer guard clauses, early returns, small methods, and explicit state objects.
- Behavior changes should be test-first when practical.
- Add extension points instead of editing central logic repeatedly.
- Keep one responsibility per class. Split packages when a package becomes hard to scan.
- Avoid duplicated pathing rules across Minecraft and core. Minecraft should classify/adapt; core should decide.
- Keep debug models in core and rendering implementation in the loader module.
- Use conventional commits.

## Pathfinder Architecture

The pure graph layer exposes:

- `Graph<N>`;
- `Connection<N>`;
- `Heuristic<N>`;
- `GraphPath<N>`;
- `MutableGraphPath<N>`;
- `Pathfinder<N>`;
- `PathfinderRequest<N>`;
- `PathfinderResult<N>`;
- `PathfinderStatus`;
- `AStarPathfinder<N>`.

Negative connection costs are rejected. A* supports normal and budgeted search.

The surface route layer builds on top of that:

- `RouteSearchService` coordinates route search;
- `RouteSearchComponents` wires graph factory, smoother, selectors, and policies;
- `SurfaceTraversalGraph` exposes walkable surface nodes;
- `SurfaceConnectionProvider` composes adjacent, drop, jump, and special surface connections;
- `SurfaceClearanceScorer` and line-of-walk policies prefer safer routes with body clearance;
- `SurfaceSmoothingPolicy` preserves important action nodes and avoids cutting through risky geometry.

## Block Behavior Model

Special blocks are modeled as behavior classes, not simple penalty numbers.

The direction is:

- Minecraft resolves block state and voxel/collision information;
- behavior resolvers classify special cases;
- core movement logic consumes behavior contracts;
- route/smoothing/navigation preserve required actions.

Current important cases include:

- full blocks;
- air / empty space;
- slabs;
- stairs;
- fluids and waterlogged surfaces.

Future special cases should be added through resolver/behavior extension points, especially barriers, carpets, vines, ladders, trapdoors, doors, fences, walls, powder snow, honey, soul sand, magma, cactus, berry bushes, cobwebs, and climbable blocks.

## Smoothing Rules

The smoother is extension-oriented:

- `PathSmoother<N>` owns generic smoothing;
- `LineOfWalk<N>` decides whether a segment is physically valid;
- `PathNodePreservation<N>` prevents skipping important action nodes;
- `PathSmoothingSelector<N>` allows alternative smoothing strategies;
- surface smoothing uses player dimensions and clearance, not only block centers.

Important rule: smoothing may remove noise, but it must not remove nodes required for jumps, drops, special block handling, or safe clearance.

## Navigation Model

Navigation should not think in raw keys first. It should produce an intention and then map it to local player inputs.

The desired direction is built from:

- route tangent;
- lookahead target;
- lateral correction;
- obstacle and wall clearance;
- action setup for jump, stair, slab, drop, and similar transitions.

Input mapping should use player/camera space:

- forward/back from dot product against camera forward;
- strafe from dot product against camera right;
- jump/action from route step metadata;
- rotation through a smooth camera controller with anti-360 safety.

Strategic strafe is expected for turns, centering before actions, avoiding walls, and keeping a human-like line.

## Render And Debug

Render framework contracts live in core. Fabric only renders them.

Current debug direction:

- path line rendered higher through the player/body area rather than buried in blocks;
- nodes can be rendered as transparent block-sized volumes;
- debug data should distinguish safe, risky, action, and skipped/preserved nodes;
- chat debug reports should include route status, passability, failure reasons, node counts, costs, and anomalies.

Avoid debug visuals attached directly to the player when they make the scene noisy.

## Commands

Commands should be modular and route-based:

- core owns command metadata/catalog abstractions;
- Minecraft common owns BuildMyCommand declarations and command use cases;
- Fabric owns actual client registration;
- each command group should have a base route and clean subroutes.

Commands used during development include path test, path to block, navigation to block, debug/status helpers, and render toggles.

## Async Path Jobs

Pathfinding should not freeze the client.

The intended model is:

- capture an immutable world snapshot on the client thread;
- queue a `PathJob` on a single custom worker thread;
- expose `PathJobHandle` with status/cancel/result;
- apply final result back on the client thread;
- cancel older jobs of the same type when a newer destination replaces them;
- report queue/start/final/failure information in chat and debug state.

Core owns the async abstractions. Minecraft common coordinates snapshot capture and result application.

## Quality Gates

Main verification command:

```powershell
./gradlew.bat check
```

Useful targeted checks:

```powershell
./gradlew.bat :core:test
./gradlew.bat :mc_1_21_11_common:test
./gradlew.bat :mc_1_21_11_fabric:build
```

Performance checks should keep using JFR/flamegraph-style profiling around path searches. Recent profiling after the route/smoothing extensibility cleanup showed:

```text
found=25 searches=25 mode=smooth millis=1522 blockReads=1079330
```

Performance regressions should be treated as bugs, especially when snapshot capture, special block handling, smoothing, or clearance scoring changes.

## Recent Work

Recent important commits:

- `6f5bc42 refactor(mc): resolve block behaviors through chain`
- `d331471 refactor(route): compose search pipeline`
- `4578f51 refactor(path): make surface connections pluggable`
- `2dffea1 refactor(path): make smoothing selection pluggable`
- `b30c7b5 fix(path): compact flat special surface runs`
- `1ef2cd2 fix(path): route out of slab stair enclosures`
- `0bd368b test(architecture): guard command search boundaries`
- `a45133a feat(navigation): preserve route segment actions`
- `98be607 refactor(mc): delegate path search to core routes`
- `3fef140 feat(core): move route search into core`
- `2cf9aa5 feat(core): centralize surface movement decisions`

The latest full check passed before this document was added:

```powershell
./gradlew.bat check
```

## Next Priorities

- Add researched behaviors for barrier, carpet, vines, ladders, and other special blocks.
- Keep special block handling behavior-based, not penalty-based.
- Continue splitting large coordinators when responsibilities become mixed.
- Improve debug render layering and anomaly coloring.
- Keep navigation steering unified instead of adding isolated rules per symptom.
- Keep profiling after pathing/navigation changes.
