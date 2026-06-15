package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.api.CommandContext;
import dev.traveler.core.command.TravelerCommandBlockPosition;
import dev.traveler.core.command.TravelerCommandPosition;
import dev.traveler.core.command.TravelerCommandSource;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

final class BuildMyCommandTravelerSource implements TravelerCommandSource {
    private final CommandContext context;

    private BuildMyCommandTravelerSource(CommandContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    static BuildMyCommandTravelerSource from(CommandContext context) {
        return new BuildMyCommandTravelerSource(context);
    }

    @Override
    public BlockPosition blockPosition() {
        return context.source()
                .unwrap(TravelerCommandPosition.class)
                .flatMap(TravelerCommandPosition::blockPosition)
                .map(TravelerCommandBlockPosition::toCorePosition)
                .orElse(null);
    }

    @Override
    public void reply(String message) {
        context.source().reply(message);
    }
}
