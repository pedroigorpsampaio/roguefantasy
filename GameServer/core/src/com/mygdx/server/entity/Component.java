package com.mygdx.server.entity;

import static com.mygdx.server.entity.WorldMap.TILES_HEIGHT;
import static com.mygdx.server.entity.WorldMap.TILES_WIDTH;
import static com.mygdx.server.network.GameRegister.N_COLS;
import static com.mygdx.server.network.GameRegister.N_ROWS;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.server.network.GameRegister;
import com.mygdx.server.ui.RogueFantasyServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dev.dominion.ecs.api.Entity;


public class Component {
    public static class Character {
        public String token;
        public int role_level;
        public long lastMoveId = 0;
        public long lastMoveTs = 0;
        public Vector2 dir;
        public Component.Tag tag;
        public Component.Attributes attr;
        public Component.Position position;
        public Vector2 lastTilePos = null;

        /**
         * Prepares character data to be sent to clients with
         * only data that is safe to send and it is of clients interest for login
         * @return  the Register character class containing the data safe to send
         */
        public GameRegister.Character toSendToClient() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.tag.id; charData.speed = this.attr.speed;
            charData.x = this.position.x; charData.y = this.position.y; charData.name = this.tag.name; this.lastMoveId = 0;
            return charData;
        }

        /**
         * Prepares character data to be sent to clients with
         * data necessary to update this character in clients
         * @return  the update character message containing the data to update this character in clients
         */
        public GameRegister.UpdateCharacter getCharacterData() {
            GameRegister.UpdateCharacter update = new GameRegister.UpdateCharacter();
            update.character = new GameRegister.Character();
            update.character.id = tag.id;
            update.character.x = position.x;
            update.character.y = position.y;
            update.character.name = tag.name;
            update.character.speed = attr.speed;
            update.character.role_level = role_level;
            update.dir = dir;
            update.lastRequestId = lastMoveId;
            return update;
        }

        /**
         * Moves character position and updates itself on 2d array grid if needed (changed tile)
         * @param movement  the movement to increase into character position
         */
        public void move(Vector2 movement, Entity entity) {
            this.position.x += movement.x;
            this.position.y += movement.y;
            updatePositionIn2dArray(entity); // calls method that manages entity position in 2d array
        }

        /**
         * Checks and updates position in 2d Array of entities if different from last one
         */
        public void updatePositionIn2dArray(Entity entity) {
            position.updatePositionIn2dArray(entity);
        }

        /**
         * Method responsible for building the message containing the state of AoI of player
         * which includes visible tilemap and entities such as other players, creatures, npcs etc...
         * @return  the state message to be sent to this client containing all of its AoI data
         */
        public GameRegister.UpdateState buildStateAoI() {
            GameRegister.UpdateState state = new GameRegister.UpdateState(); // the state message
            // get this players position in isometric tile map
            Vector2 tPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(this.position.x, this.position.y));
            float pX = tPos.x;
            float pY = tPos.y;
            // clamp AoI limits of player
            int minI = MathUtils.ceil(pY - N_ROWS / 2);
            int maxI = MathUtils.ceil(pY + N_ROWS / 2)-1;

            int minJ = MathUtils.ceil(pX - N_COLS / 2);
            int maxJ = MathUtils.ceil(pX + N_COLS / 2)-1;
            // clamp map limits but always send same amount of info
            if (minI < 0) {
                maxI -= minI;
                minI = 0;
            }
            if (minJ < 0) {
                maxJ -= minJ;
                minJ = 0;
            }
            if (maxI > TILES_HEIGHT) {
                minI += TILES_HEIGHT - maxI;
                maxI = TILES_HEIGHT;
            }
            if (maxJ > TILES_WIDTH) {
                minJ += TILES_WIDTH - maxJ;
                maxJ = TILES_WIDTH;
            }

            state.tileOffsetX = minJ;
            state.tileOffsetY = minI;

            TiledMap map = RogueFantasyServer.world.getMap();
            for (MapLayer mapLayer : map.getLayers()) { // iterate through world map layers
                if (mapLayer.getClass().equals(TiledMapTileLayer.class)) { // only iterates tile map layers TODO: object layer can be at client side also? or should it be sent?
                    TiledMapTileLayer layerBase = (TiledMapTileLayer) mapLayer;
                    // add aoi tiles as a layer of the state message
                    GameRegister.Layer aoiLayer = new GameRegister.Layer();
                    aoiLayer.isEntityLayer = layerBase.getProperties().get("entity_layer", Boolean.class);
                    // stores visible tiles in the aoi 2d Array of tiles
                    for (int row = minI; row <= maxI; row++) {
                        for (int col = minJ; col <= maxJ; col++) {
                            final TiledMapTileLayer.Cell cell = layerBase.getCell(col, row);
                            if (cell != null)
                                aoiLayer.tiles[col - minJ][row - minI] = (short) cell.getTile().getId();
                            else
                                aoiLayer.tiles[col - minJ][row - minI] = -1; // represents null tile/cell

                            // if its entity layer, prepare creature and character updates of the ones that are in range
                            if (layerBase.getProperties().get("entity_layer", Boolean.class)) {
                                Map<Integer, Entity> tileEntities = EntityController.getInstance().getEntitiesAtTilePos(col, row);
                                if (tileEntities.size() > 0) { // for each visible tile that has entities
                                    synchronized (tileEntities) {
                                        for (Map.Entry<Integer, Entity> entry : tileEntities.entrySet()) {
                                            Entity entity = entry.getValue();
                                            if (!entity.has(Component.Character.class)) {// non-player entity // TODO: SEPARATE TYPES OF ENTITIES!!
                                                if (entity.get(Component.Position.class) == null)
                                                    continue;
                                                state.creatureUpdates.add(EntityController.getInstance().getCreatureData(entity));
                                            } else { // player entity
                                                if (entity.get(Component.Character.class) == null)
                                                    continue;
                                                state.characterUpdates.add(entity.get(Component.Character.class).getCharacterData());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    state.tileLayers.add(aoiLayer);
                }
            }
            //state.tileLayers = aoiLayers;
            return state;
        }
    }

    public static class Position {
        public float x, y;
        public float lastVelocityX = 0, lastVelocityY = 0; // the last velocities that changed the position values
        public Vector2 lastTilePos = null; // the last position in tile state 2d array (i,j)

        public Position(float x, float y) {this.x = x; this.y = y;}

        /**
         * Checks and updates position in 2d Array of entities if different from last one
         */
        public void updatePositionIn2dArray(Entity entity) {
            // calculate current tilePos
            Vector2 tPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(x, y));

//            System.out.printf("Entity moved to %s\n",
//                    new Vector2(x, y));

           // System.out.println(tPos);

            // makes sure its within world state bounds
            if(tPos.x < 0) tPos.x = 0;
            if(tPos.y < 0) tPos.y = 0;
            if(tPos.x > TILES_WIDTH) tPos.x = TILES_WIDTH-1;
            if(tPos.y > TILES_HEIGHT) tPos.y = TILES_HEIGHT-1;

            int id = -1;
            if(!entity.has(Component.Character.class)) {// non-player entity
                id = entity.get(Tag.class).id;
            }
            else // player entity
                id = entity.get(Component.Character.class).tag.id;

            // if it has no last tile (null tilepos) just add to the 2d array
            if(lastTilePos == null) {
                EntityController.getInstance().entityWorldState[(int) tPos.x][(int) tPos.y].entities.putIfAbsent(id, entity);
                lastTilePos = new Vector2(tPos.x, tPos.y); // create last tile position vector2
            }
            else {
                if(lastTilePos.equals(tPos)) // if its the same, there is no need to update in 2d state array
                    return;
                // if its different:
                // remove from old position in state array
                EntityController.getInstance().entityWorldState[(int)lastTilePos.x][(int)lastTilePos.y].entities.remove(id);
                // add in new position
                EntityController.getInstance().entityWorldState[(int)tPos.x][(int)tPos.y].entities.putIfAbsent(id, entity);
//                System.out.printf("Entity moved from %s to %s\n",
//                        lastTilePos, tPos);
                // update last position
                lastTilePos.x = tPos.x; lastTilePos.y = tPos.y;
            }
        }

        @Override
        public String toString() {
            return "Position: {x=" + x + ", y=" + y + '}';
        }
    }

    public static class Velocity {
        public float x, y;

        public Velocity(float x, float y) {this.x = x; this.y = y;}

        @Override
        public String toString() {
            return "Velocity: {x=" + x + ", y=" + y + '}';
        }
    }

    public static class Tag {
        public int id;
        public String name;
        public Tag(int id) {this.id = id;}
        public Tag(int id, String name) {this.id = id; this.name = name;}
    }

    public static class Attributes {
        public float width, height, speed, attackSpeed, range;
        public Attributes(float width, float height, float speed, float attackSpeed, float range) {
            this.width = width; this.height = height; this.attackSpeed = attackSpeed;
            this.speed = speed; this.range = range;
        }
    }

    public static class Spawn {
        public long id; // spawn id
        public Position position; // spawn position
        public float respawnTime; // time between respawns in seconds

        public Spawn(long id, Position position, float respawnTime) {
            this.position = position; this.respawnTime = respawnTime;
        }
    }

    public static class Event {

        public enum Type { // types of events
            PLAYER_ENTERED_AOI,
            TARGET_IN_RANGE, TARGET_OUT_OF_RANGE, PLAYER_LEFT_AOI
        }
        private Type type; // the type of this event
        public Entity trigger; // the entity responsible for triggering the event

        public Event(Type type, Entity trigger) {this.type = type; this.trigger = trigger;}
    }

    public static class AI {
        private final Entity body; // the body that this ai controls (the entity with AI component)
        public Entity target = null;
        public State state = State.IDLE;

        public AI(Entity body) {
            this.body = body;
        }

        public enum State {
            WALKING("WALKING"),
            RUNNING("RUNNING"),
            IDLE("IDLE"),
            IDLE_WALKING("IDLE_WALKING"),
            FOLLOWING("FOLLOWING"),
            ATTACKING("ATTACKING"),
            FLEEING("FLEEING"),
            DYING("DYING");

            public String name;

            State(String name) {
                this.name = name;
            }

            public String getName() {
                return this.name;
            }

            public static State getStateFromName(String name){
                for (State st : State.values()) {
                    if (st.name.equalsIgnoreCase(name)) {
                        return st;
                    }
                }
                //throw new Exception("No enum constant with text " + text + " found");
                return null;
            }

        }

        // responsible for reacting to events
        public void react(Event event) {
            switch(event.type) {
                case PLAYER_ENTERED_AOI:
                case TARGET_OUT_OF_RANGE:
                    this.state = State.FOLLOWING;
                    target = event.trigger;
                    break;
                case PLAYER_LEFT_AOI:
                    if (target != null && target.equals(event.trigger)) {
                        // TODO: SEARCH FOR OTHER TARGET IN AOI RANGE
                        // if there is no target close, idle walk or do nothing?
                        this.state = State.IDLE_WALKING;
                        Component.Position position = body.get(Component.Position.class);
                        Component.Spawn spawn = body.get(Component.Spawn.class);
                        spawn.position.x = position.x;
                        spawn.position.y = position.y;
                        target = null;
                    }
                    break;
                case TARGET_IN_RANGE:
                    this.state = State.ATTACKING;
                    break;
                default:
                    break;
            }
        }

        // responsible for controlling the AI in the loop
        public void act() {
            switch(state) {
                case IDLE_WALKING:
                    idleWalk();
                    break;
                case FOLLOWING:
                    follow();
                    break;
                case ATTACKING:
                    attack();
                    break;
                default:
                    break;
            }
        }

        // attack if in range
        private void attack() {
            if(target == null) return; // no target to attack

            Component.Position position = body.get(Component.Position.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);

            Component.Position targetPos = null;
            Component.Attributes targetAttr = null;
            if(!target.has(Component.Character.class)) {
                targetPos = target.get(Component.Position.class);
                targetAttr = target.get(Component.Attributes.class);
            } else {
                targetPos = target.get(Component.Character.class).position;
                targetAttr = target.get(Component.Character.class).attr;
            }

            Vector2 goalPos = new Vector2(targetPos.x - targetAttr.width/2f, targetPos.y - targetAttr.height/2f);
            Vector2 aiPos = new Vector2(position.x, position.y);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) > attributes.range) { // not close enough, do not attack anymore
                react(new Event(Event.Type.TARGET_OUT_OF_RANGE, target));
                return;
            }

            // TODO: ATTACK TARGET
        }

        // just follows target until in range
        private void follow() {
            if(target == null) return; // no target to follow

            Component.Position position = body.get(Component.Position.class);
            Component.Velocity velocity = body.get(Component.Velocity.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);

            Component.Position targetPos = null;
            Component.Attributes targetAttr = null;
            if(!target.has(Component.Character.class)) {
                targetPos = target.get(Component.Position.class);
                targetAttr = target.get(Component.Attributes.class);
            } else {
                targetPos = target.get(Component.Character.class).position;
                targetAttr = target.get(Component.Character.class).attr;
            }

//            System.out.printf("Target %s\n",
//                    targetAttr.width/2f);

            Vector2 goalPos = new Vector2(targetPos.x - targetAttr.width/4f, targetPos.y + targetAttr.height/4f);
            Vector2 aiPos = new Vector2(position.x, position.y);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) <= attributes.range) { // close enough, do not move anymore
                velocity.x = 0; velocity.y = 0;
                react(new Event(Event.Type.TARGET_IN_RANGE, target));
                return;
            }

            velocity.x = deltaVec.x; velocity.y = deltaVec.y;
        }

        private void idleWalk() {
            Component.Position position = body.get(Component.Position.class);
            Component.Velocity velocity = body.get(Component.Velocity.class);
            Component.Spawn spawn = body.get(Component.Spawn.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);
            if(target == null) { // move aimlessly
                if(position.x <= spawn.position.x && position.y <= spawn.position.y) {
                    velocity.x = 1 * attributes.speed * GameRegister.clientTickrate();
                    velocity.y = 0;
                }
                if (position.x >= spawn.position.x + 5 && position.y <= spawn.position.y) {
                    velocity.x = 0;
                    velocity.y = 1 * attributes.speed * GameRegister.clientTickrate();
                }
                if(position.x >= spawn.position.x + 5 && position.y >= spawn.position.y + 5) {
                    velocity.x = -1 * attributes.speed * GameRegister.clientTickrate();
                    velocity.y = 0;
                }
                if(position.x <= spawn.position.x && position.y >= spawn.position.y + 5){
                    velocity.x = 0;
                    velocity.y = -1 * attributes.speed * GameRegister.clientTickrate();
                }
            }
        }
    }

}
