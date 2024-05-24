package com.mygdx.server.network;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.regex.Pattern;

/**
 * The register for the LOGIN SERVER
 * Contains all the necessary structures common to login server-client communication
 */
public class LoginRegister {

    static public final int port = 43570; // LOGIN SERVER PORT (ONLY TCP FOR LOGIN SERVER)

    /**
     * Registers objects that are going to be sent over the network
     * during login server-client communication
     *
     * @param endPoint the end point to register the objects to be sent (can be the client or the server)
     */
    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(byte[].class);
        kryo.register(Response.class);
        kryo.register(Response.Type.class);
        kryo.register(Login.class);
        kryo.register(RegistrationRequired.class);
        kryo.register(Register.class);
        kryo.register(Character.class);
    }

    // contains information flag data to be shared between client and server
    static public class Response {
        public enum Type{
            USER_ALREADY_REGISTERED, // user name already registered
            CHAR_ALREADY_REGISTERED, // char name already registered
            EMAIL_ALREADY_REGISTERED, // email already registered
            USER_SUCCESSFULLY_REGISTERED, // registration was successful
            DB_ERROR, // error while doing operation on database
            DISCARD; // useless response
        }
        public Type type;
        public Response() {this.type = Type.DISCARD;}
        public Response(Type type) {this.type = type;}
    }

    // contains login data
    static public class Login {
        public String name;
    }

    static public class RegistrationRequired {
    }

    // contains registration data
    static public class Register {
        public byte[] userName;
        public byte[] charName;
        public byte[] password;
        public byte[] email;
        static final int defaultMaxNameSize = 26;
        static final int defaultMaxPasswordSize = 64;
        static final int defaultMinNameSize = 2;
        static final int defaultMinPasswordSize = 8;
        static final int defaultMinEmailSize = 6;
        static final int defaultMaxEmailSize = 254;

        public static boolean isValidAndFitUser(String user) {
            if(user.length() <= defaultMaxNameSize && user.length() >= defaultMinNameSize && isValidUser(user))
                return true;
            else
                return false;
        }

        public static boolean isValidAndFitName(String name) {
            if(name.length() <= defaultMaxNameSize && name.length() >= defaultMinNameSize && isValidName(name))
                return true;
            else
                return false;
        }

        public static boolean isValidAndFitEmail(String email) {
            if(email.length() <= defaultMaxEmailSize && email.length() >= defaultMinEmailSize && isValidEmail(email))
                return true;
            else
                return false;
        }

        public static boolean isValidAndFitPassword(String password) {
            if(password.length() <= defaultMaxPasswordSize && password.length() >= defaultMinPasswordSize && isValidPassword(password))
                return true;
            else
                return false;
        }

        public static boolean isValidUser(String user) {
            Pattern regex = Pattern.compile("[^a-zA-Z0-9]");
            if (regex.matcher(user).find())
                return false;
            return true;
        }

        public static boolean isValidName(String name) {
            Pattern regex = Pattern.compile("^[a-zA-Z0-9]+( [a-zA-Z0-9]+)*$");
            if (regex.matcher(name).find())
                return true;
            return false;
        }

        public static boolean isValidEmail(String email) {
            String regexPattern = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

            Pattern regex = Pattern.compile(regexPattern);
            if (regex.matcher(email).find())
                return true;
            return false;
        }

        public static boolean isValidPassword(String password) {
            Pattern regex = Pattern.compile("[^a-zA-Z0-9!@#$%^&*?/.;:_,-]");
            if (regex.matcher(password).find())
                return false;
            return true;
        }
    }

    public static class Character {
        public String name;
        public String otherStuff;
        public int id, x, y;
    }

}