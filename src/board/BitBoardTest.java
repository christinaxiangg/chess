
package board;

import piece.Piece;
import piece.PieceColor;
import piece.PieceType;
import move.Move;

/**
 * Test class for BitBoard that prints the initial board and gets material values from Evaluator.
 */
public class BitBoardTest {

    public static void main(String[] args) {
        // Create a new board with the starting position
        BitBoard board = BitBoard.startingPosition();

        // Print the initial board
        System.out.println("=== Initial Chess Board ===");
        printBoard(board);

        // Print the FEN representation
        System.out.println("\nFEN: " + board.toFEN());

        // Get and print material values
        System.out.println("\n=== Material Values ===");
        printMaterialValues(board);

        // Get and print the evaluation score
        int score = Evaluator.evaluate(board);
        System.out.println("\n=== Evaluation Score ===");
        System.out.println("Total Score: " + score + " centipawns");
        System.out.println("(Positive favors White, Negative favors Black)");
    }

    /**
     * Prints the chess board in a visual format.
     */
    private static void printBoard(BitBoard board) {
        System.out.println("  a b c d e f g h");
        System.out.println("  ----------------");

        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + "|");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                Piece piece = board.getPiece(square);

                if (piece == null) {
                    System.out.print("  ");
                } else {
                    System.out.print(piece.getSymbol() + " ");
                }
            }
            System.out.println("|" + (rank + 1));
        }

        System.out.println("  ----------------");
        System.out.println("  a b c d e f g h");
    }

    /**
     * Prints the material values for each piece type and color.
     */
    private static void printMaterialValues(BitBoard board) {
        int whiteMaterial = 0;
        int blackMaterial = 0;

        System.out.println("\nWhite Pieces:");
        for (Piece piece : Piece.values()) {
            if (piece.isWhite()) {
                int count = board.getPieces(piece).size();
                int value = piece.getValue();
                int totalValue = count * value;
                whiteMaterial += totalValue;

                System.out.printf("  %-8s: %2d pieces x %4d = %4d centipawns\n", 
                    piece.getType(), count, value, totalValue);
            }
        }

        System.out.println("\nBlack Pieces:");
        for (Piece piece : Piece.values()) {
            if (piece.isBlack()) {
                int count = board.getPieces(piece).size();
                int value = piece.getValue();
                int totalValue = count * value;
                blackMaterial += totalValue;

                System.out.printf("  %-8s: %2d pieces x %4d = %4d centipawns\n", 
                    piece.getType(), count, value, totalValue);
            }
        }

        System.out.println("\nMaterial Balance:");
        System.out.println("  White Total: " + whiteMaterial + " centipawns");
        System.out.println("  Black Total: " + blackMaterial + " centipawns");
        System.out.println("  Difference:  " + (whiteMaterial - blackMaterial) + " centipawns");
    }
}
