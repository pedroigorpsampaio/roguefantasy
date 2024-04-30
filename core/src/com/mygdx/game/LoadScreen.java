package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Locale;

/**
 * Load screen that loads resources async while showing a loading bar
 */
public class LoadScreen implements Screen {
    final RogueFantasy game;
    final String screen;
    Stage stage;
    AssetManager manager;
    Skin skin;
    ProgressBar pgBar;
    Preferences prefs;


    /**
     * Loads the resources for the desired screen to be loaded
     *
     * @param  game  the reference to the game object that controls game screens
     * @param screen the screen to be loaded ("menu" or "game")
     */
    public LoadScreen(RogueFantasy game, String screen, AssetManager manager) {
        this.game = game;
        this.screen = screen;
        this.manager = manager;

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // loads skin blocking progress
        if(!manager.isLoaded("skin/neutralizer/neutralizer-ui.json")) {
            // loads skin to use for loading screen (blocks until finish loading, to display correctly the loading screen)
            manager.load("skin/neutralizer/neutralizer-ui.json", Skin.class);
            // load font
            FileHandleResolver resolver = new InternalFileHandleResolver();
            manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
            manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontSmall = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            fontSmall.fontFileName = "fonts/immortal.ttf";
            fontSmall.fontParameters.size = 25;
            fontSmall.fontParameters.borderColor = Color.BLACK;
            fontSmall.fontParameters.borderWidth = 1.5f;
            manager.load("fonts/immortal13.ttf", BitmapFont.class, fontSmall);
            // blocks until skin is fully loaded
            manager.finishLoading();
        }

        // gets skin that should be loaded by now
        if(manager.isLoaded("skin/neutralizer/neutralizer-ui.json")) {
            // skin is available, let's fetch it and do something interesting
            skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            // gets font loaded
            BitmapFont font8 = manager.get("fonts/immortal13.ttf", BitmapFont.class);
            skin.add("font8", font8, BitmapFont.class);
            // creates new styles based on the new font
            Window.WindowStyle wStyle =  skin.get(Window.WindowStyle.class);
            wStyle.titleFont = font8;
            wStyle.titleFontColor = Color.WHITE;
            skin.add("newWindowStyle", wStyle, Window.WindowStyle.class);

            Label.LabelStyle lStyle = skin.get(Label.LabelStyle.class);
            lStyle.font = font8;
            lStyle.fontColor = Color.WHITE;
            skin.add("newLabelStyle", lStyle, Label.LabelStyle.class);

            TextField.TextFieldStyle tfStyle = skin.get(TextField.TextFieldStyle.class);
            tfStyle.font = font8;
            tfStyle.fontColor = Color.WHITE;
            skin.add("newTextFieldStyle", tfStyle, TextField.TextFieldStyle.class);

            TextButton.TextButtonStyle tbStyle = skin.get(TextButton.TextButtonStyle.class);
            tbStyle.font = font8;
            tbStyle.fontColor = Color.WHITE;
            skin.add("newTextButtonStyle", tbStyle, TextButton.TextButtonStyle.class);

            CheckBox.CheckBoxStyle cbStyle =  skin.get(CheckBox.CheckBoxStyle.class);
            cbStyle.font = font8;
            cbStyle.fontColor = Color.WHITE;
            skin.add("newCheckBoxStyle", cbStyle, CheckBox.CheckBoxStyle.class);

            SelectBox.SelectBoxStyle sbStyle =  skin.get(SelectBox.SelectBoxStyle.class);
            sbStyle.font = font8;
            sbStyle.fontColor = Color.WHITE;
            skin.add("newSelectBoxStyle", sbStyle, SelectBox.SelectBoxStyle.class);

            List.ListStyle liStyle =  skin.get(List.ListStyle.class);
            liStyle.font = font8;
            skin.add("newListStyle", liStyle, List.ListStyle.class);
        }

        // loads assets based on the next screen
        if(screen.equals("menu")) {
            //manager.load("eldamar.mp3", Music.class);
            // load music
            manager.load("eldamar.mp3", Music.class);
            // loads language bundles
            Locale defaultLocale = new Locale(prefs.getString("lastLang", "en"), prefs.getString("lastCountry", ""));
            I18NBundleLoader.I18NBundleParameter langParams = new I18NBundleLoader.I18NBundleParameter(defaultLocale, "UTF-8");
            manager.load("lang/langbundle", I18NBundle.class, langParams);
        } else {

        }

        pgBar = new ProgressBar(0, 1, 0.1f, false, skin);
        stage.addActor(pgBar);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        if(manager.update()) {
            // we are done loading, let's move to another screen!
            game.setScreen(new MainMenuScreen(game, manager));
            dispose();
        }

        // display loading information
        float progress = manager.getProgress();
        System.out.println(progress);
        pgBar.setValue(progress);

        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
