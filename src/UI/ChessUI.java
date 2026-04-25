package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import board.BitBoard;
import move.Move;
import move.MoveGenerator;
import search.*;
import move.*;
import piece.*;
import board.ZobristHash;

/**
 * Production-quality chessboard UI with move validation, engine integration,
 * and all standard chess features.
 */
public class ChessUI extends JFrame {
    private static final int SQUARE_SIZE = 80;
    private static final int BOARD_SIZE = SQUARE_SIZE * 8;
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 0, 128);
    private static final Color LAST_MOVE_COLOR = new Color(155, 199, 0, 128);
    private static final Color SELECTED_SQUARE_COLOR = new Color(106, 176, 76, 180);

    private BitBoard board;
    private Move lastMove;
    private int selectedSquare = -1;
    private List<Move> legalMovesForSelected = new ArrayList<>();
    private boolean boardFlipped = false;
    private List<Move> moveHistory;
    private JList<String> moveList;
    private DefaultListModel<String> moveListModel;
    private JLabel statusLabel;
    private JButton flipButton;
    private JButton newGameButton;
    private JButton undoButton;
    private JComboBox<String> playerModeCombo;
    private SearchEngine whiteEngine;
    private SearchEngine blackEngine;
    private ExecutorService engineExecutor;
    private boolean engineThinking = false;

    public ChessUI() {
        super("Chess Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize game state
        board = BitBoard.startingPosition();
        moveHistory = new ArrayList<>();

        // Initialize engines
        whiteEngine = new SearchEngine(128);
        blackEngine = new SearchEngine(128);
        engineExecutor = Executors.newSingleThreadExecutor();

        // Create UI components
        initComponents();

        // Add window listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                engineExecutor.shutdownNow();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create chessboard panel
        ChessBoardPanel boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        boardPanel.addMouseListener(new BoardMouseListener());

        // Create move history panel
        JPanel historyPanel = createMoveHistoryPanel();

        // Create control panel
        JPanel controlPanel = createControlPanel();

        // Add components to main panel
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(historyPanel, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Create status bar
        statusLabel = new JLabel("White to move");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        statusLabel.setPreferredSize(new Dimension(BOARD_SIZE, 30));

        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createMoveHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(200, BOARD_SIZE));
        panel.setBorder(BorderFactory.createTitledBorder("move.Move History"));

        moveListModel = new DefaultListModel<>();
        moveList = new JList<>(moveListModel);
        moveList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        moveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(moveList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Player mode combo
        playerModeCombo = new JComboBox<>(new String[]{
            "Human vs Human",
            "Human vs Engine",
            "Engine vs Human",
            "Engine vs Engine"
        });
        playerModeCombo.addActionListener(e -> onPlayerModeChanged());

        // Control buttons
        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> startNewGame());

        flipButton = new JButton("Flip Board");
        flipButton.addActionListener(e -> flipBoard());

        undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> undoLastMove());

        panel.add(playerModeCombo);
        panel.add(newGameButton);
        panel.add(flipButton);
        panel.add(undoButton);

        return panel;
    }

    private class ChessBoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Build set of legal destination squares for highlighting
            Set<Integer> legalTargets = new HashSet<>();
            for (Move m : legalMovesForSelected) {
                legalTargets.add(m.getTo());
            }

            // Draw squares
            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    int displayRank = boardFlipped ? 7 - rank : rank;
                    int displayFile = boardFlipped ? 7 - file : file;

                    int x = file * SQUARE_SIZE;
                    int y = (7 - rank) * SQUARE_SIZE;

                    // Determine square color
                    Color squareColor = ((file + rank) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                    g2d.setColor(squareColor);
                    g2d.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);

                    // Highlight last move
                    if (lastMove != null) {
                        int from = lastMove.getFrom();
                        int to = lastMove.getTo();
                        int fromRank = from / 8;
                        int fromFile = from % 8;
                        int toRank = to / 8;
                        int toFile = to % 8;

                        if ((displayRank == fromRank && displayFile == fromFile) ||
                            (displayRank == toRank && displayFile == toFile)) {
                            g2d.setColor(LAST_MOVE_COLOR);
                            g2d.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                        }
                    }

                    // Highlight selected square
                    if (selectedSquare != -1) {
                        int selRank = selectedSquare / 8;
                        int selFile = selectedSquare % 8;
                        if (displayRank == selRank && displayFile == selFile) {
                            g2d.setColor(SELECTED_SQUARE_COLOR);
                            g2d.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                        }
                    }

                    // Highlight legal destination squares
                    int sq = displayRank * 8 + displayFile;
                    if (legalTargets.contains(sq)) {
                        g2d.setColor(HIGHLIGHT_COLOR);
                        g2d.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                    }

                    // Draw piece
                    Piece piece = board.getPiece(displayFile, displayRank);
                    if (piece != null) {
                        drawPiece(g2d, piece, x, y);
                    }

                    // Draw coordinates
                    drawCoordinates(g2d, file, rank, x, y);
                }
            }
        }

        private void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
            String pieceCode = (piece.isWhite() ? "w" : "b") + 
                             getPieceTypeLetter(piece.getType());
            CburnettSVGRenderer.drawPiece(g2d, pieceCode, x, y, SQUARE_SIZE);
        }

        private String getPieceTypeLetter(PieceType type) {
            switch (type) {
                case PAWN: return "P";
                case KNIGHT: return "N";
                case BISHOP: return "B";
                case ROOK: return "R";
                case QUEEN: return "Q";
                case KING: return "K";
                default: return "P";
            }
        }

        private void drawCoordinates(Graphics2D g2d, int file, int rank, int x, int y) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.setColor(((file + rank) % 2 == 0) ? DARK_SQUARE : LIGHT_SQUARE);

            // Draw file letters (a-h) at the bottom
            if (rank == 0) {
                char fileChar = (char)('a' + (boardFlipped ? 7 - file : file));
                g2d.drawString(String.valueOf(fileChar), x + SQUARE_SIZE - 15, y + SQUARE_SIZE - 3);
            }

            // Draw rank numbers (1-8) on the left
            if (file == 0) {
                int rankNum = (boardFlipped ? rank : 7 - rank) + 1;
                g2d.drawString(String.valueOf(rankNum), x + 3, y + 12);
            }
        }

        private String getPieceSymbol(Piece piece) {
            switch (piece.getType()) {
                case PieceType.KING: return piece.isWhite() ? "♔" : "♚";
                case PieceType.QUEEN: return piece.isWhite() ? "♕" : "♛";
                case PieceType.ROOK: return piece.isWhite() ? "♖" : "♜";
                case PieceType.BISHOP: return piece.isWhite() ? "♗" : "♝";
                case PieceType.KNIGHT: return piece.isWhite() ? "♘" : "♞";
                case PieceType.PAWN: return piece.isWhite() ? "♙" : "♟";
                default: return "";
            }
        }
    }

    private class BoardMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (engineThinking) {
                return; // Don't allow moves while engine is thinking
            }

            int file = e.getX() / SQUARE_SIZE;
            int rank = 7 - (e.getY() / SQUARE_SIZE);

            if (file < 0 || file > 7 || rank < 0 || rank > 7) {
                return;
            }

            // Adjust for flipped board
            if (boardFlipped) {
                file = 7 - file;
                rank = 7 - rank;
            }

            int clickedSquare = rank * 8 + file;

            if (selectedSquare == -1) {
                // Select a piece belonging to the side to move
                Piece piece = board.getPiece(clickedSquare);
                if (piece != null && piece.getColor() == board.getSideToMove()) {
                    selectedSquare = clickedSquare;
                    // Cache legal moves for this piece only — generated once per selection
                    legalMovesForSelected.clear();
                    for (Move m : MoveGenerator.generateLegalMoves(board)) {
                        if (m.getFrom() == clickedSquare) {
                            legalMovesForSelected.add(m);
                        }
                    }
                }
            } else {
                // Clicking the same square deselects
                if (clickedSquare == selectedSquare) {
                    clearSelection();
                    repaint();
                    return;
                }

                // Clicking another friendly piece re-selects
                Piece clickedPiece = board.getPiece(clickedSquare);
                if (clickedPiece != null && clickedPiece.getColor() == board.getSideToMove()) {
                    selectedSquare = clickedSquare;
                    legalMovesForSelected.clear();
                    for (Move m : MoveGenerator.generateLegalMoves(board)) {
                        if (m.getFrom() == clickedSquare) {
                            legalMovesForSelected.add(m);
                        }
                    }
                    repaint();
                    return;
                }

                // Attempt move — look up from the cached legal move list
                Move move = findLegalMove(clickedSquare);

                if (move != null && move.isPromotion()) {
                    // Ask user which piece, then find that specific promotion move
                    Piece movingPiece = board.getPiece(selectedSquare);
                    PieceType promoType = showPromotionDialog(movingPiece.getColor());
                    if (promoType == null) {
                        // User cancelled promotion dialog — keep piece selected
                        repaint();
                        return;
                    }
                    move = findLegalMoveWithPromotion(clickedSquare, promoType);
                }

                if (move != null) {
                    makeMove(move);
                    clearSelection();
                    statusLabel.setForeground(Color.BLACK);
                } else {
                    statusLabel.setText("Illegal move! Try again.");
                    statusLabel.setForeground(Color.RED);
                    // Keep piece selected so user can try another destination
                }
            }

            repaint();
        }
    }

    /**
     * Finds the legal move from the currently selected square to the given destination.
     * For promotions, returns the first matching promotion move (any piece type).
     */
    private Move findLegalMove(int to) {
        for (Move m : legalMovesForSelected) {
            if (m.getTo() == to) return m;
        }
        return null;
    }

    /**
     * Finds the legal promotion move to the given destination with the specific piece type.
     */
    private Move findLegalMoveWithPromotion(int to, PieceType promoType) {
        for (Move m : legalMovesForSelected) {
            if (m.getTo() == to && m.isPromotion()
                    && m.getPromotionPieceType() == promoType) {
                return m;
            }
        }
        return null;
    }

    private void clearSelection() {
        selectedSquare = -1;
        legalMovesForSelected.clear();
    }

    private PieceType showPromotionDialog(PieceColor color) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Choose promotion piece:",
            "Pawn Promotion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice >= 0 && choice < options.length) {
            switch (options[choice]) {
                case "Queen": return PieceType.QUEEN;
                case "Rook": return PieceType.ROOK;
                case "Bishop": return PieceType.BISHOP;
                case "Knight": return PieceType.KNIGHT;
            }
        }
        return null;
    }

    private void makeMove(Move move) {
        board.makeMove(move);
        lastMove = move;
        moveHistory.add(move);

        // Update move list
        updateMoveList();

        // Update status
        updateStatus();

        // Check for game end
        checkGameEnd();

        // Trigger engine move if needed
        triggerEngineMove();
    }

    private void updateMoveList() {
        int moveNumber = (moveHistory.size() + 1) / 2;
        if (moveHistory.size() % 2 == 1) {
            // White's move
            moveListModel.addElement(moveNumber + ". " + lastMove.toUCI());
        } else {
            // Black's move - append to last line
            int lastIndex = moveListModel.size() - 1;
            String lastEntry = moveListModel.getElementAt(lastIndex);
            moveListModel.setElementAt(lastEntry + " " + lastMove.toUCI(), lastIndex);
        }

        // Scroll to bottom
        moveList.ensureIndexIsVisible(moveListModel.size() - 1);
    }

    private void updateStatus() {
        PieceColor sideToMove = board.getSideToMove();
        String color = sideToMove == PieceColor.WHITE ? "White" : "Black";

        if (CheckValidator.isKingInCheck(board, sideToMove)) {
            statusLabel.setText(color + " to move (CHECK!)");
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setText(color + " to move");
            statusLabel.setForeground(Color.BLACK);
        }
    }

    private void checkGameEnd() {
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);

        if (legalMoves.isEmpty()) {
            PieceColor sideToMove = board.getSideToMove();
            if (CheckValidator.isKingInCheck(board, sideToMove)) {
                String winner = sideToMove == PieceColor.WHITE ? "Black" : "White";
                JOptionPane.showMessageDialog(this, winner + " wins by checkmate!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Stalemate! The game is a draw.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void triggerEngineMove() {
        String mode = (String) playerModeCombo.getSelectedItem();
        PieceColor sideToMove = board.getSideToMove();
        boolean engineToMove = false;

        if (mode.equals("Human vs Engine") && sideToMove == PieceColor.BLACK) {
            engineToMove = true;
        } else if (mode.equals("Engine vs Human") && sideToMove == PieceColor.WHITE) {
            engineToMove = true;
        } else if (mode.equals("Engine vs Engine")) {
            engineToMove = true;
        }

        if (engineToMove) {
            engineThinking = true;
            statusLabel.setText("Engine thinking...");

            // Create a copy of the board for the engine to search on
            // This prevents the engine from modifying the UI's board state during search
            final BitBoard boardCopy = board.copy();

            engineExecutor.submit(() -> {
                try {
                    SearchEngine engine = sideToMove == PieceColor.WHITE ? whiteEngine : blackEngine;
                    SearchEngine.SearchResult result = engine.search(boardCopy, 14, 4000);

                    SwingUtilities.invokeLater(() -> {
                        if (result.bestMove != null) {
                            makeMove(result.bestMove);
                        }
                        engineThinking = false;
                        repaint();
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ChessUI.this, "Engine error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        engineThinking = false;
                        repaint();
                    });
                }
            });
        }
    }

    private void onPlayerModeChanged() {
        // Reset game when mode changes
        startNewGame();
    }

    private void flipBoard() {
        boardFlipped = !boardFlipped;
        repaint();
    }

    private void startNewGame() {
        board = BitBoard.startingPosition();
        lastMove = null;
        clearSelection();
        moveHistory.clear();
        moveListModel.clear();

        // Reset engines
        whiteEngine = new SearchEngine(128);
        blackEngine = new SearchEngine(128);

        updateStatus();
        repaint();

        // Trigger engine move if needed
        triggerEngineMove();
    }

    private void undoLastMove() {
        if (moveHistory.isEmpty() || engineThinking) {
            return;
        }

        String mode = (String) playerModeCombo.getSelectedItem();
        int movesToUndo = mode.equals("Human vs Human") ? 1 : 2;
        // Don't undo more moves than available
        movesToUndo = Math.min(movesToUndo, moveHistory.size());

        // Restart game and replay all moves except the last ones
        BitBoard newBoard = BitBoard.startingPosition();
        for (int i = 0; i < moveHistory.size() - movesToUndo; i++) {
            newBoard.makeMove(moveHistory.get(i));
        }

        board = newBoard;

        // Remove moves from history
        for (int i = 0; i < movesToUndo; i++) {
            moveHistory.remove(moveHistory.size() - 1);
        }

        // Update last move
        lastMove = moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
        clearSelection();
        // Rebuild move list
        rebuildMoveList();

        updateStatus();
        repaint();
    }

    private void rebuildMoveList() {
        moveListModel.clear();
        for (int i = 0; i < moveHistory.size(); i++) {
            Move move = moveHistory.get(i);
            int moveNumber = (i + 2) / 2;
            if (i % 2 == 0) {
                moveListModel.addElement(moveNumber + ". " + move.toUCI());
            } else {
                int lastIndex = moveListModel.size() - 1;
                String lastEntry = moveListModel.getElementAt(lastIndex);
                moveListModel.setElementAt(lastEntry + " " + move.toUCI(), lastIndex);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            ChessUI ui = new ChessUI();
            ui.setVisible(true);
        });
    }
}
