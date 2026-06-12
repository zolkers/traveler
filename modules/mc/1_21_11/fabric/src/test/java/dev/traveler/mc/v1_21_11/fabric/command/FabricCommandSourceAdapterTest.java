package dev.traveler.mc.v1_21_11.fabric.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class FabricCommandSourceAdapterTest {
    @Test
    void repliesThroughFabricFeedback() {
        TestFabricSource source = new TestFabricSource();
        FabricCommandSourceAdapter adapter = new FabricCommandSourceAdapter(source);

        adapter.reply("hello traveler");

        assertEquals(List.of("hello traveler"), source.feedbackMessages());
    }

    @Test
    void unwrapsFabricSource() {
        TestFabricSource source = new TestFabricSource();
        FabricCommandSourceAdapter adapter = new FabricCommandSourceAdapter(source);

        assertSame(source, adapter.unwrap(FabricClientCommandSource.class).orElseThrow());
        assertTrue(adapter.unwrap(String.class).isEmpty());
    }

    @Test
    void hasNoNameWhenPlayerIsUnavailable() {
        FabricCommandSourceAdapter adapter = new FabricCommandSourceAdapter(new TestFabricSource());

        assertTrue(adapter.name().isEmpty());
    }

    @Test
    void registeredCommandRepliesThroughFabricFeedback() throws Exception {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        TestFabricSource source = new TestFabricSource();

        FabricCommandBootstrap.registerInto(dispatcher, new TravelerCommandModule());
        dispatcher.execute("traveler path test", source);

        assertEquals(List.of("path test status=FOUND nodes=2"), source.feedbackMessages());
    }

    private static final class TestFabricSource implements FabricClientCommandSource {
        private final List<String> feedbackMessages = new java.util.ArrayList<>();

        private List<String> feedbackMessages() {
            return List.copyOf(feedbackMessages);
        }

        @Override
        public void sendFeedback(Component message) {
            feedbackMessages.add(message.getString());
        }

        @Override
        public void sendError(Component message) {
        }

        @Override
        public Minecraft getClient() {
            return null;
        }

        @Override
        public LocalPlayer getPlayer() {
            return null;
        }

        @Override
        public ClientLevel getWorld() {
            return null;
        }

        @Override
        public Collection<String> getOnlinePlayerNames() {
            return List.of();
        }

        @Override
        public Collection<String> getAllTeams() {
            return List.of();
        }

        @Override
        public Stream<Identifier> getAvailableSounds() {
            return Stream.empty();
        }

        @Override
        public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
            return Suggestions.empty();
        }

        @Override
        public Set<ResourceKey<Level>> levels() {
            return Set.of();
        }

        @Override
        public RegistryAccess registryAccess() {
            return null;
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            return null;
        }

        @Override
        public CompletableFuture<Suggestions> suggestRegistryElements(
                ResourceKey<? extends Registry<?>> resourceKey,
                SharedSuggestionProvider.ElementSuggestionType registryKey,
                SuggestionsBuilder builder,
                CommandContext<?> context) {
            return Suggestions.empty();
        }

        @Override
        public PermissionSet permissions() {
            return PermissionSet.ALL_PERMISSIONS;
        }
    }
}
