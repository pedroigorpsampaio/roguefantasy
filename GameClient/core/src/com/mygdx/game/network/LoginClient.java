package com.mygdx.game.network;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygdx.game.network.LoginRegister.Login;
import com.mygdx.game.network.LoginRegister.Register;
import com.mygdx.game.network.LoginRegister.RegistrationRequired;
import com.mygdx.game.network.LoginRegister.Character;
import com.esotericsoftware.minlog.Log;

public class LoginClient extends DispatchServer{
    private static LoginClient instance; // login client instance (singleton)
    Client client;
    String name="";
    String host = "192.168.0.192";
    public boolean isConnected() {return isConnected;}
    private boolean isConnected = false;

    public LoginClient () {
        super(); // calls constructor of the superclass to instantiate listeners list

        client = new Client(65535, 65535);
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        LoginRegister.register(client);

        // listeners that are going to be notified on server responses
        listeners = new PropertyChangeSupport(this);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new ThreadedListener(new Listener() {
            public void connected (Connection connection) {
                isConnected = true;
            }

            /**
             *  Client received callback dispatches work to interested listeners
             */
            public void received (Connection connection, Object object) {
                if (object instanceof RegistrationRequired) {
//                    Register register = new Register();
//                    register.name = name;
//                    register.otherStuff = "ui.inputOtherStuff()";
//                    client.sendTCP(register);
                } else if (object instanceof LoginRegister.Response) { // Response type object
                    LoginRegister.Response response = (LoginRegister.Response) object;
                    switch(response.type) {
                        case CHAR_ALREADY_REGISTERED: // Response of registration type
                        case USER_ALREADY_REGISTERED:  // ...
                        case EMAIL_ALREADY_REGISTERED:  // ...
                        case DB_ERROR:                    // ...
                        case USER_SUCCESSFULLY_REGISTERED:  // Response of registration type
                            listeners.firePropertyChange("registrationResponse", null, response);
                            break;
                        case LOGIN_SUCCESSFUL:
                        case LOGIN_INVALID_CREDENTIALS:
                            listeners.firePropertyChange("loginResponse", null, response);
                            break;
                        default:
                            System.out.println("Unhandled login server response: "+response.type);
                            break;
                    }
                }

            }

            public void disconnected (Connection connection) {
                System.err.println("Disconnected from login server");
                isConnected = false;
            }
        }));
    }

    // tries to connect with the login server
    public void connect() {
        try {
            client.connect(5000, host, LoginRegister.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Could not connect with login server");
            isConnected = false;
        }
    }

    // creates connection when not connected
    public static LoginClient getInstance() {
        if(instance == null)
            instance = new LoginClient();
//        else if(!instance.isConnected)
//            instance.connect();

        return instance;
    }

    public void sendLoginAttempt(Login login) {
        client.sendTCP(login);
    }

    public void sendRegisterAttempt(Register register) {
        client.sendTCP(register);
    }

    // sends hello message to login server initiating communication
    public void sendHello() {
        client.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DISCARD));
    }
}
