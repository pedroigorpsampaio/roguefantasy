package com.mygdx.game.ui;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.I18NBundleLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.mygdx.game.RogueFantasy;
import com.mygdx.game.network.DispatchServer;
import com.mygdx.game.network.LoginClient;
import com.mygdx.game.util.Encoder;

import java.util.Locale;

/**
 * A class that encapsulates the option menu window
 */
public class OptionWindow extends GameWindow {
    private Label bgmLabel;
    private Label sfxLabel;
    private Label langLabel;
    private Slider bgmSlider;
    private Slider sfxSlider;
    private SelectBox langBox;
    private TextButton backBtn;
    private TextButton logoffBtn;
    private CheckBox openChatOnPrivateMsgCb;

    /**
     * Builds the option window, to be used as an actor in any screen
     *
     * @param game    the reference to the game object that controls game screens
     * @param stage   the current stage in use
     * @param parent  the parent screen that invoked the option window
     * @param manager the asset manager containing assets loaded from the loading process
     * @param title   the title of the options window
     * @param skin    the game skin to be used when building the window
     * @param styleName the style name present in the skin to be used when building the window
     */
    public OptionWindow(RogueFantasy game, Stage stage, Screen parent, AssetManager manager, String title, Skin skin, String styleName) {
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
        this.getTitleLabel().setText(" "+langBundle.format("option"));

        // music volume slider
        bgmSlider = new Slider(0, 1, 0.1f, false, skin);
        bgmSlider.setValue(prefs.getFloat("bgmVolume", 1.0f));
        bgmSlider.setAnimateDuration(0.1f);

        // music volume slider
        sfxSlider = new Slider(0, 1, 0.1f, false, skin);
        sfxSlider.setValue(prefs.getFloat("sfxVolume", 1.0f));
        sfxSlider.getStyle().background.setMinHeight(24);
        sfxSlider.getStyle().knobBefore.setMinHeight(14);
        sfxSlider.getStyle().knob.setMinHeight(35);
        sfxSlider.getStyle().knob.setMinWidth(25);
        sfxSlider.getStyle().knobDown.setMinHeight(35);
        sfxSlider.getStyle().knobDown.setMinWidth(25);
        sfxSlider.setAnimateDuration(0.1f);

        // options labels
        bgmLabel = new Label( " "+ langBundle.format("bgm"), skin, "fontMedium", Color.WHITE);
        sfxLabel = new Label( " "+ langBundle.format("sfx"), skin, "fontMedium", Color.WHITE);
        langLabel = new Label( " "+ langBundle.format("lang"), skin, "fontMedium", Color.WHITE);

        // back button
        backBtn = new TextButton(langBundle.format("back"), skin);

        // logoff button (for game screen)
        logoffBtn = new TextButton(langBundle.format("logoff"), skin);

        // language select box
        //TODO: Find a way to add emoji flags to items of lang box?
        langBox = new SelectBox(skin, "newSelectBoxStyle");
        langBox.setAlignment(Align.right);
        langBox.getList().setStyle(skin.get("newListStyle", List.ListStyle.class));
        langBox.getList().setAlignment(Align.right);
        langBox.getStyle().listStyle.selection.setRightWidth(10);
        langBox.getStyle().listStyle.selection.setLeftWidth(20);
        langBox.setItems(langBundle.format("en"), langBundle.format("pt_br"), langBundle.format("de"), langBundle.format("es"));
        langBox.setSelectedIndex(prefs.getInteger("lastLangIdx", 0));

        openChatOnPrivateMsgCb = new CheckBox(langBundle.get("openChatOnePrivateMessage"), skin, "newCheckBoxStyle");
        openChatOnPrivateMsgCb.getImage().setScaling(Scaling.fill);
        openChatOnPrivateMsgCb.getImageCell().size(26);
        openChatOnPrivateMsgCb.getImageCell().padTop(6);
        openChatOnPrivateMsgCb.setChecked(prefs.getBoolean("openChatOnPrivateMsg", false));

        // builds options window
        this.getTitleTable().padBottom(6);
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320);
        this.setPosition(Gdx.graphics.getWidth() / 2.0f ,Gdx.graphics.getHeight() / 2.0f, Align.center);
        this.getTitleTable().padBottom(6);
        this.setMovable(false);
        this.defaults().spaceBottom(10).padRight(5).padLeft(5).padBottom(2).minWidth(320).padTop(21);
        this.add(bgmLabel).colspan(1);
        this.add(bgmSlider).width(240).fillX().colspan(3).spaceTop(35);
        this.row();
        this.add(sfxLabel).colspan(1);
        this.add(sfxSlider).width(240).fillX().colspan(3);
        this.row();
        this.add(langLabel).colspan(1);
        this.add(langBox).width(240).spaceTop(16);
        this.row();
        this.add(openChatOnPrivateMsgCb).colspan(2).center();
        this.row();
        if(parent instanceof GameScreen) { // game screen option buttons
            this.add(logoffBtn).minWidth(182).colspan(1).spaceTop(21).padBottom(10).center().padRight(20);
            this.add(backBtn).minWidth(182).colspan(1).spaceTop(21).padBottom(10).left();
        } else
            this.add(backBtn).minWidth(182).colspan(2).spaceTop(21).padBottom(10);
        this.pack();

        // instantiate the controller that adds listeners and acts when needed
        new OptionController();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void startServerListening(DispatchServer client) {

    }

    @Override
    public void stopServerListening() {

    }

    @Override
    public void softKeyboardClosed() {

    }

    @Override
    public void softKeyboardOpened() {

    }

    @Override
    public void reloadLanguage() {

    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);

        if (stage != null) {
            // Actor added to stage
        } else {
            // Actor removed from stage
            sfxSlider.getStyle().background.setMinHeight(16);
            sfxSlider.getStyle().knobBefore.setMinHeight(10);
            sfxSlider.getStyle().knob.setMinHeight(20);
            sfxSlider.getStyle().knob.setMinWidth(15);
            sfxSlider.getStyle().knobDown.setMinHeight(20);
            sfxSlider.getStyle().knobDown.setMinWidth(15);
        }
    }

    /**
     * A nested class that controls the option window
     */
    class OptionController {

        // constructor adds listeners to the actors
        public OptionController() {
            langBox.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    changeLanguage(langBox.getSelectedIndex());
                }
            });
            backBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    backBtnOnClick(event, x, y);
                }
            });
            logoffBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    logoffBtnOnClick(event, x, y);
                }
            });
            bgmSlider.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    changeVolume("bgm", + bgmSlider.getValue());
                }
            });
            sfxSlider.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    changeVolume("sfx", + sfxSlider.getValue());
                }
            });
            openChatOnPrivateMsgCb.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    prefs.putBoolean("openChatOnPrivateMsg", openChatOnPrivateMsgCb.isChecked());
                    //updates preferences
                    prefs.flush();
                }
            });
        }

        // called when logout button is clicked (only appears in game screen options)
        private void logoffBtnOnClick(InputEvent event, float x, float y) {
            if (parent instanceof GameScreen)
                ((GameScreen) parent).processWindowCmd(getInstance(), false, CommonUI.ScreenCommands.LOGOUT);
        }

        // called when volume of any kind is changed
        private void changeVolume(String type, float value) {
            switch(type) {
                case "bgm":
                    prefs.putFloat("bgmVolume", value);
                    break;
                case "sfx":
                    prefs.putFloat("sfxVolume", value);
                    break;
                default:
                    System.out.println("unknown volume type");
                    break;
            }

            //updates preferences
            prefs.flush();

            // calls parent update method to properly reload volume according to the type of screen this window is on
            if(parent instanceof MainMenuScreen)
                ((MainMenuScreen) parent).update(getInstance(), false, CommonUI.ScreenCommands.RELOAD_VOLUME);
            else if(parent instanceof  GameScreen)
                ((GameScreen) parent).processWindowCmd(getInstance(), false, CommonUI.ScreenCommands.RELOAD_VOLUME);

            // calls screen parent resume method to update needed vars
            //parent.resume();
        }

        // called when language is changed on the select box
        private void changeLanguage(int selected) {
            // update prefs depending on the language and set vars
            String lang, country = "";

            if(langBox.getSelected().equals(langBundle.format("de"))) {
                prefs.putInteger("lastLangIdx", selected);
                prefs.putString("lastLangLocale", "de");
                prefs.putString("lastCountry", "");
                lang = "de";
            } else if(langBox.getSelected().equals(langBundle.format("es"))) {
                prefs.putInteger("lastLangIdx", selected);
                prefs.putString("lastLangLocale", "es");
                prefs.putString("lastCountry", "");
                lang = "es";
            } else if(langBox.getSelected().equals(langBundle.format("pt_br"))) {
                prefs.putInteger("lastLangIdx", selected);
                prefs.putString("lastLangLocale", "pt");
                prefs.putString("lastCountry", "BR");
                lang = "pt";
                country = "BR";
            } else { //english
                prefs.putInteger("lastLangIdx", selected);
                prefs.putString("lastLangLocale", "en");
                prefs.putString("lastCountry", "");
                lang = "en";
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

            // calls parent update method to properly reload language according to the type of screen this window is on
            if(parent instanceof MainMenuScreen)
                ((MainMenuScreen) parent).update(getInstance(), true, CommonUI.ScreenCommands.RELOAD_LANGUAGE);
            else if(parent instanceof  GameScreen)
                ((GameScreen) parent).processWindowCmd(getInstance(), true, CommonUI.ScreenCommands.RELOAD_LANGUAGE);

        }

        // called when back button is pressed
        private void backBtnOnClick(InputEvent event, float x, float y) {
            if(parent instanceof MainMenuScreen)
                remove(); // removes option window
            else if(parent instanceof  GameScreen) {
                GameScreen.getInstance().toggleWindow(getInstance(), false, true);
            }
        }
    }

    private OptionWindow getInstance() {
        return this;
    }
}
