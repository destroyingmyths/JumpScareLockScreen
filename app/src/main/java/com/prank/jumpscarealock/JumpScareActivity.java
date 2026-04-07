package com.prank.jumpscarealock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JumpScareActivity extends Activity {

    private ScareView scareView;
    private boolean scared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen and keep screen on
        Window window = getWindow();
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        scareView = new ScareView(this, () -> {
            if (!scared) {
                scared = true;
                triggerScare();
            }
        });

        setContentView(scareView);
    }

    private void triggerScare() {
        // 1. Play scream using ToneGenerator (no audio file needed)
        //    We use a jarring CDMA_ABBR_ALERT + max volume trick
        playScreamSound();

        // 2. Vibrate hard
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 80, 40, 80, 40, 200, 40, 80, 40, 80};
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(new long[]{0, 80, 40, 80, 40, 200, 40, 80, 40, 80}, -1);
            }
        }

        // 3. Show scary face
        scareView.showScare();

        // 4. Close after 3 seconds so they can actually unlock
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
    }

    private void playScreamSound() {
        try {
            // Max volume
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            }
            // Use ToneGenerator to produce a harsh jarring tone — no file needed
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            // CDMA_HIGH_L + CDMA_LOW_L alternating for scream-like effect
            tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tg.startTone(ToneGenerator.TONE_CDMA_LOW_L, 600);
            }, 200);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800);
                new Handler(Looper.getMainLooper()).postDelayed(tg::release, 1000);
            }, 500);
        } catch (Exception e) {
            // Fallback: silent if audio fails
        }
    }

    @Override
    public void onBackPressed() {
        // Block back button — they can't escape until scare is done
        if (scared) super.onBackPressed();
    }

    // =====================================================================
    // Custom View: draws the fake lock screen, then the scary face
    // =====================================================================
    static class ScareView extends View {

        interface OnUnlockAttempt { void onAttempt(); }

        private final OnUnlockAttempt listener;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final GestureDetector gestureDetector;
        private boolean showingFace = false;

        // Scare flash alpha for red flash effect
        private float flashAlpha = 0f;
        private final Handler handler = new Handler(Looper.getMainLooper());

        ScareView(Context ctx, OnUnlockAttempt listener) {
            super(ctx);
            this.listener = listener;
            gestureDetector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                    // Swipe up = unlock attempt
                    if (e1 != null && (e1.getY() - e2.getY()) > 60) {
                        listener.onAttempt();
                    }
                    return true;
                }
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    listener.onAttempt();
                    return true;
                }
            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            return true;
        }

        void showScare() {
            showingFace = true;
            animateFlash();
            invalidate();
        }

        private void animateFlash() {
            flashAlpha = 1f;
            invalidate();
            handler.postDelayed(() -> {
                flashAlpha = 0.5f;
                invalidate();
                handler.postDelayed(() -> {
                    flashAlpha = 0.8f;
                    invalidate();
                    handler.postDelayed(() -> {
                        flashAlpha = 0f;
                        invalidate();
                    }, 150);
                }, 100);
            }, 100);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();

            if (!showingFace) {
                drawLockScreen(canvas, w, h);
            } else {
                drawScaryFace(canvas, w, h);
                // Red flash overlay
                if (flashAlpha > 0) {
                    paint.setColor(Color.argb((int)(flashAlpha * 180), 255, 0, 0));
                    canvas.drawRect(0, 0, w, h, paint);
                }
            }
        }

        private void drawLockScreen(Canvas canvas, int w, int h) {
            // Dark gradient background
            paint.setColor(Color.parseColor("#0f1524"));
            canvas.drawRect(0, 0, w, h, paint);

            // Subtle blue glow top-left
            paint.setColor(Color.argb(60, 30, 60, 140));
            canvas.drawCircle(-80, -80, 380, paint);

            // Time
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.13f);
            paint.setFakeBoldText(false);
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText(time, w / 2f, h * 0.28f, paint);

            // Date
            paint.setTextSize(h * 0.025f);
            paint.setColor(Color.argb(180, 255, 255, 255));
            String date = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
            canvas.drawText(date, w / 2f, h * 0.35f, paint);

            // Lock icon (circle + shackle drawn manually)
            float lx = w / 2f;
            float ly = h * 0.52f;
            float lr = h * 0.04f;
            paint.setColor(Color.argb(200, 255, 255, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            // Shackle arc
            RectF arc = new RectF(lx - lr * 0.7f, ly - lr * 1.5f, lx + lr * 0.7f, ly - lr * 0.3f);
            canvas.drawArc(arc, 180, 180, false, paint);
            // Body
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(200, 255, 255, 255));
            RectF body = new RectF(lx - lr, ly - lr * 0.3f, lx + lr, ly + lr);
            canvas.drawRoundRect(body, lr * 0.3f, lr * 0.3f, paint);
            paint.setStyle(Paint.Style.FILL);

            // "Swipe up to unlock" hint
            paint.setColor(Color.argb(140, 255, 255, 255));
            paint.setTextSize(h * 0.020f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Swipe up to unlock", w / 2f, h * 0.88f, paint);

            // Small upward arrow
            float ax = w / 2f;
            float ay = h * 0.85f;
            float as2 = h * 0.015f;
            paint.setColor(Color.argb(120, 255, 255, 255));
            paint.setStrokeWidth(4f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(ax, ay, ax - as2, ay + as2, paint);
            canvas.drawLine(ax, ay, ax + as2, ay + as2, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawScaryFace(Canvas canvas, int w, int h) {
            // Black background
            canvas.drawColor(Color.BLACK);

            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.38f;

            // Face - sickly pale green
            paint.setColor(Color.parseColor("#8fad6a"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, r, paint);

            // Dark veins (lines across face)
            paint.setColor(Color.parseColor("#3a5a20"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            canvas.drawLine(cx - r*0.3f, cy - r*0.6f, cx - r*0.1f, cy + r*0.2f, paint);
            canvas.drawLine(cx + r*0.2f, cy - r*0.5f, cx + r*0.4f, cy + r*0.3f, paint);

            // Eyes — hollow black, bloodshot
            float eyeY = cy - r * 0.15f;
            float eyeR = r * 0.18f;
            // Left eye
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cx - r * 0.35f, eyeY, eyeR, paint);
            // Right eye
            canvas.drawCircle(cx + r * 0.35f, eyeY, eyeR, paint);
            // Red pupils
            paint.setColor(Color.parseColor("#cc0000"));
            canvas.drawCircle(cx - r * 0.35f, eyeY, eyeR * 0.45f, paint);
            canvas.drawCircle(cx + r * 0.35f, eyeY, eyeR * 0.45f, paint);
            // White glints
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - r * 0.35f + eyeR*0.2f, eyeY - eyeR*0.2f, eyeR*0.12f, paint);
            canvas.drawCircle(cx + r * 0.35f + eyeR*0.2f, eyeY - eyeR*0.2f, eyeR*0.12f, paint);

            // Nose — two dark slits
            paint.setColor(Color.parseColor("#3a2a1a"));
            paint.setStyle(Paint.Style.FILL);
            RectF nL = new RectF(cx - r*0.12f, cy + r*0.05f, cx - r*0.04f, cy + r*0.18f);
            RectF nR = new RectF(cx + r*0.04f, cy + r*0.05f, cx + r*0.12f, cy + r*0.18f);
            canvas.drawOval(nL, paint);
            canvas.drawOval(nR, paint);

            // Mouth — wide jagged grin
            paint.setColor(Color.parseColor("#1a0a0a"));
            paint.setStyle(Paint.Style.FILL);
            RectF mouth = new RectF(cx - r*0.5f, cy + r*0.28f, cx + r*0.5f, cy + r*0.58f);
            canvas.drawArc(mouth, 0, 180, true, paint);
            // Teeth
            paint.setColor(Color.parseColor("#d4c9a0"));
            float toothW = r * 0.16f;
            float toothTop = cy + r * 0.33f;
            float toothBot = cy + r * 0.50f;
            for (int i = 0; i < 5; i++) {
                float tx = cx - r*0.4f + i * toothW * 1.05f;
                canvas.drawRect(tx, toothTop, tx + toothW * 0.85f, toothBot, paint);
            }

            // Dark shadow under face
            paint.setColor(Color.argb(180, 0, 0, 0));
            paint.setStyle(Paint.Style.FILL);
            RectF shadow = new RectF(cx - r, cy + r * 0.7f, cx + r, cy + r * 1.1f);
            canvas.drawOval(shadow, paint);

            // "BOO!" text
            paint.setColor(Color.parseColor("#ff1111"));
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(h * 0.12f);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("BOO!", cx, cy - r * 1.1f, paint);
        }
    }
}
