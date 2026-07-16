package com.example.smartroad.util;

import android.app.Activity;

import androidx.core.content.ContextCompat;

import com.example.smartroad.R;
import com.google.android.material.snackbar.Snackbar;

public final class SnackbarUtil {

    private SnackbarUtil() {
    }

    public static void showSuccess(Activity activity, String message) {
        build(activity, message, R.color.status_resolved, Snackbar.LENGTH_SHORT).show();
    }

    public static void showSuccess(Activity activity, String message, Runnable onDismissed) {
        Snackbar snackbar = build(activity, message, R.color.status_resolved, Snackbar.LENGTH_SHORT);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                onDismissed.run();
            }
        });
        snackbar.show();
    }

    public static void showError(Activity activity, String message) {
        build(activity, message, R.color.smartroad_primary_dark, Snackbar.LENGTH_LONG).show();
    }

    public static void showError(Activity activity, String message, Runnable onDismissed) {
        Snackbar snackbar = build(activity, message, R.color.smartroad_primary_dark, Snackbar.LENGTH_LONG);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                onDismissed.run();
            }
        });
        snackbar.show();
    }

    private static Snackbar build(Activity activity, String message, int backgroundColorRes, int duration) {
        Snackbar snackbar = Snackbar.make(
                activity.findViewById(android.R.id.content), message, duration);
        snackbar.setBackgroundTint(ContextCompat.getColor(activity, backgroundColorRes));
        snackbar.setTextColor(ContextCompat.getColor(activity, R.color.white));
        return snackbar;
    }
}
