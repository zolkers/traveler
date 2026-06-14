package dev.traveler.mc.v1_21_11.fabric.hud;

import dev.traveler.core.hud.KeyboardHudModel;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class FabricHudBootstrap {
    private static final Identifier KEYBOARD_HUD_ID =
            Identifier.fromNamespaceAndPath("traveler", "keyboard_hud");

    private FabricHudBootstrap() {}

    public static void registerDefaultHud() {
        FabricKeyboardHudRenderer renderer = new FabricKeyboardHudRenderer(
                KeyboardHudModel.defaultTopLeft(),
                new FabricKeyboardHudSnapshotProvider());
        HudElementRegistry.addLast(
                KEYBOARD_HUD_ID,
                renderer);
    }
}
