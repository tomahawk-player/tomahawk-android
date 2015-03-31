/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.activities;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import de.greenrobot.event.EventBus;

public class WebViewActivity extends Activity {

    public static final String TAG = WebViewActivity.class.getSimpleName();

    public static final String URL_EXTRA = "url";

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_view_activity);

        String url = getIntent().getStringExtra(URL_EXTRA);
        String redirectUri = "";
        String[] parts = url.split("redirect_uri=");
        if (parts.length > 1) {
            parts = parts[1].split("&");
            if (parts.length > 0) {
                redirectUri = parts[0];
            }
        }
        try {
            final String finalRedirectUri = URLDecoder.decode(redirectUri, "UTF-8");

            mWebView = (WebView) findViewById(R.id.webview);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (finalRedirectUri != null && url.startsWith(finalRedirectUri)) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                        finish();
                        return true;
                    } else {
                        view.loadUrl(url);
                        return false;
                    }
                }
            });
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.loadUrl(url);
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "onCreate - " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            AuthenticatorManager.ConfigTestResultEvent event
                    = new AuthenticatorManager.ConfigTestResultEvent();
            event.mComponent = PipeLine.getInstance()
                    .getResolver(TomahawkApp.PLUGINNAME_SPOTIFY);
            EventBus.getDefault().post(event);
            super.onBackPressed();
        }
    }
}
