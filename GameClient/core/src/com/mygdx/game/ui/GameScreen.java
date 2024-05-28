package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GameScreen implements Screen {
    private static GameClient gameClient;
    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Stage bgStage;
    private AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private RegisterWindow registerWindow;
    private I18NBundle langBundle;
    private Texture bgTexture;
    private Image bg;
    private SpriteBatch batch;

    public GameScreen(RogueFantasy game, AssetManager manager, GameClient gameClient) {
        this.game = game;
        this.manager = manager;
        this.gameClient = gameClient;

        batch = new SpriteBatch();

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // gets font from asset manager
        // BitmapFont fontMedium = manager.get("fonts/immortalMedium.ttf", BitmapFont.class);

        // get and play music
        bgm = manager.get("bgm/time_commando.mp3", Music.class);
        bgm.setLooping(true);
        bgm.setVolume(prefs.getFloat("bgmVolume", 1.0f));
        bgm.play();

        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        //skin.add("fontMedium", fontMedium, BitmapFont.class);

        stage = new Stage(new ScreenViewport());
        //Gdx.input.setInputProcessor(stage);

        fpsLabel = new Label("fps:", skin, "fontMedium", Color.WHITE);
        fpsLabel.setAlignment(Align.center);

        stage.addActor(fpsLabel);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());
        Pixmap pixmap = new Pixmap(22, 22, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fillCircle(0, 0, 42);
        Texture texture = new Texture(pixmap);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        // render

      //  Gdx.app.postRunnable(() -> {
        HashMap<Integer, GameRegister.Character> onlineChars = gameClient.getOnlineCharacters();
        synchronized(onlineChars) {
            Iterator<Map.Entry<Integer, GameRegister.Character>> i = onlineChars.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Integer, GameRegister.Character> entry = i.next();
                GameRegister.Character c = entry.getValue();
                batch.begin();
                batch.draw(texture, c.x, c.y);
                batch.end();
            }
        }

        //});

        pixmap.dispose();
        texture.dispose();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
