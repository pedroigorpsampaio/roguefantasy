package com.mygdx.game.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.mygdx.game.network.GameClient;

import java.util.HashMap;

public class Jukebox {
    public static AssetManager manager;
    public enum SoundType {
        WEAPON,
        CREATURE
    }

    public static void playSound(SoundType type, String identifier) {
        // only play sound effects if client player is alive
        if(!GameClient.getInstance().getClientCharacter().isAlive)
            return;

        // builds path to song asset
        StringBuilder path = new StringBuilder();
        switch (type) {
            case CREATURE:
                path.append("sounds/creatures/");
                break;
            case WEAPON:
                path.append("sounds/weapons/");
                break;
            default:
                Gdx.app.log("sound", "unknown sound type");
                break;
        }
        path.append(identifier);
        path.append(".mp3");
        // gets sound
        Sound sound = manager.get(String.valueOf(path), Sound.class);
        // gets preferences reference, that stores simple data persisted between executions
        Preferences prefs = Gdx.app.getPreferences("globalPrefs");
        long id = sound.play();
        // sets attributes
        sound.setVolume(id, prefs.getFloat("sfxVolume", 1.0f));
        sound.setLooping(id, false);
    }

}
