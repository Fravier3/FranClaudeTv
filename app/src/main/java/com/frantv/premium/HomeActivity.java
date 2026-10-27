package com.frantv.premium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {
    private static final int SPAN = 4;
    private static final String KEY_SEARCH = "__search";
    private static final String KEY_REFRESH = "__refresh";

    private RecyclerView grid;
    private ChannelAdapter adapter;
    private LinearLayout nav;
    private TextView heroLine1, heroLine2, heroSub, clock, empty, loadingText;
    private View hero, searchBar, loading;
    private EditText searchInput;

    private String currentKey = Repo.KEY_ALL;
    private String keyBeforeSearch = Repo.KEY_ALL;
    private boolean searchMode = false;
    private int lastGridPos = 0;
    private SharedPreferences prefs;
    private final List<View> navViews = new ArrayList<>();
    private final Handler h = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.US);

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            clock.setText(fmt.format(new Date()));
            h.postDelayed(this, 20000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Repo.loadCached(this);
        prefs = getSharedPreferences("frantv", MODE_PRIVATE);
        setContentView(R.layout.activity_home);

        grid = findViewById(R.id.grid);
        nav = findViewById(R.id.navContainer);
        heroLine1 = findViewById(R.id.heroLine1);
        heroLine2 = findViewById(R.id.heroLine2);
        heroSub = findViewById(R.id.heroSub);
        clock = findViewById(R.id.clock);
        empty = findViewById(R.id.emptyView);
        hero = findViewById(R.id.hero);
        searchBar = findViewById(R.id.searchBar);
        searchInput = findViewById(R.id.searchInput);
        loading = findViewById(R.id.loading);
        loadingText = findViewById(R.id.loadingText);
        Util.wordmark((TextView) findViewById(R.id.brand));

        grid.setLayoutManager(new GridLayoutManager(this, SPAN));
        grid.setItemAnimator(null);
        grid.setHasFixedSize(false);
        adapter = new ChannelAdapter(new ChannelAdapter.Listener() {
            @Override
            public void onClick(Channel c, int pos) {
                play(pos);
            }

            @Override
            public void onLongClick(Channel c, int pos) {
                boolean fav = Repo.toggleFav(c.id);
                Toast.makeText(HomeActivity.this, fav ? "★ Agregado a favoritos" : "Quitado de favoritos",
                        Toast.LENGTH_SHORT).show();
                adapter.notifyItemChanged(pos);
            }

            @Override
            public boolean onKey(int keyCode, int pos) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos % SPAN == 0) {
                    focusNavSelected();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && pos < SPAN) {
                    if (searchMode) searchInput.requestFocus();
                    return true;
                }
                return false;
            }

            @Override
            public void onFocus(int pos) {
                lastGridPos = pos;
            }
        });
        grid.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchMode) showSearchResults(s.toString());
            }
        });
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                hideKeyboard();
                focusGrid();
                return true;
            }
        });
        searchInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    hideKeyboard();
                    focusGrid();
                    return true;
                }
                return false;
            }
        });

        String saved = prefs.getString("homeKey", null);
        currentKey = pickKey(saved);
        buildNav();
        if (Repo.isEmpty()) {
            showLoading(true, "Cargando canales…");
            updateHero(currentKey);
        } else {
            selectKey(currentKey);
        }
        nav.post(new Runnable() {
            @Override
            public void run() {
                focusNavSelected();
            }
        });

        Repo.refresh(this, new Repo.Callback() {
            @Override
            public void onDone(boolean ok) {
                showLoading(false, null);
                if (isFinishing()) return;
                if (ok) {
                    boolean wasSearch = searchMode;
                    String k = pickKey(currentKey);
                    buildNav();
                    if (!wasSearch) {
                        selectKey(k);
                        focusNavSelected();
                    }
                } else if (Repo.isEmpty()) {
                    updateEmpty();
                }
            }
        });

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) UpdateManager.check(HomeActivity.this);
            }
        }, 3500);
    }

    private String pickKey(String saved) {
        if (Repo.KEY_ALL.equals(saved) || Repo.KEY_FAV.equals(saved)) return saved;
        List<String> cats = Repo.categories();
        if (saved != null && cats.contains(saved)) return saved;
        return cats.isEmpty() ? Repo.KEY_ALL : cats.get(0);
    }

    // ---------------- Menu lateral ----------------

    private void buildNav() {
        nav.removeAllViews();
        navViews.clear();
        addNav(KEY_SEARCH, "Buscar");
        addNav(Repo.KEY_ALL, "Todos los canales");
        addNav(Repo.KEY_FAV, "Favoritos");
        addDivider();
        TextView label = new TextView(this);
        label.setText("CATEGORÍAS");
        label.setTextColor(Color.parseColor("#7E93BE"));
        label.setTextSize(10f);
        label.setLetterSpacing(0.18f);
        label.setPadding(Util.dp(this, 12), Util.dp(this, 4), 0, Util.dp(this, 6));
        nav.addView(label);
        for (String cat : Repo.categories()) addNav(cat, cat);
        addDivider();
        addNav(KEY_REFRESH, "Actualizar canales");
        refreshNavSelection();
    }

    private void addDivider() {
        View d = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 1));
        lp.setMargins(Util.dp(this, 6), Util.dp(this, 8), Util.dp(this, 6), Util.dp(this, 8));
        d.setLayoutParams(lp);
        d.setBackgroundResource(R.drawable.divider_line);
        nav.addView(d);
    }

    private void addNav(final String key, String text) {
        View v = getLayoutInflater().inflate(R.layout.item_nav, nav, false);
        ImageView icon = v.findViewById(R.id.navIcon);
        TextView tv = v.findViewById(R.id.navText);
        tv.setText(text);
        if (KEY_SEARCH.equals(key)) {
            icon.setImageResource(R.drawable.ic_search);
            icon.setColorFilter(Color.parseColor("#D6E2FF"));
        } else if (KEY_REFRESH.equals(key)) {
            icon.setImageResource(R.drawable.ic_refresh);
            icon.setColorFilter(Color.parseColor("#D6E2FF"));
        } else {
            Util.setKeyIcon(icon, key);
        }
        v.setTag(key);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNav(key);
            }
        });
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && !KEY_SEARCH.equals(key) && !KEY_REFRESH.equals(key)
                        && (searchMode || !key.equals(currentKey))) {
                    selectKey(key);
                }
            }
        });
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (searchMode && KEY_SEARCH.equals(key)) {
                        searchInput.requestFocus();
                    } else {
                        focusGrid();
                    }
                    return true;
                }
                return false;
            }
        });
        nav.addView(v);
        navViews.add(v);
    }

    private void onNav(String key) {
        if (KEY_SEARCH.equals(key)) {
            enterSearch();
        } else if (KEY_REFRESH.equals(key)) {
            showLoading(true, "Actualizando canales…");
            Repo.refresh(this, new Repo.Callback() {
                @Override
                public void onDone(boolean ok) {
                    showLoading(false, null);
                    if (isFinishing()) return;
                    Toast.makeText(HomeActivity.this, ok ? "Canales actualizados" : "No se pudo actualizar. Revisa tu conexión.",
                            Toast.LENGTH_SHORT).show();
                    if (ok) {
                        String k = pickKey(currentKey);
                        buildNav();
                        selectKey(k);
                        focusNavSelected();
                    }
                }
            });
        } else {
            selectKey(key);
            focusGrid();
        }
    }

    private void refreshNavSelection() {
        for (View v : navViews) {
            Object tag = v.getTag();
            boolean sel;
            if (searchMode) sel = KEY_SEARCH.equals(tag);
            else sel = tag != null && tag.equals(currentKey);
            v.setSelected(sel);
        }
    }

    private void focusNavSelected() {
        String want = searchMode ? KEY_SEARCH : currentKey;
        for (View v : navViews) {
            if (want.equals(v.getTag())) {
                v.requestFocus();
                return;
            }
        }
        if (!navViews.isEmpty()) navViews.get(0).requestFocus();
    }

    private void focusGrid() {
        final int count = adapter.getItemCount();
        if (count == 0) return;
        final int pos = Math.min(lastGridPos, count - 1);
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            vh.itemView.requestFocus();
        } else {
            grid.scrollToPosition(pos);
            grid.post(new Runnable() {
                @Override
                public void run() {
                    RecyclerView.ViewHolder v2 = grid.findViewHolderForAdapterPosition(pos);
                    if (v2 != null) v2.itemView.requestFocus();
                }
            });
        }
    }

    // ---------------- Contenido ----------------

    private void selectKey(String key) {
        if (searchMode) leaveSearch();
        currentKey = key;
        prefs.edit().putString("homeKey", key).apply();
        updateHero(key);
        adapter.setData(Repo.channelsOf(key));
        lastGridPos = 0;
        grid.scrollToPosition(0);
        updateEmpty();
        refreshNavSelection();
    }

    private void updateHero(String key) {
        String[] t = Util.heroText(key);
        heroLine1.setText(t[0]);
        heroLine2.setText(t[1]);
        heroSub.setText(t[2]);
    }

    private void updateEmpty() {
        if (adapter.getItemCount() > 0) {
            empty.setVisibility(View.GONE);
            return;
        }
        String msg;
        if (searchMode) {
            String q = searchInput.getText().toString().trim();
            msg = q.isEmpty() ? "Escribe para buscar canales." : "Sin resultados para «" + q + "».";
        } else if (Repo.isEmpty()) {
            msg = "No se pudieron cargar los canales.\nRevisa tu conexión y elige «Actualizar canales».";
        } else if (Repo.KEY_FAV.equals(currentKey)) {
            msg = "Aún no tienes favoritos.\nMantén OK sobre un canal para agregarlo.";
        } else {
            msg = "No hay canales en esta categoría.";
        }
        empty.setText(msg);
        empty.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show, String msg) {
        if (msg != null) loadingText.setText(msg);
        loading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void play(int pos) {
        List<Channel> list = adapter.getData();
        if (pos < 0 || pos >= list.size()) return;
        Repo.playlist = new ArrayList<>(list);
        Repo.playlistIndex = pos;
        Repo.playlistKey = searchMode ? Repo.KEY_ALL : currentKey;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    // ---------------- Busqueda ----------------

    private void enterSearch() {
        if (!searchMode) keyBeforeSearch = currentKey;
        searchMode = true;
        hero.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        setGridTop(124);
        searchInput.setText("");
        adapter.setData(new ArrayList<Channel>());
        updateEmpty();
        refreshNavSelection();
        searchInput.requestFocus();
        searchInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 250);
    }

    private void leaveSearch() {
        searchMode = false;
        hero.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        setGridTop(196);
        hideKeyboard();
    }

    private void showSearchResults(String q) {
        adapter.setData(Repo.search(q));
        lastGridPos = 0;
        updateEmpty();
    }

    private void setGridTop(int dp) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) grid.getLayoutParams();
        lp.topMargin = Util.dp(this, dp);
        grid.setLayoutParams(lp);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    // ---------------- Ciclo de vida ----------------

    @Override
    protected void onResume() {
        super.onResume();
        h.removeCallbacks(tick);
        h.post(tick);
        if (adapter.getItemCount() > 0) adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    protected void onPause() {
        h.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (searchMode) {
            hideKeyboard();
            selectKey(keyBeforeSearch);
            focusNavSelected();
            return;
        }
        View f = getCurrentFocus();
        if (f != null && grid.findContainingItemView(f) != null) {
            focusNavSelected();
            return;
        }
        super.onBackPressed();
    }
}
