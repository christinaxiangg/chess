package search;

import board.BitBoard;
import move.Move;

/**
 * Test class demonstrating the OpeningBook functionality.
 */
public class OpeningBookTest {

    public static void main(String[] args) {
        System.out.println("=== OpeningBook Tests ===\n");

        testBookLoading();
        testStartingPosition();
        testCommonOpenings();
        testPositionNotInBook();
        testPolyglotKeyConsistency();
        testMoveDecoding();
        testWeightedRandomSelection();
    }


    /**
     * Test that the book file is loaded correctly.
     */
    private static void testBookLoading() {
        System.out.println("2. Book Loading Test");
        System.out.println("---------------------");

        OpeningBook book = OpeningBook.getInstance();
        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        if (book.isLoaded()) {
            System.out.println("PASS: Book loaded successfully");
        } else {
            System.out.println("FAIL: Book appears empty");
        }
        System.out.println();
    }

    /**
     * Test book probe from the starting position.
     */
    private static void testStartingPosition() {
        System.out.println("3. Starting Position Test");
        System.out.println("-------------------------");

        BitBoard board = BitBoard.startingPosition();
        OpeningBook book = OpeningBook.getInstance();

        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        System.out.println("Position: Starting position");
        board.print();

        Move move = book.probe(board);
        if (move != null) {
            System.out.println("PASS: Book move found: " + move.toUCI());
            System.out.println("Move from: " + Move.squareToString(move.getFrom()));
            System.out.println("Move to: " + Move.squareToString(move.getTo()));
        } else {
            System.out.println("INFO: No book move found (book may not contain starting position)");
        }
        System.out.println();
    }

    /**
     * Test book probes for common opening positions.
     */
    private static void testCommonOpenings() {
        System.out.println("4. Common Openings Test");
        System.out.println("-----------------------");

        OpeningBook book = OpeningBook.getInstance();
        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        // Test Italian Game
        System.out.println("\nPosition: Italian Game (1.e4 e5 2.Nf3 Nc6 3.Bc4)");
        BitBoard italian = BitBoard.fromFEN("r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3");
        italian.print();
        Move italianMove = book.probe(italian);
        if (italianMove != null) {
            System.out.println("Book move: " + italianMove.toUCI());
        } else {
            System.out.println("No book move found");
        }

        // Test Sicilian Defense
        System.out.println("\nPosition: Sicilian Defense (1.e4 c5)");
        BitBoard sicilian = BitBoard.fromFEN("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");
        sicilian.print();
        Move sicilianMove = book.probe(sicilian);
        if (sicilianMove != null) {
            System.out.println("Book move: " + sicilianMove.toUCI());
        } else {
            System.out.println("No book move found");
        }

        // Test Ruy Lopez
        System.out.println("\nPosition: Ruy Lopez (1.e4 e5 2.Nf3 Nc6 3.Bb5)");
        BitBoard ruy = BitBoard.fromFEN("r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3");
        ruy.print();
        Move ruyMove = book.probe(ruy);
        if (ruyMove != null) {
            System.out.println("Book move: " + ruyMove.toUCI());
        } else {
            System.out.println("No book move found");
        }
        System.out.println();
    }

    /**
     * Test that positions not in the book return null.
     */
    private static void testPositionNotInBook() {
        System.out.println("5. Position Not in Book Test");
        System.out.println("------------------------------");

        OpeningBook book = OpeningBook.getInstance();
        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        // A random endgame position unlikely to be in the book
        System.out.println("\nPosition: Endgame (unlikely to be in book)");
        BitBoard endgame = BitBoard.fromFEN("8/8/8/8/8/5k2/3r4/4K3 w - - 0 1");
        endgame.print();

        Move move = book.probe(endgame);
        if (move == null) {
            System.out.println("PASS: Correctly returned null for position not in book");
        } else {
            System.out.println("FAIL: Unexpectedly found a book move: " + move.toUCI());
        }
        System.out.println();
    }

    /**
     * Test that Polyglot key computation is consistent.
     */
    private static void testPolyglotKeyConsistency() {
        System.out.println("6. Polyglot Key Consistency Test");
        System.out.println("---------------------------------");

        BitBoard board = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        long key1 = OpeningBook.polyglotKey(board);
        long key2 = OpeningBook.polyglotKey(board);

        if (key1 == key2) {
            System.out.println("PASS: Polyglot key is consistent");
            System.out.println("Key: 0x" + Long.toHexString(key1));
        } else {
            System.out.println("FAIL: Polyglot keys differ");
            System.out.println("Key1: 0x" + Long.toHexString(key1));
            System.out.println("Key2: 0x" + Long.toHexString(key2));
        }

        // Test that different positions have different keys
        BitBoard board2 = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        long key3 = OpeningBook.polyglotKey(board2);

        if (key1 != key3) {
            System.out.println("PASS: Different positions have different keys");
            System.out.println("Key for e4: 0x" + Long.toHexString(key1));
            System.out.println("Key for e3: 0x" + Long.toHexString(key3));
        } else {
            System.out.println("FAIL: Different positions have same key");
        }
        System.out.println();
    }

    /**
     * Test move decoding from Polyglot format.
     */
    private static void testMoveDecoding() {
        System.out.println("7. Move Decoding Test");
        System.out.println("---------------------");

        OpeningBook book = OpeningBook.getInstance();
        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        // Test with a position that should have book moves
        BitBoard board = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

        Move move = book.probe(board);
        if (move != null) {
            System.out.println("PASS: Successfully decoded move: " + move.toUCI());
            System.out.println("From square: " + move.getFrom() + " (" + Move.squareToString(move.getFrom()) + ")");
            System.out.println("To square: " + move.getTo() + " (" + Move.squareToString(move.getTo()) + ")");
            if (move.isPromotion()) {
                System.out.println("Promotion piece: " + move.getPromotionPieceType());
            }
        } else {
            System.out.println("INFO: No book move found to test decoding");
        }
        System.out.println();
    }

    /**
     * Test that weighted random selection works over multiple probes.
     */
    private static void testWeightedRandomSelection() {
        System.out.println("8. Weighted Random Selection Test");
        System.out.println("----------------------------------");

        OpeningBook book = OpeningBook.getInstance();
        if (book == null) {
            System.out.println("FAIL: Could not get book instance");
            return;
        }

        BitBoard board = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

        System.out.println("Testing 10 consecutive probes to check for variety:");
        java.util.Set<String> moves = new java.util.HashSet<>();

        for (int i = 0; i < 10; i++) {
            Move move = book.probe(board);
            if (move != null) {
                String moveStr = move.toUCI();
                moves.add(moveStr);
                System.out.println("Probe " + (i+1) + ": " + moveStr);
            } else {
                System.out.println("Probe " + (i+1) + ": No move found");
            }
        }

        if (moves.size() > 1) {
            System.out.println("PASS: Found " + moves.size() + " different moves (weighted random working)");
        } else if (moves.size() == 1) {
            System.out.println("INFO: Only one move in book for this position");
        } else {
            System.out.println("INFO: No moves found in book for this position");
        }
        System.out.println();
    }
}
