# Traveler Command Ecosystem Design

## Goal

Build a small modular command ecosystem for Traveler so commands are no longer concentrated in one Minecraft class. The design keeps command intent and feature composition pure in `core`, while Minecraft and BuildMyCommand stay in the Minecraft adapter modules.

## Scope

This milestone adds the command framework shape only. It does not add permissions, suggestions, configuration persistence, command UI, or player movement commands.

## Architecture

`core` owns neutral command concepts:

- `TravelerCommandFeature`: a feature module that contributes command routes.
- `TravelerCommandCatalog`: an immutable collection of routes from one or more features.
- `TravelerCommandRoute`: a route path, description, and handler.
- `TravelerCommandContext`: parsed arguments plus feedback output.
- `TravelerCommandResult`: stable success or failure result.
- `TravelerCommandFeedback`: output abstraction for replies.

`mc/common` owns the BuildMyCommand adapter. It creates a `CommandFramework`, registers routes from the core catalog, maps BuildMyCommand arguments into `TravelerCommandContext`, and sends replies through the source.

`mc/common` may still own Minecraft-specific command features because those features can depend on Minecraft adapters, snapshots, and version-specific world objects. The reusable command ecosystem remains in `core`.

`fabric` remains bootstrap-only. It receives the assembled Minecraft command module and registers its framework into Fabric using BuildMyCommand's Fabric integration.

## Initial Command Features

The first feature is `PathTravelerCommandFeature` in `mc/common`. It contributes:

- `traveler path test`
- `traveler path block <x:int> <y:int> <z:int>`

The handlers keep the existing behavior:

- run the current small debug path search;
- update `PathfinderDebugState`;
- return a message that the adapter forwards to the command source;
- never move the player.

## Error Handling

Handlers return `TravelerCommandResult.failure(message)` for controlled failures. Unexpected exceptions should be allowed to fail tests during development and can later be wrapped by adapter-level error handling when the command surface grows.

## Testing

Core tests cover catalog composition, route lookup, immutable route lists, argument reading, feedback capture, and command result behavior.

Minecraft common tests cover that the BuildMyCommand adapter registers catalog routes, dispatches existing path commands, updates debug state, and forwards replies.

Architecture tests continue to ensure `core` has no Minecraft, loader, Brigadier, Mixin, or BuildMyCommand imports.
