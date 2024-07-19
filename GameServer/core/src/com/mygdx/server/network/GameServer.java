package com.mygdx.server.network;

import static com.mygdx.server.network.GameRegister.Interaction.ATTACK_ENTITY;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.entity.Component;
import com.mygdx.server.entity.EntityController;
import com.mygdx.server.entity.WorldMap;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.ui.RogueFantasyServer;
import com.mygdx.server.util.Encoder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.dominion.ecs.api.Entity;


/**
 * Game SERVER
 * The server that manages game interactions between players and the game world
 */
public class GameServer implements CmdReceiver {
    private static GameServer instance;
    private WorldMap world;
    Server server;
    //Set<CharacterConnection> loggedIn = ConcurrentHashMap.newKeySet();
    public Map<Integer, CharacterConnection> loggedIn = new ConcurrentHashMap<>();
    Set<Component.Character> registeredTokens = ConcurrentHashMap.newKeySet();
    private boolean isOnline = false; // is this server online?
    private LagNetwork lagNetwork; // for lag simulation
    private EntityController entityController; // controls server entities

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

//                if(character != null && character.attr !=null)
//                    System.out.println("SPEEDATK; " +character.attr.attackSpeed);

                if (object instanceof GameRegister.Ping) { // if it is ping, just send it back asap
                    if(lagNetwork != null && GameRegister.lagSimulation) // send with simulated lag
                        lagNetwork.send(new GameRegister.Ping(true), connection);
                    else
                        connection.sendUDP(new GameRegister.Ping(true));

                    // update char avg ping
                    character.avgLatency = ((GameRegister.Ping)object).avg;

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
                        Iterator<Map.Entry<Integer, CharacterConnection>> i = loggedIn.entrySet().iterator();
                        while (i.hasNext()) {
                            Map.Entry<Integer, CharacterConnection> entry = i.next();
                            CharacterConnection charConn = entry.getValue();
                            Component.Character charComp = charConn.character;
                            if(charComp.token.equals(decryptedToken)) {
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

                if (object instanceof GameRegister.Response) {
                    GameRegister.Response msg = (GameRegister.Response)object;
                    switch (msg.type) {
                        case LOGOFF:
                            disconnected(c);
                            break;
                        case TELEPORT_IN_FINISHED:
                            character.state = GameRegister.EntityState.TELEPORTING_OUT;
                            break;
                        case TELEPORT_FINISHED:
                            character.isTeleporting = false;
                            character.state = GameRegister.EntityState.FREE;
                            break;
                        default:
                            Log.debug("game-server", "Received unknown response type message");
                            break;
                    }
                }

                if (object instanceof GameRegister.InteractionRequest) {
                    // Ignore if not logged in.
                    if (character == null) return;

                    GameRegister.InteractionRequest msg = (GameRegister.InteractionRequest)object;

                    //System.out.println(msg.type + " : " + msg.entityType + " : " + msg.targetId );
                    processInteraction(connection, msg);
                }

                if (object instanceof GameRegister.MoveCharacter) {
                    // Ignore if not logged in.
                    if (character == null) return;

                    GameRegister.MoveCharacter msg = (GameRegister.MoveCharacter)object;

                    long now = System.currentTimeMillis();
                    //System.out.println(now - character.lastMoveTs);
                    // possibly cheat - check if its at least a little bit faster than expected
                    if(now - character.lastMoveTs < GameRegister.clientTickrate()*450f) {
                        // Log.warn("cheat", "Possible move spam cheating - player: " + character.tag.name);
                        //return; // ignore possible cheat movement
                        //TODO: FIND A WAY TO DETECT HACKED MOVE SPAM CLIENTS
                    }
                    character.lastMoveTs = System.currentTimeMillis();

                    if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                        Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                        Vector2 charPos = new Vector2(character.position.x, character.position.y);
                        Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                        deltaVec.nor().scl(character.attr.speed*GameRegister.clientTickrate());
                        Vector2 futurePos = new Vector2(charPos).add(deltaVec);
                        character.dir = new Vector2(deltaVec.x, deltaVec.y*2f).nor();

                        if(touchPos.dst(futurePos) <= deltaVec.len()) // close enough, do not move anymore
                            return;

                        if(!RogueFantasyServer.world.isWithinWorldBounds(futurePos) ||
                            !RogueFantasyServer.world.isWalkable(futurePos))
                            return;

                        // check if has jumped more than one tile in this movement (forbidden!)
                        Vector2 tInitialPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(character.position.x, character.position.y));
                        Vector2 tFuturePos = RogueFantasyServer.world.toIsoTileCoordinates(futurePos);
                        if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                            return;

                        character.move(deltaVec);
                    } else { // wasd movement already has direction in it, just normalize and scale
                        Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(character.attr.speed*GameRegister.clientTickrate());
                        character.dir = new Vector2(msg.x, msg.y*2f).nor();

                        Vector2 futurePos = new Vector2(character.position.x, character.position.y).add(moveVec);

                        if(!RogueFantasyServer.world.isWithinWorldBounds(futurePos) ||
                                !RogueFantasyServer.world.isWalkable(futurePos))
                            return;

                        // check if has jumped more than one tile in this movement (forbidden!)
                        Vector2 tInitialPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(character.position.x, character.position.y));
                        Vector2 tFuturePos = RogueFantasyServer.world.toIsoTileCoordinates(futurePos);
                        if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                            return;

                        // checks if movement is possible
//                        if(!character.isMovePossible(msg)) {
//                            // search for another direction to slide when colliding
//                            Vector2 newMove = character.findSlide(msg);
//
//                            msg.x = newMove.x;
//                            msg.y = newMove.y;
//                            if(msg.hasEndPoint) msg.hasEndPoint = false; // new dir has been calculated, send it as wasd move to server
//
//                            // test new move once again
//                            if(!character.isMovePossible(msg) || newMove.len() == 0) // if failed again, give up this movement
//                                return;
//                        }
//

//                        while(!RogueFantasyServer.world.isWithinWorldBounds(futurePos)) {
////                            if(moveVec.x != 0 && moveVec.y != 0) { // diagonal inputs
////                                if (RogueFantasyServer.world.isWithinWorldBounds(new Vector2(futurePos.x, character.position.y)))
////                                    character.position.x += moveVec.x;
////                                else if (RogueFantasyServer.world.isWithinWorldBounds(new Vector2(character.position.x, futurePos.y)))
////                                    character.position.y += moveVec.y;
////                                else if (RogueFantasyServer.world.isWithinWorldBounds(new Vector2(futurePos.x, character.position.y - moveVec.y))) {
////                                    character.position.x += moveVec.x;
////                                    character.position.y -= moveVec.y;
////                                } else if(RogueFantasyServer.world.isWithinWorldBounds(new Vector2(character.position.x - moveVec.x, futurePos.y)) &&
////                                            moveVec.y == 0) {
////                                    character.position.x -= moveVec.x;
////                                    character.position.y += moveVec.y;
////                                }
////                            } //TODO: IMPROVE MOVEMENTS IN CLIENT THEN COPY HERE
//                            //return;
//                            moveVec.scl(0.1f); // just try to move as much as possible
//                            futurePos = new Vector2(character.position.x, character.position.y).add(moveVec);
//                        }

                        character.move(moveVec); // calls method that updates char position both in object as well as in 2d array of entities
                    }

                    character.lastMoveId = msg.requestId;

                    // character has a target, direction changes as char should always look at target
                    if(character.target.entity != null) {
                        // calculates new direction based on target
                        Vector2 targetPos = character.target.getPosition();
                        if(targetPos != null)
                            character.dir = targetPos.sub(new Vector2(character.position.x, character.position.y)).nor();
                    }

                    if (!saveCharacter(character)) {
                        connection.close();
                        return;
                    }
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
                if (connection.character != null && connection.character.tag != null) {
                    synchronized (loggedIn) {
                        loggedIn.remove(connection.character.tag.id); // remove from logged in list
                    }

                    // saves character
                    saveCharacter(connection.character);

                    GameRegister.RemoveCharacter removeCharacter = new GameRegister.RemoveCharacter();
                    Component.Character charComp = connection.character;
                    removeCharacter.id = charComp.tag.id;

                    if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
                        Collection<Connection> connections = server.getConnections();
                        Iterator<Connection> it = connections.iterator();
                        for (int i = 0, n = connections.size(); i < n; i++) {
                            lagNetwork.send(removeCharacter, (CharacterConnection)it.next());
                        }
                    } else {
                        server.sendToAllTCP(removeCharacter);
                    }

                    // dispatch event for ai reactors
                    EntityController.getInstance().dispatchEventToAll
                            (new Component.Event(Component.Event.Type.PLAYER_LEFT_AOI,  connection.character));

                    connection.character.reset(); // reset data that needs to be reset before reconnection of this character

                    connection.close();
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

        // instantiates ECS Dominion object that controls server entities (excluding players)
        entityController = EntityController.getInstance();

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
        // fill entity with character information
        //Component.Character charComp = c.character.get(Component.Character.class);
        character.dir = new Vector2(0, -1); // start looking south
        character.tag = new Component.Tag(character.tag.id, character.tag.name);
        character.attr = new Component.Attributes(character.attr.width, character.attr.height, character.attr.maxHealth, character.attr.health,
                                                      character.attr.speed, character.attr.attackSpeed, character.attr.range);
        character.position = new Component.Position(character.position.x, character.position.y);
        character.connection = c;
        character.updatePositionIn2dArray();
        c.character = character;
        //c.character.remove(charComp);
        //charComp.lastMoveTs = character.lastMoveTs;
        //c.character.add(character);
//        charComp.lastTilePos = character.lastTilePos;
//        charComp.attr = character.attr;
//        charComp.dir = character.dir;
//        charComp.position = character.position;
//        charComp.role_level = character.role_level;
//        charComp.lastMoveTs = character.lastMoveTs;
//        charComp.lastMoveId = character.lastMoveId;
//        charComp.tag = character.tag;
//        charComp.token = character.token;
//        charComp.updatePositionIn2dArray();

        //character.lastTilePos = EntityController.getInstance().placeEntity(c.character, character.position);
        //c.character.get(Component.Character.class).updatePositionIn2dArray(); // updates position in 2d entities array if needed

        // Add existing characters to new logged in connection.
//        Iterator<Map.Entry<Integer, CharacterConnection>> i = loggedIn.entrySet().iterator();
//        while (i.hasNext()) {
//            Map.Entry<Integer, CharacterConnection> entry = i.next();
//            CharacterConnection other = entry.getValue();
//            GameRegister.AddCharacter addCharacter = new GameRegister.AddCharacter();
//            // translate to safe to send character data
//            Component.Character comp = other.character;
//            addCharacter.character = comp.toSendToClient();
//            c.sendTCP(addCharacter);
//        }

//        for (CharacterConnection other : loggedIn) {
//            GameRegister.AddCharacter addCharacter = new GameRegister.AddCharacter();
//            // translate to safe to send character data
//            Component.Character comp = other.character.get(Component.Character.class);
//            addCharacter.character = comp.toSendToClient();
//            c.sendTCP(addCharacter);
//        }

        loggedIn.putIfAbsent(character.tag.id, c);

        // dispatch event for ai reactors
        EntityController.getInstance().dispatchEventToAll
                (new Component.Event(Component.Event.Type.PLAYER_ENTERED_AOI,  c.character));

        // sends to client his ID so he can distinguish itself from his list of characters
        GameRegister.ClientId clientId = new GameRegister.ClientId();
        clientId.id = character.tag.id;
        c.sendTCP(clientId);

        // Add logged in character to all connections.
//        GameRegister.AddCharacter addCharacter = new GameRegister.AddCharacter();
//        addCharacter.character = character.toSendToClient();
//        server.sendToAllTCP(addCharacter);
    }

    boolean saveCharacter (Component.Character character) {
        File file = new File("characters", character.tag.name.toLowerCase());
        file.getParentFile().mkdirs();

        if (character.tag.id == 0) {
            String[] children = file.getParentFile().list();
            if (children == null) return false;
            character.tag.id = children.length + 1;
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(file));
            output.writeInt(character.tag.id);
            output.writeInt(character.role_level);
            output.writeFloat(character.position.x);
            output.writeFloat(character.position.y);
            output.writeFloat(character.attr.maxHealth);
            output.writeFloat(character.attr.health);
            output.writeFloat(character.attr.speed);
            output.writeFloat(character.attr.width);
            output.writeFloat(character.attr.height);
            output.writeFloat(character.attr.attackSpeed);
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
        synchronized(loggedIn) { // prepare character updates
            Iterator<Map.Entry<Integer, CharacterConnection>> i = loggedIn.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Integer, CharacterConnection> entry = i.next();
                CharacterConnection charConn = entry.getValue();
                Component.Character charComp = charConn.character;
                charComp.lastState = charComp.buildStateAoI();
                //if(charComp.compareStates(state)) return; // if last state is the same, dont bother send to client

                //state.characterUpdates.add(update);

                if(lagNetwork != null && GameRegister.lagSimulation)
                    lagNetwork.send(charComp.lastState, charConn);
                else
                    charConn.sendUDP(charComp.lastState);
            }

        }

        EntityController.getInstance().clearDamageData(); // clears damage data after sending state to all for the next tick
    }

    Component.Character loadCharacter (Component.Character character) {
        File file = new File("characters", character.tag.name.toLowerCase());
        if (!file.exists()) { // creates char if it does not exist yet
            saveCharacter(character);
            return character;
        }
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(file));
            character.tag.id = input.readInt();
            character.role_level = input.readInt();
            character.position.x = input.readFloat();
            character.position.y = input.readFloat();
            character.attr.maxHealth = input.readFloat();
            character.attr.health = input.readFloat();
            character.attr.speed = input.readFloat();
            character.attr.width = input.readFloat();
            character.attr.height = input.readFloat();
            character.attr.attackSpeed = input.readFloat();
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

    /**
     * Processes interactions requests received by clients
     *
     * @param connection    the character connection that requested the interaction
     * @param interaction the interaction request containing the data of the request
     */
    private void processInteraction(CharacterConnection connection, GameRegister.InteractionRequest interaction) {
        Component.Character character = connection.character;

        // gets the correct entity
        Object entity = null;

        System.out.println(interaction.type);

        Vector2 clientPos = new Vector2(character.position.x, character.position.y);
        Vector2 targetPos = null;
        switch (interaction.entityType) {
            case CHARACTER:
                entity = character.aoIEntities.characters.get(interaction.targetId);
                break;
            case TREE:
                entity = character.aoIEntities.trees.get(interaction.targetId);
                break;
            case CREATURE:
                entity = character.aoIEntities.creatures.get(interaction.targetId);
                break;
            default:
                break;
        }

        if(entity == null) {
            System.out.println("Could not find entity to interact: " + interaction.targetId + " : " + interaction.entityType);
            return; // could not find entity to interact with
        }

        character.target.id = interaction.targetId;
        character.target.type = interaction.entityType;
        character.target.entity = entity;
        character.target.timestamp = interaction.timestamp;
        character.target.isAlive = true; // only receives interactions if entity is alive

        // new target of character
        if(interaction.type != GameRegister.Interaction.STOP_INTERACTION) {
            // calculates new direction based on new target
            targetPos = character.target.getPosition();
            if(targetPos != null) {
                character.dir = targetPos.sub(clientPos).nor();
                startTimerInteraction(connection, interaction.type);
            }
        } else { // if client has stopped interaction, stop interaction
            character.stopInteraction();
        }

    }

    private void startTimerInteraction(CharacterConnection connection, GameRegister.Interaction type) {
        Component.Character character = connection.character;

        switch (type) {
            case ATTACK_ENTITY:
                //if(!character.isInteracting) // only start attacking if no interactions are happening at the moment
                    character.attack();
                break;
            case STOP_INTERACTION: // THIS SHOULD NOT BE THE CASE EVER!
                character.stopInteraction();
                character.target.id = -1;
                character.target.type = null;
                character.target.entity = null;
                return;
            default:
                break;
        }
    }

//    /**
//     * Processes attack requests received by clients
//     *
//     * @param connection    the character connection that is requesting attack
//     * @param target        the target entity that is receiving the attack
//     */
//    private void startTimerInteraction(CharacterConnection connection, GameRegister.Interaction interaction) {
//        Component.Character character = connection.character;
//
//        switch (target.type) {
//            case CHARACTER:
//                Component.Character targetCharacter = (Component.Character) target.entity;
//                targetCharacter.attr.health-=5f;
//                break;
//            case TREE:
//                GameRegister.Tree tree = (GameRegister.Tree) target.entity;
//                tree.health-=5f;
//                break;
//            case CREATURE:
//                Entity targetCreature = (Entity) target.entity;
//                targetCreature.get(Component.Attributes.class).health-=5f;
//                break;
//            default:
//                break;
//        }
//    }

    public void stop() {
        if(isOnline) {
            // TODO: SAVE GAME STATE BEFORE ENDING
            Log.info("game-server", "Game server is stopping...");
            server.stop();
            server.close();
            Log.info("game-server", "Game server has stopped!");
            entityController.dispose();
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
            case ATTRIBUTE:
                changePlayerAttribute(cmd);
                break;
            default:
                break;
        }
    }

    /**
     * Change attributes of online players
     * @param cmd   the command received from console containing the necessary data to change attribute
     */
    private void changePlayerAttribute(Command cmd) {
        String pName = cmd.getArgs()[0];
        String attrName = cmd.getArgs()[1];
        float value = -1;
        try {
            value = Float.parseFloat(cmd.getArgs()[2]);
        } catch (NumberFormatException e) {
            Log.info("cmd", "Value parameter should be a float number (use . instead of ,)");
            return;
        }

        System.out.println(pName + " / " + attrName + " / " + value);

        Component.Character c = null;
        boolean foundPlayerOnline = false;
        synchronized(loggedIn) {
            Iterator<Map.Entry<Integer, CharacterConnection>> i = loggedIn.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Integer, CharacterConnection> entry = i.next();
                CharacterConnection connectChar = entry.getValue();
                c = connectChar.character;

                if(c.tag.name.equals(pName)) {
                    foundPlayerOnline = true;
                    break;
                }
            }
        }
        if(!foundPlayerOnline || c == null) {
            Log.info("cmd", "Player "+pName+" is not online");
            return;
        }

        switch (attrName) {
            case "speed":
                if(value >= 1.0 && value <= 9.0)
                    c.attr.speed = value;
                else {
                    Log.info("cmd", "Attribute " + attrName + " value should be between 1.0 and 9.0");
                    return;
                }
                break;
            case "range":
                c.attr.range = value;
                break;
            case "attack_speed":
                c.attr.attackSpeed = value;
                break;
            default:
                Log.info("cmd", "Attribute "+attrName+" is not valid to change (only speed, attack_speed and range can be changed)");
                return;
        }

        Log.info("cmd", "Changed player "+pName+" " +attrName+ " to "+value);
        saveCharacter(c);

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
            Iterator<Map.Entry<Integer, CharacterConnection>> i = loggedIn.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Integer, CharacterConnection> entry = i.next();
                CharacterConnection connectChar = entry.getValue();
                Component.Character c = connectChar.character;
                if(c.tag.id == conn.charData.id) {
                    conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.USER_ALREADY_LOGGED_IN));
                    conn.close();
                    return;
                }
            }
        }
        Component.Character character = null;
        synchronized(registeredTokens) { // check if it has token already
            Iterator i = registeredTokens.iterator();
            while (i.hasNext()) {
                character = (Component.Character) i.next();
                if(character.tag.id == conn.charData.id) { // has token, send token without generating one
                    sendTokenAsync(conn, character.token);
                    Log.debug("login-server", character.tag.name+" is registered and has a token: "+character.token);
                    return;
                }
            }
            // if char does not have a token, generate a new one
            /** CHARACTER WILL BE LOADED LATER (IF THERE IS NO SAVED CHAR - THIS IS THE FIRST LOGIN
             SO THESE ARE THE INITIAL ATTRIBUTES OF A NEWLY MADE CHARACTER!! NEW PLAYER NEW CHARACTER!! **/
            character = new Component.Character();
            character.token = Encoder.generateNewToken();
            character.role_level = conn.charData.roleLevel;
            character.tag = new Component.Tag(conn.charData.id, conn.charData.character);
            character.position = new Component.Position(26, 4);
            character.attr = new Component.Attributes(32f*RogueFantasyServer.world.getUnitScale(), 48f*RogueFantasyServer.world.getUnitScale(),
                                100f, 100f, 250f*RogueFantasyServer.world.getUnitScale(),1f,
                                10f*RogueFantasyServer.world.getUnitScale());
            Log.debug("login-server", character.tag.name+" is NOT registered, new token generated! "+character.token);
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

    /**
     * This method should be called when teleporting characters
     * to force no interpolation and instant change of position on client
     * @param character     the character to teleport with position already updated on destination of teleport
     */
    public void teleport(Component.Character character) {
        GameRegister.Teleport tp = new GameRegister.Teleport();
        tp.x = character.position.x; tp.y = character.position.y;
        character.connection.sendTCP(tp);
    }

    // This holds per connection state.
    public static class CharacterConnection extends Connection {
        public Component.Character character;

        public CharacterConnection() {
            character = new Component.Character();
        }
    }

}
