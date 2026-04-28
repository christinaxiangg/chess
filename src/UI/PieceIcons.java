
package UI;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Manages chess piece icons using SVG-based images
 * similar to lichess and chess.com piece sets.
 */
public class PieceIcons {
    private static final String[] PIECE_FILES = {
        "wP.svg", "wN.svg", "wB.svg", "wR.svg", "wQ.svg", "wK.svg",
        "bP.svg", "bN.svg", "bB.svg", "bR.svg", "bQ.svg", "bK.svg"
    };

    private static final String[] PIECE_NAMES = {
        "white-pawn", "white-knight", "white-bishop", "white-rook", "white-queen", "white-king",
        "black-pawn", "black-knight", "black-bishop", "black-rook", "black-queen", "black-king"
    };

    private static final ImageIcon[] pieceIcons = new ImageIcon[12];
    private static boolean loaded = false;

    public static void loadIcons() {
        if (loaded) return;

        for (int i = 0; i < PIECE_FILES.length; i++) {
            try {
                URL url = PieceIcons.class.getResource("/pieces/" + PIECE_FILES[i]);
                if (url != null) {
                    pieceIcons[i] = new ImageIcon(url);
                }
            } catch (Exception e) {
                System.err.println("Could not load piece icon: " + PIECE_FILES[i]);
            }
        }

        loaded = true;
    }

    public static ImageIcon getIcon(int pieceIndex) {
        if (!loaded) {
            loadIcons();
        }
        return pieceIcons[pieceIndex];
    }

    public static void drawPiece(Graphics2D g2d, int pieceIndex, int x, int y, int size) {
        ImageIcon icon = getIcon(pieceIndex);
        if (icon != null) {
            Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            g2d.drawImage(scaled, x, y, null);
        }
    }
}
