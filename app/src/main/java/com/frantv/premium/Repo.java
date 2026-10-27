package com.frantv.premium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Datos de la app: lista de canales (se actualiza sola), favoritos y lista en reproduccion. */
public final class Repo {
    public static final String FEED_URL =
            "https://raw.githubusercontent.com/Fravier3/IPTv-Fran/refs/heads/main/channels.json";
    public static final String UPDATE_URL =
            "https://raw.githubusercontent.com/Fravier3/FranClaudeTv/main/update.json";

    public static final String KEY_ALL = "__all";
    public static final String KEY_FAV = "__fav";

    private static final List<Channel> ALL = new ArrayList<>();
    private static SharedPreferences prefs;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Lista que se esta reproduciendo (la usa el reproductor para cambiar de canal). */
    public static List<Channel> playlist = new ArrayList<>();
    public static int playlistIndex = 0;
    public static String playlistKey = KEY_ALL;

    private Repo() {}

    public interface Callback {
        void onDone(boolean ok);
    }

    public static void init(Context c) {
        if (prefs == null) {
            prefs = c.getApplicationContext().getSharedPreferences("frantv", Context.MODE_PRIVATE);
        }
    }

    public static synchronized List<Channel> all() {
        return new ArrayList<>(ALL);
    }

    public static synchronized boolean isEmpty() {
        return ALL.isEmpty();
    }

    private static File cacheFile(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "channels_cache.json");
    }

    /** Carga la ultima copia guardada (arranque rapido, tambien sin internet). */
    public static synchronized void loadCached(Context c) {
        if (!ALL.isEmpty()) return;
        try {
            File f = cacheFile(c);
            if (!f.exists()) return;
            String json = readAll(new FileInputStream(f));
            List<Channel> list = parse(json);
            if (!list.isEmpty()) {
                ALL.clear();
                ALL.addAll(list);
            }
        } catch (Exception ignored) {
        }
    }

    /** Descarga la lista actual de canales en segundo plano. */
    public static void refresh(final Context c, final Callback cb) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    String json = httpGet(FEED_URL + "?t=" + (System.currentTimeMillis() / 30000));
                    List<Channel> list = parse(json);
                    if (!list.isEmpty()) {
                        synchronized (Repo.class) {
                            ALL.clear();
                            ALL.addAll(list);
                        }
                        FileOutputStream fo = new FileOutputStream(cacheFile(app));
                        fo.write(json.getBytes("UTF-8"));
                        fo.close();
                        ok = true;
                    }
                } catch (Exception ignored) {
                }
                final boolean result = ok;
                if (cb != null) {
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onDone(result);
                        }
                    });
                }
            }
        }).start();
    }

    static String httpGet(String u) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(u).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);
        con.setRequestProperty("User-Agent", "FranTV/1.0");
        con.setRequestProperty("Cache-Control", "no-cache");
        try {
            if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
            return readAll(con.getInputStream());
        } finally {
            con.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toString("UTF-8");
    }

    private static List<Channel> parse(String json) throws Exception {
        List<Channel> out = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("﻿")) json = json.substring(1);
        JSONArray arr;
        if (json.startsWith("[")) {
            arr = new JSONArray(json);
        } else {
            JSONObject o = new JSONObject(json);
            arr = o.has("channels") ? o.getJSONArray("channels") : new JSONArray();
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String name = o.optString("name", "").trim();
            String url = o.optString("url", "").trim();
            if (name.isEmpty() || url.isEmpty()) continue;
            String id = o.optString("id", name);
            String category = o.optString("category", "").trim();
            if (category.isEmpty()) category = o.optString("group", "").trim();
            if (category.isEmpty()) category = "Otros";
            out.add(new Channel(id, name, o.optString("country", ""), category,
                    o.optString("logo", ""), url));
        }
        return out;
    }

    // ---------- Categorias y listas ----------

    public static synchronized List<String> categories() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Channel c : ALL) set.add(c.category);
        return new ArrayList<>(set);
    }

    public static synchronized List<Channel> channelsOf(String key) {
        List<Channel> out = new ArrayList<>();
        if (KEY_ALL.equals(key)) {
            out.addAll(ALL);
        } else if (KEY_FAV.equals(key)) {
            Set<String> favs = favSet();
            for (Channel c : ALL) if (favs.contains(c.id)) out.add(c);
        } else {
            for (Channel c : ALL) if (c.category.equals(key)) out.add(c);
        }
        return out;
    }

    public static synchronized List<Channel> search(String q) {
        List<Channel> out = new ArrayList<>();
        String n = norm(q);
        if (n.isEmpty()) return out;
        for (Channel c : ALL) {
            if (norm(c.name).contains(n) || norm(c.category).contains(n) || norm(c.country).contains(n)) {
                out.add(c);
            }
        }
        return out;
    }

    public static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t.toLowerCase(Locale.ROOT).trim();
    }

    // ---------- Favoritos ----------

    private static Set<String> favSet() {
        if (prefs == null) return new HashSet<>();
        return new HashSet<>(prefs.getStringSet("favs", new HashSet<String>()));
    }

    public static boolean isFav(String id) {
        return favSet().contains(id);
    }

    /** Devuelve true si quedo como favorito. */
    public static boolean toggleFav(String id) {
        Set<String> s = favSet();
        boolean now;
        if (s.contains(id)) {
            s.remove(id);
            now = false;
        } else {
            s.add(id);
            now = true;
        }
        prefs.edit().putStringSet("favs", s).apply();
        return now;
    }

    public static void setLast(String id) {
        if (prefs != null) prefs.edit().putString("last", id).apply();
    }
}
