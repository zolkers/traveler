package dev.traveler.mc.v1_21_11.fabric;

import dev.riege.buildmycommand.api.CommandSource;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

final class FabricCommandSourceAdapter implements CommandSource {
    private final FabricClientCommandSource source;

    FabricCommandSourceAdapter(FabricClientCommandSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public Optional<String> name() {
        LocalPlayer player = source.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(player.getName().getString());
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isInstance(source)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(source));
    }

    @Override
    public void reply(String message) {
        source.sendFeedback(Component.literal(message));
    }
}
