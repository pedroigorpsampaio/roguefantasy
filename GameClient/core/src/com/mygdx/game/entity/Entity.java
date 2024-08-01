package com.mygdx.game.entity;

import static com.mygdx.game.entity.WorldMap.TEX_HEIGHT;
import static com.mygdx.game.entity.WorldMap.TEX_WIDTH;
import static com.mygdx.game.entity.WorldMap.edgeFactor;
import static com.mygdx.game.entity.WorldMap.unitScale;
import static com.mygdx.game.ui.ChatWindow.MAX_CHAT_MSG_CHARACTERS;
import static com.mygdx.game.ui.CommonUI.FLOATING_CHAT_TEXT_SCALE;
import static com.mygdx.game.ui.CommonUI.MAX_LINE_CHARACTERS_FLOATING_TEXT;
import static com.mygdx.game.ui.CommonUI.getPixmapCircle;
import static com.mygdx.game.ui.GameScreen.camera;
import static com.mygdx.game.ui.GameScreen.shapeDebug;

import com.badlogic.gdx.Application;
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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.ui.ChatWindow;
import com.mygdx.game.ui.CommonUI;
import com.mygdx.game.ui.FloatingText;
import com.mygdx.game.ui.GameScreen;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Jukebox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    public int contextId; // the id of this id in context, based on what type of entity it is and its the Id that should be used for identification in server
    public GameRegister.EntityType type; // the type of this entity, specifying its child class
    public GameRegister.EntityState state = GameRegister.EntityState.FREE; // the server current state known of this entity
    protected Timer interactionTimer=new Timer(); // timer that controls the interaction of entity
    public boolean isInteractive = false; // if this entity is interactive
    public boolean isTargetAble = false; // if this entity is target-able
    public boolean isObfuscator = false; // if this entity is obfuscator it will be rendered transparent when on top of player
    public boolean isTeleporting = false;
    public boolean isAlive = true;
    public boolean teleportInAnim = false;
    public boolean teleportOutAnim = false;
    public boolean finalDrawPosSet = false;
    public float alpha = 1f;
    protected boolean fadeIn = false;
    protected boolean fadeOut = false;
    protected float fadeSpeed = 2f;
    protected float animSizeFactor = 1f; // for yo yo size animation
    protected float animDir = 1; // for yo yo movement animation
    public float health, maxHealth, attackSpeed;
    public float spriteW;
    public float spriteH;
    protected TextureRegion currentFrame = null;
    protected Vector2 center = new Vector2();
    private GameRegister.AttackType attackType = null; // current attack type if any
    public boolean isRespawning = false;
    public ArrayList<FloatingText> currentFloatingTexts = new ArrayList<>();
    public FloatingText tagFloatingText = null;
    public Rectangle tagHitBox = new Rectangle();

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

    public boolean isTargetAble() {
        return (health > 0f && !isTeleporting &&
                state != GameRegister.EntityState.TELEPORTING_IN &&
                state != GameRegister.EntityState.TELEPORTING_OUT &&
                !fadeOut && !fadeIn);
    }

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
     * Updates current health and deals with the damages received
     * synchronizing the timing if needed (in case other client is the attacker)
     * @param healthState    the current health of this entity in server
     * @param damages   the list of damages received since last state
     */
    public void updateDamage(float healthState, ArrayList<GameRegister.Damage> damages) {
        if(this.uId != GameClient.getInstance().getClientUid()) {
            if (damages.size() == 0 && healthState <= 0f && isAlive)
                die(); // make sure entity is treated as dead when it should (if its not client)
        }

        /** update damages received **/
        for(int i = 0; i < damages.size() ; i++) {
            if(damages.get(i).attackerType == GameRegister.EntityType.CHARACTER &&
                    damages.get(i).attackerId != GameClient.getInstance().getClientCharacter().id) { // character attacker and was not client, delay
                float delay = 0;
                int finalI = i;

                if(GameClient.getInstance().getCharacter(damages.get(i).attackerId) == null) { // could not find attacker character in list
                    System.out.println("could not find character in map of characters: " + damages.get(i).attackerId);
                    continue;
                }

                delay = GameClient.getInstance().getCharacter(damages.get(i).attackerId).avgLatency/1000f;


                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        updateHealth(health-damages.get(finalI).value);
                        renderDamagePoint(damages.get(finalI));
                        if(finalI == damages.size()-1) { // be sure we are in sync if we at last damage from list
                            if(healthState != health)
                                updateHealth(healthState);
                        }
                    }
                }, delay);

            }
            else { // no delay needed since it was a damage triggered from client or from a non-character entity
                this.updateHealth(this.health-damages.get(i).value);
                this.renderDamagePoint(damages.get(i));
                if(i == damages.size()-1) { // be sure we are in sync if we at last damage from list
                    if(healthState != health)
                        updateHealth(healthState);
                }
            }
        }

        // make sure its is reset in case it respawns without recreating entity
        if(!isAlive && healthState > 0f) {
            if(type != GameRegister.EntityType.CHARACTER) // if its not player respawn immediately
                respawn();
            else if(!isRespawning) { // respawn player after respawn animation is done
                respawn();
            }
            return;
        }

        if(damages.size() == 0) // no damage nor respawn
            updateHealth(healthState); // just updates health
    }

    /**
     * Renders floating texts above entity
     * wrapping it based on max amount of characters per line defined
     * If necessary new floating text will be appended to older alive floating texts
     *
     * The text will not follow entity
     *
     * @param text  the text to render above entity
     */
    public void renderFloatingText(String text) {
        // remove token reserved characters
        text = text.replaceAll("[\\[\\]\\{\\}]", "");
        if (text.trim().length() == 0) { // after removing of token chars nothing remained, return
            return;
        }

        // makes sure its within max size
        if(text.length() > MAX_CHAT_MSG_CHARACTERS)
            text = text.substring(0, MAX_CHAT_MSG_CHARACTERS-1);

//        for(int i = 0 ; i < currentFloatingTexts.size(); i++) // remove old texts if it exists
//            currentFloatingTexts.get(i).die();

        Color c = (new Color(1,1,0.035f,1));
        float scale = FLOATING_CHAT_TEXT_SCALE;
        if(Gdx.app.getType() == Application.ApplicationType.Android) scale /= Common.ANDROID_VIEWPORT_SCALE;
        float offsetY = spriteH / 2.75f;
        float lifeTime = 3f + (8f * (text.length()*1.0f/ MAX_CHAT_MSG_CHARACTERS));

        // remder tag
        StringBuilder sb = new StringBuilder();
        sb.append(this.entityName);
        sb.append(":");
        FloatingText tag = GameScreen.getFloatingTextPool().obtain();

        // save old list if it exists
        float biggestAliveLifeTime = 0f;
        ArrayList<FloatingText> oldTexts = new ArrayList<>();
        for(int i = 0; i < currentFloatingTexts.size(); i++) {
            oldTexts.add(currentFloatingTexts.get(i));
            if(currentFloatingTexts.get(i).lifetime > biggestAliveLifeTime)
                biggestAliveLifeTime = currentFloatingTexts.get(i).lifetime;
        }

        // break line on max line characters creating new floating texts for each line
        //if(text.length() > MAX_LINE_CHARACTERS_FLOATING_TEXT) {
        List<String> strings = Common.splitEqually(text, MAX_LINE_CHARACTERS_FLOATING_TEXT);
        FloatingText txt = null;
        float firstY = -1, firstYTag = -1;

        for(int i = 0; i < strings.size(); i++) {
            txt = GameScreen.getFloatingTextPool().obtain();
            int nOffsets = strings.size() - i - 1;

            // adds "-" when a word is split in half
            if(i < strings.size()-1) {
                if(strings.get(i).charAt(strings.get(i).length()-1) != ' ' &&
                        strings.get(i+1).charAt(0) != ' ' && strings.get(i+1).charAt(0) != ','
                        && strings.get(i+1).charAt(0) != '.' && strings.get(i+1).charAt(0) != '-' &&
                        strings.get(i+1).charAt(0) != ';' && strings.get(i+1).charAt(0) != ':')
                    strings.set(i, strings.get(i) + "-");
            }

            float offset = offsetY+(txt.getHeight()*scale*nOffsets);

            if(firstY == -1)
                firstY = offset;

            if(biggestAliveLifeTime > lifeTime)
                lifeTime = biggestAliveLifeTime;

            txt.init(this, strings.get(i), 0, offset, scale,
                    c, lifeTime, 0f, false);
            GameScreen.addFloatingText(txt);
            currentFloatingTexts.add(txt);
        }

        for(int i = 0 ; i < oldTexts.size(); i++) { // update position and lifetime of old texts if exists
            int nOffsets = oldTexts.size() - i;
            float newOffY = firstY + oldTexts.get(i).getHeight()*nOffsets;
            if(firstYTag == -1)
                firstYTag = newOffY;

            oldTexts.get(i).position.set(getEntityCenter().x, getEntityCenter().y + newOffY);
        }

        if(oldTexts.size()==0)  firstYTag = firstY;

        if(tagFloatingText == null) { // creates tag if not there yet
            tag.init(this, String.valueOf(sb), 0, firstYTag+txt.getHeight(), scale,
                    c, lifeTime, 0f, false);
            GameScreen.addFloatingText(tag);
            tagFloatingText = tag;
        } else { // otherwise update its position and life time
            tagFloatingText.position.set(getEntityCenter().x, getEntityCenter().y + firstYTag+txt.getHeight());
            tagFloatingText.lifetime = lifeTime;
            tagFloatingText.elapsed = 0f;
        }
//        } else { // only one line - one floating text (plus tag) needed and no splitting necessary
//            FloatingText txt = GameScreen.getFloatingTextPool().obtain();
//            txt.init(this, text, 0, offsetY, scale,
//                    c, lifeTime, 0f, false);
//
////            tag.init(this, String.valueOf(sb), 0, offsetY+txt.getHeight(), scale,
////                    c, lifeTime, 0f, false);
////
////            GameScreen.addFloatingText(tag);
//            GameScreen.addFloatingText(txt);
//            //           currentFloatingTexts.add(tag);
//            currentFloatingTexts.add(txt);
//
//            float firstY =offsetY, firstYTag = -1;
//
//            for(int i = 0 ; i < oldTexts.size(); i++) { // update position and lifetime of old texts if exists
//                int nOffsets = oldTexts.size() - i;
//                float newOffY = firstY + txt.getHeight()*nOffsets;
//                if(firstYTag == -1)
//                    firstYTag = newOffY;
//                oldTexts.get(i).position.set(getEntityCenter().x, getEntityCenter().y + newOffY);
////                currentFloatingTexts.get(i).lifetime = lifeTime;
////                currentFloatingTexts.get(i).elapsed = 0f;
//            }
//
//            if(tagFloatingText == null) { // creates tag if not there yet
//                tag.init(this, String.valueOf(sb), 0, firstYTag+txt.getHeight(), scale,
//                        c, lifeTime, 0f, false);
//                GameScreen.addFloatingText(tag);
//                tagFloatingText = tag;
//            } else { // otherwise update its position and life time
//                tagFloatingText.position.set(getEntityCenter().x, getEntityCenter().y + firstYTag+txt.getHeight());
//                tagFloatingText.lifetime = lifeTime;
//                tagFloatingText.elapsed = 0f;
//            }
//        }
    }

    /**
     * Clears dead floating texts (should be called in render)
     */
    public void clearFloatingTexts() {
        for(int i = currentFloatingTexts.size()-1 ; i >= 0; i--) { // remove dead texts
            if(!currentFloatingTexts.get(i).alive) {
                float scale = FLOATING_CHAT_TEXT_SCALE;
                if(Gdx.app.getType() == Application.ApplicationType.Android) scale /= Common.ANDROID_VIEWPORT_SCALE;
                if(tagFloatingText!=null) // adjusts tag position if its showing
                    tagFloatingText.position.set(tagFloatingText.position.x, tagFloatingText.position.y - currentFloatingTexts.get(i).getHeight() * scale);
                // remove dead floating text
                currentFloatingTexts.remove(i);
            }
        }
        if(tagFloatingText!= null && !tagFloatingText.alive) // if tag is dead, null it
            tagFloatingText = null;
    }

    public void renderDamagePoint(GameRegister.Damage damage) {
        Color c = (new Color(1,0.35f,0.35f,1));
        if(this.type == GameRegister.EntityType.TREE)
            c = Color.LIGHT_GRAY;

        FloatingText dmgTxt = GameScreen.getFloatingTextPool().obtain();
        dmgTxt.init(this, String.valueOf(damage.value), 0, spriteH/2f,  1.2f,
                c, 1.5f, 0.75f, true);
        GameScreen.addFloatingText(dmgTxt);
    }

    public void renderDamagePoints(ArrayList<GameRegister.Damage> damages) {
        Color c = (new Color(1,0.35f,0.35f,1));
        if(this.type == GameRegister.EntityType.TREE)
            c = Color.LIGHT_GRAY;

        for(GameRegister.Damage dmg : damages) {
            FloatingText dmgTxt = GameScreen.getFloatingTextPool().obtain();
            dmgTxt.init(this, String.valueOf(dmg.value), 0, spriteH/2f,  1.2f,
                    c, 1.5f, 0.75f, true);
            GameScreen.addFloatingText(dmgTxt);
        }
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
                // only remove if its not client, else let client handle end of fade out also
                if(this.uId != GameClient.getInstance().getClientUid()) {
                    remove();
                    this.fadeOut = false;
                }

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
        synchronized (EntityController.getInstance().entities) {
            EntityController.getInstance().entities.remove(uId); // remove from list of entities to draw
        }
        GameClient.getInstance().removeEntity(uId); // remove from data list of entities
        if(GameClient.getInstance().getClientCharacter().getTarget() != null && // if this entity is target, remove it from target and hover vars
                GameClient.getInstance().getClientCharacter().getTarget().uId == uId) {
            WorldMap.hoverEntity = null;
            // first send msg to stop targetting in server
            GameClient.getInstance().requestInteraction(GameRegister.Interaction.STOP_INTERACTION,
                    GameClient.getInstance().getClientCharacter().getTarget().contextId,
                    GameClient.getInstance().getClientCharacter().getTarget().type);
            // then set client target to null
            GameClient.getInstance().getClientCharacter().setTarget(null);
        }
    }

    public TextureRegion getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Updates the current attack type of entity
     * @param attackType    the new attack type of the entity
     */
    public void updateAttack(GameRegister.AttackType attackType) {
        if(this.attackType!=attackType)
            this.attackType = attackType;
    }

    /**
     * For correct health updating of entities
     */
    public abstract void updateHealth(float health);

    /**
     * Dies - deals with entity death
     */
    public abstract void die();

    /**
     * Respawns - deals with entity respawn
     */
    public abstract void respawn();

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
        if(this.healthBar == null || this.healthBarBg == null) return;

        float eAlpha = alpha;
        if(alpha>1.0f) eAlpha = 1.0f;

        /** in case obfuscates client floating chat text, batch will have lower alpha **/
        if(batch.getColor().a < 1)
            eAlpha = batch.getColor().a;

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

        /**
         * hit box
         */
        tagHitBox.set(x+spriteW/2f - w/2f, y+spriteH -h/2, w, h * 4f);

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

    protected Jukebox.SoundType sfxAttackType;
    protected String sfxAttackName = "";
    private long lastAttackSFX = System.currentTimeMillis();

    /**
     *
     */
    public Timer.Task attackSFX = new Timer.Task() {
        @Override
        public void run() {
            if(sfxAttackName.equals("")) {
                this.cancel();
                return;
            }
            long now = System.currentTimeMillis();
            if(now - lastAttackSFX >= 1000f/attackSpeed ) {
                Jukebox.playSound(sfxAttackType, sfxAttackName);
                lastAttackSFX = System.currentTimeMillis();
            }
        }
    };

    public static class Character extends Entity {
        // Constant rows and columns of the sprite sheet
        private static final int FRAME_COLS = 6, FRAME_ROWS = 24;
        private static final float ANIM_BASE_INTERVAL = 0.25175f; // the base interval between anim frames
        public float avgLatency = 0;
        private Polygon collider = new Polygon(); // this players collider
        public AtomicLong lastRequestId;
        public boolean assetsLoaded = false;
        Texture spriteSheet;
        Map<Direction, Animation<TextureRegion>> walk, idle, attack; // animations
        private AtomicReference<Entity> target = new AtomicReference<>(); // the current character target - selected entity
        float animTime; // A variable for tracking elapsed time for the animation
        public String name, map, zone;
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
        private boolean isInteracting = false; // if character is interacting with an entity atm
        private long attackStartTs = System.currentTimeMillis();

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
            return target.get();
        }

        public synchronized void setTarget(Entity e) {
            if(getTarget() != null && e != null && e.uId == getTarget().uId) return; // same target as before, do nothing
            if(e != null && !e.isTargetAble) return; // if its not targetable return
            if(isUnmovable()) return; // in case unmovable(logging in, teleportin...) cannot interact
            if(e != null && (e.isUnmovable() || e.state == GameRegister.EntityState.TELEPORTING_IN
                    || e.state == GameRegister.EntityState.TELEPORTING_OUT)) return; // in case target cannot move, do not target it
            if(e != null && e.uId == GameClient.getInstance().getClientUid()) // makes sure to not attack itself

            if(e != null) e.resetYoYoAnim(); // if a entity is selected, reset its yo yo target animation

            stopInteraction(); // in case there was an interaction going on, stop it before starts a new one

            this.target.set(e); // set target to an entity or null representing client has no target atm

            if(e != null) {   // only starts interaction if a target is selected
                startInteraction();
            } else {
                stopInteractionTimer(); // makes sure everything is stopped when there is no target
            }
        }

        /**
         * This method is for other players when attacking state is received from server
         * @param targetId      the target context id
         * @param targetType    the type of the target entity
         */
        public void updateTarget(int targetId, GameRegister.EntityType targetType) {
            if(targetType == null) { // no target, let set target handle it
                setTarget(null);
                return;
            }

            // get correct target and let setTarget handle its
            Entity eTarget = null;

            switch(targetType) {
                case CHARACTER:
                    eTarget = GameClient.getInstance().getCharacter(targetId);
                    break;
                case CREATURE:
                    eTarget= GameClient.getInstance().getCreature(targetId);
                    break;
                case TREE:
                    eTarget = GameClient.getInstance().getTree(targetId);
                    break;
                default:
                    eTarget =null;
                    break;
            }

            //setTarget(eTarget);

            if(getTarget() != null && eTarget != null && eTarget.uId == getTarget().uId) return; // same target as before, do nothing
            if(eTarget != null && !eTarget.isTargetAble) return; // if its not targetable return
            if(isUnmovable()) return; // in case unmovable(logging in, teleportin...) cannot interact

            this.target.set(eTarget); // set target to an entity or null representing client has no target atm
            startInteraction();
        }

        //updates state of character
        public void updateState(GameRegister.UpdateCharacter msg) {
            GameRegister.EntityState newState = msg.character.state;
            if(this.state != newState) {
                if(this.state == GameRegister.EntityState.ATTACKING) // new state and player was attacking
                    this.setTarget(null); // stops attacking

                this.state = newState;

                if(state == GameRegister.EntityState.RESPAWNED) // just respawned server-side, update pos
                    this.spawnPlayer(msg.character.x, msg.character.y);

                if(state == GameRegister.EntityState.ATTACKING) // just started attacking, save timestamp
                    this.attackStartTs = System.currentTimeMillis();
            }
        }

        /**
         * Checks if there are the necessity to update server state for clients
         * in case of loss of synchronization - for clients only
         */
        public void stateCheck(GameRegister.UpdateCharacter msg) {
            if(this.state != msg.character.state) {
                if(GameClient.getInstance().getClientId() == this.id) {
                    //System.out.println(this.state + " sv: " + msg.character.state);
                    if(this.state == GameRegister.EntityState.ATTACKING) {
                        if(getTarget() != null && getTarget().isAlive && getTarget().isTargetAble && !getTarget().isTeleporting)
                            GameClient.getInstance().requestInteraction(GameRegister.Interaction.ATTACK_ENTITY, getTarget().contextId, getTarget().type);
                    } else if (this.state == GameRegister.EntityState.FREE) {
                        if(getTarget() != null)
                            GameClient.getInstance().requestInteraction(GameRegister.Interaction.STOP_INTERACTION, getTarget().contextId, getTarget().type);
                    }
                }
            }
        }

        public static class EntityInterPos {
            public long timestamp;
            public Vector2 position;
        }

        public Character(String name, int id, int role_level, float x, float y, float maxHealth, float health, float speed, float attackSpeed) {
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
            this.attackSpeed = attackSpeed;
            this.target.set(null);
            this.contextId = this.id;
            this.type = GameRegister.EntityType.CHARACTER;
            this.isInteractive = true;
            this.isTargetAble = true;
            this.map = "Nova Terra";
            this.zone = "Initium";
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
                            attack.put(Direction.SOUTH, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 3:
                            idle.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 4:
                            walk.put(Direction.SOUTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 5:
                            attack.put(Direction.SOUTHWEST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 6:
                            idle.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 7:
                            walk.put(Direction.WEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 8:
                            attack.put(Direction.WEST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 9:
                            idle.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 10:
                            walk.put(Direction.NORTHWEST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 11:
                            attack.put(Direction.NORTHWEST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 12:
                            idle.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 13:
                            walk.put(Direction.NORTH, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 14:
                            attack.put(Direction.NORTH, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 15:
                            idle.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 16:
                            walk.put(Direction.SOUTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 17:
                            attack.put(Direction.SOUTHEAST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 18:
                            idle.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 19:
                            walk.put(Direction.EAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 20:
                            attack.put(Direction.EAST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        case 21:
                            idle.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL, frames));
                            break;
                        case 22:
                            walk.put(Direction.NORTHEAST, new Animation<TextureRegion>(ANIM_BASE_INTERVAL * walkFactor, frames));
                            break;
                        case 23:
                            attack.put(Direction.NORTHEAST, new Animation<TextureRegion>((1f/attackSpeed)/frames.length, frames));
                            break;
                        default:
                            break;
                    }
                }

                // reset the elapsed animation time
                animTime = 0f;

                centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2.5f);
                nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                        this.centerPos.y + spriteH/2f,
                        nameLabel.getWidth(), nameLabel.getHeight());
                assetsLoaded = true;
            });
        }

        /**
         * Updates attack speed of character adjusting animation speed as well (should be used instead of changing attack speed var directly)
         * @param atkSpeed the new attack speed of the player (should be bigger than 0.5)
         */
        public void updateAtkSpeed(float atkSpeed) {
            if(atkSpeed <= 0.5) return;

            this.attackSpeed = atkSpeed;
            synchronized (attack) {
                Iterator<Animation<TextureRegion>> itr = attack.values().iterator();
                while(itr.hasNext()) {
                    Animation<TextureRegion> anim = itr.next();
                    anim.setFrameDuration((1f/attackSpeed)/anim.getKeyFrames().length);
                }
            }
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
                    centerPos.y = this.position.y + spriteH/2.5f;
                    nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth()/2f,
                            this.centerPos.y + spriteH/2f,
                            nameLabel.getWidth(), nameLabel.getHeight());
                });
            }
        }

        public void spawnPlayer(float x, float y) {
            this.interPos.x = x;
            this.interPos.y = y;
            this.drawPos = new Vector2(x, y);
            this.position.x = x;
            this.position.y = y;
            this.finalDrawPos.x = this.interPos.x + spriteW/12f;
            this.finalDrawPos.y = this.interPos.y + spriteH/12f;
            //this.collider.setPosition(x, y);
            this.updateStage(x, y, true);
            this.x = x;
            this.y = y;
            centerPos.x = this.interPos.x + spriteW / 2f;
            centerPos.y = this.interPos.y + spriteH / 2.5f;
            nameLabel.setBounds(this.centerPos.x - nameLabel.getWidth() / 2f,
                    this.centerPos.y + spriteH / 2f,
                    nameLabel.getWidth(), nameLabel.getHeight());
        }

        public void teleport(float x, float y) {
            if(!isAlive) return; // do not teleport if dead

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
                centerPos.y = this.interPos.y + spriteH / 2.5f;
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
                    centerPos.y = this.position.y + spriteH/2.5f;
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
            centerPos.y = this.interPos.y + spriteH/2.5f;
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
            if(!isAlive) return; // do not trigger if dead

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
            // starts update timer that control user interaction
            //interactionTimer=new Timer();

            float delay = (1/attackSpeed)*0.25f;
            isInteracting = true;

            if(hitTarget.isScheduled())
                hitTarget.cancel();

            interactionTimer.scheduleTask(hitTarget,0f, GameRegister.clientTickrate());
        }

        public Timer.Task hitTarget = new Timer.Task() {
            @Override
            public void run() {
                attackTarget();
            }
        };

        /** Stops any interaction that this entity is doing **/
        public void stopInteraction() {
            if(isInteracting && getTarget() != null) {
                // send msg to stop interaction to server (if its client character)
                if (GameClient.getInstance().getClientCharacter().id == this.id)
                    GameClient.getInstance().requestInteraction(GameRegister.Interaction.STOP_INTERACTION, getTarget().contextId, getTarget().type);
                stopInteractionTimer();
            }
        }

        public void stopInteractionTimer() {
//            if (interactionTimer != null)
//                interactionTimer.clear();
            if(hitTarget.isScheduled())
                hitTarget.cancel();

            if (this.state != GameRegister.EntityState.FREE)
                this.state = GameRegister.EntityState.FREE;

            isInteracting = false; // sets interaction flag to false

            if(getTarget() != null)
                target.set(null);
        }

        long lastAttack = System.currentTimeMillis();

        public void resetLastAttack() {
            lastAttack = 0;
        }

        /**
         * Deals damage to target
         */
        public void attackTarget() {
            if (getTarget() != null && getTarget().health >= 0f) {
                synchronized (target.get()) {
                    // if there is a wall between player and target, do not attack
                    if (!EntityController.getInstance().isTargetOnAttackRange(this, getTarget())) {
                        this.state = GameRegister.EntityState.FREE;
                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastAttack >= 1000f / this.attackSpeed) {
                        lastAttack = System.currentTimeMillis();
                        // player is able to attack target
                        if (this.state != GameRegister.EntityState.ATTACKING) { // started attacking
                            // SEND INTERACTION START TO SERVER TO PROCESS INTERACTION SERVER-SIDE - OF ATTACK TYPE IN THIS CASE (if its client)
                            if (getTarget() != null && GameClient.getInstance().getClientCharacter().id == this.id) {
                                GameClient.getInstance().requestInteraction(GameRegister.Interaction.ATTACK_ENTITY, getTarget().contextId, getTarget().type);
                                this.state = GameRegister.EntityState.ATTACKING;
                            }
                        }

                        // do not attack if target is already dead or dying
                        if (getTarget() != null && getTarget().health <= 0 || getTarget().fadeOut) {
                            return;
                        }
                        spawnMagicProjectile();
                    }
                }
            }
        }

        /** Spawns magic projectile **/
        private void spawnMagicProjectile() {
            Projectile projectile = GameScreen.getProjectilePool().obtain();
            projectile.init(sfxAtlas.findRegions("PrismaWand"), 0.7f, 0.7f, 1.0f/attackSpeed,
                    1/30f,this, getTarget(), true, true, Interpolation.bounce, "prisma_wand");
            GameScreen.addProjectile(projectile);
        }

        @Override
        public void renderUI(SpriteBatch batch) {
            renderEntityTag(batch, this.finalDrawPos.x-unitScale*0.47f, this.finalDrawPos.y-unitScale*8.81f, 0.7f, 0.12f, new Color(0.3f, 0.9f, 0.0f, 1f));
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
        public void updateHealth(float health) {
            this.health = health;
            if(this.health<=0 && isAlive) {
                die();
            }
        }

        @Override
        public void die() {
            this.isAlive = false;
            this.isInteractive = false;
            this.isTargetAble = false;
            if(GameClient.getInstance().getClientCharacter() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                GameClient.getInstance().getClientCharacter().stopInteractionTimer();
                GameClient.getInstance().getClientCharacter().setTarget(null);
                GameClient.getInstance().getClientCharacter().resetLastAttack();
            }
            if(GameClient.getInstance().getClientCharacter() != null &&
                    this.id == GameClient.getInstance().getClientCharacter().id) { // if its client
                // shows button to respawn
                GameScreen.showDeathUI();
                WorldMap.hoverEntity = null;
                stopInteraction(); // stop interaction of player
                // starts fading out effect
                fadeOut(5f);
            }
        }

        @Override
        public void respawn() {
            // TODO: show button, to respawn and on click call back do the following, PLUS SEND MESSAGE TO SERVER  TCPTCP!1 (OR SEND MSG FIRST AND WAIT FOR RESPONSE TO RESPAWN):
            fadeOut  = false; // makes sure fade out is over
            fadeIn(10f); //starts fading in
            /** sets flags back to true **/
            this.isAlive = true;
            this.isInteractive = true;
            this.isTargetAble = true;
            updateHealth(maxHealth); // makes sure health is back to max
        }

        /**
         * Checks if chat floating text collides with character tag (for client only)
         * @return true if collides, false otherwise
         */
        public boolean floatingTextCollision(SpriteBatch batch) {
            //           batch.end();
//            shapeDebug.setProjectionMatrix(camera.combined);
//            shapeDebug.begin(ShapeRenderer.ShapeType.Line);
//            shapeDebug.setColor(Color.ORANGE);

            //makes sure taghitbox is updated (check renderEntityTag values)
//            float x = this.finalDrawPos.x-unitScale*0.47f;
//            float y = this.finalDrawPos.y-unitScale*8.81f;
//            float w = 0.7f; float h = 0.12f;
//            tagHitBox.set(x+spriteW/2f - w/2f, y+spriteH -h/2, w, h * 4f);

            for(int i = 0; i < currentFloatingTexts.size(); i++) {
//                shapeDebug.rect(currentFloatingTexts.get(i).getHitBox().getX(), currentFloatingTexts.get(i).getHitBox().getY(),
//                        currentFloatingTexts.get(i).getHitBox().getWidth(), currentFloatingTexts.get(i).getHitBox().getHeight());
//                if(tagHitBox.contains(currentFloatingTexts.get(i).getHitBox())) {
//                    System.out.println("imhere");
//                    return true;
                // }
                Rectangle intersection = new Rectangle();
                if (Intersector.intersectRectangles(currentFloatingTexts.get(i).getHitBox(), tagHitBox, intersection))
                {
                    return true;
                }
            }

            //shapeDebug.rect(tagHitBox.getX(), tagHitBox.getY(), tagHitBox.getWidth(), tagHitBox.getHeight());

//            shapeDebug.end();
            //           batch.begin();
            return false;
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            isWalking = false;

            clearFloatingTexts(); // clear floating chat texts that are dead

            if(!isAlive && !fadeOut) return; // make sure we do not render dead players after they faded out

            if(isRespawning) return; // we do not render respawning players

            animTime += Gdx.graphics.getDeltaTime(); // accumulates anim timer
            Map<Direction, Animation<TextureRegion>> currentAnimation = idle;

            Entity currTarget = getTarget();

            // predicts direction if a target is selected and its client
            if(currTarget != null && GameClient.getInstance().getClientCharacter() != null &&
                    GameClient.getInstance().getClientCharacter().id == this.id) {
                // predict direction
                Vector2 dir = currTarget.centerPos.sub(this.centerPos).nor();
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
                if(getTarget() != null) {
                    currentAnimation = attack;
                }
            }

            if(isUnmovable()) currentAnimation = idle;

            // animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

            // if its in teleport in animation, deal with fade animation since client never is removed from AoI
            if(GameClient.getInstance().getClientCharacter() != null &&
                    GameClient.getInstance().getClientCharacter().id == this.id) {
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

            if(!finalDrawPosSet) finalDrawPosSet = true;

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
            stopInteraction();
            //remove(); // remove itself from stage
            Gdx.app.postRunnable(() -> {
                spriteSheet.dispose();
                debugTex.dispose();
                debugCircle.dispose();
            });
        }

        public static Character toCharacter(GameRegister.Character charData) {
            return new Character(charData.name, charData.id, charData.role_level, charData.x, charData.y,
                    charData.maxHealth, charData.health, charData.speed, charData.attackSpeed);
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
            this.type = GameRegister.EntityType.WALL;

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
                this.type = GameRegister.EntityType.PORTAL;
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
        public void updateHealth(float health) {

        }

        @Override
        public void die() {

        }

        @Override
        public void respawn() {

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
            updateHealth(health); // updates health - changes sprite if necessary
            this.maxHealth = maxHealth;
            this.isWalkable = tile.getProperties().get("walkable", Boolean.class);
            this.isObfuscator = tile.getProperties().get("obfuscator", Boolean.class);
            this.contextId = this.spawnId;
            this.type = GameRegister.EntityType.TREE;
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

        /**
         * Updates health dealing with the consequences of new health value
         * @param health    the new value of health to update to
         */
        @Override
        public void updateHealth(float health) {
            this.health = health;
            if(this.health<=0) {
                die();
            }
        }

        @Override
        public void die() {
            this.tile = WorldMap.getInstance().getMap().getTileSets().getTile(this.tileId-1);
            this.isAlive = false;
            this.isInteractive = false;
            this.isTargetAble = false;
            if(GameClient.getInstance().getClientCharacter() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                GameClient.getInstance().getClientCharacter().stopInteractionTimer();
                GameClient.getInstance().getClientCharacter().setTarget(null);
                GameClient.getInstance().getClientCharacter().resetLastAttack();
            }
        }

        @Override
        public void respawn() {
            this.tile = WorldMap.getInstance().getMap().getTileSets().getTile(this.tileId);
            this.isAlive = true;
            this.isInteractive = true;
            this.isTargetAble = true;
            updateHealth(maxHealth);
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
            this.type = GameRegister.EntityType.CREATURE;
            this.isInteractive = true;
            this.isTargetAble = true;
            direction = Direction.SOUTHWEST;
            nameLabel = new TypingLabel("{SIZE=55%}{COLOR=LIGHT_GRAY}"+this.name+"[%]", font);
            nameLabel.skipToTheEnd();
            outlineLabel = new TypingLabel("{SIZE=55%}{COLOR=black}"+this.name+"[%]", font);
            outlineLabel.skipToTheEnd();
            // sfx vars
            sfxAttackType = Jukebox.SoundType.CREATURE;
            StringBuilder sb = new StringBuilder();
            sb.append(name.toLowerCase());
            sb.append("_attack");
            sfxAttackName = String.valueOf(sb);
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
                attack.put(Direction.SOUTHWEST, new Animation<>((1f/attackSpeed)/frames.length, frames));
                attack.put(Direction.SOUTH, new Animation<>((1f/attackSpeed)/frames.length, frames));
                attack.put(Direction.WEST, new Animation<>((1f/attackSpeed)/frames.length, frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[1][j];
                }
                attack.put(Direction.SOUTHEAST, new Animation<>((1f/attackSpeed)/frames.length, frames));
                attack.put(Direction.EAST, new Animation<>((1f/attackSpeed)/frames.length, frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[2][j];
                }
                attack.put(Direction.NORTHWEST, new Animation<>((1f/attackSpeed)/frames.length, frames));
                attack.put(Direction.NORTH, new Animation<>((1f/attackSpeed)/frames.length, frames));
                index = 0;
                frames = new TextureRegion[15];
                for (int j = 0; j < 15; j++) {
                    frames[index++] = tmp[3][j];
                }
                attack.put(Direction.NORTHEAST, new Animation<>((1f/attackSpeed)/frames.length, frames));
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
        public void updateHealth(float health) {
            this.health = health;
            if(this.health<=0) {
                die();
            }
        }

        @Override
        public void die() {
            this.isAlive = false;
            this.isInteractive = false;
            this.isTargetAble = false;
            if(GameClient.getInstance().getClientCharacter() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget() != null &&
                    GameClient.getInstance().getClientCharacter().getTarget().uId == this.uId) {
                GameClient.getInstance().getClientCharacter().stopInteractionTimer();
                GameClient.getInstance().getClientCharacter().setTarget(null);
                GameClient.getInstance().getClientCharacter().resetLastAttack();
            }
            if(attackSFX.isScheduled()) attackSFX.cancel(); // makes sure to cancel attack sfx
        }

        @Override
        public void respawn() {
            this.isAlive = true;
            this.isInteractive = true;
            this.isTargetAble = true;
            updateHealth(maxHealth);
        }

        @Override
        public void render(SpriteBatch batch) {
            // if assets are not loaded, return
            if(assetsLoaded == false) return;
            if(GameClient.getInstance().getClientCharacter() == null) return;

            Map<Direction, Animation<TextureRegion>> currentAnimation = walk; // only walk anim for now

            if(isAlive && health <= 0f) die(); // makes sure to die when needed

            switch (state) {
                case IDLE:
                case DYING:
                    currentAnimation = idle;
                    break;
                case FOLLOWING:
                case IDLE_WALKING:
                    currentAnimation = walk;
                    break;
                case ATTACKING:
                    currentAnimation = attack;
                    if(!attackSFX.isScheduled()) { // if not running, schedule sound effects of attack
                        Timer.schedule(attackSFX, 0f, GameRegister.clientTickrate());
                    }
                    break;
                default:
                    break;
            }

            // makes sure to cancel attack sfx when not attacking or when dying
            if((state != State.ATTACKING || fadeOut) && attackSFX.isScheduled()) attackSFX.cancel();

            // try to predict position if its following this player
            if(target != null && target.id == GameClient.getInstance().getClientCharacter().id && isAlive) {
                this.x = target.drawPos.x - spriteW/2f;
                this.y = target.drawPos.y - spriteH/3f;
                this.position.x = target.drawPos.x - spriteW/2f;
                this.position.y = target.drawPos.y - spriteH/3f;
            }

            if(interPos.dst(position) != 0f && Common.entityInterpolation && isAlive) {
                interpolate2();
                if(target == null) // if there is no target and there is interpolation, walk
                    currentAnimation = walk;
            }

            animTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

            //centerPos = new Vector2(this.interPos.x + spriteW/2f, this.interPos.y + spriteH/2f);
            this.centerPos.x = this.interPos.x + spriteW/2f;
            this.centerPos.y = this.interPos.y + spriteH/2f;
            this.drawPos.x = this.interPos.x + spriteW/2f;
            this.drawPos.y = this.interPos.y + spriteH/3f;

            // tries to predict direction if its targeting someone and target moved but not enough to interpolate
            if(target != null && isAlive) {
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
                lastVelocity.x = 0; lastVelocity.y = 0;
                // predict state (only if it is the target)
                if(target != null  && target.id == GameClient.getInstance().getClientCharacter().id)
                    state = State.ATTACKING;

                if(target == null) // if there is no target we can stop at goal position without worrying for range of attack
                    interPos.set(position.x, position.y);

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
            if(attackSFX.isScheduled()) attackSFX.cancel();
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