package dev.traveler.core.world.geometry;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public final class BlockShape {
    private static final int CELL_COUNT = 4;
    private static final BlockShape EMPTY = new BlockShape(List.of());
    private static final BlockShape FULL_CUBE =
            new BlockShape(List.of(new CollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)));
    private static final BlockShape BOTTOM_SLAB =
            new BlockShape(List.of(new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)));
    private static final BlockShape TOP_SLAB =
            new BlockShape(List.of(new CollisionBox(0.0, 0.5, 0.0, 1.0, 1.0, 1.0)));

    private final List<CollisionBox> boxes;
    private final double[] floorHeights;

    private BlockShape(List<CollisionBox> boxes) {
        this.boxes = List.copyOf(boxes);
        this.floorHeights = precomputeFloorHeights(this.boxes);
    }

    public static BlockShape empty() {
        return EMPTY;
    }

    public static BlockShape fullCube() {
        return FULL_CUBE;
    }

    public static BlockShape bottomSlab() {
        return BOTTOM_SLAB;
    }

    public static BlockShape topSlab() {
        return TOP_SLAB;
    }

    public static BlockShape of(List<CollisionBox> boxes) {
        List<CollisionBox> safeBoxes = List.copyOf(Objects.requireNonNull(boxes, "boxes"));
        if (safeBoxes.isEmpty()) {
            return empty();
        }
        return new BlockShape(safeBoxes);
    }

    public List<CollisionBox> boxes() {
        return boxes;
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public OptionalDouble floorHeightForCell(int cellX, int cellZ) {
        double floor = floorHeightForCellOrNaN(cellX, cellZ);
        if (Double.isNaN(floor)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(floor);
    }

    public double floorHeightForCellOrNaN(int cellX, int cellZ) {
        SurfaceCell.requireIndex(cellX, "cellX");
        SurfaceCell.requireIndex(cellZ, "cellZ");
        return floorHeights[cellIndex(cellX, cellZ)];
    }

    public boolean collidesWithCellBody(int cellX, int cellZ, double minY, double maxY) {
        double minX = SurfaceCell.min(cellX);
        double minZ = SurfaceCell.min(cellZ);
        double maxX = SurfaceCell.max(cellX);
        double maxZ = SurfaceCell.max(cellZ);
        for (CollisionBox box : boxes) {
            if (box.overlapsHorizontal(minX, minZ, maxX, maxZ) && box.overlapsVertical(minY, maxY)) {
                return true;
            }
        }
        return false;
    }

    private static double[] precomputeFloorHeights(List<CollisionBox> boxes) {
        double[] heights = emptyFloorHeights();
        for (int cellX = 0; cellX <= 1; cellX++) {
            precomputeCellColumn(boxes, heights, cellX);
        }
        return heights;
    }

    private static void precomputeCellColumn(List<CollisionBox> boxes, double[] heights, int cellX) {
        for (int cellZ = 0; cellZ <= 1; cellZ++) {
            heights[cellIndex(cellX, cellZ)] = floorHeightFor(boxes, cellX, cellZ);
        }
    }

    private static double floorHeightFor(List<CollisionBox> boxes, int cellX, int cellZ) {
        double floor = Double.NaN;
        double minX = SurfaceCell.min(cellX);
        double minZ = SurfaceCell.min(cellZ);
        double maxX = SurfaceCell.max(cellX);
        double maxZ = SurfaceCell.max(cellZ);
        for (CollisionBox box : boxes) {
            floor = highestFloor(floor, box, minX, minZ, maxX, maxZ);
        }
        return floor;
    }

    private static double highestFloor(
            double current,
            CollisionBox box,
            double minX,
            double minZ,
            double maxX,
            double maxZ) {
        if (!box.overlapsHorizontal(minX, minZ, maxX, maxZ)) {
            return current;
        }
        if (Double.isNaN(current)) {
            return box.maxY();
        }
        return Math.max(current, box.maxY());
    }

    private static double[] emptyFloorHeights() {
        double[] heights = new double[CELL_COUNT];
        for (int index = 0; index < CELL_COUNT; index++) {
            heights[index] = Double.NaN;
        }
        return heights;
    }

    private static int cellIndex(int cellX, int cellZ) {
        return cellX * 2 + cellZ;
    }
}
