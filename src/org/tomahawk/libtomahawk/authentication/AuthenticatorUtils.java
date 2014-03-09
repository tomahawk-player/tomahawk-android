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
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.content.Context;

public abstract class AuthenticatorUtils {

    protected TomahawkApp mTomahawkApp;

    protected TomahawkService mTomahawkService;

    protected boolean mIsAuthenticating;

    public abstract int getTitleResourceId();

    public abstract int getIconResourceId();

    public abstract String getAuthenticatorUtilsName();

    public abstract String getAuthenticatorUtilsTokenType();

    public abstract int getUserIdEditTextHintResId();

    public abstract void login(String email, String password);

    public abstract void logout();

    public static String getUserName(Context context, String authenticatorName) {
        Account account = TomahawkUtils.getAccountByName(context, authenticatorName);
        if (account != null) {
            return account.name;
        }
        return null;
    }

    public static boolean isLoggedIn(Context context, String authenticatorName,
            String authTokenType) {
        if (authenticatorName.equals(TomahawkService.AUTHENTICATOR_NAME_SPOTIFY)) {
            return SpotifyAuthenticatorUtils.isSpotifyLoggedIn();
        } else {
            return TomahawkUtils.peekAuthTokenForAccount(context, authenticatorName, authTokenType)
                    != null;
        }
    }

    public boolean isAuthenticating() {
        return mIsAuthenticating;
    }
}
