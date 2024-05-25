package com.mygdx.game.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
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

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Common UI methods that help different UI modules
public class CommonUI {
    // creates a dialog from the parameters provided
    public static Dialog createDialog(Stage stage, Skin skin, I18NBundle langBundle,
                                      Font iconFont, String title, String content, boolean anim, boolean closeable) {
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

    // creates a dialog from the parameters provided with a timer
    public static Dialog createDialog(Stage stage, Skin skin, I18NBundle langBundle,
                                      Font iconFont, String title, String content, boolean anim, boolean closeable, float timeOut) {
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


    public static String insert(String text, String insert, int period) {
        Pattern p = Pattern.compile("(.{" + period + "})", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.replaceAll("$1" + insert);
    }
}
