package UI;

import piece.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

/**
 * Integrates the modern chess board UI with an existing chess engine
 * Provides methods to sync game state and handle user interactions
 */
public class ChessBoardAdapter extends ModernChessBoard {
    
    // Callback interfaces for game logic
    public interface MoveValidator {
        boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol);
        List<int[]> getValidMovesFor(int row, int col);
    }
    
    public interface MoveHandler {
        void onMove(int fromRow, int fromCol, int toRow, int toCol);
        void onPieceSelected(int row, int col);
    }
    
    private MoveValidator moveValidator;
    private MoveHandler moveHandler;
    private boolean whiteToMove = true;
    
    public ChessBoardAdapter() {
        super();
    }
    
    public ChessBoardAdapter(ColorScheme scheme) {
        super(scheme);
    }
    
    public void setMoveValidator(MoveValidator validator) {
        this.moveValidator = validator;
    }
    
    public void setMoveHandler(MoveHandler handler) {
        this.moveHandler = handler;
    }
    
    /**
     * Sync board with game state using Piece objects
     */
    public void updateFromPieceArray(Piece[][] pieces) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (pieces[row][col] == null) {
                    setPiece(row, col, null);
                } else {
                    setPiece(row, col, pieceToCode(pieces[row][col]));
                }
            }
        }
    }
    
    /**
     * Convert Piece object to piece code
     */
    private String pieceToCode(Piece piece) {
        if (piece == null) return null;
        
        String color = piece.isWhite() ? "w" : "b";
        String type = "";
        
        switch (piece.getType()) {
            case KING: type = "K"; break;
            case QUEEN: type = "Q"; break;
            case ROOK: type = "R"; break;
            case BISHOP: type = "B"; break;
            case KNIGHT: type = "N"; break;
            case PAWN: type = "P"; break;
        }
        
        return color + type;
    }
    
    /**
     * Set the current player
     */
    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
    }
    
    /**
     * Highlight last move
     */
    public void highlightMove(int fromRow, int fromCol, int toRow, int toCol) {
        setLastMove(fromRow, fromCol, toRow, toCol);
    }
    
    /**
     * Clear all highlights
     */
    public void clearHighlights() {
        clearLastMove();
    }
    
    // Protected methods to be overridden from parent
    protected void setLastMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Access parent's lastMoveFrom and lastMoveTo through reflection or public setters
        // For now, make the move to update highlights
        // This would need the parent class to expose these fields or provide setters
    }
    
    protected void clearLastMove() {
        // Clear the last move highlight
    }
}

/**
 * Example usage with a complete chess game integration
 */
class ChessGameUI {
    private JFrame frame;
    private ChessBoardAdapter board;
    private JPanel controlPanel;
    private JLabel statusLabel;
    
    public ChessGameUI() {
        setupUI();
    }
    
    private void setupUI() {
        frame = new JFrame("Professional Chess Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create board with lichess color scheme
        board = new ChessBoardAdapter(ModernChessBoard.LICHESS_BLUE);
        
        // Setup move validation
        board.setMoveValidator(new ChessBoardAdapter.MoveValidator() {
            @Override
            public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
                // Connect to your game engine's move validation
                return true; // Placeholder
            }
            
            @Override
            public List<int[]> getValidMovesFor(int row, int col) {
                // Connect to your game engine's valid move calculation
                return new ArrayList<>(); // Placeholder
            }
        });
        
        // Setup move handler
        board.setMoveHandler(new ChessBoardAdapter.MoveHandler() {
            @Override
            public void onMove(int fromRow, int fromCol, int toRow, int toCol) {
                // Connect to your game engine's move execution
                updateStatus("Move: " + (char)('a' + fromCol) + (8 - fromRow) + 
                           " to " + (char)('a' + toCol) + (8 - toRow));
            }
            
            @Override
            public void onPieceSelected(int row, int col) {
                // Handle piece selection
            }
        });
        
        // Create control panel
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        
        JButton newGameBtn = new JButton("New Game");
        newGameBtn.addActionListener(e -> newGame());
        
        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> undo());
        
        JComboBox<String> themeSelector = new JComboBox<>(
            new String[]{"Lichess Blue", "Chess.com Green", "Classic Brown"}
        );
        themeSelector.addActionListener(e -> {
            int index = themeSelector.getSelectedIndex();
            switch (index) {
                case 0: board.setColorScheme(ModernChessBoard.LICHESS_BLUE); break;
                case 1: board.setColorScheme(ModernChessBoard.CHESSCOM_GREEN); break;
                case 2: board.setColorScheme(ModernChessBoard.CLASSIC_BROWN); break;
            }
        });
        
        statusLabel = new JLabel("White to move");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        controlPanel.add(newGameBtn);
        controlPanel.add(undoBtn);
        controlPanel.add(new JLabel("Theme:"));
        controlPanel.add(themeSelector);
        controlPanel.add(statusLabel);
        
        frame.add(board, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
    }
    
    private void newGame() {
        // Reset board to starting position
        board.clearHighlights();
        updateStatus("New game started - White to move");
    }
    
    private void undo() {
        // Undo last move
        updateStatus("Move undone");
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    public void show() {
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessGameUI game = new ChessGameUI();
            game.show();
        });
    }
}
