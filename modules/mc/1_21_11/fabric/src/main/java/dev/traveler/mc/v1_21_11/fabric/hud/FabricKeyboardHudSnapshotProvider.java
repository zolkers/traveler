package dev.traveler.mc.v1_21_11.fabric.hud;

import dev.traveler.core.hud.KeyboardHudSnapshot;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public final class FabricKeyboardHudSnapshotProvider implements Supplier<KeyboardHudSnapshot> {
    @Override
    public KeyboardHudSnapshot get() {
        Options options = Minecraft.getInstance().options;
        return new KeyboardHudSnapshot(
                options.keyUp.isDown(),
                options.keyLeft.isDown(),
                options.keyDown.isDown(),
                options.keyRight.isDown(),
                options.keyJump.isDown(),
                options.keyShift.isDown(),
                options.keySprint.isDown());
    }
}
