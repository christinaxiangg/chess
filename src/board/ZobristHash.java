package board;
import java.util.Random;
import piece.Piece;
import piece.PieceColor;

/**
 * Zobrist hashing implementation for chess positions.
 * Provides fast incremental hash updates for move making/unmaking.
 */
public class ZobristHash {
    
    // Hash keys for pieces on squares [piece][square]
    private static final long[][] PIECE_KEYS = new long[12][64];
    
    // Hash keys for castling rights [4 bits]
    private static final long[] CASTLING_KEYS = new long[16];
    
    // Hash keys for en passant file [8 files]
    private static final long[] EN_PASSANT_KEYS = new long[8];
    
    // Hash key for side to move
    private static final long SIDE_TO_MOVE_KEY;
    
    static {
        // Initialize all zobrist keys with random numbers
        Random random = new Random(12345); // Fixed seed for reproducibility
        
        // Initialize piece keys
        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) {
                PIECE_KEYS[piece][square] = random.nextLong();
            }
        }
        
        // Initialize castling keys
        for (int i = 0; i < 16; i++) {
            CASTLING_KEYS[i] = random.nextLong();
        }
        
        // Initialize en passant keys
        for (int i = 0; i < 8; i++) {
            EN_PASSANT_KEYS[i] = random.nextLong();
        }
        
        // Initialize side to move key
        SIDE_TO_MOVE_KEY = random.nextLong();
    }
    
    /**
     * Computes the zobrist hash for a given board position.
     */
    public static long computeHash(BitBoard board) {
        long hash = 0L;
        
        // Hash all pieces
        for (int square = 0; square < 64; square++) {
            Piece piece = board.getPiece(square);
            if (piece != null) {
                hash ^= PIECE_KEYS[piece.ordinal()][square];
            }
        }
        
        // Hash castling rights
        hash ^= CASTLING_KEYS[board.getCastlingRights()];
        
        // Hash en passant square
        if (board.getEnPassantSquare() != -1) {
            int file = board.getEnPassantSquare() & 7;
            hash ^= EN_PASSANT_KEYS[file];
        }
        
        // Hash side to move
        if (board.getSideToMove() == PieceColor.BLACK) {
            hash ^= SIDE_TO_MOVE_KEY;
        }
        
        return hash;
    }
    
    /**
     * Updates hash when a piece is added to a square.
     */
    public static long addPiece(long hash, Piece piece, int square) {
        return hash ^ PIECE_KEYS[piece.ordinal()][square];
    }
    
    /**
     * Updates hash when a piece is removed from a square.
     */
    public static long removePiece(long hash, Piece piece, int square) {
        return hash ^ PIECE_KEYS[piece.ordinal()][square];
    }
    
    /**
     * Updates hash when a piece moves.
     */
    public static long movePiece(long hash, Piece piece, int from, int to) {
        hash ^= PIECE_KEYS[piece.ordinal()][from];
        hash ^= PIECE_KEYS[piece.ordinal()][to];
        return hash;
    }
    
    /**
     * Updates hash when castling rights change.
     */
    public static long updateCastlingRights(long hash, int oldRights, int newRights) {
        hash ^= CASTLING_KEYS[oldRights];
        hash ^= CASTLING_KEYS[newRights];
        return hash;
    }
    
    /**
     * Updates hash when en passant square changes.
     */
    public static long updateEnPassant(long hash, int oldEP, int newEP) {
        if (oldEP != -1) {
            int oldFile = oldEP & 7;
            hash ^= EN_PASSANT_KEYS[oldFile];
        }
        if (newEP != -1) {
            int newFile = newEP & 7;
            hash ^= EN_PASSANT_KEYS[newFile];
        }
        return hash;
    }
    
    /**
     * Toggles the side to move in the hash.
     */
    public static long toggleSideToMove(long hash) {
        return hash ^ SIDE_TO_MOVE_KEY;
    }
}
