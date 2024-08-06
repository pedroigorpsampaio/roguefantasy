package com.mygdx.server.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.db.DbController;
import com.mygdx.server.entity.Component;
import com.mygdx.server.entity.EntityController;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.network.ChatRegister.Message;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Chat SERVER
 * Server that deals with player messaging
 */
public class ChatServer extends DispatchServer implements CmdReceiver {
    private static ChatServer instance;
    Server server;
    private boolean isOnline = false; // is this server online?
    public Map<Integer, CharacterConnection> loggedIn = new ConcurrentHashMap<>(); // logged in clients in chat-server

    private ChatServer() {
        super(); // calls constructor of the superclass to instantiate listeners list
        server = new Server(65535, 65535) {
            protected Connection newConnection () {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new CharacterConnection();
            }
        };

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        ChatRegister.register(server);

        server.addListener(new Listener() {
            public void received (Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                CharacterConnection connection = (CharacterConnection)c;
                ChatRegister.Writer writer = connection.writer;

                // msg to login
                if(object instanceof ChatRegister.Writer) {
                    ChatRegister.Writer msg = (ChatRegister.Writer) object;
                    connection.writer.id = msg.id;
                    connection.writer.name = msg.name;
                    loggedIn.putIfAbsent(msg.id, connection);
                }

                if(object instanceof ChatRegister.Response) {
                    ChatRegister.Response msg = (ChatRegister.Response) object;
                    switch(msg.type) {
                        case LOGOFF:
                            disconnected(c);
                        default:
                            break;
                    }
                }

                if(object instanceof Message) {
                    Message msg = (Message) object;
                    DispatchMessage(connection, msg);
                }
            }

            public void disconnected (Connection c) {
                CharacterConnection connection = (CharacterConnection)c;
                synchronized (loggedIn) {
                    loggedIn.remove(connection.writer.id); // remove from logged in list
                }
            }
        });

    }

    /**
     * Dispatches message received by client
     * distributing it across the interested clients
     *
     * @param writer the writer of the message
     * @param msg   the message to be distributed
     */
    private void DispatchMessage(CharacterConnection writer, Message msg) {

        switch (msg.channel) {
            case PRIVATE:
                sendPrivateMessage(writer, msg);
                break;
            case DEFAULT:
                sendDefaultMessage(writer, msg);
                break;
            default:
                break;
        }

    }

    /**
     * Send default message to players in AoI of writer
     *
     * @param writer the writer of the message
     * @param msg   the message to be distributed
     */
    private void sendDefaultMessage(CharacterConnection writer, Message msg) {
        // writer character
        Component.Character character = GameServer.getInstance().getLoggedCharacter(writer.writer.id);

        synchronized (character.aoIEntities.characters) {
            Iterator<Map.Entry<Integer, Component.Character>> it = character.aoIEntities.characters.entrySet().iterator();
            while (it.hasNext()) { // iterate through characters in AoI of writer
                Map.Entry<Integer, Component.Character> entry = it.next();
                if(entry.getKey() == writer.writer.id) // don't send to itself
                    continue;
                CharacterConnection recipient = loggedIn.get(entry.getKey());
                if(recipient != null) // makes sure that recipient is logged in chat server still
                    recipient.sendTCP(msg);
            }
        }
    }

    /**
     * For private messages, just send it to the recipient player (if player is online)
     * if not send player offline response
     *
     * @param writer the writer of the message
     * @param msg   the message to be distributed
     */
    private void sendPrivateMessage(CharacterConnection writer, Message msg) {
        if(msg.recipientId == writer.writer.id) // don't send msg to itself
            return;
        CharacterConnection recipient = loggedIn.get(msg.recipientId);
        if(recipient == null) { // recipient is not online
            writer.sendTCP(new ChatRegister.Response(ChatRegister.Response.Type.PLAYER_IS_OFFLINE));
        } else { // recipient is online, send the message
            recipient.sendTCP(msg);
        }
    }

    public void connect() {
        try {
            server.bind(ChatRegister.port);
        } catch (IOException e) {
            Log.info("login-server", "Could not bind port " + LoginRegister.port + " and start login server");
            isOnline = false;
            return;
        }
        // server online
        server.start();
        isOnline = true;

        Log.info("chat-server", "Chat Server is running!");
    }

    public static ChatServer getInstance() {
        if(instance == null)
            instance = new ChatServer();

        return instance;
    }

    public void stop() {
        if(isOnline) {
            // TODO: SAVE MESSAGE STATE BEFORE ENDING?
            Log.info("chat-server", "Chat server is stopping...");
            server.stop();
            server.close();
            Log.info("chat-server", "Chat server has stopped!");
            isOnline = false;
        } else
            Log.info("cmd", "Chat server is not running!");
    }

    // process commands received via dispatcher
    @Override
    public void process(Command cmd) {
        switch (cmd.getType()) {
            case STOP:
            case RESTART:   // only stops server, will be restarted in UI module
                stop();
                break;
            default:
                break;
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    // This holds per connection state.
    public static class CharacterConnection extends Connection {
        public ChatRegister.Writer writer;

        public CharacterConnection() {
            writer = new ChatRegister.Writer();
        }
    }

}
