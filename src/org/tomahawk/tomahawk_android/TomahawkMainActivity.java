/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.network.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;


/**
 * This class represents the main Activity for the app.
 */
public class TomahawkMainActivity extends SherlockFragmentActivity {

    private static final String TAG = TomahawkMainActivity.class.getName();

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.tomahawk_main_activity, null);
        setContentView(view);

        /** Setup account. */
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(TomahawkService.ACCOUNT_TYPE);

        if (accounts.length <= 0)
            startActivity(new Intent(this, TomahawkAccountAuthenticatorActivity.class));
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        /** Setup account. */
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(TomahawkService.ACCOUNT_TYPE);

        if (accounts.length <= 0)
            return;

        /**
         * 'Getting' the auth token here is asynchronous. When the
         * AccountManager has the auth token the TomahawkMainActivity.run is
         * called and starts the TomahawkServerConnection.
         */
        // if (TomahawkPreferences.goOnline())
            accountManager.getAuthToken(accounts[0], TomahawkService.AUTH_TOKEN_TYPE, null,
                    new TomahawkAccountAuthenticatorActivity(), (TomahawkApp) getApplication(), null);
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        //<TEMPORARY>
        finish();
        //<TEMPORARY/>
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
	}

    public void onCollectionClicked(View view) {

        Intent i = new Intent(this, CollectionActivity.class);
        i.putExtra(CollectionActivity.COLLECTION_ID_EXTRA,
                ((TomahawkApp) getApplication()).getSourceList().getLocalSource().getCollection().getId());
        startActivity(i);
    }
}
