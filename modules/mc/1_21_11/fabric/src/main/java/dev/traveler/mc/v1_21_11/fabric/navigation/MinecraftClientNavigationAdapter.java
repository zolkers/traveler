package dev.traveler.mc.v1_21_11.fabric.navigation;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

public final class MinecraftClientNavigationAdapter implements ClientNavigationAdapter {
    private final Minecraft client;

    public MinecraftClientNavigationAdapter(Minecraft client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public static MinecraftClientNavigationAdapter currentClient() {
        return new MinecraftClientNavigationAdapter(Minecraft.getInstance());
    }

    @Override
    public Optional<NavigationFrameInput> frameInput(double deltaSeconds) {
        LocalPlayer player = client.player;
        if (player == null) {
            return Optional.empty();
        }
        NavigationPoint position = new NavigationPoint(player.getX(), player.getY(), player.getZ());
        CameraAngles camera = new CameraAngles(player.getYRot(), player.getXRot());
        return Optional.of(new NavigationFrameInput(position, camera, deltaSeconds));
    }

    @Override
    public void apply(NavigationControlFrame frame) {
        NavigationControlFrame controlFrame = Objects.requireNonNull(frame, "frame");
        applyCamera(controlFrame.cameraAngles());
        applyIntent(controlFrame.intent());
    }

    @Override
    public void release() {
        applyIntent(MovementIntent.idle());
    }

    private void applyCamera(CameraAngles angles) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        player.setYRot((float) angles.yawDegrees());
        player.setXRot((float) angles.pitchDegrees());
    }

    private void applyIntent(MovementIntent intent) {
        Options options = client.options;
        if (options == null) {
            return;
        }
        options.keyUp.setDown(intent.forward());
        options.keyDown.setDown(intent.back());
        options.keyLeft.setDown(intent.left());
        options.keyRight.setDown(intent.right());
        options.keyJump.setDown(intent.jump());
        options.keySprint.setDown(intent.sprint());
    }
}
