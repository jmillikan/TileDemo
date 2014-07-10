package com.highestqualitygames.tiledemo;

import sun.security.ssl.Debug;

import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import com.highestqualitygames.tiledemo.TileDemoGame.Tile;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

/*
A Scene2D.ui Widget for a rectangular grid of game tiles supporting a
highlighted region and a selection mechanism. Not very general.

As much as I don't like inheritance I'm too lazy to rip it out right now.
Here's a typical use of Board:

		tileChoice = new Board(4,1){
			Tile tileAt(int row, int col){
				return playerTileChoice[col];
			}
			
			// Let's uh... let them select even columns only.
			boolean tileSelectable(int row, int col){
			    return col % 2 == 0;
			}
			
			Sprite tileSprite(Tile t){ return ts(t); }
		};
		
highlightSet sets a rectangular region of highlighted tiles.
Selectable tiles are constrained by selectionSet and then by tileSelectable.
 */

abstract class Board extends Widget {
	// Note: Just about everything here is row major, row first - int row, int col; int i (row) vs int j (col) etc
	// I try not to refer to row/col as y/x or x/y.
	
	protected int tilesWide, tilesHigh;
	// Because of brain damage in libgdx, use this as a manual scaling factor... 
	float tileSize;
	float scale;
	// 
	String name;
	
	// Is there currently a highlighted set?
	protected boolean highlight = false;
	protected int highlightRow, highlightCol, highlightRows, highlightCols;
	
	// Is there currently a selectable set of tiles?
	protected boolean selecting = false;
	protected int selectionRow, selectionCol, selectionRows, selectionCols;	
	
	// Is there currently a selected tile?
	protected boolean selection = false;
	protected int selectedRow;
	protected int selectedCol;
	
	abstract Tile tileAt(int row, int column);
	abstract Sprite tileSprite(Tile t);

	boolean tileSelectable(int row, int column){
		return true;
	}
	
	public Board(int tilesWide, int tilesHigh, float tileSize, final String name){
		//super();
		this.tilesWide = tilesWide;
		this.tilesHigh = tilesHigh;
		this.tileSize = tileSize;
		scale = tileSize / 200f;
		this.name = name;
		
		this.setSize(this.getPrefWidth(), this.getPrefHeight());
		
		this.addListener(new ClickListener(){
			public void clicked(InputEvent event, float x, float y){
				click(x,y);
			}
		});
	}
	
	void click(float x, float y){
		Debug.println("Board", String.format("On %s at (%f,%f), %f x %f @ (%f,%f)", 
				name, getX(), getY(), getWidth(), getHeight(), x, y));

		if(selecting){
			int col = (int) java.lang.Math.floor(x / tileSize);
			int row = (int) java.lang.Math.floor(y / tileSize);
			
			if(tileSelectable(row, col)){
				selectedCol = col;
				selectedRow = row;
				selection = true;
			}
		}
	}
	
	// Sets possible selection area and clears any selection
	public void selectionSet(int row, int col, int rows, int cols){
		selectionRow = row;
		selectionCol = col;
		selectionRows = rows;
		selectionCols = cols;
		
		selecting = true;
		selection = false;
	}
	
	// This makes selectionSet seem like a very bad method name.
	public void setSelection(int row, int col){
		selection = true;
		selectedRow = row; selectedCol = col;
	}
	
	// Clears both any selectable set as well as current selection
	public void clearSelecting(){
		selecting = false;
		selection = false;
	}
	
	public void clearSelection(){
		selection = false;
	}
	
	public boolean hasSelection(){
		return selection;
	}
	
	// Currnetly selected row or -1
	public int selectedRow(){
		return selection ? selectedRow : -1;
	}
	
	public int selectedCol(){
		return selection ? selectedCol : -1;
	}
	
	public void highlightSet(int row, int col, int rows, int cols){
		highlight = true;
		highlightRow = row; highlightCol = col;
		highlightRows = rows; highlightCols = cols;
	}
	
	public void clearHighlights(){
		highlight = false;
		selection = false;
	}
	
	public void resizeBoard(int tilesWide, int tilesHigh){
		this.tilesWide = tilesWide;
		this.tilesHigh = tilesHigh;

		this.invalidateHierarchy();
	}
	
	public float getPrefHeight(){
		return tileSize * tilesHigh * getScaleY();
	}
	
	public float getPrefWidth(){
		return tileSize * tilesWide * getScaleX();
	}
	
	boolean highlightCell(int i, int j){
		return highlight && i >= highlightRow && i < highlightRow + highlightRows
				&& j >= highlightCol && j < highlightCol + highlightCols;
	}
	
	boolean selectableCell(int i, int j){
		return selecting && i >= selectionRow && i < selectionRow + selectionRows
				&& j >= selectionCol && j < selectionCol + selectionCols &&
				tileSelectable(i,j);
	}
	
	boolean selectedCell(int i, int j){
		return selection && i == selectedRow && j == selectedCol;
	}
	
	void drawTile(Sprite s, Batch batch, float alpha, float x, float y){
		s.setScale(scale);
		s.setOrigin(getOriginX(), getOriginY());
		s.setBounds(x, y, 200f, 200f);
		s.draw(batch, alpha);
	}
	
	public void draw(Batch batch, float a) {
		validate();
		
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, a);
		
		for(int i = 0; i < tilesHigh; i++){
			for(int j = 0; j < tilesWide; j++){
				float x = getX() + tileSize * j * getScaleX();
				float y = getY() + tileSize * i * getScaleY();
				
				if(tileAt(i,j) != Tile.Empty){
					drawTile(tileSprite(tileAt(i, j)), batch, 1f, x, y);
				}

				if(highlightCell(i,j) || selectedCell(i,j)){
					drawTile(TileDemoGame.highlight, batch, 0.3f, x, y);
				}
				else if(selectableCell(i,j)){
					drawTile(TileDemoGame.selectable, batch, 0.3f, x, y);
				}
			}
		}
	}
}

