package com.mygdx.game.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import com.mygdx.game.entity.Entity;
import com.mygdx.game.network.ChatClient;
import com.mygdx.game.network.ChatRegister;
import com.mygdx.game.network.DispatchServer;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.GameRegister;
import com.mygdx.game.util.Common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * A class that encapsulates the option menu window
 */
public class ContactWindow extends GameWindow implements PropertyChangeListener {
    public static final Color CONTACT_OFFLINE_COLOR = new Color(0.95f, 0.34f, 0.25f, 1f);
    public static final Color CONTACT_ONLINE_COLOR = new Color(0.15f, 0.99f, 0.45f, 1f);
    private Table uiTable;
    private TextButton closeBtn;
    private Table scrollTable;
    private ScrollPane scrollPane;
    private TextField friendNameInputTxt;
    private boolean isUpdate = false;
    private float txtFieldOffsetY = 0;
    private float originalY;
    private boolean orderByName = false;
    private boolean labelClicked = false;

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

    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByNames(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        ChatRegister.Writer s1 = (ChatRegister.Writer) e1.getValue();
                        ChatRegister.Writer s2 = (ChatRegister.Writer) e2.getValue();

                        return s1.compareTo(s2);
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    /**
     * Build contacts scroll table from contacts stored in client contacts map
     *
     * @param orderByName if true, will order by name; if false, will order by status (online first, then offline) and then by name
     */
    public void buildContacts(boolean orderByName) {
        if(scrollTable == null) return;

        scrollTable.clear();

        int count = 0;
        synchronized (ChatClient.contacts) {
            for (Map.Entry<Integer, ChatRegister.Writer> entry : entriesSortedByNames(ChatClient.contacts)) {
                ChatRegister.Writer contact = entry.getValue();

                TypingLabel label = new TypingLabel(contact.name, skin);
                label.setName(contact.name);
                //label.setZIndex(contact.id);
                label.setAlignment(Align.left);
                label.skipToTheEnd();

                if(contact.online)
                    label.setColor(CONTACT_ONLINE_COLOR);
                else
                    label.setColor(CONTACT_OFFLINE_COLOR);

                // right click to context menu for desktop
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
                    label.addListener(new ClickListener(Input.Buttons.LEFT) {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if( getTapCount() == 2) { // double click - open chat with contact
                                GameScreen.openChannelWindow.openChannel(contact.name, contact.id);
                            }
                        }
                    });
                    label.addListener(new ClickListener(Input.Buttons.RIGHT) {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            labelClicked = true;
                            openContextMenu(contact);
                        }
                    });
                } else if (Gdx.app.getType() == Application.ApplicationType.Android) {
                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            labelClicked = true;
                            openContextMenu(contact);
                        }

                    });
                }
                if(count == 0) {
                    scrollTable.add(label).growX().padLeft(12).padTop(6).padBottom(1);
                    scrollTable.row();
                }
                else if (count < ChatClient.contacts.size() - 1) {
                    scrollTable.add(label).growX().padLeft(12).padBottom(1f);
                    scrollTable.row();
                } else {
                    scrollTable.add(label).growX().padLeft(12).padBottom(6);
                }

                count++;
            }
        }

        scrollTable.layout();

    }

    /**
     * Opens context menu based on contact clicked (or if clicked in empty part of window table)
     *
     * @param contact   the contact clicked. if null will build generic context window for contact list
     */
    private void openContextMenu(ChatRegister.Writer contact) {
        Table t = new Table();

        int contactId = -1;
        String contactName = "";

        if(contact != null) {
            contactId = contact.id;
            contactName = contact.name;
        }

        /**
         * Context menu buttons
         */
        TextButton sendMsg = new TextButton(langBundle.format("contextMenuSendMessage", contactName), skin);
        TextButton editContact = new TextButton(langBundle.format("contextMenuEditContact", contactName), skin);
        TextButton removeContact = new TextButton(langBundle.format("contextMenuRemoveContact", contactName), skin);
        TextButton cpyName = new TextButton(langBundle.get("contextMenuCopyName"), skin);

        TextButton addNewContact = new TextButton(langBundle.get("contextMenuAddNewContact"), skin);
        String sortString = orderByName ? "status" : langBundle.get("name");
        TextButton sortBy = new TextButton(langBundle.format("contextMenuSortBy", sortString), skin);

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
        editContact.setStyle(newStyle);
        editContact.getLabel().setAlignment(Align.left);
        cpyName.setStyle(newStyle);
        cpyName.getLabel().setAlignment(Align.left);
        removeContact.setStyle(newStyle);
        removeContact.getLabel().setAlignment(Align.left);
        addNewContact.setStyle(newStyle);
        addNewContact.getLabel().setAlignment(Align.left);
        sortBy.setStyle(newStyle);
        sortBy.getLabel().setAlignment(Align.left);

        /**
         * Listeners
         */
        String finalContactName = contactName;
        int finalContactId = contactId;
        sendMsg.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameScreen.openChannelWindow.openChannel(finalContactName, finalContactId);
                GameScreen.getInstance().hideContextMenu();
            }
        });

        editContact.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("TODO edit contact (add emote/icon and tool tip on hover commentary)");
                GameScreen.getInstance().hideContextMenu();
            }
        });

        removeContact.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("TODO  REMOVE CONTACT FROM CLIENT LIST AND SEND REMOVE MSG TO SERVER");
                //TODO REMOVE CONTACT FROM CLIENT LIST AND SEND REMOVE MSG TO SERVER
                GameScreen.getInstance().hideContextMenu();
            }
        });

        cpyName.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Common.copyToClipboard(finalContactName);
                GameScreen.getInstance().hideContextMenu();
                GameScreen.getInstance().showInfo("copiedName");
            }
        });

        addNewContact.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("TODO ADD CONTACT: OPEN TEXT FIELD TO WRITE CONTACT NAME - CHECK IF USER EXIST SIMILAR TO OPENCHANNEL - IF SO ADD IN CLIENT AND SEND ADD MSG TO SERVER");
                //TODO ADD CONTACT: OPEN TEXT FIELD TO WRITE CONTACT NAME - CHECK IF USER EXIST SIMILAR TO OPENCHANNEL - IF SO ADD IN CLIENT AND SEND ADD MSG TO SERVER
                GameScreen.getInstance().hideContextMenu();
            }
        });

        sortBy.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                orderByName = orderByName ? false : true;
                buildContacts(orderByName);
                GameScreen.getInstance().hideContextMenu();
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
         * Section available only when contact is not null (player clicked on a contact name)
         */
        if(contact != null) {
            t.add(sendMsg).fillX();
            t.row();
            t.add(editContact).fillX();
            t.row();
            t.add(removeContact).fillX();
            t.row();
            t.add(cpyName).fillX();
            t.row();
            t.add(strokeLine).fill().padBottom(5).padTop(8);
            t.row();
        }
        /**
         * Section for all contact window context menu clicks
         */
        t.add(addNewContact).fillX();
        t.row();
        t.add(sortBy).fillX();
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
     * Adds contact to the map and to the label list
     * @param id    the id of the contact to be added
     * @param name  the name of the contact to be added
     * @param online if the contact is currently online
     */
    public void addContact(int id, String name, boolean online) {

    }

    /**
     * Removes contact from map and from label list
     * @param id    the id of the contact to be removed
     */
    public void removeContact(int id) {

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

        scrollTable = new Table();
        scrollTable.setBackground(new Image(uiAtlas.findRegion("UiBg")).getDrawable());
        scrollTable.align(Align.topLeft);
        scrollTable.defaults().padLeft(10).padTop(0).padRight(10);
        scrollTable.pack();

        scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsOnTop(true);
        scrollPane.setSmoothScrolling(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setupOverscroll(0,0,0);


        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            scrollPane.addListener(new ClickListener(Input.Buttons.RIGHT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(labelClicked) {
                        labelClicked = false;
                        return;
                    }
                    openContextMenu(null);
                }
            });
        } else if (Gdx.app.getType() == Application.ApplicationType.Android) {
            scrollPane.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(labelClicked) {
                        labelClicked = false;
                        return;
                    }
                    openContextMenu(null);
                }

            });
        }

        friendNameInputTxt = new TextField("", skin);

        uiTable.add(scrollPane).size(stage.getWidth()/8f, scrollTable.getMinHeight()+stage.getHeight()/8f);
        uiTable.row();
        HorizontalGroup horizontalBtnGroup = new HorizontalGroup();
        horizontalBtnGroup.space(20);
        horizontalBtnGroup.addActor(closeBtn);
        uiTable.add(horizontalBtnGroup).padTop(10);
        uiTable.pack();

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

        // builds initial contacts list (will use chat loaded contacts from server)
        buildContacts(orderByName);
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
