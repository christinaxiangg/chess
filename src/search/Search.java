package search;

import board.BitBoard;

public interface Search {
     SearchResult search(BitBoard board, int depthLimit, long timeLimitMs);
}
