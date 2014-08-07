/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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

import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.accounts.Account;
import android.app.Activity;

public abstract class AuthenticatorUtils {

    public static final String ACCOUNT_NAME
            = "org.tomahawk.tomahawk_android.authenticator_name";

    public static final String ACCOUNT_NAME_PREFIX
            = "org.tomahawk.tomahawk_android.authenticator_name_";

    public static final String PACKAGE_PREFIX = "org.tomahawk.";

    public static final String AUTH_TOKEN_SUFFIX = ".authtoken";

    public static final String ACCESS_TOKEN_SUFFIX = ".accesstoken";

    public static final String ACCESS_TOKEN_EXPIRES_IN_SUFFIX = ".accesstokenexpiresin";

    public static final String AUTH_TOKEN_EXPIRES_IN_HATCHET
            = "org.tomahawk.hatchet.authtokenexpiresin";

    public static final String MANDELLA_ACCESS_TOKEN_HATCHET
            = "org.tomahawk.hatchet.mandellaaccesstoken";

    public static final String MANDELLA_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET
            = "org.tomahawk.hatchet.mandellaaccesstokenexpiresin";

    public static final String CALUMET_ACCESS_TOKEN_HATCHET
            = "org.tomahawk.hatchet.calumetaccesstoken";

    public static final String CALUMET_ACCESS_TOKEN_EXPIRATIONTIME_HATCHET
            = "org.tomahawk.hatchet.calumetaccesstokenexpiresin";

    private String mPrettyName;

    private String mId;

    protected boolean mAllowRegistration;

    protected AuthenticatorUtils(String id, String prettyName) {
        mId = id;
        mPrettyName = prettyName;
    }

    public String getPrettyName() {
        return mPrettyName;
    }

    public String getId() {
        return mId;
    }

    public abstract int getIconResourceId();

    public String getAccountName() {
        return ACCOUNT_NAME_PREFIX + getId();
    }

    public String getAuthTokenName() {
        return PACKAGE_PREFIX + getId() + AUTH_TOKEN_SUFFIX;
    }

    public String getAccessTokenName() {
        return PACKAGE_PREFIX + getId() + ACCESS_TOKEN_SUFFIX;
    }

    public String getAccessTokenExpiresInName() {
        return PACKAGE_PREFIX + getId() + ACCESS_TOKEN_EXPIRES_IN_SUFFIX;
    }

    public abstract int getUserIdEditTextHintResId();

    public abstract void onInit();

    public abstract void onLogin(String username, String refreshToken,
            long refreshTokenExpiresIn, String accessToken, long accessTokenExpiresIn);

    public abstract void onLoginFailed(int type, String message);

    public abstract void onLogout();

    public abstract void register(String name, String password, String email);

    public abstract void login(Activity activity, String email, String password);

    public abstract void logout(Activity activity);

    public void login(String email, String password) {
        login(null, email, password);
    }

    public void logout() {
        logout(null);
    }

    public boolean isLoggedIn() {
        return TomahawkUtils.peekAuthTokenForAccount(getAccountName(), getAuthTokenName()) != null;
    }

    public String getUserName() {
        Account account = TomahawkUtils.getAccountByName(getAccountName());
        if (account != null) {
            return account.name;
        }
        return null;
    }

    public boolean doesAllowRegistration() {
        return mAllowRegistration;
    }
}
