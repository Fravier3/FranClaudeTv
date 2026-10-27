package com.frantv.premium;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.HashSet;
import java.util.Set;
import android.widget.TextView;

public final class Util {
    private Util() {}

    private static Set<String> localLogos;

    private static synchronized boolean hasLocalLogo(Context c, String id) {
        if (localLogos == null) {
            localLogos = new HashSet<>();
            try {
                String[] l = c.getApplicationContext().getAssets().list("logos");
                if (l != null) for (String f : l) localLogos.add(f);
            } catch (Exception ignored) {
            }
        }
        return localLogos.contains(id + ".png");
    }

    /** Logo del canal: 1) copia guardada en la app, 2) enlace del feed, 3) iniciales. */
    public static void loadLogo(final ImageView iv, Channel c) {
        Context ctx = iv.getContext();
        iv.setBackground(null);
        Drawable fallback = new InitialsDrawable(c);
        boolean remote = c.logo != null && (c.logo.startsWith("http://") || c.logo.startsWith("https://"));
        RequestBuilder<Drawable> remoteReq = remote
                ? Glide.with(iv).load(c.logo).error(fallback).fitCenter()
                : Glide.with(iv).load(fallback).fitCenter();
        if (hasLocalLogo(ctx, c.id)) {
            Glide.with(iv).load(Uri.parse("file:///android_asset/logos/" + c.id + ".png"))
                    .error(remoteReq).fitCenter().listener(backdrop(iv)).into(iv);
        } else if (remote) {
            remoteReq.listener(backdrop(iv)).into(iv);
        } else {
            Glide.with(iv).clear(iv);
            iv.setImageDrawable(fallback);
        }
    }

    /** Si el logo es oscuro, le pone detras un fondo claro para que se lea. */
    private static RequestListener<Drawable> backdrop(final ImageView iv) {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean first) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable r, Object model, Target<Drawable> target, DataSource src, boolean first) {
                boolean dark = false;
                try {
                    if (r instanceof BitmapDrawable) {
                        Bitmap b = ((BitmapDrawable) r).getBitmap();
                        long lum = 0;
                        int n = 0;
                        for (int y = 0; y < b.getHeight(); y += 4)
                            for (int x = 0; x < b.getWidth(); x += 4) {
                                int px = b.getPixel(x, y);
                                if (Color.alpha(px) > 128) {
                                    lum += (Color.red(px) + Color.green(px) + Color.blue(px)) / 3;
                                    n++;
                                }
                            }
                        dark = n > 0 && lum / n < 115;
                    }
                } catch (Exception ignored) {
                }
                if (dark) {
                    GradientDrawable g = new GradientDrawable();
                    g.setColor(0xFFE3EDF4);
                    g.setCornerRadius(dp(iv.getContext(), 8));
                    iv.setBackground(g);
                } else {
                    iv.setBackground(null);
                }
                return false;
            }
        };
    }

    /** Portada segun la seccion. */
    public static int heroRes(String key) {
        if (key == null) return R.drawable.hero_bg;
        String k = Repo.norm(key);
        if (k.contains("colombia")) return R.drawable.hero_co;
        if (k.contains("mexico")) return R.drawable.hero_mx;
        if (k.equals("usa") || k.contains("estados unidos")) return R.drawable.hero_us;
        if (k.contains("deport")) return R.drawable.hero_dep;
        if (k.contains("entret")) return R.drawable.hero_ent;
        return R.drawable.hero_bg;
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** "Fran" en blanco y "TV" en cian. */
    public static void wordmark(TextView tv) {
        SpannableString s = new SpannableString("FranTV");
        s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#34DCFF")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(s);
    }

    public static boolean isFlag(String category) {
        return flagRes(category) != 0;
    }

    private static int flagRes(String category) {
        String n = Repo.norm(category);
        if (n.contains("puerto rico")) return R.drawable.flag_pr;
        if (n.contains("colombia")) return R.drawable.flag_co;
        if (n.equals("usa") || n.contains("estados unidos") || n.contains("united states")) return R.drawable.flag_us;
        if (n.contains("mexico")) return R.drawable.flag_mx;
        return 0;
    }

    /** Icono de una categoria (bandera o simbolo). */
    public static void setCategoryIcon(ImageView iv, String category) {
        int flag = flagRes(category);
        if (flag != 0) {
            iv.setImageResource(flag);
            iv.clearColorFilter();
            return;
        }
        String n = Repo.norm(category);
        int res = R.drawable.ic_tv;
        if (n.contains("deport")) res = R.drawable.ic_ball;
        else if (n.contains("entret")) res = R.drawable.ic_clapper;
        iv.setImageResource(res);
        iv.setColorFilter(Color.parseColor("#D6E2FF"));
    }

    public static String[] heroText(String key) {
        String k = Repo.norm(key);
        if (Repo.KEY_FAV.equals(key)) return new String[]{"Tus favoritos.", "Siempre a la mano.", "Mantén OK sobre un canal para agregarlo o quitarlo."};
        if (Repo.KEY_ALL.equals(key)) return new String[]{"Tu televisión.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.contains("puerto rico")) return new String[]{"Tu isla.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.contains("colombia")) return new String[]{"Tu tierra.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.contains("mexico")) return new String[]{"Tu México.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.equals("usa") || k.contains("estados unidos")) return new String[]{"Lo mejor de USA.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.contains("deport")) return new String[]{"Todo el deporte.", "Tus canales.", "Elige un canal para comenzar."};
        if (k.contains("entret")) return new String[]{"Tu diversión.", "Tus canales.", "Elige un canal para comenzar."};
        return new String[]{key + ".", "Tus canales.", "Elige un canal para comenzar."};
    }

    public static String categoryLabel(String key) {
        if (Repo.KEY_ALL.equals(key)) return "Todos los canales";
        if (Repo.KEY_FAV.equals(key)) return "Favoritos";
        return key;
    }

    /** Icono para una clave de lista (todos, favoritos o categoria). */
    public static void setKeyIcon(ImageView iv, String key) {
        if (Repo.KEY_ALL.equals(key)) {
            iv.setImageResource(R.drawable.ic_grid);
            iv.setColorFilter(Color.parseColor("#D6E2FF"));
        } else if (Repo.KEY_FAV.equals(key)) {
            iv.setImageResource(R.drawable.ic_heart);
            iv.setColorFilter(Color.parseColor("#D6E2FF"));
        } else {
            setCategoryIcon(iv, key);
        }
    }
}
