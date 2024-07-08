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
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.ui.CommonUI;
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
    public String entityName = "";
    public boolean isClient = false;
    private ShapeRenderer shapeRenderer; // for debug
    protected Pixmap debugCircle; // for debug
    protected Texture debugTex; // for debug
    public static float tagScale = unitScale * 0.66f;
    static float rectOffsetUp = 0.2f, rectOffsetDown = 0.2f, rectOffsetLeft = 0.4f, rectOffsetRight = 0.4f;
    public Polygon hitBox = new Polygon();
    public Vector2 drawPos = new Vector2(0,0); // position to draw this entity
    public Vector2 finalDrawPos = new Vector2(0,0); // final position to draw this entity (for y-ordering, sometimes drawPos differs from final draw pos)
    public int uId; // this entity unique id
    public int contextId; // the id of this id in context, based on what type of entity it is
    public Type type; // the type of this entity, specifying its child class
    public GameRegister.EntityState state; // the server current state known of this entity
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
    protected TextureRegion currentFrame = null;

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

    public enum Type {
        CREATURE,
        NPC,
        WALL,
        PORTAL,
        TREE,
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
     * Entities that belongs to the world can be drawn in the world using this method
     * @param batch     the batch to draw entity
     */
    public abstract void render(SpriteBatch batch) ;

    /**
     * Entities that belongs to the world can draw UI on UI camera if needed by implementing this method
     * @param batch     the batch to draw UI
     * @param worldCamera   the world camera reference for necessary projections to screen position
     */
    public abstract void renderUI(SpriteBatch batch, OrthographicCamera worldCamera) ;

    /**
     * This method is called when entity is target of client player to render UI for target selected
     * @param batch the batch to draw UI of target selected
     */
    public abstract void renderTargetUI(SpriteBatch batch);

    public static class Character extends Entity {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 6, FRAME_ROWS = 24;
        private static final float ANIM_BASE_INTERVAL = 0.25175f; // the base interval between anim frames
        private final Skin skin;
        private final Font font;
        private Polygon collider = new Polygon(); // this players collider
        public AtomicLong lastRequestId;
        public boolean assetsLoaded = false;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        public Entity target = null; // the current character target - selected entity
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int id, role_level, outfitId;
        public Vector2 position, interPos;
        public Direction direction;
        public float x, y, speed, spriteW, spriteH;
        public TypingLabel nameLabel, outlineLabel;
        private Vector2 centerPos = new Vector2();
        private Vector2 startPos; // used for interpolation
        public ConcurrentSkipListMap<Long, Vector2> bufferedPos;
        private Map.Entry<Long, Vector2> oldestEntry;
        public CopyOnWriteArrayList<EntityInterPos> buffer = new CopyOnWriteArrayList<>();
        private Vector2 goalPos;
        private float tIntElapsed;

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

        public static class EntityInterPos {
            public long timestamp;
            public Vector2 position;
        }

        public Character(String name, int id, int role_level, float x, float y, float speed) {
            this.name = name; this.id = id; this.role_level = role_level; this.outfitId = 0;
            this.entityName = name;
            this.uId = EntityController.getInstance().generateUid();
            this.position = new Vector2(x, y); this.interPos = new Vector2(x, y);
            this.drawPos = new Vector2(x, y);
            this.x = x; this.y = y; this.speed = speed; lastRequestId = new AtomicLong(0);
            this.direction = Direction.SOUTH;
            this.bufferedPos = new ConcurrentSkipListMap<>();
            this.contextId = this.id;
            this.type = Type.CHARACTER;
            this.isInteractive = true;
            this.isTargetAble = true;
            //this.bufferedPos.putIfAbsent(System.currentTimeMillis(), new Vector2(this.x, this.y));
            startPos = new Vector2(this.x, this.y);
            skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            font = skin.get("emojiFont", Font.class); // gets typist font with icons
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

                float atkFactor = 20f / (speed*0.33f); // TODO: ATTK SPEEd
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
                isTeleporting = true;
                teleportInAnim = true;
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

        @Override
        public void renderUI(SpriteBatch batch, OrthographicCamera worldCamera) {

        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            if(this.id != GameClient.getInstance().getClientCharacter().id) { // if its other character, render selected ui
                float tileHeight = TEX_HEIGHT * unitScale;
                float halfTileHeight = tileHeight * 0.5f;
                float targetScale = 0.75f;
                batch.end();
                Gdx.gl.glLineWidth(4);
                shapeDebug.setProjectionMatrix(camera.combined);
                shapeDebug.begin(ShapeRenderer.ShapeType.Line);
                shapeDebug.setColor(Color.RED);
                shapeDebug.ellipse(finalDrawPos.x+TEX_WIDTH*unitScale*0.11f, finalDrawPos.y - halfTileHeight*0.22f,
                        TEX_WIDTH* unitScale*targetScale, TEX_HEIGHT*unitScale*targetScale, 120);
                //shapeDebug.rect(drawPos.x, drawPos.y, 1f, 45f/32f);
                shapeDebug.end();
                Gdx.gl.glLineWidth(1);
                batch.begin();
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;

            animTime += Gdx.graphics.getDeltaTime(); // accumulates anim timer
            Map<Direction, Animation<TextureRegion>> currentAnimation = idle;

            if(interPos.dst(position) != 0f && Common.entityInterpolation) {
                currentAnimation = walk;
                if (GameClient.getInstance().getClientCharacter().id == this.id)
                    interpolate(true);
                else
                    interpolate(false);
            } else { // not walking, set idle animations
                currentAnimation = idle;
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
            batch.draw(currentFrame, this.finalDrawPos.x, this.finalDrawPos.y, currentFrame.getRegionWidth()* unitScale,
                    currentFrame.getRegionHeight()* unitScale);

            //System.out.println("playerW: " + currentFrame.getRegionWidth() + " / playerH: " + currentFrame.getRegionHeight());

            // updates positions and dimensions
            spriteH = currentFrame.getRegionHeight()* unitScale;
            spriteW = currentFrame.getRegionWidth()* unitScale;
            centerPos = new Vector2(this.interPos.x + spriteW/2f + spriteW/20f, this.interPos.y + spriteH/2f + spriteH/20f);
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

            // renders player tag
            nameLabel.getFont().scale(tagScale, tagScale);

            nameLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/1.75f), this.centerPos.y + spriteH/2f + 8f,
                    nameLabel.getWidth(), nameLabel.getHeight());
            outlineLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/1.75f)-tagScale, this.centerPos.y + spriteH/2f + 8f-tagScale,
                    nameLabel.getWidth() +tagScale, nameLabel.getHeight()+tagScale);

            float fontAlpha = alpha;
            if(alpha>1.0f) fontAlpha = 1.0f;
            outlineLabel.draw(batch, fontAlpha);
            nameLabel.draw(batch, fontAlpha);

            nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);

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
            return new Character(charData.name, charData.id, charData.role_level, charData.x, charData.y, charData.speed);
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
        public void renderUI(SpriteBatch batch, OrthographicCamera worldCamera) {

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
        public float health;
        public boolean isWalkable;
        private boolean assetsLoaded;

        public Tree(int treeId, int spawnId, int tileId, TiledMapTile tile, float health, int tileX, int tileY, Polygon hitBox) {
            this.tile = tile;
            this.tileX = tileX;
            this.tileY = tileY;
            this.treeId = treeId;
            this.tileId = tileId;
            this.spawnId = spawnId;
            this.health = health;
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
                assetsLoaded = true;
            });
        }

        public static Tree toTree(GameRegister.Tree treeUpdate) {
            return new Tree(treeUpdate.treeId, treeUpdate.spawnId, treeUpdate.tileId,  WorldMap.getInstance().getTileFromId(treeUpdate.tileId),
                                treeUpdate.health, treeUpdate.tileX, treeUpdate.tileY, new Polygon(treeUpdate.hitBox));
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
        public void renderUI(SpriteBatch batch, OrthographicCamera worldCamera) {
        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileHeight = tileHeight * 0.5f;
            float targetScale = 0.75f;
            batch.end();
            Gdx.gl.glLineWidth(4);
            shapeDebug.setProjectionMatrix(camera.combined);
            shapeDebug.begin(ShapeRenderer.ShapeType.Line);
            shapeDebug.setColor(Color.RED);
            shapeDebug.ellipse(finalDrawPos.x+TEX_WIDTH*unitScale*0.155f, finalDrawPos.y - halfTileHeight*0.26f,
                    TEX_WIDTH* unitScale*targetScale, TEX_HEIGHT*unitScale*targetScale, 120);
            //shapeDebug.rect(drawPos.x, drawPos.y, 1f, 45f/32f);
            shapeDebug.end();
            Gdx.gl.glLineWidth(1);
            batch.begin();
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
        private Skin skin;
        private Font font;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        public Direction direction; // direction of this creature
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int creatureId;
        public int spawnId;
        public Vector2 position, interPos, lastVelocity, startPos;
        public float x, y, speed, attackSpeed, range, spriteW, spriteH;
        public State state;
        public TypingLabel nameLabel, outlineLabel;
        public int targetId;
        private Vector2 centerPos;

        public Creature(String name, int creatureId, int spawnId, float x, float y, float speed, float attackSpeed,
                        float range, float lastVelocityX, float lastVelocityY, String stateName, int targetId) {
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
            this.speed = speed;
            this.attackSpeed = attackSpeed;
            this.range = range;
            this.contextId = this.spawnId;
            this.type = Type.CREATURE;
            this.isInteractive = true;
            this.isTargetAble = true;
            direction = Direction.SOUTHWEST;
            skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            font = skin.get("emojiFont", Font.class); // gets typist font with icons
            nameLabel = new TypingLabel("{SIZE=55%}{COLOR=brick}"+this.name+"[%]", font);
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

            centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
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

            // draw creature tag
            nameLabel.getFont().scale(tagScale, tagScale);

            nameLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f), this.centerPos.y + spriteH/2f + 8f,
                    nameLabel.getWidth(), nameLabel.getHeight());
            outlineLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f)-tagScale, this.centerPos.y + spriteH/2f + 8f-tagScale,
                    nameLabel.getWidth() +tagScale, nameLabel.getHeight()+tagScale);

            outlineLabel.draw(batch, 1.0f);
            nameLabel.draw(batch, 1.0f);

            nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);

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
                    creatureUpdate.x, creatureUpdate.y, creatureUpdate.speed, creatureUpdate.attackSpeed,
                    creatureUpdate.range, creatureUpdate.lastVelocityX, creatureUpdate.lastVelocityY,
                    creatureUpdate.state, creatureUpdate.targetId);
        }

        @Override
        public void renderUI(SpriteBatch batch, OrthographicCamera worldCamera) {


        }

        @Override
        public void renderTargetUI(SpriteBatch batch) {
            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileHeight = tileHeight * 0.5f;
            float targetScale = 0.8f;
            batch.end();
            Gdx.gl.glLineWidth(4);
            shapeDebug.setProjectionMatrix(camera.combined);
            shapeDebug.begin(ShapeRenderer.ShapeType.Line);
            shapeDebug.setColor(Color.RED);
            shapeDebug.ellipse(finalDrawPos.x+TEX_WIDTH*unitScale*0.6f, finalDrawPos.y + halfTileHeight*2.25f,
                    TEX_WIDTH* unitScale*targetScale, TEX_HEIGHT*unitScale*targetScale, 120);
            //shapeDebug.rect(drawPos.x, drawPos.y, 1f, 45f/32f);
            shapeDebug.end();
            Gdx.gl.glLineWidth(1);
            batch.begin();
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