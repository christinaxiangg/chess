package search;
import board.Evaluator;

import java.util.Scanner;
import java.util.List;
import board.BitBoard;
import move.CheckValidator;
import move.Move;
import move.MoveGenerator;
import piece.PieceColor;
/**
 * UCI (Universal Chess Interface) implementation for the chess engine.
 * Allows the engine to communicate with chess GUIs like Arena, ChessBase, etc.
 */
public class UCIInterface {
    
    private BitBoard board;
    private SearchEngine searchEngine;
    private boolean running;
    
    // UCI options
    private int hashSize = 64; // MB
    private int searchDepth = 20;
    
    public UCIInterface() {
        this.board = BitBoard.startingPosition();
        this.searchEngine = new SearchEngine(hashSize);
        this.running = false;
    }
    
    /**
     * Main loop for UCI protocol.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        running = true;
        
        while (running) {
            String line = scanner.nextLine().trim();
            handleCommand(line);
        }
        
        scanner.close();
    }
    
    /**
     * Handles UCI commands.
     */
    private void handleCommand(String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length == 0) return;
        
        String command = tokens[0];
        
        switch (command) {
            case "uci":
                handleUCI();
                break;
            case "isready":
                handleIsReady();
                break;
            case "ucinewgame":
                handleNewGame();
                break;
            case "position":
                handlePosition(tokens);
                break;
            case "go":
                handleGo(tokens);
                break;
            case "stop":
                handleStop();
                break;
            case "quit":
                handleQuit();
                break;
            case "setoption":
                handleSetOption(tokens);
                break;
            case "d":
            case "display":
                board.print();
                break;
            case "eval":
                handleEval();
                break;
            default:
                // Unknown command, ignore
                break;
        }
    }
    
    /**
     * Handles the 'uci' command - identifies the engine.
     */
    private void handleUCI() {
        System.out.println("id name JavaChessEngine 1.0");
        System.out.println("id author Chess Developer");
        System.out.println();
        System.out.println("option name Hash type spin default 64 min 1 max 1024");
        System.out.println("option name SearchDepth type spin default 20 min 1 max 50");
        System.out.println("uciok");
    }
    
    /**
     * Handles the 'isready' command.
     */
    private void handleIsReady() {
        System.out.println("readyok");
    }
    
    /**
     * Handles the 'ucinewgame' command.
     */
    private void handleNewGame() {
        board = BitBoard.startingPosition();
        searchEngine.clearTranspositionTable();
    }
    
    /**
     * Handles the 'position' command.
     * Format: position [fen <fenstring> | startpos] moves <move1> ... <moveN>
     */
    private void handlePosition(String[] tokens) {
        int movesIndex = -1;
        
        // Find where moves start
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("moves")) {
                movesIndex = i;
                break;
            }
        }
        
        // Set up position
        if (tokens.length > 1) {
            if (tokens[1].equals("startpos")) {
                board = BitBoard.startingPosition();
            } else if (tokens[1].equals("fen")) {
                // Build FEN string
                StringBuilder fenBuilder = new StringBuilder();
                int fenStart = 2;
                int fenEnd = movesIndex > 0 ? movesIndex : tokens.length;
                
                for (int i = fenStart; i < fenEnd; i++) {
                    if (i > fenStart) fenBuilder.append(" ");
                    fenBuilder.append(tokens[i]);
                }
                
                board = BitBoard.fromFEN(fenBuilder.toString());
            }
        }
        
        // Apply moves
        if (movesIndex > 0) {
            for (int i = movesIndex + 1; i < tokens.length; i++) {
                Move move = parseMove(tokens[i]);
                if (move != null) {
                    board.makeMove(move);
                }
            }
        }
    }
    
    /**
     * Handles the 'go' command.
     * Format: go [searchmoves <move1> ... <moveN>] [ponder] [wtime <ms>] [btime <ms>]
     *         [winc <ms>] [binc <ms>] [movestogo <n>] [depth <n>] [nodes <n>]
     *         [mate <n>] [movetime <ms>] [infinite]
     */
    private void handleGo(String[] tokens) {
         int depth = searchDepth;
         long timeLimit = 30000; // Default 30 seconds
        
        // Parse go parameters
        for (int i = 1; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "depth":
                    if (i + 1 < tokens.length) {
                        depth = Integer.parseInt(tokens[i + 1]);
                        i++;
                    }
                    break;
                case "movetime":
                    if (i + 1 < tokens.length) {
                        timeLimit = Long.parseLong(tokens[i + 1]);
                        i++;
                    }
                    break;
                case "wtime":
                    if (i + 1 < tokens.length && board.getSideToMove() == PieceColor.WHITE) {
                        long wtime = Long.parseLong(tokens[i + 1]);
                        timeLimit = calculateTimeLimit(wtime, tokens, i);
                        i++;
                    }
                    break;
                case "btime":
                    if (i + 1 < tokens.length && board.getSideToMove() == PieceColor.BLACK) {
                        long btime = Long.parseLong(tokens[i + 1]);
                        timeLimit = calculateTimeLimit(btime, tokens, i);
                        i++;
                    }
                    break;
                case "infinite":
                    timeLimit = Long.MAX_VALUE;
                    break;
            }
        }
        final int finalDepth = depth;
        final long finalTimeLimit = timeLimit;
        // Search in a separate thread to allow stop command
        Thread searchThread = new Thread(() -> {
            List<Long> history = board.getPositionHashes();
            SearchEngine.SearchResult result = searchEngine.search(board, finalDepth, finalTimeLimit, history);
            
            if (result.bestMove() != null) {
                System.out.println("bestmove " + result.bestMove().toUCI());
            } else {
                // No legal moves (checkmate or stalemate)
                System.out.println("bestmove 0000");
            }
        });
        
        searchThread.start();
    }
    
    /**
     * Calculates time limit for the current move.
     */
    private long calculateTimeLimit(long timeRemaining, String[] tokens, int currentIndex) {
        // Simple time management: use 1/30 of remaining time
        long timeForMove = timeRemaining / 30;
        
        // Look for increment
        for (int i = currentIndex + 1; i < tokens.length - 1; i++) {
            if ((tokens[i].equals("winc") && board.getSideToMove() == PieceColor.WHITE) ||
                (tokens[i].equals("binc") && board.getSideToMove() == PieceColor.BLACK)) {
                long increment = Long.parseLong(tokens[i + 1]);
                timeForMove += increment / 2;
                break;
            }
        }
        
        // Ensure minimum time
        return Math.max(timeForMove, 100);
    }
    
    /**
     * Handles the 'stop' command.
     */
    private void handleStop() {
        searchEngine.stop();
    }
    
    /**
     * Handles the 'quit' command.
     */
    private void handleQuit() {
        running = false;
        searchEngine.stop();
    }
    
    /**
     * Handles the 'setoption' command.
     * Format: setoption name <name> value <value>
     */
    private void handleSetOption(String[] tokens) {
        if (tokens.length < 5) return;
        
        String optionName = "";
        String optionValue = "";
        
        // Parse option name and value
        int nameIndex = -1;
        int valueIndex = -1;
        
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("name")) nameIndex = i + 1;
            if (tokens[i].equals("value")) valueIndex = i + 1;
        }
        
        if (nameIndex > 0 && nameIndex < tokens.length) {
            optionName = tokens[nameIndex];
        }
        
        if (valueIndex > 0 && valueIndex < tokens.length) {
            optionValue = tokens[valueIndex];
        }
        
        // Apply option
        switch (optionName) {
            case "Hash":
                try {
                    hashSize = Integer.parseInt(optionValue);
                    searchEngine = new SearchEngine(hashSize);
                } catch (NumberFormatException e) {
                    // Ignore invalid value
                }
                break;
            case "SearchDepth":
                try {
                    searchDepth = Integer.parseInt(optionValue);
                } catch (NumberFormatException e) {
                    // Ignore invalid value
                }
                break;
        }
    }
    
    /**
     * Handles the 'eval' command (custom command for debugging).
     */
    private void handleEval() {
        int eval = Evaluator.evaluate(board);
        System.out.println("Evaluation: " + eval + " centipawns");
        
        boolean whiteInCheck = CheckValidator.isKingInCheck(board, PieceColor.WHITE);
        boolean blackInCheck = CheckValidator.isKingInCheck(board, PieceColor.BLACK);
        
        System.out.println("White in check: " + whiteInCheck);
        System.out.println("Black in check: " + blackInCheck);
        
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
        System.out.println("Legal moves: " + legalMoves.size());
    }
    
    /**
     * Parses a move in UCI format.
     */
    private Move parseMove(String uciMove) {
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
        
        for (Move move : legalMoves) {
            if (move.toUCI().equals(uciMove)) {
                return move;
            }
        }
        
        return null;
    }
    
    /**
     * Main entry point for UCI mode.
     */
    public static void main(String[] args) {
        UCIInterface uci = new UCIInterface();
        uci.run();
    }
}
