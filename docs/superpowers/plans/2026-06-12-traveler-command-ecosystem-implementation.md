# Traveler Command Ecosystem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a small modular command ecosystem with pure core command contracts and a BuildMyCommand adapter in Minecraft common.

**Architecture:** `core` defines neutral command features, routes, catalogs, context, feedback, and results. `mc/common` adapts a core catalog into BuildMyCommand and keeps Minecraft-specific path command features. Fabric remains registration-only.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit, BuildMyCommand, Fabric client adapter.

---

## File Structure

- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandFeature.java`: feature contract that contributes routes.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandCatalog.java`: immutable route collection and feature composition.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandRoute.java`: route path, description, and handler.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandHandler.java`: functional handler interface.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandContext.java`: argument and feedback access for handlers.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandFeedback.java`: reply abstraction.
- Create `modules/core/src/main/java/dev/traveler/core/command/TravelerCommandResult.java`: success/failure command result.
- Test `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandCatalogTest.java`.
- Test `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandContextTest.java`.
- Test `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandResultTest.java`.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/BuildMyCommandCatalogAdapter.java`: maps core catalog routes to BuildMyCommand routes.
- Create `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/PathTravelerCommandFeature.java`: existing path commands as a feature.
- Modify `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModule.java`: assemble framework from catalog.
- Modify existing common and Fabric command tests to use the module unchanged at the public surface.

## Task 1: Core Command Contracts

**Files:**
- Create: `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandCatalogTest.java`
- Create: `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandContextTest.java`
- Create: `modules/core/src/test/java/dev/traveler/core/command/TravelerCommandResultTest.java`
- Create production files under `modules/core/src/main/java/dev/traveler/core/command/`

- [ ] **Step 1: Write failing core tests**

Add tests that expect:

```java
TravelerCommandRoute route = new TravelerCommandRoute(
        "traveler test",
        "Runs a test command",
        context -> TravelerCommandResult.success("ok"));
TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(feature -> feature.add(route));
assertEquals(List.of(route), catalog.routes());
assertEquals(route, catalog.route("traveler test").orElseThrow());
```

Add context tests that expect `context.arg("x", int.class)` to return an integer and `context.feedback().reply("hello")` to call the supplied feedback.

Add result tests that expect success and failure messages to be present.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :core:test --tests dev.traveler.core.command.TravelerCommandCatalogTest --tests dev.traveler.core.command.TravelerCommandContextTest --tests dev.traveler.core.command.TravelerCommandResultTest
```

Expected: compilation fails because the command package does not exist.

- [ ] **Step 3: Implement minimal core command package**

Create the command contracts with guard clauses, immutable copies, and max nesting depth 2.

- [ ] **Step 4: Run tests and verify GREEN**

Run the same `:core:test` command. Expected: all three test classes pass.

- [ ] **Step 5: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/command modules/core/src/test/java/dev/traveler/core/command
git commit -m "feat(core): add command ecosystem contracts"
```

## Task 2: BuildMyCommand Adapter and Path Feature

**Files:**
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/BuildMyCommandCatalogAdapter.java`
- Create: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/PathTravelerCommandFeature.java`
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModule.java`
- Modify: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModuleTest.java`

- [ ] **Step 1: Write failing adapter tests**

Extend `TravelerCommandModuleTest` with assertions that:

```java
assertEquals(2, module.catalog().routes().size());
assertTrue(module.catalog().route("traveler path test").isPresent());
assertTrue(module.catalog().route("traveler path block <x:int> <y:int> <z:int>").isPresent());
```

Existing dispatch tests must remain unchanged and continue to verify replies and debug state.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest
```

Expected: compilation fails because `catalog()` and the new adapter/feature do not exist.

- [ ] **Step 3: Implement adapter and feature**

Use:

```java
framework.registry()
        .route(route.path())
        .description(route.description())
        .executes(context -> execute(route, context));
```

The adapter creates a `TravelerCommandContext` from `context.arguments()` and feedback that calls `context.source().reply(message)`. It converts `TravelerCommandResult.success` to `Results.success(message)` and failure to `Results.failure(message)`.

Move existing path command logic into `PathTravelerCommandFeature`. `TravelerCommandModule` should assemble `TravelerCommandCatalog.fromFeatures(new PathTravelerCommandFeature(...))`, create a framework, and call the adapter.

- [ ] **Step 4: Run common command tests and verify GREEN**

Run the same `:mc_1_21_11_common:test` command. Expected: command tests pass.

- [ ] **Step 5: Run Fabric command test**

Run:

```powershell
.\gradlew.bat :mc_1_21_11_fabric:test --tests dev.traveler.mc.v1_21_11.fabric.FabricCommandSourceAdapterTest
```

Expected: Fabric dispatch still replies with `path test status=FOUND nodes=2`.

- [ ] **Step 6: Commit**

```powershell
git add modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command modules/mc/1_21_11/fabric/src/test/java/dev/traveler/mc/v1_21_11/fabric/FabricCommandSourceAdapterTest.java
git commit -m "feat(mc-common): adapt modular command catalog"
```

## Task 3: Full Verification

**Files:**
- No source edits unless verification exposes a concrete issue.

- [ ] **Step 1: Run full checks**

Run:

```powershell
.\gradlew.bat clean check
```

Expected: build successful.

- [ ] **Step 2: Run Fabric build**

Run:

```powershell
.\gradlew.bat :mc_1_21_11_fabric:build
```

Expected: build successful.

- [ ] **Step 3: Inspect final state**

Run:

```powershell
git status --short --branch
```

Expected: clean working tree on the feature branch.
