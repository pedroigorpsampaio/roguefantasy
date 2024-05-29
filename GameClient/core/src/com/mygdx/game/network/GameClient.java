package com.mygdx.game.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.entity.Component;
import com.mygdx.game.network.GameRegister.AddCharacter;
import com.mygdx.game.network.GameRegister.ClientId;
import com.mygdx.game.network.GameRegister.MoveCharacter;
import com.mygdx.game.network.GameRegister.Register;
import com.mygdx.game.network.GameRegister.RegistrationRequired;
import com.mygdx.game.network.GameRegister.RemoveCharacter;
import com.mygdx.game.network.GameRegister.UpdateCharacter;
import com.mygdx.game.util.Encoder;

import java.io.IOException;
import java.util.HashMap;

public class GameClient {
    private static GameClient instance; // login client instance
    private InputProcessor oldIProcessor;
    UI ui;
    private Client client;
    String name="";
    String host="";
    private int clientCharId; // this client's char id

    public boolean isConnected() {return isConnected;}
    private boolean isConnected = false;

    protected GameClient() {
        client = new Client(65535, 65535);
        client.start();

//        oldIProcessor = Gdx.input.getInputProcessor();
//        Gdx.input.setInputProcessor(new InputAdapter() {
//            @Override
//            public boolean keyTyped (char character) {
//                moveCharacter(character);
//                return true;
//            }
//        });

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        GameRegister.register(client);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
//                oldIProcessor = Gdx.input.getInputProcessor();
//                Gdx.input.setInputProcessor(new InputAdapter() {
//                    @Override
//                    public boolean keyTyped (char character) {
//                        moveCharacter(character);
//                        return true;
//                    }
//                });
            }

            public void received (Connection connection, Object object) {
                if (object instanceof RegistrationRequired) {
                    Register register = new Register();
                    register.name = name;
                    register.otherStuff = "ui.inputOtherStuff()";
                    client.sendTCP(register);
                }

                if (object instanceof ClientId) {
                    ClientId msg = (ClientId)object;
                    clientCharId =  msg.id;
                    return;
                }

                if (object instanceof AddCharacter) {
                    AddCharacter msg = (AddCharacter)object;
                    Component.Character character = Component.Character.toCharacter(msg.character);
                    ui.addCharacter(character);
                    return;
                }

                if (object instanceof UpdateCharacter) {
                    ui.updateCharacter((UpdateCharacter)object);
                    return;
                }

                if (object instanceof RemoveCharacter) {
                    RemoveCharacter msg = (RemoveCharacter)object;
                    ui.removeCharacter(msg.id);
                    return;
                }
            }

            public void disconnected (Connection connection) {
                System.err.println("Disconnected from game server");
                isConnected = false;
                //Gdx.input.setInputProcessor(oldIProcessor);
            }
        }));

        ui = new UI();
        host = "192.168.0.192";
    }

    public void connect() {
        try {
            client.connect(5000, host, GameRegister.tcp_port, GameRegister.udp_port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
        }

//        name = "b";
//        Login login = new Login();
//        login.name = name;
//        client.sendTCP(login);

        // show keyboard on android for testing
        //Gdx.input.setOnscreenKeyboardVisible(true);
    }

    public static GameClient getInstance() {
        if(instance == null)
            instance = new GameClient();
        return instance;
    }

    public void moveCharacter(MoveCharacter msg) {
        if (msg != null) client.sendUDP(msg);
    }

    /**
     * Logins into game server with token received from login server
     * @param token the token received from login server for the current authorized user
     */
    public void sendTokenAsync(String token) {
        // Encrypt and send to server on another thread
        new Thread(() -> {
            Encoder encoder = new Encoder();
            byte[] encryptedToken = encoder.signAndEncryptData(token);
            GameRegister.Token tk = new GameRegister.Token();
            tk.token = encryptedToken;
            client.sendTCP(tk);
        }).start();
    }

    public HashMap<Integer, Component.Character> getOnlineCharacters() {
        return ui.characters;
    }
    public Component.Character getClientCharacter() {return ui.characters.get(clientCharId);}

    static class UI {

        HashMap<Integer, Component.Character> characters = new HashMap();

//
//        public String inputOtherStuff () {
//            String input = (String)JOptionPane.showInputDialog(null, "Other Stuff:", "Create account", JOptionPane.QUESTION_MESSAGE,
//                    null, null, "other stuff");
//            if (input == null || input.trim().length() == 0) System.exit(1);
//            return input.trim();
//        }

        public void addCharacter (Component.Character character) {
            characters.put(character.id, character);
            System.out.println(character.name + " added at " + character.x + ", " + character.y);
            character.update(character.x, character.y);
        }

        public void updateCharacter (UpdateCharacter msg) {
            Component.Character character = characters.get(msg.id);
            if (character == null) return;
            character.update(msg.x, msg.y);
            //System.out.println(character.name + " moved to " + character.x + ", " + character.y);
        }

        public void removeCharacter (int id) {
            Component.Character character = characters.remove(id); // remove from list of logged chars
            if (character != null) {
                character.dispose();
                System.out.println(character.name + " removed");
            }
        }
    }
}
