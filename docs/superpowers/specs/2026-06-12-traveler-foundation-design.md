# Traveler Foundation Design

Date: 2026-06-12

## Goal

Build the first enterprise-grade foundation for Traveler, a client-side Minecraft pathfinding mod with a multi-loader architecture. This first milestone covers:

- A Java multi-module Gradle project.
- A pure Java `core` module with zero Minecraft, loader, Brigadier, or BuildMyCommand dependencies.
- A Minecraft `1_21_11` version family with `common`, `fabric`, and future `neoforge` modules.
- A small but real pathfinding core inspired by gdx-ai concepts, implemented from scratch.
- A Fabric-first client runtime path with BuildMyCommand-backed commands.
- Quality gates and tests suitable for later SonarLint/SonarQube analysis.

The MVP is intentionally client-only. It should not add server behavior.

## Decisions

- Language: Java.
- Namespace: `dev.traveler`.
- Mod id: `traveler`.
- Minecraft target family: `1.21.11`.
- Mappings: Mojang official mappings everywhere.
- Loader priority: Fabric first.
- NeoForge: prepare structure later, but do not make it the first runtime target.
- Pathfinding reference: gdx-ai is architectural inspiration only. No copied code and no gdx-ai dependency.
- Command framework: BuildMyCommand for Minecraft client commands, outside `core`.

## References

- Ghidra uses broad module families such as `Framework` and `Features`, with Gradle-driven inclusion and centralized build scripts. Traveler should follow the same spirit with a stable framework module and thin runtime adapters.
- gdx-ai pathfinding separates `Graph`, `Connection`, `Heuristic`, `PathFinder`, path containers, interruptible requests, and smoothing. Traveler should keep those ideas while adapting the domain model to Minecraft movement.
- BuildMyCommand provides route DSL, annotations, suggestions, permissions, middleware, help APIs, and platform adapters. Traveler should use it for client command surfaces, never for `core`.

Reference links:

- https://github.com/NationalSecurityAgency/ghidra
- https://github.com/NationalSecurityAgency/ghidra/blob/master/settings.gradle
- https://github.com/NationalSecurityAgency/ghidra/blob/master/gradle/support/settingsUtil.gradle
- https://github.com/NationalSecurityAgency/ghidra/tree/master/Ghidra/Framework
- https://github.com/NationalSecurityAgency/ghidra/tree/master/Ghidra/Features
- https://github.com/libgdx/gdx-ai/tree/master/gdx-ai/src/com/badlogic/gdx/ai
- https://github.com/libgdx/gdx-ai/tree/master/gdx-ai/src/com/badlogic/gdx/ai/pfa
- https://github.com/zolkers/BuildMyCommand
- https://raw.githubusercontent.com/zolkers/BuildMyCommand/master/PLATFORM-INFO.md

## Project Layout

```text
traveler/
  build-logic/
    src/main/java/dev/traveler/gradle/...
  gradle/
  modules/
    core/
      src/main/java/dev/traveler/core/...
      src/test/java/dev/traveler/core/...
    mc/
      1_21_11/
        common/
          src/main/java/dev/traveler/mc/v1_21_11/common/...
        fabric/
          src/main/java/dev/traveler/mc/v1_21_11/fabric/...
        neoforge/
          src/main/java/dev/traveler/mc/v1_21_11/neoforge/...
```

Gradle should use explicit module names and project directories. A later iteration may add a Ghidra-like helper for discovering modules by convention, but the MVP should prefer explicit registration for clarity.

## Dependency Boundaries

`modules/core`:

- Depends only on the JDK at runtime.
- May use JUnit 5 and test utilities in test scope.
- Must not import Minecraft, Fabric, NeoForge, Brigadier, Mixin, or BuildMyCommand.

`modules/mc/1_21_11/common`:

- Depends on `core`.
- May import Minecraft classes and mixins because the module is version-specific.
- Contains Minecraft adapters, block classification, movement profile adaptation, shared client event abstractions, debug state, and shared command declarations if they can stay loader-neutral.
- Must not contain Fabric or NeoForge bootstrap code.

`modules/mc/1_21_11/fabric`:

- Depends on `common`.
- Contains only Fabric client entrypoint, Fabric event registration, BuildMyCommand Fabric registration, and Fabric-specific metadata.
- Must not contain core pathfinding logic.

`modules/mc/1_21_11/neoforge`:

- Not required as a runtime target in this MVP.
- May be created as structure later, but should not slow down the Fabric-first path.

## Core Pathfinding Design

The `core` module acts as the stable framework. It should expose small contracts and independent implementations:

```text
dev.traveler.core
  graph/
    Graph<N>
    Connection<N>
    Heuristic<N>
    GraphPath<N>
    MutableGraphPath<N>
  path/
    Pathfinder<N>
    PathfinderRequest<N>
    PathfinderResult<N>
    PathfinderStatus
    AStarPathfinder<N>
    PathfinderQueue<N>
  smooth/
    PathSmoother<N>
    SmoothablePath<N>
  world/
    BlockPosition
    EntityDimensions
    MovementCapabilities
    MovementProfile
    TraversalCost
    TraversalRules
```

Minimum behavior:

- Synchronous A* for unit tests and simple command/debug flows.
- Interruptible A* through `search(request, budgetNanos)`.
- Injectable heuristics.
- Abstract traversal costs.
- Mutable path containers for nodes and, if needed, connections.
- Minimal path smoothing that removes intermediate points when a supplied line-of-walk predicate says the movement is valid.
- In-memory grid test world with obstacles and weighted costs.

Minecraft physics in `core` must remain abstract. `MovementProfile`, `MovementCapabilities`, and `TraversalRules` can represent concepts such as step height, fall limits, entity dimensions, fluid handling, and block passability, but they cannot depend on Minecraft classes.

## Minecraft/Fabric MVP Design

Shared Minecraft version module:

```text
modules/mc/1_21_11/common
  adapter/
    MinecraftWorldSnapshot
    MinecraftBlockClassifier
    MinecraftMovementProfileAdapter
  events/
    TravelerClientEventBus
    TravelerTickEvents
  command/
    TravelerCommandModule
  debug/
    PathfinderDebugState
```

Fabric module:

```text
modules/mc/1_21_11/fabric
  TravelerFabricClientMod
  FabricCommandBootstrap
  FabricEventBootstrap
```

Runtime behavior:

- Register a Fabric client entrypoint only.
- Use BuildMyCommand for client command registration.
- Add a first command such as `/traveler path test` or `/traveler path block <x> <y> <z>`.
- The command runs or schedules a debug pathfinding request.
- The result is stored in `PathfinderDebugState`.
- The MVP does not take control of the player.
- The MVP does not implement the final GLSL renderer, advanced input steering, or camera-driven movement. It should create boundaries that make those later additions natural.

## Quality And Testing

Build conventions should live in `build-logic`:

- `traveler.java-conventions`
- `traveler.test-conventions`
- `traveler.quality-conventions`
- Later: `traveler.minecraft-common`, `traveler.fabric`, `traveler.neoforge`

Initial tooling:

- Gradle Wrapper.
- Java toolchain.
- JUnit 5.
- JaCoCo for coverage reports on testable modules.
- Checkstyle or Spotless for style/format enforcement.
- Sonar-friendly code shape: small classes, low cyclomatic complexity, clear interfaces, limited mutable state, no dependency leakage.

Initial test mirrors:

- `AStarPathfinderTest`
- `PathfinderRequestTest`
- `MutableGraphPathTest`
- `PathSmootherTest`
- `GridWorldTest`

Testing expectations:

- `core` should have the strongest coverage because it is pure JVM code.
- Minecraft/Fabric tests may start with compilation and adapter-level unit tests where practical.
- Fabric runtime smoke tests should come after the foundation compiles.
- NeoForge smoke tests should come later, especially because BuildMyCommand marks NeoForge as compile-tested rather than production-tested.

## Out Of Scope For This MVP

- Player autopilot.
- Rotation smoothing.
- ZQSD/camera-relative movement control.
- GLSL shader framework.
- Custom 3D renderer.
- Full Minecraft collision/physics parity.
- Full special block support.
- Server-side behavior.
- NeoForge runtime validation.

These are important follow-up milestones, but adding them to the first foundation would make the initial architecture harder to validate.

## Acceptance Criteria

- The repository contains the documented multi-module structure.
- `core` compiles without Minecraft or loader dependencies.
- The first pathfinding contracts and A* implementation are unit-tested.
- A fake grid world test suite proves normal paths, blocked paths, weighted costs, and budgeted/interruptible search.
- Fabric client module compiles and registers a basic Traveler command through BuildMyCommand.
- Build conventions and quality tooling are centralized.
- Architectural boundaries are visible in package names and Gradle dependencies.
- No code in `core` imports Minecraft, Fabric, NeoForge, Brigadier, Mixin, or BuildMyCommand.

## Risks And Mitigations

- Minecraft 1.21.11 loader setup may be sensitive to current Fabric tooling. Mitigation: Fabric-first, Mojmap-only, minimal runtime surface.
- BuildMyCommand NeoForge support is not fully runtime-validated. Mitigation: defer NeoForge runtime support and add a dedicated smoke-test milestone.
- A pathfinder that ignores Minecraft physics would become misleading. Mitigation: keep `core` movement abstractions explicit from the beginning, then adapt real Minecraft state in `common`.
- Overbuilding the renderer/input layer too early would dilute the foundation. Mitigation: defer player movement and rendering while reserving clean extension points.

## Follow-Up Milestones

1. Implementation plan for foundation and mini pathfinder.
2. Fabric runtime smoke test and first in-game command validation.
3. Minecraft block classification and movement profile adapter.
4. Debug path rendering.
5. Human-like movement controller with camera-relative input and smooth rotations.
6. GLSL shader and custom 3D rendering framework.
7. NeoForge runtime validation.
