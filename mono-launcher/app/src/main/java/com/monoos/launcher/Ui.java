package com.monoos.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Programmatic UI helpers. No XML layouts, no support libraries. */
final class Ui {
    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static void applyWindow(Activity a, Theme t) {
        Window w = a.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(t.bg));
        w.setStatusBarColor(t.bg);
        // Before Android 8 the navigation icons are always white, so keep that bar black there.
        boolean navLight = Build.VERSION.SDK_INT >= 26;
        w.setNavigationBarColor(navLight || t.dark ? t.bg : 0xFF000000);
        View d = w.getDecorView();
        int f = d.getSystemUiVisibility();
        if (t.dark) {
            f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            f &= ~0x10; // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR (API 26)
        } else {
            f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (navLight) f |= 0x10;
        }
        d.setSystemUiVisibility(f);
    }

    static TextView tv(Context c, Theme t, CharSequence s, float sp, boolean thin) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextColor(t.fg);
        v.setTextSize(sp);
        v.setTypeface(thin ? t.thin : t.light);
        v.setIncludeFontPadding(false);
        return v;
    }

    /** Fade the view slightly while pressed; does not consume the touch. */
    static void press(View v) {
        v.setOnTouchListener((view, ev) -> {
            int a = ev.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) view.setAlpha(0.35f);
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) view.setAlpha(1f);
            return false;
        });
    }

    static void goHome(Activity a) {
        Intent i = new Intent(a, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
    }

    /** Top bar: "‹ Home" on the left, title in the middle, optional action on the right. */
    static LinearLayout header(final Activity a, Theme t, String title, String action, View.OnClickListener onAction) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int minW = dp(a, 88);

        TextView home = tv(a, t, "\u2039 Home", 16, false);
        home.setTextColor(t.dim);
        home.setGravity(Gravity.CENTER_VERTICAL);
        home.setMinWidth(minW);
        home.setMinHeight(dp(a, 56));
        home.setPadding(dp(a, 20), 0, 0, 0);
        home.setOnClickListener(v -> goHome(a));
        press(home);
        bar.addView(home, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView ttl = tv(a, t, title, 20, false);
        ttl.setGravity(Gravity.CENTER);
        bar.addView(ttl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView act = tv(a, t, action == null ? "" : action, 16, false);
        act.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        act.setMinWidth(minW);
        act.setMinHeight(dp(a, 56));
        act.setPadding(0, 0, dp(a, 20), 0);
        if (action != null && onAction != null) {
            act.setOnClickListener(onAction);
            press(act);
        }
        bar.addView(act, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return bar;
    }

    static View rule(Context c, Theme t) {
        View v = new View(c);
        v.setBackgroundColor(t.dim);
        v.setAlpha(0.35f);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    static String fmt(long ms) {
        return DateFormat.format("EEE d MMM, HH:mm", ms).toString();
    }
}
