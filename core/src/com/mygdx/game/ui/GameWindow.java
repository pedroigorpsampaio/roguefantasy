package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.RogueFantasy;

/**
 * A class that encapsulates a generic game window
 */
public abstract class GameWindow extends Window {
    protected Font iconFont;
    protected RogueFantasy game;
    protected Stage stage;
    protected Screen parent;
    protected Preferences prefs;
    protected AssetManager manager;
    protected Skin skin;
    protected I18NBundle langBundle;
    /**
     * Sets the window vars to be properly used in the game stage
     * Build should be implemented by each subclass and should build
     * the window accordingly to the stored vars of the superclass
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public GameWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(title, skin, styleName);
        this.game = game;
        this.stage = stage;
        this.parent = parent;
        this.manager = manager;
        this.skin = skin;
        iconFont = skin.get("iconFont", Font.class); // gets typist font with icons

        // gets language bundle from asset manager
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");
    }

    /**
     * Abstract method for subclasses to build the window according to their necessities
     */
    public abstract void build();

}
