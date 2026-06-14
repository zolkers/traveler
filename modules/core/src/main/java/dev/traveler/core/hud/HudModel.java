package dev.traveler.core.hud;

@FunctionalInterface
public interface HudModel<T> {
    HudFrame frameFor(T state, HudViewport viewport);
}
