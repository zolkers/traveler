package dev.traveler.core.path;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.Heuristic;
import java.util.Comparator;
import java.util.PriorityQueue;

final class SearchState<N> {
    private final PathfinderRequest<N> request;
    private final PriorityQueue<SearchNode<N>> openNodes = new PriorityQueue<>(Comparator
            .<SearchNode<N>>comparingDouble(SearchNode::estimatedTotalCost)
            .thenComparingDouble(SearchNode::heuristicCost)
            .thenComparingLong(SearchNode::sequence));
    private final SearchRecords<N> records;
    private long nextSequence;

    private SearchState(PathfinderRequest<N> request) {
        this.request = request;
        this.records = SearchRecords.create(request.graph());
    }

    static <N> SearchState<N> create(PathfinderRequest<N> request) {
        SearchState<N> state = new SearchState<>(request);
        state.addStartNode();
        return state;
    }

    Graph<N> graph() {
        return request.graph();
    }

    N start() {
        return request.start();
    }

    N goal() {
        return request.goal();
    }

    Heuristic<N> heuristic() {
        return request.heuristic();
    }

    boolean hasOpenNodes() {
        return !openNodes.isEmpty();
    }

    SearchNode<N> pollOpenNode() {
        return openNodes.poll();
    }

    boolean isGoal(N node) {
        return records.isSame(node, goal());
    }

    boolean isStart(N node) {
        return records.isSame(node, start());
    }

    boolean isStale(SearchNode<N> node) {
        return Double.compare(node.cost(), costOf(node.node())) != 0;
    }

    double costOf(N node) {
        return records.costOf(node);
    }

    Connection<N> previousConnection(N node) {
        return records.previousConnection(node);
    }

    boolean hasBetterCost(N node, double cost) {
        return records.hasBetterCost(node, cost);
    }

    void update(Connection<N> connection, double cost) {
        records.put(connection.to(), cost, connection);
        open(connection.to(), cost, cost + heuristic().estimate(connection.to(), goal()));
    }

    private void addStartNode() {
        records.put(start(), 0.0, null);
        open(start(), 0.0, heuristic().estimate(start(), goal()));
    }

    private void open(N node, double cost, double estimatedTotalCost) {
        openNodes.add(new SearchNode<>(node, cost, estimatedTotalCost, nextSequence));
        nextSequence++;
    }
}
