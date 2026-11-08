package com.monoos.launcher;

import android.graphics.Typeface;

/** Pure black / white plus one dim grey. Nothing else. */
final class Theme {
    final boolean dark;
    final int bg, fg, dim;
    final Typeface thin, light;

    Theme(Prefs p) {
        dark = p.dark();
        bg = dark ? 0xFF000000 : 0xFFFFFFFF;
        fg = dark ? 0xFFFFFFFF : 0xFF000000;
        dim = dark ? 0xFF9A9A9A : 0xFF666666;
        switch (p.font()) {
            case 1:
                thin = Typeface.create("serif", Typeface.NORMAL);
                light = thin;
                break;
            case 2:
                thin = Typeface.MONOSPACE;
                light = thin;
                break;
            default:
                thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);
                light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
    }
}
