package piece;

/**
 * Represents a chess piece with its color and type.
 */
public enum Piece {
    // White pieces
    WHITE_PAWN('P', PieceType.PAWN, PieceColor.WHITE),
    WHITE_KNIGHT('N', PieceType.KNIGHT, PieceColor.WHITE),
    WHITE_BISHOP('B', PieceType.BISHOP, PieceColor.WHITE),
    WHITE_ROOK('R', PieceType.ROOK, PieceColor.WHITE),
    WHITE_QUEEN('Q', PieceType.QUEEN, PieceColor.WHITE),
    WHITE_KING('K', PieceType.KING, PieceColor.WHITE),
    
    // Black pieces
    BLACK_PAWN('p', PieceType.PAWN, PieceColor.BLACK),
    BLACK_KNIGHT('n', PieceType.KNIGHT, PieceColor.BLACK),
    BLACK_BISHOP('b', PieceType.BISHOP, PieceColor.BLACK),
    BLACK_ROOK('r', PieceType.ROOK, PieceColor.BLACK),
    BLACK_QUEEN('q', PieceType.QUEEN, PieceColor.BLACK),
    BLACK_KING('k', PieceType.KING, PieceColor.BLACK);
    
    private final char symbol;
    private final PieceType type;
    private final PieceColor color;
    
    Piece(char symbol, PieceType type, PieceColor color) {
        this.symbol = symbol;
        this.type = type;
        this.color = color;
    }
    
    public char getSymbol() {
        return symbol;
    }
    
    public PieceType getType() {
        return type;
    }
    
    public PieceColor getColor() {
        return color;
    }
    
    public boolean isWhite() {
        return color == PieceColor.WHITE;
    }
    
    public boolean isBlack() {
        return color == PieceColor.BLACK;
    }
    
    /**
     * Gets the piece from its symbol character.
     */
    public static Piece fromSymbol(char symbol) {
        for (Piece piece : values()) {
            if (piece.symbol == symbol) {
                return piece;
            }
        }
        return null;
    }
    
    /**
     * Gets a piece by color and type.
     */
    public static Piece getPiece(PieceColor color, PieceType type) {
        for (Piece piece : values()) {
            if (piece.color == color && piece.type == type) {
                return piece;
            }
        }
        return null;
    }
    
    /**
     * Returns the value of the piece for evaluation purposes.
     */
    public int getValue() {
        return type.getValue();
    }
}
