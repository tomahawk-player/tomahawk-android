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
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.DataBaseResolver;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.ContentViewer;
import org.tomahawk.tomahawk_android.utils.TomahawkExceptionReporter;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
public class TomahawkApp extends Application implements
        TomahawkService.TomahawkServiceConnection.TomahawkServiceConnectionListener {

    private static final String TAG = TomahawkApp.class.getName();

    public static final String TOMAHAWKSERVICE_READY
            = "org.tomahawk.tomahawk_android.tomahawkservice_ready";

    public static final int RESOLVER_ID_USERCOLLECTION = 0;

    public static final int RESOLVER_ID_JAMENDO = 100;

    public static final int RESOLVER_ID_OFFICIALFM = 101;

    public static final int RESOLVER_ID_EXFM = 102;

    public static final int RESOLVER_ID_SOUNDCLOUD = 103;

    public static final int RESOLVER_ID_SPOTIFY = 200;

    public static final String ID_COUNTER = "org.tomahawk.tomahawk_android.id_counter";

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(
            Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private static Context sApplicationContext;

    private SourceList mSourceList;

    private PipeLine mPipeLine;

    private InfoSystem mInfoSystem;

    private UserPlaylistsDataSource mUserPlaylistsDataSource;

    private ContentViewer mContentViewer;

    private TomahawkService.TomahawkServiceConnection mTomahawkServiceConnection
            = new TomahawkService.TomahawkServiceConnection(this);

    private TomahawkService mTomahawkService;

    private static long mSessionIdCounter = 0;

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
            if (Collection.COLLECTION_UPDATED.equals(intent.getAction())) {
                onCollectionUpdated();
                mPipeLine.onCollectionUpdated();
            }
        }
    }

    @Override
    public void onCreate() {
        TomahawkExceptionReporter.init(this);
        super.onCreate();

        // Load the LibSpotifyWrapper libaries
        System.loadLibrary("spotify");
        System.loadLibrary("spotifywrapper");

        // Initialize LibSpotifyWrapper
        LibSpotifyWrapper
                .init(LibSpotifyWrapper.class.getClassLoader(), getFilesDir() + "/Spotify");

        sApplicationContext = getApplicationContext();

        mSourceList = new SourceList();
        mPipeLine = new PipeLine(this);
        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, sCollectionUpdateIntentFilter);
        }
        mPipeLine.addResolver(new DataBaseResolver(RESOLVER_ID_USERCOLLECTION, this));
        ScriptResolver scriptResolver = new ScriptResolver(RESOLVER_ID_JAMENDO, this,
                "js/jamendo/content/contents/code/jamendo.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_OFFICIALFM, this,
                "js/official.fm/content/contents/code/officialfm.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_EXFM, this,
                "js/exfm/content/contents/code/exfm.js");
        mPipeLine.addResolver(scriptResolver);
        scriptResolver = new ScriptResolver(RESOLVER_ID_SOUNDCLOUD, this,
                "js/soundcloud/content/contents/code/soundcloud.js");
        mPipeLine.addResolver(scriptResolver);
        SpotifyResolver spotifyResolver = new SpotifyResolver(RESOLVER_ID_SPOTIFY, this);
        mPipeLine.addResolver(spotifyResolver);
        mPipeLine.setAllResolversAdded(true);
        mInfoSystem = new InfoSystem(this);
        mInfoSystem.addInfoPlugin(new HatchetInfoPlugin(this));
        Intent intent = new Intent(this, TomahawkService.class);
        startService(intent);
        bindService(intent, mTomahawkServiceConnection, Context.BIND_AUTO_CREATE);

        // Initialize UserPlaylistsDataSource, which makes it possible to retrieve persisted
        // UserPlaylists
        mUserPlaylistsDataSource = new UserPlaylistsDataSource(this);
        mUserPlaylistsDataSource.open();

        initialize();
    }

    @Override
    public void onLowMemory() {
        LibSpotifyWrapper.pause();
        LibSpotifyWrapper.destroy();
    }

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        ((DataBaseResolver) mPipeLine.getResolver(RESOLVER_ID_USERCOLLECTION))
                .setCollection(mSourceList.getLocalSource().getCollection());
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

    public UserPlaylistsDataSource getUserPlaylistsDataSource() {
        return mUserPlaylistsDataSource;
    }

    public ContentViewer getContentViewer() {
        return mContentViewer;
    }

    public void setContentViewer(ContentViewer contentViewer) {
        mContentViewer = contentViewer;
    }

    /**
     * Returns the context for the application
     */
    public static Context getContext() {
        return sApplicationContext;
    }

    @Override
    public void setTomahawkService(TomahawkService ps) {
        mTomahawkService = ps;
    }

    /**
     * If the TomahawkService signals, that it is ready, this method is being called
     */
    @Override
    public void onTomahawkServiceReady() {
        sendBroadcast(new Intent(TOMAHAWKSERVICE_READY));
    }

    public TomahawkService getTomahawkService() {
        return mTomahawkService;
    }

    public static long getSessionUniqueId() {
        return mSessionIdCounter++;
    }

    public static String getSessionUniqueStringId() {
        return String.valueOf(getSessionUniqueId());
    }

    public static long getLifetimeUniqueId() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        long id = sharedPreferences.getLong(ID_COUNTER, 0);
        sharedPreferences.edit().putLong(ID_COUNTER, id + 1).commit();
        return id;
    }

    public static String getLifetimeUniqueStringId() {
        return String.valueOf(getLifetimeUniqueId());
    }
}
