package com.cbrmod.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
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
    private boolean moved;
    private String selected = "PLAYER";
    private LinearLayout panelBody;
    private static final int PURPLE = Color.rgb(139, 83, 255);
    private static final int CYAN = Color.rgb(71, 218, 255);
    private static final int WHITE = Color.rgb(245, 242, 255);

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
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private void createBubble() {
        bubble = text("CBR", 13, Color.WHITE, true);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(shape(PURPLE, 18, CYAN));
        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58), type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(18);
        bubbleParams.y = dp(180);
        bubble.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX(); downY = e.getRawY();
                startX = bubbleParams.x; startY = bubbleParams.y; moved = false;
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
                if (moved) {
                    bubbleParams.x = startX + (int)dx; bubbleParams.y = startY + (int)dy;
                    wm.updateViewLayout(bubble, bubbleParams);
                }
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!moved) {
                    long now = System.currentTimeMillis();
                    tapCount = now - lastTap < 550 ? tapCount + 1 : 1;
                    lastTap = now;
                    if (tapCount >= 3) { togglePanel(); tapCount = 0; }
                } else tapCount = 0;
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_CANCEL) { tapCount = 0; return true; }
            return true;
        });
        wm.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel != null) {
            wm.removeView(panel); panel = null; panelBody = null;
            return;
        }
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(14), dp(14), dp(14), dp(12));
        shell.setBackground(shape(Color.rgb(16, 12, 31), 20, PURPLE));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.addView(text("CBR", 22, CYAN, true));
        header.addView(text("  MOD  /  PAINEL", 17, WHITE, true));
        TextView close = text("✕", 20, WHITE, true);
        close.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(close, new LinearLayout.LayoutParams(0, dp(38), 1));
        close.setOnClickListener(v -> togglePanel());
        shell.addView(header);

        TextView subtitle = text("PAINEL FLUTUANTE  •  ANDROID", 9, CYAN, true);
        subtitle.setPadding(0, dp(3), 0, dp(10));
        shell.addView(subtitle);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        String[] categories = {"AIMBOT", "ESP", "MEMORY", "PLAYER", "GRAPHICS", "SETTINGS", "CONFIGS"};
        for (String category : categories) {
            TextView tab = text(category, 11, WHITE, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(6), dp(11), dp(6), dp(11));
            tab.setBackground(shape(category.equals(selected) ? Color.rgb(75, 45, 125) : Color.rgb(31, 25, 51), 10, category.equals(selected) ? CYAN : Color.rgb(67, 56, 91)));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(118); lp.height = dp(42);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(tab, lp);
            tab.setOnClickListener(v -> { selected = category; updatePanelContent(); refreshTabs(grid, categories); });
        }
        shell.addView(grid);

        panelBody = new LinearLayout(this);
        panelBody.setOrientation(LinearLayout.VERTICAL);
        panelBody.setPadding(dp(12), dp(12), dp(12), dp(12));
        panelBody.setBackground(shape(Color.rgb(25, 20, 43), 12, Color.rgb(58, 47, 82)));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, dp(150));
        bodyLp.topMargin = dp(8);
        shell.addView(panelBody, bodyLp);
        updatePanelContent();

        TextView hint = text("TOQUE 3 VEZES NA BOLHA PARA OCULTAR / MOSTRAR", 8, Color.rgb(165, 153, 190), true);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(10), 0, 0);
        shell.addView(hint);

        panel = shell;
        panelParams = new WindowManager.LayoutParams(dp(290), -2, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;
        wm.addView(panel, panelParams);
    }

    private void refreshTabs(GridLayout grid, String[] categories) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            TextView tab = (TextView)grid.getChildAt(i);
            String category = categories[i];
            tab.setBackground(shape(category.equals(selected) ? Color.rgb(75, 45, 125) : Color.rgb(31, 25, 51), 10, category.equals(selected) ? CYAN : Color.rgb(67, 56, 91)));
        }
    }

    private void updatePanelContent() {
        if (panelBody == null) return;
        panelBody.removeAllViews();
        panelBody.addView(text(selected, 14, CYAN, true));
        String message;
        if ("AIMBOT".equals(selected) || "ESP".equals(selected) || "MEMORY".equals(selected)) {
            message = "Módulo não implementado. Esta versão não altera a mira, não revela jogadores e não modifica a memória do Free Fire.";
        } else if ("PLAYER".equals(selected)) {
            message = "Área do jogador. Use ABRIR FREE FIRE na tela inicial do CBR MOD para iniciar o jogo.";
        } else if ("GRAPHICS".equals(selected)) {
            message = "Ajustes gráficos internos do jogo não são controlados por este painel. Use as opções gráficas do próprio Free Fire.";
        } else if ("SETTINGS".equals(selected)) {
            message = "A bolha CBR pode ser arrastada. Toque nela 3 vezes para mostrar ou ocultar este painel.";
        } else {
            message = "As configurações desta versão são locais. Perfis avançados ainda não estão disponíveis.";
        }
        TextView detail = text(message, 11, WHITE, false);
        detail.setPadding(0, dp(8), 0, 0);
        panelBody.addView(detail);
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