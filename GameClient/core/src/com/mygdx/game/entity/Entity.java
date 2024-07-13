package com.mygdx.game.entity;

import static com.mygdx.game.entity.WorldMap.TEX_HEIGHT;
import static com.mygdx.game.entity.WorldMap.TEX_WIDTH;
import static com.mygdx.game.entity.WorldMap.edgeFactor;
import static com.mygdx.game.entity.WorldMap.unitScale;
import static com.mygdx.game.ui.CommonUI.getPixmapCircle;
import static com.mygdx.game.ui.GameScreen.camera;
import static com.mygdx.game.ui.GameScreen.shapeDebug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.ui.CommonUI;
import com.mygdx.game.ui.GameScreen;
import com.mygdx.game.util.Common;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Entity implements Comparable<Entity> {
    public static AssetManager assetManager; // screen asset manager
    public static Stage stage; // screen stage
    protected final TextureAtlas uiAtlas, sfxAtlas;
    protected final Image uiBg;
    protected Image healthBar;
    protected final Image healthBarBg;
    protected Skin skin;
    protected Font font;
    public String entityName = "";
    public boolean isClient = false;
    private ShapeRenderer shapeRenderer; // for debug
    protected Pixmap debugCircle; // for debug
    protected Texture debugTex; // for debug
    public TypingLabel nameLabel; // name label of entity
    public static float tagScale = unitScale * 0.415f;
    static float rectOffsetUp = 0.2f, rectOffsetDown = 0.2f, rectOffsetLeft = 0.4f, rectOffsetRight = 0.4f;
    public Polygon hitBox = new Polygon();
    public Vector2 drawPos = new Vector2(0,0); // position to draw this entity
    public Vector2 finalDrawPos = new Vector2(0,0); // final position to draw this entity (for y-ordering, sometimes drawPos differs from final draw pos)
    public Vector2 centerPos = new Vector2(0,0); // center position of entity sprite
    public int uId; // this entity unique id
    public int contextId; // the id of this id in context, based on what type of entity it is
    public Type type; // the type of this entity, specifying its child class
    public GameRegister.EntityState state = GameRegister.EntityState.FREE; // the server current state known of this entity
    protected Timer interactionTimer; // timer that controls the interaction of entity
    public boolean isInteractive = false; // if this entity is interactive
    public boolean isTargetAble = false; // if this entity is target-able
    public boolean isObfuscator = false; // if this entity is obfuscator it will be rendered transparent when on top of player
    public boolean isTeleporting = false;
    public boolean isAlive = true;
    public boolean teleportInAnim = false;
    public boolean teleportOutAnim = false;
    public float alpha = 1f;
    protected boolean fadeIn = false;
    protected boolean fadeOut = false;
    protected float fadeSpeed = 2f;
    protected float animSizeFactor = 1f; // for yo yo size animation
    protected float animDir = 1; // for yo yo movement animation
    public float health, maxHealth, attackSpeed = 1f;
    public float spriteW;
    public float spriteH;
    protected TextureRegion currentFrame = null;
    protected Vector2 center = new Vector2();

    public Entity() {
        /**
         * Load assets common to entities
         */
        sfxAtlas = assetManager.get("sfx/packed_textures/sfx.atlas");
        uiAtlas = assetManager.get("ui/packed_textures/ui.atlas");
        skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        font = skin.get("emojiFont", Font.class); // gets typist font with icons
        uiBg = new Image(uiAtlas.findRegion("UiBg"));
        healthBarBg = new Image(uiAtlas.findRegion("healthBarBg"));
        Gdx.app.postRunnable(() -> {
            TextureAtlas.AtlasRegion reg = uiAtlas.findRegion("healthBar");
            reg.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            healthBar = new Image(reg);
        });
    }

    public abstract Vector2 getEntityCenter();

    /**
     * Checks if point hits this entity
     *
     * @param point the point to check
     * @return true if it hits, false otherwise
     */
    public boolean rayCast(Vector2 point) {
        return hitBox.contains(point);
    }
    public boolean rayCast(Vector3 point) {
        return hitBox.contains(point.x, point.y);
    }

    /**
     * Returns if this entity is unmovable (teleporting, dead or other state that should not be moved)
     * @return  true if its unmovable, false otherwise
     */
    public boolean isUnmovable() {
        return teleportInAnim || teleportOutAnim || fadeIn || fadeOut || !isAlive;
    }

    public void resetYoYoAnim () {
        animSizeFactor = 1f;
        animDir = 1;
    }

    public void fadeIn(float fadeSpeed) {
        if(fadeIn || fadeOut) return;

        alpha = 0f;
        fadeIn = true;
        this.fadeSpeed = fadeSpeed;
    }

    public void fadeOut(float fadeSpeed) {
        if(fadeIn || fadeOut) return;

        alpha = 1f;
        fadeOut = true;
        this.fadeSpeed = fadeSpeed;
    }

    /**
     * Applies effects to this entity and deals with the aftermath of effects
     * @param batch the batch to apply effects
     */
    public void applyEffects(SpriteBatch batch) {
        if(this.fadeIn) {
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, alpha); //set alpha interpolated

            alpha += fadeSpeed * Gdx.graphics.getDeltaTime();
            if(alpha >= 1f) {
                this.fadeIn = false;
                alpha = 1f;
            }
        }
        if(this.fadeOut) {
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, alpha); //set alpha interpolated
            alpha -= fadeSpeed * Gdx.graphics.getDeltaTime();
            if (alpha <= 0f) {
                this.fadeOut = false;
                remove(); // after fade out, remove entity from world
                alpha = 0f;
            }
        }
    }

    /**
     * Removes this entity from the world, meaning
     * removing it from visible entities list, player AoI map and
     * from target/hover vars if its current client target
     */
    public void remove() {
        // remove from lists
        EntityController.getInstance().entities.remove(uId); // remove from list of entities to draw
        GameClient.getInstance().removeEntity(uId); // remove from data list of entities
        if(GameClient.getInstance().getClientCharacter().target != null && // if this entity is target, remove it from target and hover vars
                GameClient.getInstance().getClientCharacter().target.uId == uId) {
            WorldMap.hoverEntity = null;
            GameClient.getInstance().getClientCharacter().target = null;
        }
    }

    public TextureRegion getCurrentFrame() {
        return currentFrame;
    }

    /**
     * For DEBUG ONLY!
     */
    public abstract void takeDamage();

    public enum Type {
        CREATURE,
        NPC,
        WALL,
        PORTAL,
        TREE,
        PROJECTILE,
        CHARACTER
    }

    public enum Direction {
        NORTHWEST(-1, 1),
        NORTH(0, 1),
        NORTHEAST(1, 1),
        EAST(1, 0),
        SOUTHEAST(1, -1),
        SOUTH(0, -1),
        SOUTHWEST(-1, -1),
        WEST(-1, 0)
        ;

        public int dirX, dirY;

        Direction(int x, int y) {
            this.dirX = x;
            this.dirY = y;
        }

        public static Direction getDirection(int x, int y){
            //System.out.println(x + " / \\ " + y);
            for (Direction dir : Direction.values()) {
                if (dir.dirX == x && dir.dirY == y) {
                    return dir;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return Direction.SOUTH;// if no direction was found, return south as default
        }

        public static Vector2 fromDirection(Direction dir) {
            return new Vector2(dir.dirX, dir.dirY);
        }
    }

    public enum State {
        WALKING("WALKING"),
        RUNNING("RUNNING"),
        IDLE("IDLE"),
        IDLE_WALKING("IDLE_WALKING"),
        FOLLOWING("FOLLOWING"),
        ATTACKING("ATTACKING"),
        FLEEING("FLEEING"),
        DYING("DYING");

        public String name;

        State(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static State getStateFromName(String name){
            for (State st : State.values()) {
                if (st.name.equalsIgnoreCase(name)) {
                    return st;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return null;
        }

    }

    /**
     * Draws entity tag ui containing entity info including name and health and mana if client
     *
     * @param batch     the batch to draw
     * @param x         the entities' x position to use as anchor for ui
     * @param y         the entities' y position to use as anchor for ui
     * @param w         the desired width of tag ui
     * @param h         the desired height of tag ui
     * @param nameColor the color of the tag name
     */
    public void renderEntityTag(SpriteBatch batch, float x, float y, float w, float h, Color nameColor) {
        if(this.health<=0) return; // don't render if not alive

        float eAlpha = alpha;
        if(alpha>1.0f) eAlpha = 1.0f;

        /**health bar background**/
        healthBarBg.setBounds(x+spriteW/2f - w/2f, y+spriteH -h/2, w, h);
        healthBarBg.draw(batch, eAlpha);

        /**health bar**/
        float percent = health/maxHealth;
        Color c = Color.GREEN;
        if(percent < 0.61)
            c = Color.YELLOW;
        if(percent < 0.25)
            c = Color.RED;
        healthBar.setColor(c);

        w *= 0.938f;
        h *= 0.525f;
        healthBar.setBounds(healthBarBg.getX()+w*0.032f, healthBarBg.getY()+h*0.45f, w*percent, h);
        healthBar.draw(batch, eAlpha);


        /**entity tag name**/
        nameLabel.getFont().scale(tagScale, tagScale);
        nameLabel.setColor(nameColor);
        nameLabel.setBounds(healthBarBg.getX()+healthBarBg.getWidth()/2f -  (nameLabel.getWidth()*tagScale/2f) -w*0.0267f,
                healthBarBg.getY() + 8.16f + healthBarBg.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());

        nameLabel.draw(batch, eAlpha);

        nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);

        // makes sure batch color is reset with correct alpha
        batch.setColor(Color.WHITE);
    }

    /**
     * Render entity complete ui
     *
     * @param batch     the batch to draw
     * @param x         the entities' x position to use as anchor for ui
     * @param y         the entities' y position to use as anchor for ui
     * @param w         the desired width of tag ui
     * @param h         the desired height of tag ui
     * @param nameColor the color of the tag name
     */
    public void renderEntityUI(SpriteBatch batch, float x, float y, float w, float h, Color nameColor) {
        if(health<=0) return;

        // entity may teleport
        float eAlpha = alpha;
        if(alpha>1.0f) eAlpha = 1.0f;

        /**
         * Entity INFO
         */
        /**ui background**/
        uiBg.setBounds(x+spriteW/2f - w/2f, y+spriteH, w, h);
        uiBg.draw(batch, 0.74f * eAlpha);

        /**health bar**/
        // background
        w *= 0.8157f;
        h *= 0.48f;
        healthBarBg.setBounds(uiBg.getX()+uiBg.getWidth()/2f - w/2f, uiBg.getY()+h*0.18f, w, h);
        healthBarBg.draw(batch, eAlpha);

        // health bar
        float percent = health/maxHealth;
        healthBar.setColor(Color.RED.cpy().lerp(Color.OLIVE, percent*1.25f + 0.15f));

        w *= 0.938f;
        h *= 0.525f;
        healthBar.setBounds(healthBarBg.getX()+w*0.032f, healthBarBg.getY()+h*0.45f, w*percent, h);
        healthBar.draw(batch, eAlpha);

        /**tag name**/
        nameLabel.getFont().scale(tagScale, tagScale);
        nameLabel.setColor(nameColor);
        nameLabel.setAlignment(Align.center);

        nameLabel.setBounds(healthBarBg.getX()+healthBarBg.getWidth()/2f -  (nameLabel.getWidth()*tagScale/2f) - 0.01f*w,
                healthBarBg.getY() +8.16f + 2*h,
                nameLabel.getWidth(), nameLabel.getHeight());
        nameLabel.draw(batch, eAlpha);

        nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);

        // percent text
        int percentInt = (int) (percent * 100f);
        StringBuilder sb = new StringBuilder();
        sb.append(percentInt);
        sb.append("%");

        TypingLabel tmpLabel = new TypingLabel(String.valueOf(sb), font);
        float percentScale = tagScale*0.43f;
        float percentW = tmpLabel.getWidth()*percentScale;
        font.scale(percentScale, percentScale);
        font.drawText(batch, sb, healthBarBg.getX() + healthBarBg.getWidth()/2f - percentW/2f, healthBarBg.getY()+h*0.34f);
        font.scaleTo(font.originalCellWidth, font.originalCellHeight);

        // makes sure batch color is reset with correct alpha
        batch.setColor(Color.WHITE);
    }

    public void renderTargetCircle(SpriteBatch batch, float xOffset, float yOffset,float scale) {
        if(this.health<=0) return;
        /**
         * TARGET CIRCLE
         */
        float tileHeight = TEX_HEIGHT * unitScale;
        float tileWidth = TEX_WIDTH * unitScale;

        float step =  0.15f * Gdx.graphics.getDeltaTime() * animDir;

        animSizeFactor = ((animSizeFactor + step));

        if(animSizeFactor > 1.05f) animDir = -1;
        if(animSizeFactor < 0.95f) animDir = 1;

        float targetScale = scale * animSizeFactor;

        float x = finalDrawPos.x + tileWidth*xOffset;
        float y = finalDrawPos.y + tileHeight*yOffset;
        float a = x + tileWidth/2f;
        float b = y + tileHeight/2f;
        float scaleFactor = tileWidth*targetScale;
        float smallerFactor = 0.81f;

        batch.end();
        Gdx.gl.glLineWidth(4);
        shapeDebug.setProjectionMatrix(camera.combined);
        shapeDebug.begin(ShapeRenderer.ShapeType.Line);
        shapeDebug.setColor(Color.FIREBRICK);
        shapeDebug.ellipse((x - a) * scaleFactor + a, (y - b) * scaleFactor + b,
                TEX_WIDTH* unitScale*targetScale, TEX_HEIGHT*unitScale*targetScale, 20);
        shapeDebug.setColor(Color.MAROON);
        scaleFactor = tileWidth*targetScale*smallerFactor;
        shapeDebug.setAutoShapeType(true);
        shapeDebug.set(ShapeRenderer.ShapeType.Filled);
        shapeDebug.ellipse((x - a) * scaleFactor + a, (y - b) * scaleFactor + b,
                TEX_WIDTH* unitScale*targetScale*smallerFactor, TEX_HEIGHT*unitScale*targetScale*smallerFactor, 20);

        shapeDebug.end();
        Gdx.gl.glLineWidth(1);
        batch.begin();
    }

    /**
     * Entities that belongs to the world can be drawn in the world using this method
     * @param batch     the batch to draw entity
     */
    public abstract void render(SpriteBatch batch) ;

    /**
     * Entities that belongs to the world can draw UI on top of itself using this method
     *
     * @param batch the batch to draw UI
     */
    public abstract void renderUI(SpriteBatch batch) ;

    /**
     * This method is called when entity is target of client player to render target selected circle
     * @param batch the batch to draw UI of target selected
     */
    public abstract void renderTargetUI(SpriteBatch batch);

    public static class Character extends Entity {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 6, FRAME_ROWS = 24;
        private static final float ANIM_BASE_INTERVAL = 0.25175f; // the base interval between anim frames
        private Polygon collider = new Polygon(); // this players collider
        public AtomicLong lastRequestId;
        public boolean assetsLoaded = false;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        private Entity target = null; // the current character target - selected entity
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int id, role_level, outfitId;
        public Vector2 position, interPos;
        public Direction direction;
        public float x, y, speed;
        public TypingLabel outlineLabel;
        private Vector2 startPos; // used for interpolation
        public ConcurrentSkipListMap<Long, Vector2> bufferedPos;
        private Map.Entry<Long, Vector2> oldestEntry;
        public CopyOnWriteArrayList<EntityInterPos> buffer = new CopyOnWriteArrayList<>();
        private Vector2 goalPos;
        private float tIntElapsed;
        private boolean isWalking = false;

        @Override
        public int compareTo(Entity entity) {
//            Vector3 e1Iso = WorldMap.translateScreenToIso(this.drawPos);
//            Vector3 e2Iso = WorldMap.translateScreenToIso(entity.drawPos);
//            float e1Depth = e1Iso.x + e1Iso.y;
//            float e2Depth = e2Iso.x + e2Iso.y;
//            if(e1Depth <= e2Depth)
//                return -1;
//            else
//                return 1;
            Vector2 e1Iso = this.drawPos;
            Vector2 e2Iso = entity.drawPos;
            float e1Depth = e1Iso.y;
            float e2Depth = e2Iso.y;
            if(e1Depth > e2Depth)
                return -1;
            else
                return 1;
        }

        public Entity getTarget() {
            return target;
        }

        public void setTarget(Entity e) {
            if(target != null && e != null && e.uId == target.uId) return; // same target as before, do nothing
            if(e != null && !e.isTargetAble) return; // if its not targetable return

            if(e != null) e.resetYoYoAnim(); // if a entity is selected, reset its yo yo target animation

            this.target = e; // set target to an entity or null representing client has no target atm

            stopInteraction(); // in case there was an interaction going on, stop it before starts a new one

            if(e != null)   // only starts interaction if a target is selected
                startInteraction();
        }

        public static class EntityInterPos {
            public long timestamp;
            public Vector2 position;
        }

        public Character(String name, int id, int role_level, float x, float y, float maxHealth, float health, float speed) {
            super();
            this.name = name; this.id = id; this.role_level = role_level; this.outfitId = 0;
            this.entityName = name;
            this.uId = EntityController.getInstance().generateUid();
            this.position = new Vector2(x, y); this.interPos = new Vector2(x, y);
            this.drawPos = new Vector2(x, y);
            this.x = x; this.y = y; this.speed = speed; lastRequestId = new AtomicLong(0);
            this.maxHealth = maxHealth;
            this.health = health;
            this.direction = Direction.SOUTH;
            this.bufferedPos = new ConcurrentSkipListMap<>();
            this.contextId = this.id;
            this.type = Type.CHARACTER;
            this.isInteractive = true;
            this.isTargetAble = true;
            //this.bufferedPos.putIfAbsent(System.currentTimeMillis(), new Vector2(this.x, this.y));
            startPos = new Vector2(this.x, this.y);
            if(role_level == 0)
                nameLabel = new TypingLabel("{SIZE=69%}{COLOR=GOLD}"+this.name+"[%]", font);
            else if(role_level == 4)
                nameLabel = new TypingLabel("{SIZE=69%}{COLOR=RED}{RAINBOW=0.5;0.3}[[adm]"+this.name+"{ENDRAINBOW}", font);
            nameLabel.skipToTheEnd();
            outlineLabel = new TypingLabel("{SIZE=69%}{COLOR=black}"+this.name+"[%]", font);
            outlineLabel.skipToTheEnd();
            // initialize animators
            walk = new ConcurrentHashMap<>();
            attack = new ConcurrentHashMap<>();
            idle = new ConcurrentHashMap<>();

            //stage.addActor(this);

            Gdx.app.postRunnable(() -> {
                debugCircle = getPixmapCircle(10, Color.RED,true);
                debugTex=new Texture(debugCircle);
                debugTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Load the sprite sheet as a Texture
                spriteSheet = new Texture(Gdx.files.internal("spritesheet/outfit/"+this.outfitId+".png"));
                spriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Use the split utility method to create a 2D array of TextureRegions. This is
                // possible because this sprite sheet contains frames of equal size and they are
                // all aligned.
                spriteW = (spriteSheet.getWidth() / FRAME_COLS) * unitScale;
                spriteH = (spriteSheet.getHeight() / FRAME_ROWS) * unitScale;
                TextureRegion[][] tmp = TextureRegion.split(spriteSheet,
                        spriteSheet.getWidth() / FRAME_COLS,
                        spriteSheet.getHeight() / FRAME_ROWS);

                float atkFactor = 1.5f / (speed*0.33f); // TODO: ATTK SPEEd
                float walkFactor = (4f / (speed*0.10f)) * unitScale;

                // Place the regions into a 1D array in the correct order, starting from the top
                // left, going across first. The Animation constructor requires a 1D array.
                for(int i = 0; i < FRAME_ROWS; i++) {
                    TextureRegion[] frames = new TextureRegion[FRAME_COLS];
                    int index = 0;
                    for (int j = 0; j < FRAME_COLS; j++) {
                        frames[index++] = tmp[i][j];
                    }
                    switch(i) { // switch rows to add each animation correctly
                        case 0:
                            idle.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 1:
                            walk.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 2:
                            attack.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 3:
                            idle.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 4:
                            walk.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 5:
                            attack.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 6:
                            idle.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 7:
                            walk.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 8:
                            attack.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 9:
                            idle.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 10:
                            walk.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 11:
                            attack.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 12:
                            idle.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 13:
                            walk.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 14:
                            attack.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 15:
                            idle.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 16:
                            walk.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 17:
                            attack.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 18:
                            idle.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 19:
                            walk.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 20:
                            attack.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        case 21:
                            idle.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 22:
                            walk.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 23:
                            attack.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * atkFactor, frames));
                            break;
                        default:
                            break;
                    }
                }

                // reset the elapsed animation time
                animTime = 0f;

                centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
                nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                        this.centerPos.y + spriteH/2f,
                        nameLabel.getWidth(), nameLabel.getHeight());
                assetsLoaded = true;
            });
        }

        /**
         * Updates speed of character adjusting animation speed as well (should be used instead of changing speed var directly)
         * @param speed the new speed of the player (should be between 1.0 and 9.0)
         */
        public void updateSpeed(float speed) {
            if(speed < 1.0 || speed > 9.0) return;

            this.speed = speed;
            float newFrameDuration = (4f / (speed*0.10f)) * unitScale * ANIM_BASE_INTERVAL;
            synchronized (walk) {
                Iterator<Animation<TextureRegion>> itr = walk.values().iterator();
                while(itr.hasNext()) {
                    Animation<TextureRegion> anim = itr.next();
                    anim.setFrameDuration(newFrameDuration);
                }
            }
        }

        // updates based on character changes
        public void update(float x, float y) {
            this.x = x;
            this.y = y;
            this.position.x = x;
            this.position.y = y;
            //this.collider.setPosition(x, y);
            if(!Common.entityInterpolation) {
                Gdx.app.postRunnable(() -> {
                    centerPos.x = this.position.x + spriteW/2f;
                    centerPos.y = this.position.y + spriteH/2f;
                    nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                            this.centerPos.y + spriteH/2f,
                            nameLabel.getWidth(), nameLabel.getHeight());
                });
            }
        }

        public void teleport(float x, float y) {
            if(!teleportInAnim) { // server detected collision and sent it before client (can happen on low latencies)
                this.isTeleporting = true;
                this.teleportInAnim = true;
                this.state = GameRegister.EntityState.TELEPORTING_IN;
                this.stopInteraction();
            }
            new Thread(() -> {
                while (this.teleportInAnim) { // wait until teleport in animation to end to update to destination
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                GameClient.getInstance().sendResponse(new GameRegister.Response(GameRegister.Response.Type.TELEPORT_IN_FINISHED));
                this.interPos.x = x;
                this.interPos.y = y;
                this.drawPos = new Vector2(x, y);
                this.position.x = x;
                this.position.y = y;
                //this.collider.setPosition(x, y);
                this.updateStage(x, y, true);
                this.x = x;
                this.y = y;
                centerPos.x = this.interPos.x + spriteW / 2f;
                centerPos.y = this.interPos.y + spriteH / 2f;
                nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth() / 2f,
                        this.centerPos.y + spriteH / 2f,
                        nameLabel.getWidth(), nameLabel.getHeight());
            }).start();
        }

        public void move(Vector2 movement) {
            this.x += movement.x;
            this.y += movement.y;
            this.position.add(movement);
            if(!Common.entityInterpolation) {
                Gdx.app.postRunnable(() -> {
                    centerPos.x = this.position.x + spriteW/2f;
                    centerPos.y = this.position.y + spriteH/2f;
                    nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                            this.centerPos.y + spriteH/2f,
                            nameLabel.getWidth(), nameLabel.getHeight());;
                });
            }
        }

        // updates stage and positions
        public void updateStage(float x, float y, boolean interpolation) {
            if(interpolation) {
                this.interPos.x = x;
                this.interPos.y = y;
                this.drawPos = new Vector2(x, y);
            } else {
                this.x = x;
                this.y = y;
            }
            centerPos.x = this.interPos.x + spriteW/2f;
            centerPos.y = this.interPos.y + spriteH/2f;
            nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                    this.centerPos.y + spriteH/2f,
                    nameLabel.getWidth(), nameLabel.getHeight());
        }

        /**
         * Given a movement msg that is to be sent to the server, tries
         * to predict the outcome before receiving server answer updating character pos
         * @param msg   the message containing the raw movement from inputs
         */
        public void predictMovement(GameRegister.MoveCharacter msg) {
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                if (touchPos.dst(futurePos) <= deltaVec.len()) // close enough, do not move anymore
                    return;

                this.move(deltaVec);
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                this.move(moveVec);
            }
            // check for walkable actions
            triggerWalkableActions();
        }

        // predict walkable actions for client
        private void triggerWalkableActions() {
            // check for portal collisions
            for(Rectangle portal : WorldMap.portals) {
                if (Common.intersects(collider, portal)) {
                    position.x = portal.x - portal.width;
                    position.y = portal.y - portal.height/2f;
                    if(!this.isTeleporting) {
                        this.isTeleporting = true;
                        this.teleportInAnim = true;
                        this.state = GameRegister.EntityState.TELEPORTING_IN;
                        this.stopInteraction();
                    }
                }
            }
        }

        /**
         * Checks if movement is possible (not blocked by walls, unwalkable tiles, other collisions...)
         * @param msg   the move character message containing the desired movement to be checked
         * @return  true if move is possible, false otherwise
         */
        public boolean isMovePossible(GameRegister.MoveCharacter msg) {
            Vector2 futurePos;
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                futurePos = new Vector2(charPos).add(deltaVec);
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                futurePos = new Vector2(this.x, this.y).add(moveVec);
            }
            if(!WorldMap.isWithinWorldBounds(futurePos) ||
                    !WorldMap.getInstance().isWalkable(futurePos))
                return false;

            // check if has jumped more than one tile in this movement (forbidden!)
            Vector2 tInitialPos = WorldMap.toIsoTileCoordinates(new Vector2(this.x, this.y));
            Vector2 tFuturePos = WorldMap.toIsoTileCoordinates(futurePos);
            if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                return false;

            return true;
        }

        /**
         * Given an initial movement that collided, search for a direction to slide and return a possible movement
         * @param initMove  the initial move that collided
         * @return  the new movement with the new direction to slide on collision
         */
        public Vector2 findSlide(GameRegister.MoveCharacter initMove) {
            Vector2 newMove = new Vector2(0,0);
            Vector2 moveVec = new Vector2(initMove.x, initMove.y).nor().scl(this.speed * GameRegister.clientTickrate());
            Vector2 moveVecCounter = new Vector2(initMove.x, initMove.y).nor().scl(this.speed * GameRegister.clientTickrate());
            // degree step for each rotation try
            float degree = 9f;

            if (initMove.hasEndPoint) { // if it has endpoint, do the movement calculations accordingly
                Vector2 touchPos = new Vector2(initMove.xEnd, initMove.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                moveVecCounter.x = deltaVec.x;
                moveVecCounter.y = deltaVec.y;
                moveVec.x = deltaVec.x;
                moveVec.y = deltaVec.y;
                degree /= 1.1f;
            }

            Vector2 futurePos = new Vector2(this.x, this.y).add(moveVec);
            Vector2 futurePosCounter = new Vector2(this.x, this.y).add(moveVecCounter);

            int tries = 0;
            while(tries < 90f/degree) { // search for new angle to move
                Vector2 tInitialPos = WorldMap.toIsoTileCoordinates(new Vector2(this.x, this.y));
                Vector2 tFuturePos = WorldMap.toIsoTileCoordinates(futurePos);
                Vector2 tFuturePosCounter = WorldMap.toIsoTileCoordinates(futurePosCounter);
                boolean isWithinOneTile = true;
                boolean isWithinOneTileCounter = true;
                if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                    isWithinOneTile = false;
                if(Math.abs(tInitialPos.x-tFuturePosCounter.x) > 1 || Math.abs(tInitialPos.y-tFuturePosCounter.y) > 1)
                    isWithinOneTileCounter = false;

                if(WorldMap.isWithinWorldBounds(futurePos) &&
                        WorldMap.getInstance().isWalkable(futurePos) &&
                        isWithinOneTile) { // found a new movement to make searching clockwise
                    newMove.x = moveVec.x;
                    newMove.y = moveVec.y;
                    break;
                }
                if(WorldMap.isWithinWorldBounds(futurePosCounter) &&
                        WorldMap.getInstance().isWalkable(futurePosCounter) &&
                        isWithinOneTileCounter) { // found a new movement to make searching counter-clockwise
                    newMove.x = moveVecCounter.x;
                    newMove.y = moveVecCounter.y;
                    break;
                }
                moveVecCounter.rotateDeg(degree);
                moveVec.rotateDeg(-degree);
//                System.out.println("Rot: " + moveVec + " / " + moveVecCounter);
                futurePos = new Vector2(this.x, this.y).add(moveVec);
                futurePosCounter = new Vector2(this.x, this.y).add(moveVecCounter);
                tries++;
            }
            return newMove;
        }

        public void startInteraction() {
            // starts update timer that control user inputs and server communication
            interactionTimer=new Timer();

            //TODO: SEND INTERACTION START TO SERVER TO CALCULATE DMG AND TO ACTUALLY TAKE HEALTH AND STUFF

            interactionTimer.scheduleTask(new Timer.Task() {
                @Override
                public void run() {
                    attackTarget();
                }
            },1/this.attackSpeed, 1/this.attackSpeed);
        }

        /** Stops any interaction that this entity is doing **/
        public void stopInteraction() {
            if(interactionTimer != null)
                interactionTimer.stop();

            if(this.state != GameRegister.EntityState.FREE)
                this.state = GameRegister.EntityState.FREE;
        }

        /**
         * Deals damage to target
         */
        public void attackTarget() {
            if(target != null) {
                // if there is a wall between player and target, do not attack
                Entity hit = EntityController.getInstance().hit(this, this.getEntityCenter(), target.getEntityCenter(), true);
                if (hit != null) {
                    this.state = GameRegister.EntityState.FREE;
                    return;
                }
                // player is able to attack target
                this.state = GameRegister.EntityState.ATTACKING;
                spawnMagicProjectile();
            }
        }

        /** Spawns magic projectile **/
        private void spawnMagicProjectile() {
            Projectile projectile = GameScreen.getProjectilePool().obtain();
            projectile.init(sfxAtlas.findRegions("PrismaWand"), 0.7f, 0.7f, 1.0f/attackSpeed,
                            1/30f,this, target, true, true, Interpolation.bounce);
            GameScreen.addProjectile(projectile);
        }

        @Override
        public void renderUI(SpriteBatch batch) {
            renderEntityTag(batch, this.finalDrawPos.x-unitScale*0.47f, this.finalDrawPos.y-unitScale*8.81f, 0.7f, 0.12f, Color.YELLOW);
        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            if(this.id != GameClient.getInstance().getClientCharacter().id) { // if its other character, render selected ui
                renderTargetCircle(batch, -0.015f, -0.23f,0.85f);
            }
        }
        //player
        @Override
        public Vector2 getEntityCenter() {
            center.set(finalDrawPos.x + spriteW/2f, finalDrawPos.y + spriteH/2.5f);
            return center;
        }

        @Override
        public void takeDamage() {
            health-=5f;
            if(health<0) {
                health = maxHealth;
                if(GameClient.getInstance().getClientCharacter().getTarget() != null &&
                        GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                    GameClient.getInstance().getClientCharacter().setTarget(null);
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            isWalking = false;

            animTime += Gdx.graphics.getDeltaTime(); // accumulates anim timer
            Map<Direction, Animation<TextureRegion>> currentAnimation = idle;

            // predicts direction if a target is selected
            if(target != null) {
                // predict direction
                Vector2 dir = target.centerPos.sub(this.centerPos).nor();
                direction = Direction.getDirection(Math.round(dir.x), Math.round(dir.y));
            }

            if(interPos.dst(position) != 0f && Common.entityInterpolation) {
                isWalking = true;
                currentAnimation = walk;
                if (GameClient.getInstance().getClientCharacter().id == this.id)
                    interpolate(true);
                else
                    interpolate(false);
            } else { // not walking, set idle animations
                currentAnimation = idle;
            }

            // attacking animation only when not walking (since we dont have attacking animation while walking atm!)
            if(this.state == GameRegister.EntityState.ATTACKING && !isWalking) {
                if(target != null) {
                    currentAnimation = attack;
                }
            }

            if(isUnmovable()) currentAnimation = idle;

            // animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

            // if its in teleport in animation, deal with fade animation since client never is removed from AoI
            if(GameClient.getInstance().getClientCharacter().id == this.id) {
                if (teleportInAnim) {
                    Color c = batch.getColor();
                    batch.setColor(c.r, c.g, c.b, alpha); //set alpha interpolated
                    alpha -= 0.8f * Gdx.graphics.getDeltaTime();
                    if (alpha <= 0.15f) {
                        alpha = 0.15f;
                        teleportInAnim = false;
                        teleportOutAnim = true;
                        this.state = GameRegister.EntityState.TELEPORTING_OUT;
                    }
                }

                if (teleportOutAnim) { // if its in teleport out animation, fade in player
                    Color c = batch.getColor();
                    batch.setColor(c.r, c.g, c.b, alpha); //set alpha interpolated
                    alpha += 0.8f * Gdx.graphics.getDeltaTime();
                    if (alpha >= 1.0f) { // give a lil bit of delay to discard unwanted lagged movements
                        teleportOutAnim = false;
                        alpha = 1f;
                        Timer timer=new Timer();
                        timer.scheduleTask(new Timer.Task() {
                            @Override
                            public void run() {
                                isTeleporting = false; // finished teleporting - inform server
                                state = GameRegister.EntityState.FREE;
                                GameClient.getInstance().sendResponse(new GameRegister.Response(GameRegister.Response.Type.TELEPORT_FINISHED));
                            }
                        },0.5f); // delay flag and server response, to be discard unwanted lagged movements

                    }
                }
            }

            applyEffects(batch); // apply entity effects before drawing
            this.finalDrawPos.x = this.interPos.x + spriteW/12f;
            this.finalDrawPos.y = this.interPos.y + spriteH/12f;

            // Get current frame of animation for the current stateTime
            currentFrame = currentAnimation.get(direction).getKeyFrame(animTime, true);
            batch.draw(currentFrame, this.finalDrawPos.x, this.finalDrawPos.y, currentFrame.getRegionWidth() * unitScale,
                    currentFrame.getRegionHeight() * unitScale);

            //System.out.println("playerW: " + currentFrame.getRegionWidth() + " / playerH: " + currentFrame.getRegionHeight());

            // updates positions and dimensions
            spriteH = currentFrame.getRegionHeight()* unitScale;
            spriteW = currentFrame.getRegionWidth()* unitScale;
            //centerPos = new Vector2(this.interPos.x + spriteW/2f + spriteW/20f, this.interPos.y + spriteH/2f + spriteH/20f);
            this.centerPos.x = this.interPos.x + spriteW/2f + spriteW/20f;
            this.centerPos.y = this.interPos.y + spriteH/2f + spriteH/20f;
            this.drawPos.x = this.interPos.x + spriteW/2f + spriteW/15f;
            this.drawPos.y = this.interPos.y + spriteH/12f + spriteH/18f;

            Vector2 cPos = new Vector2();
            cPos.x = this.position.x + spriteW/2f + spriteW/15f;
            cPos.y = this.position.y + spriteH/12f + spriteH/18f;

            // updates collider
            collider = new Polygon(new float[]{
                    cPos.x, cPos.y-rectOffsetDown*0.15f, // down
                    cPos.x-rectOffsetLeft*0.4f, cPos.y, // left
                    cPos.x, cPos.y+rectOffsetUp*0.75f, // up
                    cPos.x+rectOffsetRight*0.4f, cPos.y}); // right

            cPos.x = this.interPos.x + spriteW/2f + spriteW/15f;
            cPos.y = this.interPos.y + spriteH/12f + spriteH/18f;

            // updates hitbox (for mouse/touch interactions)
            hitBox = new Polygon(new float[]{
                    cPos.x-spriteW*0.23f, cPos.y, // down-left
                    cPos.x-spriteW*0.23f, cPos.y+spriteH*0.71f, // up-left
                    cPos.x+spriteW*0.23f, cPos.y+spriteH*0.71f, // up-right
                    cPos.x+spriteW*0.23f, cPos.y}); // down-right

            if(CommonUI.enableDebugTex) {
                Polygon tileCollider = new Polygon(new float[]{
                        drawPos.x, drawPos.y - rectOffsetDown, // down
                        drawPos.x - rectOffsetLeft, drawPos.y, // left
                        drawPos.x, drawPos.y + rectOffsetUp, // up
                        drawPos.x + rectOffsetRight, drawPos.y}); // right

                batch.end();
                shapeDebug.setProjectionMatrix(camera.combined);
                shapeDebug.begin(ShapeRenderer.ShapeType.Line);
                shapeDebug.setColor(Color.YELLOW);
                shapeDebug.polygon(collider.getTransformedVertices());
                shapeDebug.setColor(Color.PINK);
                shapeDebug.polygon(hitBox.getTransformedVertices());
                shapeDebug.setColor(Color.RED);
                shapeDebug.polygon(tileCollider.getTransformedVertices());
                shapeDebug.end();
                batch.begin();
            }

            // resets batch alpha for further rendering
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1f);//set alpha back to 1
        }

        // interpolates stage assets to player current position
        private void interpolate(boolean isClient) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(interPos.dst(position) == 0f) return; // nothing to interpolate
            if(isClient && GameClient.getInstance().isPredictingRecon.get()) return;

            if (teleportOutAnim) {
                updateStage(position.x, position.y, true);
                return;
            }

            if(!isClient) {
                if (fadeIn)
                    return;
                if (fadeOut) {
                    updateStage(position.x, position.y, true);
                    return;
                }
            }

            float speedFactor = speed*Gdx.graphics.getDeltaTime();
            //if(!isClient)
            speedFactor *= 0.98573f;

            float dist = interPos.dst(position);

            if(dist <= speedFactor || dist >= 2f) { // updates to players position if its close enough or too far away
                updateStage(position.x, position.y, true);
                return;
            } // if not, interpolate
            Vector2 dir = new Vector2(position.x, position.y).sub(interPos);
            dir = dir.nor();
//            if(!isClient) // update direction here if its not client
//                direction = Direction.getDirection(MathUtils.round(dir.x), MathUtils.round(dir.y));

            Vector2 move = new Vector2(dir.x, dir.y).scl(speedFactor);
            Vector2 futurePos = new Vector2(interPos.x, interPos.y).add(move);
            updateStage(futurePos.x, futurePos.y, true);
        }

        public void dispose() {
            //remove(); // remove itself from stage
            Gdx.app.postRunnable(() -> {
                spriteSheet.dispose();
                debugTex.dispose();
                debugCircle.dispose();
            });
        }

        public static Character toCharacter(GameRegister.Character charData) {
            return new Character(charData.name, charData.id, charData.role_level, charData.x, charData.y,
                    charData.maxHealth, charData.health, charData.speed);
        }

        /**
         * Prepares character data to be sent to server with useful data
         * @return  the Register character class containing the data to send
         */
        public GameRegister.Character toSendToServer() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.id;
            charData.x = this.x; charData.y = this.y; charData.name = this.name;
            return charData;
        }

        public Vector2 getCenter() {
            return centerPos;
        }

    }

    public static class Wall extends Entity {
        private final int tileId;
        public int tileX, tileY;
        public TiledMapTile tile;
        public int wallId;
        public boolean isWalkable;
        private boolean assetsLoaded;

        public Wall(int wallId, int tileId, TiledMapTile tile, int tileX, int tileY) {
            super();
            this.tile = tile;
            this.tileX = tileX;
            this.tileY = tileY;
            this.wallId = wallId;
            this.tileId = tileId;
            this.isWalkable = tile.getProperties().get("walkable", Boolean.class);
            this.isObfuscator = tile.getProperties().get("obfuscator", Boolean.class);
            this.contextId = this.wallId;
            this.type = Type.WALL;

            float tileWidth = TEX_WIDTH * unitScale;
            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileWidth = tileWidth * 0.5f;
            float halfTileHeight = tileHeight * 0.5f;
            this.drawPos.x =  (tileX * halfTileWidth) + (tileY * halfTileWidth);
            this.drawPos.y = (tileY * halfTileHeight) - (tileX * halfTileHeight) + tileHeight; // adjust to match origin to tile origin
            this.uId = EntityController.getInstance().generateUid();

            hitBox = new Polygon(new float[] {
                    0, 0,
                    tile.getTextureRegion().getRegionWidth()* unitScale * 0.5f, 0,
                    tile.getTextureRegion().getRegionWidth()* unitScale * 0.5f, tile.getTextureRegion().getRegionHeight()* unitScale * 0.6f,
                    0, tile.getTextureRegion().getRegionHeight()* unitScale * 0.6f });

            hitBox.setPosition(this.drawPos.x + halfTileWidth*0.5f, this.drawPos.y + halfTileHeight*0.8f);

            if(tile.getProperties().get("type", String.class).equals("portal")) {
                this.type = Type.PORTAL;
                this.isInteractive = true;

                hitBox = new Polygon(new float[] {
                        0, 0,
                        tile.getTextureRegion().getRegionWidth()* unitScale * 0.5f, 0,
                        tile.getTextureRegion().getRegionWidth()* unitScale * 0.5f, tile.getTextureRegion().getRegionHeight()* unitScale,
                        0, tile.getTextureRegion().getRegionHeight()* unitScale });

                hitBox.setPosition(this.drawPos.x+ halfTileWidth*0.6f, this.drawPos.y - halfTileHeight*1.2f);
            }
//
//            this.hitBox.x = this.drawPos.x + halfTileWidth*0.5f;
//            this.hitBox.y = this.drawPos.y + halfTileHeight*0.8f;
//            this.hitBox.width = tile.getTextureRegion().getRegionWidth()* unitScale * 0.5f;
//            this.hitBox.height = tile.getTextureRegion().getRegionHeight()* unitScale * 0.6f;


            Gdx.app.postRunnable(() -> {
                debugCircle = getPixmapCircle(40, Color.BROWN,true);
                debugTex=new Texture(debugCircle);
                debugTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                assetsLoaded = true;
            });
        }

        public static Wall toWall(GameRegister.Wall wallUpdate) {
            return new Wall(wallUpdate.wallId, wallUpdate.tileId,  WorldMap.getInstance().getTileFromId(wallUpdate.tileId),
                    wallUpdate.tileX, wallUpdate.tileY);
        }
        //wall
        @Override
        public Vector2 getEntityCenter() {
            center.set(finalDrawPos.x + spriteW/2f, finalDrawPos.y + spriteH/2f);
            return center;
        }

        @Override
        public void takeDamage() {

        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(tile == null) return;

            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileHeight = tileHeight * 0.5f;

            applyEffects(batch); // apply entity effects before drawing

            this.finalDrawPos.x = this.drawPos.x;
            this.finalDrawPos.y = this.drawPos.y - halfTileHeight*1.38f;
            this.currentFrame = tile.getTextureRegion();
            batch.draw(tile.getTextureRegion(), this.finalDrawPos.x, this.finalDrawPos.y,
                    tile.getTextureRegion().getRegionWidth()* unitScale * edgeFactor,
                    tile.getTextureRegion().getRegionHeight()* unitScale * edgeFactor);
            if(CommonUI.enableDebugTex) {
                batch.end();
                shapeDebug.setProjectionMatrix(camera.combined);
                shapeDebug.begin(ShapeRenderer.ShapeType.Line);
                shapeDebug.setColor(Color.RED);
                shapeDebug.polygon(hitBox.getTransformedVertices());
                //shapeDebug.rect(hitBox.x, hitBox.y, hitBox.width, hitBox.height);
                shapeDebug.end();
                batch.begin();
            }

            // resets batch alpha for further rendering
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1f);//set alpha back to 1
        }

        @Override
        public void renderUI(SpriteBatch batch) {

        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            // walls are not selectable
        }

        @Override
        public int compareTo(Entity entity) {
            Vector2 e1Iso = this.drawPos;
            Vector2 e2Iso = entity.drawPos;
            float e1Depth = e1Iso.y;
            float e2Depth = e2Iso.y;

            if(e1Depth > e2Depth)
                return -1;
            else
                return 1;
        }
    }

    public static class Tree extends Entity {
        public final int spawnId;
        private final int treeId;
        public int tileX, tileY;
        public TiledMapTile tile;
        public int tileId;
        public boolean isWalkable;
        public String name;
        private boolean assetsLoaded;

        public Tree(int treeId, int spawnId, int tileId, TiledMapTile tile, String name, float maxHealth, float health, int tileX, int tileY, Polygon hitBox) {
            super();
            this.tile = tile;
            this.tileX = tileX;
            this.tileY = tileY;
            this.treeId = treeId;
            this.tileId = tileId;
            this.spawnId = spawnId;
            this.name = name;
            this.entityName = name;
            this.health = health;
            this.maxHealth = maxHealth;
            this.isWalkable = tile.getProperties().get("walkable", Boolean.class);
            this.isObfuscator = tile.getProperties().get("obfuscator", Boolean.class);
            this.contextId = this.spawnId;
            this.type = Type.TREE;
            this.isInteractive = true;
            this.isTargetAble = true;
            this.uId = EntityController.getInstance().generateUid();
            this.hitBox = hitBox;

            float tileWidth = TEX_WIDTH * unitScale;
            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileWidth = tileWidth * 0.5f;
            float halfTileHeight = tileHeight * 0.5f;
            this.drawPos.x =  (tileX * halfTileWidth) + (tileY * halfTileWidth);
            this.drawPos.y = (tileY * halfTileHeight) - (tileX * halfTileHeight) + tileHeight; // adjust to match origin to tile origin
            this.hitBox.setPosition(drawPos.x, drawPos.y - halfTileHeight*1.38f);
            //this.hitBox.setPosition(0,0);

            Gdx.app.postRunnable(() -> {
                debugCircle = getPixmapCircle(40, Color.BROWN,true);
                debugTex=new Texture(debugCircle);
                debugTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                nameLabel = new TypingLabel("{SIZE=55%}{COLOR=LIGHT_GRAY}"+this.name+"[%]", font);
                nameLabel.skipToTheEnd();
                assetsLoaded = true;
            });
        }

        public static Tree toTree(GameRegister.Tree treeUpdate) {
            return new Tree(treeUpdate.treeId, treeUpdate.spawnId, treeUpdate.tileId,  WorldMap.getInstance().getTileFromId(treeUpdate.tileId),
                    treeUpdate.name, treeUpdate.maxHealth, treeUpdate.health, treeUpdate.tileX, treeUpdate.tileY, new Polygon(treeUpdate.hitBox));
        }
        //tree
        @Override
        public Vector2 getEntityCenter() {
            center.set(finalDrawPos.x + spriteW/2f, finalDrawPos.y + spriteH/2f);
            return center;
        }

        @Override
        public void takeDamage() {
            health-=5f;
            if(health<=0) {
                this.tile = WorldMap.getInstance().getMap().getTileSets().getTile(this.tileId-1);
                this.isInteractive = false;
                this.isTargetAble = false;
                if(GameClient.getInstance().getClientCharacter().getTarget() != null &&
                        GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                    GameClient.getInstance().getClientCharacter().setTarget(null);
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(tile == null) return;

            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileHeight = tileHeight * 0.5f;

            applyEffects(batch); // apply entity effects before drawing

            this.finalDrawPos.x = this.drawPos.x;
            this.finalDrawPos.y = this.drawPos.y - halfTileHeight*1.38f;
            this.currentFrame = tile.getTextureRegion();
            spriteH = currentFrame.getRegionHeight() * unitScale;
            spriteW = currentFrame.getRegionWidth() * unitScale;
            this.centerPos.x = this.finalDrawPos.x + spriteW/2f;
            this.centerPos.y = this.finalDrawPos.y + spriteH/2f;
            batch.draw(tile.getTextureRegion(), this.finalDrawPos.x, this.finalDrawPos.y,
                    tile.getTextureRegion().getRegionWidth()* unitScale * edgeFactor,
                    tile.getTextureRegion().getRegionHeight()* unitScale * edgeFactor);
            if(CommonUI.enableDebugTex) {
                batch.end();
                shapeDebug.setProjectionMatrix(camera.combined);
                shapeDebug.begin(ShapeRenderer.ShapeType.Line);
                shapeDebug.setColor(Color.PINK);
                shapeDebug.polygon(hitBox.getTransformedVertices());
                //shapeDebug.rect(drawPos.x, drawPos.y, 1f, 45f/32f);
                shapeDebug.end();
                batch.begin();
            }

            // resets batch alpha for further rendering
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1f);//set alpha back to 1
        }

        @Override
        public void renderUI(SpriteBatch batch) {
            renderEntityTag(batch, this.finalDrawPos.x+unitScale*0.75f, this.finalDrawPos.y+unitScale*4.81f, 0.7f, 0.12f, Color.LIME);
        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            renderTargetCircle(batch, 0.033f, -0.3f,0.75f);
        }

        @Override
        public int compareTo(Entity entity) {
            Vector2 e1Iso = this.drawPos;
            Vector2 e2Iso = entity.drawPos;
            float e1Depth = e1Iso.y;
            float e2Depth = e2Iso.y;

            if(e1Depth > e2Depth)
                return -1;
            else
                return 1;
        }
    }

    public static class Creature extends Entity {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 15, FRAME_ROWS = 16;
        private static final float ANIM_BASE_INTERVAL = 0.25175f; // the base interval between anim frames
        public Character target;
        public boolean assetsLoaded = false;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        public Direction direction; // direction of this creature
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int creatureId;
        public int spawnId;
        public Vector2 position, interPos, lastVelocity, startPos;
        public float x, y, speed, range;
        public State state;
        public TypingLabel outlineLabel;
        public int targetId;

        public Creature(String name, int creatureId, int spawnId, float x, float y, float maxHealth, float health, float speed, float attackSpeed,
                        float range, float lastVelocityX, float lastVelocityY, String stateName, int targetId) {
            super();
            this.name = name; this.entityName = name;
            this.creatureId = creatureId; this.spawnId = spawnId;
            this.uId = EntityController.getInstance().generateUid();
            this.position = new Vector2(x, y);
            this.interPos = new Vector2(x, y);
            this.drawPos =  new Vector2(x, y); // updates draw position for correct draw ordering
            this.lastVelocity = new Vector2(lastVelocityX, lastVelocityY);
            this.startPos = new Vector2(interPos.x, interPos.y);
            this.state = State.getStateFromName(stateName);
            this.target = GameClient.getInstance().getOnlineCharacters().get(targetId);
            this.x = x;
            this.y = y;
            this.maxHealth = maxHealth;
            this.health = health;
            this.speed = speed;
            this.attackSpeed = attackSpeed;
            this.range = range;
            this.contextId = this.spawnId;
            this.type = Type.CREATURE;
            this.isInteractive = true;
            this.isTargetAble = true;
            direction = Direction.SOUTHWEST;
            nameLabel = new TypingLabel("{SIZE=55%}{COLOR=LIGHT_GRAY}"+this.name+"[%]", font);
            nameLabel.skipToTheEnd();
            outlineLabel = new TypingLabel("{SIZE=55%}{COLOR=black}"+this.name+"[%]", font);
            outlineLabel.skipToTheEnd();
            // initialize animators
            walk = new ConcurrentHashMap<>();
            attack = new ConcurrentHashMap<>();
            idle = new ConcurrentHashMap<>();

            Gdx.app.postRunnable(() -> { // must wait for libgdx UI thread
                debugCircle = getPixmapCircle(10, Color.RED,true);
                debugTex=new Texture(debugCircle);
                debugTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Load the sprite sheet as a Texture
                spriteSheet = new Texture(Gdx.files.internal("spritesheet/creature/"+this.creatureId+".png"));
                spriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Use the split utility method to create a 2D array of TextureRegions. This is
                // possible because this sprite sheet contains frames of equal size and they are
                // all aligned.
                TextureRegion[][] tmp = TextureRegion.split(spriteSheet,
                        spriteSheet.getWidth() / FRAME_COLS,
                        spriteSheet.getHeight() / FRAME_ROWS);
                // Place the regions into a 1D array in the correct order, starting from the top
                // left, going across first. The Animation constructor requires a 1D array.

                float walkFactor = (20f / (speed*0.85f)) * unitScale;

                TextureRegion[] frames = new TextureRegion[8];
                int index = 0;
                for (int j = 0; j < 8; j++) {
                    frames[index++] = tmp[12][j];
                }
                walk.put(Direction.SOUTHWEST, new Animation<>(ANIM_BASE_INTERVAL * walkFactor, frames));
                walk.put(Direction.SOUTH, new Animation<>(ANIM_BASE_INTERVAL  * walkFactor, frames));
                walk.put(Direction.WEST, new Animation<>(ANIM_BASE_INTERVAL  * walkFactor, frames));
                index = 0;
                frames = new TextureRegion[8];
                for (int j = 0; j < 8; j++) {
                    frames[index++] = tmp[13][j];
                }
                walk.put(Direction.SOUTHEAST, new Animation<>(ANIM_BASE_INTERVAL  * walkFactor, frames));
                walk.put(Direction.EAST, new Animation<>(ANIM_BASE_INTERVAL * walkFactor, frames));
                index = 0;
                frames = new TextureRegion[8];
                for (int j = 0; j < 8; j++) {
                    frames[index++] = tmp[14][j];
                }
                walk.put(Direction.NORTHWEST, new Animation<>(ANIM_BASE_INTERVAL * walkFactor, frames));
                walk.put(Direction.NORTH, new Animation<>(ANIM_BASE_INTERVAL * walkFactor, frames));
                index = 0;
                frames = new TextureRegion[8];
                for (int j = 0; j < 8; j++) {
                    frames[index++] = tmp[15][j];
                }
                walk.put(Direction.NORTHEAST, new Animation<>(ANIM_BASE_INTERVAL * walkFactor, frames));
                spriteW = frames[0].getRegionWidth() * unitScale;
                spriteH = frames[0].getRegionHeight() * unitScale;
                // idle
                frames = new TextureRegion[4];
                index = 0;
                for (int j = 11; j < 15; j++) {
                    frames[index++] = tmp[12][j];
                }
                idle.put(Direction.SOUTHWEST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                idle.put(Direction.SOUTH, new Animation<>(ANIM_BASE_INTERVAL, frames));
                idle.put(Direction.WEST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                index = 0;
                frames = new TextureRegion[4];
                for (int j = 11; j < 15; j++) {
                    frames[index++] = tmp[13][j];
                }
                idle.put(Direction.SOUTHEAST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                idle.put(Direction.EAST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                index = 0;
                frames = new TextureRegion[4];
                for (int j = 11; j < 15; j++) {
                    frames[index++] = tmp[14][j];
                }
                idle.put(Direction.NORTHWEST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                idle.put(Direction.NORTH, new Animation<>(ANIM_BASE_INTERVAL, frames));
                index = 0;
                frames = new TextureRegion[4];
                for (int j = 11; j < 15; j++) {
                    frames[index++] = tmp[15][j];
                }
                idle.put(Direction.NORTHEAST, new Animation<>(ANIM_BASE_INTERVAL, frames));
                // attack
                frames = new TextureRegion[15];
                index = 0;
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[0][j];
                }
                attack.put(Direction.SOUTHWEST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                attack.put(Direction.SOUTH, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                attack.put(Direction.WEST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[1][j];
                }
                attack.put(Direction.SOUTHEAST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                attack.put(Direction.EAST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[2][j];
                }
                attack.put(Direction.NORTHWEST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                attack.put(Direction.NORTH, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[3][j];
                }
                attack.put(Direction.NORTHEAST, new Animation<>(ANIM_BASE_INTERVAL / (attackSpeed * 0.075f), frames));
                // reset the elapsed animation time
                animTime = 0f;

                assetsLoaded = true;
            });
        }

        public Vector2 getCenter() {
            return new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
        }

        public void update(float x, float y, float lastVelocityX, float lastVelocityY, float speed, float attackSpeed, String stateName, Character target) {
            this.x = x;
            this.y = y;
            this.position.x = x;
            this.position.y = y;
            this.lastVelocity.x = lastVelocityX;
            this.lastVelocity.y = lastVelocityY;
            this.attackSpeed = attackSpeed;
            this.speed = speed;
            this.state = State.getStateFromName(stateName);
            this.target = target;
        }
        // creature
        @Override
        public Vector2 getEntityCenter() {
            center.set(finalDrawPos.x + spriteW/2f, finalDrawPos.y + spriteH/2f);
            return center;
        }

        @Override
        public void takeDamage() {
            health-=5f;
            if(health<0) {
                health = maxHealth;
                if(GameClient.getInstance().getClientCharacter().getTarget() != null &&
                        GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                    GameClient.getInstance().getClientCharacter().setTarget(null);
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(GameClient.getInstance().getClientCharacter() == null) return;

            Map<Direction, Animation<TextureRegion>> currentAnimation = walk; // only walk anim for now

            switch (state) {
                case IDLE:
                    currentAnimation = idle;
                    break;
                case FOLLOWING:
                case IDLE_WALKING:
                    currentAnimation = walk;
                    break;
                case ATTACKING:
                    currentAnimation = attack;
                    break;
                default:
                    break;
            }

            // try to predict position if its following this player
            if(target != null && target.id == GameClient.getInstance().getClientCharacter().id) {
                this.x = target.drawPos.x - spriteW/2f;
                this.y = target.drawPos.y - spriteH/3f;
                this.position.x = target.drawPos.x - spriteW/2f;
                this.position.y = target.drawPos.y - spriteH/3f;
            }

            if(interPos.dst(position) != 0f && Common.entityInterpolation) {
                interpolate2();
            }

            animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

            //centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
            this.centerPos.x = this.interPos.x + spriteW/2f;
            this.centerPos.y = this.interPos.y + spriteH/2f;
            this.drawPos.x = this.interPos.x + spriteW/2f;
            this.drawPos.y = this.interPos.y + spriteH/3f;

            // tries to predict direction if its targeting someone and target moved but not enough to interpolate
            if(target != null) {
                Vector2 dir = new Vector2(target.drawPos.x, target.drawPos.y).sub(this.drawPos);
                dir = dir.nor();
                direction = Direction.getDirection(Math.round(dir.x), Math.round(dir.y));
            }

            applyEffects(batch); // apply entity effects before drawing

            // Get current frame of animation for the current stateTime
            currentFrame = currentAnimation.get(direction).getKeyFrame(animTime, true);

            this.finalDrawPos = this.interPos;
            batch.draw(currentFrame, this.interPos.x, this.interPos.y, currentFrame.getRegionWidth()* unitScale,
                    currentFrame.getRegionHeight()* unitScale);//, 60, 60,
            //                120, 120, 1f, 1f, 0);

            //System.out.println("wolf: " + currentFrame.getRegionWidth() + " / wolf: " + currentFrame.getRegionHeight());
            spriteH = currentFrame.getRegionHeight()* unitScale;
            spriteW = currentFrame.getRegionWidth()* unitScale;

            // updates hitbox (for mouse/touch interactions) TODO - CREATURES SHOULD USE PREDEFINED HITBOXES! USE TILED!
            hitBox = new Polygon(new float[]{
                    finalDrawPos.x+spriteW*0.23f, finalDrawPos.y+ spriteH*0.32f, // down-left
                    finalDrawPos.x+spriteW*0.23f, finalDrawPos.y+spriteH*0.71f, // up-left
                    finalDrawPos.x+spriteW*0.75f, finalDrawPos.y+spriteH*0.71f, // up-right
                    finalDrawPos.x+spriteW*0.75f, finalDrawPos.y+ spriteH*0.32f}); // down-right

//            if(CommonUI.enableDebugTex)
//                batch.draw(debugTex, this.drawPos.x, this.drawPos.y, 2* unitScale, 2* unitScale);

            if(CommonUI.enableDebugTex) {
                batch.end();
                shapeDebug.setProjectionMatrix(camera.combined);
                shapeDebug.begin(ShapeRenderer.ShapeType.Line);
                shapeDebug.setColor(Color.PINK);
                shapeDebug.polygon(hitBox.getTransformedVertices());
                //shapeDebug.rect(drawPos.x, drawPos.y, 1f, 45f/32f);
                shapeDebug.end();
                batch.begin();
            }

//            // draw creature tag
//            nameLabel.getFont().scale(tagScale, tagScale);
//
//            nameLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f), this.centerPos.y + spriteH/2f + 8f,
//                    nameLabel.getWidth(), nameLabel.getHeight());
//            outlineLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f)-tagScale, this.centerPos.y + spriteH/2f + 8f-tagScale,
//                    nameLabel.getWidth() +tagScale, nameLabel.getHeight()+tagScale);
//
//            outlineLabel.draw(batch, 1.0f);
//            nameLabel.draw(batch, 1.0f);
//
//            nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);

            // resets batch alpha for further rendering
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1f);//set alpha back to 1
        }

        // interpolates inbetween position vector
        private void interpolate() {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(interPos.dst(position) == 0f) return; // nothing to interpolate

            //float speedFactor = speed*Gdx.graphics.getDeltaTime();
            float deltaX = lastVelocity.x / (Gdx.graphics.getFramesPerSecond() / GameRegister.clientTickrate);
            float deltaY = lastVelocity.y / (Gdx.graphics.getFramesPerSecond() / GameRegister.clientTickrate);
            Vector2 factor = new Vector2(deltaX, deltaY);

            if(factor.len() > 0.02f)
                System.out.println(Gdx.graphics.getFramesPerSecond());

            if(interPos.dst(position) <= factor.len()) {
                interPos.x = position.x; interPos.y = position.y;
                lastVelocity.x = 0; lastVelocity.y = 0;
                return;
            }

            interPos.x += deltaX;
            interPos.y += deltaY;
        }

        private void interpolate2() {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(interPos.dst(position) == 0f) return; // nothing to interpolate

            float speedFactor = this.speed * Gdx.graphics.getDeltaTime();

            // adjust for client prediction if this is the client player
            float clientOffset = 0f;
            if(target != null && target.id == GameClient.getInstance().getClientCharacter().id)
                clientOffset = range + speedFactor;

            if(interPos.dst(position) <= speedFactor + clientOffset) { // walks towards position until its close enough
                //updateStage(position.x, position.y, true);
                //interPos.x = position.x; interPos.y = position.y;
                lastVelocity.x = 0; lastVelocity.y = 0;
                // predict state (only if it is the target)
                if(target != null  && target.id == GameClient.getInstance().getClientCharacter().id)
                    state = State.ATTACKING;
                return;
            } // if not, interpolate

            Vector2 dir = new Vector2(position.x, position.y).sub(interPos);
            dir = dir.nor();
            direction = Direction.getDirection(Math.round(dir.x), Math.round(dir.y));
            Vector2 move = new Vector2(dir.x, dir.y).scl(speedFactor);
            Vector2 futurePos = new Vector2(interPos.x, interPos.y).add(move);
            this.interPos.x = futurePos.x; this.interPos.y = futurePos.y;
            // predict state (only if it is the target)
            if(target != null  && target.id == GameClient.getInstance().getClientCharacter().id)
                state = State.FOLLOWING;
            //updateStage(futurePos.x, futurePos.y, true);
        }

        public void dispose() { // SpriteBatches and Textures must always be disposed
            spriteSheet.dispose();
            debugTex.dispose();
            debugCircle.dispose();
        }

        // transform a creature update from server into a creature object from client
        public static Creature toCreature(GameRegister.UpdateCreature creatureUpdate) {
            return new Creature(creatureUpdate.name, creatureUpdate.creatureId, creatureUpdate.spawnId,
                    creatureUpdate.x, creatureUpdate.y, creatureUpdate.maxHealth, creatureUpdate.health,
                    creatureUpdate.speed, creatureUpdate.attackSpeed,
                    creatureUpdate.range, creatureUpdate.lastVelocityX, creatureUpdate.lastVelocityY,
                    creatureUpdate.state, creatureUpdate.targetId);
        }

        @Override
        public void renderUI(SpriteBatch batch) {
            renderEntityTag(batch, this.finalDrawPos.x, this.finalDrawPos.y-unitScale*7.51f, 0.7f, 0.12f, Color.RED);
        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            renderTargetCircle(batch, 0.5f, 1.0f,0.85f);
        }

        @Override
        public int compareTo(Entity entity) {
            Vector2 e1Iso = this.drawPos;
            Vector2 e2Iso = entity.drawPos;
            float e1Depth = e1Iso.y;
            float e2Depth = e2Iso.y;

            if(e1Depth > e2Depth)
                return -1;
            else
                return 1;
        }
    }
}