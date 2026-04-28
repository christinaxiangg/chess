package board;
import piece.Piece;
import piece.PieceType;
import piece.PieceColor;

/**
 * Position evaluator for chess.
 * Evaluates positions based on material, piece-square tables, and positional factors.
 */
public class Evaluator2 {
    
    // Checkmate and stalemate scores
    public static final int CHECKMATE_SCORE = 30000;
    public static final int STALEMATE_SCORE = 0;
    
    // piece.Piece-square tables for positional evaluation
    // Scores are from white's perspective (positive is good for white)
    
    // Pawn piece-square table
    private static final int[] PAWN_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    
    // Knight piece-square table
    private static final int[] KNIGHT_TABLE = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    
    // Bishop piece-square table
    private static final int[] BISHOP_TABLE = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    
    // Rook piece-square table
    private static final int[] ROOK_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] ROOK_END_GAME_TABLE = {
            0,  5, 10, 15, 15, 10,  5,  0,
            5, 10, 15, 20, 20, 15, 10,  5,
            0,  5, 10, 15, 15, 10,  5,  0,
            0,  5, 10, 15, 15, 10,  5,  0,
            0,  5, 10, 15, 15, 10,  5,  0,
            0,  5, 10, 15, 15, 10,  5,  0,
            5, 10, 15, 20, 20, 15, 10,  5,
            0,  5, 10, 15, 15, 10,  5,  0
    };

    // Queen piece-square table
    private static final int[] QUEEN_TABLE = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    
    // King middle-game piece-square table
    private static final int[] KING_MIDDLE_GAME_TABLE = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };
    
    // King end-game piece-square table
    private static final int[] KING_END_GAME_TABLE = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };
    
    /**
     * Evaluates a chess position.
     * Returns a score in centipawns from white's perspective.
     * Positive scores favor white, negative scores favor black.
     */
    public static int evaluate(BitBoard board) {
        int score = 0;
        
        // Material and positional evaluation
        score += evaluateMaterial(board);
        score += evaluatePosition(board);
        
        // Return score from the perspective of the side to move
        return board.getSideToMove() == PieceColor.WHITE ? score : -score;
    }
    
    /**
     * Evaluates material balance.
     */
    private static int evaluateMaterial(BitBoard board) {
        int score = 0;
        
        for (Piece piece : Piece.values()) {
            int count = board.getPieces(piece).size();
            int value = piece.getValue();
            
            if (piece.isWhite()) {
                score += count * value;
            } else {
                score -= count * value;
            }
        }
        
        return score;
    }
    
    /**
     * Evaluates positional factors using piece-square tables.
     */
    private static int evaluatePosition(BitBoard board) {
        int score = 0;
        boolean isEndGame = isEndGame(board);
        
        for (int square = 0; square < 64; square++) {
            Piece piece = board.getPiece(square);
            if (piece == null) continue;
            
            int pieceSquareValue = getPieceSquareValue(piece, square, isEndGame);
            
            if (piece.isWhite()) {
                score += pieceSquareValue;
            } else {
                score -= pieceSquareValue;
            }
        }
        
        return score;
    }
    
    /**
     * Gets the piece-square table value for a piece at a given square.
     */
    private static int getPieceSquareValue(Piece piece, int square, boolean isEndGame) {
        // For white pieces, flip the square vertically because it from white visual board perspective,
        // the array is actually indexed from black's perspective (0 is a1, 63 is h8)
        int tableSquare = piece.isWhite() ? (square ^ 56) : square;

        return switch (piece.getType()) {
            case PAWN -> PAWN_TABLE[tableSquare];
            case KNIGHT -> KNIGHT_TABLE[tableSquare];
            case BISHOP -> BISHOP_TABLE[tableSquare];
            case ROOK -> ROOK_TABLE[tableSquare];
            case QUEEN -> QUEEN_TABLE[tableSquare];
            case KING -> isEndGame ? KING_END_GAME_TABLE[tableSquare] : KING_MIDDLE_GAME_TABLE[tableSquare];
            default -> 0;
        };
    }
    
    /**
     * Determines if the position is in the endgame phase.
     */
    private static boolean isEndGame(BitBoard board) {
        // Simple endgame detection: few pieces remaining
        int pieceCount = 0;
        for (int square = 0; square < 64; square++) {
            Piece piece = board.getPiece(square);
            if (piece != null && piece.getType() != PieceType.PAWN && piece.getType() != PieceType.KING) {
                pieceCount++;
            }
        }
        return pieceCount <= 6;
    }
    
    /**
     * Returns a mate score adjusted for the distance to mate.
     * Closer mates get higher scores.
     */
    public static int mateScore(int plyFromRoot) {
        return CHECKMATE_SCORE - plyFromRoot;
    }
    
    /**
     * Returns a mated score adjusted for the distance to mate.
     */
    public static int matedScore(int plyFromRoot) {
        return -CHECKMATE_SCORE + plyFromRoot;
    }
    
    /**
     * Checks if a score represents a mate.
     */
    public static boolean isMateScore(int score) {
        return Math.abs(score) >= CHECKMATE_SCORE - 1000;
    }
}
