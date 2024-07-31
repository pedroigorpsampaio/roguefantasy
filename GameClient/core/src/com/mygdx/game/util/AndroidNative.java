package com.mygdx.game.util;

public interface AndroidNative {
    public int getKeyboardHeight();
    public boolean isKeyboardShowing();
    public void copyToClipboard(String text);
}