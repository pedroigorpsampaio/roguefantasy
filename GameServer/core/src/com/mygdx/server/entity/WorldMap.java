package com.mygdx.server.entity;

import static com.badlogic.gdx.graphics.g2d.Batch.C1;
import static com.badlogic.gdx.graphics.g2d.Batch.C2;
import static com.badlogic.gdx.graphics.g2d.Batch.C3;
import static com.badlogic.gdx.graphics.g2d.Batch.C4;
import static com.badlogic.gdx.graphics.g2d.Batch.U1;
import static com.badlogic.gdx.graphics.g2d.Batch.U2;
import static com.badlogic.gdx.graphics.g2d.Batch.U3;
import static com.badlogic.gdx.graphics.g2d.Batch.U4;
import static com.badlogic.gdx.graphics.g2d.Batch.V1;
import static com.badlogic.gdx.graphics.g2d.Batch.V2;
import static com.badlogic.gdx.graphics.g2d.Batch.V3;
import static com.badlogic.gdx.graphics.g2d.Batch.V4;
import static com.badlogic.gdx.graphics.g2d.Batch.X1;
import static com.badlogic.gdx.graphics.g2d.Batch.X2;
import static com.badlogic.gdx.graphics.g2d.Batch.X3;
import static com.badlogic.gdx.graphics.g2d.Batch.X4;
import static com.badlogic.gdx.graphics.g2d.Batch.Y1;
import static com.badlogic.gdx.graphics.g2d.Batch.Y2;
import static com.badlogic.gdx.graphics.g2d.Batch.Y3;
import static com.badlogic.gdx.graphics.g2d.Batch.Y4;
import static com.mygdx.server.entity.Component.rectOffsetDown;
import static com.mygdx.server.entity.Component.rectOffsetLeft;
import static com.mygdx.server.entity.Component.rectOffsetRight;
import static com.mygdx.server.entity.Component.rectOffsetUp;
import static com.mygdx.server.network.GameRegister.N_COLS;
import static com.mygdx.server.network.GameRegister.N_ROWS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.network.GameRegister;
import com.mygdx.server.network.GameServer;
import com.mygdx.server.ui.RogueFantasyServer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import dev.dominion.ecs.api.Entity;


/**
 * Class responsible for loading map from file and to
 * control the 2D array containing world map logic
 */
public class WorldMap implements InputProcessor {
    private OrthographicCamera cam;
    private Kryo kryo;
    private ByteArrayOutputStream bos;
    private static WorldMap instance = null;
    private Random rand;
    private TiledMapTileLayer layerCut;
    private Matrix4 isoTransform;
    private Matrix4 invIsotransform;
    private float unitScale;
    private IsometricTiledMapRenderer renderer;
    public static float WORLD_WIDTH, WORLD_HEIGHT;
    public static int TILES_WIDTH, TILES_HEIGHT, TEX_WIDTH, TEX_HEIGHT;
    private TiledMap map;
    public TiledMap getMap() {return map;}
    private Vector3 screenPos = new Vector3();
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer; // for debug
    private int specId = 0; // id of concurrent hash map for players to spectate (in order of login)
    private Component.Character spectatee; // the spectatee to watch for debug from server view
    private ArrayList<TiledMapTileLayer> cutLayers; // the spliced map layers containing only in view tiles to player

    private WorldMap() {

    }
    // loads map from file and prepares the vars for controlling world state
    public void init(String fileName, SpriteBatch batch) {
        Log.info("game-server", "Loading world map...");
        map = new TmxMapLoader().load(fileName);
        this.batch = batch;
        unitScale = 1 / 32f;
        renderer = new IsometricTiledMapRenderer(map, unitScale, batch);
        shapeRenderer = new ShapeRenderer();
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        rand = new Random();

        // for serialization debug
        kryo = new Kryo();
        // Register all classes to be serialized.
        kryo.register(short[][].class);
        kryo.register(short[].class);
        kryo.register(int[][].class);
        kryo.register(int[].class);
        kryo.register(ArrayList.class);
        kryo.register(GameRegister.Layer.class);
        bos = new ByteArrayOutputStream();

       // cam.setToOrtho(false, 30, 20);
        TiledMapTileLayer tileLayer =  (TiledMapTileLayer) map.getLayers().get(0);
        WORLD_WIDTH = tileLayer.getWidth() * 32f;
        WORLD_HEIGHT = tileLayer.getHeight() * 16f;
        TILES_WIDTH = tileLayer.getWidth();
        TILES_HEIGHT = tileLayer.getHeight();
        TEX_WIDTH = tileLayer.getTileWidth();
        TEX_HEIGHT = tileLayer.getTileHeight();

        cam =  new OrthographicCamera(32f, 32f * (h / w));
        cam.zoom = 2f;

        // layer section to render
        layerCut = new TiledMapTileLayer(N_COLS, N_ROWS, 32, 16);

        // create the isometric transform
        isoTransform = new Matrix4();
        isoTransform.idt();

        // isoTransform.translate(0, 32, 0);
        isoTransform.scale((float)(Math.sqrt(2.0) / 2.0), (float)(Math.sqrt(2.0) / 4.0), 1.0f);
        isoTransform.rotate(0.0f, 0.0f, 1.0f, -45);

        // ... and the inverse matrix
        invIsotransform = new Matrix4(isoTransform);
        invIsotransform.inv();

        TiledMapTileLayer entityLayer =  (TiledMapTileLayer) map.getLayers().get(2);
        loadEntityLayer(entityLayer); // loads entity layer of floor
        loadObjects(map.getLayers().get(3)); // loads object layer of floor
    }

    public static WorldMap getInstance() {
        if(instance == null)
            instance = new WorldMap();
        return instance;
    }

    /**
     * Loads objects from the object layer of the map, which includes portals, spawns and more.
     *
     * @param layer   the object layer of the floor
     */
    private void loadObjects(MapLayer layer) {
        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                RectangleMapObject rectangleMapObject = (RectangleMapObject) object;
                Rectangle rectangle = rectangleMapObject.getRectangle();
                float x = rectangle.getX();
                float y = rectangle.getY();
                int j = (int) Math.floor(x / (TEX_WIDTH * 0.5f));
                int i = (int) Math.floor(y / (TEX_WIDTH * 0.5f)) - 1;
                if(j < 0) j = 0;
                if(i < 0) i = 0;
                if(j > TILES_WIDTH) j = TILES_WIDTH-1;
                if(i > TILES_HEIGHT) i  = TILES_HEIGHT-1;
                //rectangle.setX((x / (TEX_WIDTH * 0.5f)) + 0.5f);
                //rectangle.setY((y / (TEX_WIDTH * 0.5f)) - 1) ;
                //rectangle.setWidth(rectangle.getWidth()*unitScale*2f);
                //rectangle.setHeight(rectangle.getHeight()*unitScale*2f);
                //System.out.println(j + " / "+ i);

                Vector3 sPos = new Vector3(x, y, 0);
                sPos.mul(isoTransform); // tiled coordinates are iso projected- unproject it for game world coordinates
                Vector2 worldPos = new Vector2((sPos.x / (WorldMap.TEX_WIDTH * 0.5f))  + 0.1f,
                                (sPos.y / (WorldMap.TEX_WIDTH * 0.5f)) + 0.25f);
                rectangle.x = worldPos.x; rectangle.y = worldPos.y;
                float tmp = rectangle.width*unitScale;
                rectangle.width = rectangle.height*unitScale;
                rectangle.height = tmp;

                String actionStr = object.getProperties().get("action_type", String.class);
                if(actionStr != null) {
                    Action action = Action.getAction(actionStr);
                    switch(action) {
                        case PORTAL: // one portal per tile max
                            Portal portal = new Portal();
                            portal.portalId = object.getProperties().get("portal_id", Integer.class);
                            portal.tileX = j;
                            portal.tileY = i;
                            portal.destX = object.getProperties().get("destination_x", Integer.class);
                            portal.destY = TILES_HEIGHT - object.getProperties().get("destination_y", Integer.class);
                            Vector3 dPos = new Vector3(portal.destX, portal.destY, 0);
                            dPos.mul(isoTransform); // tiled coordinates are iso projected- unproject it for game world coordinates
                            portal.destX = (int) dPos.x;
                            portal.destY = (int) dPos.y;
                            portal.hitBox = rectangle;
                            EntityController.getInstance().entityWorldState[j][i].portal = portal;
                            break;
                        case SPAWN:  // one spawn per tile max
                            break;
                        default:
                            Log.info("map", "Unknown action can't be loaded");
                            break;
                    }
                }
            }

        }
    }

    /**
     * Enum that contains the possible actions that happens in map interactions
     */
    public enum Action {
        PORTAL("portal"),
        SPAWN("spawn"),
        UNKNOWN("unknown");

        public String type;

        Action(String type) {
            this.type = type;
        }

        public static Action getAction(String str){
            for (Action action : Action.values()) {
                if (action.type.equals(str)) {
                    return action;
                }
            }
            //throw new Exception("No enum constant with text " + text + " found");
            return Action.UNKNOWN;// if no action was found, return unknown
        }
    }

    /**
     * Loads entity layer of map containing walls, trees and other map entities into world 2d array
     * to send to clients that treats them as usual entities to render in correct order
     *
     * @param layer   the entity layer of the floor
     */
    private void loadEntityLayer(TiledMapTileLayer layer) {
        int wallSpawnId = 0, treeSpawnId = 0;
        if (layer.getProperties().get("entity_layer", Boolean.class)) {
            for(int i = 0; i < layer.getHeight(); i++) {
                for (int j = 0; j < layer.getWidth(); j++) {
                    final TiledMapTileLayer.Cell cell = layer.getCell(j, i);

                    if(cell != null && cell.getTile() != null) {
                        String type = cell.getTile().getProperties().get("type", String.class);
                        switch(type) {
                            case "tree":
                                GameRegister.Tree tree = new GameRegister.Tree();
                                tree.health = 100f; // initial health
                                tree.maxHealth = 100f; // max health
                                tree.name = cell.getTile().getProperties().get("name", String.class);
                                tree.tileId = cell.getTile().getId();
                                tree.treeId = cell.getTile().getProperties().get("tree_id", Integer.class);
                                tree.tileX = j; tree.tileY = i;
                                tree.spawnId = treeSpawnId;
                                for(MapObject object : cell.getTile().getObjects()) {
                                    if (object instanceof PolygonMapObject) {
                                        Polygon polygon = ((PolygonMapObject)object).getPolygon();
                                        // adjusting tiled tree template
                                        polygon.setPosition(.32f,.01f);
                                        polygon.setOrigin(0, 0);
                                        polygon.setScale(unitScale*1.02f, unitScale*1.05f);
                                        // set hitbox vertices to send to players
                                        tree.hitBox = polygon.getTransformedVertices();
                                    }
                                }
                                EntityController.getInstance().entityWorldState[j][i].tree = tree;
                                treeSpawnId++;
                                break;
                            default:
                                GameRegister.Wall wall = new GameRegister.Wall();
                                wall.wallId = wallSpawnId;
                                wall.tileId = cell.getTile().getId();
                                wall.tileX = j; wall.tileY = i;
                                EntityController.getInstance().entityWorldState[j][i].wall = wall;
                                wallSpawnId++;
                                break;
                        }

                    }
                }
            }
        }
    }

    public void resize(int width, int height) {
        cam.viewportWidth = 32f;
        cam.viewportHeight = 32f * height/width;
        cam.update();
    }

    private void updateCamera() {
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            cam.zoom += 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            cam.zoom -= 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cam.translate(-1, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cam.translate(1, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cam.translate(0, -1, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cam.translate(0, 1, 0);
        }

        cam.zoom = MathUtils.clamp(cam.zoom, 0.1f, 100/cam.viewportWidth);
//
//        float effectiveViewportWidth = cam.viewportWidth * cam.zoom;
//        float effectiveViewportHeight = cam.viewportHeight * cam.zoom;
//
//        cam.position.x = MathUtils.clamp(cam.position.x, effectiveViewportWidth / 2f, 100 - effectiveViewportWidth / 2f);
//        cam.position.y = MathUtils.clamp(cam.position.y, effectiveViewportHeight / 2f, 100 - effectiveViewportHeight / 2f);

        if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
            if(spectatee == null) {
                spectatee = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                        get().getValue().character;
                Log.info("game-server", "Spectating player: " + spectatee.tag.name);
            } else {
                cam.position.x = spectatee.position.x + spectatee.attr.width / 2f;
                cam.position.y = spectatee.position.y + spectatee.attr.height / 2f;
            }
        }

//        float effectiveViewportWidth = cam.viewportWidth * cam.zoom;
//        float effectiveViewportHeight = cam.viewportHeight * cam.zoom;
//        cam.position.x = MathUtils.clamp(cam.position.x, effectiveViewportWidth / 2f, WORLD_WIDTH - effectiveViewportWidth / 2f);
//        cam.position.y = MathUtils.clamp(cam.position.y, -effectiveViewportHeight * 11f, WORLD_HEIGHT - effectiveViewportHeight / 2f);

        float zoom = cam.zoom;
        float zoomedHalfWorldWidth = zoom * cam.viewportWidth / 2;
        float zoomedHalfWorldHeight = zoom * cam.viewportHeight / 2;

        //min and max values for camera's x coordinate
        float minX = zoomedHalfWorldWidth;
        float maxX = WORLD_WIDTH - zoomedHalfWorldWidth;

        //min and max values for camera's y coordinate
        float minY = zoomedHalfWorldHeight;
        float maxY = WORLD_HEIGHT - zoomedHalfWorldHeight;
//
//        cam.position.x = MathUtils.clamp(cam.position.x, minX, maxX);
//        cam.position.y = MathUtils.clamp(cam.position.y, minY, maxY);

        //TODO: FIND A WAY TO CORRECTLY CLAMP LIMITS OF ISO MAP

        cam.update();
    }

    private Vector2 topRight = new Vector2();
    private Vector2 bottomLeft = new Vector2();
    private Vector2 topLeft = new Vector2();
    private Vector2 bottomRight = new Vector2();

    /**
     * Given position in world space, return the respective indexes in world 2d iso tile array
     * @param position the x,y position in world space as a Vec2
     * @return a Vec2 containing the indexes of given position in the 2d iso tile array
     */
    public Vector2 toIsoTileCoordinates(Vector2 position) {
        float tileWidth = TEX_WIDTH * unitScale;

        float pX = MathUtils.floor(translateScreenToIso(position).x / tileWidth) + 1;
        float pY = MathUtils.floor(translateScreenToIso(position).y / tileWidth);

        return new Vector2(pX, pY);
    }

    /**
     * Checks if world space coordinates is within world bound
     * @return true if it is, false otherwise
     */
    public boolean isWithinWorldBounds(Vector2 position) {
//        TiledMapTileLayer layerBase = (TiledMapTileLayer) map.getLayers().get(0); // all layers are the same size
//        float tileWidth = layerBase.getTileWidth() * unitScale;
//
//        float pX = MathUtils.floor(translateScreenToIso(position).x / tileWidth);
//        float pY = MathUtils.floor(translateScreenToIso(position).y / tileWidth);
        Vector2 tPos = toIsoTileCoordinates(position);
        Vector2 tPosDown = toIsoTileCoordinates(new Vector2(position.x, position.y-rectOffsetDown));
        Vector2 tPosUp = toIsoTileCoordinates(new Vector2(position.x, position.y+rectOffsetUp));
        Vector2 tPosLeft = toIsoTileCoordinates(new Vector2(position.x-rectOffsetLeft, position.y));
        Vector2 tPosRight = toIsoTileCoordinates(new Vector2(position.x+rectOffsetRight, position.y));

        if(tPos.x < 0 || tPosDown.x < 0 || tPosUp.x < 0 || tPosLeft.x < 0 || tPosRight.x < 0) { return false; }
        if(tPos.y < 0 || tPosDown.y < 0 || tPosUp.y < 0 || tPosLeft.y < 0 || tPosRight.y < 0) { return false; }
        if(tPos.x > TILES_WIDTH || tPosDown.x > TILES_WIDTH || tPosUp.x > TILES_WIDTH || tPosLeft.x > TILES_WIDTH || tPosRight.x > TILES_WIDTH) { return false; }
        if(tPos.y > TILES_HEIGHT || tPosDown.y > TILES_HEIGHT || tPosUp.y > TILES_HEIGHT || tPosLeft.y > TILES_HEIGHT || tPosRight.y > TILES_HEIGHT) {return false; }

        return true;
    }

    /**
     * Checks if tile at position received in parameter is walkable
     * @param position  world coordinates to check if respective isometric tile is walkable
     * @return true if it is, false otherwise
     */
    public boolean isWalkable(Vector2 position) {
        Vector2 tPos = toIsoTileCoordinates(position);
        Vector2 tPosDown = toIsoTileCoordinates(new Vector2(position.x, position.y-rectOffsetDown));
        Vector2 tPosUp = toIsoTileCoordinates(new Vector2(position.x, position.y+rectOffsetUp));
        Vector2 tPosLeft = toIsoTileCoordinates(new Vector2(position.x-rectOffsetLeft, position.y));
        Vector2 tPosRight = toIsoTileCoordinates(new Vector2(position.x+rectOffsetRight, position.y));
        TiledMapTileLayer floorLayer = (TiledMapTileLayer) map.getLayers().get(0);

        if(floorLayer.getCell((int)tPos.x, (int)tPos.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosDown.x, (int)tPosDown.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosUp.x, (int)tPosUp.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosLeft.x, (int)tPosLeft.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosRight.x, (int)tPosRight.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        TiledMapTileLayer wallLayer =  (TiledMapTileLayer) map.getLayers().get(2); // gets wall layer
        final TiledMapTileLayer.Cell cell = wallLayer.getCell((int)tPos.x, (int)tPos.y); // gets cells to check if it has unwalkable map entity
        final TiledMapTileLayer.Cell cellUp = wallLayer.getCell((int)tPosDown.x, (int)tPosDown.y); // gets cell to check if it has unwalkable map entity
        final TiledMapTileLayer.Cell cellDown = wallLayer.getCell((int)tPosUp.x, (int)tPosUp.y); // gets cell to check if it has unwalkable map entity
        final TiledMapTileLayer.Cell cellLeft = wallLayer.getCell((int)tPosLeft.x, (int)tPosLeft.y); // gets cell to check if it has unwalkable map entity
        final TiledMapTileLayer.Cell cellRight = wallLayer.getCell((int)tPosRight.x, (int)tPosRight.y); // gets cell to check if it has unwalkable map entity

        if(cell != null && cell.getTile() != null) { // there is a map entity here
            if(!cell.getTile().getProperties().get("walkable", Boolean.class)) { // check if its walkable
                return false;
            }
        }
        if(cellUp != null && cellUp.getTile() != null) { // there is a map entity here
            if(!cellUp.getTile().getProperties().get("walkable", Boolean.class)) { // check if its walkable
                return false;
            }
        }
        if(cellDown != null && cellDown.getTile() != null) { // there is a map entity here
            if(!cellDown.getTile().getProperties().get("walkable", Boolean.class)) { // check if its walkable
                return false;
            }
        }
        if(cellLeft != null && cellLeft.getTile() != null) { // there is a map entity here
            if(!cellLeft.getTile().getProperties().get("walkable", Boolean.class)) { // check if its walkable
                return false;
            }
        }
        if(cellRight != null && cellRight.getTile() != null) { // there is a map entity here
            if(!cellRight.getTile().getProperties().get("walkable", Boolean.class)) { // check if its walkable
                return false;
            }
        }

        if(floorLayer.getCell((int)tPos.x, (int)tPos.y).getTile().getProperties().get("walkable", Boolean.class) &&
            floorLayer.getCell((int)tPosUp.x, (int)tPosUp.y).getTile().getProperties().get("walkable", Boolean.class) &&
            floorLayer.getCell((int)tPosDown.x, (int)tPosDown.y).getTile().getProperties().get("walkable", Boolean.class) &&
            floorLayer.getCell((int)tPosLeft.x, (int)tPosLeft.y).getTile().getProperties().get("walkable", Boolean.class) &&
            floorLayer.getCell((int)tPosRight.x, (int)tPosRight.y).getTile().getProperties().get("walkable", Boolean.class))
            return true;
        else
            return false;
    }

    public void render() {
        updateCamera();

        renderer.setView(cam);
        Batch batch = renderer.getBatch();
        //batch.setColor(new Color(Color.RED));

        // Array of layers with only what is interesting to the player
        cutLayers = new ArrayList<>();
        // Array containing layers with whats interesting to the player but in world size for correct rendering
        ArrayList<TiledMapTileLayer> renderLayers = new ArrayList<>();
        // List of 2D arrays containing only tile IDs that are interesting to the player
        ArrayList<GameRegister.Layer> aoiLayers = new ArrayList<>();

        if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
            if (spectatee == null) { // cant spectate if its null
                spectatee = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                        get().getValue().character;
                Log.info("game-server", "Spectating player: " + spectatee.tag.name);
                return;
            }
            int l = 0;
            for (MapLayer mapLayer : map.getLayers()) { // cut layers so it only contains whats interesting to spectatee
                if (mapLayer.getClass().equals(TiledMapTileLayer.class)) { // only cuts tile map layers TODO: object layer can be at client side also? or should it be sent?
                    TiledMapTileLayer layerBase = (TiledMapTileLayer) mapLayer;

                    float playerX = spectatee.position.x;
                    float playerY = spectatee.position.y;
                    Vector2 playerPos = new Vector2(playerX, playerY);

                    Vector2 tPos = toIsoTileCoordinates(playerPos);
                    float pX = tPos.x;
                    float pY = tPos.y;

                    int minI = MathUtils.ceil(pY - N_ROWS / 2);
                    int maxI = MathUtils.ceil(pY + N_ROWS / 2)-1;

                    int minJ = MathUtils.ceil(pX - N_COLS / 2);
                    int maxJ = MathUtils.ceil(pX + N_COLS / 2)-1;

                    // clamp to limits but always send same amount of info
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

                    // the layer containing only visible tiles for player
                    layerCut = new TiledMapTileLayer(N_COLS, N_ROWS, 32, 16);
                    layerCut.setName(layerBase.getName());
                    layerCut.setOpacity(layerBase.getOpacity());
                    layerCut.setOffsetX(layerBase.getOffsetX());
                    layerCut.setOffsetY(layerBase.getOffsetY());
                    layerCut.setParallaxX(layerBase.getParallaxX());
                    layerCut.setParallaxY(layerBase.getParallaxY());
                    layerCut.setVisible(layerBase.isVisible());
                    layerCut.getProperties().putAll(layerBase.getProperties());

                    int[][] aoiTiles = new int[N_ROWS][N_COLS];
                    // stores visible tiles in the cull layer
                    for (int row = minI; row <= maxI; row++) {
                        for (int col = minJ; col <= maxJ; col++) {
                            final TiledMapTileLayer.Cell cell = layerBase.getCell(col, row);
                            layerCut.setCell(col - minJ, row - minI, cell);
                            if(cell != null)
                                aoiTiles[col-minJ][row-minI] = cell.getTile().getId();
                            else
                                aoiTiles[col-minJ][row-minI] = -1; // represents null tile/cell
                        }
                    }
                    cutLayers.add(layerCut);
                    GameRegister.Layer aoiLayer = new GameRegister.Layer();
                    aoiLayer.tiles = aoiTiles;
                    aoiLayers.add(aoiLayer);
                    // AFTER SENDING TO CLIENT, CLIENT CAN RECREATE IN THE CORRECT POSITION WITH THE FOLLOWING STEPS:

                    // create big layer that contains the size of the world
                    TiledMapTileLayer bigLayer = new TiledMapTileLayer(TILES_WIDTH, TILES_HEIGHT, 32, 16);

                    TiledMapTileSets tilesets = map.getTileSets();
                    int MASK_CLEAR = 0xE0000000;
                    int FLAG_FLIP_HORIZONTALLY = 0x80000000;
                    int FLAG_FLIP_VERTICALLY = 0x40000000;
                    int FLAG_FLIP_DIAGONALLY = 0x20000000;

                    // places the layer culled by the server in the correct position in world 2d array for correct rendering
                    for (int row = minI; row <= maxI; row++) {
                        for (int col = minJ; col <= maxJ; col++) {
                            int id = aoiTiles[col-minJ][row-minI];
                            boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
                            boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
                            boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);
                            TiledMapTile tile = tilesets.getTile(id & ~MASK_CLEAR);
                            TiledMapTileLayer.Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
                            cell.setTile(tile);
                            //final TiledMapTileLayer.Cell cell = layerCut.getCell(col - minJ, row - minI);
                            bigLayer.setCell(col, row, cell);
                        }
                    }

                    renderLayers.add(bigLayer);
                    l++; // next layer
                }
            }
        }


//        System.out.println("### aoiTiles DEBUG ### \n\n");
//
//        for(int l = 0; l < aoiLayers.size(); l++) {
//            System.out.println("\n\nLAYER "+l+": isEntityLayer? : " + aoiLayers.get(l).isEntityLayer + "\n");
//            int[][] aoiTiles = aoiLayers.get(l).tiles;
//            for (int i = 0; i < aoiTiles.length; i++) {
//                System.out.println(" ");
//                for (int j = 0; j < aoiTiles[i].length; j++) {
//                    System.out.printf(aoiTiles[i][j] + " ,");
//                }
//            }
//        }

//        System.out.println("\n\n### END DEBUG ### ");

//        String fprint = GraphLayout.parseInstance(aoiLayers).toFootprint();
//        String[] lines = fprint.split("\n"); String lastLine = lines[lines.length - 2];

        // WRITE SIZE OF MAP STATE MESSAGE IN METRICS FOR DEBUG
//        Output output = new Output(1024, -1);
//        kryo.writeObject(output, aoiLayers);
//        RogueFantasyServer.worldStateMessageSize = String.valueOf(output.total());

//        System.out.println("CUT: " + GraphLayout.parseInstance(cutLayers).toFootprint());
//        System.out.println("RENDER: " + GraphLayout.parseInstance(renderLayers).toFootprint());

        /** THE FOLLOWING METHOD IS ALREADY OPTIMIZED FOR DRAWING VISIBLE TILES!!! **/

        int rl = 0;
        // render each layer from lowest to highest (higher layers will be on top)
        for(TiledMapTileLayer layer : renderLayers) {
            rl++;
            float tileWidth = layer.getTileWidth() * unitScale;
            float tileHeight = layer.getTileHeight() * unitScale;

            final float layerOffsetX = layer.getRenderOffsetX() * unitScale - renderer.getViewBounds().x * (layer.getParallaxX() - 1);
            // offset in tiled is y down, so we flip it
            final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - renderer.getViewBounds().y * (layer.getParallaxY() - 1);

            float halfTileWidth = tileWidth * 0.5f;
            float halfTileHeight = tileHeight * 0.5f;

            int it = 0;

            // setting up the screen points
            // COL1
            topRight.set(renderer.getViewBounds().x + renderer.getViewBounds().width - layerOffsetX, renderer.getViewBounds().y - layerOffsetY);
            // COL2
            bottomLeft.set(renderer.getViewBounds().x - layerOffsetX, renderer.getViewBounds().y + renderer.getViewBounds().height - layerOffsetY);
            // ROW1
            topLeft.set(renderer.getViewBounds().x - layerOffsetX, renderer.getViewBounds().y - layerOffsetY);
            // ROW2
            bottomRight.set(renderer.getViewBounds().x + renderer.getViewBounds().width - layerOffsetX, renderer.getViewBounds().y + renderer.getViewBounds().height - layerOffsetY);

            //if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
            // transforming screen coordinates to iso coordinates
            int row1 = (int) (translateScreenToIso(topLeft).y / tileWidth) - 2;
            int row2 = (int) (translateScreenToIso(bottomRight).y / tileWidth) + 2;

            int col1 = (int) (translateScreenToIso(bottomLeft).x / tileWidth) - 2;
            int col2 = (int) (translateScreenToIso(topRight).x / tileWidth) + 2;

            // clamp to limits but always send same amount of info
            if (row1 < 0) {
                row2 -= row1;
                row1 = 0;
            }
            if (col1 < 0) {
                col2 -= col1;
                col1 = 0;
            }
            if (row2 > TILES_HEIGHT) {
                row1 += TILES_HEIGHT - row2;
                row2 = TILES_HEIGHT;
            }
            if (col2 > TILES_WIDTH) {
                col1 += TILES_WIDTH - col2;
                col2 = TILES_WIDTH;
            }

            float playerX = spectatee.position.x;
            float playerY = spectatee.position.y;
            Vector2 playerPos = new Vector2(playerX, playerY);

            float pX = (int) translateScreenToIso(playerPos).x;
            float pY = (int) translateScreenToIso(playerPos).y;

            int minI = MathUtils.ceil(pY - N_ROWS / 2);
            int maxI = MathUtils.ceil(pY + N_ROWS / 2);

            int minJ = MathUtils.ceil(pX - N_COLS / 2);
            int maxJ = MathUtils.ceil(pX + N_COLS / 2);

            // clamp to player view limits also
            if (row1 < minI) {
                row1 = minI;
            }
            if (col1 < minJ) {
                col1 = minJ;
            }
            if (row2 > maxI) {
                row2 = maxI;
            }
            if (col2 > maxJ) {
                col2 = maxJ;
            }

            it = 0;
            for (int row = row2; row >= row1; row--) {
                for (int col = col1; col <= col2; col++) {
                    float x = (col * halfTileWidth) + (row * halfTileWidth);
                    float y = (row * halfTileHeight) - (col * halfTileHeight);
                    final TiledMapTileLayer.Cell cell = layer.getCell(col, row);

                    it++;
                    if (cell != null)
                        renderCell(cell, x, y, layerOffsetX, layerOffsetY, layer.getOpacity());

                    if (rl == renderLayers.size()) {
                        Map<Integer, Entity> tileEntities = EntityController.getInstance().getEntitiesAtTilePos(col, row);
                        Map<Integer, Component.Character> characters = EntityController.getInstance().entityWorldState[col][row].characters;
                        Portal portal = EntityController.getInstance().entityWorldState[col][row].portal;
                        if(portal != null) {

                        }
                        if (tileEntities.size() > 0) { // render entities of tile if any exists
                            for (Map.Entry<Integer, Entity> entry : tileEntities.entrySet()) {
                                Integer eId = entry.getKey();
                                Entity entity = entry.getValue();
                                Vector2 pos;
                                Component.Attributes attr;

                                if(entity.get(Component.Position.class) == null) continue;

                                pos = new Vector2(entity.get(Component.Position.class).x, entity.get(Component.Position.class).y);
                                attr = entity.get(Component.Attributes.class);

                                rand = new Random(eId);
                                float r = rand.nextFloat();
                                float g = rand.nextFloat();
                                float b = rand.nextFloat();

                                shapeRenderer.setProjectionMatrix(cam.combined);
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                                shapeRenderer.setColor(new Color(r, g, b, 1f));
                                shapeRenderer.rect(pos.x, pos.y, attr.width, attr.height);
                                shapeRenderer.end();
                            }
                        }
                        if (characters.size() > 0) { // render characters of tile if any exists
                            for (Map.Entry<Integer, Component.Character> entry : characters.entrySet()) {
                                Integer eId = entry.getKey();
                                Component.Character entity = entry.getValue();
                                Vector2 pos;
                                Component.Attributes attr;

                                if(entity == null) continue;

                                pos = new Vector2(entity.position.x, entity.position.y);
                                attr = entity.attr;

                                rand = new Random(eId);
                                float r = rand.nextFloat();
                                float g = rand.nextFloat();
                                float b = rand.nextFloat();

                                shapeRenderer.setProjectionMatrix(cam.combined);
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                                shapeRenderer.setColor(new Color(r, g, b, 1f));
                                shapeRenderer.rect(pos.x, pos.y, attr.width, attr.height);
                                shapeRenderer.end();
                            }
                        }
                    }
                }
            }
        }

//        if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
//            if(spectatee == null  || spectatee.get(Component.Character.class) == null) {
//                spectatee = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
//                        get().getValue().character;
//                Log.info("game-server", "Spectating player: " + spectatee.get(Component.Character.class).tag.name);
//                return;
//            }

//            for(TiledMapTileLayer layer : renderLayers) {
//                float playerX = spectatee.get(Component.Character.class).position.x;
//                float playerY = spectatee.get(Component.Character.class).position.y;
//                Vector2 playerPos = new Vector2(playerX, playerY);
//
//                float pX = (int) translateScreenToIso(playerPos).x;
//                float pY = (int) translateScreenToIso(playerPos).y;
//
//                int minI = MathUtils.ceil(pY - N_ROWS / 2);
//                int maxI = MathUtils.ceil(pY + N_ROWS / 2);
//
//                int minJ = MathUtils.ceil(pX - N_COLS / 2);
//                int maxJ = MathUtils.ceil(pX + N_COLS / 2);
//
//                // clamp to limits but always send same amount of info
//                if (minI < 0) {
//                    maxI -= minI;
//                    minI = 0;
//                }
//                if (minJ < 0) {
//                    maxJ -= minJ;
//                    minJ = 0;
//                }
//                if (maxI > TILES_HEIGHT) {
//                    minI += TILES_HEIGHT - maxI;
//                    maxI = TILES_HEIGHT;
//                }
//                if (maxJ > TILES_WIDTH) {
//                    minJ += TILES_WIDTH - maxJ;
//                    maxJ = TILES_WIDTH;
//                }
//
//                float tileWidth = layer.getTileWidth() * unitScale;
//                float tileHeight = layer.getTileHeight() * unitScale;
//
//                final float layerOffsetX = layer.getRenderOffsetX() * unitScale - renderer.getViewBounds().x * (layer.getParallaxX() - 1);
//                // offset in tiled is y down, so we flip it
//                final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - renderer.getViewBounds().y * (layer.getParallaxY() - 1);
//
//                float halfTileWidth = tileWidth * 0.5f;
//                float halfTileHeight = tileHeight * 0.5f;
//
//                // draw only visible tiles for player (in correct order for isometric rendering)
//                int it = 0;
//                for (int row = maxI; row >= minI; row--) {
//                    for (int col = minJ; col <= maxJ; col++) {
//                        float x = (col * halfTileWidth) + (row * halfTileWidth);
//                        float y = (row * halfTileHeight) - (col * halfTileHeight);
//                        final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
//                        it++;
//                        if (cell != null)
//                            renderCell(cell, x, y, layerOffsetX, layerOffsetY, layer.getOpacity());
//
//                        if (layer.getName().contains("Wall")) {
//                            Map<Integer, Entity> tileEntities = EntityController.getInstance().getEntitiesAtTilePos(col, row);
//                            if (tileEntities.size() > 0) { // render entities of tile if any exists
//                                for (Map.Entry<Integer, Entity> entry : tileEntities.entrySet()) {
//                                    Integer eId = entry.getKey();
//                                    Entity entity = entry.getValue();
//                                    Vector2 pos;
//                                    Component.Attributes attr;
//                                    if (!entity.has(Component.Character.class)) {// non-player entity
//                                        pos = new Vector2(entity.get(Component.Position.class).x, entity.get(Component.Position.class).y);
//                                        attr = entity.get(Component.Attributes.class);
//                                    } else {
//                                        pos = new Vector2(entity.get(Component.Character.class).position.x,
//                                                entity.get(Component.Character.class).position.y);
//                                        attr = entity.get(Component.Character.class).attr;
//                                    }
//                                    rand = new Random(eId);
//                                    float r = rand.nextFloat();
//                                    float g = rand.nextFloat();
//                                    float b = rand.nextFloat();
//
//                                    shapeRenderer.setProjectionMatrix(cam.combined);
//                                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//                                    shapeRenderer.setColor(new Color(r, g, b, 1f));
//                                    shapeRenderer.rect(pos.x, pos.y + attr.height / 2f, attr.width, attr.height);
//                                    shapeRenderer.end();
//                                }
//                            }
//                        }
//                    }
//                }
//                System.out.println(it);
//            }

//            shapeRenderer.setProjectionMatrix(cam.combined);
//            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//            shapeRenderer.setColor(Color.RED);
//            shapeRenderer.rect(playerX, playerY+0.3f, 0.3f, 0.6f);
//            shapeRenderer.end();
 //       }

    }

    protected TiledMapTileLayer.Cell createTileLayerCell (boolean flipHorizontally, boolean flipVertically, boolean flipDiagonally) {
        TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
        if (flipDiagonally) {
            if (flipHorizontally && flipVertically) {
                cell.setFlipHorizontally(true);
                cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270);
            } else if (flipHorizontally) {
                cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270);
            } else if (flipVertically) {
                cell.setRotation(TiledMapTileLayer.Cell.ROTATE_90);
            } else {
                cell.setFlipVertically(true);
                cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270);
            }
        } else {
            cell.setFlipHorizontally(flipHorizontally);
            cell.setFlipVertically(flipVertically);
        }
        return cell;
    }

    public Vector3 translateScreenToIso (Vector2 vec) {
        screenPos.set(vec.x, vec.y, 0);
        screenPos.mul(invIsotransform);

        return screenPos;
    }

    public void renderCell(TiledMapTileLayer.Cell cell, float x, float y, float layerOffsetX, float layerOffsetY, float layerOpacity) {
        if (cell == null) return;
        final TiledMapTile tile = cell.getTile();

        if (tile != null) {
            final boolean flipX = cell.getFlipHorizontally();
            final boolean flipY = cell.getFlipVertically();
            final int rotations = cell.getRotation();
            AnimatedTiledMapTile.updateAnimationBaseTime(); // update tile animation before drawing in case its an animated tile

            TextureRegion region = tile.getTextureRegion();

            float x1 = x + tile.getOffsetX() * unitScale + layerOffsetX;
            float y1 = y + tile.getOffsetY() * unitScale + layerOffsetY;
            float x2 = x1 + region.getRegionWidth() * unitScale;
            float y2 = y1 + region.getRegionHeight() * unitScale;

            float u1 = region.getU();
            float v1 = region.getV2();
            float u2 = region.getU2();
            float v2 = region.getV();

            float vertices[] = new float[20];
            final Color batchColor = batch.getColor();
            float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layerOpacity);

            vertices[X1] = x1;
            vertices[Y1] = y1;
            vertices[C1] = color;
            vertices[U1] = u1;
            vertices[V1] = v1;

            vertices[X2] = x1;
            vertices[Y2] = y2;
            vertices[C2] = color;
            vertices[U2] = u1;
            vertices[V2] = v2;

            vertices[X3] = x2;
            vertices[Y3] = y2;
            vertices[C3] = color;
            vertices[U3] = u2;
            vertices[V3] = v2;

            vertices[X4] = x2;
            vertices[Y4] = y1;
            vertices[C4] = color;
            vertices[U4] = u2;
            vertices[V4] = v1;

            if (flipX) {
                float temp = vertices[U1];
                vertices[U1] = vertices[U3];
                vertices[U3] = temp;
                temp = vertices[U2];
                vertices[U2] = vertices[U4];
                vertices[U4] = temp;
            }
            if (flipY) {
                float temp = vertices[V1];
                vertices[V1] = vertices[V3];
                vertices[V3] = temp;
                temp = vertices[V2];
                vertices[V2] = vertices[V4];
                vertices[V4] = temp;
            }
            if (rotations != 0) {
                switch (rotations) {
                    case TiledMapTileLayer.Cell.ROTATE_90: {
                        float tempV = vertices[V1];
                        vertices[V1] = vertices[V2];
                        vertices[V2] = vertices[V3];
                        vertices[V3] = vertices[V4];
                        vertices[V4] = tempV;

                        float tempU = vertices[U1];
                        vertices[U1] = vertices[U2];
                        vertices[U2] = vertices[U3];
                        vertices[U3] = vertices[U4];
                        vertices[U4] = tempU;
                        break;
                    }
                    case TiledMapTileLayer.Cell.ROTATE_180: {
                        float tempU = vertices[U1];
                        vertices[U1] = vertices[U3];
                        vertices[U3] = tempU;
                        tempU = vertices[U2];
                        vertices[U2] = vertices[U4];
                        vertices[U4] = tempU;
                        float tempV = vertices[V1];
                        vertices[V1] = vertices[V3];
                        vertices[V3] = tempV;
                        tempV = vertices[V2];
                        vertices[V2] = vertices[V4];
                        vertices[V4] = tempV;
                        break;
                    }
                    case TiledMapTileLayer.Cell.ROTATE_270: {
                        float tempV = vertices[V1];
                        vertices[V1] = vertices[V4];
                        vertices[V4] = vertices[V3];
                        vertices[V3] = vertices[V2];
                        vertices[V2] = tempV;

                        float tempU = vertices[U1];
                        vertices[U1] = vertices[U4];
                        vertices[U4] = vertices[U3];
                        vertices[U3] = vertices[U2];
                        vertices[U2] = tempU;
                        break;
                    }
                }
            }
            batch.begin();
            batch.draw(region.getTexture(), vertices, 0, 20);
            batch.end();
        }
    }

    public void dispose () {
        map.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.M && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            RogueFantasyServer.hideMap();
        } else if(keycode == Input.Keys.RIGHT && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            Iterator<Map.Entry<Integer, GameServer.CharacterConnection >> it = GameServer.getInstance().loggedIn.entrySet().iterator();
            specId = (specId + 1) % GameServer.getInstance().loggedIn.size();
            int i = 0;
            while (it.hasNext()) {
                Map.Entry<Integer, GameServer.CharacterConnection> entry = it.next();
                if(i == specId) {
                    spectatee = entry.getValue().character;
                    Log.info("game-server", "Spectating player: " + spectatee.tag.name);
                    break;
                }
                i++;
            }
        } else if(keycode == Input.Keys.LEFT && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            Iterator<Map.Entry<Integer, GameServer.CharacterConnection>> it = GameServer.getInstance().loggedIn.entrySet().iterator();
            specId = (specId + GameServer.getInstance().loggedIn.size() - 1) % GameServer.getInstance().loggedIn.size();
            int i = 0;
            while (it.hasNext()) {
                Map.Entry<Integer, GameServer.CharacterConnection> entry = it.next();
                if (i == specId) {
                    spectatee = entry.getValue().character;
                    Log.info("game-server", "Spectating player: " + spectatee.tag.name);
                    break;
                }
                i++;
            }
        } else if(keycode == Input.Keys.W && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            if(spectatee != null && spectatee.position !=null) {
                spectatee.position.y++;
            }
        } else if(keycode == Input.Keys.S && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            if(spectatee != null && spectatee.position !=null) {
                spectatee.position.y--;
            }
        } else if(keycode == Input.Keys.A && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            if(spectatee != null && spectatee.position !=null) {
                spectatee.position.x--;
            }
        } else if(keycode == Input.Keys.D && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            if(spectatee != null && spectatee.position !=null) {
                spectatee.position.x++;
            }
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float x = Gdx.input.getDeltaX() * Gdx.graphics.getDeltaTime() * cam.zoom * 25f;
        float y = Gdx.input.getDeltaY() * Gdx.graphics.getDeltaTime() * cam.zoom * 25f;

        cam.translate(-x,y);
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        cam.zoom += amountY * Gdx.graphics.getDeltaTime() * 3f;
        return false;
    }

    public float getUnitScale() {
        return unitScale;
    }

    /**
     * Class that represents portals of the map
     */
    public class Portal {
        public int portalId;
        public int tileX, tileY;
        public Rectangle hitBox;
        public int destX, destY;
    }
}
