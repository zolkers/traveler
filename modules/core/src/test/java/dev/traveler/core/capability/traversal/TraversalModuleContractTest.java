package dev.traveler.core.capability.traversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import org.junit.jupiter.api.Test;

class TraversalModuleContractTest {
    @Test
    void noTraversalModuleContributesNothing() {
        NoTraversalModule module = new NoTraversalModule("traversal.none");

        assertEquals(PathfinderModuleType.TRAVERSAL, module.descriptor().type());
        assertTrue(module.connectionContributors().isEmpty());
        assertTrue(module.routeContributors().isEmpty());
        assertTrue(module.executionContributors().isEmpty());
        assertTrue(module.recoveryContributors().isEmpty());
        assertTrue(module.debugContributors().isEmpty());
    }

    @Test
    void noTraversalModuleDescriptorIsDisabledAndLastPriority() {
        NoTraversalModule module = new NoTraversalModule("traversal.none");

        assertEquals("traversal.none", module.descriptor().id());
        assertEquals(Integer.MAX_VALUE, module.descriptor().priority());
        assertFalse(module.descriptor().enabled());
    }

    @Test
    void noTraversalModuleContributorListsAreImmutable() {
        NoTraversalModule module = new NoTraversalModule("traversal.none");

        assertThrows(UnsupportedOperationException.class, () -> module.connectionContributors().add(null));
        assertThrows(UnsupportedOperationException.class, () -> module.routeContributors().add(null));
        assertThrows(UnsupportedOperationException.class, () -> module.executionContributors().add(null));
        assertThrows(UnsupportedOperationException.class, () -> module.recoveryContributors().add(null));
        assertThrows(UnsupportedOperationException.class, () -> module.debugContributors().add(null));
    }

    @Test
    void noTraversalModuleRejectsInvalidIdThroughDescriptor() {
        assertThrows(NullPointerException.class, () -> new NoTraversalModule(null));
        assertThrows(IllegalArgumentException.class, () -> new NoTraversalModule(" "));
    }
}
