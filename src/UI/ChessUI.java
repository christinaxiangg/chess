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

            int clickedSquare = rank * 8 + file;
            System.out.println("Mouse click: file=" + file + ", rank=" + rank + ", square=" + clickedSquare);

            if (selectedSquare == -1) {
                // Select a piece
                Piece piece = board.getPiece(clickedSquare);
                System.out.println("Piece at square: " + piece);
                if (piece != null && piece.getColor() == board.getSideToMove()) {
                    selectedSquare = clickedSquare;
                    System.out.println("Selected square: " + selectedSquare);
                }
            } else {
                // Check if clicking on another piece of the same color
                Piece clickedPiece = board.getPiece(clickedSquare);
                if (clickedPiece != null && clickedPiece.getColor() == board.getSideToMove()) {
                    // Select the new piece instead
                    selectedSquare = clickedSquare;
                    System.out.println("Changed selection to: " + selectedSquare);
                    repaint();
                    return;
                }

                // Try to make a move
                if (clickedSquare == selectedSquare) {
                    // Deselect
                    selectedSquare = -1;
                } else {
                    // Attempt move
                    Piece movingPiece = board.getPiece(selectedSquare);
                    Piece capturedPiece = board.getPiece(clickedSquare);

                    // Determine move flags
                    int moveFlags = Move.QUIET_MOVE;

                    // Check for pawn double push
                    if (movingPiece != null && movingPiece.getType() == PieceType.PAWN) {
                        int fromRank = selectedSquare / 8;
                        int toRank = clickedSquare / 8;
                        int fromFile = selectedSquare % 8;
                        int toFile = clickedSquare % 8;

                        // Check for double pawn push
                        if (Math.abs(toRank - fromRank) == 2 && fromFile == toFile) {
                            moveFlags = Move.DOUBLE_PAWN_PUSH;
                        }

                        // Check for diagonal capture (including en passant)
                        if (fromFile != toFile) {
                            if (clickedSquare == board.getEnPassantSquare()) {
                                moveFlags = Move.EP_CAPTURE;
                            } else if (capturedPiece != null) {
                                moveFlags = Move.CAPTURE;
                            }
                        }
                    }

                    Move move;
                    System.out.println("Created move flags: " + moveFlags);
                    if (capturedPiece != null && moveFlags != Move.EP_CAPTURE && moveFlags < Move.KNIGHT_PROMOTION) {
                        move = new Move(selectedSquare, clickedSquare, moveFlags, capturedPiece.getType());
                    } else {
                        move = new Move(selectedSquare, clickedSquare, moveFlags);
                    }
                    System.out.println("Created move: " + move.toUCI() + ", flags: " + move.getFlags());

                    // Check for pawn promotion
                    Piece movingPiece2 = board.getPiece(selectedSquare);
                    if (movingPiece2 != null && movingPiece2.getType() == PieceType.PAWN) {
                        int toRank = clickedSquare / 8;
                        if ((movingPiece2.isWhite() && toRank == 7) || 
                            (!movingPiece2.isWhite() && toRank == 0)) {
                            // Show promotion dialog
                            PieceType promoType = showPromotionDialog(movingPiece2.getColor());
                            if (promoType != null) {
                                int promoFlag = getPromotionFlag(promoType, capturedPiece != null);
                                move = new Move(selectedSquare, clickedSquare, promoFlag);
                            }
                        }
                    }

                    // Check for castling
                    if (movingPiece2 != null && movingPiece2.getType() == PieceType.KING) {
                        if (clickedSquare == selectedSquare + 2) {
                            move = new Move(selectedSquare, clickedSquare, Move.KING_CASTLE);
                        } else if (clickedSquare == selectedSquare - 2) {
                            move = new Move(selectedSquare, clickedSquare, Move.QUEEN_CASTLE);
                        }
                    }

                    // Validate and make move
                    System.out.println("Validating move: " + move.toUCI());
                    boolean isLegal = MoveValidator.isMoveLegal(board, move);
                    System.out.println("Is move legal? " + isLegal);
                    if (isLegal) {
                        System.out.println("Move is legal, executing...");
                        makeMove(move);
                    } else {
                        // Show error message for illegal move
                        statusLabel.setText("Illegal move! Try again.");
                        statusLabel.setForeground(Color.RED);
                        // Don't deselect the piece so user can try another move
                        return;
                    }

                    selectedSquare = -1;
                    statusLabel.setForeground(Color.BLACK);
                }
            }

            repaint();
        }
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

    private int getPromotionFlag(PieceType promoType, boolean isCapture) {
        switch (promoType) {
            case PieceType.QUEEN: return isCapture ? Move.QUEEN_PROMO_CAPTURE : Move.QUEEN_PROMOTION;
            case PieceType.ROOK: return isCapture ? Move.ROOK_PROMO_CAPTURE : Move.ROOK_PROMOTION;
            case PieceType.BISHOP: return isCapture ? Move.BISHOP_PROMO_CAPTURE : Move.BISHOP_PROMOTION;
            case PieceType.KNIGHT: return isCapture ? Move.KNIGHT_PROMO_CAPTURE : Move.KNIGHT_PROMOTION;
            default: return Move.QUIET_MOVE;
        }
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
                    SearchEngine.SearchResult result = engine.search(boardCopy, 14, 3000);

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
        selectedSquare = -1;
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
        int movesToUndo = 1;

        // In engine modes, undo both engine and human moves
        if (!mode.equals("Human vs Human")) {
            movesToUndo = 2;
        }

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
