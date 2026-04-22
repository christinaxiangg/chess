package piece;

/**
 * Represents the color of a chess piece or player.
 */
public enum PieceColor {
    WHITE,
    BLACK;
    
    /**
     * Returns the opposite color.
     */
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
