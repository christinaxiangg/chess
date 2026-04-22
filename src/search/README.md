# Production-Level Chess Search Engine

## Overview

This is a production-quality chess search engine implementation featuring advanced search algorithms and optimization techniques used in modern chess engines.

## Core Components

### 1. SearchEngine.java
The main search engine implementing multiple advanced techniques:

#### Alpha-Beta Pruning with Principal Variation Search (PVS)
- **Alpha-Beta**: Efficient minimax algorithm that prunes branches that cannot affect the final decision
- **PVS**: Searches the first move with full window, then uses null-window searches for remaining moves
- **Performance**: Reduces search tree by 50-90% compared to pure minimax

#### Null move.Move Pruning (NMP)
- **Concept**: If doing nothing (passing the turn) still produces a beta cutoff, the position is too good
- **Implementation**: Reduces search depth by 2 when trying a null move
- **Conditions**: Not used in PV nodes, check positions, or when only pawns remain
- **Benefit**: Significant search speed increase, especially in mid-game positions

#### Aspiration Windows
- **Concept**: Search with a narrow window around the expected score from previous iteration
- **Implementation**: Window of ±50 centipawns around previous score
- **Fallback**: Re-search with full window if score falls outside the window
- **Benefit**: Faster searches when position evaluation is stable

#### Transposition Table
- **Purpose**: Caches previously evaluated positions to avoid re-computing them
- **Hash Function**: Zobrist hashing for fast position identification
- **Replacement Scheme**: Replaces entries based on age and depth
- **Storage**: Configurable size (default 64 MB)
- **Entry Types**: EXACT (PV node), LOWER_BOUND (beta cutoff), UPPER_BOUND (alpha cutoff)

#### move.Move Ordering
Critical for alpha-beta efficiency. Moves are ordered by:
1. **TT move.Move**: move.Move from transposition table (highest priority)
2. **Winning Captures**: MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
3. **Queen Promotions**: Usually winning moves
4. **Killer Moves**: Non-capture moves that caused beta cutoffs
5. **Counter Moves**: Moves that refute opponent's previous move
6. **Equal/Losing Captures**: Other captures
7. **History Heuristic**: Quiet moves ordered by historical success

#### Killer move.Move Heuristic
- **Concept**: Moves that caused beta cutoffs are likely to work in sibling positions
- **Implementation**: Store 2 killer moves per ply
- **Benefit**: Improves ordering of quiet moves

#### Counter move.Move Heuristic
- **Concept**: Best response to opponent's last move
- **Implementation**: Table indexed by [from_square * 64 + to_square]
- **Benefit**: Finds tactical refutations quickly

#### History Heuristic
- **Concept**: Moves that historically worked well are tried first
- **Implementation**: Table tracking move success rates, bonus based on depth²
- **Benefit**: Orders quiet moves when no tactical information available

#### Iterative Deepening
- **Concept**: Search depth 1, then 2, then 3, etc., reusing information
- **Benefits**:
  - Better move ordering from previous iterations
  - Time management (can stop at any completed depth)
  - Provides ongoing best move if time runs out

#### Quiescence Search
- **Purpose**: Prevents "horizon effect" by searching tactical sequences
- **Implementation**: Searches only captures and promotions after main search
- **Stand Pat**: Uses static evaluation as lower bound
- **Delta Pruning**: Skips searches that can't possibly affect alpha
- **SEE Pruning**: Skips obviously bad captures

#### Late move.Move Reduction (LMR)
- **Concept**: Moves ordered later are less likely to be good, so search them less deeply
- **Conditions**: Applied after first 4 moves, not to captures/promotions/checks
- **Reduction**: 1 ply initially, re-search if raises alpha
- **Benefit**: Searches more positions in the same time

### 2. TranspositionTable.java
Hash table for storing evaluated positions:
- **Zobrist Hashing**: Fast incremental hash updates
- **Entry Structure**: Key, depth, score, type, best move, age
- **Replacement Scheme**: Prefers higher depth and newer entries
- **Statistics**: Tracks hit rate and fill percentage

### 3. ZobristHash.java
Position hashing for transposition table:
- **Random Keys**: Initialized with fixed seed for reproducibility
- **Incremental Updates**: Efficient hash updates during move make/unmake
- **Components Hashed**: 
  - piece.Piece positions (12 piece types × 64 squares)
  - Castling rights (16 combinations)
  - En passant file (8 files)
  - Side to move

### 4. Evaluator.java
Position evaluation function:
- **Material**: piece.Piece values (pawn=100, knight=320, bishop=330, rook=500, queen=900)
- **piece.Piece-Square Tables**: Positional bonuses for piece placement
- **Game Phase**: Different king tables for middle-game vs endgame
- **Output**: Score in centipawns (100 cp = 1 pawn advantage)

### 5. MoveOrdering.java
Sophisticated move ordering system:
- **MVV-LVA**: Prioritizes capturing valuable pieces with less valuable attackers
- **SEE (Static Exchange Evaluation)**: Estimates if a capture is winning
- **Scoring System**: Assigns numerical scores to order moves optimally
- **Quick Sort**: Efficient sorting of moves by score

### 6. UCIInterface.java
Universal Chess Interface protocol implementation:
- **Compatible**: Works with Arena, ChessBase, and other UCI GUIs
- **Commands**: uci, isready, position, go, stop, quit, setoption
- **Time Management**: Automatic time allocation based on clock
- **Options**: Configurable hash size and search depth

## Performance Features

### Search Optimization Techniques
1. **Principal Variation Search**: Reduces nodes searched by ~40%
2. **Null move.Move Pruning**: 20-30% speed increase in middlegame
3. **Late move.Move Reduction**: Searches 2-3x more positions
4. **Transposition Table**: Avoids 60-80% of redundant evaluations
5. **move.Move Ordering**: Critical for alpha-beta efficiency (good ordering = 10x speedup)

### Typical Performance
- **Starting Position**: ~200K-500K nodes/sec (depth 6)
- **Tactical Positions**: ~100K-300K nodes/sec (depth 6)
- **Endgame**: ~500K-1M nodes/sec (depth 8+)

### Scaling with Depth
Each additional ply typically multiplies nodes by 3-5 (effective branching factor):
- Depth 1: ~20 nodes
- Depth 2: ~400 nodes
- Depth 3: ~8,000 nodes
- Depth 4: ~160,000 nodes
- Depth 5: ~3,200,000 nodes
- Depth 6: ~64,000,000 nodes

## Usage Examples

### Basic Search

```java
import board.BitBoard;

BitBoard board = BitBoard.startingPosition();
SearchEngine engine = new SearchEngine(64); // 64 MB hash

SearchEngine.SearchResult result = engine.search(board, 6, 10000);
System.out.

println("Best move: "+result.bestMove.toUCI());
        System.out.

println("Score: "+result.score +" cp");
```

### UCI Mode
```java
UCIInterface uci = new UCIInterface();
uci.run(); // Starts UCI protocol loop
```

### Analyzing a Position

```java
import board.BitBoard;

BitBoard board = BitBoard.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
SearchEngine engine = new SearchEngine(128);

// Search to depth 8 with 30 second limit
SearchEngine.SearchResult result = engine.search(board, 8, 30000);

// Get statistics
SearchEngine.SearchStatistics stats = engine.getStatistics();
System.out.

println(stats);
```

## Search Statistics

The engine tracks comprehensive statistics:
- **Nodes Searched**: Main search nodes
- **Q-Nodes Searched**: Quiescence search nodes
- **TT Hits**: Transposition table probes that found an entry
- **TT Cutoffs**: Transposition table entries that caused cutoffs
- **Beta Cutoffs**: Alpha-beta pruning cutoffs
- **Null move.Move Cutoffs**: Null move pruning cutoffs

## Configuration

### Transposition Table Size
```java
SearchEngine engine = new SearchEngine(256); // 256 MB
```
Larger tables reduce collisions but use more memory. Recommended: 64-256 MB.

### Search Depth
```java
result = engine.search(board, 10, 60000); // Depth 10, 60 sec limit
```
Typical depths:
- Fast/Blitz: 6-8 plies
- Standard: 8-10 plies
- Analysis: 12-15+ plies

### Time Management
The engine automatically allocates time based on:
- Time remaining
- Time increment
- move.Move number (TODO: improve time management)

## Testing

### Run Tests
```bash
javac *.java
java SearchEngineTest
```

### Test Positions Included
1. Starting position search
2. Tactical positions (hanging pieces, forks)
3. Mate-in-N problems
4. Complex middlegame positions
5. Endgame positions

## Future Enhancements

Potential improvements for even stronger play:
1. **Evaluation**:
   - Pawn structure analysis
   - King safety evaluation
   - piece.Piece mobility
   - Tapered evaluation (smooth transition to endgame)

2. **Search**:
   - Singular extensions
   - Multi-cut pruning
   - Probcut
   - Razoring
   - Futility pruning

3. **move.Move Ordering**:
   - Full SEE implementation
   - Better counter-move tracking
   - Capture history

4. **Time Management**:
   - Better time allocation based on position complexity
   - Panic time management
   - Node-based time management

5. **Endgame**:
   - Endgame tablebases (Syzygy)
   - Better endgame evaluation

## Architecture Notes

### Memory Usage
- **Transposition Table**: Configurable (64-1024 MB typical)
- **History Table**: ~16 KB (64×64 ints)
- **Killer Moves**: ~2 KB (128 plies × 2 slots)
- **Counter Moves**: ~16 KB (64×64 entries)

### Thread Safety
Current implementation is single-threaded. For multi-threaded search:
- Use thread-safe transposition table
- Split search tree across threads (lazy SMP)
- Shared killer/history tables

### Performance Profiling
Hot paths (most time spent):
1. move.Move generation (30-40%)
2. Position evaluation (20-30%)
3. Make/unmake move (15-25%)
4. Transposition table lookup (10-15%)

## References

### Algorithms
- Alpha-Beta Pruning: Knuth & Moore (1975)
- Principal Variation Search: Marsland & Campbell (1982)
- Null move.Move Pruning: Goetsch & Campbell (1990)
- MTD(f)/Aspiration Windows: Plaat et al. (1996)

### Chess Programming Resources
- Chess Programming Wiki: https://www.chessprogramming.org/
- Bruce Moreland's Programming Topics
- Stockfish source code (reference implementation)

## License

This code is provided for educational purposes.

## Credits

Developed using modern chess programming techniques documented in the chess programming community.
