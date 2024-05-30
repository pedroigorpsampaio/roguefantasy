package com.mygdx.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.kotcrab.vis.ui.VisUI;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.ui.GameScreen;

import java.util.Random;

public class Component {
    public static AssetManager assetManager; // screen asset manager
    public static Stage stage; // screen stage

    public static class Character {
        private final Skin skin;
        private final Font font;
        private Texture spriteSheet;
        private TextureRegion spriteTex;
        public String name;
        public int id, role_level;
        public float x;
        public float y;
        public TypingLabel nameLabel;
        private Sprite sprite;
        private Image spriteImg;
        private Vector2 centerPos;

        public Character(String name, int id, int role_level, float x, float y) {
            this.name = name; this.id = id; this.role_level = role_level;
            this.x = x; this.y = y;
            skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
            font = skin.get("emojiFont", Font.class); // gets typist font with icons
            if(role_level == 0)
                nameLabel = new TypingLabel("{SIZE=95%}{COLOR=SKY}"+this.name+"{ENDRAINBOW}", font);
            else if(role_level == 4)
                nameLabel = new TypingLabel("{SIZE=95%}{COLOR=RED}{RAINBOW=0.5;0.3}[[adm]"+this.name+"{ENDRAINBOW}", font);
            System.out.println(nameLabel.getWidth());
            nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + nameLabel.getHeight()/2f, nameLabel.getWidth(), nameLabel.getHeight());
            nameLabel.skipToTheEnd();
            stage.addActor(nameLabel);
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
                //sprite = new Sprite(spriteTex);
                stage.addActor(spriteImg);
                centerPos = new Vector2(this.x+spriteImg.getWidth()/2f, this.y+spriteImg.getHeight()/2f);
            });

        }

        // updates based on character changes
        public void update(float x, float y) {
            Gdx.app.postRunnable(() -> {
                this.x = x;
                this.y = y;
                float screenX = this.x - spriteImg.getWidth()/2f;
                float screenY = this.y - spriteImg.getHeight()/2f;
                nameLabel.setBounds(this.x - nameLabel.getWidth()/2f, this.y + spriteImg.getHeight()/1.5f+nameLabel.getHeight(), nameLabel.getWidth(), nameLabel.getHeight());
                spriteImg.setBounds(screenX, screenY, spriteImg.getWidth(), spriteImg.getHeight());
                centerPos.x = this.x;
                centerPos.y = this.y;
            });
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

        /**
         * called to remove scene2d - stage components
         */
        public void removeFromStage() {
            nameLabel.remove();
            spriteImg.remove();
        }

        public void dispose() {
            spriteSheet.dispose();
        }

        public static Character toCharacter(GameRegister.Character charData) {
            return new Character(charData.name, charData.id, charData.role_level, charData.x, charData.y);
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
