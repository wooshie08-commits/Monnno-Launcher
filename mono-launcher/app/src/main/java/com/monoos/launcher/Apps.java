package com.monoos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finding and launching installed apps, plus the music / files / settings shortcuts. */
final class Apps {
    private Apps() {}

    static final class Entry {
        final String label, pkg;
        Entry(String label, String pkg) { this.label = label; this.pkg = pkg; }
    }

    static List<Entry> load(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(q, 0);
        Set<String> seen = new HashSet<>();
        List<Entry> out = new ArrayList<>();
        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            out.add(new Entry(String.valueOf(ri.loadLabel(pm)), pkg));
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    static Entry find(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            if (pm.getLaunchIntentForPackage(pkg) == null) return null;
            return new Entry(String.valueOf(pm.getApplicationInfo(pkg, 0).loadLabel(pm)), pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static boolean launch(Context c, String pkg) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    static void openMusic(Context c) {
        try {
            Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            toast(c, "No music player installed");
        }
    }

    static void openFiles(Context c) {
        String[] pkgs = {
                "com.mi.android.globalFileexplorer", "com.android.fileexplorer",
                "com.google.android.apps.nbu.files", "com.sec.android.app.myfiles",
                "com.android.documentsui", "com.google.android.documentsui"
        };
        for (String p : pkgs) {
            if (launch(c, p)) return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            Intent chooser = Intent.createChooser(i, "Files");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(chooser);
        } catch (Exception e) {
            toast(c, "No file manager found");
        }
    }

    static void openSettings(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            toast(c, "Cannot open Settings");
        }
    }

    /** Long-press menu for an app. */
    static void showOptions(final Activity a, final Entry e, final Prefs prefs, final Runnable changed) {
        final List<String> pins = prefs.pinned();
        final boolean pinned = pins.contains(e.pkg);
        String[] items = {"App info", "Uninstall", pinned ? "Remove from home" : "Pin to home"};
        new AlertDialog.Builder(a)
                .setTitle(e.label)
                .setItems(items, (d, which) -> {
                    try {
                        if (which == 0) {
                            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", e.pkg, null)));
                        } else if (which == 1) {
                            a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", e.pkg, null)));
                        } else {
                            if (pinned) pins.remove(e.pkg);
                            else pins.add(e.pkg);
                            prefs.setPinned(pins);
                            if (changed != null) changed.run();
                        }
                    } catch (Exception ex) {
                        toast(a, "Not available for this app");
                    }
                })
                .show();
    }
}
