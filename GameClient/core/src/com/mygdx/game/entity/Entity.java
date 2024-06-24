package com.mygdx.game.entity;

import static com.mygdx.game.ui.CommonUI.getPixmapCircle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.ui.GameScreen;
import com.mygdx.game.util.Common;

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
    protected Pixmap debugCircle; // for debug
    protected Texture debugTex; // for debug
    public static float tagScale = WorldMap.unitScale * 0.66f;
    public Vector2 drawPos = new Vector2(0,0); // position to draw this entity
    public int uId;
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

    public static class Character extends Entity {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 6, FRAME_ROWS = 24;
        private static final float ANIM_BASE_INTERVAL = 0.25175f; // the base interval between anim frames
        private final Skin skin;
        private final Font font;
        public AtomicLong lastRequestId;
        public boolean assetsLoaded = false;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int id, role_level, outfitId;
        public Vector2 position, interPos;
        public Direction direction;
        public float x, y, speed, spriteW, spriteH;
        public TypingLabel nameLabel, outlineLabel;
        private Vector2 centerPos;
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
                spriteW = (spriteSheet.getWidth() / FRAME_COLS) * WorldMap.unitScale;
                spriteH = (spriteSheet.getHeight() / FRAME_ROWS) * WorldMap.unitScale;
                TextureRegion[][] tmp = TextureRegion.split(spriteSheet,
                        spriteSheet.getWidth() / FRAME_COLS,
                        spriteSheet.getHeight() / FRAME_ROWS);

                float atkFactor = 20f / (speed*0.33f); // TODO: ATTK SPEEd
                float walkFactor = (20f / (speed*0.33f)) * WorldMap.unitScale;

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

        // updates based on character changes
        public void update(float x, float y) {
            this.x = x;
            this.y = y;
            this.position.x = x;
            this.position.y = y;
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
        private void updateStage(float x, float y, boolean interpolation) {
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
        }

        /**
         * Checks if movement is possible (not blocked by walls, unwalkable tiles, other collisions...)
         * @param msg   the move character message containing the desired movement to be checked
         * @return  true if move is possible, false otherwise
         */
        public boolean isMovePossible(GameRegister.MoveCharacter msg) {
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                if(!WorldMap.isWithinWorldBounds(futurePos) ||
                        !WorldMap.getInstance().isWalkable(futurePos))
                    return false;
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(this.x, this.y).add(moveVec);

                if(!WorldMap.isWithinWorldBounds(futurePos) ||
                        !WorldMap.getInstance().isWalkable(futurePos))
                    return false;
            }
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
            float degree = 2f;

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
            while(tries < 45) { // search for new angle to move
                if(WorldMap.isWithinWorldBounds(futurePos) &&
                        WorldMap.getInstance().isWalkable(futurePos)) { // found a new movement to make searching clockwise
                    newMove.x = moveVec.x;
                    newMove.y = moveVec.y;
                    break;
                }
                if(WorldMap.isWithinWorldBounds(futurePosCounter) &&
                        WorldMap.getInstance().isWalkable(futurePosCounter)) { // found a new movement to make searching counter-clockwise
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

            // animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

            // Get current frame of animation for the current stateTime
            TextureRegion currentFrame = currentAnimation.get(direction).getKeyFrame(animTime, true);
            batch.draw(currentFrame, this.interPos.x + spriteW/12f, this.interPos.y + spriteH/12f, currentFrame.getRegionWidth()*WorldMap.unitScale,
                    currentFrame.getRegionHeight()*WorldMap.unitScale);

            //System.out.println("playerW: " + currentFrame.getRegionWidth() + " / playerH: " + currentFrame.getRegionHeight());
            spriteH = currentFrame.getRegionHeight()*WorldMap.unitScale;
            spriteW = currentFrame.getRegionWidth()*WorldMap.unitScale;
            centerPos = new Vector2(this.interPos.x + spriteW/2f + spriteW/20f, this.interPos.y + spriteH/2f + spriteH/20f);
            this.drawPos.x = this.interPos.x + spriteW/2f + spriteW/20f;
            this.drawPos.y = this.interPos.y + spriteH/12f + spriteH/20f;

            batch.draw(debugTex, this.drawPos.x, this.drawPos.y, 2*WorldMap.unitScale, 2*WorldMap.unitScale);

            // draw player tag

            nameLabel.getFont().scale(tagScale, tagScale);

            nameLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/1.75f), this.centerPos.y + spriteH/2f + 8f,
                    nameLabel.getWidth(), nameLabel.getHeight());
            outlineLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/1.75f)-tagScale, this.centerPos.y + spriteH/2f + 8f-tagScale,
                    nameLabel.getWidth() +tagScale, nameLabel.getHeight()+tagScale);

            outlineLabel.draw(batch, 1.0f);
            nameLabel.draw(batch, 1.0f);

            nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);
        }

        // interpolates stage assets to player current position
        private void interpolate(boolean isClient) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(interPos.dst(position) == 0f) return; // nothing to interpolate
            if(isClient && GameClient.getInstance().isPredictingRecon.get()) return;

            float speedFactor = speed*Gdx.graphics.getDeltaTime();
            //if(!isClient)
            speedFactor *= 0.97573f;

            if(interPos.dst(position) <= speedFactor) { // updates to players position if its close enough
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
        public long spawnId;
        public Vector2 position, interPos, lastVelocity, startPos;
        public float x, y, speed, attackSpeed, range, spriteW, spriteH;
        public State state;
        public TypingLabel nameLabel, outlineLabel;
        public int targetId;
        private Vector2 centerPos;

        public Creature(String name, int creatureId, long spawnId, float x, float y, float speed, float attackSpeed,
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

                float walkFactor = (20f / (speed*0.85f)) * WorldMap.unitScale;

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
                spriteW = frames[0].getRegionWidth() * WorldMap.unitScale;
                spriteH = frames[0].getRegionHeight() * WorldMap.unitScale;
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

            // Get current frame of animation for the current stateTime
            TextureRegion currentFrame = currentAnimation.get(direction).getKeyFrame(animTime, true);

            batch.draw(currentFrame, this.interPos.x, this.interPos.y, currentFrame.getRegionWidth()*WorldMap.unitScale,
                    currentFrame.getRegionHeight()*WorldMap.unitScale);//, 60, 60,
            //                120, 120, 1f, 1f, 0);

            //System.out.println("wolf: " + currentFrame.getRegionWidth() + " / wolf: " + currentFrame.getRegionHeight());
            spriteH = currentFrame.getRegionHeight()*WorldMap.unitScale;
            spriteW = currentFrame.getRegionWidth()*WorldMap.unitScale;

            batch.draw(debugTex, this.drawPos.x, this.drawPos.y, 2*WorldMap.unitScale, 2*WorldMap.unitScale);


            // draw creature tag
            nameLabel.getFont().scale(tagScale, tagScale);

            nameLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f), this.centerPos.y + spriteH/2f + 8f,
                    nameLabel.getWidth(), nameLabel.getHeight());
            outlineLabel.setBounds(this.centerPos.x - (nameLabel.getWidth()*tagScale/2f)-tagScale, this.centerPos.y + spriteH/2f + 8f-tagScale,
                    nameLabel.getWidth() +tagScale, nameLabel.getHeight()+tagScale);

            outlineLabel.draw(batch, 1.0f);
            nameLabel.draw(batch, 1.0f);

            nameLabel.getFont().scaleTo(nameLabel.getFont().originalCellWidth, nameLabel.getFont().originalCellHeight);
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
        public int compareTo(Entity entity) {
            Vector2 e1Iso = this.drawPos;
            Vector2 e2Iso = entity.drawPos;
            float e1Depth = e1Iso.y;
            float e2Depth = e2Iso.y;

            //System.out.println("Wolf Depth: " + e1Depth + " / Player: " + e2Depth);

            if(e1Depth > e2Depth)
                return -1;
            else
                return 1;
        }
    }
}