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
import android.content.Context;

public abstract class AuthenticatorUtils {

    public static final String AUTHENTICATOR_NAME
            = "org.tomahawk.tomahawk_android.authenticator_name";

    public static final String AUTHENTICATOR_NAME_SPOTIFY
            = "org.tomahawk.tomahawk_android.authenticator_name_spotify";

    public static final String AUTHENTICATOR_NAME_HATCHET
            = "org.tomahawk.tomahawk_android.authenticator_name_hatchet";

    public static final String AUTH_TOKEN_TYPE_SPOTIFY = "org.tomahawk.spotify.authtoken";

    public static final String AUTH_TOKEN_TYPE_HATCHET = "org.tomahawk.hatchet.authtoken";

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

    protected Context mContext;

    protected boolean mIsAuthenticating;

    public abstract int getTitleResourceId();

    public abstract int getIconResourceId();

    public abstract String getAuthenticatorUtilsName();

    public abstract String getAuthenticatorUtilsTokenType();

    public abstract int getUserIdEditTextHintResId();

    public abstract void onInit();

    public abstract void onLogin(String username);

    public abstract void onLoginFailed(final String message);

    public abstract void onLogout();

    public abstract void onAuthTokenProvided(String username, String refreshToken,
            int refreshTokenExpiresIn, String accessToken, int accessTokenExpiresIn);

    public abstract void login(String email, String password);

    public abstract void logout();

    public boolean isLoggedIn() {
        return TomahawkUtils.peekAuthTokenForAccount(mContext, getAuthenticatorUtilsName(),
                getAuthenticatorUtilsTokenType()) != null;
    }

    public boolean isAuthenticating() {
        return mIsAuthenticating;
    }

    public String getUserName() {
        Account account = TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName());
        if (account != null) {
            return account.name;
        }
        return null;
    }
}
