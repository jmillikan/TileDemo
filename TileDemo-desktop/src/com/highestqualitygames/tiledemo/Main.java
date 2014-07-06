package com.highestqualitygames.tiledemo;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "TileDemo";
		cfg.width = 854;
		cfg.height = 480;
		
		new LwjglApplication(new TileDemoGame(), cfg);
	}
}
