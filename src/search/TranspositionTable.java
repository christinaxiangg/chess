package search;
import piece.PieceColor;
import piece.Piece;
import piece.PieceType;
import move.Move;

/**
 * Transposition table for storing previously evaluated positions.
 * Uses Zobrist hashing for efficient position identification and a replacement scheme
 * to manage table entries when collisions occur.
 */
public class TranspositionTable {
    
    /**
     * Entry types for transposition table.
     */
    public enum EntryType {
        EXACT,      // Exact score (PV node)
        LOWER_BOUND, // Beta cutoff (fail-high)
        UPPER_BOUND  // Alpha cutoff (fail-low)
    }
    
    /**
     * Represents a single entry in the transposition table.
     */
    public static class TTEntry {
        public long zobristKey;     // Position hash key
        public int depth;           // Search depth
        public int score;           // Position evaluation
        public EntryType type;      // Entry type
        public Move bestMove;       // Best move found
        public int age;             // Age for replacement scheme
        
        public TTEntry() {
            this.zobristKey = 0L;
            this.depth = 0;
            this.score = 0;
            this.type = EntryType.EXACT;
            this.bestMove = null;
            this.age = 0;
        }
        
        public TTEntry(long zobristKey, int depth, int score, EntryType type, Move bestMove, int age) {
            this.zobristKey = zobristKey;
            this.depth = depth;
            this.score = score;
            this.type = type;
            this.bestMove = bestMove;
            this.age = age;
        }
        
        public boolean isValid() {
            return zobristKey != 0L;
        }
    }
    
    private final TTEntry[] table;
    private final int size;
    private int currentAge;
    
    /**
     * Creates a transposition table with the specified size in MB.
     */
    public TranspositionTable(int sizeInMB) {
        // Each entry is approximately 32 bytes
        int entrySize = 32;
        this.size = (sizeInMB * 1024 * 1024) / entrySize;
        this.table = new TTEntry[size];
        
        for (int i = 0; i < size; i++) {
            table[i] = new TTEntry();
        }
        
        this.currentAge = 0;
    }
    
    /**
     * Probes the transposition table for a position.
     */
    public TTEntry probe(long zobristKey) {
        int index = getIndex(zobristKey);
        TTEntry entry = table[index];
        
        if (entry.zobristKey == zobristKey) {
            return entry;
        }
        
        return null;
    }
    
    /**
     * Stores a position in the transposition table.
     */
    public void store(long zobristKey, int depth, int score, EntryType type, Move bestMove) {
        int index = getIndex(zobristKey);
        TTEntry entry = table[index];
        
        // Replacement scheme: replace if:
        // 1. Empty slot
        // 2. Same position
        // 3. Entry is from an older search
        // 4. New entry has greater depth
        boolean shouldReplace = !entry.isValid() ||
                               entry.zobristKey == zobristKey ||
                               entry.age < currentAge ||
                               depth > entry.depth;
        
        if (shouldReplace) {
            entry.zobristKey = zobristKey;
            entry.depth = depth;
            entry.score = score;
            entry.type = type;
            entry.bestMove = bestMove;
            entry.age = currentAge;
        }
    }
    
    /**
     * Increments the age counter for a new search.
     */
    public void incrementAge() {
        currentAge++;
    }
    
    /**
     * Clears all entries in the table.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            table[i] = new TTEntry();
        }
        currentAge = 0;
    }
    
    /**
     * Gets the index in the table for a given zobrist key.
     */
    private int getIndex(long zobristKey) {
        // Use modulo to map the key to table size
        return (int) ((zobristKey & 0x7FFFFFFFFFFFFFFFL) % size);
    }
    
    /**
     * Returns the fill percentage of the table.
     */
    public double getFillPercentage() {
        int filled = 0;
        int sampleSize = Math.min(1000, size);
        
        for (int i = 0; i < sampleSize; i++) {
            if (table[i].isValid()) {
                filled++;
            }
        }
        
        return (filled * 100.0) / sampleSize;
    }
    
    /**
     * Returns statistics about the table.
     */
    public String getStatistics() {
        return String.format("TT size: %d entries (%.1f%% filled, age: %d)", 
                           size, getFillPercentage(), currentAge);
    }
}
