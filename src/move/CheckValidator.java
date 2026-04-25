package move;

import board.BitBoard;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;
import java.util.List;
//This class should be removed in the future
/**
 * Validates whether a king is in check by determining if any enemy piece attacks the king's square.
 * Uses efficient bitboard-based attack generation for all piece types.
 */
public class CheckValidator {
    
    // Precomputed knight move patterns
    private static final long[] KNIGHT_ATTACKS = new long[64];
    
    // Precomputed king move patterns
    private static final long[] KING_ATTACKS = new long[64];
    
    // Precomputed pawn attack patterns (separate for white and black)
    private static final long[] WHITE_PAWN_ATTACKS = new long[64];
    private static final long[] BLACK_PAWN_ATTACKS = new long[64];
    
    static {
        initializeAttackTables();
    }
    
    /**
     * Checks if the king of the specified color is in check.
     * @param board The current board state
     * @param kingColor The color of the king to check
     * @return true if the king is in check, false otherwise
     */
    public static boolean isKingInCheck(BitBoard board, PieceColor kingColor) {
        // Find the king's position
        Piece kingPiece = Piece.getPiece(kingColor, PieceType.KING);
        List<Integer> kingSquares = board.getPieces(kingPiece);
        
        if (kingSquares.isEmpty()) {

            return false; // No king on board (shouldn't happen in valid position)
        }
        
        int kingSquare = kingSquares.get(0);
        return MoveGenerator.isSquareAttacked(board, kingSquare, kingColor.opposite());
    }
    
    /**
     * Checks if a specific square is attacked by any piece of the attacking color.
     * @param board The current board state
     * @param square The square to check (0-63)
     * @param attackingColor The color of the attacking pieces
     * @return true if the square is attacked, false otherwise
     */
    public static boolean isSquareAttacked(BitBoard board, int square, PieceColor attackingColor) {
        // Check for pawn attacks
        if (isAttackedByPawn(board, square, attackingColor)) {
            return true;
        }
        
        // Check for knight attacks
        if (isAttackedByKnight(board, square, attackingColor)) {
            return true;
        }
        
        // Check for bishop/queen diagonal attacks
        if (isAttackedByBishopOrQueen(board, square, attackingColor)) {
            return true;
        }
        
        // Check for rook/queen straight attacks
        if (isAttackedByRookOrQueen(board, square, attackingColor)) {
            return true;
        }
        
        // Check for king attacks
        if (isAttackedByKing(board, square, attackingColor)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Why I think wrong one make more sense than your correct one?8:04 AMClaude responded: That's a very natural intuition — here's why it feels backwards:That's a very natural intuition — here's why it feels backwards:
     * What your brain thinks: "I want white pawn attacks, so I use WHITE_PAWN_ATTACKS."
     * What the code actually needs: You're not asking "where does a white pawn on this square attack?" — you're asking "which squares would a white pawn have to be on, to be attacking THIS square?"
     * Think of it as shooting a ray backwards from the king to find the attacker.
     * A white pawn on d7 attacks e8 diagonally forward. So to ask "is e8 attacked by a white pawn?", you stand on e8 and look backwards the way a white pawn would move — which is downward, like a black pawn would move. If you find a white pawn there, it's attacking you.
     * A concrete example:
     * WHITE_PAWN_ATTACKS[e8] → looks UP from e8 → off the board → empty → 0
     * BLACK_PAWN_ATTACKS[e8] → looks DOWN from e8 → d7, f7 → finds white pawn on d7 ✓
     * So the rule is:
     * To find if a white pawn attacks a square → use black pawn pattern (look backwards/downward)
     * To find if a black pawn attacks a square → use white pawn pattern (look backwards/upward)
     * The same "backwards ray" logic is why MoveGenerator.isSquareAttacked works correctly for all sliding pieces too — you cast rays outward from the target square and check if you hit the right attacker, rather than iterating over all enemy pieces.
     */
    private static boolean isAttackedByPawn(BitBoard board, int square, PieceColor attackingColor) {
// WRONG:
//        long attackMask = attackingColor == PieceColor.WHITE ?
//                WHITE_PAWN_ATTACKS[square] : BLACK_PAWN_ATTACKS[square];

// CORRECT:
        long attackMask = attackingColor == PieceColor.WHITE ?
                BLACK_PAWN_ATTACKS[square] : WHITE_PAWN_ATTACKS[square];
        Piece attackingPawn = Piece.getPiece(attackingColor, PieceType.PAWN);
        long pawnBitboard = board.getBitboard(attackingPawn);
        
        return (attackMask & pawnBitboard) != 0;
    }
    
    /**
     * Checks if a square is attacked by an enemy knight.
     */
    private static boolean isAttackedByKnight(BitBoard board, int square, PieceColor attackingColor) {
        long attackMask = KNIGHT_ATTACKS[square];
        
        Piece attackingKnight = Piece.getPiece(attackingColor, PieceType.KNIGHT);
        long knightBitboard = board.getBitboard(attackingKnight);
        
        return (attackMask & knightBitboard) != 0;
    }
    
    /**
     * Checks if a square is attacked by an enemy bishop or queen diagonally.
     */
    private static boolean isAttackedByBishopOrQueen(BitBoard board, int square, PieceColor attackingColor) {
        long diagonalAttacks = getDiagonalAttacks(square, board.getAllPieces());
        
        Piece attackingBishop = Piece.getPiece(attackingColor, PieceType.BISHOP);
        Piece attackingQueen = Piece.getPiece(attackingColor, PieceType.QUEEN);
        
        long bishopBitboard = board.getBitboard(attackingBishop);
        long queenBitboard = board.getBitboard(attackingQueen);
        
        return (diagonalAttacks & (bishopBitboard | queenBitboard)) != 0;
    }
    
    /**
     * Checks if a square is attacked by an enemy rook or queen along ranks/files.
     */
    private static boolean isAttackedByRookOrQueen(BitBoard board, int square, PieceColor attackingColor) {
        long straightAttacks = getStraightAttacks(square, board.getAllPieces());
        
        Piece attackingRook = Piece.getPiece(attackingColor, PieceType.ROOK);
        Piece attackingQueen = Piece.getPiece(attackingColor, PieceType.QUEEN);
        
        long rookBitboard = board.getBitboard(attackingRook);
        long queenBitboard = board.getBitboard(attackingQueen);
        
        return (straightAttacks & (rookBitboard | queenBitboard)) != 0;
    }
    
    /**
     * Checks if a square is attacked by an enemy king.
     */
    private static boolean isAttackedByKing(BitBoard board, int square, PieceColor attackingColor) {
        long attackMask = KING_ATTACKS[square];
        
        Piece attackingKing = Piece.getPiece(attackingColor, PieceType.KING);
        long kingBitboard = board.getBitboard(attackingKing);
        
        return (attackMask & kingBitboard) != 0;
    }
    
    /**
     * Generates diagonal attacks (for bishops and queens) from a given square.
     */
    private static long getDiagonalAttacks(int square, long occupied) {
        return getNorthEastAttacks(square, occupied) |
               getNorthWestAttacks(square, occupied) |
               getSouthEastAttacks(square, occupied) |
               getSouthWestAttacks(square, occupied);
    }
    
    /**
     * Generates straight attacks (for rooks and queens) from a given square.
     */
    private static long getStraightAttacks(int square, long occupied) {
        return getNorthAttacks(square, occupied) |
               getSouthAttacks(square, occupied) |
               getEastAttacks(square, occupied) |
               getWestAttacks(square, occupied);
    }
    
    /**
     * Generates attacks in the north direction.
     */
    private static long getNorthAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int r = rank + 1; r < 8; r++) {
            int sq = r * 8 + file;
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the south direction.
     */
    private static long getSouthAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int r = rank - 1; r >= 0; r--) {
            int sq = r * 8 + file;
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the east direction.
     */
    private static long getEastAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int f = file + 1; f < 8; f++) {
            int sq = rank * 8 + f;
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the west direction.
     */
    private static long getWestAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int f = file - 1; f >= 0; f--) {
            int sq = rank * 8 + f;
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the north-east direction.
     */
    private static long getNorthEastAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int i = 1; file + i < 8 && rank + i < 8; i++) {
            int sq = (rank + i) * 8 + (file + i);
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the north-west direction.
     */
    private static long getNorthWestAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int i = 1; file - i >= 0 && rank + i < 8; i++) {
            int sq = (rank + i) * 8 + (file - i);
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the south-east direction.
     */
    private static long getSouthEastAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int i = 1; file + i < 8 && rank - i >= 0; i++) {
            int sq = (rank - i) * 8 + (file + i);
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates attacks in the south-west direction.
     */
    private static long getSouthWestAttacks(int square, long occupied) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int i = 1; file - i >= 0 && rank - i >= 0; i++) {
            int sq = (rank - i) * 8 + (file - i);
            long mask = 1L << sq;
            attacks |= mask;
            if ((occupied & mask) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Initializes precomputed attack tables for knights, kings, and pawns.
     */
    private static void initializeAttackTables() {
        for (int square = 0; square < 64; square++) {
            KNIGHT_ATTACKS[square] = generateKnightAttacks(square);
            KING_ATTACKS[square] = generateKingAttacks(square);
            WHITE_PAWN_ATTACKS[square] = generateWhitePawnAttacks(square);
            BLACK_PAWN_ATTACKS[square] = generateBlackPawnAttacks(square);
        }
    }
    
    /**
     * Generates knight attack pattern for a given square.
     */
    private static long generateKnightAttacks(int square) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        int[][] offsets = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        
        for (int[] offset : offsets) {
            int newRank = rank + offset[0];
            int newFile = file + offset[1];
            
            if (newRank >= 0 && newRank < 8 && newFile >= 0 && newFile < 8) {
                int targetSquare = newRank * 8 + newFile;
                attacks |= (1L << targetSquare);
            }
        }
        
        return attacks;
    }
    
    /**
     * Generates king attack pattern for a given square.
     */
    private static long generateKingAttacks(int square) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        for (int dr = -1; dr <= 1; dr++) {
            for (int df = -1; df <= 1; df++) {
                if (dr == 0 && df == 0) continue;
                
                int newRank = rank + dr;
                int newFile = file + df;
                
                if (newRank >= 0 && newRank < 8 && newFile >= 0 && newFile < 8) {
                    int targetSquare = newRank * 8 + newFile;
                    attacks |= (1L << targetSquare);
                }
            }
        }
        
        return attacks;
    }
    
    /**
     * Generates white pawn attack pattern for a given square.
     */
    private static long generateWhitePawnAttacks(int square) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        // White pawns attack diagonally upward
        if (rank < 7) {
            if (file > 0) {
                attacks |= (1L << ((rank + 1) * 8 + (file - 1)));
            }
            if (file < 7) {
                attacks |= (1L << ((rank + 1) * 8 + (file + 1)));
            }
        }
        
        return attacks;
    }
    
    /**
     * Generates black pawn attack pattern for a given square.
     */
    private static long generateBlackPawnAttacks(int square) {
        long attacks = 0L;
        int file = square & 7;
        int rank = square >>> 3;
        
        // Black pawns attack diagonally downward
        if (rank > 0) {
            if (file > 0) {
                attacks |= (1L << ((rank - 1) * 8 + (file - 1)));
            }
            if (file < 7) {
                attacks |= (1L << ((rank - 1) * 8 + (file + 1)));
            }
        }
        
        return attacks;
    }
}
