package com.mygdx.game.ui;

import static com.mygdx.game.network.LoginRegister.Register.isValidAndFitEmail;
import static com.mygdx.game.network.LoginRegister.Register.isValidAndFitName;
import static com.mygdx.game.network.LoginRegister.Register.isValidAndFitPassword;
import static com.mygdx.game.network.LoginRegister.Register.isValidAndFitUser;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.network.LoginRegister;
import com.mygdx.game.util.Encoder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A class that encapsulates the option menu window
 */
public class LoginWindow extends GameWindow implements PropertyChangeListener {
    private Label projectDescLabel;
    private Label usernameLabel;
    private Label passwordLabel;
    private TypingLabel forgotPwLabel;
    private TypingLabel registerLinkLabel;
    private TextField passwordTextField;
    private TextField userNameTextField;
    private CheckBox showPasswordCB;
    private CheckBox rememberCB;
    private TextButton backBtn;
    private TextButton loginBtn;
    private Dialog infoDialog;
    public float txtFieldOffsetY;
    private float originalY;

    /**
     * Builds the option window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public LoginWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // makes sure title is in the correct language
        this.getTitleLabel().setText(" "+langBundle.format("loginWindowTitle"));

        // label that describes the project name/version
        String descStr = " "+ langBundle.format("game") + " " + langBundle.format("version");
        projectDescLabel = new Label(descStr, skin, "fontMedium", Color.WHITE);
        projectDescLabel.setAlignment(Align.center);

        // remember checkbox
        String rememberStr = " "+ langBundle.format("rememberLogin");
        rememberCB = new CheckBox(rememberStr, skin, "newCheckBoxStyle");

        // TODO: LOGIN - CRUD - DB
        // TODO: DO NOT ACCEPT SPECIAL CHARS IN USERNAME/FILTER SOME IN PASSWORD
        // TODO: ADJUST LOGIN MENU UP ON KEYBOARD OPEN - ON ANDROID
        // username label
        String usrStr = langBundle.format("username").substring(0, 1).toUpperCase() + langBundle.format("username").substring(1);
        usernameLabel = new Label( usrStr + ": ", skin, "fontMedium", Color.WHITE);
        usernameLabel.setAlignment(Align.center);
        // username text field
        userNameTextField = new TextField(prefs.getString("username", ""), skin, "newTextFieldStyle");
        userNameTextField.setMessageText(" "+ langBundle.format("username"));
        userNameTextField.setAlignment(Align.center);

        // password label
        String passStr = langBundle.format("password").substring(0, 1).toUpperCase() + langBundle.format("password").substring(1);
        passwordLabel = new Label(passStr+": ", skin, "fontMedium", Color.WHITE);
        passwordLabel.setAlignment(Align.center);
        // password text field in password mode.
        passwordTextField = new TextField("", skin, "newTextFieldStyle");
        passwordTextField.setMessageText(" "+ langBundle.format("password"));
        passwordTextField.setAlignment(Align.center);
        passwordTextField.setPasswordCharacter('*');
        passwordTextField.setPasswordMode(true);

        // forgot password label with different color
        // TODO: FORGOT PASSWORD RECOVERY
        // TODO: INSTANT TEXT WITH NO EFFECT WITH TYPIST , HOW?
        forgotPwLabel = new TypingLabel("{SIZE=95%}{COLOR=SKY}[_]"+langBundle.format("forgotPassword"), iconFont);
        //forgotPwLabel.setFontScale(0.9f);
        forgotPwLabel.setAlignment(Align.center);
        forgotPwLabel.skipToTheEnd();

        // register account link label
        registerLinkLabel = new TypingLabel("{SPEED=1.5}"+langBundle.format("dontHaveAccount") +
                " {COLOR=SKY}[_]" + langBundle.format("register"), iconFont);
        //registerLinkLabel.setFontScale(0.9f);
        registerLinkLabel.setAlignment(Align.center);
        registerLinkLabel.skipToTheEnd();

        // remember me checkbox to choose the option to store login credentials
        // TODO: STORE LOGIN CREDENTIALS - SEND PREFERENCES TO BUILD WINDOW WITH PREVIOUS DATA
        // TODO: DO THE SAME WHEN CHANGING LANGUAGES
        rememberCB.getImage().setScaling(Scaling.fill);
        rememberCB.getImageCell().size(26);
        rememberCB.getImageCell().padTop(6);
        rememberCB.setChecked(prefs.getBoolean("remember", false));

        // show password check box
        String showPasswordString = " "+ langBundle.format("showPassword");
        showPasswordCB = new CheckBox(showPasswordString, skin, "newCheckBoxStyle");
        showPasswordCB.setChecked(false);
        showPasswordCB.getImage().setScaling(Scaling.fill);
        showPasswordCB.getImageCell().size(26);
        showPasswordCB.getImageCell().padTop(6);

        // login button
        loginBtn = new TextButton(langBundle.format("login"), skin);

        // back button
        backBtn = new TextButton(langBundle.format("back"), skin);

        // builds options window
        this.getTitleTable().padBottom(6);
        this.defaults().spaceBottom(10).padRight(22).padLeft(1).padBottom(10).minWidth(320);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.setMovable(false);
        this.add(projectDescLabel).colspan(2);
        this.row();
        this.add(usernameLabel).colspan(1);
        this.add(userNameTextField).minWidth(324).colspan(1);
        this.row();
        this.add(passwordLabel).colspan(1);
        this.add(passwordTextField).minWidth(324).colspan(1);
        this.row();
        this.add(rememberCB).colspan(1);
        this.add(showPasswordCB).colspan(1);
        this.row();
        this.add(backBtn).minWidth(182).colspan(1).spaceTop(25);
        this.add(loginBtn).minWidth(182).colspan(2).spaceTop(25);
        this.row();
        this.add(forgotPwLabel).colspan(2).spaceTop(25);
        this.row();
        this.add(registerLinkLabel).colspan(2).spaceTop(24);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new loginController();

        originalY = this.getY();

        // if its not listening to login responses, start listening to it
        if(!loginClient.isListening("loginResponse", this))
            loginClient.addListener("loginResponse", this);
    }

    @Override
    public void resize(int width, int height) {
        if(infoDialog != null)
            infoDialog.setPosition( stage.getWidth() / 2f - infoDialog.getWidth() / 2f, stage.getHeight() / 2f - infoDialog.getHeight() / 2f );
    }

    /**
     * to be able to access and communicate with servers
     */
    public void startServerListening(LoginClient loginClient, Encoder encoder) {
        this.loginClient = loginClient; this.encoder = encoder;
        // if its not listening to login responses, start listening to it
        if(!loginClient.isListening("loginResponse", this))
            loginClient.addListener("loginResponse", this);
        if(!loginClient.isListening("tokenResponse", this))
            loginClient.addListener("tokenResponse", this);
    }

    /**
     * cut communication with server property changes
     */
    public void stopServerListening() {
        // if its listening to login responses, stops listening to it
        if(loginClient.isListening("loginResponse", this))
            loginClient.removeListener("loginResponse", this);
        if(loginClient.isListening("tokenResponse", this))
            loginClient.removeListener("tokenResponse", this);
    }

    @Override
    public void softKeyboardClosed() {
        float deltaY = txtFieldOffsetY - MainMenuScreen.chatOffsetY;
        if(deltaY < 0) // keyboard obfuscates
            moveBy(0, deltaY);
    }

    @Override
    public void softKeyboardOpened() {
        //Gdx.app.log("keyboardtst", "wtf " + String.valueOf(txtFieldOffsetY));
        float deltaY = txtFieldOffsetY - MainMenuScreen.chatOffsetY;
        if(deltaY < 0) // keyboard obfuscates
            moveBy(0, -deltaY);
    }

    @Override
    public void reloadLanguage() {

    }

    /**
     * Method that reacts on server responses (login responses)
     * NOTE:  Gdx.app.postRunnable(() makes it thread-safe with libGDX UI
     * @param propertyChangeEvent   the server response encapsulated in PCE
     */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        Gdx.app.postRunnable(() -> {
            if(propertyChangeEvent.getPropertyName().equals("loginResponse")) { // received a login response
                LoginRegister.Response response = (LoginRegister.Response) propertyChangeEvent.getNewValue();
                if(infoDialog != null && infoDialog.getStage() != null) infoDialog.remove(); // removes any dialog
                switch (response.type) { // adapts info dialog accordingly to server response
                    case LOGIN_SUCCESSFUL: // char already registered
                        // TODO: LOGIN SUCESSFULLY MADE - GET USER DATA AND LOAD GAME SCREEN
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("contactingServerTitle"),
                                langBundle.format("loadingCharContent"), true, false,
                                prefs.getInteger("defaultMaxTimeOutValue", 15000) / 1000f);
                        break;
                    case USER_ALREADY_LOGGED_IN:
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("contactingServerTitle"),
                                langBundle.format("userAlreadyLoggedInContent"), false, true);
                        break;
                    case GAME_SERVER_OFFLINE:
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("contactingServerTitle"),
                                langBundle.format("gameServerOfflineContent"), false, true);
                        break;
                    default:
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("loginWrongInfo"),
                            langBundle.format("invalidLoginContent"), false, true);
                        break;
                }
            } else if(propertyChangeEvent.getPropertyName().equals("tokenResponse")) { // login authorized, received token to connect to game server
                LoginRegister.Token token = (LoginRegister.Token) propertyChangeEvent.getNewValue();
                if(encoder == null) encoder = Encoder.getInstance();
                String decryptedToken = encoder.decryptSignedData(token.token);
                if(parent instanceof MainMenuScreen) { // perform change screen from menu to game screen
                    ((MainMenuScreen) parent).update(getInstance(), false, CommonUI.ScreenCommands.DISPOSE);
                    game.setScreen(new LoadScreen("game", manager, decryptedToken));
                }
            }
        });
    }

    /**
     * A nested class that controls the option window
     */
    class loginController {
        // constructor adds listeners, filters etc to the actors
        public loginController() {
            // adds the username filter
            userNameTextField.setTextFieldFilter((textField, c) -> {return userNameFilter(textField, c);});
            // adds listener to username typing, that calls for each input
            userNameTextField.setTextFieldListener((textField, c) -> {onUsernameInput(textField, c);});
            // adds the password filter
            passwordTextField.setTextFieldFilter((textField, c) -> {return passwordFilter(textField, c);});

            // txt fields click listeners
            userNameTextField.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if(Gdx.app.getType() == Application.ApplicationType.Android && !RogueFantasy.isKeyboardShowing())
                        txtFieldOffsetY = userNameTextField.getY() + getY();
                    return false;
                }
            });

            passwordTextField.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if(Gdx.app.getType() == Application.ApplicationType.Android && !RogueFantasy.isKeyboardShowing())
                        txtFieldOffsetY = passwordTextField.getY() + getY();
                    return false;
                }
            });

            // back button listener
            backBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    backBtnOnClick();
                }
            });
            // remember check box listener
            rememberCB.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    onRememberCheckBoxClicked(event, actor);
                }
            });
            // show password check box listener
            showPasswordCB.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    onShowPasswordClicked(event, actor);
                }
            });
            // login button listener
            loginBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    loginBtnOnClick();
                }
            });
            forgotPwLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    forgotPwOnClick(event, x, y);
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
            });
            registerLinkLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    registerLinkOnClick(event, x, y);
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
            });
            getInstance().addListener(new InputListener(){ //  for keyboard shortcuts
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                        loginBtnOnClick();
                    } else if (keycode == Input.Keys.ESCAPE) {
                        backBtnOnClick();
                    } else if (keycode == Input.Keys.TAB) {
                        if(stage.getKeyboardFocus().getClass() != userNameTextField.getClass())
                            stage.setKeyboardFocus(userNameTextField);
                    }
                    return false;
                }
            });
        }

        // called when register link is clicked, loads register window
        private void registerLinkOnClick(InputEvent event, float x, float y) {
            // calls parent screen update method to properly switch to register window
            if(parent instanceof MainMenuScreen) {
                ((MainMenuScreen) parent).update(getInstance(), false, CommonUI.ScreenCommands.LOAD_REGISTER_WINDOW);
            }
        }

        // called when forgot password is clicked, gives user option to recover
        // TODO: IMPLEMENT FORGOT PASSWORD RECOVERY
        private void forgotPwOnClick(InputEvent event, float x, float y) {
            //new GameClient();
        }

        // toggle password visibility
        private void onShowPasswordClicked(ChangeListener.ChangeEvent event, Actor actor) {
            passwordTextField.setPasswordMode(!showPasswordCB.isChecked());
        }

        // stores username if remember me is checked
        private void onRememberCheckBoxClicked(ChangeListener.ChangeEvent event, Actor actor) {
            if(rememberCB.isChecked()) {
                prefs.putString("username", userNameTextField.getText());
                prefs.putBoolean("remember", true);
            } else { // clears pref
                prefs.remove("username");
                prefs.remove("remember");
            }
            // updates preferences
            prefs.flush();
        }

        // Makes username field accept alphanumeric and digits only
        private boolean userNameFilter(TextField textField, char c) {
            if (Character.toString(c).matches("^[a-zA-Z0-9]"))
                return true;
            return false;
        }

        // Accepts alphanumeric, digits and some special characters that increases password strength
        private boolean passwordFilter(TextField textField, char c) {
            if (Character.toString(c).matches("^[a-zA-Z0-9!@#$%^&*?/.;:_,-]"))
                return true;
            return false;
        }

        // updates stored username if it remember cb is checked
        private void onUsernameInput(TextField textField, char c) {
            if(rememberCB.isChecked()) {
                prefs.putString("username", userNameTextField.getText());
                prefs.flush();
            }
        }

        // called when back button is pressed
        private void backBtnOnClick() {
            remove(); // removes option window
        }

        // called when login button is pressed - tries to login
        private void loginBtnOnClick() {
            // unfocus everything on stage
            stage.unfocusAll();
            // gets data to validate it before sending it to the server
            String userName = userNameTextField.getText().replaceFirst("\\s++$", "");
            String password = passwordTextField.getText().replaceFirst("\\s++$", "");

            // checks if any of the data is empty
            boolean hasEmptyField = userName == "" || password == "";

            // checks if all data is valid
            boolean dataIsValid = isValidAndFitUser(userName) && isValidAndFitPassword(password);

            // if any of the field is empty, show respective info dialog and return
            if (hasEmptyField) {
                infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("emptyFieldTitle"),
                        langBundle.format("emptyFieldContent"), false, true);
                infoDialog.show(stage);
                return;
            }
            // if data is not all valid, show respective info dialog and return
            if (!dataIsValid) {
                infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("loginWrongInfo"),
                        langBundle.format("invalidLoginContent"), false, true);
                infoDialog.show(stage);
                return;
            }
            // only after data is validated it is sent to the server to check if user/password exist
            // encrypting and signing it before sending it to the login server

            // shows a dialog informing the contacting of the server (to be closed when server responds or with timeout)
            infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("contactingServerTitle"),
                    langBundle.format("contactingServerContent"), true, false,
                    prefs.getInteger("defaultMaxTimeOutValue", 15000) / 1000f);

            // encrypts and sends to server from another thread
            sendLoginAsync(userName, password);
        }

        // creates a thread to encrypt and send user data for login request
        private void sendLoginAsync(String userName, String password) {
            // Encrypt and send to server on another thread
            new Thread(() -> {
                // asynchronously to the rendering thread
                if(!loginClient.isConnected())
                    loginClient.connect();
                // sign and encrypt data
                encoder = Encoder.getInstance();
                byte[] encryptedPass = encoder.signAndEncryptData(password);
                byte[] encryptedUser = encoder.signAndEncryptData(userName);
                LoginRegister.Login login = new LoginRegister.Login();
                login.userName = encryptedUser;
                login.password = encryptedPass;
                if(loginClient.isConnected()) // sends attempt if login client is connected
                    loginClient.sendLoginAttempt(login);
                // processes the result in a thread safe way with libgdx UI
                Gdx.app.postRunnable(() -> {
                    if(!loginClient.isConnected()) { // connection has failed
                        if(infoDialog!= null && infoDialog.getStage() != null) infoDialog.remove(); // removes waiting dialog
                        // show connection failed dialog
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("connectionRefusedContent"), false, true);
                        infoDialog.show(stage);
                    }
                });
            }).start();
        }
    }

    private LoginWindow getInstance() {
        return this;
    }
}
