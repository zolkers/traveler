package dev.traveler.core.settings;

import java.util.Objects;
import java.util.function.Predicate;

public record Setting<T>(
        String key,
        Class<T> type,
        T defaultValue,
        Predicate<T> validator,
        String validationMessage) {
    public Setting {
        requireKey(key);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(validationMessage, "validationMessage");
        defaultValue = validateValue(type, validator, validationMessage, defaultValue);
    }

    public static <T> Setting<T> of(String key, Class<T> type, T defaultValue) {
        return of(key, type, defaultValue, value -> true, key + " is invalid");
    }

    public static <T> Setting<T> of(
            String key,
            Class<T> type,
            T defaultValue,
            Predicate<T> validator,
            String validationMessage) {
        return new Setting<>(key, type, defaultValue, validator, validationMessage);
    }

    public T validate(T value) {
        return validateValue(type, validator, validationMessage, value);
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Setting key must not be blank.");
        }
    }

    private static <T> T validateValue(
            Class<T> type,
            Predicate<T> validator,
            String validationMessage,
            T value) {
        Objects.requireNonNull(value, "value");
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Setting value must be a " + type.getSimpleName() + ".");
        }
        if (!validator.test(value)) {
            throw new IllegalArgumentException(validationMessage);
        }
        return value;
    }
}
