package dev.traveler.core.hud;

public record KeyboardHudSnapshot(
        boolean forward,
        boolean left,
        boolean back,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint) {}
