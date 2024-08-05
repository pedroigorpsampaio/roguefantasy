package com.mygdx.game.network;

import com.badlogic.gdx.utils.Timer;
import com.esotericsoftware.kryonet.Client;

import java.util.ArrayList;

/**
 * A class to help simulate lag between client and game server
 */
public class LagNetwork {
    private ArrayList<QueuedMessage> messages;
    Client client;

    public LagNetwork(Client client) {
        this.messages = new ArrayList<>();
        this.client = client;

        // starts timer that checks messages to be sent
        Timer.schedule(messageTimer, 0f, 0.016f);
    }

    private Timer.Task messageTimer = new Timer.Task() {
        @Override
        public void run() {
            if(client == null || !client.isConnected()) { messages.clear(); return ; }

            if(messages != null && messages.size() > 0) {
                for(int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).timeToSend <= System.currentTimeMillis()) {
                        if(messages.get(i).type == 0)
                            client.sendUDP(messages.get(i).message);
                        else
                            client.sendTCP(messages.get(i).message);
                        messages.remove(i);
                    }
                }
            }
        }
    };

    /**
     * Send UDP message
     * @param msg   the message to be sent
     */
    public void send(Object msg) {
        this.messages.add(new QueuedMessage(msg, System.currentTimeMillis()
                + GameRegister.lag));
    }

    /**
     * send message UDP/TCP based on type
     * @param msg   the msg to be sent
     * @param type  0 for udp, 1 for tcp
     */
    public void send(Object msg, int type) {
        this.messages.add(new QueuedMessage(msg, System.currentTimeMillis()
                + GameRegister.lag, type));
    }
}

class QueuedMessage {
    public Object message;  // the message (should be a class from register)
    public long timeToSend; // the timestamp in which this message should be sent
    //public Type type; // the type of this queued message to help cast later
    public int type; // type 0 == udp / type 1 == tcp

    public QueuedMessage(Object message, long timeToSend) {
        this.message = message; this.timeToSend = timeToSend;
        this.type = 0;
    }

    public QueuedMessage(Object message, long timeToSend, int type) {
        this.message = message; this.timeToSend = timeToSend;
        this.type = type;
    }

}
