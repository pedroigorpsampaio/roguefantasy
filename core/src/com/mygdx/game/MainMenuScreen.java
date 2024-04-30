package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Locale;

public class MainMenuScreen implements Screen {

    private Skin skin;
    private Stage stage;
    private Label fpsLabel;
    private Window window;
    private AssetManager manager;
    private Music bgm;
    private RogueFantasy game;
    private Preferences prefs;
    private TextField passwordTextField;
    private TextField userNameTextField;
    private CheckBox showPasswordCB;


    /**
     * Builds the game main menu containing login screen mainly (prototype version)
     *
     * @param game    the reference to the game object that controls game screens
     * @param manager the asset manager containing assets loaded from the loading process
     */
    public MainMenuScreen(RogueFantasy game, AssetManager manager) {
        this.manager = manager;
        this.game = game;

        // gets preferences reference, that stores simple data persisted between executions
        prefs = Gdx.app.getPreferences("globalPrefs");

        // gets font from asset manager
       // BitmapFont font8 = manager.get("fonts/immortal13.ttf", BitmapFont.class);

        // get and play music
        bgm = manager.get("eldamar.mp3", Music.class);
        bgm.play();

        // loads game chosen skin
        skin = manager.get("skin/neutralizer/neutralizer-ui.json", Skin.class);
        //skin.add("font8", font8, BitmapFont.class);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        // stage.setDebugAll(true);

        // gets language bundle from asset manager
        I18NBundle langBundle = manager.get("lang/langbundle", I18NBundle.class);

        // styles from the skin
        Label.LabelStyle lStyle = skin.get("newLabelStyle", Label.LabelStyle.class);
        TextButton.TextButtonStyle tbStyle = skin.get("newTextButtonStyle", TextButton.TextButtonStyle.class);

        // label that describes the project name/version
        String descStr = " "+ langBundle.format("game") + " " + langBundle.format("version");
        final Label projectDescLabel = new Label(descStr, skin, "font8", Color.WHITE);

        // just a fps label (TODO: TO BE CHANGED IN THE FUTURE)
        fpsLabel = new Label("fps:", skin, "font8", Color.WHITE);

        // remember checkbox
        String rememberStr = " "+ langBundle.format("rememberLogin");
        final CheckBox rememberCB = new CheckBox(rememberStr, skin, "newCheckBoxStyle");

        // TODO: LOGIN - CRUD - DB
        // TODO: DO NOT ACCEPT SPECIAL CHARS IN USERNAME/FILTER SOME IN PASSWORD
        // TODO: ADJUST LOGIN MENU UP ON KEYBOARD OPEN - ON ANDROID
        // username text field
        userNameTextField = new TextField(prefs.getString("username", ""), skin, "newTextFieldStyle");
        userNameTextField.setMessageText(" "+ langBundle.format("username"));
        userNameTextField.setAlignment(Align.center);
        final Label usernameLabel = new Label(langBundle.format("username") + ": ", skin, "font8", Color.WHITE);
        usernameLabel.setAlignment(Align.center);
        // Accepts alphanumeric and digits only
        userNameTextField.setTextFieldFilter((textField, c) -> {
            if (Character.toString(c).matches("^[a-zA-Z0-9]"))
                return true;
            return false;
        });
        userNameTextField.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                // updates stored username if it remember cb is checked
                if(rememberCB.isChecked()) {
                    prefs.putString("username", userNameTextField.getText());
                    prefs.flush();
                }
            }
        });

        // password text field in password mode.
        final Label passwordLabel = new Label(langBundle.format("password")+": ", skin, "font8", Color.WHITE);
        passwordLabel.setAlignment(Align.center);
        passwordTextField = new TextField("", skin, "newTextFieldStyle");
        passwordTextField.setMessageText(" "+ langBundle.format("password"));
        passwordTextField.setAlignment(Align.center);
        passwordTextField.setPasswordCharacter('*');
        passwordTextField.setPasswordMode(true);
        // Accepts alphanumeric, digits and some special characters that increases password strength
        passwordTextField.setTextFieldFilter((textField, c) -> {
            if (Character.toString(c).matches("^[a-zA-Z0-9!@#$%^&*?/.;:_,-]"))
                return true;
            return false;
        });

        // language select box
        final SelectBox langBox = new SelectBox(skin, "newSelectBoxStyle");
        langBox.setAlignment(Align.right);
        langBox.getList().setStyle(skin.get("newListStyle", List.ListStyle.class));
        langBox.getList().setAlignment(Align.right);
        langBox.getStyle().listStyle.selection.setRightWidth(10);
        langBox.getStyle().listStyle.selection.setLeftWidth(20);
//        langBox.setItems("Android1", "Windows1 long text in item", "Linux1", "OSX1", "Android2", "Windows2", "Linux2", "OSX2",
//                "Android3", "Windows3", "Linux3", "OSX3", "Android4", "Windows4", "Linux4", "OSX4", "Android5", "Windows5", "Linux5",
//                "OSX5", "Android6", "Windows6", "Linux6", "OSX6", "Android7", "Windows7", "Linux7", "OSX7");
        langBox.setItems("EN", "PT_BR", "DE", "ES");
        //Locale currentLocale = manager.get("lang/langbundle", I18NBundle.class).getLocale();
        //System.out.println(currentLocale.toString().toUpperCase());
        // gets last language and country to select it in box
        String lastLang = prefs.getString("lastCountry", "") != "" ? prefs.getString("lastLang", "en") +
                            "_" + prefs.getString("lastCountry", "") :  prefs.getString("lastLang", "en");
        langBox.setSelected(lastLang.toUpperCase());
        langBox.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                changeLanguage((String) langBox.getSelected());
            }
        });

        // remember me checkbox to choose the option to store login credentials
        // TODO: STORE LOGIN CREDENTIALS - SEND PREFERENCES TO BUILD WINDOW WITH PREVIOUS DATA
        // TODO: DO THE SAME WHEN CHANGING LANGUAGES
        rememberCB.getImage().setScaling(Scaling.fill);
        rememberCB.getImageCell().size(26);
        rememberCB.getImageCell().padTop(6);
        rememberCB.setChecked(prefs.getBoolean("remember", false));
        rememberCB.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                // stores username if remember me is checked
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
        });

        String showPasswordString = " "+ langBundle.format("showPassword");
        showPasswordCB = new CheckBox(showPasswordString, skin, "newCheckBoxStyle");
        showPasswordCB.setChecked(false);
        showPasswordCB.getImage().setScaling(Scaling.fill);
        showPasswordCB.getImageCell().size(26);
        showPasswordCB.getImageCell().padTop(6);
        // remember me checkbox listener
        showPasswordCB.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                //Gdx.graphics.setContinuousRendering(rememberCB.isChecked());
                passwordTextField.setPasswordMode(!showPasswordCB.isChecked());
            }
        });

        // login button TODO: LOGIN IN SERVER - SERVER
        Button playBtn = new TextButton(langBundle.format("login"), skin);
        final String[] infoLogin = {langBundle.format("loginSuccess")};
        final String[] infoLoginBtn = {langBundle.format("ok")};
        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

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
        });

        // TODO: BETTER ORGANIZE WINDOW / REGISTER MODE
        // Builds Window with the desired actors to represent main menu
        // window.debug();
        window = new Window(" "+langBundle.format("loginWindowTitle"), skin, "newWindowStyle");
        window.getTitleTable().padBottom(6);
        //window.getTitleTable().add(new TextButton("X", skin)).height(window.getPadTop());
        window.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        if(Gdx.app.getType() == Application.ApplicationType.Android) // zoom menu if user is using android device
            ((OrthographicCamera)stage.getCamera()).zoom -= 0.50f; // zoom in  window menu
        //window.setOrigin(window.getWidth(),window.getHeight()/2);
        window.setMovable(false);
        window.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        window.add(projectDescLabel).colspan(0);
        window.row();
        window.add(usernameLabel).colspan(2);
        window.add(userNameTextField).minWidth(200).expandX().fillX().colspan(3);
        window.row();
        window.add(passwordLabel).colspan(2);
        window.add(passwordTextField).minWidth(200).expandX().fillX().colspan(3);
        window.row();
        window.add(rememberCB).colspan(1);
        window.add(showPasswordCB).colspan(5);
        window.row();
        window.add(langBox).colspan(1).maxWidth(160);
        window.add(playBtn).colspan(4).minWidth(200);
        window.row();
        window.add(fpsLabel).colspan(5);
        window.pack();
        // stage.addActor(new Button("Behind Window", skin));
        stage.addActor(window);

//
//        userNameTextField.setTextFieldListener((textField, key) -> {
//            if (key == '\n') textField.getOnscreenKeyboard().show(false);
//        });
    }

    private void changeLanguage(String selected) {
        String lang, country = "";
        switch(selected){
            case "DE":
                prefs.putString("lastLang", "de");
                prefs.putString("lastCountry", "");
                lang = "de";
                break;
            case "ES":
                prefs.putString("lastLang", "es");
                prefs.putString("lastCountry", "");
                lang = "es";
                break;
            case "PT_BR":
                prefs.putString("lastLang", "pt");
                prefs.putString("lastCountry", "BR");
                lang = "pt";
                country = "BR";
                break;
            default:
                prefs.putString("lastLang", "en");
                prefs.putString("lastCountry", "");
                lang = "en";
                break;
        }

        //updates preferences
        prefs.flush();

        // loads language bundles
        Locale newLocale = new Locale(lang, country);
        I18NBundleLoader.I18NBundleParameter newLangParams = new I18NBundleLoader.I18NBundleParameter(newLocale, "UTF-8");
        manager.unload("lang/langbundle");
        manager.load("lang/langbundle", I18NBundle.class, newLangParams);
        // blocks until new language is fully loaded
        manager.finishLoading();
//                I18NBundle newBnd = manager.get("lang/langbundle", I18NBundle.class);
//                System.out.println(newBnd.format("tryAgain"));
        MainMenuScreen rebuiltMenu = new MainMenuScreen(game, manager);
        rebuiltMenu.refillData(userNameTextField.getText(), passwordTextField.getText(), showPasswordCB.isChecked());
        game.setScreen(rebuiltMenu);
        dispose();
    }

    // used to refill data that are not stored for specific reasons
    // on window rebuild, such as in language change actions
    private void refillData(String user, String pw, Boolean showPw) {
        userNameTextField.setText(user); passwordTextField.setText(pw); showPasswordCB.setChecked(showPw);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);

        fpsLabel.setText("fps: " + Gdx.graphics.getFramesPerSecond());

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        window.setPosition(MathUtils.floor(Gdx.graphics.getWidth() / 2.0f ), MathUtils.floor(Gdx.graphics.getHeight() / 2.0f), Align.center);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        //skin.dispose();
    }
}
