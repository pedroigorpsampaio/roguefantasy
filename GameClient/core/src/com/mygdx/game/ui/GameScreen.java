package com.mygdx.game.ui;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import static com.mygdx.game.ui.CommonUI.ENABLE_TARGET_UI;
import static com.mygdx.game.ui.CommonUI.FADE_CLIENT_TAG_ON_ALL_FLOATING_TEXT;
import static com.mygdx.game.ui.CommonUI.MENU_ICONS_PADDING;
import static com.mygdx.game.ui.CommonUI.removeWindowWithAction;

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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
import com.mygdx.game.network.ChatClient;
import com.mygdx.game.network.ChatRegister;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Jukebox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implements the game screen
 */
public class GameScreen implements Screen, PropertyChangeListener {
    public static boolean lockWorldRender = true;
    private static GameScreen instance;
    private static GameClient gameClient;
    private static ChatClient chatClient;
    private final WorldMap world;
    private final OrthographicCamera uiCam;
    private final int resW, resH;
    private final EntityController entityController;
    private final GestureDetector gestureDetector;
    private final InputMultiplexer inputMultiplexer;
    private final Touchpad touchPad;
    private final TextureAtlas uiAtlas;
    private static TextButton respawnBtn;
    private static TypingLabel deathMsgLabel, mapNameLabel, zoneNameLabel;
    private static Stack targetStack;
    public static ChatWindow chatWindow;
    private static Table chatTabs; // tabs of chat window
    public static OpenChannelWindow openChannelWindow; // open channel window
    private Image closestEntityImg, targetImg;
    private Button selectTargetBtn;
    private static Button lastTargetBtn;
    private static Button nextTargetBtn;
    static Button optionsBtn;
    static Button contactsBtn;
    private FitViewport uiViewport;
    private Font font;
    private Timer updateTimer;
    private Texture mapTexture;
    private Skin skin;
    private static Stage stage;
    private Label fpsLabel;
    private Label pingLabel;
    private Label ramLabel;
    private Label bCallsLabel;
    private Label mouseOnLabel;
    private static InfoToast infoToast, pmToast, lookToast; // for informing player of useful info
    private Image uiBg, healthBar, healthBarBg;
    private TypingLabel nameLabel, percentLabel;
    private Stack targetUiStack;
    private static AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private OptionWindow optionWindow;
    private ContactWindow contactWindow;
    private static ContextWindow contextWindow;
    private I18NBundle langBundle;
    private Texture bgTexture;
    private Image bg;
    private SpriteBatch batch;
    public static OrthographicCamera camera;
    public static float ORIGINAL_ZOOM = 0.45f;
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
    public static boolean onStageActor = false, onStageActorDown = false;
    private static boolean isInputHappening = false;
    public static Vector3 unprojectedMouse = new Vector3();
    private Vector3 screenMouse = new Vector3();
    private Vector2 joystickDir = new Vector2(); // the current joystick direction (for android)
    private Texture testTexure;
    public static int chatOffsetY = 0; // chat y offset from bottom
    private boolean openKeyboard = false;
    private Actor onActor = null; // actor that mouse is currently on (null if none)
    private boolean draggingActor = false;
    private boolean firstTouchOnActor = false;


    /** poolable objects **/

    // array containing the active projectiles.
    private static final Array<Projectile> projectiles = new Array<Projectile>();
    private boolean isPanning = false;

    public static void addProjectile(Projectile projectile) {projectiles.add(projectile);}
    // projectile pool.
    private static final Pool<Projectile> projectilePool = new Pool<Projectile>() {
        @Override
        protected Projectile newObject() {
            return new Projectile();
        }
    };
    public static Pool<Projectile> getProjectilePool() {return projectilePool;}

    // floatingtext pool
    private static final Pool<FloatingText> floatingTextPool = new Pool<FloatingText>() {
        @Override
        protected FloatingText newObject() {
            return new FloatingText();
        }
    };

    private static final Queue<FloatingText> floatingTexts = new ConcurrentLinkedQueue<FloatingText>();
    public static void addFloatingText(FloatingText floatingText) {floatingTexts.add(floatingText);}
    public static Pool<FloatingText> getFloatingTextPool() {return floatingTextPool;}

    /**
     * A lil class for information label with a lang key for its content in different languages
     */
    public static class InfoToast {
        public Object arg0;
        Label label;
        String langKey;
    }

    /**
     * Prepares the screen/stage of the game
     *
     * @param manager    the asset manager containing loaded assets
     * @param gameClient the reference to the game client responsible for the communication with
     *                   the game server
     * @param chatClient
     */
    public GameScreen(AssetManager manager, GameClient gameClient, ChatClient chatClient) {
        this.game = RogueFantasy.getInstance();
        this.manager = manager;
        this.gameClient = gameClient;
        this.chatClient = chatClient;
        Entity.assetManager = manager;
        Jukebox.manager = manager;
        entityController = EntityController.getInstance();
        instance = this;

        // gets language bundle from asset manager
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);

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
        camera.zoom = ORIGINAL_ZOOM;
        aimZoom = camera.zoom;
        uiCam = new OrthographicCamera(resW, resH);
        uiCam.position.set(resW / 2f, resH / 2f, 0);
        //viewport = new FitViewport(800, 480, camera);

        camera.update();
        uiCam.update();

        // loads world map
        TiledMap map = manager.get("world/novaterra.tmx");
        world = WorldMap.getInstance();
        world.init(map, batch, camera);

        // ui atlas
        uiAtlas = manager.get("ui/packed_textures/ui.atlas");

        /**
         * Load ui assets
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
        region = uiAtlas.findRegion("forwardIcon");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        nextTargetBtn = new Button(up, up.tint(Color.GRAY));

        region = uiAtlas.findRegion("backwardIcon");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        lastTargetBtn = new Button(up, up.tint(Color.GRAY));

        region = uiAtlas.findRegion("optionsIcon");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        optionsBtn = new Button(up, up.tint(Color.GRAY));

        region = uiAtlas.findRegion("contactsIcon");
        up = new TextureRegionDrawable(new TextureRegion(new Sprite(region)));
        contactsBtn = new Button(up, up.tint(Color.GRAY));

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // get and play music
        Random rand = new Random();
        bgm = manager.get("bgm/maps/bgm_"+rand.nextInt(3)+".mp3", Music.class);
        bgm.setLooping(true);
        bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f));
        bgm.play();

        // viewport size based on device
        if(Gdx.app.getType() == Application.ApplicationType.Desktop)
            stage = new Stage(new StretchViewport(1920*1.2f, 1080*1.2f));
        else
            stage = new Stage(new StretchViewport(1920*Common.ANDROID_VIEWPORT_SCALE, 1080*Common.ANDROID_VIEWPORT_SCALE));

        gestureDetector = new GestureDetector(20, 0.4f,
                                    0.2f, Integer.MAX_VALUE, new GameGestureListener());
        inputMultiplexer = new InputMultiplexer(gestureDetector, stage);

        Gdx.input.setInputProcessor(inputMultiplexer);
        //Gdx.input.setInputProcessor(stage);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons


        /**
         * Create windows
         */
        chatWindow = new ChatWindow(game, stage, this, manager, "", skin, "newWindowStyle");
        buildChatWindow();

        optionWindow = new OptionWindow(game, stage, this, manager, " "+langBundle.format("option"),
                skin, "newWindowStyle");

        contactWindow = new ContactWindow(game, stage, this, manager, " "+langBundle.format("contacts"),
                skin, "newWindowStyle");

        contextWindow = new ContextWindow(game, stage, this, manager, "", skin, "newWindowStyle");

        /**
         * Menu buttons
         */
        optionsBtn.setTransform(true);
        optionsBtn.setScale(4f, 4f);
        optionsBtn.setX(stage.getWidth() - optionsBtn.getWidth()*optionsBtn.getScaleX()-MENU_ICONS_PADDING);
        optionsBtn.setY(stage.getHeight() - optionsBtn.getHeight()*optionsBtn.getScaleY()-MENU_ICONS_PADDING);

        contactsBtn.setTransform(true);
        contactsBtn.setScale(4f, 4f);
        contactsBtn.setX(optionsBtn.getX() - contactsBtn.getWidth()*contactsBtn.getScaleX()-MENU_ICONS_PADDING);
        contactsBtn.setY(optionsBtn.getY());

        /**
         * info toast labels
         */
        infoToast = new InfoToast();
        infoToast.label = new Label("infoLabel", skin, "fontMedium", Color.WHITE);
        infoToast.label.setAlignment(Align.center);
        infoToast.label.setX(stage.getWidth()/2f - infoToast.label.getWidth()/2f);
        infoToast.label.setY(chatWindow.getY()+chatWindow.getHeight()+chatWindow.getTabHeight()+infoToast.label.getHeight()/3f);
        infoToast.label.setVisible(false);
        // pm toast
        pmToast = new InfoToast();
        pmToast.label = new Label("infoLabel", skin, "largeLabelStyle");
        pmToast.label.setColor(ChatWindow.PRIVATE_CHAT_MESSAGE_COLOR);
        pmToast.label.setAlignment(Align.center);
        pmToast.label.setVisible(false);
        pmToast.label.setFontScale(1.2f);
        float widthPm = stage.getWidth()/2.5f;
        pmToast.label.setWidth(widthPm);
        pmToast.label.setWrap(true);
        pmToast.label.layout();
        pmToast.label.setX(stage.getWidth()/2f -  pmToast.label.getWidth()/2f);
        pmToast.label.setY(stage.getHeight()/1.2f - pmToast.label.getHeight()/2f);
        // look toast
        lookToast = new InfoToast();
        lookToast.label = new Label("infoLabel", skin, "largeLabelStyle");
        lookToast.label.setColor(ChatWindow.LOOK_MESSAGE_COLOR);
        lookToast.label.setAlignment(Align.center);
        lookToast.label.setVisible(false);
        lookToast.label.setFontScale(1.2f);
        lookToast.label.setWidth(widthPm);
        lookToast.label.setWrap(true);
        lookToast.label.layout();
        lookToast.label.setX(stage.getWidth()/2f -  lookToast.label.getWidth()/2f);
        lookToast.label.setY(stage.getHeight()/2f - lookToast.label.getHeight()/2f);

        /**
         * debug
         */
        fpsLabel = new Label("fps: 1441", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.left);
        fpsLabel.setX(12);
        fpsLabel.setY(stage.getHeight()-fpsLabel.getHeight());

        pingLabel = new Label("ping: 3334", skin, "fontMedium", Color.WHITE);
        pingLabel.setAlignment(Align.left);
        pingLabel.setX(fpsLabel.getX()+fpsLabel.getWidth()+22);
        pingLabel.setY(stage.getHeight()-fpsLabel.getHeight());

        ramLabel = new Label("RAM: 00MB"+Common.getRamUsage(), skin, "fontMedium", Color.WHITE);
        ramLabel.setAlignment(Align.left);
        ramLabel.setX(pingLabel.getX()+pingLabel.getWidth()+22);
        ramLabel.setY(stage.getHeight()-fpsLabel.getHeight());

        bCallsLabel = new Label("batch calls: 110", skin, "fontMedium", Color.WHITE);
        bCallsLabel.setAlignment(Align.left);
        bCallsLabel.setX(ramLabel.getX()+ramLabel.getWidth()+22);
        bCallsLabel.setY(stage.getHeight()-fpsLabel.getHeight());

        mouseOnLabel = new Label("Entity: none", skin, "fontMedium", Color.WHITE);
        mouseOnLabel.setAlignment(Align.left);
        mouseOnLabel.setX(bCallsLabel.getX()+bCallsLabel.getWidth()+22);
        mouseOnLabel.setY(stage.getHeight()-fpsLabel.getHeight());

        /**
         * Android UI for target interaction
         */

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


        targetStack = new Stack();
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

        /**
         * Map and zone Label
         */
        mapNameLabel = new TypingLabel( "{SIZE=250%}{COLOR=WHITE}Nova Terra{ENDCOLOR}{STYLE=%}", font);
        zoneNameLabel = new TypingLabel( "{SIZE=170%}{COLOR=LIGHTGRAY}Initium{ENDCOLOR}{STYLE=%}", font);
        mapNameLabel.setPosition(stage.getWidth()/2f - mapNameLabel.getWidth()/2f, stage.getHeight()/1.45f - mapNameLabel.getHeight()/2f);
        zoneNameLabel.setPosition(stage.getWidth()/2f - zoneNameLabel.getWidth()/2f, stage.getHeight()/1.75f - zoneNameLabel.getHeight()/2f);

        /**
         * Death UI
         */
        respawnBtn = new TextButton(langBundle.format("respawn"), skin);
        deathMsgLabel = new TypingLabel( "{SIZE=220%}{COLOR=LIGHTGRAY}[+skull and crossbones] "+langBundle.format("deathMessage")+" [+skull and crossbones]{ENDCOLOR}{STYLE=%}", font);

        respawnBtn.setPosition(stage.getWidth()/2f - respawnBtn.getWidth()/2f, stage.getHeight()/2.75f - respawnBtn.getHeight()/2f);
        deathMsgLabel.setPosition(stage.getWidth()/2f - deathMsgLabel.getWidth()/2f, stage.getHeight()/1.45f - deathMsgLabel.getHeight()/2f);

//        stage.addActor(infoToast.label);
//        stage.addActor(pmToast.label);
//        stage.addActor(lookToast.label);
        stage.addActor(fpsLabel);
        stage.addActor(pingLabel);
        stage.addActor(ramLabel);
        stage.addActor(bCallsLabel);
        stage.addActor(mouseOnLabel);
        stage.addActor(optionsBtn);
        stage.addActor(contactsBtn);
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

        stage.addCaptureListener(new ClickListener(Input.Buttons.RIGHT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if ( Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    // context menu is on stage, remove if touch up happens outside of it
//                    if (contextWindow.getStage() != null && !contextWindow.isPointOn(screenMouse.x, screenMouse.y))
//                        hideContextMenu();

                    if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {  // show context menu if left control is pressed if hover entity not null
                        if(WorldMap.hoverEntity != null)
                            showContextMenu(WorldMap.hoverEntity.buildContextMenu());
                    }
                }
            }
        });
        stage.addCaptureListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //if ( Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    // context menu is on stage, remove if touch up happens outside of it
//                if (contextWindow.getStage() != null && !contextWindow.isPointOn(screenMouse.x, screenMouse.y)) {
//                    hideContextMenu();
//                }
                //}
                if ( Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {  // show look info directly on ctrl+left click in desktop
                        if (WorldMap.hoverEntity != null)
                            GameScreen.getInstance().showLookMessage(WorldMap.hoverEntity.generateLookInfo());
                    }
                }
            }
        });
        stage.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if ( Gdx.app.getType() == Application.ApplicationType.Desktop && contextWindow.getStage()!=null) return false; //context menu is opened
                if( Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !onStageActor) return false; // ignore if there is clicking/touching
                if(chatWindow.hasFocus()) return false; // dont walk if chat window has focus

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
                if(keycode == Input.Keys.ESCAPE) {
                    if(chatWindow.hasFocus())
                        chatWindow.clearMessageField(false);
                    else if(contextWindow.getStage()!=null)
                        hideContextMenu(); //context menu is opened
                    else if(GameClient.getInstance().getClientCharacter().getTarget() != null)
                        GameClient.getInstance().getClientCharacter().setTarget(null);
                }

                if ( Gdx.app.getType() == Application.ApplicationType.Desktop && contextWindow.getStage()!=null) return false; //context menu is opened

                if(keycode == Input.Keys.ENTER) {
                    if(chatWindow.hasFocus())
                        chatWindow.sendMessage();
                    else
                        chatWindow.setFocus();
                }

                if (keycode == Input.Keys.TAB && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) { // chat tab change
                    if(!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
                        chatWindow.navigateTab(false);
                    else
                        chatWindow.navigateTab(true);
                }

                if(keycode == Input.Keys.TAB && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {// tab targeting
                    if(!chatWindow.hasFocus()) {
                        Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), false);
                        if(nextTarget == null || nextTarget.uId != GameClient.getInstance().getClientUid()) // makes sure to not attack itself
                            GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    }
                }

                if(keycode == Input.Keys.TAB && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)  && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {// tab targeting in reverse
                    if(!chatWindow.hasFocus()) {
                        Entity nextTarget = EntityController.getInstance().getNextTargetEntity(GameClient.getInstance().getClientCharacter().getTarget(), true);
                        if(nextTarget == null || nextTarget.uId != GameClient.getInstance().getClientUid()) // makes sure to not attack itself
                            GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    }
                }

                if(keycode == Input.Keys.SPACE) {// closest interactive entity targeting
                    if(!chatWindow.hasFocus()) {
                        Entity nextTarget = EntityController.getInstance().getNextTargetEntity();
                        if(nextTarget == null || nextTarget.uId != GameClient.getInstance().getClientUid()) // makes sure to not attack itself
                            GameClient.getInstance().getClientCharacter().setTarget(nextTarget);
                    }
                }

                /**
                 * SHORTCUTS
                 */

                if(keycode == Input.Keys.L && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                    logoff();

                if(keycode == Input.Keys.D && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    CommonUI.enableDebugTex = !CommonUI.enableDebugTex;
                }

                if(keycode == Input.Keys.O && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    toggleWindow(optionWindow, false, true);
                }

                if(keycode == Input.Keys.F && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    toggleWindow(contactWindow, false, false);
                }

                // lag simulation controls
                if(keycode == Input.Keys.COMMA && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    updatePingSimulation(-10, false);
                }

                if(keycode == Input.Keys.PERIOD && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    updatePingSimulation(10, false);
                }

                // lag simulation controls server
                if(keycode == Input.Keys.COMMA && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    updatePingSimulation(-10, true);
                }

                if(keycode == Input.Keys.PERIOD && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    updatePingSimulation(10, true);
                }

                /**
                 * MOVEMENT INTERACTIONS
                 */
                if( Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !onStageActor) return false; // ignore if there is clicking/touching in world
                if(chatWindow.hasFocus()) return false; // dont walk if chat window has focus

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
                if(onStageActor) {
                    return super.scrolled(event, x, y, amountX, amountY); // do not zoom world if mouse is on stage actor
                }
                // change world zoom
                aimZoom = camera.zoom + amountY * 0.064f;
                //world.zoom(amountY);
                //camera.zoom += amountY;
                return super.scrolled(event, x, y, amountX, amountY);
            }

        });
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
                    onActor = isActorHit(screenMouse.x, screenMouse.y);
                    firstTouchOnActor = onActor == null ? false : true;

                    // context menu is on stage, remove if touch up happens outside of it
                    if (contextWindow.getStage()!=null) {
                        if(!contextWindow.isPointOn(screenMouse.x, screenMouse.y))
                            hideContextMenu();

                        return false;
                    }

                    if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) return false; // if left control is selected ignore and return

                    // if its first touch and there is a interactive entity, interact with it
                    if(Gdx.input.justTouched() && button == 1 && !onStageActor) { // Only acts on world if it did not hit any UI actor)
                        if(WorldMap.hoverEntity == null || WorldMap.hoverEntity.uId != GameClient.getInstance().getClientCharacter().uId) // if null or not player, select as new target
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
                    if(!onStageActor) { // Only acts on world if it did not hit any UI actor)
                        //if(WorldMap.hoverEntity != null)
                        if(pointer == 1 || !joystick.isActive()) { // if first finger and no joystick or second finger with joystick active
                            if(WorldMap.hoverEntity != null) { // show context menu on tap (long press will attack now)
                                showContextMenu(WorldMap.hoverEntity.buildContextMenu());
                            } else { // if tap on no entity, stop attacking (if its attacking)
                                GameClient.getInstance().getClientCharacter().setTarget(null);
                            }
                        }
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
     * This method is called when client character is loaded from game-server
     * It is a safe method to perform initial operations that depends on client character info
     * and is also used for managing locks related to logging in
     */
    public void onClientCharacterLoaded() {
        // sets render and animation flags
        lockWorldRender = false;
        respawnAnimOn = true;
        // create default chats for chat window after client character is loaded
        chatWindow.createDefaultChannels();
    }

    /**
     * Shows information to player through info toast label
     * that appears for a set amount of time above chat window
     *
     * @param langKey the language key to find desired information in the correct language
     */
    public void showInfo(String langKey) {
        String info = langBundle.get(langKey);
        info = info.replaceAll("[\\n]", " "); // if it has \n embedded in it, switch for a space
        infoToast.label.setText(info);
        infoToast.label.setVisible(true);
        infoToast.langKey = langKey;

        stage.addActor(infoToast.label);

        float lifeTime = 1f + info.length() * 1/24f;

        if(showInfoTask.isScheduled())
            showInfoTask.cancel();

        Timer.schedule(showInfoTask, lifeTime);
    }

    public void showInfo(String langKey, Object arg) {
        String info = "";

        if(arg instanceof Integer)
            info = langBundle.format(langKey, (int)arg);
        else if(arg instanceof String)
            info = langBundle.format(langKey, (String)arg);

        if(info.equals("")) {
            System.out.println("Unknown argument type for bundle retrieval");
            return;
        }

        info = info.replaceAll("[\\n]", " "); // if it has \n embedded in it, switch for a space
        infoToast.label.setText(info);
        infoToast.label.setVisible(true);
        infoToast.langKey = langKey;
        infoToast.arg0 = arg;

        float lifeTime = 1f + info.length() * 1/24f;

        if(showInfoTask.isScheduled())
            showInfoTask.cancel();

        Timer.schedule(showInfoTask, lifeTime);
    }

    private Timer.Task showInfoTask = new Timer.Task() {
        @Override
        public void run() {
            hideToast(infoToast);
        }
    };

    /**
     * Shows private messages received
     *
     * @param message the message to be shown
     */
    public void showPrivateMessage(String message) {
        pmToast.label.setText(message);
        pmToast.label.setVisible(true);
        stage.addActor(pmToast.label);

        float lifeTime = 3f + message.length() * 1/24f;

        if(hidePmTask.isScheduled())
            hidePmTask.cancel();

        Timer.schedule(hidePmTask, lifeTime);
    }

    /**
     * Show look message containing information about
     * the examined entity by client player
     * @param info  the information about the entity examined
     */
    public void showLookMessage(String info) {
        lookToast.label.setText(langBundle.format("lookMessage", info));
        lookToast.label.setVisible(true);
        stage.addActor(lookToast.label);

        float lifeTime = 3f + info.length() * 1/24f;

        if(hideLookToast.isScheduled())
            hideLookToast.cancel();

        Timer.schedule(hideLookToast, lifeTime);
    }


    private Timer.Task hidePmTask = new Timer.Task() {
        @Override
        public void run() {
            hideToast(pmToast);
        }
    };

    private Timer.Task hideLookToast = new Timer.Task() {
        @Override
        public void run() {
            hideToast(lookToast);
        }
    };

    public static GameScreen getInstance() {
        return instance;
    }

    public static void hideContextMenu() {
        //contextWindow.remove();
        removeWindowWithAction(contextWindow, fadeOut(0.1f));
    }

    /**
     * Shows context menu on mouse position
     *
     * @param options   the table containing the label options of the context menu
     */
    public void showContextMenu(Table options) {
        hideContextMenu();

        if(!gameClient.getClientCharacter().isAlive) // do not show context menu if player is dead
            return;

        Color c = contextWindow.getColor();
        contextWindow.setColor(c.r, c.g, c.b, 1f);
        contextWindow.loadTable(options);
        contextWindow.setBounds(screenMouse.x, screenMouse.y, contextWindow.getWidth(), contextWindow.getHeight());
        contextWindow.setScale(0);
        contextWindow.addAction(scaleTo(1.0f,1.0f,0.05f));
        stage.addActor(contextWindow);
    }

    public ChatWindow getChatWindow() {
        return chatWindow;
    }

    public static Stage getStage() {
        return stage;
    }

    private void buildChatWindow() {
        /**
         * Chat window
         */
        chatWindow.build();
        //chatWindow.setScale(0.7f, 0.7f);
        //chatWindow.setWidth(stage.getWidth()/2f);
        chatWindow.setX(stage.getWidth() /2f - chatWindow.getWidth() * chatWindow.getScaleX() /2f);
        chatWindow.setY(chatOffsetY);
        chatWindow.updateHitBox();
        stage.addActor(chatWindow);

        /**
         * Chat tab buttons
         */
        chatTabs = chatWindow.getChannelTabs();
        chatTabs.setPosition(chatWindow.getX(), chatWindow.getY()+chatWindow.getHeight());
        chatTabs.align(Align.bottomLeft);
        stage.addActor(chatTabs);

        openChannelWindow = chatWindow.getOpenChannelWindow();
    }

    public static void hideChatWindow() {
        chatWindow.remove();
        chatTabs.remove();
        if(openChannelWindow.getStage()!=null)
            openChannelWindow.remove();
    }

    public static void showChatWindow() {
        stage.addActor(chatWindow);
        stage.addActor(chatTabs);
    }

    public static void hideMenuButtons() {
        if(optionsBtn.getStage() != null)
            optionsBtn.remove();
        if(contactsBtn.getStage() != null)
            contactsBtn.remove();
    }

    public static void showMenuButtons() {
        if(optionsBtn.getStage() == null)
            stage.addActor(optionsBtn);
        if(contactsBtn.getStage() == null)
            stage.addActor(contactsBtn);
    }

    public static void showDeathUI() {
        hideAndroidTargetInteraction();
        hideChatWindow();
        hideContextMenu();
        hideMenuButtons();
//        deathMsgLabel.setText(deathMsgLabel.storedText);
        if(deathMsgLabel.hasEnded())
            deathMsgLabel.restart(); // restart if it has ended
        stage.addActor(deathMsgLabel);
        
        hideToast(pmToast);
        // let the respawn btn be shown by render method after animation
    }

    /**
     * Just hides toast
     * @param toast the toast to hide
     */
    private static void hideToast(InfoToast toast) {
        toast.label.setText("");
        toast.label.setVisible(false);
        toast.langKey = null;
        toast.label.remove();
    }

    public static void hideDeathUI() {
        respawnBtn.remove();
        deathMsgLabel.remove();
        showAndroidTargetInteraction();
        showChatWindow();
        showMenuButtons();
    }

    public static void showAndroidTargetInteraction() {
        if(Gdx.app.getType() == Application.ApplicationType.Android) {
            stage.addActor(lastTargetBtn);
            stage.addActor(nextTargetBtn);
            stage.addActor(targetStack);
        }
    }

    public static void hideAndroidTargetInteraction() {
        if(Gdx.app.getType() == Application.ApplicationType.Android) {
            lastTargetBtn.remove();
            nextTargetBtn.remove();
            targetStack.remove();
        }
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
        if(respawnAnimOn) return; // don't move while respawn anim is on

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

        Entity.Character clientChar = gameClient.getClientCharacter();

        // if its on android, change chat y position offset if keyboard is showing
        if(Gdx.app.getType() == Application.ApplicationType.Android) {
            //Gdx.app.log("inputtest", String.valueOf(RogueFantasy.isKeyboardShowing()));
            if(RogueFantasy.isKeyboardShowing()) {
                chatOffsetY = RogueFantasy.getKeyboardHeight();
                chatWindow.setY(chatOffsetY);
                chatTabs.setY(chatWindow.getY()+chatWindow.getHeight());
                infoToast.label.setY(chatWindow.getY()+chatWindow.getHeight()+chatWindow.getTabHeight()+infoToast.label.getHeight()/3f);
            } else {
                chatOffsetY = 1;
                chatWindow.setY(chatOffsetY);
                chatTabs.setY(chatWindow.getY()+chatWindow.getHeight());
                infoToast.label.setY(chatWindow.getY()+chatWindow.getHeight()+chatWindow.getTabHeight()+infoToast.label.getHeight()/3f);
            }

            if(RogueFantasy.isKeyboardShowing()) {
                if(!openKeyboard) { // keyboard just opened
                    chatOffsetY = RogueFantasy.getKeyboardHeight();
                    openKeyboard = true;
                    openChannelWindow.softKeyboardOpened();
                }
            } else {
                //chatOffsetY = 0;
                if(openKeyboard) { // keyboard just closed
                    openKeyboard = false;
                    openChannelWindow.softKeyboardClosed();
                }
            }
        }

        // update whether mouse is on actor of stage
        screenMouse = stage.getViewport().getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) { // gets hit with actor on mouse position
            onActor = isActorHit(screenMouse.x, screenMouse.y);
            onStageActor = onActor == null ? false : true;

            // update scroll focus if on chat window
            if(chatWindow.isPointOn(screenMouse.x, screenMouse.y)) { // on chat window
                chatWindow.setScrollFocus();
            } else {
                chatWindow.removeScrollFocus();
            }

        }

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
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && ((!onStageActor && !onStageActorDown && !draggingActor) || isPanning) &&
                contextWindow.getStage()==null) { // only left mouse button walks and if not on ui stage actor or context menu is opened
                    if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) { // if ctrl is pressed, just change direction
                        touchPos = new Vector2(unprojectedMouse.x - clientChar.spriteW / 2f,  // compensate to use center of char sprite as anchor
                                unprojectedMouse.y - clientChar.spriteH / 2f);
                        Vector2 charPos = new Vector2(clientChar.position.x, clientChar.position.y);
                        Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                        Vector2 dir = deltaVec.nor();
                        clientChar.direction = Entity.Direction.getDirection(Math.round(dir.x), Math.round(dir.y));
                    } else {
                        touchPos = new Vector2(unprojectedMouse.x - gameClient.getClientCharacter().spriteW / 2f,  // compensate to use center of char sprite as anchor
                                unprojectedMouse.y - gameClient.getClientCharacter().spriteH / 2f);
                        movement.xEnd = touchPos.x;
                        movement.yEnd = touchPos.y;
                        movement.hasEndPoint = true;
                    }
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

    private Actor isActorHit(float x, float y) {
//        if(stage.hit(x, y, false) == null)
//            return false;
//        else
//            return true;
        return stage.hit(x, y, false);
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
                float playerX = player.getEntityCenter().x;
                float playerY = player.getEntityCenter().y;

                Vector3 target = new Vector3(playerX,playerY,0);
                final float speed=Gdx.graphics.getDeltaTime()*9.0f,ispeed=1.0f-speed;
                Vector3 cameraPosition = camera.position;
                cameraPosition.scl(ispeed);
                target.scl(speed);
                cameraPosition.add(target);
                camera.position.set(cameraPosition);

                if(player.teleportOutAnim) {
                    camera.position.x = player.getEntityCenter().x;
                    camera.position.y = player.getEntityCenter().y;
                }
            } else {
                camera.position.x = player.getCenter().x + player.spriteW/12f;
                camera.position.y = player.getCenter().y + player.spriteH/12f;
                if(player.finalDrawPosSet) {
                    camera.position.x = player.getEntityCenter().x;
                    camera.position.y = player.getEntityCenter().y;
                    playerIsTarget = true; // sets player as camera target
                }
            }

//            if(gameClient.getClientCharacter().isRespawning) {
//                camera.position.x = player.getEntityCenter().x;
//                camera.position.y = player.getEntityCenter().y;
//            }
        }

        // clamp values for camera //TODO: CORRECTLY CLAMP ISO WORLD!
//        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
//        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;
//        camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, WORLD_WIDTH - effectiveViewportWidth / 2f);
//        camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, WORLD_HEIGHT - effectiveViewportHeight / 2f);
        // updates camera
        camera.update();
    }

    /**
     * Points camera to received point
     * @param target    the point to point camera to
     */
    public static void pointCameraTo(Vector2 target) {
        camera.position.x = target.x;
        camera.position.y = target.y;
    }

    float timeElapsed = 0f;
    boolean startDelay = true;

    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, false);

    float timeDead = 0f, timeRespawning = 0f;
    float blackOutLifeTime = 2f, recolorLifeTime = 1f;
    public static boolean respawnAnimOn = false;
    @Override
    public void render(float delta) {
        if(gameClient.getClientCharacter() == null) return;

        // updates cursor if on desktop
        if(Gdx.app.getType() == Application.ApplicationType.Desktop)
            updateCursor();

        updateCamera(); // updates camera
        updateProjectiles(); // update alive projectiles
        updateMenuButtons(); // update menu buttons - highlights if menus are opened

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

        Color bColor = new Color(batch.getColor());

        if(lockWorldRender)
            batch.setColor(Color.BLACK);
        else
            batch.setColor(bColor);

        Entity.Character clientChar = gameClient.getClientCharacter();
        if(!clientChar.isAlive && !clientChar.isRespawning) {
            timeDead+=delta;
            float progress = Math.min(1f, timeDead/blackOutLifeTime);   // 0 -> 1
            batch.setColor(bColor.cpy().lerp(Color.BLACK, progress));

            // adds respawn button after finishing the animation
            if(progress >= 1f) {
                lockWorldRender = true;
                respawnAnimOn = true;
                if(respawnBtn.getStage() == null) { // only adds if its not in stage yet
                    stage.addActor(respawnBtn);
                }
                clearPools(); // clear all pool objects
            }
        } else {
            timeDead = 0f;
            if(respawnBtn.getStage() != null) // if player is alive, remove respawn button from stage if its on
                respawnBtn.remove();
            if(deathMsgLabel.getStage() != null) { // also remove death msg if its on
                deathMsgLabel.remove();
            }
        }

        if(respawnAnimOn && !lockWorldRender) {
            timeRespawning+=delta;
            float progress = Math.min(1f, timeRespawning/recolorLifeTime);   // 0 -> 1
            batch.setColor(Color.BLACK.cpy().lerp(bColor, progress));

            if(progress >= 0.1f) { // finishes respawn
                clientChar.isRespawning = false;
            }
            if(progress >= 1.0f) { // finishes animation
                respawnAnimOn = false;
                timeRespawning = 0f;
                showMapAndZoneLabel();
            }
        }

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
        if(gameClient.getClientCharacter() == null) return;
        Entity target = gameClient.getClientCharacter().getTarget();
        if(target != null) {
            target.renderUI(batch);
            if(ENABLE_TARGET_UI)
                showEntityTargetUI(target);
        }

        batch.setProjectionMatrix(camera.combined);

        if(gameClient.getClientCharacter() == null) return;

        // always draw client simplified ui (actually only if no chat texts are being displayed atm)
        if(FADE_CLIENT_TAG_ON_ALL_FLOATING_TEXT) {
            if(floatingTextCollision(gameClient.getClientCharacter().tagHitBox))
                batch.setColor(bColor.r, bColor.g, bColor.b, 0.3f); // render tag with transparency

            gameClient.getClientCharacter().renderUI(batch);
            batch.setColor(bColor.r, bColor.g, bColor.b, bColor.a);
        } else {
            if (!gameClient.getClientCharacter().floatingTextCollision(batch))
                gameClient.getClientCharacter().renderUI(batch);
            else {
                batch.setColor(bColor.r, bColor.g, bColor.b, 0.3f); // render tag with transparency
                gameClient.getClientCharacter().renderUI(batch);
                batch.setColor(bColor.r, bColor.g, bColor.b, bColor.a);
            }
        }

        // renders hover entity information if player exists and is alive
        if(GameClient.getInstance().getClientCharacter() != null // only look for hover entity if player exists and is alive
                && GameClient.getInstance().getClientCharacter().isAlive) {
            if ((Gdx.app.getType() == Application.ApplicationType.Desktop &&
                    WorldMap.hoverEntity != null)) {
                if (target == null
                        || target.uId !=
                        WorldMap.hoverEntity.uId) { // only renders if target is not the one being hovered, as info is already being rendered by being a target
                    WorldMap.hoverEntity.renderUI(batch);
                    if (ENABLE_TARGET_UI)
                        showEntityTargetUI(WorldMap.hoverEntity);
                }
            }
        }

        if(ENABLE_TARGET_UI && target == null && WorldMap.hoverEntity == null)
            hideEntityTargetUI(); // makes sure to hide target ui when no target nor hover exists


        renderFloatingTexts(); // updates and renders floating texts alive

        batch.end();
        batch.setColor(bColor.r, bColor.g, bColor.b, bColor.a);

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

//        shapeDebug.setProjectionMatrix(stage.getCamera().combined);
//        shapeDebug.begin(ShapeRenderer.ShapeType.Filled);
//        shapeDebug.setColor(Color.BLUE);
//        //shapeDebug.circle(unprojectedMouse.x, unprojectedMouse.y, 1);
////        for(Rectangle portal : WorldMap.portals) {
////            shapeDebug.rect(portal.x, portal.y, portal.width, portal.height);
////        }
//        shapeDebug.rect(chatWindow.getHitBox().getX(), chatWindow.getHitBox().getY(), chatWindow.getHitBox().width, chatWindow.getHitBox().getHeight());
//        shapeDebug.end();

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
            if(WorldMap.hoverEntity.type == GameRegister.EntityType.TREE) {
                Entity.Tree tree = GameClient.getInstance().getTreeByUid(WorldMap.hoverEntity.uId);
                if (tree != null) //{
                    mouseOnLabel.setText("Entity: " + GameClient.getInstance().getTreeByUid(WorldMap.hoverEntity.uId).spawnId + " (" + WorldMap.hoverEntity.type.toString() + ")");
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

    private void toggleMenuButtonColor(GameWindow window, Button btn) {
        if(window.getStage() == null) {
            btn.setColor(Color.WHITE);
        } else
            btn.setColor(CommonUI.MENU_BUTTON_OPENED_COLOR);
    }

    private void updateMenuButtons() {
        toggleMenuButtonColor(contactWindow, contactsBtn);
        toggleMenuButtonColor(optionWindow, optionsBtn);
    }

    /**
     * Show map and zone label on ui for a short period of time
     */
    private void showMapAndZoneLabel() {
        mapNameLabel.setText("{SIZE=230%}{COLOR=WHITE}"+gameClient.getClientCharacter().map+"{ENDCOLOR}{STYLE=%}");
        zoneNameLabel.setText("{SIZE=120%}{COLOR=LIGHTGRAY}"+gameClient.getClientCharacter().zone+"{ENDCOLOR}{STYLE=%}");
        mapNameLabel.setPosition(stage.getWidth()/2f - mapNameLabel.getWidth()/2f, stage.getHeight()/1.12f - mapNameLabel.getHeight()/2f);
        zoneNameLabel.setPosition(stage.getWidth()/2f - zoneNameLabel.getWidth()/2f, mapNameLabel.getY()*0.9f - zoneNameLabel.getHeight()/2f);
        if(mapNameLabel.hasEnded())
            mapNameLabel.restart();
        if(zoneNameLabel.hasEnded())
            zoneNameLabel.restart();
        stage.addActor(mapNameLabel);
        stage.addActor(zoneNameLabel);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                mapNameLabel.remove();
                zoneNameLabel.remove();
            }
        },CommonUI.MAP_INFO_LABEL_LIFETIME);
    }

    /**
     * Update current alive floating texts
     */
    private void renderFloatingTexts() {
        Iterator<FloatingText> iter = floatingTexts.iterator();
        while (iter.hasNext()) {
            FloatingText floatingText = iter.next();

            // only render floating texts if client player is alive or floating text is from dying client
            if(gameClient.getClientCharacter().isAlive || floatingText.creator.uId == gameClient.getClientUid())
                floatingText.render(batch, Gdx.graphics.getDeltaTime());

            // if floating text is dead removes it and free pool
            if (floatingText.alive == false) {
                iter.remove();
                floatingTextPool.free(floatingText);
            }
        }
    }

    /**
     * Checks collision with every alive static floating text
     */
    private boolean floatingTextCollision(Rectangle rect) {
        Iterator<FloatingText> it = floatingTexts.iterator();
        while(it.hasNext()) {
            FloatingText ft = it.next();
            if(ft.animSpeed > 0f) // ignore non-static floating texts (damage points for instance...)
                continue;
            Rectangle intersection = new Rectangle();
            if (Intersector.intersectRectangles(ft.getHitBox(), rect, intersection))
                return true;
        }

        return false;
    }

    /** clears pool objects  **/
    private void clearPools() {
        Iterator<FloatingText> iter = floatingTexts.iterator();
        while (iter.hasNext()) {
            FloatingText floatingText = iter.next();
            iter.remove();
            floatingTextPool.free(floatingText);
        }
        floatingTextPool.clear();
        floatingTexts.clear();

//        Iterator<Projectile> iter2 = projectiles.iterator();
//        while (iter2.hasNext()) {
//            Projectile projectile = iter2.next();
//            iter2.remove();
//            projectilePool.free(projectile);
//        }
    }

    /**
     * Update current alive projectiles
     */
    private void updateProjectiles() {
        Iterator<Projectile> iter = projectiles.iterator();
        while (iter.hasNext()) {
            Projectile projectile = iter.next();
            projectile.update(Gdx.graphics.getDeltaTime());

            if (projectile.alive == false) {
                iter.remove();
                projectilePool.free(projectile);
            }
        }
    }

    /**
     * Updates cursor based on current hovered entity
     */
    private void updateCursor() {
        if(WorldMap.hoverEntity == null || !gameClient.getClientCharacter().isAlive ||
                WorldMap.hoverEntity.uId == GameClient.getInstance().getClientCharacter().uId) {
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

        uiCam.viewportWidth = width;
        uiCam.viewportHeight = height;
        uiCam.position.set(width / 2, height / 2, 0);
        uiCam.update();

        /**
         * Update centered windows
         */
        optionWindow.setPosition(stage.getWidth() / 2.0f , stage.getHeight() / 2.0f, Align.center);
        optionWindow.resize(width, height);

//        contactWindow.setPosition(stage.getWidth() / 2.0f , stage.getHeight() / 2.0f, Align.center);
//        contactWindow.resize(width, height);
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
        projectiles.clear();
        floatingTexts.clear();
        chatWindow.dispose();
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
                onActor = isActorHit(screenMouse.x, screenMouse.y);
                onStageActor = onActor == null ? false : true;
                firstTouchOnActor = onActor == null ? false : true;

                if (contextWindow.getStage() != null && !contextWindow.isPointOn(screenMouse.x, screenMouse.y)) {
                    hideContextMenu();
                }

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

            if(WorldMap.hoverEntity != null) { // if long press on an entity , set it as target.
                GameClient.getInstance().getClientCharacter().setTarget(WorldMap.hoverEntity);
                onStageActor = true;
                return true;
            }

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
            if(firstTouchOnActor) {
                draggingActor = true;
                isPanning = false;
                return false; // Only acts with joystick if it did not hit any UI actor
            } else {
                isPanning = true;
            }

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
            if(draggingActor) draggingActor = false;
            if(isPanning) isPanning = false;
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
     * Clear any opened windows that can be closed
     */
    private void clearWindows() {
        if(optionWindow.getStage() != null)
            CommonUI.removeWindowWithAction(optionWindow, fadeOut(0.2f));
        if(contactWindow.getStage() != null)
            CommonUI.removeWindowWithAction(contactWindow, fadeOut(0.2f));
        chatWindow.clearWindows();
    }

    /**
     * Should reload texts to update to new language settings
     */
    private void reloadLanguage() {
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        chatWindow.reloadLanguage();
        // if info label is active, set text in new language
        if(infoToast.label.isVisible()) {
            if(infoToast.arg0 == null)
                infoToast.label.setText(langBundle.get(infoToast.langKey));
            else {
                String info = "";
                if(infoToast.arg0 instanceof Integer)
                    info = langBundle.format(infoToast.langKey, (int)infoToast.arg0);
                else if(infoToast.arg0 instanceof String)
                    info = langBundle.format(infoToast.langKey, (String)infoToast.arg0);
                infoToast.label.setText(info);
            }
        }
        openChannelWindow.reloadLanguage();
    }

    /**
     * Process commands received via opened windows
     */
    public void processWindowCmd(GameWindow window, boolean rebuild, CommonUI.ScreenCommands cmd) {
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
                case LOGOUT:
                    logoff(); // logs off
                    break;
                default:
                    Gdx.app.error("Unknown Window Command", "Current screen received an unknown command from option window");
                    break;
            }

            // reloads option window if requested (only if its opened) - updates language changes
            if(rebuild && optionWindow.getStage().equals(stage))
                loadWindow(optionWindow, true, true);
        }
    }

    private void logoff() {
        gameClient.logoff();
        chatClient.logoff();
    }

    /**
     * Loads a game window into stage
     * @param gameWindow the game window to be loaded
     * @param single if window should be the only one opened, if so close all other windows
     * @param centered if this window should be centered
     */
    private void loadWindow(GameWindow gameWindow, boolean single, boolean centered) {
        // clear all windows to make sure only one is going to be opened at the same time
        if(single)
            clearWindows();
        // builds game window - builds make sure that this window is not opened already and that it is clear for rebuilding
        gameWindow.build();
        // centers game window if centered is set to true
        if(centered)
            gameWindow.setPosition(stage.getWidth() / 2.0f , stage.getHeight() / 2.0f, Align.center);
        else {
            if(gameWindow.lastPosition == null) {
                gameWindow.lastPosition = new Vector2(stage.getWidth() / 2.0f - gameWindow.getWidth()/2f,
                                        stage.getHeight() / 2.0f  - gameWindow.getHeight()/2f);
            }
            gameWindow.setPosition(gameWindow.lastPosition.x, gameWindow.lastPosition.y);
        }

        if(gameWindow.getCaptureListeners().size == 0) {
            gameWindow.addCaptureListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    onStageActor = true;
                    onStageActorDown = true;
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    onStageActorDown = false;
                }

            });
        }

        //gameWindow.setOrigin(Align.center);
        //gameWindow.setTransform(true);
        //gameWindow.addAction(sequence(scaleTo(1.2f,1.2f,0.1f),scaleTo(1f,1f,0.1f)));
        Color c = gameWindow.getColor();
        gameWindow.setColor(c.r, c.g, c.b, 0f);
        gameWindow.addAction(fadeIn(0.1f));
        // adds built game window to the stage to display it
        stage.addActor(gameWindow);
        stage.draw();
    }

    public static void updatePingSimulation(int inc, boolean updateServerPing) {
        if(updateServerPing) {
            GameClient.getInstance().sendPingUpdate(inc);
        } else {
            GameRegister.lag += inc;
            if(GameRegister.lag <= 0) {GameRegister.lag = 0; return;} // minimum = no lag simulation
            if(GameRegister.lag >= 2000) {GameRegister.lag = 2000; return;}// maximum = 2 sec

            chatWindow.sendMessage("Debug", -1, ChatRegister.ChatChannel.DEFAULT, "Changed client lag to: " + GameRegister.lag, null, Color.ORANGE, -1, false);
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
             * ALL PLATFORM UI CONTROLS
             */
            respawnBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    GameClient.getInstance().getClientCharacter().isRespawning = true; // sets flag that informs that player is respawning
                    GameClient.getInstance().respawnClient(); // inform server that client player requested respawn
                    event.stop();
                    event.handle();
                    event.cancel();
                }
            });
            optionsBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    toggleWindow(optionWindow,false, true);
                }
            });
            contactsBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStageActor = true;
                    toggleWindow(contactWindow, false, false);
                }
            });
        }
    }

    void toggleWindow(GameWindow window, boolean single, boolean centered) {
        if(window.getStage()==null) {
            loadWindow(window, single, centered);
        }
        else {
            if(window.lastPosition != null) // for draggable windows
                window.lastPosition.set(window.getX(), window.getY());
            CommonUI.removeWindowWithAction(window, fadeOut(0.1f));
        }
    }
}