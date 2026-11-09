package com.d3tr01t.texasholdem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String GAME_URL =
        "https://raw.githubusercontent.com/FREEzzzGames/TexasHoldem/main/app/src/main/assets/index.html";
    private static final String APP_ORIGIN = "https://local.poker.app/";
    private static final int CONNECT_TIMEOUT = 7000;
    private static final int READ_TIMEOUT = 10000;

    private WebView webView;
    private final ExecutorService updater = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterImmersiveMode();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(10, 5, 7));
        getWindow().setNavigationBarColor(Color.rgb(10, 5, 7));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(10, 5, 7));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !url.startsWith(APP_ORIGIN);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                String report = "WebView renderer crashed\\nDid crash: " + detail.didCrash()
                    + "\\nPriority: " + detail.rendererPriorityAtExit()
                    + "\\nAndroid: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")";
                CrashReporter.saveReport(MainActivity.this, report);
                if (view != null) {
                    view.loadDataWithBaseURL(APP_ORIGIN,
                        "<meta charset='utf-8'><body style='background:#0a0507;color:white;font:16px sans-serif;padding:24px'>" +
                        "Сбой игрового движка WebView. Отчёт сохранён. Перезапустите приложение.</body>",
                        "text/html", "UTF-8", null);
                }
                return true;
            }
        });

        // Show the most recent locally cached build immediately; update in the background.
        loadCachedOrBundledGame();
        showPreviousCrashReport();
        updateGameOnEveryLaunch();
    }

    private void enterImmersiveMode() {
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }


    private void showPreviousCrashReport() {
        File reportFile = new File(getFilesDir(), "crash-last.txt");
        if (!reportFile.isFile()) return;
        try {
            String report = readFile(reportFile);
            if (report.trim().isEmpty()) return;
            new AlertDialog.Builder(this)
                .setTitle("Диагностика предыдущего сбоя")
                .setMessage(report.length() > 12000 ? report.substring(0, 12000) : report)
                .setPositiveButton("Скопировать отчёт", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Texas Hold'em crash report", report));
                    Toast.makeText(this, "Отчёт скопирован", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Закрыть", (dialog, which) -> { })
                .show();
        } catch (Exception ignored) { }
    }

    private File cacheFile() {
        return new File(getFilesDir(), "latest-game.html");
    }

    private void loadCachedOrBundledGame() {
        try {
            File cached = cacheFile();
            if (cached.isFile() && cached.length() > 1000) {
                loadHtml(readFile(cached));
                return;
            }
            InputStream in = getAssets().open("index.html");
            loadHtml(readStream(in));
            in.close();
        } catch (Exception ignored) {
            webView.loadDataWithBaseURL(APP_ORIGIN,
                "<meta charset='utf-8'><body style='background:#0a0507;color:#fff;font:16px sans-serif;padding:24px'>Не удалось загрузить локальную игру.</body>",
                "text/html", "UTF-8", null);
        }
    }

    private void updateGameOnEveryLaunch() {
        updater.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(GAME_URL).openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("User-Agent", "TexasHoldem-Android");
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) return;

                String html = readStream(connection.getInputStream());
                // Basic integrity guard: only accept a complete-looking HTML game document.
                if (html.length() < 1000 ||
                    !html.toLowerCase().contains("<html") ||
                    !html.contains("</html>") ||
                    !html.contains("function lobby")) return;

                File tmp = new File(getFilesDir(), "latest-game.tmp");
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    out.write(html.getBytes(StandardCharsets.UTF_8));
                    out.getFD().sync();
                }
                File target = cacheFile();
                if (target.exists()) target.delete();
                if (!tmp.renameTo(target)) {
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        out.write(html.getBytes(StandardCharsets.UTF_8));
                    }
                    tmp.delete();
                }

                runOnUiThread(() -> {
                    if (webView != null && !isFinishing()) {
                        int y = webView.getScrollY();
                        loadHtml(html);
                        webView.post(() -> webView.scrollTo(0, y));
                    }
                });
            } catch (Exception ignored) {
                // Offline or update failure: keep the cached/bundled game running.
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void loadHtml(String html) {
        if (webView != null) {
            webView.loadDataWithBaseURL(APP_ORIGIN, html, "text/html", "UTF-8", null);
        }
    }

    private String readFile(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return readStream(in);
        }
    }

    private String readStream(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        return out.toString("UTF-8");
    }

    @Override
    public void onBackPressed() {
        // Keep Android Back from navigating to an external page.
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        updater.shutdownNow();
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
