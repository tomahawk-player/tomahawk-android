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

import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class HatchetAuthenticatorUtils extends AuthenticatorUtils {

    private static final String TAG = HatchetAuthenticatorUtils.class.getSimpleName();

    public static final String AUTH_SERVER = "https://auth.hatchet.is/v1/authentication/password";

    public static final String REFRESH_TOKEN_SERVER
            = "https://auth.hatchet.is/v1/tokens/refresh/bearer";

    public static final String TOKEN_SERVER = "https://auth.hatchet.is/v1/tokens/fetch/";

    public static final String PARAMS_GRANT_TYPE = "grant_type";

    public static final String PARAMS_GRANT_TYPE_PASSWORD = "password";

    public static final String PARAMS_GRANT_TYPE_REFRESHTOKEN = "refresh_token";

    public static final String PARAMS_AUTHORIZATION = "authorization";

    public static final String PARAMS_USERNAME = "username";

    public static final String PARAMS_PASSWORD = "password";

    public static final String PARAMS_REFRESHTOKEN = "refresh_token";

    public static final String RESPONSE_TOKEN_TYPE_BEARER = "bearer";

    public static final String RESPONSE_TOKEN_TYPE_CALUMET = "calumet";

    public static final String RESPONSE_ERROR_INVALID_REQUEST = "invalid_request";

    private static final int EXPIRING_LIMIT = 300;

    private User mLoggedInUser;

    public HatchetAuthenticatorUtils(String id, String prettyName) {
        super(id, prettyName);

        onInit();
    }

    @Override
    public void onInit() {
    }

    @Override
    public void onLogin(String username, String refreshToken,
            long refreshTokenExpiresIn, String accessToken, long accessTokenExpiresIn) {
        Log.d(TAG,
                "Hatchet user '" + username + "' logged in successfully :)");
        if (username != null && !TextUtils.isEmpty(username) && refreshToken != null
                && !TextUtils.isEmpty(refreshToken)) {
            Log.d(TAG, "Hatchet auth token is served and yummy");
            Account account = new Account(username,
                    TomahawkApp.getContext().getString(R.string.accounttype_string));
            AccountManager am = AccountManager.get(TomahawkApp.getContext());
            if (am != null) {
                am.addAccountExplicitly(account, null, new Bundle());
                am.setUserData(account, AuthenticatorUtils.ACCOUNT_NAME,
                        getAccountName());
                am.setAuthToken(account, getAuthTokenName(),
                        refreshToken);
                am.setUserData(account, AuthenticatorUtils.AUTH_TOKEN_EXPIRES_IN_HATCHET,
                        String.valueOf(refreshTokenExpiresIn));
                am.setUserData(account, AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_HATCHET,
                        accessToken);
                am.setUserData(account,
                        AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                        String.valueOf(accessTokenExpiresIn));
                ensureAccessTokens();
            }
        }
        CollectionManager.getInstance().fetchHatchetUserPlaylists();
        CollectionManager.getInstance().fetchLovedItemsUserPlaylists();
        CollectionManager.getInstance().fetchStarredArtists();
        CollectionManager.getInstance().fetchStarredAlbums();
        InfoSystem.getInstance().resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SELF,
                null);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
    }

    @Override
    public void onLoginFailed(int type, String message) {
        Log.d(TAG,
                "Hatchet login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "Hatchet user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.hatchet_icon;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return R.string.logindialog_username_label_string;
    }

    @Override
    public void login(Activity activity, final String name, final String password) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_AUTHENTICATING) {
                    @Override
                    public void run() {
                        Map<String, String> dataMap = new HashMap<String, String>();
                        dataMap.put(PARAMS_PASSWORD, password);
                        dataMap.put(PARAMS_USERNAME, name);
                        dataMap.put(PARAMS_GRANT_TYPE, PARAMS_GRANT_TYPE_PASSWORD);
                        String data = TomahawkUtils.paramsListToString(dataMap);
                        try {
                            TomahawkUtils.HttpResponse response =
                                    TomahawkUtils.httpPost(AUTH_SERVER, null, data,
                                            TomahawkUtils.HTTP_CONTENT_TYPE_FORM);
                            ObjectMapper objectMapper = InfoSystemUtils.constructObjectMapper();
                            HatchetAuthResponse authResponse = objectMapper
                                    .readValue(response.mResponseText, HatchetAuthResponse.class);
                            if (authResponse != null) {
                                if (authResponse.error != null &&
                                        authResponse.error.equals(RESPONSE_ERROR_INVALID_REQUEST)) {
                                    onLoginFailed(
                                            AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_INVALIDCREDS,
                                            authResponse.error_description);
                                } else {
                                    onLogin(authResponse.canonical_username,
                                            authResponse.refresh_token,
                                            authResponse.refresh_token_expires_in,
                                            authResponse.access_token,
                                            authResponse.expires_in);
                                }
                            } else {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR,
                                        "");
                            }
                        } catch (NoSuchAlgorithmException e) {
                            Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                            onLoginFailed(
                                    AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                    e.getClass() + ": " + e.getLocalizedMessage());
                        } catch (IOException e) {
                            Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                            onLoginFailed(
                                    AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                    e.getClass() + ": " + e.getLocalizedMessage());
                        } catch (KeyManagementException e) {
                            Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                            onLoginFailed(
                                    AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                    e.getClass() + ": " + e.getLocalizedMessage());
                        }
                    }
                }
        );
    }

    @Override
    public void logout(Activity activity) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = TomahawkUtils.getAccountByName(getAccountName());
        if (am != null && account != null) {
            am.removeAccount(account, null, null);
        }
        mLoggedInUser = null;
        onLogout();
    }

    /**
     * Ensure that the calumet access token is available and valid. Get it from the cache if it
     * hasn't yet expired. Otherwise refetch and cache it again. Also refetches the mandella access
     * token if necessary.
     *
     * @return the calumet access token
     */
    public String ensureAccessTokens() {
        Map<String, String> userData = new HashMap<String, String>();
        userData.put(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_HATCHET, null);
        userData.put(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET, null);
        userData.put(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_HATCHET, null);
        userData.put(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET, null);
        userData = TomahawkUtils
                .getUserDataForAccount(userData, getAccountName());
        String mandellaAccessToken = userData.get(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_HATCHET);
        int mandellaExpirationTime = -1;
        String mandellaExpirationTimeString =
                userData.get(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET);
        if (mandellaExpirationTimeString != null) {
            mandellaExpirationTime = Integer.valueOf(mandellaExpirationTimeString);
        }
        String calumetAccessToken = userData.get(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_HATCHET);
        int calumetExpirationTime = -1;
        String calumetExpirationTimeString =
                userData.get(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET);
        if (calumetExpirationTimeString != null) {
            calumetExpirationTime = Integer.valueOf(mandellaExpirationTimeString);
        }
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        String refreshToken = TomahawkUtils.peekAuthTokenForAccount(getAccountName(),
                getAuthTokenName());
        if (refreshToken != null && (mandellaAccessToken == null
                || currentTime > mandellaExpirationTime - EXPIRING_LIMIT)) {
            Log.d(TAG, "Mandella access token has expired, refreshing ...");
            mandellaAccessToken = fetchAccessToken(RESPONSE_TOKEN_TYPE_BEARER, refreshToken);
        }
        if (mandellaAccessToken != null && (calumetAccessToken == null
                || currentTime > calumetExpirationTime - EXPIRING_LIMIT)) {
            Log.d(TAG, "Calumet access token has expired, refreshing ...");
            calumetAccessToken = fetchAccessToken(RESPONSE_TOKEN_TYPE_CALUMET, mandellaAccessToken);
        }
        if (calumetAccessToken == null) {
            Log.d(TAG, "Calumet access token couldn't be fetched. "
                    + "Most probably because no Hatchet account is logged in.");
        }
        return calumetAccessToken;
    }

    /**
     * Fetch the accessToken of the given tokenType by providing an existent token. The token is
     * cached and then returned.
     *
     * @param tokenType The token type ("bearer"(aka mandella) or "calumet")
     * @param token     In the case of fetching the bearer token, this token should be the bearer
     *                  refresh token provided by the initial auth process. If the calumet access
     *                  token should be fetched, then the given token should be the bearer access
     *                  token.
     * @return the fetched access token
     */
    public String fetchAccessToken(String tokenType, String token) {
        String accessToken = null;
        try {
            String responseText;
            if (tokenType.equals(RESPONSE_TOKEN_TYPE_BEARER)) {
                Map<String, String> dataMap = new HashMap<String, String>();
                dataMap.put(PARAMS_REFRESHTOKEN, token);
                dataMap.put(PARAMS_GRANT_TYPE, PARAMS_GRANT_TYPE_REFRESHTOKEN);
                String data = TomahawkUtils.paramsListToString(dataMap);
                responseText = TomahawkUtils.httpPost(REFRESH_TOKEN_SERVER, null, data,
                        TomahawkUtils.HTTP_CONTENT_TYPE_FORM).mResponseText;
            } else {
                Map<String, String> params = new HashMap<String, String>();
                params.put(PARAMS_AUTHORIZATION, RESPONSE_TOKEN_TYPE_BEARER + " " + token);
                responseText = TomahawkUtils.httpGet(TOKEN_SERVER + RESPONSE_TOKEN_TYPE_CALUMET,
                        params).mResponseText;
            }
            ObjectMapper objectMapper = InfoSystemUtils.constructObjectMapper();
            HatchetAuthResponse authResponse = objectMapper
                    .readValue(responseText, HatchetAuthResponse.class);
            if (authResponse.error != null || !TomahawkUtils
                    .containsIgnoreCase(tokenType, authResponse.token_type)) {
                logout();
                onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                        "Please reenter your Hatchet credentials");
            } else if (authResponse.access_token != null) {
                Map<String, String> data = new HashMap<String, String>();
                int currentTime = (int) (System.currentTimeMillis() / 1000);
                long expirationTime = currentTime + authResponse.expires_in;
                accessToken = authResponse.access_token;
                Log.d(TAG, "Access token fetched, current time: '" + currentTime +
                        "', expiration time: '" + expirationTime + "'");
                if (TomahawkUtils.containsIgnoreCase(tokenType, RESPONSE_TOKEN_TYPE_BEARER)) {
                    data.put(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_HATCHET, accessToken);
                    data.put(AuthenticatorUtils.MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                            String.valueOf(expirationTime));
                } else {
                    data.put(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_HATCHET, accessToken);
                    data.put(AuthenticatorUtils.CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                            String.valueOf(expirationTime));
                }
                TomahawkUtils.setUserDataForAccount(data, getAccountName());
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG,
                    "fetchAccessToken: " + e.getClass() + ": " + e.getLocalizedMessage());
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG,
                    "fetchAccessToken: " + e.getClass() + ": " + e.getLocalizedMessage());
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    e.getClass() + ": " + e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,
                    "fetchAccessToken: " + e.getClass() + ": " + e.getLocalizedMessage());
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    e.getClass() + ": " + e.getLocalizedMessage());
        } catch (KeyManagementException e) {
            Log.e(TAG,
                    "fetchAccessToken: " + e.getClass() + ": " + e.getLocalizedMessage());
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    e.getClass() + ": " + e.getLocalizedMessage());
        }
        return accessToken;
    }

    public User getLoggedInUser() {
        return mLoggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        mLoggedInUser = loggedInUser;
    }
}

