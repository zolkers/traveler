package dev.traveler.core.path;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.KeyedGraph;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

interface SearchRecords<N> {
    static <N> SearchRecords<N> create(Graph<N> graph) {
        Objects.requireNonNull(graph, "graph");
        if (graph instanceof KeyedGraph<?> keyedGraph) {
            return keyed(keyedGraph);
        }
        return new HashSearchRecords<>();
    }

    @SuppressWarnings("unchecked")
    private static <N> SearchRecords<N> keyed(KeyedGraph<?> graph) {
        return new LongSearchRecords<>((KeyedGraph<N>) graph);
    }

    boolean isSame(N first, N second);

    double costOf(N node);

    Connection<N> previousConnection(N node);

    boolean hasBetterCost(N node, double cost);

    void put(N node, double cost, Connection<N> previousConnection);

    final class HashSearchRecords<N> implements SearchRecords<N> {
        private final Map<N, Double> costs = new HashMap<>();
        private final Map<N, Connection<N>> previousConnections = new HashMap<>();

        @Override
        public boolean isSame(N first, N second) {
            return Objects.equals(first, second);
        }

        @Override
        public double costOf(N node) {
            return costs.getOrDefault(node, Double.POSITIVE_INFINITY);
        }

        @Override
        public Connection<N> previousConnection(N node) {
            return previousConnections.get(node);
        }

        @Override
        public boolean hasBetterCost(N node, double cost) {
            Double knownCost = costs.get(node);
            return knownCost != null && knownCost <= cost;
        }

        @Override
        public void put(N node, double cost, Connection<N> previousConnection) {
            costs.put(node, cost);
            previousConnections.put(node, previousConnection);
        }
    }

    final class LongSearchRecords<N> implements SearchRecords<N> {
        private static final int INITIAL_CAPACITY = 256;
        private static final double LOAD_FACTOR = 0.6;

        private final KeyedGraph<N> graph;
        private long[] keys = new long[INITIAL_CAPACITY];
        private double[] costs = new double[INITIAL_CAPACITY];
        private Connection<N>[] previousConnections = connectionArray(INITIAL_CAPACITY);
        private boolean[] used = new boolean[INITIAL_CAPACITY];
        private int size;

        LongSearchRecords(KeyedGraph<N> graph) {
            this.graph = Objects.requireNonNull(graph, "graph");
            Arrays.fill(costs, Double.POSITIVE_INFINITY);
        }

        @Override
        public boolean isSame(N first, N second) {
            return keyOf(first) == keyOf(second);
        }

        @Override
        public double costOf(N node) {
            int slot = slotOf(keyOf(node));
            if (!used[slot]) {
                return Double.POSITIVE_INFINITY;
            }
            return costs[slot];
        }

        @Override
        public Connection<N> previousConnection(N node) {
            int slot = slotOf(keyOf(node));
            if (!used[slot]) {
                return null;
            }
            return previousConnections[slot];
        }

        @Override
        public boolean hasBetterCost(N node, double cost) {
            int slot = slotOf(keyOf(node));
            return used[slot] && costs[slot] <= cost;
        }

        @Override
        public void put(N node, double cost, Connection<N> previousConnection) {
            ensureCapacity();
            putByKey(keyOf(node), cost, previousConnection);
        }

        private long keyOf(N node) {
            return graph.keyOf(Objects.requireNonNull(node, "node"));
        }

        private void ensureCapacity() {
            if ((size + 1) <= keys.length * LOAD_FACTOR) {
                return;
            }
            resize();
        }

        private void resize() {
            long[] oldKeys = keys;
            double[] oldCosts = costs;
            Connection<N>[] oldPreviousConnections = previousConnections;
            boolean[] oldUsed = used;
            keys = new long[oldKeys.length * 2];
            costs = new double[keys.length];
            previousConnections = connectionArray(keys.length);
            used = new boolean[keys.length];
            size = 0;
            Arrays.fill(costs, Double.POSITIVE_INFINITY);
            reinsert(oldKeys, oldCosts, oldPreviousConnections, oldUsed);
        }

        private void reinsert(
                long[] oldKeys,
                double[] oldCosts,
                Connection<N>[] oldPreviousConnections,
                boolean[] oldUsed) {
            for (int index = 0; index < oldKeys.length; index++) {
                if (oldUsed[index]) {
                    putByKey(oldKeys[index], oldCosts[index], oldPreviousConnections[index]);
                }
            }
        }

        private void putByKey(long key, double cost, Connection<N> previousConnection) {
            int slot = slotOf(key);
            if (!used[slot]) {
                used[slot] = true;
                keys[slot] = key;
                size++;
            }
            costs[slot] = cost;
            previousConnections[slot] = previousConnection;
        }

        private int slotOf(long key) {
            int slot = mix(key) & (keys.length - 1);
            while (used[slot] && keys[slot] != key) {
                slot = (slot + 1) & (keys.length - 1);
            }
            return slot;
        }

        private static int mix(long value) {
            long mixed = value ^ (value >>> 33);
            mixed *= 0xff51afd7ed558ccdL;
            mixed ^= mixed >>> 33;
            mixed *= 0xc4ceb9fe1a85ec53L;
            mixed ^= mixed >>> 33;
            return (int) mixed;
        }

        @SuppressWarnings("unchecked")
        private static <N> Connection<N>[] connectionArray(int capacity) {
            return (Connection<N>[]) new Connection<?>[capacity];
        }
    }

}
