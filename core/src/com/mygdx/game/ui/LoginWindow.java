package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.mygdx.game.RogueFantasy;

/**
 * A class that encapsulates the option menu window
 */
public class LoginWindow extends Window {
    private RogueFantasy game;
    private Stage stage;
    private Preferences prefs;
    private AssetManager manager;
    private Skin skin;
    private TextButton backBtn;
    private I18NBundle langBundle;
    private Label projectDescLabel;
    private Label usernameLabel;
    private Label passwordLabel;
    private TextField passwordTextField;
    private TextField userNameTextField;
    private CheckBox showPasswordCB;
    private CheckBox rememberCB;
    private TextButton loginBtn;

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
    public LoginWindow(RogueFantasy game, Stage stage, AssetManager manager, String title, Skin skin, String styleName) {
        super(title, skin, styleName);
        this.game = game;
        this.stage = stage;
        this.manager = manager;
        this.skin = skin;

        // gets language bundle from asset manager
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // label that describes the project name/version
        String descStr = " "+ langBundle.format("game") + " " + langBundle.format("version");
        final Label projectDescLabel = new Label(descStr, skin, "font8", Color.WHITE);

        // remember checkbox
        String rememberStr = " "+ langBundle.format("rememberLogin");
        rememberCB = new CheckBox(rememberStr, skin, "newCheckBoxStyle");

        // TODO: LOGIN - CRUD - DB
        // TODO: DO NOT ACCEPT SPECIAL CHARS IN USERNAME/FILTER SOME IN PASSWORD
        // TODO: ADJUST LOGIN MENU UP ON KEYBOARD OPEN - ON ANDROID
        // username label
        String usrStr = langBundle.format("username").substring(0, 1).toUpperCase() + langBundle.format("username").substring(1);
        final Label usernameLabel = new Label( usrStr + ": ", skin, "font8", Color.WHITE);
        usernameLabel.setAlignment(Align.center);
        // username text field
        userNameTextField = new TextField(prefs.getString("username", ""), skin, "newTextFieldStyle");
        userNameTextField.setMessageText(" "+ langBundle.format("username"));
        userNameTextField.setAlignment(Align.center);

        // password label
        String passStr = langBundle.format("password").substring(0, 1).toUpperCase() + langBundle.format("password").substring(1);
        final Label passwordLabel = new Label(passStr+": ", skin, "font8", Color.WHITE);
        passwordLabel.setAlignment(Align.center);
        // password text field in password mode.
        passwordTextField = new TextField("", skin, "newTextFieldStyle");
        passwordTextField.setMessageText(" "+ langBundle.format("password"));
        passwordTextField.setAlignment(Align.center);
        passwordTextField.setPasswordCharacter('*');
        passwordTextField.setPasswordMode(true);

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
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.getTitleTable().padBottom(6);
        this.setMovable(false);
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        this.add(projectDescLabel).colspan(0);
        this.row();
        this.add(usernameLabel).colspan(1);
        this.add(userNameTextField).minWidth(213).expandX().fillX().colspan(1);
        this.row();
        this.add(passwordLabel).colspan(1);
        this.add(passwordTextField).minWidth(213).expandX().fillX().colspan(1);
        this.row();
        this.add(rememberCB).colspan(1);
        this.add(showPasswordCB).colspan(1);
        this.row();
        this.add(backBtn).minWidth(182).colspan(1);
        this.add(loginBtn).minWidth(182).colspan(1);
        this.row();
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new loginController();
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
            // back button listener
            backBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    backBtnOnClick(event, x, y);
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
                    loginBtnOnClick(event, x, y);
                }
            });
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

        // Makes username field acccept alphanumeric and digits only
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
        private void backBtnOnClick(InputEvent event, float x, float y) {
            remove(); // removes option window
        }

        // called when login button is pressed - try to login
        private void loginBtnOnClick(InputEvent event, float x, float y) {
            // localized strings
            final String[] infoLogin = {langBundle.format("loginSuccess")};
            final String[] infoLoginBtn = {langBundle.format("ok")};

            // styles from the skin
            Label.LabelStyle lStyle = skin.get("newLabelStyle", Label.LabelStyle.class);
            TextButton.TextButtonStyle tbStyle = skin.get("newTextButtonStyle", TextButton.TextButtonStyle.class);


            if (userNameTextField.getText().length() == 0 || passwordTextField.getText().length() == 0) {
                infoLogin[0] = langBundle.format("loginWrongInfo");
                infoLoginBtn[0] = langBundle.format("back");
            } else {
                infoLogin[0] = langBundle.format("loginSuccess");
                infoLoginBtn[0] = langBundle.format("ok");
            }
            System.out.println("Login with Following Credentials: " + userNameTextField.getText() + "/" + passwordTextField.getText());


            Dialog infoDialog = new Dialog(langBundle.format("loginInfoDialog"), skin, "newWindowStyle")
                    .text(infoLogin[0], lStyle).button(infoLoginBtn[0], skin, tbStyle).key(Input.Keys.ENTER, true)
                    .key(Input.Keys.ESCAPE, false);

            infoDialog.getTitleTable().padBottom(3).padLeft(2);
            infoDialog.show(stage);
        }
    }
}
