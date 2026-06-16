package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.testing.NavigationDebugFrames;
import dev.traveler.core.navigation.debug.DebugLayer;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathDebugRenderModelTest {
    private static final ColorRgba ACTIVE_SEGMENT_COLOR = new ColorRgba(0.1f, 0.75f, 1.0f, 0.9f);
    private static final ColorRgba LOOKAHEAD_SEGMENT_COLOR = new ColorRgba(0.2f, 1.0f, 0.35f, 0.85f);
    private static final ColorRgba LATEST_SEARCH_COLOR = new ColorRgba(0.7f, 0.75f, 0.8f, 0.55f);
    private static final ColorRgba JUNCTION_WARNING_COLOR = new ColorRgba(1.0f, 0.48f, 0.08f, 0.8f);

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
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.update(foundPath(
                new BlockPosition(1, 64, 1),
                new BlockPosition(2, 64, 1),
                new BlockPosition(3, 65, 1)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(3, frame.lines().size());
        assertEquals(3, frame.boxes().size());
        assertEquals(new RenderVertex(1.5, 64.5, 1.5), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.5, 64.5, 1.5), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(2.5, 64.5, 1.5), frame.lines().get(1).from());
        assertEquals(new RenderVertex(2.5, 65.5, 1.5), frame.lines().get(1).to());
        assertEquals(new RenderVertex(2.5, 65.5, 1.5), frame.lines().get(2).from());
        assertEquals(new RenderVertex(3.5, 65.5, 1.5), frame.lines().get(2).to());
        assertEquals(new RenderVertex(1.0, 64.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 65.0, 2.0), frame.boxes().getFirst().max());
        assertTrue(frame.boxes().getFirst().color().alpha() < color.alpha());
        assertEquals(0.08, frame.lines().getFirst().thickness());
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
    void surfacePathsRenderLinesThroughVisualBlockCentersAndBoxesAroundVisualBlocks() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);
        PathDebugRenderModel model = new PathDebugRenderModel(color, 0.5);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(new BlockPosition(1, 63, 1), 1, 1, 63.5),
                new SurfaceNode(new BlockPosition(2, 63, 1), 0, 1, 64.0)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.lines().size());
        assertEquals(new RenderVertex(1.75, 64.0, 1.75), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(2.25, 64.5, 1.75), frame.lines().getFirst().to());
        assertEquals(new RenderVertex(1.0, 63.0, 1.0), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(2.0, 64.0, 2.0), frame.boxes().getFirst().max());
        assertEquals(new RenderVertex(2.0, 64.0, 1.0), frame.boxes().get(1).min());
        assertEquals(new RenderVertex(3.0, 65.0, 2.0), frame.boxes().get(1).max());
    }

    @Test
    void surfacePathLineYUsesSurfaceFloorPlusOffset() {
        PathDebugRenderModel model = new PathDebugRenderModel(new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f), 0.35);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(foundSurfacePath(
                new SurfaceNode(new BlockPosition(1, 63, 1), 1, 1, 63.5),
                new SurfaceNode(new BlockPosition(2, 63, 1), 0, 1, 64.0)));

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(63.85, frame.lines().getFirst().from().y());
        assertEquals(64.35, frame.lines().getFirst().to().y());
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
    void routePointsRenderExecutionLineWhileSurfaceNodesStillRenderSupportBoxes() {
        PathDebugRenderModel model = new PathDebugRenderModel(new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f), 0.5);
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateSurface(
                foundSurfacePath(
                        new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0),
                        new SurfaceNode(new BlockPosition(1, 64, 0), 0, 1, 64.0)),
                List.of(
                        new WorldPoint(0.75, 64.0, 0.75),
                        new WorldPoint(1.3, 64.0, 0.5)),
                "path");

        DebugRenderFrame frame = model.frameFor(state.snapshot());

        assertEquals(1, frame.lines().size());
        assertEquals(new RenderVertex(0.75, 64.5, 0.75), frame.lines().getFirst().from());
        assertEquals(new RenderVertex(1.3, 64.5, 0.5), frame.lines().getFirst().to());
        assertEquals(2, frame.boxes().size());
    }

    @Test
    void navigationDebugAddsOnlyTargetMarkerOverlay() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState state = new PathfinderDebugState();
        state.updateNavigation(
                new NavigationFrameInput(
                        new WorldPoint(0.0, 64.0, 0.0),
                        new CameraAngles(0.0, 0.0),
                        0.016),
                navigationFrame());

        DebugRenderFrame frame = model.frameFor(state);

        assertEquals(0, frame.lines().size());
        assertEquals(1, frame.boxes().size());
        assertEquals(new RenderVertex(0.75, 64.75, 2.75), frame.boxes().getFirst().min());
        assertEquals(new RenderVertex(1.25, 65.25, 3.25), frame.boxes().getFirst().max());
    }

    @Test
    void navigationStateRendersActiveLookaheadAndLatestSearchAsSeparateLayers() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        navigationState.start(navigationPath(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0)), "active", goalPlan());
        navigationState.prepareLookahead(navigationPath(
                new WorldPoint(10.0, 64.0, 0.0),
                new WorldPoint(20.0, 64.0, 0.0)), "lookahead", goalPlan());
        debugState.update(foundPath(
                new BlockPosition(50, 64, 0),
                new BlockPosition(51, 64, 0),
                new BlockPosition(52, 64, 0)));

        DebugRenderFrame frame = model.frameFor(debugState, navigationState);

        assertTrue(frame.lines().stream().anyMatch(line -> line.color().equals(ACTIVE_SEGMENT_COLOR)));
        assertTrue(frame.lines().stream().anyMatch(line -> line.color().equals(LOOKAHEAD_SEGMENT_COLOR)));
        assertFalse(frame.lines().stream().anyMatch(line -> line.color().equals(LATEST_SEARCH_COLOR)));
    }

    @Test
    void debugFrameExposesStableLayerRoles() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        navigationState.start(navigationPath(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0)), "active", goalPlan());
        navigationState.prepareLookahead(navigationPath(
                new WorldPoint(10.0, 64.0, 0.0),
                new WorldPoint(20.0, 64.0, 0.0)), "lookahead", goalPlan());

        var frame = model.debugFrameFor(debugState, navigationState);

        assertTrue(frame.layers().contains(DebugLayer.ACTIVE_SEGMENT));
        assertTrue(frame.layers().contains(DebugLayer.PREPARED_SEGMENT));
    }

    @Test
    void mismatchedLookaheadStartRendersJunctionWarningBox() {
        PathDebugRenderModel model = PathDebugRenderModel.defaultModel();
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        navigationState.start(navigationPath(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0)), "active", goalPlan());
        navigationState.prepareLookahead(navigationPath(
                new WorldPoint(10.5, 64.0, 0.0),
                new WorldPoint(20.0, 64.0, 0.0)), "lookahead", goalPlan());

        DebugRenderFrame frame = model.frameFor(debugState, navigationState);

        assertTrue(frame.boxes().stream().anyMatch(box -> box.color().equals(JUNCTION_WARNING_COLOR)));
    }

    @Test
    void colorChannelsMustStayInUnitRange() {
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(-0.1f, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new ColorRgba(0.0f, 1.1f, 0.0f, 1.0f));
    }

    @Test
    void lineThicknessMustBePositive() {
        ColorRgba color = new ColorRgba(0.1f, 0.6f, 1.0f, 0.85f);

        assertThrows(IllegalArgumentException.class, () -> new PathDebugRenderModel(color, 0.5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PathDebugRenderModel(color, 0.5, -0.01));
    }

    private static PathfinderResult<BlockPosition> foundPath(
            BlockPosition first, BlockPosition second, BlockPosition third) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        path.addNode(third);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static NavigationPath navigationPath(WorldPoint first, WorldPoint second) {
        return NavigationPath.of(List.of(first, second));
    }

    private static NavigationGoalPlan goalPlan() {
        return new NavigationGoalPlan(RouteGoal.xz(100, 0), RouteGoal.xz(20, 0), false, 8.0);
    }

    private static PathfinderResult<SurfaceNode> foundSurfacePath(SurfaceNode first, SurfaceNode second) {
        MutableGraphPath<SurfaceNode> path = new MutableGraphPath<>();
        path.addNode(first);
        path.addNode(second);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static dev.traveler.core.navigation.NavigationControlFrame navigationFrame() {
        return NavigationDebugFrames.approachFrame(new WorldPoint(1.0, 65.0, 3.0));
    }
}
