package dev.traveler.core.path;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.graph.MutableGraphPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AStarPathfinder<N> implements Pathfinder<N> {
    @Override
    public PathfinderResult<N> search(PathfinderRequest<N> request) {
        Objects.requireNonNull(request, "request");
        return complete(SearchState.create(request));
    }

    @Override
    public PathfinderResult<N> search(PathfinderRequest<N> request, long budgetNanos) {
        Objects.requireNonNull(request, "request");
        SearchState<N> state = reusableState(request);
        PathfinderResult<N> result = advance(state, SearchBudget.limited(budgetNanos));
        clearCompletedState(request, result);
        return result;
    }

    private PathfinderResult<N> complete(SearchState<N> state) {
        return advance(state, SearchBudget.unlimited());
    }

    private PathfinderResult<N> advance(SearchState<N> state, SearchBudget budget) {
        while (state.hasOpenNodes()) {
            SearchNode<N> current = state.pollOpenNode();
            if (state.isStale(current)) {
                continue;
            }
            if (state.isGoal(current.node())) {
                return PathfinderResult.found(buildPath(state, current.node()));
            }
            expand(state, current);
            if (budget.isSpent()) {
                return PathfinderResult.running();
            }
        }
        return PathfinderResult.notFound();
    }

    private void expand(SearchState<N> state, SearchNode<N> current) {
        for (Connection<N> connection : state.graph().outgoingConnections(current.node())) {
            relax(state, connection, current.cost());
        }
    }

    private void relax(SearchState<N> state, Connection<N> connection, double currentCost) {
        double cost = currentCost + connection.cost();
        if (state.hasBetterCost(connection.to(), cost)) {
            return;
        }
        state.update(connection, cost);
    }

    private SearchState<N> reusableState(PathfinderRequest<N> request) {
        SearchState<N> state = request.searchState();
        if (state != null) {
            return state;
        }
        SearchState<N> created = SearchState.create(request);
        request.searchState(created);
        return created;
    }

    private void clearCompletedState(PathfinderRequest<N> request, PathfinderResult<N> result) {
        if (result.status() == PathfinderStatus.RUNNING) {
            return;
        }
        request.reset();
    }

    private GraphPath<N> buildPath(SearchState<N> state, N goal) {
        MutableGraphPath<N> path = new MutableGraphPath<>();
        for (N node : traceNodes(state, goal)) {
            path.addNode(node);
        }
        path.setCost(state.costOf(goal));
        return path;
    }

    private List<N> traceNodes(SearchState<N> state, N goal) {
        List<N> nodes = new ArrayList<>();
        N current = goal;
        nodes.add(current);
        while (!state.isStart(current)) {
            Connection<N> connection = state.previousConnection(current);
            if (connection == null) {
                return List.of();
            }
            current = connection.from();
            nodes.add(current);
        }
        Collections.reverse(nodes);
        return nodes;
    }
}
