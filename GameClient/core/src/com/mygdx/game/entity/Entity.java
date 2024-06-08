package com.mygdx.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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

public class Entity {
    public static AssetManager assetManager; // screen asset manager
    public static Stage stage; // screen stage
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
            for (Direction dir : Direction.values()) {
                if (dir.dirX == x && dir.dirY == y) {
                    return dir;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return null;
        }
    }

    public static class Character {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 6, FRAME_ROWS = 24;
        private static final float ANIM_BASE_INTERVAL = 0.00035f; // the base interval between anim frames
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

        public static class EntityInterPos {
            public long timestamp;
            public Vector2 position;
        }

        public Character(String name, int id, int role_level, float x, float y, float speed) {
            this.name = name; this.id = id; this.role_level = role_level; this.outfitId = 0;
            this.position = new Vector2(x, y); this.interPos = new Vector2(x, y);
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
                // Load the sprite sheet as a Texture
                spriteSheet = new Texture(Gdx.files.internal("spritesheet/outfit/"+this.outfitId+".png"));
                spriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Use the split utility method to create a 2D array of TextureRegions. This is
                // possible because this sprite sheet contains frames of equal size and they are
                // all aligned.
                spriteW = spriteSheet.getWidth() / FRAME_COLS;
                spriteH = spriteSheet.getHeight() / FRAME_ROWS;
                TextureRegion[][] tmp = TextureRegion.split(spriteSheet,
                        spriteSheet.getWidth() / FRAME_COLS,
                        spriteSheet.getHeight() / FRAME_ROWS);

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
                            idle.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 1:
                            walk.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 2:
                            attack.put(Direction.SOUTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 3:
                            idle.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 4:
                            walk.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 5:
                            attack.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 6:
                            idle.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 7:
                            walk.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 8:
                            attack.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 9:
                            idle.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 10:
                            walk.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 11:
                            attack.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 12:
                            idle.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 13:
                            walk.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 14:
                            attack.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 15:
                            idle.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 16:
                            walk.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 17:
                            attack.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 18:
                            idle.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 19:
                            walk.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 20:
                            attack.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 21:
                            idle.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 22:
                            walk.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
                            break;
                        case 23:
                            attack.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * speed, frames));
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

                if (touchPos.dst(futurePos) <= 10f) // close enough, do not move anymore
                    return;

                this.move(deltaVec);
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                this.move(moveVec);

            }
        }

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

            centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
            nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                    this.centerPos.y + spriteH/2f,
                    nameLabel.getWidth(), nameLabel.getHeight());
            outlineLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f - 1,
                    this.centerPos.y + spriteH/2f -1,
                    nameLabel.getWidth() +1, nameLabel.getHeight()+1);

            // animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time


            // Get current frame of animation for the current stateTime
            TextureRegion currentFrame = currentAnimation.get(direction).getKeyFrame(animTime, true);
            batch.draw(currentFrame, this.interPos.x, this.interPos.y);
            outlineLabel.draw(batch, 1.0f);
            nameLabel.draw(batch, 1.0f);
        }

        // interpolates stage assets to player current position
        private void interpolate(boolean isClient) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(interPos.dst(position) == 0f) return; // nothing to interpolate

            float speedFactor = speed*Gdx.graphics.getDeltaTime();
            if(!isClient) speedFactor *= 0.94f;

            if(interPos.dst(position) <= speedFactor) { // updates to players position if its close enough
                updateStage(position.x, position.y, true);
                return;
            } // if not, interpolate
            Vector2 dir = new Vector2(position.x, position.y).sub(interPos);
            dir = dir.nor();
//            int dirX = dir.x > 0 ? 1 : dir.x == 0 ? 0 : -1; // to update current direction
//            int dirY = dir.y > 0 ? 1 : dir.y == 0 ? 0 : -1;
            if(!isClient || GameScreen.isInputHappening()) // update direction
                direction = Direction.getDirection(MathUtils.round(dir.x), MathUtils.round(dir.y));

            Vector2 move = new Vector2(dir.x, dir.y).scl(speedFactor);
            Vector2 futurePos = new Vector2(interPos.x, interPos.y).add(move);
            updateStage(futurePos.x, futurePos.y, true);
        }

        public void dispose() {
            //remove(); // remove itself from stage
            Gdx.app.postRunnable(() -> {
                spriteSheet.dispose();
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

    public static class Creature {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 15, FRAME_ROWS = 16;
        private static final float ANIM_BASE_INTERVAL = 0.00075f; // the base interval between anim frames
        public boolean assetsLoaded = false;
        private Skin skin;
        private Font font;
        Texture walkSheet;
        Animation<TextureRegion> walkAnimation; // Must declare frame type (TextureRegion)
        float animTime; // A variable for tracking elapsed time for the animation
        public String name;
        public int creatureId;
        public long spawnId;
        public Vector2 position, interPos, lastVelocity, startPos;
        public float x, y, speed, spriteW, spriteH;
        public TypingLabel nameLabel, outlineLabel;

        public Creature(String name, int creatureId, long spawnId, float x, float y, float speed,
                        float lastVelocityX, float lastVelocityY) {
            this.name = name;
            this.creatureId = creatureId; this.spawnId = spawnId;
            this.position = new Vector2(x, y);
            this.interPos = new Vector2(x, y);
            this.lastVelocity = new Vector2(lastVelocityX, lastVelocityY);
            this.startPos = new Vector2(interPos.x, interPos.y);
            this.x = x;
            this.y = y;
            skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            font = skin.get("emojiFont", Font.class); // gets typist font with icons
            nameLabel = new TypingLabel("{SIZE=69%}{COLOR=brick}"+this.name+"[%]", font);
            nameLabel.skipToTheEnd();
            outlineLabel = new TypingLabel("{SIZE=69%}{COLOR=black}"+this.name+"[%]", font);
            outlineLabel.skipToTheEnd();

            Gdx.app.postRunnable(() -> { // must wait for libgdx UI thread
                // Load the sprite sheet as a Texture
                walkSheet = new Texture(Gdx.files.internal("spritesheet/creature/"+this.creatureId+".png"));
                walkSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                // Use the split utility method to create a 2D array of TextureRegions. This is
                // possible because this sprite sheet contains frames of equal size and they are
                // all aligned.
                TextureRegion[][] tmp = TextureRegion.split(walkSheet,
                        walkSheet.getWidth() / FRAME_COLS,
                        walkSheet.getHeight() / FRAME_ROWS);
                // Place the regions into a 1D array in the correct order, starting from the top
                // left, going across first. The Animation constructor requires a 1D array.
                TextureRegion[] walkFrames = new TextureRegion[8];
                int index = 0;
                for (int j = 0; j < 8; j++) {
                    walkFrames[index++] = tmp[12][j];
                }
                spriteW = walkFrames[0].getRegionWidth();
                spriteH = walkFrames[0].getRegionHeight();
                // Initialize the Animation with the frame interval and array of frames
                walkAnimation = new Animation<>(ANIM_BASE_INTERVAL * speed, walkFrames);

                // reset the elapsed animation time
                animTime = 0f;

                assetsLoaded = true;
            });
        }

        public Vector2 getCenter() {
            return new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
        }

        public void update(float x, float y, float lastVelocityX, float lastVelocityY, float speed) {
            this.x = x;
            this.y = y;
            this.position.x = x;
            this.position.y = y;
            this.lastVelocity.x = lastVelocityX;
            this.lastVelocity.y = lastVelocityY;
            this.speed = speed;
        }

        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;

            if(interPos.dst(position) != 0f && Common.entityInterpolation)
                interpolate2();

            animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time
            // updates label positions
            Vector2 centerPos = getCenter();
            outlineLabel.setBounds(getCenter().x - nameLabel.getWidth()/2f -1,
                    centerPos.y + spriteH/2.5f - 1,
                    nameLabel.getWidth() + 1, nameLabel.getHeight() +1);
            nameLabel.setBounds(getCenter().x - nameLabel.getWidth()/2f,
                    centerPos.y + spriteH/2.5f,
                    nameLabel.getWidth(), nameLabel.getHeight());

            // Get current frame of animation for the current stateTime
            TextureRegion currentFrame = walkAnimation.getKeyFrame(animTime, true);
           // batch.begin();
            batch.draw(currentFrame, this.interPos.x, this.interPos.y);//, 60, 60,
            //                120, 120, 1f, 1f, 0);
            outlineLabel.draw(batch, 1.0f);
            nameLabel.draw(batch, 1.0f);
            //batch.end();
        }

        // interpolates inbetween position vector
        private void interpolate() {
            if(walkAnimation == null) return; // sprite not loaded
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
            if(walkAnimation == null) return; // sprite not loaded
            if(interPos.dst(position) == 0f) return; // nothing to interpolate

            float speedFactor = this.speed * Gdx.graphics.getDeltaTime();

            if(interPos.dst(position) <= speedFactor) { // updates to players position if its close enough
                //updateStage(position.x, position.y, true);
                interPos.x = position.x; interPos.y = position.y;
                lastVelocity.x = 0; lastVelocity.y = 0;
                return;
            } // if not, interpolate
            Vector2 dir = new Vector2(position.x, position.y).sub(interPos);
            dir = dir.nor();
            Vector2 move = new Vector2(dir.x, dir.y).scl(speedFactor);
            Vector2 futurePos = new Vector2(interPos.x, interPos.y).add(move);
            this.interPos.x = futurePos.x; this.interPos.y = futurePos.y;
            //updateStage(futurePos.x, futurePos.y, true);
        }

        public void dispose() { // SpriteBatches and Textures must always be disposed
            walkSheet.dispose();
        }

        // transform a creature update from server into a creature object from client
        public static Creature toCreature(GameRegister.UpdateCreature creatureUpdate) {
            return new Creature(creatureUpdate.name, creatureUpdate.creatureId, creatureUpdate.spawnId,
                    creatureUpdate.x, creatureUpdate.y, creatureUpdate.speed,
                    creatureUpdate.lastVelocityX, creatureUpdate.lastVelocityY);
        }
    }
}
