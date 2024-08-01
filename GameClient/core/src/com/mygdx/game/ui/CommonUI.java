package com.mygdx.game.ui;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.mygdx.game.RogueFantasy;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Common UI methods that help different UI modules
public class CommonUI {
    public static final int MAX_LINE_CHARACTERS_FLOATING_TEXT = 50;
    public static final int MAX_LINES_FLOATING_TEXT = 13;
    public static final float FLOATING_CHAT_TEXT_SCALE = 1f;
    public static final float TARGET_UI_WIDTH = 340;
    public static final float TARGET_UI_HEIGHT = 105;
    public static final float TARGET_UI_BG_ALPHA = 1.00f;
    public static final boolean ENABLE_TARGET_UI = false;
    public static final boolean FADE_CLIENT_TAG_ON_ALL_FLOATING_TEXT = true;
    public static final float MAP_INFO_LABEL_LIFETIME = 3f;
    public static boolean enableDebugTex = false;
    public static HashMap<String, Cursor> cursorBank;

    // possible screen commands to receive from windows
    enum ScreenCommands {
        RELOAD_LANGUAGE,
        RELOAD_VOLUME,
        LOAD_REGISTER_WINDOW,
        DISPOSE,
        LOGOUT, LOAD_LOGIN_WINDOW
    }

    public static void removeWindowWithAction(GameWindow window, Action effect) {
        Action completeAction = new Action(){
            public boolean act( float delta ) {
                window.remove();
                return true;
            }
        };

        window.addAction(sequence(effect, completeAction));
    }

    // creates a dialog from the parameters provided
    public static Dialog createDialog(Stage stage, Skin skin, I18NBundle langBundle,
                                      Font iconFont, String title, String content, boolean anim, boolean closeable) {
        // hides android soft keyboard if its open
        if(RogueFantasy.isKeyboardShowing())
            Gdx.input.setOnscreenKeyboardVisible(false);

        TextButton.TextButtonStyle tbStyle = skin.get("newTextButtonStyle", TextButton.TextButtonStyle.class);
        Dialog dialog = new Dialog(title, skin, "newWindowStyle");
        int nLetters = content.length();
        if(anim) {
            TypingLabel text = new TypingLabel(content, iconFont);
            float dW = dialog.getWidth() < 612 ? 612 : dialog.getWidth();
            int period = MathUtils.ceil(dW / (text.getWidth() / nLetters)) + 1;
            content = insert(content, "\n", period);
            content = "{JUMP=0.6;0.8;0.8}" + content + "{ENDJUMP}";
            text.setText(content);
            text.skipToTheEnd();
            text.setWidth(400);
            text.setAlignment(Align.center);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(10).padBottom(1).minHeight(120);
        } else {
            Label text = new Label(content, skin, "newLabelStyle");
            text.setWidth(400);
            text.setAlignment(Align.center);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            text.setWrap(true);
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(10).padBottom(1).minHeight(120);
        }
        if(closeable) {
            dialog.button(langBundle.format("ok"), skin, tbStyle).padBottom(10);
            dialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);
        }
        dialog.getTitleTable().padTop(9).padBottom(15).padLeft(2).padRight(6);
        dialog.pack();
        dialog.show(stage);
        return dialog;
    }

    public static Pixmap getPixmapCircle(int radius, Color color, boolean isFilled) {
        Pixmap pixmap=new Pixmap(2*radius+1, 2*radius+1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        if(isFilled)
            pixmap.fillCircle(radius, radius, radius);
        else
            pixmap.drawCircle(radius, radius, radius);
        pixmap.drawLine(radius, radius, 2*radius, radius);
        pixmap.setFilter(Pixmap.Filter.NearestNeighbour);
        return pixmap;
    }

    // creates a dialog from the parameters provided with a timer
    public static Dialog createDialog(Stage stage, Skin skin, I18NBundle langBundle,
                                      Font iconFont, String title, String content, boolean anim, boolean closeable, float timeOut) {

        // hides keyboard if its open
        if(Gdx.app.getType() == Application.ApplicationType.Android && RogueFantasy.isKeyboardShowing())
            Gdx.input.setOnscreenKeyboardVisible(false);

        TextButton.TextButtonStyle tbStyle = skin.get("newTextButtonStyle", TextButton.TextButtonStyle.class);
        Dialog dialog = new Dialog(title, skin, "newWindowStyle");
        int nLetters = content.length();
        if(anim) {
            TypingLabel text = new TypingLabel(content, iconFont);
            float dW = dialog.getWidth() < 612 ? 612 : dialog.getWidth();
            int period = MathUtils.ceil(dW / (text.getWidth() / nLetters)) + 1;
            content = insert(content, "\n", period);
            content = "{JUMP=0.6;0.8;0.8}" + content + "{ENDJUMP}";
            text.setText(content);
            text.skipToTheEnd();
            text.setWidth(400);
            text.setAlignment(Align.center);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(10).padBottom(1).minHeight(120);
        } else {
            Label text = new Label(content, skin, "newLabelStyle");
            text.setWidth(400);
            text.setAlignment(Align.center);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            text.setWrap(true);
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(10).padBottom(1).minHeight(120);
        }
        if(closeable) {
            dialog.button(langBundle.format("ok"), skin, tbStyle).padBottom(10);
            dialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);
        }
        dialog.getTitleTable().padTop(9).padBottom(15).padLeft(2).padRight(6);
        dialog.pack();
        dialog.show(stage);
        createDialogTimer(dialog, timeOut); // starts time to close
        return dialog;
    }

    public static void createDialogTimer(Dialog dialog, float timeOutSeconds) {
        Timer.Task dialogTimeOutTimer = new Timer.Task() {
            @Override
            public void run() {
                dialog.remove();
            }
        };
        Timer.schedule(dialogTimeOutTimer, timeOutSeconds);
    }

    public static Pixmap pixmapFromTextureRegion(TextureRegion textureRegion) {
        TextureData textureData = textureRegion.getTexture().getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        Pixmap pixmap = new Pixmap(
                textureRegion.getRegionWidth(),
                textureRegion.getRegionHeight(),
                textureData.getFormat()
        );
        pixmap.drawPixmap(
                textureData.consumePixmap(), // The other Pixmap
                0, // The target x-coordinate (top left corner)
                0, // The target y-coordinate (top left corner)
                textureRegion.getRegionX(), // The source x-coordinate (top left corner)
                textureRegion.getRegionY(), // The source y-coordinate (top left corner)
                textureRegion.getRegionWidth(), // The width of the area from the other Pixmap in pixels
                textureRegion.getRegionHeight() // The height of the area from the other Pixmap in pixels
        );
        return pixmap;
    }

    public static String insert(String text, String insert, int period) {
        Pattern p = Pattern.compile("(.{" + period + "})", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.replaceAll("$1" + insert);
    }

    public static Pixmap createRoundedRectangle(int width, int height, int cornerRadius, Color color) {

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(color);

        pixmap.fillCircle(cornerRadius, cornerRadius, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, cornerRadius, cornerRadius);
        pixmap.fillCircle(cornerRadius, height - cornerRadius - 1, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, height - cornerRadius - 1, cornerRadius);

        pixmap.fillRectangle(cornerRadius, 0, width - cornerRadius * 2, height);
        pixmap.fillRectangle(0, cornerRadius, width, height - cornerRadius * 2);

        ret.setColor(color);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixmap.getPixel(x, y) != 0) ret.drawPixel(x, y);
            }
        }
        pixmap.dispose();

        return ret;
    }

    public static class Arc extends ShapeRenderer {

        private final ImmediateModeRenderer renderer;
        private Color color = new Color(1, 1, 1, 1);

        public Arc() {
            renderer = super.getRenderer();
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void arc(float x, float y, float radius, float start, float degrees) {
            int segments = 30;

            if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.");
            float colorBits = color.toFloatBits();
            float theta = (2 * MathUtils.PI * (degrees / 360.0f)) / segments;
            float cos = MathUtils.cos(theta);
            float sin = MathUtils.sin(theta);
            float cx = radius * MathUtils.cos(start * MathUtils.degreesToRadians);
            float cy = radius * MathUtils.sin(start * MathUtils.degreesToRadians);

            for (int i = 0; i < segments; i++) {
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
            }
        }
    }

}
