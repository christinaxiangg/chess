#!/bin/bash

# Run script for Modern Chess UI Demo

echo "=========================================="
echo "Starting Modern Chess UI Demo..."
echo "=========================================="
echo ""

# Check if compiled
if [ ! -d "bin" ]; then
    echo "Project not compiled yet. Running build script..."
    ./build.sh
    echo ""
fi

# Run the demo
cd bin && java UI.ChessUIDemo
