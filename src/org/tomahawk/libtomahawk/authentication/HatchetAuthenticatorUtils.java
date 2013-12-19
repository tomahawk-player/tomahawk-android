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

import com.codebutler.android_websockets.WebSocketClient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HatchetAuthenticatorUtils extends AuthenticatorUtils
        implements WebSocketClient.Listener {

    private static final String TAG = HatchetAuthenticatorUtils.class.getName();

    public static final String LOGIN_SERVER = "https://auth.hatchet.is/v1";

    public static final String ACCESS_TOKEN_SERVER = "https://auth.hatchet.is/v1";

    public static final String PATH_AUTH_CREDENTIALS = "/auth/credentials";

    public static final String PATH_TOKENS = "/tokens/";

    public static final String PARAMS_USERNAME = "username";

    public static final String PARAMS_PASSWORD = "password";

    public static final String PARAMS_CLIENT = "client";

    public static final String PARAMS_NONCE = "nonce";

    public static final String PARAMS_TYPE = "type";

    public static final String PARAMS_TOKENS = "tokens";

    public static final String PARAMS_REFRESH_TOKEN = "refresh_token";

    public static final String PARAMS_EXPIRATION = "expiration";

    public static final String PARAMS_RESULT = "result";

    public static final String PARAMS_ERRORINFO = "errorinfo";

    public static final String PARAMS_DESCRIPTION = "description";

    private PublicKey mPublicKey;

    private WebSocketClient mWebSocketClient;

    // This listener handles every event regarding the login/logout methods
    private AuthenticatorListener mAuthenticatorListener = new AuthenticatorListener() {

        @Override
        public void onInit() {

        }

        @Override
        public void onLogin(String username) {
            Log.d(TAG,
                    "TomahawkService: Hatchet user '" + username + "' logged in successfully :)");
        }

        @Override
        public void onLoginFailed(String message) {
            Log.d(TAG, "TomahawkService: Hatchet login failed :( message: " + message);
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_HATCHET, false);
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "TomahawkService: Hatchet user logged out");
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_HATCHET, false);
        }

        @Override
        public void onAuthTokenProvided(String username, String authToken) {
            if (username != null && !TextUtils.isEmpty(username) && authToken != null && !TextUtils
                    .isEmpty(authToken)) {
                Log.d(TAG, "TomahawkService: Hatchet auth token is served and yummy");
                Account account = new Account(username,
                        mTomahawkApp.getString(R.string.accounttype_string));
                AccountManager am = AccountManager.get(mTomahawkApp);
                if (am != null) {
                    am.addAccountExplicitly(account, null, new Bundle());
                    am.setUserData(account, TomahawkService.AUTHENTICATOR_NAME, mName);
                    am.setAuthToken(account, mAuthTokenType, authToken);
                }
            }
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_HATCHET, true);
        }
    };

    public HatchetAuthenticatorUtils(TomahawkApp tomahawkApp, TomahawkService tomahawkService) {
        mTomahawkApp = tomahawkApp;
        mTomahawkService = tomahawkService;
        mName = TomahawkService.AUTHENTICATOR_NAME_HATCHET;
        mAuthTokenType = TomahawkService.AUTH_TOKEN_TYPE_HATCHET;
        mAuthenticatorListener.onInit();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = TomahawkApp.getContext().getResources().openRawResource(
                            R.raw.mandella);
                    Scanner scanner = new Scanner(in).useDelimiter("\\A");
                    String rawString = scanner.hasNext() ? scanner.next() : "";
                    in.close();

                    String publicKeyPEM = rawString.replace("\n", "");
                    publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
                    publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
                    byte[] decoded = Base64.decode(publicKeyPEM, Base64.DEFAULT);
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    mPublicKey = keyFactory.generatePublic(spec);
                } catch (InvalidKeySpecException e) {
                    Log.e(TAG, "TomahawkAuthenticator(constructor): " + e.getClass() + ":" + e
                            .getLocalizedMessage());
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "TomahawkAuthenticator(constructor): " + e.getClass() + ":" + e
                            .getLocalizedMessage());
                } catch (IOException e) {
                    Log.e(TAG, "TomahawkAuthenticator(constructor): " + e.getClass() + ":" + e
                            .getLocalizedMessage());
                }

                AccountManager am = AccountManager.get(mTomahawkApp);
                if (am != null) {
                    Account[] accounts = am
                            .getAccountsByType(mTomahawkApp.getString(R.string.accounttype_string));
                    String userId = null;
                    String authToken = null;
                    if (accounts != null) {
                        for (Account account : accounts) {
                            if (mName.equals(am
                                    .getUserData(account, TomahawkService.AUTHENTICATOR_NAME))) {
                                userId = account.name;
                                authToken = am.peekAuthToken(account, mAuthTokenType);
                            }
                        }
                    }
                    if (userId != null && authToken != null) {
                        mAccessTokens = requestAccessTokens(userId, authToken);
                    }
                }

                if (mAccessTokens == null) {
                    return;
                }

                mWebSocketClient = new WebSocketClient(URI.create(ACCESS_TOKEN_SERVER),
                        HatchetAuthenticatorUtils.this, null);
                mWebSocketClient.connect();
            }
        }).start();
    }

    /**
     * Called when the websocket client has connected.
     */
    @Override
    public void onConnect() {
        Log.d(TAG, "Tomahawk websocket connected.");

        /** For testing we will attempt to register. */
        AccessToken token = mAccessTokens.get(0);

        JSONObject register = new JSONObject();
        try {
            register.put("command", "register");
            register.put("hostname", token.localhost);
            register.put("port", token.port);
            register.put("accesstoken", token.token);
            //register.put("username", mUserId);
            register.put("dbid", "nil");
        } catch (JSONException e) {
            Log.e(TAG, "onConnect: " + e.getClass() + ":" + e.getLocalizedMessage());
        }
        mWebSocketClient.send(register.toString());
    }

    /**
     * Called when the websocket client has received a message.
     */
    @Override
    public void onMessage(String msg) {
        Log.d(TAG, "Message from Tomahawk server: " + msg);
    }

    /**
     * Called when the websocket client has received a binary message.
     */
    @Override
    public void onMessage(byte[] data) {
        Log.d(TAG, "Binary message from Tomahawk server.");
    }

    /**
     * Called when the websocket client has been disconnected.
     */
    @Override
    public void onDisconnect(int code, String reason) {
        Log.d(TAG, "Tomahawk websocket disconnected.");
    }

    /**
     * Called when an error has occurred with the websocket client.
     */
    @Override
    public void onError(Exception error) {
        throw new IllegalArgumentException(error.toString());
    }

    /**
     * Requests access tokens for the given user id and valid auth token.
     */
    public static List<AccessToken> requestAccessTokens(String userid, String authToken) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAMS_USERNAME, userid);
        params.put(PARAMS_TOKENS, authToken);

        try {
            String jsonString = post(new JSONObject(params));
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.has(PARAMS_RESULT)) {
                JSONObject result = jsonObject.getJSONObject(PARAMS_RESULT);
                if (result.has(PARAMS_TOKENS)) {
                    List<AccessToken> accessTokens = new ArrayList<AccessToken>();
                    JSONArray tokens = jsonObject.getJSONObject(PARAMS_RESULT).getJSONArray(
                            PARAMS_TOKENS);
                    for (int i = 0; i < tokens.length(); i++) {
                        JSONObject host = tokens.getJSONObject(i);
                        AccessToken token = new AccessToken(host.getString("token"),
                                host.getString("host"),
                                host.getString("type"), host.getInt("port"),
                                host.getInt("expiration"));

                        accessTokens.add(token);
                    }
                    return accessTokens;
                } else if (result.has(PARAMS_ERRORINFO)) {
                    Log.e(TAG, "requestAccessTokens: " + result.getJSONObject(PARAMS_ERRORINFO)
                            .getString(PARAMS_DESCRIPTION));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "requestAccessTokens: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "requestAccessTokens: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "requestAccessTokens: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public int getTitleResourceId() {
        return R.string.authenticator_title_hatchet;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.hatchet_icon;
    }

    @Override
    public void login(final String name, final String password) {
        mIsAuthenticating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(PARAMS_PASSWORD, password);
                params.put(PARAMS_USERNAME, name);
                params.put(PARAMS_CLIENT, "Tomahawk Android (" + android.os.Build.MODEL + ")");
                /*byte[] uuid = UUID.randomUUID().toString().getBytes();
                Cipher cipher = Cipher.getInstance("OAEP");
                cipher.init(Cipher.ENCRYPT_MODE, mPublicKey);
                byte[] encryptedUuidBytes = cipher.doFinal(uuid);
                String encryptedUuid = Base64.encodeToString(
                        encryptedUuidBytes, Base64.DEFAULT);
                params.put(PARAMS_NONCE, encryptedUuid);*/
                params.put(PARAMS_NONCE, "");
                try {
                    String string = post(new JSONObject(params));
                    JSONObject jsonObject = new JSONObject(string);
                    if (jsonObject.has(PARAMS_RESULT)) {
                        JSONObject result = jsonObject.getJSONObject(PARAMS_RESULT);
                        if (result.has(PARAMS_ERRORINFO)) {
                            mAuthenticatorListener.onLoginFailed(result.getJSONObject(
                                    PARAMS_ERRORINFO).getString(PARAMS_DESCRIPTION));
                        } else if (result.has(PARAMS_USERNAME) && result
                                .has(PARAMS_REFRESH_TOKEN)) {
                            mAuthenticatorListener.onLogin(result.getString(PARAMS_USERNAME));
                            mAuthenticatorListener
                                    .onAuthTokenProvided(result.getString(PARAMS_USERNAME),
                                            result.getString(PARAMS_REFRESH_TOKEN));
                        } else {
                            mAuthenticatorListener.onLoginFailed("Unknown error");
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage());
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void loginWithToken() {
        mIsAuthenticating = true;
    }

    @Override
    public void logout() {
        mIsAuthenticating = true;
        final AccountManager am = AccountManager.get(mTomahawkApp);
        if (am != null) {
            Account[] accounts = am
                    .getAccountsByType(mTomahawkApp.getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (mName.equals(am.getUserData(account, TomahawkService.AUTHENTICATOR_NAME))) {
                        am.removeAccount(account, null, null);
                    }
                }
            }
        }
        mAuthenticatorListener.onLogout();
    }

    private static String post(JSONObject params) throws IOException {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        TomahawkHttpClient httpClient = new TomahawkHttpClient(httpParams);

        String query = params.has(PARAMS_REFRESH_TOKEN) ? PATH_TOKENS : PATH_AUTH_CREDENTIALS;
        HttpPost httpPost = new HttpPost(LOGIN_SERVER + query);

        httpPost.setEntity(new StringEntity(params.toString()));

        httpPost.setHeader("Content-type", "application/json; charset=utf-8");
        HttpResponse httpresponse = httpClient.execute(httpPost);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(httpresponse.getEntity().getContent(), "UTF-8"));
        return reader.readLine();
    }
}
