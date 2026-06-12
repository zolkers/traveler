package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.command.TravelerCommandRoute;
import dev.traveler.core.command.TravelerCommandResult;
import java.util.Objects;

public final class BuildMyCommandCatalogAdapter {
    private final CommandRegistry registry;

    public BuildMyCommandCatalogAdapter(CommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(TravelerCommandCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog").routes().forEach(this::registerRoute);
    }

    private void registerRoute(TravelerCommandRoute route) {
        registry.route(route.path())
                .description(route.description())
                .executes(context -> execute(route, context));
    }

    private static CommandResult execute(TravelerCommandRoute route, CommandContext context) {
        dev.traveler.core.command.TravelerCommandContext travelerContext =
                new dev.traveler.core.command.TravelerCommandContext(
                        context.arguments(),
                        context.source()::reply);
        TravelerCommandResult result = route.handler().execute(travelerContext);
        result.message().ifPresent(travelerContext.feedback()::reply);
        return toBuildMyCommandResult(result);
    }

    private static CommandResult toBuildMyCommandResult(TravelerCommandResult result) {
        if (result.message().isEmpty()) {
            return Results.silent();
        }
        if (result.status() == TravelerCommandResult.Status.FAILURE) {
            return Results.failure(result.message().orElseThrow());
        }
        return Results.success(result.message().orElseThrow());
    }
}
