/* Copyright (C) 2026 ParasaurPal. Licensed under GPL-3.0-or-later. */
package com.stario.launcher.sheet.drawer.niagara;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.themes.ThemedActivity;
import java.util.Locale;

public final class NiagaraIconPicker {
    public static final int STYLE_BLACK_WHITE = 0;
    public static final int STYLE_INVERTED = 1;
    public static final int STYLE_DARK_COLOR = 2;
    public static final int STYLE_LIGHT_COLOR = 3;
    private static final String PREFS = "niagara_category_icons";
    private static final String[] CATEGORIES = {
        "Art & Design","Auto & Vehicles","Beauty","Books & Reference","Business","Comics",
        "Communication","Dating","Education","Entertainment","Events","Finance","Food & Drink",
        "Health & Fitness","House & Home","Libraries & Demo","Lifestyle","Maps & Navigation",
        "Medical","Music & Audio","News & Magazines","Parenting","Personalization","Photography",
        "Productivity","Shopping","Social","Sports","Tools","Travel & Local",
        "Video Players & Editors","Weather","Games: Action","Games: Adventure","Games: Arcade",
        "Games: Board","Games: Card","Games: Casino","Games: Casual","Games: Educational",
        "Games: Music","Games: Puzzle","Games: Racing","Games: Role Playing","Games: Simulation",
        "Games: Sports","Games: Strategy","Games: Trivia","Games: Word"
    };
    private NiagaraIconPicker() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static String key(LauncherApplication app) {
        return app.getInfo().packageName + "_" + app.getProfile().hashCode();
    }
    public static Drawable icon(Context context, LauncherApplication app, int automaticCategory) {
        SharedPreferences p = prefs(context);
        String k = key(app);
        int category = p.getInt(k + "_category", -2);
        int style = p.getInt(k + "_style", STYLE_BLACK_WHITE);
        if (category == -2) category = automaticCategory;
        if (category < 0 || category >= CATEGORIES.length) category = 0;
        return new CategoryDrawable(CATEGORIES[category], category, style);
    }

    public static void show(ThemedActivity activity, LauncherApplication app, int automaticCategory,
                            Runnable refresh) {
        String k = key(app);
        SharedPreferences p = prefs(activity);
        int saved = p.getInt(k + "_category", -2);
        String[] choices = new String[CATEGORIES.length + 1];
        choices[0] = "Automatic (Android category)";
        System.arraycopy(CATEGORIES, 0, choices, 1, CATEGORIES.length);
        int selected = saved == -2 ? 0 : Math.max(1, Math.min(CATEGORIES.length, saved + 1));
        new AlertDialog.Builder(activity)
            .setTitle("Choose category icon")
            .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                int category = which - 1;
                new AlertDialog.Builder(activity)
                    .setTitle("Icon appearance")
                    .setSingleChoiceItems(new String[]{"Black & white","Inverted black & white",
                            "Adaptive dark colors","Adaptive light colors"},
                        p.getInt(k + "_style", STYLE_BLACK_WHITE),
                        (styleDialog, style) -> {
                            if (category < 0) p.edit().remove(k + "_category").putInt(k + "_style", style).apply();
                            else p.edit().putInt(k + "_category", category).putInt(k + "_style", style).apply();
                            styleDialog.dismiss();
                            dialog.dismiss();
                            refresh.run();
                        }).setNegativeButton("Cancel", null).show();
            })
            .setNeutralButton("Reset icon", (d, w) -> {
                p.edit().remove(k + "_category").remove(k + "_style").apply();
                refresh.run();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static final class CategoryDrawable extends Drawable {
        private final String label;
        private final int category;
        private final int style;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        CategoryDrawable(String label, int category, int style) {
            this.label = label;
            this.category = category;
            this.style = style;
        }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            float size = Math.min(b.width(), b.height());
            float inset = size * .07f;
            rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            int bg = style == STYLE_BLACK_WHITE ? Color.WHITE :
                style == STYLE_INVERTED ? Color.BLACK :
                style == STYLE_DARK_COLOR ? Color.rgb(35, 31, 48) : Color.rgb(245, 241, 250);
            int fg = style == STYLE_BLACK_WHITE ? Color.BLACK :
                style == STYLE_INVERTED ? Color.WHITE :
                style == STYLE_DARK_COLOR ? colorFor(category) : darken(colorFor(category));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(bg);
            canvas.drawRoundRect(rect, size * .24f, size * .24f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * .065f);
            paint.setColor(fg);
            canvas.drawRoundRect(rect, size * .24f, size * .24f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size * (label.length() > 15 ? .27f : .38f));
            String mark = abbreviation(label);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float y = b.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(mark, b.centerX(), y, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * .035f);
            float dot = size * .08f;
            canvas.drawCircle(b.centerX(), b.top + size * .2f, dot, paint);
        }
        private String abbreviation(String value) {
            String[] words = value.replace("Games: ", "").split("[ &:-]+");
            if (words.length > 1) return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(Locale.ROOT);
            return value.substring(0, Math.min(2, value.length())).toUpperCase(Locale.ROOT);
        }
        private int colorFor(int index) {
            float hue = (index * 47f) % 360f;
            return Color.HSVToColor(new float[]{hue, .72f, .98f});
        }
        private int darken(int color) {
            float[] hsv = new float[3]; Color.colorToHSV(color, hsv); hsv[2] = .48f; hsv[1] = .8f;
            return Color.HSVToColor(hsv);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return 48; }
        @Override public int getIntrinsicHeight() { return 48; }
        @Override public ConstantState getConstantState() {
            final String l = label; final int c = category; final int s = style;
            return new ConstantState() {
                @Override public Drawable newDrawable() { return new CategoryDrawable(l, c, s); }
                @Override public int getChangingConfigurations() { return 0; }
            };
        }
    }
}
