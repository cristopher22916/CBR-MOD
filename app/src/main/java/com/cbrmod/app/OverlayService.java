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
        shell.setPadding(dp(11), dp(9), dp(11), dp(8));
        shell.setBackground(shape(Color.rgb(16, 12, 31), 20, PURPLE));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        ImageView emblem = new ImageView(this);
        emblem.setImageResource(R.drawable.cbr_floating_logo_full);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams emblemLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        emblemLp.rightMargin = dp(7);
        header.addView(emblem, emblemLp);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(text("CBR MOD", 16, WHITE, true));
        brand.addView(text("PAINEL FLUTUANTE", 7, CYAN, true));
        header.addView(brand);
        TextView close = text("✕", 19, WHITE, true);
        close.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(close, new LinearLayout.LayoutParams(0, dp(34), 1));
        close.setOnClickListener(v -> togglePanel());
        shell.addView(header);

        TextView subtitle = text("FREE FIRE  •  PAINEL COMPANHEIRO", 8, CYAN, true);
        subtitle.setPadding(0, dp(2), 0, dp(5));
        shell.addView(subtitle);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        String[] categories = {"AIMBOT", "ESP", "MEMORY", "PLAYER", "GRAPHICS", "SETTINGS", "CONFIGS"};
        for (String category : categories) {
            TextView tab = text(category, 10, WHITE, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(5), dp(5), dp(5), dp(5));
            tab.setBackground(shape(category.equals(selected) ? Color.rgb(75, 45, 125) : Color.rgb(31, 25, 51), 9, category.equals(selected) ? CYAN : Color.rgb(67, 56, 91)));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(145); lp.height = dp(32);
            lp.setMargins(dp(3), dp(2), dp(3), dp(2));
            grid.addView(tab, lp);
            tab.setOnClickListener(v -> { selected = category; updatePanelContent(); refreshTabs(grid, categories); });
        }
        shell.addView(grid);

        panelBody = new LinearLayout(this);
        panelBody.setOrientation(LinearLayout.VERTICAL);
        panelBody.setPadding(dp(9), dp(7), dp(9), dp(7));
        panelBody.setBackground(shape(Color.rgb(25, 20, 43), 11, Color.rgb(58, 47, 82)));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, dp(88));
        bodyLp.topMargin = dp(5);
        shell.addView(panelBody, bodyLp);
        updatePanelContent();

        TextView hint = text("3 TOQUES NA BOLHA: MOSTRAR / OCULTAR", 7, Color.rgb(165, 153, 190), true);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(5), 0, 0);
        shell.addView(hint);

        panel = shell;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int panelWidth = Math.min(dp(330), screenWidth - dp(20));
        panelParams = new WindowManager.LayoutParams(panelWidth, -2, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;
        wm.addView(panel, panelParams);
    }

    private void refreshTabs(GridLayout grid, String[] categories) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            TextView tab = (TextView)grid.getChildAt(i);
            String category = categories[i];
            tab.setBackground(shape(category.equals(selected) ? Color.rgb(75, 45, 125) : Color.rgb(31, 25, 51), 9, category.equals(selected) ? CYAN : Color.rgb(67, 56, 91)));
        }
    }

    private void updatePanelContent() {
        if (panelBody == null) return;
        panelBody.removeAllViews();
        panelBody.addView(text(selected, 12, CYAN, true));
        String message;
        if ("AIMBOT".equals(selected) || "ESP".equals(selected) || "MEMORY".equals(selected)) {
            message = "Módulo demonstrativo. Não altera a mira, não revela jogadores e não modifica a memória do Free Fire.";
        } else if ("PLAYER".equals(selected)) {
            message = "Área do jogador. Use a tela inicial do CBR MOD para abrir o Free Fire.";
        } else if ("GRAPHICS".equals(selected)) {
            message = "Ajustes gráficos são feitos nas opções oficiais do próprio jogo.";
        } else if ("SETTINGS".equals(selected)) {
            message = "Arraste a bolha para reposicionar. Três toques alternam o painel.";
        } else {
            message = "Perfis e preferências locais. Mais opções serão organizadas após as referências visuais.";
        }
        TextView detail = text(message, 10, WHITE, false);
        detail.setPadding(0, dp(4), 0, 0);
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