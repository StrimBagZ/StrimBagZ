package net.lubot.strimbagzrewrite.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;

import org.polaric.colorful.CActivity;

import java.net.URISyntaxException;

public class LoginActivity extends CActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WebView webView = (WebView) findViewById(R.id.loginWebView);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle("Login");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return shouldOverrideUrl(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldOverrideUrl(view, url);
            }
        });

        webView.loadUrl(Constants.URL_TWITCH_AUTHENTICATION);
    }

    public boolean shouldOverrideUrl(WebView view, String url) {
        if (url.startsWith(Constants.CALLBACK_URL)) {
            Log.d("shouldOverride", "Callback URL detected");
            Context context = view.getContext();
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    view.stopLoading();
                    PackageManager manager = context.getPackageManager();
                    ResolveInfo info = manager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        LoginActivity.this.finish();
                        context.startActivity(intent);
                    }
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return true;
        }
        view.loadUrl(url);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
