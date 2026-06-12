package dev.traveler.mc.v1_21_11.fabric.command;

import dev.riege.buildmycommand.api.CommandSource;
import dev.traveler.mc.v1_21_11.common.command.TravelerCommandBlockPosition;
import dev.traveler.mc.v1_21_11.common.command.TravelerCommandPosition;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

final class FabricCommandSourceAdapter implements CommandSource, TravelerCommandPosition {
    private final FabricClientCommandSource source;

    FabricCommandSourceAdapter(FabricClientCommandSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public Optional<String> name() {
        return player().map(value -> value.getName().getString());
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Optional<T> adapter = unwrapValue(this, type);
        if (adapter.isPresent()) {
            return adapter;
        }
        return unwrapValue(source, type);
    }

    @Override
    public Optional<TravelerCommandBlockPosition> blockPosition() {
        return player().map(player -> new TravelerCommandBlockPosition(
                player.blockPosition().getX(),
                player.blockPosition().getY(),
                player.blockPosition().getZ()));
    }

    @Override
    public void reply(String message) {
        source.sendFeedback(Component.literal(message));
    }

    private Optional<LocalPlayer> player() {
        LocalPlayer player = source.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(player);
    }

    private static <T> Optional<T> unwrapValue(Object value, Class<T> type) {
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }
}
