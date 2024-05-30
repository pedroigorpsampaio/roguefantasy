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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.tommyettinger.textra.Font;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Component;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GameScreen implements Screen {
    private static GameScreen instance;
    private static GameClient gameClient;
    private final Font font;
    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Label pingLabel;
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

    public GameScreen(RogueFantasy game, AssetManager manager, GameClient gameClient) {
        this.game = game;
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

        stage = new Stage(new ExtendViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons

        fpsLabel = new Label("fps: 144", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.left);
        fpsLabel.setX(12);

        pingLabel = new Label("ping: 333", skin, "fontMedium", Color.WHITE);
        pingLabel.setAlignment(Align.left);
        pingLabel.setX(fpsLabel.getX()+fpsLabel.getWidth()+22);

        stage.addActor(fpsLabel);
        stage.addActor(pingLabel);

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
    }

    @Override
    public void show() {

    }

    private void moveCharacter(float deltaTime) {
        Vector2 normalizedVec = new Vector2(movement.x, movement.y);
        normalizedVec = normalizedVec.nor();
        GameRegister.MoveCharacter msg = new GameRegister.MoveCharacter();
        msg.x = normalizedVec.x * deltaTime * 250f;
        msg.y = normalizedVec.y * deltaTime * 250f;
        msg.hasEndPoint = movement.hasEndPoint;
        msg.xEnd = movement.xEnd; msg.yEnd = movement.yEnd;
        msg.deltaTime = deltaTime;

        if(GameRegister.serverAuthoritative)
            gameClient.moveCharacter(msg);
        else {
            gameClient.getClientCharacter().update(gameClient.getClientCharacter().x + msg.x,
                    gameClient.getClientCharacter().y + msg.y);
        }

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

        ScreenUtils.clear(0.2f, 0.6f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());
        pingLabel.setText("ping: " + gameClient.getAvgLatency());

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
            moveCharacter(delta); // moves character if there is velocity or endpoint

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

    }

}
