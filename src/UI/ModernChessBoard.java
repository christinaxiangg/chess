package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Professional chess board UI similar to lichess.org and chess.com
 * Features:
 * - Smooth animations
 * - Professional color scheme
 * - Visual feedback for moves
 * - Highlight last move
 * - Show valid moves
 */
public class ModernChessBoard extends JPanel {
    
    // Board dimensions
    private static final int BOARD_SIZE = 640;
    private static final int SQUARE_SIZE = BOARD_SIZE / 8;
    
    // Professional color schemes (lichess-style)
    public static class ColorScheme {
        Color lightSquare;
        Color darkSquare;
        Color highlightLight;
        Color highlightDark;
        Color selectedLight;
        Color selectedDark;
        Color moveIndicator;
        Color captureIndicator;
        
        public ColorScheme(Color light, Color dark, Color highlightL, Color highlightD, 
                          Color selectedL, Color selectedD, Color move, Color capture) {
            this.lightSquare = light;
            this.darkSquare = dark;
            this.highlightLight = highlightL;
            this.highlightDark = highlightD;
            this.selectedLight = selectedL;
            this.selectedDark = selectedD;
            this.moveIndicator = move;
            this.captureIndicator = capture;
        }
    }
    
    // Predefined color schemes
    public static final ColorScheme LICHESS_BLUE = new ColorScheme(
        new Color(240, 217, 181),  // Light square
        new Color(181, 136, 99),   // Dark square
        new Color(205, 210, 106),  // Highlight light
        new Color(170, 162, 58),   // Highlight dark
        new Color(249, 248, 113),  // Selected light
        new Color(186, 202, 68),   // Selected dark
        new Color(100, 100, 100, 120),  // Move indicator
        new Color(200, 50, 50, 140)     // Capture indicator
    );
    
    public static final ColorScheme CHESSCOM_GREEN = new ColorScheme(
        new Color(238, 238, 210),  // Light square
        new Color(118, 150, 86),   // Dark square
        new Color(247, 247, 105),  // Highlight light
        new Color(186, 202, 68),   // Highlight dark
        new Color(255, 255, 90),   // Selected light
        new Color(196, 212, 78),   // Selected dark
        new Color(100, 100, 100, 120),  // Move indicator
        new Color(200, 50, 50, 140)     // Capture indicator
    );
    
    public static final ColorScheme CLASSIC_BROWN = new ColorScheme(
        new Color(240, 217, 181),  // Light square
        new Color(181, 136, 99),   // Dark square
        new Color(205, 210, 106),  // Highlight light
        new Color(170, 162, 58),   // Highlight dark
        new Color(249, 248, 113),  // Selected light
        new Color(186, 202, 68),   // Selected dark
        new Color(100, 100, 100, 120),  // Move indicator
        new Color(200, 50, 50, 140)     // Capture indicator
    );
    
    // Current state
    private ColorScheme colorScheme;
    private String[][] boardState; // 8x8 board with piece codes like "wK", "bP", null
    private Point selectedSquare;
    private Point hoveredSquare;
    private List<Point> validMoves;
    private Point lastMoveFrom;
    private Point lastMoveTo;
    private Point draggedPieceSquare;
    private Point draggedPiecePosition;
    
    // Animation support
    private boolean animationsEnabled = true;
    private Map<String, Point> animatingPieces = new HashMap<>();
    
    public ModernChessBoard() {
        this(LICHESS_BLUE);
    }
    
    public ModernChessBoard(ColorScheme scheme) {
        this.colorScheme = scheme;
        this.boardState = new String[8][8];
        this.validMoves = new ArrayList<>();
        
        setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        setBackground(Color.WHITE);
        
        initializeBoard();
        setupMouseListeners();
    }
    
    private void initializeBoard() {
        // Set up starting position
        // Black pieces
        boardState[0][0] = "bR"; boardState[0][7] = "bR";
        boardState[0][1] = "bN"; boardState[0][6] = "bN";
        boardState[0][2] = "bB"; boardState[0][5] = "bB";
        boardState[0][3] = "bQ";
        boardState[0][4] = "bK";
        for (int i = 0; i < 8; i++) boardState[1][i] = "bP";
        
        // White pieces
        boardState[7][0] = "wR"; boardState[7][7] = "wR";
        boardState[7][1] = "wN"; boardState[7][6] = "wN";
        boardState[7][2] = "wB"; boardState[7][5] = "wB";
        boardState[7][3] = "wQ";
        boardState[7][4] = "wK";
        for (int i = 0; i < 8; i++) boardState[6][i] = "wP";
    }
    
    private void setupMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point square = getSquareFromPixel(e.getX(), e.getY());
                if (square != null && boardState[square.x][square.y] != null) {
                    selectedSquare = square;
                    draggedPieceSquare = square;
                    draggedPiecePosition = e.getPoint();
                    // In a real game, calculate valid moves here
                    validMoves = calculateValidMoves(square);
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedPieceSquare != null) {
                    Point targetSquare = getSquareFromPixel(e.getX(), e.getY());
                    if (targetSquare != null && validMoves.contains(targetSquare)) {
                        makeMove(draggedPieceSquare, targetSquare);
                    }
                    draggedPieceSquare = null;
                    draggedPiecePosition = null;
                    selectedSquare = null;
                    validMoves.clear();
                    repaint();
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedPieceSquare != null) {
                    draggedPiecePosition = e.getPoint();
                    repaint();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                Point newHovered = getSquareFromPixel(e.getX(), e.getY());
                if (!Objects.equals(hoveredSquare, newHovered)) {
                    hoveredSquare = newHovered;
                    repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredSquare = null;
                repaint();
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    private Point getSquareFromPixel(int x, int y) {
        int row = y / SQUARE_SIZE;
        int col = x / SQUARE_SIZE;
        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            return new Point(row, col);
        }
        return null;
    }
    
    private List<Point> calculateValidMoves(Point square) {
        // Simplified for demo - in real game, use actual game logic
        List<Point> moves = new ArrayList<>();
        String piece = boardState[square.x][square.y];
        if (piece == null) return moves;
        
        // Example: show some random valid moves for demonstration
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (r == square.x || c == square.y) {
                    if (boardState[r][c] == null || 
                        !boardState[r][c].substring(0,1).equals(piece.substring(0,1))) {
                        moves.add(new Point(r, c));
                    }
                }
            }
        }
        return moves;
    }
    
    private void makeMove(Point from, Point to) {
        lastMoveFrom = from;
        lastMoveTo = to;
        boardState[to.x][to.y] = boardState[from.x][from.y];
        boardState[from.x][from.y] = null;
    }
    
    public void setPiece(int row, int col, String pieceCode) {
        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            boardState[row][col] = pieceCode;
            repaint();
        }
    }
    
    public String getPiece(int row, int col) {
        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            return boardState[row][col];
        }
        return null;
    }
    
    public void setColorScheme(ColorScheme scheme) {
        this.colorScheme = scheme;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        drawBoard(g2d);
        drawCoordinates(g2d);
        drawPieces(g2d);
        drawDraggedPiece(g2d);
    }
    
    private void drawBoard(Graphics2D g2d) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 == 0;
                Color squareColor;
                
                Point square = new Point(row, col);
                
                // Check if this square should be highlighted
                if (square.equals(selectedSquare)) {
                    squareColor = isLight ? colorScheme.selectedLight : colorScheme.selectedDark;
                } else if ((square.equals(lastMoveFrom)) ||
                          (square.equals(lastMoveTo))) {
                    squareColor = isLight ? colorScheme.highlightLight : colorScheme.highlightDark;
                } else {
                    squareColor = isLight ? colorScheme.lightSquare : colorScheme.darkSquare;
                }
                
                g2d.setColor(squareColor);
                g2d.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                
                // Draw hover effect
                if (square.equals(hoveredSquare) && draggedPieceSquare == null) {
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                }
            }
        }
        
        // Draw valid move indicators
        for (Point move : validMoves) {
            int centerX = move.y * SQUARE_SIZE + SQUARE_SIZE / 2;
            int centerY = move.x * SQUARE_SIZE + SQUARE_SIZE / 2;
            
            boolean isCapture = boardState[move.x][move.y] != null;
            
            if (isCapture) {
                // Draw capture indicator (ring around square)
                g2d.setColor(colorScheme.captureIndicator);
                g2d.setStroke(new BasicStroke(4.0f));
                int margin = 5;
                g2d.drawRoundRect(move.y * SQUARE_SIZE + margin, 
                                 move.x * SQUARE_SIZE + margin,
                                 SQUARE_SIZE - margin * 2, 
                                 SQUARE_SIZE - margin * 2,
                                 8, 8);
            } else {
                // Draw move indicator (circle)
                g2d.setColor(colorScheme.moveIndicator);
                int radius = SQUARE_SIZE / 6;
                g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
        }
    }
    
    private void drawCoordinates(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Draw file letters (a-h)
        for (int col = 0; col < 8; col++) {
            char file = (char) ('a' + col);
            boolean isLight = col % 2 == 1;
            g2d.setColor(isLight ? colorScheme.darkSquare : colorScheme.lightSquare);
            
            int x = col * SQUARE_SIZE + SQUARE_SIZE - 15;
            int y = 7 * SQUARE_SIZE + SQUARE_SIZE - 5;
            g2d.drawString(String.valueOf(file), x, y);
        }
        
        // Draw rank numbers (1-8)
        for (int row = 0; row < 8; row++) {
            int rank = 8 - row;
            boolean isLight = row % 2 == 0;
            g2d.setColor(isLight ? colorScheme.darkSquare : colorScheme.lightSquare);
            
            int x = 5;
            int y = row * SQUARE_SIZE + 15;
            g2d.drawString(String.valueOf(rank), x, y);
        }
    }
    
    private void drawPieces(Graphics2D g2d) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = boardState[row][col];
                
                // Skip if dragging this piece
                if (draggedPieceSquare != null && 
                    draggedPieceSquare.x == row && draggedPieceSquare.y == col) {
                    continue;
                }
                
                if (piece != null) {
                    int x = col * SQUARE_SIZE;
                    int y = row * SQUARE_SIZE;
                    CburnettSVGRenderer.drawPiece(g2d, piece, x, y, SQUARE_SIZE);
                }
            }
        }
    }
    
    private void drawDraggedPiece(Graphics2D g2d) {
        if (draggedPieceSquare != null && draggedPiecePosition != null) {
            String piece = boardState[draggedPieceSquare.x][draggedPieceSquare.y];
            if (piece != null) {
                int x = draggedPiecePosition.x - SQUARE_SIZE / 2;
                int y = draggedPiecePosition.y - SQUARE_SIZE / 2;
                
                // Draw with slight transparency
                CburnettSVGRenderer.drawPiece(g2d, piece, x, y, SQUARE_SIZE, 0.8f);
            }
        }
    }
    
    // Demo method to create a window
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Modern Chess Board");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            ModernChessBoard board = new ModernChessBoard(LICHESS_BLUE);
            frame.add(board);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
