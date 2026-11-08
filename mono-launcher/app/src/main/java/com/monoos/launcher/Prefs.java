package com.monoos.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Launcher settings, stored locally in SharedPreferences. */
final class Prefs {
    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("mono", Context.MODE_PRIVATE);
    }

    boolean dark() { return sp.getBoolean("dark", false); }
    void setDark(boolean v) { sp.edit().putBoolean("dark", v).apply(); }

    boolean hour24() { return sp.getBoolean("hour24", true); }
    void setHour24(boolean v) { sp.edit().putBoolean("hour24", v).apply(); }

    boolean greeting() { return sp.getBoolean("greeting", true); }
    void setGreeting(boolean v) { sp.edit().putBoolean("greeting", v).apply(); }

    /** 0 = thin sans, 1 = serif, 2 = mono. */
    int font() { return sp.getInt("font", 0); }
    void setFont(int v) { sp.edit().putInt("font", v).apply(); }

    List<String> pinned() {
        List<String> out = new ArrayList<>();
        String s = sp.getString("pinned", "");
        if (s.length() > 0) {
            for (String p : s.split(",")) {
                if (p.length() > 0) out.add(p);
            }
        }
        return out;
    }

    void setPinned(List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p);
        }
        sp.edit().putString("pinned", sb.toString()).apply();
    }

    String signature() {
        return dark() + "|" + hour24() + "|" + greeting() + "|" + font() + "|" + sp.getString("pinned", "");
    }
}
