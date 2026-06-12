package dev.traveler.core.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MutableGraphPathTest {
    @Test
    void startsEmptyAndExposesReadOnlyNodes() {
        MutableGraphPath<String> path = new MutableGraphPath<>();

        assertTrue(path.isEmpty());
        assertEquals(0, path.nodeCount());
        assertEquals(0.0, path.cost());
        assertIterableEquals(List.of(), path);
        assertThrows(UnsupportedOperationException.class, () -> path.nodes().add("spawn"));
    }

    @Test
    void storesSingleNodePath() {
        MutableGraphPath<String> path = new MutableGraphPath<>();

        path.addNode("spawn");
        path.setCost(0.0);

        assertFalse(path.isEmpty());
        assertEquals(1, path.nodeCount());
        assertEquals("spawn", path.nodeAt(0));
        assertEquals(List.of("spawn"), path.nodes());
        assertIterableEquals(List.of("spawn"), path);
    }

    @Test
    void rejectsNegativeConnectionCost() {
        assertThrows(IllegalArgumentException.class, () -> new Connection<>("a", "b", -1.0));
    }
}
