package com.monoos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Writes a note. Saves automatically while typing and when leaving. */
public class NoteEditorActivity extends Activity {
    private long id;
    private EditText edit;
    private boolean deleted;
    private String lastSaved = "";
    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private final Runnable autosave = this::save;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme t = new Theme(new Prefs(this));
        Ui.applyWindow(this, t);
        id = getIntent().getLongExtra("id", 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(t.bg);
        root.addView(Ui.header(this, t, "Note", "Delete", v -> confirmDelete()));
        root.addView(Ui.rule(this, t));

        edit = new EditText(this);
        edit.setBackground(null);
        edit.setTextColor(t.fg);
        edit.setHintTextColor(t.dim);
        edit.setHint("Write\u2026");
        edit.setTextSize(20);
        edit.setTypeface(t.light);
        edit.setLineSpacing(0, 1.2f);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setPadding(Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20));
        root.addView(edit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        if (id != 0) {
            NoteStore.Note n = NoteStore.get(this, id);
            if (n != null) {
                edit.setText(n.text);
                edit.setSelection(n.text.length());
                lastSaved = n.text;
            }
        }
        edit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int c, int d) {}
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(autosave);
                handler.postDelayed(autosave, 700);
            }
        });
        if (id == 0) edit.requestFocus();
    }

    private void save() {
        if (deleted) return;
        String text = edit.getText().toString();
        if (text.equals(lastSaved)) return;
        if (text.trim().length() == 0) {
            if (id != 0) {
                NoteStore.delete(this, id); // an emptied note disappears
                id = 0;
            }
        } else {
            id = NoteStore.upsert(this, id, text);
        }
        lastSaved = text;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage("Delete this note?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    handler.removeCallbacks(autosave);
                    if (id != 0) NoteStore.delete(this, id);
                    deleted = true;
                    finish();
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autosave);
        save();
    }
}
