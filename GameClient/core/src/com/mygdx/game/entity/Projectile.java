package com.mygdx.game.entity;

import static com.mygdx.game.entity.WorldMap.unitScale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

public class Projectile implements Pool.Poolable {

    public Vector2 position, direction, move, goalPos;
    public Entity creator, target;
    public float speed;
    public boolean alive;

    /**
     * Projectile constructor. Just initialize variables.
     */
    public Projectile() {
        this.position = new Vector2();
        this.direction = new Vector2();
        this.goalPos = new Vector2();
        this.creator = null;
        this.target = null;
        this.move = new Vector2();
        this.alive = false;
    }

    /**
     * Initialize the Projectile. Call this method after getting a Projectile from the pool.
     */
    public void init(float speed, Entity creator, Entity target) {
        this.position.set(creator.getEntityCenter().x,  creator.getEntityCenter().y);
        this.goalPos.set(target.getEntityCenter().x, target.getEntityCenter().x);
        this.direction = this.goalPos.sub(position).nor();
        this.creator = creator;
        this.target = target;
        this.speed = position.dst(goalPos) * speed * Gdx.graphics.getDeltaTime();
        this.move.set(direction.x*Gdx.graphics.getDeltaTime()*speed, direction.y*Gdx.graphics.getDeltaTime()*speed);
        this.alive = true;
    }

    /**
     * Callback method when the object is freed. It is automatically called by Pool.free()
     * Must reset every meaningful field of this Projectile.
     */
    @Override
    public void reset() {
        position.set(0,0);
        goalPos.set(0,0);
        direction.set(0,0);
        speed = 0;
        creator = null;
        target = null;
        alive = false;
    }

    /**
     * Method called each frame, which updates the Projectile.
     */
    public void update (float delta) {
        this.goalPos.set(target.getEntityCenter().x, target.getEntityCenter().y);
        this.direction = new Vector2(goalPos.x, goalPos.y).sub(position).nor();
        this.move.set(direction.x*speed, direction.y*speed);
        // update bullet position
        position.add(move.x, move.y);

        // creates acceleration that helps projectile always find target, even moving ones
        this.speed += position.dst(goalPos)* unitScale * delta;

        // if Projectile reaches goal, set it to dead
        if (position.dst(goalPos) <= move.len()) {
            target.takeDamage(); // take damage of target
            alive = false;
        }
    }

    /**
     * Renders this projectile
     * @param batch
     */
    public void render(SpriteBatch batch) {

    }
}