package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.ui.LoadScreen;

public class RogueFantasy extends Game {

	// sets defaults and loads first screen
	public void create() {
		// gets preferences reference, that stores simple data persisted between executions
		Preferences prefs = Gdx.app.getPreferences("globalPrefs");

		// Set default values in preferences
		prefs.putInteger("defaultTextColor", Color.WHITE.toIntBits());
		prefs.putInteger("defaultErrorColor", Color.RED.toIntBits());
		prefs.putInteger("defaultMaxTimeOutValue", 15000); // in milli
		
		prefs.flush();

		this.setScreen(new LoadScreen(this, "menu", new AssetManager()));
	}

	public void render() {
		super.render(); // important!
	}

	public void dispose() {

	}

}