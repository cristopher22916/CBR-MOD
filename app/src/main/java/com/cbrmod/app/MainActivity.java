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
        showWelcome();
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(54));
        lp.topMargin = dp(16);
        root.addView(b, lp);
        return b;
    }

    private void note(String value) {
        TextView info = new TextView(this);
        info.setText(value);
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(12);
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(76)));
    }

    // First screen: the requested single action.
    private void showWelcome() {
        base("CBR MOD", "FREE FIRE  •  PAINEL COMPANHEIRO");
        root.addView(new android.view.View(this), new LinearLayout.LayoutParams(1, 0, 1));
        Button start = button("INJETAR PAINEL NO FREE FIRE");
        start.setOnClickListener(v -> showMiniPanel());
        note("Toque para abrir o mini painel CBR MOD.");
    }

    // Second screen: a compact launcher step before authentication.
    private void showMiniPanel() {
        base("CBR MOD", "MINI PAINEL");
        TextView card = new TextView(this);
        card.setText("PAINEL EXTERNO\n\nEntre para liberar a etapa de ativação.");
        card.setTextColor(Color.WHITE);
        card.setTextSize(16);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundColor(Color.rgb(25, 19, 45));
        root.addView(card, new LinearLayout.LayoutParams(-1, dp(180)));
        Button access = button("ACESSAR PAINEL");
        access.setOnClickListener(v -> showLogin());
        Button back = button("VOLTAR");
        back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55, 48, 75)));
        back.setOnClickListener(v -> showWelcome());
    }

    private void showLogin() {
        base("CBR MOD", "LOGIN DE ACESSO");
        EditText user = field("Usuário", false);
        EditText pass = field("Senha", true);
        Button enter = button("ENTRAR");
        enter.setOnClickListener(v -> {
            if ("criscbr".equals(user.getText().toString().trim()) && "1234".equals(pass.getText().toString())) {
                prefs.edit().putBoolean("logged", true).apply();
                showInjectScreen();
            } else Toast.makeText(this, "Usuário ou senha incorretos.", Toast.LENGTH_SHORT).show();
        });
        note("Acesso permanente • informe seus dados para continuar.");
    }

    private void showInjectScreen() {
        base("CBR MOD", "ACESSO LIBERADO");
        TextView status = new TextView(this);
        status.setText("LOGIN APROVADO\n\nFree Fire pronto para abrir.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(Color.rgb(25, 19, 45));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(150)));
        Button inject = button("INJETAR NO JOGO");
        inject.setOnClickListener(v -> requestOverlayAndLaunch());
        note("Esta ação inicia o painel companheiro e abre o Free Fire. Não injeta código nem modifica o jogo.");
    }

    private void requestOverlayAndLaunch() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, OVERLAY_REQUEST);
        } else startOverlayAndGame();
    }

    private void startOverlayAndGame() {
        Intent service = new Intent(this, OverlayService.class);
        service.putExtra("open_panel", true);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        openGame();
        Toast.makeText(this, "Painel companheiro iniciado.", Toast.LENGTH_SHORT).show();
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
            if (Settings.canDrawOverlays(this)) startOverlayAndGame();
            else Toast.makeText(this, "Permissão de sobreposição não concedida.", Toast.LENGTH_SHORT).show();
        }
    }
}