package com.highestqualitygames.tiledemo;

import java.util.Arrays;

import com.highestqualitygames.tiledemo.GameScreen;
import com.highestqualitygames.tiledemo.GameScreen.*;
import com.highestqualitygames.tiledemo.Assets.*;

class TileDemoGame extends com.badlogic.gdx.Game {
	public void create() {
		// Start game with hardcoded player list...
//		this.setScreen(new GameScreen(
//				Arrays.asList(new Player(Worker.Black, "Bob", PlayerType.LocalCPU),
//						new Player(Worker.Orange, "Jane", PlayerType.LocalHuman),
//						new Player(Worker.Teal, "Hilel", PlayerType.LocalCPU),
//						new Player(Worker.Purple, "Marius", PlayerType.LocalCPU))));
		

		// Start game with hardcoded player list...
		this.setScreen(new GameScreen(
				Arrays.asList(new Player(Worker.Black, "Bob", PlayerType.LocalHuman),
						new Player(Worker.Orange, "Jane", PlayerType.LocalHuman),
						new Player(Worker.Teal, "Hilel", PlayerType.LocalHuman))));
	}
}