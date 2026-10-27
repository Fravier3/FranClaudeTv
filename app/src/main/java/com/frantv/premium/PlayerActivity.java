package com.frantv.premium;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends Activity {
    private static final long DOUBLE_OK_MS = 450;

    private ExoPlayer player;
    private PlayerView playerView;
    private View panel, infoCard, hints, statusBox;
    private ProgressBar spinner;
    private TextView statusMsg, clock, infoTitle, infoSub, catName;
    private ImageView infoStar, catIcon, favPillIcon;
    private RecyclerView panelList;
    private PanelAdapter panelAdapter;

    private List<Channel> list = new ArrayList<>();
    private int index = 0;
    private String listKey = Repo.KEY_ALL;
    private List<Channel> panelItems = new ArrayList<>();
    private String panelKey = Repo.KEY_ALL;
    private boolean favOnly = false;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.US);
    private int retries = 0;
    private long lastOk = 0;
    private long lastZap = 0;
    private long lastRowClick = 0;
    private int lastRowPos = -1;
    private Runnable pendingPlay;
    private MediaItem currentItem;

    private final Runnable hideInfo = new Runnable() {
        @Override
        public void run() {
            if (!menuOpen()) {
                infoCard.setVisibility(View.GONE);
                clock.setVisibility(View.GONE);
            }
        }
    };
    private final Runnable openMenuRun = new Runnable() {
        @Override
        public void run() {
            openMenu();
        }
    };
    private final Runnable retryRun = new Runnable() {
        @Override
        public void run() {
            if (player != null && currentItem != null) {
                player.setMediaItem(currentItem);
                player.prepare();
                player.setPlayWhenReady(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        list = new ArrayList<>(Repo.playlist);
        if (list.isEmpty()) {
            finish();
            return;
        }
        index = Math.max(0, Math.min(Repo.playlistIndex, list.size() - 1));
        listKey = Repo.playlistKey;
        panelItems = list;
        panelKey = listKey;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        panel = findViewById(R.id.panel);
        infoCard = findViewById(R.id.infoCard);
        hints = findViewById(R.id.hints);
        statusBox = findViewById(R.id.statusBox);
        spinner = findViewById(R.id.spinner);
        statusMsg = findViewById(R.id.statusMsg);
        clock = findViewById(R.id.clock);
        infoTitle = findViewById(R.id.infoTitle);
        infoSub = findViewById(R.id.infoSub);
        infoStar = findViewById(R.id.infoStar);
        catName = findViewById(R.id.catName);
        catIcon = findViewById(R.id.catIcon);
        favPillIcon = findViewById(R.id.favPillIcon);
        panelList = findViewById(R.id.panelList);
        Util.wordmark((TextView) findViewById(R.id.panelBrand));

        panelList.setLayoutManager(new LinearLayoutManager(this));
        panelList.setItemAnimator(null);
        panelAdapter = new PanelAdapter(new PanelAdapter.Listener() {
            @Override
            public void onClick(int pos) {
                onRowClick(pos);
            }
        });
        panelList.setAdapter(panelAdapter);

        findViewById(R.id.catPill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleCategory();
            }
        });
        findViewById(R.id.favPill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavOnly();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (list.isEmpty()) return;
        initPlayer();
        playCurrent();
    }

    @Override
    protected void onStop() {
        h.removeCallbacks(retryRun);
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
        super.onStop();
    }

    private void initPlayer() {
        if (player != null) return;
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setUserAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 FranTV/1.0");
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    showSpinner(statusMsg.getText().length() > 0 && retries > 0 ? "Reconectando…" : null);
                } else if (state == Player.STATE_READY) {
                    retries = 0;
                    statusBox.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                handleError();
            }
        });
    }

    // ---------------- Reproduccion ----------------

    private void playCurrent() {
        if (player == null || list.isEmpty()) return;
        Channel c = list.get(index);
        retries = 0;
        h.removeCallbacks(retryRun);
        showSpinner(null);
        MediaItem.Builder b = new MediaItem.Builder().setUri(c.url);
        if (c.url.toLowerCase(Locale.ROOT).contains(".m3u8")) b.setMimeType(MimeTypes.APPLICATION_M3U8);
        currentItem = b.build();
        player.setMediaItem(currentItem);
        player.prepare();
        player.setPlayWhenReady(true);
        Repo.setLast(c.id);
        Repo.playlist = list;
        Repo.playlistIndex = index;
        panelAdapter.setCurrent(c.id);
        showInfo();
    }

    private void handleError() {
        if (player == null) return;
        retries++;
        if (retries <= 5) {
            showSpinner("Reconectando…");
            h.removeCallbacks(retryRun);
            h.postDelayed(retryRun, 1500L * retries);
        } else {
            statusBox.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.GONE);
            statusMsg.setVisibility(View.VISIBLE);
            statusMsg.setText("No se pudo reproducir este canal.\nUsa ▲ ▼ para probar otro.");
        }
    }

    private void showSpinner(String msg) {
        statusBox.setVisibility(View.VISIBLE);
        spinner.setVisibility(View.VISIBLE);
        if (msg == null || msg.isEmpty()) {
            statusMsg.setText("");
            statusMsg.setVisibility(View.GONE);
        } else {
            statusMsg.setText(msg);
            statusMsg.setVisibility(View.VISIBLE);
        }
    }

    private void changeChannel(int delta) {
        long now = SystemClock.uptimeMillis();
        if (now - lastZap < 280) return;
        lastZap = now;
        index = (index + delta + list.size()) % list.size();
        panelItems = list;
        panelKey = listKey;
        favOnly = false;
        playCurrent();
    }

    // ---------------- Informacion ----------------

    private boolean menuOpen() {
        return panel.getVisibility() == View.VISIBLE;
    }

    private void updateInfo() {
        Channel c = list.get(index);
        infoTitle.setText(String.format(Locale.US, "%03d • %s", index + 1, c.name));
        infoSub.setText(c.category);
        infoStar.setImageResource(Repo.isFav(c.id) ? R.drawable.ic_star_gold : R.drawable.ic_star_outline);
        clock.setText(fmt.format(new Date()));
    }

    private void showInfo() {
        updateInfo();
        infoCard.setVisibility(View.VISIBLE);
        clock.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideInfo);
        if (!menuOpen()) h.postDelayed(hideInfo, 4500);
    }

    private void toggleFavCurrent() {
        Channel c = list.get(index);
        boolean fav = Repo.toggleFav(c.id);
        Toast.makeText(this, fav ? "★ Agregado a favoritos" : "Quitado de favoritos", Toast.LENGTH_SHORT).show();
        showInfo();
        if (menuOpen()) panelAdapter.notifyDataSetChanged();
    }

    // ---------------- Menu de canales ----------------

    private void openMenu() {
        if (menuOpen()) return;
        h.removeCallbacks(hideInfo);
        panelAdapter.setData(panelItems);
        panelAdapter.setCurrent(list.get(index).id);
        updatePills();
        panel.setVisibility(View.VISIBLE);
        panel.setTranslationX(-Util.dp(this, 310));
        panel.animate().translationX(0f).setDuration(220).start();
        hints.setVisibility(View.VISIBLE);
        showInfo();
        final int pos = Math.max(0, panelAdapter.indexOf(list.get(index).id));
        panelList.scrollToPosition(pos);
        panelList.postDelayed(new Runnable() {
            @Override
            public void run() {
                focusRow(pos, true);
            }
        }, 140);
    }

    private void focusRow(final int pos, boolean retry) {
        RecyclerView.ViewHolder vh = panelList.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            vh.itemView.requestFocus();
        } else if (retry) {
            panelList.postDelayed(new Runnable() {
                @Override
                public void run() {
                    focusRow(pos, false);
                }
            }, 150);
        }
    }

    private void closeMenu() {
        cancelPendingPlay();
        panel.setVisibility(View.GONE);
        hints.setVisibility(View.GONE);
        View f = getCurrentFocus();
        if (f != null) f.clearFocus();
        showInfo();
    }

    private void cancelPendingPlay() {
        if (pendingPlay != null) {
            h.removeCallbacks(pendingPlay);
            pendingPlay = null;
        }
    }

    private void onRowClick(final int pos) {
        if (pos < 0 || pos >= panelItems.size()) return;
        long now = SystemClock.uptimeMillis();
        if (pos == lastRowPos && now - lastRowClick < DOUBLE_OK_MS) {
            cancelPendingPlay();
            lastRowPos = -1;
            Channel c = panelItems.get(pos);
            boolean fav = Repo.toggleFav(c.id);
            Toast.makeText(this, fav ? "★ Agregado a favoritos" : "Quitado de favoritos", Toast.LENGTH_SHORT).show();
            panelAdapter.notifyItemChanged(pos);
            updateInfo();
        } else {
            lastRowPos = pos;
            lastRowClick = now;
            cancelPendingPlay();
            pendingPlay = new Runnable() {
                @Override
                public void run() {
                    pendingPlay = null;
                    lastRowPos = -1;
                    playFromPanel(pos);
                }
            };
            h.postDelayed(pendingPlay, DOUBLE_OK_MS);
        }
    }

    private void playFromPanel(int pos) {
        if (pos < 0 || pos >= panelItems.size()) return;
        Channel c = panelItems.get(pos);
        boolean same = c.id.equals(list.get(index).id);
        list = new ArrayList<>(panelItems);
        listKey = panelKey;
        index = pos;
        closeMenu();
        if (!same) playCurrent();
    }

    private void cycleCategory() {
        List<String> keys = new ArrayList<>();
        keys.add(Repo.KEY_ALL);
        keys.addAll(Repo.categories());
        int i = keys.indexOf(panelKey);
        panelKey = keys.get((i + 1) % keys.size());
        favOnly = false;
        panelItems = Repo.channelsOf(panelKey);
        panelAdapter.setData(panelItems);
        panelAdapter.setCurrent(list.get(index).id);
        panelList.scrollToPosition(0);
        updatePills();
    }

    private void toggleFavOnly() {
        if (!favOnly) {
            List<Channel> favs = Repo.channelsOf(Repo.KEY_FAV);
            if (favs.isEmpty()) {
                Toast.makeText(this, "Aún no tienes favoritos", Toast.LENGTH_SHORT).show();
                return;
            }
            favOnly = true;
            panelKey = Repo.KEY_FAV;
            panelItems = favs;
        } else {
            favOnly = false;
            panelKey = listKey;
            panelItems = Repo.KEY_FAV.equals(listKey) ? Repo.channelsOf(Repo.KEY_ALL) : Repo.channelsOf(listKey);
            if (Repo.KEY_FAV.equals(panelKey)) panelKey = Repo.KEY_ALL;
        }
        panelAdapter.setData(panelItems);
        panelAdapter.setCurrent(list.get(index).id);
        panelList.scrollToPosition(0);
        updatePills();
    }

    private void updatePills() {
        catName.setText(Util.categoryLabel(panelKey));
        Util.setKeyIcon(catIcon, panelKey);
        favPillIcon.setImageResource(favOnly ? R.drawable.ic_star_gold : R.drawable.ic_star_outline);
    }

    // ---------------- Control remoto ----------------

    private static boolean isOk(int k) {
        return k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;

        if (menuOpen()) {
            if (down && k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                View f = getCurrentFocus();
                if (f != null && panelList.findContainingItemView(f) != null) {
                    closeMenu();
                    return true;
                }
            }
            return super.dispatchKeyEvent(e);
        }

        if (isOk(k)) {
            if (down && e.getRepeatCount() == 0) {
                long now = SystemClock.uptimeMillis();
                if (now - lastOk < DOUBLE_OK_MS) {
                    h.removeCallbacks(openMenuRun);
                    lastOk = 0;
                    toggleFavCurrent();
                } else {
                    lastOk = now;
                    h.postDelayed(openMenuRun, DOUBLE_OK_MS);
                }
            }
            return true;
        }

        if (down) {
            switch (k) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    changeChannel(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    changeChannel(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    openMenu();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_INFO:
                    showInfo();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (player != null) player.setPlayWhenReady(!player.getPlayWhenReady());
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        if (menuOpen()) {
            closeMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
