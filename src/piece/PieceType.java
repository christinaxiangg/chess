package piece;

/**
 * Represents the type of a chess piece.
 */
public enum PieceType {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20000);
    
    private final int value;
    
    PieceType(int value) {
        this.value = value;
    }
    
    /**
     * Returns the material value of this piece type in centipawns.
     */
    public int getValue() {
        return value;
    }
}
