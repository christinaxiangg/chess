#!/bin/bash

# Build script for Modern Chess UI
# This script compiles all Java files and creates a runnable demo

echo "=========================================="
echo "Modern Chess UI - Build Script"
echo "=========================================="
echo ""

# Create bin directory if it doesn't exist
mkdir -p bin

echo "Compiling Java files..."

# Compile all Java files
javac -d bin \
    EnhancedSVGPieces.java \
    ModernChessBoard.java \
    ChessBoardAdapter.java \
    ChessUIDemo.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful!"
    echo ""
    echo "To run the demo:"
    echo "  cd bin && java UI.ChessUIDemo"
    echo ""
    echo "Or use the provided run script:"
    echo "  ./run_demo.sh"
else
    echo "✗ Compilation failed!"
    exit 1
fi
