package move;

import board.BitBoard;
import piece.PieceColor;
import piece.PieceType;

/**
 * Test class demonstrating the usage of move.CheckValidator and move.MoveValidator.
 */
public class ValidatorTest {
    
    public static void main(String[] args) {
        System.out.println("=== Chess move.Move Validator Tests ===\n");
        
        testCheckDetection();
        testMoveValidation();
        testCastling();
        testEnPassant();
        testPinDetection();
        testLegalMoveGeneration();
    }
    
    /**
     * Test check detection.
     */
    private static void testCheckDetection() {
        System.out.println("1. Check Detection Test");
        System.out.println("------------------------");
        
        // Test position: White king in check by black queen
        BitBoard board = BitBoard.fromFEN("4k3/8/8/8/8/8/4q3/4K3 w - - 0 1");
        board.print();
        
        boolean whiteInCheck = CheckValidator.isKingInCheck(board, PieceColor.WHITE);
        System.out.println("White king in check: " + whiteInCheck);
        System.out.println("Expected: true");
        System.out.println("Result: " + (whiteInCheck ? "PASS" : "FAIL") + "\n");
        
        // Test position: Neither king in check
        board = BitBoard.startingPosition();
        System.out.println("Starting position:");
        board.print();
        
        whiteInCheck = CheckValidator.isKingInCheck(board, PieceColor.WHITE);
        boolean blackInCheck = CheckValidator.isKingInCheck(board, PieceColor.BLACK);
        System.out.println("White king in check: " + whiteInCheck);
        System.out.println("Black king in check: " + blackInCheck);
        System.out.println("Expected: false, false");
        System.out.println("Result: " + (!whiteInCheck && !blackInCheck ? "PASS" : "FAIL") + "\n");
    }
    
    /**
     * Test basic move validation.
     */
    private static void testMoveValidation() {
        System.out.println("2. move.Move Validation Test");
        System.out.println("------------------------");
        
        BitBoard board = BitBoard.startingPosition();
        
        // Test valid move: e2-e4
        Move validMove = Move.fromUCI("e2e4");
        boolean isValid = MoveValidator.isMoveLegal(board, validMove);
        System.out.println("move.Move e2-e4 is legal: " + isValid);
        System.out.println("Expected: true");
        System.out.println("Result: " + (isValid ? "PASS" : "FAIL") + "\n");
        
        // Test invalid move: e2-e5 (pawn can't move 3 squares)
        Move invalidMove = Move.fromUCI("e2e5");
        isValid = MoveValidator.isMoveLegal(board, invalidMove);
        System.out.println("move.Move e2-e5 is legal: " + isValid);
        System.out.println("Expected: false");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
        
        // Test invalid move: e1-e2 (king blocked by pawn)
        invalidMove = Move.fromUCI("e1e2");
        isValid = MoveValidator.isMoveLegal(board, invalidMove);
        System.out.println("move.Move e1-e2 is legal: " + isValid);
        System.out.println("Expected: false");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
    }
    
    /**
     * Test castling validation.
     */
    private static void testCastling() {
        System.out.println("3. Castling Test");
        System.out.println("----------------");
        
        // Position where white can castle kingside
        BitBoard board = BitBoard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1");
        board.print();
        
        Move kingSideCastle = new Move(4, 6, Move.KING_CASTLE);
        boolean isValid = MoveValidator.isMoveLegal(board, kingSideCastle);
        System.out.println("White kingside castling is legal: " + isValid);
        System.out.println("Expected: true");
        System.out.println("Result: " + (isValid ? "PASS" : "FAIL") + "\n");
        
        // Position where castling is illegal (king in check)
        board = BitBoard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K1rR w KQkq - 0 1");
        board.print();
        
        kingSideCastle = new Move(4, 6, Move.KING_CASTLE);
        isValid = MoveValidator.isMoveLegal(board, kingSideCastle);
        System.out.println("White kingside castling (in check) is legal: " + isValid);
        System.out.println("Expected: false");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
        
        // Position where castling would pass through check
        board = BitBoard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2r w KQkq - 0 1");
        board.print();
        
        kingSideCastle = new Move(4, 6, Move.KING_CASTLE);
        isValid = MoveValidator.isMoveLegal(board, kingSideCastle);
        System.out.println("White kingside castling (through check) is legal: " + isValid);
        System.out.println("Expected: false");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
    }
    
    /**
     * Test en passant validation.
     */
    private static void testEnPassant() {
        System.out.println("4. En Passant Test");
        System.out.println("------------------");
        
        // Position with en passant available
        BitBoard board = BitBoard.fromFEN("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 1");
        board.print();
        
        Move enPassant = new Move(Move.stringToSquare("e5"), Move.stringToSquare("f6"), Move.EP_CAPTURE, PieceType.PAWN);
        boolean isValid = MoveValidator.isMoveLegal(board, enPassant);
        System.out.println("En passant e5xf6 is legal: " + isValid);
        System.out.println("Expected: true");
        System.out.println("Result: " + (isValid ? "PASS" : "FAIL") + "\n");
    }
    
    /**
     * Test pinned piece detection.
     */
    private static void testPinDetection() {
        System.out.println("5. Pinned piece.Piece Test");
        System.out.println("--------------------");
        
        // Position where white knight on f3 is pinned by black bishop on c6
        BitBoard board = BitBoard.fromFEN("rnbqk2r/pppp1ppp/2b5/4p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 1");
        board.print();
        
        // Try to move the pinned knight
        Move pinnedMove = Move.fromUCI("f3g5");
        boolean isValid = MoveValidator.isMoveLegal(board, pinnedMove);
        System.out.println("Moving pinned knight f3-g5 is legal: " + isValid);
        System.out.println("Expected: false (would expose king to check)");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
        
        // Try to move the pinned knight along the pin line
        Move alongPinLine = Move.fromUCI("f3d4");
        isValid = MoveValidator.isMoveLegal(board, alongPinLine);
        System.out.println("Moving pinned knight f3-d4 is legal: " + isValid);
        System.out.println("Expected: false (knight doesn't move along diagonals)");
        System.out.println("Result: " + (!isValid ? "PASS" : "FAIL") + "\n");
    }
    
    /**
     * Test legal move generation.
     */
    private static void testLegalMoveGeneration() {
        System.out.println("6. Legal move.Move Generation Test");
        System.out.println("------------------------------");
        
        BitBoard board = BitBoard.startingPosition();
        
        java.util.List<Move> legalMoves = MoveValidator.generateLegalMoves(board);
        System.out.println("Legal moves from starting position: " + legalMoves.size());
        System.out.println("Expected: 20");
        System.out.println("Result: " + (legalMoves.size() == 20 ? "PASS" : "FAIL"));
        
        System.out.println("\nAll legal moves:");
        for (Move move : legalMoves) {
            System.out.println("  " + move.toUCI());
        }
        System.out.println();
        
        // Test a position with fewer legal moves
        board = BitBoard.fromFEN("4k3/8/8/8/8/8/4R3/4K3 b - - 0 1");
        board.print();
        
        legalMoves = MoveValidator.generateLegalMoves(board);
        System.out.println("Legal moves for black (king in check): " + legalMoves.size());
        System.out.println("Black must move king out of check");
        System.out.println("\nLegal moves:");
        for (Move move : legalMoves) {
            System.out.println("  " + move.toUCI());
        }
        System.out.println();
    }
}
