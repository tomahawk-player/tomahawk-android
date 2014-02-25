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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import java.util.Map;

public abstract class AuthenticatorUtils {

    protected TomahawkApp mTomahawkApp;

    protected TomahawkService mTomahawkService;

    protected Map<String, AccessToken> mAccessTokens;

    protected String mName;

    protected boolean mIsAuthenticating;

    public abstract int getTitleResourceId();

    public abstract int getIconResourceId();

    public abstract void login(String email, String password);

    public abstract void logout();

    public static String getUserId(Context context, String authenticatorName) {
        final AccountManager am = AccountManager.get(context);
        if (am != null) {
            Account[] accounts = am
                    .getAccountsByType(context.getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (authenticatorName
                            .equals(am.getUserData(account, TomahawkService.AUTHENTICATOR_NAME))) {
                        return account.name;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isLoggedIn(Context context, String authenticatorName,
            String authTokenType) {
        if (authenticatorName.equals(TomahawkService.AUTHENTICATOR_NAME_SPOTIFY)) {
            return SpotifyAuthenticatorUtils.isSpotifyLoggedIn();
        } else {
            final AccountManager am = AccountManager.get(context);
            if (am != null) {
                Account[] accounts = am
                        .getAccountsByType(context.getString(R.string.accounttype_string));
                if (accounts != null) {
                    for (Account account : accounts) {
                        if (authenticatorName
                                .equals(am.getUserData(account,
                                        TomahawkService.AUTHENTICATOR_NAME))) {
                            return am.peekAuthToken(account, authTokenType) != null;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isAuthenticating() {
        return mIsAuthenticating;
    }
}
