package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.mygdx.game.RogueFantasy;

import java.util.Locale;

/**
 * A class that encapsulates the option menu window
 */
public class InfoWindow extends Window {
    private RogueFantasy game;
    private Screen parent;
    private Preferences prefs;
    private AssetManager manager;
    private Skin skin;
    private Label infoLabel;
    private Label discLabel;
    private TextButton backBtn;
    private I18NBundle langBundle;
    /**
     * Builds the option window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public InfoWindow(RogueFantasy game, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(title, skin, styleName);
        this.game = game;
        this.parent = parent;
        this.manager = manager;
        this.skin = skin;

        // gets language bundle from asset manager
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // label that describes the project name/version
        infoLabel = new Label( " "+ langBundle.format("gameInfo"), skin, "font8", Color.WHITE);
        infoLabel.setWrap(true);
        infoLabel.setAlignment(Align.left);
        infoLabel.setHeight(infoLabel.getPrefHeight());

        // discord link label with different color
        discLabel = new Label( "http://discord.gg/invite", skin, "font8", Color.CYAN);
        discLabel.setWrap(true);
        discLabel.setAlignment(Align.center);
        discLabel.setHeight(discLabel.getPrefHeight());

        // back button
        backBtn = new TextButton(langBundle.format("back"), skin);

        // builds options window
        this.getTitleTable().padBottom(6);
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.getTitleTable().padBottom(6);
        this.setMovable(false);
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        this.add(infoLabel).width(677);
        this.row();
        this.add(discLabel).width(677);
        this.row();
        this.add(backBtn).minWidth(182).spaceTop(25);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new InfoController();
    }

    /**
     * A nested class that controls the info window
     */
    class InfoController {
        // constructor adds listeners to the actors
        public InfoController() {
            backBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    backBtnOnClick(event, x, y);
                }
            });
            discLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    discLabelOnClick(event, x, y);
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
            });
        }

        // called when discord label is pressed
        private void discLabelOnClick(InputEvent event, float x, float y) {
            Gdx.net.openURI("http://discord.gg/invite"); // opens discord link
        }

        // called when back button is pressed
        private void backBtnOnClick(InputEvent event, float x, float y) {
            remove(); // removes option window
        }
    }
}
