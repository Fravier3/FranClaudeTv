package com.frantv.premium;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.TextView;

public final class Util {
    private Util() {}

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
