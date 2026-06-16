package dev.traveler.mc.v1_21_11.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.render.ColorRgba;
import dev.traveler.core.render.DebugBox;
import dev.traveler.core.render.DebugLine;
import dev.traveler.core.render.DebugRenderFrame;
import dev.traveler.core.render.LinePrism;
import dev.traveler.core.render.PathDebugRenderModel;
import dev.traveler.core.render.RenderVertex;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.Vec3;

public final class FabricPathDebugRenderer {
    private static final double MIN_LINE_LENGTH = 1.0E-6D;

    private final PathDebugRenderModel renderModel;
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;

    public FabricPathDebugRenderer(PathDebugRenderModel renderModel, PathfinderDebugState debugState) {
        this(renderModel, debugState, null);
    }

    public FabricPathDebugRenderer(
            PathDebugRenderModel renderModel,
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState) {
        this.renderModel = Objects.requireNonNull(renderModel, "renderModel");
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = navigationState;
    }

    public void render(WorldRenderContext context) {
        if (context == null) {
            return;
        }
        DebugRenderFrame frame = navigationState == null
                ? renderModel.frameFor(debugState)
                : renderModel.frameFor(debugState, navigationState);
        render(context, frame);
    }

    private void render(WorldRenderContext context, DebugRenderFrame frame) {
        if (frame.lines().isEmpty() && frame.boxes().isEmpty()) {
            return;
        }
        DrawContext drawContext = DrawContext.create(context).orElse(null);
        if (drawContext == null) {
            return;
        }
        emitBoxes(frame, drawContext);
        emitLines(frame, drawContext);
    }

    public static Optional<Vec3> cameraPosition(WorldRenderContext context) {
        if (context == null || context.worldState() == null) {
            return Optional.empty();
        }
        LevelRenderState worldState = context.worldState();
        CameraRenderState cameraState = worldState.cameraRenderState;
        return Optional.ofNullable(cameraState).map(state -> state.pos);
    }

    private static void emitLines(DebugRenderFrame frame, DrawContext context) {
        for (DebugLine line : frame.lines()) {
            emitLine(line, context);
        }
    }

    private static void emitBoxes(DebugRenderFrame frame, DrawContext context) {
        for (DebugBox box : frame.boxes()) {
            emitBox(box, context);
        }
    }

    private static void emitLine(DebugLine line, DrawContext context) {
        RelativeLine relative = RelativeLine.create(line, context.camera());
        if (relative.length() <= MIN_LINE_LENGTH) {
            return;
        }
        emitLinePrism(LinePrism.around(relative.from(), relative.to(), line.thickness()), line.color(), context);
    }

    private static void emitBox(DebugBox box, DrawContext context) {
        RelativeBox relative = RelativeBox.create(box, context.camera());
        emitBottomFace(relative, context);
        emitTopFace(relative, context);
        emitNorthFace(relative, context);
        emitSouthFace(relative, context);
        emitWestFace(relative, context);
        emitEastFace(relative, context);
    }

    private static void emitBottomFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(false, false, false), box.vertex(true, false, false),
                box.vertex(true, false, true), box.vertex(false, false, true));
    }

    private static void emitTopFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(false, true, false), box.vertex(false, true, true),
                box.vertex(true, true, true), box.vertex(true, true, false));
    }

    private static void emitNorthFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(false, false, false), box.vertex(false, true, false),
                box.vertex(true, true, false), box.vertex(true, false, false));
    }

    private static void emitSouthFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(false, false, true), box.vertex(true, false, true),
                box.vertex(true, true, true), box.vertex(false, true, true));
    }

    private static void emitWestFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(false, false, false), box.vertex(false, false, true),
                box.vertex(false, true, true), box.vertex(false, true, false));
    }

    private static void emitEastFace(RelativeBox box, DrawContext context) {
        emitFace(context, box.color(), box.vertex(true, false, false), box.vertex(true, true, false),
                box.vertex(true, true, true), box.vertex(true, false, true));
    }

    private static void emitFace(
            DrawContext context,
            ColorRgba color,
            RenderVertex first,
            RenderVertex second,
            RenderVertex third,
            RenderVertex fourth) {
        emitBoxVertex(context.pose(), context.boxVertices(), first, color);
        emitBoxVertex(context.pose(), context.boxVertices(), second, color);
        emitBoxVertex(context.pose(), context.boxVertices(), third, color);
        emitBoxVertex(context.pose(), context.boxVertices(), fourth, color);
    }

    private static void emitLinePrism(LinePrism prism, ColorRgba color, DrawContext context) {
        emitFace(context, color, prism.startA(), prism.endA(), prism.endB(), prism.startB());
        emitFace(context, color, prism.startB(), prism.endB(), prism.endC(), prism.startC());
        emitFace(context, color, prism.startC(), prism.endC(), prism.endD(), prism.startD());
        emitFace(context, color, prism.startD(), prism.endD(), prism.endA(), prism.startA());
        emitFace(context, color, prism.startA(), prism.startB(), prism.startC(), prism.startD());
        emitFace(context, color, prism.endD(), prism.endC(), prism.endB(), prism.endA());
    }

    private static void emitBoxVertex(
            PoseStack.Pose pose, VertexConsumer vertices, RenderVertex vertex, ColorRgba color) {
        vertices.addVertex(pose, (float) vertex.x(), (float) vertex.y(), (float) vertex.z())
                .setColor(channel(color.red()), channel(color.green()), channel(color.blue()), channel(color.alpha()));
    }

    private static int channel(float value) {
        return Math.round(value * 255.0f);
    }

    private static RenderVertex relative(RenderVertex vertex, Vec3 camera) {
        return new RenderVertex(vertex.x() - camera.x, vertex.y() - camera.y, vertex.z() - camera.z);
    }

    private record DrawContext(PoseStack.Pose pose, MultiBufferSource consumers, Vec3 camera) {
        private static Optional<DrawContext> create(WorldRenderContext context) {
            PoseStack matrices = context.matrices();
            MultiBufferSource consumers = context.consumers();
            Optional<Vec3> camera = cameraPosition(context);
            if (matrices == null || consumers == null || camera.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new DrawContext(matrices.last(), consumers, camera.orElseThrow()));
        }

        private VertexConsumer boxVertices() {
            return consumers.getBuffer(RenderTypes.debugQuads());
        }
    }

    private record RelativeLine(RenderVertex from, RenderVertex to, double length) {
        private static RelativeLine create(DebugLine line, Vec3 camera) {
            RenderVertex from = relative(line.from(), camera);
            RenderVertex to = relative(line.to(), camera);
            double deltaX = to.x() - from.x();
            double deltaY = to.y() - from.y();
            double deltaZ = to.z() - from.z();
            double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            return new RelativeLine(from, to, length);
        }

    }

    private record RelativeBox(RenderVertex min, RenderVertex max, ColorRgba color) {
        private static RelativeBox create(DebugBox box, Vec3 camera) {
            return new RelativeBox(relative(box.min(), camera), relative(box.max(), camera), box.color());
        }

        private RenderVertex vertex(boolean maxX, boolean maxY, boolean maxZ) {
            return new RenderVertex(x(maxX), y(maxY), z(maxZ));
        }

        private double x(boolean useMax) {
            return useMax ? max.x() : min.x();
        }

        private double y(boolean useMax) {
            return useMax ? max.y() : min.y();
        }

        private double z(boolean useMax) {
            return useMax ? max.z() : min.z();
        }

    }
}
