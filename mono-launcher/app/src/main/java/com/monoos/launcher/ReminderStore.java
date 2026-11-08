package com.monoos.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reminders (files/reminders.json) and their AlarmManager alarms. */
final class ReminderStore {
    private ReminderStore() {}

    static final class Reminder {
        long id;
        String text;
        long when;
    }

    private static final Object LOCK = new Object();

    private static File file(Context c) {
        return new File(c.getFilesDir(), "reminders.json");
    }

    static List<Reminder> load(Context c) {
        synchronized (LOCK) {
            List<Reminder> out = new ArrayList<>();
            File f = file(c);
            if (!f.exists()) return out;
            try {
                JSONArray a = new JSONArray(Io.read(f));
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Reminder r = new Reminder();
                    r.id = o.getLong("id");
                    r.text = o.getString("text");
                    r.when = o.getLong("when");
                    out.add(r);
                }
            } catch (Exception e) {
                f.renameTo(new File(f.getPath() + ".corrupt"));
                out.clear();
            }
            Collections.sort(out, (x, y) -> Long.compare(x.when, y.when));
            return out;
        }
    }

    private static void save(Context c, List<Reminder> list) {
        try {
            JSONArray a = new JSONArray();
            for (Reminder r : list) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("text", r.text);
                o.put("when", r.when);
                a.put(o);
            }
            Io.writeAtomic(file(c), a.toString());
        } catch (Exception e) {
            Apps.toast(c, "Could not save reminder");
        }
    }

    static void add(Context c, String text, long when) {
        synchronized (LOCK) {
            List<Reminder> list = load(c);
            Reminder r = new Reminder();
            r.id = System.currentTimeMillis();
            r.text = text;
            r.when = when;
            list.add(r);
            save(c, list);
            schedule(c, r);
        }
    }

    static void remove(Context c, long id) {
        synchronized (LOCK) {
            List<Reminder> list = load(c);
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).id == id) {
                    cancel(c, list.get(i));
                    list.remove(i);
                }
            }
            save(c, list);
        }
    }

    /** Removes the entry without touching the alarm (used when it has just fired). */
    static String consume(Context c, long id) {
        synchronized (LOCK) {
            List<Reminder> list = load(c);
            String text = null;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).id == id) {
                    text = list.get(i).text;
                    list.remove(i);
                }
            }
            save(c, list);
            return text;
        }
    }

    static int pendingCount(Context c) {
        int n = 0;
        long now = System.currentTimeMillis();
        for (Reminder r : load(c)) if (r.when > now) n++;
        return n;
    }

    private static PendingIntent pending(Context c, Reminder r) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("com.monoos.launcher.REMINDER." + r.id);
        i.putExtra("id", r.id);
        i.putExtra("text", r.text);
        return PendingIntent.getBroadcast(c, (int) (r.id ^ (r.id >>> 32)), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c, Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(c, r);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.when, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.when, pi);
        }
    }

    static void cancel(Context c, Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(c, r));
    }

    static void rescheduleAll(Context c) {
        long now = System.currentTimeMillis();
        for (Reminder r : load(c)) {
            if (r.when > now) schedule(c, r);
        }
    }
}
