package com.mygdx.server.network;


import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The register for the CHAT SERVER
 * Contains all the necessary structures common to chat server-client communication
 */
public class ChatRegister {

    static public final int port = 43574; // LOGIN SERVER PORT (ONLY TCP FOR LOGIN SERVER)
    public static final int MESSAGE_REGISTRY_CHANNEL_COOLDOWN = 9;
    public static final int MAX_NUM_CONTACTS = 30;
    public static final int MAX_NUM_IGNORE_LIST = 50;

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
        kryo.register(ChatRegistration.class);
        kryo.register(Map.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(ContactsRequest.class);
        kryo.register(Comparable.class);
        kryo.register(AddContact.class);
        kryo.register(RemoveContact.class);
        kryo.register(AddIgnore.class);
        kryo.register(RemoveIgnore.class);
        kryo.register(OnlineCheck.class);
    }

    static public class Writer implements Comparable<Writer> {
        public String name;
        public int id;
        public boolean online;
        public long lastTradeTs = 0, lastWorldTs = 0, lastHelpTs = 0;

        @Override
        public int compareTo(Writer writer) {
            return name.compareTo(writer.name);
        }
    }

    // contains information flag data to be shared between client and server
    static public class Response {
        public enum Type{
            PLAYER_IS_OFFLINE, PLAYER_IS_IGNORING_YOU,
            DISCARD, LOGOFF, CONTACT_ADDED, CONTACT_REMOVED,
            FULL_CONTACT_LIST, CONTACT_REMOVED_FROM_IGNORE_LIST, FULL_IGNORE_LIST, CONTACT_ADDED_TO_IGNORE_LIST
        }
        public Type type;
        public Response() {this.type = Type.DISCARD;}
        public Response(Type type) {this.type = type;}
    }

    static public class ChatRegistration {
        public boolean register;
        public ChatChannel channel;
        public int id;
        public String name;
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

    public static class ContactsRequest {
        public int requesterId;
        public Map<Integer, Writer> contacts = new ConcurrentHashMap<>();
        public Map<Integer, Writer> ignoreList = new ConcurrentHashMap<>();
    }

    public static class AddContact {
        public int contactId;
    }

    public static class RemoveContact {
        public int contactId;
    }

    public static class AddIgnore {
        public int contactId;
    }

    public static class RemoveIgnore {
        public int contactId;
    }

    public static class OnlineCheck {
        public int contactId;
        public String contactName;
        public boolean online;
    }
}
