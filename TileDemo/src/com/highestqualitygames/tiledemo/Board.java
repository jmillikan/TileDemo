package com.highestqualitygames.tiledemo;

import java.util.List;
import java.util.ArrayList;

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

abstract class Board<TileT extends Board.TileSet, PieceT extends Board.TileSet> extends Widget {
	// Note: Just about everything here is row major, row first - int row, int col; int i (row) vs int j (col) etc
	// I try not to refer to row/col as y/x or x/y.
	
	public interface TileSet {
		TextureRegion tr();
		boolean IsEmpty();
	}
	
	protected int tilesWide, tilesHigh;
	
	// Because of brain damage in libgdx, use this as a manual scaling factor... 
	float tileSize, pieceSize;
	float scale;
	// 
	String name;
	
	// Board mediates between this and the internal selections mechanisms in a non-trivial way
	// ... read it
	
	public static enum TileDecoration { None, Highlight, Select }
	
	public Selection selection;
	
	public static abstract class Selection {
		boolean tileSelectable(int row, int column){
			return true;
		}
		
		void selected(int row, int column){
			
		}
		
		TileDecoration tileDecoration(int row, int column){
			return TileDecoration.None;
		}
	}
	
	abstract TileT tileAt(int row, int column);
	List<PieceT> piecesAt(int row, int column){return new ArrayList<PieceT>();}

	public Board(int tilesWide, int tilesHigh, float tileSize, float pieceSize, final String n){
		//super();
		this.tilesWide = tilesWide;
		this.tilesHigh = tilesHigh;
		this.tileSize = tileSize;
		this.pieceSize = pieceSize;
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
		if(selection != null){
			int col = (int) java.lang.Math.floor(x / tileSize);
			int row = (int) java.lang.Math.floor(y / tileSize);
			
			if(selection.tileSelectable(row, col))
				selection.selected(row, col);
		}
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
		
	void drawTile(TextureRegion tr, Batch batch, float x, float y, float scale){
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
					
					drawTile(tileAt(i, j).tr(), batch, x, y, scale);
				}
				
				if(selection != null){
					switch(selection.tileDecoration(i, j)){
					case Highlight: 
						batch.setColor(overlay);
						drawTile(Assets.highlight, batch, x, y, scale);
						break;
					case Select:
						batch.setColor(overlay);
						drawTile(Assets.selectable, batch, x, y, scale);
						break;
					default: 
					}
				}

				float pieceX = 0, pieceY = 0;
				for(PieceT piece : this.piecesAt(i, j)){
					drawTile(piece.tr(), batch, x + pieceX, y + pieceY, pieceSize / 200f);
					pieceX += pieceSize;
				}
			}
		}
	}
}

