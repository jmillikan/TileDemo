package com.highestqualitygames.tiledemo;

import java.util.List;

import sun.security.ssl.Debug;

import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import com.highestqualitygames.tiledemo.GameScreen.Player;
import com.highestqualitygames.tiledemo.GameScreen.PlayerState;

public class Players extends Widget {
	List<Player> players;
	List<PlayerState> playerStates;

	// The height we'd like to have to draw each player, also the size of playerBG
	float prefPlayerHeight = 200f;
	float prefPlayerWidth = 300f;
	
	float scale = 1f;
	
	public Players(List<Player> p, List<PlayerState> ps){ 
		this.setTouchable(Touchable.childrenOnly);
		players = p;
		playerStates = ps;
		this.setSize(this.getPrefWidth(), this.getPrefHeight());
	}
	
	public float getPrefHeight(){
		return players.size() * prefPlayerHeight;
	}
	
	public float getPrefWidth(){
		return prefPlayerWidth;
	}
	
	public void layout(){
		super.layout();
		scale = getHeight() / getPrefHeight();
		
		Debug.println("Players", String.format("Scale: %f (%f,%f)", scale, getScaleX(), getScaleY()));
		Debug.println("Players", String.format("(%f,%f) @ (%f,%f) @ (%f,%f)", getWidth(), getHeight(), getX(), getY(), getOriginX(), getOriginY()));
	}

	public void draw(Batch batch, float a) {
		validate();
		
		float x = getX();
		float y = getY();
		
		batch.setColor(getColor());
		
		for(int i = players.size() - 1; i >= 0; i--){
			PlayerState ps = playerStates.get(i);
			
			batch.draw(Assets.playerBG, x, y, getOriginX(), getOriginY(), 300f, 200f, scale, scale, 0f);
			
			Assets.font.draw(batch, players.get(i).name, x + 40f * scale, y + 50f * scale);
			
			batch.draw(players.get(i).worker.tr(), x + 10f * scale, y + 60f * scale, getOriginX(), getOriginY(), 100f, 100f, scale, scale, 0f);
			
			Assets.font.draw(batch, String.format("x %d", playerStates.get(i).numWorkers), x + 110f * scale, y + 100f * scale);

			Assets.font.draw(batch, String.format("%d", playerStates.get(i).score), x + 110f * scale, y + 50f * scale);

			if(!ps.currentTile.IsEmpty()){
				batch.draw(ps.currentTile.tr(), x + 200f * scale, y + 50f * scale, getOriginX(), getOriginY(), 100f, 100f, scale, scale, 0f);
			}

			if(!ps.currentRole.IsEmpty()){
				batch.draw(ps.currentRole.tr(), x + 200f * scale, y + 50f * scale, getOriginX(), getOriginY(), 100f, 100f, scale, scale, 0f);
			}
			
			y += scale * prefPlayerHeight;
		}
	}
}
