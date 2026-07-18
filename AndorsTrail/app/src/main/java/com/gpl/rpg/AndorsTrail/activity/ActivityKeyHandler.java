package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.gpl.rpg.AndorsTrail.controller.InputController;

public final class ActivityKeyHandler {
    private static long lastMouseBackEventDownTime = -1;

    private ActivityKeyHandler() {}

    /**
     * Handles a configured key mapping for the Back action.
     *
     * @param activity the activity on which to invoke Back
     * @param event the key event to inspect
     * @return {@code true} if the event triggered Back, otherwise {@code false}
     */
    public static boolean handleBackMappedKey(Activity activity, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (InputController.isMappedKey(event.getKeyCode(), InputController.KEY_BACK)) {
                activity.onBackPressed();
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a secondary pointer button press as the Back action.
     *
     * @param activity the activity on which to invoke Back
     * @param event the motion event to inspect
     * @return {@code true} if the event was handled, otherwise {@code false}
     */
    public static boolean handleBackMappedMouseButton(Activity activity, MotionEvent event) {
        if (!isPointerEvent(event)) return false;
        if (!isSecondaryButtonPress(event)) return false;

        if (event.getDownTime() == lastMouseBackEventDownTime) return true;
        lastMouseBackEventDownTime = event.getDownTime();

        activity.onBackPressed();
        return true;
    }

    /**
     * Determines whether an event originated from a pointer-class input device.
     *
     * @param event the motion event to inspect
     * @return {@code true} for pointer-class events, otherwise {@code false}
     */
    private static boolean isPointerEvent(MotionEvent event) {
        return (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0;
    }

    /**
     * Determines whether an event represents a secondary-button press.
     *
     * @param event the motion event to inspect
     * @return {@code true} for a secondary-button press, otherwise {@code false}
     */
    private static boolean isSecondaryButtonPress(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_BUTTON_PRESS) return false;
        final int buttons = event.getButtonState();
        // Only return a secondary press if the primary button is up.
        return (buttons & MotionEvent.BUTTON_SECONDARY) != 0
                && (buttons & MotionEvent.BUTTON_PRIMARY) == 0;
    }
}
