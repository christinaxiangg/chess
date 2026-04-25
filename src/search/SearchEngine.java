package search;
import java.util.List;
import java.util.ArrayList;
import board.BitBoard;
import board.Evaluator;
import board.ZobristHash;
import move.*;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;

/**
 * Production-level chess search engine with:
 * - Alpha-Beta pruning with Principal Variation Search (PVS)
 * - Null move.Move Pruning
 * - Aspiration Windows
 * - Transposition Table
 * - Killer move.Move Heuristic
 * - Counter move.Move Heuristic
 * - History Heuristic
 * - Iterative Deepening
 * - Quiescence Search
 */
public class SearchEngine {
    
    // Search limits
    private static final int MAX_PLY = 128;
    private static final int ASPIRATION_WINDOW = 50;
    
    // Null move pruning parameters
    private static final int NULL_MOVE_REDUCTION = 2;
    private static final int NULL_MOVE_MIN_DEPTH = 3;
    
    // Late move reduction parameters
    private static final int LMR_MIN_DEPTH = 3;
    private static final int LMR_FULL_DEPTH_MOVES = 4;
    private static final int LMR_REDUCTION_BASE = 1;
    private static final int LMR_REDUCTION_SCALING = 2;  // For adaptive LMR at deeper depths
    
    // Search data structures
    private final TranspositionTable transpositionTable;
    private final int[][] historyTable;           // [from][to]
    private final Move[][] killerMoves;           // [ply][slot]
    private final Move[] counterMoves;            // [from*64 + to]
    
    // Search statistics
    private long nodesSearched;
    private long qNodesSearched;
    private long ttHits;
    private long ttCutoffs;
    private long betaCutoffs;
    private long nullMoveCutoffs;
    
    // Search control
    private boolean stopSearch;
    private long startTime;
    private long timeLimit;
    
    // Best move found
    private Move bestMove;
    private int bestScore;
    
    // Selective depth tracking for deep searches
    private int selectiveDepth;

    private final OpeningBook openingBook = OpeningBook.getInstance();

    /**


    /**
     * Creates a new search engine with specified TT size.
     */
    public SearchEngine(int ttSizeInMB) {
        this.transpositionTable = new TranspositionTable(ttSizeInMB);
        this.historyTable = new int[64][64];
        this.killerMoves = new Move[MAX_PLY][2];
        this.counterMoves = new Move[64 * 64];
        resetStatistics();
    }
    
    /**
     * Searches for the best move in the given position.
     */
    public SearchResult search(BitBoard board, int depthLimit, long timeLimitMs) {

        // ── Opening book probe ──────────────────────────────────────────────
        if (openingBook != null && openingBook.isLoaded() && board.getFullMoveNumber() <= 12) {
            Move bookMove = openingBook.probe(board);
            if (bookMove != null) {
                System.out.println("info string Book move: " + bookMove.toUCI());
                // Score 0 is conventional for book moves.
                return new SearchResult(bookMove, 0, 0, 0);
            }
        }
        // ── End book probe ──────────────────────────────────────────────────

        this.startTime = System.currentTimeMillis();
        this.timeLimit = timeLimitMs;
        this.stopSearch = false;
        this.bestMove = null;
        this.bestScore = 0;
        this.selectiveDepth = 0;
        
        resetSearchData();
        resetStatistics();
        transpositionTable.incrementAge();
        
        // Iterative deepening
        int score = 0;
        for (int depth = 1; depth <= depthLimit && !stopSearch; depth++) {
            // Use aspiration windows for depths > 1
            if (depth > 1) {
                score = aspirationSearch(board, depth, score);
            } else {
                score = alphaBeta(board, depth, 0, -Evaluator.CHECKMATE_SCORE,
                                Evaluator.CHECKMATE_SCORE, true);
            }
            
            if (stopSearch) {
                break;
            }
            
            bestScore = score;
            
            // Print search info
            long elapsed = System.currentTimeMillis() - startTime;
            long nps = (nodesSearched * 1000) / Math.max(elapsed, 1);
            
            System.out.printf("info depth %d seldepth %d score cp %d nodes %d time %d nps %d pv %s\n",
                            depth, selectiveDepth, score, nodesSearched, elapsed, nps, 
                            bestMove != null ? bestMove.toUCI() : "none");
        }
        
        return new SearchResult(bestMove, bestScore, nodesSearched, qNodesSearched);
    }
    
    /**
     * Aspiration window search.
     */
    private int aspirationSearch(BitBoard board, int depth, int previousScore) {
        int alpha = previousScore - ASPIRATION_WINDOW;
        int beta = previousScore + ASPIRATION_WINDOW;
        int score = alphaBeta(board, depth, 0, alpha, beta, true);
        
        // If we fail low or high, research with full window
        if (score <= alpha || score >= beta) {
            score = alphaBeta(board, depth, 0, -Evaluator.CHECKMATE_SCORE, 
                            Evaluator.CHECKMATE_SCORE, true);
        }
        
        return score;
    }
    
    /**
     * Alpha-Beta search with PVS (Principal Variation Search).
     */
    private int alphaBeta(BitBoard board, int depth, int ply, int alpha, int beta, boolean allowNull) {
        // Update selective depth tracking
        if (ply > selectiveDepth) {
            selectiveDepth = ply;
        }
        
        // Check time limit
        if (shouldStop()) {
            return 0;
        }
        
        // CRITICAL: Prevent search explosion at very deep plies
        if (ply >= MAX_PLY - 1) {
            return Evaluator.evaluate(board);
        }
        
        // Check for draw by repetition or fifty-move rule
        if (ply > 0 && (board.getHalfMoveClock() >= 100 || isRepetition(board))) {
            return Evaluator.STALEMATE_SCORE;
        }
        
        boolean isPVNode = beta - alpha > 1;
        boolean inCheck = CheckValidator.isKingInCheck(board, board.getSideToMove());
        // Temporary sanity check
        if (!inCheck) {
            // Verify using MoveGenerator's own isSquareAttacked
            PieceColor us = board.getSideToMove();
            Piece ourKing = us == PieceColor.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
            List<Integer> kingSquares = board.getPieces(ourKing);
            if (!kingSquares.isEmpty()) {
                boolean crossCheck = MoveGenerator.isSquareAttacked(board, kingSquares.get(0), us.opposite());
                if (crossCheck) {
                    System.out.println("BUG: CheckValidator says not in check but isSquareAttacked disagrees!");
                    System.out.println("FEN: " + board.toFEN());
                }
            }
        }
        // Check extension - FIXED: Now correctly checks ply instead of depth
        if (inCheck) {
            depth++;
        }
        
        // Quiescence search at leaf nodes
        if (depth <= 0) {
            return quiescence(board, ply, alpha, beta);
        }
        
        nodesSearched++;
        
        // Probe transposition table
        long zobristKey = ZobristHash.computeHash(board);
        TranspositionTable.TTEntry ttEntry = transpositionTable.probe(zobristKey);
        Move ttMove = null;
        
        if (ttEntry != null) {
            ttHits++;
            ttMove = ttEntry.bestMove;

            // Use TT score if depth is sufficient and not a PV node
            if (ttMove != null && ttEntry.depth >= depth && !isPVNode) {
                int ttScore = ttEntry.score;
                if (ttScore > Evaluator.CHECKMATE_SCORE - 1000) {
                    ttScore -= ply;
                } else if (ttScore < -Evaluator.CHECKMATE_SCORE + 1000){
                    ttScore += ply;
                }

                switch (ttEntry.type) {
                    case EXACT:
                        return ttScore;
                    case LOWER_BOUND:
                        if (ttScore >= beta) {
                            ttCutoffs++;
                            return ttScore;
                        }
                        alpha = Math.max(alpha, ttScore);
                        break;
                    case UPPER_BOUND:
                        if (ttScore <= alpha) {
                            return ttScore;
                        }
                        beta = Math.min(beta, ttScore);
                        break;
                }
            }
        }
        
        // Null move pruning
        if (allowNull && !isPVNode && !inCheck && depth >= NULL_MOVE_MIN_DEPTH && hasNonPawnMaterial(board)) {
            board.makeNullMove();
            int nullScore = -alphaBeta(board, depth - 1 - NULL_MOVE_REDUCTION, ply + 1, 
                                      -beta, -beta + 1, false);
            board.undoNullMove();
            if (nullScore >= beta) {
                nullMoveCutoffs++;
                return beta;
            }
        }
        
        // Generate and order moves
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        
        if (moves.isEmpty()) {
            // Checkmate or stalemate
            if (inCheck) {
                return Evaluator.matedScore(ply);
            } else {
                return Evaluator.STALEMATE_SCORE;
            }
        }
        
        // Get counter move
        Move counterMove = null;
        
        MoveOrdering.orderMoves(board, moves, ttMove, killerMoves[ply], counterMove, historyTable);
        
        // Search moves
        Move bestMoveFound = null;
        int bestScoreFound = -Evaluator.CHECKMATE_SCORE;
        TranspositionTable.EntryType entryType = TranspositionTable.EntryType.UPPER_BOUND;
        boolean raisedAlpha = false;
        
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            board.makeMove(move);
            
            // Verify move legality (king not in check after the move) redundant check, but safer than relying on MoveGenerator's legality checks alone
//            if (CheckValidator.isKingInCheck(board, board.getSideToMove().opposite())) {
//                board.undoMakeMove(move);
//                continue;
//            }
            int score;
            
            // Principal Variation Search
            if (i == 0) {
                // Search first move with full window
                score = -alphaBeta(board, depth - 1, ply + 1, -beta, -alpha, true);
            } else {
                // Enhanced Late Move Reduction (LMR) - scales with depth for deeper searches
                int reduction = 0;
                if (depth >= LMR_MIN_DEPTH && i >= LMR_FULL_DEPTH_MOVES && 
                    !inCheck && !move.isCapture() && !move.isPromotion()) {
                    
                    // Adaptive reduction: more aggressive at higher depths
                    reduction = LMR_REDUCTION_BASE;
                    if (depth >= 6) {
                        reduction += (depth - 6) / LMR_REDUCTION_SCALING;
                    }
                    // Cap reduction to prevent over-pruning
                    reduction = Math.min(reduction, depth - 1);
                }
                
                // Try null window search first
                score = -alphaBeta(board, depth - 1 - reduction, ply + 1, -alpha - 1, -alpha, true);
                
                // If it fails high and we reduced, search again at full depth
                if (score > alpha && reduction > 0) {
                    score = -alphaBeta(board, depth - 1, ply + 1, -beta, -alpha, true);
                }
                
                // If still fails high, do full window search
                if (score > alpha && score < beta) {
                    score = -alphaBeta(board, depth - 1, ply + 1, -beta, -alpha, true);
                }
            }
            // CRITICAL: Undo the move before processing results
            board.undoMakeMove(move);
            
            if (score > bestScoreFound) {
                bestScoreFound = score;
                bestMoveFound = move;
                
                // Always track the best move at root so we have a valid move even if time expires
                if (ply == 0) {
                    bestMove = move;
                }
                
                if (score > alpha) {
                    alpha = score;
                    raisedAlpha = true;
                    entryType = TranspositionTable.EntryType.EXACT;
                }
                
                if (alpha >= beta) {
                    betaCutoffs++;
                    entryType = TranspositionTable.EntryType.LOWER_BOUND;
                    
                    // Update killer moves for non-captures
                    if (!move.isCapture()) {
                        updateKillerMoves(move, ply);
                        updateHistoryTable(move, depth);
                    }
                    
                    break; // Beta cutoff
                }
            }
        }
        
        // Store in transposition table
        transpositionTable.store(zobristKey, depth, bestScoreFound,  ply, entryType, bestMoveFound);
        
        return bestScoreFound;
    }
    
    /**
     * Quiescence search to avoid horizon effect.
     * Enhanced with deeper ply limit for depth 64+ searches.
     */
    private int quiescence(BitBoard board, int ply, int alpha, int beta) {
        qNodesSearched++;
        
        // Update selective depth
        if (ply > selectiveDepth) {
            selectiveDepth = ply;
        }
        
        // Check time limit
        if (shouldStop()) {
            return 0;
        }
        
        // Prevent quiescence explosion
        if (ply >= MAX_PLY - 1) {
            return Evaluator.evaluate(board);
        }
        
        // Stand pat score
        int standPat = Evaluator.evaluate(board);
        
        if (standPat >= beta) {
            return beta;
        }
        
        if (standPat > alpha) {
            alpha = standPat;
        }
        
        // Generate capture moves only
        List<Move> captureMoves = generateCaptureMoves(board);
        
        // Delta pruning: skip if we can't possibly reach alpha
// 1. Calculate the maximum possible gain from any single move.
        // We use Queen Value + a safety margin (e.g., 200).
        int safetyMargin = 200;
        int maxGain = PieceType.QUEEN.getValue() + safetyMargin;

        // 2. If even gaining a Queen doesn't help reach alpha, we can safely prune.
        // CRITICAL: Only prune if the side to move is NOT in check.
        boolean inCheck = CheckValidator.isKingInCheck(board, board.getSideToMove());
        if (!inCheck && (standPat + maxGain < alpha)) {
            return alpha;
        }
        
        for (Move move : captureMoves) {
            // OPTIMIZED: makeMove/undoMakeMove instead of board.copy()
            board.makeMove(move);
            
            // Prune if move leaves king in check (illegal) - seems redundant, comment out for now
//            if (CheckValidator.isKingInCheck(board, board.getSideToMove().opposite())) {
//                board.undoMakeMove(move);
//                continue;
//            }
            
            int score = -quiescence(board, ply + 1, -beta, -alpha);
            
            // CRITICAL: Undo the move
            board.undoMakeMove(move);
            
            if (score >= beta) {
                return beta;
            }
            
            if (score > alpha) {
                alpha = score;
            }
        }
        
        return alpha;
    }
    
    /**
     * Generates only capture moves.
     */
    private List<Move> generateCaptureMoves(BitBoard board) {
        List<Move> allMoves = MoveGenerator.generateLegalMoves(board);
        List<Move> captures = new ArrayList<>();
        
        for (Move move : allMoves) {
            if (move.isCapture() || move.isPromotion()) {
                captures.add(move);
            }
        }
        
        return captures;
    }
    
    /**
     * Updates killer move heuristic.
     */
    private void updateKillerMoves(Move move, int ply) {
        if (ply >= MAX_PLY) return;
        
        // Don't add the same move twice
        if (killerMoves[ply][0] != null && killerMoves[ply][0].equals(move)) {
            return;
        }
        
        // Shift moves down
        killerMoves[ply][1] = killerMoves[ply][0];
        killerMoves[ply][0] = move;
    }
    
    /**
     * Updates history table for move ordering.
     */
    private void updateHistoryTable(Move move, int depth) {
        int from = move.getFrom();
        int to = move.getTo();
        
        // Bonus based on depth (deeper = more important)
        historyTable[from][to] += depth * depth;
        
        // Cap the history value to prevent overflow
        if (historyTable[from][to] > 1000000) {
            historyTable[from][to] = 1000000;
        }
    }
    
    /**
     * Checks if position has non-pawn material.
     */
    private boolean hasNonPawnMaterial(BitBoard board) {
        PieceColor color = board.getSideToMove();
        for (Piece piece : Piece.values()) {
            if (piece.getColor() == color && 
                piece.getType() != PieceType.PAWN && 
                piece.getType() != PieceType.KING &&
                !board.getPieces(piece).isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Simple repetition detection (should be improved with position history).
     */
    private boolean isRepetition(BitBoard board) {
        // TODO: Implement proper repetition detection with position history
        return false;
    }
    
    /**
     * Checks if search should stop.
     */
    private boolean shouldStop() {
        if (stopSearch) {
            return true;
        }
        
        // Check time every 1024 nodes
        if ((nodesSearched & 1023) == 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeLimit) {
                stopSearch = true;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Resets search data structures.
     */
    private void resetSearchData() {
        // Reset history table
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                historyTable[i][j] = 0;
            }
        }
        
        // Reset killer moves
        for (int i = 0; i < MAX_PLY; i++) {
            killerMoves[i][0] = null;
            killerMoves[i][1] = null;
        }
        
        // Reset counter moves
        for (int i = 0; i < 64 * 64; i++) {
            counterMoves[i] = null;
        }
    }
    
    /**
     * Resets search statistics.
     */
    private void resetStatistics() {
        nodesSearched = 0;
        qNodesSearched = 0;
        ttHits = 0;
        ttCutoffs = 0;
        betaCutoffs = 0;
        nullMoveCutoffs = 0;
    }
    
    /**
     * Stops the current search.
     */
    public void stop() {
        stopSearch = true;
    }
    
    /**
     * Gets search statistics.
     */
    public SearchStatistics getStatistics() {
        return new SearchStatistics(nodesSearched, qNodesSearched, ttHits, 
                                   ttCutoffs, betaCutoffs, nullMoveCutoffs);
    }
    
    /**
     * Clears the transposition table.
     */
    public void clearTranspositionTable() {
        transpositionTable.clear();
    }

    /**
         * Result of a search.
         */
        public record SearchResult(Move bestMove, int score, long nodesSearched, long qNodesSearched) {
    }

    /**
         * Search statistics.
         */
        public record SearchStatistics(long nodesSearched, long qNodesSearched, long ttHits, long ttCutoffs,
                                       long betaCutoffs, long nullMoveCutoffs) {

        @Override
            public String toString() {
                return String.format(
                        "Nodes: %d (Q: %d), TT Hits: %d, TT Cutoffs: %d, Beta Cutoffs: %d, NMP Cutoffs: %d",
                        nodesSearched, qNodesSearched, ttHits, ttCutoffs, betaCutoffs, nullMoveCutoffs
                );
            }
        }
}
