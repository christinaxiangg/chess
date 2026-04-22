package search;

import board.Evaluator;
import board.BitBoard;
import move.Move;

/**
 * Test class demonstrating the SearchEngine functionality.
 */
public class SearchEngineTest {
    
    public static void main(String[] args) {
        System.out.println("=== Chess Search Engine Tests ===\n");
        
        testBasicSearch();
        testTacticalPositions();
        testMateInN();
        testSearchStatistics();
    }
    
    /**
     * Test basic search from starting position.
     */
    private static void testBasicSearch() {
        System.out.println("1. Basic Search Test - Starting Position");
        System.out.println("-----------------------------------------");
        
        BitBoard board = BitBoard.startingPosition();
        board.print();
        
        SearchEngine engine = new SearchEngine(64); // 64 MB transposition table
        
        System.out.println("Searching to depth 5...\n");
        SearchEngine.SearchResult result = engine.search(board, 5, 10000); // 10 second limit
        
        System.out.println("\n=== Search Complete ===");
        System.out.println("Best move: " + (result.bestMove != null ? result.bestMove.toUCI() : "none"));
        System.out.println("Score: " + result.score + " centipawns");
        System.out.println("Nodes searched: " + result.nodesSearched);
        System.out.println("Q-nodes searched: " + result.qNodesSearched);
        System.out.println("Total nodes: " + (result.nodesSearched + result.qNodesSearched));
        
        SearchEngine.SearchStatistics stats = engine.getStatistics();
        System.out.println("\n" + stats);
        System.out.println();
    }
    
    /**
     * Test tactical positions.
     */
    private static void testTacticalPositions() {
        System.out.println("2. Tactical Position Tests");
        System.out.println("---------------------------");
        
        // Test position 1: Simple queen capture
        System.out.println("\nPosition 1: Queen hanging");
        BitBoard board1 = BitBoard.fromFEN("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        board1.print();
        
        SearchEngine engine = new SearchEngine(64);
        System.out.println("Searching to depth 4...\n");
        SearchEngine.SearchResult result1 = engine.search(board1, 4, 5000);
        
        System.out.println("\nBest move: " + (result1.bestMove != null ? result1.bestMove.toUCI() : "none"));
        System.out.println("Expected: A move capturing the queen on h4");
        System.out.println("Score: " + result1.score + " cp");
        
        // Test position 2: Fork opportunity
        System.out.println("\n\nPosition 2: Knight fork opportunity");
        BitBoard board2 = BitBoard.fromFEN("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        board2.print();
        
        engine.clearTranspositionTable();
        System.out.println("Searching to depth 5...\n");
        SearchEngine.SearchResult result2 = engine.search(board2, 5, 5000);
        
        System.out.println("\nBest move: " + (result2.bestMove != null ? result2.bestMove.toUCI() : "none"));
        System.out.println("Score: " + result2.score + " cp");
        System.out.println();
    }
    
    /**
     * Test mate-in-N positions.
     */
    private static void testMateInN() {
        System.out.println("3. Mate-in-N Tests");
        System.out.println("------------------");
        
        // Mate in 1
        System.out.println("\nMate in 1:");
        BitBoard board1 = BitBoard.fromFEN("r5rk/5p1p/5R2/4B3/8/8/7P/7K w - - 0 1");
        board1.print();
        
        SearchEngine engine = new SearchEngine(64);
        System.out.println("Searching to depth 3...\n");
        SearchEngine.SearchResult result1 = engine.search(board1, 3, 5000);
        
        System.out.println("\nBest move: " + (result1.bestMove != null ? result1.bestMove.toUCI() : "none"));
        System.out.println("Expected: Rf8# (back rank mate)");
        System.out.println("Score: " + result1.score + " cp");
        System.out.println("Is mate score: " + Evaluator.isMateScore(result1.score));
        
        // Mate in 2 (more complex)
        System.out.println("\n\nMate in 2:");
        BitBoard board2 = BitBoard.fromFEN("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K2R b KQkq - 0 4");
        board2.print();
        
        engine.clearTranspositionTable();
        System.out.println("Searching to depth 5...\n");
        SearchEngine.SearchResult result2 = engine.search(board2, 5, 5000);
        
        System.out.println("\nBest move: " + (result2.bestMove != null ? result2.bestMove.toUCI() : "none"));
        System.out.println("Score: " + result2.score + " cp");
        System.out.println("Is mate score: " + Evaluator.isMateScore(result2.score));
        System.out.println();
    }
    
    /**
     * Test and display search statistics.
     */
    private static void testSearchStatistics() {
        System.out.println("4. Search Statistics Test");
        System.out.println("-------------------------");
        
        BitBoard board = BitBoard.fromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        board.print();
        
        SearchEngine engine = new SearchEngine(128); // Larger TT for this test
        
        System.out.println("Searching to depth 6...\n");
        long startTime = System.currentTimeMillis();
        SearchEngine.SearchResult result = engine.search(board, 6, 15000);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("\n=== Search Complete ===");
        System.out.println("Time taken: " + elapsed + " ms");
        System.out.println("Best move: " + (result.bestMove != null ? result.bestMove.toUCI() : "none"));
        System.out.println("Score: " + result.score + " centipawns");
        System.out.println();
        
        SearchEngine.SearchStatistics stats = engine.getStatistics();
        System.out.println("=== Detailed Statistics ===");
        System.out.println(stats);
        
        long totalNodes = result.nodesSearched + result.qNodesSearched;
        long nps = (totalNodes * 1000) / Math.max(elapsed, 1);
        
        System.out.println("\nPerformance:");
        System.out.println("  Nodes per second: " + String.format("%,d", nps));
        System.out.println("  TT hit rate: " + String.format("%.2f%%", 
            (stats.ttHits * 100.0) / Math.max(result.nodesSearched, 1)));
        System.out.println("  Beta cutoff rate: " + String.format("%.2f%%", 
            (stats.betaCutoffs * 100.0) / Math.max(result.nodesSearched, 1)));
        System.out.println("  Null move cutoff rate: " + String.format("%.2f%%", 
            (stats.nullMoveCutoffs * 100.0) / Math.max(result.nodesSearched, 1)));
        System.out.println();
        
        // Test iterative deepening progression
        System.out.println("\n=== Iterative Deepening Test ===");
        engine.clearTranspositionTable();
        
        for (int depth = 1; depth <= 6; depth++) {
            engine.clearTranspositionTable();
            startTime = System.currentTimeMillis();
            result = engine.search(board, depth, 5000);
            elapsed = System.currentTimeMillis() - startTime;
            
            totalNodes = result.nodesSearched + result.qNodesSearched;
            nps = (totalNodes * 1000) / Math.max(elapsed, 1);
            
            System.out.printf("Depth %d: %s (%.2f sec, %,d nodes, %,d nps)\n",
                            depth, 
                            result.bestMove != null ? result.bestMove.toUCI() : "none",
                            elapsed / 1000.0,
                            totalNodes,
                            nps);
        }
        System.out.println();
    }
    
    /**
     * Helper method to display move evaluation.
     */
    private static void analyzeMove(BitBoard board, Move move, SearchEngine engine) {
        System.out.println("\nAnalyzing move: " + move.toUCI());
        
        BitBoard newBoard = board.copy();
        newBoard.makeMove(move);
        
        SearchEngine.SearchResult result = engine.search(newBoard, 5, 3000);
        
        System.out.println("Resulting score: " + (-result.score) + " cp");
        System.out.println("Best reply: " + (result.bestMove != null ? result.bestMove.toUCI() : "none"));
    }
}
