package dev.traveler.mc.v1_21_11.fabric.hud;

import dev.traveler.core.hud.HudBox;
import dev.traveler.core.hud.HudFrame;
import dev.traveler.core.hud.HudModel;
import dev.traveler.core.hud.HudText;
import dev.traveler.core.hud.HudViewport;
import dev.traveler.core.hud.KeyboardHudSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class FabricKeyboardHudRenderer implements HudElement {
    private final HudModel<KeyboardHudSnapshot> model;
    private final Supplier<KeyboardHudSnapshot> input;

    public FabricKeyboardHudRenderer(HudModel<KeyboardHudSnapshot> model, Supplier<KeyboardHudSnapshot> input) {
        this.model = Objects.requireNonNull(model, "model");
        this.input = Objects.requireNonNull(input, "input");
    }

    @Override
    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        if (context == null) {
            return;
        }
        HudFrame frame = model.frameFor(input.get(), new HudViewport(context.guiWidth(), context.guiHeight()));
        renderBoxes(context, frame);
        renderText(context, frame);
    }

    private static void renderBoxes(GuiGraphics context, HudFrame frame) {
        for (HudBox box : frame.boxes()) {
            context.fill(
                    box.bounds().x(),
                    box.bounds().y(),
                    box.bounds().x() + box.bounds().width(),
                    box.bounds().y() + box.bounds().height(),
                    box.color().argb());
        }
    }

    private static void renderText(GuiGraphics context, HudFrame frame) {
        for (HudText text : frame.texts()) {
            context.drawString(
                    Minecraft.getInstance().font,
                    text.value(),
                    text.position().x(),
                    text.position().y(),
                    text.color().argb(),
                    false);
        }
    }
}
