package com.mygdx.game.ui;

import static com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader;
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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.KnownFonts;
import com.kotcrab.vis.ui.VisUI;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.ChatClient;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Encoder;

import java.util.ArrayList;
import java.util.HashMap;
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
        bgTexture.setFilter(Linear, Linear);
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
            fontMedium.fontParameters.borderWidth = 1.75f;
            fontMedium.fontParameters.magFilter = Texture.TextureFilter.Linear;
            fontMedium.fontParameters.minFilter = Texture.TextureFilter.Linear;
            manager.load("fonts/immortalMedium.ttf", BitmapFont.class, fontMedium);
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontLarge = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            // large font
            fontLarge.fontFileName = "fonts/immortal.ttf";
            fontLarge.fontParameters.size = 30;
            fontLarge.fontParameters.borderColor = Color.BLACK;
            fontLarge.fontParameters.borderWidth = 2f;
            fontLarge.fontParameters.magFilter = Texture.TextureFilter.Linear;
            fontLarge.fontParameters.minFilter = Texture.TextureFilter.Linear;
            manager.load("fonts/fontLarge.ttf", BitmapFont.class, fontLarge);
            // chat font
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontChat = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            fontChat.fontFileName = "fonts/immortal.ttf";
            fontChat.fontParameters.size = 21;
            fontChat.fontParameters.borderColor = Color.BLACK;
            fontChat.fontParameters.borderWidth = 1.5f;
            fontChat.fontParameters.magFilter = Texture.TextureFilter.Linear;
            fontChat.fontParameters.minFilter = Texture.TextureFilter.Linear;
            manager.load("fonts/chatFont.ttf", BitmapFont.class, fontChat);
            // floating text font
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontFloatingText = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            fontFloatingText.fontFileName = "fonts/immortal.ttf";
            fontFloatingText.fontParameters.size = 18;
            fontFloatingText.fontParameters.borderColor = Color.BLACK;
            fontFloatingText.fontParameters.borderWidth = 1.25f;
            fontFloatingText.fontParameters.magFilter = Texture.TextureFilter.Linear;
            fontFloatingText.fontParameters.minFilter = Texture.TextureFilter.Linear;
            manager.load("fonts/floatingTextFont.ttf", BitmapFont.class, fontFloatingText);
            // blocks until skin is fully loaded
            manager.finishLoading();
        }

        // gets skin that should be loaded by now (ignore if its changing to game screen)
        if(manager.isLoaded("skin/neutralizer/neutralizer-ui.json") && !screen.equals("game")) {
            // skin is available, let's fetch it and do something interesting
            skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            // gets font loaded
            BitmapFont fontMedium = manager.get("fonts/immortalMedium.ttf", BitmapFont.class);
            BitmapFont fontLarge = manager.get("fonts/fontLarge.ttf", BitmapFont.class);
            BitmapFont fontChat = manager.get("fonts/chatFont.ttf", BitmapFont.class);
            BitmapFont fontFloatingText = manager.get("fonts/floatingTextFont.ttf", BitmapFont.class);
            fontMedium.getData().markupEnabled = true;
            fontLarge.getData().markupEnabled = true;
            fontChat.getData().markupEnabled = true;
            fontFloatingText.getData().markupEnabled = true;
            // creates the typist font containing emojis and game icons that is ready for effects
            Font iconFont = new Font(fontMedium);
            Font emojiFont = new Font(fontMedium);
            Font floatingTextFont = new Font(fontFloatingText);
            iconFont.setInlineImageMetrics(-50f, -4f, 0f);
            emojiFont.setInlineImageMetrics(-15f, -12f, 0f);
            iconFont.setUnderlineMetrics(-0.4f,-0.1f,-0.1f,0f);
            emojiFont.setUnderlineMetrics(-0.4f,-0.1f,-0.1f,0f);
            floatingTextFont.setUnderlineMetrics(-0.4f,-0.1f,-0.1f,0f);
            KnownFonts.addEmoji(floatingTextFont);
            KnownFonts.addEmoji(emojiFont);
            KnownFonts.addGameIcons(iconFont);
            skin.add("emojiFont", emojiFont, Font.class);
            skin.add("iconFont", iconFont, Font.class);
            skin.add("fontMedium", fontMedium, BitmapFont.class);
            skin.add("fontLarge", fontLarge, BitmapFont.class);
            skin.add("fontChat", fontChat, BitmapFont.class);
            skin.add("floatingTextFont", floatingTextFont, Font.class);
            // creates new styles based on the new font
            Window.WindowStyle wStyle =  skin.get(Window.WindowStyle.class);
            wStyle.titleFont = fontMedium;
            wStyle.titleFontColor = Color.WHITE;
            skin.add("newWindowStyle", wStyle, Window.WindowStyle.class);

            Label.LabelStyle lStyle = skin.get(Label.LabelStyle.class);
            lStyle.font = fontMedium;
            lStyle.fontColor = Color.WHITE;
            skin.add("newLabelStyle", lStyle, Label.LabelStyle.class);

            Label.LabelStyle largeLabelStyle = skin.get(Label.LabelStyle.class);
            largeLabelStyle.font = fontLarge;
            largeLabelStyle.font.getData().scale(1.2f);
            largeLabelStyle.fontColor = Color.WHITE;
            skin.add("largeLabelStyle", largeLabelStyle, Label.LabelStyle.class);

            Label.LabelStyle lChatStyle = skin.get(Label.LabelStyle.class);
            lChatStyle.font = fontChat;
            lChatStyle.fontColor = Color.WHITE;
            skin.add("chatLabelStyle", lChatStyle, Label.LabelStyle.class);

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

//            down: button-pressed
//            up: button
//            font: font
//            fontColor: white
//            checked: button-pressed
//            downFontColor: light-gray

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

        manager.load("sfx/packed_textures/sfx.atlas", TextureAtlas.class);
        manager.load("ui/packed_textures/ui.atlas", TextureAtlas.class);
        // blocks until ui atlas is fully loaded
        manager.finishLoading();

        // loads assets based on the next screen
        if(screen.equals("menu")) {
            //manager.load("eldamar.mp3", Music.class);
            TextureAtlas sfxAtlas = manager.get("sfx/packed_textures/sfx.atlas", TextureAtlas.class);
            for (Texture t: sfxAtlas.getTextures()) // apply linear filter to smooth texture resizing
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            TextureAtlas uiAtlas = manager.get("ui/packed_textures/ui.atlas", TextureAtlas.class);
            for (Texture t: uiAtlas.getTextures()) // apply linear filter to smooth texture resizing
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // builds cursor bank (only once, menu shares it with game screen)
            CommonUI.cursorBank = new HashMap<>();
            TextureAtlas.AtlasRegion region = uiAtlas.findRegion("Cursor Default");
            TextureRegion texRegion = new TextureRegion(new Sprite(region));
            Cursor c = Gdx.graphics.newCursor(CommonUI.pixmapFromTextureRegion(texRegion), 0, 0);
            CommonUI.cursorBank.put("Cursor Default", c);
            Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Default"));

            region = uiAtlas.findRegion("Cursor Chop Green");
            CommonUI.cursorBank.put("Cursor Chop Green",
                    Gdx.graphics.newCursor(CommonUI.pixmapFromTextureRegion(new TextureRegion(new Sprite(region))), 0, 0));

            region = uiAtlas.findRegion("Cursor Attack Green");
            CommonUI.cursorBank.put("Cursor Attack Green",
                    Gdx.graphics.newCursor(CommonUI.pixmapFromTextureRegion(new TextureRegion(new Sprite(region))), 0, 0));

            region = uiAtlas.findRegion("Cursor Cannot Use");
            CommonUI.cursorBank.put("Cursor Cannot Use",
                    Gdx.graphics.newCursor(CommonUI.pixmapFromTextureRegion(new TextureRegion(new Sprite(region))), 0, 0));

            region = uiAtlas.findRegion("Cursor Magic Use Green");
            CommonUI.cursorBank.put("Cursor Magic Use Green",
                    Gdx.graphics.newCursor(CommonUI.pixmapFromTextureRegion(new TextureRegion(new Sprite(region))), 0, 0));

            // load music
            manager.load("bgm/menu/menu_0.mp3", Music.class);
            manager.load("bgm/menu/menu_1.mp3", Music.class);
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
        bgTexture.setFilter(Linear, Linear);
        bg=new Image(bgTexture);

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        /** load game specific assets **/

        // create dir catalogs
        ArrayList<String> creatureSounds = new ArrayList<>();
        ArrayList<String> weaponSounds = new ArrayList<>();
        if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
            creatureSounds = Common.createDirCatalog("assets/sounds/creatures");
            weaponSounds = Common.createDirCatalog("assets/sounds/weapons");
        } else if(Gdx.app.getType() == Application.ApplicationType.Android) {
            creatureSounds = Common.createDirCatalogAndroid("sounds/creatures");
            weaponSounds = Common.createDirCatalogAndroid("sounds/weapons");
        }

        // load sounds
        for(int i = 0; i < creatureSounds.size(); i++) {
            manager.load("sounds/creatures/" + creatureSounds.get(i), Sound.class);
        }
        for(int i = 0; i < weaponSounds.size(); i++) {
            manager.load("sounds/weapons/" + weaponSounds.get(i), Sound.class);
        }

        // load music
        manager.load("bgm/maps/bgm_0.mp3", Music.class);
        manager.load("bgm/maps/bgm_1.mp3", Music.class);
        manager.load("bgm/maps/bgm_2.mp3", Music.class);

        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);

        // only needed once
        manager.setLoader(TiledMap.class, new AtlasTmxMapLoader(new InternalFileHandleResolver()));
        AtlasTmxMapLoader.AtlasTiledMapLoaderParameters params = new AtlasTmxMapLoader.AtlasTiledMapLoaderParameters();
        params.forceTextureFilters = true;
        params.generateMipMaps = true;
        params.textureMinFilter = MipMapLinearNearest;
        params.textureMagFilter = Linear;

//        AtlasTmxMapLoader.Parameters par = new AtlasTmxMapLoader.Parameters();
//        par.textureMinFilter = Texture.TextureFilter.MipMapLinearNearest;
//        par.textureMagFilter = Texture.TextureFilter.Linear;
//        par.generateMipMaps = true;

        // load map
        manager.load("world/novaterra.tmx", TiledMap.class, params);

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
            pgBar.remove();
            bg.remove();
            //dispose();
            if(screen.equals("menu")) {
                game.setScreen(new MainMenuScreen(manager, LoginClient.getInstance()));
            } else if(screen.equals("game")) {
                GameClient gameClient = GameClient.getInstance(); // gets gameclient instance
                ChatClient chatClient = ChatClient.getInstance(); // gets chat client instance
                GameScreen gameScreen = new GameScreen(manager, gameClient, chatClient);
                new Thread(() -> {
                    chatClient.connect(); // connects chat client with server
                    gameClient.connect(decryptedToken); // connects gameclient with server
                }).start();

                while(gameClient.getClientCharacter() == null);

                chatClient.login(gameClient.getClientCharacter()); // send msg to chat server log in correctly in chat
                chatClient.loadContacts(gameClient.getClientCharacter().id); // loads client contacts
                //while(!chatClient.isContactsLoaded);
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
