package com.mygdx.game.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.DispatchServer;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;

/**
 * A class that encapsulates the option menu window
 */
public class ContactWindow extends GameWindow implements PropertyChangeListener {

    private Table uiTable;
    private TextButton closeBtn;
    private Table scrollTable;
    private ScrollPane scrollPane;
    private TextField friendNameInputTxt;
    private boolean isUpdate = false;
    private float txtFieldOffsetY = 0;
    private float originalY;

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
    public ContactWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        //this.clear();
        this.clearChildren();
        this.remove();
        this.getTitleLabel().setText(langBundle.format("contacts"));
        this.getTitleLabel().setAlignment(Align.center);

        TextureAtlas uiAtlas = manager.get("ui/packed_textures/ui.atlas");

        closeBtn = new TextButton(langBundle.get("back"), skin);

        uiTable = new Table();

        TypingLabel testLabel = new TypingLabel(langBundle.format("worldChat"), skin);
        testLabel.setName("worldChat");
        testLabel.setAlignment(Align.left);
        testLabel.skipToTheEnd();

        scrollTable = new Table();
        scrollTable.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        scrollTable.align(Align.topLeft);
        scrollTable.defaults().padLeft(10).padTop(0).padRight(10);
        scrollTable.add(testLabel).left().growX().padTop(6);
        scrollTable.row();

        scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsOnTop(true);
        scrollPane.setSmoothScrolling(false);
        scrollPane.setupOverscroll(0,0,0);

        friendNameInputTxt = new TextField("", skin);

        uiTable.add(scrollPane).size(stage.getWidth()/6f, scrollTable.getMinHeight()+4);
        uiTable.row();
        HorizontalGroup horizontalBtnGroup = new HorizontalGroup();
        horizontalBtnGroup.space(20);
        horizontalBtnGroup.addActor(closeBtn);
        uiTable.add(horizontalBtnGroup).padTop(10);

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

       // this.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        this.defaults().padTop(7).padBottom(7).padLeft(9).padRight(9);
        this.setMovable(true);

        this.getTitleTable().padBottom(6);
        this.defaults().minWidth(uiTable.getMinWidth()).minHeight(uiTable.getMinHeight());
        this.add(uiTable).expand().bottom().fillX();
        this.pack();
        this.layout();
        this.validate();
        stage.draw();

        new ContactWindowController();

        startServerListening(GameClient.getInstance());
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void startServerListening(DispatchServer client) {
        this.listeningClient = client;
        // if its not listening to id by name responses, start listening to it
        if(!client.isListening("idByNameRetrieved", this))
            client.addListener("idByNameRetrieved", this);
    }

    @Override
    public void stopServerListening() {
        if(listeningClient.isListening("idByNameRetrieved", this))
            listeningClient.removeListener("idByNameRetrieved", this);
    }

    @Override
    public void softKeyboardClosed() {
        this.setY(originalY);
    }

    @Override
    public void softKeyboardOpened() {
        originalY = getY();
        float deltaY = txtFieldOffsetY - RogueFantasy.getKeyboardHeight();
        if(deltaY < 0) // keyboard obfuscates
            moveBy(0, -deltaY);
    }

    @Override
    public void reloadLanguage() {
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        this.getTitleLabel().setText(" "+langBundle.format("contacts"));
        closeBtn.setText(langBundle.get("back"));
    }

    public boolean isPointOn(float x, float y) {
        Vector2 coordinates = stageToLocalCoordinates(new Vector2(x, y));
        return this.hit(coordinates.x, coordinates.y, true) != null;
    }

    /**
     * Toggle window (as a result of back button, will close it)
     */
    private void toggleWindow() {
        GameScreen.getInstance().toggleWindow(this, false, false);
    }


    public void reset() {
        friendNameInputTxt.setText("");
    }

    public boolean hasFocus() {
        return friendNameInputTxt.hasKeyboardFocus();
    }

    public static boolean isValidName(String name) {
        Pattern regex = Pattern.compile("^[a-zA-Z0-9]+( [a-zA-Z0-9]+)*$");
        if (regex.matcher(name).find())
            return true;
        return false;
    }

    private boolean isFriendNameTextValid() {
        String text = friendNameInputTxt.getText();

        if(friendNameInputTxt.getText().trim().length() == 0) { // empty text, return
            GameScreen.getInstance().showInfo("invalidLength"); // show invalid length message
            return false;
        }

        int minLength = prefs.getInteger("defaultMinNameSize", 2);
        int maxLength = prefs.getInteger("defaultMaxNameSize", 26);
        boolean isFit = text.length() <= maxLength ? text.length() >= minLength ? true : false : false;

        if(!isFit) {
            GameScreen.getInstance().showInfo("invalidLength"); // show invalid length message
            return false;
        }

        if(!isValidName(text)) {
            GameScreen.getInstance().showInfo("invalidName"); // show invalid name message
            return false;
        }

        return true;
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        Gdx.app.postRunnable(() -> {
            if(propertyChangeEvent.getPropertyName().equals("idByNameRetrieved")) { // received a if by name response
                GameRegister.CharacterIdRequest response = (GameRegister.CharacterIdRequest) propertyChangeEvent.getNewValue();
                if(response.requester.equals("OpenChannelWindow")) { // this is a response to this module, act
                    if(response.id == -1) { // player not found - does not exist
                        GameScreen.getInstance().showInfo("playerDoesNotExist"); // show invalid length message
                        return;
                    }
                }
            }
        });
    }

    /**
     * Interactions controller
     */
    private class ContactWindowController {
        public ContactWindowController() {
            closeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    toggleWindow();
                }
            });

            friendNameInputTxt.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if(Gdx.app.getType() == Application.ApplicationType.Android && !RogueFantasy.isKeyboardShowing())
                        txtFieldOffsetY = friendNameInputTxt.getY();
                    return false;
                }
            });
        }
    }
}
