package com.mygdx.game.ui;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.KnownFonts;
import com.kotcrab.vis.ui.VisUI;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Encoder;

import java.util.Locale;
import java.util.Random;

/**
 * Load screen that loads resources async while showing a loading bar
 */
public class LoadScreen implements Screen {
    final RogueFantasy game;
    final String screen;
    private Texture bgTexture;
    private String decryptedToken;
    Stage stage;
    AssetManager manager;
    Skin skin;
    ProgressBar pgBar;
    Preferences prefs;
    private Image bg;
    private Screen nextScreen;


    /**
     * Loads the resources for the desired screen to be loaded
     * @param screen the screen to be loaded ("menu" or "game")
     */
    public LoadScreen(String screen) {
        this.game = RogueFantasy.getInstance();
        this.screen = screen;
        this.manager = new AssetManager();

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Random rand = new Random();
        bgTexture=new Texture("img/initial_splash_screen_"+rand.nextInt(2)+".png");
        bg=new Image(bgTexture);

        // loads skin blocking progress
        if(!manager.isLoaded("skin/neutralizer/neutralizer-ui.json")) {
            // loads skin to use for loading screen (blocks until finish loading, to display correctly the loading screen)
            manager.load("skin/neutralizer/neutralizer-ui.json", Skin.class);
            // TODO: LOAD DIFFERENT SIZES OF FONT
            // load fonts
            FileHandleResolver resolver = new InternalFileHandleResolver();
            manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
            manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontMedium = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            // medium font
            fontMedium.fontFileName = "fonts/immortal.ttf";
            fontMedium.fontParameters.size = 25;
            fontMedium.fontParameters.borderColor = Color.BLACK;
            fontMedium.fontParameters.borderWidth = 1.5f;
            fontMedium.fontParameters.magFilter = Texture.TextureFilter.Linear;
            fontMedium.fontParameters.minFilter = Texture.TextureFilter.Linear;
            manager.load("fonts/immortalMedium.ttf", BitmapFont.class, fontMedium);
            // blocks until skin is fully loaded
            manager.finishLoading();
        }

        // gets skin that should be loaded by now (ignore if its changing to game screen)
        if(manager.isLoaded("skin/neutralizer/neutralizer-ui.json") && !screen.equals("game")) {
            // skin is available, let's fetch it and do something interesting
            skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            // gets font loaded
            BitmapFont fontMedium = manager.get("fonts/immortalMedium.ttf", BitmapFont.class);
            fontMedium.getData().markupEnabled = true;
            // creates the typist font containing emojis and game icons that is ready for effects
            Font iconFont = new Font(fontMedium);
            Font emojiFont = new Font(fontMedium);
            iconFont.setInlineImageMetrics(-50f, -4f, 0f);
            emojiFont.setInlineImageMetrics(-15f, -12f, 0f);
            iconFont.setUnderlineMetrics(-0.4f,-0.1f,-0.1f,0f);
            emojiFont.setUnderlineMetrics(-0.4f,-0.1f,-0.1f,0f);
            KnownFonts.addEmoji(emojiFont);
            KnownFonts.addGameIcons(iconFont);
            skin.add("emojiFont", emojiFont, Font.class);
            skin.add("iconFont", iconFont, Font.class);
            skin.add("fontMedium", fontMedium, BitmapFont.class);
            // creates new styles based on the new font
            Window.WindowStyle wStyle =  skin.get(Window.WindowStyle.class);
            wStyle.titleFont = fontMedium;
            wStyle.titleFontColor = Color.WHITE;
            skin.add("newWindowStyle", wStyle, Window.WindowStyle.class);

            Label.LabelStyle lStyle = skin.get(Label.LabelStyle.class);
            lStyle.font = fontMedium;
            lStyle.fontColor = Color.WHITE;
            skin.add("newLabelStyle", lStyle, Label.LabelStyle.class);

            TextField.TextFieldStyle tfStyle = skin.get(TextField.TextFieldStyle.class);
            tfStyle.font = fontMedium;
            tfStyle.fontColor = Color.WHITE;
            Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
            pxColor.setColor(new Color(0x75757575));
            pxColor.fill();
            tfStyle.focusedBackground = new Image(new Texture(pxColor)).getDrawable();
            pxColor.setColor(new Color(Color.TEAL));
            pxColor.fill();
            tfStyle.selection =  new Image(new Texture(pxColor)).getDrawable();
            pxColor.dispose();
            skin.add("newTextFieldStyle", tfStyle, TextField.TextFieldStyle.class);

            TextButton.TextButtonStyle tbStyle = skin.get(TextButton.TextButtonStyle.class);
            tbStyle.font = fontMedium;
            tbStyle.fontColor = Color.WHITE;
            skin.add("newTextButtonStyle", tbStyle, TextButton.TextButtonStyle.class);

            CheckBox.CheckBoxStyle cbStyle =  skin.get(CheckBox.CheckBoxStyle.class);
            cbStyle.font = fontMedium;
            cbStyle.fontColor = Color.WHITE;
            skin.add("newCheckBoxStyle", cbStyle, CheckBox.CheckBoxStyle.class);

            SelectBox.SelectBoxStyle sbStyle =  skin.get(SelectBox.SelectBoxStyle.class);
            sbStyle.font = fontMedium;
            sbStyle.fontColor = Color.WHITE;
            skin.add("newSelectBoxStyle", sbStyle, SelectBox.SelectBoxStyle.class);

            List.ListStyle liStyle =  skin.get(List.ListStyle.class);
            liStyle.font = fontMedium;
            skin.add("newListStyle", liStyle, List.ListStyle.class);

            if(!VisUI.isLoaded())
                VisUI.load(skin); //load VisUI
        }

        // loads assets based on the next screen
        if(screen.equals("menu")) {
            //manager.load("eldamar.mp3", Music.class);
            // load music
            manager.load("bgm/mystic_dungeons.mp3", Music.class);
            // loads language bundles
            Locale defaultLocale = new Locale(prefs.getString("lastLangLocale", "en"), prefs.getString("lastCountry", ""));
            I18NBundleLoader.I18NBundleParameter langParams = new I18NBundleLoader.I18NBundleParameter(defaultLocale, "UTF-8");
            manager.load("lang/langbundle", I18NBundle.class, langParams);
        } else if(screen.equals("game")){

        }

        stage.addActor(bg);

        pgBar = new ProgressBar(0, 1, 0.01f, false, skin);
        pgBar.setBounds(Gdx.graphics.getWidth()/2f - Gdx.graphics.getWidth() / 8f, Gdx.graphics.getHeight()/32f,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight()/4f);
        stage.addActor(pgBar);
    }

    // to be used for game screen
    public LoadScreen(String screen, AssetManager manager, String decryptedToken) {
        this.game = RogueFantasy.getInstance();
        this.screen = screen;
        this.manager = manager;
        this.decryptedToken = decryptedToken;

        // background image
        bgTexture=new Texture("img/loading_screen_bg.png");
        bg=new Image(bgTexture);

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // load game specific assets
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        manager.load("bgm/time_commando.mp3", Music.class);
        // only needed once
        manager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
        TmxMapLoader.Parameters par = new TmxMapLoader.Parameters();
        par.textureMinFilter = Texture.TextureFilter.MipMapLinearNearest;
        par.textureMagFilter = Texture.TextureFilter.Linear;
        par.generateMipMaps = true;
        manager.load("world/testmap.tmx", TiledMap.class, par);

        stage.addActor(bg);

        pgBar = new ProgressBar(0, 1, 0.01f, false, skin);
        pgBar.setBounds(Gdx.graphics.getWidth()/2f - Gdx.graphics.getWidth() / 8f, Gdx.graphics.getHeight()/32f,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight()/4f);
        stage.addActor(pgBar);
    }

    @Override
    public void show() {

    }

    public void switchScreen(){
        stage.getRoot().getColor().a = 1;
        SequenceAction sequenceAction = new SequenceAction();
        sequenceAction.addAction(fadeOut(0.5f));
        sequenceAction.addAction(run(() -> {
            if(screen == "game") {
                pgBar.remove();
                bg.remove();
            }
            //dispose();
            if(screen.equals("menu")) {
                game.setScreen(new MainMenuScreen(manager, LoginClient.getInstance()));
            } else if(screen.equals("game")) {
                GameClient gameClient = GameClient.getInstance(); // gets gameclient instance
                GameScreen gameScreen = new GameScreen(manager, gameClient);
                gameClient.connect(); // connects gameclient with server
                gameClient.sendTokenAsync(decryptedToken); // login using token received
                while(gameClient.getClientCharacter() == null);
                game.setScreen(gameScreen);
            }
        }));
        stage.getRoot().addAction(sequenceAction);
    }

    float lifeTime = 2f;
    float elapsed = 0f;
    @Override
    public void render(float delta) {
        elapsed += delta;

        // display loading information
        float progress = manager.getProgress();
        float alpha = Math.min(1f, elapsed/lifeTime);
        float simProgress = progress*alpha;

        pgBar.setValue(simProgress);

        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        if(manager.update() && simProgress >= 1f) {
            switchScreen(); // switches screen with a fade out transition
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        pgBar.setBounds(Gdx.graphics.getWidth()/2f - Gdx.graphics.getWidth() / 8f, Gdx.graphics.getHeight()/32f,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight()/4f);
        bg.setBounds(0, 0, width, height);
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
        //pgBar.remove();
        stage.dispose();
    }
}
