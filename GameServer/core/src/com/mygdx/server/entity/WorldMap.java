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

import com.badlogic.gdx.ApplicationAdapter;
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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.network.GameServer;
import com.mygdx.server.ui.RogueFantasyServer;


/**
 * Class responsible for loading map from file and to
 * control the 2D array containing world map logic
 */
public class WorldMap implements InputProcessor {
    private static final int N_ROWS = 46;
    private static final int N_COLS = 46;
    private final OrthographicCamera cam;
    private TiledMapTileLayer layerCut;
    private final Matrix4 isoTransform;
    private final Matrix4 invIsotransform;
    private final float unitScale;
    private IsometricTiledMapRenderer renderer;
    public static float WORLD_WIDTH, WORLD_HEIGHT, VIEWPORT_WIDTH = 1280, VIEWPORT_HEIGHT = 720;
    private TiledMap map;
    private Vector3 screenPos = new Vector3();
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer; // for debug

    // constructor loads map from file and prepares the vars for controlling world state
    public WorldMap(String fileName, SpriteBatch batch) {
        Log.info("game-server", "Loading world map...");
        map = new TmxMapLoader().load(fileName);
        this.batch = batch;
        unitScale = 1 / 32f;
        renderer = new IsometricTiledMapRenderer(map, unitScale, batch);
        shapeRenderer = new ShapeRenderer();
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

       // cam.setToOrtho(false, 30, 20);
        TiledMapTileLayer tileLayer =  (TiledMapTileLayer) map.getLayers().get(0);
        WORLD_WIDTH = tileLayer.getWidth() * 32f;
        WORLD_HEIGHT = tileLayer.getHeight() * 16f;

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
            cam.position.x = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.x;
            cam.position.y = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.y;
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

    public void render() {
        updateCamera();

        renderer.setView(cam);
        Batch batch = renderer.getBatch();
        //batch.setColor(new Color(Color.RED));

        TiledMapTileLayer layerBase = (TiledMapTileLayer) map.getLayers().get(0);

        // cuts olny what its interesting to the player
        TiledMapTileLayer layer = layerBase;

        if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
            //for (MapLayer layer : map.getLayers()) {
            float playerX = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.x;
            float playerY = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.y;
            Vector2 playerPos = new Vector2(playerX, playerY);

            float tileWidth = layer.getTileWidth() * unitScale;

            int row1 = (int)(translateScreenToIso(playerPos).y / tileWidth) - 14;
            int row2 = (int)(translateScreenToIso(playerPos).y / tileWidth) + 15;

            int col1 = (int)(translateScreenToIso(playerPos).x / tileWidth) - 14;
            int col2 = (int)(translateScreenToIso(playerPos).x / tileWidth) + 15;

            float pX = (int)translateScreenToIso(playerPos).x;
            float pY = (int)translateScreenToIso(playerPos).y;

            int minI =  MathUtils.ceil(pY - N_ROWS/2);
            int maxI =  MathUtils.ceil(pY + N_ROWS/2);

            int minJ = MathUtils.ceil(pX - N_COLS/2);
            int maxJ =  MathUtils.ceil(pX + N_COLS/2);

            // clamp to limits but always send same amount of info
            if(minI < 0) { maxI -= minI; minI = 0; }
            if(minJ < 0) { maxJ -= minJ; minJ = 0; }
            if(maxI > layerBase.getHeight()) {minI += layerBase.getHeight() - maxI ; maxI = layerBase.getHeight(); }
            if(maxJ > layerBase.getWidth()) {minJ += layerBase.getHeight() - maxJ ; maxJ = layerBase.getWidth(); }

            // the layer containing only visible tiles for player
            layerCut = new TiledMapTileLayer(N_COLS, N_ROWS, 32, 16);

            // stores visible tiles in the cull layer
            for (int row = minI; row <= maxI; row++) {
                for (int col = minJ; col <= maxJ; col++) {
                    final TiledMapTileLayer.Cell cell = layerBase.getCell(col, row);
                    layerCut.setCell(col-minJ, row-minI, cell);
                }
            }

            // AFTER SENDING TO CLIENT, CLIENT CAN RECREATE IN THE CORRECT POSITION WITH THE FOLLOWING STEPS:


            // create big layer that contains the size of the world
            TiledMapTileLayer bigLayer = new TiledMapTileLayer(layerBase.getWidth(), layerBase.getHeight(), 32, 16);

            // places the layer culled by the server in the correct position in world 2d array for correct rendering
            for (int row = minI; row <= maxI; row++) {
                for (int col = minJ; col <= maxJ; col++) {
                    final TiledMapTileLayer.Cell cell = layerCut.getCell(col-minJ, row-minI);
                    bigLayer.setCell(col, row, cell);
                }
            }

            layer = bigLayer;
        }

        /** THE FOLLOWING METHOD IS ALREADY OPTIMIZED FOR DRAWING VISIBLE TILES!!! **/

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

        it = 0;
        for (int row = row2; row >= row1; row--) {
            for (int col = col1; col <= col2; col++) {
                float x = (col * halfTileWidth) + (row * halfTileWidth);
                float y = (row * halfTileHeight) - (col * halfTileHeight);
                final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null) continue;
                renderCell(cell, x, y, layerOffsetX, layerOffsetY, layer.getOpacity());
                it++;
            }
        }

        if(GameServer.getInstance().getNumberOfPlayersOnline() > 0) {
            float playerX = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.x;
            float playerY = GameServer.getInstance().loggedIn.entrySet().stream().findFirst().
                    get().getValue().character.get(Component.Character.class).position.y;
//            Vector2 playerPos = new Vector2(playerX, playerY);
//
//            float pX = (int)translateScreenToIso(playerPos).x;
//            float pY = (int)translateScreenToIso(playerPos).y;
//
//            int minI =  MathUtils.ceil(pY - N_ROWS/2);
//            int maxI =  MathUtils.ceil(pY + N_ROWS/2);
//
//            int minJ = MathUtils.ceil(pX - N_COLS/2);
//            int maxJ =  MathUtils.ceil(pX + N_COLS/2);
//
//            // clamp to limits but always send same amount of info
//            if(minI < 0) { maxI -= minI; minI = 0; }
//            if(minJ < 0) { maxJ -= minJ; minJ = 0; }
//            if(maxI > layerBase.getHeight()) {minI += layerBase.getHeight() - maxI ; maxI = layerBase.getHeight(); }
//            if(maxJ > layerBase.getWidth()) {minJ += layerBase.getHeight() - maxJ ; maxJ = layerBase.getWidth(); }
//
//
//            // draw only visible tiles for player (in correct order for isometric rendering)
//            it = 0;
//            for (int row = maxI; row >= minI; row--) {
//                for (int col = minJ; col <= maxJ; col++) {
//                    float x = (col * halfTileWidth) + (row * halfTileWidth);
//                    float y = (row * halfTileHeight) - (col * halfTileHeight);
//                    final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
//                    if (cell == null) continue;
//                    renderCell(cell, x, y, layerOffsetX, layerOffsetY, layer.getOpacity());
//                    it++;
//                }
//            }

            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(playerX, playerY, 0.3f, 0.6f);
            shapeRenderer.end();
        }

        System.out.println("Current fps: "+Gdx.graphics.getFramesPerSecond());
    }

    private Vector3 translateScreenToIso (Vector2 vec) {
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
}
