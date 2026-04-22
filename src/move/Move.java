package move;
import piece.PieceType;
/**
 * Represents a chess move with source square, destination square, and special flags.
 * Uses a compact integer representation for efficiency.
 */
public class Move {
    // move.Move encoding:
    // bits 0-5: from square (0-63)
    // bits 6-11: to square (0-63)
    // bits 12-15: flags (promotion, capture, castling, en passant)
    // bits 16-19: promoted piece type (if applicable)
    // bits 20-23: captured piece type (if applicable)
    
    private final int moveData;
    
    // move.Move flags
    public static final int QUIET_MOVE = 0;
    public static final int DOUBLE_PAWN_PUSH = 1;
    public static final int KING_CASTLE = 2;
    public static final int QUEEN_CASTLE = 3;
    public static final int CAPTURE = 4;
    public static final int EP_CAPTURE = 5;
    public static final int KNIGHT_PROMOTION = 8;
    public static final int BISHOP_PROMOTION = 9;
    public static final int ROOK_PROMOTION = 10;
    public static final int QUEEN_PROMOTION = 11;
    public static final int KNIGHT_PROMO_CAPTURE = 12;
    public static final int BISHOP_PROMO_CAPTURE = 13;
    public static final int ROOK_PROMO_CAPTURE = 14;
    public static final int QUEEN_PROMO_CAPTURE = 15;
    
    public static final Move NULL_MOVE = new Move(0, 0, QUIET_MOVE);
    
    /**
     * Creates a move from raw data.
     */
    private Move(int moveData) {
        this.moveData = moveData;
    }
    
    /**
     * Creates a move with source, destination, and flags.
     */
    public Move(int from, int to, int flags) {
        this.moveData = from | (to << 6) | (flags << 12);
    }
    
    /**
     * Creates a move with source, destination, flags, and captured piece.
     */
    public Move(int from, int to, int flags, PieceType captured) {
        this.moveData = from | (to << 6) | (flags << 12) | (captured.ordinal() << 20);
    }
    
    /**
     * Gets the source square (0-63).
     */
    public int getFrom() {
        return moveData & 0x3F;
    }
    
    /**
     * Gets the destination square (0-63).
     */
    public int getTo() {
        return (moveData >>> 6) & 0x3F;
    }
    
    /**
     * Gets the move flags.
     */
    public int getFlags() {
        return (moveData >>> 12) & 0xF;
    }
    
    /**
     * Gets the captured piece type (if applicable).
     */
    public PieceType getCapturedPieceType() {
        int ordinal = (moveData >>> 20) & 0xF;
        if (ordinal == 0 && !isCapture()) {
            return null;
        }
        return PieceType.values()[ordinal];
    }
    
    /**
     * Gets the promotion piece type (if applicable).
     */
    public PieceType getPromotionPieceType() {
        int flags = getFlags();
        if (flags >= KNIGHT_PROMOTION && flags <= QUEEN_PROMO_CAPTURE) {
            int promoIndex = flags & 0x3; // Extract last 2 bits
            return PieceType.values()[promoIndex + 1]; // +1 to skip PAWN
        }
        return null;
    }
    
    /**
     * Checks if this is a capture move.
     */
    public boolean isCapture() {
        int flags = getFlags();
        return flags == CAPTURE || flags == EP_CAPTURE || 
               (flags >= KNIGHT_PROMO_CAPTURE && flags <= QUEEN_PROMO_CAPTURE);
    }
    
    /**
     * Checks if this is a promotion move.
     */
    public boolean isPromotion() {
        int flags = getFlags();
        return flags >= KNIGHT_PROMOTION && flags <= QUEEN_PROMO_CAPTURE;
    }
    
    /**
     * Checks if this is a castling move.
     */
    public boolean isCastling() {
        int flags = getFlags();
        return flags == KING_CASTLE || flags == QUEEN_CASTLE;
    }
    
    /**
     * Checks if this is an en passant capture.
     */
    public boolean isEnPassant() {
        return getFlags() == EP_CAPTURE;
    }
    
    /**
     * Checks if this is a double pawn push.
     */
    public boolean isDoublePawnPush() {
        return getFlags() == DOUBLE_PAWN_PUSH;
    }
    
    /**
     * Checks if this is a quiet move (no capture, no special).
     */
    public boolean isQuiet() {
        return getFlags() == QUIET_MOVE;
    }
    
    /**
     * Returns the move in UCI format (e.g., "e2e4", "e7e8q").
     */
    public String toUCI() {
        StringBuilder sb = new StringBuilder();
        sb.append(squareToString(getFrom()));
        sb.append(squareToString(getTo()));
        
        if (isPromotion()) {
            PieceType promo = getPromotionPieceType();
            sb.append(Character.toLowerCase(promo.name().charAt(0)));
        }
        
        return sb.toString();
    }
    
    /**
     * Converts a square index (0-63) to algebraic notation (e.g., "e4").
     */
    public static String squareToString(int square) {
        int file = square & 7;
        int rank = square >>> 3;
        return "" + (char)('a' + file) + (rank + 1);
    }
    
    /**
     * Converts algebraic notation to square index.
     */
    public static int stringToSquare(String square) {
        int file = square.charAt(0) - 'a';
        int rank = square.charAt(1) - '1';
        return rank * 8 + file;
    }
    
    /**
     * Creates a move from UCI notation.
     */
    public static Move fromUCI(String uci) {
        if (uci.length() < 4) {
            return NULL_MOVE;
        }
        
        int from = stringToSquare(uci.substring(0, 2));
        int to = stringToSquare(uci.substring(2, 4));
        
        int flags = QUIET_MOVE;
        if (uci.length() == 5) {
            char promo = uci.charAt(4);
            switch (promo) {
                case 'n': flags = KNIGHT_PROMOTION; break;
                case 'b': flags = BISHOP_PROMOTION; break;
                case 'r': flags = ROOK_PROMOTION; break;
                case 'q': flags = QUEEN_PROMOTION; break;
            }
        }
        
        return new Move(from, to, flags);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        // Compare only the relevant bits (from, to, flags)
        return (moveData & 0xFFFF) == (other.moveData & 0xFFFF);
    }
    
    @Override
    public int hashCode() {
        return moveData & 0xFFFF;
    }
    
    @Override
    public String toString() {
        return toUCI();
    }
    
    /**
     * Returns the raw move data for storage/retrieval.
     */
    public int getMoveData() {
        return moveData;
    }
    
    /**
     * Creates a move from raw move data.
     */
    public static Move fromMoveData(int moveData) {
        return new Move(moveData);
    }
}
