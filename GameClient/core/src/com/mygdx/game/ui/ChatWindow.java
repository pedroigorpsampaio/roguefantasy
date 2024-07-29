package com.mygdx.game.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Common;
import com.mygdx.game.util.Encoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that encapsulates the chat window
 */
public class ChatWindow extends GameWindow {
    private static final int MAX_CHAT_LOG_DISPLAY_SIZE = 50;
    private static final int MAX_CHAT_MSG_CHARACTERS = 255;
    public static final Color DEFAULT_CHAT_MESSAGE_COLOR = new Color(0.95f, 0.99f, 0.75f, 1f);
    public static final Color SERVER_CHAT_MESSAGE_COLOR = new Color(0.77f, 0.77f, 1f, 1f);
    public static final Color DEBUG_CHAT_MESSAGE_COLOR = new Color(0.97f, 0.67f, 0.3f, 1f);
    private static final float CHAT_WIDTH = GameScreen.getStage().getWidth()*0.5f;
    private Table uiTable;
    private List<ChatMessage> defaultChatLog, worldChatLog, mapChatMap, guildChatLog, partyChatLog, helpChatLog, privateChatLog; // contain logs from each chat log
    private TextButton defaultChatTabBtn, worldChatTabBtn, mapChatTabBtn, guildChatTabBtn, partyChatTabBtn, helpChatTabBtn, privateChatTabBtn; // chat tab buttons
    private ChatChannel currentChannel; // current chat channel
    private int currentRecipientId; // current recipient Id (for private chats)
    private TextButton sendBtn;
    private TextField msgField; // message field
    private final Label chatHistoryLabel, tmpLabel;
    private ScrollPane chatScrollPane;
    private Rectangle hitBox;
    private Table scrollTable;

    public static class ChatMessage {
        String sender; // the sender name
        String message; // the message content without color tag or sender
        int senderId, recipientId; // sender -1 == server ; recipient -1 == no recipient
        Color color; // color to display this message
        long timestamp; // time stamp of message
        String langKey = null; // in case is a translatable msg from server
        float height; // the height this message occupies in label (for click detection)
    }


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
        chatHistoryLabel = new Label("", skin, "chatLabelStyle");
        tmpLabel = new Label("", skin, "chatLabelStyle");
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        currentChannel = ChatChannel.DEFAULT; // starts at default tab always
        currentRecipientId = -1; // no recipient in default chat

        // makes sure language is up to date with current selected chat
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // makes sure title is in the correct language
        //this.getTitleLabel().setText(" "+langBundle.format("chat"));

        // chat log label
        chatHistoryLabel.setAlignment(Align.bottomLeft);
        chatHistoryLabel.setColor(0.969f, 0.957f, 0.982f, 1.0f);
        //chatHistoryLabel.setFontScale(0.95f);
        chatHistoryLabel.setWrap(true);

        // text input textfield
        msgField = new TextField("", skin);
        msgField.setMaxLength(MAX_CHAT_MSG_CHARACTERS);
        msgField.setColor(new Color(0.22f,0.4f,0.32f,1f));
        TextField.TextFieldStyle newStyle = new TextField.TextFieldStyle(msgField.getStyle());
        Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pxColor.setColor( new Color(0.35f, 0.29f, 0.35f, 1f));
        pxColor.fill();
        newStyle.focusedBackground = new Image(new Texture(pxColor)).getDrawable();
        pxColor = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pxColor.setColor( new Color(0.35f, 0.29f, 0.35f, 0.4f));
        pxColor.fill();
        newStyle.background = new Image(new Texture(pxColor)).getDrawable();
        pxColor.dispose();
        newStyle.background.setLeftWidth(8f);
        newStyle.focusedBackground.setLeftWidth(8f);
        newStyle.focusedFontColor = DEFAULT_CHAT_MESSAGE_COLOR;
        newStyle.messageFontColor = new Color(0.75f, 0.79f, 0.55f, 1f);
        msgField.setStyle(newStyle);
        //msgField.setMessageText(langBundle.format("chatTipMessage"));

        // send button
        sendBtn = new TextButton(langBundle.format("send"), skin);
        sendBtn.setColor(Color.FOREST);

        // ui table with ui actors
        uiTable = new Table(skin);
        //uiTable.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        //uiTable.defaults().spaceTop(0).padRight(0).padLeft(5).minWidth(320);
        //uiTable.add(titleLabel).center().colspan(2).padBottom(10).padTop(10);

        // scrollable log table
        scrollTable = new Table();
        scrollTable.align(Align.bottomLeft);
        scrollTable.defaults().spaceTop(0).spaceBottom(0).padTop(-4).padBottom(-4);
        scrollTable.padBottom(5);
        //scrollTable.add(chatHistoryLabel).grow().padRight(20).padLeft(5);
        scrollTable.pack();
        // uses scrollpane to make table scrollable
        chatScrollPane = new ScrollPane(scrollTable, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollbarsOnTop(true);
        chatScrollPane.setSmoothScrolling(true);
        chatScrollPane.setupOverscroll(0,0,0);
        //chatScrollPane.setColor(Color.YELLOW);

        uiTable.add(chatScrollPane).colspan(2).size(CHAT_WIDTH, GameScreen.getStage().getHeight()*0.22f);
        uiTable.row();
        uiTable.add(msgField).padBottom(5).left().colspan(1).growX().height(36);
        uiTable.add(sendBtn).padBottom(5).colspan(1).width(144).height(37);
        uiTable.pack();

        Pixmap labelColor = new Pixmap(1, 1,Pixmap.Format.RGBA8888);
        labelColor.setColor(new Color(0.1f, 0.1f, 0.1f, 0.4f));
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
        sendMessage("[Server]", -1, ChatChannel.DEFAULT, langBundle.get("welcomeChatMessage"), "welcomeChatMessage", SERVER_CHAT_MESSAGE_COLOR, -1, false);
    }

    /**
     * Logs message to the chat list keeping it within the max set size
     * To do so it keeps only the MAX_SIZE new message inputs
     * @param chatLog	the chat log that the log will be added
     * @param message		the new message to be added to the chat log
     */
    private void storeMessage(List<ChatMessage> chatLog, ChatMessage message) {
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
        List<ChatMessage> list = chatLogFromChannel(channel, recipientId); // gets chat from tab
        StringBuilder stringBuilder = new StringBuilder(); // str builder that will build chat text
        // chat list may be accessed concurrently by chat client thread when other clients send messages
        scrollTable.clear();
        synchronized(list) {
            int count=0;
            Iterator i = list.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                ChatMessage msg = (ChatMessage) i.next();
                stringBuilder.append("[#");
                stringBuilder.append(msg.color);
                stringBuilder.append("]");
                stringBuilder.append(Common.getTimeTag(msg.timestamp));
                stringBuilder.append(" ");
                stringBuilder.append(msg.sender);
                stringBuilder.append(": ");
                if(msg.langKey!=null) // get translated msg if its a translatable server msg
                    stringBuilder.append(langBundle.format(msg.langKey));
                else
                    stringBuilder.append(msg.message);
                stringBuilder.append("[]");
                //if(i.hasNext())
                    //stringBuilder.append("\n");

                Label l = new Label(stringBuilder, skin, "chatLabelStyle");
                l.setAlignment(Align.bottomLeft);
                //chatHistoryLabel.setFontScale(0.95f);
                l.setWrap(true);

                // right click to context menu for desktop
                if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    l.addListener(new ClickListener(Input.Buttons.RIGHT) {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            openContextMenu(msg);
                        }
                    });
                }

                scrollTable.add(l).growX().padRight(20).padLeft(5);
                if(i.hasNext())
                    scrollTable.row();
                stringBuilder.setLength(0);
                count++;
            }
        }


        //chatHistoryLabel.setText(stringBuilder);
    }

    private void openContextMenu(ChatMessage msg) {
        GameScreen.getInstance().showContextMenu(null);
    }

    /**
     * Returns the chat list that corresponds to the tab received in parameter
     * @param channel	the tab to get the chat list from
     * @param recipientId in case its a private message channel, the id of the recipient to load private messages
     * @return		the chat list that corresponds to the tab
     */
    private List<ChatMessage> chatLogFromChannel(ChatChannel channel, int recipientId) {
        List<ChatMessage> list = defaultChatLog;

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
        //Gdx.app.postRunnable(() -> {
            if (chatScrollPane.isBottomEdge()) {
                chatScrollPane.layout();
                chatScrollPane.scrollTo(0, 0, 0, 0);
            }
        //});
    }

    /**
     * Sends a message to the desired channel
     * @param sender        the name of the sender of the message
     * @param senderId      the sender id of the message
     * @param channel       the channel to send the message to
     * @param message       the message to be sent
     * @param langKey       if its a translatable server message, the key in the lang bundle (this param is null if its not translatable)
     * @param color         the color of the message
     * @param recipientId   in case its a private message, the recipient id
     * @param sendToServer  if this is true, will send this message to server for other clients
     */
    public void sendMessage(String sender, int senderId, ChatChannel channel, String message, String langKey, Color color, int recipientId, boolean sendToServer) {
        if(message == "" || message.trim().length() == 0) return; // returns if there is no message

        Gdx.app.postRunnable(() -> {
            ChatMessage msg = new ChatMessage();
            msg.sender = sender;
            msg.senderId = senderId;
            if(message.length() > MAX_CHAT_MSG_CHARACTERS)
                msg.message = message.substring(0, MAX_CHAT_MSG_CHARACTERS-1);
            else
                msg.message = message;
            msg.color = color;
            msg.langKey = langKey;
            msg.recipientId = recipientId;
            msg.timestamp = System.currentTimeMillis();
            msg.height = calculateMsgHeight(msg);

            storeMessage(chatLogFromChannel(channel, recipientId), msg); // stores message in desired channel

            if (channel == currentChannel) { // update chat label if its current channel
                updateChatLabel(channel, recipientId);
                scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)
            }

            if(sendToServer) {
                //TODO SEND TO SERVER
            }
        });
    }

    /**
     * Sends the message in the message field to the current channel with default color
     */
    public void sendMessage() {
        Gdx.app.postRunnable(() -> {
            if(msgField.getText() == "" || msgField.getText().trim().length() == 0) {
                clearMessageField(false); // lose focus and return if there is no message
                return;
            }

            Entity.Character senderChar = GameClient.getInstance().getClientCharacter();
            String message = msgField.getText();

            ChatMessage msg = new ChatMessage();
            msg.sender = senderChar.name;
            msg.senderId = senderChar.id;
            if(message.length() > MAX_CHAT_MSG_CHARACTERS)
                msg.message = message.substring(0, MAX_CHAT_MSG_CHARACTERS-1);
            else
                msg.message = message;
            msg.color = DEFAULT_CHAT_MESSAGE_COLOR;
            msg.recipientId = -1;
            msg.langKey = null;
            msg.timestamp = System.currentTimeMillis();
            msg.height = calculateMsgHeight(msg);

            storeMessage(chatLogFromChannel(currentChannel, msg.recipientId), msg); // stores message in current channel
            updateChatLabel(currentChannel, msg.recipientId);// update chat label since its current channel
            scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)

            clearMessageField(false); // we can clear message field since we know message was sent from client

            //TODO SEND TO SERVER
        });
    }

    private float calculateMsgHeight(ChatMessage msg) {
        // simulate msg
        StringBuilder sb = new StringBuilder(Common.getTimeTag(msg.timestamp));
        sb.append(" ");
        sb.append(msg.sender);
        sb.append(" ");
        sb.append(msg.message);
        tmpLabel.setText(String.valueOf(sb));
        tmpLabel.setWidth(CHAT_WIDTH - 21*(1080/720f));
        tmpLabel.setWrap(true);
        tmpLabel.layout();
        GlyphLayout layout = tmpLabel.getGlyphLayout();
        float height = layout.height * 1.46f;
        System.out.println(height);
        return height;
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


    /**
     * Reload interface language and server messages language
     */
    public void reloadLanguage() {
        langBundle = manager.get("lang/langbundle", I18NBundle.class);
        msgField.setMessageText(langBundle.format("chatTipMessage"));
        sendBtn.setText(langBundle.format("send"));
        updateChatLabel(currentChannel, -1);
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

    @Override
    public void softKeyboardClosed() {

    }

    @Override
    public void softKeyboardOpened() {

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
                        sendMessage(GameClient.getInstance().getClientCharacter().name, GameClient.getInstance().getClientCharacter().id,
                                currentChannel, msgField.getText(), null, DEFAULT_CHAT_MESSAGE_COLOR, -1, true);
                        clearMessageField(false); // clear message field
                    }
                }
            });

            chatHistoryLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    float delta = 1 - chatScrollPane.getScrollPercentY();
                    ChatMessage msg = getChatMessagedByY(y + delta);
                    if(msg != null)
                        System.out.println(msg.message);
                }
           });
        }

        // called when back button is pressed
        private void backBtnOnClick(InputEvent event, float x, float y) {
            remove(); // removes chat window
        }
    }

    private ChatMessage getChatMessagedByY(float y) {
        List<ChatMessage> list = chatLogFromChannel(currentChannel, currentRecipientId); // gets chat from current channel
        // chat list may be accessed concurrently by chat client thread when other clients send messages
        synchronized(list) {
            ListIterator i = list.listIterator(list.size());
            int acc = 8; // initial offset
            while (i.hasPrevious()) {
                ChatMessage msg = (ChatMessage) i.previous();
                acc += msg.height;

                if(y < acc) {
                    return msg;
                }
            }
        }
        return null;
    }

    private ChatWindow getInstance() {
        return this;
    }
}
