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
import android.widget.ImageView;
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
    private static final int GREEN = Color.rgb(65, 220, 125);
    private static final int YELLOW = Color.rgb(255, 205, 65);
    private static final int PURPLE = Color.rgb(139, 83, 255);
    private static final int CYAN = GREEN;
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
        bubble.setBackground(shape(Color.rgb(20, 27, 25), 18, GREEN));
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
        shell.setPadding(dp(8), dp(6), dp(8), dp(6));
        shell.setBackground(shape(Color.rgb(13, 17, 16), 20, GREEN));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView emblem = text("♛", 24, GREEN, true);
        emblem.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emblemLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        emblemLp.rightMargin = dp(7);
        header.addView(emblem, emblemLp);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(text("CBR MOD", 16, WHITE, true));
        brand.addView(text("PAINEL FLUTUANTE", 7, YELLOW, true));
        header.addView(brand);
        TextView close = text("✕", 19, WHITE, true);
        close.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(close, new LinearLayout.LayoutParams(0, dp(30), 1));
        close.setOnClickListener(v -> togglePanel());
        shell.addView(header);

        TextView subtitle = text("FREE FIRE  •  PAINEL COMPANHEIRO", 8, CYAN, true);
        subtitle.setPadding(0, dp(1), 0, dp(3));
        shell.addView(subtitle);

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setGravity(Gravity.TOP);
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.VERTICAL);
        navigation.setPadding(dp(3), dp(3), dp(3), dp(3));
        navigation.setBackground(shape(Color.rgb(25, 29, 27), 10, YELLOW));
        String[] categories = {"AIMBOT", "ESP", "MEMORY", "PLAYER", "GRAPHICS", "SETTINGS", "CONFIGS"};
        for (String category : categories) {
            TextView tab = text(category, 8, WHITE, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(2), dp(4), dp(2), dp(4));
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(-1, dp(23));
            tabLp.bottomMargin = dp(2);
            navigation.addView(tab, tabLp);
            tab.setBackground(shape(category.equals(selected) ? Color.rgb(28, 83, 53) : Color.rgb(38, 43, 40), 7, category.equals(selected) ? GREEN : Color.rgb(80, 86, 82)));
            tab.setOnClickListener(v -> {
                selected = category;
                updatePanelContent();
                for (int i = 0; i < navigation.getChildCount(); i++) {
                    TextView item = (TextView) navigation.getChildAt(i);
                    String name = categories[i];
                    item.setBackground(shape(name.equals(selected) ? Color.rgb(28, 83, 53) : Color.rgb(38, 43, 40), 7, name.equals(selected) ? GREEN : Color.rgb(80, 86, 82)));
                }
            });
        }
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(dp(82), -2);
        navLp.rightMargin = dp(7);
        workspace.addView(navigation, navLp);
        panelBody = new LinearLayout(this);
        panelBody.setOrientation(LinearLayout.VERTICAL);
        panelBody.setPadding(dp(8), dp(6), dp(8), dp(6));
        panelBody.setBackground(shape(Color.rgb(29, 34, 31), 10, PURPLE));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(0, dp(174), 1);
        workspace.addView(panelBody, bodyLp);
        shell.addView(workspace);
        updatePanelContent();

        TextView hint = text("3 TOQUES NA BOLHA: MOSTRAR / OCULTAR", 7, Color.rgb(165, 153, 190), true);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(3), 0, 0);
        shell.addView(hint);

        TextView exit = text("SAIR DO PAINEL", 9, WHITE, true);
        exit.setGravity(Gravity.CENTER);
        exit.setPadding(dp(8), dp(7), dp(8), dp(7));
        exit.setBackground(shape(Color.rgb(27, 67, 45), 9, GREEN));
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(-1, dp(27));
        exitLp.topMargin = dp(4);
        shell.addView(exit, exitLp);
        exit.setOnClickListener(v -> {
            if (panel != null) {
                stopSelf();
            }
        });

        panel = shell;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int panelWidth = Math.min(dp(430), screenWidth - dp(12));
        panelParams = new WindowManager.LayoutParams(panelWidth, -2, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;
        wm.addView(panel, panelParams);
    }

    private void updatePanelContent() {
        if (panelBody == null) return;
        panelBody.removeAllViews();
        panelBody.addView(text(selected, 12, CYAN, true));
        panelBody.addView(text("━━━━━━━━━━━━━━━━━━━━", 8, YELLOW, false));
        String message;
        if ("AIMBOT".equals(selected) || "ESP".equals(selected) || "MEMORY".equals(selected)) {
            message = "Área reservada para informações do módulo. Esta versão não altera a mira, não revela jogadores e não modifica a memória do jogo.";
        } else if ("PLAYER".equals(selected)) {
            message = "Informações e preferências do jogador serão organizadas aqui.";
        } else if ("GRAPHICS".equals(selected)) {
            message = "Atalhos e orientações para as opções gráficas oficiais do Free Fire.";
        } else if ("SETTINGS".equals(selected)) {
            message = "Arraste a bolha para reposicionar. Três toques alternam a exibição do painel.";
        } else {
            message = "Área para perfis e preferências locais. As opções serão definidas com suas referências.";
        }
        TextView detail = text(message, 10, WHITE, false);
        detail.setPadding(0, dp(8), 0, 0);
        panelBody.addView(detail);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("open_panel", false) && panel == null) togglePanel();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (wm != null) {
            if (panel != null) { wm.removeView(panel); panel = null; }
            if (bubble != null) { wm.removeView(bubble); bubble = null; }
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}