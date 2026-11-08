package com.monoos.launcher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Notes live in files/notes.json inside the app's private storage. */
final class NoteStore {
    private NoteStore() {}

    static final class Note {
        long id;
        String text;
        long updated;

        String title() {
            for (String line : text.split("\n")) {
                String t = line.trim();
                if (t.length() > 0) return t;
            }
            return "";
        }
    }

    private static final Object LOCK = new Object();

    private static File file(Context c) {
        return new File(c.getFilesDir(), "notes.json");
    }

    static List<Note> load(Context c) {
        synchronized (LOCK) {
            List<Note> out = new ArrayList<>();
            File f = file(c);
            if (!f.exists()) return out;
            try {
                JSONArray a = new JSONArray(Io.read(f));
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Note n = new Note();
                    n.id = o.getLong("id");
                    n.text = o.getString("text");
                    n.updated = o.getLong("updated");
                    out.add(n);
                }
            } catch (Exception e) {
                // Keep the unreadable file instead of overwriting it on the next save.
                f.renameTo(new File(f.getPath() + ".corrupt"));
                out.clear();
            }
            Collections.sort(out, (x, y) -> Long.compare(y.updated, x.updated));
            return out;
        }
    }

    private static void save(Context c, List<Note> notes) {
        try {
            JSONArray a = new JSONArray();
            for (Note n : notes) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("text", n.text);
                o.put("updated", n.updated);
                a.put(o);
            }
            Io.writeAtomic(file(c), a.toString());
        } catch (Exception e) {
            Apps.toast(c, "Could not save note");
        }
    }

    static Note get(Context c, long id) {
        for (Note n : load(c)) if (n.id == id) return n;
        return null;
    }

    /** Creates (id == 0) or updates a note and returns its id. */
    static long upsert(Context c, long id, String text) {
        synchronized (LOCK) {
            List<Note> notes = load(c);
            Note target = null;
            for (Note n : notes) if (n.id == id) target = n;
            if (target == null) {
                target = new Note();
                target.id = System.currentTimeMillis();
                notes.add(target);
            }
            target.text = text;
            target.updated = System.currentTimeMillis();
            save(c, notes);
            return target.id;
        }
    }

    static void delete(Context c, long id) {
        synchronized (LOCK) {
            List<Note> notes = load(c);
            for (int i = notes.size() - 1; i >= 0; i--) {
                if (notes.get(i).id == id) notes.remove(i);
            }
            save(c, notes);
        }
    }
}
