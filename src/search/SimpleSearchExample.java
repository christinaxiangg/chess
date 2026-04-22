package search;

import board.BitBoard;
import move.Move;
import move.MoveGenerator;

import java.util.List;
import piece.Piece;
import piece.PieceColor;

/**
 * Example integration of move generation with a simple minimax search.
 * This shows how to use the board.BitBoard and move.MoveGenerator with your own search.
 */
public class SimpleSearchExample {
    
    private static final int INFINITY = 1000000;
    private static int nodesSearched = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Simple Chess Search Example ===\n");
        
        // Position with a tactical opportunity
        BitBoard position = BitBoard.fromFEN("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        position.print();
        
        System.out.println("Searching to depth 4...\n");
        nodesSearched = 0;
        long startTime = System.currentTimeMillis();
        
        MoveScore best = findBestMove(position, 4);
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("\nSearch Results:");
        System.out.println("===============");
        System.out.println("Best move: " + best.move.toUCI());
        System.out.println("Score: " + best.score + " centipawns");
        System.out.println("Nodes searched: " + nodesSearched);
        System.out.println("Time: " + elapsed + "ms");
        System.out.println("Nodes per second: " + (nodesSearched * 1000 / Math.max(elapsed, 1)));
        
        // Show the move in context
        System.out.println("\nPosition after best move:");
        BitBoard after = position.copy();
        after.makeMove(best.move);
        after.print();
    }
    
    /**
     * Finds the best move using minimax search.
     */
    public static MoveScore findBestMove(BitBoard board, int depth) {
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        
        if (moves.isEmpty()) {
            return new MoveScore(Move.NULL_MOVE, 0);
        }
        
        Move bestMove = moves.get(0);
        int bestScore = -INFINITY;
        
        for (Move move : moves) {
            BitBoard copy = board.copy();
            copy.makeMove(move);
            
            int score = -minimax(copy, depth - 1, -INFINITY, INFINITY);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return new MoveScore(bestMove, bestScore);
    }
    
    /**
     * Minimax search with alpha-beta pruning.
     */
    private static int minimax(BitBoard board, int depth, int alpha, int beta) {
        nodesSearched++;
        
        // Terminal conditions
        if (depth == 0) {
            return evaluate(board);
        }
        
        // Check for checkmate or stalemate
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        if (moves.isEmpty()) {
            if (MoveGenerator.isInCheck(board)) {
                return -INFINITY + (100 - depth); // Checkmate (prefer faster mates)
            } else {
                return 0; // Stalemate
            }
        }
        
        int maxScore = -INFINITY;
        
        for (Move move : moves) {
            BitBoard copy = board.copy();
            copy.makeMove(move);
            
            int score = -minimax(copy, depth - 1, -beta, -alpha);
            
            maxScore = Math.max(maxScore, score);
            alpha = Math.max(alpha, score);
            
            // Beta cutoff
            if (alpha >= beta) {
                break;
            }
        }
        
        return maxScore;
    }
    
    /**
     * Simple material-based evaluation.
     * In a real engine, you'd add positional evaluation, king safety, etc.
     */
    private static int evaluate(BitBoard board) {
        int score = 0;
        PieceColor perspective = board.getSideToMove();
        
        // Material evaluation
        for (Piece piece : Piece.values()) {
            int count = board.getPieces(piece).size();
            int value = piece.getValue();
            
            if (piece.getColor() == perspective) {
                score += count * value;
            } else {
                score -= count * value;
            }
        }
        
        // Simple piece activity bonus (number of legal moves)
        score += MoveGenerator.generateLegalMoves(board).size() * 5;
        
        return score;
    }
    
    /**
     * Helper class to store a move with its score.
     */
    private static class MoveScore {
        Move move;
        int score;
        
        MoveScore(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}
