package move;

import board.BitBoard;

import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;

/**
 * Generates all legal moves for a chess position using bitboard techniques.
 * Includes sliding piece attack generation, pawn moves, and special moves.
 */
public class MoveGenerator {
    
    // Precomputed attack tables for non-sliding pieces
    private static final long[] KNIGHT_ATTACKS = new long[64];
    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] WHITE_PAWN_ATTACKS = new long[64];
    private static final long[] BLACK_PAWN_ATTACKS = new long[64];
    
    // File and rank masks
    private static final long FILE_A = 0x0101010101010101L;
    private static final long FILE_H = 0x8080808080808080L;
    private static final long FILE_AB = 0x0303030303030303L;
    private static final long FILE_GH = 0xC0C0C0C0C0C0C0C0L;
    private static final long RANK_1 = 0x00000000000000FFL;
    private static final long RANK_2 = 0x000000000000FF00L;
    private static final long RANK_4 = 0x00000000FF000000L;
    private static final long RANK_5 = 0x000000FF00000000L;
    private static final long RANK_7 = 0x00FF000000000000L;
    private static final long RANK_8 = 0xFF00000000000000L;
    
    static {
        initializeAttackTables();
    }
    
    /**
     * Generates all legal moves for the current position.
     */
    public static List<Move> generateLegalMoves(BitBoard board) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(board);
        List<Move> legal = new ArrayList<>();
        
        for (Move move : pseudoLegal) {
            if (isLegalMove(board, move)) {
                legal.add(move);
            }
        }
        
        return legal;
    }
    
    /**
     * Generates all pseudo-legal moves (may leave king in check).
     */
    public static List<Move> generatePseudoLegalMoves(BitBoard board) {
        List<Move> moves = new ArrayList<>();
        PieceColor us = board.getSideToMove();
        
        // Generate moves for each piece type
        generatePawnMoves(board, moves, us);
        generateKnightMoves(board, moves, us);
        generateBishopMoves(board, moves, us);
        generateRookMoves(board, moves, us);
        generateQueenMoves(board, moves, us);
        generateKingMoves(board, moves, us);
        generateCastlingMoves(board, moves, us);
        
        return moves;
    }
    
    /**
     * Checks if a move is legal (doesn't leave king in check).
     */
    private static boolean isLegalMove(BitBoard board, Move move) {
        // Find our king position
        PieceColor us = board.getSideToMove();
        board.makeMove(move);
        Piece ourKing = us == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
        List<Integer> kingSquares = board.getPieces(ourKing);

        if (kingSquares.isEmpty()) {
            System.out.println("=== KING CAPTURE DEBUG ===");
            System.out.println("Side that just moved: " + us);
            System.out.println("Move that caused it: " + move);
            System.out.println("Board after makeMove:");
            board.print();
            board.undoMakeMove(move);
            System.out.println("Board after undoMakeMove (should be restored):");
            board.print();
            System.out.println("History:");
            board.printHistory();  // we'll add this below
            throw new RuntimeException("King was captured");
        }
        
        int kingSquare = kingSquares.get(0);
        boolean attacked = isSquareAttacked(board, kingSquare, us.opposite());
        // Only log when we're about to return TRUE for a king move
        // (i.e. declaring a king move legal — this is where wrong calls sneak through)
        if (!attacked && move.getTo() == kingSquare) {
            Piece ourKingPiece = us == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
            if (board.getPiece(kingSquare) == ourKingPiece) {
                long enemyPawns = board.getBitboard(
                        us == PieceColor.WHITE ? Piece.BLACK_PAWN : Piece.WHITE_PAWN);
                long pawnAttackMask = us == PieceColor.WHITE ?
                        BLACK_PAWN_ATTACKS[kingSquare] : WHITE_PAWN_ATTACKS[kingSquare];
                long pawnThreat = enemyPawns & pawnAttackMask;
                if (pawnThreat != 0) {
                    System.out.println("BUG: King move to " + Move.squareToString(kingSquare)
                            + " passed as legal but enemy pawn on "
                            + Move.squareToString(Long.numberOfTrailingZeros(pawnThreat))
                            + " attacks it!");
                    board.undoMakeMove(move);
                    return false; // force-reject it
                }
            }
        }

        boolean isLegal = !attacked;
        board.undoMakeMove(move);
        return isLegal;
    }
    
    /**
     * Checks if a square is attacked by a given side.
     */
    public static boolean isSquareAttacked(BitBoard board, int square, PieceColor attackerColor) {
        // Check pawn attacks
        Piece attackerPawn = attackerColor == PieceColor.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
        long pawnAttacks = attackerColor == PieceColor.WHITE ? 
            BLACK_PAWN_ATTACKS[square] : WHITE_PAWN_ATTACKS[square];
        if ((board.getBitboard(attackerPawn) & pawnAttacks) != 0) {
            return true;
        }
        
        // Check knight attacks
        Piece attackerKnight = attackerColor == PieceColor.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
        if ((board.getBitboard(attackerKnight) & KNIGHT_ATTACKS[square]) != 0) {
            return true;
        }
        
        // Check king attacks
        Piece attackerKing = attackerColor == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
        if ((board.getBitboard(attackerKing) & KING_ATTACKS[square]) != 0) {
            return true;
        }
        
        // Check bishop/queen diagonal attacks
        long bishopAttacks = getBishopAttacks(square, board.getAllPieces());
        Piece attackerBishop = attackerColor == PieceColor.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
        Piece attackerQueen = attackerColor == PieceColor.WHITE ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
        if ((bishopAttacks & (board.getBitboard(attackerBishop) | board.getBitboard(attackerQueen))) != 0) {
            return true;
        }
        
        // Check rook/queen straight attacks
        long rookAttacks = getRookAttacks(square, board.getAllPieces());
        Piece attackerRook = attackerColor == PieceColor.WHITE ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
        if ((rookAttacks & (board.getBitboard(attackerRook) | board.getBitboard(attackerQueen))) != 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Generates pawn moves including pushes, captures, promotions, and en passant.
     */
    private static void generatePawnMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourPawn = us == PieceColor.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
        long pawns = board.getBitboard(ourPawn);
        long occupied = board.getAllPieces();
        long enemies = us == PieceColor.WHITE ? board.getBlackPieces() : board.getWhitePieces();
        
        int pushDirection = us == PieceColor.WHITE ? 8 : -8;
        long promotionRank = us == PieceColor.WHITE ? RANK_8 : RANK_1;
        long startRank = us == PieceColor.WHITE ? RANK_2 : RANK_7;
        
        while (pawns != 0) {
            int from = Long.numberOfTrailingZeros(pawns);
            long fromMask = 1L << from;
            pawns &= pawns - 1; // Clear the lowest bit
            
            int to = from + pushDirection;
            long toMask = 1L << to;
            
            // Single push
            if ((toMask & occupied) == 0) {
                if ((toMask & promotionRank) != 0) {
                    // Promotions
                    addPromotions(moves, from, to, false, null);
                } else {
                    moves.add(new Move(from, to, Move.QUIET_MOVE));
                    
                    // Double push
                    if ((fromMask & startRank) != 0) {
                        int doubleTo = to + pushDirection;
                        if (doubleTo >= 0 && doubleTo < 64) {
                        long doubleToMask = 1L << doubleTo;
                        if ((doubleToMask & occupied) == 0) {
                            moves.add(new Move(from, doubleTo, Move.DOUBLE_PAWN_PUSH));
                            }
                        }
                    }
                }
            }
            
            // Captures
            long attacks = us == PieceColor.WHITE ? WHITE_PAWN_ATTACKS[from] : BLACK_PAWN_ATTACKS[from];
            long captures = attacks & enemies;
            
            while (captures != 0) {
                int captureTo = Long.numberOfTrailingZeros(captures);
                long captureToMask = 1L << captureTo;
                captures &= captures - 1;
                
                Piece captured = board.getPiece(captureTo);
                if ((captureToMask & promotionRank) != 0) {
                    addPromotions(moves, from, captureTo, true, captured.getType());
                } else {
                    moves.add(new Move(from, captureTo, Move.CAPTURE, captured.getType()));
                }
            }
            
            // En passant
            int epSquare = board.getEnPassantSquare();
            if (epSquare != -1 && (attacks & (1L << epSquare)) != 0) {
                moves.add(new Move(from, epSquare, Move.EP_CAPTURE, PieceType.PAWN));
            }
        }
    }
    
    /**
     * Adds all four promotion moves (knight, bishop, rook, queen).
     */
    private static void addPromotions(List<Move> moves, int from, int to,
                                      boolean isCapture, PieceType capturedType) {
        if (isCapture) {
            moves.add(new Move(from, to, Move.KNIGHT_PROMO_CAPTURE, capturedType));
            moves.add(new Move(from, to, Move.BISHOP_PROMO_CAPTURE, capturedType));
            moves.add(new Move(from, to, Move.ROOK_PROMO_CAPTURE,   capturedType));
            moves.add(new Move(from, to, Move.QUEEN_PROMO_CAPTURE,  capturedType));
        } else {
            moves.add(new Move(from, to, Move.KNIGHT_PROMOTION));
            moves.add(new Move(from, to, Move.BISHOP_PROMOTION));
            moves.add(new Move(from, to, Move.ROOK_PROMOTION));
            moves.add(new Move(from, to, Move.QUEEN_PROMOTION));
        }
    }
    
    /**
     * Generates knight moves.
     */
    private static void generateKnightMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourKnight = us == PieceColor.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
        long knights = board.getBitboard(ourKnight);
        long ourPieces = board.getColorPieces(us);
        
        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            
            long attacks = KNIGHT_ATTACKS[from] & ~ourPieces;
            addMovesFromBitboard(board, moves, from, attacks);
        }
    }
    
    /**
     * Generates bishop moves.
     */
    private static void generateBishopMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourBishop = us == PieceColor.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
        long bishops = board.getBitboard(ourBishop);
        long ourPieces = board.getColorPieces(us);
        long occupied = board.getAllPieces();
        
        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            
            long attacks = getBishopAttacks(from, occupied) & ~ourPieces;
            addMovesFromBitboard(board, moves, from, attacks);
        }
    }
    
    /**
     * Generates rook moves.
     */
    private static void generateRookMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourRook = us == PieceColor.WHITE ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
        long rooks = board.getBitboard(ourRook);
        long ourPieces = board.getColorPieces(us);
        long occupied = board.getAllPieces();
        
        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            
            long attacks = getRookAttacks(from, occupied) & ~ourPieces;
            addMovesFromBitboard(board, moves, from, attacks);
        }
    }
    
    /**
     * Generates queen moves.
     */
    private static void generateQueenMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourQueen = us == PieceColor.WHITE ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
        long queens = board.getBitboard(ourQueen);
        long ourPieces = board.getColorPieces(us);
        long occupied = board.getAllPieces();
        
        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            
            long attacks = (getBishopAttacks(from, occupied) | getRookAttacks(from, occupied)) & ~ourPieces;
            addMovesFromBitboard(board, moves, from, attacks);
        }
    }
    
    /**
     * Generates king moves (non-castling).
     */
    private static void generateKingMoves(BitBoard board, List<Move> moves, PieceColor us) {
        Piece ourKing = us == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
        long kings = board.getBitboard(ourKing);
        long ourPieces = board.getColorPieces(us);
        
        if (kings != 0) {
            int from = Long.numberOfTrailingZeros(kings);
            long attacks = KING_ATTACKS[from] & ~ourPieces;
            addMovesFromBitboard(board, moves, from, attacks);
        }
    }
    
    /**
     * Generates castling moves.
     */
    private static void generateCastlingMoves(BitBoard board, List<Move> moves, PieceColor us) {
        long occupied = board.getAllPieces();
        PieceColor them = us.opposite();
        
        if (us == PieceColor.WHITE) {
            // White king-side castling
            if (board.canCastle(BitBoard.WHITE_KING_SIDE)) {
                Piece rookH1 = board.getPiece(7);
                if (rookH1 != null && rookH1.getType() == PieceType.ROOK && rookH1.isWhite()) {
                if ((occupied & 0x60L) == 0) { // f1 and g1 empty
                    if (!isSquareAttacked(board, 4, them) &&  // e1
                        !isSquareAttacked(board, 5, them) &&  // f1
                        !isSquareAttacked(board, 6, them)) {  // g1
                        moves.add(new Move(4, 6, Move.KING_CASTLE));
                    }
                }
            }
            }
            
            // White queen-side castling
            if (board.canCastle(BitBoard.WHITE_QUEEN_SIDE)) {
                Piece rookA1 = board.getPiece(0);
                if (rookA1 != null && rookA1.getType() == PieceType.ROOK && rookA1.isWhite()) {
                if ((occupied & 0x0EL) == 0) { // b1, c1, d1 empty
                    if (!isSquareAttacked(board, 4, them) &&  // e1
                        !isSquareAttacked(board, 3, them) &&  // d1
                        !isSquareAttacked(board, 2, them)) {  // c1
                        moves.add(new Move(4, 2, Move.QUEEN_CASTLE));
                    }
                }
            }
            }
        } else {
            // Black king-side castling
            if (board.canCastle(BitBoard.BLACK_KING_SIDE)) {
                Piece rookH8 = board.getPiece(63);
                if (rookH8 != null && rookH8.getType() == PieceType.ROOK && rookH8.isBlack()) {
                if ((occupied & 0x6000000000000000L) == 0) { // f8 and g8 empty
                    if (!isSquareAttacked(board, 60, them) &&  // e8
                        !isSquareAttacked(board, 61, them) &&  // f8
                        !isSquareAttacked(board, 62, them)) {  // g8
                        moves.add(new Move(60, 62, Move.KING_CASTLE));
                    }
                }
            }
            }
            
            // Black queen-side castling
            if (board.canCastle(BitBoard.BLACK_QUEEN_SIDE)) {
                Piece rookA8 = board.getPiece(56);
                if (rookA8 != null && rookA8.getType() == PieceType.ROOK && rookA8.isBlack()) {
                if ((occupied & 0x0E00000000000000L) == 0) { // b8, c8, d8 empty
                    if (!isSquareAttacked(board, 60, them) &&  // e8
                        !isSquareAttacked(board, 59, them) &&  // d8
                        !isSquareAttacked(board, 58, them)) {  // c8
                        moves.add(new Move(60, 58, Move.QUEEN_CASTLE));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Adds moves from a bitboard of target squares.
     */
    private static void addMovesFromBitboard(BitBoard board, List<Move> moves, int from, long targets) {
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;
            
            Piece captured = board.getPiece(to);
            if (captured != null) {
                moves.add(new Move(from, to, Move.CAPTURE, captured.getType()));
            } else {
                moves.add(new Move(from, to, Move.QUIET_MOVE));
            }
        }
    }
    
    /**
     * Gets bishop attacks using classical approach (can be replaced with magic bitboards).
     */
    private static long getBishopAttacks(int square, long occupied) {
        long attacks = 0L;
        
        // Northeast
        attacks |= rayAttacks(square, occupied, 9, ~FILE_H);
        // Northwest
        attacks |= rayAttacks(square, occupied, 7, ~FILE_A);
        // Southeast
        attacks |= rayAttacks(square, occupied, -7, ~FILE_H);
        // Southwest
        attacks |= rayAttacks(square, occupied, -9, ~FILE_A);
        
        return attacks;
    }
    
    /**
     * Gets rook attacks using classical approach (can be replaced with magic bitboards).
     */
    private static long getRookAttacks(int square, long occupied) {
        long attacks = 0L;
        
        // North
        attacks |= rayAttacks(square, occupied, 8, ~0L);
        // South
        attacks |= rayAttacks(square, occupied, -8, ~0L);
        // East
        attacks |= rayAttacks(square, occupied, 1, ~FILE_H);
        // West
        attacks |= rayAttacks(square, occupied, -1, ~FILE_A);
        
        return attacks;
    }
    
    /**
     * Generates attacks along a ray direction until hitting a piece or board edge.
     */
    private static long rayAttacks(int square, long occupied, int direction, long edgeMask) {
        long attacks = 0L;
        long piece = 1L << square;
        
        while (true) {
            // Check if we're at an edge before shifting
            if ((piece & edgeMask) == 0) break;
            
            // Shift the piece
            if (direction > 0) {
                piece <<= direction;
            } else {
                piece >>>= -direction;
            }
            
            // Check if we went off the board
            if (piece == 0) break;
            
            // Add this square to attacks
            attacks |= piece;
            
            // Stop if we hit another piece
            if ((piece & occupied) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Initializes precomputed attack tables for knights, kings, and pawns.
     */
    private static void initializeAttackTables() {
        for (int square = 0; square < 64; square++) {
            KNIGHT_ATTACKS[square] = computeKnightAttacks(square);
            KING_ATTACKS[square] = computeKingAttacks(square);
            WHITE_PAWN_ATTACKS[square] = computePawnAttacks(square, PieceColor.WHITE);
            BLACK_PAWN_ATTACKS[square] = computePawnAttacks(square, PieceColor.BLACK);
        }
    }
    
    /**
     * Computes knight attack bitboard for a square.
     */
    private static long computeKnightAttacks(int square) {
        long attacks = 0L;
        long piece = 1L << square;
        
        // All 8 knight moves with edge detection
        if ((piece & ~FILE_GH & ~RANK_8) != 0) attacks |= piece << 10;  // 2 up, 1 right
        if ((piece & ~FILE_AB & ~RANK_8) != 0) attacks |= piece << 6;   // 2 up, 1 left
        if ((piece & ~FILE_H & ~(RANK_7 | RANK_8)) != 0) attacks |= piece << 17;  // 1 up, 2 right
        if ((piece & ~FILE_A & ~(RANK_7 | RANK_8)) != 0) attacks |= piece << 15;  // 1 up, 2 left
        if ((piece & ~FILE_GH & ~RANK_1) != 0) attacks |= piece >>> 6;   // 2 down, 1 right
        if ((piece & ~FILE_AB & ~RANK_1) != 0) attacks |= piece >>> 10;  // 2 down, 1 left
        if ((piece & ~FILE_H & ~(RANK_1 | RANK_2)) != 0) attacks |= piece >>> 15; // 1 down, 2 right
        if ((piece & ~FILE_A & ~(RANK_1 | RANK_2)) != 0) attacks |= piece >>> 17; // 1 down, 2 left
        
        return attacks;
    }
    
    /**
     * Computes king attack bitboard for a square.
     */
    private static long computeKingAttacks(int square) {
        long attacks = 0L;
        long piece = 1L << square;
        
        // All 8 king moves with edge detection
        if ((piece & ~RANK_8) != 0) attacks |= piece << 8;   // North
        if ((piece & ~RANK_1) != 0) attacks |= piece >>> 8;  // South
        if ((piece & ~FILE_H) != 0) attacks |= piece << 1;   // East
        if ((piece & ~FILE_A) != 0) attacks |= piece >>> 1;  // West
        if ((piece & ~FILE_H & ~RANK_8) != 0) attacks |= piece << 9;  // NE
        if ((piece & ~FILE_A & ~RANK_8) != 0) attacks |= piece << 7;  // NW
        if ((piece & ~FILE_H & ~RANK_1) != 0) attacks |= piece >>> 7; // SE
        if ((piece & ~FILE_A & ~RANK_1) != 0) attacks |= piece >>> 9; // SW
        
        return attacks;
    }
    
    /**
     * Computes pawn attack bitboard for a square.
     */
    private static long computePawnAttacks(int square, PieceColor color) {
        long attacks = 0L;
        long piece = 1L << square;
        
        if (color == PieceColor.WHITE) {
            if ((piece & ~FILE_A & ~RANK_8) != 0) attacks |= piece << 7;  // NW
            if ((piece & ~FILE_H & ~RANK_8) != 0) attacks |= piece << 9;  // NE
        } else {
            if ((piece & ~FILE_A & ~RANK_1) != 0) attacks |= piece >>> 9; // SW
            if ((piece & ~FILE_H & ~RANK_1) != 0) attacks |= piece >>> 7; // SE
        }
        
        return attacks;
    }
    
    /**
     * Checks if the current position is checkmate.
     */
    public static boolean isCheckmate(BitBoard board) {
        if (!isInCheck(board)) return false;
        return generateLegalMoves(board).isEmpty();
    }
    
    /**
     * Checks if the current position is stalemate.
     */
    public static boolean isStalemate(BitBoard board) {
        if (isInCheck(board)) return false;
        return generateLegalMoves(board).isEmpty();
    }
    
    /**
     * Checks if the current side is in check.
     */
    public static boolean isInCheck(BitBoard board) {
        PieceColor us = board.getSideToMove();
        Piece ourKing = us == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
        List<Integer> kingSquares = board.getPieces(ourKing);
        
        if (kingSquares.isEmpty()) return false;
        
        int kingSquare = kingSquares.get(0);
        return isSquareAttacked(board, kingSquare, us.opposite());
    }
    
    /**
     * Pretty prints a bitboard (for debugging).
     */
    public static void printBitboard(long bitboard) {
        System.out.println("\n  a b c d e f g h");
        System.out.println("  ---------------");
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + "|");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                boolean bit = ((bitboard >>> square) & 1L) == 1L;
                System.out.print(bit ? "X " : ". ");
            }
            System.out.println("|" + (rank + 1));
        }
        System.out.println("  ---------------");
        System.out.println("  a b c d e f g h\n");
    }
}
