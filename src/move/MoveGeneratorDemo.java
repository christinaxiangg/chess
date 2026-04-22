package move;

import board.BitBoard;

import java.util.List;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;
/**
 * Demonstrates and tests the move generation functionality.
 */
public class MoveGeneratorDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Chess move.Move Generator Demo ===\n");
        
        // Test 1: Starting position
        System.out.println("Test 1: Starting Position move.Move Generation");
        System.out.println("==========================================");
        BitBoard startPos = BitBoard.startingPosition();
        startPos.print();
        
        List<Move> moves = MoveGenerator.generateLegalMoves(startPos);
        System.out.println("Legal moves: " + moves.size() + " (should be 20)");
        System.out.println("Moves: ");
        printMovesByPiece(startPos, moves);
        
        // Test 2: After 1.e4
        System.out.println("\n\nTest 2: After 1.e4");
        System.out.println("==================");
        BitBoard afterE4 = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        afterE4.print();
        
        moves = MoveGenerator.generateLegalMoves(afterE4);
        System.out.println("Legal moves for Black: " + moves.size() + " (should be 20)");
        
        // Test 3: Tactical position with pins and checks
        System.out.println("\n\nTest 3: Tactical Position (Scholar's Mate Threat)");
        System.out.println("==================================================");
        BitBoard tactical = BitBoard.fromFEN("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4");
        tactical.print();
        
        boolean inCheck = MoveGenerator.isInCheck(tactical);
        System.out.println("Black in check: " + inCheck);
        
        moves = MoveGenerator.generateLegalMoves(tactical);
        System.out.println("Legal moves: " + moves.size());
        if (moves.isEmpty()) {
            System.out.println("CHECKMATE!");
        } else {
            for (Move move : moves) {
                System.out.println("  " + move.toUCI());
            }
        }
        
        // Test 4: Promotion
        System.out.println("\n\nTest 4: Pawn Promotion");
        System.out.println("=======================");
        BitBoard promotion = BitBoard.fromFEN("4k3/4P3/8/8/8/8/8/4K3 w - - 0 1");
        promotion.print();
        
        moves = MoveGenerator.generateLegalMoves(promotion);
        System.out.println("Legal moves: " + moves.size());
        for (Move move : moves) {
            if (move.isPromotion()) {
                System.out.println("  " + move.toUCI() + " (promotion to " + 
                    move.getPromotionPieceType() + ")");
            }
        }
        
        // Test 5: En Passant
        System.out.println("\n\nTest 5: En Passant Capture");
        System.out.println("===========================");
        BitBoard enPassant = BitBoard.fromFEN("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        enPassant.print();
        
        moves = MoveGenerator.generateLegalMoves(enPassant);
        System.out.println("Legal moves: " + moves.size());
        for (Move move : moves) {
            if (move.isEnPassant()) {
                System.out.println("  " + move.toUCI() + " (en passant capture!)");
            }
        }
        
        // Test 6: Castling
        System.out.println("\n\nTest 6: Castling Available");
        System.out.println("===========================");
        BitBoard castling = BitBoard.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        castling.print();
        
        moves = MoveGenerator.generateLegalMoves(castling);
        System.out.println("Legal moves for White: " + moves.size());
        for (Move move : moves) {
            if (move.isCastling()) {
                String side = move.getFlags() == Move.KING_CASTLE ? "king-side" : "queen-side";
                System.out.println("  " + move.toUCI() + " (" + side + " castling)");
            }
        }
        
        // Test 7: Perft (performance test and move generation verification)
        System.out.println("\n\nTest 7: Perft Test (move.Move Generation Verification)");
        System.out.println("==================================================");
        BitBoard perftPos = BitBoard.startingPosition();
        
        System.out.println("Running perft to depth 3...");
        long startTime = System.currentTimeMillis();
        long nodes = perft(perftPos, 3);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("Nodes searched: " + nodes + " (should be 8,902)");
        System.out.println("Time: " + elapsed + "ms");
        System.out.println("Nodes per second: " + (nodes * 1000 / Math.max(elapsed, 1)));
        
        // Test 8: Check detection
        System.out.println("\n\nTest 8: Check Detection");
        System.out.println("========================");
        BitBoard checkPos = BitBoard.fromFEN("rnb1kbnr/pppp1ppp/8/4p3/5PPq/8/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        checkPos.print();
        
        System.out.println("White in check: " + MoveGenerator.isInCheck(checkPos));
        moves = MoveGenerator.generateLegalMoves(checkPos);
        System.out.println("Legal moves: " + moves.size());
        System.out.println("King must move or block:");
        for (Move move : moves) {
            Piece piece = checkPos.getPiece(move.getFrom());
            System.out.println("  " + piece.getSymbol() + " " + move.toUCI());
        }
        
        // Test 9: Stalemate
        System.out.println("\n\nTest 9: Stalemate Detection");
        System.out.println("============================");
        BitBoard stalemate = BitBoard.fromFEN("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        stalemate.print();
        
        System.out.println("Black in check: " + MoveGenerator.isInCheck(stalemate));
        System.out.println("Is stalemate: " + MoveGenerator.isStalemate(stalemate));
        moves = MoveGenerator.generateLegalMoves(stalemate);
        System.out.println("Legal moves: " + moves.size());
        
        // Test 10: Square attack detection
        System.out.println("\n\nTest 10: Square Attack Detection");
        System.out.println("=================================");
        BitBoard attackTest = BitBoard.startingPosition();
        
        System.out.println("Is e4 attacked by black? " + 
            MoveGenerator.isSquareAttacked(attackTest, Move.stringToSquare("e4"), PieceColor.BLACK));
        System.out.println("Is e5 attacked by white? " + 
            MoveGenerator.isSquareAttacked(attackTest, Move.stringToSquare("e5"), PieceColor.WHITE));
        System.out.println("Is d4 attacked by black? " + 
            MoveGenerator.isSquareAttacked(attackTest, Move.stringToSquare("d4"), PieceColor.BLACK));
    }
    
    /**
     * Prints moves grouped by piece type.
     */
    private static void printMovesByPiece(BitBoard board, List<Move> moves) {
        System.out.println("Pawn moves:");
        for (Move move : moves) {
            Piece piece = board.getPiece(move.getFrom());
            if (piece != null && piece.getType() == PieceType.PAWN) {
                System.out.print(move.toUCI() + " ");
            }
        }
        
        System.out.println("\nKnight moves:");
        for (Move move : moves) {
            Piece piece = board.getPiece(move.getFrom());
            if (piece != null && piece.getType() == PieceType.KNIGHT) {
                System.out.print(move.toUCI() + " ");
            }
        }
        System.out.println();
    }
    
    /**
     * Perft (performance test) - counts leaf nodes at a given depth.
     * Used to verify move generation correctness.
     */
    private static long perft(BitBoard board, int depth) {
        if (depth == 0) return 1;
        
        long nodes = 0;
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        
        for (Move move : moves) {
            BitBoard copy = board.copy();
            copy.makeMove(move);
            nodes += perft(copy, depth - 1);
        }
        
        return nodes;
    }
    
    /**
     * Perft with move breakdown (divide) - shows nodes for each root move.
     */
    private static void perftDivide(BitBoard board, int depth) {
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        long totalNodes = 0;
        
        for (Move move : moves) {
            BitBoard copy = board.copy();
            copy.makeMove(move);
            long nodes = perft(copy, depth - 1);
            totalNodes += nodes;
            System.out.println(move.toUCI() + ": " + nodes);
        }
        
        System.out.println("\nTotal: " + totalNodes);
    }
}
