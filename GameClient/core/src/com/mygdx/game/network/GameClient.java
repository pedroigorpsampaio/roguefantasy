package com.mygdx.game.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Timer;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.entity.EntityController;
import com.mygdx.game.entity.WorldMap;
import com.mygdx.game.network.GameRegister.AddCharacter;
import com.mygdx.game.network.GameRegister.ClientId;
import com.mygdx.game.network.GameRegister.MoveCharacter;
import com.mygdx.game.network.GameRegister.RemoveCharacter;
import com.mygdx.game.network.GameRegister.UpdateCharacter;
import com.mygdx.game.network.GameRegister.UpdateCreature;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Encoder;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameClient extends DispatchServer {
    private static GameClient instance; // login client instance
    private ServerController serverController;
    private Client client;
    String name="";
    String host="";
    private int clientCharId; // this client's char id
    private int clientUid; // this client's char uid
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
    public boolean firstStateProcessed = false; // processed first game state received
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
                    if(!pingDelay.isScheduled())
                        Timer.schedule(pingDelay, 5f); // delay new ping update
                }
                if (object instanceof ClientId) {
                    ClientId msg = (ClientId)object;
                    clientCharId =  msg.id;
                    return;
                }
                if (object instanceof AddCharacter) {
                    AddCharacter msg = (AddCharacter)object;
                    Entity.Character character = Entity.Character.toCharacter(msg.character);
                    serverController.addCharacter(character);
                    return;
                }

                if (object instanceof GameRegister.UpdateState) {
                    serverController.updateState((GameRegister.UpdateState)object);
                    return;
                }

                if (object instanceof UpdateCharacter) {
                    serverController.updateCharacter((UpdateCharacter)object);
                    return;
                }

                if (object instanceof RemoveCharacter) {
                    RemoveCharacter msg = (RemoveCharacter)object;
                    serverController.removeCharacter(msg.id);
                    return;
                }
            }

            public void disconnected (Connection connection) {
//                System.err.println("Disconnected from game server");
//                isConnected = false;
//                // tell interested listeners that server has lost connection
//                listeners.firePropertyChange("lostConnection", null, true);
//                // disposes lists of entities
//                serverController.dispose();
                logoff();
            }
        }));

        serverController = new ServerController();
        host = "192.168.0.192";
    }

    public void connect(String decryptedToken) {
        new Thread(() -> {
            try {
                client.connect(15000, host, GameRegister.tcp_port, GameRegister.udp_port);
                sendTokenAsync(decryptedToken); // login using token received
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();

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

    // log off from server
    public void logoff() {
        // resets counts requests of each type of request
        // for server reconciliation
        requestsCounter = new ConcurrentHashMap<>();
        // resets pending move msgs
        pendingMoves = new ConcurrentLinkedQueue<>();
        isConnected = false;
        // tell interested listeners that server has lost connection
        listeners.firePropertyChange("lostConnection", null, true);
        // disposes lists of entities
        serverController.dispose();
        new Thread(() -> {
            client.sendTCP(new GameRegister.Response(GameRegister.Response.Type.LOGOFF)); // sends msg to server to log me off
            client.close();
        }).start();
        if(pingDelay.isScheduled()) pingDelay.cancel();
    }


    public Map<Integer, Entity.Character> getOnlineCharacters() {
        return serverController.characters;
    }
    public Map<Long, Entity.Creature> getCreatures() {
        return serverController.creatures;
    }

    public Entity.Character getClientCharacter() {return serverController.characters.get(clientCharId);}
    public int getClientUid() {return clientUid;}

    public void setUpdateDelta(float delta) {
        this.updateDelta = delta;
    }

    /**
     * Removes entities from their data map using their unique entity ID
     * @param uId   the unique id given of the entity to be removed
     */
    public void removeEntity(int uId) {
        serverController.removeEntity(uId);
    }

    public Map<Integer, Entity.Wall> getWalls() {
        return serverController.walls;
    }

    static class ServerController {

        Map<Integer, Entity.Character> characters = new ConcurrentHashMap<>();
        Map<Long, Entity.Creature> creatures = new ConcurrentHashMap<>();
        Map<Integer, Entity.Wall> walls = new ConcurrentHashMap<>();

        public void addCharacter (Entity.Character character) {
//            characters.put(character.id, character);
//            System.out.println("first add to draw list: " + character.id + " / " + character.name);
//            EntityController.getInstance().entities.put(character.uId, character); // put on list of entities (which will manage if its visible or not)
//            System.out.println(character.name + " added at " + character.x + ", " + character.y);
//            // if is client, save client uId
//            if(character.id == instance.clientCharId)
//                instance.clientUid = character.uId;
//            character.update(character.x, character.y);
        }

        public void updateState(GameRegister.UpdateState state) {
            //System.out.println("creatures:" + state.creatureUpdates.size() + " / chars: " + state.characterUpdates.size());
            // clears list of entities received in the last state
            EntityController.getInstance().lastAoIEntities.clear();

            if(state.tileLayers != null) {
                WorldMap.layers = state.tileLayers;
                WorldMap.tileOffsetX = state.tileOffsetX;
                WorldMap.tileOffsetY = state.tileOffsetY;
                //WorldMap.debugMap();
            }
            if(state.wallUpdates != null) { // there are wall updates
                for(GameRegister.Wall wallUpdate : state.wallUpdates){
                    updateWall(wallUpdate);
                    Entity.Wall wall = walls.get(wallUpdate.wallId);
                    EntityController.getInstance().lastAoIEntities.add(wall.uId);
                }
            }
            if(state.characterUpdates != null) { // there are character updates
                for (UpdateCharacter charUpdate : state.characterUpdates) {
                    updateCharacter(charUpdate);
                    Entity.Character character = characters.get(charUpdate.character.id);
                    EntityController.getInstance().lastAoIEntities.add(character.uId);
                }
            }
            if(state.creatureUpdates != null) { // there are game creature updates
                for (UpdateCreature creatureUpdate : state.creatureUpdates) {
                    updateCreature(creatureUpdate);
                    Entity.Creature creature = creatures.get(creatureUpdate.spawnId);
                    EntityController.getInstance().lastAoIEntities.add(creature.uId);
                }
            }

            EntityController.getInstance().removeOutOfAoIEntities(); // removes entities not in AoI

            // sets flag to inform that first update was processed
            if(!instance.firstStateProcessed)
                instance.firstStateProcessed = true;
        }

        private void updateWall(GameRegister.Wall wallUpdate) {
            Entity.Wall wall = null;
            if(!walls.containsKey(wallUpdate.wallId)) {
                wall = Entity.Wall.toWall(wallUpdate);
                walls.put(wallUpdate.wallId, wall);
                EntityController.getInstance().entities.put(wall.uId, wall); // put on list of entities (which will manage if its visible or not)
            }
        }

        private void updateCreature(UpdateCreature creatureUpdate) {
//            System.out.printf("Creature %s moved with %s to %s\n",
//                    creatureUpdate.name, new Vector2(creatureUpdate.lastVelocityX, creatureUpdate.lastVelocityY),
//                    new Vector2(creatureUpdate.x, creatureUpdate.y));

            Entity.Creature creature = null;
            if(!creatures.containsKey(creatureUpdate.spawnId)) { // if creature spawn is not in client list, create and add it
                creature = Entity.Creature.toCreature(creatureUpdate);
                creatures.put(creatureUpdate.spawnId, creature);
                EntityController.getInstance().entities.put(creature.uId, creature); // put on list of entities (which will manage if its visible or not)
            } else { // if its already on list, get it to update it
                creature = creatures.get(creatureUpdate.spawnId);
                //EntityController.getInstance().removeEntity(creature.uId); // remove to reorder list correctly
                //EntityController.getInstance().entities.add(creature); // put on list of entities (which will manage if its visible or not)
            }

            Entity.Character target = characters.get(creatureUpdate.targetId);

            if(target != null && target.id == getInstance().clientCharId
                    && (Entity.State.getStateFromName(creatureUpdate.state) == Entity.State.FOLLOWING
                    || Entity.State.getStateFromName(creatureUpdate.state) == Entity.State.ATTACKING)) {
//                creature.update(creatureUpdate.x, creatureUpdate.y, creatureUpdate.lastVelocityX,
//                        creatureUpdate.lastVelocityY, creatureUpdate.speed, creatureUpdate.attackSpeed, creatureUpdate.state,
//                        target);
                //System.out.println(creatureUpdate.state);
                creature.target = target; creature.speed = creatureUpdate.speed; creature.attackSpeed = creatureUpdate.attackSpeed;
                // leave the rest of info for the client prediction
                return;
            }

            creature.update(creatureUpdate.x, creatureUpdate.y, creatureUpdate.lastVelocityX,
                    creatureUpdate.lastVelocityY, creatureUpdate.speed, creatureUpdate.attackSpeed, creatureUpdate.state,
                    target);

            //TODO: REMOVAL OF LIST WHEN NOT VISIBLE OR AFTER DEATH ANIMATION
        }

        public void updateCharacter (UpdateCharacter msg) {
            Entity.Character character = null;

            if(!characters.containsKey(msg.character.id)) { // if character is not in client list, create and add it
                character = Entity.Character.toCharacter(msg.character);
                characters.put(msg.character.id, character);
                //System.out.println("second add to draw list: " + character.uId + " / " + character.name + " / " + character.drawPos);
                EntityController.getInstance().entities.put(character.uId, character); // put on list of entities (which will manage if its visible or not)
                if(character.id == instance.clientCharId)
                    instance.clientUid = character.uId;
            } else { // if its already on list, get it to update it
                character = characters.get(msg.character.id);
                //EntityController.getInstance().removeEntity(creature.uId); // remove to reorder list correctly
                //EntityController.getInstance().entities.add(creature); // put on list of entities (which will manage if its visible or not)
            }

            // updates direction if its other player
            if(GameClient.getInstance().clientCharId != msg.character.id)
                character.direction = Entity.Direction.getDirection(MathUtils.round(msg.dir.x), MathUtils.round(msg.dir.y));

            // updates attributes
            character.updateSpeed(msg.character.speed);

            // if there is no change in pos, ignore movement
            if(character.x - msg.character.x == 0 && character.y - msg.character.y == 0) return;

            character.update(msg.character.x, msg.character.y);

            if(GameClient.getInstance().clientCharId == msg.character.id) {
                //System.out.println(msg.lastRequestId + " / " + msg.character.name);
                if(Common.serverReconciliation) {
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
                            character.predictMovement(pendingInput);
                        }
                    }
                    GameClient.getInstance().isPredictingRecon.set(false);
                } else {
                    GameClient.getInstance().getMoveMsgListCopy().clear();
                }
            }
        }

        public void removeCharacter (int id) {
            synchronized (characters) {
                Entity.Character character = characters.remove(id); // remove from list of logged chars
                if (character != null) { // only proceeds if remove is made, meaning id was found in list still
                    EntityController.getInstance().removeEntity(character.uId); // remove from list of drawn entities
                    character.dispose();
                    System.out.println(character.name + " removed");
                }
            }
        }

        public void removeAllCreatures() {
            Gdx.app.postRunnable(() -> { //  wait for libgdx UI thread in case its iterating it
                for (Entity.Creature c : creatures.values()) {
                    c.dispose();
                }
                creatures.clear();
            });
        }

        public void removeCreature(long id) {
            synchronized (creatures) {
                Entity.Creature creature = creatures.remove(id); // remove from list of creatures
                if (creature != null) { // only proceeds if remove is made, meaning id was found in list still
                    EntityController.getInstance().removeEntity(creature.uId); // remove from list of drawn entities
                    creature.dispose();
                    System.out.println(creature.name + " removed");
                }
            }
        }

        // disposes list of characters online
        public void dispose() {
//            for (Entity.Character c : characters.values()) {
//                c.dispose();
//            }
//            for (Entity.Creature c : creatures.values()) {
//                c.dispose();
//            }
            characters.clear();
            creatures.clear();
            walls.clear();
            EntityController.getInstance().entities.clear();
        }

        // iterates through data maps containing the different types of entities to remove one by its unique id
        public void removeEntity(int uId) {
            // look in walls map
            synchronized (walls) {
                Iterator<Map.Entry<Integer, Entity.Wall>> iterator = walls.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, Entity.Wall> entry = iterator.next();
                    if(entry.getValue().uId == uId) {
                        iterator.remove();
                        return;
                    }
                }
            }
            // look uId in creatures map
            synchronized (creatures) {
                Iterator<Map.Entry<Long, Entity.Creature>> iterator = creatures.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, Entity.Creature> entry = iterator.next();
                    if(entry.getValue().uId == uId) {
                        iterator.remove();
                        return;
                    }
                }
            }
            // look uId in characters map
            synchronized (characters) {
                Iterator<Map.Entry<Integer, Entity.Character>> iterator = characters.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, Entity.Character> entry = iterator.next();
                    if(entry.getValue().uId == uId) {
                        iterator.remove();
                        return;
                    }
                }
            }
        }
    }
}