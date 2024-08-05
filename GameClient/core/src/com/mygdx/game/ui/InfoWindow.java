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
import com.mygdx.game.network.DispatchServer;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Encoder;

/**
 * A class that encapsulates the option menu window
 */
public class InfoWindow extends GameWindow {
    private Label infoLabel;
    private TypingLabel discLabel;
    private TypingLabel creditsLabel;
    private TextButton backBtn;

    /**
     * Builds the option window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public InfoWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // makes sure title is in the correct language
        this.getTitleLabel().setText(" "+langBundle.format("info"));

        // label that describes the project name/version
        infoLabel = new Label( " "+ langBundle.format("gameInfo"), skin, "fontMedium", Color.WHITE);
        infoLabel.setWrap(true);
        infoLabel.setAlignment(Align.left);
        infoLabel.setHeight(infoLabel.getPrefHeight());

        // discord link label with different color and effects
        //https://game-icons.net/about.html#authors
        String discText = "{SIZE=135%}{COLOR=sage}[+talk][%]{ENDCOLOR}{FADE}{GRADIENT=ROYAL;azure}" +
                "{LINK=http://discord.gg/invite}http://discord.gg/invite{ENDGRADIENT}{ENDFADE}           ";
        discLabel = new TypingLabel( discText, iconFont) ;
        discLabel.setAlignment(Align.center);

        String creditsText = "{SIZE=79%}{COLOR=ROYAL}[+pencil-brush][%]{ENDCOLOR}" +
                "{SIZE=79%}{FADE}{GRADIENT=GOLD;red}" +
                "{LINK=https://game-icons.net/about.html#authors}@game icons{ENDGRADIENT}{ENDFADE}[%]      ";
        creditsLabel = new TypingLabel( creditsText, iconFont) ;
        creditsLabel.setAlignment(Align.center);

        // back button
        backBtn = new TextButton(langBundle.format("back"), skin);

        // builds options window
        this.getTitleTable().padBottom(6);
        this.defaults().spaceBottom(10).padLeft(22).padRight(8).padBottom(2).minWidth(320);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.setMovable(false);
        this.add(infoLabel).width(677);
        this.row();
        this.add(discLabel).padTop(15);
        this.row();
        this.add(creditsLabel).padTop(51).padBottom(14);
        this.row();
        this.add(backBtn).minWidth(182).spaceTop(25).padBottom(10);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new InfoController();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void startServerListening(DispatchServer client) {

    }

    @Override
    public void stopServerListening() {

    }

    @Override
    public void softKeyboardClosed() {

    }

    @Override
    public void softKeyboardOpened() {

    }

    @Override
    public void reloadLanguage() {

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
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
            });
            creditsLabel.addListener(new ClickListener() {
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


        // called when back button is pressed
        private void backBtnOnClick(InputEvent event, float x, float y) {
            remove(); // removes option window
        }
    }
}
