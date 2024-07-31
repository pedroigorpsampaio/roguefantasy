package com.mygdx.game.ui;

import static com.mygdx.game.entity.Entity.assetManager;
import static com.mygdx.game.ui.CommonUI.FLOATING_CHAT_TEXT_SCALE;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.entity.WorldMap;
import com.mygdx.game.util.Common;

import org.w3c.dom.css.Rect;

/**
 * Represents a floating text in the game
 * contains methods and information to be able to render texts over entities
 * representing damage texts, exp points, player messages texts and others.
 */
public class FloatingText implements Pool.Poolable {
    private final Font font;
    private final TypingLabel label;
    private final Skin skin;
    public String text;   // text to be drawn
    public Vector2 position; // where to draw
    public float scale; // scale of the text
    public Entity creator;
    public float lifetime; // lifetime in seconds of this floating text
    public float animSpeed; // animation speed of this floating text
    public boolean alive;
    public float elapsed;
    public float offsetX, offsetY;
    private boolean followCreator;

    public FloatingText() {
        this.alive = false;
        this.skin = assetManager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        this.font = skin.get("floatingTextFont", Font.class); // gets typist font with icons
        this.label = new TypingLabel("", font);
        this.label.skipToTheEnd();
        this.text = "";
        this.offsetX = 0f;
        this.offsetY = 0f;
        this.creator = null;
        this.position = new Vector2(0,0);
        this.lifetime = 0;
        this.scale = 1;
        this.animSpeed = 0;
    }

    public void init(Entity creator, String text, float offsetX, float offsetY, float scale, Color color, float lifeTime, float animSpeed, boolean followCreator) {
        StringBuilder sb = new StringBuilder();
        sb.append("{COLOR=#");
        sb.append(color.toString());
        sb.append("}");
        sb.append(text);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.followCreator = followCreator;
        this.creator = creator;
        this.scale = scale;
        this.label.setText(String.valueOf(sb));
        this.position.set(creator.getEntityCenter().x + offsetX,  creator.getEntityCenter().y + offsetY);
        this.lifetime = lifeTime;
        this.animSpeed = animSpeed;
        this.alive = true;
        this.elapsed = 0f;
    }

    public void die() {
        alive = false;
        elapsed = 0f;
    }

    /**
     * Renders floating text
     * @param batch the batch to drawn
     * @param delta the last delta time
     */
    public void render(SpriteBatch batch, float delta) {
        elapsed += delta;

        this.offsetY += delta * animSpeed; // the animation of floating text is simply going up (when there is animation speed)

        if(followCreator)
            this.position.set(creator.getEntityCenter().x + offsetX,  creator.getEntityCenter().y + offsetY);
        else {
            if(animSpeed != 0f)
                this.position.y += this.offsetY;
        }

        if(elapsed >= lifetime) // if it has surpassed its lifetime, die
            die();

        if(alive) {
            float tagScale = WorldMap.unitScale * 0.25f * scale;
//            font.scale(tagScale, tagScale);
//            font.drawText(batch, text, position.x - font.cellWidth*tagScale/2f, position.y - font.cellHeight*tagScale/2f);
//            font.scaleTo(font.originalCellWidth, font.originalCellHeight);


            //nameLabel.setColor(nameColor);
            label.setBounds(position.x -  (label.getWidth()*tagScale/2f),
                    position.y + label.getFont().originalCellHeight/2f, label.getWidth(), label.getHeight());
            label.getFont().scale(tagScale, tagScale);

            label.draw(batch, 1.0f);

            label.getFont().scaleTo(label.getFont().originalCellWidth, label.getFont().originalCellHeight);

            // makes sure batch color is reset with correct alpha
            //batch.setColor(Color.WHITE);
        }
    }

    Rectangle hitBox = new Rectangle();
    public Rectangle getHitBox() {
        float tagScale = WorldMap.unitScale * 0.25f * scale;
        hitBox.set(label.getX(), label.getY() - label.getFont().originalCellHeight/2f, label.getWidth()*tagScale, getHeight()/FLOATING_CHAT_TEXT_SCALE);
        return hitBox;
    }

    @Override
    public void reset() {
        this.text = "";
        this.label.setText("");
        this.position.set(0, 0);
        this.lifetime = 0;
        this.animSpeed = 0;
        this.offsetX = 0f;
        this.offsetY = 0f;
        this.elapsed = 0f;
        this.creator = null;
        this.followCreator = false;
        this.alive = false;
        this.scale = 1f;
    }

    public void moveByY(float val) {
        this.offsetY += val;
    }

    public float getHeight() {
        TypingLabel tmpLabel = new TypingLabel("test", font);
        float tagScale = WorldMap.unitScale * 0.25f * scale;
        return tmpLabel.getLineHeight(0)*tagScale * FLOATING_CHAT_TEXT_SCALE;
    }
}
