package move;

import board.BitBoard;

import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;

/**
 * Validates chess moves according to the rules of chess.
 * Ensures moves are legal (follow piece movement rules) and don't leave the king in check.
 */
public class MoveValidator {
    
    /**
     * Validates if a move is legal in the current position.
     * A move is legal if:
     * 1. The piece can move that way according to chess rules
     * 2. The move doesn't leave the player's own king in check
     * 
     * @param board The current board state
     * @param move The move to validate
     * @return true if the move is legal, false otherwise
     */
    public static boolean isMoveLegal(BitBoard board, Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        
        Piece piece = board.getPiece(from);
        
        // Check if there's a piece at the source square
        if (piece == null) {
            return false;
        }
        
        // Check if it's the correct player's turn
        if (piece.getColor() != board.getSideToMove()) {
            return false;
        }
        
        // Check if the piece can make this move according to its movement rules
        if (!isPseudoLegalMove(board, move)) {
            return false;
        }
        
        // Check if the move leaves the king in check (illegal)
        return !moveLeavesKingInCheck(board, move);
    }
    
    /**
     * Checks if a move is pseudo-legal (follows piece movement rules but may leave king in check).
     */
    public static boolean isPseudoLegalMove(BitBoard board, Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        
        Piece piece = board.getPiece(from);
        if (piece == null) {
            return false;
        }
        
        Piece targetPiece = board.getPiece(to);
        
        // Can't capture your own pieces (except for castling which is handled separately)
        if (targetPiece != null && targetPiece.getColor() == piece.getColor() && !move.isCastling()) {
            return false;
        }
        
        // Validate based on piece type
        switch (piece.getType()) {
            case PieceType.PAWN:
                return isValidPawnMove(board, move, piece);
            case PieceType.KNIGHT:
                return isValidKnightMove(from, to);
            case PieceType.BISHOP:
                return isValidBishopMove(board, from, to);
            case PieceType.ROOK:
                return isValidRookMove(board, from, to);
            case PieceType.QUEEN:
                return isValidQueenMove(board, from, to);
            case PieceType.KING:
                return isValidKingMove(board, move, piece);
            default:
                return false;
        }
    }
    
    /**
     * Checks if a move would leave the moving player's king in check.
     */
    public static boolean moveLeavesKingInCheck(BitBoard board, Move move) {
        // Make the move on a copy of the board
        BitBoard tempBoard = board.copy();
        tempBoard.makeMove(move);
        
        // Check if the player who just moved is now in check
        // (Note: after makeMove, sideToMove has switched, so we check the opposite color)
        PieceColor movedColor = board.getSideToMove();
        return CheckValidator.isKingInCheck(tempBoard, movedColor);
    }
    
    /**
     * Validates pawn moves.
     */
    private static boolean isValidPawnMove(BitBoard board, Move move, Piece pawn) {
        int from = move.getFrom();
        int to = move.getTo();
        int fromRank = from >>> 3;
        int toRank = to >>> 3;
        int fromFile = from & 7;
        int toFile = to & 7;
        
        int direction = pawn.isWhite() ? 1 : -1;
        int startRank = pawn.isWhite() ? 1 : 6;
        int promotionRank = pawn.isWhite() ? 7 : 0;
        
        Piece targetPiece = board.getPiece(to);
        
        // Single push forward
        if (fromFile == toFile && toRank == fromRank + direction && targetPiece == null) {
            if (toRank == promotionRank) {
                return move.isPromotion();
            }
            return !move.isPromotion();
        }
        
        // Double push forward from starting position
        if (fromFile == toFile && toRank == fromRank + 2 * direction && 
            fromRank == startRank && targetPiece == null) {
            int middleSquare = from + direction * 8;
            if (board.getPiece(middleSquare) == null) {
                return move.isDoublePawnPush();
            }
        }
        
        // Capture diagonally
        if (Math.abs(toFile - fromFile) == 1 && toRank == fromRank + direction) {
            // Regular capture
            if (targetPiece != null && targetPiece.getColor() != pawn.getColor()) {
                if (toRank == promotionRank) {
                    return move.isPromotion() && move.isCapture();
                }
                return move.isCapture() && !move.isPromotion();
            }
            
            // En passant capture
            if (to == board.getEnPassantSquare() && targetPiece == null) {
                return move.isEnPassant();
            }
        }
        
        return false;
    }
    
    /**
     * Validates knight moves.
     */
    private static boolean isValidKnightMove(int from, int to) {
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;
        
        int rankDiff = Math.abs(toRank - fromRank);
        int fileDiff = Math.abs(toFile - fromFile);
        
        return (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2);
    }
    
    /**
     * Validates bishop moves.
     */
    private static boolean isValidBishopMove(BitBoard board, int from, int to) {
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;
        
        int rankDiff = Math.abs(toRank - fromRank);
        int fileDiff = Math.abs(toFile - fromFile);
        
        // Must move diagonally
        if (rankDiff != fileDiff) {
            return false;
        }
        
        // Check if path is clear
        return isPathClear(board, from, to);
    }
    
    /**
     * Validates rook moves.
     */
    private static boolean isValidRookMove(BitBoard board, int from, int to) {
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;
        
        // Must move along rank or file
        if (fromRank != toRank && fromFile != toFile) {
            return false;
        }
        
        // Check if path is clear
        return isPathClear(board, from, to);
    }
    
    /**
     * Validates queen moves.
     */
    private static boolean isValidQueenMove(BitBoard board, int from, int to) {
        // Queen moves like a rook or a bishop
        return isValidRookMove(board, from, to) || isValidBishopMove(board, from, to);
    }
    
    /**
     * Validates king moves.
     */
    private static boolean isValidKingMove(BitBoard board, Move move, Piece king) {
        int from = move.getFrom();
        int to = move.getTo();
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;
        
        int rankDiff = Math.abs(toRank - fromRank);
        int fileDiff = Math.abs(toFile - fromFile);
        
        // Castling
        if (move.isCastling()) {
            return isValidCastling(board, move, king);
        }
        
        // Normal king move (one square in any direction)
        return rankDiff <= 1 && fileDiff <= 1 && (rankDiff + fileDiff > 0);
    }
    
    /**
     * Validates castling moves.
     */
    private static boolean isValidCastling(BitBoard board, Move move, Piece king) {
        PieceColor color = king.getColor();
        
        // King must not be in check
        if (CheckValidator.isKingInCheck(board, color)) {
            return false;
        }
        
        int kingSquare = move.getFrom();
        boolean isKingSide = move.getFlags() == Move.KING_CASTLE;
        
        if (color == PieceColor.WHITE) {
            if (kingSquare != 4) return false;
            
            if (isKingSide) {
                // White king-side castling
                if (!board.canCastle(BitBoard.WHITE_KING_SIDE)) return false;
                
                // Squares between king and rook must be empty
                if (board.isOccupied(5) || board.isOccupied(6)) return false;
                
                // King must not pass through or land on attacked squares
                if (CheckValidator.isSquareAttacked(board, 5, PieceColor.BLACK) ||
                    CheckValidator.isSquareAttacked(board, 6, PieceColor.BLACK)) {
                    return false;
                }
                
                // Rook must be present
                Piece rook = board.getPiece(7);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isWhite();
            } else {
                // White queen-side castling
                if (!board.canCastle(BitBoard.WHITE_QUEEN_SIDE)) return false;
                
                // Squares between king and rook must be empty
                if (board.isOccupied(1) || board.isOccupied(2) || board.isOccupied(3)) return false;
                
                // King must not pass through or land on attacked squares
                if (CheckValidator.isSquareAttacked(board, 3, PieceColor.BLACK) ||
                    CheckValidator.isSquareAttacked(board, 2, PieceColor.BLACK)) {
                    return false;
                }
                
                // Rook must be present
                Piece rook = board.getPiece(0);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isWhite();
            }
        } else {
            if (kingSquare != 60) return false;
            
            if (isKingSide) {
                // Black king-side castling
                if (!board.canCastle(BitBoard.BLACK_KING_SIDE)) return false;
                
                // Squares between king and rook must be empty
                if (board.isOccupied(61) || board.isOccupied(62)) return false;
                
                // King must not pass through or land on attacked squares
                if (CheckValidator.isSquareAttacked(board, 61, PieceColor.WHITE) ||
                    CheckValidator.isSquareAttacked(board, 62, PieceColor.WHITE)) {
                    return false;
                }
                
                // Rook must be present
                Piece rook = board.getPiece(63);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isBlack();
            } else {
                // Black queen-side castling
                if (!board.canCastle(BitBoard.BLACK_QUEEN_SIDE)) return false;
                
                // Squares between king and rook must be empty
                if (board.isOccupied(57) || board.isOccupied(58) || board.isOccupied(59)) return false;
                
                // King must not pass through or land on attacked squares
                if (CheckValidator.isSquareAttacked(board, 59, PieceColor.WHITE) ||
                    CheckValidator.isSquareAttacked(board, 58, PieceColor.WHITE)) {
                    return false;
                }
                
                // Rook must be present
                Piece rook = board.getPiece(56);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isBlack();
            }
        }
    }
    
    /**
     * Checks if the path between two squares is clear (no pieces blocking).
     */
    private static boolean isPathClear(BitBoard board, int from, int to) {
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;
        
        int rankStep = Integer.compare(toRank, fromRank);
        int fileStep = Integer.compare(toFile, fromFile);
        
        int currentRank = fromRank + rankStep;
        int currentFile = fromFile + fileStep;
        
        while (currentRank != toRank || currentFile != toFile) {
            int square = currentRank * 8 + currentFile;
            if (board.isOccupied(square)) {
                return false;
            }
            currentRank += rankStep;
            currentFile += fileStep;
        }
        
        return true;
    }
    
    /**
     * Generates all legal moves for the current position.
     * @param board The current board state
     * @return List of all legal moves
     */
    public static List<Move> generateLegalMoves(BitBoard board) {
        List<Move> legalMoves = new ArrayList<>();
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves(board);
        
        for (Move move : pseudoLegalMoves) {
            if (!moveLeavesKingInCheck(board, move)) {
                legalMoves.add(move);
            }
        }
        
        return legalMoves;
    }
    
    /**
     * Generates all pseudo-legal moves (moves that follow piece rules but may leave king in check).
     */
    public static List<Move> generatePseudoLegalMoves(BitBoard board) {
        List<Move> moves = new ArrayList<>();
        PieceColor color = board.getSideToMove();
        
        for (Piece piece : Piece.values()) {
            if (piece.getColor() == color) {
                List<Integer> pieceSquares = board.getPieces(piece);
                for (int from : pieceSquares) {
                    moves.addAll(generatePieceMoves(board, from, piece));
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Generates all pseudo-legal moves for a specific piece at a given square.
     */
    private static List<Move> generatePieceMoves(BitBoard board, int from, Piece piece) {
        List<Move> moves = new ArrayList<>();
        
        switch (piece.getType()) {
            case PieceType.PAWN:
                generatePawnMoves(board, from, piece, moves);
                break;
            case PieceType.KNIGHT:
                generateKnightMoves(board, from, piece, moves);
                break;
            case PieceType.BISHOP:
                generateBishopMoves(board, from, piece, moves);
                break;
            case PieceType.ROOK:
                generateRookMoves(board, from, piece, moves);
                break;
            case PieceType.QUEEN:
                generateQueenMoves(board, from, piece, moves);
                break;
            case PieceType.KING:
                generateKingMoves(board, from, piece, moves);
                break;
        }
        
        return moves;
    }
    
    /**
     * Generates pawn moves.
     */
    private static void generatePawnMoves(BitBoard board, int from, Piece pawn, List<Move> moves) {
        int rank = from >>> 3;
        int file = from & 7;
        int direction = pawn.isWhite() ? 1 : -1;
        int startRank = pawn.isWhite() ? 1 : 6;
        int promotionRank = pawn.isWhite() ? 7 : 0;
        
        // Single push
        int to = from + direction * 8;
        if (to >= 0 && to < 64 && board.isEmpty(to)) {
            if ((rank + direction) == promotionRank) {
                // Promotions
                moves.add(new Move(from, to, Move.QUEEN_PROMOTION));
                moves.add(new Move(from, to, Move.ROOK_PROMOTION));
                moves.add(new Move(from, to, Move.BISHOP_PROMOTION));
                moves.add(new Move(from, to, Move.KNIGHT_PROMOTION));
            } else {
                moves.add(new Move(from, to, Move.QUIET_MOVE));
            }
            
            // Double push
            if (rank == startRank) {
                to = from + 2 * direction * 8;
                if (board.isEmpty(to)) {
                    moves.add(new Move(from, to, Move.DOUBLE_PAWN_PUSH));
                }
            }
        }
        
        // Captures
        int[] captureFiles = {file - 1, file + 1};
        for (int captureFile : captureFiles) {
            if (captureFile >= 0 && captureFile < 8) {
                to = (rank + direction) * 8 + captureFile;
                if (to >= 0 && to < 64) {
                    Piece target = board.getPiece(to);
                    
                    if (target != null && target.getColor() != pawn.getColor()) {
                        if ((rank + direction) == promotionRank) {
                            // Capture promotions
                            moves.add(new Move(from, to, Move.QUEEN_PROMO_CAPTURE, target.getType()));
                            moves.add(new Move(from, to, Move.ROOK_PROMO_CAPTURE, target.getType()));
                            moves.add(new Move(from, to, Move.BISHOP_PROMO_CAPTURE, target.getType()));
                            moves.add(new Move(from, to, Move.KNIGHT_PROMO_CAPTURE, target.getType()));
                        } else {
                            moves.add(new Move(from, to, Move.CAPTURE, target.getType()));
                        }
                    } else if (to == board.getEnPassantSquare()) {
                        // En passant
                        moves.add(new Move(from, to, Move.EP_CAPTURE, PieceType.PAWN));
                    }
                }
            }
        }
    }
    
    /**
     * Generates knight moves.
     */
    private static void generateKnightMoves(BitBoard board, int from, Piece knight, List<Move> moves) {
        int rank = from >>> 3;
        int file = from & 7;
        
        int[][] offsets = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        
        for (int[] offset : offsets) {
            int toRank = rank + offset[0];
            int toFile = file + offset[1];
            
            if (toRank >= 0 && toRank < 8 && toFile >= 0 && toFile < 8) {
                int to = toRank * 8 + toFile;
                Piece target = board.getPiece(to);
                
                if (target == null) {
                    moves.add(new Move(from, to, Move.QUIET_MOVE));
                } else if (target.getColor() != knight.getColor()) {
                    moves.add(new Move(from, to, Move.CAPTURE, target.getType()));
                }
            }
        }
    }
    
    /**
     * Generates bishop moves.
     */
    private static void generateBishopMoves(BitBoard board, int from, Piece bishop, List<Move> moves) {
        generateSlidingMoves(board, from, bishop, moves, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
    }
    
    /**
     * Generates rook moves.
     */
    private static void generateRookMoves(BitBoard board, int from, Piece rook, List<Move> moves) {
        generateSlidingMoves(board, from, rook, moves, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
    }
    
    /**
     * Generates queen moves.
     */
    private static void generateQueenMoves(BitBoard board, int from, Piece queen, List<Move> moves) {
        generateSlidingMoves(board, from, queen, moves, 
            new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
    }
    
    /**
     * Generates sliding piece moves (bishop, rook, queen).
     */
    private static void generateSlidingMoves(BitBoard board, int from, Piece piece, List<Move> moves, int[][] directions) {
        int rank = from >>> 3;
        int file = from & 7;
        
        for (int[] dir : directions) {
            int r = rank + dir[0];
            int f = file + dir[1];
            
            while (r >= 0 && r < 8 && f >= 0 && f < 8) {
                int to = r * 8 + f;
                Piece target = board.getPiece(to);
                
                if (target == null) {
                    moves.add(new Move(from, to, Move.QUIET_MOVE));
                } else {
                    if (target.getColor() != piece.getColor()) {
                        moves.add(new Move(from, to, Move.CAPTURE, target.getType()));
                    }
                    break;
                }
                
                r += dir[0];
                f += dir[1];
            }
        }
    }
    
    /**
     * Generates king moves including castling.
     */
    private static void generateKingMoves(BitBoard board, int from, Piece king, List<Move> moves) {
        int rank = from >>> 3;
        int file = from & 7;
        
        // Normal king moves
        for (int dr = -1; dr <= 1; dr++) {
            for (int df = -1; df <= 1; df++) {
                if (dr == 0 && df == 0) continue;
                
                int toRank = rank + dr;
                int toFile = file + df;
                
                if (toRank >= 0 && toRank < 8 && toFile >= 0 && toFile < 8) {
                    int to = toRank * 8 + toFile;
                    Piece target = board.getPiece(to);
                    
                    if (target == null) {
                        moves.add(new Move(from, to, Move.QUIET_MOVE));
                    } else if (target.getColor() != king.getColor()) {
                        moves.add(new Move(from, to, Move.CAPTURE, target.getType()));
                    }
                }
            }
        }
        
        // Castling
        if (king.isWhite() && from == 4) {
            if (board.canCastle(BitBoard.WHITE_KING_SIDE)) {
                moves.add(new Move(4, 6, Move.KING_CASTLE));
            }
            if (board.canCastle(BitBoard.WHITE_QUEEN_SIDE)) {
                moves.add(new Move(4, 2, Move.QUEEN_CASTLE));
            }
        } else if (king.isBlack() && from == 60) {
            if (board.canCastle(BitBoard.BLACK_KING_SIDE)) {
                moves.add(new Move(60, 62, Move.KING_CASTLE));
            }
            if (board.canCastle(BitBoard.BLACK_QUEEN_SIDE)) {
                moves.add(new Move(60, 58, Move.QUEEN_CASTLE));
            }
        }
    }
}
