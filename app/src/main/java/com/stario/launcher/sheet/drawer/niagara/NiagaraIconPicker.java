/* Copyright (C) 2026 ParasaurPal. Licensed under GPL-3.0-or-later. */
package com.stario.launcher.sheet.drawer.niagara;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.themes.ThemedActivity;

public final class NiagaraIconPicker {
    public static final int STYLE_BLACK_WHITE = 0;
    public static final int STYLE_INVERTED = 1;
    public static final int STYLE_DARK_COLOR = 2;
    public static final int STYLE_LIGHT_COLOR = 3;
    private static final String PREFS = "niagara_category_icons";
    private static final String[] CATEGORIES = {
        "Apps & Utilities","Art & Design","Auto & Vehicles","Beauty","Books & Reference","Business","Comics",
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
        int currentStyle = p.getInt(k + "_style", STYLE_BLACK_WHITE);
        GridView grid = new GridView(activity);
        grid.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 440)));
        grid.setNumColumns(3);
        grid.setVerticalSpacing(dp(activity, 8));
        grid.setHorizontalSpacing(dp(activity, 6));
        grid.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return CATEGORIES.length + 1; }
            @Override public Object getItem(int position) { return position; }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                LinearLayout cell = new LinearLayout(activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(activity, 3), dp(activity, 5), dp(activity, 3), dp(activity, 5));
                ImageView image = new ImageView(activity);
                int category = position == 0 ? automaticCategory : position - 1;
                String name = position == 0 ? "Automatic" : CATEGORIES[position - 1];
                image.setImageDrawable(new CategoryDrawable(CATEGORIES[Math.max(0, Math.min(CATEGORIES.length - 1, category))],
                        Math.max(0, category), currentStyle));
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48));
                cell.addView(image, iconParams);
                TextView label = new TextView(activity);
                label.setText(position == 0 ? name : name);
                label.setTextSize(11);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                cell.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 34)));
                return cell;
            }
        });
        AlertDialog categoryDialog = new AlertDialog.Builder(activity)
                .setTitle("Choose category icon")
                .setView(grid)
                .setNeutralButton("Reset icon", (d, w) -> {
                    p.edit().remove(k + "_category").remove(k + "_style").apply();
                    refresh.run();
                })
                .setNegativeButton("Cancel", null)
                .create();
        grid.setOnItemClickListener((parent, view, position, id) -> {
            int category = position - 1;
            new AlertDialog.Builder(activity)
                .setTitle("Icon appearance")
                .setSingleChoiceItems(new String[]{"Black & white","Inverted black & white",
                        "Adaptive dark colors","Adaptive light colors"}, currentStyle,
                    (styleDialog, style) -> {
                        if (category < 0) p.edit().remove(k + "_category").putInt(k + "_style", style).apply();
                        else p.edit().putInt(k + "_category", category).putInt(k + "_style", style).apply();
                        styleDialog.dismiss();
                        categoryDialog.dismiss();
                        refresh.run();
                    }).setNegativeButton("Cancel", null).show();
        });
        categoryDialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
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
            canvas.save();
            canvas.translate(b.centerX() - size * .36f, b.centerY() - size * .30f);
            canvas.scale(size / 48f * .72f, size / 48f * .72f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(2.8f);
            drawGlyph(canvas, category, paint);
            canvas.restore();
        }
        private void drawGlyph(Canvas c, int id, Paint p) {
            Path path = new Path();
            switch (id) {
                case 0: // Apps
                    c.drawRoundRect(5, 5, 19, 19, 3, 3, p); c.drawRoundRect(29, 5, 43, 19, 3, 3, p);
                    c.drawRoundRect(5, 29, 19, 43, 3, 3, p); c.drawRoundRect(29, 29, 43, 43, 3, 3, p); break;
                case 1: // Art
                    c.drawCircle(24, 24, 17, p); c.drawCircle(18, 17, 2, p); c.drawCircle(29, 15, 2, p);
                    c.drawCircle(34, 25, 2, p); c.drawCircle(18, 30, 2, p); break;
                case 2: // Auto
                    poly(c,p,4,29,8,20,15,17,34,17,40,23,44,29,44,34,4,34,4,29);
                    c.drawCircle(13,34,4,p); c.drawCircle(35,34,4,p); break;
                case 3: // Beauty
                    c.drawRoundRect(15,15,33,42,4,4,p); c.drawRoundRect(19,8,29,15,2,2,p);
                    line(c,p,19,25,29,25); break;
                case 4: // Books
                    poly(c,p,5,9,21,12,24,16,27,12,43,9,43,39,27,42,24,45,21,42,5,39,5,9);
                    line(c,p,24,16,24,45); break;
                case 5: // Business
                    c.drawRoundRect(5,16,43,40,4,4,p); c.drawRoundRect(17,9,31,17,2,2,p);
                    line(c,p,5,25,43,25); line(c,p,20,25,28,25); break;
                case 6: // Comics
                    poly(c,p,6,8,42,8,42,34,24,34,14,42,14,34,6,34,6,8); line(c,p,13,17,35,17); line(c,p,13,24,29,24); break;
                case 7: // Communication
                    c.drawRoundRect(14,5,34,43,4,4,p); line(c,p,20,11,28,11); line(c,p,21,37,27,37); break;
                case 8: // Dating
                    heart(c,p,24,26,18); break;
                case 9: // Education
                    poly(c,p,3,17,24,7,45,17,24,27,3,17); line(c,p,12,22,12,34);
                    poly(c,p,12,34,24,40,36,34,36,23); break;
                case 10: // Entertainment
                    c.drawRoundRect(6,10,42,38,4,4,p); line(c,p,13,10,13,38); line(c,p,35,10,35,38);
                    for(int y=15;y<35;y+=8){line(c,p,6,y,13,y);line(c,p,35,y,42,y);} break;
                case 11: // Events
                    c.drawRoundRect(6,10,42,41,4,4,p); line(c,p,6,19,42,19); line(c,p,15,6,15,14); line(c,p,33,6,33,14);
                    c.drawCircle(17,27,2,p); c.drawCircle(25,27,2,p); c.drawCircle(33,27,2,p); break;
                case 12: // Finance
                    c.drawRoundRect(5,11,43,39,4,4,p); line(c,p,5,20,43,20); c.drawCircle(33,30,4,p); break;
                case 13: // Food
                    line(c,p,12,6,12,42); line(c,p,7,6,7,17); line(c,p,17,6,17,17);
                    c.drawArc(27,6,41,26,0,180,false,p); line(c,p,34,19,34,42); break;
                case 14: // Health
                    heart(c,p,24,25,17); line(c,p,7,25,17,25); line(c,p,17,25,21,17);
                    line(c,p,21,17,27,33); line(c,p,27,33,31,25); line(c,p,31,25,41,25); break;
                case 15: // Home
                    poly(c,p,4,22,24,5,44,22,39,22,39,42,9,42,9,22,4,22);
                    c.drawRect(20,29,28,42,p); break;
                case 16: // Libraries
                    c.drawRoundRect(7,8,17,40,2,2,p); c.drawRoundRect(20,5,30,40,2,2,p);
                    c.drawRoundRect(33,11,41,40,2,2,p); break;
                case 17: // Lifestyle
                    path.moveTo(8,38); path.cubicTo(8,8,38,5,40,8); path.cubicTo(42,35,22,43,8,38); path.close();
                    c.drawPath(path,p); line(c,p,10,38,34,15); break;
                case 18: // Maps
                    path.moveTo(24,43); path.cubicTo(20,36,9,25,9,18); path.cubicTo(9,1,39,1,39,18);
                    path.cubicTo(39,25,28,36,24,43); path.close(); c.drawPath(path,p); c.drawCircle(24,18,6,p); break;
                case 19: // Medical
                    c.drawRoundRect(7,7,41,41,5,5,p); line(c,p,24,14,24,34); line(c,p,14,24,34,24); break;
                case 20: // Music
                    path.moveTo(19,32); line(c,p,19,32,19,9); line(c,p,19,9,37,5); line(c,p,37,5,37,27);
                    c.drawPath(path,p); c.drawOval(8,29,20,40,p); c.drawOval(26,25,38,36,p); break;
                case 21: // News
                    c.drawRoundRect(7,6,41,42,3,3,p); line(c,p,13,13,35,13); line(c,p,13,20,35,20);
                    line(c,p,13,27,23,27); line(c,p,13,34,35,34); c.drawRect(27,25,35,30,p); break;
                case 22: // Parenting
                    c.drawCircle(24,17,8,p); c.drawArc(7,24,41,46,180,180,false,p); c.drawCircle(21,16,1, p); c.drawCircle(27,16,1,p); break;
                case 23: // Personalization
                    star(c,p,24,24,18); break;
                case 24: // Photography
                    c.drawRoundRect(5,12,43,39,5,5,p); c.drawRoundRect(14,7,25,13,2,2,p);
                    c.drawCircle(25,25,9,p); c.drawCircle(25,25,4,p); break;
                case 25: // Productivity
                    c.drawRoundRect(9,6,39,42,4,4,p); for(int y=15;y<36;y+=10){c.drawRect(14,y-3,19,y+2,p);line(c,p,23,y,34,y);} break;
                case 26: // Shopping
                    c.drawRoundRect(9,16,39,42,4,4,p); c.drawArc(15,5,33,26,180,180,false,p); break;
                case 27: // Social
                    c.drawCircle(18,16,6,p); c.drawCircle(33,19,5,p); c.drawArc(5,23,31,44,180,180,false,p);
                    c.drawArc(25,27,44,43,180,180,false,p); break;
                case 28: // Sports
                    c.drawCircle(24,24,18,p); line(c,p,7,15,41,33); line(c,p,7,33,41,15);
                    c.drawArc(12,6,36,42,70,220,false,p); break;
                case 29: // Tools
                    line(c,p,12,36,32,16); c.drawArc(25,5,43,23,35,250,false,p); c.drawRoundRect(5,31,17,43,3,3,p); break;
                case 30: // Travel
                    poly(c,p,3,26,20,22,18,8,23,5,29,21,44,17,45,23,30,29,25,43,20,42,21,30,3,26); break;
                case 31: // Video
                    c.drawRoundRect(5,10,43,38,5,5,p); poly(c,p,19,16,33,24,19,32,19,16); break;
                case 32: // Weather
                    c.drawCircle(24,24,9,p); for(int a=0;a<8;a++){double t=a*Math.PI/4; line(c,p,(float)(24+13*Math.cos(t)),(float)(24+13*Math.sin(t)),(float)(24+19*Math.cos(t)),(float)(24+19*Math.sin(t)));} break;
                case 33: // Games
                    c.drawRoundRect(5,15,43,34,9,9,p); line(c,p,15,20,15,29); line(c,p,10,24,20,24);
                    c.drawCircle(32,22,1.5f,p); c.drawCircle(37,28,1.5f,p); break;
                case 34: // Adventure
                    poly(c,p,5,39,22,12,29,23,34,17,44,39,5,39); break;
                case 35: // Arcade
                    c.drawRoundRect(9,5,39,43,4,4,p); c.drawCircle(24,18,5,p); line(c,p,24,23,24,34); break;
                case 36: // Board
                    c.drawRect(7,7,41,41,p); for(int n=1;n<4;n++){line(c,p,7+n*8.5f,7,7+n*8.5f,41);line(c,p,7,7+n*8.5f,41,7+n*8.5f);} break;
                case 37: // Card
                    c.drawRoundRect(8,5,40,43,4,4,p); heart(c,p,24,25,10); break;
                case 38: // Casino
                    c.drawRoundRect(7,7,41,41,5,5,p); c.drawCircle(16,16,2,p); c.drawCircle(32,16,2,p);
                    c.drawCircle(24,24,2,p); c.drawCircle(16,32,2,p); c.drawCircle(32,32,2,p); break;
                case 39: // Casual
                    c.drawCircle(24,24,17,p); c.drawCircle(18,20,1.5f,p); c.drawCircle(30,20,1.5f,p);
                    c.drawArc(16,19,32,34,15,150,false,p); break;
                case 40: // Educational
                    poly(c,p,3,17,24,7,45,17,24,27,3,17); line(c,p,12,22,12,34); line(c,p,12,34,24,40); line(c,p,24,40,36,34); break;
                case 41: // Music game
                    line(c,p,21,31,21,8); line(c,p,21,8,38,5); line(c,p,38,5,38,27); c.drawOval(10,29,22,40,p); c.drawOval(27,25,39,36,p); break;
                case 42: // Puzzle
                    c.drawRoundRect(7,7,41,41,4,4,p); c.drawCircle(24,7,5,p); c.drawCircle(41,24,5,p);
                    c.drawCircle(24,41,5,p); c.drawCircle(7,24,5,p); break;
                case 43: // Racing
                    poly(c,p,10,7,38,7,38,14,17,14,17,21,34,21,34,28,17,28,17,41,10,41,10,7); break;
                case 44: // Role playing
                    poly(c,p,7,19,15,9,24,15,33,9,41,19,35,39,13,39,7,19); line(c,p,24,15,24,39); break;
                case 45: // Simulation
                    poly(c,p,5,22,24,6,43,22,38,22,38,41,10,41,10,22,5,22); break;
                case 46: // Sports game
                    c.drawCircle(24,24,17,p); line(c,p,10,12,38,36); line(c,p,10,36,38,12); break;
                case 47: // Strategy
                    c.drawRect(11,7,37,42,p); line(c,p,11,16,37,16); line(c,p,18,7,18,42); line(c,p,30,7,30,42); break;
                case 48: // Trivia
                    c.drawCircle(24,24,18,p); p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
                    p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(25); c.drawText("?",24,33,p); p.setStyle(Paint.Style.STROKE); break;
                default: // Word
                    p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD);
                    p.setTextSize(23); c.drawText("Aa",24,32,p); p.setStyle(Paint.Style.STROKE); break;
            }
        }
        private void line(Canvas c, Paint p, float x1, float y1, float x2, float y2) {
            c.drawLine(x1,y1,x2,y2,p);
        }
        private void poly(Canvas c, Paint p, float... points) {
            Path path = new Path(); path.moveTo(points[0], points[1]);
            for (int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);
            c.drawPath(path,p);
        }
        private void heart(Canvas c, Paint p, float x, float y, float r) {
            Path path = new Path(); path.moveTo(x,y+r*.85f);
            path.cubicTo(x-r*1.7f,y-r*.2f,x-r*.9f,y-r*1.4f,x,y-r*.55f);
            path.cubicTo(x+r*.9f,y-r*1.4f,x+r*1.7f,y-r*.2f,x,y+r*.85f);
            c.drawPath(path,p);
        }
        private void star(Canvas c, Paint p, float x, float y, float r) {
            Path path = new Path();
            for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float radius=i%2==0?r:r*.42f;
                float px=x+(float)Math.cos(a)*radius, py=y+(float)Math.sin(a)*radius;
                if(i==0) path.moveTo(px,py); else path.lineTo(px,py);}
            path.close(); c.drawPath(path,p);
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
