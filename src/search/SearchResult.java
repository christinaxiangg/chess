package search;

import move.Move;

public record SearchResult(Move bestMove, int score, long nodesSearched, long qNodesSearched) {
}
