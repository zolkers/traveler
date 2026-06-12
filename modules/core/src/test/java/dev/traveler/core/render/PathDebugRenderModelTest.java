package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.BlockPosition;
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
    void pathNodesBecomeCenteredLineSegmentsAndSquares() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35, 0.25);
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(1, 64, 1),
                new BlockPosition(2, 64, 1),
                new BlockPosition(3, 65, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(15, frame.lines().size());
        assertEquals(new RenderVertex(1.5, 64.35, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.5, 64.35, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(2.5, 64.35, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().get(1).to());
        assertEquals(new RenderVertex(2.5, 65.35, 1.5), frame.lines().get(2).from());
        assertEquals(new RenderVertex(3.5, 65.35, 1.5), frame.lines().get(2).to());
        assertEquals(new RenderVertex(1.25, 64.35, 1.25), frame.lines().get(3).from());
        assertEquals(new RenderVertex(1.75, 64.35, 1.25), frame.lines().get(3).to());
        assertEquals(color, frame.lines().getFirst().color());
    }

    @Test
    void stepDownSegmentsMoveHorizontallyBeforeDropping() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.35, 0.25);
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
}
