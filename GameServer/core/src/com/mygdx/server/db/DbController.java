package com.mygdx.server.db;

import static com.mygdx.server.network.LoginRegister.Register.isValidAndFitEmail;
import static com.mygdx.server.network.LoginRegister.Register.isValidAndFitName;
import static com.mygdx.server.network.LoginRegister.Register.isValidAndFitPassword;
import static com.mygdx.server.network.LoginRegister.Register.isValidAndFitUser;

import com.esotericsoftware.minlog.Log;
import com.mygdx.server.network.LoginRegister;
import com.mygdx.server.network.LoginRegister.*;
import com.mygdx.server.network.LoginServer;
import com.mygdx.server.util.Encoder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

class PostgresDB {
    private Connection connection;
    private static PostgresDB instance;

    // constructor starts connection with db
    protected PostgresDB() {
        String jdbcUrl =  DatabaseConfig.getDbUrl();
        String username =  DatabaseConfig.getDbUsername();
        String password = DatabaseConfig.getDbPassword();

        // Register the PostgreSQL driver
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
        System.out.println("Opened database successfully");
    }

    /**
     * Register user into the database, hashing and salting the password before storing it
     * @param userName  the decrypted username
     * @param password  the decrypted password (raw) - will be hashed and salted
     * @param email     the decrypted email
     * @param charName  the decrypted character name
     * @return true if insert was executed correctly, false otherwise
     */
    public boolean registerUser(String userName, String password, String email, String charName) {
        // if not connected return
        if(connection == null) {
            Log.error("postgres", "Unable to do operation: Database not connected");
            return false;
        }

        String sql = "INSERT INTO users(username, password, email, character) VALUES(?,?,?,?)";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userName);
            pstmt.setString(2, Encoder.generateHash(password));
            pstmt.setString(3, email);
            pstmt.setString(4, charName);
            int i = pstmt.executeUpdate();
            pstmt.close();
            if(i > 0)
                return true; // insert was properly done
            else {
                Log.error("postgres", "Insert was not made in database");
                return false;
            }
        } catch (SQLException e) {
            Log.error("postgres", "Could not register user into database: "+e.getMessage());
            return false;
        }
    }

    /**
     * Checks if registration unique values (username, email, character name) already exist in db
     * @param userName  the username to check if it exists in db
     * @param email     the email to check if it exists in db
     * @param charName  the character name to check if it exist in db
     * @return a response indicating if user can be registered or if a unique field already exists
     *          in order of: username, email, character name
     */
    public Response.Type checkRegisterData(String userName, String email, String charName) {
        // if not connected return
        if(connection == null) {
            Log.error("postgres", "Unable to do operation: Database not connected");
            return Response.Type.DB_ERROR;
        }

        String sqlUser = "SELECT * FROM users WHERE LOWER(username)=LOWER(?)";
        String sqlEmail = "SELECT * FROM users WHERE LOWER(email)=LOWER(?)";
        String sqlChar = "SELECT * FROM users WHERE LOWER(character)=LOWER(?)";

        try {
            PreparedStatement pstmtUser = connection.prepareStatement(sqlUser);
            pstmtUser.setString(1, userName);
            PreparedStatement pstmEmail = connection.prepareStatement(sqlEmail);
            pstmEmail.setString(1, email);
            PreparedStatement pstmtChar = connection.prepareStatement(sqlChar);
            pstmtChar.setString(1, charName);

            if(pstmtUser.executeQuery().isBeforeFirst()) // user already exists
                return Response.Type.USER_ALREADY_REGISTERED;
            else if(pstmEmail.executeQuery().isBeforeFirst()) // email already exists
                return Response.Type.EMAIL_ALREADY_REGISTERED;
            else if(pstmtChar.executeQuery().isBeforeFirst()) // character name already exists
                return Response.Type.CHAR_ALREADY_REGISTERED;
            else
                return Response.Type.USER_SUCCESSFULLY_REGISTERED; // everything is ok, user can be registered
        } catch (SQLException e) {
            Log.error("postgres", "Could not query user into database: "+e.getMessage());
            return Response.Type.DB_ERROR;
        }
    }

    /**
     * Retrieve user from database if user is found and password matches with hashed salted verification
     * @param userName  the decrypted username
     * @param password  the decrypted password (raw) - will be hashed and salted
     * @return CharacterLoginData with user data if user found and password matches, null otherwise
     */
    public DbController.CharacterLoginData retrieveUser(String userName, String password) {
        // if not connected return
        if (connection == null) {
            Log.error("postgres", "Unable to do operation: Database not connected");
            return null;
        }

        // queries for user in database
        String sqlUser = "SELECT * FROM users WHERE LOWER(username)=LOWER(?)";
        String hashPass = null, character = null;
        int id = -1;
        try {
            PreparedStatement pstmtUser = connection.prepareStatement(sqlUser);
            pstmtUser.setString(1, userName);
            ResultSet rs = pstmtUser.executeQuery();
            if (rs.isBeforeFirst()) { // user found!!
                while (rs.next()) { // get values from db to create register data
                    id = rs.getInt("id");
                    character = rs.getString("character");
                    hashPass = rs.getString("password");
                }
            } else // user not found, return null indicating invalid credentials
                return null;
        } catch (SQLException e) {
            Log.error("postgres", "Could not query user into database: " + e.getMessage());
            return null;
        }

        // if user was found, check if password matches with hashed pass
        if (Encoder.verifyBCryptHash(password, hashPass)) { // password matches
            // builds user data into register class to return it
            return new DbController.CharacterLoginData(id, character);
        }
         return null;
    }

    // closes database connection, should be called on end
    public void close() {
        if(connection == null) return; // returns if there is no connection
        // Close the connection
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

class DatabaseConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("data/db.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties");
                System.exit(1);
            }
            // Load the properties file
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDbUrl() {

        return properties.getProperty("db.url");
    }

    public static String getDbUsername() {
        return properties.getProperty("db.username");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}

/**
 * DbController bridges the database operation for PostgresDB
 * Any available database operation to the outside classes
 * will be available through DbController methods.
 * Use getInstance to get a DbController instance (singleton)
 */
public class DbController implements PropertyChangeListener {
    private static DbController instance;
    private final Encoder encoder;
    private final PostgresDB postgresDb;
    private DbController() {
        encoder = new Encoder();
        postgresDb = new PostgresDB();
    }
    // returns DbController instance
    public static DbController getInstance() {
        if(instance == null)
            instance = new DbController();
        return instance;
    }

    /**
     * Tries to register user in the postgres database
     * @param request   the received client request containing user data
     */
    public void registerNewUserInDb(LoginServer.Request request) {
        com.esotericsoftware.kryonet.Connection conn = request.getConnection();
        Register register = (Register) request.getContent();

        String userName = encoder.decryptSignedData(register.userName);
        String password = encoder.decryptSignedData(register.password);
        String email = encoder.decryptSignedData(register.email);
        String charName = encoder.decryptSignedData(register.charName);

        Log.debug("login-server", "Register Request: ");
        Log.debug("login-server", "user: " + userName);
        Log.debug("login-server", "email: "+ email);
        Log.debug("login-server", "char: "+ charName);

        // invalid data wont be registered, return db error
        if(!registrationIsValid(userName, password, email, charName)) {
            conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DB_ERROR));
            return;
        }

        // checks for existing unique user data in database
        LoginRegister.Response.Type type = postgresDb.checkRegisterData(userName, email, charName);

        Log.debug("login-server", "Result: "+ type.name());

        // only registers user if no unique fields are violated
        if(type == LoginRegister.Response.Type.USER_SUCCESSFULLY_REGISTERED) { // user has not been registered yet but data is valid!!
            if(postgresDb.registerUser(userName, password, email, charName)) // if insert was properly executed - now user has been registered!
                conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.USER_SUCCESSFULLY_REGISTERED));
            else // else send db error indicating it
                conn.sendTCP(new LoginRegister.Response(LoginRegister.Response.Type.DB_ERROR));
        } else // send the response indicating that some unique data is duplicated (in order form user, email, charname)
            conn.sendTCP(new LoginRegister.Response(type));
    }

    private boolean registrationIsValid (String userName, String password, String email, String charName) {
        boolean dataIsValid = isValidAndFitName(charName) && isValidAndFitEmail(email) &&
                isValidAndFitUser(userName) && isValidAndFitPassword(password);
        return dataIsValid;
    }

    /**
     * Tries to login user authenticating it through postgres database
     * @param request   the received client request containing user data
     */
    public void loginUser(LoginServer.Request request) {
        LoginServer.CharacterConnection conn = request.getConnection();
        Login login = (Login) request.getContent();

        // decrypt data received from client
        String userName = encoder.decryptSignedData(login.userName);
        String password = encoder.decryptSignedData(login.password);

        Log.debug("login-server", "Login Request: ");
        Log.debug("login-server", "user: " + userName);

        // invalid data wont be considered, return invalid credentials
        if(!loginIsValid(userName, password)) {
            conn.sendTCP(new LoginRegister.Response(Response.Type.LOGIN_INVALID_CREDENTIALS));
            return;
        }

        // tries to retrieve user from database in case request contains valid data
        DbController.CharacterLoginData charData = postgresDb.retrieveUser(userName, password);

        if(charData != null) { // user was found and password matches!
            conn.sendTCP(new LoginRegister.Response(Response.Type.LOGIN_SUCCESSFUL)); // sends login success msg to client
            // TODO: SEND CHAR DATA TO GAME SERVER TO LOAD CHAR AND SEND CHAR GAME DATA TO CLIENT
            conn.charData = charData;
            // gameServer.loginChar(conn); // passes control to game server sending necessary char data
            Log.debug("login-server", "Result: LOGIN VALID FOR USER ID: "+ charData.id);
        } else { // user or password did not match
            conn.sendTCP(new LoginRegister.Response(Response.Type.LOGIN_INVALID_CREDENTIALS));
            Log.debug("login-server", "Result: "+ Response.Type.LOGIN_INVALID_CREDENTIALS.name());
        }

    }

    private boolean loginIsValid (String userName, String password) {
        boolean dataIsValid = isValidAndFitUser(userName) && isValidAndFitPassword(password);
        return dataIsValid;
    }

    /**
     * Method that reacts on client requests (database requests)
     * @param propertyChangeEvent   the client request encapsulated in PCE
     */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        // casts to server request, the default encapsulator of client requests
        LoginServer.Request request = (LoginServer.Request) propertyChangeEvent.getNewValue();
        // switches through the client different requests that this class listens to
        // calling the respective methods to react to the request accordingly
        switch(propertyChangeEvent.getPropertyName()) {
            case "registerRequest":
                registerNewUserInDb(request); // register users in db if everything is ok
                break;
            case "loginRequest":
                loginUser(request);  // tries to login user if everything is ok
                break;
            default:
                Log.debug("postgres", "Unknown client request to PostgresDb controller"
                                                        +propertyChangeEvent.getPropertyName());
                break;
        }
    }

    // just closes connection with postgres db
    public void close() {
        postgresDb.close(); // stops connection with db
    }

    // contains character login data from postgres database
    static public class CharacterLoginData {
        public int id;
        public String character;
        public CharacterLoginData(){}
        public CharacterLoginData(int id, String character){this.id = id; this.character = character;}
    }
}