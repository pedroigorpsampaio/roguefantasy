package com.mygdx.game.entity;

import static com.mygdx.game.entity.WorldMap.unitScale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

public class Projectile extends Entity implements Pool.Poolable {

    private static final float GLOBAL_PROJECTILE_ACCELERATION = 0.152f;

    public Vector2 initPos, position, direction, move, goalPos;
    public Entity creator, target;
    public float speed, width, height;
    public boolean alive, followCreator, followTarget;
    Animation<TextureRegion> animation;
    private float animTime;
    Interpolation interpolation;
    float lifeTime;
    float elapsed;

    /**
     * Projectile constructor. Just initialize variables.
     */
    public Projectile() {
        this.initPos = new Vector2();
        this.position = new Vector2();
        this.direction = new Vector2();
        this.goalPos = new Vector2();
        this.interpolation = Interpolation.exp5In;
        this.animation = null;
        this.creator = null;
        this.target = null;
        this.move = new Vector2();
        this.alive = false;
        this.animTime = 0f;
        this.lifeTime = 0f;
        this.elapsed = 0f;
    }

    /**
     * Initialize the Projectile. Call this method after getting a Projectile from the pool.
     */
    public void init(Array<TextureAtlas.AtlasRegion> frames, float width, float height, float lifeTime,
                     float animSpeed, Entity creator, Entity target, boolean followCreator, boolean followTarget, Interpolation interpolation) {
        this.initPos.set(creator.getEntityCenter().x,  creator.getEntityCenter().y);
        this.position.set(creator.getEntityCenter().x,  creator.getEntityCenter().y);
        this.goalPos.set(target.getEntityCenter().x, target.getEntityCenter().x);
        this.direction = this.goalPos.sub(position).nor();
        this.animation = new Animation<>(animSpeed, frames);
        this.followCreator = followCreator;
        this.followTarget = followTarget;
        this.interpolation = interpolation;
        this.width = width;
        this.height = height;
        this.creator = creator;
        this.target = target;
        this.lifeTime = lifeTime;
        this.move.set(direction.x*Gdx.graphics.getDeltaTime()*speed, direction.y*Gdx.graphics.getDeltaTime()*speed);
        this.alive = true;
        this.uId = EntityController.getInstance().generateUid();
        this.type = GameRegister.EntityType.PROJECTILE;
        EntityController.getInstance().entities.put(uId, this);
    }

    @Override
    public Vector2 getEntityCenter() {
        center.set(position.x + width/2f, position.y + height/2f);
        return center;
    }

    @Override
    public void updateHealth(float health) {

    }

    /**
     * Callback method when the object is freed. It is automatically called by Pool.free()
     * Must reset every meaningful field of this Projectile.
     */
    @Override
    public void reset() {
        initPos.set(0,0);
        position.set(0,0);
        goalPos.set(0,0);
        direction.set(0,0);
        speed = 0f;
        width = 0f;
        height = 0f;
        elapsed = 0f;
        lifeTime = 0f;
        interpolation = Interpolation.exp5In;
        followCreator = false;
        followTarget = false;
        targetPosNotSet = true;
        animation = null;
        creator = null;
        target = null;
        alive = false;
        animTime = 0f;
    }

    boolean targetPosNotSet = true;
    /**
     * Method called each frame, which updates the Projectile.
     */
    public void update (float delta) {
        elapsed += delta;
        float progress = Math.min(1f, elapsed/lifeTime);   // 0 -> 1
        float alpha = interpolation.apply(progress);

        if(followTarget || targetPosNotSet) {
            this.goalPos.set(target.getEntityCenter().x, target.getEntityCenter().y);
            targetPosNotSet = false;
        }

        // update bullet position
        if(followCreator)
            position.set(creator.getEntityCenter().x + (goalPos.x - creator.getEntityCenter().x)*alpha,
                            creator.getEntityCenter().y+(goalPos.y - creator.getEntityCenter().y)*alpha);
        else
            position.set(initPos.x + (goalPos.x - initPos.x)*alpha,
                    initPos.y+(goalPos.y - initPos.y)*alpha);

        if(elapsed >= lifeTime) {
            die();
        }

        if(!followTarget) {
            // check if collided with an entity
            Entity e = EntityController.getInstance().hit(creator, position);
            if(e != null && e.isTargetAble) {
                die();
            }
        }

        // if target of projectile is dead die also
        if(target.health <= 0f) die();

    }

    /** just "kills" this projectile **/
    public void die() {
        alive = false;
        elapsed = 0f;
        EntityController.getInstance().entities.remove(uId); // remove from entities drawing list
    }

    @Override
    public void respawn() {

    }

    /**
     * Renders this projectile
     * @param batch
     */
    public void render(SpriteBatch batch) {
        animTime += Gdx.graphics.getDeltaTime(); // accumulates anim timer
        currentFrame = animation.getKeyFrame(animTime, true);
        batch.draw(currentFrame, position.x-width/2f, position.y-height/2f, width, height);
    }

    @Override
    public void renderUI(SpriteBatch batch) {

    }

    @Override
    public void renderTargetUI(SpriteBatch batch) {

    }

    @Override
    public int compareTo(Entity entity) {
        Vector2 e1Iso = this.position;
        Vector2 e2Iso = entity.drawPos;
        float e1Depth = e1Iso.y - creator.spriteH/3.75f;
        //float e1Depth = e1Iso.y - height/2.1f;
        float e2Depth = e2Iso.y;

        if(e1Depth > e2Depth)
            return -1;
        else
            return 1;
    }
}