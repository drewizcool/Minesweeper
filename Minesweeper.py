# -*- coding: utf-8 -*-
"""
Created on Wed May 19 10:49:20 2021
Formatted for GitHub on 7/27/25

@author: Andrew Anderson
"""

import os
import pygame
import random

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

pygame.init()
pygame.display.set_caption('Minesweeper')

#==============================================================================

OFF_WHITE = (243,243,243)
GREY = (185,185,185)
LIGHT_GREY = (195,195,195)
DARK_GREY = (127,127,127)
BASICFONT = pygame.font.SysFont(None, 29)
ORIGIN = (20,60)

#==============================================================================
COORDINATES = 0
REMOVED = 1
FLAGGED = 2
MINE = 3
CLOSE_MINES = 4
#==============================================================================
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

#==============================================================================

Corner_image = pygame.image.load(os.path.join(BASE_DIR, "assets/corner.png"))
CornerFlip_image = pygame.image.load(os.path.join(BASE_DIR, "assets/cornerFlip.png"))
Square_image = pygame.image.load(os.path.join(BASE_DIR, "assets/square.png"))
Zero_image = pygame.image.load(os.path.join(BASE_DIR, "assets/NOsquare.png"))
Flag_image = pygame.image.load(os.path.join(BASE_DIR, "assets/flag.png"))
Wrong_image = pygame.image.load(os.path.join(BASE_DIR, "assets/wrong.png"))
Mine_image = pygame.image.load(os.path.join(BASE_DIR, "assets/mine.png"))
Explode_image = pygame.image.load(os.path.join(BASE_DIR, "assets/explode.png"))
Happy_image = pygame.image.load(os.path.join(BASE_DIR, "assets/happy.png"))
HappyClick_image = pygame.image.load(os.path.join(BASE_DIR, "assets/happyClick.png"))
Sad_image = pygame.image.load(os.path.join(BASE_DIR, "assets/sad.png"))
Click_image = pygame.image.load(os.path.join(BASE_DIR, "assets/clicking.png"))
Settings_image = pygame.image.load(os.path.join(BASE_DIR, "assets/settings.png"))
SettingsClick_image = pygame.image.load(os.path.join(BASE_DIR, "assets/settingsClick.png"))
Menu_image = pygame.image.load(os.path.join(BASE_DIR, "assets/menu.png"))
B1_image = pygame.image.load(os.path.join(BASE_DIR, "assets/b1.png"))
B1click_image = pygame.image.load(os.path.join(BASE_DIR, "assets/b1click.png"))
B2_image = pygame.image.load(os.path.join(BASE_DIR, "assets/b2.png"))
B3_image = pygame.image.load(os.path.join(BASE_DIR, "assets/b3.png"))
I1_image = pygame.image.load(os.path.join(BASE_DIR, "assets/i1.png"))
I2_image = pygame.image.load(os.path.join(BASE_DIR, "assets/i2.png"))
E_image = pygame.image.load(os.path.join(BASE_DIR, "assets/expert.png"))
Won_image = pygame.image.load(os.path.join(BASE_DIR, "assets/won.png"))
Zero_image = pygame.image.load(os.path.join(BASE_DIR, "assets/NOsquare.png"))
One_image = pygame.image.load(os.path.join(BASE_DIR, "assets/one.png"))
Two_image = pygame.image.load(os.path.join(BASE_DIR, "assets/two.png"))
Three_image = pygame.image.load(os.path.join(BASE_DIR, "assets/three.png"))
Four_image = pygame.image.load(os.path.join(BASE_DIR, "assets/four.png"))
Five_image = pygame.image.load(os.path.join(BASE_DIR, "assets/five.png"))
Six_image = pygame.image.load(os.path.join(BASE_DIR, "assets/six.png"))
Seven_image = pygame.image.load(os.path.join(BASE_DIR, "assets/seven.png"))
Eight_image = pygame.image.load(os.path.join(BASE_DIR, "assets/eight.png"))

#==============================================================================

#==============================================================================

class Grid:
    def __init__(self, difficulty = 1):
        self.squareSize = 30
        self.difficulty = difficulty       
        self.W = DIFFICULTIES[self.difficulty][0]
        self.H = DIFFICULTIES[self.difficulty][1]
        self.numSquares = int((DIFFICULTIES[self.difficulty][0]/self.squareSize)
                              * (DIFFICULTIES[self.difficulty][1]/self.squareSize))
        self.numRemoved = 0
        self.numMines = DIFFICULTIES[self.difficulty][2]
        self.numFlags = self.numMines
        self.columns = int(self.W/self.squareSize)
        self.rows = int(self.H/self.squareSize)
        self.squares = [[(x,y), False, False, False, 0] #coordinates, removed, flagged, mine, closeMines
                        for x in range(ORIGIN[0],self.W+ORIGIN[0],self.squareSize) 
                        for y in range(ORIGIN[1],self.H+ORIGIN[1],self.squareSize)]        
        self.matrix = [self.squares[x:self.rows+x] 
                       for x in range(0,len(self.squares), self.rows)] 
        
        coordinates = [(x,y) for i in range(self.numSquares)
                       for x in range(self.squares[i][0][0], self.squares[i][0][0]+self.squareSize)
                       for y in range(self.squares[i][0][1], self.squares[i][0][1]+self.squareSize)]
        temp = [coordinates[i:900+i] for i in range(0,900*self.numSquares,900)]
        self.coordMatrix = [temp[i:self.rows+i] for i in range(0,len(self.squares), self.rows)]
        self.firstSquare = None

    def generateMines(self):
        count = 0
        
        excluded_positions = set()
        if self.firstSquare is not None:
            adjacent_squares = getAdjacent(self.firstSquare)
            excluded_positions = set(adjacent_squares)
        while count < self.numMines:
            x = random.randrange(0,self.columns)
            y = random.randrange(0,self.rows)  
            
            if not self.matrix[x][y][MINE] and (x,y) not in excluded_positions:  #IF MINE NOT WHERE FIRST CLICKED          
                count += 1
                self.matrix[x][y][MINE] = True
        
    def setFirstSquare(self, square):
        self.firstSquare = square
#==============================================================================

class Screen:
    def __init__(self, grid):
        self.grid = grid
        self.width_height = (self.grid.W+ORIGIN[0]*2,self.grid.H+ORIGIN[1]+20)                      
        self.win = pygame.display.set_mode((self.grid.W+ORIGIN[0]*2,self.grid.H+ORIGIN[1]+20))
        self.resetButton = Button(self.win, (int(self.grid.W/2),10), 40, 40, Happy_image, HappyClick_image)
        self.settingsButton = Button(self.win, (self.grid.W - 16,10), 40, 40, Settings_image, SettingsClick_image) 
        pygame.draw.rect(self.win, GREY, (0,0,self.grid.W+ORIGIN[0]*2,self.grid.H+ORIGIN[1]+20))
        pygame.draw.rect(self.win, LIGHT_GREY, (ORIGIN[0],ORIGIN[1],self.grid.W,self.grid.H))
        pygame.draw.rect(self.win, DARK_GREY, (ORIGIN[0]-4,ORIGIN[1]-4,4,self.grid.H+4))
        pygame.draw.rect(self.win, OFF_WHITE, (ORIGIN[0],ORIGIN[1]+ self.grid.H,self.grid.W,4))
        pygame.draw.rect(self.win, DARK_GREY, (ORIGIN[0],ORIGIN[1]-4,self.grid.W,4))
        pygame.draw.rect(self.win, OFF_WHITE, (self.grid.W+ORIGIN[0],ORIGIN[1],4,self.grid.H+4))
        self.win.blit(Corner_image, (ORIGIN[0]+self.grid.W,ORIGIN[1]-4))
        self.win.blit(Corner_image, (ORIGIN[0]-4,ORIGIN[1]+self.grid.H))                
        self.win.blit(CornerFlip_image, (0,self.grid.H+ORIGIN[1]+16))
        self.win.blit(CornerFlip_image, (self.grid.W+ORIGIN[0]*2 - 4,0))
        pygame.draw.rect(self.win, OFF_WHITE, (0,0,4, self.grid.H+ORIGIN[1]+16))
        pygame.draw.rect(self.win, OFF_WHITE, (0,0,self.grid.W+ORIGIN[0]+16, 4))
        pygame.draw.rect(self.win, DARK_GREY, (4,self.grid.H+ORIGIN[1]+16,self.grid.W+ORIGIN[0]*2,4))
        pygame.draw.rect(self.win, DARK_GREY, (self.grid.W+ORIGIN[0]*2 - 4,4,4, self.grid.H+ORIGIN[1]+16))
        
    def draw(self, playing = False, exploded = None, click = None, win = False):        
        self.win.blit(self.resetButton.image, self.resetButton.xy)
        self.win.blit(self.settingsButton.image, self.settingsButton.xy)
        
        for column in self.grid.matrix:
            for square in column:
                if playing:
                    if square[1]:
                        if not square[3]:
                            if square[4] == 0:
                                self.win.blit(Zero_image, square[COORDINATES])
                            elif square[4] == 1:
                                self.win.blit(One_image, square[COORDINATES])
                            elif square[4] == 2:
                                self.win.blit(Two_image, square[COORDINATES])
                            elif square[4] == 3:
                                self.win.blit(Three_image, square[COORDINATES])
                            elif square[4] == 4:
                                self.win.blit(Four_image, square[COORDINATES])
                            elif square[4] == 5:
                                self.win.blit(Five_image, square[COORDINATES])
                            elif square[4] == 6:
                                self.win.blit(Six_image, square[COORDINATES])
                            elif square[4] == 7:
                                self.win.blit(Seven_image, square[COORDINATES])
                            elif square[4] == 8:
                                self.win.blit(Eight_image, square[COORDINATES])
                        
                            
                    else:
                        if not square[FLAGGED]:
                            self.win.blit(Square_image, square[COORDINATES])
                        else:
                            self.win.blit(Flag_image, square[COORDINATES])
                if not playing:
                    if square == exploded:
                        self.win.blit(Explode_image, square[COORDINATES])
                        self.win.blit(Sad_image, self.resetButton.xy)
                    elif not square[REMOVED] and square[3] and not square[FLAGGED]:                        
                        self.win.blit(Mine_image, square[0])
                    elif not square[REMOVED] and not square[3] and square[FLAGGED]:
                        self.win.blit(Wrong_image, square[0])
                        
                    if win:                        
                        self.resetButton = Button(self.win, (int(self.grid.W/2),10), 40, 40, Won_image, HappyClick_image)
                        if square[REMOVED]:
                            if not square[3]:
                                if square[4] == 0:
                                    self.win.blit(Zero_image, square[COORDINATES])
                                elif square[4] == 1:
                                    self.win.blit(One_image, square[COORDINATES])
                                elif square[4] == 2:
                                    self.win.blit(Two_image, square[COORDINATES])
                                elif square[4] == 3:
                                    self.win.blit(Three_image, square[COORDINATES])
                                elif square[4] == 4:
                                    self.win.blit(Four_image, square[COORDINATES])
                                elif square[4] == 5:
                                    self.win.blit(Five_image, square[COORDINATES])
                                elif square[4] == 6:
                                    self.win.blit(Six_image, square[COORDINATES])
                                elif square[4] == 7:
                                    self.win.blit(Seven_image, square[COORDINATES])
                                elif square[4] == 8:
                                    self.win.blit(Eight_image, square[COORDINATES])
                        elif square[3]:
                            self.win.blit(Flag_image, square[COORDINATES])
                    
    def drawSurrounding(self, surrounding):
        for i in surrounding:
            if not self.grid.matrix[i[0]][i[1]][REMOVED]:
                if not self.grid.matrix[i[0]][i[1]][FLAGGED]:
                    self.win.blit(Square_image, self.grid.matrix[i[0]][i[1]][COORDINATES])
            
                
                
                    
#==============================================================================

class Button:
    def __init__(self, screen, xy, width, height, image, clickImage = None, code = 0):
        self.xy = xy
        self.width = width
        self.height = height
        self.image = image
        self.clickImage = clickImage        
        self.screen = screen 
        self.range = []
        self.code = code
        
        for x in range(self.xy[0], self.xy[0]+self.width):
            for y in range(self.xy[1], self.xy[1]+self.height):
                z = (x,y)
                self.range.append(z)
        
    def draw(self):
        self.screen.win.blit(self.image, self.xy)
                            
#============================================================================== 
               
        
class SettingsMenu:
    def __init__(self, screen, grid):
        self.screen = screen
        self.grid = grid
        self.button1 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+8),
                              163, 25, B1_image, None, 1)
        self.button2 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+ 37),
                              163, 25, B2_image, None, 2)
        self.button3 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+66),
                              163, 25, B3_image, None, 3)
        self.button4 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+95),
                              163, 25, I1_image, None, 4)
        self.button5 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+124),
                              163, 25, I2_image, None, 5)
        self.button6 = Button(self.screen, (self.grid.W - 152, ORIGIN[1]+153),
                              163, 25, E_image, None, 6)
        self.buttons = [self.button1, self.button2, self.button3, 
                        self.button4, self.button5, self.button6]
        
        
    def draw(self):
        self.screen.win.blit(Menu_image, (self.grid.W-160, ORIGIN[1]))
        for button in self.buttons:
            button.draw()
                                               
#==============================================================================



#============================================================================== 
def getCloseMines(square):
    adjacent = getAdjacent(square)
    
    for x in range(1, len(adjacent)):
        if GameGrid.matrix[adjacent[x][0]][adjacent[x][1]][3]:
            GameGrid.matrix[square[0]][square[1]][4] += 1 
    
#==============================================================================
 
def getCloseFlags(square):
    adjacent = getAdjacent(square)
    close = 0
    for x in range(1, len(adjacent)):
        if GameGrid.matrix[adjacent[x][0]][adjacent[x][1]][2]:
            close += 1 
    return close
    
#==============================================================================

def selectButton(button, mouse_pos):
    
    if mouse_pos in button.range:
        return True
    
#==============================================================================

def selectSquare(mouse_pos):
                              
    for i in range(GameGrid.columns):
        for j in range(GameGrid.rows):            
           
            start = 0
            end = 899
            mid = 0  
                    
            while start <= end:
                
                mid = (start + end) // 2
                
                if mouse_pos == GameGrid.coordMatrix[i][j][mid]:
                    
                    
                    return (i,j)
                
                if mouse_pos < GameGrid.coordMatrix[i][j][mid]:
                    end = mid - 1
                    
                else:
                    start = mid + 1
                                                
    return None
#==============================================================================

def getSurrounding(square):
    column = square[0]
    row = square[1]
    surrounding = None
    if column != None and row != None:
        surrounding = []
        for i in range(4):
            for j in range(4):
                if i == 0 and j == 0:
                    surrounding.append((column,row))
                if (column+i, row-j) != (column -i , row+j):
                    if column +i < GameGrid.columns and row - j >=0:
                        surrounding.append((column+i, row-j))
                    if column - i >= 0 and row + j < GameGrid.rows:
                        surrounding.append((column-i, row+j))
                    
                    if (column+i, row+j) != (column-i, row+j) and (column+i, row+j) != (column+i, row-j):
                        if column +i < GameGrid.columns and row +j < GameGrid.rows:
                            surrounding.append((column+i, row+j))
                        
                    if (column-i, row-j) != (column-i, row+j) and (column-i, row-j) != (column+i, row-j):
                        if column - i >= 0 and row - j >= 0:
                            surrounding.append((column-i, row-j))
                                                                                
    return surrounding
    

#==============================================================================

def getAdjacent(square):
    column = square[0]
    row = square[1]
    adjacent = None
    if column != None and row != None:
        adjacent = []
        for i in range(2):
            for j in range(2):
                if i == 0 and j == 0:
                    adjacent.append((column,row))
                if (column+i, row-j) != (column -i , row+j):
                    if column +i < GameGrid.columns and row - j >=0:
                        adjacent.append((column+i, row-j))
                    if column - i >= 0 and row + j < GameGrid.rows:
                        adjacent.append((column-i, row+j))
                    
                    if (column+i, row+j) != (column-i, row+j) and (column+i, row+j) != (column+i, row-j):
                        if column +i < GameGrid.columns and row +j < GameGrid.rows:
                            adjacent.append((column+i, row+j))
                        
                    if (column-i, row-j) != (column-i, row+j) and (column-i, row-j) != (column+i, row-j):
                        if column - i >= 0 and row - j >= 0:
                            adjacent.append((column-i, row-j))
                                                                              
    return adjacent
    

#==============================================================================

def removeSquare(square):
    
    if GameGrid.matrix[square[0]][square[1]][1] or GameGrid.matrix[square[0]][square[1]][2]:       
        return False
    if GameGrid.matrix[square[0]][square[1]][2] == False:        
        GameGrid.matrix[square[0]][square[1]][REMOVED] = True
        GameGrid.numRemoved += 1
        clearField(square)               
        return True
                   
#==============================================================================
def clearField(square):
     adjacent = getAdjacent(square)
     for x in adjacent:
         if not GameGrid.matrix[x[0]][x[1]][2] and GameGrid.matrix[square[0]][square[1]][4] == 0:
             removeSquare(x)
             
     return True
#==============================================================================

def placeFlag(square):
     if not GameGrid.matrix[square[0]][square[1]][1]:
         #if not removed
         if not GameGrid.matrix[square[0]][square[1]][2]:
             #if not already flagged
             if GameGrid.numFlags > 0:
                 GameGrid.numFlags -= 1
                 GameGrid.matrix[square[0]][square[1]][2] = True
                 return True
         else:
            GameGrid.numFlags += 1
            GameGrid.matrix[square[0]][square[1]][2] = False
            return False
                                  
#==============================================================================

def holdButton(BUTTON):
    clicked = True
    GameScreen.win.blit(BUTTON.clickImage, BUTTON.xy)
    pygame.display.flip()
    while clicked:
        mouse_pos = pygame.mouse.get_pos()
       
        for event in pygame.event.get():
            if event.type == pygame.MOUSEBUTTONUP and selectButton(BUTTON, mouse_pos):
                return True                               
                
                
            elif event.type == pygame.MOUSEBUTTONUP and not selectButton(BUTTON, mouse_pos):
                return False
                
            elif not selectButton(BUTTON, mouse_pos):
                GameScreen.draw(playing)
                pygame.display.flip()
                
            elif selectButton(BUTTON, mouse_pos):
                GameScreen.win.blit(BUTTON.clickImage, BUTTON.xy)
                pygame.display.flip()
    

#==============================================================================

def holdSquare(square):
    clicked = True
    adjacent = getAdjacent(square)
    if not GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][REMOVED] and not GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][FLAGGED]:
        GameScreen.win.blit(Click_image, (GameScreen.resetButton.xy))             
        GameScreen.win.blit(Zero_image, GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][COORDINATES])
        pygame.display.flip()
            
    while clicked:
                
        for event in pygame.event.get():
            mouse_pos = pygame.mouse.get_pos()
            square = selectSquare(mouse_pos)
            if square != None:
                adjacent = getAdjacent(square)
            if event.type == pygame.MOUSEBUTTONUP:
                clicked = False
                
            if adjacent != None:
                if not GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][REMOVED] and not GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][FLAGGED]:
                    GameScreen.win.blit(Click_image, (GameScreen.resetButton.xy))
                    GameScreen.win.blit(Zero_image, GameGrid.matrix[adjacent[0][0]][adjacent[0][1]][COORDINATES])
                    pygame.display.flip()
                    GameScreen.drawSurrounding(adjacent) 
            if square == None:
                adjacent = None
                pygame.display.flip()                                                        
    return adjacent
                
#==============================================================================

def holdAdjacent(square):
    clicked = True
    surrounding = getSurrounding(square)
    adjacent = getAdjacent(square)
    for i in range(len(adjacent)):
        if not GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][REMOVED] and not GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][FLAGGED]:
            GameScreen.win.blit(Click_image, (GameScreen.resetButton.xy))             
            GameScreen.win.blit(Zero_image, GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][COORDINATES])  
            pygame.display.flip()
            
    while clicked:
                        
        for event in pygame.event.get():
            mouse_pos = mouse_pos = pygame.mouse.get_pos()
            square = selectSquare(mouse_pos)
            if square != None:
                surrounding = getSurrounding(square)        
                adjacent = getAdjacent(square)
            if event.type == pygame.MOUSEBUTTONUP:
                clicked = False
                
            if adjacent != None:
                for i in range(len(adjacent)):
                    if not GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][REMOVED] and not GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][FLAGGED]:
                        GameScreen.win.blit(Click_image, (GameScreen.resetButton.xy))             
                        GameScreen.win.blit(Zero_image, GameGrid.matrix[adjacent[i][0]][adjacent[i][1]][COORDINATES])
            if surrounding != None:            
                pygame.display.flip()                                    
                GameScreen.drawSurrounding(surrounding)
            if square == None:
                adjacent = None
                surrounding = None
            
    return adjacent

#==============================================================================

def openSettings():
    inSettings = True
    while inSettings:
        GameScreen.win.blit(GameScreen.settingsButton.image, GameScreen.settingsButton.xy)
        mouse_pos = pygame.mouse.get_pos()
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit()            
                
            if event.type == pygame.MOUSEBUTTONDOWN:
                if event.button == 1:
                    for button in GameSettings.buttons:
                        if selectButton(button, mouse_pos):
                            return button.code
                            inSettings = False
                        else:
                            inSettings = False                            
                            
                    if selectButton(GameScreen.settingsButton, mouse_pos):
                        if holdButton(GameScreen.settingsButton):
                            return GameGrid.difficulty                                                                                                                                  
                                
        GameSettings.draw()
        pygame.display.flip()

#==============================================================================


GameGrid = Grid(3)
GameScreen = Screen(GameGrid)
GameSettings = SettingsMenu(GameScreen, GameGrid)


for i in range(GameGrid.columns):
    for j in range(GameGrid.rows):
        getCloseMines((i,j))
        
run = True
generated = False
playing = True
exploded = None
win = False




while run:
                                    
    keys = pygame.key.get_pressed()
    mouse_pos = pygame.mouse.get_pos()
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            run = False
            
        if keys[pygame.K_F2]:
            setting = GameGrid.difficulty
            del GameGrid
            del GameScreen
            del GameSettings
            #MAKE GRID HERE
            GameGrid = Grid(setting)
            GameScreen = Screen(GameGrid)
            GameSettings = SettingsMenu(GameScreen, GameGrid)
            #for i in range(GameGrid.columns):
                #for j in range(GameGrid.rows):
                    #getCloseMines((i,j))
            generated = False
            playing = True
            
        elif event.type == pygame.MOUSEBUTTONDOWN:
            square = selectSquare(mouse_pos)
            if event.button == 1:
                
                if selectButton(GameScreen.resetButton, mouse_pos):                 
                    if holdButton(GameScreen.resetButton):
                        setting = GameGrid.difficulty
                        del GameGrid
                        del GameScreen
                        del GameSettings
                        #MAKE GRID HERE
                        GameGrid = Grid(setting)
                        GameScreen = Screen(GameGrid)
                        GameSettings = SettingsMenu(GameScreen, GameGrid)
                        #for i in range(GameGrid.columns):
                            #for j in range(GameGrid.rows):
                                #getCloseMines((i,j))
                        generated = False
                        playing = True
                    
                elif selectButton(GameScreen.settingsButton, mouse_pos):
                    if holdButton(GameScreen.settingsButton):
                        setting = openSettings()
                        if setting != GameGrid.difficulty and setting != None:
                            del GameGrid
                            del GameScreen
                            del GameSettings
                            #MAKE GRID HERE
                            GameGrid = Grid(setting)
                            GameScreen = Screen(GameGrid)
                            GameSettings = SettingsMenu(GameScreen, GameGrid)
                            #for i in range(GameGrid.columns):
                                #for j in range(GameGrid.rows):
                                    #getCloseMines((i,j))
                            playing = True
                            generated = False
                 

                elif square != None and playing:
                    if not generated:
                        GameGrid.setFirstSquare(square)
                        GameGrid.generateMines()
                        for i in range(GameGrid.columns):
                            for j in range(GameGrid.rows):
                                getCloseMines((i,j))
                        generated = True
                    adjacent = holdSquare(square)
                    if adjacent != None:
                        removed = removeSquare(adjacent[0])
                        if removed:
                            if GameGrid.matrix[square[0]][square[1]][MINE]:                                                            
                                exploded = GameGrid.matrix[square[0]][square[1]]
                                playing = False
                                win = False
                                generated = False
                                
                            else:
                                                                                                
                                if GameGrid.numRemoved == GameGrid.numSquares - GameGrid.numMines:                                            
                                    playing = False
                                    win = True
                            
                        
            elif event.button == 2 and playing:
                if square != None and playing:
                    adjacent = holdAdjacent(square)
                    if GameGrid.matrix[square[0]][square[1]][REMOVED]:
                        close = getCloseFlags(square)                        
                        if close == GameGrid.matrix[square[0]][square[1]][CLOSE_MINES] and close > 0:
                            for x in range(len(adjacent)):                            
                                removed = removeSquare(adjacent[x])
                                if removed:
                                    if GameGrid.matrix[adjacent[x][0]][adjacent[x][1]][MINE]:                                                                                
                                        exploded = GameGrid.matrix[adjacent[x][0]][adjacent[x][1]]
                                        playing = False
                                        win = False
                                        
                                    else:
                                                                               
                                        if GameGrid.numRemoved == GameGrid.numSquares - GameGrid.numMines:                                            
                                            playing = False
                                            win = True
                                        
                                    
                    
            elif event.button == 3 and playing:
                if square != None:
                    placeFlag(square)
                
                        
                
                    
            
        
    GameScreen.draw(playing, exploded, event.type, win)
    pygame.display.flip()
    
pygame.quit()
