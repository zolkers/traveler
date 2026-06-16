# Pathfinder Kernel Modules

## Purpose

The pathfinder kernel is the composition seam for route search capabilities. It lets Traveler assemble a route-only pathfinder from explicit modules, replace those modules during migration, and keep route search behavior observable through stable diagnostics and active module descriptors.

The kernel is not a new top-level product identity. `Traveler` is the mod name. Internal architecture classes should use domain names such as `PathfinderKernel`, `TraversalModule`, `RouteSearchComponents`, and `BlockBehaviorResolver`, not prefixes such as `TravelerPathfinderKernel`.

## Goals

1. Make pathfinding capabilities interchangeable without changing callers.
2. Keep public contracts small and stable while implementation packages can move.
3. Report missing capabilities as typed route diagnostics instead of infrastructure failures.
4. Expose which enabled modules participated in a search.
5. Keep fallbacks conservative, disabled, and explicit.

## Package Shape

Kernel and capability packages follow the same vocabulary:

- `api`: public contracts used by other domains or platform composition code.
- `spi`: provider contracts implemented by modules to contribute route, traversal, behavior, smoothing, execution, recovery, or debug pieces.
- `impl`: default production implementations.
- `noop`: disabled fallbacks that make absence explicit and safe.
- `internal`: migration or implementation details that production callers outside the domain should not depend on.
- `testing`: reusable test fixtures only. Production code must not depend on it.

Current examples:

- `dev.traveler.core.pathfinder.kernel.api` exposes `PathfinderKernel`, `PathfinderKernelMode`, and `PathfinderKernelResult`.
- `dev.traveler.core.pathfinder.kernel.spi` exposes module descriptors and module types.
- `dev.traveler.core.pathfinder.kernel.impl` owns default kernel construction.
- `dev.traveler.core.pathfinder.kernel.noop` contains disabled fallback modules.
- `dev.traveler.core.pathfinder.module.api` and `impl` describe module selection and registry behavior.
- `dev.traveler.core.capability.traversal.api`, `spi`, `impl`, and `noop` define traversal module contracts, contributors, standard modules, and disabled traversal fallback.

## Composition Root

`PathfinderKernels.routeOnly()` is the current route-only composition root. It wires standard traversal modules into `RouteSearchComponents.withTraversalModules(...)`, then executes `RouteSearchService` behind the `PathfinderKernel` API.

Composition should stay at this boundary:

- callers choose a kernel mode and module set
- modules contribute providers through SPI contracts
- route search consumes composed providers
- search results report route diagnostics and active descriptors

Domain services should not instantiate platform adapters, and platform code should not duplicate route logic. During migration, core may keep small composition helpers, but their job is wiring, not owning traversal semantics.

## Fallbacks And Interchangeability

Modules are interchangeable only if absence is explicit.

- Enabled module descriptors are eligible for composition and appear in `PathfinderKernelResult.activeModules()`.
- Disabled module descriptors are fallbacks. They do not contribute providers and do not appear as active modules.
- A kernel with only `WalkTraversalModule` must still find simple flat walk routes.
- A kernel with no enabled traversal module must still build and return typed route diagnostics from search.
- Fallbacks should be conservative. A `noop` module should say "this capability is unavailable", not silently emulate a real feature.

This keeps tests honest: contract coverage should exercise route behavior or diagnostics, not only descriptor shape.

## Naming Rule

Use `Traveler` for the mod, user-facing commands, or integration surfaces where the product name is the correct noun. Do not use `Traveler` as a blanket prefix for internal architecture classes.

Preferred internal names:

- `PathfinderKernel`
- `RouteOnlyPathfinderKernel`
- `TraversalModule`
- `PathfinderModuleRegistry`
- `BlockBehaviorResolver`

Avoid new internal names like:

- `TravelerPathfinderKernel`
- `TravelerTraversalModule`
- `TravelerBlockBehaviorResolver`

The package and domain already provide context. Shorter domain names keep the architecture easier to read and easier to move.
