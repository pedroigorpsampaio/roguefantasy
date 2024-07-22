package com.mygdx.server.entity;

import static com.mygdx.server.entity.WorldMap.TEX_HEIGHT;
import static com.mygdx.server.entity.WorldMap.TEX_WIDTH;
import static com.mygdx.server.entity.WorldMap.TILES_HEIGHT;
import static com.mygdx.server.entity.WorldMap.TILES_WIDTH;
import static com.mygdx.server.entity.WorldMap.unitScale;
import static com.mygdx.server.network.GameRegister.N_COLS;
import static com.mygdx.server.network.GameRegister.N_ROWS;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.mygdx.server.network.GameRegister;
import com.mygdx.server.network.GameServer;
import com.mygdx.server.ui.RogueFantasyServer;
import com.mygdx.server.util.Common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import dev.dominion.ecs.api.Entity;


public class Component {
    static float rectOffsetUp = 0.2f, rectOffsetDown = 0.2f, rectOffsetLeft = 0.4f, rectOffsetRight = 0.4f;
    public enum Direction {
        NORTHWEST(-1, 1),
        NORTH(0, 1),
        NORTHEAST(1, 1),
        EAST(1, 0),
        SOUTHEAST(1, -1),
        SOUTH(0, -1),
        SOUTHWEST(-1, -1),
        WEST(-1, 0)
        ;

        public int dirX, dirY;

        Direction(int x, int y) {
            this.dirX = x;
            this.dirY = y;
        }

        public static Direction getDirection(int x, int y){
            //System.out.println(x + " / \\ " + y);
            for (Direction dir : Direction.values()) {
                if (dir.dirX == x && dir.dirY == y) {
                    return dir;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return Direction.SOUTH;// if no direction was found, return south as default
        }

        public static Vector2 fromDirection(Direction dir) {
            return new Vector2(dir.dirX, dir.dirY);
        }
    }

    public static class Tree {
        public String name;
        public int treeId, tileId;
        public ArrayList<GameRegister.Damage> damages = new ArrayList<>();
        public int tileX, tileY;
        public float[] hitBox;
        public Component.Attributes attr;
        public Component.Spawn spawn;

        /**
         * Called when character is hit
         * @param attackerId the id of the attacker
         * @param attackerType the type of the attacker
         * @param attacker  the attributes of the attacker
         * @return if entity is alive after hit
         */
        public boolean hit(int attackerId, GameRegister.EntityType attackerType, Attributes attacker) {
            if(attr.health > 0) {
                GameRegister.Damage dmg = new GameRegister.Damage();
                dmg.attackerId = attackerId;
                dmg.attackerType = attackerType;
                dmg.type = GameRegister.DamageType.NORMAL;
                dmg.value = (int) attacker.attack;
                attr.health -= dmg.value;
                this.damages.add(dmg);
                EntityController.getInstance().damagedEntities.trees.put(spawn.id, this);
            }

            if(attr.health <= 0) {
                spawn.respawn(this);
                return false;
            }
            return true;
        }
    }

    public static class Character {
        public String token;
        public int role_level;
        public final static GameRegister.EntityType ENTITY_TYPE = GameRegister.EntityType.CHARACTER;
        public GameRegister.AttackType attackType = null;
        public long lastMoveId = 0;
        public long lastMoveTs = 0;
        public Vector2 dir;
        public Component.Tag tag;
        public Component.Attributes attr;
        public Component.Position position;
        public Vector2 lastTilePos = null;
        public GameServer.CharacterConnection connection = null;
        public GameRegister.EntityState state = GameRegister.EntityState.FREE;
        public GameRegister.UpdateState lastState = null; // last state sent to this character
        public boolean isTeleporting = false;
        public ArrayList<GameRegister.Damage> damages = new ArrayList<>(); // list of recent damages
        public Target target = new Target(); // player current target
        public AoIEntities aoIEntities = new AoIEntities(); // current AoI entities of player (last game state)
        public int avgLatency = 0; // average latency of this player
        protected Timer interactionTimer=new Timer(); // timer that controls the interaction of this character
        //public boolean isInteracting = false; // flag that controls if this character is interacting at any given time

        /**
         * Prepares character data to be sent to clients with
         * only data that is safe to send and it is of clients interest for login
         * @return  the Register character class containing the data safe to send
         */
        public GameRegister.Character toSendToClient() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.tag.id; charData.speed = this.attr.speed;
            charData.x = this.position.x; charData.y = this.position.y; charData.name = this.tag.name;
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
            for(GameRegister.Damage dmg : damages)
                update.character.damages.add(dmg);
            update.character.x = position.x;
            update.character.y = position.y;
            update.character.name = tag.name;
            update.character.health = attr.health;
            update.character.maxHealth = attr.maxHealth;
            update.character.speed = attr.speed;
            update.character.role_level = role_level;
            update.character.state = state;
            update.character.avgLatency = avgLatency;
            update.character.attackSpeed = attr.attackSpeed;
            update.dir = dir;
            update.lastRequestId = lastMoveId;
            update.character.isTeleporting = isTeleporting;
            update.character.attackType = attackType;
            update.character.targetId = target.id;
            update.character.targetType = target.type;
            return update;
        }

        /**
         * Teleports character to position and updates itself on 2d array
         * @param destination  the destination to teleport character to
         */
        public void teleport(Vector2 destination) {
            isTeleporting = true;
            this.position.x = destination.x;
            this.position.y = destination.y;
            state = GameRegister.EntityState.TELEPORTING_IN;
            // update position in 2d array
            updatePositionIn2dArray();
            // check for actions on current tile (actions that get triggered by walking on it)
            triggerWalkableActions();
            // stops any current interactions
            stopInteraction(false);
        }

        /**
         * Moves character position and updates itself on 2d array grid if needed (changed tile)
         * @param movement  the movement to increase into character position
         */
        public void move(Vector2 movement) {
            this.position.x += movement.x;
            this.position.y += movement.y;
            // update position in 2d array
            updatePositionIn2dArray();
            // check for actions on current tile (actions that get triggered by walking on it)
            triggerWalkableActions();
        }

        /**
         * Trigger walk on actions of the map such as teleports in case player walks in on one
         */
        private void triggerWalkableActions() {
            if(position.lastTilePos == null) return; // player tile not set yet

            // current tile
            Tile tileCenter = EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x][(int) position.lastTilePos.y];
            // also checks tiles around player for collision with action objects
            ArrayList<Tile> tiles = new ArrayList<>();
            tiles.add(tileCenter);
            if(position.lastTilePos.x+1 < EntityController.getInstance().entityWorldState.length)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x+1][(int) position.lastTilePos.y]);
            if(position.lastTilePos.x+1 < EntityController.getInstance().entityWorldState.length &&
                    position.lastTilePos.y+1 < EntityController.getInstance().entityWorldState[0].length)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x+1][(int) position.lastTilePos.y+1]);
            if(position.lastTilePos.x-1 >= 0)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x-1][(int) position.lastTilePos.y]);
            if(position.lastTilePos.x-1 >= 0 && position.lastTilePos.y-1 >= 0)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x-1][(int) position.lastTilePos.y-1]);
            if(position.lastTilePos.x+1 < EntityController.getInstance().entityWorldState.length  && position.lastTilePos.y-1 >= 0)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x+1][(int) position.lastTilePos.y-1]);
            if(position.lastTilePos.x-1 >= 0 && position.lastTilePos.y+1 < EntityController.getInstance().entityWorldState[0].length)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x-1][(int) position.lastTilePos.y+1]);
            if(position.lastTilePos.y+1 < EntityController.getInstance().entityWorldState[0].length)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x][(int) position.lastTilePos.y+1]);
            if(position.lastTilePos.y-1 >= 0)
                tiles.add(EntityController.getInstance().entityWorldState[(int) position.lastTilePos.x][(int) position.lastTilePos.y-1]);

            Vector2 cPos = new Vector2();
            cPos.x = this.position.x + this.attr.width/2f + this.attr.width/15f;
            cPos.y = this.position.y + this.attr.height/12f + this.attr.height/18f;

//            Vector2 tPosDown = new Vector2(centerPos.x, centerPos.y-rectOffsetDown*0.25f);
//            Vector2 tPosUp = new Vector2(centerPos.x, centerPos.y+rectOffsetUp*0.75f);
//            Vector2 tPosLeft = new Vector2(centerPos.x-rectOffsetLeft*0.5f, centerPos.y);
//            Vector2 tPosRight = new Vector2(centerPos.x+rectOffsetRight*0.5f, centerPos.y);

            Polygon playerIsoBox = new Polygon(new float[]{
                    cPos.x, cPos.y-rectOffsetDown*0.15f, // down
                    cPos.x-rectOffsetLeft*0.4f, cPos.y, // left
                    cPos.x, cPos.y+rectOffsetUp*0.75f, // up
                    cPos.x+rectOffsetRight*0.4f, cPos.y}); // right

            for(Tile tile : tiles) {
                if (tile.portal != null) { // if there is a portal in this tile, check if it collides with it
                    if(Common.intersects(playerIsoBox, tile.portal.hitBox)) {
                        teleport(new Vector2(tile.portal.destX, tile.portal.destY)); // teleports in world
                        GameServer.getInstance().teleport(this); // sends teleport msg to connection
                    }
                }
            }
        }

        /**
         * Checks if movement is possible (not blocked by walls, unwalkable tiles, other collisions...)
         * @param msg   the move character message containing the desired movement to be checked
         * @return  true if move is possible, false otherwise
         */
        public boolean isMovePossible(GameRegister.MoveCharacter msg) {
            Vector2 futurePos;
            if (msg.hasEndPoint) { // if it has endpoint, do the movement calculations
                Vector2 touchPos = new Vector2(msg.xEnd, msg.yEnd);
                Vector2 charPos = new Vector2(this.position.x, this.position.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.attr.speed * GameRegister.clientTickrate());
                futurePos = new Vector2(charPos).add(deltaVec);
            } else { // wasd movement already has direction in it, just normalize and scale
                Vector2 moveVec = new Vector2(msg.x, msg.y).nor().scl(this.attr.speed * GameRegister.clientTickrate());
                futurePos = new Vector2(this.position.x, this.position.y).add(moveVec);
            }

            if(!RogueFantasyServer.world.isWithinWorldBounds(futurePos) ||
                    !RogueFantasyServer.world.isWalkable(futurePos))
                return false;

            // check if has jumped more than one tile in this movement (forbidden!)
            Vector2 tInitialPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(this.position.x, this.position.y));
            Vector2 tFuturePos = RogueFantasyServer.world.toIsoTileCoordinates(futurePos);
            if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                return false;

            return true;
        }

        /**
         * Given an initial movement that collided, search for a direction to slide and return a possible movement
         * @param initMove  the initial move that collided
         * @return  the new movement with the new direction to slide on collision
         */
        public Vector2 findSlide(GameRegister.MoveCharacter initMove) {
            Vector2 newMove = new Vector2(0,0);
            Vector2 moveVec = new Vector2(initMove.x, initMove.y).nor().scl(this.attr.speed * GameRegister.clientTickrate());
            Vector2 moveVecCounter = new Vector2(initMove.x, initMove.y).nor().scl(this.attr.speed * GameRegister.clientTickrate());
            // degree step for each rotation try
            float degree = 2f;

            if (initMove.hasEndPoint) { // if it has endpoint, do the movement calculations accordingly
                Vector2 touchPos = new Vector2(initMove.xEnd, initMove.yEnd);
                Vector2 charPos = new Vector2(this.position.x, this.position.y);
                Vector2 deltaVec = new Vector2(touchPos).sub(charPos);
                deltaVec.nor().scl(this.attr.speed * GameRegister.clientTickrate());
                moveVecCounter.x = deltaVec.x;
                moveVecCounter.y = deltaVec.y;
                moveVec.x = deltaVec.x;
                moveVec.y = deltaVec.y;
                degree /= 1.1f;
            }

            Vector2 futurePos = new Vector2(this.position.x, this.position.y).add(moveVec);
            Vector2 futurePosCounter = new Vector2(this.position.x, this.position.y).add(moveVecCounter);

            int tries = 0;
            while(tries < 45) { // search for new angle to move
                Vector2 tInitialPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(this.position.x, this.position.y));
                Vector2 tFuturePos = RogueFantasyServer.world.toIsoTileCoordinates(futurePos);
                Vector2 tFuturePosCounter = RogueFantasyServer.world.toIsoTileCoordinates(futurePosCounter);
                boolean isWithinOneTile = true;
                boolean isWithinOneTileCounter = true;
                if(Math.abs(tInitialPos.x-tFuturePos.x) > 1 || Math.abs(tInitialPos.y-tFuturePos.y) > 1)
                    isWithinOneTile = false;
                if(Math.abs(tInitialPos.x-tFuturePosCounter.x) > 1 || Math.abs(tInitialPos.y-tFuturePosCounter.y) > 1)
                    isWithinOneTileCounter = false;
                if(RogueFantasyServer.world.isWithinWorldBounds(futurePos) &&
                        RogueFantasyServer.world.isWalkable(futurePos) &&
                        isWithinOneTile) { // found a new movement to make searching clockwise
                    newMove.x = moveVec.x;
                    newMove.y = moveVec.y;
                    break;
                }
                if(RogueFantasyServer.world.isWithinWorldBounds(futurePosCounter) &&
                        RogueFantasyServer.world.isWalkable(futurePosCounter) &&
                        isWithinOneTileCounter) { // found a new movement to make searching counter-clockwise
                    newMove.x = moveVecCounter.x;
                    newMove.y = moveVecCounter.y;
                    break;
                }
                moveVecCounter.rotateDeg(degree);
                moveVec.rotateDeg(-degree);
//                System.out.println("Rot: " + moveVec + " / " + moveVecCounter);
                futurePos = new Vector2(this.position.x, this.position.y).add(moveVec);
                futurePosCounter = new Vector2(this.position.x, this.position.y).add(moveVecCounter);
                tries++;
            }
            return newMove;
        }

        /**
         * Checks and updates position in 2d Array of characters if different from last one
         */
        public void updatePositionIn2dArray() {
            position.updatePositionIn2dArray(this);
        }

        /**
         * Removes itself from the 2d array of characters
         */
        public void removeFrom2dArray() {
            Vector2 tPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(position.x, position.y));

            // makes sure its within world state bounds
            if(tPos.x < 0) tPos.x = 0;
            if(tPos.y < 0) tPos.y = 0;
            if(tPos.x > TILES_WIDTH) tPos.x = TILES_WIDTH-1;
            if(tPos.y > TILES_HEIGHT) tPos.y = TILES_HEIGHT-1;

            EntityController.getInstance().entityWorldState[(int)tPos.x][(int)tPos.y].characters.remove(tag.id);
        }

        /**
         * Method responsible for building the message containing the state of AoI of player
         * which includes visible tilemap and entities such as other players, creatures, npcs etc...
         * @return  the state message to be sent to this client containing all of its AoI data
         */
        public GameRegister.UpdateState buildStateAoI() {
            GameRegister.UpdateState state = new GameRegister.UpdateState(); // the state message
            // reset aoi entities helper data
            aoIEntities = new AoIEntities();
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
                    // stores visible tiles in the aoi 2d Array of tiles
                    for (int row = minI; row <= maxI; row++) {
                        for (int col = minJ; col <= maxJ; col++) {
                            final TiledMapTileLayer.Cell cell = layerBase.getCell(col, row);
                            if (!layerBase.getProperties().get("entity_layer", Boolean.class)) {
                                if (cell != null)
                                    aoiLayer.tiles[col - minJ][row - minI] = (short) cell.getTile().getId();
                                else
                                    aoiLayer.tiles[col - minJ][row - minI] = -1; // represents null tile/cell
                            } else {  // if its entity layer, prepare creature and character updates of the ones that are in range as well as walls
                                synchronized ( EntityController.getInstance().entityWorldState[col][row].entities) {
                                    Map<Integer, Entity> tileEntities = EntityController.getInstance().entityWorldState[col][row].entities;
                                    if (tileEntities.size() > 0) { // for each visible tile that has entities
                                        Iterator<Entity> iterator = tileEntities.values().iterator();
                                        while (iterator.hasNext()) {
                                            Entity entity = iterator.next();
                                            if (entity.get(Component.Position.class) == null)
                                                    continue;
                                            // let entity know that this player is in AoI of it
                                            entity.get(AI.class).react(new Event(Event.Type.PLAYER_IN_AOI, this));
                                            state.creatureUpdates.add(EntityController.getInstance().getCreatureData(entity));
                                            aoIEntities.creatures.put(entity.get(Component.Spawn.class).id, entity);
                                        }
                                    }
                                }
                                synchronized ( EntityController.getInstance().entityWorldState[col][row].characters) { // for each character
                                    Map<Integer, Character> tileCharacters = EntityController.getInstance().entityWorldState[col][row].characters;
                                    if (tileCharacters.size() > 0) { // for each visible tile that has entities
                                        Iterator<Character> iterator = tileCharacters.values().iterator();
                                        while (iterator.hasNext()) {
                                            Character character = iterator.next();
                                            if (character == null)
                                                continue;
                                            state.characterUpdates.add(character.getCharacterData());
                                            aoIEntities.characters.put(character.tag.id, character);
                                        }
                                    }
                                }
                                if(EntityController.getInstance().entityWorldState[col][row].wall != null) { // add wall as wall update for client that treats it as entity
                                    state.wallUpdates.add(EntityController.getInstance().entityWorldState[col][row].wall);
                                    if(WorldMap.getInstance().getTileFromId(EntityController.getInstance()
                                                    .entityWorldState[col][row].wall.tileId).getProperties()
                                                    .get("obfuscator", Boolean.class))
                                        aoIEntities.obfuscatorWalls.put(EntityController.getInstance().entityWorldState[col][row].wall.wallId,
                                                                        EntityController.getInstance().entityWorldState[col][row].wall);
                                }
                                if(EntityController.getInstance().entityWorldState[col][row].portal != null) { // add portal
                                    state.portal.add(EntityController.getInstance().entityWorldState[col][row].portal.hitBox);
                                }
                                if(EntityController.getInstance().entityWorldState[col][row].tree != null) { // add tree
                                    GameRegister.Tree tree = new GameRegister.Tree();
                                    tree.treeId = EntityController.getInstance().entityWorldState[col][row].tree.treeId;
                                    for(GameRegister.Damage dmg : EntityController.getInstance().entityWorldState[col][row].tree.damages) {
                                        tree.damages.add(dmg);
                                    }
                                    tree.health = EntityController.getInstance().entityWorldState[col][row].tree.attr.health;
                                    tree.hitBox = EntityController.getInstance().entityWorldState[col][row].tree.hitBox;
                                    tree.tileX = EntityController.getInstance().entityWorldState[col][row].tree.tileX;
                                    tree.tileY = EntityController.getInstance().entityWorldState[col][row].tree.tileY;
                                    tree.tileId = EntityController.getInstance().entityWorldState[col][row].tree.tileId;
                                    tree.maxHealth = EntityController.getInstance().entityWorldState[col][row].tree.attr.maxHealth;
                                    tree.name = EntityController.getInstance().entityWorldState[col][row].tree.name;
                                    tree.spawnId = EntityController.getInstance().entityWorldState[col][row].tree.spawn.id;

                                    state.trees.add(tree);
                                    aoIEntities.trees.put(EntityController.getInstance().entityWorldState[col][row].tree.spawn.id,
                                                            EntityController.getInstance().entityWorldState[col][row].tree);
                                }
                            }
                        }
                    }
                    if (!layerBase.getProperties().get("entity_layer", Boolean.class))
                        state.tileLayers.add(aoiLayer);
                }
            }
            //state.tileLayers = aoiLayers;
            return state;
        }

        public void reset() {
            this.removeFrom2dArray(); // remove from state 2d array
            this.lastMoveTs = 0;
            this.lastMoveId = 0;
            this.lastTilePos = null;
            this.lastState = null;
            this.state = GameRegister.EntityState.FREE;
            this.isTeleporting = false;
            this.target.id = -1;
            this.target.type = null;
            this.target.entity = null;
            this.avgLatency = 0;
            this.stopInteraction(false);
        }

        /**
         * Proceeds to attack current target in intervals based on attack speed and avg ping
         */
        public void attack() {
            if(target == null || target.entity == null) return; // no target to attack

            // update to attack state
            if(state != GameRegister.EntityState.ATTACKING && target.isAlive) {
                state = GameRegister.EntityState.ATTACKING;
                attackType = GameRegister.AttackType.MAGIC_PRISMA;
            }

            //if(isInteracting) return; // an interaction must be stopped before starting a new one

            // starts update timer that control user interaction
//            interactionTimer=new Timer();
//
//            float latency = (System.currentTimeMillis() - target.timestamp)*2f;
            float delay = (1/attr.attackSpeed)-(avgLatency/1000f);
            if(delay <= 0) delay = 0;

            //isInteracting = true;
//
            if(hitTarget.isScheduled())
               hitTarget.cancel();

            target.hitCount = 0;

            if(!target.isAlive) { // if target is not alive anymore
                // reset state and attack info
                resetTarget();
                return; // return
            }

            if(!hitTarget.isScheduled()) {
                lastHitSchedule = System.currentTimeMillis();
                interactionTimer.scheduleTask(hitTarget, delay, GameRegister.clientTickrate());
            }
        }

        public long lastHitSchedule = System.currentTimeMillis();

        public Timer.Task hitTarget = new Timer.Task() {
            @Override
            public void run() {
                // there is an obfuscator (blocking) wall between player and target blocking the attack
                if(!aoIEntities.isTargetOnAttackRange(position.getDrawPos(GameRegister.EntityType.CHARACTER), target.getPosition())) {
                    long delay = (long) ((1000f/attr.attackSpeed)-(avgLatency));
                    if(delay <= 0) delay = 0;
                    interactionTimer.delay(delay);
                    return;
                }
                target.hit(tag.id, ENTITY_TYPE, attr);

                if(!target.isAlive) { // if target is not alive anymore
                    // reset state and attack info
                    resetTarget();
                }
            }
        };
;

        /**
         * Stops interaction of character
         * @param force if this is true, it will stop asap any interaction.
         */
        public void stopInteraction(boolean force) {
            if(force) {
                interactionTimer.clear();
                resetTarget();
                return;
            }

            if(hitTarget.isScheduled()) {
                float hitTargetMillisToExec = hitTarget.getExecuteTimeMillis();
                hitTarget.cancel();
                long elapsed = System.currentTimeMillis() - lastHitSchedule;
                int correctHits = (int)(elapsed/(1000f/ attr.attackSpeed)) + 1;
                Object e = target.entity;
                GameRegister.EntityType type = target.type;
                float timeLeft = (hitTargetMillisToExec - System.nanoTime()/1000000) / 1000f;

                // if hits are misaligned, send one last one delayed with last task remaining time to trigger as a delay
                if(correctHits > target.hitCount) {
                    if(state != GameRegister.EntityState.ATTACKING && target.isAlive) {
                        state = GameRegister.EntityState.ATTACKING;
                        attackType = GameRegister.AttackType.MAGIC_PRISMA;
                    }
                    // hit only if there isn't an obfuscator (blocking) wall between player and target blocking the attack
                    if(aoIEntities.isTargetOnAttackRange(position.getDrawPos(GameRegister.EntityType.CHARACTER), target.getPosition())) {
                        target.hitDelayed(this, timeLeft, tag.id, ENTITY_TYPE, attr, e, type);

                        if(!target.isAlive) { // if target is not alive anymore
                            // reset state and attack info
                            resetTarget();
                        }
                    }
                }
                else {
                    resetTarget();
                }

//                interactionTimer.scheduleTask(new Timer.Task() {
//                    @Override
//                    public void run() {
//                        target.hitDelayed(timeLeft, attr, e, type);
                        //interactionTimer.clear();

//                        target.id = -1;
//                        target.type = null;
//                        target.entity = null;
//                    }
//                }, timeLeft);
            }

            //interactionTimer.clear();
            //stopInteraction.set(true); // set stop interaction flag to true, so current task will be the last one
        }

        /**
         * Resets target
         */
        public void resetTarget() {
            // reset target
            target.id = -1;
            target.type = null;
            target.entity = null;

            // reset state and attack info
            if (state == GameRegister.EntityState.ATTACKING)
                state = GameRegister.EntityState.FREE;
            if (attackType != null) attackType = null;
        }

        public void hitDelayedFinished() {
            if(!hitTarget.isScheduled()) { // reset target for this player if its not on another attack interaction
                resetTarget();
            }
        }

        /**
         * Called when character is hit
         *
         * @param attackerId   the id of the attacker
         * @param attackerType the type of the attacker
         * @param attacker     the attributes of the attacker
         * @return  if entity is alive after hit
         */
        public boolean hit(int attackerId, GameRegister.EntityType attackerType, Attributes attacker) {
            if(attr.health <= 0) return false;

            GameRegister.Damage dmg = new GameRegister.Damage();
            dmg.attackerId = attackerId;
            dmg.attackerType = attackerType;
            dmg.type = GameRegister.DamageType.NORMAL;
            dmg.value = (int) attacker.attack;
            attr.health -= dmg.value;
            this.damages.add(dmg);
            EntityController.getInstance().damagedEntities.characters.put(tag.id, this);

            return true;
        }

//        public boolean compareStates(GameRegister.UpdateState state) {
//            // state message serialization
//            Output newState = new Output(1024, -1);
//            Kryo kryo = new Kryo();
//            kryo.register(GameRegister.UpdateCreature.class);
//            kryo.register(GameRegister.UpdateCharacter.class);
//            kryo.register(GameRegister.UpdateState.class);
//            kryo.register(ArrayList.class);
//            kryo.register(short[][].class);
//            kryo.register(short[].class);
//            kryo.register(int[][].class);
//            kryo.register(int[].class);
//            kryo.register(com.mygdx.server.network.GameRegister.Character.class);
//            kryo.register(Vector2.class);
//            kryo.register(GameRegister.Layer.class);
//            kryo.writeObject(newState, state);
//
//            if(lastState == null) {
//                lastState = new Output(1024, -1);
//                return false;
//            }
//            if(!Arrays.equals(this.lastState.getBuffer(), newState.getBuffer())) {
//                lastState.setBuffer(newState.getBuffer());
//                RogueFantasyServer.worldStateMessageSize = String.valueOf(newState.total());
//                return false;
//            }
//
//            return true;
//        }
    }

    public static class Target {
        public int id = -1;
        public GameRegister.EntityType type;
        public Object entity;
        public long timestamp;
        public int hitCount = 0;
        public boolean isAlive = true;
        private long lastAttack = System.currentTimeMillis();

        /**
         * called when this target is hit
         *
         * @param attackerId the id of the attacker
         * @param attackerType  the type of the attacker
         * @param attacker the attributes of the attacker
         */
        public void hit(int attackerId, GameRegister.EntityType attackerType, Attributes attacker) {
            if(type == null || entity == null) return;

            long now = System.currentTimeMillis();
            if(now - lastAttack >= 1000f/attacker.attackSpeed ) { // only attacks respecting attack speed of attacker
                hitCount++;
                lastAttack = System.currentTimeMillis();
                switch (type) {
                    case CHARACTER:
                        Component.Character targetCharacter = (Component.Character) entity;
                        if(targetCharacter != null)
                            isAlive = targetCharacter.hit(attackerId, attackerType, attacker);
                        break;
                    case TREE:
                        Component.Tree tree = (Component.Tree) entity;
//                        GameRegister.Damage dmg = new GameRegister.Damage();
//                        dmg.attackerId = attackerId;
//                        dmg.attackerType = attackerType;
//                        dmg.type = GameRegister.DamageType.NORMAL;
//                        dmg.value = 5;
                        if(tree != null)
                            isAlive = tree.hit(attackerId, attackerType, attacker);
//                        tree.damages.add(dmg);
//                        EntityController.getInstance().damagedEntities.trees.put(tree.spawnId, tree);
                        break;
                    case CREATURE:
                        Entity targetCreature = (Entity) entity;
                        if(targetCreature != null && targetCreature.get(Component.AI.class) != null)
                            isAlive = targetCreature.get(Component.AI.class).hit(attackerId, attackerType, attacker);
                        break;
                    default:
                        break;
                }
            }
        }

        /**
         * Does a delayed hit on target (also respecting attack speed interval of attacker
         *
         * @param character    the character component that triggered this delayed hit (to callback when it is finished)
         * @param delay        the delay to consider for attack, respecting attack speed interval of attacker
         * @param attackerId   the id of the attacker
         * @param attackerType the type of the attacker
         * @param attacker     the attributes of the attacker
         * @param e            the target entity (since its a delayed attack, target object can lose its references)
         * @param t            the target type (since its a delayed attack, target object can lose its references)
         */
        public void hitDelayed(Character character, float delay, int attackerId, GameRegister.EntityType attackerType, Attributes attacker, Object e, GameRegister.EntityType t) {
            if(t == null || e == null) return;

            long now = System.currentTimeMillis();
//            if((now - lastAttack) + delay < 1000f/attacker.attackSpeed)
//                delay += 1000f/attacker.attackSpeed - ((now - lastAttack) + delay);

            if(delay + (now-lastAttack) < 1000f/attacker.attackSpeed) {
                delay = (1000f/attacker.attackSpeed - ((now-lastAttack) + delay)) / 1000f;
                if(delay<0) delay = 0;
            }

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    lastAttack = System.currentTimeMillis();
                    switch (t) {
                        case CHARACTER:
                            Component.Character targetCharacter = (Component.Character) e;
                            if(targetCharacter != null)
                                isAlive = targetCharacter.hit(attackerId, attackerType, attacker);
                            break;
                        case TREE:
                            Component.Tree tree = (Component.Tree) e;
                            if(tree != null)
                                isAlive = tree.hit(attackerId, attackerType, attacker);
                            break;
                        case CREATURE:
                            Entity targetCreature = (Entity) e;
                            if(targetCreature != null && targetCreature.get(Component.AI.class) != null)
                                isAlive = targetCreature.get(Component.AI.class).hit(attackerId, attackerType, attacker);
                            break;
                        default:
                            break;
                    }
                    character.hitDelayedFinished();
                }
            }, delay);
        }

        /**
         * Gets target position
         * @return          the position of this target in the world
         */
        public Vector2 getPosition() {
            Vector2 position = null;
            if(type == null) return null;

            switch (type) {
                case CHARACTER:
                    Component.Character targetCharacter = (Component.Character) entity;
                    if(targetCharacter == null || targetCharacter.position == null) return null;
                    position = targetCharacter.position.getDrawPos(GameRegister.EntityType.CHARACTER);
                    break;
                case TREE:
                    Component.Tree tree = (Component.Tree) entity;
                    if(tree == null) return null;
                    float tileWidth = TEX_WIDTH * unitScale;
                    float tileHeight = TEX_HEIGHT * unitScale;
                    float halfTileWidth = tileWidth * 0.5f;
                    float halfTileHeight = tileHeight * 0.5f;

                    position = new Vector2( (tree.tileX * halfTileWidth) + (tree.tileY * halfTileWidth),
                            (tree.tileY * halfTileHeight) - (tree.tileX * halfTileHeight) + tileHeight - halfTileHeight*1.38f);
                    break;
                case CREATURE:
                    Entity targetCreature = (Entity) entity;
                    if(targetCreature == null || targetCreature.get(Component.Position.class) == null) return null;
                    position = targetCreature.get(Component.Position.class).getDrawPos(GameRegister.EntityType.CHARACTER);
                    break;
                default:
                    break;
            }
            return position;
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

            int id = entity.get(Spawn.class).id;

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

        /**
         * Removes entity from the 2d array of entities
         */
        public void removeFrom2dArray(Entity entity) {
//            Vector2 tPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(x, y));
//
//            // makes sure its within world state bounds
//            if(tPos.x < 0) tPos.x = 0;
//            if(tPos.y < 0) tPos.y = 0;
//            if(tPos.x > TILES_WIDTH) tPos.x = TILES_WIDTH-1;
//            if(tPos.y > TILES_HEIGHT) tPos.y = TILES_HEIGHT-1;
//
//            int id = entity.get(Tag.class).id;
//
//            EntityController.getInstance().entityWorldState[(int)tPos.x][(int)tPos.y].entities.remove(id);
            EntityController.getInstance().entityWorldState[(int)lastTilePos.x][(int)lastTilePos.y].entities.remove(entity.get(Spawn.class).id);
        }

        /**
         * Checks and updates position in 2d Array of characters if different from last one
         */
        public void updatePositionIn2dArray(Character character) {
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

            int id = character.tag.id;

            // if it has no last tile (null tilepos) just add to the 2d array
            if(lastTilePos == null) {
                EntityController.getInstance().entityWorldState[(int) tPos.x][(int) tPos.y].characters.putIfAbsent(id, character);
                lastTilePos = new Vector2(tPos.x, tPos.y); // create last tile position vector2
            }
            else {
                if(lastTilePos.equals(tPos)) // if its the same, there is no need to update in 2d state array
                    return;
                // if its different:
                // remove from old position in state array
                EntityController.getInstance().entityWorldState[(int)lastTilePos.x][(int)lastTilePos.y].characters.remove(id);
                // add in new position
                EntityController.getInstance().entityWorldState[(int)tPos.x][(int)tPos.y].characters.putIfAbsent(id, character);
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

        public Vector2 getDrawPos(GameRegister.EntityType type) {
            if(type == null) return new Vector2(x, y);

            float tileHeight = TEX_HEIGHT * unitScale;
            float tileWidth = TEX_WIDTH * unitScale;

            Vector2 finalDrawPos = new Vector2(x,y);

            switch (type) {
                case CHARACTER:
                    float spriteH = tileHeight*3;
                    float spriteW = tileWidth;
                    finalDrawPos.x = x + spriteW/12f;
                    finalDrawPos.y = y + spriteH/12f;
                    break;
                default:
                    break;
            }

            return finalDrawPos;
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
        public float width, height, speed, attackSpeed, range, attack, defense, visionRange;
        public float health, maxHealth;

        public Attributes() {
        }

        public Attributes(float width, float height, float maxHealth, float health, float speed, float attackSpeed,
                          float range, float visionRange, float attack, float defense) {
            this.width = width; this.height = height; this.attackSpeed = attackSpeed; this.visionRange = visionRange;
            this.speed = speed; this.range = range; this.health = health; this.maxHealth = maxHealth;
            this.attack = attack; this.defense = defense;
        }
    }

    public static class Spawn {
        public int id; // spawn id
        public final Position position; // spawn position
        public float respawnTime; // time between respawns in seconds
        public float respawnRange; // range of possible respawn position offset

        public Spawn() {
            position = new Position(0,0);
        }

        public Spawn(int id, Position position, float respawnTime, float respawnRange) {
            this.id = id; this.respawnRange = respawnRange;
            this.position = position; this.respawnTime = respawnTime;
        }

        /**
         * Respawns entities respecting its respawn time and peculiarities
         * @param entity    the entity to be respawn
         */
        public void respawn(Object entity) {
            if(entity instanceof Tree) { // for trees we just need to set health back to max health
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        Component.Tree tree = (Component.Tree) entity;
                        tree.attr.health = tree.attr.maxHealth;
                    }
                }, respawnTime);
            }
            else if(entity instanceof Entity) { // for now creatures are the only entities
                final Entity e = (Entity) entity;
                final int cId = e.get(Tag.class).id;
                //EntityController.getInstance().damagedEntities.creatures.remove();
                e.get(Position.class).removeFrom2dArray(e); // removes from world 2d array
                EntityController.getInstance().removeEntity(e); // makes sure to remove it from world dominion before respawning a new one

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        Entity prefab = EntityController.loadCreaturePrefab(cId);

                        Entity newSpawn = EntityController.getInstance().getDominion().createEntity(
                                "Herr Wolfgang IV",
                                new Component.Tag(prefab.get(Component.Tag.class).id, prefab.get(Component.Tag.class).name),
                                new Component.Position(position.x, position.y),
                                new Component.Velocity(0, 0),
                                new Component.Attributes(prefab.get(Component.Attributes.class).width,
                                        prefab.get(Component.Attributes.class).height,
                                        prefab.get(Attributes.class).maxHealth, prefab.get(Attributes.class).health,
                                        prefab.get(Component.Attributes.class).speed,
                                        prefab.get(Attributes.class).attackSpeed,
                                        prefab.get(Attributes.class).range,
                                        prefab.get(Attributes.class).visionRange,
                                        prefab.get(Attributes.class).attack, prefab.get(Attributes.class).defense),
                                new Component.Spawn(id, new Component.Position(position.x, position.y),
                                        prefab.get(Spawn.class).respawnTime, prefab.get(Spawn.class).respawnRange)
                        ).setState(Component.AI.State.IDLE);
                        newSpawn.add(new Component.AI(newSpawn));

                        //Entity creature = EntityController.spawnCreature(cId);
//                        Entity creature = EntityController.spawnCreature(cId); // spawns creature
//
//                        creature.get(Position.class).x = position.x;
//                        creature.get(Position.class).y = position.y;
//                        System.out.println(id);
//                        creature.get(Spawn.class).id = id;
//                        creature.get(Spawn.class).position.x = position.x;
//                        creature.get(Spawn.class).position.y = position.y;

//                        creature.add(new Component.AI(creature));  // adds ai brain
//                        creature.add(new Component.Spawn(id, new Component.Position(position.x, position.y),
//                                5,
//                                5));
//                        creature.setEnabled(true);
                    }
                }, respawnTime);
            }
        }

    }

    public static class Event {

        public enum Type { // types of events
            PLAYER_IN_AOI,
            TARGET_IN_RANGE, TARGET_OUT_OF_RANGE, PLAYER_LEFT_VISION_RANGE, PLAYER_DIED, PLAYER_LEFT_AOI
        }
        private Type type; // the type of this event
        public Character trigger; // the character responsible for triggering the event

        public Event(Type type, Character trigger) {this.type = type; this.trigger = trigger;}
    }

    /**
     * Helper structure to quickly get entities in AoI of client (each client saves last state AoIEntities data)
     */
    public static class AoIEntities {
        public Map<Integer, Entity> creatures; // map of entities in AoI of client (creatures for now)
        public Map<Integer, Component.Character> characters; // map of character in AoI of client
        public Map<Integer, Component.Tree> trees; // map of trees in AoI of client
        public Map<Integer, GameRegister.Wall> obfuscatorWalls; // map of obfuscator walls in AoI of client

        public AoIEntities() {
            creatures = new ConcurrentHashMap<>();
            obfuscatorWalls = new ConcurrentHashMap<>();
            characters = new ConcurrentHashMap<>();
            trees = new ConcurrentHashMap<>();
        }

        /**
         * Checks if attacker is able to attack target
         * @param attacker  the attacker position (bottom-left)
         * @param target    the target entity (bottom-left)
         * @return  true if its possible to attack, false otherwise
         */
        public boolean isTargetOnAttackRange(Vector2 attacker, Vector2 target) {
            float tileHeight = TEX_HEIGHT * unitScale;
            float tileWidth = TEX_WIDTH * unitScale;
            float halfTileWidth = tileWidth * 0.5f;

            if(attacker == null || target == null) return false;

            // if there is a wall between player and target, do not attack
            GameRegister.Wall hit1 = hitObfuscator(
                    new Vector2(attacker.x + halfTileWidth, attacker.y+tileHeight*0.5f),
                    new Vector2(target.x + halfTileWidth, target.y+tileHeight*0.5f));
            GameRegister.Wall hit2 = hitObfuscator(
                    new Vector2(attacker.x + halfTileWidth, attacker.y+tileHeight),
                    new Vector2(target.x + halfTileWidth, target.y+tileHeight));
            GameRegister.Wall hit3 = hitObfuscator(
                    new Vector2(attacker.x + halfTileWidth, attacker.y+tileHeight*2.5f),
                    new Vector2(target.x + halfTileWidth, target.y+tileHeight*2.5f));

            return !(hit1 != null && hit2 != null && hit3 !=null);
        }

        /**
         * Checks if a cast line between two points hits a obfuscator wall in AoI
         *
         * @param p1 the first point of the line to check if hits an entity
         * @param p2 the second point of the line to check if hits an entity
         * @return the wall hit or null if no obfuscator wall was hit
         */
        public GameRegister.Wall hitObfuscator(Vector2 p1, Vector2 p2) {
            if(p1 == null || p2 == null) return null;

            float tileWidth = TEX_WIDTH * unitScale;
            float tileHeight = TEX_HEIGHT * unitScale;
            float halfTileWidth = tileWidth * 0.5f;
            float halfTileHeight = tileHeight * 0.5f;

            synchronized (obfuscatorWalls) {
                for (Map.Entry<Integer, GameRegister.Wall> entry : obfuscatorWalls.entrySet()) {
                    GameRegister.Wall wall = entry.getValue();

                    TiledMapTile tile = WorldMap.getInstance().getTileFromId(wall.tileId);
                    Vector2 drawPos = new Vector2();
                    drawPos.x = (wall.tileX * halfTileWidth) + (wall.tileY * halfTileWidth);
                    drawPos.y = (wall.tileY * halfTileHeight) - (wall.tileX * halfTileHeight) + tileHeight; // adjust to match origin to tile origin

                    Polygon hitBox = new Polygon(new float[]{
                            0, 0,
                            tile.getTextureRegion().getRegionWidth() * unitScale * 0.5f, 0,
                            tile.getTextureRegion().getRegionWidth() * unitScale * 0.5f, tile.getTextureRegion().getRegionHeight() * unitScale * 0.6f,
                            0, tile.getTextureRegion().getRegionHeight() * unitScale * 0.6f});

                    hitBox.setPosition(drawPos.x + halfTileWidth * 0.5f, drawPos.y + halfTileHeight * 0.8f);

                    // found a entity that hits
                    if (Intersector.intersectSegmentPolygon(p1, p2, hitBox)) {
                        return wall;
                    }
                }
                // return null means no entity was hit by the point
                return null;
            }
        }
    }

    public static class AI {
        private final Entity body; // the body that this ai controls (the entity with AI component)
        public Target target = new Target();
        public State state = State.IDLE;
        public ArrayList<GameRegister.Damage> damages = new ArrayList<>(); // list of recent damages

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
                case PLAYER_IN_AOI:
                    if(target.entity == null) {// does not have a target
                        if (isInVisionRange(event.trigger)) {
                            this.state = State.FOLLOWING;
                            target.id = event.trigger.tag.id;
                            target.type = GameRegister.EntityType.CHARACTER;
                            target.entity = event.trigger;
                            target.isAlive = (event.trigger.attr.health > 0f);
                        }
                    } else {
                        // lost vision range of target
                        if (target.id == event.trigger.tag.id && !isInVisionRange(event.trigger)) {
                            this.state = State.IDLE;
                            target.entity = null;
                        }
                    }
                    break;
                case TARGET_OUT_OF_RANGE:
                    this.state = State.FOLLOWING;
                    target.id = event.trigger.tag.id;
                    target.type = GameRegister.EntityType.CHARACTER;
                    target.entity = event.trigger;
                    target.isAlive = (event.trigger.attr.health > 0f);
                    break;
                case PLAYER_LEFT_AOI:
                case PLAYER_LEFT_VISION_RANGE:
                case PLAYER_DIED:
                    if (target.entity != null && target.id == event.trigger.tag.id) {
                        // TODO: SEARCH FOR OTHER TARGET IN AOI RANGE
                        // if there is no target close, idle walk or do nothing?
                        this.state = State.IDLE;
                        target.entity = null;
                    }
                    break;
                case TARGET_IN_RANGE:
                    this.state = State.ATTACKING;
                    break;
                default:
                    break;
            }
        }

        private boolean isInVisionRange(Character character) {
            float dist = character.position.getDrawPos(GameRegister.EntityType.CHARACTER).
                    dst(body.get(Component.Position.class).getDrawPos(GameRegister.EntityType.CREATURE));

            if(dist <= body.get(Attributes.class).visionRange)
                return true;

            return false;
        }

        // responsible for controlling the AI in the loop
        public void act() {
            switch(state) {
                case IDLE_WALKING:
                    idleWalk();
                    break;
                case IDLE:
                    idle();
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

        private void idle() {
            this.body.get(Velocity.class).x = 0;
            this.body.get(Velocity.class).y = 0;
        }

        /**
         * Called when creature is hit
         * @param attackerId the id of the attacker
         * @param attackerType the type of the attacker
         * @param attacker  the attributes of the attacker
         * @return if entity is alive after hit
         */
        public boolean hit(int attackerId, GameRegister.EntityType attackerType, Attributes attacker) {
            if(body.get(Attributes.class).health > 0) { // entity is alive
                GameRegister.Damage dmg = new GameRegister.Damage();
                dmg.attackerId = attackerId;
                dmg.attackerType = attackerType;
                dmg.type = GameRegister.DamageType.NORMAL;
                dmg.value = (int) attacker.attack;
                body.get(Attributes.class).health -= dmg.value;
                this.damages.add(dmg);
                EntityController.getInstance().damagedEntities.creatures.put(body.get(Spawn.class).id, body);
            }

            if(body.get(Attributes.class).health <= 0f) {
                body.get(Spawn.class).respawn(body);
                return false;
            }

            return true;
        }

        // attack if in range
        private void attack() {
            if(target.entity == null) return; // no target to attack

            Component.Position position = body.get(Component.Position.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);

            if(target.type != GameRegister.EntityType.CHARACTER) return; // only attack players atm

            Character character = (Character) target.entity;

            Component.Position targetPos = character.position;
            Component.Attributes targetAttr = character.attr;

            Vector2 goalPos = new Vector2(targetPos.x + targetAttr.width/2f, targetPos.y + targetAttr.height/12f);
            Vector2 aiPos = new Vector2(position.x + attributes.width/2f, position.y + attributes.height/3f);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) > attributes.range) { // not close enough, do not attack anymore
                react(new Event(Event.Type.TARGET_OUT_OF_RANGE, character));
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

            if(target.type != GameRegister.EntityType.CHARACTER) return; // only follow  players atm

            Character character = (Character) target.entity;


            Component.Position targetPos = character.position;
            Component.Attributes targetAttr = character.attr;

//            System.out.printf("Target %s\n",
//                    targetAttr.width/2f);

            Vector2 goalPos = new Vector2(targetPos.x + targetAttr.width/2f, targetPos.y + targetAttr.height/12f);
            Vector2 aiPos = new Vector2(position.x + attributes.width/2f, position.y + attributes.height/3f);
            Vector2 deltaVec = new Vector2(goalPos).sub(aiPos);
            deltaVec.nor().scl(attributes.speed*GameRegister.clientTickrate());
            Vector2 futurePos = new Vector2(aiPos).add(deltaVec);

            if(goalPos.dst(futurePos) <= attributes.range) { // close enough, do not move anymore
                velocity.x = 0; velocity.y = 0;
                react(new Event(Event.Type.TARGET_IN_RANGE, character));
                return;
            }

            velocity.x = deltaVec.x; velocity.y = deltaVec.y;
        }

        private void idleWalk() {
            Component.Position position = body.get(Component.Position.class);
            Component.Spawn spawn = body.get(Component.Spawn.class);
            Component.Velocity velocity = body.get(Component.Velocity.class);
            Component.Attributes attributes = body.get(Component.Attributes.class);
            if(target.entity == null) { // move aimlessly if there is no target
//                if(position.x <= spawn.position.x && position.y <= spawn.position.y) {
//                    velocity.x = 1 * attributes.speed * GameRegister.clientTickrate();
//                    velocity.y = 0;
//                }
//                if (position.x >= spawn.position.x + 5 && position.y <= spawn.position.y) {
//                    velocity.x = 0;
//                    velocity.y = 1 * attributes.speed * GameRegister.clientTickrate();
//                }
//                if(position.x >= spawn.position.x + 5 && position.y >= spawn.position.y + 5) {
//                    velocity.x = -1 * attributes.speed * GameRegister.clientTickrate();
//                    velocity.y = 0;
//                }
//                if(position.x <= spawn.position.x && position.y >= spawn.position.y + 5){
//                    velocity.x = 0;
//                    velocity.y = -1 * attributes.speed * GameRegister.clientTickrate();
//                }
                Vector2 move = new Vector2(new Random().nextInt(-1, 2) * attributes.speed * GameRegister.clientTickrate(),
                        new Random().nextInt(-1, 2) * attributes.speed * GameRegister.clientTickrate());
                Vector2 futurePos = new Vector2(move.x, move.y).add(position.x, position.y);

                if(futurePos.dst(new Vector2(position.x, position.y)) > spawn.respawnRange) // dont walk too far away from where it last saw player
                    return;

                velocity.x = move.x;
                velocity.y = move.y;
            }
        }
    }

}
