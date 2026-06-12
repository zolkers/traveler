# Traveler Foundation + Mini Pathfinder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Fabric-first, client-only Traveler MVP from the validated spec: Java multi-module Gradle foundation, pure `core` pathfinding framework, Minecraft `1.21.11` common/Fabric modules, BuildMyCommand client commands, and quality gates.

**Architecture:** `core` is a pure Java framework with no Minecraft or loader imports. Minecraft code is isolated under versioned `modules/mc/1_21_11`, with `common` holding shared Mojmap Minecraft adapters and `fabric` holding only Fabric bootstrap.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Fabric Loom, Mojang official mappings, Fabric Loader/API, BuildMyCommand, JUnit, JaCoCo, Spotless, Checkstyle.

---

## Key Decisions

- Java 21 toolchain, Gradle Kotlin DSL, Gradle Wrapper `9.5.1`.
- Versions: Minecraft `1.21.11`, Fabric Loader `0.19.3`, Fabric API `0.141.4+1.21.11`, Fabric Loom `1.17.11`, BuildMyCommand `0.3.6`, JUnit `6.1.0`, Spotless `8.6.0`, JaCoCo `0.8.15`, Checkstyle `13.5.0`.
- Gradle project names:
  - `:core` -> `modules/core`
  - `:mc_1_21_11_common` -> `modules/mc/1_21_11/common`
  - `:mc_1_21_11_fabric` -> `modules/mc/1_21_11/fabric`
- `core` has no Minecraft, loader, Brigadier, Mixin, or BuildMyCommand imports.
- Production code nesting depth is max 2. Use guard clauses, early returns, small methods, or state machines.

## Task 1: Build Foundation

- [ ] Add `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle Wrapper, and `.gitignore`.
- [ ] Add convention plugins in `build-logic`.
- [ ] Configure Maven Central, Fabric Maven, Java 21, JUnit Platform, JaCoCo reports, Spotless Java formatting, Checkstyle, and client-only Fabric run defaults.
- [ ] Create empty module build files for `:core`, `:mc_1_21_11_common`, and `:mc_1_21_11_fabric`.
- [ ] Run `./gradlew projects` and commit.

## Task 2: Core Pathfinder API

- [ ] Write failing tests for path containers, sync A*, blocked paths, weighted costs, budgeted search, and negative costs.
- [ ] Create pure Java packages under `dev.traveler.core`.
- [ ] Add `Graph`, `Connection`, `Heuristic`, `GraphPath`, `MutableGraphPath`, `Pathfinder`, `PathfinderRequest`, `PathfinderResult`, and `PathfinderStatus`.
- [ ] Implement `AStarPathfinder` with synchronous and budgeted search.
- [ ] Run `./gradlew :core:test` and commit.

## Task 3: Core Movement + Smoothing

- [ ] Write failing tests for movement value objects and path smoothing.
- [ ] Add `BlockPosition`, `EntityDimensions`, `MovementCapabilities`, `MovementProfile`, `TraversalCost`, and `TraversalRules`.
- [ ] Add `PathSmoother` using an injected line-of-walk predicate.
- [ ] Add test-only fake grid world helpers.
- [ ] Run `./gradlew :core:test` and commit.

## Task 4: Minecraft Common Module

- [ ] Configure Loom with Mojang official mappings.
- [ ] Add `PathfinderDebugState`.
- [ ] Add `MinecraftWorldSnapshot`, `MinecraftBlockClassifier`, and `MinecraftMovementProfileAdapter`.
- [ ] Add `TravelerCommandModule` command handlers outside Fabric bootstrap.
- [ ] Run `./gradlew :mc_1_21_11_common:build` and commit.

## Task 5: Fabric Client Module

- [ ] Add `fabric.mod.json` with a client entrypoint only.
- [ ] Add `TravelerFabricClientMod`, `FabricCommandBootstrap`, and `FabricEventBootstrap`.
- [ ] Register BuildMyCommand Fabric client commands `/traveler path test` and `/traveler path block <x> <y> <z>`.
- [ ] Ensure commands update `PathfinderDebugState` and never move the player.
- [ ] Run `./gradlew :mc_1_21_11_fabric:build` and commit.

## Task 6: Architecture Guards And Final Verification

- [ ] Add tests or Checkstyle rules proving no forbidden imports in `core`.
- [ ] Add tests or source scans proving `common` has no Fabric/NeoForge imports.
- [ ] Enforce production nesting depth of 2.
- [ ] Run `./gradlew clean check`.
- [ ] Run `./gradlew :mc_1_21_11_fabric:build`.
- [ ] Commit final verification fixes.

## Test Plan

- `./gradlew clean check`
- `./gradlew :core:test`
- `./gradlew :mc_1_21_11_common:build`
- `./gradlew :mc_1_21_11_fabric:build`
- Manual later: `./gradlew :mc_1_21_11_fabric:runClient`, then test `/traveler path test` in a client world.
