
package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import piece.*;

/**
 * Loads and renders chess piece images for professional appearance.
 * Pieces should be stored as PNG files in resources/pieces/ directory.
 */
public class ChessPieceImages {
    private static final int PIECE_SIZE = 80;

    // Piece image cache
    private static final Image[] whitePieces = new Image[6];
    private static final Image[] blackPieces = new Image[6];
    private static boolean loaded = false;

    // Piece type to file name mapping
    private static final String[] PIECE_FILES = {
        "pawn", "knight", "bishop", "rook", "queen", "king"
    };

    public static void loadPieceImages() {
        if (loaded) return;

        try {
            // Try to load from resources directory
            for (int i = 0; i < 6; i++) {
                whitePieces[i] = loadPieceImage("white_" + PIECE_FILES[i] + ".png");
                blackPieces[i] = loadPieceImage("black_" + PIECE_FILES[i] + ".png");
            }
            loaded = true;
        } catch (Exception e) {
            System.err.println("Could not load piece images: " + e.getMessage());
        }
    }

    private static Image loadPieceImage(String filename) {
        try {
            // Try to load from classpath
            URL url = ChessPieceImages.class.getResource("/pieces/" + filename);
            if (url != null) {
                return ImageIO.read(url);
            }

            // Try to load from file system
            File file = new File("resources/pieces/" + filename);
            if (file.exists()) {
                return ImageIO.read(file);
            }

            return null;
        } catch (IOException e) {
            System.err.println("Could not load image: " + filename);
            return null;
        }
    }

    public static void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        if (!loaded) {
            loadPieceImages();
        }

        int typeIndex = piece.getType().ordinal();
        Image pieceImage = piece.isWhite() ? whitePieces[typeIndex] : blackPieces[typeIndex];

        if (pieceImage != null) {
            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Draw piece image
            g2d.drawImage(pieceImage, x, y, PIECE_SIZE, PIECE_SIZE, null);
        } else {
            // Fallback to simple text rendering if images not available
            drawFallbackPiece(g2d, piece, x, y);
        }
    }

    private static void drawFallbackPiece(Graphics2D g2d, Piece piece, int x, int y) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String symbol = getPieceSymbol(piece);
        g2d.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 64));

        FontMetrics fm = g2d.getFontMetrics();
        int symbolWidth = fm.stringWidth(symbol);
        int symbolHeight = fm.getAscent();

        int drawX = x + (PIECE_SIZE - symbolWidth) / 2;
        int drawY = y + (PIECE_SIZE + symbolHeight) / 2 - fm.getDescent();

        g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);
        g2d.drawString(symbol, drawX, drawY);
    }

    private static String getPieceSymbol(Piece piece) {
        return switch (piece.getType()) {
            case KING -> piece.isWhite() ? "♔" : "♚";
            case QUEEN -> piece.isWhite() ? "♕" : "♛";
            case ROOK -> piece.isWhite() ? "♖" : "♜";
            case BISHOP -> piece.isWhite() ? "♗" : "♝";
            case KNIGHT -> piece.isWhite() ? "♘" : "♞";
            case PAWN -> piece.isWhite() ? "♙" : "♟";
            default -> "";
        };
    }
}
