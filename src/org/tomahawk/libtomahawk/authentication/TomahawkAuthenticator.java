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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class TomahawkAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = TomahawkAuthenticator.class.getSimpleName();

    private final Context mContext;

    /**
     * Service which handles authentication requests.
     */
    public static class HatchetAuthenticationService extends Service {

        private TomahawkAuthenticator mTomahawkAuthenticator;

        @Override
        public void onCreate() {
            mTomahawkAuthenticator = new TomahawkAuthenticator(getApplicationContext());
        }

        @Override
        public IBinder onBind(Intent intent) {
            return mTomahawkAuthenticator.getIBinder();
        }
    }

    public TomahawkAuthenticator(Context context) {
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
        //final Intent intent = new Intent(mContext, TomahawkMainActivity.class);
        //intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        //intent.putExtra(TomahawkService.AUTHENTICATOR_ID, TomahawkService.AUTHENTICATOR_ID_HATCHET);
        //bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return new Bundle();
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
        if (authToken != null && authToken.length() > 0) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE,
                    HatchetAuthenticatorUtils.ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        /*final Intent intent = new Intent(mContext, TomahawkMainActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(TomahawkService.AUTHENTICATOR_ID, TomahawkService.AUTHENTICATOR_ID_HATCHET);
        intent.putExtra(PARAMS_USERNAME, account.name);
        intent.putExtra(PARAMS_TYPE, authTokenType);*/

        //bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return new Bundle();
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
}
