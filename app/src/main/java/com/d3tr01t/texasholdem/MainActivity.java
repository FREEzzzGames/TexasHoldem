package com.d3tr01t.texasholdem;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView gameView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(10, 5, 7));
        getWindow().setNavigationBarColor(Color.rgb(10, 5, 7));
        enterImmersive();

        try {
            String html = readGameAsset();
            gameView = new WebView(getApplicationContext());
            gameView.setBackgroundColor(Color.rgb(10, 5, 7));
            gameView.setOverScrollMode(View.OVER_SCROLL_NEVER);

            WebSettings settings = gameView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setTextZoom(100);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setMediaPlaybackRequiresUserGesture(true);

            gameView.setWebChromeClient(new WebChromeClient());
            gameView.setWebViewClient(new WebViewClient());
            setContentView(gameView);
            gameView.loadDataWithBaseURL(
                "https://texasholdem.local/",
                html,
                "text/html",
                "UTF-8",
                null
            );
        } catch (Throwable error) {
            TextView message = new TextView(this);
            message.setTextColor(Color.WHITE);
            message.setBackgroundColor(Color.rgb(10, 5, 7));
            message.setTextSize(16);
            message.setPadding(32, 48, 32, 32);
            message.setText("Не удалось запустить игру.\\n\\n" + error.getClass().getSimpleName()
                + ": " + String.valueOf(error.getMessage()));
            setContentView(message);
        }
    }

    private String readGameAsset() throws Exception {
        try (InputStream input = getAssets().open("index.html");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String html = output.toString("UTF-8");
            if (html.length() < 1000 || !html.contains("</html>")) {
                throw new IllegalStateException("Game asset is incomplete");
            }
            return html;
        }
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.stopLoading();
            gameView.loadUrl("about:blank");
            gameView.destroy();
            gameView = null;
        }
        super.onDestroy();
    }
}
