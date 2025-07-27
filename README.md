# 💣 Minesweeper Game

A Minesweeper implementation built with Python and Pygame, featuring multiple difficulty levels, intuitive gameplay, and a classic Windows-style interface.

![Minesweeper Gameplay](screenshots/GameplayScreenshot.PNG)

## 🎮 Features

- **Multiple Difficulty Levels**: From Beginner (8x8) to Expert (30x16) and custom extreme modes
- **Classic Gameplay**: Left-click to reveal, right-click to flag, middle-click for advanced clearing
- **Smart Mine Generation**: First click is always safe with intelligent mine placement
- **Visual Feedback**: Hover effects, click animations, and clear game state indicators  
- **Win/Loss Detection**: Automatic game completion with appropriate visual feedback
- **Settings Menu**: Easy difficulty switching during gameplay
- **Recursive Field Clearing**: Automatic clearing of adjacent empty squares

## 🚀 Quick Start

### Prerequisites
```bash
pip install pygame
```

### Installation & Running
```bash
git clone https://github.com/drewizcool/Minesweeper.git
cd Minesweeper
python minesweeper.py
```

## 🎯 How to Play

- **Left Click**: Reveal a square
- **Right Click**: Place/remove flag
- **Middle Click**: Quick clear adjacent squares (when flags match mine count)
- **F2**: Reset game
- **Settings Button**: Change difficulty level

## 🔧 Technical Implementation

### Core Architecture

**Grid System**: Dynamic grid generation supporting various board sizes and mine densities
```python
# Grid supports multiple difficulty configurations
DIFFICULTIES = {
    1:[240,240,10], #beginner 1
    2:[300,300,10], #beginner 2
    3:[360,360,15], #beginner 3
    4:[480,480,40], #intermediate 1
    5:[540,540,60], #intermediate 2
    6:[900,480,99], #expert
    7:[1800,900,375], #extreme 
    8:[810,420,50]}  #test
```

**Smart Algorithms**:
- **Mine Generation**: Ensures first click is never a mine
- **Flood Fill**: Recursive algorithm for clearing adjacent empty squares
- **Adjacency Detection**: Efficient neighbor calculation for mine counting
- **Binary Search**: Optimized coordinate-to-grid mapping for click detection

### Key Classes

- `Grid`: Manages game state, mine placement, and win/loss logic
- `Screen`: Handles all rendering and visual feedback
- `Button`: Reusable UI component system
- `SettingsMenu`: Dynamic difficulty selection interface

## 🎨 Visual Design

- **Classic Aesthetic**: Faithful recreation of the original Windows Minesweeper
- **Responsive UI**: Clean button interactions and visual feedback
- **State Indicators**: Clear visual representation of game states (playing, won, lost)
- **Custom Graphics**: Hand-designed sprites for mines, flags, and numbers

## 📁 Project Structure

```
Minesweeper/
├── minesweeper.py          # Main game logic and entry point
├── assets/                 # Game sprites and images
│   ├── mine.png
│   ├── flag.png
│   └── numbers/
├── screenshots/            # Demo images and GIFs
└── README.md
```

## 🛠️ Development Notes

Built as a comprehensive software development project demonstrating:
- **Object-Oriented Design**: Clean class hierarchy and separation of concerns
- **Algorithm Implementation**: Efficient pathfinding and game logic
- **User Interface Design**: Intuitive controls and visual feedback
- **Game State Management**: Robust handling of complex game states
- **Code Organization**: Modular, maintainable code structure



**Author**: Andrew Anderson  
**Created**: May 2021  
**Language**: Python 3.x  
**Framework**: Pygame

*This project demonstrates proficiency in Python programming, game development and algorithm implementation.*
