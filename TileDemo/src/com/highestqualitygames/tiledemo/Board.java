package com.highestqualitygames.tiledemo;

import sun.security.ssl.Debug;

import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

/*
A Scene2D.ui Widget for a rectangular grid of game tiles supporting a
highlighted region and a selection mechanism. Not very general.

As much as I don't like inheritance I'm too lazy to rip it out right now.
Here's a typical use of Board:

		tileChoice = new Board<Tile>(...,"tileChoice",tileTS){
			Tile tileAt(int row, int col){
				return playerTileChoice[col];
			}
			
			// Let's uh... let them select even columns only.
			boolean tileSelectable(int row, int col){
			    return col % 2 == 0;
			}
		};
		
highlightSet sets a rectangular region of highlighted tiles.
Selectable tiles are constrained by selectionSet and then by tileSelectable.
 */

abstract class Board<TileT extends Board.TileSet> extends Widget {
	// Note: Just about everything here is row major, row first - int row, int col; int i (row) vs int j (col) etc
	// I try not to refer to row/col as y/x or x/y.
	
	public interface TileSet {
		TextureRegion tr();
		boolean IsEmpty();
	}
	
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
	
	abstract TileT tileAt(int row, int column);

	boolean tileSelectable(int row, int column){
		return true;
	}
	
	public Board(int tilesWide, int tilesHigh, float tileSize, final String n){
		//super();
		this.tilesWide = tilesWide;
		this.tilesHigh = tilesHigh;
		this.tileSize = tileSize;
		scale = tileSize / 200f;
		name = n;
		
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
			
			if(selectableCell(row, col)){
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
	
	void drawTile(TextureRegion tr, Batch batch, float x, float y){
		batch.draw(tr, x, y, getOriginX(), getOriginY(), 200f, 200f, scale, scale, 0f);
	}
	
	public void draw(Batch batch, float a) {
		validate();
		
		Color overlay = getColor(), opaque = getColor().cpy();
		opaque.a = a;
		overlay.a = 0.3f * a;
		
		for(int i = 0; i < tilesHigh; i++){
			for(int j = 0; j < tilesWide; j++){
				float x = getX() + tileSize * j * getScaleX();
				float y = getY() + tileSize * i * getScaleY();
				
				if(!tileAt(i,j).IsEmpty()){
					batch.setColor(opaque);
					
					drawTile(tileAt(i, j).tr(), batch, x, y);
				}

				if(highlightCell(i,j) || selectedCell(i,j)){
					batch.setColor(overlay);
					
					drawTile(Assets.highlight, batch, x, y);
				}
				else if(selectableCell(i,j)){
					batch.setColor(overlay);
					
					drawTile(Assets.selectable, batch, x, y);
				}
			}
		}
	}
}

