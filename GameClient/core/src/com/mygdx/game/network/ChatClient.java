package com.mygdx.game.network;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.ui.ChatWindow;
import com.mygdx.game.ui.GameScreen;

public class ChatClient extends DispatchServer {
    private static ChatClient instance; // login client instance
    public boolean isContactsLoaded;
    private Client client;
    String name="";
    String host = "192.168.0.192";
    private LagNetwork lagNetwork;

    public static Map<Integer, ChatRegister.Writer> contacts = new ConcurrentHashMap<>(); // contacts of this client
    public static Map<Integer, ChatRegister.Writer> ignoreList = new ConcurrentHashMap<>(); // ignore list of this client

    /**
     * Adds the contact to local map and and send message to server to store contact server-side
     * @param contact   the contact to be added for this client contacts list
     */
    public void addContact(ChatRegister.Writer contact) {
        // if contact is already on map, return showing info toast
        if(contacts.containsKey(contact.id)) {
            GameScreen.getInstance().showInfo("contactAlreadyRegistered");
            return;
        }

        // add to the local list (if not there yet)
        synchronized (contacts) {
            if(contacts.size() < ChatRegister.MAX_NUM_CONTACTS) // only add if its not full client-side
                contacts.put(contact.id, contact);
        }

        // send contact to be saved server-side
        ChatRegister.AddContact ac = new ChatRegister.AddContact();
        ac.contactId = contact.id;

        // if its full, server-side will send a full contact list response
        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(ac, 1);
        } else {
            client.sendTCP(ac);
        }
    }

    /**
     * Removes the contact from local map and and send message to server to remove contact server-side
     * @param id   the id of the contact to be removed from this client contacts list
     */
    public void removeContact(int id) {
        // if contact is not on list, return (does not need to show toast, player won't input text for contact removal, this should be a bug if it happens)
        if(!contacts.containsKey(id)) {
            Gdx.app.log("contacts", "Error removing contact: contact does not exist in map - " + id);
            return;
        }

        // remove from the local list
        synchronized (contacts) {
            contacts.remove(id);
        }

        // send message to exclude contact server-side
        ChatRegister.RemoveContact rc = new ChatRegister.RemoveContact();
        rc.contactId = id;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(rc, 1);
        } else {
            client.sendTCP(rc);
        }
    }

    /**
     * Returns if a contact is currently being ignored by client player
     * @param contactId the id to check if its ignored
     */
    public boolean isIgnored(int contactId) {
        return ignoreList.containsKey(contactId);
    }

    /**
     * Returns if a contact is currently in client player contact list
     * @param contactId the id to check if its in contact list
     */
    public boolean isContact(int contactId) {
        return contacts.containsKey(contactId);
    }

    /**
     * Adds the ignore to local map and and send message to server to store ignore server-side
     * @param contact   the contact to be added for this client ignore list
     */
    public void addToIgnoreList(ChatRegister.Writer contact) {
        // if contact is already on map, return showing info toast
        if(ignoreList.containsKey(contact.id)) {
            GameScreen.getInstance().showInfo("contactAlreadyIgnored");
            return;
        }

        // add to the local list (if not there yet)
        synchronized (ignoreList) {
            if(ignoreList.size() < ChatRegister.MAX_NUM_IGNORE_LIST) // only add if its not full client-side
                ignoreList.put(contact.id, contact);
        }

        // send contact to be saved server-side
        ChatRegister.AddIgnore ai = new ChatRegister.AddIgnore();
        ai.contactId = contact.id;

        // if its full, server-side will send a full contact list response
        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(ai, 1);
        } else {
            client.sendTCP(ai);
        }
    }

    /**
     * Removes the ignore from local map and and send message to server to remove ignore server-side
     * @param id   the id of the contact to be removed from this client ignore list
     */
    public void removeFromIgnoreList(int id) {
        // if contact is not on list, return (does not need to show toast, player won't input text for contact removal, this should be a bug if it happens)
        if(!ignoreList.containsKey(id)) {
            Gdx.app.log("ignore_list", "Error removing contact: contact does not exist in ignore list map - " + id);
            return;
        }

        // remove from the local list
        synchronized (ignoreList) {
            ignoreList.remove(id);
        }

        // send message to exclude contact server-side
        ChatRegister.RemoveIgnore ri = new ChatRegister.RemoveIgnore();
        ri.contactId = id;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(ri, 1);
        } else {
            client.sendTCP(ri);
        }
    }

    public boolean isConnected() {return isConnected;}
    private boolean isConnected = false;

    protected ChatClient () {
        super(); // calls constructor of the superclass to instantiate listeners list

        isContactsLoaded = false;

        client = new Client(65535, 65535);
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        ChatRegister.register(client);

        // listeners that are going to be notified on server responses
        listeners = new PropertyChangeSupport(this);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
                isConnected = true;
                lagNetwork = new LagNetwork(client); // to simulate lag when desired
            }

            /**
             *  Client received callback dispatches work to interested listeners
             */
            public void received (Connection connection, Object object) {
                if (object instanceof ChatRegister.Message) { // token received to login in game server
                    ChatRegister.Message msg = (ChatRegister.Message) object;
                    listeners.firePropertyChange("messageReceived", null, msg);
                } else if(object instanceof  ChatRegister.Response) {
                    ChatRegister.Response msg = (ChatRegister.Response) object;
                    switch (msg.type) {
                        case PLAYER_IS_OFFLINE:
                            GameScreen.getInstance().showInfo("playerIsNotOnline");
                            break;
                        case CONTACT_ADDED:
                            listeners.firePropertyChange("contactAdded", null, msg);
                            break;
                        case CONTACT_REMOVED:
                            listeners.firePropertyChange("contactRemoved", null, msg);
                            break;
                        case FULL_CONTACT_LIST:
                            listeners.firePropertyChange("fullContactList", null, msg);
                            break;
                        case CONTACT_ADDED_TO_IGNORE_LIST:
                            listeners.firePropertyChange("contactAddedToIgnoreList", null, msg);
                            break;
                        case CONTACT_REMOVED_FROM_IGNORE_LIST:
                            listeners.firePropertyChange("contactRemovedFromIgnoreList", null, msg);
                            break;
                        case FULL_IGNORE_LIST:
                            listeners.firePropertyChange("fullIgnoreList", null, msg);
                            break;
                        default:
                            break;
                    }
                } else if(object instanceof ChatRegister.ContactsRequest) {
                    ChatRegister.ContactsRequest msg = (ChatRegister.ContactsRequest) object;
                    contacts = msg.contacts;
                    ignoreList = msg.ignoreList;
                    isContactsLoaded = true;
                } else if(object instanceof ChatRegister.OnlineCheck) { // someone requested online check
                    ChatRegister.OnlineCheck msg = (ChatRegister.OnlineCheck) object;
                    listeners.firePropertyChange("onlineCheck", null, msg);
                }
            }

            public void disconnected (Connection connection) {
                System.err.println("Disconnected from chat server");
                isConnected = false;
            }
        }));
    }

    // tries to connect with the login server
    public void connect() {
        try {
            client.connect(5000, host, ChatRegister.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Could not connect with chat server");
            isConnected = false;
        }
    }

    // creates connection when not connected
    public static ChatClient getInstance() {
        if(instance == null)
            instance = new ChatClient();
//        else if(!instance.isConnected)
//            instance.connect();

        return instance;
    }


    /**
     * Sends message to chat server that will deal with the distribution
     *
     * @param chatMsg the chat message containing the necessary info to compose the message for chat server
     * @param channel the channel that this chat message was sent to
     */
    public void sendMessage(ChatWindow.ChatMessage chatMsg, ChatRegister.ChatChannel channel) {
        ChatRegister.Message message = new ChatRegister.Message();
        message.message = chatMsg.message;
        message.channel = channel;
        message.recipientId = chatMsg.recipientId;
        message.sender = chatMsg.sender;
        message.senderId = chatMsg.senderId;
        message.timestamp = chatMsg.timestamp;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(message, 1);
        } else {
            client.sendTCP(message);
        }
    }

    /**
     * Logs in in chat server to be able to receive messages from it
     * @param character     the character info of this client containing id and name
     */
    public void login(Entity.Character character) {
        ChatRegister.Writer writer = new ChatRegister.Writer();
        writer.id = character.id;
        writer.name = character.name;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(writer, 1);
        } else {
            client.sendTCP(writer);
        }
    }

    public void logoff() {
        isConnected = false;
        contacts.clear();
        ignoreList.clear();

        // tell interested listeners that server has lost connection
        listeners.firePropertyChange("lostConnection", null, true);
        new Thread(() -> {
            client.sendTCP(new ChatRegister.Response(ChatRegister.Response.Type.LOGOFF)); // sends msg to server to log me off
            client.close();
        }).start();
    }

    /**
     * Sends a message to the chat server requesting update in registry for this client
     * for chat channels that require registration to be able to receive/send messages
     * @param type      the type of the channel to update registry of this client
     * @param register  true if this update is to register, false if it is to unregister
     */
    public void sendRegistryUpdate(ChatRegister.ChatChannel type, boolean register) {
        ChatRegister.ChatRegistration cr = new ChatRegister.ChatRegistration();
        cr.channel = type; cr.register = register; cr.id = GameClient.getInstance().getClientId();
        cr.name = GameClient.getInstance().getClientCharacter().name;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(cr, 1);
        } else {
            client.sendTCP(cr);
        }
    }

    /**
     * Sends a request to server to load client contacts
     * @param id    the client id to load contacts
     */
    public void loadContacts(int id) {
        ChatRegister.ContactsRequest cr = new ChatRegister.ContactsRequest();
        cr.requesterId = id;

        client.sendTCP(cr);
    }

    /**
     * Checks online status - request server info about specific contact
     * Information will be sent back by server encapsulated on an OnlineCheck object
     *
     * @param id        the id of the contact to be checked
     * @param name      the name of the contact to be checked
     */
    public void checkContactStatus(int id, String name) {
        ChatRegister.OnlineCheck oc = new ChatRegister.OnlineCheck();
        oc.contactId = id; oc.contactName = name;

        if(lagNetwork != null && GameRegister.lagSimulation) { // send with simulated lag
            lagNetwork.send(oc, 1);
        } else {
            client.sendTCP(oc);
        }
    }
}
