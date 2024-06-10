package com.mygdx.server.network;

import com.badlogic.gdx.utils.Timer;

import com.mygdx.server.network.GameServer.CharacterConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to help simulate lag between client and game server
 */
public class LagNetwork {
    private List<QueuedMessage> messages;

    public LagNetwork() {
        this.messages = Collections.synchronizedList(new ArrayList<>());

        // starts timer that checks messages to be sent
        Timer.schedule(messageTimer, 0f, 0.016f);
    }

    private Timer.Task messageTimer = new Timer.Task() {
        @Override
        public void run() {
        if(messages != null && messages.size() > 0) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).timeToSend <= System.currentTimeMillis()) {
                    if (messages.get(i).recipient == null || !messages.get(i).recipient.isConnected()) {
                        messages.remove(i);
                        continue;
                    }
                    messages.get(i).recipient.sendUDP(messages.get(i).message);
                    messages.remove(i);
                }
            }
        }
        }
    };

    public void send(Object msg, CharacterConnection recipient) {
        this.messages.add(new QueuedMessage(msg, System.currentTimeMillis()
                            + GameRegister.lag, recipient));
    }
}

class QueuedMessage {
    public Object message;  // the message (should be a class from register)
    public long timeToSend; // the timestamp in which this message should be sent
    CharacterConnection recipient; // the recipient of this message

    public QueuedMessage(Object message, long timeToSend, CharacterConnection recipient) {
        this.message = message; this.timeToSend = timeToSend; this.recipient = recipient;
    }

}
