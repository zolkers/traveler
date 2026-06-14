package dev.traveler.core.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KeyboardHudModel implements HudModel<KeyboardHudSnapshot> {
    public static final HudColor PRESSED_BACKGROUND = new HudColor(0xCC4FA3FF);
    public static final HudColor RELEASED_BACKGROUND = new HudColor(0x80303030);
    public static final HudColor TEXT = new HudColor(0xFFFFFFFF);

    private static final int KEY_WIDTH = 24;
    private static final int KEY_HEIGHT = 18;
    private static final int ACTION_HEIGHT = 16;
    private static final int GAP = 2;
    private static final int MOVEMENT_ACTION_GAP = 4;
    private static final int GRID_WIDTH = KEY_WIDTH * 3 + GAP * 2;
    private static final int GRID_HEIGHT = KEY_HEIGHT * 2 + ACTION_HEIGHT * 2 + GAP + MOVEMENT_ACTION_GAP * 2;
    private static final HudSize SIZE = new HudSize(GRID_WIDTH, GRID_HEIGHT);

    private final HudPlacement placement;

    public KeyboardHudModel(HudPlacement placement) {
        this.placement = Objects.requireNonNull(placement, "placement");
    }

    public static KeyboardHudModel defaultTopLeft() {
        return new KeyboardHudModel(HudPlacement.topLeft(6, 6));
    }

    @Override
    public HudFrame frameFor(KeyboardHudSnapshot state, HudViewport viewport) {
        KeyboardHudSnapshot safeState = Objects.requireNonNull(state, "state");
        HudPoint origin = placement.position(Objects.requireNonNull(viewport, "viewport"), SIZE);
        List<HudBox> boxes = new ArrayList<>();
        List<HudText> texts = new ArrayList<>();
        addMovementRows(boxes, texts, origin, safeState);
        addActionRows(boxes, texts, origin, safeState);
        return new HudFrame(boxes, texts);
    }

    private static void addMovementRows(
            List<HudBox> boxes,
            List<HudText> texts,
            HudPoint origin,
            KeyboardHudSnapshot state) {
        int secondColumn = origin.x() + KEY_WIDTH + GAP;
        addKey(boxes, texts, "keyboard.forward", "W", secondColumn, origin.y(), KEY_WIDTH, KEY_HEIGHT, state.forward());
        int rowY = origin.y() + KEY_HEIGHT + GAP;
        addKey(boxes, texts, "keyboard.left", "A", origin.x(), rowY, KEY_WIDTH, KEY_HEIGHT, state.left());
        addKey(boxes, texts, "keyboard.back", "S", secondColumn, rowY, KEY_WIDTH, KEY_HEIGHT, state.back());
        addKey(boxes, texts, "keyboard.right", "D", secondColumn + KEY_WIDTH + GAP, rowY,
                KEY_WIDTH, KEY_HEIGHT, state.right());
    }

    private static void addActionRows(
            List<HudBox> boxes,
            List<HudText> texts,
            HudPoint origin,
            KeyboardHudSnapshot state) {
        int jumpY = origin.y() + KEY_HEIGHT * 2 + GAP + MOVEMENT_ACTION_GAP;
        addKey(boxes, texts, "keyboard.jump", "SPACE", origin.x(), jumpY, GRID_WIDTH, ACTION_HEIGHT, state.jump());
        int bottomY = jumpY + ACTION_HEIGHT + GAP;
        int actionWidth = (GRID_WIDTH - GAP) / 2;
        addKey(boxes, texts, "keyboard.sneak", "SHIFT", origin.x(), bottomY,
                actionWidth, ACTION_HEIGHT, state.sneak());
        addKey(boxes, texts, "keyboard.sprint", "CTRL", origin.x() + actionWidth + GAP, bottomY,
                actionWidth, ACTION_HEIGHT, state.sprint());
    }

    private static void addKey(
            List<HudBox> boxes,
            List<HudText> texts,
            String id,
            String label,
            int x,
            int y,
            int width,
            int height,
            boolean pressed) {
        boxes.add(new HudBox(
                id,
                new HudRectangle(x, y, width, height),
                pressed ? PRESSED_BACKGROUND : RELEASED_BACKGROUND));
        texts.add(new HudText(id + ".label", label, new HudPoint(x + 4, y + 5), TEXT));
    }
}
