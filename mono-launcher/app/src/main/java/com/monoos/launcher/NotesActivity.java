package com.monoos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** List of notes, newest first. */
public class NotesActivity extends Activity {
    private Theme theme;
    private List<NoteStore.Note> notes = new ArrayList<>();
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
        root.addView(Ui.header(this, theme, "Notes", "+ New", v -> openEditor(0)));
        root.addView(Ui.rule(this, theme));

        empty = Ui.tv(this, theme, "No notes yet.\nTap + New to write one.", 18, false);
        empty.setTextColor(theme.dim);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 32), Ui.dp(this, 64), Ui.dp(this, 32), 0);
        root.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView lv = new ListView(this);
        lv.setDivider(new ColorDrawable(0x33808080));
        lv.setDividerHeight(1);
        lv.setVerticalScrollBarEnabled(false);
        lv.setSelector(new ColorDrawable(theme.dark ? 0x33FFFFFF : 0x1A000000));
        adapter = new BaseAdapter() {
            public int getCount() { return notes.size(); }
            public Object getItem(int i) { return notes.get(i); }
            public long getItemId(int i) { return notes.get(i).id; }
            public View getView(int i, View old, ViewGroup parent) {
                LinearLayout row;
                TextView title, sub;
                if (old instanceof LinearLayout) {
                    row = (LinearLayout) old;
                    title = (TextView) row.getChildAt(0);
                    sub = (TextView) row.getChildAt(1);
                } else {
                    row = new LinearLayout(NotesActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setMinimumHeight(Ui.dp(NotesActivity.this, 72));
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding(Ui.dp(NotesActivity.this, 24), Ui.dp(NotesActivity.this, 10),
                            Ui.dp(NotesActivity.this, 24), Ui.dp(NotesActivity.this, 10));
                    title = Ui.tv(NotesActivity.this, theme, "", 20, false);
                    title.setSingleLine(true);
                    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    sub = Ui.tv(NotesActivity.this, theme, "", 13, false);
                    sub.setTextColor(theme.dim);
                    row.addView(title);
                    row.addView(sub);
                }
                NoteStore.Note n = notes.get(i);
                title.setText(n.title());
                sub.setText(Ui.fmt(n.updated));
                return row;
            }
        };
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> openEditor(notes.get(pos).id));
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            final NoteStore.Note n = notes.get(pos);
            new AlertDialog.Builder(this)
                    .setTitle(n.title())
                    .setItems(new String[]{"Delete note"}, (d, which) -> confirmDelete(n))
                    .show();
            return true;
        });
        root.addView(lv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void confirmDelete(final NoteStore.Note n) {
        new AlertDialog.Builder(this)
                .setMessage("Delete this note?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    NoteStore.delete(this, n.id);
                    reload();
                })
                .show();
    }

    private void openEditor(long id) {
        Intent i = new Intent(this, NoteEditorActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    private void reload() {
        notes = NoteStore.load(this);
        empty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }
}
