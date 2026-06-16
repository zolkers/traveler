package dev.traveler.core.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.behavior.noop.UnknownBlockBehaviorModule;
import dev.traveler.core.capability.execution.noop.NoOpExecutionModule;
import dev.traveler.core.capability.execution.spi.ControlProjectionIntent;
import dev.traveler.core.capability.execution.spi.ControlProjectionStrategy;
import dev.traveler.core.capability.smoothing.noop.IdentitySmoothingModule;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.ControlProjectionFrame;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import org.junit.jupiter.api.Test;

class FeatureFallbackContractTest {
    @Test
    void identitySmoothingReturnsOriginalPath() {
        IdentitySmoothingModule<String> module = new IdentitySmoothingModule<>();
        MutableGraphPath<String> path = new MutableGraphPath<>();
        path.addNode("a");
        path.addNode("b");

        assertSame(path, module.strategy().smooth(path));
        assertEquals(PathfinderModuleType.SMOOTHING, module.descriptor().type());
    }

    @Test
    void identitySmoothingIsDisabledFallback() {
        IdentitySmoothingModule<String> module = new IdentitySmoothingModule<>();

        assertEquals("smoothing.identity", module.descriptor().id());
        assertEquals(Integer.MAX_VALUE, module.descriptor().priority());
        assertFalse(module.descriptor().enabled());
        assertThrows(NullPointerException.class, () -> module.strategy().smooth(null));
    }

    @Test
    void unknownBehaviorModuleIsConservative() {
        UnknownBlockBehaviorModule module = new UnknownBlockBehaviorModule();

        assertEquals(PathfinderModuleType.BEHAVIOR, module.descriptor().type());
        assertFalse(module.descriptor().enabled());
    }

    @Test
    void unknownBehaviorModuleHasNoResolvers() {
        UnknownBlockBehaviorModule module = new UnknownBlockBehaviorModule();

        assertEquals("behavior.unknown", module.descriptor().id());
        assertEquals(Integer.MAX_VALUE, module.descriptor().priority());
        assertTrue(module.resolvers().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> module.resolvers().add(null));
    }

    @Test
    void noOpExecutionDisablesInputProjection() {
        NoOpExecutionModule module = new NoOpExecutionModule();

        assertEquals(PathfinderModuleType.EXECUTION, module.descriptor().type());
        assertFalse(module.supportsExecution());
    }

    @Test
    void noOpExecutionModuleHasNoControlProjectionStrategy() {
        NoOpExecutionModule module = new NoOpExecutionModule();

        assertEquals("execution.none", module.descriptor().id());
        assertEquals(Integer.MAX_VALUE, module.descriptor().priority());
        assertFalse(module.descriptor().enabled());
        assertTrue(module.controlProjectionStrategy().isEmpty());
    }

    @Test
    void controlProjectionStrategyUsesExecutionScopedIntent() throws NoSuchMethodException {
        assertEquals(
                ControlProjectionFrame.class,
                ControlProjectionStrategy.class
                        .getMethod(
                                "project",
                                ControlProjectionIntent.class,
                                NavigationFrameInput.class,
                                MovementIntent.class)
                        .getReturnType());
    }
}
