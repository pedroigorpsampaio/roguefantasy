package com.mygdx.server.network;

import static com.mygdx.server.network.LoginRegister.Register.*;

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
import com.mygdx.server.db.PostgresDB;
import com.mygdx.server.ui.CommandDispatcher.CmdReceiver;
import com.mygdx.server.ui.CommandDispatcher.Command;
import com.mygdx.server.util.Encoder;


/**
 * LOGIN SERVER
 * Server that deals with login, registration, character creation
 * and other communications related to the authentication of the user
 */
public class LoginServer implements CmdReceiver {
    private final Encoder encoder;
    Server server;
    HashSet<LoginRegister.Character> loggedIn = new HashSet();
    private boolean isOnline = false; // is this server online?
    PostgresDB postgresDB;

    public LoginServer() throws IOException {
        server = new Server(65535, 65535) {
            protected Connection newConnection () {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new CharacterConnection();
            }
        };

        postgresDB = new PostgresDB();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        LoginRegister.register(server);

        encoder = new Encoder();

        server.addListener(new Listener() {
            public void received (Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                CharacterConnection connection = (CharacterConnection)c;
                LoginRegister.Character character = connection.character;

                if (object instanceof LoginRegister.Login) {
                    // Ignore if already logged in.
                    if (character != null) return;

                    // Reject if the name is invalid.
                    String name = ((LoginRegister.Login)object).name;
                    if (!isValid(name)) {
                        c.close();
                        return;
                    }

                    // Reject if already logged in.
                    for (LoginRegister.Character other : loggedIn) {
                        if (other.name.equals(name)) {
                            c.close();
                            return;
                        }
                    }

                    character = loadCharacter(name);

                    // Reject if couldn't load character.
                    if (character == null) {
                        c.sendTCP(new LoginRegister.RegistrationRequired());
                        return;
                    }

                    loggedIn(connection, character);
                    return;
                }

                if (object instanceof LoginRegister.Register) {
                    // Ignore if already logged in.
                    //if (character != null) return;

                    LoginRegister.Register register = (LoginRegister.Register)object;
                    String userName = encoder.decryptSignedData(register.userName);
                    String password = encoder.decryptSignedData(register.password);
                    String email = encoder.decryptSignedData(register.email);
                    String charName = encoder.decryptSignedData(register.charName);

                    Log.debug("login-server", "Register Request: ");
                    Log.debug("login-server", "user: " + userName);
                    Log.debug("login-server", "pass: "+ password);
                    Log.debug("login-server", "email: "+ email);
                    Log.debug("login-server", "char: "+ charName);

                    // invalid data wont be registered, return db error
                    if(!registrationIsValid(userName, password, email, charName)) {
                        c.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DB_ERROR));
                        return;
                    }

                    c.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.USER_SUCCESSFULLY_REGISTERED));

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

            private boolean registrationIsValid (String userName, String password, String email, String charName) {
                boolean dataIsValid = isValidAndFitName(charName) && isValidAndFitEmail(email) &&
                        isValidAndFitUser(userName) && isValidAndFitPassword(password);
                return dataIsValid;
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
        try {
            server.bind(LoginRegister.port);
        } catch (IOException e) {
            Log.info("login-server", "Could not bind port " + LoginRegister.port + " and start login server");
            return;
        }
        // server online
        server.start();
        isOnline = true;
        Log.info("login-server", "Login Server is running!");
    }

    void loggedIn (CharacterConnection c, LoginRegister.Character character) {
//        c.character = character;
//
//        // Add existing characters to new logged in connection.
//        for (LoginRegister.Character other : loggedIn) {
//            LoginRegister.AddCharacter addCharacter = new LoginRegister.AddCharacter();
//            addCharacter.character = other;
//            c.sendTCP(addCharacter);
//        }
//
//        loggedIn.add(character);
//
//        // Add logged in character to all connections.
//        LoginRegister.AddCharacter addCharacter = new LoginRegister.AddCharacter();
//        addCharacter.character = character;
//        server.sendToAllTCP(addCharacter);
    }

    boolean saveCharacter (LoginRegister.Character character) {
        File file = new File("characters", character.name.toLowerCase());
        file.getParentFile().mkdirs();

        if (character.id == 0) {
            String[] children = file.getParentFile().list();
            if (children == null) return false;
            character.id = children.length + 1;
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(file));
            output.writeInt(character.id);
            output.writeUTF(character.otherStuff);
            output.writeInt(character.x);
            output.writeInt(character.y);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }

    LoginRegister.Character loadCharacter (String name) {
        File file = new File("characters", name.toLowerCase());
        if (!file.exists()) return null;
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(file));
            LoginRegister.Character character = new LoginRegister.Character();
            character.id = input.readInt();
            character.name = name;
            character.otherStuff = input.readUTF();
            character.x = input.readInt();
            character.y = input.readInt();
            input.close();
            return character;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (input != null) input.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void stop() {
        if(isOnline) {
            // TODO: SAVE LOGIN STATE BEFORE ENDING?
            Log.info("login-server", "Login server is stopping...");
            server.stop();
            server.close();
            Log.info("login-server", "Login server has stopped!");
            isOnline = false;
            postgresDB.close(); // stops connection with db
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

    // This holds per connection state.
    static class CharacterConnection extends Connection {
        public LoginRegister.Character character;
    }

}
