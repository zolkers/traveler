package dev.traveler.command.buildmycommand.testing;

import dev.riege.buildmycommand.api.CommandSource;
import dev.traveler.core.command.TravelerCommandBlockPosition;
import dev.traveler.core.command.TravelerCommandPosition;
import dev.traveler.core.world.block.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TestCommandSource implements CommandSource, TravelerCommandPosition {
    private final List<String> replies = new ArrayList<>();
    private final BlockPosition position;

    public TestCommandSource() {
        this(null);
    }

    public TestCommandSource(BlockPosition position) {
        this.position = position;
    }

    @Override
    public Optional<TravelerCommandBlockPosition> blockPosition() {
        return Optional.ofNullable(position)
                .map(pos -> new TravelerCommandBlockPosition(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (!type.isInstance(this)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(this));
    }

    @Override
    public void reply(String message) {
        replies.add(message);
    }

    public List<String> replies() {
        return List.copyOf(replies);
    }
}
