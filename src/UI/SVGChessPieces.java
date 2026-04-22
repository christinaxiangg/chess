
package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Renders chess pieces using SVG paths for professional appearance
 * similar to lichess and chess.com piece sets.
 */
public class SVGChessPieces {

    // SVG path data for each piece (from Cburnett piece set)
    private static final Map<String, String> PIECE_PATHS = new HashMap<>();

    static {
        // White King
        PIECE_PATHS.put("wK", "M22.5 11.63V6M20 8h5M22.5 25s4.5-7.5 3-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c0 0-4.5 3-3 10.5M11.5 37c5.5 3.5 15.5 3.5 21 0v-7s9-4.5 6-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c0 0-4.5 3-3 10.5");

        // White Queen
        PIECE_PATHS.put("wQ", "M8 12a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M24.5 7.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M41 12a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M10.5 20.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M38.5 20.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M25 8a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4");

        // White Rook
        PIECE_PATHS.put("wR", "M9 39h27v-3H9v3zM12 36v-4h21v4H12zM11 14V9h4v2h5V9h5v2h5V9h4v5");

        // White Bishop
        PIECE_PATHS.put("wB", "M22 9c-2.21 0-4 1.79-4 4 0 .89.29 1.71.78 2.38C17.33 16.5 16 18.59 16 21c0 2.03.94 3.84 2.41 5.03-3 1.06-7.41 5.55-7.41 5.55s4.38 4.5 7.41 5.55c-1.47 1.19-2.41 3-2.41 5.03 0 2.41 1.33 4.5 2.78 5.62.49.67.78 1.49.78 2.38 0 2.21-1.79 4-4 4s-4-1.79-4-4c0-.89.29-1.71.78-2.38C26.94 30.84 25 28.75 25 26.5c0-2.03.94-3.84 2.41-5.03-3-1.06-7.41-5.55-7.41-5.55s4.38-4.5 7.41-5.55c-1.47-1.19-2.41-3-2.41-5.03 0-2.41 1.33-4.5 2.78-5.62.49-.67.78-1.49.78-2.38 0-2.21-1.79-4-4-4z");

        // White Knight
        PIECE_PATHS.put("wN", "M22 10c10.5 1 16.5 8 16 29H15c0-9 10-6.5 8-21");

        // White Pawn
        PIECE_PATHS.put("wP", "M22.5 9c-2.21 0-4 1.79-4 4 0 .89.29 1.71.78 2.38C17.33 16.5 16 18.59 16 21c0 2.03.94 3.84 2.41 5.03-3 1.06-7.41 5.55-7.41 5.55s4.38 4.5 7.41 5.55c-1.47 1.19-2.41 3-2.41 5.03 0 2.41 1.33 4.5 2.78 5.62.49.67.78 1.49.78 2.38 0 2.21-1.79 4-4 4s-4-1.79-4-4c0-.89.29-1.71.78-2.38C26.94 30.84 25 28.75 25 26.5c0-2.03.94-3.84 2.41-5.03-3-1.06-7.41-5.55-7.41-5.55s4.38-4.5 7.41-5.55c-1.47-1.19-2.41-3-2.41-5.03 0-2.41 1.33-4.5 2.78-5.62.49-.67.78-1.49.78-2.38 0-2.21-1.79-4-4-4z");

        // Black King
        PIECE_PATHS.put("bK", "M22.5 11.63V6M20 8h5M22.5 25s4.5-7.5 3-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c0 0-4.5 3-3 10.5M11.5 37c5.5 3.5 15.5 3.5 21 0v-7s9-4.5 6-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c0 0-4.5 3-3 10.5");

        // Black Queen
        PIECE_PATHS.put("bQ", "M8 12a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M24.5 7.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M41 12a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M10.5 20.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M38.5 20.5a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4M25 8a2 2 0 1 1-.4-4 2 2 0 0 1 .4 4");

        // Black Rook
        PIECE_PATHS.put("bR", "M9 39h27v-3H9v3zM12 36v-4h21v4H12zM11 14V9h4v2h5V9h5v2h5V9h4v5");

        // Black Bishop
        PIECE_PATHS.put("bB", "M22 9c-2.21 0-4 1.79-4 4 0 .89.29 1.71.78 2.38C17.33 16.5 16 18.59 16 21c0 2.03.94 3.84 2.41 5.03-3 1.06-7.41 5.55-7.41 5.55s4.38 4.5 7.41 5.55c-1.47 1.19-2.41 3-2.41 5.03 0 2.41 1.33 4.5 2.78 5.62.49.67.78 1.49.78 2.38 0 2.21-1.79 4-4 4s-4-1.79-4-4c0-.89.29-1.71.78-2.38C26.94 30.84 25 28.75 25 26.5c0-2.03.94-3.84 2.41-5.03-3-1.06-7.41-5.55-7.41-5.55s4.38-4.5 7.41-5.55c-1.47-1.19-2.41-3-2.41-5.03 0-2.41 1.33-4.5 2.78-5.62.49-.67.78-1.49.78-2.38 0-2.21-1.79-4-4-4z");

        // Black Knight
        PIECE_PATHS.put("bN", "M22 10c10.5 1 16.5 8 16 29H15c0-9 10-6.5 8-21");

        // Black Pawn
        PIECE_PATHS.put("bP", "M22.5 9c-2.21 0-4 1.79-4 4 0 .89.29 1.71.78 2.38C17.33 16.5 16 18.59 16 21c0 2.03.94 3.84 2.41 5.03-3 1.06-7.41 5.55-7.41 5.55s4.38 4.5 7.41 5.55c-1.47 1.19-2.41 3-2.41 5.03 0 2.41 1.33 4.5 2.78 5.62.49.67.78 1.49.78 2.38 0 2.21-1.79 4-4 4s-4-1.79-4-4c0-.89.29-1.71.78-2.38C26.94 30.84 25 28.75 25 26.5c0-2.03.94-3.84 2.41-5.03-3-1.06-7.41-5.55-7.41-5.55s4.38-4.5 7.41-5.55c-1.47-1.19-2.41-3-2.41-5.03 0-2.41 1.33-4.5 2.78-5.62.49-.67.78-1.49.78-2.38 0-2.21-1.79-4-4-4z");
    }

    public static void drawPiece(Graphics2D g2d, String pieceCode, int x, int y, int size) {
        String pathData = PIECE_PATHS.get(pieceCode);
        if (pathData == null) return;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Parse and draw SVG path
        Path2D path = parseSVGPath(pathData);

        // Scale to fit
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.scale(size / 50.0, size / 50.0);

        Path2D scaledPath = (Path2D) path.createTransformedShape(transform);

        // Fill and stroke
        boolean isWhite = pieceCode.startsWith("w");
        g2d.setColor(isWhite ? Color.WHITE : Color.BLACK);
        g2d.fill(scaledPath);

        g2d.setColor(isWhite ? new Color(80, 80, 80) : new Color(20, 20, 20));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(scaledPath);
    }

    private static Path2D parseSVGPath(String pathData) {
        Path2D path = new Path2D.Double();
        Pattern pattern = Pattern.compile("([MmLlHhVvCcSsQqTtAaZz])([^MmLlHhVvCcSsQqTtAaZz]*)");
        Matcher matcher = pattern.matcher(pathData);

        while (matcher.find()) {
            String command = matcher.group(1);
            String args = matcher.group(2);
            String[] coords = args.trim().split("[\s,]+");

            double[] values = new double[coords.length];
            for (int i = 0; i < coords.length; i++) {
                if (!coords[i].isEmpty()) {
                    values[i] = Double.parseDouble(coords[i]);
                }
            }

            switch (command) {
                case "M":
                    if (values.length >= 2) {
                        path.moveTo(values[0], values[1]);
                        for (int i = 2; i < values.length - 1; i += 2) {
                            path.lineTo(values[i], values[i + 1]);
                        }
                    }
                    break;
                case "m":
                    if (values.length >= 2) {
                        path.moveTo(values[0], values[1]);
                        for (int i = 2; i < values.length - 1; i += 2) {
                            path.lineTo(values[i], values[i + 1]);
                        }
                    }
                    break;
                case "L":
                    if (values.length >= 2) {
                        path.lineTo(values[0], values[1]);
                        for (int i = 2; i < values.length - 1; i += 2) {
                            path.lineTo(values[i], values[i + 1]);
                        }
                    }
                    break;
                case "l":
                    if (values.length >= 2) {
                        path.lineTo(values[0], values[1]);
                        for (int i = 2; i < values.length - 1; i += 2) {
                            path.lineTo(values[i], values[i + 1]);
                        }
                    }
                    break;
                case "H":
                    if (values.length >= 1) {
                        path.lineTo(values[0], path.getCurrentPoint().getY());
                    }
                    break;
                case "h":
                    if (values.length >= 1) {
                        Point2D current = path.getCurrentPoint();
                        path.lineTo(current.getX() + values[0], current.getY());
                    }
                    break;
                case "V":
                    if (values.length >= 1) {
                        path.lineTo(path.getCurrentPoint().getX(), values[0]);
                    }
                    break;
                case "v":
                    if (values.length >= 1) {
                        Point2D current = path.getCurrentPoint();
                        path.lineTo(current.getX(), current.getY() + values[0]);
                    }
                    break;
                case "C":
                    if (values.length >= 6) {
                        path.curveTo(values[0], values[1], values[2], values[3], values[4], values[5]);
                        for (int i = 6; i < values.length - 5; i += 6) {
                            path.curveTo(values[i], values[i + 1], values[i + 2], values[i + 3], 
                                        values[i + 4], values[i + 5]);
                        }
                    }
                    break;
                case "c":
                    if (values.length >= 6) {
                        Point2D current = path.getCurrentPoint();
                        path.curveTo(current.getX() + values[0], current.getY() + values[1],
                                    current.getX() + values[2], current.getY() + values[3],
                                    current.getX() + values[4], current.getY() + values[5]);
                        for (int i = 6; i < values.length - 5; i += 6) {
                            Point2D curr = path.getCurrentPoint();
                            path.curveTo(curr.getX() + values[i], curr.getY() + values[i + 1],
                                        curr.getX() + values[i + 2], curr.getY() + values[i + 3],
                                        curr.getX() + values[i + 4], curr.getY() + values[i + 5]);
                        }
                    }
                    break;
                case "Z":
                case "z":
                    path.closePath();
                    break;
            }
        }

        return path;
    }
}
