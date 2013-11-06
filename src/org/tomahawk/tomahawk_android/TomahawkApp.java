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
package org.tomahawk.tomahawk_android;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Source;
import org.tomahawk.libtomahawk.collection.SourceList;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.resolver.DataBaseResolver;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.TomahawkExceptionReporter;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class contains represents the Application core.
 */
@ReportsCrashes(formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class TomahawkApp extends Application {

    private static final String TAG = TomahawkApp.class.getName();

    public static final int RESOLVER_ID_USERCOLLECTION = 0;

    public static final int RESOLVER_ID_JAMENDO = 100;

    public static final int RESOLVER_ID_OFFICIALFM = 101;

    public static final int RESOLVER_ID_EXFM = 102;

    public static final int RESOLVER_ID_SOUNDCLOUD = 103;

    public static final int RESOLVER_ID_SPOTIFY = 200;

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(
            Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private static Context sApplicationContext;

    private AccountManager mAccountManager = null;

    private SourceList mSourceList;

    private PipeLine mPipeLine;

    private InfoSystem mInfoSystem;

    private long mTrackIdCounter;

    private long mAlbumIdCounter;

    private long mArtistIdCounter;

    private long mQueryIdCounter;

    private long mInfoRequestIdCounter;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED)) {
                onCollectionUpdated();
            }
        }
    }

    @Override
    public void onCreate() {
        TomahawkExceptionReporter.init(this);
        super.onCreate();
        sApplicationContext = getApplicationContext();

        mSourceList = new SourceList();
        mPipeLine = new PipeLine(this);
        mInfoSystem = new InfoSystem(this);
        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, sCollectionUpdateIntentFilter);
        }
        ScriptResolver scriptResolver = new ScriptResolver(RESOLVER_ID_JAMENDO, this,
                "js/jamendo/jamendo-resolver.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_OFFICIALFM, this,
                "js/official.fm/officialfm.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_EXFM, this, "js/exfm/exfm.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_SOUNDCLOUD, this,
                "js/soundcloud/soundcloud.js");
        mPipeLine.addResolver(scriptResolver);
        SpotifyResolver spotifyResolver = new SpotifyResolver(RESOLVER_ID_SPOTIFY, this);
        mPipeLine.addResolver(spotifyResolver);

        initialize();
    }

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        mPipeLine.addResolver(new DataBaseResolver(RESOLVER_ID_USERCOLLECTION, this,
                mSourceList.getLocalSource().getCollection()));
    }

    /**
     * Initialize the Tomahawk app.
     */
    public void initialize() {
        initLocalCollection();
    }

    /**
     * Initializes a new Collection of all local tracks.
     */
    public void initLocalCollection() {
        Log.d(TAG, "Initializing Local Collection.");
        Source src = new Source(new UserCollection(this), 0, "My Collection");
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

    public PipeLine getPipeLine() {
        return mPipeLine;
    }

    public InfoSystem getInfoSystem() {
        return mInfoSystem;
    }

    /**
     * Returns the context for the application
     */
    public static Context getContext() {
        return sApplicationContext;
    }

    public long getUniqueTrackId() {
        return mTrackIdCounter++;
    }

    public long getUniqueAlbumId() {
        return mAlbumIdCounter++;
    }

    public long getUniqueArtistId() {
        return mArtistIdCounter++;
    }

    public String getUniqueQueryId() {
        return String.valueOf(mQueryIdCounter++);
    }

    public String getUniqueInfoRequestId() {
        return String.valueOf(mInfoRequestIdCounter++);
    }
}
