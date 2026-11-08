package com.monoos.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Basic launcher customisation. Tap a row to change it. */
public class SettingsActivity extends Activity {
    private Prefs prefs;
    private Theme theme;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        theme = new Theme(prefs);
        Ui.applyWindow(this, theme);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg);
        root.addView(Ui.header(this, theme, "Launcher", null, null));
        root.addView(Ui.rule(this, theme));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        String[] fonts = {"Thin sans", "Serif", "Mono"};
        list.addView(row("Theme", prefs.dark() ? "Dark" : "Light", v -> {
            prefs.setDark(!prefs.dark());
            recreate();
        }));
        list.addView(row("Clock", prefs.hour24() ? "24-hour" : "12-hour", v -> {
            prefs.setHour24(!prefs.hour24());
            recreate();
        }));
        list.addView(row("Greeting", prefs.greeting() ? "On" : "Off", v -> {
            prefs.setGreeting(!prefs.greeting());
            recreate();
        }));
        list.addView(row("Font", fonts[prefs.font() % 3], v -> {
            prefs.setFont((prefs.font() + 1) % 3);
            recreate();
        }));
        list.addView(row("Default home app", "Open", v -> {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (Exception e) {
                Apps.openSettings(this);
            }
        }));
        list.addView(row("Android settings", "Open", v -> Apps.openSettings(this)));

        TextView about = Ui.tv(this, theme, "MONO Launcher 1.0.0\nNo internet access. No tracking. Notes and reminders stay on this device.", 13, false);
        about.setTextColor(theme.dim);
        about.setPadding(Ui.dp(this, 24), Ui.dp(this, 32), Ui.dp(this, 24), Ui.dp(this, 32));
        list.addView(about);

        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private View row(String label, String value, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(Ui.dp(this, 64));
        r.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        TextView l = Ui.tv(this, theme, label, 20, false);
        TextView v = Ui.tv(this, theme, value, 18, false);
        v.setTextColor(theme.dim);
        r.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(v);
        r.setOnClickListener(click);
        Ui.press(r);
        return r;
    }
}
