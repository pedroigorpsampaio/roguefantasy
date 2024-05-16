package com.mygdx.game.network;

import java.io.IOException;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.network.LoginRegister.AddCharacter;
import com.mygdx.game.network.LoginRegister.Login;
import com.mygdx.game.network.LoginRegister.MoveCharacter;
import com.mygdx.game.network.LoginRegister.Register;
import com.mygdx.game.network.LoginRegister.RegistrationRequired;
import com.mygdx.game.network.LoginRegister.RemoveCharacter;
import com.mygdx.game.network.LoginRegister.UpdateCharacter;
import com.mygdx.game.network.LoginRegister.Character;
import com.esotericsoftware.minlog.Log;

public class LoginClient {
    UI ui;
    Client client;
    String name="";
    String host="";

    public LoginClient () {
        client = new Client();
        client.start();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped (char character) {
                moveCharacter(character);
                return true;
            }
        });

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        LoginRegister.register(client);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
            }

            public void received (Connection connection, Object object) {
                if (object instanceof RegistrationRequired) {
                    Register register = new Register();
                    register.name = name;
                    register.otherStuff = "ui.inputOtherStuff()";
                    client.sendTCP(register);
                }

                if (object instanceof AddCharacter) {
                    AddCharacter msg = (AddCharacter)object;
                    ui.addCharacter(msg.character);
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
                System.exit(0);
            }
        }));

        ui = new UI();
        host = "192.168.0.192";

        try {
            client.connect(5000, host, LoginRegister.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        name = "b";
        Login login = new Login();
        login.name = name;
        client.sendTCP(login);

        // shhow keyboard onz android for testing
        Gdx.input.setOnscreenKeyboardVisible(true);
    }

    public void moveCharacter(char c) {
        MoveCharacter msg = new MoveCharacter();
        switch (c) {
            case 'w':
                msg.y = -1;
                break;
            case 's':
                msg.y = 1;
                break;
            case 'a':
                msg.x = -1;
                break;
            case 'd':
                msg.x = 1;
                break;
            default:
                msg = null;
        }
        if (msg != null) client.sendTCP(msg);
    }

    static class UI {

        HashMap<Integer, Character> characters = new HashMap();

//
//        public String inputOtherStuff () {
//            String input = (String)JOptionPane.showInputDialog(null, "Other Stuff:", "Create account", JOptionPane.QUESTION_MESSAGE,
//                    null, null, "other stuff");
//            if (input == null || input.trim().length() == 0) System.exit(1);
//            return input.trim();
//        }

        public void addCharacter (Character character) {
            characters.put(character.id, character);
            System.out.println(character.name + " added at " + character.x + ", " + character.y);
        }

        public void updateCharacter (UpdateCharacter msg) {
            Character character = characters.get(msg.id);
            if (character == null) return;
            character.x = msg.x;
            character.y = msg.y;
            System.out.println(character.name + " moved to " + character.x + ", " + character.y);
        }

        public void removeCharacter (int id) {
            Character character = characters.remove(id);
            if (character != null) System.out.println(character.name + " removed");
        }
    }
}
