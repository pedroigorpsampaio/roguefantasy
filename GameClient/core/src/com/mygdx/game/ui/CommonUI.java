package com.mygdx.game.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Common UI methods that help different UI modules
public class CommonUI {
    // creates a dialog from the parameters provided
    public static Dialog createDialog(Skin skin, I18NBundle langBundle,
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
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(20).padBottom(1).minHeight(120);
        } else {
            Label text = new Label(content, skin, "newLabelStyle");
            text.setWidth(400);
            text.setAlignment(Align.center);
            text.setFillParent(true);
            text.setHeight(text.getPrefHeight());
            text.setWrap(true);
            dialog.getContentTable().add(text).grow().minWidth(612).padLeft(4).padRight(25).padTop(20).padBottom(1).minHeight(120);
        }
        if(closeable) {
            dialog.button(langBundle.format("ok"), skin, tbStyle);
            dialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);
        }
        dialog.getTitleTable().padBottom(3).padLeft(2);
        dialog.pack();

        return dialog;
    }

    public static String insert(String text, String insert, int period) {
        Pattern p = Pattern.compile("(.{" + period + "})", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.replaceAll("$1" + insert);
    }
}
