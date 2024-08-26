package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
public class ContextWindow extends GameWindow {

    private Table uiTable;

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
    public ContextWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {

    }

    /**
     * Builds context menu with the table provided
     * @param table     the table containing the context menu options
     */
    public void loadTable(Table table) {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        TextureAtlas uiAtlas = manager.get("ui/packed_textures/ui.atlas");
        this.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        this.defaults().padTop(7).padBottom(7).padLeft(9).padRight(9);
        this.setMovable(false);

        this.pack();

        this.defaults().minWidth(table.getMinWidth()).minHeight(table.getMinHeight());
        this.add(table).expand().bottom().fillX();
        this.pack();
        this.layout();
        this.validate();
        stage.draw();
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
    public void dispose() {

    }

    @Override
    public void reloadLanguage() {

    }

    public boolean isPointOn(float x, float y) {
        Vector2 coordinates = stageToLocalCoordinates(new Vector2(x, y));
        return this.hit(coordinates.x, coordinates.y, true) != null;
    }
}
