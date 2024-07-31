package com.mygdx.game;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.mygdx.game.util.AndroidNative;
import com.mygdx.game.util.Common;

public class Android implements AndroidNative {
    public static AndroidGraphics graphics;
    public static Context context;

    public Android() {
        final View rootView = graphics.getView();
        // ContentView is the root view of the layout of this activity/fragment
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> {
                    Rect r = new Rect();
                    rootView.getWindowVisibleDisplayFrame(r);
                    int screenHeight = rootView.getRootView().getHeight();

                    // r.bottom is the position above soft keypad or device button.
                    // if keypad is shown, the r.bottom is smaller than that before.
                    int keypadHeight = screenHeight - r.bottom;

                    //Log.d("inputtst", "keypadHeight = " + keypadHeight);

                    if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                        // keyboard is opened
                        if (!isKeyboardShowing) {
                            keyboardHeight = keypadHeight;
                            isKeyboardShowing = true;
                            onKeyboardVisibilityChanged(true);
                        }
                    }
                    else {
                        // keyboard is closed
                        if (isKeyboardShowing) {
                            isKeyboardShowing = false;
                            onKeyboardVisibilityChanged(false);
                        }
                    }
                });
    }

    public boolean isKeyboardShowing = false;
    public int keyboardHeight = 0;
    void onKeyboardVisibilityChanged(boolean opened) {
        Log.d("inputtst", "keyboard " + opened);
    }

    @Override
    public int getKeyboardHeight() {
        return (int) (keyboardHeight* Common.ANDROID_VIEWPORT_SCALE);
    }

    @Override
    public boolean isKeyboardShowing() {
        return isKeyboardShowing;
    }

    @Override
    public void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager)  context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", text);
        clipboard.setPrimaryClip(clip);
    }
}
