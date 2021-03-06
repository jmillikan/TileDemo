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

import com.highestqualitygames.tiledemo.Assets.Role.PointYield;
import com.highestqualitygames.tiledemo.Assets.*;
import com.highestqualitygames.tiledemo.Board.TileDecoration;

import static com.highestqualitygames.tiledemo.Board.TileDecoration.*;

public class GameScreen implements Screen {
	public enum PlayerType {
		LocalHuman, LocalCPU
	}

	Random random = new Random();

	// PROPERTIES - UI
	Stage stage;
	Actor roleChooseLayer;
	
	Board<Tile,Tile> tileQueue, tileChoice; // 1D
	// Note: Board displays tiles "Y up", so we reverse that in tileAt and 
	// elsewhere with lastRow - <row in board> or 1 - <row in board>
	Board<Tile,Worker> tileBoard; // 2D - Y reversed
	Board<Role,Role> roleChoose, roleChoice; // 2D - Y reversed
	Label announcement;
	
	// Non-changing player information
	public static class Player {
		public Worker worker;
		public String name;
		public PlayerType type;
		
		public Player(Worker w, String n, PlayerType p){
			worker = w; name = n; type = p;
		}
	}
	
	public static class PlayerState {
		public int score = 0;
		public Tile currentTile = Tile.Empty;
		public Role currentRole = Role.Empty;
		public int numWorkers = 0;
	}
	
	int initQueueSize = 45;
		
	// PROPERTIES - GAME LOGIC - NON-MUTATING (after constructor)
	int numPlayers;
	List<Player> players;

	
	// PROPERTIES - GAME LOGIC - MUTATING
	List<PlayerState> playerStates;

	enum GamePhase { TileChoose, TilePlace, RoleChoose, WorkerPlace }
	GamePhase phase;
	
	List<Tile> queue = new ArrayList<Tile>();
	Tile[][] tiles;
	int lastRow = 0;
	boolean[][][] workers;

	Role[][] roles;
	/* Index into arrays (NOT Board coordinates) */
	class Pair {
		public int row, col;
		public Pair(int r, int c){ row = r; col = c; }
		public boolean equals(Object o){
			if(!(o instanceof Pair)){
				return false;
			}
			
			Pair other = (Pair) o;
			return other.row == row && other.col == col;
		}
	}

	// Current queue is implicitly the first 5 tiles (4 players + 1 slack)
	public GameScreen(List<Player> playerList) {
		Assets.load();
		
		numPlayers = playerList.size();
		players = playerList;
		playerStates = new ArrayList<PlayerState>();
		for(int i = 0; i < numPlayers; i++){
			playerStates.add(new PlayerState());
		}
		
		tiles = new Tile[100][numPlayers + 1];
		workers = new boolean[numPlayers + 1][100][numPlayers + 1];
		
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		
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
		
		phase = GamePhase.TileChoose;
		tileChoicePhase().beginPhase();
	}
	
	// This could be done more directly, but hoping to move some stuff out...
	// Or to replace this with a way more explicit state based system
	public Action nextPhase = new Action(){
			public boolean act(float d){
				switch(phase){
				case TileChoose:
					phase = GamePhase.TilePlace;
					tilePlacePhase().beginPhase();
					break;
				case TilePlace:
					phase = GamePhase.RoleChoose;
					roleChoicePhase().beginPhase();
					break;
				case RoleChoose:
					phase = GamePhase.WorkerPlace;
					placeWorkerPhase().beginPhase();
					break;
				case WorkerPlace:
					if(queue.size() > numPlayers + 1){
						phase = GamePhase.TileChoose;
						tileChoicePhase().beginPhase();
					}
					else {
						announcement.setText("The Game Is Over");
					}
				}

				return true;
			}
		};
	
	
	void beginRound(){
		// TODO: Have an explicit phase ordering somewhere...
		tileChoicePhase().beginPhase();
	}

	// Used in place of delay to speed/slow actions for smoke testing
	Action ui_delay(float f){
		return delay(f);
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
	
	interface PlayerChoiceHandler<Choice> {
		void initHumanChoice(int player);
		boolean humanMadeChoice(int player);
		void initHumanLongChoice(int player);
		Choice completeHumanChoice(int player);
		
		void initCPUChoice(int player);
		Choice completeCPUChoice(int player);
		void applyPlayerChoice(int player, Choice c);
		void initRound();
		
		void roundOver();
	}
	
	class PlayerChoicePhase<Choice> {
		String roundName, beginHumanChoiceText, longHumanChoiceText;
		PlayerChoiceHandler<Choice> phase;
		Action next;
		
		public PlayerChoicePhase(String name, String beginHuman, String longHuman, Action next, PlayerChoiceHandler<Choice> phase){
			roundName = name;
			beginHumanChoiceText = beginHuman;
			longHumanChoiceText = longHuman;
			this.phase = phase;
			this.next = next;
		}

		void beginPhase(){
			announcement.setText(roundName);
			
			phase.initRound();
			
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
			phase.initCPUChoice(player);
			
			stage.addAction(sequence(ui_delay(3.0f), new Action(){
				public boolean act(float f){
					Choice c = phase.completeCPUChoice(player);

					completePlayerChoice(player, c);
					
					return true;
				}
			}));		
		}

		void completePlayerChoice(int player, Choice c){
			// Test: Move first tile in queue to first spot in lastRow
			
			phase.applyPlayerChoice(player, c);
			
			if(player + 1 >= numPlayers){
				phase.roundOver();
				next.act(0);
			}
			else {
				beginPlayerChoice(player + 1);
			}
		}
		
		void beginHumanChoice(final int player){
			phase.initHumanChoice(player);

			announcement.setText(beginHumanChoiceText);
			
			stage.addAction(sequence(ui_delay(3.0f), new Action(){
				public boolean act(float f){
					if(phase.humanMadeChoice(player)){
						announcement.setText("");
						
						Choice c = phase.completeHumanChoice(player);
						
						completePlayerChoice(player, c);
					}
					else {
						announcement.setText(longHumanChoiceText);
						
						phase.initHumanLongChoice(player);

						stage.addAction(sequence(ui_delay(12.0f), new Action(){
							public boolean act(float f){
								Choice c = phase.completeHumanChoice(player);

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
	
	class HighlightTile extends Board.Selection {
		int row, col;
		
		public HighlightTile(int r, int c){
			row = r; col = c;
		}
		
		Board.TileDecoration tileDecoration(int r, int c){
			return row == r && col == c ? Board.TileDecoration.Highlight : Board.TileDecoration.None;
		}
	}
	
	public PlayerChoicePhase<Integer> tileChoicePhase(){
		return new PlayerChoicePhase<Integer>("Tile Choice Round", "Pick a tile. >3s for speed bonus!", "Pick a tile. 12s remaining", 
				nextPhase,
				new PlayerChoiceHandler<Integer>() {
					int choice;
					
					public void initCPUChoice(int player){
						choice = randomAvailableTile(player);
		
						tileQueue.selection = new HighlightTile(0, choice);
					}
					
					public Integer completeCPUChoice(int player){
						tileQueue.selection = null;
						
						return choice;
					}
					
					public void applyPlayerChoice(int player, Integer queueIndex){
						playerStates.get(player).currentTile = queue.get(queueIndex);
						queue.set(queueIndex, Tile.Empty);
					}
					
					public void roundOver(){
						shiftTileQueue();
					}
					
					public void initHumanChoice(int player){
						choice = -1;
						
						// We will usually want clicked to act more like a "select" constrained by tileSelectable...
						// Question is "always" or not
						
						tileQueue.selection = new Board.Selection(){
							boolean tileSelectable(int row, int column){
								return column < numPlayers + 1 && queue.get(column) != Tile.Empty;
							}
							
							void selected(int row, int column){
								choice = column;
							}
							
							TileDecoration tileDecoration(int row, int column){
								return column == choice ? Highlight :
									tileSelectable(row, column) ? Select : None;
							}
						};
					}
					
					public void initHumanLongChoice(int player){
						choice = defaultTileIndex();
					}
					
					public Integer completeHumanChoice(int player){
						announcement.setText("");
						tileQueue.selection = null;
						
						return choice;
					}
					
					public boolean humanMadeChoice(int player){
						return choice != -1; 
					}
					
					public void initRound(){}
				});
	}
	
	public PlayerChoicePhase<Integer> tilePlacePhase(){
		return new PlayerChoicePhase<Integer>("Tile placement round", "Pick a column. <3s for speed bonus!", "Pick a column. 12s remaining.",
				nextPhase,
				new PlayerChoiceHandler<Integer>(){
					int choice = -1;
					
					public void initRound(){
						addRow();
					}

					public void roundOver(){}

					public void initCPUChoice(int player){
						choice = cpuChooseRandomColumn(player, true);
						tileBoard.selection = new HighlightTile(0, choice);
					}
					
					public Integer completeCPUChoice(int player){
						tileBoard.selection = null;
						return choice;
					}
					
					public void initHumanChoice(int player){
						choice = -1;
						
						tileBoard.selection = new Board.Selection(){
							boolean tileSelectable(int row, int column){
								return row == 0 && column < numPlayers + 1 && tiles[lastRow][column] == Tile.Empty;
							}
							
							void selected(int row, int column){
								choice = column;
							}
							
							TileDecoration tileDecoration(int row, int column){
								return row == 0 && column == choice ? Highlight :
									tileSelectable(row, column) ? Select : None;
							}
						};
					}
					
					public void initHumanLongChoice(int player){
						choice = defaultColumn(true);
					}
					
					public Integer completeHumanChoice(int player){
						tileBoard.selection = null;
						return choice;
					}
					
					public boolean humanMadeChoice(int player){
						return choice != -1;
					}
					
					
					public void applyPlayerChoice(int player, Integer column){
						Tile t = playerStates.get(player).currentTile;
						playerStates.get(player).currentTile = Tile.Empty;
						
						tiles[lastRow][column] = t;
					}
				});
	}
	
	PlayerChoicePhase<Pair> roleChoicePhase(){
		return new PlayerChoicePhase<Pair>("Role choice round", "Pick a role. >3s for speed bonus!", "Pick a role. You have 12s.",
				nextPhase,
				new PlayerChoiceHandler<Pair>(){
					Pair choice;
					
					public void initRound(){
						roleChooseLayer.setVisible(true);
						setAllRoles();
					}
					
					public void initCPUChoice(int player){
						choice = cpuChooseRandomRole(player);
						roleChoose.selection = new HighlightTile(1 - choice.row, choice.col);
					}
					
					public Pair completeCPUChoice(int player){
						roleChoose.selection = null;
						return choice;
					}
					
					public void initHumanChoice(int player){
						choice = null;
						
						roleChoose.selection = new Board.Selection(){
							boolean tileSelectable(int row, int column){
								return roles[1 - row][column] != Role.Empty;
							}
							
							void selected(int row, int column){
								choice = new Pair(1 - row, column);
							}
							
							TileDecoration tileDecoration(int row, int column){
								return new Pair(1 - row, column).equals(choice) ? Highlight :
									tileSelectable(row, column) ? Select : None;
							}
						};
					}
					
					public void initHumanLongChoice(int player){
						choice = defaultRole(player);
					}
					
					public Pair completeHumanChoice(int player){
						roleChoose.selection = null;
						return choice;
					}
					
					public boolean humanMadeChoice(int player){
						return choice != null;
					}
					
					public void roundOver(){
						roleChooseLayer.setVisible(false);
					}
					
					public void applyPlayerChoice(int player, Pair role){
						Role r = roles[1 - role.row][role.col];
						playerStates.get(player).numWorkers += r.workers();
						
						for(PointYield py : r.points()){
							playerStates.get(player).score += py.points * countPlayerRegions(player, py.tile); 
						}
						
						roles[0][role.col] = Role.Empty;
						roles[1][role.col] = Role.Empty; 
					}
					
					// Expand from i,j outward on only Tile t, adding to seen list.
					// If player is encountered, return true after crawl
					boolean crawlAndCheck(List<Pair> seen, int i, int j, Tile t, int player){
						boolean foundPlayerWorker = false;
						
						// These will not be large... TODO: Sets or whatever.
						List<Pair> visited = new ArrayList<Pair>();
						List<Pair> frontier = new ArrayList<Pair>();
						
						frontier.add(new Pair(i,j));
						
						while(frontier.size() > 0){
							Pair p = frontier.get(0);
							frontier.remove(p);
							if(!visited.contains(p)) visited.add(p);
							
							// Add to frontier in four directions...
							expandFrontier(frontier, visited, p, i + 1, j, t);
							expandFrontier(frontier, visited, p, i - 1, j, t);
							expandFrontier(frontier, visited, p, i, j + 1, t);
							expandFrontier(frontier, visited, p, i, j - 1, t);
							
							if(workers[player][p.row][p.col])
								foundPlayerWorker = true;
						}
						
						for(Pair p : visited){
							seen.add(p);
						}
						
						return foundPlayerWorker;
					}
					
					void expandFrontier(List<Pair> frontier, List<Pair> visited, Pair center, int i, int j, Tile t){
						Pair p = new Pair(center.row + i, center.col + j);
						
						if(p.row >= 0 && p.col >= 0 && tiles.length > p.row && tiles[p.row].length > p.col && 
								tiles[p.row][p.col] == t && !visited.contains(p)){
							frontier.add(p);
						}
					}
					
					// 
					int countPlayerRegions(int player, Tile t){
						// Starting at 0,0, when we encounter this tile, if it has not been seen, 
						// spread out to all unseen tiles, adding all to alreadySeen. If any workers
						// for this player is in this region, add ONLY one to count.
						List<Pair> alreadySeen = new ArrayList<Pair>();
						int regions = 0;
						
						for(int i = 0; i < numPlayers + 1; i++){
							for(int j = 0; j < tiles[i].length; j++){
								if(tiles[i][j] == t && 
										!alreadySeen.contains(new Pair(i,j)) && 
										crawlAndCheck(alreadySeen,i,j,t,player)){
									regions += 1;
								}
							}
						}
						
						return regions;
					}
				});
	}
	
	PlayerChoicePhase<Integer> placeWorkerPhase(){
		return new PlayerChoicePhase<Integer>("Place workers round", "Short blach blach", "Long blah blah.",
				nextPhase,
				new PlayerChoiceHandler<Integer>(){
					public void initRound(){}
					public void roundOver(){}
					
					Integer choice;
			
					public void applyPlayerChoice(int player, Integer column){
						if(column != null) {
							if(playerStates.get(player).numWorkers <= 0)
								throw new Error("Able to place worker without any!");
							
							workers[player][lastRow][column] = true;
							playerStates.get(player).numWorkers -= 1;
						}
					}
			
					// CPU Choice
					public void initCPUChoice(int player){
						choice = playerStates.get(player).numWorkers > 0 ? 
								cpuChooseRandomColumn(player, false) : null; 
						
						if(choice != null)
							tileBoard.selection = new HighlightTile(0, choice);
					}
					
					public Integer completeCPUChoice(int player){
						tileBoard.selection = null;
						return choice;
					}
					
					// Human choice
					public void initHumanChoice(final int player){
						choice = null;
						
						tileBoard.selection = new Board.Selection(){
							boolean tileSelectable(int row, int column){
								return row == 0 && tiles[lastRow - row][column] != Tile.Empty && playerStates.get(player).numWorkers > 0;
							}
							
							void selected(int row, int column){
								choice = column;
							}
							
							TileDecoration tileDecoration(int row, int column){
								return choice != null && row == 0 && choice.equals(column) ? Highlight :
									tileSelectable(row, column) ? Select : None;
							}
						};
					}
					
					public void initHumanLongChoice(int player){
						
					}
					
					public Integer completeHumanChoice(int player){
						tileBoard.selection = null;
			
						return choice;
					}
					
					public boolean humanMadeChoice(int player){
						return choice != null;
					}
				});
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
		return first(availableIndices(Arrays.asList(tiles[lastRow]), numPlayers + 1, emptyAvailable));
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
				{ Role.FieldTop, Role.PastureTop, Role.VillageTop, Role.ManorTop, Role.ForestTop },
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
			  
		for(int i = 0; i < initQueueSize; i++){
			// + 1 dodges the Empty tile
			// Not sure if this is supposed to work
			queue.add(tileValues.get(r.nextInt(tileValues.size() - 1) + 1));
		}
	}
	
	// Move blanks off the front of the queue
	// Error if tile queue ends up short.
	void shiftTileQueue(){
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
	
	/*
	 *  MAKE GAME DISPLAY LAYERS
	 */

	<A extends Actor> Container<A> container(A a){
		Container<A> c = new Container<A>(a);
		c.setTouchable(Touchable.childrenOnly);
		return c;
	}
	
	ScrollPane scrollPane(Actor a){
		ScrollPane sp = new ScrollPane(a);
		sp.setTouchable(Touchable.childrenOnly);
		return sp;
	}

	Actor makeTileBoard(){
		tileBoard = new Board<Tile,Worker>(numPlayers + 1,1,200f,50f,"tileBoard"){
			Tile tileAt(int row, int col){
				return tiles[lastRow - row][col];
			}
			
			// This... is not really a good thing to do every frame.
			// for every tile. Need an array/filter backed List class...
			List<Worker> piecesAt(int row, int col){
				ArrayList<Worker> tileWorkers = new ArrayList<Worker>();
				
				for(int i = 0; i < numPlayers; i++){
					if(workers[i][lastRow - row][col]) {
						tileWorkers.add(players.get(i).worker);
					}
				}
				
				return tileWorkers;
			}
		};
		
		ScrollPane p = scrollPane(container(tileBoard).pad(100f).center());
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
		
		return container(announcement).padTop(100f).top();
	}
	
	Actor makeTileQueueLayer(){
		tileQueue = new Board<Tile,Tile>(initQueueSize,1,100f,0f,"tileQueue"){
			Tile tileAt(int row, int col){
				return queue.get(col);
			}
		};
		
		ScrollPane pane = scrollPane(container(tileQueue).padLeft(this.stage.getWidth() - 180f).fill(0, 0));
		pane.setupOverscroll(0f, 0f, 0f);
		
		return container(pane).padTop(30f).top().fill(1f,0f);
	}

	Actor makeChooseRoleLayer(){
		roleChoose = new Board<Role,Role>(5,2,150f,0f,"chooseRole"){
			Role tileAt(int row, int col){
				return roles[1 - row][col];	
			}
		};
		
		return container(scrollPane(roleChoose)).bottom();
	}
}