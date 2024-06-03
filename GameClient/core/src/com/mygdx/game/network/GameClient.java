package com.mygdx.game.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.entity.Component;
import com.mygdx.game.network.GameRegister.AddCharacter;
import com.mygdx.game.network.GameRegister.ClientId;
import com.mygdx.game.network.GameRegister.MoveCharacter;
import com.mygdx.game.network.GameRegister.RemoveCharacter;
import com.mygdx.game.network.GameRegister.UpdateCharacter;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Encoder;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameClient extends DispatchServer {
    private static GameClient instance; // login client instance
    private InputProcessor oldIProcessor;
    UI ui;
    private Client client;
    String name="";
    String host="";
    private int clientCharId; // this client's char id
    private int latWindowSize = 10;
    private long avgLatency = 0;
    private ConcurrentLinkedQueue<MoveCharacter> pendingMoves; // will contain a copy of all MoveMessages sent (for server recon.)
    private float updateDelta;
    public float getUpdateDelta() {return updateDelta;}
    public boolean isConnected() {return isConnected;}
    private boolean isConnected = false;
    private ArrayList<Long> latencies; // list of last calculated latencies
    private long lastPingTs = 0;
    private LagNetwork lagNetwork; // for lag simulation
    public AtomicBoolean isPredictingRecon = new AtomicBoolean(false);
    private Map<Class<?>, Long> requestsCounter;
    public Map<Class<?>, Long> getRequestsCounter() {return requestsCounter;}

    protected GameClient() {
        client = new Client(65535, 65535);
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        GameRegister.register(client);

        // counts requests of each type of request
        // for server reconciliation
        requestsCounter = new ConcurrentHashMap<>();
        // instantiates pending move msgs
        pendingMoves = new ConcurrentLinkedQueue<>();

        // listeners that are going to be notified on server responses
        listeners = new PropertyChangeSupport(this);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
                //client.updateReturnTripTime(); // for ping measurement
                isConnected = true;
                lagNetwork = new LagNetwork(client); // to simulate lag when desired
                latencies = new ArrayList<>();
                sendPing();
            }

            public void received (Connection connection, Object object) {
                if (object instanceof GameRegister.Ping) { // calculates latency accordingly
                    GameRegister.Ping ping = (GameRegister.Ping)object;
                    if (ping.isReply) {
                        long now = System.currentTimeMillis();
                        latencies.add(now - lastPingTs); // calculates delta since last ping
                        if(latencies.size() > latWindowSize) // keep within desired window size
                            latencies.remove(0); // remove oldest latency
                        //System.out.println("Ping: " + Common.calculateAverage(latencies));
                        avgLatency = Common.calculateAverage(latencies); // stores avg ping value
                    }
                    Timer.schedule(pingDelay, 5f); // delay new ping update
                }
                if (object instanceof ClientId) {
                    ClientId msg = (ClientId)object;
                    clientCharId =  msg.id;
                    return;
                }
                if (object instanceof AddCharacter) {
                    AddCharacter msg = (AddCharacter)object;
                    Component.Character character = Component.Character.toCharacter(msg.character);
                    ui.addCharacter(character);
                    return;
                }

                if (object instanceof UpdateCharacter) {
                    ui.updateCharacter((UpdateCharacter)object);
                    return;
                }

                if (object instanceof RemoveCharacter) {
                    RemoveCharacter msg = (RemoveCharacter)object;
                    ui.removeCharacter(msg.id);
                    return;
                }
            }

            public void disconnected (Connection connection) {
                System.err.println("Disconnected from game server");
                isConnected = false;
                // tell interested listeners that server has lost connection
                listeners.firePropertyChange("lostConnection", null, true);
                // disposes list of characters
                ui.dispose();
            }
        }));

        ui = new UI();
        host = "192.168.0.192";
    }

    public void connect() {
        try {
            client.connect(5000, host, GameRegister.tcp_port, GameRegister.udp_port);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

//        name = "b";
//        Login login = new Login();
//        login.name = name;
//        client.sendTCP(login);

        // show keyboard on android for testing
        //Gdx.input.setOnscreenKeyboardVisible(true);
    }

    public static GameClient getInstance() {
        if(instance == null)
            instance = new GameClient();
        return instance;
    }

    public long getAvgLatency() {
        return avgLatency;
    }

    private void sendPing() {
        if(client == null || !isConnected) return;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(new GameRegister.Ping(false));
        } else {
            client.sendUDP(new GameRegister.Ping(false));
        }
        lastPingTs = System.currentTimeMillis(); // updates last ping sent timestamp
    }

    // send ping message with delay
    private Timer.Task pingDelay = new Timer.Task() {
        @Override
        public void run() {
            sendPing();
        }
    };

    public void moveCharacter(MoveCharacter msg) {
        if (client == null || msg == null || !isConnected) return;

        // keeps a copy of the sent message for server reconciliation
        pendingMoves.offer(msg);

        // if lag simulation is on, add to queue with a timer to be sent
        if(lagNetwork != null && GameRegister.lagSimulation)
            lagNetwork.send(msg);
        else
            client.sendUDP(msg);
    }

    public ConcurrentLinkedQueue<MoveCharacter> getMoveMsgListCopy() {
        return pendingMoves;
    }

    // gets request ids for messages that need it
    public long getRequestId(Class<?> msgClass) {
        long reqId = 0;

        if(requestsCounter.containsKey(msgClass))
            reqId = requestsCounter.get(msgClass);

        requestsCounter.put(msgClass, ++reqId);

        return reqId;
    }

    /**
     * Logins into game server with token received from login server
     * @param token the token received from login server for the current authorized user
     */
    public void sendTokenAsync(String token) {
        // Encrypt and send to server on another thread
        new Thread(() -> {
            Encoder encoder = new Encoder();
            byte[] encryptedToken = encoder.signAndEncryptData(token);
            GameRegister.Token tk = new GameRegister.Token();
            tk.token = encryptedToken;
            client.sendTCP(tk);
        }).start();
    }

    public HashMap<Integer, Component.Character> getOnlineCharacters() {
        return ui.characters;
    }
    public Component.Character getClientCharacter() {return ui.characters.get(clientCharId);}

    public void setUpdateDelta(float delta) {
        this.updateDelta = delta;
    }

    static class UI {

        HashMap<Integer, Component.Character> characters = new HashMap();

//
//        public String inputOtherStuff () {
//            String input = (String)JOptionPane.showInputDialog(null, "Other Stuff:", "Create account", JOptionPane.QUESTION_MESSAGE,
//                    null, null, "other stuff");
//            if (input == null || input.trim().length() == 0) System.exit(1);
//            return input.trim();
//        }

        public void addCharacter (Component.Character character) {
            characters.put(character.id, character);
            System.out.println(character.name + " added at " + character.x + ", " + character.y);
            character.update(character.x, character.y);
        }

        public void updateCharacter (UpdateCharacter msg) {
//            entity.x = state.position;
//
//            if (this.server_reconciliation) {
//                // Server Reconciliation. Re-apply all the inputs not yet processed by
//                // the server.
//                var j = 0;
//                while (j < this.pending_inputs.length) {
//                    var input = this.pending_inputs[j];
//                    if (input.input_sequence_number <= state.last_processed_input) {
//                        // Already processed. Its effect is already taken into account into the world update
//                        // we just got, so we can drop it.
//                        this.pending_inputs.splice(j, 1);
//                    } else {
//                        // Not processed by the server yet. Re-apply it.
//                        entity.applyInput(input);
//                        j++;
//                    }
//                }
//            } else {
//                // Reconciliation is disabled, so drop all the saved inputs.
//                this.pending_inputs = [];

            Component.Character character = characters.get(msg.id);
            if (character == null) return;

            character.update(msg.x, msg.y);

            if(GameClient.getInstance().clientCharId == msg.id) {
                if(GameRegister.serverReconciliation) {
                    GameClient.getInstance().isPredictingRecon.set(true);
                    ConcurrentLinkedQueue<GameRegister.MoveCharacter> pending = GameClient.getInstance().getMoveMsgListCopy();
                    Iterator iterator = pending.iterator();
                    while (iterator.hasNext()) {
                        MoveCharacter pendingInput = (MoveCharacter) iterator.next();
                        if (pendingInput.requestId <= msg.lastRequestId) {
                            // Already processed. Its effect is already taken into account into the world update
                            // we just got, so we can drop it.
                            iterator.remove();
                        } else {
                            // Not processed by the server yet. Re-apply it.
//                            entity.applyInput(input);
//                            j++;
                            character.predictMovement(pendingInput);
                        }
                    }
                    GameClient.getInstance().isPredictingRecon.set(false);
                } else {
                    GameClient.getInstance().getMoveMsgListCopy().clear();
                }
            }

//            Component.Character character = characters.get(msg.id);
//            if (character == null) return;
//
//            // do server reconciliation if its enabled and is the client char
//            if(GameRegister.serverReconciliation && GameClient.getInstance().clientCharId == msg.id) {
//                GameClient.getInstance().isPredictingRecon.set(true);
//                if(!GameRegister.entityInterpolation)
//                    character.update(msg.x, msg.y);
//
//                character.lastRequestId.set(msg.lastRequestId);
//
//                if(msg.lastRequestId < character.lastRequestId.get()) return; // already processed a more up to date msg
//
//                List<MoveCharacter> movesCpy = GameClient.getInstance().getMoveMsgListCopy();
//                //long lastMoveSent = movesCpy.get(movesCpy.size() - 1).requestId;
//                // System.out.println("Last move sent: " + lastMoveSent + " | last processed: " + msg.lastRequestId);
//                // discards already processed msgs from copy list
//                MoveCharacter moveMsg = null;
//
//                synchronized (movesCpy) {
//                    // must be in synchronized block
//                    Iterator it = movesCpy.iterator();
//
//                    float startX = 0;
//                    float startY = 0;
//                    while (it.hasNext()) {
//                        moveMsg = (MoveCharacter) it.next();
//                        if (moveMsg.requestId == msg.lastRequestId) {
//                            //character.update(msg.x, msg.y);
//                            character.virtualX = msg.x; character.virtualY = msg.y;
//                        }
//                        if (moveMsg.requestId <= msg.lastRequestId) // removed all processed msgs
//                            it.remove(); // removes this msg
//                        else { // process the inputs that are not processed by the server still
//                            if(GameRegister.entityInterpolation) { // interpolate
//                                character.virtualMove(moveMsg); // updates position virtually
//                                //character.predictMovement(moveMsg);
//                            }
//                            else // apply each movement
//                                character.predictMovement(moveMsg);
//                        }
//                    }
//                }
//                GameClient.getInstance().isPredictingRecon.set(false);
//
//                if(GameRegister.entityInterpolation) {
//                    //character.update(character.virtualX, character.virtualY);
//                    character.addMovePos(msg.lastRequestId, new Vector2(character.virtualX, character.virtualY));
//                    // if its the answer to the last movement not processed goes to server last pos
//                    // to avoid drifting accumulation
//                    if(msg.lastRequestId == instance.getRequestsCounter().get(MoveCharacter.class)) {
//                        System.out.println("lastMoveServer: " + msg.x + " / " + msg.y);
//                        character.lastServerPosX = msg.x; character.lastServerPosY = msg.y;
//                        //character.addMovePos(msg.lastRequestId, new Vector2(msg.x, msg.y));
//                    }
////                        character.update(msg.x, msg.y);
//                    //character.addMovePos(msg.lastRequestId, new Vector2(msg.x, msg.y));
//                }
//
//            } else { // server reconciliation disabled or it isnt client char
//                character.update(msg.x, msg.y);
//            }
            //System.out.println(character.name + " moved to " + character.x + ", " + character.y);
        }

        public void removeCharacter (int id) {
            Component.Character character = characters.remove(id); // remove from list of logged chars
            if (character != null) {
                character.dispose();
                System.out.println(character.name + " removed");
            }
        }

        // disposes list of characters online
        public void dispose() {
            for (Component.Character c : characters.values()) {
                c.dispose();
            }
            characters.clear();
        }
    }
}
