package dev.traveler.core.path;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.Heuristic;
import java.util.Objects;

public final class PathfinderRequest<N> {
    private final Graph<N> graph;
    private final N start;
    private final N goal;
    private final Heuristic<N> heuristic;
    private SearchState<N> searchState;

    public PathfinderRequest(Graph<N> graph, N start, N goal, Heuristic<N> heuristic) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.start = Objects.requireNonNull(start, "start");
        this.goal = Objects.requireNonNull(goal, "goal");
        this.heuristic = Objects.requireNonNull(heuristic, "heuristic");
    }

    public Graph<N> graph() {
        return graph;
    }

    public N start() {
        return start;
    }

    public N goal() {
        return goal;
    }

    public Heuristic<N> heuristic() {
        return heuristic;
    }

    public void reset() {
        searchState = null;
    }

    SearchState<N> searchState() {
        return searchState;
    }

    void searchState(SearchState<N> searchState) {
        this.searchState = searchState;
    }
}
