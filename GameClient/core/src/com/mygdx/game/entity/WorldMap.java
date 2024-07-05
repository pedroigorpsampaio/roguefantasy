package com.mygdx.game.entity;

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
import static com.mygdx.game.entity.Entity.rectOffsetDown;
import static com.mygdx.game.entity.Entity.rectOffsetLeft;
import static com.mygdx.game.entity.Entity.rectOffsetRight;
import static com.mygdx.game.entity.Entity.rectOffsetUp;
import static com.mygdx.game.network.GameRegister.N_COLS;
import static com.mygdx.game.network.GameRegister.N_ROWS;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.minlog.Log;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class WorldMap {
    public static ArrayList<Rectangle> portals = new ArrayList<>();
    private static WorldMap instance = null;
    public static ArrayList<GameRegister.Layer> layers = new ArrayList<>();
    public static int tileOffsetX=0,tileOffsetY=0;
    private TiledMap map;
    private SpriteBatch batch;
    public static Entity hoverEntity = null; // current entity mouse is hovering on
    public static float WORLD_WIDTH, WORLD_HEIGHT;
    public static int TILES_WIDTH = 1000, TILES_HEIGHT = 1000, TEX_WIDTH = 32, TEX_HEIGHT = 16; // in agreement with server map
    public final static float edgeFactor = 1.0605f; // min value that prevents tiles from edge bleeding with linear filter in texture
    public static final float unitScale = 1 / 32f;
    private OrthographicCamera camera;
    private Vector2 topRight = new Vector2();
    private Vector2 bottomLeft = new Vector2();
    private Vector2 topLeft = new Vector2();
    private Vector2 bottomRight = new Vector2();
    private static Vector3 screenPos = new Vector3();
    public static Matrix4 isoTransform;
    private static Matrix4 invIsotransform;
    // Array containing layers with whats interesting to the player but in world size for correct rendering
    ArrayList<TiledMapTileLayer> tmxLayers = new ArrayList<>();
    private Rectangle viewBounds = new Rectangle();

    public static WorldMap getInstance() {
        if(instance == null)
            instance = new WorldMap();
        return instance;
    }

    private WorldMap() {

    }

    public void init(TiledMap map, SpriteBatch batch, OrthographicCamera camera) {
        Log.info("game-server", "Loading world map...");
        this.map = map; // map loaded in load screen
        this.batch = batch;
        this.camera = camera;
        tmxLayers = new ArrayList<>();
        viewBounds = new Rectangle();
        layers = new ArrayList<>();

        WORLD_WIDTH = TILES_WIDTH * 32f;
        WORLD_HEIGHT = TILES_HEIGHT * 16f;

        // creates array of tmx layers with the right size to be able to correctly draw info received by server (only tile layers and not entity layers)
        for(int l = 0; l < map.getLayers().size(); l++) {
            if(!map.getLayers().get(l).getClass().equals(TiledMapTileLayer.class)) continue;
            if(map.getLayers().get(l).getProperties().get("entity_layer", Boolean.class)) continue;
            TiledMapTileLayer tmxLayer = new TiledMapTileLayer(TILES_WIDTH, TILES_HEIGHT, 32, 16);
            tmxLayer.getProperties().putAll(map.getLayers().get(l).getProperties());
            tmxLayer.setName(map.getLayers().get(l).getName());
            tmxLayer.setVisible(map.getLayers().get(l).isVisible());
            tmxLayer.setParallaxX(map.getLayers().get(l).getParallaxX());
            tmxLayer.setParallaxY(map.getLayers().get(l).getParallaxY());
            tmxLayer.setOffsetX(map.getLayers().get(l).getOffsetX());
            tmxLayer.setOffsetY(map.getLayers().get(l).getOffsetY());
            tmxLayer.setOpacity(map.getLayers().get(l).getOpacity());
            tmxLayers.add(tmxLayer);
        }

        // create the isometric transform
        isoTransform = new Matrix4();
        isoTransform.idt();

        // isoTransform.translate(0, 32, 0);
        isoTransform.scale((float)(Math.sqrt(2.0) / 2.0), (float)(Math.sqrt(2.0) / 4.0), 1.0f);
        isoTransform.rotate(0.0f, 0.0f, 1.0f, -45);

        // ... and the inverse matrix
        invIsotransform = new Matrix4(isoTransform);
        invIsotransform.inv();
    }

    /**
     * Projects to iso perspective
     * @param vec   the vec2 containing points to be projected
     * @return  vec3 containing the projection
     */
    public static Vector3 translateScreenToIso (Vector2 vec) {
        screenPos.set(vec.x, vec.y, 0);
        screenPos.mul(invIsotransform);

        return screenPos;
    }

    /**
     * Given position in world space, return the respective indexes in world 2d iso tile array
     * @param position the x,y position in world space as a Vec2
     * @return a Vec2 containing the indexes of given position in the 2d iso tile array
     */
    public static Vector2 toIsoTileCoordinates(Vector2 position) {
        float tileWidth = TEX_WIDTH * unitScale;

        float pX = MathUtils.floor(translateScreenToIso(position).x / tileWidth) +1;
        float pY = MathUtils.floor(translateScreenToIso(position).y / tileWidth);

        return new Vector2(pX, pY);
    }

    /**
     * Checks if world space coordinates is within player AoI
     * @return true if it is, false otherwise
     */
    public static boolean isWithinPlayerAOI(Vector2 position) {
        Vector2 otherTilePos = toIsoTileCoordinates(position);
        Entity.Character player = GameClient.getInstance().getClientCharacter();
        if(player == null) return false;
        Vector2 playerPos = new Vector2(player.interPos.x, player.interPos.y);
        Vector2 playerTilePos = toIsoTileCoordinates(playerPos);
        float pX = playerTilePos.x;
        float pY = playerTilePos.y;

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

        if(otherTilePos.x < minJ+1) { return false; } // +1/-1s are a way to guarantee that we remove them before server stops considering
        if(otherTilePos.y < minI+1) { return false; } // out of AoI and stop sending message updates about it
        if(otherTilePos.x > maxJ-1) { return false; }
        if(otherTilePos.y > maxI-1) {return false; }

        return true;
    }

    /**
     * Checks if world space coordinates is within world bound
     * @return true if it is, false otherwise
     */
    public static boolean isWithinWorldBounds(Vector2 position) {
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
        Vector2 tPosCenter = toIsoTileCoordinates(new Vector2(position.x, position.y));
        Vector2 tPosDown = toIsoTileCoordinates(new Vector2(position.x, position.y-rectOffsetDown));
        Vector2 tPosUp = toIsoTileCoordinates(new Vector2(position.x, position.y+rectOffsetUp));
        Vector2 tPosLeft = toIsoTileCoordinates(new Vector2(position.x-rectOffsetLeft, position.y));
        Vector2 tPosRight = toIsoTileCoordinates(new Vector2(position.x+rectOffsetRight, position.y));
        TiledMapTileLayer floorLayer = tmxLayers.get(0);

        if(floorLayer.getCell((int)tPosCenter.x, (int)tPosCenter.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosDown.x, (int)tPosDown.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosUp.x, (int)tPosUp.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosLeft.x, (int)tPosLeft.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        if(floorLayer.getCell((int)tPosRight.x, (int)tPosRight.y) == null) // if its a cell that does not exist, its not walkable
            return false;

        // check if there is a wall on tile
        synchronized (GameClient.getInstance().getWalls()) {
            Iterator<Map.Entry<Integer, Entity.Wall>> iterator = GameClient.getInstance().getWalls().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Entity.Wall> entry = iterator.next();
                if(entry.getValue().tileX == (int)tPosDown.x &&
                        entry.getValue().tileY == (int)tPosDown.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosUp.x &&
                        entry.getValue().tileY == (int)tPosUp.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosCenter.x &&
                        entry.getValue().tileY == (int)tPosCenter.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosLeft.x &&
                        entry.getValue().tileY == (int)tPosLeft.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosRight.x &&
                        entry.getValue().tileY == (int)tPosRight.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
            }
        }
        // check if there is a tree on tile
        synchronized (GameClient.getInstance().getTrees()) {
            Iterator<Map.Entry<Integer, Entity.Tree>> iterator = GameClient.getInstance().getTrees().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Entity.Tree> entry = iterator.next();
                if(entry.getValue().tileX == (int)tPosDown.x &&
                        entry.getValue().tileY == (int)tPosDown.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosUp.x &&
                        entry.getValue().tileY == (int)tPosUp.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosCenter.x &&
                        entry.getValue().tileY == (int)tPosCenter.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosLeft.x &&
                        entry.getValue().tileY == (int)tPosLeft.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
                if(entry.getValue().tileX == (int)tPosRight.x &&
                        entry.getValue().tileY == (int)tPosRight.y) {
                    if(!entry.getValue().isWalkable)
                        return false;
                }
            }
        }

        if(floorLayer.getCell((int)tPosUp.x, (int)tPosUp.y).getTile() == null) return false;

        if(floorLayer.getCell((int)tPosUp.x, (int)tPosUp.y).getTile().getProperties().get("walkable", Boolean.class) &&
                floorLayer.getCell((int)tPosDown.x, (int)tPosDown.y).getTile().getProperties().get("walkable", Boolean.class) &&
                floorLayer.getCell((int)tPosCenter.x, (int)tPosCenter.y).getTile().getProperties().get("walkable", Boolean.class) &&
                floorLayer.getCell((int)tPosLeft.x, (int)tPosLeft.y).getTile().getProperties().get("walkable", Boolean.class) &&
                floorLayer.getCell((int)tPosRight.x, (int)tPosRight.y).getTile().getProperties().get("walkable", Boolean.class))
            return true;
        else
            return false;
    }

    /**
     * Updates world map view bounds based on cameras current data
     */
    public void setView () {
        //batch.setProjectionMatrix(camera.combined);
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
    }

    /**
     * Render world layers recreating it from state message tile layers
     */
    public void render() {
        if(layers == null || layers.size() == 0) return ; // server hasn't sent tile data yet

        setView(); // update world map view bounds based on game camera

        Entity.Character spectatee = GameClient.getInstance().getClientCharacter();

        if(spectatee == null) return;

        float playerX = spectatee.interPos.x;
        float playerY = spectatee.interPos.y;
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

        TiledMapTileSets tilesets = map.getTileSets();
        int MASK_CLEAR = 0xE0000000;
        int FLAG_FLIP_HORIZONTALLY = 0x80000000;
        int FLAG_FLIP_VERTICALLY = 0x40000000;
        int FLAG_FLIP_DIAGONALLY = 0x20000000;

        int l = 0;
        // loop through tile layers to be rendered TODO: object layer can be at client side also? or should it be sent?
        for (TiledMapTileLayer layer : tmxLayers) {
            float tileWidth = layer.getTileWidth() * unitScale;
            float tileHeight = layer.getTileHeight() * unitScale;

            final float layerOffsetX = layer.getRenderOffsetX() * unitScale - viewBounds.x * (layer.getParallaxX() - 1);
            // offset in tiled is y down, so we flip it
            final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - viewBounds.y * (layer.getParallaxY() - 1);

            float halfTileWidth = tileWidth * 0.5f;
            float halfTileHeight = tileHeight * 0.5f;

            // loop only through AoI of player creating tiles and placing it in correct position of world
            for (int row = maxI, i = 0; row >= minI; row--, i++) {
                for (int col = minJ, j = 0; col <= maxJ; col++, j++) {
                    int id = layers.get(l).tiles[j][i];
                    boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
                    boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
                    boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);
                    TiledMapTile tile = tilesets.getTile(id & ~MASK_CLEAR);

                    if(tile != null) {
                        //tile.getTextureRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    }
                    TiledMapTileLayer.Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
                    cell.setTile(tile);

                    int originalX = j + tileOffsetX; int originalY = i + tileOffsetY;
                    layer.setCell(originalX, originalY, cell);

                    float x = (col * halfTileWidth) + (row * halfTileWidth);
                    float y = (row * halfTileHeight) - (col * halfTileHeight);
                    if(!layer.getProperties().get("entity_layer", Boolean.class))
                        renderCell(layer.getCell(col, row), x, y, layerOffsetX, layerOffsetY, layer.getOpacity());
                }
            }
            l++;
        }
    }

    /**
     * Creates a cell for tile map layers
     * @param flipHorizontally  if its flipped horizontally
     * @param flipVertically    if its flipped vertically
     * @param flipDiagonally    if its flipped diagonally
     * @return  the cell created with the provided flags
     */
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

//    public ArrayList<Entity.Wall> getWallList() {
//        ArrayList<Entity.Wall> walls = new ArrayList<>();
//
//        Entity.Character spectatee = GameClient.getInstance().getClientCharacter();
//
//        if(spectatee == null) return walls; // player is not loaded yet, return empty walls
//
//        TiledMapTileSets tilesets = map.getTileSets();
//        int MASK_CLEAR = 0xE0000000;
//        int FLAG_FLIP_HORIZONTALLY = 0x80000000;
//        int FLAG_FLIP_VERTICALLY = 0x40000000;
//        int FLAG_FLIP_DIAGONALLY = 0x20000000;
//
//        int l = 0;
//        for (TiledMapTileLayer layer : tmxLayers) {
//            if(!layer.getProperties().get("entity_layer", Boolean.class)) {
//                l++;
//                continue;
//            }
//
//            // loop only through AoI of player creating walls as entities to be rendered in order
//            for (int i = 0; i < layers.get(l).tiles.length; i++) {
//                for (int j = 0; j < layers.get(l).tiles[i].length; j++) {
//                    int id = layers.get(l).tiles[j][i];
//                    if (id < 0) continue;
//
//                    TiledMapTile tile = tilesets.getTile(id & ~MASK_CLEAR);
//
//                    if (tile != null) {
//                        walls.add(new Entity.Wall(id, tile, new Vector2(j, i)));
//                    }
//                }
//            }
//        }
//        return walls;
//    }

    /**
     * Renders a cell from a tile map layer
     * @param cell  the cell to be rendered
     * @param x     the position x of rendering
     * @param y     the position y of rendering
     * @param layerOffsetX  the current X layer offset
     * @param layerOffsetY  the current Y layer offset
     * @param layerOpacity  the current opacity of layer
     */
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
            float x2 = x1 + region.getRegionWidth() * unitScale * edgeFactor;
            float y2 = y1 + region.getRegionHeight() * unitScale * edgeFactor;

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
            batch.draw(region.getTexture(), vertices, 0, 20);
        }
    }

    /**
     * Debugs map tiles
     */
    public static void debugMap () {
        System.out.println("### MAP DEBUG ### \n\n");

        for(int l = 0; l < layers.size(); l++) {
            int[][] tiles = layers.get(l).tiles;
            for (int i = 0; i < tiles.length; i++) {
                System.out.println(" ");
                for (int j = 0; j < tiles[i].length; j++) {
                    System.out.printf(tiles[i][j] + " ,");
                }
            }
        }

        System.out.println("\n\n### END DEBUG ### ");
    }

    public void dispose () {
        map.dispose();
    }

    public TiledMapTile getTileFromId(int tileId) {
        TiledMapTileSets tileset = map.getTileSets();
        int MASK_CLEAR = 0xE0000000;
        TiledMapTile tile = tileset.getTile(tileId & ~MASK_CLEAR);
        return tile;
    }
}
