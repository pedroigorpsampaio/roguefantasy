package com.mygdx.server.network;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;

/**
 * The register for the GAME SERVER
 * Contains all the necessary structures common to login server-client communication
 */
public class GameRegister {
    static public final int tcp_port = 43572; // GAME SERVER TCP PORT
    static public final int udp_port = 38572; // GAME SERVER UDP PORT
    static public final int serverTickrate = 15; // the amount of communication updates per second in server
    static public final int clientTickrate = 20; // the amount of communication updates per second in client
    static public boolean lagSimulation = true; // simulate lag
    static public int lag = 450; // simulated lag value in ms

    /** gets Tick rates interval in seconds **/
    static public float serverTickrate() {return 1f/serverTickrate;}
    static public float clientTickrate() {return 1f/clientTickrate;}

    /**
     * Registers objects that are going to be sent over the network
     * during login server-client communication
     *
     * @param endPoint the end point to register the objects to be sent (can be the client or the server)
     */
    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(byte[].class);
        kryo.register(ArrayList.class);
        kryo.register(Ping.class);
        kryo.register(Response.class);
        kryo.register(Response.Type.class);
        kryo.register(Login.class);
        kryo.register(Token.class);
        kryo.register(Register.class);
        kryo.register(AddCharacter.class);
        kryo.register(UpdateCreature.class);
        kryo.register(UpdateCharacter.class);
        kryo.register(UpdateState.class);
        kryo.register(RemoveCharacter.class);
        kryo.register(Character.class);
        kryo.register(ClientId.class);
        kryo.register(MoveCharacter.class);
    }

    static public class Ping {
        public boolean isReply;
        public Ping() {}
        public Ping(boolean isReply) {this.isReply = isReply;}
    }

    // contains information flag data to be shared between client and server
    static public class Response {
        public enum Type{
            USER_ALREADY_LOGGED_IN,
            DISCARD, // useless response
        }
        public Type type;
        public Response() {this.type = Type.DISCARD;}
        public Response(Type type) {this.type = type;}
    }

    static public class Login {
        public String name;
    }

    // contains user generated token data
    static public class Token {
        public byte[] token;
    }

    static public class Register {
        public String name;
        public String otherStuff;
    }

    static public class UpdateCharacter {
        public int id;
        public float x, y;
        public long lastRequestId; // the last move request processed that resulted in this x,y
    }

    static public class UpdateCreature {
        public long spawnId;
        public int creatureId;
        public float x, y, speed, lastVelocityX, lastVelocityY;
        public String name;
    }

    static public class UpdateState {
        ArrayList<UpdateCharacter> characterUpdates = new ArrayList<>();
        ArrayList<UpdateCreature> creatureUpdates = new ArrayList<>();
    }

    static public class AddCharacter {
        public Character character;
    }

    static public class RemoveCharacter {
        public int id;
    }

    static public class MoveCharacter {
        public float x, y, xEnd, yEnd;
        public boolean hasEndPoint;
        public long requestId; // id of this request (to be used for server reconciliation)
    }

    public static class Character {
        public String name;
        public int id, role_level;
        public float x, y, speed;
    }

    public static class ClientId {
        int id;
    }
}
