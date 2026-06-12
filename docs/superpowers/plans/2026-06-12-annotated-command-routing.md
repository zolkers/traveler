# Annotated Command Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add loader-neutral annotation-based command routing for Traveler commands.

**Architecture:** Add `@TravelerCommand`, `@TravelerSubcommand`, and a small reflection scanner in core. Existing MC common command code becomes an annotated command feature, while BuildMyCommand remains only an adapter from `TravelerCommandCatalog`.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit, existing Traveler command catalog.

---

### Task 1: Core Annotation Scanner

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/command/TravelerCommand.java`
- Create: `modules/core/src/main/java/dev/traveler/core/command/TravelerSubcommand.java`
- Create: `modules/core/src/main/java/dev/traveler/core/command/AnnotatedTravelerCommandFeature.java`
- Modify: `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandCatalogTest.java`

- [ ] Write failing tests proving annotated command objects produce full routes and reject invalid signatures.
- [ ] Run `./gradlew :core:test --tests dev.traveler.core.command.TravelerCommandCatalogTest` and verify the tests fail because the annotation API is missing.
- [ ] Implement the annotations and scanner with small methods and guard clauses.
- [ ] Re-run the same core test and verify it passes.

### Task 2: Path Command Migration

**Files:**
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/PathTravelerCommandFeature.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModule.java`
- Modify: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModuleTest.java`

- [ ] Write a failing test proving the path command class exposes exactly `traveler path test` and `traveler path block <x:int> <y:int> <z:int>` through annotations.
- [ ] Run `./gradlew :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest` and verify the expected failure.
- [ ] Replace manual route registration with `AnnotatedTravelerCommandFeature.from(pathFeature)`.
- [ ] Re-run the common command test and verify it passes.

### Task 3: Final Verification And Commit

**Files:**
- All files touched by Tasks 1 and 2.

- [ ] Run `./gradlew check :mc_1_21_11_fabric:build`.
- [ ] Run `git diff --check`.
- [ ] Run `rg "dev.riege.buildmycommand|net.minecraft" modules/core/src/main/java/dev/traveler/core/command -g "*.java"` and verify there are no core command framework leaks.
- [ ] Commit with `feat(core): add annotated command routing`.
