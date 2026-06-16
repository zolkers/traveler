# Pathfinder Kernel Modularization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the core pathfinding/navigation architecture into a neutral `PathfinderKernel` composed from interchangeable feature modules, with explicit interfaces for every feature type and fallbacks that keep route planning working when optional modules are removed.

**Architecture:** Keep the existing domain dependency direction (`world -> route -> navigation -> platform`) while adding a second organization axis: feature modules. Each feature type exposes contracts in its own `api`/`spi` packages, standard implementations in `impl`, private helpers in `internal`, fallbacks in `noop`, and reusable contract fixtures in `testing`. The kernel is the only composition root; production feature code must depend on interfaces, not concrete implementations.

**Tech Stack:** Java 21, Gradle, JUnit 5, existing `modules/core` architecture guard tests, existing Traveler path/route/navigation code. New internal architecture classes must not be named `Traveler*`; that prefix is reserved for mod branding, command adapters, and already-existing integration names.

---

## Scope And Ordering

This plan is intentionally a staged architecture migration. Do not move every existing traversal into the new feature layout in one commit. First add kernel contracts and guardrails, then adapters around existing code, then migrate one feature at a time while keeping `.\gradlew.bat --no-daemon check` green.

The first implementation target is **route-only interchangeability**:

- removing a traversal module removes its graph/route/navigation contributions without breaking the kernel;
- removing smoothing produces a raw but valid route through `IdentitySmoothingModule`;
- removing execution/controller keeps planning and debug snapshots available;
- removing a behavior module falls back to conservative unknown behavior.

## Global Java Rules

- Use dependency inversion: orchestration depends on interfaces from `api` or `spi`, never concrete `impl` classes.
- Use interface segregation: each feature type has a narrow interface; do not create a single giant module interface with unrelated methods.
- Use composition over inheritance: feature modules contribute providers, strategies, policies, and layers.
- Use immutable value objects: prefer records for module descriptors, stage inputs, stage outputs, and snapshots.
- Use fail-fast constructor validation with `Objects.requireNonNull` and finite-number checks.
- Keep package-private helpers package-private.
- Keep `internal` imports private to their owning feature or kernel package.
- Provide no-op/identity/fallback implementations for every optional feature type.
- Keep classes small and named by responsibility.
- Avoid static composition outside the kernel; `standard()` factories may create local defaults, but central assembly belongs to `PathfinderKernel`.
- Do not introduce new classes named `Traveler*`.

## Package Shape Rule

Every new feature area follows this shape:

```text
dev.traveler.core.<area>.<feature>.api
dev.traveler.core.<area>.<feature>.spi
dev.traveler.core.<area>.<feature>.impl
dev.traveler.core.<area>.<feature>.internal
dev.traveler.core.<area>.<feature>.noop
dev.traveler.core.<area>.<feature>.testing
```

Meaning:

- `api`: public contracts and stable value objects.
- `spi`: extension contracts consumed by the kernel or module registries.
- `impl`: standard implementation classes.
- `internal`: private orchestration details for the owning package only.
- `noop`: no-op, identity, conservative fallback implementations.
- `testing`: contract-test helpers and fixtures.

## Target File Map

### Kernel And Module Contracts

- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernel.java`
  - Public entry point for route planning and, later, executable navigation.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernelResult.java`
  - Immutable result containing route plan, diagnostics, and active module metadata.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernelMode.java`
  - Enum: `ROUTE_ONLY`, `EXECUTABLE_NAVIGATION`.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModule.java`
  - Small base interface for all module contributions.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModuleType.java`
  - Enum for module categories.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModuleDescriptor.java`
  - Record with module id, type, priority, enabled flag.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/internal/DefaultPathfinderKernel.java`
  - Private standard kernel implementation.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/impl/PathfinderKernels.java`
  - Public factories for standard and custom kernels.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/noop/EmptyPathfinderModule.java`
  - Fallback module for disabled extension points.

### Module Registry

- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/module/api/PathfinderModuleRegistry.java`
  - Immutable registry queried by type.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/module/api/PathfinderModuleSelection.java`
  - Record with resolved active modules and disabled modules.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/module/impl/DefaultPathfinderModuleRegistry.java`
  - Immutable ordered registry implementation.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/module/noop/EmptyPathfinderModuleRegistry.java`
  - Registry that returns only safe fallbacks.

### Pipeline Stage Contracts

- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/stage/api/PathfinderStage.java`
  - Generic stage interface.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/stage/api/StageContext.java`
  - Shared immutable context record.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/stage/api/StageFailure.java`
  - Typed failure record.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/stage/api/StageResult.java`
  - Generic success/failure wrapper.
- Create `modules/core/src/main/java/dev/traveler/core/pathfinder/stage/noop/NoOpStage.java`
  - Stage that passes through unchanged input when a stage is optional.

### Feature Type Contracts

- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/api/TraversalModule.java`
  - Interface every movement feature must implement.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalConnectionContributor.java`
  - Supplies graph connections.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalRouteContributor.java`
  - Supplies route step conversion.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalExecutionContributor.java`
  - Supplies executable navigation contribution.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalRecoveryContributor.java`
  - Supplies recovery policy contribution.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalDebugContributor.java`
  - Supplies debug layer contribution.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/noop/NoTraversalModule.java`
  - Disabled traversal fallback.

- Create `modules/core/src/main/java/dev/traveler/core/capability/smoothing/api/SmoothingModule.java`
  - Interface for optional smoothing feature type.
- Create `modules/core/src/main/java/dev/traveler/core/capability/smoothing/spi/SmoothingStrategy.java`
  - Strategy contract for path/route smoothing.
- Create `modules/core/src/main/java/dev/traveler/core/capability/smoothing/noop/IdentitySmoothingModule.java`
  - Raw path passthrough fallback.

- Create `modules/core/src/main/java/dev/traveler/core/capability/behavior/api/BlockBehaviorModule.java`
  - Interface for behavior packs.
- Create `modules/core/src/main/java/dev/traveler/core/capability/behavior/spi/BlockBehaviorResolver.java`
  - Resolver contract for behavior packs.
- Create `modules/core/src/main/java/dev/traveler/core/capability/behavior/noop/UnknownBlockBehaviorModule.java`
  - Conservative fallback behavior pack.

- Create `modules/core/src/main/java/dev/traveler/core/capability/execution/api/ExecutionModule.java`
  - Interface for navigation execution/controller contribution.
- Create `modules/core/src/main/java/dev/traveler/core/capability/execution/spi/ControlProjectionStrategy.java`
  - Interface around movement input projection.
- Create `modules/core/src/main/java/dev/traveler/core/capability/execution/noop/NoOpExecutionModule.java`
  - Keeps route-only planning alive without controller inputs.

### Adapters Around Existing Code

- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/WalkTraversalModule.java`
  - Wraps existing walk providers.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/JumpTraversalModule.java`
  - Wraps existing jump connection/route/navigation/recovery pieces.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/ClimbTraversalModule.java`
  - Wraps existing climb connection/route/navigation/recovery pieces.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/DropTraversalModule.java`
  - Wraps existing drop providers.
- Create `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/SwimTraversalModule.java`
  - Starts as route/navigation/recovery only if no graph contribution exists yet.
- Create `modules/core/src/main/java/dev/traveler/core/capability/smoothing/impl/FarthestReachableSmoothingModule.java`
  - Wraps existing `PathSmoothingSelector.farthestReachable()`.
- Create `modules/core/src/main/java/dev/traveler/core/capability/execution/impl/StandardExecutionModule.java`
  - Wraps existing `NavigationFramePlanner` and `ControlProjector`.

### Existing Files To Modify

- Modify `modules/core/src/main/java/dev/traveler/core/route/RouteSearchComponents.java`
  - Add module-based construction entry points.
- Modify `modules/core/src/main/java/dev/traveler/core/route/internal/SurfaceTraversalFeatures.java`
  - Delegate to traversal modules instead of owning hard-coded standard features.
- Modify `modules/core/src/main/java/dev/traveler/core/route/internal/DefaultRoutePlanner.java`
  - Accept module-driven components.
- Modify `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationFramePlanner.java`
  - Move action-specific execution contributions behind module interfaces.
- Modify `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementHealthPolicyRegistry.java`
  - Build from traversal recovery contributors.
- Modify `modules/core/src/main/java/dev/traveler/core/layer/SurfaceBlockFactory.java`
  - Accept behavior modules via resolver chains.
- Modify `modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java`
  - Enforce `api`/`spi`/`impl`/`noop`/`internal` import rules and no new `Traveler*` internal classes.

---

### Task 1: Add Architecture Tests For Modular Package Rules

**Files:**
- Modify: `modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java`

- [ ] **Step 1: Add failing tests for package visibility and naming**

Add these test methods to `DependencyGuardTest`:

```java
@Test
void internalArchitectureClassesMustNotUseTravelerPrefix() throws IOException {
    List<String> violations = JavaSourceRules.productionSources(SOURCE_ROOT).stream()
            .filter(path -> !path.toString().contains("\\command\\"))
            .filter(path -> !path.toString().contains("/command/"))
            .filter(path -> !path.toString().contains("\\mc\\"))
            .filter(path -> !path.toString().contains("/mc/"))
            .filter(path -> path.getFileName().toString().startsWith("Traveler"))
            .map(SOURCE_ROOT::relativize)
            .map(Path::toString)
            .toList();

    assertTrue(violations.isEmpty(), () -> "Internal architecture classes must not use Traveler prefix: "
            + violations);
}

@Test
void featureImplementationsMustNotImportOtherFeatureInternals() throws IOException {
    List<String> violations = JavaSourceRules.importLines(SOURCE_ROOT).stream()
            .filter(line -> line.contains(".capability."))
            .filter(line -> line.contains(".internal."))
            .filter(line -> !sameFeatureInternalImport(line))
            .toList();

    assertTrue(violations.isEmpty(), () -> "Feature implementation imports another feature internal package: "
            + violations);
}

@Test
void concreteFeatureImplementationsMustStayOutOfApiPackages() throws IOException {
    List<String> violations = JavaSourceRules.productionSources(SOURCE_ROOT).stream()
            .filter(path -> path.toString().contains("\\api\\") || path.toString().contains("/api/"))
            .filter(path -> fileContains(path, "class Default")
                    || fileContains(path, "class Standard")
                    || fileContains(path, "class NoOp")
                    || fileContains(path, "class Identity"))
            .map(SOURCE_ROOT::relativize)
            .map(Path::toString)
            .toList();

    assertTrue(violations.isEmpty(), () -> "Concrete implementations belong in impl/noop, not api: "
            + violations);
}
```

Add these private helpers to the same test class:

```java
private static boolean fileContains(Path path, String pattern) {
    try {
        return Files.readString(path).contains(pattern);
    } catch (IOException exception) {
        throw new UncheckedIOException(exception);
    }
}

private static boolean sameFeatureInternalImport(String importLine) {
    String normalized = importLine.replace('\\', '.').replace('/', '.');
    int capability = normalized.indexOf(".capability.");
    int internal = normalized.indexOf(".internal.");
    if (capability < 0 || internal < 0) {
        return false;
    }
    String suffix = normalized.substring(capability + ".capability.".length(), internal);
    String[] parts = suffix.split("\\.");
    return parts.length >= 1 && normalized.contains(".capability." + parts[0] + ".");
}
```

Add imports if missing:

```java
import java.io.UncheckedIOException;
```

- [ ] **Step 2: Run the architecture tests to verify failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.architecture.DependencyGuardTest"
```

Expected: FAIL because the helper methods `JavaSourceRules.productionSources(...)` and `JavaSourceRules.importLines(...)` do not exist yet, or because existing internal naming/package rules are not enforced.

- [ ] **Step 3: Add missing `JavaSourceRules` helpers**

Modify `modules/core/src/test/java/dev/traveler/core/architecture/JavaSourceRules.java` to expose production source iteration and import lines:

```java
static List<Path> productionSources(Path sourceRoot) throws IOException {
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
        return paths.filter(path -> path.toString().endsWith(".java"))
                .toList();
    }
}

static List<String> importLines(Path sourceRoot) throws IOException {
    List<String> lines = new ArrayList<>();
    for (Path source : productionSources(sourceRoot)) {
        for (String line : Files.readAllLines(source)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                lines.add(sourceRoot.relativize(source) + ": " + trimmed);
            }
        }
    }
    return List.copyOf(lines);
}
```

Ensure these imports exist in `JavaSourceRules.java`:

```java
import java.util.ArrayList;
import java.util.stream.Stream;
```

- [ ] **Step 4: Run the architecture tests again**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.architecture.DependencyGuardTest"
```

Expected: PASS or actionable failures that name concrete current files. If current files fail the new `Traveler*` rule, narrow the rule to new architecture packages only:

```java
.filter(path -> path.toString().contains("\\pathfinder\\") || path.toString().contains("/pathfinder/")
        || path.toString().contains("\\capability\\") || path.toString().contains("/capability/"))
```

- [ ] **Step 5: Commit**

```powershell
git add modules/core/src/test/java/dev/traveler/core/architecture/DependencyGuardTest.java modules/core/src/test/java/dev/traveler/core/architecture/JavaSourceRules.java
git commit -m "test: guard modular pathfinder package rules"
```

---

### Task 2: Introduce Kernel And Module Base Contracts

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernelMode.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api/PathfinderKernelResult.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModuleDescriptor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi/PathfinderModuleType.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/noop/EmptyPathfinderModule.java`
- Test: `modules/core/src/test/java/dev/traveler/core/pathfinder/kernel/PathfinderModuleContractTest.java`

- [ ] **Step 1: Write failing contract tests**

Create `PathfinderModuleContractTest.java`:

```java
package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import org.junit.jupiter.api.Test;

class PathfinderModuleContractTest {
    @Test
    void descriptorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
                new PathfinderModuleDescriptor(" ", PathfinderModuleType.TRAVERSAL, 0, true));
    }

    @Test
    void emptyModuleIsDisabledAndStable() {
        EmptyPathfinderModule module = new EmptyPathfinderModule(PathfinderModuleType.EXECUTION, "execution.none");

        assertEquals("execution.none", module.descriptor().id());
        assertEquals(PathfinderModuleType.EXECUTION, module.descriptor().type());
        assertFalse(module.descriptor().enabled());
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderModuleContractTest"
```

Expected: FAIL because the new kernel module contracts do not exist.

- [ ] **Step 3: Add module type enum**

Create `PathfinderModuleType.java`:

```java
package dev.traveler.core.pathfinder.kernel.spi;

public enum PathfinderModuleType {
    TRAVERSAL,
    SMOOTHING,
    BEHAVIOR,
    EXECUTION,
    RECOVERY,
    DEBUG
}
```

- [ ] **Step 4: Add module descriptor record**

Create `PathfinderModuleDescriptor.java`:

```java
package dev.traveler.core.pathfinder.kernel.spi;

import java.util.Objects;

public record PathfinderModuleDescriptor(
        String id,
        PathfinderModuleType type,
        int priority,
        boolean enabled) {
    public PathfinderModuleDescriptor {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(type, "type");
    }
}
```

- [ ] **Step 5: Add module base interface**

Create `PathfinderModule.java`:

```java
package dev.traveler.core.pathfinder.kernel.spi;

public interface PathfinderModule {
    PathfinderModuleDescriptor descriptor();
}
```

- [ ] **Step 6: Add empty module fallback**

Create `EmptyPathfinderModule.java`:

```java
package dev.traveler.core.pathfinder.kernel.noop;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.Objects;

public final class EmptyPathfinderModule implements PathfinderModule {
    private final PathfinderModuleDescriptor descriptor;

    public EmptyPathfinderModule(PathfinderModuleType type, String id) {
        descriptor = new PathfinderModuleDescriptor(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(type, "type"),
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }
}
```

- [ ] **Step 7: Add kernel mode and public kernel result**

Create `PathfinderKernelMode.java`:

```java
package dev.traveler.core.pathfinder.kernel.api;

public enum PathfinderKernelMode {
    ROUTE_ONLY,
    EXECUTABLE_NAVIGATION
}
```

Create `PathfinderKernelResult.java`:

```java
package dev.traveler.core.pathfinder.kernel.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.route.RouteSearchDiagnostics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PathfinderKernelResult(
        Optional<RoutePlan> routePlan,
        RouteSearchDiagnostics diagnostics,
        List<PathfinderModuleDescriptor> activeModules) {
    public PathfinderKernelResult {
        routePlan = Objects.requireNonNull(routePlan, "routePlan");
        Objects.requireNonNull(diagnostics, "diagnostics");
        activeModules = List.copyOf(Objects.requireNonNull(activeModules, "activeModules"));
    }
}
```

Create `PathfinderKernel.java`:

```java
package dev.traveler.core.pathfinder.kernel.api;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementProfile;

public interface PathfinderKernel {
    PathfinderKernelResult findRoute(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            MovementProfile movementProfile);

    PathfinderKernelMode mode();
}
```

- [ ] **Step 8: Run contract tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderModuleContractTest"
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/pathfinder modules/core/src/test/java/dev/traveler/core/pathfinder
git commit -m "feat: add pathfinder kernel module contracts"
```

---

### Task 3: Add Immutable Module Registry With Fallback Selection

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/module/api/PathfinderModuleRegistry.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/module/api/PathfinderModuleSelection.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/module/impl/DefaultPathfinderModuleRegistry.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/module/noop/EmptyPathfinderModuleRegistry.java`
- Test: `modules/core/src/test/java/dev/traveler/core/pathfinder/module/PathfinderModuleRegistryTest.java`

- [ ] **Step 1: Write failing registry tests**

Create `PathfinderModuleRegistryTest.java`:

```java
package dev.traveler.core.pathfinder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.impl.DefaultPathfinderModuleRegistry;
import dev.traveler.core.pathfinder.module.noop.EmptyPathfinderModuleRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderModuleRegistryTest {
    @Test
    void registryReturnsEnabledModulesByPriority() {
        TestModule low = new TestModule("low", 10, true);
        TestModule high = new TestModule("high", 1, true);

        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(List.of(low, high));

        assertEquals(List.of(high, low), registry.enabled(PathfinderModuleType.TRAVERSAL));
    }

    @Test
    void emptyRegistryReturnsDisabledFallback() {
        EmptyPathfinderModuleRegistry registry = new EmptyPathfinderModuleRegistry();

        assertTrue(registry.enabled(PathfinderModuleType.TRAVERSAL).isEmpty());
        assertEquals(PathfinderModuleType.TRAVERSAL, registry.fallback(PathfinderModuleType.TRAVERSAL).descriptor().type());
    }

    private record TestModule(String id, int priority, boolean enabled)
            implements dev.traveler.core.pathfinder.kernel.spi.PathfinderModule {
        @Override
        public PathfinderModuleDescriptor descriptor() {
            return new PathfinderModuleDescriptor(id, PathfinderModuleType.TRAVERSAL, priority, enabled);
        }
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.module.PathfinderModuleRegistryTest"
```

Expected: FAIL because registry classes do not exist.

- [ ] **Step 3: Add registry API**

Create `PathfinderModuleRegistry.java`:

```java
package dev.traveler.core.pathfinder.module.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

public interface PathfinderModuleRegistry {
    List<PathfinderModule> enabled(PathfinderModuleType type);

    PathfinderModule fallback(PathfinderModuleType type);
}
```

Create `PathfinderModuleSelection.java`:

```java
package dev.traveler.core.pathfinder.module.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;
import java.util.Objects;

public record PathfinderModuleSelection(
        List<PathfinderModule> enabled,
        List<PathfinderModule> disabled) {
    public PathfinderModuleSelection {
        enabled = List.copyOf(Objects.requireNonNull(enabled, "enabled"));
        disabled = List.copyOf(Objects.requireNonNull(disabled, "disabled"));
    }
}
```

- [ ] **Step 4: Add default registry implementation**

Create `DefaultPathfinderModuleRegistry.java`:

```java
package dev.traveler.core.pathfinder.module.impl;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class DefaultPathfinderModuleRegistry implements PathfinderModuleRegistry {
    private final List<PathfinderModule> modules;

    public DefaultPathfinderModuleRegistry(List<? extends PathfinderModule> modules) {
        this.modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    }

    @Override
    public List<PathfinderModule> enabled(PathfinderModuleType type) {
        PathfinderModuleType requested = Objects.requireNonNull(type, "type");
        return modules.stream()
                .filter(module -> module.descriptor().enabled())
                .filter(module -> module.descriptor().type() == requested)
                .sorted(Comparator.comparingInt(module -> module.descriptor().priority()))
                .toList();
    }

    @Override
    public PathfinderModule fallback(PathfinderModuleType type) {
        return new EmptyPathfinderModule(type, fallbackId(type));
    }

    private static String fallbackId(PathfinderModuleType type) {
        return "fallback." + type.name().toLowerCase(java.util.Locale.ROOT);
    }
}
```

Create `EmptyPathfinderModuleRegistry.java`:

```java
package dev.traveler.core.pathfinder.module.noop;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleRegistry;
import java.util.List;

public final class EmptyPathfinderModuleRegistry implements PathfinderModuleRegistry {
    @Override
    public List<PathfinderModule> enabled(PathfinderModuleType type) {
        return List.of();
    }

    @Override
    public PathfinderModule fallback(PathfinderModuleType type) {
        return new EmptyPathfinderModule(type, "fallback." + type.name().toLowerCase(java.util.Locale.ROOT));
    }
}
```

Remove unused imports flagged by Checkstyle after compiling.

- [ ] **Step 5: Run tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.module.PathfinderModuleRegistryTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/pathfinder/module modules/core/src/test/java/dev/traveler/core/pathfinder/module
git commit -m "feat: add immutable pathfinder module registry"
```

---

### Task 4: Add Traversal Feature Type Interfaces

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/api/TraversalModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalConnectionContributor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalRouteContributor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalExecutionContributor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalRecoveryContributor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/spi/TraversalDebugContributor.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/noop/NoTraversalModule.java`
- Test: `modules/core/src/test/java/dev/traveler/core/capability/traversal/TraversalModuleContractTest.java`

- [ ] **Step 1: Write failing traversal module contract test**

Create `TraversalModuleContractTest.java`:

```java
package dev.traveler.core.capability.traversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import org.junit.jupiter.api.Test;

class TraversalModuleContractTest {
    @Test
    void noTraversalModuleContributesNothing() {
        NoTraversalModule module = new NoTraversalModule("traversal.none");

        assertEquals(PathfinderModuleType.TRAVERSAL, module.descriptor().type());
        assertTrue(module.connectionContributors().isEmpty());
        assertTrue(module.routeContributors().isEmpty());
        assertTrue(module.executionContributors().isEmpty());
        assertTrue(module.recoveryContributors().isEmpty());
        assertTrue(module.debugContributors().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.traversal.TraversalModuleContractTest"
```

Expected: FAIL because traversal capability interfaces do not exist.

- [ ] **Step 3: Add traversal SPI contributor interfaces**

Create `TraversalConnectionContributor.java`:

```java
package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import java.util.List;

public interface TraversalConnectionContributor {
    List<SurfaceConnectionProvider> surfaceConnectionProviders();
}
```

Create `TraversalRouteContributor.java`:

```java
package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import java.util.List;

public interface TraversalRouteContributor {
    List<SurfaceRouteStartProvider> routeStartProviders();

    List<SurfaceTransitionProvider> transitionProviders();

    List<SurfaceRouteStepProvider> routeStepProviders();
}
```

Create `TraversalExecutionContributor.java`:

```java
package dev.traveler.core.capability.traversal.spi;

public interface TraversalExecutionContributor {
    boolean supportsExecution();
}
```

Create `TraversalRecoveryContributor.java`:

```java
package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.navigation.recovery.MovementHealthPolicy;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Map;

public interface TraversalRecoveryContributor {
    Map<MovementAction, MovementHealthPolicy> movementHealthPolicies();
}
```

Create `TraversalDebugContributor.java`:

```java
package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.navigation.debug.DebugLayer;
import java.util.List;

public interface TraversalDebugContributor {
    List<DebugLayer> debugLayers();
}
```

- [ ] **Step 4: Add traversal module interface**

Create `TraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.api;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;

public interface TraversalModule extends PathfinderModule {
    List<TraversalConnectionContributor> connectionContributors();

    List<TraversalRouteContributor> routeContributors();

    List<TraversalExecutionContributor> executionContributors();

    List<TraversalRecoveryContributor> recoveryContributors();

    List<TraversalDebugContributor> debugContributors();
}
```

- [ ] **Step 5: Add disabled traversal fallback**

Create `NoTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.noop;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;
import java.util.Objects;

public final class NoTraversalModule implements TraversalModule {
    private final PathfinderModuleDescriptor descriptor;

    public NoTraversalModule(String id) {
        descriptor = new PathfinderModuleDescriptor(
                Objects.requireNonNull(id, "id"),
                PathfinderModuleType.TRAVERSAL,
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRouteContributor> routeContributors() {
        return List.of();
    }

    @Override
    public List<TraversalExecutionContributor> executionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRecoveryContributor> recoveryContributors() {
        return List.of();
    }

    @Override
    public List<TraversalDebugContributor> debugContributors() {
        return List.of();
    }
}
```

- [ ] **Step 6: Run traversal contract tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.traversal.TraversalModuleContractTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/capability/traversal modules/core/src/test/java/dev/traveler/core/capability/traversal
git commit -m "feat: add traversal module contracts"
```

---

### Task 5: Add Smoothing, Behavior, And Execution Feature Interfaces

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/capability/smoothing/api/SmoothingModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/smoothing/spi/SmoothingStrategy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/smoothing/noop/IdentitySmoothingModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/behavior/api/BlockBehaviorModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/behavior/spi/BlockBehaviorResolver.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/behavior/noop/UnknownBlockBehaviorModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/execution/api/ExecutionModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/execution/spi/ControlProjectionStrategy.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/execution/noop/NoOpExecutionModule.java`
- Test: `modules/core/src/test/java/dev/traveler/core/capability/FeatureFallbackContractTest.java`

- [ ] **Step 1: Write failing fallback tests**

Create `FeatureFallbackContractTest.java`:

```java
package dev.traveler.core.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.traveler.core.capability.behavior.noop.UnknownBlockBehaviorModule;
import dev.traveler.core.capability.execution.noop.NoOpExecutionModule;
import dev.traveler.core.capability.smoothing.noop.IdentitySmoothingModule;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import org.junit.jupiter.api.Test;

class FeatureFallbackContractTest {
    @Test
    void identitySmoothingReturnsOriginalPath() {
        IdentitySmoothingModule module = new IdentitySmoothingModule();
        MutableGraphPath<String> path = new MutableGraphPath<>();
        path.add("a");
        path.add("b");

        assertSame(path, module.strategy().smooth(path));
        assertEquals(PathfinderModuleType.SMOOTHING, module.descriptor().type());
    }

    @Test
    void unknownBehaviorModuleIsConservative() {
        UnknownBlockBehaviorModule module = new UnknownBlockBehaviorModule();

        assertEquals(PathfinderModuleType.BEHAVIOR, module.descriptor().type());
        assertFalse(module.descriptor().enabled());
    }

    @Test
    void noOpExecutionDisablesInputProjection() {
        NoOpExecutionModule module = new NoOpExecutionModule();

        assertEquals(PathfinderModuleType.EXECUTION, module.descriptor().type());
        assertFalse(module.supportsExecution());
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.FeatureFallbackContractTest"
```

Expected: FAIL because feature interfaces and fallbacks do not exist.

- [ ] **Step 3: Add smoothing contracts and fallback**

Create `SmoothingStrategy.java`:

```java
package dev.traveler.core.capability.smoothing.spi;

import dev.traveler.core.graph.GraphPath;

public interface SmoothingStrategy<N> {
    GraphPath<N> smooth(GraphPath<N> path);
}
```

Create `SmoothingModule.java`:

```java
package dev.traveler.core.capability.smoothing.api;

import dev.traveler.core.capability.smoothing.spi.SmoothingStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;

public interface SmoothingModule<N> extends PathfinderModule {
    SmoothingStrategy<N> strategy();
}
```

Create `IdentitySmoothingModule.java`:

```java
package dev.traveler.core.capability.smoothing.noop;

import dev.traveler.core.capability.smoothing.api.SmoothingModule;
import dev.traveler.core.capability.smoothing.spi.SmoothingStrategy;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;

public final class IdentitySmoothingModule<N> implements SmoothingModule<N> {
    private final SmoothingStrategy<N> strategy = new IdentitySmoothingStrategy<>();

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return new PathfinderModuleDescriptor(
                "smoothing.identity",
                PathfinderModuleType.SMOOTHING,
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public SmoothingStrategy<N> strategy() {
        return strategy;
    }

    private static final class IdentitySmoothingStrategy<N> implements SmoothingStrategy<N> {
        @Override
        public GraphPath<N> smooth(GraphPath<N> path) {
            return java.util.Objects.requireNonNull(path, "path");
        }
    }
}
```

- [ ] **Step 4: Add behavior contracts and fallback**

Create `BlockBehaviorResolver.java`:

```java
package dev.traveler.core.capability.behavior.spi;

import dev.traveler.core.layer.SurfaceBlockSample;
import dev.traveler.core.world.behavior.BlockBehavior;
import java.util.Optional;

public interface BlockBehaviorResolver {
    Optional<BlockBehavior> resolve(SurfaceBlockSample sample);
}
```

Create `BlockBehaviorModule.java`:

```java
package dev.traveler.core.capability.behavior.api;

import dev.traveler.core.capability.behavior.spi.BlockBehaviorResolver;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;

public interface BlockBehaviorModule extends PathfinderModule {
    List<BlockBehaviorResolver> resolvers();
}
```

Create `UnknownBlockBehaviorModule.java`:

```java
package dev.traveler.core.capability.behavior.noop;

import dev.traveler.core.capability.behavior.api.BlockBehaviorModule;
import dev.traveler.core.capability.behavior.spi.BlockBehaviorResolver;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

public final class UnknownBlockBehaviorModule implements BlockBehaviorModule {
    @Override
    public PathfinderModuleDescriptor descriptor() {
        return new PathfinderModuleDescriptor(
                "behavior.unknown",
                PathfinderModuleType.BEHAVIOR,
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public List<BlockBehaviorResolver> resolvers() {
        return List.of();
    }
}
```

- [ ] **Step 5: Add execution contracts and fallback**

Create `ControlProjectionStrategy.java`:

```java
package dev.traveler.core.capability.execution.spi;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.ControlProjectionFrame;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.internal.TraversalIntent;

public interface ControlProjectionStrategy {
    ControlProjectionFrame project(
            TraversalIntent intent,
            NavigationFrameInput input,
            MovementIntent previousIntent);
}
```

Create `ExecutionModule.java`:

```java
package dev.traveler.core.capability.execution.api;

import dev.traveler.core.capability.execution.spi.ControlProjectionStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.Optional;

public interface ExecutionModule extends PathfinderModule {
    boolean supportsExecution();

    Optional<ControlProjectionStrategy> controlProjectionStrategy();
}
```

Create `NoOpExecutionModule.java`:

```java
package dev.traveler.core.capability.execution.noop;

import dev.traveler.core.capability.execution.api.ExecutionModule;
import dev.traveler.core.capability.execution.spi.ControlProjectionStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.Optional;

public final class NoOpExecutionModule implements ExecutionModule {
    @Override
    public PathfinderModuleDescriptor descriptor() {
        return new PathfinderModuleDescriptor(
                "execution.none",
                PathfinderModuleType.EXECUTION,
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public boolean supportsExecution() {
        return false;
    }

    @Override
    public Optional<ControlProjectionStrategy> controlProjectionStrategy() {
        return Optional.empty();
    }
}
```

- [ ] **Step 6: Run fallback contract tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.FeatureFallbackContractTest"
```

Expected: PASS. If generics require explicit type inference, instantiate as `new IdentitySmoothingModule<String>()` in the test.

- [ ] **Step 7: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/capability modules/core/src/test/java/dev/traveler/core/capability
git commit -m "feat: add feature type interfaces and fallbacks"
```

---

### Task 6: Wrap Existing Traversals As Standard Modules

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/WalkTraversalModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/JumpTraversalModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/ClimbTraversalModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/DropTraversalModule.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/StandardTraversalModules.java`
- Test: `modules/core/src/test/java/dev/traveler/core/capability/traversal/StandardTraversalModulesTest.java`

- [ ] **Step 1: Write failing standard traversal module tests**

Create `StandardTraversalModulesTest.java`:

```java
package dev.traveler.core.capability.traversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.ClimbTraversalModule;
import dev.traveler.core.capability.traversal.impl.JumpTraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardTraversalModulesTest {
    @Test
    void standardTraversalModulesExposeSeparateFeatureModules() {
        List<TraversalModule> modules = StandardTraversalModules.standard();

        assertEquals(List.of("traversal.walk", "traversal.drop", "traversal.jump", "traversal.climb"),
                modules.stream().map(module -> module.descriptor().id()).toList());
    }

    @Test
    void jumpContributesConnectionsButNoRouteStartProvider() {
        JumpTraversalModule module = new JumpTraversalModule();

        assertEquals(PathfinderModuleType.TRAVERSAL, module.descriptor().type());
        assertFalse(module.connectionContributors().isEmpty());
        assertTrue(module.routeContributors().stream()
                .allMatch(contributor -> contributor.routeStartProviders().isEmpty()));
    }

    @Test
    void climbContributesAllClimbRouteParts() {
        ClimbTraversalModule module = new ClimbTraversalModule();

        assertFalse(module.connectionContributors().isEmpty());
        assertFalse(module.routeContributors().isEmpty());
        assertTrue(module.routeContributors().stream()
                .anyMatch(contributor -> !contributor.routeStepProviders().isEmpty()));
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.traversal.StandardTraversalModulesTest"
```

Expected: FAIL because standard traversal modules do not exist.

- [ ] **Step 3: Add module helper base**

Inside `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl`, create package-private `AbstractTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

abstract class AbstractTraversalModule implements TraversalModule {
    private final PathfinderModuleDescriptor descriptor;

    AbstractTraversalModule(String id, int priority) {
        descriptor = new PathfinderModuleDescriptor(id, PathfinderModuleType.TRAVERSAL, priority, true);
    }

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRouteContributor> routeContributors() {
        return List.of();
    }

    @Override
    public List<TraversalExecutionContributor> executionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRecoveryContributor> recoveryContributors() {
        return List.of();
    }

    @Override
    public List<TraversalDebugContributor> debugContributors() {
        return List.of();
    }
}
```

- [ ] **Step 4: Add walk/drop/jump/climb wrappers**

Create `WalkTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;

public final class WalkTraversalModule extends AbstractTraversalModule {
    public WalkTraversalModule() {
        super("traversal.walk", 10);
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of(() -> List.of(SurfaceConnectionProviders.adjacent()));
    }
}
```

Create `DropTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;

public final class DropTraversalModule extends AbstractTraversalModule {
    public DropTraversalModule() {
        super("traversal.drop", 20);
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of(() -> List.of(SurfaceConnectionProviders.drop()));
    }
}
```

Create `JumpTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;

public final class JumpTraversalModule extends AbstractTraversalModule {
    public JumpTraversalModule() {
        super("traversal.jump", 30);
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of(() -> List.of(SurfaceConnectionProviders.jump()));
    }
}
```

Create `ClimbTraversalModule.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.route.start.ClimbSurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.ClimbSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.ClimbSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import java.util.List;

public final class ClimbTraversalModule extends AbstractTraversalModule {
    public ClimbTraversalModule() {
        super("traversal.climb", 40);
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of(() -> List.of(SurfaceConnectionProviders.climb()));
    }

    @Override
    public List<TraversalRouteContributor> routeContributors() {
        return List.of(new ClimbRouteContributor());
    }

    private static final class ClimbRouteContributor implements TraversalRouteContributor {
        @Override
        public List<SurfaceRouteStartProvider> routeStartProviders() {
            return List.of(new ClimbSurfaceRouteStartProvider());
        }

        @Override
        public List<SurfaceTransitionProvider> transitionProviders() {
            return List.of(new ClimbSurfaceTransitionProvider());
        }

        @Override
        public List<SurfaceRouteStepProvider> routeStepProviders() {
            return List.of(new ClimbSurfaceRouteStepProvider());
        }
    }
}
```

Create `StandardTraversalModules.java`:

```java
package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import java.util.List;

public final class StandardTraversalModules {
    private StandardTraversalModules() {}

    public static List<TraversalModule> standard() {
        return List.of(
                new WalkTraversalModule(),
                new DropTraversalModule(),
                new JumpTraversalModule(),
                new ClimbTraversalModule());
    }
}
```

- [ ] **Step 5: Run standard traversal module tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.capability.traversal.StandardTraversalModulesTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/capability/traversal/impl modules/core/src/test/java/dev/traveler/core/capability/traversal/StandardTraversalModulesTest.java
git commit -m "feat: wrap traversal features as modules"
```

---

### Task 7: Drive RouteSearchComponents From Traversal Modules

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/route/RouteSearchComponents.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/internal/SurfaceTraversalFeatures.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/RouteSearchComponentsTest.java`

- [ ] **Step 1: Write failing tests for module-driven route components**

Create `RouteSearchComponentsTest.java` if absent, or add:

```java
package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteSearchComponentsTest {
    @Test
    void routeComponentsCanBeBuiltWithoutTraversalModules() {
        RouteSearchComponents components = RouteSearchComponents.withTraversalModules(List.of(
                new NoTraversalModule("traversal.none")));

        assertNotNull(components.surfaceGraphFactory());
        assertNotNull(components.surfaceStartResolver());
        assertNotNull(components.surfaceTransitionResolver());
        assertNotNull(components.surfaceRouteStepResolver());
    }

    @Test
    void standardComponentsUseTraversalModules() {
        RouteSearchComponents components = RouteSearchComponents.standard();

        assertTrue(components.surfaceGraphFactory() instanceof DefaultSurfaceRouteGraphFactory);
    }
}
```

- [ ] **Step 2: Run route component tests to verify failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.route.RouteSearchComponentsTest"
```

Expected: FAIL because `RouteSearchComponents.withTraversalModules(...)` does not exist.

- [ ] **Step 3: Add module aggregation to SurfaceTraversalFeatures**

Modify `SurfaceTraversalFeatures` to add:

```java
public static List<SurfaceConnectionProvider> connectionProvidersFromModules(
        List<? extends TraversalModule> modules) {
    List<SurfaceConnectionProvider> providers = new ArrayList<>();
    for (TraversalModule module : safeModules(modules)) {
        for (TraversalConnectionContributor contributor : module.connectionContributors()) {
            providers.addAll(contributor.surfaceConnectionProviders());
        }
    }
    return List.copyOf(providers);
}

public static List<SurfaceRouteStartProvider> routeStartProvidersFromModules(
        List<? extends TraversalModule> modules) {
    List<SurfaceRouteStartProvider> providers = new ArrayList<>();
    for (TraversalModule module : safeModules(modules)) {
        for (TraversalRouteContributor contributor : module.routeContributors()) {
            providers.addAll(contributor.routeStartProviders());
        }
    }
    return List.copyOf(providers);
}

public static List<SurfaceTransitionProvider> transitionProvidersFromModules(
        List<? extends TraversalModule> modules) {
    List<SurfaceTransitionProvider> providers = new ArrayList<>();
    for (TraversalModule module : safeModules(modules)) {
        for (TraversalRouteContributor contributor : module.routeContributors()) {
            providers.addAll(contributor.transitionProviders());
        }
    }
    return List.copyOf(providers);
}

public static List<SurfaceRouteStepProvider> routeStepProvidersFromModules(
        List<? extends TraversalModule> modules) {
    List<SurfaceRouteStepProvider> providers = new ArrayList<>();
    for (TraversalModule module : safeModules(modules)) {
        for (TraversalRouteContributor contributor : module.routeContributors()) {
            providers.addAll(contributor.routeStepProviders());
        }
    }
    return List.copyOf(providers);
}

private static List<TraversalModule> safeModules(List<? extends TraversalModule> modules) {
    return List.copyOf(Objects.requireNonNull(modules, "modules"));
}
```

Add imports:

```java
import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
```

- [ ] **Step 4: Add module-based RouteSearchComponents factory**

Modify `RouteSearchComponents`:

```java
public static RouteSearchComponents standard() {
    return withTraversalModules(StandardTraversalModules.standard());
}

public static RouteSearchComponents withTraversalModules(List<? extends TraversalModule> modules) {
    List<TraversalModule> traversalModules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    SurfaceTransitionResolver transitionResolver = new SurfaceTransitionResolver(
            SurfaceTraversalFeatures.transitionProvidersFromModules(traversalModules));
    return new RouteSearchComponents(
            new DefaultBlockRouteGraphFactory(),
            new DefaultSurfaceRouteGraphFactory(
                    SurfaceTraversalFeatures.connectionProvidersFromModules(traversalModules),
                    transitionResolver),
            new SurfaceRouteStartResolver(
                    SurfaceTraversalFeatures.routeStartProvidersFromModules(traversalModules)),
            transitionResolver,
            new SurfaceRouteStepResolver(
                    SurfaceTraversalFeatures.routeStepProvidersFromModules(traversalModules)),
            new AStarPathfinder<>(),
            new AStarPathfinder<>(),
            PathSmoothingSelector.farthestReachable(),
            PathSmoothingSelector.farthestReachable());
}
```

Add imports:

```java
import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
```

- [ ] **Step 5: Run route tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.route.RouteSearchComponentsTest" --tests "dev.traveler.core.route.RouteSearchServiceTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/route modules/core/src/test/java/dev/traveler/core/route/RouteSearchComponentsTest.java
git commit -m "feat: compose route search from traversal modules"
```

---

### Task 8: Add Default Pathfinder Kernel Route-Only Adapter

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/internal/DefaultPathfinderKernel.java`
- Create: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/impl/PathfinderKernels.java`
- Test: `modules/core/src/test/java/dev/traveler/core/pathfinder/kernel/PathfinderKernelRouteOnlyTest.java`

- [ ] **Step 1: Write failing route-only kernel tests**

Create `PathfinderKernelRouteOnlyTest.java`:

```java
package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.impl.PathfinderKernels;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderKernelRouteOnlyTest {
    @Test
    void routeOnlyKernelCanBeBuiltWithoutTraversalModules() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(
                new NoTraversalModule("traversal.none")));

        assertEquals(PathfinderKernelMode.ROUTE_ONLY, kernel.mode());
        assertNotNull(kernel);
    }
}
```

- [ ] **Step 2: Run test to verify compile failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderKernelRouteOnlyTest"
```

Expected: FAIL because `PathfinderKernels` and `DefaultPathfinderKernel` do not exist.

- [ ] **Step 3: Add internal kernel implementation**

Create `DefaultPathfinderKernel.java`:

```java
package dev.traveler.core.pathfinder.kernel.internal;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelResult;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.RouteSearchDiagnostics;
import dev.traveler.core.route.RouteSearchService;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementProfile;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultPathfinderKernel implements PathfinderKernel {
    private final RouteSearchService routeSearchService;
    private final PathfinderKernelMode mode;
    private final List<PathfinderModuleDescriptor> activeModules;

    public DefaultPathfinderKernel(
            RouteSearchService routeSearchService,
            PathfinderKernelMode mode,
            List<PathfinderModuleDescriptor> activeModules) {
        this.routeSearchService = Objects.requireNonNull(routeSearchService, "routeSearchService");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.activeModules = List.copyOf(Objects.requireNonNull(activeModules, "activeModules"));
    }

    @Override
    public PathfinderKernelResult findRoute(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            MovementProfile movementProfile) {
        var result = routeSearchService.findRoute(
                Objects.requireNonNull(worldLayer, "worldLayer"),
                Objects.requireNonNull(start, "start"),
                Objects.requireNonNull(goal, "goal"),
                Objects.requireNonNull(movementProfile, "movementProfile"));
        Optional<RoutePlan> routePlan = result.route().map(route -> routeSearchService.plan(route));
        RouteSearchDiagnostics diagnostics = result.diagnostics();
        return new PathfinderKernelResult(routePlan, diagnostics, activeModules);
    }

    @Override
    public PathfinderKernelMode mode() {
        return mode;
    }
}
```

If `RouteSearchService.plan(...)` is not public, add a public delegating method to `RouteSearchService`:

```java
public RoutePlan plan(RoutePath route) {
    return planner.plan(route);
}
```

- [ ] **Step 4: Add public kernel factory**

Create `PathfinderKernels.java`:

```java
package dev.traveler.core.pathfinder.kernel.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.internal.DefaultPathfinderKernel;
import dev.traveler.core.route.RouteSearchComponents;
import dev.traveler.core.route.RouteSearchService;
import java.util.List;
import java.util.Objects;

public final class PathfinderKernels {
    private PathfinderKernels() {}

    public static PathfinderKernel standardRouteOnly() {
        return routeOnly(dev.traveler.core.capability.traversal.impl.StandardTraversalModules.standard());
    }

    public static PathfinderKernel routeOnly(List<? extends TraversalModule> traversalModules) {
        List<TraversalModule> modules = List.copyOf(Objects.requireNonNull(traversalModules, "traversalModules"));
        RouteSearchComponents components = RouteSearchComponents.withTraversalModules(modules);
        RouteSearchService routeSearchService = new RouteSearchService(components);
        return new DefaultPathfinderKernel(
                routeSearchService,
                PathfinderKernelMode.ROUTE_ONLY,
                modules.stream().map(TraversalModule::descriptor).toList());
    }
}
```

- [ ] **Step 5: Run kernel tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderKernelRouteOnlyTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/pathfinder/kernel modules/core/src/test/java/dev/traveler/core/pathfinder/kernel modules/core/src/main/java/dev/traveler/core/route/RouteSearchService.java
git commit -m "feat: add route-only pathfinder kernel"
```

---

### Task 9: Make Recovery Registry Module-Driven

**Files:**
- Modify: `modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementHealthPolicyRegistry.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/JumpTraversalModule.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/ClimbTraversalModule.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/DropTraversalModule.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/capability/traversal/impl/WalkTraversalModule.java`
- Test: `modules/core/src/test/java/dev/traveler/core/navigation/recovery/MovementHealthPolicyRegistryTest.java`

- [ ] **Step 1: Write failing module-driven recovery tests**

Add to `MovementHealthPolicyRegistryTest.java`:

```java
@Test
void registryCanBeBuiltFromTraversalModules() {
    MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.fromTraversalModules(
            StandardTraversalModules.standard());

    assertNotNull(registry.policyFor(MovementAction.WALK));
    assertNotNull(registry.policyFor(MovementAction.JUMP));
    assertNotNull(registry.policyFor(MovementAction.CLIMB));
    assertNotNull(registry.policyFor(MovementAction.DROP));
}

@Test
void missingTraversalPolicyFallsBackToWalkPolicy() {
    MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.fromTraversalModules(List.of(
            new NoTraversalModule("traversal.none")));

    assertNotNull(registry.policyFor(MovementAction.WALK));
}
```

Add imports:

```java
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import java.util.List;
```

- [ ] **Step 2: Run recovery registry tests to verify failure**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.navigation.recovery.MovementHealthPolicyRegistryTest"
```

Expected: FAIL because `fromTraversalModules(...)` does not exist.

- [ ] **Step 3: Add recovery contributors to traversal modules**

In `WalkTraversalModule`, override:

```java
@Override
public List<TraversalRecoveryContributor> recoveryContributors() {
    return List.of(() -> java.util.Map.of(
            MovementAction.WALK, new WalkMovementHealthPolicy(),
            MovementAction.STEP_UP, new WalkMovementHealthPolicy()));
}
```

In `JumpTraversalModule`, override:

```java
@Override
public List<TraversalRecoveryContributor> recoveryContributors() {
    return List.of(() -> java.util.Map.of(
            MovementAction.JUMP, new JumpMovementHealthPolicy(),
            MovementAction.STEP_UP, new JumpMovementHealthPolicy()));
}
```

In `ClimbTraversalModule`, override:

```java
@Override
public List<TraversalRecoveryContributor> recoveryContributors() {
    return List.of(() -> java.util.Map.of(
            MovementAction.CLIMB, new ClimbMovementHealthPolicy()));
}
```

In `DropTraversalModule`, override:

```java
@Override
public List<TraversalRecoveryContributor> recoveryContributors() {
    return List.of(() -> java.util.Map.of(
            MovementAction.DROP, new DropMovementHealthPolicy()));
}
```

Add imports per file for `TraversalRecoveryContributor`, `MovementAction`, and the matching health policy.

- [ ] **Step 4: Add module-driven factory to MovementHealthPolicyRegistry**

Add:

```java
public static MovementHealthPolicyRegistry fromTraversalModules(List<? extends TraversalModule> modules) {
    EnumMap<MovementAction, MovementHealthPolicy> policies = new EnumMap<>(MovementAction.class);
    policies.put(MovementAction.WALK, new WalkMovementHealthPolicy());
    for (TraversalModule module : List.copyOf(Objects.requireNonNull(modules, "modules"))) {
        for (TraversalRecoveryContributor contributor : module.recoveryContributors()) {
            policies.putAll(contributor.movementHealthPolicies());
        }
    }
    return new MovementHealthPolicyRegistry(policies);
}
```

Add imports:

```java
import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import java.util.List;
```

- [ ] **Step 5: Run recovery tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.navigation.recovery.MovementHealthPolicyRegistryTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/navigation/recovery/MovementHealthPolicyRegistry.java modules/core/src/main/java/dev/traveler/core/capability/traversal/impl modules/core/src/test/java/dev/traveler/core/navigation/recovery/MovementHealthPolicyRegistryTest.java
git commit -m "feat: compose recovery policies from traversal modules"
```

---

### Task 10: Extract Climb Internals Behind SPI Without Changing Behavior

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/climb/spi/ClimbContactResolver.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/climb/spi/ClimbTargetProjector.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/climb/impl/DefaultClimbContactResolver.java`
- Create: `modules/core/src/main/java/dev/traveler/core/capability/traversal/climb/impl/DefaultClimbTargetProjector.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/world/navigation/SurfaceClimbTraversal.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/route/step/ClimbSurfaceRouteStepProvider.java`
- Test: `modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceClimbTraversalTest.java`
- Test: `modules/core/src/test/java/dev/traveler/core/route/step/ClimbSurfaceRouteStepProviderTest.java`

- [ ] **Step 1: Add characterization tests before extraction**

Add tests that pin existing behavior:

```java
@Test
void climbFaceTargetStillUsesProjectedFaceTarget() {
    Map<BlockPosition, SurfaceBlock> blocks = Map.of(
            new BlockPosition(1, 64, 0), ladder(HorizontalFacing.EAST),
            new BlockPosition(1, 65, 0), ladder(HorizontalFacing.EAST));
    SurfaceWorldLayer layer = position -> blocks.getOrDefault(position, SurfaceBlock.empty());

    WorldPoint target = SurfaceClimbTraversal.climbFaceTarget(
                    layer,
                    eastOfLadder(63, 64.0),
                    eastOfLadder(65, 66.0),
                    PLAYER)
            .orElseThrow();

    assertEquals(new WorldPoint(1.7, 66.0, 0.5), target);
}
```

Add the required imports to `SurfaceClimbTraversalTest`: `assertEquals`, `WorldPoint`, and `SurfaceWorldLayer`. Keep the existing `eastOfLadder(...)`, `ladder(...)`, and `PLAYER` fixtures. The expected x-coordinate is `1.7` because the default `TravelerSettings.CLIMB_FACE_INSET` is `0.3`, and `ClimbSurfaceGeometry.onFace(EAST, 0.3)` targets `1.0 - 0.3` inside block column `x = 1`.

- [ ] **Step 2: Run characterization tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "*SurfaceClimbTraversalTest" --tests "*ClimbSurfaceRouteStepProviderTest"
```

Expected: PASS before extraction.

- [ ] **Step 3: Extract SPI interfaces**

Create `ClimbContactResolver.java`:

```java
package dev.traveler.core.capability.traversal.climb.spi;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Optional;

public interface ClimbContactResolver {
    boolean canClimb(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities);

    Optional<List<SurfaceNode>> routeNodes(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities);
}
```

Create `ClimbTargetProjector.java`:

```java
package dev.traveler.core.capability.traversal.climb.spi;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Optional;

public interface ClimbTargetProjector {
    Optional<WorldPoint> faceTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities);
}
```

- [ ] **Step 4: Add default adapters around current static implementation**

Create `DefaultClimbContactResolver.java`:

```java
package dev.traveler.core.capability.traversal.climb.impl;

import dev.traveler.core.capability.traversal.climb.spi.ClimbContactResolver;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Optional;

public final class DefaultClimbContactResolver implements ClimbContactResolver {
    @Override
    public boolean canClimb(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return SurfaceClimbTraversal.canClimb(worldLayer, from, to, capabilities);
    }

    @Override
    public Optional<List<SurfaceNode>> routeNodes(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return SurfaceClimbTraversal.climbRouteNodes(worldLayer, from, to, capabilities);
    }
}
```

Create `DefaultClimbTargetProjector.java`:

```java
package dev.traveler.core.capability.traversal.climb.impl;

import dev.traveler.core.capability.traversal.climb.spi.ClimbTargetProjector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Optional;

public final class DefaultClimbTargetProjector implements ClimbTargetProjector {
    @Override
    public Optional<WorldPoint> faceTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return SurfaceClimbTraversal.climbFaceTarget(worldLayer, from, to, capabilities);
    }
}
```

- [ ] **Step 5: Inject projector into route step provider**

Modify `ClimbSurfaceRouteStepProvider`:

```java
private final ClimbContactResolver contactResolver;
private final ClimbTargetProjector targetProjector;

public ClimbSurfaceRouteStepProvider() {
    this(new DefaultClimbContactResolver(), new DefaultClimbTargetProjector());
}

public ClimbSurfaceRouteStepProvider(
        ClimbContactResolver contactResolver,
        ClimbTargetProjector targetProjector) {
    this.contactResolver = Objects.requireNonNull(contactResolver, "contactResolver");
    this.targetProjector = Objects.requireNonNull(targetProjector, "targetProjector");
}
```

Replace static route node call:

```java
List<SurfaceNode> climbNodes = contactResolver.routeNodes(
                context.worldLayer(),
                context.from(),
                context.to(),
                context.capabilities())
        .orElseGet(List::of);
```

Replace face target call:

```java
return targetProjector.faceTarget(
                context.worldLayer(),
                from,
                to,
                context.capabilities())
        .orElseGet(() -> context.pointOf(to));
```

Make `climbRouteSteps` and `climbStepTargetPoint` instance methods so they can use injected collaborators.

- [ ] **Step 6: Run climb tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "*SurfaceClimbTraversalTest" --tests "*ClimbSurfaceRouteStepProviderTest"
```

Expected: PASS with no behavior changes.

- [ ] **Step 7: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/capability/traversal/climb modules/core/src/main/java/dev/traveler/core/route/step/ClimbSurfaceRouteStepProvider.java modules/core/src/test/java/dev/traveler/core/world/navigation/SurfaceClimbTraversalTest.java modules/core/src/test/java/dev/traveler/core/route/step/ClimbSurfaceRouteStepProviderTest.java
git commit -m "refactor: extract climb traversal contracts"
```

---

### Task 11: Add Kernel Interchangeability Contract Tests

**Files:**
- Create: `modules/core/src/test/java/dev/traveler/core/pathfinder/kernel/PathfinderKernelInterchangeabilityTest.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/impl/PathfinderKernels.java`

- [ ] **Step 1: Write contract tests for removed modules**

Create `PathfinderKernelInterchangeabilityTest.java`:

```java
package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.DropTraversalModule;
import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.impl.PathfinderKernels;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderKernelInterchangeabilityTest {
    @Test
    void kernelWithoutClimbStillBuildsRouteOnlyPlanner() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(
                new WalkTraversalModule(),
                new DropTraversalModule()));

        assertEquals(PathfinderKernelMode.ROUTE_ONLY, kernel.mode());
    }

    @Test
    void kernelWithoutAnyEnabledTraversalStillBuilds() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(
                new NoTraversalModule("traversal.none")));

        assertEquals(PathfinderKernelMode.ROUTE_ONLY, kernel.mode());
    }

    @Test
    void kernelExposesOnlyEnabledTraversalDescriptors() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.<TraversalModule>of(
                new WalkTraversalModule(),
                new NoTraversalModule("traversal.none")));

        assertTrue(kernel.toString() != null);
    }
}
```

The third test intentionally starts simple. After `PathfinderKernel` exposes module descriptors through a query method, replace it with a direct assertion:

```java
assertEquals(List.of("traversal.walk"), kernel.activeModules().stream()
        .map(PathfinderModuleDescriptor::id)
        .toList());
```

- [ ] **Step 2: Run interchangeability tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderKernelInterchangeabilityTest"
```

Expected: PASS once `PathfinderKernels.routeOnly(...)` handles disabled modules.

- [ ] **Step 3: Filter disabled modules in kernel factory**

Modify `PathfinderKernels.routeOnly(...)`:

```java
List<TraversalModule> modules = List.copyOf(Objects.requireNonNull(traversalModules, "traversalModules"));
List<TraversalModule> enabledModules = modules.stream()
        .filter(module -> module.descriptor().enabled())
        .toList();
RouteSearchComponents components = RouteSearchComponents.withTraversalModules(enabledModules);
```

Return descriptors from `enabledModules`, not `modules`.

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.pathfinder.kernel.PathfinderKernelInterchangeabilityTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/impl/PathfinderKernels.java modules/core/src/test/java/dev/traveler/core/pathfinder/kernel/PathfinderKernelInterchangeabilityTest.java
git commit -m "test: prove kernel module interchangeability"
```

---

### Task 12: Update Documentation And Full Verification

**Files:**
- Modify: `docs/architecture/traveler-architecture-target.md`
- Create: `docs/architecture/pathfinder-kernel-modules.md`
- Verify: full Gradle checks

- [ ] **Step 1: Add architecture document**

Create `docs/architecture/pathfinder-kernel-modules.md`:

```markdown
# Pathfinder Kernel Modules

`Traveler` is the mod name. Core architecture classes use functional names such as
`PathfinderKernel`, `TraversalModule`, `SmoothingModule`, and `ExecutionModule`.

## Goals

- Keep route planning working when optional movement, smoothing, behavior, execution,
  recovery, or debug modules are absent.
- Keep abstractions and implementations in separate subpackages.
- Make every feature type expose an interface.
- Keep concrete implementations replaceable through the kernel composition root.

## Package Shape

Each feature area uses:

- `api` for stable contracts.
- `spi` for extension contracts.
- `impl` for standard implementations.
- `noop` for fallbacks.
- `internal` for private orchestration.
- `testing` for contract fixtures.

## Composition

`PathfinderKernel` is the composition root. Route search, traversal features,
smoothing, behavior resolution, execution, recovery, and debug are supplied through
module interfaces. Production code outside a feature must not import another feature's
`internal` package.

## Fallbacks

- No traversal module: route graph loses that movement's edges.
- No smoothing module: route keeps its raw path.
- No behavior module: unknown blocks resolve conservatively.
- No execution module: route-only mode remains available.
- No recovery module: no recovery action is emitted for that movement.
```

- [ ] **Step 2: Update target architecture doc**

In `docs/architecture/traveler-architecture-target.md`, add a short section:

```markdown
## Pathfinder Kernel Direction

The pathfinder core is moving toward a neutral `PathfinderKernel` composition root.
New internal architecture classes must not use the `Traveler` prefix; that prefix is
reserved for mod branding, commands, and platform entry points. Feature types must
publish interfaces in their `api` or `spi` packages and keep standard implementations
under `impl`, fallbacks under `noop`, private helpers under `internal`, and contract
fixtures under `testing`.
```

- [ ] **Step 3: Run targeted tests**

Run:

```powershell
.\gradlew.bat --no-daemon :core:test --tests "dev.traveler.core.architecture.DependencyGuardTest" --tests "dev.traveler.core.pathfinder.*" --tests "dev.traveler.core.capability.*"
```

Expected: PASS.

- [ ] **Step 4: Run full verification**

Run:

```powershell
.\gradlew.bat --no-daemon check
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add docs/architecture/pathfinder-kernel-modules.md docs/architecture/traveler-architecture-target.md
git commit -m "docs: document pathfinder kernel module architecture"
```

---

## Self-Review Checklist

- Every new feature type has an interface:
  - `TraversalModule`
  - `SmoothingModule`
  - `BlockBehaviorModule`
  - `ExecutionModule`
- Every feature type has a fallback:
  - `NoTraversalModule`
  - `IdentitySmoothingModule`
  - `UnknownBlockBehaviorModule`
  - `NoOpExecutionModule`
- Package organization separates abstractions from implementations:
  - `api`
  - `spi`
  - `impl`
  - `internal`
  - `noop`
  - `testing`
- No new internal architecture class uses `Traveler*`.
- The first kernel mode is route-only and does not require controller/execution.
- Existing behavior is preserved while standard traversal modules initially wrap current providers.
- Full verification command remains `.\gradlew.bat --no-daemon check`.

## Execution Notes

Implement this plan on top of the branch where the architecture guardrails and route/navigation decoupling are already green. Keep each task as a small commit. If any task exposes a larger design problem, stop at the failing test and revise the plan before broadening the refactor.
