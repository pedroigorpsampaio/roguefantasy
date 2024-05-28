package com.mygdx.game.ui;

import static com.mygdx.game.network.LoginRegister.Register.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TypingLabel;
import com.github.tommyettinger.textra.TypingListener;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.network.LoginRegister;
import com.mygdx.game.util.Encoder;
import com.mygdx.game.util.NameGenerator;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;

/**
 * A class that encapsulates the option menu window
 * implements PropertyChangeListener to listen to login server messages
 */
public class RegisterWindow extends GameWindow implements PropertyChangeListener {
    private TypingLabel usernameLabel;
    private TypingLabel passwordLabel;
    private Label regsiterTitleLabel;
    private TypingLabel emailLabel;
    private TypingLabel charLabel;
    private TypingLabel infoCharLabel;
    private TypingLabel infoUserLabel;
    private TypingLabel infoEmailLabel;
    private TypingLabel infoPassLabel;
    private TextField charTextField;
    private TextField emailTextField;
    private TextField passwordTextField;
    private TextField userNameTextField;
    private CheckBox showPasswordCB;
    private TextButton backBtn;
    private TextButton registerBtn;
    private String usrStr;
    private String charStr;
    private String passStr;
    private String emailStr;
    private TypingLabel suggestNameLabel;
    private NameGenerator nameGenerator;
    private TypingLabel tosLabel;
    private CheckBox tosCB;
    private Dialog infoDialog;

    /**
     * Builds the register window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public RegisterWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
        super(game, stage, parent, manager, title, skin, styleName);
    }

    @Override
    public void build() {
        // makes sure window is clear and not in stage before building it
        this.clear();
        this.remove();

        // makes sure language is up to date with current selected option
        langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // initializes name generator
        nameGenerator = new NameGenerator("data/names_set_1.txt", 1, 4);

        // makes sure title is in the correct language
        this.getTitleLabel().setText(" "+langBundle.format("register"));

        // label with register form title
        String descStr = langBundle.format("createAccount");
        regsiterTitleLabel = new Label(descStr, skin, "fontMedium", Color.WHITE);
        regsiterTitleLabel.setAlignment(Align.center);

        // username label
        usrStr = langBundle.format("username").substring(0, 1).toUpperCase() + langBundle.format("username").substring(1);
        usernameLabel = new TypingLabel( usrStr + ":{SIZE=80%}{COLOR=GRAY}[+white square button]{ENDCOLOR}{STYLE=%}", emojiFont);
        usernameLabel.skipToTheEnd();
        usernameLabel.setAlignment(Align.left);
        // username text field
        userNameTextField = new TextField("", skin, "newTextFieldStyle");
        userNameTextField.setMessageText(" "+ langBundle.format("username"));
        userNameTextField.setAlignment(Align.left);

        // email label
        emailStr = langBundle.format("email").substring(0, 1).toUpperCase() + langBundle.format("email").substring(1);
        emailLabel = new TypingLabel( emailStr + ":{SIZE=80%}{COLOR=GRAY}[+white square button]{ENDCOLOR}{STYLE=%}", emojiFont);
        emailLabel.skipToTheEnd();
        emailLabel.setAlignment(Align.left);
        // email text field
        emailTextField = new TextField("", skin, "newTextFieldStyle");
        emailTextField.setMessageText(" "+ langBundle.format("email"));
        emailTextField.setAlignment(Align.left);

        // password label
        passStr = langBundle.format("password").substring(0, 1).toUpperCase() + langBundle.format("password").substring(1);
        passwordLabel =  new TypingLabel( passStr + ":{SIZE=80%}{COLOR=GRAY}[+white square button]{ENDCOLOR}{STYLE=%}", emojiFont);
        passwordLabel.skipToTheEnd();
        passwordLabel.setAlignment(Align.left);
        // password text field in password mode.
        passwordTextField = new TextField("", skin, "newTextFieldStyle");
        passwordTextField.setMessageText(" "+ langBundle.format("password"));
        passwordTextField.setAlignment(Align.left);
        passwordTextField.setPasswordCharacter('*');
        passwordTextField.setPasswordMode(true);

        // character name label
        charStr = langBundle.format("char").substring(0, 1).toUpperCase() + langBundle.format("char").substring(1);
        charLabel = new TypingLabel( charStr + ":{SIZE=80%}{COLOR=GRAY}[+white square button]{ENDCOLOR}{STYLE=%}", emojiFont);
        charLabel.skipToTheEnd();
        charLabel.setAlignment(Align.left);
        // character name text field
        charTextField = new TextField("", skin, "newTextFieldStyle");
        charTextField.setMessageText(" "+ langBundle.format("char"));
        charTextField.setAlignment(Align.left);

        // show password check box
        String showPasswordString = " "+ langBundle.format("showPassword");
        showPasswordCB = new CheckBox(showPasswordString, skin, "newCheckBoxStyle");
        showPasswordCB.setChecked(false);
        showPasswordCB.getImage().setScaling(Scaling.fill);
        showPasswordCB.align(Align.left);
        showPasswordCB.getImageCell().size(26);
        showPasswordCB.getImageCell().padTop(6);

        // info message labels
        infoCharLabel = new TypingLabel("", emojiFont);
        infoCharLabel.setWrap(true);
        infoCharLabel.setAlignment(Align.left);
        infoUserLabel = new TypingLabel("", emojiFont);
        infoUserLabel.setWrap(true);
        infoUserLabel.setAlignment(Align.left);
        infoPassLabel = new TypingLabel("", emojiFont);
        infoPassLabel.setWrap(true);
        infoPassLabel.setAlignment(Align.left);
        infoEmailLabel = new TypingLabel("", emojiFont);
        infoEmailLabel.setWrap(true);
        infoEmailLabel.setAlignment(Align.left);

        // Suggest name label
        suggestNameLabel = new TypingLabel("{SIZE=97%}{GRADIENT=BROWN;GOLD}[+pencil] "+langBundle.format("suggestName")
                                                    +"{ENDGRADIENT}{STYLE=%}", emojiFont);
        suggestNameLabel.setAlignment(Align.right);
        suggestNameLabel.skipToTheEnd();

        // ToS check box
        tosCB = new CheckBox("", skin, "newCheckBoxStyle");
        tosCB.setChecked(false);
        tosCB.getImage().setScaling(Scaling.fill);
        tosCB.align(Align.left);
        tosCB.getImageCell().size(26);
        // ToS label
        String termsStr = "{COLOR=SKY}{TRIGGER=terms}"+langBundle.format("terms")+"{ENDCOLOR}{ENDTRIGGER}";
        String rulesStr = "{COLOR=SKY}{TRIGGER=rules}"+langBundle.format("rules")+"{ENDCOLOR}{ENDTRIGGER}";
        String privacyStr = "{COLOR=SKY}{TRIGGER=privacy}"+langBundle.format("privacy")+"{ENDCOLOR}{ENDTRIGGER}";
        tosLabel = new TypingLabel("{SIZE=98%}"+langBundle.format("tosAgreement", termsStr, rulesStr, privacyStr)+"{STYLE=%}", emojiFont);
        tosLabel.setWrap(true);
        tosLabel.setAlignment(Align.left);
        tosLabel.skipToTheEnd();

        // back button
        backBtn = new TextButton(langBundle.format("back"), skin);

        // register button
        registerBtn = new TextButton(langBundle.format("register"), skin);

        // builds options window
        this.getTitleTable().padBottom(6);
        this.defaults().spaceBottom(10).padLeft(22).padRight(1).padBottom(2).minWidth(600);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.setMovable(false);
        // creates a table that will contain scrollable widgets
        final Table scrollTable = new Table();
        scrollTable.add(regsiterTitleLabel).colspan(2).padRight(20).padBottom(5);
        scrollTable.row();
        scrollTable.add(usernameLabel).colspan(2).align(Align.left).padTop(5);
        scrollTable.row();
        scrollTable.add(userNameTextField).minWidth(524).colspan(2).fillX().padRight(20);
        scrollTable.row();
        scrollTable.add(infoUserLabel).colspan(2).padTop(10).fillX().left();
        scrollTable.row();
        scrollTable.add(emailLabel).colspan(1).align(Align.left).padTop(10);
        scrollTable.row();
        scrollTable.add(emailTextField).minWidth(324).colspan(2).fillX().padRight(20);
        scrollTable.row();
        scrollTable.add(infoEmailLabel).colspan(2).padTop(10).fillX().left();
        scrollTable.row();
        scrollTable.add(passwordLabel).colspan(1).align(Align.left).padTop(20);
        scrollTable.row();
        scrollTable.add(passwordTextField).minWidth(324).colspan(2).fillX().padRight(20);
        scrollTable.row();
        scrollTable.add(infoPassLabel).colspan(2).padTop(10).fillX().left();
        scrollTable.row();
        scrollTable.add(showPasswordCB).colspan(2).align(Align.right).padRight(20);
        scrollTable.row();
        scrollTable.add(charLabel).colspan(1).align(Align.left);
        scrollTable.row();
        scrollTable.add(charTextField).minWidth(324).colspan(2).fillX().padRight(20);
        scrollTable.row();
        scrollTable.add(suggestNameLabel).colspan(2).align(Align.center).padTop(15).padRight(35);
        scrollTable.row();
        scrollTable.add(infoCharLabel).colspan(2).padTop(10).padBottom(15).fillX().left();
        scrollTable.row();
        scrollTable.add(tosCB).colspan(0).padTop(20).right().top();
        scrollTable.add(tosLabel).colspan(2).padLeft(28).grow().left().top();
        scrollTable.row();
        scrollTable.add(backBtn).minWidth(182).spaceTop(35).padRight(20).padBottom(10).align(Align.left);
        scrollTable.add(registerBtn).minWidth(182).spaceTop(35).padRight(25).padBottom(10).align(Align.right);
        //scrollTable.add(registerBtn).minWidth(182).spaceTop(95).colspan(1).align(Align.left);
        scrollTable.row();
        scrollTable.pack();
        // uses scrollpane to make table scrollable
        final ScrollPane scroller = new ScrollPane(scrollTable, skin);
        scroller.setFadeScrollBars(false);
        scroller.setScrollbarsOnTop(true);
        scroller.setSmoothScrolling(true);
        scroller.setupOverscroll(0,0,0);
        // finishes adding the scrollable table to the window
        float uiZoomFactor = 720f / Gdx.graphics.getHeight();
        this.add(scroller).size(610, 0.94f*Gdx.graphics.getHeight()*uiZoomFactor);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new RegisterController();
    }

    /**
     * to be able to access and communicate with servers
     */
    public void startServerListening(LoginClient loginClient, Encoder encoder) {
        this.loginClient = loginClient; this.encoder = encoder;
        // if its not listening to registration responses, start listening to it
        if(!loginClient.isListening("registrationResponse", this))
            loginClient.addListener("registrationResponse", this);
    }

    /**
     * cut communication with server property changes
     */
    public void stopServerListening() {
        // if its listening to registration responses, stops listening to it
        if(loginClient.isListening("registrationResponse", this))
            loginClient.removeListener("registrationResponse", this);
    }

    // resizes info dialogs on game resize
    @Override
    public void resize(int width, int height) {
        if(infoDialog != null)
            infoDialog.setPosition( stage.getWidth() / 2f - infoDialog.getWidth() / 2f, stage.getHeight() / 2f - infoDialog.getHeight() / 2f );
    }

    /**
     * Method that reacts on server responses (registration responses)
     * NOTE:  Gdx.app.postRunnable(() makes it thread-safe with libGDX UI
     * @param propertyChangeEvent   the server response encapsulated in PCE
     */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        Gdx.app.postRunnable(() -> {
            if(propertyChangeEvent.getPropertyName().equals("registrationResponse")) { // received a registration response
                LoginRegister.Response response = (LoginRegister.Response) propertyChangeEvent.getNewValue();
                if(infoDialog != null && infoDialog.getStage() != null) infoDialog.remove(); // removes any dialog
                switch (response.type) { // adapts info dialog accordingly to server response
                    case CHAR_ALREADY_REGISTERED: // char already registered
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("charAlreadyExistsContent"), false, true);
                        break;
                    case USER_ALREADY_REGISTERED:  // user already registered
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("userAlreadyExistsContent"), false, true);
                        break;
                    case EMAIL_ALREADY_REGISTERED:  // email already registered
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("emailAlreadyExistsContent"), false, true);
                        break;
                    case USER_SUCCESSFULLY_REGISTERED:  // user successfully registered
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("successDialogTitle"),
                                langBundle.format("userRegisterSuccessContent"), false, true);
                        break;
                    case DB_ERROR:
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("databaseErrorContent"), false, true);
                        break;
                    default:
                        infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("errorDialogTitle"),
                                langBundle.format("connectionRefusedContent"), false, true);
                        break;
                }
        }
    });
    }

    /**
     * A nested class that controls the register window
     */
    class RegisterController {
        // constructor adds listeners to the actors
        public RegisterController() {
            // adds listeners to validators that show feedback on textfield lose of focus
            userNameTextField.setTextFieldFilter((textField, c) -> spaceBarFilter(textField, c));
            userNameTextField.addListener(new FocusListener() {
                public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                    if(!focused) validateTextInput(userNameTextField, usrStr, usernameLabel, infoUserLabel, InputType.USER);
                }
            });
            //charTextField.setTextFieldFilter((textField, c) -> consecutiveSpaceFilter(textField, c));
            charTextField.addListener(new FocusListener() {
                public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                    if(!focused) validateTextInput(charTextField, charStr, charLabel, infoCharLabel, InputType.NAME);
                }
            });
            passwordTextField.setTextFieldFilter((textField, c) -> spaceBarFilter(textField, c));
            passwordTextField.addListener(new FocusListener() {
                public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                    if(!focused) validateTextInput(passwordTextField, passStr, passwordLabel, infoPassLabel, InputType.PASSWORD);
                }
            });
            emailTextField.setTextFieldFilter((textField, c) -> spaceBarFilter(textField, c));
            emailTextField.addListener(new FocusListener() {
                public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                    if(!focused) validateTextInput(emailTextField, emailStr, emailLabel, infoEmailLabel, InputType.EMAIL);
                }
            });


            // show password check box listener
            showPasswordCB.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    onShowPasswordClicked(event, actor);
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
            registerBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    registerAccount();
                }
            });
            suggestNameLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    onSuggestNameClick(event, x, y);
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
            tosLabel.setTypingListener(new TypingListener() {
                @Override
                public void event(String event) {
                    showEventDialog(event);
                }

                @Override
                public void end() {

                }

                @Override
                public String replaceVariable(String variable) {
                    return null;
                }

                @Override
                public void onChar(long ch) {

                }
            });
            getInstance().addListener(new InputListener(){ //  for keyboard shortcuts
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                        registerAccount();
                    } else if (keycode == Input.Keys.ESCAPE) {
                        backBtnOnClick();
                    }
                    return false;
                }
            });
        }

        // displays a dialog info window based on event that triggered it
        private void showEventDialog(String event) {
            // styles from the skin
            TextButton.TextButtonStyle tbStyle = skin.get("newTextButtonStyle", TextButton.TextButtonStyle.class);
            // get title and content str
            final String dialogTitle = langBundle.format(event);
            final String dialogContent = langBundle.format(event+"Content");
            final String dialogBtn = langBundle.format("ok");
            if(infoDialog != null && infoDialog.getStage() != null)
                return;

            infoDialog = new Dialog(dialogTitle, skin, "newWindowStyle");
            Label title = new Label(dialogTitle, skin,"newLabelStyle");
            title.setAlignment(Align.center);
            Label text = new Label(dialogContent, skin,"newLabelStyle");
            text.setWidth(400);
            text.setAlignment(Align.left);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            text.setWrap(true);
            Pixmap pxColor = new Pixmap(1, 1, Pixmap.Format.RGB888);
            pxColor.setColor(new Color(0x35353555));
            pxColor.fill();
            Label.LabelStyle nlStyle = new Label.LabelStyle(text.getStyle());
            nlStyle.background = new Image(new Texture(pxColor)).getDrawable();
            text.setStyle(nlStyle);
            pxColor.dispose();

            Table scrollTable = new Table();
            scrollTable.add(text).minWidth(182).spaceTop(35).padRight(425).padBottom(10).align(Align.left).colspan(1);
            scrollTable.row();
            scrollTable.pack();
            // uses scrollpane to make table scrollable
            final ScrollPane scroller = new ScrollPane(scrollTable, skin);
            scroller.setFadeScrollBars(false);
            scroller.setScrollbarsOnTop(true);
            scroller.setSmoothScrolling(true);
            scroller.setupOverscroll(0,0,0);

            float uiZoomFactor = 720f / Gdx.graphics.getHeight();

            infoDialog.getContentTable().add(title).center().padBottom(30).padTop(30);
            infoDialog.getContentTable().row();
            infoDialog.getContentTable().add(scroller).size(610,
                                0.65f*Gdx.graphics.getHeight()*uiZoomFactor).padBottom(30).padRight(5).left();
            infoDialog.button(dialogBtn, skin, tbStyle).padBottom(10);
            infoDialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);
            infoDialog.getTitleTable().padBottom(3).padLeft(2);
            infoDialog.pack();
            infoDialog.show(stage).clearActions();
            stage.setScrollFocus(scroller);
            stage.setKeyboardFocus(infoDialog);
            stage.draw();
        }

        // sends register request to the server if valid data is provided
        private void registerAccount() {
            // unfocus everything on stage
            stage.unfocusAll();
            // gets data to validate it before sending it to the server
            String userName = userNameTextField.getText().replaceFirst("\\s++$", "");
            String charName = charTextField.getText().replaceFirst("\\s++$", "");
            String password = passwordTextField.getText().replaceFirst("\\s++$", "");
            String email = emailTextField.getText().replaceFirst("\\s++$", "");

            //String bCryptHash = Encoder.generateHash(password);
//            System.out.println(bCryptHash);
//            System.out.println(Encoder.verifyBCryptHash(password, bCryptHash));

            // checks if any of the data is empty
            boolean hasEmptyField = userName == "" || email  == "" || charName  == "" || password == "";

            // checks if all data is valid
            boolean dataIsValid = isValidAndFitName(charName) && isValidAndFitEmail(email) &&
                                    isValidAndFitUser(userName) && isValidAndFitPassword(password);

            // if user has not agreed, show respective info dialog and return
            if (!tosCB.isChecked()) {
                infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("userMustAgreeTitle"),
                                        langBundle.format("userMustAgree"), false, true);
                infoDialog.show(stage);
                return;
            }
            // if any of the field is empty, show respective info dialog and return
            if (hasEmptyField) {
                infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("emptyFieldTitle"),
                                        langBundle.format("emptyFieldContent"), false, true);
                infoDialog.show(stage);
                return;
            }
            // if data is not all valid, show respective info dialog and return
            if (!dataIsValid) {
                infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("registerInvalidDataTitle"),
                                        langBundle.format("registerInvalidData"), false, true);
                infoDialog.show(stage);
                return;
            }

            // everything is ok with the inputs, proceed to send registration request

            // shows a dialog informing the contacting of the server (to be closed when server responds or with timeout)
            infoDialog = CommonUI.createDialog(stage, skin, langBundle, iconFont, langBundle.format("contactingServerTitle"),
                                            langBundle.format("contactingServerContent"), true, false,
                                        prefs.getInteger("defaultMaxTimeOutValue", 15000) / 1000f);
            //infoDialog.show(stage);
            //close info dialog if too much time has passed (timeout value defined in prefs)


            // encrypts and sends to server from another thread
            sendRegistrationAsync(userName, charName, password, email);
        }

        // creates a thread to encrypt and send user data for registration request
        private void sendRegistrationAsync(String userName, String charName, String password, String email) {
            // Encrypt and send to server on another thread
            new Thread(() -> {
                // asynchronously to the rendering thread
                if(!loginClient.isConnected())
                    loginClient.connect();
                // sign and encrypt data
                encoder = Encoder.getInstance();
                byte[] encryptedPass = encoder.signAndEncryptData(password);
                byte[] encryptedUser = encoder.signAndEncryptData(userName);
                byte[] encryptedEmail = encoder.signAndEncryptData(email);
                byte[] encryptedChar = encoder.signAndEncryptData(charName);
                LoginRegister.Register reg = new LoginRegister.Register();
                reg.charName = encryptedChar; reg.userName = encryptedUser;
                reg.email = encryptedEmail; reg.password = encryptedPass;
                if(loginClient.isConnected()) // sends register attempt if login is connected
                    loginClient.sendRegisterAttempt(reg);
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

        // called when suggest name is called - generates a name and fills char name label
        // TODO: better generated names
        private void onSuggestNameClick(InputEvent event, float x, float y) {
            charTextField.setText(nameGenerator.nextName().replaceFirst("\\s++$", ""));
            validateTextInput(charTextField, charStr, charLabel, infoCharLabel, InputType.NAME);
        }

        // Validates name for each input
        private void validateTextInput(TextField textField, String baseText, TypingLabel label, TypingLabel infoLabel, InputType type) {
            // gets default values for correct validation
            Color defColor = new Color(prefs.getInteger("defaultTextColor", Color.WHITE.toIntBits()));
            Color errorColor = new Color(prefs.getInteger("defaultErrorColor", Color.RED.toIntBits()));

            int minLength, maxLength;
            String text = textField.getText();
            boolean isValid;
            boolean isEmpty = text.isEmpty();
            String invalidText = "", invalidLength = "";
            // prepares vars based on the type of input
            switch(type) {
                case USER:
                    minLength = prefs.getInteger("defaultMinNameSize", 2);
                    maxLength = prefs.getInteger("defaultMaxNameSize", 26);
                    isValid =  isEmpty ? true : isValidUser(text);
                    invalidText = langBundle.format("invalidName");
                    invalidLength = langBundle.format("invalidLength");
                    break;
                case EMAIL:
                    minLength = prefs.getInteger("defaultMinEmailSize", 3);
                    maxLength = prefs.getInteger("defaultMaxEmailSize", 254);
                    isValid = isEmpty ? true : isValidEmail(text);
                    invalidText = langBundle.format("invalidEmail");
                    break;
                case PASSWORD:
                    minLength = prefs.getInteger("defaultMinPasswordSize", 8);
                    maxLength = prefs.getInteger("defaultMaxPasswordSize", 64);
                    isValid = isEmpty ? true :  isValidPassword(text);
                    invalidText = langBundle.format("invalidPassword");
                    invalidLength = langBundle.format("invalidPasswordLength");
                    break;
                case NAME:
                    minLength = prefs.getInteger("defaultMinNameSize", 2);
                    maxLength = prefs.getInteger("defaultMaxNameSize", 26);
                    isValid = isEmpty ? true :  isValidName(text);
                    invalidText = langBundle.format("invalidName");
                    invalidLength = langBundle.format("invalidLength");
                    break;
                default:
                    System.out.println("Unknown type of input to validate");
                    return;
            }

            boolean isFit = text.length() <= maxLength ? text.length() >= minLength ? true : false : false;

            // change textfield/label colors based on if input is valid or not
            Color color = (isValid && isFit) || isEmpty ? defColor : errorColor;
            TextField.TextFieldStyle newStyle = new TextField.TextFieldStyle(textField.getStyle());
            label.setColor(color);
            newStyle.fontColor = color;
            newStyle.focusedFontColor = color;
            textField.setStyle(newStyle);

            // updates labels in case validation fails
            String infoTxt = String.valueOf(infoLabel.getOriginalText());
            String invalidStr = "\n{SIZE=80%}\n{COLOR=RED}- "+invalidText+"{ENDCOLOR}{STYLE=%}";
            String invalidLen = "\n{SIZE=80%}\n{COLOR=RED}- "+invalidLength+"{ENDCOLOR}{STYLE=%}";
            String invalidIcon = "{SIZE=80%}{COLOR=FIREBRICK}[+cross mark button]{ENDCOLOR}{STYLE=%}";
            String validIcon = "{SIZE=80%}[+check mark button]{STYLE=%}";
            String emptyIcon = "{SIZE=80%}{COLOR=GRAY}[+white square button]{ENDCOLOR}{STYLE=%}";

            if(!isValid) {
                if(!infoTxt.contains(invalidStr))
                    infoTxt += invalidStr;
            } else {
                if(infoTxt.contains(invalidStr))
                    infoTxt = infoTxt.replace(invalidStr, "");
            }
            if(!isFit && !isEmpty && type != InputType.EMAIL) { // only show length min and max if not email type
                if(!infoTxt.contains(invalidLen))
                    infoTxt += invalidLen;
            } else {
                if(infoTxt.contains(invalidLen))
                    infoTxt = infoTxt.replace(invalidLen, "");
            }
            infoLabel.setText(infoTxt);
            
            // update label icon
            if(isEmpty)
                label.setText(baseText + ":" + emptyIcon);
            else if(isValid && isFit) {
                label.setText(baseText + ":" + validIcon);
            } else {
                label.setText(baseText + ":" + invalidIcon);
            }

        }

        // blocks space bar inputs
        private boolean spaceBarFilter(TextField textField, char c) {
            if (c == ' ')
                return false;
            return true;
        }

        // blocks space if last char was space, if first char is space and if there are 3 spaces already
        private boolean consecutiveSpaceFilter(TextField textField, char c) {
            if(textField.getText().isEmpty())
                if(c == ' ')
                    return false;
                else
                    return true;
            char lastChar = textField.getText().charAt(textField.getText().length() - 1);
            if (c == ' ' && lastChar == ' ')
                return false;
            int count = 0;
            for(int i = 0; i < textField.getText().length(); i++)
                if(textField.getText().charAt(i) == ' ')
                    count++;
            if (c == ' ' && count >= 3)
                return false;
            return true;
        }

        // toggle password visibility
        private void onShowPasswordClicked(ChangeListener.ChangeEvent event, Actor actor) {
            passwordTextField.setPasswordMode(!showPasswordCB.isChecked());
        }

        // called when back button is pressed
        private void backBtnOnClick() {
            if(parent instanceof MainMenuScreen) {  // goes back to login screen
                ((MainMenuScreen) parent).update(getInstance(), false, MainMenuScreen.ScreenCommands.LOAD_LOGIN_WINDOW);
            }
        }
    }

    private RegisterWindow getInstance() {
        return this;
    }

    private enum InputType {
        USER,
        EMAIL,
        PASSWORD,
        NAME
    }

}
