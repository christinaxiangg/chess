package search;

import board.BitBoard;
import move.Move;
import piece.Piece;
import piece.PieceColor;

/**
 * Demonstration of the board.BitBoard chess engine usage.
 */
public class ChessDemo {
    public static void main(String[] args) {
        System.out.println("=== Chess board.BitBoard Demo ===\n");
        
        // Example 1: Create starting position
        System.out.println("1. Starting Position:");
        BitBoard board = BitBoard.startingPosition();
        board.print();
        
        // Example 2: Array-like access to pieces
        System.out.println("\n2. Array-like Access:");
        Piece e2Piece = board.getPiece(4, 1); // e2 square
        System.out.println("piece.Piece at e2: " + (e2Piece != null ? e2Piece.getSymbol() : "empty"));
        
        Piece e4Piece = board.getPiece(4, 3); // e4 square
        System.out.println("piece.Piece at e4: " + (e4Piece != null ? e4Piece.getSymbol() : "empty"));
        
        // Example 3: Making moves
        System.out.println("\n3. Making Moves:");
        Move e4 = new Move(Move.stringToSquare("e2"), Move.stringToSquare("e4"), Move.DOUBLE_PAWN_PUSH);
        System.out.println("Playing e2e4: " + e4.toUCI());
        board.makeMove(e4);
        board.print();
        
        Move e5 = new Move(Move.stringToSquare("e7"), Move.stringToSquare("e5"), Move.DOUBLE_PAWN_PUSH);
        System.out.println("Playing e7e5: " + e5.toUCI());
        board.makeMove(e5);
        board.print();
        
        // Example 4: piece.Piece tracking - get all white pawns
        System.out.println("\n4. piece.Piece Tracking:");
        var whitePawns = board.getPieces(Piece.WHITE_PAWN);
        System.out.println("White pawn squares: ");
        for (int square : whitePawns) {
            System.out.print(Move.squareToString(square) + " ");
        }
        System.out.println();
        
        // Example 5: Get all white pieces
        var whitePieces = board.getPieces(PieceColor.WHITE);
        System.out.println("\nAll white piece squares: " + whitePieces.size() + " pieces");
        
        // Example 6: Create custom position from FEN
        System.out.println("\n5. Custom Position from FEN:");
        String sicilianFEN = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
        BitBoard sicilian = BitBoard.fromFEN(sicilianFEN);
        sicilian.print();
        
        // Example 7: Board copy
        System.out.println("6. Board Copy:");
        BitBoard boardCopy = board.copy();
        System.out.println("Original and copy are independent: " + (board != boardCopy));
        System.out.println("But have same position: " + board.toFEN().equals(boardCopy.toFEN()));
        
        // Example 8: Bitboard access
        System.out.println("\n7. Bitboard Access:");
        long whitePawnBitboard = board.getBitboard(Piece.WHITE_PAWN);
        System.out.println("White pawn bitboard: 0x" + Long.toHexString(whitePawnBitboard));
        System.out.println("Number of white pawns: " + Long.bitCount(whitePawnBitboard));
        
        // Example 9: Check square occupancy
        System.out.println("\n8. Square Occupancy:");
        System.out.println("Is e4 occupied? " + board.isOccupied(Move.stringToSquare("e4")));
        System.out.println("Is e3 occupied? " + board.isOccupied(Move.stringToSquare("e3")));
        
        // Example 10: Castling and game state
        System.out.println("\n9. Game State:");
        System.out.println("Side to move: " + board.getSideToMove());
        System.out.println("Can white castle kingside? " + board.canCastle(BitBoard.WHITE_KING_SIDE));
        System.out.println("En passant square: " + 
            (board.getEnPassantSquare() == -1 ? "none" : Move.squareToString(board.getEnPassantSquare())));
        System.out.println("Half move clock: " + board.getHalfMoveClock());
        System.out.println("Full move number: " + board.getFullMoveNumber());
        
        // Example 11: move.Move parsing from UCI
        System.out.println("\n10. UCI move.Move Parsing:");
        Move uciMove = Move.fromUCI("g1f3");
        System.out.println("Parsed move from 'g1f3': from=" + Move.squareToString(uciMove.getFrom()) + 
                          " to=" + Move.squareToString(uciMove.getTo()));
        
        // Example 12: Promotion move
        System.out.println("\n11. Promotion move.Move Example:");
        BitBoard promoBoard = BitBoard.fromFEN("8/4P3/8/8/8/8/8/4k2K w - - 0 1");
        promoBoard.print();
        Move promotion = new Move(Move.stringToSquare("e7"), Move.stringToSquare("e8"), Move.QUEEN_PROMOTION);
        System.out.println("Playing e7e8q (promotion to queen)");
        promoBoard.makeMove(promotion);
        promoBoard.print();
    }
}
