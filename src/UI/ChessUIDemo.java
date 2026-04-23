package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Complete demo showcasing all features of the modern chess UI
 * 
 * Features demonstrated:
 * - Professional SVG piece rendering
 * - Multiple color schemes (Lichess, Chess.com, Classic)
 * - Drag and drop pieces
 * - Valid move indicators
 * - Last move highlighting
 * - Hover effects
 * - Smooth animations
 * - Coordinate labels
 */
public class ChessUIDemo {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Modern Chess UI - Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        
        // Create main chess board
        ModernChessBoard board = new ModernChessBoard(ModernChessBoard.LICHESS_BLUE);
        
        // Create side panel with controls
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidePanel.setPreferredSize(new Dimension(300, 640));
        
        // Title
        JLabel titleLabel = new JLabel("Chess UI Demo");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Color scheme selector
        JPanel themePanel = new JPanel();
        themePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        themePanel.add(new JLabel("Theme:"));
        
        String[] themes = {"Lichess Blue", "Chess.com Green", "Classic Brown"};
        JComboBox<String> themeCombo = new JComboBox<>(themes);
        themeCombo.setPreferredSize(new Dimension(200, 30));
        themeCombo.addActionListener(e -> {
            int selected = themeCombo.getSelectedIndex();
            switch (selected) {
                case 0:
                    board.setColorScheme(ModernChessBoard.LICHESS_BLUE);
                    break;
                case 1:
                    board.setColorScheme(ModernChessBoard.CHESSCOM_GREEN);
                    break;
                case 2:
                    board.setColorScheme(ModernChessBoard.CLASSIC_BROWN);
                    break;
            }
        });
        themePanel.add(themeCombo);
        
        // Action buttons
        JButton resetBtn = new JButton("Reset Board");
        resetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetBtn.addActionListener(e -> {
            // Reset to starting position
            frame.dispose();
            createAndShowGUI();
        });
        
        JButton clearBtn = new JButton("Clear Board");
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    board.setPiece(i, j, null);
                }
            }
        });
        
        JButton testPositionBtn = new JButton("Load Test Position");
        testPositionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        testPositionBtn.addActionListener(e -> loadTestPosition(board));
        
        // Instructions
        JTextArea instructions = new JTextArea();
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setFont(new Font("Arial", Font.PLAIN, 12));
        instructions.setText(
            "FEATURES:\n\n" +
            "• Click and drag pieces to move them\n\n" +
            "• Valid moves are highlighted with dots\n\n" +
            "• Last move is highlighted in yellow\n\n" +
            "• Hover over squares for highlighting\n\n" +
            "• Professional SVG piece rendering\n\n" +
            "• Multiple color schemes available\n\n" +
            "• Smooth drag and drop interaction\n\n" +
            "• Coordinate labels (a-h, 1-8)\n\n" +
            "Try selecting a piece and see the valid moves!"
        );
        instructions.setBackground(new Color(245, 245, 245));
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(instructions);
        scrollPane.setPreferredSize(new Dimension(280, 300));
        
        // Piece legend
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new GridLayout(0, 1, 5, 5));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Piece Codes"));
        
        String[] pieceCodes = {
            "wK = White King", "wQ = White Queen", "wR = White Rook",
            "wB = White Bishop", "wN = White Knight", "wP = White Pawn",
            "bK = Black King", "bQ = Black Queen", "bR = Black Rook",
            "bB = Black Bishop", "bN = Black Knight", "bP = Black Pawn"
        };
        
        for (String code : pieceCodes) {
            JLabel label = new JLabel(code);
            label.setFont(new Font("Monospaced", Font.PLAIN, 11));
            legendPanel.add(label);
        }
        
        // Add all components to side panel
        sidePanel.add(titleLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(themePanel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(resetBtn);
        sidePanel.add(Box.createVerticalStrut(5));
        sidePanel.add(clearBtn);
        sidePanel.add(Box.createVerticalStrut(5));
        sidePanel.add(testPositionBtn);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(scrollPane);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(legendPanel);
        
        // Add components to frame
        frame.add(board, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);
        
        // Pack and show
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private static void loadTestPosition(ModernChessBoard board) {
        // Clear board first
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board.setPiece(i, j, null);
            }
        }
        
        // Set up an interesting mid-game position
        board.setPiece(0, 4, "bK");  // Black king
        board.setPiece(0, 0, "bR");  // Black rook
        board.setPiece(1, 1, "bP");
        board.setPiece(1, 3, "bP");
        board.setPiece(1, 4, "bP");
        board.setPiece(1, 5, "bP");
        board.setPiece(2, 2, "bN");  // Black knight
        board.setPiece(3, 3, "bB");  // Black bishop
        
        board.setPiece(7, 4, "wK");  // White king
        board.setPiece(7, 7, "wR");  // White rook
        board.setPiece(6, 0, "wP");
        board.setPiece(6, 3, "wP");
        board.setPiece(6, 4, "wP");
        board.setPiece(6, 6, "wP");
        board.setPiece(5, 5, "wQ");  // White queen
        board.setPiece(4, 2, "wN");  // White knight
    }
}
