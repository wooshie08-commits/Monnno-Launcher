package com.monoos.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All installed apps with live search. Tap to open, long-press for options. */
public class DrawerActivity extends Activity {
    private Prefs prefs;
    private Theme theme;
    private List<Apps.Entry> all = new ArrayList<>();
    private final List<Apps.Entry> shown = new ArrayList<>();
    private EditText search;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        theme = new Theme(prefs);
        Ui.applyWindow(this, theme);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg);
        root.addView(Ui.header(this, theme, "Apps", "Launcher",
                v -> startActivity(new android.content.Intent(this, SettingsActivity.class))));

        search = new EditText(this);
        search.setHint("Search");
        search.setHintTextColor(theme.dim);
        search.setTextColor(theme.fg);
        search.setTextSize(22);
        search.setTypeface(theme.light);
        search.setBackground(null);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_DONE);
        search.setPadding(Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24), Ui.dp(this, 12));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int c, int d) {}
            public void afterTextChanged(Editable s) { filter(); }
        });
        search.setOnEditorActionListener((v, action, ev) -> {
            if (shown.size() >= 1) launch(shown.get(0));
            return true;
        });
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.rule(this, theme));

        ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setVerticalScrollBarEnabled(false);
        lv.setSelector(new ColorDrawable(theme.dark ? 0x33FFFFFF : 0x1A000000));
        adapter = new BaseAdapter() {
            public int getCount() { return shown.size(); }
            public Object getItem(int i) { return shown.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View old, ViewGroup parent) {
                TextView t = old instanceof TextView ? (TextView) old
                        : Ui.tv(DrawerActivity.this, theme, "", 22, false);
                t.setText(shown.get(i).label);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(Ui.dp(DrawerActivity.this, 60));
                t.setPadding(Ui.dp(DrawerActivity.this, 24), 0, Ui.dp(DrawerActivity.this, 24), 0);
                return t;
            }
        };
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> launch(shown.get(pos)));
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            Apps.showOptions(this, shown.get(pos), prefs, null);
            return true;
        });
        root.addView(lv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void launch(Apps.Entry e) {
        if (!Apps.launch(this, e.pkg)) Apps.toast(this, "Cannot open " + e.label);
    }

    private void filter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (Apps.Entry e : all) {
            if (q.length() == 0 || e.label.toLowerCase(Locale.getDefault()).contains(q)) shown.add(e);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        all = Apps.load(this);
        filter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        search.setText("");
    }
}
