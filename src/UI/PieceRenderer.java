
package UI;

import java.awt.*;
import piece.*;

/**
 * Professional piece renderer using Unicode chess symbols
 * with high-quality font rendering.
 */
public class PieceRenderer {
    // Unicode chess piece symbols
    private static final String[] WHITE_PIECES = {
        "♙", // White Pawn
        "♘", // White Knight
        "♗", // White Bishop
        "♖", // White Rook
        "♕", // White Queen
        "♔"  // White King
    };

    private static final String[] BLACK_PIECES = {
        "♟", // Black Pawn
        "♞", // Black Knight
        "♝", // Black Bishop
        "♜", // Black Rook
        "♛", // Black Queen
        "♚"  // Black King
    };

    public static void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Get piece symbol
        String symbol = getPieceSymbol(piece);

        // Use large font size for clear pieces
        Font pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, 64);
        g2d.setFont(pieceFont);

        // Get font metrics for proper positioning
        FontMetrics fm = g2d.getFontMetrics(pieceFont);
        int symbolWidth = fm.stringWidth(symbol);
        int symbolHeight = fm.getAscent();

        // Calculate centered position
        int drawX = x + (80 - symbolWidth) / 2;
        int drawY = y + (80 + symbolHeight) / 2 - fm.getDescent();

        // Draw shadow for depth
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawString(symbol, drawX + 2, drawY + 2);

        // Draw main piece
        g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);
        g2d.drawString(symbol, drawX, drawY);
    }

    private static String getPieceSymbol(Piece piece) {
        int index = piece.getType().ordinal();
        return piece.isWhite() ? WHITE_PIECES[index] : BLACK_PIECES[index];
    }
}

