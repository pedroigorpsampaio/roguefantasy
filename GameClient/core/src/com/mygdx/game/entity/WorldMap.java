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
import static com.mygdx.game.network.GameRegister.N_COLS;
import static com.mygdx.game.network.GameRegister.N_ROWS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.minlog.Log;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.util.ArrayList;

public class WorldMap {
    public static ArrayList<GameRegister.Layer> layers = new ArrayList<>();
    private final TiledMap map;
    private final SpriteBatch batch;
    public static float WORLD_WIDTH, WORLD_HEIGHT;
    public static int TILES_WIDTH, TILES_HEIGHT, TEX_WIDTH, TEX_HEIGHT;
    public static final float unitScale = 1 / 32f;
    private final IsometricTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private OrthographicCamera cam;
    private Vector2 topRight = new Vector2();
    private Vector2 bottomLeft = new Vector2();
    private Vector2 topLeft = new Vector2();
    private Vector2 bottomRight = new Vector2();
    private Vector3 screenPos = new Vector3();
    private Matrix4 isoTransform;
    private Matrix4 invIsotransform;

    public WorldMap(TiledMap map, SpriteBatch batch, OrthographicCamera camera) {
        Log.info("game-server", "Loading world map...");
        this.map = map; // map loaded in load screen
        this.batch = batch;
        this.camera = camera;
        renderer = new IsometricTiledMapRenderer(map, unitScale, batch);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        TiledMapTileLayer tileLayer =  (TiledMapTileLayer) map.getLayers().get(0);
        WORLD_WIDTH = tileLayer.getWidth() * 32f;
        WORLD_HEIGHT = tileLayer.getHeight() * 16f;
        TILES_WIDTH = tileLayer.getWidth();
        TILES_HEIGHT = tileLayer.getHeight();
        TEX_WIDTH = tileLayer.getTileWidth();
        TEX_HEIGHT = tileLayer.getTileHeight();

        cam =  new OrthographicCamera(32f, 32f * (h / w));
        cam.zoom = 2f;

        // create the isometric transform
        isoTransform = new Matrix4();
        isoTransform.idt();

        // isoTransform.translate(0, 32, 0);
        isoTransform.scale((float)(Math.sqrt(2.0) / 2.0), (float)(Math.sqrt(2.0) / 4.0), 1.0f);
        isoTransform.rotate(0.0f, 0.0f, 1.0f, -45);

        // ... and the inverse matrix
        invIsotransform = new Matrix4(isoTransform);
        invIsotransform.inv();

        // create empty layers of the map (ignore object layer for now)
//        ArrayList<TiledMapTileLayer> layers = new ArrayList<>();
//        for(MapLayer mapLayer : map.getLayers()) {
//            if (mapLayer.getClass().equals(TiledMapTileLayer.class)) { // only cuts tile map layers
//                TiledMapTileLayer tmLayer = (TiledMapTileLayer) mapLayer;
//                layers.add(tmLayer);
//            }
//        }
    }

    public void resize(int width, int height) {
        cam.viewportWidth = 32f;
        cam.viewportHeight = 32f * height/width;
        cam.update();
    }

    public void updateCamera() {
        Entity.Character spectatee = GameClient.getInstance().getClientCharacter();
        float playerX = spectatee.interPos.x;
        float playerY = spectatee.interPos.y;
//        cam.position.x = playerX;
//        cam.position.y = playerY;
//        cam.update();



        //Creating a vector 3 which represents the target location myplayer)
        Vector3 target = new Vector3(playerX,playerY,0);
        //Change speed to your need
        final float speed=Gdx.graphics.getDeltaTime()*5f,ispeed=1.0f-speed;
        //The result is roughly: old_position*0.9 + target * 0.1
        Vector3 cameraPosition = cam.position;
        cameraPosition.scl(ispeed);
        target.scl(speed);
        cameraPosition.add(target);
        cam.position.set(cameraPosition);
        cam.update();
    }

    /**
     * Projects to iso perspective
     * @param vec   the vec2 containing points to be projected
     * @return  vec3 containing the projection
     */
    private Vector3 translateScreenToIso (Vector2 vec) {
        screenPos.set(vec.x, vec.y, 0);
        screenPos.mul(invIsotransform);

        return screenPos;
    }

    /**
     * Given position in world space, return the respective indexes in world 2d iso tile array
     * @param position the x,y position in world space as a Vec2
     * @return a Vec2 containing the indexes of given position in the 2d iso tile array
     */
    public Vector2 toIsoTileCoordinates(Vector2 position) {
        float tileWidth = TEX_WIDTH * unitScale;

        float pX = MathUtils.floor(translateScreenToIso(position).x / tileWidth);
        float pY = MathUtils.floor(translateScreenToIso(position).y / tileWidth);

        return new Vector2(pX, pY);
    }

    /**
     * Checks if world space coordinates is within world bound
     * @return true if it is, false otherwise
     */
    public boolean isWithinWorldBounds(Vector2 position) {
        Vector2 tPos = toIsoTileCoordinates(position);

        if(tPos.x < 0) { return false; }
        if(tPos.y < 0) { return false; }
        if(tPos.x > TILES_WIDTH) { return false; }
        if(tPos.y > TILES_HEIGHT) {return false; }

        return true;
    }


    /**
     * Render world layers recreating it from state message tile layers
     */
    public void render() {
        if(layers == null || layers.size() == 0) return ; // server hasn't sent tile data yet

        updateCamera();
//        if(GameClient.getInstance().isPredictingRecon.get()) return;
        renderer.setView(cam);

        Entity.Character spectatee = GameClient.getInstance().getClientCharacter();
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

        TiledMapTileSets tilesets = map.getTileSets();
        int MASK_CLEAR = 0xE0000000;
        int FLAG_FLIP_HORIZONTALLY = 0x80000000;
        int FLAG_FLIP_VERTICALLY = 0x40000000;
        int FLAG_FLIP_DIAGONALLY = 0x20000000;

        int l = 0;
        for (MapLayer mapLayer : map.getLayers()) { // loop through layers
            if (mapLayer.getClass().equals(TiledMapTileLayer.class)) { // only renders tilemap layers TODO: object layer can be at client side also? or should it be sent?
                TiledMapTileLayer layer = (TiledMapTileLayer) mapLayer;

                float tileWidth = layer.getTileWidth() * unitScale;
                float tileHeight = layer.getTileHeight() * unitScale;

                final float layerOffsetX = layer.getRenderOffsetX() * unitScale - renderer.getViewBounds().x * (layer.getParallaxX() - 1);
                // offset in tiled is y down, so we flip it
                final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - renderer.getViewBounds().y * (layer.getParallaxY() - 1);

                float halfTileWidth = tileWidth * 0.5f;
                float halfTileHeight = tileHeight * 0.5f;

                // places the layer culled by the server in the correct position in world 2d array for correct rendering
                for (int row = maxI; row >= minI; row--) {
                    for (int col = minJ; col <= maxJ; col++) {
                        int id = layers.get(l).tiles[col-minJ][row-minI];
                        boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
                        boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
                        boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);
                        TiledMapTile tile = tilesets.getTile(id & ~MASK_CLEAR);
                        if(tile != null) {
                            tile.getTextureRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                            tile.setBlendMode(TiledMapTile.BlendMode.NONE);
                        }
                        TiledMapTileLayer.Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
                        cell.setTile(tile);

                        if(((TiledMapTileLayer) mapLayer).getCell(col, row) == null) {
                            ((TiledMapTileLayer) mapLayer).setCell(col, row, cell);
                        }

                        float x = (col * halfTileWidth) + (row * halfTileWidth);
                        float y = (row * halfTileHeight) - (col * halfTileHeight);
                        renderCell(((TiledMapTileLayer) mapLayer).getCell(col, row), x, y, layerOffsetX, layerOffsetY, layer.getOpacity());
                    }
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

    /**
     * Debugs map tiles
     */
    public static void debugMap () {
        System.out.println("### MAP DEBUG ### \n\n");

        for(int l = 0; l < layers.size(); l++) {
            System.out.println("\n\nLAYER "+l+": isEntityLayer? : " + layers.get(l).isEntityLayer + "\n");
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

    public void zoom(float amountY) {
        cam.zoom += amountY * Gdx.graphics.getDeltaTime() * 3f;
    }
}
