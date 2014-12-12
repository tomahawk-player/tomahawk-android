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

import org.tomahawk.libtomahawk.authentication.models.HatchetAuthResponse;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.JacksonConverter;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.Hatchet;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUserInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUsers;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;

import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class HatchetAuthenticatorUtils extends AuthenticatorUtils {

    private static final String TAG = HatchetAuthenticatorUtils.class.getSimpleName();

    public static final String STORED_USER_ID = "hatchetauthenticatorutils.stored_user_id";

    public static final String HATCHET_PRETTY_NAME = "Hatchet";

    public static final String ACCOUNT_TYPE = "is.hatchet.account";

    private static final String AUTH_TOKEN_HATCHET
            = "is.hatchet.account.authtoken";

    private static final String AUTH_TOKEN_EXPIRES_IN_HATCHET
            = "is.hatchet.account.authtokenexpiresin";

    private static final String MANDELLA_ACCESS_TOKEN_HATCHET
            = "is.hatchet.account.mandellaaccesstoken";

    private static final String MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET
            = "is.hatchet.account.mandellaaccesstokenexpiresin";

    private static final String CALUMET_ACCESS_TOKEN_HATCHET
            = "is.hatchet.account.calumetaccesstoken";

    private static final String CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET
            = "is.hatchet.account.calumetaccesstokenexpiresin";

    private static final String USER_ID_HATCHET
            = "is.hatchet.account.userid";

    private static final String HATCHET_AUTH_BASE_URL = "https://auth.hatchet.is/v1";

    private static final String PARAMS_GRANT_TYPE_PASSWORD = "password";

    private static final String PARAMS_GRANT_TYPE_REFRESHTOKEN = "refresh_token";

    private static final String RESPONSE_TOKEN_TYPE_BEARER = "bearer";

    private static final String RESPONSE_TOKEN_TYPE_CALUMET = "calumet";

    private static final String RESPONSE_ERROR_INVALID_REQUEST = "invalid_request";

    private static final int EXPIRING_LIMIT = 300;

    private HatchetAuth mHatchetAuth;

    private HashSet<String> mCorrespondingRequestIds = new HashSet<String>();

    private HatchetAuthUtilsReceiver mHatchetAuthUtilsReceiver = new HatchetAuthUtilsReceiver();

    /**
     * Handles incoming broadcasts.
     */
    private class HatchetAuthUtilsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String requestId = intent.getStringExtra(
                    InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
            if (mCorrespondingRequestIds.contains(requestId)) {
                InfoRequestData data = InfoSystem.getInstance().getInfoRequestById(requestId);
                if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
                    User user = data.getResult(User.class);
                    if (user != null) {
                        storeUserId(user.getId());
                    }
                }
            }
        }
    }

    public HatchetAuthenticatorUtils() {
        super(TomahawkApp.PLUGINNAME_HATCHET, HATCHET_PRETTY_NAME);

        mAllowRegistration = true;

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(HATCHET_AUTH_BASE_URL)
                .setConverter(new JacksonConverter(InfoSystemUtils.getObjectMapper()))
                .build();
        mHatchetAuth = restAdapter.create(HatchetAuth.class);

        TomahawkApp.getContext().registerReceiver(mHatchetAuthUtilsReceiver,
                new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
    }

    public void onLogin(String username, String refreshToken,
            long refreshTokenExpiresIn, String accessToken, long accessTokenExpiresIn) {
        Log.d(TAG,
                "Hatchet user '" + username + "' logged in successfully :)");
        if (username != null && !TextUtils.isEmpty(username) && refreshToken != null
                && !TextUtils.isEmpty(refreshToken)) {
            Log.d(TAG, "Hatchet auth token is served and yummy");
            Account account = new Account(username, ACCOUNT_TYPE);
            AccountManager am = AccountManager.get(TomahawkApp.getContext());
            if (am != null) {
                am.addAccountExplicitly(account, null, new Bundle());
                am.setAuthToken(account, AUTH_TOKEN_HATCHET, refreshToken);
                am.setUserData(account, AUTH_TOKEN_EXPIRES_IN_HATCHET,
                        String.valueOf(refreshTokenExpiresIn));
                am.setUserData(account, MANDELLA_ACCESS_TOKEN_HATCHET, accessToken);
                am.setUserData(account, MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                        String.valueOf(accessTokenExpiresIn));
                ensureAccessTokens();
            }
        }
<<<<<<< HEAD
        CollectionManager.getInstance().fetchPlaylists();
        CollectionManager.getInstance().fetchLovedItemsPlaylist();
        CollectionManager.getInstance().fetchStarredArtists();
        CollectionManager.getInstance().fetchStarredAlbums();
        InfoSystem.getInstance().resolve(getLoggedInUser());
=======
        CollectionManager.getInstance().fetchAll();
>>>>>>> upstream/master
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
    }

    public void onLoginFailed(int type, String message) {
        Log.d(TAG,
                "Hatchet login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    public void onLogout() {
        Log.d(TAG, "Hatchet user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_hatchet;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return R.string.login_username;
    }

    @Override
    public void register(final String name, final String password, final String email) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_AUTHENTICATING) {
                    @Override
                    public void run() {
                        try {
                            HatchetAuthResponse authResponse =
                                    mHatchetAuth.registerDirectly(name, password, email);
                            if (authResponse != null) {
                                onLogin(name,
                                        authResponse.refresh_token,
                                        authResponse.refresh_token_expires_in,
                                        authResponse.access_token,
                                        authResponse.expires_in);
                            } else {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR, "");
                            }
                        } catch (RetrofitError e) {
                            Log.d(TAG,
                                    "register: " + e.getClass() + ": " + e.getLocalizedMessage());
                            HatchetAuthResponse authResponse =
                                    (HatchetAuthResponse) e.getBodyAs(HatchetAuthResponse.class);
                            if (authResponse.error != null &&
                                    authResponse.error.equals(RESPONSE_ERROR_INVALID_REQUEST)) {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                        authResponse.error_description);
                            } else {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR, "");
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void login(Activity activity, final String name, final String password) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_AUTHENTICATING) {
                    @Override
                    public void run() {
                        try {
                            HatchetAuthResponse authResponse = mHatchetAuth
                                    .login(name, password, PARAMS_GRANT_TYPE_PASSWORD);
                            if (authResponse != null) {
                                onLogin(authResponse.canonical_username,
                                        authResponse.refresh_token,
                                        authResponse.refresh_token_expires_in,
                                        authResponse.access_token,
                                        authResponse.expires_in);
                            } else {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR, "");
                            }
                        } catch (RetrofitError e) {
                            Log.d(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                            HatchetAuthResponse authResponse =
                                    (HatchetAuthResponse) e.getBodyAs(HatchetAuthResponse.class);
                            if (authResponse.error != null &&
                                    authResponse.error.equals(RESPONSE_ERROR_INVALID_REQUEST)) {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_INVALIDCREDS,
                                        authResponse.error_description);
                            } else {
                                onLoginFailed(
                                        AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR, "");
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void logout(Activity activity) {
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null && getAccount() != null) {
            am.removeAccount(getAccount(), null, null);
        }
        onLogout();
    }

    public boolean isLoggedIn() {
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        return am != null && getAccount() != null
                && am.peekAuthToken(getAccount(), AUTH_TOKEN_HATCHET) != null;
    }

    public String getUserName() {
        if (getAccount() != null) {
            return getAccount().name;
        }
        return null;
    }

    private String getUserId() {
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null && getAccount() != null) {
            if (am.getUserData(getAccount(), USER_ID_HATCHET) != null) {
                return am.getUserData(getAccount(), USER_ID_HATCHET);
            } else {
<<<<<<< HEAD
                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setLogLevel(RestAdapter.LogLevel.BASIC)
                        .setEndpoint("https://api.hatchet.is/v1")
                        .setConverter(new JacksonConverter(InfoSystemUtils.getObjectMapper()))
                        .build();
                Hatchet hatchet = restAdapter.create(Hatchet.class);
                HatchetUsers users = hatchet.getUsers(null, getUserName(), null, null);
                if (users != null) {
                    HatchetUserInfo user = TomahawkUtils.carelessGet(users.users, 0);
                    if (user != null) {
                        am.setUserData(getAccount(), USER_ID_HATCHET, user.id);
                        return user.id;
                    }
                }
=======
                mCorrespondingRequestIds.add(InfoSystem.getInstance().resolveUserId(getUserName()));
>>>>>>> upstream/master
            }
        }
        return null;
    }

    public User getLoggedInUser() {
        if (getUserId() != null) {
            User user = User.get(getUserId());
            user.setName(getUserName());
            return user;
        }
        return null;
    }

    /**
     * Ensure that the calumet access token is available and valid. Get it from the cache if it
     * hasn't yet expired. Otherwise refetch and cache it again. Also refetches the mandella access
     * token if necessary.
     *
     * @return the calumet access token
     */
    public String ensureAccessTokens() {
        String refreshToken = null;
        String calumetAccessToken = null;
        String mandellaAccessToken = null;
        int mandellaExpirationTime = -1;
        int calumetExpirationTime = -1;
        int currentTime = (int) (System.currentTimeMillis() / 1000);

        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null && getAccount() != null) {
            mandellaAccessToken = am.getUserData(getAccount(), MANDELLA_ACCESS_TOKEN_HATCHET);
            String mandellaExpirationTimeString =
                    am.getUserData(getAccount(), MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET);
            if (mandellaExpirationTimeString != null) {
                mandellaExpirationTime = Integer.valueOf(mandellaExpirationTimeString);
            }
            calumetAccessToken = am.getUserData(getAccount(), CALUMET_ACCESS_TOKEN_HATCHET);
            String calumetExpirationTimeString =
                    am.getUserData(getAccount(), CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET);
            if (calumetExpirationTimeString != null) {
                calumetExpirationTime = Integer.valueOf(mandellaExpirationTimeString);
            }
            refreshToken = am.peekAuthToken(getAccount(), AUTH_TOKEN_HATCHET);
        }
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
            HatchetAuthResponse authResponse;
            if (tokenType.equals(RESPONSE_TOKEN_TYPE_BEARER)) {
                authResponse = mHatchetAuth.getBearerAccessToken(token,
                        PARAMS_GRANT_TYPE_REFRESHTOKEN);
            } else {
                authResponse = mHatchetAuth.getAccessToken(RESPONSE_TOKEN_TYPE_BEARER + " " + token,
                        tokenType);
            }
            AccountManager am = AccountManager.get(TomahawkApp.getContext());
            if (am != null && getAccount() != null && authResponse.access_token != null) {
                int currentTime = (int) (System.currentTimeMillis() / 1000);
                long expirationTime = currentTime + authResponse.expires_in;
                accessToken = authResponse.access_token;
                if (TomahawkUtils.containsIgnoreCase(tokenType, RESPONSE_TOKEN_TYPE_BEARER)) {
                    am.setUserData(getAccount(), MANDELLA_ACCESS_TOKEN_HATCHET, accessToken);
                    am.setUserData(getAccount(), MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                            String.valueOf(expirationTime));
                } else {
                    am.setUserData(getAccount(), CALUMET_ACCESS_TOKEN_HATCHET, accessToken);
                    am.setUserData(getAccount(), CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET,
                            String.valueOf(expirationTime));
                }
                Log.d(TAG, "Access token fetched, current time: '" + currentTime +
                        "', expiration time: '" + expirationTime + "'");
            } else {
                onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                        "Couldn't fetch access token");
            }
        } catch (RetrofitError e) {
            Log.e(TAG, "fetchAccessToken: " + e.getClass() + ": " + e.getLocalizedMessage());
            HatchetAuthResponse authResponse =
                    (HatchetAuthResponse) e.getBodyAs(HatchetAuthResponse.class);
            if (authResponse != null && (authResponse.error != null
                    || !TomahawkUtils.containsIgnoreCase(tokenType, authResponse.token_type))) {
                logout();
                onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                        "Please reenter your Hatchet credentials");
            } else {
                onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_COMMERROR, "");
            }
        }
        return accessToken;
    }

    /**
     * Get the Hatchet account from the AccountManager
     *
     * @return the account object or null if none could be found
     */
    public static Account getAccount() {
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
            if (accounts != null && accounts.length > 0) {
                return accounts[0];
            }
        }
        return null;
    }

    public static void storeUserId(String userId) {
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        am.setUserData(getAccount(), USER_ID_HATCHET, userId);
        TomahawkApp.getContext().sendBroadcast(new Intent(STORED_USER_ID));
    }
}

