package dev.traveler.core.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AnnotatedTravelerCommandFeature implements TravelerCommandFeature {
    private final List<TravelerCommandRoute> routes;

    private AnnotatedTravelerCommandFeature(List<TravelerCommandRoute> routes) {
        this.routes = List.copyOf(routes);
    }

    public static AnnotatedTravelerCommandFeature from(Object commandObject) {
        Object command = Objects.requireNonNull(commandObject, "commandObject");
        Class<?> commandType = command.getClass();
        TravelerCommand annotation = commandType.getAnnotation(TravelerCommand.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Command object must declare @TravelerCommand");
        }
        return new AnnotatedTravelerCommandFeature(routesFor(command, annotation.root()));
    }

    @Override
    public void register(TravelerCommandCatalog.Builder registry) {
        Objects.requireNonNull(registry, "registry");
        for (TravelerCommandRoute route : routes) {
            registry.add(route);
        }
    }

    private static List<TravelerCommandRoute> routesFor(Object command, String root) {
        List<TravelerCommandRoute> routes = new ArrayList<>();
        String rootPath = requireRoutePart(root, "root");
        for (Method method : sortedMethods(command.getClass())) {
            addRoute(command, rootPath, method, routes);
        }
        return routes;
    }

    private static List<Method> sortedMethods(Class<?> commandType) {
        List<Method> methods = new ArrayList<>(List.of(commandType.getDeclaredMethods()));
        methods.sort(Comparator.comparing(Method::getName));
        return methods;
    }

    private static void addRoute(
            Object command, String root, Method method, List<TravelerCommandRoute> routes) {
        TravelerSubcommand annotation = method.getAnnotation(TravelerSubcommand.class);
        if (annotation == null) {
            return;
        }
        validateMethod(method);
        routes.add(new TravelerCommandRoute(
                routePath(root, annotation.route()),
                annotation.description(),
                context -> invoke(command, method, context)));
    }

    private static void validateMethod(Method method) {
        if (method.getParameterCount() != 1) {
            throw invalidSignature(method);
        }
        if (!TravelerCommandContext.class.equals(method.getParameterTypes()[0])) {
            throw invalidSignature(method);
        }
        if (!TravelerCommandResult.class.equals(method.getReturnType())) {
            throw invalidSignature(method);
        }
        makeAccessible(method);
    }

    private static IllegalArgumentException invalidSignature(Method method) {
        return new IllegalArgumentException(
                "Annotated command method must accept TravelerCommandContext and return TravelerCommandResult: "
                        + method.getName());
    }

    private static void makeAccessible(Method method) {
        if (method.trySetAccessible()) {
            return;
        }
        throw new IllegalArgumentException("Annotated command method is not accessible: " + method.getName());
    }

    private static TravelerCommandResult invoke(
            Object command, Method method, TravelerCommandContext context) {
        try {
            return (TravelerCommandResult) method.invoke(command, context);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Annotated command method is not accessible", exception);
        } catch (InvocationTargetException exception) {
            throw invocationFailure(method, exception);
        }
    }

    private static IllegalStateException invocationFailure(Method method, InvocationTargetException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return new IllegalStateException("Annotated command method failed: " + method.getName(), runtimeException);
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Annotated command method failed: " + method.getName(), cause);
    }

    private static String routePath(String root, String route) {
        return root + " " + requireRoutePart(route, "route");
    }

    private static String requireRoutePart(String value, String name) {
        String text = Objects.requireNonNull(value, name).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Command " + name + " must not be blank");
        }
        return text;
    }
}
