package com.frantv.premium;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Avisa cuando hay una version nueva y la instala desde la app. */
public final class UpdateManager {
    private UpdateManager() {}

    public static void check(final Activity a) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = Repo.httpGet(Repo.UPDATE_URL + "?t=" + (System.currentTimeMillis() / 30000));
                    JSONObject o = new JSONObject(json);
                    final int code = o.optInt("versionCode", 0);
                    if (code <= BuildConfig.VERSION_CODE) return;
                    final String name = o.optString("versionName", "");
                    final String notes = o.optString("notes", "");
                    final String apk = o.optString("apkUrl", "");
                    if (apk.isEmpty()) return;
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!a.isFinishing()) show(a, name, notes, apk);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private static void show(final Activity a, String name, String notes, final String apkUrl) {
        final Dialog d = new Dialog(a, R.style.DialogFran);
        d.setContentView(R.layout.dialog_update);
        TextView body = d.findViewById(R.id.dlgBody);
        StringBuilder sb = new StringBuilder("Hay una versión nueva de Fran TV");
        if (!name.isEmpty()) sb.append(" (").append(name).append(")");
        sb.append(".");
        if (!notes.isEmpty()) sb.append("\n\n").append(notes);
        body.setText(sb.toString());
        final TextView later = d.findViewById(R.id.dlgLater);
        final TextView update = d.findViewById(R.id.dlgUpdate);
        final ProgressBar bar = d.findViewById(R.id.dlgProgress);
        later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update.setEnabled(false);
                update.setText("Descargando…");
                later.setEnabled(false);
                bar.setVisibility(View.VISIBLE);
                download(a, d, apkUrl, bar);
            }
        });
        d.show();
        update.requestFocus();
    }

    private static void download(final Activity a, final Dialog d, final String apkUrl, final ProgressBar bar) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File out = null;
                try {
                    File dir = new File(a.getCacheDir(), "update");
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();
                    out = new File(dir, "FranTV.apk");
                    HttpURLConnection con = (HttpURLConnection) new URL(apkUrl).openConnection();
                    con.setConnectTimeout(15000);
                    con.setReadTimeout(30000);
                    con.setInstanceFollowRedirects(true);
                    if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
                    final long total = con.getContentLengthLong();
                    InputStream in = con.getInputStream();
                    FileOutputStream fo = new FileOutputStream(out);
                    byte[] buf = new byte[16384];
                    long done = 0;
                    int n;
                    int lastPct = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            final int pct = (int) (done * 100 / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                a.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        bar.setProgress(pct);
                                    }
                                });
                            }
                        }
                    }
                    fo.close();
                    in.close();
                    con.disconnect();
                    final File apk = out;
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.dismiss();
                            install(a, apk);
                        }
                    });
                } catch (Exception e) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.dismiss();
                            Toast.makeText(a, "No se pudo descargar la actualización.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private static void install(Activity a, File apk) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(a, "Activa «Permitir de esta fuente» para Fran TV y vuelve a actualizar.",
                        Toast.LENGTH_LONG).show();
                Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + a.getPackageName()));
                a.startActivity(s);
                return;
            }
            Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", apk);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "No se pudo abrir el instalador.", Toast.LENGTH_LONG).show();
        }
    }
}
