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
    private Vector2 velocity; // player velocity (for tests rn)
    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
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
    private boolean isCharacterLoaded = false;
    private boolean serverAuthoritative = true;
    private boolean lagSimulation = false;
    private boolean clientPrediction = false;
    private boolean serverReconciliation = false;
    private boolean entityInterpolation = false;
    private boolean lagCompensation = false;

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

        stage = new Stage(new StretchViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons

        fpsLabel = new Label("fps:", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.center);
        fpsLabel.setX(32);

        stage.addActor(fpsLabel);

        // updates camera zoom based on current device screen
//        float uiZoomFactor = 720f / Gdx.graphics.getHeight();
//        if(Gdx.app.getType() == Application.ApplicationType.Android) // zoom menu if user is using android device
//            ((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in  window menu

        velocity = new Vector2(0,0);
        stage.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.W && !Gdx.input.isKeyPressed(Input.Keys.UP)) velocity.y += 1;
                else if (keycode == Input.Keys.UP && !Gdx.input.isKeyPressed(Input.Keys.W)) velocity.y += 1;

                if (keycode == Input.Keys.S && !Gdx.input.isKeyPressed(Input.Keys.DOWN)) velocity.y += -1;
                else if (keycode == Input.Keys.DOWN && !Gdx.input.isKeyPressed(Input.Keys.S)) velocity.y += -1;

                if (keycode == Input.Keys.A && !Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocity.x += -1;
                else if (keycode == Input.Keys.LEFT && !Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x += -1;

                if (keycode == Input.Keys.D && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocity.x += 1;
                else if (keycode == Input.Keys.RIGHT && !Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x += 1;

                if(velocity.x > 1) velocity.x = 1; if(velocity.y > 1) velocity.y = 1;
                if(velocity.x < -1) velocity.x = -1; if(velocity.y < -1) velocity.y = -1;
                return super.keyDown(event, keycode);
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if (keycode == Input.Keys.W && !Gdx.input.isKeyPressed(Input.Keys.UP)) velocity.y -= 1;
                else if (keycode == Input.Keys.UP && !Gdx.input.isKeyPressed(Input.Keys.W)) velocity.y -= 1;

                if (keycode == Input.Keys.S && !Gdx.input.isKeyPressed(Input.Keys.DOWN)) velocity.y -= -1;
                else if (keycode == Input.Keys.DOWN && !Gdx.input.isKeyPressed(Input.Keys.S)) velocity.y -= -1;

                if (keycode == Input.Keys.A && !Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocity.x -= -1;
                else if (keycode == Input.Keys.LEFT && !Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x -= -1;

                if (keycode == Input.Keys.D && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocity.x -= 1;
                else if (keycode == Input.Keys.RIGHT && !Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x -= 1;

                if(velocity.x > 1) velocity.x = 1; if(velocity.y > 1) velocity.y = 1;
                if(velocity.x < -1) velocity.x = -1; if(velocity.y < -1) velocity.y = -1;
                return super.keyUp(event, keycode);
            }
        });

        Component.stage = stage;
    }

    @Override
    public void show() {

    }

    private void moveCharacter(float deltaTime) {
        GameRegister.MoveCharacter msg = new GameRegister.MoveCharacter();
        Vector2 normalizedVec = new Vector2(velocity.x, velocity.y);
        normalizedVec = normalizedVec.nor();
        msg.x = normalizedVec.x * deltaTime * 250f;
        msg.y = normalizedVec.y * deltaTime * 250f;
        if(serverAuthoritative)
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
            HashMap<Integer, Component.Character> onlineChars = gameClient.getOnlineCharacters();
            synchronized(onlineChars) { // checks if client character is loaded
                Iterator<Map.Entry<Integer, Component.Character>> i = onlineChars.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<Integer, Component.Character> entry = i.next();
                    Component.Character c = entry.getValue();
                    if(gameClient.getClientCharacter() != null)
                        isCharacterLoaded = true;
                }
            }
        }

        // waits for the client character to be loaded before game start
        if(!isCharacterLoaded)
            return;

        ScreenUtils.clear(0.2f, 0.6f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());

        // correct velocity
        if(!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.UP) &&
                !Gdx.input.isKeyPressed(Input.Keys.S) && !Gdx.input.isKeyPressed(Input.Keys.DOWN))
            velocity.y = 0;
        if(!Gdx.input.isKeyPressed(Input.Keys.A) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            velocity.x = 0;

        // check if there is touch velocity
        if(Gdx.input.isTouched(0)){
            Component.Character clientChar = gameClient.getClientCharacter();
            Vector3 vec=new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
            vec = stage.getCamera().unproject(vec); // unproject screen touch
            Vector2 touchPos = new Vector2(vec.x, vec.y);
            Vector2 clientPos = clientChar.getCenter();
            Vector2 deltaVec = new Vector2(touchPos).sub(clientPos);
            if(touchPos.dst(clientPos) > 10f) // only moves if distance is long enough for it
                velocity = deltaVec;
        }

        if(!velocity.isZero())
            moveCharacter(Gdx.graphics.getDeltaTime()); // moves character if there is velocity

        // draws stage
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
        //});
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
