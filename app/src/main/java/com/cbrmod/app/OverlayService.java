package com.cbrmod.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams, panelParams;
    private long lastTap = 0;
    private int tapCount = 0;
    private float downX, downY;
    private int startX, startY;

    @Override public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    private void startForegroundNotification() {
        String channel = "cbr_overlay";
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(channel, "CBR MOD", NotificationManager.IMPORTANCE_LOW));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, channel) : new Notification.Builder(this);
        b.setContentTitle("CBR MOD ativo").setContentText("Painel flutuante em execução").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true);
        startForeground(77, b.build());
    }

    private int type() { return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }

    private void createBubble() {
        bubble = new TextView(this);
        bubble.setText("CBR");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(13);
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(Color.rgb(105, 55, 220));
        bubbleParams = new WindowManager.LayoutParams( dp(58), dp(58), type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(18); bubbleParams.y = dp(180);
        bubble.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX(); downY = e.getRawY(); startX = bubbleParams.x; startY = bubbleParams.y; return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                bubbleParams.x = startX + (int)(e.getRawX() - downX);
                bubbleParams.y = startY + (int)(e.getRawY() - downY);
                wm.updateViewLayout(bubble, bubbleParams); return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                long now = System.currentTimeMillis();
                if (now - lastTap < 550) tapCount++; else tapCount = 1;
                lastTap = now;
                if (tapCount >= 3) { togglePanel(); tapCount = 0; }
                return true;
            }
            return true;
        });
        wm.addView(bubble, bubbleParams);
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void togglePanel() {
        if (panel != null) { wm.removeView(panel); panel = null; return; }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackgroundColor(Color.rgb(20, 15, 38));
        TextView title = new TextView(this);
        title.setText("CBR MOD  •  PAINEL");
        title.setTextColor(Color.rgb(190, 150, 255));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(title);
        String[] categories = {"AIMBOT", "ESP", "MEMORY", "PLAYER", "GRAPHICS", "SETTINGS", "CONFIGS"};
        for (String c : categories) {
            TextView item = new TextView(this);
            item.setText(c + "  •  utilitário não disponível para modificar o jogo");
            item.setTextColor(Color.WHITE);
            item.setTextSize(12);
            item.setPadding(0, dp(12), 0, dp(12));
            box.addView(item);
        }
        TextView close = new TextView(this);
        close.setText("FECHAR PAINEL");
        close.setTextColor(Color.rgb(190, 150, 255));
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, dp(10), 0, dp(4));
        close.setOnClickListener(v -> togglePanel());
        box.addView(close);
        panel = box;
        panelParams = new WindowManager.LayoutParams(dp(300), -2, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;
        wm.addView(panel, panelParams);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() {
        if (wm != null) {
            if (panel != null) { wm.removeView(panel); panel = null; }
            if (bubble != null) { wm.removeView(bubble); bubble = null; }
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
