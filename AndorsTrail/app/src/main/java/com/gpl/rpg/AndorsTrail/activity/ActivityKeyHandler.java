package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.gpl.rpg.AndorsTrail.controller.InputController;

public final class ActivityKeyHandler {
    private static long lastMouseBackEventDownTime = -1;

    private ActivityKeyHandler() {}

    public static boolean handleBackMappedKey(Activity activity, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (InputController.isMappedKey(event.getKeyCode(), InputController.KEY_BACK)) {
                activity.onBackPressed();
                return true;
            }
        }
        return false;
    }

    public static boolean handleBackMappedMouseButton(Activity activity, MotionEvent event) {
        if (!isPointerEvent(event)) return false;
        if (!isSecondaryButtonPress(event)) return false;

        if (event.getDownTime() == lastMouseBackEventDownTime) return true;
        lastMouseBackEventDownTime = event.getDownTime();

        activity.onBackPressed();
        return true;
    }

    private static boolean isPointerEvent(MotionEvent event) {
        return (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0;
    }

    private static boolean isSecondaryButtonPress(MotionEvent event) {
        final int action = event.getActionMasked();
        return (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_BUTTON_PRESS)
                && (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0;
    }
}
