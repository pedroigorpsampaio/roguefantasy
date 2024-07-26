package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Encoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A class that encapsulates the chat window
 */
public class ChatWindow extends GameWindow {
    private static final int MAX_CHAT_LOG_DISPLAY_SIZE = 30;
    private Table uiTable;
    private List<String> defaultChatLog, worldChatLog, mapChatMap, guildChatLog, partyChatLog, helpChatLog, privateChatLog; // contain logs from each chat log
    private TextButton defaultChatTabBtn, worldChatTabBtn, mapChatTabBtn, guildChatTabBtn, partyChatTabBtn, helpChatTabBtn, privateChatTabBtn; // chat tab buttons
    private ChatChannel currentChannel; // current chat channel
    private TextButton sendBtn;
    private TextField msgField; // message field
    private Label chatHistoryLabel;
    private ScrollPane chatScrollPane;
    private Rectangle hitBox;


    /**
     * Builds the chat window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the chat window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the chat window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public ChatWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);

        // initialize lists of chat logs
        defaultChatLog = Collections.synchronizedList(new ArrayList<>());
        worldChatLog = Collections.synchronizedList(new ArrayList<>());
        mapChatMap = Collections.synchronizedList(new ArrayList<>());
        guildChatLog = Collections.synchronizedList(new ArrayList<>());
        partyChatLog = Collections.synchronizedList(new ArrayList<>());
        helpChatLog = Collections.synchronizedList(new ArrayList<>());
        privateChatLog = Collections.synchronizedList(new ArrayList<>());
        // chat hit box for hover detection
        hitBox = new Rectangle();
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        currentChannel = ChatChannel.DEFAULT; // starts at default tab always

        // makes sure language is up to date with current selected chat
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // makes sure title is in the correct language
        //this.getTitleLabel().setText(" "+langBundle.format("chat"));

        // log label
        chatHistoryLabel = new Label("", skin, "newLabelStyle");
        chatHistoryLabel.setAlignment(Align.bottomLeft);
        chatHistoryLabel.setColor(0.969f, 0.957f, 0.982f, 1.0f);
        //chatHistoryLabel.setFontScale(0.95f);
        chatHistoryLabel.setWrap(true);

        // text input textfield
        msgField = new TextField("", skin);
        msgField.setMessageText(langBundle.format("chatTipMessage"));

        // send button
        sendBtn = new TextButton(langBundle.format("send"), skin);

        // ui table with ui actors
        uiTable = new Table(skin);
        uiTable.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        uiTable.defaults().spaceBottom(0).padRight(5).padLeft(5).minWidth(320);
        //uiTable.add(titleLabel).center().colspan(2).padBottom(10).padTop(10);
        uiTable.row();
        // scrollable log table
        Table scrollTable = new Table();
        scrollTable.add(chatHistoryLabel).grow().padTop(5).padLeft(5);
        scrollTable.row();
        scrollTable.pack();
        // uses scrollpane to make table scrollable
        chatScrollPane = new ScrollPane(scrollTable, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollbarsOnTop(true);
        chatScrollPane.setSmoothScrolling(true);
        chatScrollPane.setupOverscroll(0,0,0);
        //chatScrollPane.setColor(Color.YELLOW);

        uiTable.add(chatScrollPane).colspan(2).size(GameScreen.getStage().getWidth()*0.5f, GameScreen.getStage().getHeight()*0.2f).center();
        uiTable.row();
        uiTable.add(msgField).padTop(10).left().colspan(1).grow().height(30);
        uiTable.add(sendBtn).padTop(10).colspan(1).width(152).height(32);
        uiTable.pack();

        Pixmap labelColor = new Pixmap(1, 1,Pixmap.Format.RGBA8888);
        labelColor.setColor(new Color(0.1f, 0.1f, 0.1f, 0.2f));
        labelColor.fill();
        this.setBackground(new Image(new Texture(labelColor)).getDrawable());
        labelColor.dispose();

        // builds chat window
//        this.getTitleTable().padBottom(6);
//        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
//        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
//        this.getTitleTable().padBottom(6);
//        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320).padTop(21);

        this.setMovable(false);
        this.add(uiTable);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new ChatController();

        // create welcome message
        sendMessage("Server", ChatChannel.DEFAULT, langBundle.get("welcomeChatMessage"), -1);
    }

    /**
     * Logs message to the chat list keeping it within the max set size
     * To do so it keeps only the MAX_SIZE new message inputs
     * @param chatLog	the chat log that the log will be added
     * @param message		the new message to be added to the chat log
     */
    private void storeMessage(List<String> chatLog, String message) {
        chatLog.add(message); // adds to the chat log list
        if(chatLog.size() > MAX_CHAT_LOG_DISPLAY_SIZE) // keep new elements only, within max size
            chatLog.remove(0);
    }


    /**
     * Updates the chat label text
     * @param channel	the channel to update chat list
     * @param recipientId in case its a private message channel, the id of the recipient to load private messages
     */
    private void updateChatLabel(ChatChannel channel, int recipientId) {
        List<String> list = chatLogFromChannel(channel, recipientId); // gets chat from tab
        StringBuilder stringBuilder = new StringBuilder(); // str builder that will build chat text
        // chat list may be accessed concurrently by chat client thread when other clients send messages
        synchronized(list) {
            Iterator i = list.iterator(); // Must be in synchronized block
            while (i.hasNext())
                stringBuilder.append(i.next()).append("\n");
        }

        chatHistoryLabel.setText(stringBuilder);
    }

    /**
     * Returns the chat list that corresponds to the tab received in parameter
     * @param channel	the tab to get the chat list from
     * @param recipientId in case its a private message channel, the id of the recipient to load private messages
     * @return		the chat list that corresponds to the tab
     */
    private List<String> chatLogFromChannel(ChatChannel channel, int recipientId) {
        List<String> list = defaultChatLog;

        switch (channel) {
            case DEFAULT:
                list = defaultChatLog;
                break;
            case WORLD:
                list = worldChatLog;
                break;
            case MAP:
                list = mapChatMap;
                break;
            case GUILD:
                list = guildChatLog;
                break;
            case PARTY:
                list = partyChatLog;
                break;
            case HELP:
                list = helpChatLog;
                break;
            case PRIVATE: // TODO: MUST FILTER RECIPIENT ID
            default:
                list = defaultChatLog;
                break;
        }

        return list;
    }

    /** only scrolls to the end if bar is at the end,
        so automatic scroll will happen only on new log
        and when the bar is all the way down
     **/
    public void scrollToEnd() {
        if(chatScrollPane.isBottomEdge()) {
            chatScrollPane.layout();
            chatScrollPane.scrollTo(0, 0, 0, 0);
        }
    }

    /**
     * Sends a message to the desired channel
     * @param sender        the sender of the message
     * @param channel       the channel to send the message to
     * @param message       the message to be sent
     * @param recipientId   in case its a private message, the recipient id
     */
    public void sendMessage(String sender, ChatChannel channel, String message, int recipientId) {
        if(message == "") return; // returns if there is no message

        StringBuilder sb = new StringBuilder(sender);
        sb.append(": ");
        sb.append(message);

        storeMessage(chatLogFromChannel(channel, recipientId), String.valueOf(sb)); // stores message in desired channel

        if(channel == currentChannel) { // update chat label if its current channel
            updateChatLabel(channel, recipientId);
            scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)
        }
    }

    /**
     * Sends the message in the message field to the current channel
     */
    public void sendMessage() {
        if(msgField.getText() == "") {
            clearMessageField(false); // lose focus and return if there is no message
            return;
        }

        StringBuilder sb = new StringBuilder(GameClient.getInstance().getClientCharacter().name);
        sb.append(": ");
        sb.append(msgField.getText());

        storeMessage(chatLogFromChannel(currentChannel, -1), String.valueOf(sb)); // stores message in current channel
        updateChatLabel(currentChannel, -1);// update chat label since its current channel
        scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)

        clearMessageField(false); // we can clear message field since we know message was sent from client
    }

    public boolean hasFocus() {
        return msgField.hasKeyboardFocus();
    }

    public void setScrollFocus() {
        stage.setScrollFocus(chatScrollPane);
    }

    public void removeScrollFocus() {
        stage.setScrollFocus(null);
    }

    public void setFocus() {
        stage.setKeyboardFocus(msgField);
    }

    /**
     * Clears message field keeping focus if desired
     * @param keepFocus if message field should keep keyboard focus
     */
    public void clearMessageField(boolean keepFocus) {
        msgField.setText("");

        if(!keepFocus)
            stage.setKeyboardFocus(null);
    }

    @Override
    public void resize(int width, int height) {
        updateHitBox();
    }

    @Override
    public void startServerListening(LoginClient loginClient, Encoder encoder) {

    }

    @Override
    public void stopServerListening() {

    }

    public boolean isPointOn(float x, float y) {
        return hitBox.contains(x, y);
    }

    public void updateHitBox() {
        hitBox.set(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    /**
     * Chat channels
     */
    public enum ChatChannel {
        DEFAULT("default"),
        WORLD("world"),
        MAP("map"),
        GUILD("guild"),
        PARTY("party"),
        HELP("help"),
        PRIVATE("private"),
        UNKNOWN("unknown");

        private String text;

        ChatChannel(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static ChatChannel fromString(String text) throws Exception{
            for (ChatChannel t : ChatChannel.values()) {
                if (t.text.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            throw new Exception("No enum constant with text " + text + " found");
        }
    }

    /**
     * A nested class that controls the chat window
     */
    class ChatController {

        // constructor adds listeners to the actors
        public ChatController() {
//            langBox.addListener(new ChangeListener() {
//                public void changed(ChangeEvent event, Actor actor) {
//                    changeLanguage(langBox.getSelectedIndex());
//                }
//            });
            sendBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(!msgField.getText().equals("")) {
                        sendMessage(GameClient.getInstance().getClientCharacter().name,
                                currentChannel, msgField.getText(), -1);
                        clearMessageField(false); // clear message field
                    }
                }
            });

        }

        // called when back button is pressed
        private void backBtnOnClick(InputEvent event, float x, float y) {
            remove(); // removes chat window
        }
    }

    private ChatWindow getInstance() {
        return this;
    }
}
