package com.mygdx.server.network;


import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.regex.Pattern;

/**
 * The register for the CHAT SERVER
 * Contains all the necessary structures common to chat server-client communication
 */
public class ChatRegister {

    static public final int port = 43574; // LOGIN SERVER PORT (ONLY TCP FOR LOGIN SERVER)

    /**
     * Registers objects that are going to be sent over the network
     * during chat server-client communication
     *
     * @param endPoint the end point to register the objects to be sent (can be the client or the server)
     */
    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(byte[].class);
        kryo.register(Writer.class);
        kryo.register(Response.class);
        kryo.register(Response.Type.class);
        kryo.register(ChatChannel.class);
        kryo.register(Message.class);
    }

    static public class Writer {
        String name;
        int id;
    }

    // contains information flag data to be shared between client and server
    static public class Response {
        public enum Type{
            PLAYER_IS_OFFLINE,
            DISCARD, LOGOFF,
        }
        public Type type;
        public Response() {this.type = Type.DISCARD;}
        public Response(Type type) {this.type = type;}
    }

    static public class Message {
        public String sender; // the sender name
        public String message; // the message content without color tag or sender
        public ChatChannel channel; // the channel indicating the scope of the message
        public int senderId, recipientId; // sender -1 == server ; recipient -1 == no recipient
        public long timestamp; // time stamp of message
    }


    /**
     * Chat channels
     */
    public enum ChatChannel {
        DEFAULT("defaultChat"),
        WORLD("worldChat"),
        MAP("mapChat"),
        TRADE("tradeChat"),
        GUILD("guildChat"),
        PARTY("partyChat"),
        HELP("helpChat"),
        PRIVATE("`privateChat"),
        UNKNOWN("unknown");

        private String text;

        ChatChannel(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static ChatChannel fromString(String text) {
            for (ChatChannel t : ChatChannel.values()) {
                if (t.text.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return null;
        }

    }
}
