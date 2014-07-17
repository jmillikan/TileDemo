package com.highestqualitygames.tiledemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class Assets {
	public static TextureRegion manor, forest, field, pasture, village;
	public static TextureRegion r1top, r1bottom, r2top, r2bottom, r3top, r3bottom, r4top, r4bottom, r5top, r5bottom;
	public static TextureRegion white, black, purple, teal, orange;
	public static Sprite highlight, selectable;
	public static TextureRegion bg; 
	
	// Assets.java  isn't a great place for these, but it's okay.
	// NOTE: These are (arguable) doing double duty as logical and UI values.
	// This is not perfect but is working.
	
	public enum Worker implements Board.TileSet {
		Purple, Teal, Orange, White, Black, Empty;
		
		public boolean IsEmpty(){ return this == Empty; };
		
		public TextureRegion tr(){
			switch(this){
			case Purple: return purple;
			case Teal: return teal;
			case Orange: return orange;
			case Black: return black;
			case White: return white;
			default: throw new Error("Cannot render empty worker");
			}
		}
	}
	
	public enum Tile implements Board.TileSet {
		Empty, Manor, Forest, Field, Pasture, Village;
		
		public boolean IsEmpty(){ return this == Empty; };
		
		public TextureRegion tr(){
			switch(this){
			case Manor: return manor;
			case Forest: return forest;
			case Field: return field;
			case Pasture: return pasture;
			case Village: return village;
			default: throw new Error("Bad tile request");
			}			
		}
	}
	
	/* Splitting these to top/bottom is not good game logic, but the game logic is eventually departing this class... */
	public enum Role implements Board.TileSet {
		Empty, FieldTop, FieldBottom, PastureTop, PastureBottom, VillageTop, VillageBottom, ManorTop, ManorBottom, ForestTop, ForestBottom;

		public boolean IsEmpty(){ return this == Empty; };

		public TextureRegion tr(){
			switch(this){
			case FieldTop: return r1top;
			case FieldBottom: return r1bottom;
			case PastureTop: return r2top;
			case PastureBottom: return r2bottom;
			case VillageTop: return r3top;
			case VillageBottom: return r3bottom;
			case ManorTop: return r4top;
			case ManorBottom: return r4bottom;
			case ForestTop: return r5top;
			case ForestBottom: return r5bottom;
			default: throw new Error("Bad tile request");
			}
		}
	}

	static void load(){
		TextureAtlas atlas;
		atlas = new TextureAtlas(Gdx.files.internal("data/images.atlas"));
		
		bg = atlas.findRegion("bg");
		
		highlight = atlas.createSprite("highlight");
		selectable = atlas.createSprite("selectable");
		
		manor = atlas.findRegion("manor");
		forest = atlas.findRegion("forest");
		field = atlas.findRegion("field");
		pasture = atlas.findRegion("pasture");
		village = atlas.findRegion("village");
		
		r1top = atlas.findRegion("roles-1-top");
		r1bottom = atlas.findRegion("roles-1-bottom");
		r2top = atlas.findRegion("roles-2-top");
		r2bottom = atlas.findRegion("roles-2-bottom");
		r3top = atlas.findRegion("roles-3-top");
		r3bottom = atlas.findRegion("roles-3-bottom");
		r4top = atlas.findRegion("roles-4-top");
		r4bottom = atlas.findRegion("roles-4-bottom");
		r5top = atlas.findRegion("roles-5-top");
		r5bottom = atlas.findRegion("roles-5-bottom");
		
		black = atlas.findRegion("black");
		white = atlas.findRegion("white");
		teal = atlas.findRegion("teal");
		orange = atlas.findRegion("orange");
		purple = atlas.findRegion("purple");
	}
}
