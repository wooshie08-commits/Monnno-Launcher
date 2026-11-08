package com.monoos.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home screen: split clock, date, battery, and a short vertical list. */
public class MainActivity extends Activity {
    private Prefs prefs;
    private Theme theme;
    private String builtWith = "";

    private TextView hh, mm, date, batt, greet;
    private ScrollView scroll;
    private int battPct = -1;
    private GestureDetector swipe;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(i.getAction())) {
                int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (level >= 0 && scale > 0) battPct = level * 100 / scale;
            }
            refreshClock();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        swipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dy = e1.getY() - e2.getY();
                float dx = Math.abs(e1.getX() - e2.getX());
                if (dy > Ui.dp(MainActivity.this, 90) && dy > dx && !scroll.canScrollVertically(1)) {
                    startActivity(new Intent(MainActivity.this, DrawerActivity.class));
                    return true;
                }
                return false;
            }
        });
        rebuild();
    }

    private void rebuild() {
        theme = new Theme(prefs);
        builtWith = prefs.signature();
        Ui.applyWindow(this, theme);
        setContentView(buildUi());
        refreshClock();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent sticky = registerReceiver(tick, f);
        if (sticky != null) tick.onReceive(this, sticky);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(tick);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!builtWith.equals(prefs.signature())) rebuild();
        else refreshClock();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (scroll != null) scroll.scrollTo(0, 0);
    }

    /** A launcher has nowhere to go "back" to. */
    @Override
    public void onBackPressed() {
        if (scroll != null) scroll.smoothScrollTo(0, 0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipe != null) swipe.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void refreshClock() {
        if (hh == null) return;
        Date now = new Date();
        Locale loc = Locale.getDefault();
        hh.setText(new SimpleDateFormat(prefs.hour24() ? "HH" : "hh", loc).format(now));
        mm.setText(new SimpleDateFormat("mm", loc).format(now));
        date.setText(new SimpleDateFormat("EEE, d MMM", loc).format(now));
        int due = ReminderStore.pendingCount(this);
        String s = battPct >= 0 ? battPct + "%" : "";
        if (due > 0) s += (s.length() > 0 ? " \u00b7 " : "") + due + " due";
        batt.setText(s);
        if (prefs.greeting()) {
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            greet.setText(h < 5 ? "good night" : h < 12 ? "good morning" : h < 18 ? "good afternoon" : "good evening");
        }
    }

    private View buildUi() {
        Theme t = theme;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(t.bg);

        greet = Ui.tv(this, t, "", 14, false);
        greet.setTextColor(t.dim);
        greet.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gp.topMargin = Ui.dp(this, 40);
        gp.bottomMargin = Ui.dp(this, 12);
        greet.setVisibility(prefs.greeting() ? View.VISIBLE : View.INVISIBLE);
        root.addView(greet, gp);

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.HORIZONTAL);
        clock.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout left = column();
        hh = Ui.tv(this, t, "00", 72, true);
        date = Ui.tv(this, t, "", 14, false);
        left.addView(hh);
        left.addView(date, topMargin(8));

        View divider = new View(this);
        divider.setBackgroundColor(t.fg);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.dp(this, 2), LinearLayout.LayoutParams.MATCH_PARENT);
        dp.leftMargin = Ui.dp(this, 20);
        dp.rightMargin = Ui.dp(this, 20);
        dp.topMargin = Ui.dp(this, 6);

        LinearLayout right = column();
        mm = Ui.tv(this, t, "00", 72, true);
        batt = Ui.tv(this, t, "", 14, false);
        right.addView(mm);
        right.addView(batt, topMargin(8));

        clock.addView(left);
        clock.addView(divider, dp);
        clock.addView(right);
        root.addView(clock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.CENTER);
        list.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        list.addView(item("Notes", 32, false, v -> startActivity(new Intent(this, NotesActivity.class)), null));
        list.addView(item("Music", 24, false, v -> Apps.openMusic(this), null));
        list.addView(item("Reminders", 24, false, v -> startActivity(new Intent(this, RemindersActivity.class)), null));
        list.addView(item("Files", 24, false, v -> Apps.openFiles(this), null));
        list.addView(item("Settings", 24, false, v -> Apps.openSettings(this),
                v -> {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }));

        List<String> pins = prefs.pinned();
        for (String pkg : pins) {
            final Apps.Entry e = Apps.find(this, pkg);
            if (e == null) continue;
            list.addView(item(e.label, 24, false, v -> {
                if (!Apps.launch(this, e.pkg)) Apps.toast(this, "Cannot open " + e.label);
            }, v -> {
                Apps.showOptions(this, e, prefs, this::rebuild);
                return true;
            }));
        }

        list.addView(item("All apps", 18, true, v -> startActivity(new Intent(this, DrawerActivity.class)), null));
        return root;
    }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        return c;
    }

    private LinearLayout.LayoutParams topMargin(int dpv) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, dpv);
        return p;
    }

    private TextView item(String label, float sp, boolean dim, View.OnClickListener click, View.OnLongClickListener lc) {
        TextView v = Ui.tv(this, theme, label, sp, false);
        if (dim) v.setTextColor(theme.dim);
        v.setGravity(Gravity.CENTER);
        v.setMinHeight(Ui.dp(this, 56));
        v.setPadding(Ui.dp(this, 32), 0, Ui.dp(this, 32), 0);
        v.setOnClickListener(click);
        if (lc != null) v.setOnLongClickListener(lc);
        Ui.press(v);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return v;
    }
}
