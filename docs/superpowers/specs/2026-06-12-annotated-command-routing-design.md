# Annotated Command Routing Design

## Goal
Traveler commands should be declared with Java annotations so each command class owns one base route and each command method owns one subroute.

## Architecture
The annotation API lives in `dev.traveler.core.command` because command routing is part of Traveler's loader-neutral core. Minecraft and BuildMyCommand modules keep their current role as adapters: they consume the core `TravelerCommandCatalog` and do not participate in annotation scanning.

Command classes use `@TravelerCommand(root = "...")`. Methods use `@TravelerSubcommand(route = "...", description = "...")` and must expose exactly one `TravelerCommandContext` parameter and return `TravelerCommandResult`. A core scanner converts an annotated command object into `TravelerCommandRoute` instances.

## API Shape
```java
@TravelerCommand(root = "traveler path")
public final class PathTravelerCommandFeature {
    @TravelerSubcommand(route = "test", description = "Runs a Traveler path debug search")
    public TravelerCommandResult pathTest(TravelerCommandContext context) {
        return TravelerCommandResult.success("ok");
    }
}
```

The generated route path is the trimmed class root plus the trimmed method subroute. Empty roots or subroutes are rejected by the existing route validation.

## Validation
The scanner rejects command classes without `@TravelerCommand`, methods with unsupported signatures, duplicate routes, and annotated methods that cannot be invoked. It keeps failures deterministic with `IllegalArgumentException` for invalid declarations and wraps reflective invocation failures in `IllegalStateException`.

## Scope
This design does not add permissions, aliases, async execution, argument annotations, or compile-time annotation processing. Those can be layered later without changing the route/catalog contract.
