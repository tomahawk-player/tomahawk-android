/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.annotation.ReportsCrashes;
import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.Source;
import org.tomahawk.libtomahawk.SourceList;
import org.tomahawk.libtomahawk.account.AccountManager;

import android.app.Application;
import android.util.Log;


/**
 * This class contains represents the Application core.
 */
@ReportsCrashes(formKey = "")
public class TomahawkApp extends Application {

    private static final String TAG = TomahawkApp.class.getName();

    private AccountManager mAccountManager = null;
    private SourceList mSourceList;

    @Override
    public void onCreate() {
        ACRA.init(this);
        TomahawkExceptionReporter reporter = new TomahawkExceptionReporter();
        ErrorReporter.getInstance().setReportSender(reporter);
        super.onCreate();

        mAccountManager = new AccountManager();
        mSourceList = new SourceList();

        initialize();
    }

    /**
     * Initialize the Tomahawk app.
     */
    public void initialize() {
        initAccounts();
        initLocalCollection();
    }

    /**
     * Initialize a new Tomahawk servant.
     */
    public void initAccounts() {
        mAccountManager.initAccounts();
    }

    /**
     * Initializes a new Collection of all local tracks.
     */
    public void initLocalCollection() {
        Log.d(TAG, "Initializing Local Collection.");
        Source src = new Source(new LocalCollection(getContentResolver()), 0,
                "My Collection");
        mSourceList.setLocalSource(src);
    }

    /**
     * Returns the Tomahawk AccountManager;
     */
    public AccountManager getAccountManager() {
        return mAccountManager;
    }

    /**
     * Return the list of Sources for this TomahawkApp.
     * 
     * @return SourceList
     */
    public SourceList getSourceList() {
        return mSourceList;
    }
}

