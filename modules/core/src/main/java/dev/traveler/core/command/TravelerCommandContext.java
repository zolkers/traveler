package dev.traveler.core.command;

import java.util.Map;
import java.util.Objects;

public record TravelerCommandContext(Map<String, Object> arguments, TravelerCommandFeedback feedback) {
    public TravelerCommandContext {
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        feedback = Objects.requireNonNull(feedback, "feedback");
    }

    public <T> T arg(String name, Class<T> type) {
        Object value = arguments.get(Objects.requireNonNull(name, "name"));
        if (value == null) {
            throw new IllegalArgumentException("Missing command argument: " + name);
        }
        return castArgument(name, value, type);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castArgument(String name, Object value, Class<T> type) {
        Class<?> boxed = boxedType(Objects.requireNonNull(type, "type"));
        if (!boxed.isInstance(value)) {
            throw new IllegalArgumentException("Command argument has wrong type: " + name);
        }
        return (T) boxed.cast(value);
    }

    private static Class<?> boxedType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        return Character.class;
    }
}
