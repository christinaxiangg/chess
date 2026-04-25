package move;
import java.util.List;

import board.BitBoard;
import piece.Piece;
import piece.PieceType;
/**
 * move.Move ordering for alpha-beta search optimization.
 * Orders moves to maximize cutoffs and improve search efficiency.
 */
public class MoveOrdering {
    
    // move.Move scoring values for ordering
    private static final int TT_MOVE_SCORE = 10000000;
    private static final int WINNING_CAPTURE_SCORE = 8000000;
    private static final int QUEEN_PROMOTION_SCORE = 7000000;
    private static final int KILLER_MOVE_SCORE = 5000000;
    private static final int COUNTER_MOVE_SCORE = 4000000;
    private static final int EQUAL_CAPTURE_SCORE = 1000000;
    private static final int LOSING_CAPTURE_SCORE = 2000000;  // Above history (max 1M), below equal captures


//    PAWN(100),
//    KNIGHT(320),
//    BISHOP(330),
//    ROOK(500),
//    QUEEN(900),
//    KING(20000);
    // MVV-LVA (Most Valuable Victim - Least Valuable Attacker) values
    private static final int[] VICTIM_VALUES = {100, 320, 330, 500, 900, 20000};
    private static final int[] ATTACKER_VALUES = {10, 32, 33, 50, 90, 200};
    
    /**
     * Orders moves for optimal alpha-beta search.
     * 
     * Priority order:
     * 1. TT (hash) move
     * 2. Winning captures (MVV-LVA)
     * 3. Queen promotions
     * 4. Killer moves
     * 5. Counter moves
     * 6. Equal/losing captures
     * 7. Quiet moves (history heuristic)
     */
    public static void orderMoves(BitBoard board, List<Move> moves, Move ttMove, 
                                 Move[] killerMoves, Move counterMove, int[][] historyTable) {
        // Score each move
        int[] scores = new int[moves.size()];
        
        for (int i = 0; i < moves.size(); i++) {
            scores[i] = scoreMove(board, moves.get(i), ttMove, killerMoves, counterMove, historyTable);
        }
        
        // Use insertion sort for small arrays, quicksort for larger ones
        if (moves.size() <= 16) {
            insertionSort(moves, scores);
        } else {
            quickSort(moves, scores, 0, moves.size() - 1);
        }
    }
    
    /**
     * Scores a move for ordering.
     */
    private static int scoreMove(BitBoard board, Move move, Move ttMove, 
                                 Move[] killerMoves, Move counterMove, int[][] historyTable) {
        // TT move gets highest priority
        if (move.equals(ttMove)) {
            return TT_MOVE_SCORE;
        }
        
        int from = move.getFrom();
        int to = move.getTo();
        Piece movingPiece = board.getPiece(from);
        
        // Captures
        if (move.isCapture()) {
            Piece capturedPiece = move.isEnPassant() ? 
                Piece.getPiece(board.getSideToMove().opposite(), PieceType.PAWN) :
                board.getPiece(to);
            
            if (capturedPiece != null) {
                int captureScore = getMVVLVAScore(movingPiece, capturedPiece);
                
                // Use SEE (Static Exchange Evaluation) approximation
                // If capturing piece value <= captured piece value, it's likely winning
                if (movingPiece.getValue() <= capturedPiece.getValue()) {
                    return WINNING_CAPTURE_SCORE + captureScore;
                } else {
                    // Could be a losing capture, but still prioritize over quiet moves
                    return LOSING_CAPTURE_SCORE + captureScore;
                }
            }
        }
        
        // Promotions
        if (move.isPromotion()) {
            PieceType promoType = move.getPromotionPieceType();
            if (promoType == PieceType.QUEEN) {
                return QUEEN_PROMOTION_SCORE;
            } else {
                return QUEEN_PROMOTION_SCORE - (PieceType.QUEEN.getValue() - promoType.getValue());
            }
        }
        
        // Killer moves
        if (killerMoves != null) {
            for (Move killer : killerMoves) {
                if (move.equals(killer)) {
                    return KILLER_MOVE_SCORE;
                }
            }
        }
        
        // Counter move
        if (move.equals(counterMove)) {
            return COUNTER_MOVE_SCORE;
        }
        
        // History heuristic for quiet moves
        if (historyTable != null && movingPiece != null) {
            return historyTable[from][to];
        }
        
        return 0;
    }
    
    /**
     * Calculates MVV-LVA score for a capture.
     */
    private static int getMVVLVAScore(Piece attacker, Piece victim) {
        int victimValue = VICTIM_VALUES[victim.getType().ordinal()];
        int attackerValue = ATTACKER_VALUES[attacker.getType().ordinal()];
        return victimValue * 100 - attackerValue;
    }
    
    /**
     * Quick sort implementation for move ordering.
     */
    private static void quickSort(List<Move> moves, int[] scores, int low, int high) {
        if (low < high) {
            int pi = partition(moves, scores, low, high);
            quickSort(moves, scores, low, pi - 1);
            quickSort(moves, scores, pi + 1, high);
        }
    }
    
    /**
     * Insertion sort for small arrays (more efficient than quicksort).
     */
    private static void insertionSort(List<Move> moves, int[] scores) {
        for (int i = 1; i < moves.size(); i++) {
            int j = i;
            while (j > 0 && scores[j] > scores[j - 1]) {
                // Swap moves
                Move tempMove = moves.get(j);
                moves.set(j, moves.get(j - 1));
                moves.set(j - 1, tempMove);
                // Swap scores
                int tempScore = scores[j];
                scores[j] = scores[j - 1];
                scores[j - 1] = tempScore;
                j--;
            }
        }
    }

    /**
     * Partition function for quick sort.
     */
    private static int partition(List<Move> moves, int[] scores, int low, int high) {
        int pivot = scores[high];
        int i = low - 1;
        
        for (int j = low; j < high; j++) {
            if (scores[j] > pivot) { // Descending order
                i++;
                // Swap moves
                Move tempMove = moves.get(i);
                moves.set(i, moves.get(j));
                moves.set(j, tempMove);
                // Swap scores
                int tempScore = scores[i];
                scores[i] = scores[j];
                scores[j] = tempScore;
            }
        }
        
        // Swap pivot
        Move tempMove = moves.get(i + 1);
        moves.set(i + 1, moves.get(high));
        moves.set(high, tempMove);
        int tempScore = scores[i + 1];
        scores[i + 1] = scores[high];
        scores[high] = tempScore;
        
        return i + 1;
    }
    
    /**
     * Simple SEE (Static Exchange Evaluation) for captures.
     * Returns true if the capture is likely to be winning or equal.
     */
    public static boolean isGoodCapture(BitBoard board, Move move) {
        if (!move.isCapture()) {
            return false;
        }
        
        Piece attacker = board.getPiece(move.getFrom());
        Piece victim = move.isEnPassant() ?
            Piece.getPiece(board.getSideToMove().opposite(), PieceType.PAWN) :
            board.getPiece(move.getTo());
        
        if (attacker == null || victim == null) {
            return false;
        }
        
        // Simple heuristic: good if attacker value <= victim value
        return attacker.getValue() <= victim.getValue();
    }
}
