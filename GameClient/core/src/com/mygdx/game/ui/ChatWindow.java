package com.mygdx.game.ui;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.mygdx.game.network.ChatRegister.MESSAGE_REGISTRY_CHANNEL_COOLDOWN;
import static com.mygdx.game.ui.CommonUI.MAX_LINE_CHARACTERS_FLOATING_TEXT;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
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
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.Timer;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.network.ChatClient;
import com.mygdx.game.network.ChatRegister;
import com.mygdx.game.network.DispatchServer;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.util.Common;
import com.mygdx.game.network.ChatRegister.ChatChannel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

/**
 * A class that encapsulates the chat window
 */
public class ChatWindow extends GameWindow implements PropertyChangeListener {
    private static final int MAX_CHAT_LOG_DISPLAY_SIZE = 15;
    public static final int MAX_CHAT_MSG_CHARACTERS = 255;
    public static final Color DEFAULT_CHAT_SENDER_COLOR = new Color(0.95f, 0.99f, 0.75f, 1f);
    public static final Color DEFAULT_CHAT_MESSAGE_COLOR = new Color(0.75f, 0.99f, 0.65f, 1f);
    public static final Color SERVER_CHAT_MESSAGE_COLOR = new Color(0.77f, 0.77f, 1f, 1f);
    public static final Color DEBUG_CHAT_MESSAGE_COLOR = new Color(0.97f, 0.67f, 0.3f, 1f);
    public static final Color PRIVATE_CHAT_SENDER_COLOR =  new Color(0.76f, 0.65f, 1.0f, 1.0f);
    public static final Color PRIVATE_CHAT_MESSAGE_COLOR =  new Color(0.4f, 0.9f, 0.8f, 1.0f);
    public static final Color LOOK_MESSAGE_COLOR = new Color(0.2f, 0.9f, 0.2f, 1.0f);
    public static final Color TAB_COLOR_SELECTED = new Color(0.4f, 0.9f, 0.8f, 1.0f);
    public static final Color TAB_COLOR_UNSELECTED = new Color(0.2f, 0.4f, 0.1f, 0.7f);
    private static final float CHAT_WIDTH = GameScreen.getStage().getWidth()*0.5f;
    private static final float CHAT_HEIGHT = GameScreen.getStage().getHeight()*0.1805f;
    private static final float ICON_HORIZONTAL_PAD = 1;
    private static final Color ICON_TINT_COLOR = new Color(0.85f, 0.85f, 0.85f, 0.6f);
    private static final Color PANE_BG_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.4f);
    private final TextureAtlas uiAtlas;
    private final Button leftChannelBtn, rightChannelBtn, closeChannelBtn, openChannelBtn;
    private Table uiTable;
    //private List<ChatMessage> defaultChatLog, worldChatLog, mapChatMap, guildChatLog, partyChatLog, helpChatLog; // contain logs from each chat log
    private List<Channel> channels; // list of chat channels (of ServerChannel types)
    private TextButton defaultChatTabBtn, worldChatTabBtn, mapChatTabBtn, guildChatTabBtn, partyChatTabBtn, helpChatTabBtn, privateChatTabBtn; // chat tab buttons
    private Channel currentChannel; // current chat channel
    private TextButton sendBtn;
    private TextField msgField; // message field
    private final Label tmpLabel;
    private ScrollPane chatScrollPane;
    private Rectangle hitBox;
    private Table scrollTable;
    private Table channelTabs;
    private ButtonGroup<TextButton> tabsButtonGroup;
    private HorizontalGroup tabHorizontalBtns;
    private ScrollPane tabsScrollPane;
    private int currentChannelIdx = -1;
    private OpenChannelWindow openChannelWindow; // open chat channel ui

    public float getTabHeight() {
        if(tabHorizontalBtns.hasChildren())
            return tabHorizontalBtns.getChild(0).getHeight();
        else
            return 0;
    }

    public ButtonGroup<TextButton> getTabsButtonGroup() {return tabsButtonGroup;}

    /**
     * Navigates through opened channel tabs
     * @param reverse   if the direction of navitagion should be reversed (to the left, instead)
     */
    public void navigateTab(boolean reverse) {
        int nextIdx;
        if(!reverse)
            nextIdx = (currentChannelIdx + 1) % channels.size();
        else
            nextIdx = (currentChannelIdx +  channels.size() - 1) % channels.size();

        channels.get(nextIdx).btn.setChecked(true); // sets button as checked, and checked callback will do the rest
    }

    public OpenChannelWindow getOpenChannelWindow() {
        return openChannelWindow;
    }

    /**
     * Called when receiving messages from chat server
     */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        Gdx.app.postRunnable(() -> {
            if(propertyChangeEvent.getPropertyName().equals("messageReceived")) { // received a message
                ChatRegister.Message message = (ChatRegister.Message) propertyChangeEvent.getNewValue();

                // gets channel index
                int chIdx = searchChannel(message.channel, message.senderId);

                // in case msg is on a channel different to current channel, update has a new msg bool
                if(chIdx != -1 && currentChannelIdx != chIdx) {
                    channels.get(chIdx).hasNewMessage = true;
                    channels.get(chIdx).startBlinking();
                }

                //System.out.println("New msg: " +message.channel + " / " + message );

                switch(message.channel) {
                    case PRIVATE:
                        receivePrivateMessage(message, chIdx);
                        break;
                    case HELP:
                    case TRADE:
                    case WORLD:
                    case MAP:
                        receiveChannelMessage(message, chIdx);
                        break;
                    case DEFAULT:
                        receiveDefaultMessage(message);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Deals with normal channel messages (excluding default channel)
     * @param message   the message received
     * @param chIdx   the index of the channel if its opened, -1 if its not opened
     */
    private void receiveChannelMessage(ChatRegister.Message message, int chIdx) {
        // first check if channel is opened (if not, ignore message)
        if(chIdx == -1) return;
        // else, send message to the intended channel
        sendMessage(message.sender, message.senderId, message.channel,
                message.message, null, DEFAULT_CHAT_MESSAGE_COLOR,
                -1, false);
    }

    /**
     * Deals with default messages received (from AoI players)
     * @param message   the message received
     */
    private void receiveDefaultMessage(ChatRegister.Message message) {
        sendMessage(message.sender, message.senderId, ChatRegister.ChatChannel.DEFAULT,
                message.message, null, DEFAULT_CHAT_MESSAGE_COLOR,
                -1, false);
    }

    /**
     * Deals with private messages received
     *
     * @param message the message received
     * @param chIdx   the index of the private channel if its opened
     */
    private void receivePrivateMessage(ChatRegister.Message message, int chIdx) {
        boolean openNewTab = prefs.getBoolean("openChatOnPrivateMsg", false);

        // if is ignoring the sender of the message, return (server should not send ignored player messages, but in any case block it here as well)
        if(ChatClient.ignoreList.containsKey(message.senderId)) return;

        if(chIdx == -1) {
            if(openNewTab) { // open new chat is enabled, open it and send msg to the newly open chat
                int idx = createChannel(ChatChannel.PRIVATE, message.senderId, message.sender, false);
                channels.get(idx).hasNewMessage = true;
                channels.get(idx).startBlinking();
            } else { // send message to default channel if this chat is not open at the moment and open new chat option is disabled
                sendMessage(message.sender, message.senderId, ChatRegister.ChatChannel.DEFAULT,
                        message.message, null, PRIVATE_CHAT_MESSAGE_COLOR,
                        message.senderId, false);
            }
        }

        if(chIdx != -1 || openNewTab){ // channel is opened, send msg in channel
            sendMessage(message.sender, message.senderId, ChatRegister.ChatChannel.PRIVATE,
                    message.message, null, PRIVATE_CHAT_MESSAGE_COLOR,
                    message.senderId, false);
        }

        // show pm toast in case chat is not selected
        if(chIdx == -1 || (chIdx != -1 && currentChannelIdx != chIdx)) {
            StringBuilder sb = new StringBuilder();
            sb.append(message.sender);
            sb.append(":\n");
            sb.append(message.message);
            GameScreen.getInstance().showPrivateMessage(String.valueOf(sb));
        }
    }

    public void dispose() {
        stopServerListening();
        openChannelWindow.stopServerListening();
    }

    public void clearWindows() {
        if(openChannelWindow.getStage() != null)
            CommonUI.removeWindowWithAction(openChannelWindow, fadeOut(0.2f));
    }

    public boolean isOnActor(Actor actor) {
        if(actor.equals(sendBtn) || actor.equals(msgField))
            return true;
        else
            return false;
    }

    public boolean isOnChatWindowLog(Actor actor) {
        if(scrollTable == null || actor == null) return false;

        // if click hits scroll table, its on chat window log and should not prevent interaction
        if(actor.equals(scrollTable))
            return true;
        // also if its not in any of the label children
        SnapshotArray<Actor> children = scrollTable.getChildren();
        Actor[] items = children.begin();
        for (int i = 0, n = children.size; i < n; i++) {
            Actor a = items[i];
            if(a.equals(actor))
                return true;
        }
        children.end();

        return false;
    }

    public static class Channel {
        public static float blinkInterval = 1f;
        public TextButton btn;
        public List<ChatMessage> log;
        public ChatChannel type;
        public int recipientId; // in case its a private chat
        public String recipientName; // in case its a private chat
        public boolean hasNewMessage = false; // in case there is a new msg not seen by player
        public Color originalColor = Color.WHITE;
        public boolean changeLock = true;
        StringBuilder lastChatSaved = new StringBuilder();

        public void startBlinking() {
            if(blink.isScheduled())
                blink.cancel();

            Timer.schedule(blink, 0, blinkInterval);
        }

        public void stopBlinking() {
            if(blink.isScheduled())
                blink.cancel();
            // make sure we are at original color
            btn.getLabel().setColor(originalColor);
        }

        private Timer.Task blink = new Timer.Task() {
            @Override
            public void run() {
                if(btn.getLabel().getColor().equals(originalColor)) {
                    btn.getLabel().setColor(new Color(1.0f, 0.34f, 0.1f, 1.0f));
                } else
                    btn.getLabel().setColor(originalColor);
            }
        };
    }

    public static class ChatMessage {
        public String sender; // the sender name
        public String message; // the message content without color tag or sender
        public int senderId, recipientId; // sender -1 == server ; recipient -1 == no recipient
        public Color color; // color to display this message
        public long timestamp; // time stamp of message
        public String langKey = null; // in case is a translatable msg from server
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

        // initialize channels list
        channels = Collections.synchronizedList(new ArrayList<>());

        // chat hit box for hover detection
        hitBox = new Rectangle();
        tmpLabel = new Label("", skin, "chatLabelStyle");

        /**
         * Ui atlas icons
         */
        uiAtlas = manager.get("ui/packed_textures/ui.atlas");

        TextureRegionDrawable up = new TextureRegionDrawable(new TextureRegion(new Sprite(uiAtlas.findRegion("ArrowLeft"))));
        leftChannelBtn = new Button(up, up.tint(Color.GRAY));

        up = new TextureRegionDrawable(new TextureRegion(new Sprite(uiAtlas.findRegion("ArrowRight"))));
        rightChannelBtn = new Button(up, up.tint(Color.GRAY));

        up = new TextureRegionDrawable(new TextureRegion(new Sprite(uiAtlas.findRegion("bookIcon"))));
        openChannelBtn = new Button(up, up.tint(Color.GRAY));

        up = new TextureRegionDrawable(new TextureRegion(new Sprite(uiAtlas.findRegion("closeIcon"))));
        closeChannelBtn = new Button(up, up.tint(Color.GRAY));

        /**
         * Prepare tab style
         */
        TextButton.TextButtonStyle tbChatTabStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        tbChatTabStyle.font = skin.get("fontChat", BitmapFont.class);
        tbChatTabStyle.fontColor = Color.LIGHT_GRAY;
        tbChatTabStyle.checkedFontColor = Color.WHITE;
        tbChatTabStyle.up =  new TextureRegionDrawable(skin.getAtlas().findRegion("tab")).tint(ChatWindow.TAB_COLOR_UNSELECTED);
        tbChatTabStyle.checked = new TextureRegionDrawable(skin.getAtlas().findRegion("tab-pressed")).tint(ChatWindow.TAB_COLOR_SELECTED);
        tbChatTabStyle.down = tbChatTabStyle.checked;
        skin.add("chatTab", tbChatTabStyle, TextButton.TextButtonStyle.class);

        startServerListening(ChatClient.getInstance());
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        // makes sure language is up to date with current selected chat
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // makes sure title is in the correct language
        //this.getTitleLabel().setText(" "+langBundle.format("chat"));

        /**
         * Tabs for channels
         */
        channelTabs = new Table();
        tabsButtonGroup = new ButtonGroup<>();
        tabsButtonGroup.setMinCheckCount(1);
        tabsButtonGroup.setMaxCheckCount(1);
        tabsButtonGroup.setUncheckLast(true);

        // tab buttons
        tabHorizontalBtns = new HorizontalGroup();
        tabHorizontalBtns.space(4f);
        tabsScrollPane = new ScrollPane(tabHorizontalBtns, skin);
        tabsScrollPane.setFadeScrollBars(true);
        tabsScrollPane.setupFadeScrollBars(0,0);
        tabsScrollPane.setScrollbarsOnTop(true);
        tabsScrollPane.setSmoothScrolling(false);
        tabsScrollPane.setScrollbarsVisible(false);
        tabsScrollPane.setForceScroll(false,false);
        tabsScrollPane.setFlingTime(0);
        tabsScrollPane.setScrollingDisabled(false, true);
        tabsScrollPane.setupOverscroll(0,0,0);

        // create initial channels - except private, guild and party channels (those are created during the game when necessary)
        createChannel(ChatChannel.DEFAULT, -1, null,true);
        currentChannel = channels.get(0); // starts at default tab always
        currentChannelIdx = 0; // current idx

        /**
         * chat window icon buttons
         */
        leftChannelBtn.setSize(getTabHeight(), getTabHeight());
        rightChannelBtn.setSize(getTabHeight(), getTabHeight());
        closeChannelBtn.setSize(getTabHeight(), getTabHeight());
        openChannelBtn.setSize(getTabHeight(), getTabHeight());
        leftChannelBtn.setColor(ICON_TINT_COLOR);
        closeChannelBtn.setColor(ICON_TINT_COLOR);
        openChannelBtn.setColor(ICON_TINT_COLOR);
        rightChannelBtn.setColor(ICON_TINT_COLOR);

        // adds scroll pane to table with correct limits
        Table tRight =new Table();
        Pixmap iconBgPx = new Pixmap(1, 1,Pixmap.Format.RGBA8888);
        //iconBgPx.setColor(new Color(0.0f, 0.2f, 0.0f, 0.6f));
        iconBgPx.setColor(PANE_BG_COLOR);
        iconBgPx.fill();
        tRight.setBackground(new Image(new Texture(iconBgPx)).getDrawable());
        tRight.defaults().padRight(1).padTop(1);
        tRight.add(rightChannelBtn).size(getTabHeight(), getTabHeight()+2).padLeft(ICON_HORIZONTAL_PAD);
        tRight.add(closeChannelBtn).size(getTabHeight(), getTabHeight());
        tRight.add(openChannelBtn).size(getTabHeight(), getTabHeight());

        Table tLeft = new Table();
        tLeft.setBackground(new Image(new Texture(iconBgPx)).getDrawable());
        tLeft.defaults().padTop(1);
        tLeft.add(leftChannelBtn).size(getTabHeight(), getTabHeight()+2).padRight(ICON_HORIZONTAL_PAD);

        iconBgPx.dispose();

        channelTabs.add(tLeft).padRight(ICON_HORIZONTAL_PAD*4);
        channelTabs.add(tabsScrollPane).colspan(1).left().size(CHAT_WIDTH - getIconsWidth(), getTabHeight()+2);
        channelTabs.add(tRight).padLeft(ICON_HORIZONTAL_PAD*4);

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
        newStyle.focusedFontColor = DEFAULT_CHAT_SENDER_COLOR;
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
        chatScrollPane.setSmoothScrolling(false);
        chatScrollPane.setupOverscroll(0,0,0);
        scrollToEnd();
        //chatScrollPane.setColor(Color.YELLOW);

        float adjustedHeight = CHAT_HEIGHT;
        if(Gdx.app.getType() == Application.ApplicationType.Android) adjustedHeight *= 1.15f;

        uiTable.add(chatScrollPane).colspan(2).size(CHAT_WIDTH, adjustedHeight);
        uiTable.row();
        uiTable.add(msgField).padBottom(5).left().colspan(1).growX().height(36);
        uiTable.add(sendBtn).padBottom(5).colspan(1).width(144).height(37);
        uiTable.pack();

        Pixmap labelColor = new Pixmap(1, 1,Pixmap.Format.RGBA8888);
        labelColor.setColor(PANE_BG_COLOR);
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

        // build open channel window
        buildOpenChannelWindow();

        // create welcome message
        sendMessage("[Server]", -1, ChatChannel.DEFAULT, langBundle.get("welcomeChatMessage"), "welcomeChatMessage", SERVER_CHAT_MESSAGE_COLOR, -1, false);
    }

    /**
     * Create default channels that need registry
     * so this should be called after client character is loaded
     */
    public void createDefaultChannels() {
        if(GameClient.getInstance().getClientCharacter() == null) return;

        // create other channels
        createChannel(ChatChannel.WORLD, -1, null,false);
        createChannel(ChatChannel.MAP, -1, null,false);
        createChannel(ChatChannel.HELP, -1, null,false);
        createChannel(ChatChannel.TRADE, -1, null,false);
    }

    private float getIconsWidth() {
        return rightChannelBtn.getWidth()+closeChannelBtn.getWidth()+openChannelBtn.getWidth()+leftChannelBtn.getWidth()+(10*ICON_HORIZONTAL_PAD) + 3;
    }

    /**
     * Creates a chat channel adding it to the current chat channels
     * @param type          the type of channel to be created
     * @param recipientId   the recipient id, in case of private message channel
     * @param recipientName the recipient name, in case of private message channel
     * @param select        if this channel should be selected after creation
     * @return  the index of the channel after creation in channels list, or -1 if channel was not created properly
     */
    public int createChannel(ChatChannel type, int recipientId, String recipientName, boolean select) {
        /**
         * Do not create private channels for itself
         */
        if(recipientId != -1 && recipientId == GameClient.getInstance().getClientId()) {
            GameScreen.getInstance().showInfo("cannotMessageYourself");
            return -1;
        }

        /**
         * Check if channel is already opened
         */
        int chIdx = searchChannel(type, recipientId);
        if(chIdx != -1) { // found channel already opened
            changeTab(chIdx);
            return -1;
        }

        List<ChatMessage> log = Collections.synchronizedList(new ArrayList<>());
        Channel ch = new Channel();
        ch.log = log; ch.type = type; ch.recipientId = recipientId;
        ch.recipientName = recipientName;

        /**
         * add channel to tabs table
         */
        TextButton btn;
        if(recipientId == -1) // its not a private msg
            btn = new TextButton(langBundle.get(type.getText()), skin, "chatTab"); // get translatable tab name
        else // its a private msg, get recipient name
            btn = new TextButton(ch.recipientName, skin, "chatTab");
        btn.padTop(1).padBottom(1).padLeft(11).padRight(11);

//        if(select)
//            btn.setColor(TAB_COLOR_SELECTED);
//        else
//            btn.setColor(TAB_COLOR_UNSELECTED);

        tabsButtonGroup.add(btn);
        btn.setChecked(select);
        // change listener for dealing with tab changes
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(btn.isChecked()) { // this button was checked
                    //btn.setColor(TAB_COLOR_SELECTED);

                    if(btn.getX() < tabsScrollPane.getScrollX()) {// getting cut left - decrease pane scrollX
                        float newX = tabsScrollPane.getScrollX() - (tabsScrollPane.getScrollX() - btn.getX());
                        //if(newX < 0) newX = 0;
                        tabsScrollPane.setScrollX(newX);
                    }
                    else if(btn.getX() > tabsScrollPane.getScrollX() + CHAT_WIDTH - getIconsWidth() - btn.getWidth()) { // getting cut right - increase pane scrollX
                        float newX = tabsScrollPane.getScrollX() + btn.getX() - (tabsScrollPane.getScrollX() + CHAT_WIDTH - getIconsWidth() - btn.getWidth());
                        tabsScrollPane.setScrollX(newX);
                    }
                    changeTab(searchChannel(ch.type, ch.recipientId));

                } else { // this button was unchecked
                    //btn.setColor(TAB_COLOR_UNSELECTED);
                    int chIdx = searchChannel(ch.type, ch.recipientId);
                    if(chIdx != -1) // channel still exists, was not closed
                        channels.get(chIdx).changeLock = true;
                }
            }
        });

        tabHorizontalBtns.addActor(btn); // add to button group ui
        channelTabs.pack();

        ch.btn = btn;
        channels.add(ch);   // add to channels data

        // listener to open tab context menu
        if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
            btn.addCaptureListener(new ClickListener(Input.Buttons.RIGHT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    openContextMenuTab(ch);
                }
            });
        } else if (Gdx.app.getType() == Application.ApplicationType.Android) {
            btn.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // only show if channel is already selected, to not mess up with change tab listener
                    if(!ch.changeLock)
                        openContextMenuTab(ch);

                    if(btn.isChecked())
                        ch.changeLock = false;
                }

            });
        }

        if(select) // if this channel is selected change to it
            changeTab(searchChannel(ch.type, ch.recipientId));

        // send registration message to server if channels are registry-based
        if(ch.type == ChatChannel.TRADE || ch.type == ChatChannel.WORLD || ch.type == ChatChannel.HELP)
            ChatClient.getInstance().sendRegistryUpdate(ch.type, true);

        return channels.size()-1;
    }

    /**
     * Closes an open channel
     * OBS: Default channel cannot be closed
     *
     * @param channelIndex   the index of the channel to be closed
     */
    public void closeChannel(int channelIndex) {
        Channel channel = channels.get(channelIndex);
        /**
         * DEFAULT CHANNEL CANT BE CLOSED!!
         */
        if(channel.type == ChatChannel.DEFAULT) {
            GameScreen.getInstance().showInfo("defaultChannelCannotClose");
            return;
        }

        /**
         * Searches for opened channel to be closed, and close it if found
         */
        int chIdx = searchChannel(channel.type, channel.recipientId);
        if(chIdx != -1) { // channel found
           // navigateTab(true); // navigate one channel to the left
            tabHorizontalBtns.removeActor(channels.get(chIdx).btn); // remove button ui actor
            tabHorizontalBtns.pack();
            channelTabs.pack();
            channels.remove(chIdx); // remove channel
            // if closed channel was the current channel...
            if(currentChannelIdx == chIdx)
                changeTab(chIdx - 1); // changes to left channel (at most it will be standard channel)
            else // update current idx
                updateCurrentIndex();
            // send registration message to server if channels are registry-based to unregister client in closed channel
            if(channel.type == ChatChannel.TRADE || channel.type == ChatChannel.WORLD || channel.type == ChatChannel.HELP)
                ChatClient.getInstance().sendRegistryUpdate(channel.type, false);
        }

        if(openChannelWindow.getStage()!=null) {
            openChannelWindow.updateCheckedCb();
        }
    }

    private void updateCurrentIndex() {
        currentChannelIdx = searchChannel(currentChannel.type, currentChannel.recipientId);
    }

    /**
     * Searches for channel in opened channels list
     *
     * @param type          the type of channel to be searched
     * @param recipientId   the recipient id in case of private channel type
     * @return  the channel index if found, -1 otherwise
     */
    public int searchChannel(ChatChannel type, int recipientId) {
        Iterator<Channel> it = channels.iterator();

        int i = 0;
        while(it.hasNext()) {
            Channel channel = it.next();

            if((channel.type == type && channel.type != ChatChannel.PRIVATE) ||
                    channel.type == ChatChannel.PRIVATE && channel.recipientId == recipientId) {
                return i;
            }
            i++;
        }

        return -1;
    }

    /**
     * builds the window for opening new channels
     */
    private void buildOpenChannelWindow() {
        openChannelWindow = new OpenChannelWindow(game, stage, parent, manager, "", skin, "newWindowStyle");
        openChannelWindow.build();
        openChannelWindow.centerInStage();
    }

    /**
     * shows the window for opening new channels
     */
    private void showOpenChannelWindow() {
        if(openChannelWindow.getStage() == null) {
            openChannelWindow.reset();
            stage.addActor(openChannelWindow);
        }
    }

    /**
     * hides the window for opening new channels
     */
    private void hideOpenChannelWindow() {
        if(openChannelWindow.getStage()!=null)
            openChannelWindow.remove();
    }


    /**
     * Gets the buttons that compose the channels buttons
     * that are stored in a table
     */
    public Table getChannelTabs() {
        return channelTabs;
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
     */
    private void updateChatLabel(Channel channel) {
        if(scrollTable == null) return; // still creating chat window

        List<ChatMessage> list = channel.log; // gets chat from tab
        StringBuilder stringBuilder = new StringBuilder(); // str builder that will build chat text
        // chat list may be accessed concurrently by chat client thread when other clients send messages

        scrollTable.clear();
        channel.lastChatSaved.setLength(0);
        synchronized(list) {
            int count=0;
            Iterator i = list.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                ChatMessage msg = (ChatMessage) i.next();
                stringBuilder.append("[#");
                stringBuilder.append(msg.color);
                stringBuilder.append("]");
                stringBuilder.append(Common.getTimeTag(msg.timestamp));
                channel.lastChatSaved.append(Common.getTimeTag(msg.timestamp));
                stringBuilder.append(" ");
                channel.lastChatSaved.append(" ");
                stringBuilder.append(msg.sender);
                channel.lastChatSaved.append(msg.sender);
                stringBuilder.append(": ");
                channel.lastChatSaved.append(": ");
                if(msg.langKey!=null) {// get translated msg if its a translatable server msg
                    stringBuilder.append(langBundle.format(msg.langKey));
                    channel.lastChatSaved.append(langBundle.format(msg.langKey));
                }
                else {
                    stringBuilder.append(msg.message);
                    channel.lastChatSaved.append(msg.message);
                }
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
                } else if (Gdx.app.getType() == Application.ApplicationType.Android) {
                    l.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            openContextMenu(msg);
                        }

                    });
                }

                scrollTable.add(l).growX().padRight(20).padLeft(5).padTop(-1f);
                if(i.hasNext()) {
                    scrollTable.row();
                    channel.lastChatSaved.append("\n");
                }

                stringBuilder.setLength(0);
                count++;
            }
        }

        scrollTable.layout();
        //chatHistoryLabel.setText(stringBuilder);
        //chatScrollPane.layout();
    }

    /**
     * Open chat context menu based on message clicked.
     * Builds the table with the respective interaction buttons and send to the screen to show it
     * @param msg   the chat message clicked to open context menu on
     */
    private void openContextMenu(ChatMessage msg) {
        Table t = new Table();

        /**
         * Context menu buttons
         */
        TextButton sendMsg = new TextButton(langBundle.format("contextMenuSendMessage", msg.sender), skin);
        TextButton cpyMsg = new TextButton(langBundle.get("contextMenuCopyMessage"), skin);
        TextButton cpyName = new TextButton(langBundle.get("contextMenuCopyName"), skin);
        TextButton cpyAll = new TextButton(langBundle.get("contextMenuCopyAll"), skin);
        TextButton addContact = null;
        if(!ChatClient.getInstance().isContact(msg.senderId)) {
            addContact = new TextButton(langBundle.format("contextMenuAddContact", msg.sender), skin);
        } else {
            addContact = new TextButton(langBundle.format("contextMenuRemoveContact", msg.sender), skin);
        }
        TextButton ignorePerson = null;

        if(!ChatClient.getInstance().isIgnored(msg.senderId)) {
            ignorePerson = new TextButton(langBundle.format("contextMenuIgnorePerson", msg.sender), skin);
        } else {
            ignorePerson = new TextButton(langBundle.format("contextMenuStopIgnorePerson", msg.sender), skin);
        }

        /**
         * Context menu button style
         */
        TextButton.TextButtonStyle newStyle = new TextButton.TextButtonStyle(sendMsg.getStyle());
        Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pxColor.setColor(new Color(0x75757575));
        pxColor.fill();
        newStyle.up = null;
        newStyle.over = new Image(new Texture(pxColor)).getDrawable();
        newStyle.down = null;
        newStyle.font.getData().setScale(0.81f, 0.81f);

        /**
         * Default config
         */
        sendMsg.setStyle(newStyle);
        sendMsg.getLabel().setAlignment(Align.left);
        cpyMsg.setStyle(newStyle);
        cpyMsg.getLabel().setAlignment(Align.left);
        cpyName.setStyle(newStyle);
        cpyName.getLabel().setAlignment(Align.left);
        cpyAll.setStyle(newStyle);
        cpyAll.getLabel().setAlignment(Align.left);
        addContact.setStyle(newStyle);
        addContact.getLabel().setAlignment(Align.left);
        ignorePerson.setStyle(newStyle);
        ignorePerson.getLabel().setAlignment(Align.left);

        /**
         * Listeners
         */
        sendMsg.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openChannelWindow.openChannel(msg.sender, msg.senderId);
                GameScreen.getInstance().hideContextMenu();
            }
        });

        addContact.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(!ChatClient.getInstance().isContact(msg.senderId)) {
                    ChatClient.getInstance().checkContactStatus(msg.senderId, msg.sender); // check server response will be treated as a command to add to contact list in GameScreen
                } else {
                    GameScreen.getInstance().getContactWindow().removeContact(msg.senderId);
                }
                GameScreen.getInstance().hideContextMenu();
            }
        });

        ignorePerson.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(!ChatClient.getInstance().isIgnored(msg.senderId)) // if its not in ignore list, add to it
                    GameScreen.getInstance().getContactWindow().ignoreContact(msg.senderId,  msg.sender);
                else // else, remove from contact list, as button will be toggled to un-ignore
                    ChatClient.getInstance().removeFromIgnoreList(msg.senderId);
                GameScreen.getInstance().hideContextMenu();
            }
        });

        cpyMsg.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Common.copyToClipboard(msg.message);
                GameScreen.getInstance().hideContextMenu();
                GameScreen.getInstance().showInfo("copiedMessage");
            }
        });

        cpyName.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Common.copyToClipboard(msg.sender);
                GameScreen.getInstance().hideContextMenu();
                GameScreen.getInstance().showInfo("copiedName");
            }
        });

        cpyAll.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Common.copyToClipboard(String.valueOf(currentChannel.lastChatSaved));
                GameScreen.getInstance().hideContextMenu();
                GameScreen.getInstance().showInfo("copiedAll");
            }
        });

        /**
         * Decorations
         */
        Pixmap strokePixmap = new Pixmap(1, 1, Pixmap.Format.RGB888);
        strokePixmap.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));
        strokePixmap.drawLine(0,0,1,0);
        Image strokeLine = new Image(new Texture(strokePixmap));
        strokePixmap.dispose();

        /**
         * Builds table
         */

        /**
         * Section available only for senders that are other players
         */
        if(msg.senderId != -1 && msg.senderId != GameClient.getInstance().getClientId()) {
            t.add(sendMsg).fillX();
            t.row();
            t.add(addContact).fillX();
            t.row();
            t.add(ignorePerson).fillX();
            t.row();
            t.add(strokeLine).fill().padBottom(5).padTop(8);
            t.row();
        }
        /**
         * Section for all chat messages
         */
        t.add(cpyMsg).fillX();
        t.row();
        t.add(cpyName).fillX();
        t.row();
        t.add(cpyAll).fillX();
        t.pack();
        t.layout();
        t.validate();

        /**
         * Send to screen to be shown
         */
        GameScreen.getInstance().showContextMenu(t);
        pxColor.dispose();
    }

    /**
     * Open channel context menu based on tab clicked.
     * Builds the table with the respective interaction buttons and send to the screen to show it
     * @param channel   the chat channel clicked to open context menu on
     */
    private void openContextMenuTab(Channel channel) {
        Table t = new Table();

        /**
         * Context menu buttons
         */
        TextButton closeTab = new TextButton(langBundle.format("close"), skin);
        TextButton clearTab = new TextButton(langBundle.get("contextMenuClearMessages"), skin);
        TextButton saveTab = new TextButton(langBundle.get("contextMenuSaveMessages"), skin);

        /**
         * Context menu button style
         */
        TextButton.TextButtonStyle newStyle = new TextButton.TextButtonStyle(closeTab.getStyle());
        Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pxColor.setColor(new Color(0x75757575));
        pxColor.fill();
        newStyle.up = null;
        newStyle.over = new Image(new Texture(pxColor)).getDrawable();
        newStyle.down = null;
        newStyle.font.getData().setScale(0.81f, 0.81f);

        /**
         * Default config
         */
        closeTab.setStyle(newStyle);
        closeTab.getLabel().setAlignment(Align.left);
        clearTab.setStyle(newStyle);
        clearTab.getLabel().setAlignment(Align.left);
        saveTab.setStyle(newStyle);
        saveTab.getLabel().setAlignment(Align.left);

        /**
         * Listeners
         */
        closeTab.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeChannel(searchChannel(channel.type, channel.recipientId));
                GameScreen.getInstance().hideContextMenu();
            }
        });

        clearTab.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clearChannelLog(channel);
                GameScreen.getInstance().hideContextMenu();
            }
        });

        saveTab.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveChannelLog(channel);
                GameScreen.getInstance().hideContextMenu();
                // TODO TELL THAT CHAT WAS SAVED LOCATION NAME ETC
            }
        });

        /**
         * Decorations
         */
        Pixmap strokePixmap = new Pixmap(1, 1, Pixmap.Format.RGB888);
        strokePixmap.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));
        strokePixmap.drawLine(0,0,1,0);
        Image strokeLine = new Image(new Texture(strokePixmap));
        strokePixmap.dispose();

        /**
         * Builds table
         */

        /**
         * Section available only for tabs that are not default tab, which is not closeable
         */
        if(channel.type != ChatChannel.DEFAULT) {
            t.add(closeTab).fillX();
            t.row();
            t.add(strokeLine).fill().padBottom(5).padTop(8);
            t.row();
        }
        /**
         * Section for all chat tabs
         */
        t.add(clearTab).fillX();
        t.row();
        t.add(saveTab).fillX();
        t.pack();
        t.layout();
        t.validate();

        /**
         * Send to screen to be shown
         */
        GameScreen.getInstance().showContextMenu(t);
        pxColor.dispose();
    }

    /**
     * Saves channel messages in a log file
     * @param channel   the channel to save the messages
     */
    private void saveChannelLog(Channel channel) {
        if(String.valueOf(channel.lastChatSaved).trim().length() == 0) return; // don't save empty chats

        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        int year =  calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        //System.out.println(String.valueOf(lastChatSaved));
        StringBuilder sbPath = new StringBuilder("Chat/");
        sbPath.append(channel.type.getText());
        sbPath.append(".txt");

        StringBuilder sbText = new StringBuilder("Chat Channel: ");
        sbText.append(channel.type.getText());
        sbText.append("\nDate: ");
        if (day <= 9) sbText.append('0'); // avoid using format methods to decrease work
        sbText.append(day);
        sbText.append("-");
        if (month <= 9) sbText.append('0'); // avoid using format methods to decrease work
        sbText.append(month);
        sbText.append("-");
        sbText.append(year);
        sbText.append(" ");
        if (hours <= 9) sbText.append('0'); // avoid using format methods to decrease work
        sbText.append(hours);
        sbText.append(":");
        if (minutes <= 9) sbText.append('0');
        sbText.append(minutes);
        sbText.append(":");
        if (seconds <= 9) sbText.append('0');
        sbText.append(seconds);
        sbText.append(" (SERVER TIME)");
        sbText.append("\n\n");
        sbText.append(channel.lastChatSaved);
        sbText.append("\n\n\n");

        FileHandle file = null;

        if(Gdx.app.getType() == Application.ApplicationType.Desktop) {
            file = Gdx.files.local(String.valueOf(sbPath));
        } else if(Gdx.app.getType() == Application.ApplicationType.Android) {
            file = Gdx.files.external(String.valueOf(sbPath));
        } else {
            return;
        }

        file.writeString(String.valueOf(sbText), true);

        GameScreen.getInstance().showInfo("chatMessagesSaved", String.valueOf(sbPath));
    }

    /**
     * Clear the message log for the channel received in parameter
     * @param channel   the channel to clear the message log
     */
    private void clearChannelLog(Channel channel) {
        synchronized (channel.log) {
            channel.log.clear();
        }

        if(currentChannelIdx == searchChannel(channel.type, channel.recipientId))
            updateChatLabel(channel);

        if(channel.hasNewMessage) { // stops blinking if it was blinking due to new message
            channel.hasNewMessage = false;
            channel.stopBlinking();
        }
    }

    /**
     * Returns the chat list that corresponds to the tab received in parameter
     * @param type	        the channel type to get the chat list from
     * @param recipientId   in case its a private message channel, the id of the recipient to load private messages
     * @return		the chat list that corresponds to the channel tab, null if no chat channel was found
     */
    private List<ChatMessage> chatLogFromChannel(ChatChannel type, int recipientId) {
        Iterator<Channel> it = channels.iterator();

        while(it.hasNext()) {
            Channel channel = it.next();
            if(channel.type == type) {
                if(type != ChatChannel.PRIVATE || (type == ChatChannel.PRIVATE && channel.recipientId == recipientId))
                    return channel.log;
            }
        }

        return null;
    }

    /**
     * Change channel tab to the provided index
     * The method searchTab should be used before to get the correct index
     * @param channelIndex  the channel index of the channel to be selected in the channels list
     */
    private void changeTab(int channelIndex) {
        if(chatScrollPane == null) return; // still building window
        currentChannel = channels.get(channelIndex);
        channels.get(channelIndex).btn.setChecked(true);
        currentChannelIdx = channelIndex;
        updateChatLabel(currentChannel);
        chatScrollPane.layout();
        chatScrollPane.setScrollPercentY(100);
        chatScrollPane.updateVisualScroll();
        if(currentChannel.hasNewMessage) { // stops blinking if it was blinking due to new message
            currentChannel.hasNewMessage = false;
            currentChannel.stopBlinking();
        }
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
        // remove token reserved characters
        String trimmedMsg = message.replaceAll("[\\[\\]\\{\\}]", "");

        if(trimmedMsg == "" || trimmedMsg.trim().length() == 0) return; // returns if there is no message

        Gdx.app.postRunnable(() -> {
            ChatMessage msg = new ChatMessage();
            msg.sender = sender;
            msg.senderId = senderId;
            if(trimmedMsg.length() > MAX_CHAT_MSG_CHARACTERS)
                msg.message = trimmedMsg.substring(0, MAX_CHAT_MSG_CHARACTERS-1);
            else
                msg.message = trimmedMsg;
            msg.color = color;
            msg.langKey = langKey;
            msg.recipientId = recipientId;
            msg.timestamp = System.currentTimeMillis();

            storeMessage(chatLogFromChannel(channel, recipientId), msg); // stores message in desired channel

            if (channel == currentChannel.type) { // update chat label if its current channel
                updateChatLabel(currentChannel);
                scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)
            }

            /**Create floating text above char that sent msg if its default msg (and not pm)*/
            if(channel == ChatChannel.DEFAULT && recipientId == -1) {
                Entity.Character senderChar = GameClient.getInstance().getCharacter(senderId);
                if(senderChar != null)
                    senderChar.renderFloatingText(msg.message);
            }

            /** Send to chat server **/
            if(sendToServer) {
                ChatClient.getInstance().sendMessage(msg, channel);
            }
        });
    }

    /**
     * Sends the message in the message field to the current channel with default color
     * This method is only called by client character messages
     */
    public void sendMessage() {
        Gdx.app.postRunnable(() -> {
            // remove token reserved characters
            String trimmedMsg = msgField.getText().replaceAll("[\\[\\]\\{\\}]", "");

            if(trimmedMsg == "" || trimmedMsg.trim().length() == 0) {
                clearMessageField(false); // lose focus and return if there is no message
                return;
            }

            // makes sure its within max size
            if(trimmedMsg.length() > MAX_CHAT_MSG_CHARACTERS)
                trimmedMsg = trimmedMsg.substring(0, MAX_CHAT_MSG_CHARACTERS-1);
            
            Entity.Character senderChar = GameClient.getInstance().getClientCharacter();

            // check cool down of registry channels
            if(isInCoolDown(currentChannel.type)) {
                clearMessageField(false);
                return;
            }

            List<String> texts = Common.splitEqually(trimmedMsg, MAX_LINE_CHARACTERS_FLOATING_TEXT);

            /** block player from messaging if it is full of floating text already **/
            if(senderChar.currentFloatingTexts.size() + texts.size() >= CommonUI.MAX_LINES_FLOATING_TEXT) {
                clearMessageField(false);
                GameScreen.getInstance().showInfo("infoWaitForMessaging");
                return;
            }

            ChatMessage msg = new ChatMessage();
            msg.sender = senderChar.name;
            msg.senderId = senderChar.id;
//            if(message.length() > MAX_CHAT_MSG_CHARACTERS)
//                msg.message = message.substring(0, MAX_CHAT_MSG_CHARACTERS-1);
//            else
            msg.message = trimmedMsg;
            Color c = DEFAULT_CHAT_SENDER_COLOR;
            if(currentChannel.type == ChatChannel.PRIVATE)
                c = PRIVATE_CHAT_SENDER_COLOR;
            msg.color = c;
            msg.recipientId = currentChannel.recipientId;
            msg.langKey = null;
            msg.timestamp = System.currentTimeMillis();

            storeMessage(currentChannel.log, msg); // stores message in current channel
            updateChatLabel(currentChannel);// update chat label since its current channel
            scrollToEnd(); // scroll to keep up with chat log (only scrolls if bar is already bottom)

            clearMessageField(false); // we can clear message field since we know message was sent from client

            /** Send to chat server **/
            ChatClient.getInstance().sendMessage(msg, currentChannel.type);

            /**Create floating text above char that sent msg*/
            if(currentChannel.type == ChatChannel.DEFAULT) {
                GameClient.getInstance().getClientCharacter().renderFloatingText(msg.message);
            }
        });
    }

    public boolean isInCoolDown(ChatChannel type) {
        int secondsLeft = 0;
        Entity.Character senderChar = GameClient.getInstance().getClientCharacter();

        if(type == ChatChannel.TRADE) {
            secondsLeft = MathUtils.ceil((System.currentTimeMillis() - senderChar.lastTradeTs) / 1000f);
            if(secondsLeft > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {
                senderChar.lastTradeTs = System.currentTimeMillis();
                return false;
            }
        } else if(type == ChatChannel.HELP) {
            secondsLeft = MathUtils.ceil((System.currentTimeMillis() - senderChar.lastHelpTs) / 1000f);
            if(secondsLeft > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {
                senderChar.lastHelpTs = System.currentTimeMillis();
                return false;
            }
        } else if(type == ChatChannel.WORLD) {
            secondsLeft = MathUtils.ceil((System.currentTimeMillis() - senderChar.lastWorldTs) / 1000f);
            if(secondsLeft > MESSAGE_REGISTRY_CHANNEL_COOLDOWN) {
                senderChar.lastWorldTs = System.currentTimeMillis();
                return false;
            }
        } else { // its a channel with no cool down
            return false;
        }

        secondsLeft = MESSAGE_REGISTRY_CHANNEL_COOLDOWN - secondsLeft;
        GameScreen.getInstance().showInfo("channelCooldown", secondsLeft);

        return true;
    }

    public boolean hasFocus() {
        return msgField.hasKeyboardFocus() || openChannelWindow.hasFocus();
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
        updateChatLabel(currentChannel);

        /** goes through tab buttons updating language of translatable channels **/
        Iterator<Channel> it = channels.iterator();
        while(it.hasNext()) {
            Channel ch = it.next();
            if(ch.type != ChatChannel.PRIVATE)
                ch.btn.setText(langBundle.get(ch.type.getText()));
        }
    }

    @Override
    public void resize(int width, int height) {
        updateHitBox();
    }

    @Override
    public void startServerListening(DispatchServer client) {
        this.listeningClients.add(client);
        // if its not listening to id by name responses, start listening to it
        if(!client.isListening("messageReceived", this))
            client.addListener("messageReceived", this);

    }

    @Override
    public void stopServerListening() {
        if(this.listeningClients == null) return;

        for(DispatchServer listeningClient : this.listeningClients) {
            // if its listening to login responses, stops listening to it
            if (listeningClient.isListening("messageReceived", this))
                listeningClient.removeListener("messageReceived", this);
        }

        this.listeningClients.clear();
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
     * A nested class that controls the chat window
     */
    class ChatController {

        // constructor adds listeners to the actors
        public ChatController() {
            sendBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    GameScreen.onStageActor = true;

                    String txt = msgField.getText();
                    if(txt == "" || txt.trim().length() == 0) return; // returns if there is no message

                    Color c = DEFAULT_CHAT_SENDER_COLOR;
                    if(currentChannel.type == ChatChannel.PRIVATE) // if its private use sender private color
                        c = PRIVATE_CHAT_SENDER_COLOR;

                    sendMessage(GameClient.getInstance().getClientCharacter().name, GameClient.getInstance().getClientCharacter().id,
                            currentChannel.type, txt, null, c, currentChannel.recipientId, true);
                    clearMessageField(false); // clear message field
                }
            });
            msgField.setTextFieldFilter((textField, c) -> {return chatFilter(textField, c);}); // filter unwanted chars

            /**
             * Icon btns
             */
            rightChannelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    navigateTab(false);
                }
            });
            leftChannelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    navigateTab(true);
                }
            });
            closeChannelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    closeChannel(currentChannelIdx);
                    if(openChannelWindow.getStage()!=null) {
                        openChannelWindow.updateCheckedCb();
                    }
                }
            });
            openChannelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(openChannelWindow.getStage() == null)
                        showOpenChannelWindow();
                    else
                        hideOpenChannelWindow();
                }
            });
        }

        //  Filters token chars
        private boolean chatFilter(TextField textField, char c) {
            if (Character.toString(c).matches("[{}\\[\\]]"))
                return false;
            return true;
        }
    }

    private ChatWindow getInstance() {
        return this;
    }
}
