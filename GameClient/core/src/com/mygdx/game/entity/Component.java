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
import com.mygdx.game.util.Common;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.text.Position;

public class Component {
    public static AssetManager assetManager; // screen asset manager
    public static Stage stage; // screen stage

    public static class Character extends Actor {
        private final Skin skin;
        private final Font font;
        public AtomicLong lastRequestId;
        private Texture spriteSheet;
        private TextureRegion spriteTex;
        public String name;
        public int id, role_level;
        public Vector2 position, interPos;
        public float x, y, speed;
        public ConcurrentSkipListMap<Long, Vector2> posQueue;
        public TypingLabel nameLabel;
        private Image spriteImg;
        private Vector2 centerPos;
        private Vector2 startPos; // used for interpolation

        public Character(String name, int id, int role_level, float x, float y, float speed) {
            this.name = name; this.id = id; this.role_level = role_level;
            this.position = new Vector2(x, y); this.interPos = new Vector2(x, y);
            this.x = x; this.y = y; this.speed = speed; lastRequestId = new AtomicLong(0);
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
                centerPos = new Vector2(this.x+spriteImg.getWidth()/2f, this.y+spriteImg.getHeight()/2f);
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
                    float screenX = this.x - spriteImg.getWidth()/2f;
                    float screenY = this.y - spriteImg.getHeight()/2f;
                    nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(),
                            nameLabel.getWidth(), nameLabel.getHeight());
                    spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                    centerPos.x = this.x;
                    centerPos.y = this.y;
                });
            }
        }

        public void move(Vector2 movement) {
            this.x += movement.x;
            this.y += movement.y;
            this.position.add(movement);
            if(!Common.entityInterpolation) {
                Gdx.app.postRunnable(() -> {
                    float screenX = this.x - spriteImg.getWidth()/2f;
                    float screenY = this.y - spriteImg.getHeight()/2f;
                    nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(),
                            nameLabel.getWidth(), nameLabel.getHeight());
                    spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                    centerPos.x = this.x;
                    centerPos.y = this.y;
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
            float screenX = x - spriteImg.getWidth()/2f;
            float screenY = y - spriteImg.getHeight()/2f;
            nameLabel.setBounds(x - nameLabel.getWidth()/2f, y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
            spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
            centerPos.x = x;
            centerPos.y = y;
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

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if(spriteImg == null) return; // sprite not loaded

            nameLabel.draw(batch, parentAlpha);
            spriteImg.draw(batch, parentAlpha);

            // if there is interpolation to be made and entity interpolation is active, interpolate
            if(interPos.dst(position) != 0f && Common.entityInterpolation)
                interpolate();
        }

        // interpolates stage assets to player current position
        private void interpolate() {
            if(spriteImg == null) return; // sprite not loaded
            if(interPos.dst(position) == 0f) return; // nothing to interpolate

            float speedFactor = speed*Gdx.graphics.getDeltaTime();
            if(interPos.dst(position) <= speedFactor) { // updates to players position if its close enough
                updateStage(position.x, position.y, true);
                return;
            } // if not, interpolate
            Vector2 dir = new Vector2(position.x, position.y).sub(interPos);
            dir = dir.nor();
            Vector2 move = new Vector2(dir.x, dir.y).scl(speedFactor);
            Vector2 futurePos = new Vector2(interPos.x, interPos.y).add(move);
            updateStage(futurePos.x, futurePos.y, true);
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
