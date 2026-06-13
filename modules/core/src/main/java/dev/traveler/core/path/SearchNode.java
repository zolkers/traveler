package dev.traveler.core.path;

record SearchNode<N>(N node, double cost, double estimatedTotalCost, long sequence) {
    double heuristicCost() {
        return estimatedTotalCost - cost;
    }
}
