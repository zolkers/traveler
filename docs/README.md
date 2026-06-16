# Traveler Docs

This directory was reset on 2026-06-16 to become the single source of truth for the Traveler rebuild.

## Start Here

1. Read [architecture/traveler-architecture-target.md](/C:/Users/vriegert/traveler/docs/architecture/traveler-architecture-target.md) to understand the target core, the dependency rules, and the reasons behind the refactor.
2. Read [superpowers/plans/2026-06-16-traveler-architecture-rebuild.md](/C:/Users/vriegert/traveler/docs/superpowers/plans/2026-06-16-traveler-architecture-rebuild.md) to execute the rebuild task-by-task.

## What Changed

- Old plans, research notes, and architecture fragments were removed.
- The new docs intentionally describe one coherent architecture instead of multiple partial directions.
- The new plan is written for agentic execution and assumes zero prior context.

## Ground Rules

- Core defines the laws; features attach to the laws.
- Behaviors describe world semantics; they do not drive inputs or recovery.
- Route planning, traversal planning, frame planning, progress monitoring, recovery, and debug rendering must share the same contracts.
- Minecraft/Fabric stays an adapter layer only.
