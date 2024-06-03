package com.mygdx.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class Component {
    public static AssetManager assetManager; // screen asset manager
    public static Stage stage; // screen stage

    public static class Character extends Actor {
        private final Skin skin;
        private final Font font;
        public AtomicLong lastRequestId;
        public float lastServerPosX, lastServerPosY;
        private Texture spriteSheet;
        private TextureRegion spriteTex;
        public String name;
        public int id, role_level;
        public float x, y, speed;
        public float virtualX, virtualY; // used for interpolation
        public ConcurrentSkipListMap<Long, Vector2> posQueue;
        public TypingLabel nameLabel;
        private Sprite sprite;
        private Image spriteImg;
        private Vector2 centerPos;
        private Vector2 startPos; // used for interpolation
        private Vector2 goalPos;
        private Map.Entry<Long, Vector2> oldestEntry;

        public Character(String name, int id, int role_level, float x, float y, float speed) {
            this.name = name; this.id = id; this.role_level = role_level;
            this.x = x; this.y = y; this.speed = speed; lastRequestId = new AtomicLong(0);
            lastServerPosX = x; lastServerPosY = y;
            startPos = new Vector2(this.x, this.y);
            posQueue = new ConcurrentSkipListMap<>();
            skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            font = skin.get("emojiFont", Font.class); // gets typist font with icons
            if(role_level == 0)
                nameLabel = new TypingLabel("{SIZE=95%}{COLOR=SKY}"+this.name+"{ENDRAINBOW}", font);
            else if(role_level == 4)
                nameLabel = new TypingLabel("{SIZE=95%}{COLOR=RED}{RAINBOW=0.5;0.3}[[adm]"+this.name+"{ENDRAINBOW}", font);
            nameLabel.skipToTheEnd();
            stage.addActor(this);
            //getStage().addActor(nameLabel);
//            Random rand = new Random();
//            // Will produce only bright / light colours:
//            float r = rand.nextFloat() / 2f + 0.5f;
//            float g = rand.nextFloat() / 2f + 0.5f;
//            float b = rand.nextFloat() / 2f + 0.5f;
//            this.color = new Color(r,g,b,1);
            // char sprite placeholder

            //stage.addActor(img);
            //pixmap.dispose();
            //texture.dispose();
            Gdx.app.postRunnable(() -> {
                // process the result, e.g. add it to an Array<Result> field of the ApplicationListener.
                spriteSheet = new Texture(Gdx.files.internal("img/SpriteSheet.png"));
                spriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                spriteTex = new TextureRegion(spriteSheet, 0, 0, 23, 36);
                spriteImg = new Image(spriteTex);
                float screenX = this.x - spriteImg.getWidth()/2f;
                float screenY = this.y - spriteImg.getHeight()/2f;
                nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
                spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                //sprite = new Sprite(spriteTex);
                //getStage().addActor(spriteImg);
                centerPos = new Vector2(this.x+spriteImg.getWidth()/2f, this.y+spriteImg.getHeight()/2f);
            });

        }

        // updates based on character changes
        public void update(float x, float y) {
            this.x = x;
            this.y = y;
            this.virtualX = this.x;
            this.virtualY = this.y;
            Gdx.app.postRunnable(() -> {
                float screenX = this.x - spriteImg.getWidth()/2f;
                float screenY = this.y - spriteImg.getHeight()/2f;
                nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
                spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                centerPos.x = this.x;
                centerPos.y = this.y;
            });
        }

        public void move(Vector2 movement) {
            this.x += movement.x;
            this.y += movement.y;
            this.virtualX = this.x;
            this.virtualY = this.y;
            Gdx.app.postRunnable(() -> {
                float screenX = this.x - spriteImg.getWidth()/2f;
                float screenY = this.y - spriteImg.getHeight()/2f;
                nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
                spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                centerPos.x = this.x;
                centerPos.y = this.y;
            });
        }

        // adds a new moving position to the move queue to be interpolated by render
        public void addMovePos(Long requestId, Vector2 movePos) {
            synchronized (posQueue) {
                posQueue.putIfAbsent(requestId, movePos);
            }
        }

        public void virtualMove(GameRegister.MoveCharacter msg) {
//            Gdx.app.postRunnable(() -> {
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed*GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                if(touchPos.dst(futurePos) <= 10f) // close enough, do not move anymore
                    return;

                virtualX += deltaVec.x; virtualY += deltaVec.y;
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed*GameRegister.clientTickrate());
                virtualX += moveVec.x; virtualY += moveVec.y;
            }
            //           });
        }

        /**
         * Given a movement msg that is to be sent to the server, tries
         * to predict the outcome before receiving server answer updating character pos
         * @param msg   the message containing the raw movement from inputs
         */
        public void predictMovement(GameRegister.MoveCharacter msg) {
            //Gdx.app.postRunnable(() -> {
                if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                    Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                    Vector2 charPos = new Vector2(this.x, this.y);
                    Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                    deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                    Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                    if (touchPos.dst(futurePos) <= 10f) // close enough, do not move anymore
                        return;

                    //this.move(deltaVec);
                    if (!GameRegister.entityInterpolation)
                        this.move(deltaVec);
                    else if (!GameClient.getInstance().isPredictingRecon.get())
                        addMovePos(msg.requestId, futurePos);
                    //posQueue.add(futurePos);
                    //else
                    //this.move(deltaVec);
                } else { // wasd movement already has direction in it, just normalize and scale
                    Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                    Vector2 charPos = new Vector2(this.x, this.y);
                    Vector2 futurePos = new Vector2(charPos).add(moveVec);
                    //this.move(moveVec);
                    if (!GameRegister.entityInterpolation)
                        this.move(moveVec);
                    else if (!GameClient.getInstance().isPredictingRecon.get())
                        addMovePos(msg.requestId, futurePos);
                    //posQueue.add(futurePos);
//                else
//                    this.move(moveVec);
                }
            //});
        }

        public void predictMovementNoBlocking(GameRegister.MoveCharacter msg) {
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.speed * GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                if (touchPos.dst(futurePos) <= 10f) // close enough, do not move anymore
                    return;

                //this.move(deltaVec);
                if (!GameRegister.entityInterpolation)
                    this.move(deltaVec);
                else if (!GameClient.getInstance().isPredictingRecon.get())
                    addMovePos(msg.requestId, futurePos);
                //posQueue.add(futurePos);
                //else
                //this.move(deltaVec);
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.speed * GameRegister.clientTickrate());
                Vector2 charPos = new Vector2(this.x, this.y);
                Vector2 futurePos = new Vector2(charPos).add(moveVec);
                //this.move(moveVec);
                if (!GameRegister.entityInterpolation)
                    this.move(moveVec);
                else if (!GameClient.getInstance().isPredictingRecon.get())
                    addMovePos(msg.requestId, futurePos);
                //posQueue.add(futurePos);
//                else
//                    this.move(moveVec);
            }
        }

        public void render(SpriteBatch batch) {
//            Pixmap pixmap = new Pixmap(22, 22, Pixmap.Format.RGBA8888);
//            pixmap.setColor(color);
//            pixmap.fillCircle(0, 0, 42);
//            Texture texture = new Texture(pixmap);
//            batch.begin();
//            batch.draw(texture, this.x, this.y);
//            batch.end();
//            pixmap.dispose();
//            texture.dispose();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if(spriteImg == null) return; // sprite not loaded

            // if there is positions to be moved in queue, interpolate
            interpolate(); // TODO INTERPOLATION DIFFERS IF ITS NOT CLIENT

            nameLabel.draw(batch, parentAlpha);
            spriteImg.draw(batch, parentAlpha);

            System.out.println("pos client: " + this.x + " / " + this.y);
            //System.out.println(this.lastRequestId + " / " + GameClient.getInstance().getRequestsCounter().get(GameRegister.MoveCharacter.class));

            //batch.draw(nameLabel, 400, 90, 900, 600);
        }

        Interpolation ease = Interpolation.pow2Out;
        float lifeTime = GameRegister.clientTickrate();
        float elapsed = 0f;

        private void interpolate() {
            if(spriteImg == null) return; // sprite not loaded
            if(GameClient.getInstance().isPredictingRecon.get()) return;
            if(posQueue.size() == 0) return; // only proceeds if there is a queued pos to go to
            if(this.id != GameClient.getInstance().getClientCharacter().id) return;

            // get oldest position not achieved
            synchronized (posQueue) {
                oldestEntry = posQueue.firstEntry();
                goalPos = oldestEntry.getValue();
            }

            elapsed += Gdx.graphics.getDeltaTime();

            float fps = Gdx.graphics.getFramesPerSecond();
            float cps = GameRegister.clientTickrate;
            float ratio = cps / fps;
            float deltaMove = Gdx.graphics.getDeltaTime() / fps;
            float deltaSpeed = this.speed * GameRegister.clientTickrate();

            Vector2 deltaPos = new Vector2(goalPos.x - this.x, goalPos.y - this.y);

//            System.out.println(ratio + "/" + Gdx.graphics.getDeltaTime());
//
//            deltaPos.scl(deltaSpeed*ratio);
//            this.move(deltaPos);
//
//            nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(),
//                                         nameLabel.getWidth(), nameLabel.getHeight());
//
//            if(new Vector2(this.x, this.y).dst(goalPos) <= 10f) // close enough, pop head of queue
//                posQueue.remove();

            float progress = Math.min(1f, elapsed/lifeTime);   // 0 -> 1
            if(progress < 1f) {
                //float alpha = ease.apply(progress);
                //float startPosX = charPos.x - nameLabel.getWidth()/2f;
                //float moveX = this.x - lastX;
                float newPosX = startPos.x + ((goalPos.x - startPos.x) * progress);
                //float startPosY = charPos.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight();
                //float moveY = this.y - lastY;
                float newPosY = startPos.y + ((goalPos.y - startPos.y) * progress);
//                this.x = newPosX;
//                this.y = newPosY;
//                this.virtualX = this.x;
//                this.virtualY = this.y;
                this.update(newPosX, newPosY);
            } else {
                elapsed = 0f;
//                this.x = goalPos.x;
//                this.y = goalPos.y;
//                this.virtualX = this.x;
//                this.virtualY = this.y;
                synchronized (posQueue) {
                    posQueue.remove(oldestEntry.getKey());

//                    if(posQueue.size() == 0 && (this.id == GameClient.getInstance().getClientCharacter().id)) {
//                        System.out.println(this.lastRequestId + " / " + GameClient.getInstance().getRequestsCounter().get(GameRegister.MoveCharacter.class));
//                        if(this.x != lastServerPosX || this.y != lastServerPosY) {
////                            this.x = lastServerPosX;
////                            this.y = lastServerPosY;
//                            this.update(lastServerPosX, lastServerPosY);
//                        }
//                    }
                }

                startPos = new Vector2(this.x, this.y);

                //this.update(goalPos.x, goalPos.y);  // makes sure it finished in the goal position

                //System.out.println(new Vector2(this.x, this.y).dst(goalPos));
                //posQueue.remove();
            }

            // nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
            // float screenX = this.x - spriteImg.getWidth()/2f;
            // float screenY = this.y - spriteImg.getHeight()/2f;
            //spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
        }

        public void dispose() {
            remove(); // remove itself from stage
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
}
