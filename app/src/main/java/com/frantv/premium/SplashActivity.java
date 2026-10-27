package com.frantv.premium;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

/** Presentacion que sale siempre al entrar a la app. */
public class SplashActivity extends Activity {
    private MediaPlayer sound;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable go = new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Repo.loadCached(this);
        Repo.refresh(this, null);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.logo);
        View ring1 = findViewById(R.id.ring1);
        View ring2 = findViewById(R.id.ring2);
        View line = findViewById(R.id.line);
        TextView title = findViewById(R.id.title);
        TextView subtitle = findViewById(R.id.subtitle);

        String t = "Bienvenido a FranTV.";
        SpannableString s = new SpannableString(t);
        int i = t.indexOf("TV");
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#34DCFF")), i, i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(s);

        float d = getResources().getDisplayMetrics().density;
        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        title.setAlpha(0f);
        title.setTranslationY(18 * d);
        subtitle.setAlpha(0f);
        subtitle.setTranslationY(14 * d);
        line.setAlpha(0f);
        line.setScaleX(0.1f);
        ring1.setAlpha(0f);
        ring2.setAlpha(0f);

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.1f)).start();
        title.animate().alpha(1f).translationY(0f).setStartDelay(550).setDuration(650).start();
        subtitle.animate().alpha(1f).translationY(0f).setStartDelay(950).setDuration(650).start();
        line.animate().alpha(1f).scaleX(1f).setStartDelay(1300).setDuration(800).start();
        pulse(ring1, 400);
        pulse(ring2, 1500);

        h.postDelayed(go, 3600);
        playSound();
    }

    /** Sonido de la presentacion (no altera su duracion). */
    private void playSound() {
        try {
            sound = MediaPlayer.create(this, R.raw.logo_audio,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);
            if (sound != null) {
                sound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        releaseSound();
                    }
                });
                sound.start();
            }
        } catch (Exception ignored) {
            releaseSound();
        }
    }

    private void releaseSound() {
        if (sound != null) {
            try {
                sound.release();
            } catch (Exception ignored) {
            }
            sound = null;
        }
    }

    private void pulse(final View v, long delay) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(2400);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                float t = (Float) anim.getAnimatedValue();
                float sc = 0.85f + 0.5f * t;
                v.setScaleX(sc);
                v.setScaleY(sc);
                v.setAlpha(0.8f * (1f - t));
            }
        });
        a.start();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(go);
        releaseSound();
        super.onDestroy();
    }
}
