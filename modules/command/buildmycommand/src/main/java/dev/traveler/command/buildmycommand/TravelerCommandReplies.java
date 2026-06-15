package dev.traveler.command.buildmycommand;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.traveler.core.command.TravelerCommandResponse;
import java.util.Objects;

final class TravelerCommandReplies {
    private TravelerCommandReplies() {
    }

    static CommandResult result(TravelerCommandResponse response) {
        return Results.success(Objects.requireNonNull(response, "response").message());
    }
}
