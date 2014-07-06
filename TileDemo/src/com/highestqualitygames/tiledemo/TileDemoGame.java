package com.highestqualitygames.tiledemo;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import sun.security.ssl.Debug;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import com.badlogic.gdx.scenes.scene2d.Action;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.utils.*;


public class TileDemoGame implements ApplicationListener {
	Action ui_delay(float f){
		return delay(f / 5);
	}
	
	public enum Tile {
		Empty, Manor, Forest, Field, Pasture, Village
	}
	
	public enum PlayerType {
		LocalHuman, LocalCPU
	}
	
	Sprite ts(Tile t){
		switch(t){
		case Empty: return null;
		case Manor: return manor;
		case Forest: return forest;
		case Field: return field;
		case Pasture: return pasture;
		case Village: return village;
		default: throw new Error("Null tile request");
		}
	}
	
	public static Sprite highlight, selectable;
	
	/* Basically handles drawing an m*n grid of Tiles using tileSprite */

	TextureRegion bg; 
	private Stage stage;
	private Sprite manor, forest, field, pasture, village;
	
	Random random = new Random();
	

	@Override
	public void create() {
		TextureAtlas atlas;
		atlas = new TextureAtlas(Gdx.files.internal("data/images.atlas"));
		bg = atlas.findRegion("bg");
		manor = atlas.createSprite("manor");
		forest = atlas.createSprite("forest");
		field = atlas.createSprite("field");
		pasture = atlas.createSprite("pasture");
		village = atlas.createSprite("village");
		
		highlight = atlas.createSprite("highlight");
		selectable = atlas.createSprite("selectable");
		
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		
		playerType = new PlayerType[] { PlayerType.LocalCPU, PlayerType.LocalHuman, PlayerType.LocalCPU, PlayerType.LocalCPU };
		fillTileQueue();
		
		Stack st = new Stack();
		st.add(makeTileBoard());
		st.add(makeTileQueueLayer());
		st.add(makeTileChoiceLayer());
		st.add(makeAnnouncementLayer());
		st.setFillParent(true);
		
		stage.addActor(st);

		beginTileSelectRound();
	}
	
	// Note: Board displays tiles "Y up", so we reverse that in tileAt.
	// But 
	private Tile[][] tiles = new Tile[100][5];
	int lastRow = 0;
	Board tileBoard, tileQueue, tileChoice;
	Label announcement;
	
	private List<Tile> queue = new ArrayList<Tile>();
	private Tile[] playerTileChoice = new Tile[4];
	PlayerType[] playerType = new PlayerType[4];
	int players = 4;
	// Current queue is implicitly the first 5 tiles (4 players + 1 slack)
	
	void beginTileSelectRound(){
		announcement.setText("Tile Selection Round");
		
		stage.addAction(sequence(ui_delay(5.0f), new Action(){
			public boolean act(float delta){
				announcement.setText("");
				
				beginPlayerTileChoice(0);

				return true;				
			}
		}));
	}

	// Computer player X selects a tile.
	// Highlight tile and wait 3 seconds, then move on.
	public void cpuSelectTile(final int player, final int queueIndex){
		tileQueue.highlightSet(0, queueIndex, 1, 1);
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				tileQueue.clearHighlights();
				
				playerSelectedTile(player, queueIndex);
				
				return true;
			}
		}));		
	}
	
	void beginPlayerTileChoice(int player){
		Debug.println("tile", String.format("Player index: %d", player));
		
		if(playerType[player] == PlayerType.LocalCPU){
			cpuSelectTile(player, cpuChooseTile(player));
		}
		else {
			humanSelectTile(player);
		}
	}
	
	void playerSelectedTile(int player, int queueIndex){
		// Test: Move first tile in queue to first spot in lastRow
		Tile selectedTile = queue.get(queueIndex);
		queue.set(queueIndex, Tile.Empty);
		playerTileChoice[player] = selectedTile;
						
		if(player + 1 >= players){
			shiftTileQueue();
			beginTilePlaceRound();
		}
		else {
			beginPlayerTileChoice(player + 1);
		}
	}
	
	// TODO: Rpelace announcement.setText with something unclickable
	// that scrolls and fades out messages
	void humanSelectTile(final int player){
		tileQueue.selectionSet(0, 0, 1, players + 1);
		
		announcement.setText("Pick a tile. >3s for speed bonus!");
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				if(tileQueue.selectedCol() >= 0){
					announcement.setText("");
					
					tileQueue.clearSelecting();
					playerSelectedTile(player, tileQueue.selectedCol());
				}
				else {
					announcement.setText("Pick a tile. 12s remaining");

					stage.addAction(sequence(ui_delay(12.0f), new Action(){
						public boolean act(float f){
							announcement.setText("");

							tileQueue.clearSelecting();
							if(tileQueue.selectedCol() >= 0){
								playerSelectedTile(player, tileQueue.selectedCol());
							}
							else {
								playerSelectedTile(player, defaultTileIndex());
							}
							
							return true;
						}
					}));		
				}
				
				return true;
			}
		}));		
	}
	
	// Give an index into the queue of tile choice for player player
	// Currently random.
	public int cpuChooseTile(int player){
		List<Integer> availableIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < players + 1; i++){
			if(queue.get(i) != Tile.Empty){
				availableIndices.add(i);
			}
		}
		
		return availableIndices.get(random.nextInt(availableIndices.size()));
	}
	
	public int defaultTileIndex(){
		for(int i = 0; i < players + 1; i++){
			if(queue.get(i) != Tile.Empty){
				return i;
			}
		}
		
		throw new Error("No default tile");
	}
	
	public void beginTilePlaceRound(){
		announcement.setText("Tile placement round");
		
		lastRow++;
		tileBoard.resizeBoard(players + 1, lastRow + 1);
		this.fillLastRow(Tile.Empty);
		
		stage.addAction(sequence(ui_delay(5.0f), new Action(){
			public boolean act(float delta){
				announcement.setText("");
				
				beginPlayerTilePlace(0);
				
				return true;
			}
		}));
	}
	
	public void beginPlayerTilePlace(int player){
		if(playerType[player] == PlayerType.LocalCPU){
			cpuPlaceTile(player, cpuChooseRandomColumn(player));
		}
		else {
			humanPlaceTile(player);
		}
	}
	
	public void cpuPlaceTile(final int player, final int column){
		tileBoard.highlightSet(0, column, 1, 1); // Currently does nothing since empty tile is a no-show
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float delta){
				tileBoard.clearHighlights();
				
				playerTilePlaced(player, column);
				
				return true;
			}
		}));
	}
	
	public void humanPlaceTile(final int player){
		announcement.setText("Pick a column. >3s for speed bonus!");
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				if(tileBoard.selectedCol() >= 0){
					announcement.setText("");
					
					playerTilePlaced(player, tileBoard.selectedCol());
				}
				else {
					announcement.setText("Pick a column. 12s remaining");

					stage.addAction(sequence(ui_delay(12.0f), new Action(){
						public boolean act(float f){
							announcement.setText("");
							
							playerTilePlaced(player,
									tileBoard.selectedCol() >= 0 ? 
											tileBoard.selectedCol() : 
											defaultColumn());
							
							return true;
						}
					}));		
				}
				
				return true;
			}
		}));		
	}
	
	public int defaultColumn(){
		for(int i = 0; i < players + 1; i++){
			if(tiles[lastRow][i] == Tile.Empty){
				return i;
			}
		}
		
		throw new Error("No default column available");
	}
	
	public void playerTilePlaced(int player, int column){
		Tile t = TileDemoGame.this.playerTileChoice[player];
		playerTileChoice[player] = Tile.Empty;
		
		tiles[lastRow][column] = t;
		
		if(player + 1 >= players){
			TileDemoGame.this.beginTileSelectRound();
		}
		else {
			beginPlayerTilePlace(player + 1);
		}
	}
	
	// Move blanks off the front of the queue
	// Error if tile queue ends up short.
	public void shiftTileQueue(){
		if(queue.size() < 2 * players + 1){
			throw new Error("Ran out of players!");
		}

		for(int checked = 0, i = 0; checked < players + 1; checked++){
			if(queue.get(i) == Tile.Empty){
				queue.remove(i);
			}
			else {
				i++;
			}
		}
		
		tileQueue.resizeBoard(queue.size(), 1);
	}
	
	public int cpuChooseRandomColumn(int player){
		List<Integer> availableColumns = new ArrayList<Integer>();
		
		for(int i = 0; i < players + 1; i++){
			if(tiles[lastRow][i] == Tile.Empty){
				availableColumns.add(i);
			}
		}
		
		return availableColumns.get(random.nextInt(availableColumns.size()));
	}

	public void fillLastRow(Tile t){
		for(int i = 0; i < tiles[lastRow].length; i++){
			tiles[lastRow][i] = t;
		}
	}
	
	// TODO: Generate a set of tiles and randomize it instead...
	public void fillTileQueue(){
		java.util.List<Tile> tileValues = Arrays.asList(Tile.values());
		Random r = new Random();
			  
		for(int i = 0; i < 50; i++){
			// + 1 dodges the Empty tile
			// Not sure if this is supposed to work
			queue.add(tileValues.get(r.nextInt(tileValues.size() - 1) + 1));
		}
	}
	
	public Actor makeTileBoard(){
		fillLastRow(Tile.Manor);
		
		tileBoard = new Board(5,1){
			Tile tileAt(int row, int col){
				return tiles[lastRow - row][col];
			}

			Sprite tileSprite(Tile t){ return ts(t); }
		};
		
		tileBoard.setPosition(0f, 0f);
		tileBoard.setScale(1f);
		
		ScrollPane p = new ScrollPane(tileBoard);
		ScrollPaneStyle s = new ScrollPaneStyle(new TiledDrawable(bg), null, null, null, null);
		p.setStyle(s);
		
		p.setScrollingDisabled(false, false);
		p.setOverscroll(true, true);
		p.setFlickScroll(true);
		p.setFillParent(true);
		
		return p;
	}
	
	public Actor makeAnnouncementLayer(){
		announcement = new Label("Announcement", new Label.LabelStyle(new BitmapFont(), Color.BLACK));
		announcement.setAlignment(Align.center);
		
		return new Container<Label>(announcement).padTop(100f).top();
	}
	
	public Actor makeTileQueueLayer(){
		tileQueue = new Board(50,1){
			Tile tileAt(int row, int col){
				return queue.get(col);
			}
			
			Sprite tileSprite(Tile t){ return ts(t); }
		};
		
		tileQueue.setScale(0.3f);
		
		ScrollPane pane = new ScrollPane(new Container<Board>(tileQueue).padLeft(200f).fill(0, 0));
		pane.setupOverscroll(0f, 0f, 0f);
		
		Container<ScrollPane> c = new Container<ScrollPane>(pane)
				.padTop(20f).top().fill(1f,0f);
		
		return c;
	}
	
	public Actor makeTileChoiceLayer(){
		for(int i = 0; i < players; i++){
			playerTileChoice[i] = Tile.Empty;
		}
		
		tileChoice = new Board(4,1){
			Tile tileAt(int row, int col){
				return playerTileChoice[col];
			}
			
			Sprite tileSprite(Tile t){ return ts(t); }
		};
		
		tileChoice.setScale(0.25f);
		
		ScrollPane pane = new ScrollPane(tileChoice);
		
		Container<ScrollPane> c = new Container<ScrollPane>(pane)
				.padBottom(10f).bottom().fill(1f,0f);
		
		return c;		
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	@Override
	public void render() {		
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				
		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
