/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.authentication;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.net.TrafficStatsCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HatchetAuthenticator extends AbstractAccountAuthenticator implements Authenticator {

    private static final String TAG = HatchetAuthenticator.class.getName();

    public static final String AUTH_URL = "https://auth.hatchet.is/v1";

    public static final String HATCHET_URL = "ws://hatchet.toma.hk/";

    public static final String ACCOUNT_TYPE = "org.tomahawk";

    public static final String AUTH_TOKEN_TYPE = "org.tomahawk.authtoken";

    public static final String ACCOUNT_NAME = "Tomahawk";

    private Context mContext;

    private List<AccessToken> mAccessTokens;

    private static class AccessToken {

        String token;

        String remotehost;

        String localhost;

        String type;

        int port;

        int expiration;

        AccessToken(String token, String remotehost, String type, int port, int expiration) {
            this.token = token;
            this.remotehost = remotehost;
            this.type = type;
            this.port = port;
            this.expiration = expiration;

            try {
                localhost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {

                WifiManager wifiMan = (WifiManager) TomahawkApp.getContext()
                        .getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInf = wifiMan.getConnectionInfo();

                localhost = Integer.toString(wifiInf.getIpAddress());
            }
        }
    }

    public HatchetAuthenticator(Context context) {
        super(context);

        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        final Intent intent = new Intent(mContext, TomahawkMainActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.setAction(TomahawkMainActivity.CALLED_TO_ADD_ACCOUNT);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);

        if (authToken == null) {
            final String password = am.getPassword(account);
            if (password != null) {
                authToken = login(account.name, password);
            }
        }

        if (authToken != null && authToken.length() > 0) {

            am.setAuthToken(account, authTokenType, authToken);

            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        final Intent intent = new Intent(mContext, TomahawkMainActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.setAction(TomahawkMainActivity.CALLED_TO_ADD_ACCOUNT);
        intent.putExtra("username", account.name);
        intent.putExtra("authtokentype", authTokenType);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    //The methods below implement the Authenticator interface


    @Override
    public int getTitleResourceId() {
        return R.string.authenticator_title_hatchet;
    }

    @Override
    public String login(String name, String password) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", name);
        params.put("password", password);

        return post(new JSONObject(params));
    }

    @Override
    public void loginWithToken() {

    }

    @Override
    public void logout() {

    }

    @Override
    public String getUserId() {
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public boolean isAuthenticating() {
        return false;
    }

    private static String post(JSONObject params) {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        TomahawkHttpClient httpclient = new TomahawkHttpClient(httpParams);

        String query = params.has("authtoken") ? "tokens" : "login";
        HttpPost httpost = new HttpPost(AUTH_URL + query);

        TrafficStatsCompat.setThreadStatsTag(0xF00D);

        try {
            httpost.setEntity(new StringEntity(params.toString()));

            httpost.setHeader("Accept", "application/json; charset=utf-8");
            httpost.setHeader("Content-type", "application/json; charset=utf-8");
            HttpResponse httpresponse = httpclient.execute(httpost);

            BufferedReader reader = null;
            JSONObject jsonObj = null;

            reader = new BufferedReader(
                    new InputStreamReader(httpresponse.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            jsonObj = new JSONObject(json);

            Log.d(TAG, "Tomahawk server response: " + jsonObj.toString());

            /* Test if an error occurred. */
            if (jsonObj.has("error") && jsonObj.getString("error").equals("true")) {
                throw new IllegalArgumentException(jsonObj.getString("errormsg"));
            }

            JSONObject msg = jsonObj.getJSONObject("message");

            if (msg.has("accesstokens")) {
                return msg.getJSONArray("accesstokens").toString();
            }

            String token = msg.getJSONObject("authtoken").getString("token");
            return token;
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            TrafficStatsCompat.clearThreadStatsTag();
        }

        Log.e(TAG, "Unknown error authenticating against Tomahawk server.");
        return null;
    }
}
