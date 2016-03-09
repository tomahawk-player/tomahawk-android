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
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import de.greenrobot.event.EventBus;

public class WebViewActivity extends Activity {

    public static final String TAG = WebViewActivity.class.getSimpleName();

    public static final String URL_EXTRA = "url";

    public static final String REQUESTID_EXTRA = "requestId";

    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_view_activity);

        String url = getIntent().getStringExtra(URL_EXTRA);
        final int requestId = getIntent().getIntExtra(REQUESTID_EXTRA, -1);

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                ScriptAccount account;
                if (url.startsWith("tomahawkspotifyresolver")) {
                    account = PipeLine.get().getResolver(TomahawkApp.PLUGINNAME_SPOTIFY)
                            .getScriptAccount();
                } else if (url.startsWith("tomahawkdeezerresolver")) {
                    account = PipeLine.get().getResolver(TomahawkApp.PLUGINNAME_DEEZER)
                            .getScriptAccount();
                } else {
                    view.loadUrl(url);
                    return false;
                }
                account.onShowWebViewFinished(requestId, url);
                finish();
                return true;
            }
        });
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            AuthenticatorManager.ConfigTestResultEvent event
                    = new AuthenticatorManager.ConfigTestResultEvent();
            event.mComponent = PipeLine.get()
                    .getResolver(TomahawkApp.PLUGINNAME_SPOTIFY);
            EventBus.getDefault().post(event);
            super.onBackPressed();
        }
    }
}
