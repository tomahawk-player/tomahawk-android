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

import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class HatchetAuthenticatorUtils extends AuthenticatorUtils {

    private static final String TAG = HatchetAuthenticatorUtils.class.getName();

    public static final String AUTH_SERVER = "https://auth.hatchet.is/v1/authentication/password";

    public static final String TOKEN_SERVER = "https://auth.hatchet.is/v1/tokens/fetch/bearer";

    public static final String PARAMS_GRANT_TYPE = "grant_type";

    public static final String PARAMS_GRANT_TYPE_PASSWORD = "password";

    public static final String PARAMS_USERNAME = "username";

    public static final String PARAMS_PASSWORD = "password";

    public static final String RESPONSE_ACCESS_TOKEN = "access_token";

    public static final String RESPONSE_CANONICAL_USERNAME = "canonical_username";

    public static final String RESPONSE_EXPIRES_IN = "expires_in";

    public static final String RESPONSE_REFRESH_TOKEN = "refresh_token";

    public static final String RESPONSE_REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in";

    public static final String RESPONSE_TOKEN_TYPE = "token_type";

    public static final String RESPONSE_ERROR = "error";

    public static final String RESPONSE_ERROR_INVALID_REQUEST = "invalid_request";

    public static final String RESPONSE_ERROR_INVALID_CLIENT = "invalid_client";

    public static final String RESPONSE_ERROR_INVALID_GRANT = "invalid_grant";

    public static final String RESPONSE_ERROR_UNAUTHORIZED_CLIENT = "unauthorized_client";

    public static final String RESPONSE_ERROR_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    public static final String RESPONSE_ERROR_INVALID_SCOPE = "invalid_scope";

    public static final String RESPONSE_ERROR_DESCRIPTION = "error_description";

    public static final String RESPONSE_ERROR_URI = "error_uri";

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
        public void onLoginFailed(final String error, final String errorDescription) {
            Log.d(TAG,
                    "TomahawkService: Hatchet login failed :(, Error: " + error + ", Description: "
                            + errorDescription);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mTomahawkApp,
                            TextUtils.isEmpty(errorDescription) ? error : errorDescription,
                            Toast.LENGTH_LONG).show();
                }
            });
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
            ((UserCollection) mTomahawkApp.getSourceList().getLocalSource().getCollection())
                    .updateHatchetUserPlaylists();
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
                params.put(PARAMS_GRANT_TYPE, PARAMS_GRANT_TYPE_PASSWORD);
                try {
                    String jsonString = TomahawkUtils.httpsPost(AUTH_SERVER, params);
                    JSONObject jsonObject = new JSONObject(jsonString);
                    if (jsonObject.has(RESPONSE_ERROR)) {
                        String error = jsonObject.getString(RESPONSE_ERROR);
                        String errorDescription = "";
                        if (jsonObject.has(RESPONSE_ERROR_DESCRIPTION)) {
                            errorDescription += jsonObject.getString(
                                    RESPONSE_ERROR_DESCRIPTION);
                        }
                        if (jsonObject.has(RESPONSE_ERROR_URI)) {
                            errorDescription += ", URI: " + jsonObject
                                    .getString(RESPONSE_ERROR_URI);
                        }
                        mAuthenticatorListener.onLoginFailed(error, errorDescription);
                    } else if (jsonObject.has(RESPONSE_ACCESS_TOKEN) && jsonObject.has(
                            RESPONSE_CANONICAL_USERNAME) && jsonObject.has(RESPONSE_EXPIRES_IN)
                            && jsonObject.has(RESPONSE_REFRESH_TOKEN) && jsonObject.has(
                            RESPONSE_REFRESH_TOKEN_EXPIRES_IN) && jsonObject.has(
                            RESPONSE_TOKEN_TYPE)) {
                        String username = jsonObject.getString(RESPONSE_CANONICAL_USERNAME);
                        String refreshtoken = jsonObject.getString(RESPONSE_REFRESH_TOKEN);
                        mAuthenticatorListener.onLogin(username);
                        mAuthenticatorListener.onAuthTokenProvided(username, refreshtoken);
                    } else {
                        mAuthenticatorListener.onLoginFailed("Unknown error", "");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage(), "");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage(), "");
                } catch (IOException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage(), "");
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage(), "");
                } catch (KeyManagementException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    mAuthenticatorListener.onLoginFailed(e.getMessage(), "");
                }
            }
        }).start();
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
}
