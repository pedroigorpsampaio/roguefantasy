package com.mygdx.game.ui;

import static com.mygdx.game.ui.CommonUI.ENABLE_TARGET_UI;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.entity.EntityController;
import com.mygdx.game.entity.Projectile;
import com.mygdx.game.entity.WorldMap;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.util.Common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Random;

/**
 * Implements the game screen
 */
public class GameScreen implements Screen, PropertyChangeListener {
    private static GameScreen instance;
    private static GameClient gameClient;
    private final WorldMap world;
    private final OrthographicCamera uiCam;
    private final int resW, resH;
    private final EntityController entityController;
    private final GestureDetector gestureDetector;
    private final InputMultiplexer inputMultiplexer;
    private final Touchpad touchPad;
    private final TextureAtlas uiAtlas;
    private Image closestEntityImg, targetImg;
    private Button selectTargetBtn, nextTargetBtn, lastTargetBtn;
    private FitViewport uiViewport;
    private Font font;
    private Timer updateTimer;
    private Texture mapTexture;
    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Label pingLabel;
    private Label ramLabel;
    private Label bCallsLabel;
    private Label mouseOnLabel;
    private Image uiBg, healthBar, healthBarBg;
    private TypingLabel nameLabel, percentLabel;
    private Stack targetUiStack;
    private static AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private RegisterWindow registerWindow;
    private I18NBundle langBundle;
    private Texture bgTexture;
    private Image bg;
    private SpriteBatch batch;
    public static OrthographicCamera camera;
    static final int WORLD_WIDTH = 1000;
    static final int WORLD_HEIGHT = 1000;
    private float rotationSpeed;
    private boolean playerIsTarget = false;
    private Vector3 vec3;
    private Vector2 touchPos;
    private Vector2 clientPos;
    private Vector2 deltaVec;
    private Joystick joystick;
    private GameRegister.MoveCharacter movement; // player movement message (for tests rn)
    // time counters
    private float timeForUpdate = 0f;
    ShapeRenderer uiDebug;
    public static ShapeRenderer shapeDebug;
    private float aimZoom;
    public static boolean onStageActor = false;
    private static boolean isInputHappening = false;
    public static Vector3 unprojectedMouse = new Vector3();
    private Vector3 screenMouse = new Vector3();
    private Vector2 joystickDir = new Vector2(); // the current joystick direction (for android)
    private Texture testTexure;
    // array containing the active projectiles.
    private static final Array<Projectile> projectiles = new Array<Projectile>();
    public static void addProjectile(Projectile projectile) {projectiles.add(projectile);}
    // projectile pool.
    private static final Pool<Projectile> projectilePool = new Pool<Projectile>() {
        @Override
        protected Projectile newObject() {
            return new Projectile();
        }
    };
    public static Pool<Projectile> getProjectilePool() {return projectilePool;}

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
        entityController = EntityController.getInstance();

        batch = new SpriteBatch();
        resW = Gdx.graphics.getWidth();
        resH = Gdx.graphics.getHeight();

        uiDebug = new ShapeRenderer();
        shapeDebug = new ShapeRenderer();
        joystick = new Joystick(5, 20, 50);

        testTexure =new Texture(Gdx.files.internal("sfx/PrismaWand_1.png"));

        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        camera = new OrthographicCamera(32, 32 * (resH / resW));
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
        camera.zoom = 0.4f;
        aimZoom = camera.zoom;
        uiCam = new OrthographicCamera(resW, resH);
        uiCam.position.set(resW / 2f, resH / 2f, 0);
        //viewport = new FitViewport(800, 480, camera);

        camera.update();
        uiCam.update();

        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);

        // loads world map
        TiledMap map = manager.get("world/novaterra.tmx");
        world = WorldMap.getInstance();
        world.init(map, batch, camera);
        
        // ui atlas
        uiAtlas = manager.get("ui/packed_textures/ui.atlas");

        /**
         * Load entity ui assets
         */
        uiBg = new Image(uiAtlas.findRegion("panel"));
        healthBarBg = new Image(uiAtlas.findRegion("healthBarBg"));
        healthBar = new Image(uiAtlas.findRegion("healthBar"));
        nameLabel = new TypingLabel("Kane", skin);
        nameLabel.skipToTheEnd();
        percentLabel = new TypingLabel("100%", skin);
        percentLabel.skipToTheEnd();

        TextureAtlas.AtlasRegion region = uiAtlas.findRegion("RectangleBox_96x96");
        TextureRegionDrawable up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        selectTargetBtn = new Button(up, up.tint(Color.GRAY));
        // ui buttons
        region = uiAtlas.findRegion("tile041");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        nextTargetBtn = new Button(up, up.tint(Color.GRAY));

        region = uiAtlas.findRegion("tile040");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        lastTargetBtn = new Button(up, up.tint(Color.GRAY));

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // get and play music
        Random rand = new Random();
        bgm = manager.get("bgm/maps/bgm_"+rand.nextInt(3)+".mp3", Music.class);
        bgm.setLooping(true);
        bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f));
        bgm.play();

        stage = new Stage(new StretchViewport(1280, 720));
        gestureDetector = new GestureDetector(20, 0.4f,
                                    0.2f, Integer.MAX_VALUE, new GameGestureListener());
        inputMultiplexer = new InputMultiplexer(gestureDetector, stage);

        Gdx.input.setInputProcessor(inputMultiplexer);
        //Gdx.input.setInputProcessor(stage);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons

        fpsLabel = new Label("fps: 1441", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.left);
        fpsLabel.setX(12);

        pingLabel = new Label("ping: 3334", skin, "fontMedium", Color.WHITE);
        pingLabel.setAlignment(Align.left);
        pingLabel.setX(fpsLabel.getX()+fpsLabel.getWidth()+22);

        ramLabel = new Label("RAM: 00MB"+Common.getRamUsage(), skin, "fontMedium", Color.WHITE);
        ramLabel.setAlignment(Align.left);
        ramLabel.setX(pingLabel.getX()+pingLabel.getWidth()+22);

        bCallsLabel = new Label("batch calls: 110", skin, "fontMedium", Color.WHITE);
        bCallsLabel.setAlignment(Align.left);
        bCallsLabel.setX(ramLabel.getX()+ramLabel.getWidth()+22);

        mouseOnLabel = new Label("Entity: none", skin, "fontMedium", Color.WHITE);
        mouseOnLabel.setAlignment(Align.left);
        mouseOnLabel.setX(bCallsLabel.getX()+bCallsLabel.getWidth()+22);

        targetUiStack = new Stack();
        float x = stage.getWidth()/2f;
        float y = stage.getHeight() - CommonUI.TARGET_UI_HEIGHT/2f;
        float w =  CommonUI.TARGET_UI_WIDTH;
        float h =  CommonUI.TARGET_UI_HEIGHT;

        /**ui entity target background**/
        uiBg.setBounds(x - w/2f, y - h/2, w, h);
        targetImg = new Image();
        targetImg.setBounds(uiBg.getX(), uiBg.getY(), w/5f, h/1.2f);
        //.setOrigin(targetStack.getWidth()/2f,targetStack.getHeight()/2f);


        Stack targetStack = new Stack();
        //selectTargetBtn.setX(Gdx.graphics.getWidth() - selectTargetBtn.getWidth() * 2f);
        //selectTargetBtn.setY(selectTargetBtn.getHeight() * 0.1f);

        closestEntityImg = new Image();
        //closestEntityImg.setX(Gdx.graphics.getWidth() - selectTargetBtn.getWidth() * 2f);
        //closestEntityImg.setY(selectTargetBtn.getHeight() * 0.1f);
        closestEntityImg.setTouchable(Touchable.disabled);
        closestEntityImg.setScale(targetStack.getScaleX()*0.8f,targetStack.getScaleY()*0.8f);
        closestEntityImg.setOrigin(targetStack.getWidth()/2f,targetStack.getHeight()/2f);


        targetStack.add(selectTargetBtn);
        targetStack.add(closestEntityImg);
        targetStack.setTransform(true);
        targetStack.setX(stage.getWidth() - targetStack.getWidth()*1.5f);
        targetStack.setY(targetStack.getHeight() * 0.1f);
        targetStack.setScale(0.8f);

        lastTargetBtn.setTransform(true);
        lastTargetBtn.setScale(4f, 4f);
        lastTargetBtn.setX(targetStack.getX() - lastTargetBtn.getWidth()*4.5f);
        lastTargetBtn.setY(targetStack.getHeight() * 0.25f);

        nextTargetBtn.setTransform(true);
        nextTargetBtn.setScale(4f, 4f);
        nextTargetBtn.setX(targetStack.getX() + targetStack.getWidth()*targetStack.getScaleX() + nextTargetBtn.getWidth());
        nextTargetBtn.setY(targetStack.getHeight() * 0.25f);

        touchPad = new Touchpad(1f, skin);

        stage.addActor(fpsLabel);
        stage.addActor(pingLabel);
        stage.addActor(ramLabel);
        stage.addActor(bCallsLabel);
        stage.addActor(mouseOnLabel);
        /**
         * Android UI only
         */
        if(Gdx.app.getType() == Application.ApplicationType.Android) {
            stage.addActor(lastTargetBtn);
            stage.addActor(nextTargetBtn);
            stage.addActor(targetStack);
        }

        // updates camera zoom based on current device screen
//        float uiZoomFactor = 720f / Gdx.graphics.getHeight();
//        if(Gdx.app.getType() == Application.ApplicationType.Android) // zoom menu if user is using android device
//            ((OrthographicCamera)stage.getCamera()).zoom = uiZoomFactor; // zoom in  window menu

        // character current movement
        movement = new GameRegister.MoveCharacter();

        stage.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if( Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !onStageActor) return false; // ignore if there is clicking/touching

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
                /**
                 * NON-MOVEMENT INTERACTIONS
                 */
                if(keycode == Input.Keys.ESCAPE)
                    GameClient.getInstance().getClientCharacter().setTarget(null);

                if(keycode == Input.Keys.TAB && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {// tab targeting
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), false);
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                }

                if(keycode == Input.Keys.TAB && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {// tab targeting in reverse
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), true);
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                }

                if(keycode == Input.Keys.SPACE) {// closest interactive entity targeting
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity();
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                }

                if(keycode == Input.Keys.L && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                    gameClient.logoff();

                if(keycode == Input.Keys.D && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    CommonUI.enableDebugTex = !CommonUI.enableDebugTex;
                }

                /**
                 * MOVEMENT INTERACTIONS
                 */
                if( Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !onStageActor) return false; // ignore if there is clicking/touching in world

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
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    // if its first touch and there is a interactive entity, select it
                    if(Gdx.input.justTouched() && button == 1 && !onStageActor) { // Only acts on world if it did not hit any UI actor)
                        //if(WorldMap.hoverEntity != null)
//                        if(GameClient.getInstance().getClientCharacter().getTarget() != null
//                         && GameClient.getInstance().getClientCharacter().getTarget() == WorldMap.hoverEntity)
//                            GameClient.getInstance().getClientCharacter().getTarget().takeDamage(); // for debug atm

                        GameClient.getInstance().getClientCharacter().setTarget(WorldMap.hoverEntity);
                    }
                }
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if( Gdx.app.getType() == Application.ApplicationType.Android) {
                    if(pointer == 1) { // joystick is active and a second touch happened
                        vec3 = new Vector3(Gdx.input.getX(1),Gdx.input.getY(1),0); //  use second touch for interactions instead, first touch is movement
                        screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(1), Gdx.input.getY(1), 0f));
                        unprojectedMouse = camera.unproject(vec3);
                    }
                    // if its first touch and there is a interactive entity, select it
                    if(!onStageActor) { // Only acts on world if it did not hit any UI actor)
                        //if(WorldMap.hoverEntity != null)
                        if(pointer == 1)
                            GameClient.getInstance().getClientCharacter().setTarget(WorldMap.hoverEntity);
                        else if(!joystick.isActive())
                            GameClient.getInstance().getClientCharacter().setTarget(WorldMap.hoverEntity);
                    }
                    if (!Gdx.input.isTouched(0)) {
                        joystick.setActive(false);
                        joystickDir.x = 0;
                        joystickDir.y = 0;
                        joystick.reset();
                    }
                }
            }
        });

        Entity.stage = stage;

        // if its not listening to game server responses, start listening to it
        if(!gameClient.isListening(this))
            gameClient.addListener(this);

        new GameController(); // binds game controller interactions

        // starts update timer that control user inputs and server communication
        updateTimer=new Timer();
        updateTimer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                update();
            }
        },0,GameRegister.clientTickrate());
    }

    /**
     * Shows entity target UI that is shown on entity hover/targeting
     */
    private void showEntityTargetUI(Entity e) {
        if(e == null || !e.isTargetAble) return; // nothing to render if entity is null or not selectable

        float w =  CommonUI.TARGET_UI_WIDTH;
        float h =  CommonUI.TARGET_UI_HEIGHT;

        ///**ui background**/
        //uiBg.setBounds(x - w/2f, y - h/2, w, h);
        //uiBg.draw(batch, bgAlpha);

        /**target sprite**/
        TextureRegion eFrame = e.getCurrentFrame();
        targetImg.setDrawable(new TextureRegionDrawable(eFrame));
        float width = w / 3f;
        float height = h / 1.0f;
        switch(e.type) {
            case TREE:
                width = w / 4f;height = h / 1.5f;
                break;
            case CREATURE:
                width = w / 3f;height = h / 1.0f;
                break;
            case CHARACTER:
                width = w / 5f;height = h / 1.4f;
                break;
            default:
                break;
        }
        targetImg.setBounds(uiBg.getX()+uiBg.getWidth()/5.5f-width/2f, uiBg.getY()+uiBg.getHeight()/2f-height/2f, width, height);

        /**health bar**/
        // background
        w *= 0.5857f;
        h *= 0.32f;
        healthBarBg.setBounds(uiBg.getX()+uiBg.getWidth()/2f - w/3.17f, uiBg.getY()+h*0.49f, w, h);

        // health bar
        float percent = e.health/e.maxHealth;
        healthBar.setColor(Color.RED.cpy().lerp(Color.OLIVE, percent*1.25f + 0.15f));

        w *= 0.938f;
        h *= 0.525f;
        healthBar.setBounds(healthBarBg.getX()+w*0.032f, healthBarBg.getY()+h*0.45f, w*percent, h);

        /**tag name**/
        nameLabel.setText("{SIZE=75%}{COLOR=LIGHT_GRAY}"+e.entityName+"[%]{ENDCOLOR}");
        nameLabel.setAlignment(Align.center);
        nameLabel.setBounds(healthBarBg.getX()+healthBarBg.getWidth()/2f-nameLabel.getWidth()/2f,
                            healthBarBg.getY() + healthBarBg.getHeight()*1.6f,
                nameLabel.getWidth(), nameLabel.getHeight());

        /**percent text**/
        int percentInt = (int) (percent * 100f);
        StringBuilder sb = new StringBuilder();
        sb.append(percentInt);
        sb.append("%");

        percentLabel.setText("{SIZE=69%}{COLOR=LIGHT_GRAY}"+sb+"[%]{ENDCOLOR}");
        percentLabel.setAlignment(Align.center);
        percentLabel.setBounds(healthBarBg.getX() + healthBarBg.getWidth()/2f -percentLabel.getWidth()/2f,
                                healthBarBg.getY()+healthBarBg.getHeight()/3.17f,
                                    percentLabel.getWidth(), percentLabel.getHeight());

//        TypingLabel tmpLabel = new TypingLabel(String.valueOf(sb), font);
//        float percentScale = tagScale*0.66f;
//        float percentW = tmpLabel.getWidth()*percentScale;
//        font.scale(percentScale, percentScale);
//        font.drawText(batch, sb, healthBarBg.getX() + healthBarBg.getWidth()/2f - percentW/2f,
//                healthBarBg.getY()+healthBarBg.getHeight()/3.17f);
//        font.scaleTo(font.originalCellWidth, font.originalCellHeight);

        stage.addActor(uiBg);
        stage.addActor(targetImg);
        stage.addActor(healthBarBg);
        stage.addActor(healthBar);
        stage.addActor(nameLabel);
        stage.addActor(percentLabel);
    }

    public void hideEntityTargetUI() {
        uiBg.remove();
        targetImg.remove();
        healthBarBg.remove();
        healthBar.remove();
        nameLabel.remove();
        percentLabel.remove();
    }

    @Override
    public void show() {

    }

    /**
     * Apply movement to the character based on current client-server strategies enabled
     */
    private void moveCharacter() {
        Entity.Character player = gameClient.getClientCharacter();

        // if player is unmovable return
        if(player.isUnmovable()) return;
        if(player.isTeleporting) {
            return;
        }

        GameRegister.MoveCharacter msg = new GameRegister.MoveCharacter();
        msg.x = movement.x;
        msg.y = movement.y/2f; // isometric world
        msg.hasEndPoint = movement.hasEndPoint;
        msg.xEnd = movement.xEnd; msg.yEnd = movement.yEnd;
        msg.requestId = gameClient.getRequestId(GameRegister.MoveCharacter.class);

        // checks if movement is possible before sending it to server
        if(!player.isMovePossible(msg)) {
            // search for another direction to slide when colliding
            Vector2 newMove = player.findSlide(msg);

            msg.x = newMove.x;
            msg.y = newMove.y;
            if(msg.hasEndPoint) msg.hasEndPoint = false; // new dir has been calculated, send it as wasd move to server

            // test new move once again
            if(!player.isMovePossible(msg) || newMove.len() == 0) // if failed again, give up this movement
                return;

            Vector2 tInitialPos = WorldMap.toIsoTileCoordinates(new Vector2(player.position.x, player.position.y));
            Vector2 tFuturePos = WorldMap.toIsoTileCoordinates(new Vector2(player.position.x, player.position.y).add(newMove));
            if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                return;
        }

        //System.out.println("msg: " + msg.x + " / " + msg.y);

        // sends to server the raw movement to be calculated by the authoritative server
        gameClient.moveCharacter(msg);

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

        // update whether mouse is on actor of stage
        screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) // gets hit with actor on mouse position
            onStageActor = isActorHit(screenMouse.x, screenMouse.y);

        // correct velocity
        if(!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.UP) &&
                !Gdx.input.isKeyPressed(Input.Keys.S) && !Gdx.input.isKeyPressed(Input.Keys.DOWN) && !Gdx.input.isTouched(0))
            movement.y = 0;
        if(!Gdx.input.isKeyPressed(Input.Keys.A) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isTouched(0))
            movement.x = 0;

        // current mouse position in world
        if(Gdx.app.getType() == Application.ApplicationType.Desktop) { // if on android gesture listener will deal with interactions
            vec3 = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            unprojectedMouse = camera.unproject(vec3);
        }

        // check if there is touch velocity
        if(Gdx.input.isTouched(0)){
            if (Gdx.app.getType() == Application.ApplicationType.Android) { // use joystick movement if its on android
                movement.x = joystickDir.x;
                movement.y = joystickDir.y;
            } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {    // if its on pc move accordingly
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !onStageActor) { // only left mouse button walks and if not on ui stage actor
                    touchPos = new Vector2(unprojectedMouse.x - gameClient.getClientCharacter().spriteW / 2f,  // compensate to use center of char sprite as anchor
                            unprojectedMouse.y - gameClient.getClientCharacter().spriteH / 2f);
                    movement.xEnd = touchPos.x;
                    movement.yEnd = touchPos.y;
                    movement.hasEndPoint = true;
                }
            }
        } else {
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) { // if no click is made, there is no end point goal of movement for pc
                movement.xEnd = 0;
                movement.yEnd = 0;
                movement.hasEndPoint = false;
            }
        }

        if(movement.x != 0 || movement.y != 0 || movement.hasEndPoint) {
            moveCharacter(); // moves character if there is velocity or endpoint
            isInputHappening = true;
        } else {
            isInputHappening = false;
        }
    }

    private boolean isActorHit(float x, float y) {
        if(stage.hit(x, y, false) == null)
            return false;
        else
            return true;
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
        uiCam.zoom = camera.zoom;

        // updates position based on client player
        Entity.Character player = gameClient.getClientCharacter();
        if(player != null && player.assetsLoaded) {
            if(playerIsTarget) {
                float playerX = player.interPos.x + player.spriteW/2f;
                float playerY = player.interPos.y + player.spriteH/2f;

                Vector3 target = new Vector3(playerX,playerY,0);
                final float speed=Gdx.graphics.getDeltaTime()*9.0f,ispeed=1.0f-speed;
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

    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, false);

    @Override
    public void render(float delta) {
        if(gameClient.getClientCharacter() == null) return;

        // updates cursor if on desktop
        if(Gdx.app.getType() == Application.ApplicationType.Desktop)
            updateCursor();

        updateCamera(); // updates camera
        updateProjectiles(); // update alive projectiles 
        
        batch.setProjectionMatrix(camera.combined); // sets camera for projection

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // wait until player is camera target and first state is received to start rendering
        if(!playerIsTarget && !gameClient.firstStateProcessed) return;

        if(startDelay)
            timeElapsed+=Gdx.graphics.getDeltaTime();

        if(timeElapsed < 0.17f) { // TODO: ACTUALLY DO A LOAD SCREEN AND W8 FIRST STATE TO BE FULLY LOADED
            return;
        }
        startDelay = false;

        while(GameClient.getInstance().isPredictingRecon.get()); // don't render world while reconciliating pos of client


        // remember SpriteBatch's current functions
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();

        batch.totalRenderCalls = 0;

        batch.begin();

        world.render(); // render world

        // draw entities using ordered list (if there is any)
        if(entityController.entities.size() > 0)
            entityController.renderEntities(batch);

        // render projectiles
//        for (Projectile projectile : projectiles) {
//            projectile.render(batch);
//        }

        // render player target info if there is a target
        Entity target = gameClient.getClientCharacter().getTarget();
        if(target != null) {
            target.renderUI(batch);
            if(ENABLE_TARGET_UI)
                showEntityTargetUI(target);
        }

        batch.setProjectionMatrix(camera.combined);
        // always draw client simplified ui
        gameClient.getClientCharacter().renderUI(batch);

        // renders hover entity information
        if((Gdx.app.getType() == Application.ApplicationType.Desktop &&
                WorldMap.hoverEntity != null)) {
            if(target == null
                || target.uId !=
            WorldMap.hoverEntity.uId) { // only renders if target is not the one being hovered, as info is already being rendered by being a target
                WorldMap.hoverEntity.renderUI(batch);
                if(ENABLE_TARGET_UI)
                    showEntityTargetUI(WorldMap.hoverEntity);
            }
        }

        if(ENABLE_TARGET_UI && target == null && WorldMap.hoverEntity == null)
            hideEntityTargetUI(); // makes sure to hide target ui when no target nor hover exists

        batch.end();
        batch.setBlendFunction(srcFunc, dstFunc);

        if(WorldMap.portals != null && CommonUI.enableDebugTex) {
            shapeDebug.setProjectionMatrix(camera.combined);
            shapeDebug.begin(ShapeRenderer.ShapeType.Line);
            shapeDebug.setColor(Color.BLUE);
            //shapeDebug.circle(unprojectedMouse.x, unprojectedMouse.y, 1);
            for(Rectangle portal : WorldMap.portals) {
                shapeDebug.rect(portal.x, portal.y, portal.width, portal.height);
            }
            shapeDebug.end();
        }

        // line between player and target debug
        if(gameClient.getClientCharacter() != null && gameClient.getClientCharacter().getTarget() != null  && CommonUI.enableDebugTex) {
            shapeDebug.setProjectionMatrix(camera.combined);
            shapeDebug.begin(ShapeRenderer.ShapeType.Line);
            shapeDebug.setColor(Color.BLUE);
            shapeDebug.line(gameClient.getClientCharacter().getEntityCenter(), gameClient.getClientCharacter().getTarget().getEntityCenter());
            shapeDebug.end();
        }

        // draws ui
//        batch.setProjectionMatrix(uiCam.combined);
//        batch.begin();
//
//
//        batch.end();

        int calls = batch.totalRenderCalls;

        // updates and renders joystick
        if (joystick.isActive() && Gdx.app.getType() == Application.ApplicationType.Android) {
            screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            joystick.updateValues(screenMouse.x, screenMouse.y);
            joystickDir = new Vector2(joystick.getX(), joystick.getY()).nor();
            joystickDir = new Vector2(MathUtils.round(joystickDir.x), MathUtils.round(joystickDir.y));
//            viewport.getCamera().getPosition().add(
//                    joystick.getStickDistPercentageX() * delta * IMV * 10,
//                    joystick.getStickDistPercentageY() * delta * IMV * 10, 0f);

            batch.begin();
            batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
            float screenRadius = joystick.getRadius()*3f;
            Color c = new Color(batch.getColor());
            batch.setColor(0.5f, 0.6f, 0.42f, 0.52f); //set color and transparency
            touchPad.getStyle().background.draw(batch, joystick.getInitialX() - screenRadius/2f,
                    joystick.getInitialY() - screenRadius/2f, screenRadius, screenRadius);
            batch.setColor(0.5f, 0.5f, 0.44f, 0.77f); //set color
            touchPad.getStyle().knob.draw(batch, joystick.getInitialX() - screenRadius/4f
                            + (joystick.getStickDistPercentageX() * screenRadius/2f),
                    joystick.getInitialY() - screenRadius/4f
                            + (joystick.getStickDistPercentageY() * screenRadius/2f),
                    screenRadius/2f, screenRadius/2f);
            batch.setColor(c); // reset batch color
            batch.end();
//            uiDebug.begin(ShapeRenderer.ShapeType.Line);
//            uiDebug.setProjectionMatrix(stage.getViewport().getCamera().combined);
//            uiDebug.setColor(Color.YELLOW);
//            float screenRadius = joystick.getRadius()*2f;
//            uiDebug.circle(joystick.getInitialX(), joystick.getInitialY(), screenRadius, 50);
//            uiDebug.end();
//            uiDebug.begin(ShapeRenderer.ShapeType.Filled);
//            uiDebug.setColor(Color.WHITE);
//            uiDebug.circle(joystick.getInitialX() + (joystick.getStickDistPercentageX() * screenRadius),
//                    joystick.getInitialY() + (joystick.getStickDistPercentageY() * screenRadius), screenRadius/3, 50);
////            uiDebug.setColor(Color.CYAN);
////            uiDebug.circle(joystick.getInitialX() + joystick.getX(), joystick.getInitialY() + joystick.getY(), screenRadius/5, 50);
//            uiDebug.end();
            //System.out.println(joystickDir);
        }

        //Log or render to screen
        //System.out.println(calls);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());
        pingLabel.setText("ping: " + gameClient.getAvgLatency());
        ramLabel.setText("RAM: " + Common.getRamUsage() + " MB");
        bCallsLabel.setText("batch calls: " + calls);
        if(WorldMap.hoverEntity == null)
            mouseOnLabel.setText("Entity: none");
        else {
            if(WorldMap.hoverEntity.type == Entity.Type.TREE) {
                Entity.Tree tree = GameClient.getInstance().getTree(WorldMap.hoverEntity.uId);
                if (tree != null) //{
                    mouseOnLabel.setText("Entity: " + GameClient.getInstance().getTree(WorldMap.hoverEntity.uId).spawnId + " (" + WorldMap.hoverEntity.type.toString() + ")");
//            } else {
//                mouseOnLabel.setText("Entity: none");
//            }
            } else {
                mouseOnLabel.setText("Entity: " + WorldMap.hoverEntity.uId + " (" + WorldMap.hoverEntity.type.toString() + ")");
            }
        }

        // interactive button target drawing - only for android ui target
        if(Gdx.app.getType() == Application.ApplicationType.Android) {
            Entity uiTargetEntity = EntityController.getInstance().getNextTargetEntity();
            if (uiTargetEntity != null) {
                TextureRegion closestEntityFrame = uiTargetEntity.getCurrentFrame();
                if (closestEntityFrame != null) { // draw the closest interactive entity in UI (for android)
                    //selectTargetBtn.add
                    closestEntityImg.setDrawable(new TextureRegionDrawable(closestEntityFrame));
                }
            } else {
                closestEntityImg.setDrawable(null);
            }
        }

        // draws stage
        stage.act(Math.min(delta, 1 / 60f));
        stage.draw();
    }

    /**
     * Update current alive projectiles
     */
    private void updateProjectiles() {
        Iterator<Projectile> iter = projectiles.iterator();
        while (iter.hasNext()) {
            Projectile projectile = iter.next();
            projectile.update(Gdx.graphics.getDeltaTime());

            // if raindrop is not on screen anymore removes it and free pool
            if (projectile.alive == false) {
                iter.remove();
                projectilePool.free(projectile);
            }
            // if raindrop hits bucket, count it and remove it/free pool
//            if (raindrop.overlaps(bucket)) {
//                dropsGathered++;
//                dropSound.play();
//                if (raindrop.alive == true) {
//                    iter.remove();
//                    raindropsPool.free(raindrop);
//                }
//            }
        }
    }

    /**
     * Updates cursor based on current hovered entity
     */
    private void updateCursor() {
        if(WorldMap.hoverEntity == null) {
            Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Default"));
            return;
        }

        switch(WorldMap.hoverEntity.type) {
            case TREE:
                Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Chop Green"));
                break;
            case PORTAL:
                Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Magic Use Green"));
                break;
            case CREATURE:
            case CHARACTER:
                Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Attack Green"));
                break;
            default:
                Gdx.graphics.setCursor(CommonUI.cursorBank.get("Cursor Default"));
                break;
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = 32f;
        camera.viewportHeight = 32f * height/width;
        camera.update();

//        uiCam.viewportWidth = resW;
//        uiCam.viewportHeight = resW * height/width;
//        uiCam.position.set(width / 2f, height/ 2f, 0);
//        uiCam.update();

        uiCam.viewportWidth = width;
        uiCam.viewportHeight = height;
        uiCam.position.set(width / 2, height / 2, 0);
        uiCam.update();

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
        //stage.dispose();
        bgm.dispose();
        world.dispose();
        testTexure.dispose();
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
                dispose();
            }
        });
    }

    public static boolean isInputHappening() {
        return isInputHappening;
    }

    /**
     * Game screen's gesture listener
     */
    public class GameGestureListener implements GestureDetector.GestureListener {
        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            if (Gdx.app.getType() == Application.ApplicationType.Android) { // gets if an actor is hit on touchdown
                screenMouse = stage.getViewport().getCamera().unproject(new Vector3(x, y, 0f));
                onStageActor = isActorHit(screenMouse.x, screenMouse.y);

                if(!onStageActor) {
                    vec3 = new Vector3(x, y,0); //  use second touch for interactions instead, first touch is movement
                    screenMouse = stage.getViewport().getCamera().unproject(new Vector3(x, y, 0f));
                    unprojectedMouse = camera.unproject(vec3);
                }
            }
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            if(onStageActor) return false; // Only acts with joystick if it did not hit any UI actor

            if (Gdx.app.getType() == Application.ApplicationType.Android) { // in android, check if there is second touch
                if(!joystick.isActive()) { // if joystick is not active, use first touch for interaction
                    vec3 = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                    unprojectedMouse = camera.unproject(vec3);
                }
                else if(Gdx.input.isTouched(1)) { // if joystick is active and there is second touch
                    vec3 = new Vector3(Gdx.input.getX(1),Gdx.input.getY(1),0); //  use second touch for interactions instead, first touch is movement
                    unprojectedMouse = camera.unproject(vec3);
                }
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            if(onStageActor) return false; // Only acts with joystick if it did not hit any UI actor

            if(!Gdx.input.isTouched(1) && Gdx.app.getType() == Application.ApplicationType.Android) {
                screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
                joystick.setInitialX(screenMouse.x);
                joystick.setInitialY(screenMouse.y);
                joystick.setActive(true);
            }
            return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            if(onStageActor) return false; // Only acts with joystick if it did not hit any UI actor

            if(!Gdx.input.isTouched(1) && Gdx.app.getType() == Application.ApplicationType.Android
                && !joystick.isActive()) { // on pan we want only to activate it on first pan, to not change initial position on each callback
                screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
                joystick.setInitialX(screenMouse.x);
                joystick.setInitialY(screenMouse.y);
                joystick.setActive(true);
            }
            return false;
        }

        @Override
        public boolean panStop(float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean zoom (float originalDistance, float currentDistance){
            return false;
        }

        @Override
        public boolean pinch (Vector2 initialFirstPointer, Vector2 initialSecondPointer, Vector2 firstPointer, Vector2 secondPointer){
            return false;
        }
        @Override
        public void pinchStop () {
        }
    }

    /**
     * Controls UI interactions of game screen
     */
    class GameController {
        // constructor adds listeners to the actors
        public GameController() {
            /**
             * ANDROID ONLY UI CONTROLS
             */
            selectTargetBtn.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity();
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    event.stop();
                    event.handle();
                    event.cancel();
                }
            });
            lastTargetBtn.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), true);
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    event.stop();
                    event.handle();
                    event.cancel();
                }
            });
            nextTargetBtn.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), false);
                    GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    event.stop();
                    event.handle();
                    event.cancel();
                }
            });

            /**
             * ALL UI CONTROLS
             */
        }
    }

}