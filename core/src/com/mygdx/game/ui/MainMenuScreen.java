package com.mygdx.game.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.mygdx.game.RogueFantasy;

/**
 * Main menu screen class
 */
public class MainMenuScreen implements Screen {

    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Table menuTable;
    private Stage bgStage;
    private AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private Label projectDescLabel;
    private OptionWindow optionWindow;
    private LoginWindow loginWindow;
    private InfoWindow infoWindow;
    private RegisterWindow registerWindow;
    private I18NBundle langBundle;
    private TextButton playBtn;
    private TextButton infoBtn;
    private TextButton optionsBtn;
    private TextButton exitBtn;
    private Texture bgTexture;
    private Image bg;

    /**
     * Builds the game main menu screen with login, register and options menu (prototype version)
     *
     * @param game    the reference to the game object that controls game screens
     * @param manager the asset manager containing assets loaded from the loading process
     */
    public MainMenuScreen(RogueFantasy game, AssetManager manager) {
        this.manager = manager;
        this.game = game;

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // gets font from asset manager
       // BitmapFont fontMedium = manager.get("fonts/immortalMedium.ttf", BitmapFont.class);

        // get and play music
        bgm = manager.get("bgm/mystic_dungeons.mp3", Music.class);
        bgm.setLooping(true);
        bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f));
        bgm.play();

        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        //skin.add("fontMedium", fontMedium, BitmapFont.class);

        // background stage
        bgStage = new Stage(new FillViewport(1280, 720));

        // TODO: WHY STAGE/WINDOW DOES NOT POSITION ITSELF CORRECTLY ON NEXUS S 25 API ??
        // TODO: BETTER KEYBOARD SUPPORT FOR ANDROID UI
        // TODO: INCREASE SIZE FOR ANDROID UI
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        // stage.setDebugAll(true);

        // gets language bundle from asset manager
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // creates option window
        optionWindow = new OptionWindow(game, stage, this, manager, " "+langBundle.format("option"),
                                            skin, "newWindowStyle");

        // creates login window
        loginWindow = new LoginWindow(game, stage, this, manager , " "+langBundle.format("loginWindowTitle"),
                                        skin, "newWindowStyle");

        // creates info window
        infoWindow = new InfoWindow(game, stage,this, manager, " "+langBundle.format("info"),
                                    skin, "newWindowStyle");

        // creates register window
        registerWindow = new RegisterWindow(game, stage, this, manager, " "+langBundle.format("register"),
                skin, "newWindowStyle");

        // label that describes the project name/version
        String descStr = " "+ langBundle.format("game") + " " + langBundle.format("version");
        projectDescLabel = new Label(descStr, skin, "fontMedium", Color.WHITE);
        projectDescLabel.setAlignment(Align.center);

        // just a fps label (TODO: TO BE CHANGED IN THE FUTURE)
        fpsLabel = new Label("fps:", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.center);

        optionsBtn = new TextButton(langBundle.format("option"), skin);

        playBtn = new TextButton(langBundle.format("login"), skin);

        // info button
        infoBtn = new TextButton(langBundle.format("info"), skin);

        // exit button
        exitBtn = new TextButton(langBundle.format("exit"), skin);

        // background image
        bgTexture=new Texture("img/main_menu_bg.jpg");
        bg=new Image(bgTexture);

        bgStage.addActor(bg);

        // updates camera zoom based on current device screen
        float uiZoomFactor = 1280f / Gdx.graphics.getWidth();
        if(Gdx.app.getType() == Application.ApplicationType.Android) // zoom menu if user is using android device
            ((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in  window menu

        // main menu table with main menu buttons
        menuTable = new Table(skin);
        menuTable.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        menuTable.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        menuTable.add(projectDescLabel);
        menuTable.row();
        menuTable.add(playBtn).minWidth(182);
        menuTable.row();
        menuTable.add(infoBtn).minWidth(182);
        menuTable.row();
        menuTable.add(optionsBtn).minWidth(182);
        menuTable.row();
        menuTable.add(exitBtn).minWidth(182);
        menuTable.row();
        menuTable.add(fpsLabel);
        menuTable.pack();

        stage.addActor(menuTable);

        // instantiate the controller that adds listeners and acts when needed
        new MainMenuController();

        // TODO: BETTER ORGANIZE WINDOW / REGISTER MODE

    }

    // returns instance of this screen
    private Screen getInstance() {
        return this;
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());

        bgStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        bgStage.draw();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    // updates stage viewport and centers menu windows on resize
    @Override
    public void resize(int width, int height) {
        bgStage.getViewport().update(width, height, true);
        stage.getViewport().update(width, height, true);
        menuTable.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        optionWindow.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        loginWindow.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        registerWindow.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        infoWindow.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
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
        bgStage.dispose();
        skin.dispose();
        manager.dispose();
        bgTexture.dispose();
        bgm.dispose();
        VisUI.dispose();
    }

    /**
     * Updates this screen based on commands received via opened windows
     */
    public void update(GameWindow window, boolean rebuild, ScreenCommands cmd) {
        // commands received through option window
        if(window.equals(optionWindow)) {
            // execute desired command
            switch(cmd) {
                case RELOAD_VOLUME:
                    bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f)); // update volumes
                    break;
                case RELOAD_LANGUAGE:
                    reloadLanguage(); // call method that reloads language and update texts
                    break;
                default:
                    Gdx.app.error("Unknown Window Command", "Current screen received an unknown command from option window");
                    break;
            }

            // reloads option window if requested (only if its opened) - updates language changes
            if(rebuild && optionWindow.getStage().equals(stage))
                loadWindow(optionWindow, true);

        } else if(window.equals(loginWindow)) {
            switch(cmd) {
                case LOAD_REGISTER_WINDOW:
                    loadWindow(registerWindow, true); // loads register window
                    break;
                default:
                    Gdx.app.error("Unknown Window Command", "Current screen received an unknown command from login window");
            }
        }
    }

    // method that reloads language and update texts based on current selected language
    private void reloadLanguage() {
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        String descStr = " "+ langBundle.format("game") + " " + langBundle.format("version");
        projectDescLabel.setText(descStr);
        playBtn.setText(langBundle.format("play"));
        infoBtn.setText(langBundle.format("info"));
        optionsBtn.setText(langBundle.format("option"));
        exitBtn.setText(langBundle.format("exit"));
    }

    /**
     * Loads a game window into stage
     * @param gameWindow the game window to be loaded
     * @param single if window should be the only one opened, if so close all other windows
     */
    private void loadWindow(GameWindow gameWindow, boolean single) {
        // clear all windows to make sure only one is going to be opened at the same time
        if(single)
            clearWindows();
        // builds game window - builds make sure that this window is not opened already and that it is clear for rebuilding
        gameWindow.build();
        // centers game window
        gameWindow.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        // adds built game window to the stage to display it
        stage.addActor(gameWindow);
    }

    // remove all windows from stage
    private void clearWindows() {
        infoWindow.remove();
        optionWindow.remove();
        loginWindow.remove();
        registerWindow.remove();
    }

    // possible screen commands to receive from windows
    enum ScreenCommands {
        RELOAD_LANGUAGE,
        RELOAD_VOLUME,
        LOAD_REGISTER_WINDOW
    }

    /**
     * A nested class that controls the main menu screen
     */
    class MainMenuController {
        // constructor adds listeners to the actors
        public MainMenuController() {
            playBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    playBtnOnClick(event, x, y);
                }
            });
            infoBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    infoBtnOnClick(event, x, y);
                }
            });
            optionsBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    optionsBtnOnClick(event, x, y);
                }
            });
            exitBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    exitBtnOnClick(event, x, y);
                }
            });
        }

        // called when play button is pressed
        private void playBtnOnClick(InputEvent event, float x, float y) {
            // loads login window
            loadWindow(loginWindow, true);
        }

        // called when info button is pressed
        private void infoBtnOnClick(InputEvent event, float x, float y) {
            // loads info window
            loadWindow(infoWindow, true);
        }

        // called when options button is pressed
        private void optionsBtnOnClick(InputEvent event, float x, float y) {
            // loads option window
            loadWindow(optionWindow, true);
        }

        // called when exit button is pressed
        private void exitBtnOnClick(InputEvent event, float x, float y) {
            Gdx.app.exit();
            //System.exit(0);
        }
    }
}
