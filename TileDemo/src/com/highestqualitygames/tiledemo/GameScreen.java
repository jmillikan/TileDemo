package com.highestqualitygames.tiledemo;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
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

import com.highestqualitygames.tiledemo.Assets.*;

public class GameScreen implements Screen {
	public enum PlayerType {
		LocalHuman, LocalCPU
	}

	Random random = new Random();

	// PROPERTIES - UI
	Stage stage;
	Actor roleChooseLayer;
	
	// Note: Board displays tiles "Y up", so we reverse that in tileAt and 
	// elsewhere with lastRow - <row in board> or 1 - <row in board>
	Board<Tile,Worker> tileBoard; // tileBoard uses placingWorkers
	
	Board<Tile,Tile> tileQueue, tileChoice;
	Board<Role,Role> roleChoose, roleChoice;
	Label announcement;
	
	public static class Player {
		public Worker worker;
		public String name;
		public PlayerType type;
		public String playerDesignation;
		
		public Player(Worker w, String n, PlayerType p, String pd){
			worker = w; name = n; type = p; playerDesignation = pd;
		}
	}
	
	public static class PlayerState {
		public int score;
		public Tile currentTile;
		public Role currentRole;
	}
		
	// PROPERTIES - GAME LOGIC - NON-MUTATING (after constructor)
	int numPlayers;
	List<Player> players;

	// PROPERTIES - GAME LOGIC - MUTATING
	List<PlayerState> playerStates;
	
	List<Tile> queue = new ArrayList<Tile>();
	Tile[][] tiles;
	int lastRow = 0;
	boolean[][][] workers;
	// Used in tileBoard selection
	boolean placingWorkers = false;

	Role[][] roles;
	/* Index into roles (NOT Board coordinates) */
	class Pair {
		public int row, col;
		public Pair(int r, int c){ row = r; col = c; }
	}

	// Current queue is implicitly the first 5 tiles (4 players + 1 slack)

	public GameScreen(List<Player> playerList) {
		Assets.load();
		
		numPlayers = playerList.size();
		players = playerList;
		playerStates = new ArrayList<PlayerState>();
		for(Player p : players){
			PlayerState ps = new PlayerState();
			ps.score = 0;
			ps.currentTile = Tile.Empty;
			ps.currentRole = Role.Empty;
			playerStates.add(ps);
		}
		
		tiles = new Tile[100][numPlayers + 1];
		workers = new boolean[numPlayers + 1][100][numPlayers + 1];
		
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		
		resetRoleChoices();
		
		fillLastRow(Tile.Manor);
		fillTileQueue();
		
		setAllRoles();
		
		roleChooseLayer = makeChooseRoleLayer();
		
		Stack st = new Stack();
		st.add(makeTileBoard());
		st.add(makeTileQueueLayer());
		st.add(roleChooseLayer);
		st.add(makeAnnouncementLayer());
		st.add(new Players(players, playerStates));
		
		roleChooseLayer.setVisible(false);
		
		st.setFillParent(true);

		stage.addActor(st);

		new TileChoicePhase().beginRound();
	}

	// Used in place of delay to speed/slow actions for smoke testing
	Action ui_delay(float f){
		return delay(f / 5);
	}
	
	public void dispose() {
		stage.dispose();
	}

	public void render(float delta) {		
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				
		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void pause() {
	}

	public void resume() {
	}
	
	public void show() {
		// TODO Auto-generated method stub
		
	}

	public void hide() {
		// TODO Auto-generated method stub
		
	}

	/*
	 * GAME ROUNDS
	 */
	
	abstract class GamePhase<Choice> {
		String roundName, beginHumanChoiceText, longHumanChoiceText;
		
		public GamePhase(String name, String beginHuman, String longHuman){
			roundName = name;
			beginHumanChoiceText = beginHuman;
			longHumanChoiceText = longHuman;
		}
		
		abstract void initHumanChoice(int player);
		abstract boolean humanMadeChoice(int player);
		abstract void initHumanLongChoice(int player);
		abstract Choice completeHumanChoice(int player);
		
		abstract void initCPUChoice(int player);
		abstract Choice completeCPUChoice(int player);
		abstract void applyPlayerChoice(int player, Choice c);
		abstract void initRound();
		
		abstract void roundOver();

		void beginRound(){
			announcement.setText(roundName);
			
			initRound();
			
			stage.addAction(sequence(ui_delay(5.0f), new Action(){
				public boolean act(float delta){
					announcement.setText("");
					
					beginPlayerChoice(0);

					return true;				
				}
			}));
		}
		
		void beginPlayerChoice(int player){
			if(players.get(player).type == PlayerType.LocalCPU){
				beginCPUChoice(player);
			}
			else {
				beginHumanChoice(player);
			}
		}
		
		// Computer player X selects a tile.
		// Highlight tile and wait 3 seconds, then move on.
		void beginCPUChoice(final int player){
			initCPUChoice(player);
			
			stage.addAction(sequence(ui_delay(3.0f), new Action(){
				public boolean act(float f){
					Choice c = completeCPUChoice(player);

					completePlayerChoice(player, c);
					
					return true;
				}
			}));		
		}

		void completePlayerChoice(int player, Choice c){
			// Test: Move first tile in queue to first spot in lastRow
			
			applyPlayerChoice(player, c);
			
			if(player + 1 >= numPlayers){
				roundOver();
			}
			else {
				beginPlayerChoice(player + 1);
			}
		}
		
		void beginHumanChoice(final int player){
			initHumanChoice(player);

			announcement.setText(beginHumanChoiceText);
			
			stage.addAction(sequence(ui_delay(3.0f), new Action(){
				public boolean act(float f){
					if(humanMadeChoice(player)){
						announcement.setText("");
						
						Choice c = completeHumanChoice(player);
						
						completePlayerChoice(player, c);
					}
					else {
						announcement.setText(longHumanChoiceText);
						
						initHumanLongChoice(player);

						stage.addAction(sequence(ui_delay(12.0f), new Action(){
							public boolean act(float f){
								if(!humanMadeChoice(player)){
									throw new Error("No tile selected from queue. Event ordering problems likely.");
								}
		
								Choice c = completeHumanChoice(player);

								completePlayerChoice(player, c);
								
								return true;
							}
						}));		
					}
					
					return true;
				}
			}));		
		}
	}
	
	class TileChoicePhase extends GamePhase<Integer> {
		int choice;
		
		public TileChoicePhase(){
			super("Tile Choice Round", "Pick a tile. >3s for speed bonus!", "Pick a tile. 12s remaining");
		}
		
		void initCPUChoice(int player){
			choice = randomAvailableTile(player);
			tileQueue.highlightSet(0, choice, 1, 1);
		}
		
		Integer completeCPUChoice(int player){
			tileQueue.clearHighlights();
			return choice;
		}
		
		void applyPlayerChoice(int player, Integer queueIndex){
			playerStates.get(player).currentTile = queue.get(queueIndex);
			queue.set(queueIndex, Tile.Empty);
		}
		
		void roundOver(){
			shiftTileQueue();
			new TilePlacePhase().beginRound();
		}
		
		void initHumanChoice(int player){
			tileQueue.beginSelection();
		}
		
		void initHumanLongChoice(int player){
			tileQueue.setSelection(0, defaultTileIndex());
		}
		
		Integer completeHumanChoice(int player){
			int col = tileQueue.selectedCol();
			announcement.setText("");
			tileQueue.clearSelecting();
			
			return col;
		}
		
		boolean humanMadeChoice(int player){
			return tileQueue.hasSelection();
		}
		
		void initRound(){}
	}
	
	class TilePlacePhase extends GamePhase<Integer> {
		int choice;
		
		public TilePlacePhase(){
			super("Tile placement round", "Pick a column. <3s for speed bonus!", "Pick a column. 12s remaining.");
		}
		
		void initRound(){
			addRow();
		}
		
		void initCPUChoice(int player){
			choice = cpuChooseRandomColumn(player, true);
			tileBoard.highlightSet(0, choice, 1, 1);
		}
		
		Integer completeCPUChoice(int player){
			tileBoard.clearHighlights();
			return choice;
		}
		
		void initHumanChoice(int player){
			tileBoard.beginSelection();
		}
		
		void initHumanLongChoice(int player){
			tileBoard.setSelection(0, defaultColumn(true));
		}
		
		Integer completeHumanChoice(int player){
			int col = tileBoard.selectedCol(); 					
			tileBoard.clearSelecting();
			return col;
		}
		
		boolean humanMadeChoice(int player){
			return tileBoard.hasSelection();
		}
		
		void roundOver(){
			new RoleChoicePhase().beginRound();
		}
		
		void applyPlayerChoice(int player, Integer column){
			Tile t = playerStates.get(player).currentTile;
			playerStates.get(player).currentTile = Tile.Empty;
			
			tiles[lastRow][column] = t;
		}
	}
	
	class RoleChoicePhase extends GamePhase<Pair> {
		Pair choice;
		
		public RoleChoicePhase(){
			super("Role choice round", "Pick a role. >3s for speed bonus!", "Pick a role. You have 12s.");
		}
		
		void initRound(){
			roleChooseLayer.setVisible(true);
			setAllRoles();
		}
		
		void initCPUChoice(int player){
			choice = cpuChooseRandomRole(player);
			roleChoose.highlightSet(choice.row, choice.col, 1, 1);
		}
		
		Pair completeCPUChoice(int player){
			roleChoose.clearHighlights();
			return choice;
		}
		
		void initHumanChoice(int player){
			roleChoose.beginSelection();
		}
		
		void initHumanLongChoice(int player){
			Pair r = defaultRole(player);
			roleChoose.setSelection(r.row, r.col);
		}
		
		Pair completeHumanChoice(int player){
			Pair p  = new Pair(roleChoose.selectedRow(), roleChoose.selectedCol()); 					
			roleChoose.clearSelecting();
			return p;
		}
		
		boolean humanMadeChoice(int player){
			return roleChoose.hasSelection();
		}
		
		void roundOver(){
			resetRoleChoices();
			roleChooseLayer.setVisible(false);
			new PlaceWorkerPhase().beginRound();
		}
		
		void applyPlayerChoice(int player, Pair role){
			playerStates.get(player).currentRole = roles[role.row][role.col];
			roles[0][role.col] = Role.Empty;
			roles[1][role.col] = Role.Empty; 
		}
	}
	
	class PlaceWorkerPhase extends GamePhase<Integer> {
		public PlaceWorkerPhase(){
			super("Place workers round", "Short blach blach", "Long blah blah.");
		}
		
		void initRound(){
			placingWorkers = true;
		}
		
		void roundOver(){
			placingWorkers = false;
			new TileChoicePhase().beginRound();
		}
		
		int choice;

		void applyPlayerChoice(int player, Integer column){
			workers[player][lastRow][column] = true;
		}

		// CPU Choice
		void initCPUChoice(int player){
			choice = cpuChooseRandomColumn(player, false);
			tileBoard.highlightSet(0, choice, 1, 1);
		}
		
		Integer completeCPUChoice(int player){
			tileBoard.clearHighlights();
			return choice;
		}
		
		// Human choice
		void initHumanChoice(int player){
			tileBoard.beginSelection();
		}
		
		void initHumanLongChoice(int player){
			int col = defaultColumn(false);
			tileBoard.setSelection(0, col);
		}
		
		Integer completeHumanChoice(int player){
			int col = tileBoard.selectedCol();
			tileBoard.clearSelecting();
			return col;
		}
		
		boolean humanMadeChoice(int player){
			return tileBoard.hasSelection();
		}
	}

	/*
	 *  CPU CHOICE AND PLAYER DEFAULTS
	 */

	// Return all indices in the first N that either DO (emptyAvailable) or DONT (!emptyAvailable) match the empty value.
	<T extends Board.TileSet> List<Integer> availableIndices(List<T> ts, int n, boolean emptyAvailable) {
		List<Integer> indices = new ArrayList<Integer>();
		for(int i = 0; i < n; i++){
			if(emptyAvailable && ts.get(i).IsEmpty()){
				indices.add(i);
			}
			else if(!emptyAvailable && !ts.get(i).IsEmpty()){
				indices.add(i);
			}
		}
		return indices;
	}
	
	<T> T random(List<T> ts){
		if(ts.size() == 0)
			throw new Error("Empty list in random");
		
		return ts.get(random.nextInt(ts.size()));
	}
	
	<T> T first(List<T> ts){
		if(ts.size() == 0)
			throw new Error("Empty list in first");
		
		return ts.get(0);
	}

	// Give an index into the queue of tile choice for CPU player 
	// Currently random.
	int randomAvailableTile(int player){
		return random(availableIndices(queue, numPlayers + 1, false));
	}
	
	int defaultTileIndex(){
		return first(availableIndices(queue, numPlayers + 1, false));
	}
	
	int cpuChooseRandomColumn(int player, boolean selectEmpty){
		return random(availableIndices(Arrays.asList(tiles[lastRow]), numPlayers + 1, selectEmpty));
	}
	
	int defaultColumn(boolean emptyAvailable){
		return first(this.availableIndices(Arrays.asList(tiles[lastRow]), numPlayers + 1, emptyAvailable));
	}
	
	Pair cpuChooseRandomRole(int player){
		return new Pair(random.nextBoolean() ? 0 : 1, // Randomly choose top or bottom of role
				random(availableIndices(Arrays.asList(roles[0]), 5, false))); // Randomly choose available role
	}
	
	Pair defaultRole(int player){
		return new Pair(1, availableIndices(Arrays.asList(roles[0]), 5, false).get(0));
	}

	/*
	 *  HANDLE TILE BOARD, QUEUE, ROLES, WORKERS 
	 */
	
	void setAllRoles(){
		roles = new Role[][] { 
				{ Role.FieldTop, Role.PastureTop, Role.VillageTop, Role.ManorTop, Role.ManorTop, Role.ForestBottom },
				{ Role.FieldBottom, Role.PastureBottom, Role.VillageBottom, Role.ManorBottom, Role.ForestBottom }
		};
	}

	void addRow(){
		lastRow++;
		tileBoard.resizeBoard(numPlayers + 1, lastRow + 1);
		fillLastRow(Tile.Empty);
	}
	
	// Fill last row of tiles with just t (usually Tile.Empty)
	void fillLastRow(Tile t){
		for(int i = 0; i < tiles[lastRow].length; i++){
			tiles[lastRow][i] = t;
		}
	}
	
	// Add 50 random tiles to the queue
	void fillTileQueue(){
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
	void shiftTileQueue(){
		if(queue.size() < 2 * numPlayers + 1){
			throw new Error("Ran out of tiles!");
		}

		for(int checked = 0, i = 0; checked < numPlayers + 1; checked++){
			if(queue.get(i) == Tile.Empty){
				queue.remove(i);
			}
			else {
				i++;
			}
		}
		
		tileQueue.resizeBoard(queue.size(), 1);
	}
	
	void resetRoleChoices(){
		for(int i = 0; i < numPlayers; i++){
			playerStates.get(i).currentRole = Role.Empty;
		}
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

	Actor makeTileBoard(){
		tileBoard = new Board<Tile,Worker>(numPlayers + 1,1,200f,50f,"tileBoard"){
			Tile tileAt(int row, int col){
				return tiles[lastRow - row][col];
			}
			
			boolean tileSelectable(int row, int col){
				return row == 0 &&
						(placingWorkers != /* read XOR */ (tiles[lastRow - row][col] == Tile.Empty));
			}
			
			// This... is not really a good thing to do every frame.
			// for every tile.
			List<Worker> piecesAt(int row, int col){
				ArrayList<Worker> tileWorkers = new ArrayList<Worker>();
				
				for(int i = 0; i < numPlayers; i++){
					if(workers[i][lastRow - row][col])
					{
						tileWorkers.add(players.get(i).worker);
					}
				}
				
				return tileWorkers;
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
	
	Actor makeAnnouncementLayer(){
		announcement = new Label("Announcement", new Label.LabelStyle(new BitmapFont(), Color.BLACK));
		announcement.setAlignment(Align.center);
		
		return C(announcement).padTop(100f).top();
	}
	
	Actor makeTileQueueLayer(){
		tileQueue = new Board<Tile,Tile>(50,1,100f,0f,"tileQueue"){
			Tile tileAt(int row, int col){
				return queue.get(col);
			}
			
			boolean tileSelectable(int row, int col){
				return col < numPlayers + 1 && queue.get(col) != Tile.Empty;
			}
		};
		
		ScrollPane pane = SP(C(tileQueue).padLeft(this.stage.getWidth() - 180f).fill(0, 0));
		pane.setupOverscroll(0f, 0f, 0f);
		
		return C(pane).padTop(30f).top().fill(1f,0f);
	}

	Actor makeChooseRoleLayer(){
		roleChoose = new Board<Role,Role>(5,2,150f,0f,"chooseRole"){
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