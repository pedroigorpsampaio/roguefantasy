package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.game.ui.LoadScreen;

public class RogueFantasy extends Game {

	public SpriteBatch batch;
	public BitmapFont font;

	public void create() {
		batch = new SpriteBatch();
		font = new BitmapFont(); // use libGDX's default Arial font
		this.setScreen(new LoadScreen(this, "menu", new AssetManager()));
	}

	public void render() {
		super.render(); // important!
	}

	public void dispose() {
		batch.dispose();
		font.dispose();
	}

}