package com.mygdx.game.network;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

/**
 * The register for the LOGIN SERVER
 * Contains all the necessary structures common to login server-client communication
 */
public class LoginRegister {

    static public final int port = 54555; // LOGIN SERVER PORT (ONLY TCP FOR LOGIN SERVER)

    /**
     * Registers objects that are going to be sent over the network
     * during login server-client communication
     *
     * @param endPoint the end point to register the objects to be sent (can be the client or the server)
     */
    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(Login.class);
        kryo.register(RegistrationRequired.class);
        kryo.register(Register.class);
        kryo.register(AddCharacter.class);
        kryo.register(UpdateCharacter.class);
        kryo.register(RemoveCharacter.class);
        kryo.register(Character.class);
        kryo.register(MoveCharacter.class);
    }

    static public class Login {
        public String name;
    }

    static public class RegistrationRequired {
    }

    static public class Register {
        public String name;
        public String otherStuff;
    }

    static public class UpdateCharacter {
        public int id, x, y;
    }

    static public class AddCharacter {
        public Character character;
    }

    static public class RemoveCharacter {
        public int id;
    }

    static public class MoveCharacter {
        public int x, y;
    }

    public static class Character {
        public String name;
        public String otherStuff;
        public int id, x, y;
    }

}
