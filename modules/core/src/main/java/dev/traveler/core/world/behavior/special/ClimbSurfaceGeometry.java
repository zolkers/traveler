package dev.traveler.core.world.behavior.special;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.SurfaceCell;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record ClimbSurfaceGeometry(
        int cellX,
        int cellZ,
        double localTargetX,
        double localTargetZ) {
    public ClimbSurfaceGeometry {
        SurfaceCell.requireIndex(cellX, "cellX");
        SurfaceCell.requireIndex(cellZ, "cellZ");
        requireLocalTarget(localTargetX, "localTargetX");
        requireLocalTarget(localTargetZ, "localTargetZ");
    }

    public static ClimbSurfaceGeometry onFace(HorizontalFacing face, double faceInset) {
        HorizontalFacing safeFace = Objects.requireNonNull(face, "face");
        requireFaceInset(faceInset);
        return switch (safeFace) {
            case WEST -> new ClimbSurfaceGeometry(0, 1, faceInset, 0.5);
            case EAST -> new ClimbSurfaceGeometry(1, 1, 1.0 - faceInset, 0.5);
            case NORTH -> new ClimbSurfaceGeometry(1, 0, 0.5, faceInset);
            case SOUTH -> new ClimbSurfaceGeometry(1, 1, 0.5, 1.0 - faceInset);
        };
    }

    public SurfaceNode node(BlockPosition climbBlockPosition) {
        BlockPosition position = Objects.requireNonNull(climbBlockPosition, "climbBlockPosition");
        return new SurfaceNode(position, cellX, cellZ, position.y());
    }

    public NavigationPoint target(BlockPosition climbBlockPosition, double floorY) {
        BlockPosition position = Objects.requireNonNull(climbBlockPosition, "climbBlockPosition");
        if (!Double.isFinite(floorY)) {
            throw new IllegalArgumentException("floorY must be finite.");
        }
        return new NavigationPoint(
                position.x() + localTargetX,
                floorY,
                position.z() + localTargetZ);
    }

    private static void requireLocalTarget(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0.");
        }
    }

    private static void requireFaceInset(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 0.5) {
            throw new IllegalArgumentException("faceInset must be between 0.0 and 0.5.");
        }
    }
}
