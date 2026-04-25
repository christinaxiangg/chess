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

        board.makeMove(move);
        
        // Check if the player who just moved is now in check.
        // After makeMove, sideToMove has switched, so we check the color that just moved.
        PieceColor movedColor = board.getSideToMove();
        boolean isInCheck =  CheckValidator.isKingInCheck(board, movedColor);
        // Undo the move to restore original board state
        board.undoMakeMove(move);
        return isInCheck;
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
            
            // En passant capture: EP square is valid (not -1) and matches destination
            int epSquare = board.getEnPassantSquare();
            if (epSquare != -1 && to == epSquare && targetPiece == null) {
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
        if (rankDiff != fileDiff || rankDiff == 0) {
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
        
        // Must move along rank or file, and must actually move
        if (from == to) return false;
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
        // Castling is fully validated separately
        if (move.isCastling()) {
            return isValidCastling(board, move, king);
        }

        int from = move.getFrom();
        int to = move.getTo();
        int fromRank = from >>> 3;
        int fromFile = from & 7;
        int toRank = to >>> 3;
        int toFile = to & 7;

        int rankDiff = Math.abs(toRank - fromRank);
        int fileDiff = Math.abs(toFile - fromFile);

        // Normal king move: exactly one square in any direction
        return rankDiff <= 1 && fileDiff <= 1 && (rankDiff + fileDiff > 0);
    }
    
    /**
     * Validates castling moves, enforcing all FIDE rules:
     * - King must not be in check
     * - King must not pass through or land on an attacked square
     * - All squares between king and rook must be empty
     * - The appropriate rook must be present on its starting square
     * - Castling rights must still be available
     */
    private static boolean isValidCastling(BitBoard board, Move move, Piece king) {
        PieceColor color = king.getColor();
        PieceColor opponent = color.opposite();

        // King must not currently be in check
        if (CheckValidator.isKingInCheck(board, color)) {
            return false;
        }

        int kingSquare = move.getFrom();
        boolean isKingSide = move.getFlags() == Move.KING_CASTLE;

        if (color == PieceColor.WHITE) {
            if (kingSquare != 4) return false;

            if (isKingSide) {
                // White kingside: king e1→g1, rook h1→f1
                if (!board.canCastle(BitBoard.WHITE_KING_SIDE)) return false;

                // f1(5) and g1(6) must be empty
                if (board.isOccupied(5) || board.isOccupied(6)) return false;

                // King must not pass through f1 or land on g1 while either is attacked
                if (CheckValidator.isSquareAttacked(board, 5, opponent) ||
                    CheckValidator.isSquareAttacked(board, 6, opponent)) {
                    return false;
                }

                // Rook must be present on h1
                Piece rook = board.getPiece(7);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isWhite();

            } else {
                // White queenside: king e1→c1, rook a1→d1
                if (!board.canCastle(BitBoard.WHITE_QUEEN_SIDE)) return false;

                // b1(1), c1(2), d1(3) must be empty
                if (board.isOccupied(1) || board.isOccupied(2) || board.isOccupied(3)) return false;

                // King passes through d1(3) and lands on c1(2) — neither may be attacked
                if (CheckValidator.isSquareAttacked(board, 3, opponent) ||
                    CheckValidator.isSquareAttacked(board, 2, opponent)) {
                    return false;
                }

                // Rook must be present on a1
                Piece rook = board.getPiece(0);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isWhite();
            }

        } else {
            if (kingSquare != 60) return false;

            if (isKingSide) {
                // Black kingside: king e8→g8, rook h8→f8
                if (!board.canCastle(BitBoard.BLACK_KING_SIDE)) return false;

                // f8(61) and g8(62) must be empty
                if (board.isOccupied(61) || board.isOccupied(62)) return false;

                // King must not pass through f8 or land on g8 while either is attacked
                if (CheckValidator.isSquareAttacked(board, 61, opponent) ||
                    CheckValidator.isSquareAttacked(board, 62, opponent)) {
                    return false;
                }

                // Rook must be present on h8
                Piece rook = board.getPiece(63);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isBlack();

            } else {
                // Black queenside: king e8→c8, rook a8→d8
                if (!board.canCastle(BitBoard.BLACK_QUEEN_SIDE)) return false;

                // b8(57), c8(58), d8(59) must be empty
                if (board.isOccupied(57) || board.isOccupied(58) || board.isOccupied(59)) return false;

                // King passes through d8(59) and lands on c8(58) — neither may be attacked
                if (CheckValidator.isSquareAttacked(board, 59, opponent) ||
                    CheckValidator.isSquareAttacked(board, 58, opponent)) {
                    return false;
                }

                // Rook must be present on a8
                Piece rook = board.getPiece(56);
                return rook != null && rook.getType() == PieceType.ROOK && rook.isBlack();
            }
        }
    }
    
    /**
     * Checks if the path between two squares is clear (no pieces blocking).
     * Does not check the source or destination square, only the squares in between.
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
    

    

    

}
