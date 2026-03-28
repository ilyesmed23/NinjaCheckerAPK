package com.ninja.checker;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0f0f1a"));
        setContentView(webView);

        // Paramètres WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 12; Phone) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();

                // Intercepter toutes les requêtes vers ninjakitchen.fr
                if (url.contains("ninjakitchen.fr")) {
                    return fetchNative(url, request.getMethod());
                }

                // Laisser passer les autres (fichiers locaux, etc.)
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Ouvrir les liens ninjakitchen dans le webview lui-même
                if (url.contains("ninjakitchen.fr")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        // Charger l'application depuis les assets
        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * Effectue une requête HTTP native (pas de restriction CORS)
     * et retourne la réponse au WebView.
     */
    private WebResourceResponse fetchNative(String urlStr, String method) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method != null ? method : "GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);

            // Headers pour simuler un vrai navigateur
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity");

            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 400) {
                return errorResponse("HTTP " + responseCode);
            }

            // Détecter le type de contenu
            String contentType = conn.getContentType();
            String mimeType = "text/html";
            String encoding = "utf-8";

            if (contentType != null) {
                String[] parts = contentType.split(";");
                if (parts.length > 0) mimeType = parts[0].trim();
                for (String part : parts) {
                    String p = part.trim().toLowerCase();
                    if (p.startsWith("charset=")) {
                        encoding = p.substring(8).trim();
                    }
                }
            }

            // Headers CORS pour que le JS puisse lire la réponse
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Access-Control-Allow-Origin", "*");
            responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            responseHeaders.put("Access-Control-Allow-Headers", "*");

            InputStream inputStream = conn.getInputStream();
            return new WebResourceResponse(mimeType, encoding, 200, "OK",
                responseHeaders, inputStream);

        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    private WebResourceResponse errorResponse(String message) {
        String body = "{\"error\": \"" + (message != null ? message : "unknown") + "\"}";
        return new WebResourceResponse(
            "application/json", "utf-8", 500, "Error",
            null,
            new ByteArrayInputStream(body.getBytes())
        );
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
