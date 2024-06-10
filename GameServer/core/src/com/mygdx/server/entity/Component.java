package com.mygdx.server.entity;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.server.network.GameRegister;
import com.mygdx.server.ui.CommandDispatcher;

import dev.dominion.ecs.api.Entity;


public class Component {
    public static class Character {
        public String token;
        public int role_level;
        public long lastMoveId = 0;
        public long lastMoveTs = 0;
        public Vector2 dir;
        public Component.Tag tag;
        public Component.Attributes attr;
        public Component.Position position;

        /**
         * Prepares character data to be sent to clients with
         * only data that is safe to send and it is of clients interest
         * @return  the Register character class containing the data safe to send
         */
        public GameRegister.Character toSendToClient() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.tag.id; charData.speed = this.attr.speed;
            charData.x = this.position.x; charData.y = this.position.y; charData.name = this.tag.name; this.lastMoveId = 0;
            return charData;
        }
    }

    public static class Position {
        public float x, y;
        public float lastVelocityX = 0, lastVelocityY = 0; // the last velocities that changed the position values

        public Position(float x, float y) {this.x = x; this.y = y;}

        @Override
        public String toString() {
            return "Position: {x=" + x + ", y=" + y + '}';
        }
    }

    public static class Velocity {
        public float x, y;

        public Velocity(float x, float y) {this.x = x; this.y = y;}

        @Override
        public String toString() {
            return "Velocity: {x=" + x + ", y=" + y + '}';
        }
    }

    public static class Tag {
        public int id;
        public String name;
        public Tag(int id) {this.id = id;}
        public Tag(int id, String name) {this.id = id; this.name = name;}
    }

    public static class Attributes {
        public float width, height, speed, attackSpeed, range;
        public Attributes(float width, float height, float speed, float attackSpeed, float range) {
            this.width = width; this.height = height; this.attackSpeed = attackSpeed;
            this.speed = speed; this.range = range;
        }
    }

    public static class Spawn {
        public long id; // spawn id
        public Position position; // spawn position
        public float respawnTime; // time between respawns in seconds

        public Spawn(long id, Position position, float respawnTime) {
            this.position = position; this.respawnTime = respawnTime;
        }
    }

    public static class Event {

        public enum Type { // types of events
            PLAYER_ENTERED_AOI,
            TARGET_IN_RANGE, TARGET_OUT_OF_RANGE, PLAYER_LEFT_AOI
        }
        private Type type; // the type of this event
        public Entity trigger; // the entity responsible for triggering the event

        public Event(Type type, Entity trigger) {this.type = type; this.trigger = trigger;}
    }

    public static class AI {
        private final Entity body; // the body that this ai controls (the entity with AI component)
        public Entity target = null;
        public State state = State.IDLE;

        public AI(Entity body) {
            this.body = body;
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

        // responsible for reacting to events
        public void react(Event event) {
            switch(event.type) {
                case PLAYER_ENTERED_AOI:
                case TARGET_OUT_OF_RANGE:
                    this.state = State.FOLLOWING;
                    target = event.trigger;
                    break;
                case PLAYER_LEFT_AOI:
                    if (target != null && target.equals(event.trigger)) {
                        // TODO: SEARCH FOR OTHER TARGET IN AOI RANGE
                        // if there is no target close, idle walk or do nothing?
                        this.state = State.IDLE_WALKING;
                        Component.Position position = body.get(Component.Position.class);
                        Component.Spawn spawn = body.get(Component.Spawn.class);
                        spawn.position.x = position.x;
                        spawn.position.y = position.y;
                        target = null;
                    }
                    break;
                case TARGET_IN_RANGE:
                    this.state = State.ATTACKING;
                    break;
                default:
                    break;
            }
        }

        // responsible for controlling the AI in the loop
        public void act() {
            switch(state) {
                case IDLE_WALKING:
                    idleWalk();
                    break;
                case FOLLOWING:
                    follow();
                    break;
                case ATTACKING:
                    attack();
                    break;
                default:
                    break;
            }
        }

        // attack if in range
        private void attack() {
            if(target == null) return; // no target to attack

            Component.Position position = body.get(Component.Position.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);

            Component.Position targetPos = null;
            Component.Attributes targetAttr = null;
            if(!target.has(Component.Character.class)) {
                targetPos = target.get(Component.Position.class);
                targetAttr = target.get(Component.Attributes.class);
            } else {
                targetPos = target.get(Component.Character.class).position;
                targetAttr = target.get(Component.Character.class).attr;
            }

            Vector2 goalPos = new Vector2(targetPos.x - targetAttr.width/2f, targetPos.y - targetAttr.height/2f);
            Vector2 aiPos = new Vector2(position.x, position.y);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) > attributes.range) { // not close enough, do not attack anymore
                react(new Event(Event.Type.TARGET_OUT_OF_RANGE, target));
                return;
            }

            // TODO: ATTACK TARGET
        }

        // just follows target until in range
        private void follow() {
            if(target == null) return; // no target to follow

            Component.Position position = body.get(Component.Position.class);
            Component.Velocity velocity = body.get(Component.Velocity.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);

            Component.Position targetPos = null;
            Component.Attributes targetAttr = null;
            if(!target.has(Component.Character.class)) {
                targetPos = target.get(Component.Position.class);
                targetAttr = target.get(Component.Attributes.class);
            } else {
                targetPos = target.get(Component.Character.class).position;
                targetAttr = target.get(Component.Character.class).attr;
            }

            Vector2 goalPos = new Vector2(targetPos.x - targetAttr.width/2f, targetPos.y - targetAttr.height/2f);
            Vector2 aiPos = new Vector2(position.x, position.y);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) <= attributes.range) { // close enough, do not move anymore
                velocity.x = 0; velocity.y = 0;
                react(new Event(Event.Type.TARGET_IN_RANGE, target));
                return;
            }

            velocity.x = deltaVec.x; velocity.y = deltaVec.y;
        }

        private void idleWalk() {
            Component.Position position = body.get(Component.Position.class);
            Component.Velocity velocity = body.get(Component.Velocity.class);
            Component.Spawn spawn = body.get(Component.Spawn.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);
            if(target == null) { // move aimlessly
                if(position.x <= spawn.position.x && position.y <= spawn.position.y) {
                    velocity.x = 1 * attributes.speed * GameRegister.clientTickrate();
                    velocity.y = 0;
                }
                if (position.x >= spawn.position.x + 100 && position.y <= spawn.position.y) {
                    velocity.x = 0;
                    velocity.y = 1 * attributes.speed * GameRegister.clientTickrate();
                }
                if(position.x >= spawn.position.x + 100 && position.y >= spawn.position.y + 100) {
                    velocity.x = -1 * attributes.speed * GameRegister.clientTickrate();
                    velocity.y = 0;
                }
                if(position.x <= spawn.position.x && position.y >= spawn.position.y + 100){
                    velocity.x = 0;
                    velocity.y = -1 * attributes.speed * GameRegister.clientTickrate();
                }
            }
        }
    }

}
