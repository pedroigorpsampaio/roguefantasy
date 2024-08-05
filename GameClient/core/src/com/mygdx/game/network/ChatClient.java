package com.mygdx.game.network;

import java.beans.PropertyChangeSupport;
import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.ui.ChatWindow;
import com.mygdx.game.ui.GameScreen;

public class ChatClient extends DispatchServer {
    private static ChatClient instance; // login client instance
    private Client client;
    String name="";
    String host = "192.168.0.192";
    private LagNetwork lagNetwork;

    public boolean isConnected() {return isConnected;}
    private boolean isConnected = false;

    protected ChatClient () {
        super(); // calls constructor of the superclass to instantiate listeners list

        client = new Client(65535, 65535);
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        ChatRegister.register(client);

        // listeners that are going to be notified on server responses
        listeners = new PropertyChangeSupport(this);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
                isConnected = true;
                lagNetwork = new LagNetwork(client); // to simulate lag when desired
            }

            /**
             *  Client received callback dispatches work to interested listeners
             */
            public void received (Connection connection, Object object) {
                if (object instanceof ChatRegister.Message) { // token received to login in game server
                    ChatRegister.Message msg = (ChatRegister.Message) object;
                    listeners.firePropertyChange("messageReceived", null, msg);
                } else if(object instanceof  ChatRegister.Response) {
                    ChatRegister.Response msg = (ChatRegister.Response) object;
                    switch (msg.type) {
                        case PLAYER_IS_OFFLINE:
                            GameScreen.getInstance().showInfo("playerIsNotOnline");
                            break;
                        default:
                            break;
                    }
                }
            }

            public void disconnected (Connection connection) {
                System.err.println("Disconnected from chat server");
                isConnected = false;
            }
        }));
    }

    // tries to connect with the login server
    public void connect() {
        try {
            client.connect(5000, host, ChatRegister.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Could not connect with chat server");
            isConnected = false;
        }
    }

    // creates connection when not connected
    public static ChatClient getInstance() {
        if(instance == null)
            instance = new ChatClient();
//        else if(!instance.isConnected)
//            instance.connect();

        return instance;
    }


    /**
     * Sends message to chat server that will deal with the distribution
     *
     * @param chatMsg the chat message containing the necessary info to compose the message for chat server
     * @param channel the channel that this chat message was sent to
     */
    public void sendMessage(ChatWindow.ChatMessage chatMsg, ChatRegister.ChatChannel channel) {
        ChatRegister.Message message = new ChatRegister.Message();
        message.message = chatMsg.message;
        message.channel = channel;
        message.recipientId = chatMsg.recipientId;
        message.sender = chatMsg.sender;
        message.senderId = chatMsg.senderId;
        message.timestamp = chatMsg.timestamp;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(message, 1);
        } else {
            client.sendTCP(message);
        }
    }

    /**
     * Logs in in chat server to be able to receive messages from it
     * @param character     the character info of this client containing id and name
     */
    public void login(Entity.Character character) {
        ChatRegister.Writer writer = new ChatRegister.Writer();
        writer.id = character.id;
        writer.name = character.name;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(writer, 1);
        } else {
            client.sendTCP(writer);
        }
    }

    public void logoff() {
        isConnected = false;
        // tell interested listeners that server has lost connection
        listeners.firePropertyChange("lostConnection", null, true);
        new Thread(() -> {
            client.sendTCP(new ChatRegister.Response(ChatRegister.Response.Type.LOGOFF)); // sends msg to server to log me off
            client.close();
        }).start();
    }
}
