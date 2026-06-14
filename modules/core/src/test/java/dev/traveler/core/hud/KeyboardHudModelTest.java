package dev.traveler.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KeyboardHudModelTest {
    @Test
    void rendersTopLeftKeyboardGridWithPressedAndReleasedKeyStates() {
        KeyboardHudModel model = KeyboardHudModel.defaultTopLeft();
        KeyboardHudSnapshot keys = new KeyboardHudSnapshot(
                true, false, true, false, true, false, false);

        HudFrame frame = model.frameFor(keys, new HudViewport(320, 180));

        assertEquals(new HudRectangle(32, 6, 24, 18), frame.box("keyboard.forward").orElseThrow().bounds());
        assertEquals(KeyboardHudModel.PRESSED_BACKGROUND, frame.box("keyboard.forward").orElseThrow().color());
        assertEquals(KeyboardHudModel.RELEASED_BACKGROUND, frame.box("keyboard.left").orElseThrow().color());
        assertEquals(new HudRectangle(6, 48, 76, 16), frame.box("keyboard.jump").orElseThrow().bounds());
        assertEquals("W", frame.text("keyboard.forward.label").orElseThrow().value());
        assertEquals("SPACE", frame.text("keyboard.jump.label").orElseThrow().value());
    }
}
