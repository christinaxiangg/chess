# Chess board.BitBoard Engine - Java Implementation

A production-level chess bitboard implementation in Java with efficient piece tracking and an intuitive array-like interface.

## Features

- **Bitboard representation** for efficient board state and move generation
- **piece.Piece tracking** with O(1) lookup and fast iteration over specific piece types
- **Array-like interface** for intuitive access to board squares
- **FEN support** for importing/exporting positions
- **move.Move encoding** with compact integer representation
- **Full chess rules** including castling, en passant, and promotions
- **Immutable moves** with proper encoding of special moves

## Core Classes

### board.BitBoard
The main board representation combining bitboards with mailbox array for best of both worlds.

**Key Methods:**

```java
import board.BitBoard;
import piece.Piece;
import piece.PieceColor;

// Create boards
BitBoard board = BitBoard.startingPosition();
        BitBoard board = BitBoard.fromFEN("fen-string");

        // Array-like access
        Piece piece = board.getPiece(square);          // Get piece at square (0-63)
        Piece piece = board.getPiece(file, rank);      // Get piece at file/rank
board.

        setPiece(square, piece);                  // Set piece at square

// Make moves
board.

        makeMove(move);

        // piece.Piece tracking - get all pieces without scanning whole board
        List<Integer> pawns = board.getPieces(Piece.WHITE_PAWN);
        List<Integer> whitePieces = board.getPieces(PieceColor.WHITE);

        // Bitboard access for advanced move generation
        long whitePawns = board.getBitboard(Piece.WHITE_PAWN);
        long allPieces = board.getAllPieces();
        long whitePieces = board.getWhitePieces();

        // Game state
        PieceColor turn = board.getSideToMove();
        boolean canCastle = board.canCastle(BitBoard.WHITE_KING_SIDE);
        int epSquare = board.getEnPassantSquare();

        // Utility
        String fen = board.toFEN();
        BitBoard copy = board.copy();
board.

        print();  // Pretty print to console
```

### move.Move
Compact move representation with special move encoding.

**Key Methods:**

```java
import move.Move;
import piece.PieceType;

// Create moves
Move move = new Move(from, to, flags);
        Move move = Move.fromUCI("e2e4");

        // Query move properties
        int from = move.getFrom();
        int to = move.getTo();
        boolean isCapture = move.isCapture();
        boolean isPromotion = move.isPromotion();
        boolean isCastling = move.isCastling();
        boolean isEnPassant = move.isEnPassant();
        PieceType promoted = move.getPromotionPieceType();

        // Convert to UCI
        String uci = move.toUCI();  // "e2e4", "e7e8q", etc.

// move.Move flags
        Move.QUIET_MOVE
        Move.DOUBLE_PAWN_PUSH
        Move.KING_CASTLE
        Move.QUEEN_CASTLE
        Move.CAPTURE
        Move.EP_CAPTURE
        Move.KNIGHT_PROMOTION
        Move.QUEEN_PROMOTION
// ... etc
```

### piece.Piece
Enum representing all chess pieces with color and type.

**Available Pieces:**

```java
import piece.Piece;

Piece.WHITE_PAWN,Piece.WHITE_KNIGHT,Piece.WHITE_BISHOP,
Piece.WHITE_ROOK,Piece.WHITE_QUEEN,Piece.WHITE_KING
Piece.BLACK_PAWN,Piece.BLACK_KNIGHT,Piece.BLACK_BISHOP,
Piece.BLACK_ROOK,Piece.BLACK_QUEEN,Piece.BLACK_KING

// piece.Piece properties
piece.

getColor()     // piece.PieceColor.WHITE or piece.PieceColor.BLACK
piece.

getType()      // piece.PieceType enum
piece.

getSymbol()    // 'P', 'N', 'B', 'R', 'Q', 'K', 'p', 'n', etc.
piece.

getValue()     // Material value in centipawns
```

## Usage Examples

### Basic Setup

```java
import board.BitBoard;

// Starting position
BitBoard board = BitBoard.startingPosition();

// Custom position
BitBoard board = BitBoard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
```

### Accessing Pieces

```java
import piece.Piece;

// Get piece at specific square
Piece piece = board.getPiece(28);  // Square 28 (e4)

// Or use file/rank (more readable)
Piece piece = board.getPiece(4, 3);  // file e, rank 4 (e4)

// Check if square is occupied
if(board.

isOccupied(28)){
        System.out.

println("Square e4 is occupied");
}
```

### Making Moves

```java
import move.Move;
import piece.PieceType;

// Create move from UCI
Move move = Move.fromUCI("e2e4");
board.

        makeMove(move);

        // Create move manually
        int from = Move.stringToSquare("e2");
        int to = Move.stringToSquare("e4");
        Move move = new Move(from, to, Move.DOUBLE_PAWN_PUSH);
board.

        makeMove(move);

        // Capture
        Move capture = new Move(from, to, Move.CAPTURE, PieceType.PAWN);

        // Promotion
        Move promotion = new Move(from, to, Move.QUEEN_PROMOTION);

        // Castling
        Move castle = new Move(from, to, Move.KING_CASTLE);
```

### piece.Piece Tracking

```java
import move.Move;
import piece.Piece;
import piece.PieceColor;

// Get all pieces of a specific type (no board scanning needed!)
List<Integer> whitePawns = board.getPieces(Piece.WHITE_PAWN);
for(
        int square :whitePawns){
        System.out.

        println("White pawn at "+Move.squareToString(square));
        }

        // Get all pieces of a color
        List<Integer> blackPieces = board.getPieces(PieceColor.BLACK);

// Iterate over specific piece type for move generation
for(
        int square :board.

        getPieces(Piece.WHITE_KNIGHT)){
        // Generate knight moves from this square
        }
```

### Bitboard Access

```java
import piece.Piece;

// Get bitboard for specific piece
long whitePawns = board.getBitboard(Piece.WHITE_PAWN);

// Occupancy bitboards
long whitePieces = board.getWhitePieces();
long blackPieces = board.getBlackPieces();
long allPieces = board.getAllPieces();

// Use bitboards for efficient move generation
long attacks = generateKnightAttacks(square) & board.getBlackPieces();
```

### Integration with Search

```java
import board.BitBoard;
import move.Move;
import piece.Piece;

public class ChessEngine {
    private BitBoard board;

    public int search(int depth) {
        if (depth == 0) return evaluate();

        int best = Integer.MIN_VALUE;
        List<Move> moves = generateMoves(board);

        for (Move move : moves) {
            BitBoard copy = board.copy();  // Fast copy for search
            copy.makeMove(move);
            int score = -search(depth - 1);
            best = Math.max(best, score);
        }

        return best;
    }

    private int evaluate() {
        int score = 0;
        // Use piece tracking for efficient material evaluation
        for (Piece piece : Piece.values()) {
            int count = board.getPieces(piece).size();
            int value = piece.getValue();
            score += piece.isWhite() ? count * value : -count * value;
        }
        return score;
    }
}
```

## Board Coordinate System

Squares are indexed 0-63:
```
  a  b  c  d  e  f  g  h
8|56 57 58 59 60 61 62 63|8
7|48 49 50 51 52 53 54 55|7
6|40 41 42 43 44 45 46 47|6
5|32 33 34 35 36 37 38 39|5
4|24 25 26 27 28 29 30 31|4
3|16 17 18 19 20 21 22 23|3
2|08 09 10 11 12 13 14 15|2
1|00 01 02 03 04 05 06 07|1
  a  b  c  d  e  f  g  h
```

Convert between formats:

```java
import move.Move;

int square = Move.stringToSquare("e4");     // 28
String algebraic = Move.squareToString(28);  // "e4"

// File and rank
int file = square & 7;      // 0-7 (a-h)
int rank = square >>> 3;    // 0-7 (1-8)
int square = rank * 8 + file;
```

## Performance Features

1. **O(1) piece lookup** - Mailbox array for instant piece access
2. **Efficient piece iteration** - piece.Piece lists avoid scanning empty squares
3. **Bitboard operations** - Fast move generation with bitwise operations
4. **Compact move encoding** - Moves stored as single integer
5. **Fast board copy** - Efficient for search tree exploration

## Compilation and Running

```bash
# Compile all files
javac *.java

# Run demo
java search.ChessDemo
```

## Integration with Your Search

Since you already have the search implementation, you can integrate this board like:

```java
import board.BitBoard;
import move.Move;

public class YourSearchEngine {
    public Move findBestMove(BitBoard position, int depth) {
        // Your alpha-beta search here
        // Use position.getPieces() for move generation
        // Use position.copy() for making trial moves
        // Use position.makeMove() to apply moves
    }
}
```

The board provides everything you need:
- Fast piece access with `getPiece(square)`
- Efficient piece iteration with `getPieces(piece)`
- Bitboard access for advanced move generation
- Game state tracking (castling, en passant, etc.)
- FEN import/export for testing positions

## License

Free to use for any purpose.
