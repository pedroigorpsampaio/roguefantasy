package com.mygdx.server.network;

import static com.mygdx.server.network.ChatRegister.MESSAGE_REGISTRY_CHANNEL_COOLDOWN;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.entity.Component;
import com.mygdx.server.entity.WorldMap;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.network.ChatRegister.Message;

import java.io.IOException;
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
    public Map<Integer, CharacterConnection> worldRegistry = new ConcurrentHashMap<>(); // clients currently logged in world chat
    public Map<Integer, CharacterConnection> tradeRegistry = new ConcurrentHashMap<>(); // clients currently logged in trade chat
    public Map<Integer, CharacterConnection> helpRegistry = new ConcurrentHashMap<>(); // clients currently logged in help chat


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
                    warnContacts(msg.id, true); // "warn" clients that has this client as contact of online status
                }
                else if(object instanceof ChatRegister.Response) {
                    ChatRegister.Response msg = (ChatRegister.Response) object;
                    switch(msg.type) {
                        case LOGOFF:
                            disconnected(c);
                        default:
                            break;
                    }
                }
                else if(object instanceof ChatRegister.ChatRegistration) {
                    ChatRegister.ChatRegistration msg = (ChatRegister.ChatRegistration) object;
                    UpdateClientRegistry(connection, msg);
                }
                else if(object instanceof Message) {
                    Message msg = (Message) object;
                    dispatchMessage(connection, msg);
                }
                else if(object instanceof ChatRegister.ContactsRequest) {
                    ChatRegister.ContactsRequest msg = (ChatRegister.ContactsRequest) object;
                    sendContacts(connection, msg.requesterId);
                }
            }

            public void disconnected (Connection c) {
                CharacterConnection connection = (CharacterConnection)c;
                synchronized (loggedIn) {
                    loggedIn.remove(connection.writer.id); // remove from logged in list
                    warnContacts(connection.writer.id, false); // "warn" clients that has this client as contact of online status
                    //remove from registry
                    helpRegistry.remove(connection.writer.id);
                    worldRegistry.remove(connection.writer.id);
                    tradeRegistry.remove(connection.writer.id);
                }
            }
        });

    }

    /**
     * Warns online players that have "id" character as contact of a change of online status
     * @param id        the id of the character that had a change of online status
     * @param online    the online status to update interested players
     */
    private void warnContacts(int id, boolean online) {
        // TODO WARN CONTACTS THAT IS ONLINE/OFFLINE - QUERY EACH LOGGED PLAYER TO SEE IF IT HAS ID AS CONTACT
    }

    /**
     * Sends current stored contacts of the requester to the requester
     * @param connection    the requester connection
     * @param requesterId   the requester id
     */
    private void sendContacts(CharacterConnection connection, int requesterId) {
        /**
         * TODO - STORE AND LOAD CONTACTS IN MONGO DB (FOR EACH CHAR, LIST OF IDS:NAMES? or JUST IDS)
         */
        ChatRegister.ContactsRequest cr = new ChatRegister.ContactsRequest();

        cr.requesterId = requesterId;

        ChatRegister.Writer wr = new ChatRegister.Writer();
        wr.name = "Rios2a"; wr.id = 18; wr.online = isCharacterOnline(18);
        cr.contacts.putIfAbsent(18, wr);

        ChatRegister.Writer wr2 = new ChatRegister.Writer();
        wr2.name = "Rios"; wr2.id = 12; wr2.online = isCharacterOnline(12);
        cr.contacts.putIfAbsent(12, wr2);

        ChatRegister.Writer wr3 = new ChatRegister.Writer();
        wr3.name = "Endor"; wr3.id = 15; wr3.online = isCharacterOnline(15);
        cr.contacts.putIfAbsent(15, wr3);

        ChatRegister.Writer wr4 = new ChatRegister.Writer();
        wr4.name = "Rydok"; wr4.id = 14; wr4.online = isCharacterOnline(14);
        cr.contacts.putIfAbsent(14, wr4);

        connection.sendTCP(cr);
    }

    private boolean isCharacterOnline(int id) {
        return loggedIn.containsKey(id);
    }

    /**
     * Updates client registry in a particular channel
     * @param connection    the client connection
     * @param msg           the registry message received by the client with info about chat registration
     */
    private void UpdateClientRegistry(CharacterConnection connection, ChatRegister.ChatRegistration msg) {
        Map<Integer, CharacterConnection> registry = null;
        StringBuilder sb = new StringBuilder();


        switch(msg.channel) {
            case TRADE:
                registry = tradeRegistry;
                //System.out.println("TRADE REGISTRY SOLICITATION: ");
                sb.append("Trade Chat: ");
                break;
            case HELP:
                registry = helpRegistry;
                //System.out.println("HELP REGISTRY SOLICITATION: ");
                sb.append("Help Chat: ");
                break;
            case WORLD:
                registry = worldRegistry;
                //System.out.println("WORLD REGISTRY SOLICITATION: ");
                sb.append("Global Chat: ");
                break;
            default:
                //System.out.println("Channel does not have a registry: " + msg.channel);
                return;
        }

        sb.append("Player ");
        sb.append(msg.name);

        if(msg.register) {
            registry.putIfAbsent(msg.id, connection);
            //System.out.println(msg.name + " REGISTERED");
            sb.append(" registered");
        } else {
            registry.remove(msg.id);
            //System.out.println(msg.name + " UNREGISTERED");
            sb.append(" unregistered");
        }

        Log.info("chat-server", String.valueOf(sb));
    }

    /**
     * Dispatches message received by client
     * distributing it across the interested clients
     *
     * @param writer the writer of the message
     * @param msg   the message to be distributed
     */
    private void dispatchMessage(CharacterConnection writer, Message msg) {

        switch (msg.channel) {
            case PRIVATE:
                sendPrivateMessage(writer, msg);
                break;
            case TRADE:
                if((System.currentTimeMillis() - writer.writer.lastTradeTs) / 1000f > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {  // respect cooldown also server-side
                    sendRegistryChannelMessage(writer, msg, tradeRegistry);
                    writer.writer.lastTradeTs = System.currentTimeMillis();
                }
                break;
            case HELP:
                if((System.currentTimeMillis() - writer.writer.lastHelpTs) / 1000f > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {  // respect cooldown also server-side
                    sendRegistryChannelMessage(writer, msg, helpRegistry);
                    writer.writer.lastHelpTs = System.currentTimeMillis();
                }
                break;
            case WORLD:
                if((System.currentTimeMillis() - writer.writer.lastWorldTs) / 1000f > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {  // respect cooldown also server-side
                    sendRegistryChannelMessage(writer, msg, worldRegistry);
                    writer.writer.lastWorldTs = System.currentTimeMillis();
                }
                break;
            case MAP:
                sendMapMessage(writer, msg);
                break;
            case DEFAULT:
                sendDefaultMessage(writer, msg);
                break;
            default:
                break;
        }

    }

    /**
     * Sends messages to players within the same map of writer
     * @param writer    the writer of the message
     * @param msg       the message to be sent
     */
    private void sendMapMessage(CharacterConnection writer, Message msg) {
        // writer character
        Component.Character character = GameServer.getInstance().getLoggedCharacter(writer.writer.id);
        // get map players
        Map<Integer, GameServer.CharacterConnection> mapPlayers = WorldMap.getInstance().worldMaps.get(character.position.mapId).players;
        synchronized (mapPlayers) {
            Iterator<Map.Entry<Integer, GameServer.CharacterConnection>> it = mapPlayers.entrySet().iterator();
            while (it.hasNext()) { // iterate through map players
                Map.Entry<Integer, GameServer.CharacterConnection> entry = it.next();
                if (entry.getKey() == writer.writer.id) // don't send to itself
                    continue;
                CharacterConnection recipient = loggedIn.get(entry.getKey()); // gets chat connection for each player
                if(recipient != null) // makes sure that recipient is logged in chat
                    recipient.sendTCP(msg);
            }
        }
    }

    /**
     * Sends messages to registry channels
     * by iterating through registry and sending to each registered client the message
     * @param writer    the original writer of the message
     * @param msg       the message to be send
     * @param registry  the respective registry representing the clients logged in channel currently
     */
    private void sendRegistryChannelMessage(CharacterConnection writer, Message msg, Map<Integer, CharacterConnection> registry) {
        synchronized (registry) {
            Iterator<Map.Entry<Integer, CharacterConnection>> it = registry.entrySet().iterator();
            while (it.hasNext()) { // iterate through registry for every registered client
                Map.Entry<Integer, CharacterConnection> entry = it.next();
                if (entry.getKey() == writer.writer.id) // don't send to itself
                    continue;
                CharacterConnection recipient = entry.getValue();
                if(recipient != null) // makes sure that recipient is logged in channel still
                    recipient.sendTCP(msg);
            }
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
