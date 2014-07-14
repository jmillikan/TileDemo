package com.highestqualitygames.tiledemo;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.badlogic.gdx.scenes.scene2d.Action;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.utils.*;

import com.highestqualitygames.tiledemo.Assets.Tile;
import com.highestqualitygames.tiledemo.Assets.Role;

public class TileDemoGame implements ApplicationListener {
	// Used in place of delay to speed/slow actions for smoke testing
	Action ui_delay(float f){
		return delay(f / 5);
	}
	
	public enum PlayerType {
		LocalHuman, LocalCPU
	}

	Stage stage;
	Actor roleChooseLayer;
	
	Random random = new Random();

	@Override
	public void create() {
		Assets.load();
		
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		
		playerType = new PlayerType[] { PlayerType.LocalCPU, PlayerType.LocalHuman, PlayerType.LocalCPU, PlayerType.LocalCPU };
		fillTileQueue();
		
		setAllRoles();
		roleChooseLayer = makeChooseRoleLayer();
		
		Stack st = new Stack();
		st.add(makeTileBoard());
		st.add(makeTileChoiceLayer());
		st.add(makeAnnouncementLayer());
		st.add(makeTileQueueLayer());
		st.add(roleChooseLayer);
		
		roleChooseLayer.setVisible(false);
		
		st.setFillParent(true);

		stage.addActor(st);

		(new GameRound("Tile Select Round"){
			void beginPlayerChoice(int player){
				
			}
		}).beginRound();
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
	
	
	// Note: Board displays tiles "Y up", so we reverse that in tileAt.
	// But 
	Role[][] roles = new Role[2][5];
	int lastRow = 0;
	Board<Tile> tileBoard, tileQueue, tileChoice;
	Board<Role> roleChoose;
	Label announcement;
	
	List<Tile> queue = new ArrayList<Tile>();
	
	/* Strictly an index into roles - NEVER refers to the Board coordinates */
	class Pair {
		public int row, col;
		public Pair(int r, int c){ row = r; col = c; }
	}
		
	int players = 3;

	Tile[][] tiles = new Tile[100][players + 1];

	Role[] playerRoleChoice = new Role[players];
	Tile[] playerTileChoice = new Tile[players];
	PlayerType[] playerType = new PlayerType[players];
	// Current queue is implicitly the first 5 tiles (4 players + 1 slack)

	/*
	 *  TILE SELECTION ROUND 
	 *  
	 * beginTileSelectRound calls beginPlayerTileChoice
	 * beginPlayerTileChoice will call either the CPU tile choice
	 * or human tile choice functions
	 * These will trigger events calling playerTilePlaced (eventually)
	 * which will call beginPlayerTileChoice again or else begin the next round... 
	 */
	
	public abstract class GameRound {
		String roundName;
		
		public GameRound(String name){
			roundName = name;
		}
		
		abstract void beginPlayerChoice(int player);
		
		void beginRound(){
			announcement.setText(roundName);
			
			stage.addAction(sequence(ui_delay(5.0f), new Action(){
				public boolean act(float delta){
					announcement.setText("");
					
					beginPlayerTileChoice(0);

					return true;				
				}
			}));
		}
	}
	
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

	void beginPlayerTileChoice(int player){
		if(playerType[player] == PlayerType.LocalCPU){
			cpuSelectTile(player, cpuChooseTile(player));
		}
		else {
			humanSelectTile(player);
		}
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
	
	void humanSelectTile(final int player){
		tileQueue.selectionSet(0, 0, 1, players + 1);
		
		announcement.setText("Pick a tile. >3s for speed bonus!");
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				if(tileQueue.hasSelection()){
					announcement.setText("");
					
					int col = tileQueue.selectedCol();
					announcement.setText("");
					tileQueue.clearSelecting();

					playerSelectedTile(player, col);
				}
				else {
					announcement.setText("Pick a tile. 12s remaining");
					
					tileQueue.setSelection(0, defaultTileIndex());

					stage.addAction(sequence(ui_delay(12.0f), new Action(){
						public boolean act(float f){
							if(!tileQueue.hasSelection()){
								throw new Error("No tile selected from queue. Event ordering problems likely.");
							}
							
							int col = tileQueue.selectedCol();
							announcement.setText("");
							tileQueue.clearSelecting();
							
							playerSelectedTile(player, col);
							
							return true;
						}
					}));		
				}
				
				return true;
			}
		}));		
	}
	
	/*
	 *  TILE PLACE ROUND
	 *  
	 *   Structure here is basically identical to tile choice round
	 */
	
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
		tileBoard.highlightSet(0, column, 1, 1);
		
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
		
		tileBoard.selectionSet(0, 0, 1, players + 1);
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				if(tileBoard.hasSelection()){
					announcement.setText("");
					
					playerTilePlaced(player, tileBoard.selectedCol());
					
					tileBoard.clearSelecting();
				}
				else {
					announcement.setText("Pick a column. 12s remaining");
					
					tileBoard.setSelection(0, defaultColumn());

					stage.addAction(sequence(ui_delay(12.0f), new Action(){
						public boolean act(float f){
							announcement.setText("");
							
							if(!tileBoard.hasSelection()){
								throw new Error("No selection somehow, probably bad event order");
							}
							
							playerTilePlaced(player, tileBoard.selectedCol());
							
							tileBoard.clearSelecting();
							
							return true;
						}
					}));		
				}
				
				return true;
			}
		}));		
	}
	
	public void playerTilePlaced(int player, int column){
		Tile t = TileDemoGame.this.playerTileChoice[player];
		playerTileChoice[player] = Tile.Empty;
		
		tiles[lastRow][column] = t;
		
		if(player + 1 >= players){
			TileDemoGame.this.beginRoleChoiceRound();
		}
		else {
			beginPlayerTilePlace(player + 1);
		}
	}
	
	/*
	 *  ROLE CHOICE ROUND
	 *  
	 *   Structure here is basically identical to tile choice round
	 */
	
	void beginRoleChoiceRound(){
		announcement.setText("Role Choice Round");
		
		roleChooseLayer.setVisible(true);
		
		stage.addAction(sequence(ui_delay(10.0f), new Action(){
			public boolean act(float delta){
				announcement.setText("");

				beginPlayerChooseRole(0);

				return true;				
			}
		}));
	}
	
	void beginPlayerChooseRole(int player){
		if(playerType[player] == PlayerType.LocalCPU){
			cpuChooseRole(player, cpuChooseRandomRole(player));
		}
		else {
			humanChooseRole(player);
		}
	}
	
	void cpuChooseRole(final int player, final Pair roleChosen){
		roleChoose.highlightSet(1 - roleChosen.row, roleChosen.col, 1, 1);
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float delta){
				roleChoose.clearHighlights();
				
				playerRoleChosen(player, roleChosen);
				
				return true;
			}
		}));		
	}
	
	void humanChooseRole(final int player){
		announcement.setText("Choose a role. < 3s for bonus.");
		
		roleChoose.selectionSet(0, 0, 2, 5);
		
		stage.addAction(sequence(ui_delay(3.0f), new Action(){
			public boolean act(float f){
				if(roleChoose.hasSelection()){
					announcement.setText("");
					
					Pair p = new Pair(roleChoose.selectedRow(), roleChoose.selectedCol());
					
					playerRoleChosen(player, p);
					
					roleChoose.clearSelecting();
				}
				else {
					announcement.setText("Choose a role. 12s remaining");
					
					Pair defaultRoleChoice = defaultRoleChoice(player);
					roleChoose.setSelection(1 - defaultRoleChoice.row, defaultRoleChoice.col);

					stage.addAction(sequence(ui_delay(12.0f), new Action(){
						public boolean act(float f){
							announcement.setText("");
							
							if(!roleChoose.hasSelection()){
								throw new Error("No selection somehow, probably bad event order");
							}

							Pair p = new Pair(1 - roleChoose.selectedRow(), roleChoose.selectedCol());
							
							playerRoleChosen(player, p);
							
							roleChoose.clearSelecting();
							
							return true;
						}
					}));		
				}
				
				return true;
			}
		}));		
	}

	public void playerRoleChosen(int player, Pair p){
		Role t = TileDemoGame.this.roles[p.row][p.col];
		roles[0][p.col] = Role.Empty;
		roles[1][p.col] = Role.Empty;
		
		this.playerRoleChoice[player] = t;
		
		if(player + 1 >= players){
			setAllRoles();
			
			this.roleChooseLayer.setVisible(false);

			this.beginTileSelectRound();
		}
		else {
			this.beginPlayerChooseRole(player + 1);
		}
	}
	
	/*
	 *  CPU CHOICE AND PLAYER DEFAULTS
	 */

	// Return all indices in the first N that either DO (emptyAvailable) or DONT (!emptyAvailable) match the empty value.
	<T> List<Integer> availableIndices(List<T> ts, int n, T empty, boolean emptyAvailable) {
		List<Integer> indices = new ArrayList<Integer>();
		for(int i = 0; i < n; i++){
			if(emptyAvailable && ts.get(i) == empty){
				indices.add(i);
			}
			else if(!emptyAvailable && ts.get(i) != empty){
				indices.add(i);
			}
		}
		return indices;
	}

	// Give an index into the queue of tile choice for CPU player 
	// Currently random.
	int cpuChooseTile(int player){
		List<Integer> availableTiles = this.availableIndices(queue, players + 1, Tile.Empty, false);

		return availableTiles.get(random.nextInt(availableTiles.size()));
	}
	
	public int defaultTileIndex(){
		List<Integer> availableTiles = this.availableIndices(queue, players + 1, Tile.Empty, false);

		if(availableTiles.size() > 0)
			return availableTiles.get(0);
		
		throw new Error("No default tile");
	}
	
	public int cpuChooseRandomColumn(int player){
		List<Integer> availableColumns = this.availableIndices(Arrays.asList(tiles[lastRow]), players + 1, Tile.Empty, true);
		
		return availableColumns.get(random.nextInt(availableColumns.size()));
	}
	
	Pair cpuChooseRandomRole(int player){
		List<Integer> availableRoles = this.availableIndices(Arrays.asList(roles[0]), 5, Role.Empty, false);
		
		return new Pair(random.nextBoolean() ? 0 : 1, availableRoles.get(random.nextInt(availableRoles.size())));
	}
	
	Pair defaultRoleChoice(int player){
		List<Integer> availableRoles = this.availableIndices(Arrays.asList(roles[0]), 5, Role.Empty, false);
		
		return new Pair(1, availableRoles.get(0));
	}

	public int defaultColumn(){
		List<Integer> availableColumns = this.availableIndices(Arrays.asList(tiles[lastRow]), players + 1, Tile.Empty, true);

		if(availableColumns.size() > 0)
			return availableColumns.get(0);
		
		throw new Error("No default column available");
	}
	
	/*
	 *  HANDLE TILE BOARD, QUEUE, ROLES 
	 */
	
	void setAllRoles(){
		roles = new Role[][] { 
				{ Role.FieldTop, Role.PastureTop, Role.VillageTop, Role.ManorTop, Role.ManorTop, Role.ForestBottom },
				{ Role.FieldBottom, Role.PastureBottom, Role.VillageBottom, Role.ManorBottom, Role.ForestBottom }
		};
	}

	// Fill last row of tiles with just t (usually Tile.Empty)
	public void fillLastRow(Tile t){
		for(int i = 0; i < tiles[lastRow].length; i++){
			tiles[lastRow][i] = t;
		}
	}
	
	// Add 50 random tiles to the queue
	public void fillTileQueue(){
		java.util.List<Tile> tileValues = Arrays.asList(Tile.values());
		Random r = new Random();
			  
		for(int i = 0; i < 50; i++){
			// + 1 dodges the Empty tile
			// Not sure if this is supposed to work
			queue.add(tileValues.get(r.nextInt(tileValues.size() - 1) + 1));
		}
	}
	
	// Move blanks off the front of the queue
	// Error if tile queue ends up short.
	public void shiftTileQueue(){
		if(queue.size() < 2 * players + 1){
			throw new Error("Ran out of tiles!");
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
	
	/*
	 *  MAKE GAME DISPLAY LAYERS
	 */

	<A extends Actor> Container<A> C(A a){
		Container<A> c = new Container<A>(a);
		c.setTouchable(Touchable.childrenOnly);
		return c;
	}
	
	ScrollPane SP(Actor a){
		ScrollPane sp = new ScrollPane(a);
		sp.setTouchable(Touchable.childrenOnly);
		return sp;
	}

	public Actor makeTileBoard(){
		fillLastRow(Tile.Manor);
		
		tileBoard = new Board<Tile>(players + 1,1,200f,"tileBoard"){
			Tile tileAt(int row, int col){
				return tiles[lastRow - row][col];
			}
			
			boolean tileSelectable(int row, int col){
				return tiles[lastRow - row][col] == Tile.Empty;
			}
		};
		
		ScrollPane p = SP(tileBoard);
		ScrollPaneStyle s = new ScrollPaneStyle(new TiledDrawable(Assets.bg), null, null, null, null);
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
		
		return C(announcement).padTop(100f).top();
	}
	
	public Actor makeTileQueueLayer(){
		tileQueue = new Board<Tile>(50,1,100f,"tileQueue"){
			Tile tileAt(int row, int col){
				return queue.get(col);
			}
			
			boolean tileSelectable(int row, int col){
				return queue.get(col) != Tile.Empty;
			}
		};
		
		ScrollPane pane = SP(C(tileQueue).padLeft(this.stage.getWidth() - 180f).fill(0, 0));
		pane.setupOverscroll(0f, 0f, 0f);
		
		return C(pane).padTop(30f).top().fill(1f,0f);
	}

	public Actor makeTileChoiceLayer(){
		for(int i = 0; i < players; i++){
			playerTileChoice[i] = Tile.Empty;
		}
		
		tileChoice = new Board<Tile>(players,1,50f,"tileChoice"){
			Tile tileAt(int row, int col){
				return playerTileChoice[col];
			}	
		};
		
		return C(SP(tileChoice)).padBottom(10f).bottom().fill(1f,0f);
	}

	public Actor makeChooseRoleLayer(){
		roleChoose = new Board<Role>(5,2,150f,"chooseRole"){
			Role tileAt(int row, int col){
				return roles[1 - row][col];	
			}
			
			boolean tileSelectable(int row, int col){
				return roles[1 - row][col] != Role.Empty;
			}
		};
		
		return C(SP(roleChoose)).bottom();
	}
}
