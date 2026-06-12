package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class PathDebugRenderModelTest {
    @Test
    void emptyStateProducesEmptyFrame() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();

        DebugRenderFrame frame = model.frameFor(new PathfinderDebugState().snapshot());

        assertTrue(frame.lines().isEmpty());
    }

    @Test
    void nonFoundResultProducesEmptyFrame() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>()));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertTrue(frame.lines().isEmpty());
    }

    @Test
    void pathNodesBecomeCenteredLineSegmentsAndTransparentBlockBoxes() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(1, 64, 1),
                new BlockPosition(2, 64, 1),
                new BlockPosition(3, 65, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(3, frame.lines().size());
        assertEquals(3, frame.boxes().size());
        assertEquals(new RenderVertex(1.5, 64.35, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.5, 64.35, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(2.5, 64.35, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().get(1).to());
        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().get(2).from());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().get(2).to());
        assertEquals(new RenderVertex(1.0, 64.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 65.0, 2.0), frame.boxes().getFirst().max());
        assertTrue(frame.boxes().getFirst().color().alpha() < color.alpha());
        assertEquals(color, frame.lines().getFirst().color());
    }

    @Test
    void stepDownSegmentsMoveHorizontallyBeforeDropping() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(2, 65, 1),
                new BlockPosition(3, 64, 1),
                new BlockPosition(4, 64, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(3.5, 64.35, 1.5), frame.lines().get(1).to());
    }

    @Test
    void surfacePathsRenderLinesJustAboveFloorAndBoxesAroundVisualBlocks() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(new BlockPosition(1, 63, 1), 1, 1, 63.5),
                new SurfaceNode(new BlockPosition(2, 63, 1), 0, 1, 64.0)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.lines().size());
        assertEquals(new RenderVertex(1.75, 63.58, 1.75), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.25, 64.08, 1.75), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(1.0, 63.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 64.0, 2.0), frame.boxes().getFirst().max());
        assertEquals(new RenderVertex(2.0, 64.0, 1.0), frame.boxes().get(1).min());
        assertEquals(new RenderVertex(3.0, 65.0, 2.0), frame.boxes().get(1).max());
    }

    @Test
    void surfacePathBoxesAreDeduplicatedPerVisualBlock() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35);
        BlockPosition support = new BlockPosition(1, 63, 1);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(support, 0, 0, 63.5),
                new SurfaceNode(support, 1, 1, 63.5)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.boxes().size());
        assertEquals(new RenderVertex(1.0, 63.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 64.0, 2.0), frame.boxes().getFirst().max());
    }

    @Test
    void colorChannelsMustStayInUnitRange() {
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(-0.1f, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(0.0f, 1.1f, 0.0f, 1.0f));
    }

    private static PathfinderResult<BlockPosition> foundPath(
            BlockPosition first, BlockPosition second, BlockPosition third) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        path.addNode(third);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static PathfinderResult<SurfaceNode> foundSurfacePath(SurfaceNode first, SurfaceNode second) {
        MutableGraphPath<SurfaceNode> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }
}
