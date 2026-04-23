package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Professional SVG-based chess piece renderer
 * Using high-quality piece designs similar to lichess.org and chess.com
 */
public class EnhancedSVGPieces {
    
    // Professional SVG path data for each piece (CBurnett set - used by lichess)
    private static final Map<String, String> PIECE_PATHS = new HashMap<>();
    
    static {
        // White pieces
        PIECE_PATHS.put("wK", "M 22.5,11.63 L 22.5,6 M 20,8 L 25,8 M 22.5,25 C 22.5,25 27,17.5 25.5,14.5 C 25.5,14.5 24.5,12 22.5,12 C 20.5,12 19.5,14.5 19.5,14.5 C 18,17.5 22.5,25 22.5,25 M 11.5,37 C 17,40.5 27,40.5 32.5,37 L 32.5,30 C 32.5,30 41.5,25.5 38.5,19.5 C 38.5,19.5 37.5,17 35.5,17 C 33.5,17 32.5,19.5 32.5,19.5 C 32.5,19.5 31.5,22.5 34.5,29.5 C 34.5,29.5 35.5,31.5 35.5,31.5 L 35.5,37 M 11.5,30 C 11.5,30 17,27 17,19.5 C 17,19.5 15,16 13,16 C 11,16 10,19 10,19 C 7,23 11.5,30 11.5,30 Z");
        
        PIECE_PATHS.put("wQ", "M 9,26 C 17.5,24.5 30,24.5 36,26 L 38.5,13.5 L 31,25 L 30.7,10.9 L 25.5,24.5 L 22.5,10 L 19.5,24.5 L 14.3,10.9 L 14,25 L 6.5,13.5 L 9,26 z M 9,26 C 9,28 10.5,28 11.5,30 C 12.5,31.5 12.5,31 12,33.5 C 10.5,34.5 11,36 11,36 C 9.5,37.5 11,38.5 11,38.5 C 17.5,39.5 27.5,39.5 34,38.5 C 34,38.5 35.5,37.5 34,36 C 34,36 34.5,34.5 33,33.5 C 32.5,31 32.5,31.5 33.5,30 C 34.5,28 36,28 36,26 C 30,24.5 17.5,24.5 9,26 z");
        
        PIECE_PATHS.put("wR", "M 9,39 L 36,39 L 36,36 L 9,36 L 9,39 z M 12.5,32 L 14,29.5 L 31,29.5 L 32.5,32 L 12.5,32 z M 12,36 L 12,32 L 33,32 L 33,36 L 12,36 z M 14,29.5 L 14,16.5 L 31,16.5 L 31,29.5 L 14,29.5 z M 14,16.5 L 11,14 L 34,14 L 31,16.5 L 14,16.5 z M 11,14 L 11,9 L 15,9 L 15,11 L 20,11 L 20,9 L 25,9 L 25,11 L 30,11 L 30,9 L 34,9 L 34,14 L 11,14 z");
        
        PIECE_PATHS.put("wB", "M 9,36 C 12.39,35.03 19.11,36.43 22.5,34 C 25.89,36.43 32.61,35.03 36,36 C 36,36 37.65,36.54 39,38 C 38.32,38.97 37.35,38.99 36,38.5 C 32.61,37.53 25.89,38.96 22.5,37.5 C 19.11,38.96 12.39,37.53 9,38.5 C 7.65,38.99 6.68,38.97 6,38 C 7.35,36.54 9,36 9,36 z M 15,32 C 17.5,34.5 27.5,34.5 30,32 C 30.5,30.5 30,30 30,30 C 30,27.5 27.5,26 27.5,26 C 33,24.5 33.5,14.5 22.5,10.5 C 11.5,14.5 12,24.5 17.5,26 C 17.5,26 15,27.5 15,30 C 15,30 14.5,30.5 15,32 z M 25,8 A 2.5,2.5 0 1 1 20,8 A 2.5,2.5 0 1 1 25,8 z");
        
        PIECE_PATHS.put("wN", "M 22,10 C 32.5,11 38.5,18 38,39 L 15,39 C 15,30 25,32.5 23,18 M 24,18 C 24.38,20.91 18.45,25.37 16,27 C 13,29 13.18,31.34 11,31 C 9.958,30.06 12.41,27.96 11,28 C 10,28 11.19,29.23 10,30 C 9,30 5.997,31 6,26 C 6,24 12,14 12,14 C 12,14 13.89,12.1 14,10.5 C 13.27,9.506 13.5,8.5 13.5,7.5 C 14.5,6.5 16.5,10 16.5,10 L 18.5,10 C 18.5,10 19.28,8.008 21,7 C 22,7 22,10 22,10 z");
        
        PIECE_PATHS.put("wP", "M 22,9 C 19.79,9 18,10.79 18,13 C 18,13.89 18.29,14.71 18.78,15.38 C 16.83,16.5 15.5,18.59 15.5,21 C 15.5,23.03 16.44,24.84 17.91,26.03 C 14.91,27.09 10.5,31.58 10.5,39.5 L 33.5,39.5 C 33.5,31.58 29.09,27.09 26.09,26.03 C 27.56,24.84 28.5,23.03 28.5,21 C 28.5,18.59 27.17,16.5 25.22,15.38 C 25.71,14.71 26,13.89 26,13 C 26,10.79 24.21,9 22,9 z");

        // Black pieces (same paths, different rendering)
        PIECE_PATHS.put("bK", PIECE_PATHS.get("wK"));
        PIECE_PATHS.put("bQ", PIECE_PATHS.get("wQ"));
        PIECE_PATHS.put("bR", PIECE_PATHS.get("wR"));
        PIECE_PATHS.put("bB", PIECE_PATHS.get("wB"));
        PIECE_PATHS.put("bN", PIECE_PATHS.get("wN"));
        PIECE_PATHS.put("bP", PIECE_PATHS.get("wP"));
    }

    /**
     * Draw a chess piece with professional styling
     */
    public static void drawPiece(Graphics2D g2d, String pieceCode, int x, int y, int size) {
        drawPiece(g2d, pieceCode, x, y, size, 1.0f);
    }

    /**
     * Draw a chess piece with opacity (for drag effects)
     */
    public static void drawPiece(Graphics2D g2d, String pieceCode, int x, int y, int size, float opacity) {
        String pathData = PIECE_PATHS.get(pieceCode);
        if (pathData == null) return;

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Parse SVG path
        Path2D path = parseSVGPath(pathData);

        // Calculate bounding box
        Rectangle2D bounds = path.getBounds2D();
        
        // Create transform to fit piece in square
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        
        // Scale to fit with padding (90% of square size)
        double scale = (size * 0.90) / Math.max(bounds.getWidth(), bounds.getHeight());
        transform.scale(scale, scale);
        
        // Center the piece
        transform.translate(-bounds.getCenterX() + bounds.getWidth() / 2, 
                          -bounds.getCenterY() + bounds.getHeight() / 2);

        Path2D transformedPath = (Path2D) path.createTransformedShape(transform);

        boolean isWhite = pieceCode.startsWith("w");
        
        // Set opacity
        Composite originalComposite = g2d.getComposite();
        if (opacity < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        if (isWhite) {
            // White pieces: light fill with dark outline
            g2d.setColor(new Color(255, 255, 255));
            g2d.fill(transformedPath);
            
            // Dark outline
            g2d.setColor(new Color(50, 50, 50));
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(transformedPath);
        } else {
            // Black pieces: dark fill with subtle highlight
            // Inner shadow effect
            g2d.setColor(new Color(30, 30, 30));
            g2d.fill(transformedPath);
            
            // Highlight edge
            g2d.setColor(new Color(80, 80, 80));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(transformedPath);
        }
        
        // Restore composite
        if (opacity < 1.0f) {
            g2d.setComposite(originalComposite);
        }
    }

    /**
     * Parse SVG path data into Path2D object
     */
    private static Path2D parseSVGPath(String pathData) {
        Path2D path = new Path2D.Double();
        
        // Remove extra whitespace and normalize
        pathData = pathData.replaceAll("\\s+", " ").trim();
        
        String[] tokens = pathData.split("(?=[MmLlHhVvCcSsQqTtAaZz])");
        
        double lastX = 0, lastY = 0;
        double lastControlX = 0, lastControlY = 0;
        
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            
            char command = token.charAt(0);
            String params = token.substring(1).trim();
            
            if (params.isEmpty() && command != 'Z' && command != 'z') continue;
            
            String[] coords = params.isEmpty() ? new String[0] : params.split("[\\s,]+");
            double[] values = new double[coords.length];
            for (int i = 0; i < coords.length; i++) {
                values[i] = coords[i].isEmpty() ? 0 : Double.parseDouble(coords[i]);
            }

            switch (command) {
                case 'M': // Absolute moveto
                    if (values.length >= 2) {
                        path.moveTo(values[0], values[1]);
                        lastX = values[0];
                        lastY = values[1];
                    }
                    break;
                    
                case 'm': // Relative moveto
                    if (values.length >= 2) {
                        lastX += values[0];
                        lastY += values[1];
                        path.moveTo(lastX, lastY);
                    }
                    break;
                    
                case 'L': // Absolute lineto
                    for (int i = 0; i < values.length - 1; i += 2) {
                        path.lineTo(values[i], values[i + 1]);
                        lastX = values[i];
                        lastY = values[i + 1];
                    }
                    break;
                    
                case 'l': // Relative lineto
                    for (int i = 0; i < values.length - 1; i += 2) {
                        lastX += values[i];
                        lastY += values[i + 1];
                        path.lineTo(lastX, lastY);
                    }
                    break;
                    
                case 'H': // Absolute horizontal lineto
                    for (double value : values) {
                        path.lineTo(value, lastY);
                        lastX = value;
                    }
                    break;
                    
                case 'h': // Relative horizontal lineto
                    for (double value : values) {
                        lastX += value;
                        path.lineTo(lastX, lastY);
                    }
                    break;
                    
                case 'V': // Absolute vertical lineto
                    for (double value : values) {
                        path.lineTo(lastX, value);
                        lastY = value;
                    }
                    break;
                    
                case 'v': // Relative vertical lineto
                    for (double value : values) {
                        lastY += value;
                        path.lineTo(lastX, lastY);
                    }
                    break;
                    
                case 'C': // Absolute cubic Bezier
                    for (int i = 0; i < values.length - 5; i += 6) {
                        path.curveTo(values[i], values[i + 1], 
                                   values[i + 2], values[i + 3],
                                   values[i + 4], values[i + 5]);
                        lastControlX = values[i + 2];
                        lastControlY = values[i + 3];
                        lastX = values[i + 4];
                        lastY = values[i + 5];
                    }
                    break;
                    
                case 'c': // Relative cubic Bezier
                    for (int i = 0; i < values.length - 5; i += 6) {
                        double cp1x = lastX + values[i];
                        double cp1y = lastY + values[i + 1];
                        double cp2x = lastX + values[i + 2];
                        double cp2y = lastY + values[i + 3];
                        lastX += values[i + 4];
                        lastY += values[i + 5];
                        path.curveTo(cp1x, cp1y, cp2x, cp2y, lastX, lastY);
                        lastControlX = cp2x;
                        lastControlY = cp2y;
                    }
                    break;
                    
                case 'A': // Absolute arc
                case 'a': // Relative arc
                    // Simplified arc handling - convert to cubic bezier approximation
                    if (values.length >= 7) {
                        double rx = values[0];
                        double ry = values[1];
                        double x = command == 'A' ? values[5] : lastX + values[5];
                        double y = command == 'A' ? values[6] : lastY + values[6];
                        
                        // Simple approximation: draw line for now
                        path.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                    
                case 'Z':
                case 'z':
                    path.closePath();
                    break;
            }
        }
        
        return path;
    }

    /**
     * Get piece code from type and color
     */
    public static String getPieceCode(boolean isWhite, String pieceType) {
        String prefix = isWhite ? "w" : "b";
        String code = "";
        
        switch (pieceType.toUpperCase()) {
            case "KING": code = "K"; break;
            case "QUEEN": code = "Q"; break;
            case "ROOK": code = "R"; break;
            case "BISHOP": code = "B"; break;
            case "KNIGHT": code = "N"; break;
            case "PAWN": code = "P"; break;
        }
        
        return prefix + code;
    }
}
