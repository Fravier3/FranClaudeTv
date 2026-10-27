package com.frantv.premium;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/** Circulo con las iniciales del canal (cuando no hay logo). */
public class InitialsDrawable extends Drawable {
    private final String text;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] COLORS = {
            0xFFE11D48, 0xFFF97316, 0xFF2563EB, 0xFF0D9488, 0xFF9333EA, 0xFFDB2777, 0xFF0891B2, 0xFF4F46E5
    };

    public InitialsDrawable(Channel c) {
        text = c.initials();
        int h = Math.abs(c.name.hashCode());
        bg.setColor(COLORS[h % COLORS.length]);
        fg.setColor(0xFFFFFFFF);
        fg.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        fg.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float r = Math.min(b.width(), b.height()) / 2f * 0.78f;
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        canvas.drawCircle(cx, cy, r, bg);
        fg.setTextSize(r * 0.95f);
        float y = cy - (fg.descent() + fg.ascent()) / 2f;
        canvas.drawText(text, cx, y, fg);
    }

    @Override
    public void setAlpha(int alpha) {
        bg.setAlpha(alpha);
        fg.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        bg.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
