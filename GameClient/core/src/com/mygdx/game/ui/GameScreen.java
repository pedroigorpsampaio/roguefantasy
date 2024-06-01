package com.mygdx.game.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.tommyettinger.textra.Font;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Component;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.util.Common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements the game screen
 */
public class GameScreen implements Screen, PropertyChangeListener {
    private static GameScreen instance;
    private static GameClient gameClient;
    private final Font font;
    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Label pingLabel;
    private Label ramLabel;
    private Stage bgStage;
    private static AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private RegisterWindow registerWindow;
    private I18NBundle langBundle;
    private Texture bgTexture;
    private Image bg;
    private SpriteBatch batch;
    private boolean isCharacterLoaded = true;
    private Vector3 vec3;
    private Vector2 touchPos;
    private Vector2 clientPos;
    private Vector2 deltaVec;
    private GameRegister.MoveCharacter movement; // player movement message (for tests rn)
    // time counters
    private float timeForUpdate = 0f;

    /**
     * Prepares the screen/stage of the game
     * @param manager       the asset manager containing loaded assets
     * @param gameClient    the reference to the game client responsible for the communication with
     *                      the game server
     */
    public GameScreen(AssetManager manager, GameClient gameClient) {
        this.game = RogueFantasy.getInstance();
        this.manager = manager;
        this.gameClient = gameClient;
        Component.assetManager = manager;

        batch = new SpriteBatch();

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // gets font from asset manager
        // BitmapFont fontMedium = manager.get("fonts/immortalMedium.ttf", BitmapFont.class);

        // get and play music
        bgm = manager.get("bgm/time_commando.mp3", Music.class);
        bgm.setLooping(true);
        bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f));
        bgm.play();

        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        //skin.add("fontMedium", fontMedium, BitmapFont.class);

        stage = new Stage(new StretchViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons

        fpsLabel = new Label("fps: 1441", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.left);
        fpsLabel.setX(12);

        pingLabel = new Label("ping: 3334", skin, "fontMedium", Color.WHITE);
        pingLabel.setAlignment(Align.left);
        pingLabel.setX(fpsLabel.getX()+fpsLabel.getWidth()+22);

        ramLabel = new Label("RAM: "+Common.getRamUsage(), skin, "fontMedium", Color.WHITE);
        ramLabel.setAlignment(Align.left);
        ramLabel.setX(pingLabel.getX()+pingLabel.getWidth()+22);

        stage.addActor(fpsLabel);
        stage.addActor(pingLabel);
        stage.addActor(ramLabel);

        // updates camera zoom based on current device screen
//        float uiZoomFactor = 720f / Gdx.graphics.getHeight();
//        if(Gdx.app.getType() == Application.ApplicationType.Android) // zoom menu if user is using android device
//            ((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in  window menu

        // character current movement
        movement = new GameRegister.MoveCharacter();

        stage.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(Gdx.input.isTouched(0)) return false; // ignore if there is clicking/touching

                if (keycode == Input.Keys.W && !Gdx.input.isKeyPressed(Input.Keys.UP)) movement.y += 1;
                else if (keycode == Input.Keys.UP && !Gdx.input.isKeyPressed(Input.Keys.W)) movement.y += 1;

                if (keycode == Input.Keys.S && !Gdx.input.isKeyPressed(Input.Keys.DOWN)) movement.y += -1;
                else if (keycode == Input.Keys.DOWN && !Gdx.input.isKeyPressed(Input.Keys.S)) movement.y += -1;

                if (keycode == Input.Keys.A && !Gdx.input.isKeyPressed(Input.Keys.LEFT)) movement.x += -1;
                else if (keycode == Input.Keys.LEFT && !Gdx.input.isKeyPressed(Input.Keys.A)) movement.x += -1;

                if (keycode == Input.Keys.D && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) movement.x += 1;
                else if (keycode == Input.Keys.RIGHT && !Gdx.input.isKeyPressed(Input.Keys.D)) movement.x += 1;

                if(movement.x > 1) movement.x = 1; if(movement.y > 1) movement.y = 1;
                if(movement.x < -1) movement.x = -1; if(movement.y < -1) movement.y = -1;
                return super.keyDown(event, keycode);
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if(Gdx.input.isTouched(0)) return false; // ignore if there is clicking/touching

                if (keycode == Input.Keys.W && !Gdx.input.isKeyPressed(Input.Keys.UP)) movement.y -= 1;
                else if (keycode == Input.Keys.UP && !Gdx.input.isKeyPressed(Input.Keys.W)) movement.y -= 1;

                if (keycode == Input.Keys.S && !Gdx.input.isKeyPressed(Input.Keys.DOWN)) movement.y -= -1;
                else if (keycode == Input.Keys.DOWN && !Gdx.input.isKeyPressed(Input.Keys.S)) movement.y -= -1;

                if (keycode == Input.Keys.A && !Gdx.input.isKeyPressed(Input.Keys.LEFT)) movement.x -= -1;
                else if (keycode == Input.Keys.LEFT && !Gdx.input.isKeyPressed(Input.Keys.A)) movement.x -= -1;

                if (keycode == Input.Keys.D && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) movement.x -= 1;
                else if (keycode == Input.Keys.RIGHT && !Gdx.input.isKeyPressed(Input.Keys.D)) movement.x -= 1;

                if(movement.x > 1) movement.x = 1; if(movement.y > 1) movement.y = 1;
                if(movement.x < -1) movement.x = -1; if(movement.y < -1) movement.y = -1;
                return super.keyUp(event, keycode);
            }
        });

        Component.stage = stage;

        // if its not listening to game server responses, start listening to it
        if(!gameClient.isListening(this))
            gameClient.addListener(this);

        // starts update timer that control user inputs and server communication
        //Timer.schedule(update, 0f, GameRegister.clientTickrate());
    }

    @Override
    public void show() {

    }

//    private Timer.Task update = new Timer.Task() {
//        @Override
//        public void run() {
//            if(!isCharacterLoaded) // character not loaded yet
//            {
//                if(gameClient.getClientCharacter() != null)
//                    isCharacterLoaded = true;
//                else
//                    return; // wait until character is loaded
//            }
//            // correct velocity
//            if(!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.UP) &&
//                    !Gdx.input.isKeyPressed(Input.Keys.S) && !Gdx.input.isKeyPressed(Input.Keys.DOWN) && !Gdx.input.isTouched(0))
//                movement.y = 0;
//            if(!Gdx.input.isKeyPressed(Input.Keys.A) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
//                    !Gdx.input.isKeyPressed(Input.Keys.D) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isTouched(0))
//                movement.x = 0;
//
//            // check if there is touch velocity
//            if(Gdx.input.isTouched(0)){
//                vec3 = new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
//                vec3 = stage.getCamera().unproject(vec3); // unproject screen touch
//                touchPos = new Vector2(vec3.x, vec3.y);
//                movement.xEnd = touchPos.x; movement.yEnd = touchPos.y;
//                movement.hasEndPoint = true;
//            } else { // if no click/touch is made, there is no end point goal of movement
//                movement.xEnd = 0; movement.yEnd = 0;
//                movement.hasEndPoint = false;
//            }
//
//            if(movement.x != 0 || movement.y != 0 || movement.hasEndPoint)
//                moveCharacter(); // moves character if there is velocity or endpoint
//        }
//    };

    /**
     * Apply movement to the character based on current client-server strategies enabled
     */
    private void moveCharacter() {
        GameRegister.MoveCharacter msg = new GameRegister.MoveCharacter();
        msg.x = movement.x;
        msg.y = movement.y;
        msg.hasEndPoint = movement.hasEndPoint;
        msg.xEnd = movement.xEnd; msg.yEnd = movement.yEnd;
        msg.requestId = gameClient.getRequestId(GameRegister.MoveCharacter.class);

        // sends to server the raw movement to be calculated by the authoritative server
        gameClient.moveCharacter(msg);
        // if client prediction is enabled, try to predict the movement calculating it locally
        if(GameRegister.clientPrediction)
            gameClient.getClientCharacter().predictMovement(msg);
    }

    /**
     * Should update game using fixed step defined by GameRegister.clientTickrate()
     */
    public void update() {
        // correct velocity
        if(!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.UP) &&
                !Gdx.input.isKeyPressed(Input.Keys.S) && !Gdx.input.isKeyPressed(Input.Keys.DOWN) && !Gdx.input.isTouched(0))
            movement.y = 0;
        if(!Gdx.input.isKeyPressed(Input.Keys.A) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isTouched(0))
            movement.x = 0;

        // check if there is touch velocity
        if(Gdx.input.isTouched(0)){
            vec3 = new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
            vec3 = stage.getCamera().unproject(vec3); // unproject screen touch
            touchPos = new Vector2(vec3.x, vec3.y);
            movement.xEnd = touchPos.x; movement.yEnd = touchPos.y;
            movement.hasEndPoint = true;
        } else { // if no click/touch is made, there is no end point goal of movement
            movement.xEnd = 0; movement.yEnd = 0;
            movement.hasEndPoint = false;
        }

        if(movement.x != 0 || movement.y != 0 || movement.hasEndPoint)
            moveCharacter(); // moves character if there is velocity or endpoint
    }

    @Override
    public void render(float delta) {
        if(!isCharacterLoaded) // character not loaded yet
        {
            if(gameClient.getClientCharacter() != null)
                isCharacterLoaded = true;
            else
                return; // wait until character is loaded
        }

//        if(gameClient.isPredictingRecon.get())
//            return;

        // checks if its time to update game
        timeForUpdate += delta;
        if(timeForUpdate >= GameRegister.clientTickrate()) {
            update();
            gameClient.setUpdateDelta(timeForUpdate);
            timeForUpdate = 0f;
        }

        ScreenUtils.clear(0.2f, 0.6f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());
        pingLabel.setText("ping: " + gameClient.getAvgLatency());
        ramLabel.setText("RAM: " + Common.getRamUsage() + " MB");

        // draws stage
        stage.act(Math.min(delta, 1 / 30f));
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

//    public void stopUpdateTimer() {
//        if(update.isScheduled()) update.cancel();
//    }

    /**
     * Method that reacts on game server responses
     * NOTE:  Gdx.app.postRunnable(() makes it thread-safe with libGDX UI
     * @param propertyChangeEvent   the server response encapsulated in PCE
     */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        Gdx.app.postRunnable(() -> {
            // loads main menu
            RogueFantasy.getInstance().setScreen(new LoadScreen("menu"));
            // stops update timer
            //stopUpdateTimer();
        });
    }
}
