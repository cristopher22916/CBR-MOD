package com.cbrmod.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 901;
    private LinearLayout root;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10, 8, 22));
        getWindow().setNavigationBarColor(Color.rgb(10, 8, 22));
        prefs = getSharedPreferences("cbr", MODE_PRIVATE);
        if (prefs.getBoolean("logged", false)) showLauncher(); else showLogin();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void base(String title, String subtitle) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(26), dp(30), dp(26), dp(24));
        root.setBackgroundColor(Color.rgb(10, 8, 22));
        setContentView(root);
        TextView brand = new TextView(this);
        brand.setText(title);
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(30);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextColor(Color.rgb(180, 165, 255));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(42)));
    }

    private EditText field(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(150, 145, 170));
        e.setTextSize(15);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(135, 85, 255)));
        if (password) e.setInputType(129);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(14);
        root.addView(e, lp);
        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(105, 55, 220)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(16);
        root.addView(b, lp);
        return b;
    }

    private void showLogin() {
        base("CBR MOD", "PAINEL COMPANHEIRO • ANDROID");
        EditText user = field("Usuário", false);
        EditText pass = field("Senha", true);
        Button enter = button("ENTRAR");
        enter.setOnClickListener(v -> {
            if ("criscbr".equals(user.getText().toString().trim()) && "1234".equals(pass.getText().toString())) {
                prefs.edit().putBoolean("logged", true).apply();
                showLauncher();
            } else Toast.makeText(this, "Usuário ou senha incorretos.", Toast.LENGTH_SHORT).show();
        });
        TextView info = new TextView(this);
        info.setText("Acesso local de demonstração");
        info.setTextColor(Color.GRAY);
        info.setGravity(Gravity.CENTER);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(50)));
    }

    private void showLauncher() {
        base("CBR MOD", "BEM-VINDO • CENTRAL DE CONTROLE");
        Button overlay = button("INJETAR PAINEL");
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, OVERLAY_REQUEST);
            } else startOverlay();
        });
        Button game = button("ABRIR FREE FIRE");
        game.setOnClickListener(v -> openGame());
        Button logout = button("SAIR DA CONTA");
        logout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55, 48, 75)));
        logout.setOnClickListener(v -> { prefs.edit().putBoolean("logged", false).apply(); stopService(new Intent(this, OverlayService.class)); showLogin(); });
        TextView note = new TextView(this);
        note.setText("O painel é um utilitário independente. Ele não injeta código nem modifica o jogo.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(72)));
    }

    private void startOverlay() {
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Painel flutuante iniciado.", Toast.LENGTH_SHORT).show();
    }

    private void openGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
        if (i == null) i = getPackageManager().getLaunchIntentForPackage("com.dts.freefiremax");
        if (i == null) Toast.makeText(this, "Free Fire não encontrado neste aparelho.", Toast.LENGTH_LONG).show();
        else startActivity(i);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) startOverlay();
            else Toast.makeText(this, "Permissão de sobreposição não concedida.", Toast.LENGTH_SHORT).show();
        }
    }
}
