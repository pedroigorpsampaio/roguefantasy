package com.mygdx.game.ui;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Encoder;

/**
 * A class that encapsulates the option menu window
 */
public class OpenChannelWindow extends GameWindow {

    private Table uiTable;
    private Button closeBtn, openChannelBtn;
    private Table scrollTable;
    private ScrollPane scrollPane;
    private TextField recipientTxt;
    private CheckBox enabledCb;
    protected ButtonGroup<TextButton> bg;

    /**
     * Builds the option window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public OpenChannelWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();
        this.getTitleLabel().setText(langBundle.format("channels"));
        this.getTitleLabel().setAlignment(Align.center);

        TextureAtlas uiAtlas = manager.get("ui/packed_textures/ui.atlas");

        closeBtn = new TextButton("close", skin);
        openChannelBtn = new TextButton("openChannel", skin);
        enabledCb = new CheckBox("Enabled: ", skin);

        uiTable = new Table();

        TextButton worldBtn = new TextButton(langBundle.format("worldChat"), skin);
        worldBtn.setName("worldChat");
        TextButton mapBtn = new TextButton(langBundle.format("mapChat"), skin);
        mapBtn.setName("mapChat");
        TextButton guildBtn = new TextButton(langBundle.format("guildChat"), skin);
        guildBtn.setName("guildChat");
        TextButton partyBtn = new TextButton(langBundle.format("partyChat"), skin);
        partyBtn.setName("partyChat");
        TextButton helpBtn = new TextButton(langBundle.format("helpChat"), skin);
        helpBtn.setName("helpChat");
        worldBtn.getLabel().setAlignment(Align.left);
        mapBtn.getLabel().setAlignment(Align.left);
        guildBtn.getLabel().setAlignment(Align.left);
        partyBtn.getLabel().setAlignment(Align.left);
        helpBtn.getLabel().setAlignment(Align.left);


        int chIdx = GameScreen.getInstance().getChatWindow().searchChannel(ChatWindow.ChatChannel.WORLD, -1);
        if(chIdx == -1) enabledCb.setChecked(false);
        else enabledCb.setChecked(true);

        bg = new ButtonGroup<>();
        bg.setMinCheckCount(1);
        bg.setMaxCheckCount(1);
        bg.setUncheckLast(true);

        bg.add(worldBtn);
        bg.add(mapBtn);
        bg.add(guildBtn);
        bg.add(partyBtn);
        bg.add(helpBtn);

        /**
         * button style
         */
        TextButton.TextButtonStyle newStyle = new TextButton.TextButtonStyle(worldBtn.getStyle());
        Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pxColor.setColor(new Color(0x75757575));
        pxColor.fill();
        newStyle.up = null;
        newStyle.over = new Image(new Texture(pxColor)).getDrawable();
        newStyle.down = null;
        newStyle.checked = newStyle.over;
        newStyle.font.getData().setScale(0.81f, 0.81f);
        pxColor.dispose();

        worldBtn.setStyle(newStyle);
        mapBtn.setStyle(newStyle);
        guildBtn.setStyle(newStyle);
        partyBtn.setStyle(newStyle);
        helpBtn.setStyle(newStyle);

        Label privateChannel = new Label("privateChannel", skin);

        scrollTable = new Table();
        scrollTable.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        scrollTable.align(Align.topLeft);
        scrollTable.defaults().padLeft(10).padTop(0).padRight(10);
        scrollTable.add(worldBtn).left().growX().padTop(6);
        scrollTable.row();
        scrollTable.add(mapBtn).left().fillX();
        scrollTable.row();
        scrollTable.add(helpBtn).left().fillX();
        scrollTable.row();
        scrollTable.add(partyBtn).left().fillX();
        scrollTable.row();
        scrollTable.add(guildBtn).left().fillX().padBottom(1);

        scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsOnTop(true);
        scrollPane.setSmoothScrolling(false);
        scrollPane.setupOverscroll(0,0,0);

        recipientTxt = new TextField("", skin);

        uiTable.add(scrollPane).size(stage.getWidth()/6f, scrollTable.getMinHeight()+4);
        uiTable.row();
        uiTable.add(enabledCb);
        uiTable.row();
        uiTable.add(privateChannel).center();
        uiTable.row();
        uiTable.add(recipientTxt).fillX();
        uiTable.row();
        HorizontalGroup horizontalBtnGroup = new HorizontalGroup();
        horizontalBtnGroup.space(20);
        horizontalBtnGroup.addActor(closeBtn);
        horizontalBtnGroup.addActor(openChannelBtn);
        uiTable.add(horizontalBtnGroup).padTop(10);

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

       // this.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        this.defaults().padTop(7).padBottom(7).padLeft(9).padRight(9);
        this.setMovable(false);

        this.getTitleTable().padBottom(6);
        this.defaults().minWidth(uiTable.getMinWidth()).minHeight(uiTable.getMinHeight());
        this.add(uiTable).expand().bottom().fillX();
        this.pack();
        this.layout();
        this.validate();
        stage.draw();

        new OpenChannelWindowController();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void startServerListening(LoginClient loginClient, Encoder encoder) {

    }

    @Override
    public void stopServerListening() {

    }

    @Override
    public void softKeyboardClosed() {

    }

    @Override
    public void softKeyboardOpened() {

    }

    @Override
    public void reloadLanguage() {
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        this.getTitleLabel().setText(" "+langBundle.format("channels"));
    }

    public boolean isPointOn(float x, float y) {
        Vector2 coordinates = stageToLocalCoordinates(new Vector2(x, y));
        return this.hit(coordinates.x, coordinates.y, true) != null;
    }

    private void showOpenChannelWindow() {
        if(this.getStage() == null)
            stage.addActor(this);
    }

    /**
     * hides the window for opening new channels
     */
    private void hideOpenChannelWindow() {
        if(this.getStage()!=null)
            this.remove();
    }

    /**
     * Interactions controller
     */
    private class OpenChannelWindowController {
        public OpenChannelWindowController() {
            closeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(getStage() == null)
                        showOpenChannelWindow();
                    else
                        hideOpenChannelWindow();
                }
            });

            scrollTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ChatWindow chatWindow = GameScreen.getInstance().getChatWindow();
                    int chIdx = chatWindow.searchChannel(ChatWindow.ChatChannel.fromString(bg.getChecked().getName()), -1);
                    if(chIdx == -1) enabledCb.setChecked(false);
                    else enabledCb.setChecked(true);
                }
            });

            enabledCb.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    if(enabledCb.isChecked()) {

                    }
                }
            });
        }
    }
}
