package dev.traveler.core.render;

public record ColorRgba(float red, float green, float blue, float alpha) {
    public ColorRgba {
        validate(red, "red");
        validate(green, "green");
        validate(blue, "blue");
        validate(alpha, "alpha");
    }

    private static void validate(float channel, String name) {
        if (channel < 0.0f || channel > 1.0f) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}
