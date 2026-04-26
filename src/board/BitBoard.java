package board;

import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;
import move.Move;
/**
 * Production-level chess bitboard implementation with piece tracking.
 * Provides an array-like interface while using bitboards internally for efficiency.
 */
public class BitBoard {
    // Bitboards for each piece type and color (12 total)
    private final long[] pieceBitboards;
    
    // Occupancy bitboards
    private long whitePieces;
    private long blackPieces;
    private long allPieces;
    
    // piece.Piece list for fast iteration (tracks all pieces on board)
    private final PieceList[] pieceLists;
    
    // Stack to store board states for undo operations
    private final java.util.Stack<BoardState> stateHistory;
    
    // Mailbox representation for O(1) piece lookup by square
    private final Piece[] mailbox;
    
    // Game state
    private PieceColor sideToMove;
    private int castlingRights; // KQkq encoded as bits
    private int enPassantSquare; // -1 if none
    private int halfMoveClock;
    private int fullMoveNumber;

    // Zobrist hash for position identification
    private long currentHash;
    
    // Castling rights constants
    public static final int WHITE_KING_SIDE = 1;
    public static final int WHITE_QUEEN_SIDE = 2;
    public static final int BLACK_KING_SIDE = 4;
    public static final int BLACK_QUEEN_SIDE = 8;
    
    /**
     * Creates a new empty board.
     */
    public BitBoard() {
        pieceBitboards = new long[12];
        mailbox = new Piece[64];
        pieceLists = new PieceList[12];
        stateHistory = new java.util.Stack<>();
        
        for (int i = 0; i < 12; i++) {
            pieceLists[i] = new PieceList();
        }
        
        sideToMove = PieceColor.WHITE;
        castlingRights = 0;
        enPassantSquare = -1;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        currentHash = 0L;
    }
    public BitBoard(BitBoard from) {
        pieceBitboards = new long[12];
        mailbox = new Piece[64];
        pieceLists = new PieceList[12];
        stateHistory = new java.util.Stack<>();

        for (int i = 0; i < 12; i++) {
            pieceLists[i] = new PieceList();
        }
        System.arraycopy(from.mailbox, 0, mailbox, 0, 64);
        // Copy piece positions
        for (int i = 0; i < 64; i++) {
            if (mailbox[i] != null) {
                Piece piece = mailbox[i];
                int pieceIndex = piece.ordinal();
                long mask = 1L << i;
                pieceBitboards[pieceIndex] |= mask;
                pieceLists[pieceIndex].add(i);
                if (piece.isWhite()) {
                    whitePieces |= mask;
                } else {
                    blackPieces |= mask;
                }
                allPieces |= mask;
            }
        }

        // Deep copy stateHistory (bottom-to-top order preserved).
        // BoardState fields are all primitives, enums, or immutable objects (Piece, Move),
        // so re-constructing each entry is a true deep copy.
        for (BoardState s : from.stateHistory) {
            stateHistory.push(new BoardState(
                    s.castlingRights,
                    s.enPassantSquare,
                    s.halfMoveClock,
                    s.fullMoveNumber,
                    s.sideToMove,
                    s.capturedPiece,
                    s.movedPiece,
                    s.hash,
                    s.move,
                    s.fenBefore
            ));
        }

        // Copy scalar game state
        sideToMove      = from.sideToMove;
        castlingRights  = from.castlingRights;
        enPassantSquare = from.enPassantSquare;
        halfMoveClock   = from.halfMoveClock;
        fullMoveNumber  = from.fullMoveNumber;
        currentHash     = from.currentHash;
    }

    
    /**
     * Creates a board from FEN notation.
     */
    public static BitBoard fromFEN(String fen) {
        BitBoard board = new BitBoard();
        String[] parts = fen.split(" ");
        
        // Parse piece placement
        String[] ranks = parts[0].split("/");
        for (int rank = 7; rank >= 0; rank--) {
            int file = 0;
            for (char c : ranks[7 - rank].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += (c - '0');
                } else {
                    Piece piece = Piece.fromSymbol(c);
                    if (piece != null) {
                        int square = rank * 8 + file;
                        board.setPiece(square, piece);
                        file++;
                    }
                }
            }
        }
        
        // Parse side to move
        if (parts.length > 1) {
            board.sideToMove = parts[1].equals("w") ? PieceColor.WHITE : PieceColor.BLACK;
        }
        
        // Parse castling rights
        if (parts.length > 2) {
            board.castlingRights = 0;
            if (parts[2].contains("K")) board.castlingRights |= WHITE_KING_SIDE;
            if (parts[2].contains("Q")) board.castlingRights |= WHITE_QUEEN_SIDE;
            if (parts[2].contains("k")) board.castlingRights |= BLACK_KING_SIDE;
            if (parts[2].contains("q")) board.castlingRights |= BLACK_QUEEN_SIDE;
        }
        
        // Parse en passant square
        if (parts.length > 3 && !parts[3].equals("-")) {
            board.enPassantSquare = Move.stringToSquare(parts[3]);
        }
        
        // Parse move counters
        if (parts.length > 4) {
            board.halfMoveClock = Integer.parseInt(parts[4]);
        }
        if (parts.length > 5) {
            board.fullMoveNumber = Integer.parseInt(parts[5]);
        }
        // Compute initial Zobrist hash from scratch
        board.currentHash = ZobristHash.computeHash(board);
        
        return board;
    }
    
    /**
     * Returns the starting position.
     */
    public static BitBoard startingPosition() {
        return fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }
    
    /**
     * Gets the piece at a given square (array-like access).
     * @param square Square index (0-63), where 0 is a1, 7 is h1, 56 is a8, 63 is h8
     * @return The piece at the square, or null if empty
     */
    public Piece getPiece(int square) {
        return mailbox[square];
    }
    
    /**
     * Gets the piece at a given file and rank.
     * @param file File index (0-7, where 0 is 'a' and 7 is 'h')
     * @param rank Rank index (0-7, where 0 is rank 1 and 7 is rank 8)
     */
    public Piece getPiece(int file, int rank) {
        return mailbox[rank * 8 + file];
    }
    
    /**
     * Sets a piece at a given square.
     */
    public void setPiece(int square, Piece piece) {
        // Remove existing piece if present
        Piece existing = mailbox[square];
        if (existing != null) {
            removePiece(square);
        }
        
        if (piece != null) {
            mailbox[square] = piece;
            int pieceIndex = piece.ordinal();
            long mask = 1L << square;
            
            pieceBitboards[pieceIndex] |= mask;
            pieceLists[pieceIndex].add(square);
            
            if (piece.isWhite()) {
                whitePieces |= mask;
            } else {
                blackPieces |= mask;
            }
            allPieces |= mask;
        }
    }
    
    /**
     * Removes a piece from a square.
     */
    public void removePiece(int square) {
        Piece piece = mailbox[square];
        if (piece == null){
            System.out.println("removePiece Attempting to move a piece from an empty square:"+ Move.squareToString(square));
            throw new RuntimeException("removePiece Attempting to move a piece from an empty square: " + Move.squareToString(square));
        }

        mailbox[square] = null;
        int pieceIndex = piece.ordinal();
        long mask = ~(1L << square);
        
        pieceBitboards[pieceIndex] &= mask;
        pieceLists[pieceIndex].remove(square);
        
        // Only clear the bit for the correct color
        if (piece.isWhite()) {
            whitePieces &= mask;
        } else {
            blackPieces &= mask;
        }
        allPieces &= mask;
    }
    
    /**
     * Moves a piece from one square to another (used for making moves).
     */
    public void movePiece(int from, int to) {
        Piece piece = mailbox[from];
        if (piece == null) {
            System.out.println("movePiece Attempting to move a piece from an empty square:" + Move.squareToString(from));
            throw new RuntimeException("Attempting to move a piece from an empty square: " + Move.squareToString(from));
        }
        removePiece(from);
        setPiece(to, piece);
    }
    
    /**
     * Inner class to store board state for undo operations.
     */
    private static class BoardState {
        private final int castlingRights;
        private final int enPassantSquare;
        private final int halfMoveClock;
        private final int fullMoveNumber;
        private final PieceColor sideToMove;
        private final Piece capturedPiece;
        private final Piece movedPiece;
        private final long hash;
        private final Move move;
        private final String fenBefore;
        public BoardState(int castlingRights, int enPassantSquare, int halfMoveClock, 
                          int fullMoveNumber, PieceColor sideToMove, Piece capturedPiece, 
                          Piece movedPiece, long hash, Move move, String fenBefore) {
            this.castlingRights = castlingRights;
            this.enPassantSquare = enPassantSquare;
            this.halfMoveClock = halfMoveClock;
            this.fullMoveNumber = fullMoveNumber;
            this.sideToMove = sideToMove;
            this.capturedPiece = capturedPiece;
            this.movedPiece = movedPiece;
            this.hash = hash;
            this.move = move;
            this.fenBefore = fenBefore;
        }
    }

    /**
     * Makes a move on the board.
     */
    public void makeMove(Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        Piece piece = getPiece(from);

        if (piece == null) {
            // Print full move history before crashing
            System.out.println("=== MOVE HISTORY (most recent last) ===");
            List<BoardState> states = new ArrayList<>(stateHistory);
            for (int i = 0; i < states.size(); i++) {
                BoardState s = states.get(i);
                System.out.println("  " + (i + 1) + ". " + s.sideToMove + " played " + s.move );
            }
            System.out.println("makeMove Attempting to move a piece from an empty square:" + Move.squareToString(from) + " move=" + move + " fen=" + toFEN());
            throw new RuntimeException(
                    "makeMove: no piece at " + Move.squareToString(from) +
                            " move=" + move + " fen=" + toFEN()
            );
        }
        
        // Capture state BEFORE any modifications
        Piece capturedPiece = null;
        if (move.isCapture() && !move.isEnPassant()) {
            capturedPiece = getPiece(to);
        } else if (move.isEnPassant()) {
            int captureSquare = sideToMove == PieceColor.WHITE ? to - 8 : to + 8;
            capturedPiece = getPiece(captureSquare);
        }

        // Push state immediately with original values
        stateHistory.push(new BoardState(
            castlingRights,
            enPassantSquare,
            halfMoveClock,
            fullMoveNumber,
            sideToMove,
            capturedPiece,
            piece,  // The original piece before promotion
            currentHash,
                move, toFEN()
        ));

        // ── Incremental Zobrist hash update ──────────────────────────────────────
        // 1. Remove old en passant contribution
        if (enPassantSquare != -1) {
            currentHash ^= ZobristHash.EN_PASSANT_KEYS[enPassantSquare & 7];
        }
        // 2. Remove old castling contribution
        currentHash ^= ZobristHash.CASTLING_KEYS[castlingRights];
        // Handle captures
        if (move.isCapture() && !move.isEnPassant()) {
            // Remove captured piece from hash before removing from board
            currentHash ^= ZobristHash.PIECE_KEYS[capturedPiece.ordinal()][to];
            removePiece(to);
        }
        
        // Handle en passant
        if (move.isEnPassant()) {
            int captureSquare = sideToMove == PieceColor.WHITE ? to - 8 : to + 8;
            currentHash ^= ZobristHash.PIECE_KEYS[capturedPiece.ordinal()][captureSquare];
            removePiece(captureSquare);
        }
        
        // Move the piece: XOR out from-square, XOR in to-square
        currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][from];
        currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][to];
        movePiece(from, to);
        
        // Handle promotion
        if (move.isPromotion()) {
            PieceType promoType = move.getPromotionPieceType();
            Piece promoPiece = Piece.getPiece(piece.getColor(), promoType);
            // Remove pawn hash (already at 'to'), add promoted piece
            currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][to];
            currentHash ^= ZobristHash.PIECE_KEYS[promoPiece.ordinal()][to];
            removePiece(to);
            setPiece(to, promoPiece);
        }
        
        // Handle castling
        if (move.isCastling()) {
            if (move.getFlags() == Move.KING_CASTLE) {
                int rookFrom = sideToMove == PieceColor.WHITE ? 7 : 63;
                int rookTo = sideToMove == PieceColor.WHITE ? 5 : 61;
                currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][rookFrom];
                currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][rookTo];
                movePiece(rookFrom, rookTo);
            } else { // Queen side
                int rookFrom = sideToMove == PieceColor.WHITE ? 0 : 56;
                int rookTo = sideToMove == PieceColor.WHITE ? 3 : 59;
                currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][rookFrom];
                currentHash ^= ZobristHash.PIECE_KEYS[piece.ordinal()][rookTo];
                movePiece(rookFrom, rookTo);
            }
        }
        
        // Update castling rights
        updateCastlingRights(from, to);
        // Update en passant square
        if (move.isDoublePawnPush()) {
            enPassantSquare = sideToMove == PieceColor.WHITE ? from + 8 : from - 8;
        } else {
            enPassantSquare = -1;
        }

        // 3. Add new castling contribution
        currentHash ^= ZobristHash.CASTLING_KEYS[castlingRights];
        // 4. Add new en passant contribution
        if (enPassantSquare != -1) {
            currentHash ^= ZobristHash.EN_PASSANT_KEYS[enPassantSquare & 7];
        }
        // 5. Flip side to move
        currentHash ^= ZobristHash.SIDE_TO_MOVE_KEY;
        // ── End hash update ──────────────────────────────────────────────────────

        // Update move counters
        if (piece.getType() == PieceType.PAWN || move.isCapture()) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
        
        if (sideToMove == PieceColor.BLACK) {
            fullMoveNumber++;
        }
        
        // Switch side to move
        sideToMove = sideToMove.opposite();
    }
    
    /**
     * Undoes a move on the board, restoring the state to before the move was made.
     */
    public void undoMakeMove(Move move) {
        if (stateHistory.isEmpty()) {
            System.out.println("undoMakeMove called on empty history! Move: " + move);
            return;
        }
        BoardState state = stateHistory.peek(); // peek first, don't pop yet
        if (state.move == null) {
            // This is a null move entry — we're calling undoMakeMove on a null move slot!
            System.out.println("ERROR: undoMakeMove called but top of stack is a null move entry!");
            System.out.println("Tried to undo: " + move + " FEN: " + toFEN()
            );
            printHistory();
            throw new RuntimeException("Stack corruption: null move entry at top");
        }
         stateHistory.pop();
        Move move2 = state.move; // Use the stored move, not a caller-supplied one
        if (!move.equals(move2)) {
            System.out.println("Warning: undoMakeMove move mismatch! Expected: " + move2 + ", got: " + move);
        }
        int from = move.getFrom();
        int to = move.getTo();
        
        // Handle castling: move rook back FIRST (before restoring side)
        if (move.isCastling()) {
            // Use current sideToMove (which was flipped in makeMove)
            PieceColor movingSide = sideToMove.opposite();
            if (move.getFlags() == Move.KING_CASTLE) {
                int rookFrom = movingSide == PieceColor.WHITE ? 7 : 63;
                int rookTo = movingSide == PieceColor.WHITE ? 5 : 61;
                movePiece(rookTo, rookFrom);
            } else { // Queen side
                int rookFrom = movingSide == PieceColor.WHITE ? 0 : 56;
                int rookTo = movingSide == PieceColor.WHITE ? 3 : 59;
                movePiece(rookTo, rookFrom);
            }
        }
        
        // Handle promotion: get the promoted piece and replace with original pawn
        if (move.isPromotion()) {
            removePiece(to);
            setPiece(to, state.movedPiece);  // Restore original pawn
        }
        
        // Move piece back from 'to' to 'from'
        movePiece(to, from);
        
        // Restore captured piece
        if (move.isCapture()) {
            if (move.isEnPassant()) {
                // Restore the captured pawn at the en passant square
                int captureSquare = state.sideToMove == PieceColor.WHITE ? to - 8 : to + 8;
                setPiece(captureSquare, state.capturedPiece);
            } else {
                // Restore normally captured piece
                setPiece(to, state.capturedPiece);
            }
        }
        
        // Restore all game state
        sideToMove = state.sideToMove;
        castlingRights = state.castlingRights;
        enPassantSquare = state.enPassantSquare;
        halfMoveClock = state.halfMoveClock;
        fullMoveNumber = state.fullMoveNumber;
        currentHash = state.hash;
    }
    
    /**
     * Updates castling rights based on piece movement.
     */
    private void updateCastlingRights(int from, int to) {
        // King moves remove both castling rights for that side
        if (from == 4) { // White king
            castlingRights &= ~(WHITE_KING_SIDE | WHITE_QUEEN_SIDE);
        } else if (from == 60) { // Black king
            castlingRights &= ~(BLACK_KING_SIDE | BLACK_QUEEN_SIDE);
        }
        
        // Rook moves or captures remove specific castling rights
        if (from == 0 || to == 0) castlingRights &= ~WHITE_QUEEN_SIDE;
        if (from == 7 || to == 7) castlingRights &= ~WHITE_KING_SIDE;
        if (from == 56 || to == 56) castlingRights &= ~BLACK_QUEEN_SIDE;
        if (from == 63 || to == 63) castlingRights &= ~BLACK_KING_SIDE;
    }
    
    /**
     * Gets all pieces of a specific type and color.
     */
    public List<Integer> getPieces(Piece piece) {
        return pieceLists[piece.ordinal()].getSquares();
    }
    
    /**
     * Gets all pieces of a specific color.
     */
    public List<Integer> getPieces(PieceColor color) {
        List<Integer> pieces = new ArrayList<>();
        for (Piece piece : Piece.values()) {
            if (piece.getColor() == color) {
                pieces.addAll(getPieces(piece));
            }
        }
        return pieces;
    }
    
    /**
     * Gets the bitboard for a specific piece.
     */
    public long getBitboard(Piece piece) {
        return pieceBitboards[piece.ordinal()];
    }
    
    /**
     * Gets the occupancy bitboard for white pieces.
     */
    public long getWhitePieces() {
        return whitePieces;
    }
    
    /**
     * Gets the occupancy bitboard for black pieces.
     */
    public long getBlackPieces() {
        return blackPieces;
    }
    
    /**
     * Gets the occupancy bitboard for all pieces.
     */
    public long getAllPieces() {
        return allPieces;
    }
    
    /**
     * Gets the occupancy bitboard for a specific color.
     */
    public long getColorPieces(PieceColor color) {
        return color == PieceColor.WHITE ? whitePieces : blackPieces;
    }
    
    /**
     * Checks if a square is occupied.
     */
    public boolean isOccupied(int square) {
        return mailbox[square] != null;
    }
    
    /**
     * Checks if a square is empty.
     */
    public boolean isEmpty(int square) {
        return mailbox[square] == null;
    }
    
    // Getters and setters for game state
    
    public long getHash() {
        return currentHash;
    }

    public PieceColor getSideToMove() {
        return sideToMove;
    }
    
    public void setSideToMove(PieceColor color) {
        this.sideToMove = color;
    }
    
    public int getCastlingRights() {
        return castlingRights;
    }
    
    public void setCastlingRights(int rights) {
        this.castlingRights = rights;
    }
    
    public boolean canCastle(int right) {
        return (castlingRights & right) != 0;
    }
    
    public int getEnPassantSquare() {
        return enPassantSquare;
    }
    
    public void setEnPassantSquare(int square) {
        this.enPassantSquare = square;
    }
    
    public int getHalfMoveClock() {
        return halfMoveClock;
    }
    
    public void setHalfMoveClock(int clock) {
        this.halfMoveClock = clock;
    }
    
    public int getFullMoveNumber() {
        return fullMoveNumber;
    }
    
    public void setFullMoveNumber(int number) {
        this.fullMoveNumber = number;
    }
    
    /**
     * Converts the board to FEN notation.
     */
    public String toFEN() {
        StringBuilder fen = new StringBuilder();
        
        // piece.Piece placement
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                Piece piece = getPiece(square);
                
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece.getSymbol());
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (rank > 0) {
                fen.append('/');
            }
        }
        
        // Side to move
        fen.append(' ').append(sideToMove == PieceColor.WHITE ? 'w' : 'b');
        
        // Castling rights
        fen.append(' ');
        if (castlingRights == 0) {
            fen.append('-');
        } else {
            if ((castlingRights & WHITE_KING_SIDE) != 0) fen.append('K');
            if ((castlingRights & WHITE_QUEEN_SIDE) != 0) fen.append('Q');
            if ((castlingRights & BLACK_KING_SIDE) != 0) fen.append('k');
            if ((castlingRights & BLACK_QUEEN_SIDE) != 0) fen.append('q');
        }
        
        // En passant square
        fen.append(' ');
        if (enPassantSquare == -1) {
            fen.append('-');
        } else {
            fen.append(Move.squareToString(enPassantSquare));
        }
        
        // move.Move counters
        fen.append(' ').append(halfMoveClock);
        fen.append(' ').append(fullMoveNumber);
        
        return fen.toString();
    }
    


    public long[] getGameHistoryHashes() {
        long[] hashes = new long[stateHistory.size()];
        int i = 0;
        for (BoardState state : stateHistory) {
            hashes[i++] = state.hash;
        }
        return hashes;
    }

    public BitBoard copy() {
        return new BitBoard(this);
    }
    
    /**
     * Pretty prints the board to console.
     */
    public void print() {
        System.out.println("\n  a b c d e f g h");
        System.out.println("  ---------------");
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + "|");
            for (int file = 0; file < 8; file++) {
                Piece piece = getPiece(file, rank);
                System.out.print(piece == null ? ". " : piece.getSymbol() + " ");
            }
            System.out.println("|" + (rank + 1));
        }
        System.out.println("  ---------------");
        System.out.println("  a b c d e f g h\n");
        System.out.println("FEN: " + toFEN());
    }
    
    /**
     * Helper class to track pieces of a specific type efficiently.
     */
    private static class PieceList {
        private List<Integer> squares;
        
        public PieceList() {
            this.squares = new ArrayList<>();
        }
        
        public void add(int square) {
            if (!squares.contains(square)) {
                squares.add(square);
            }
        }
        
        public void remove(int square) {
            squares.remove(Integer.valueOf(square));
        }
        
        public List<Integer> getSquares() {
            return new ArrayList<>(squares);
        }
        
        public int size() {
            return squares.size();
        }
    }
    public void makeNullMove() {
        stateHistory.push(new BoardState(
                castlingRights,
                enPassantSquare,
                halfMoveClock,
                fullMoveNumber,
                sideToMove,
                null,
                null,
                currentHash,
                null,
                toFEN()
        ));
        // Update hash: remove old EP key, flip side
        if (enPassantSquare != -1) {
            currentHash ^= ZobristHash.EN_PASSANT_KEYS[enPassantSquare & 7];
        }
        currentHash ^= ZobristHash.SIDE_TO_MOVE_KEY;
        enPassantSquare = -1;
        sideToMove = sideToMove.opposite();
        halfMoveClock++;
    }

    public void undoNullMove() {
        BoardState state = stateHistory.peek();
        if (state.move != null) {
            throw new RuntimeException(
                    "undoNullMove called but top of stack is a real move entry: " + state.move
            );
        }
        stateHistory.pop();
        sideToMove = state.sideToMove;
        enPassantSquare = state.enPassantSquare;
        halfMoveClock = state.halfMoveClock;
        castlingRights = state.castlingRights;
        currentHash = state.hash;
    }

    public void printHistory() {
        List<BoardState> states = new ArrayList<>(stateHistory);
        System.out.println("=== MOVE HISTORY (" + states.size() + " moves) ===");
        for (int i = 0; i < states.size(); i++) {
            BoardState s = states.get(i);
            System.out.println("  " + (i + 1) + ". " + s.sideToMove + " played " + s.move
                    + " | FEN before: " + s.fenBefore);
        }
    }

}
