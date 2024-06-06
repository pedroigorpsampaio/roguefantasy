package com.mygdx.server.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.entity.Component;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.util.Encoder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Game SERVER
 * The server that manages game interactions between players and the game world
 */
public class GameServer implements CmdReceiver {
    private static GameServer instance;
    Server server;
    Set<CharacterConnection> loggedIn = ConcurrentHashMap.newKeySet();
    Set<Component.Character> registeredTokens = ConcurrentHashMap.newKeySet();
    private boolean isOnline = false; // is this server online?
    private LagNetwork lagNetwork; // for lag simulation

    public GameServer() {
        server = new Server(65535, 65535) {
            protected Connection newConnection () {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new CharacterConnection();
            }
        };

        // instantiates LagNetwork instance to be used for simulated lag
        lagNetwork = new LagNetwork();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        GameRegister.register(server);

        server.addListener(new Listener() {
            public void received (Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                CharacterConnection connection = (CharacterConnection)c;
                Component.Character character = connection.character;

                if (object instanceof GameRegister.Ping) { // if it is ping, just send it back asap
                    if(lagNetwork != null && GameRegister.lagSimulation) // send with simulated lag
                        lagNetwork.send(new GameRegister.Ping(true), connection);
                    else
                        connection.sendUDP(new GameRegister.Ping(true));
                    return;
                }

                // token login
                if (object instanceof GameRegister.Token) {

                    GameRegister.Token token = (GameRegister.Token) object; // gets token casting obj
                    Encoder encoder = new Encoder();
                    String decryptedToken = encoder.decryptSignedData(token.token); // decrypts token
                    Log.debug("game-server", "TOKEN AUTH: " + decryptedToken);

                    // check if already logged in
                    synchronized(loggedIn) {
                        Iterator i = loggedIn.iterator();
                        while (i.hasNext()) {
                            CharacterConnection charConn = (CharacterConnection) i.next();
                            Component.Character loggedChar = charConn.character;
                            if(loggedChar.token.equals(decryptedToken)) {
                                connection.sendTCP(new GameRegister.Response(GameRegister.Response.Type.USER_ALREADY_LOGGED_IN));
                                connection.close();
                                return;
                            }
                        }
                    }
                    // retrieve char data from registered token
                    synchronized(registeredTokens) {
                        Iterator i = registeredTokens.iterator();
                        while (i.hasNext()) {
                            Component.Character tokenizedChar = (Component.Character) i.next();
                            if(tokenizedChar.token.equals(decryptedToken)) {
                                // TODO: ACTUALLY LOAD CHAR FROM PERSISTED STORAGE MONGODB
                                character = loadCharacter(tokenizedChar);
                                login(connection, character);
                                //connection.sendUDP(new Component.CharacterState());
                                return;
                            }
                        }
                    }
                }

                if (object instanceof GameRegister.MoveCharacter) {
                    // Ignore if not logged in.
                    if (character == null) return;

                    GameRegister.MoveCharacter msg = (GameRegister.MoveCharacter)object;

                    long now = System.currentTimeMillis();
                    System.out.println(now - character.lastMoveTs);
                    // possibly cheat - check if its at least a little bit faster than expected
                    if(now - character.lastMoveTs < GameRegister.clientTickrate()*450f) {
                        // Log.warn("cheat", "Possible move spam cheating - player: " + character.name);
                        //return; // ignore possible cheat movement
                        //TODO: FIND A WAY TO DETECT HACKED MOVE SPAM CLIENTS
                    }
                    character.lastMoveTs = System.currentTimeMillis();

                    if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                        Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                        Vector2 charPos = new Vector2(character.x, character.y);
                        Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                        deltaVec.nor().scl(character.speed*GameRegister.clientTickrate());
                        Vector2 futurePos = new Vector2(charPos).add(deltaVec);

                        if(touchPos.dst(futurePos) <= 10f) // close enough, do not move anymore
                            return;

                        character.x += deltaVec.x; character.y += deltaVec.y;
                    } else { // wasd movement already has direction in it, just normalize and scale
                        Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(character.speed*GameRegister.clientTickrate());
                        character.x += moveVec.x;
                        character.y += moveVec.y;
                    }

                    character.lastMoveId = msg.requestId;

                    if (!saveCharacter(character)) {
                        connection.close();
                        return;
                    }

//                    GameRegister.UpdateCharacter update = new GameRegister.UpdateCharacter();
//                    update.id = character.id;
//                    update.x = character.x;
//                    update.y = character.y;
//                    update.lastRequestId = msg.requestId;
//
//                    System.out.println("pos server: " + character.x + " / " + character.y);
//
//                    if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
//                        Collection<Connection> connections = server.getConnections();
//                        Iterator<Connection> it = connections.iterator();
//                        for (int i = 0, n = connections.size(); i < n; i++) {
//                            lagNetwork.send(update, (CharacterConnection)it.next());
//                        }
//                    } else
//                        server.sendToAllUDP(update);

                    return;
                }
            }

            private boolean isValid (String value) {
                if (value == null) return false;
                value = value.trim();
                if (value.length() == 0) return false;
                return true;
            }

            public void disconnected (Connection c) {
                CharacterConnection connection = (CharacterConnection)c;
                if (connection.character != null) {
                    loggedIn.remove(connection);

                    GameRegister.RemoveCharacter removeCharacter = new GameRegister.RemoveCharacter();
                    removeCharacter.id = connection.character.id;
                    server.sendToAllTCP(removeCharacter);
                }
            }
        });
    }

    public void connect() {
        try {
            server.bind(GameRegister.tcp_port, GameRegister.udp_port);
        } catch (IOException e) {
            Log.info("game-server", "Could not bind ports (" + GameRegister.tcp_port
                    + "/" + GameRegister.udp_port +") and start game server");
            isOnline = false;
            return;
        }
        // server online
        server.start();
        isOnline = true;
        // instantiate mongodb controller that will act
        // on server requests related to the mongo database
        // and is responsible for the available database ops
        // mongoController = new MongoController();
        //mongoController.connect(); // connects to mongo database
        //addListener(mongoController); // adds mongo controller as listener to all game server requests

        // initiates timer that updates game state to all connections
        Timer timer=new Timer();
        timer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                sendStateToAll();
            }
        },0,GameRegister.serverTickrate());

        Log.info("game-server", "Game Server is running!");
    }

    public static GameServer getInstance() {
        if(instance == null)
            instance = new GameServer();

        return instance;
    }

    void login (CharacterConnection c, Component.Character character) {
        c.character = character;

        // Add existing characters to new logged in connection.
        for (CharacterConnection other : loggedIn) {
            GameRegister.AddCharacter addCharacter = new GameRegister.AddCharacter();
            // translate to safe to send character data
            addCharacter.character = other.character.toSendToClient();
            c.sendTCP(addCharacter);
        }

        loggedIn.add(c);
        // sends to client his ID so he can distinguish itself from his list of characters
        GameRegister.ClientId clientId = new GameRegister.ClientId();
        clientId.id = character.id;
        c.sendTCP(clientId);

        // Add logged in character to all connections.
        GameRegister.AddCharacter addCharacter = new GameRegister.AddCharacter();
        addCharacter.character = character.toSendToClient();
        server.sendToAllTCP(addCharacter);
    }

    boolean saveCharacter (Component.Character character) {
        File file = new File("characters", character.name.toLowerCase());
        file.getParentFile().mkdirs();

        if (character.id == 0) {
            String[] children = file.getParentFile().list();
            if (children == null) return false;
            character.id = children.length + 1;
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(file));
            output.writeInt(character.id);
            output.writeInt(character.role_level);
            output.writeFloat(character.x);
            output.writeFloat(character.y);
            output.writeFloat(character.speed);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void sendStateToAll() {
        GameRegister.UpdateState state = new GameRegister.UpdateState();
        synchronized(loggedIn) {
            Iterator i = loggedIn.iterator();
            while (i.hasNext()) {
                CharacterConnection charConn = (CharacterConnection) i.next();
                Component.Character loggedChar = charConn.character;
                GameRegister.UpdateCharacter update = new GameRegister.UpdateCharacter();
                update.id = loggedChar.id;
                update.x = loggedChar.x;
                update.y = loggedChar.y;
                update.lastRequestId = loggedChar.lastMoveId;
                state.characterUpdates.add(update);
            }
        }

        //System.out.println("pos server: " + character.x + " / " + character.y);

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            Collection<Connection> connections = server.getConnections();
            Iterator<Connection> it = connections.iterator();
            for (int i = 0, n = connections.size(); i < n; i++) {
                lagNetwork.send(state, (CharacterConnection)it.next());
            }
        } else
            server.sendToAllUDP(state);
    }

    Component.Character loadCharacter (Component.Character character) {
        File file = new File("characters", character.name.toLowerCase());
        if (!file.exists()) { // creates char if it does not exist yet
            saveCharacter(character);
            return character;
        }
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(file));
            character.id = input.readInt();
            character.role_level = input.readInt();
            character.x = input.readFloat();
            character.y = input.readFloat();
            character.speed = input.readFloat();
            input.close();
            return character;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (input != null) input.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void stop() {
        if(isOnline) {
            // TODO: SAVE GAME STATE BEFORE ENDING
            Log.info("game-server", "Game server is stopping...");
            server.stop();
            server.close();
            Log.info("game-server", "Game server has stopped!");
            isOnline = false; // server closed, safe to end
            //dbController.close();
        } else
            Log.info("cmd", "Game server is not running!");
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

    public int getNumberOfPlayersOnline() {
        return loggedIn.size();
    }

    /**
     * Request game server token for char that was authenticated by the login server into the game server
     * @param conn  the character connection containing the char data and the connection to the client
     */
    public void requestToken(LoginServer.CharacterConnection conn) {
        // if game server is offline return offline response
        if(!isOnline) {  // send offline response through login TCP
            conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.GAME_SERVER_OFFLINE));
            conn.close();
            return;
        }
        // if user is logged in, return and send already logged in msg
        synchronized(loggedIn) {
            Iterator i = loggedIn.iterator();
            while (i.hasNext()) {
                CharacterConnection connectChar = (CharacterConnection) i.next();
                Component.Character c = connectChar.character;
                if(c.id == conn.charData.id) {
                    conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.USER_ALREADY_LOGGED_IN));
                    conn.close();
                    return;
                }
            }
        }
        Component.Character character = null;
        synchronized(registeredTokens) {
            Iterator i = registeredTokens.iterator();
            while (i.hasNext()) {
                character = (Component.Character) i.next();
                if(character.id == conn.charData.id) { // has token, send token without generating one
                    sendTokenAsync(conn, character.token);
                    Log.debug("login-server", character.name+" is registered and has a token: "+character.token);
                    return;
                }
            }
            // if char does not have a token, generate a new one
            // TODO: LOAD CHARACTER FROM PERSISTED STORAGE
            character = new Component.Character();
            character.token = Encoder.generateNewToken(); character.id = conn.charData.id;
            character.name = conn.charData.character; character.role_level = conn.charData.roleLevel;
            character.x = 0; character.y = 0; character.speed = 250f;
            Log.debug("login-server", character.name+" is NOT registered, new token generated! "+character.token);
            registeredTokens.add(character); // adds character to registered list with new token
            sendTokenAsync(conn, character.token);
        }
    }

    // sends token to client async when login is successfully authenticated
    private void sendTokenAsync(LoginServer.CharacterConnection conn, String token) {
        // Encrypt and send to server on another thread
        new Thread(() -> {
            Encoder encoder = new Encoder();
            byte[] encryptedToken = encoder.signAndEncryptData(token);
            LoginRegister.Token tk = new LoginRegister.Token();
            tk.token = encryptedToken;
            conn.sendTCP(tk);
            // after thread is done
            Gdx.app.postRunnable(() -> {
                conn.close(); // close login connection with client, that will now connect with game server
            });
        }).start();
    }

    // This holds per connection state.
    public static class CharacterConnection extends Connection {
        public Component.Character character;
    }
}
