package com.mygdx.server.entity;

import com.mygdx.server.network.GameRegister;
import com.mygdx.server.network.LoginServer;


public class Component {
    public static class Character {
        public String name;
        public String token;
        public int id, role_level;
        public float x, y;
        public float speed;
        public long lastMoveId = 0;
        public long lastMoveTs = 0;

        /**
         * Prepares character data to be sent to clients with
         * only data that is safe to send and it is of clients interest
         * @return  the Register character class containing the data safe to send
         */
        public GameRegister.Character toSendToClient() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.id; charData.speed = this.speed;
            charData.x = this.x; charData.y = this.y; charData.name = this.name; this.lastMoveId = 0;
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
        public float speed;
        public Attributes(float speed) {this.speed = speed;}
    }

    public static class Spawn {
        public long id; // spawn id
        public Position position; // spawn position
        public float respawnTime; // time between respawns in seconds

        public Spawn(long id, Position position, float respawnTime) {
            this.position = position; this.respawnTime = respawnTime;
        }
    }

    public static class AI {
        public Character target = null;
        public State state = State.IDLE;
        public enum State {
            WALKING,
            RUNNING,
            IDLE,
            ATTACKING,
            FLEEING,
            DYING
        }
    }

}
