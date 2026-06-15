package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import java.util.Objects;

final class TravelerCommandReplies {
    private TravelerCommandReplies() {}

    static CommandResult success(CommandContext context, String message) {
        Objects.requireNonNull(context, "context").source().reply(message);
        return Results.success(message);
    }
}
