package com.mygdx.server.network;

import java.beans.PropertyChangeSupport;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygdx.server.db.DbController;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;


/**
 * LOGIN SERVER
 * Server that deals with login, registration, character creation
 * and other communications related to the authentication of the user
 */
public class LoginServer extends DispatchServer implements CmdReceiver {
    private static LoginServer instance;
    private DbController dbController; // reference to the controller of the database
    Server server;
    HashSet<LoginRegister.Character> loggedIn = new HashSet();
    private boolean isOnline = false; // is this server online?

    private LoginServer() {
        super(); // calls constructor of the superclass to instantiate listeners list
        server = new Server(65535, 65535) {
            protected Connection newConnection () {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new CharacterConnection();
            }
        };

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        LoginRegister.register(server);

        server.addListener(new Listener() {
            public void received (Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                CharacterConnection connection = (CharacterConnection)c;
                DbController.CharacterLoginData charData = connection.charData;

                if (object instanceof LoginRegister.Login) {
                    // Ignore if already logged in.
                   // if (character != null) return;

                    // creates request object to send to listeners
                    Request request = new Request(connection, object);
                    // send request to interested listeners
                    listeners.firePropertyChange("loginRequest", null, request);


                    // Reject if the name is invalid.
//                    String name = ((LoginRegister.Login)object).name;
//                    if (!isValid(name)) {
//                        c.close();
//                        return;
//                    }
//
//                    // Reject if already logged in.
//                    for (LoginRegister.Character other : loggedIn) {
//                        if (other.name.equals(name)) {
//                            c.close();
//                            return;
//                        }
//                    }
//
//                    character = loadCharacter(name);
//
//                    // Reject if couldn't load character.
//                    if (character == null) {
//                        c.sendTCP(new LoginRegister.RegistrationRequired());
//                        return;
//                    }
//
//                    loggedIn(connection, character);
                    return;
                }

                if (object instanceof LoginRegister.Register) {
                    // Ignore if already logged in.
                    //if (character != null) return;

                    // creates request object to send to listeners
                    Request request = new Request(connection, object);
                    // send request to interested listeners
                    listeners.firePropertyChange("registerRequest", null, request);

//                    LoginRegister.Register register = (LoginRegister.Register)object;
//                    String userName = encoder.decryptSignedData(register.userName);
//                    String password = encoder.decryptSignedData(register.password);
//                    String email = encoder.decryptSignedData(register.email);
//                    String charName = encoder.decryptSignedData(register.charName);
//
//                    Log.debug("login-server", "Register Request: ");
//                    Log.debug("login-server", "user: " + userName);
//                    Log.debug("login-server", "pass: "+ password);
//                    Log.debug("login-server", "email: "+ email);
//                    Log.debug("login-server", "char: "+ charName);
//
//                    // invalid data wont be registered, return db error
//                    if(!registrationIsValid(userName, password, email, charName)) {
//                        c.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DB_ERROR));
//                        return;
//                    }
//
//                    // checks for existing unique user data in database
//                    LoginRegister.Response.Type type = postgresDB.checkRegisterData(userName, email, charName);
//
//                    // only registers user if no unique fields are violated
//                    if(type == LoginRegister.Response.Type.USER_SUCCESSFULLY_REGISTERED) {
//                        if(postgresDB.registerUser(userName, password, email, charName)) // if insert was properly executed
//                            c.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.USER_SUCCESSFULLY_REGISTERED));
//                        else // else send db error indicating it
//                            c.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DB_ERROR));
//                    } else // send the response indicating that some unique data is duplicated (in order form user, email, charname)
//                        c.sendTCP(new LoginRegister.Response(type));

                    // Reject if the login is invalid.
//                    if (!isValid(register.name)) {
//                        c.close();
//                        return;
//                    }
//                    if (!isValid(register.otherStuff)) {
//                        c.close();
//                        return;
//                    }
//
//                    // Reject if character alread exists.
//                    if (loadCharacter(register.name) != null) {
//                        c.close();
//                        return;
//                    }
//
//                    character = new LoginRegister.Character();
//                    character.name = register.name;
//                    character.otherStuff = register.otherStuff;
//                    character.x = 0;
//                    character.y = 0;
//                    if (!saveCharacter(character)) {
//                        c.close();
//                        return;
//                    }
//
//                    loggedIn(connection, character);
//                    return;
                }

            }

            private boolean isValid (String value) {
                if (value == null) return false;
                value = value.trim();
                if (value.length() == 0) return false;
                return true;
            }

            public void disconnected (Connection c) {
                CharacterConnection connection = (CharacterConnection)c;
//                if (connection.character != null) {
//                    loggedIn.remove(connection.character);
//
//                    LoginRegister.RemoveCharacter removeCharacter = new LoginRegister.RemoveCharacter();
//                    removeCharacter.id = connection.character.id;
//                    server.sendToAllTCP(removeCharacter);
//                }
            }
        });

    }

    public void connect() {
        try {
            server.bind(LoginRegister.port);
        } catch (IOException e) {
            Log.info("login-server", "Could not bind port " + LoginRegister.port + " and start login server");
            isOnline = false;
            return;
        }
        // server online
        server.start();
        isOnline = true;
        // instantiate database controller that will act
        // on server requests related to the postgres database
        // and is responsible for the available database ops
        dbController = new DbController();
        //dbController.connect(); // connects to postgres database
        addListener(dbController); // adds db controller as listener to all login server requests

        Log.info("login-server", "Login Server is running!");
    }

    public static LoginServer getInstance() {
        if(instance == null)
            instance = new LoginServer();

        return instance;
    }

    public void stop() {
        if(isOnline) {
            // TODO: SAVE LOGIN STATE BEFORE ENDING?
            Log.info("login-server", "Login server is stopping...");
            server.stop();
            server.close();
            Log.info("login-server", "Login server has stopped!");
            isOnline = false;
            dbController.close();
            removeListener(dbController);
        } else
            Log.info("cmd", "Login server is not running!");
    }

    // process commands received via dispatcher
    @Override
    public void process(Command cmd) {
        switch (cmd.getType()) {
            case STOP:
            case RESTART:   // only stops server, will be restarted in UI module
                stop();
                break;
            default:
                break;
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public int getNumberOfPlayersOnline() {
        return loggedIn.size();
    }

    /**
     * Contains data from received request from clients
     * Including connection information and request content data
     */
    public class Request {
        private CharacterConnection conn;
        private Object content;
        public CharacterConnection getConnection() {return conn;}
        public Object getContent() {return content;}
        public Request(CharacterConnection conn, Object content) {
            this.conn = conn;
            this.content = content;
        }
    }

    // This holds per connection state.
    public static class CharacterConnection extends Connection {
        public DbController.CharacterLoginData charData;
    }

}
