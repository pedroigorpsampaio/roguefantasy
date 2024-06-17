package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.tommyettinger.textra.Font;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.entity.WorldMap;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.util.Common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements the game screen
 */
public class GameScreen implements Screen, PropertyChangeListener {
    private static GameScreen instance;
    private static GameClient gameClient;
    private final WorldMap world;
    private Font font;
    private Timer updateTimer;
    private Sprite mapSprite;
    private Texture mapTexture;
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
    private OrthographicCamera camera;
    static final int WORLD_WIDTH = 1000;
    static final int WORLD_HEIGHT = 1000;
    private float rotationSpeed;
    private boolean playerIsTarget = false;
    private Vector3 vec3;
    private Vector2 touchPos;
    private Vector2 clientPos;
    private Vector2 deltaVec;
    private GameRegister.MoveCharacter movement; // player movement message (for tests rn)
    // time counters
    private float timeForUpdate = 0f;
    private float aimZoom;
    private static boolean isInputHappening = false;

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
        Entity.assetManager = manager;

        batch = new SpriteBatch();
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        camera = new OrthographicCamera(32, 32 * (h / w));
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
        camera.zoom = 1.4f;
        aimZoom = camera.zoom;

        camera.update();

        // loads world map (TODO: load it in load screen)
        TiledMap map = manager.get("world/testmap.tmx");
        world = new WorldMap(map, batch, camera);

        mapTexture = new Texture(Gdx.files.internal("sc_map.png"));
        mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        mapSprite = new Sprite(mapTexture);
        mapSprite.setPosition(0, 0);
        mapSprite.setSize(WORLD_WIDTH, WORLD_HEIGHT);

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

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                aimZoom = camera.zoom + amountY * 0.1f;
                //world.zoom(amountY);
                //camera.zoom += amountY;
                return super.scrolled(event, x, y, amountX, amountY);
            }
        });

        Entity.stage = stage;

        // if its not listening to game server responses, start listening to it
        if(!gameClient.isListening(this))
            gameClient.addListener(this);

        // starts update timer that control user inputs and server communication
        updateTimer=new Timer();
        updateTimer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                update();
            }
        },0,GameRegister.clientTickrate());
    }

    @Override
    public void show() {

    }

    /**
     * Apply movement to the character based on current client-server strategies enabled
     */
    private void moveCharacter() {
        GameRegister.MoveCharacter msg = new GameRegister.MoveCharacter();
        msg.x = movement.x;
        msg.y = movement.y/2f; // isometric world
        msg.hasEndPoint = movement.hasEndPoint;
        msg.xEnd = movement.xEnd; msg.yEnd = movement.yEnd;
        msg.requestId = gameClient.getRequestId(GameRegister.MoveCharacter.class);

        // sends to server the raw movement to be calculated by the authoritative server
        gameClient.moveCharacter(msg);
        Entity.Character player = gameClient.getClientCharacter();
        // if client prediction is enabled, try to predict the movement calculating it locally
        if(Common.clientPrediction)
            player.predictMovement(msg);
        // calculates player direction
        Vector2 dir = new Vector2(0,0);
        if(msg.hasEndPoint) {
            dir = new Vector2(msg.xEnd, msg.yEnd).sub(player.position).nor();
        } else {
            dir = new Vector2(msg.x, msg.y*2f).nor(); //multiplies again to calculate direction correctly
        }
        player.direction = Entity.Direction.getDirection(MathUtils.round(dir.x), MathUtils.round(dir.y));
    }

    /**
     * Should update game using fixed step defined by GameRegister.clientTickrate()
     */
    public void update() {
        if(gameClient.getClientCharacter() == null) return; // character not loaded yet!

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
            vec3 = camera.unproject(vec3); // unproject screen touch
            touchPos = new Vector2(vec3.x - gameClient.getClientCharacter().spriteW/2f,  // compensate to use center of char sprite as anchor
                                    vec3.y - gameClient.getClientCharacter().spriteH/2f);
            movement.xEnd = touchPos.x; movement.yEnd = touchPos.y;
            movement.hasEndPoint = true;
        } else { // if no click/touch is made, there is no end point goal of movement
            movement.xEnd = 0; movement.yEnd = 0;
            movement.hasEndPoint = false;
        }

        if(movement.x != 0 || movement.y != 0 || movement.hasEndPoint) {
            moveCharacter(); // moves character if there is velocity or endpoint
            isInputHappening = true;
        } else {
            isInputHappening = false;
        }
    }

    private void updateCamera() {
        // updates zoom
        if(camera.zoom != aimZoom) {
            Vector2 tmp = new Vector2(camera.zoom, 0);
            float alpha = 5f * Gdx.graphics.getDeltaTime();
            tmp.lerp(new Vector2(aimZoom, 0), alpha);
            camera.zoom = tmp.x;
            if(Math.abs(aimZoom - camera.zoom) < 0.001f)
                camera.zoom = aimZoom;
        }
        // clamp zoom values
        camera.zoom = MathUtils.clamp(camera.zoom, 0.1f, WORLD_HEIGHT/camera.viewportWidth);

        // updates position based on client player
        Entity.Character player = gameClient.getClientCharacter();
        if(player != null && player.assetsLoaded) {
            if(playerIsTarget) {
                float playerX = player.interPos.x + player.spriteW/2f;
                float playerY = player.interPos.y + player.spriteH/2f;

                Vector3 target = new Vector3(playerX,playerY,0);
                final float speed=Gdx.graphics.getDeltaTime()*player.speed,ispeed=1.0f-speed;
                Vector3 cameraPosition = camera.position;
                cameraPosition.scl(ispeed);
                target.scl(speed);
                cameraPosition.add(target);
                camera.position.set(cameraPosition);
            } else {
                camera.position.x = player.getCenter().x;
                camera.position.y = player.getCenter().y;
                playerIsTarget = true; // sets player as camera target
            }
        }

        // clamp values for camera //TODO: CORRECTLY CLAMP ISO WORLD!
//        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
//        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;
//        camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, WORLD_WIDTH - effectiveViewportWidth / 2f);
//        camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, WORLD_HEIGHT - effectiveViewportHeight / 2f);
        // updates camera
        camera.update();
    }

    float timeElapsed = 0f;
    boolean startDelay = true;
    @Override
    public void render(float delta) {
        if(gameClient.getClientCharacter() == null) return;

        updateCamera(); // updates camera
        batch.setProjectionMatrix(camera.combined); // sets camera for projection

        //ScreenUtils.clear(0.2f, 0.6f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // wait until player is camera target and first state is received to start rendering
        if(!playerIsTarget && !gameClient.firstStateProcessed) return;

        if(startDelay)
            timeElapsed+=Gdx.graphics.getDeltaTime();

        if(timeElapsed < 0.67f) { // TODO: ACTUALLY DO A LOAD SCREEN AND W8 FIRST STATE TO BE FULLY LOADED
            return;
        }
        startDelay = false;

        while(GameClient.getInstance().isPredictingRecon.get()); // don't render world while reconciliating pos of client

        batch.totalRenderCalls = 0;

        batch.begin();

        world.render(); // render world

        //mapSprite.draw(batch);

        // draw creatures
        Map<Long, Entity.Creature> creatures = gameClient.getCreatures();
        synchronized (creatures) {
            Iterator<Map.Entry<Long, Entity.Creature>> iterator = creatures.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Entity.Creature> entry = iterator.next();
                Entity.Creature creature = entry.getValue();
                creature.render(batch);
            }
        }
        // draw characters
        Map<Integer, Entity.Character> characters = gameClient.getOnlineCharacters();
        synchronized (characters) {
            Iterator<Map.Entry<Integer, Entity.Character>> iterator = characters.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Entity.Character> entry = iterator.next();
                Entity.Character character = entry.getValue();
                character.render(batch);
            }
        }

        batch.end();

        int calls = batch.totalRenderCalls;

        //Log or render to screen
        //System.out.println(calls);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());
        pingLabel.setText("ping: " + gameClient.getAvgLatency());
        ramLabel.setText("RAM: " + Common.getRamUsage() + " MB");

        // draws stage
        stage.act(Math.min(delta, 1 / 60f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = 32f;
        camera.viewportHeight = 32f * height/width;
        camera.update();
        //world.resize(width, height);
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
        mapSprite.getTexture().dispose();
        stage.dispose();
        world.dispose();
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
            if(propertyChangeEvent.getPropertyName().equals("lostConnection")) {
                RogueFantasy.getInstance().setScreen(new LoadScreen("menu"));
                // stops update timer
                updateTimer.stop();
            }
        });
    }

    public static boolean isInputHappening() {
        return isInputHappening;
    }
}