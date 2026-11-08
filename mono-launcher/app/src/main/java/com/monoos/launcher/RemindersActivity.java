package com.monoos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Create reminders; each one fires a notification at its time. */
public class RemindersActivity extends Activity {
    private Theme theme;
    private List<ReminderStore.Reminder> items = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView empty;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        theme = new Theme(new Prefs(this));
        Ui.applyWindow(this, theme);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg);
        root.addView(Ui.header(this, theme, "Reminders", "+ New", v -> askText()));
        root.addView(Ui.rule(this, theme));

        empty = Ui.tv(this, theme, "Nothing scheduled.\nTap + New to add a reminder.", 18, false);
        empty.setTextColor(theme.dim);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 32), Ui.dp(this, 64), Ui.dp(this, 32), 0);
        root.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView lv = new ListView(this);
        lv.setDivider(new ColorDrawable(0x33808080));
        lv.setDividerHeight(1);
        lv.setVerticalScrollBarEnabled(false);
        lv.setSelector(new ColorDrawable(theme.dark ? 0x33FFFFFF : 0x1A000000));
        adapter = new BaseAdapter() {
            public int getCount() { return items.size(); }
            public Object getItem(int i) { return items.get(i); }
            public long getItemId(int i) { return items.get(i).id; }
            public View getView(int i, View old, ViewGroup parent) {
                LinearLayout row;
                TextView title, sub;
                if (old instanceof LinearLayout) {
                    row = (LinearLayout) old;
                    title = (TextView) row.getChildAt(0);
                    sub = (TextView) row.getChildAt(1);
                } else {
                    row = new LinearLayout(RemindersActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setMinimumHeight(Ui.dp(RemindersActivity.this, 72));
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(Ui.dp(RemindersActivity.this, 24), Ui.dp(RemindersActivity.this, 10),
                            Ui.dp(RemindersActivity.this, 24), Ui.dp(RemindersActivity.this, 10));
                    title = Ui.tv(RemindersActivity.this, theme, "", 20, false);
                    sub = Ui.tv(RemindersActivity.this, theme, "", 13, false);
                    sub.setTextColor(theme.dim);
                    row.addView(title);
                    row.addView(sub);
                }
                ReminderStore.Reminder r = items.get(i);
                title.setText(r.text);
                sub.setText(Ui.fmt(r.when));
                return row;
            }
        };
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> {
            final ReminderStore.Reminder r = items.get(pos);
            new AlertDialog.Builder(this)
                    .setMessage(r.text)
                    .setNegativeButton("Close", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        ReminderStore.remove(this, r.id);
                        reload();
                    })
                    .show();
        });
        root.addView(lv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void askText() {
        final EditText input = new EditText(this);
        input.setHint("Remind me to\u2026");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle("New reminder")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Next", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.length() == 0) {
                        Apps.toast(this, "Write something first");
                    } else {
                        askDate(s);
                    }
                })
                .show();
    }

    private void askDate(final String text) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, day) -> {
            final Calendar picked = Calendar.getInstance();
            picked.set(y, m, day);
            new TimePickerDialog(this, (tv, h, min) -> {
                picked.set(Calendar.HOUR_OF_DAY, h);
                picked.set(Calendar.MINUTE, min);
                picked.set(Calendar.SECOND, 0);
                picked.set(Calendar.MILLISECOND, 0);
                if (picked.getTimeInMillis() <= System.currentTimeMillis()) {
                    Apps.toast(this, "Pick a time in the future");
                    return;
                }
                ReminderStore.add(this, text, picked.getTimeInMillis());
                reload();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void reload() {
        items = ReminderStore.load(this);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }
}
