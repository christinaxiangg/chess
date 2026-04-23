# Modern Chess UI - Professional Implementation

A professional-grade chess UI implementation inspired by lichess.org and chess.com, featuring high-quality SVG piece rendering, smooth interactions, and modern visual design.

## Features

### 🎨 Professional Appearance
- **SVG-based piece rendering** using the CBurnett piece set (same as lichess.org)
- **Multiple color schemes**: Lichess Blue, Chess.com Green, Classic Brown
- **High-quality antialiasing** for crisp, sharp pieces at any size
- **Professional board styling** with coordinate labels (a-h, 1-8)

### 🖱️ Interactive Features
- **Drag and drop** - Click and drag pieces smoothly across the board
- **Valid move indicators** - Circular dots for regular moves, rings for captures
- **Last move highlighting** - Yellow highlight on the previous move
- **Hover effects** - Subtle highlighting when hovering over squares
- **Smooth animations** - Professional feel with optimized rendering

### 🏗️ Architecture
- **Modular design** - Separate components for pieces, board, and game logic
- **Easy integration** - Adapters for connecting to existing chess engines
- **Flexible API** - Simple methods for updating board state
- **Performance optimized** - Efficient rendering with no lag

## Components

### 1. EnhancedSVGPieces.java
**Professional piece renderer using SVG paths**

```java
// Draw a piece at specific location
EnhancedSVGPieces.drawPiece(g2d, "wK", x, y, size);

// Draw with opacity (for drag effects)
EnhancedSVGPieces.drawPiece(g2d, "bQ", x, y, size, 0.7f);

// Piece codes: wK, wQ, wR, wB, wN, wP (white)
//              bK, bQ, bR, bB, bN, bP (black)
```

**Key Features:**
- Renders pieces using authentic CBurnett SVG paths
- Automatic scaling to any size
- High-quality antialiasing
- Support for transparency/opacity
- Proper centering and alignment

### 2. ModernChessBoard.java
**Complete chess board UI component**

```java
// Create board with default theme
ModernChessBoard board = new ModernChessBoard();

// Create with specific theme
ModernChessBoard board = new ModernChessBoard(
    ModernChessBoard.LICHESS_BLUE
);

// Set/get pieces
board.setPiece(0, 0, "bR");  // Set black rook at a8
String piece = board.getPiece(7, 4);  // Get piece at e1

// Change color scheme dynamically
board.setColorScheme(ModernChessBoard.CHESSCOM_GREEN);
```

**Available Color Schemes:**
- `ModernChessBoard.LICHESS_BLUE` - Blue/beige (lichess.org style)
- `ModernChessBoard.CHESSCOM_GREEN` - Green/cream (chess.com style)
- `ModernChessBoard.CLASSIC_BROWN` - Traditional brown/beige

**Board Coordinates:**
- Row 0 = Rank 8 (black's back rank)
- Row 7 = Rank 1 (white's back rank)
- Col 0 = File a
- Col 7 = File h

### 3. ChessBoardAdapter.java
**Integration adapter for chess engines**

```java
ChessBoardAdapter board = new ChessBoardAdapter();

// Connect move validation
board.setMoveValidator(new MoveValidator() {
    @Override
    public boolean isValidMove(int fromRow, int fromCol, 
                               int toRow, int toCol) {
        // Your game logic here
        return gameEngine.isLegalMove(from, to);
    }
    
    @Override
    public List<int[]> getValidMovesFor(int row, int col) {
        // Return list of valid moves
        return gameEngine.getValidMoves(row, col);
    }
});

// Handle moves
board.setMoveHandler(new MoveHandler() {
    @Override
    public void onMove(int fromRow, int fromCol, 
                      int toRow, int toCol) {
        // Execute move in your engine
        gameEngine.makeMove(from, to);
    }
    
    @Override
    public void onPieceSelected(int row, int col) {
        // Handle piece selection
        highlightValidMoves(row, col);
    }
});

// Sync with game state
board.updateFromPieceArray(gameEngine.getBoard());
```

### 4. ChessUIDemo.java
**Complete demo application**

Run this to see all features in action:
```java
java UI.ChessUIDemo
```

## Quick Start

### Basic Usage

```java
import UI.ModernChessBoard;
import javax.swing.*;

public class MyChessGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create window
            JFrame frame = new JFrame("My Chess Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Create board
            ModernChessBoard board = new ModernChessBoard(
                ModernChessBoard.LICHESS_BLUE
            );
            
            // Add to window
            frame.add(board);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

### Integration with Existing Chess Engine

```java
import UI.ChessBoardAdapter;
import piece.Piece;

public class ChessGame {
    private ChessBoardAdapter board;
    private YourChessEngine engine;
    
    public ChessGame() {
        // Initialize board
        board = new ChessBoardAdapter();
        engine = new YourChessEngine();
        
        // Connect validation
        board.setMoveValidator(new ChessBoardAdapter.MoveValidator() {
            public boolean isValidMove(int fr, int fc, int tr, int tc) {
                return engine.isValidMove(fr, fc, tr, tc);
            }
            
            public List<int[]> getValidMovesFor(int r, int c) {
                return engine.getValidMoves(r, c);
            }
        });
        
        // Connect move handling
        board.setMoveHandler(new ChessBoardAdapter.MoveHandler() {
            public void onMove(int fr, int fc, int tr, int tc) {
                engine.makeMove(fr, fc, tr, tc);
                board.updateFromPieceArray(engine.getBoard());
            }
            
            public void onPieceSelected(int r, int c) {
                // Custom selection logic
            }
        });
    }
}
```

## Color Scheme Customization

Create your own color scheme:

```java
ModernChessBoard.ColorScheme myScheme = new ModernChessBoard.ColorScheme(
    new Color(240, 217, 181),  // Light square
    new Color(181, 136, 99),   // Dark square
    new Color(205, 210, 106),  // Highlight light
    new Color(170, 162, 58),   // Highlight dark
    new Color(249, 248, 113),  // Selected light
    new Color(186, 202, 68),   // Selected dark
    new Color(100, 100, 100, 120),  // Move indicator
    new Color(200, 50, 50, 140)     // Capture indicator
);

board.setColorScheme(myScheme);
```

## Best Practices

### Performance
- The board automatically handles rendering optimization
- Piece SVG paths are cached for fast rendering
- Drag operations use opacity for smooth visual feedback

### Styling
- Use the predefined color schemes for consistency
- All pieces scale automatically to square size
- Coordinate labels adjust to square colors for visibility

### Integration
- Keep board state synchronized with your chess engine
- Use the adapter pattern for clean separation of concerns
- Implement move validation in your engine, not in the UI

## API Reference

### ModernChessBoard Methods

```java
// Piece management
void setPiece(int row, int col, String pieceCode)
String getPiece(int row, int col)

// Visual customization
void setColorScheme(ColorScheme scheme)

// Board state
void clearHighlights()
void highlightMove(int fromRow, int fromCol, int toRow, int toCol)
```

### EnhancedSVGPieces Methods

```java
// Draw piece
void drawPiece(Graphics2D g2d, String pieceCode, 
               int x, int y, int size)

// Draw with opacity
void drawPiece(Graphics2D g2d, String pieceCode, 
               int x, int y, int size, float opacity)

// Get piece code
String getPieceCode(boolean isWhite, String pieceType)
```

## Piece Code Reference

| Code | Piece        | Code | Piece        |
|------|-------------|------|-------------|
| wK   | White King  | bK   | Black King  |
| wQ   | White Queen | bQ   | Black Queen |
| wR   | White Rook  | bR   | Black Rook  |
| wB   | White Bishop| bB   | Black Bishop|
| wN   | White Knight| bN   | Black Knight|
| wP   | White Pawn  | bP   | Black Pawn  |

## Requirements

- Java 8 or higher
- Swing (included in JDK)
- No external dependencies required

## License

This is an enhanced implementation for educational and professional use.
The CBurnett piece set paths are based on the open-source lichess piece set.

## Credits

- Piece designs based on CBurnett set (used by lichess.org)
- Color schemes inspired by lichess.org and chess.com
- Implementation optimized for professional chess applications

---

**Enjoy building your chess application with professional-quality UI!**
