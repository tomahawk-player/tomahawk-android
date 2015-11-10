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
package org.tomahawk.tomahawk_android.activities;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.DbCollection;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.database.TomahawkSQLiteHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.UserCollectionStubResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverUrlResult;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.libtomahawk.utils.parser.XspfParser;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.SuggestionSimpleCursorAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.dialogs.AskAccessConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.GMusicConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.InstallPluginConfigDialog;
import org.tomahawk.tomahawk_android.fragments.ArtistPagerFragment;
import org.tomahawk.tomahawk_android.fragments.CollectionPagerFragment;
import org.tomahawk.tomahawk_android.fragments.ContentHeaderFragment;
import org.tomahawk.tomahawk_android.fragments.ContextMenuFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.fragments.PreferenceAdvancedFragment;
import org.tomahawk.tomahawk_android.fragments.PreferencePagerFragment;
import org.tomahawk.tomahawk_android.fragments.SearchPagerFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.UserPagerFragment;
import org.tomahawk.tomahawk_android.fragments.WelcomeFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.SearchViewStyle;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * The main Tomahawk activity
 */
public class TomahawkMainActivity extends ActionBarActivity
        implements PlaybackServiceConnectionListener, SlidingUpPanelLayout.PanelSlideListener {

    private final static String TAG = TomahawkMainActivity.class.getSimpleName();

    public static final String HUB_ID_USERPAGE = "userpage";

    public static final String HUB_ID_FEED = "feed";

    public static final String HUB_ID_COLLECTION = "collection";

    public static final String HUB_ID_LOVEDTRACKS = "lovedtracks";

    public static final String HUB_ID_PLAYLISTS = "playlists";

    public static final String HUB_ID_SETTINGS = "settings";

    public static final String SAVED_STATE_ACTION_BAR_HIDDEN = "saved_state_action_bar_hidden";

    public static final String SHOW_PLAYBACKFRAGMENT_ON_STARTUP
            = "show_playbackfragment_on_startup";

    public static final String COACHMARK_SEEK_DISABLED = "coachmark_seek_disabled";

    public static final String COACHMARK_SEEK_TIMESTAMP = "coachmark_seek_timestamp";

    public static final String COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED
            = "coachmark_playbackfragment_navigation_disabled";

    public static final String COACHMARK_WELCOMEFRAGMENT_DISABLED
            = "coachmark_welcomefragment_disabled";

    public static final String COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED
            = "coachmark_swipelayout_enqueue_disabled";

    public static int ACTIONBAR_HEIGHT;

    public static class SlidingLayoutChangedEvent {

        public SlidingUpPanelLayout.PanelState mSlideState;

    }

    private float mSlidingOffset = -1f;

    private static long mSessionIdCounter = 0;

    protected final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    private final PlaybackServiceConnection mPlaybackServiceConnection
            = new PlaybackServiceConnection(
            this);

    private PlaybackService mPlaybackService;

    private MenuItem mSearchItem;

    private DrawerLayout mDrawerLayout;

    private StickyListHeadersListView mDrawerList;

    private TomahawkMenuAdapter mTomahawkMenuAdapter;

    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mTitle;

    private CharSequence mDrawerTitle;

    private TomahawkMainReceiver mTomahawkMainReceiver;

    private SmoothProgressBar mSmoothProgressBar;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private SlidingUpPanelLayout.PanelState mLastSlidingState;

    private PlaybackPanel mPlaybackPanel;

    private View mActionBarBg;

    private Bundle mSavedInstanceState;

    private boolean mRootViewsInitialized;

    private Runnable mRunAfterInit;

    private Map<Collection, Boolean> mCollectionLoadingMap = new HashMap<>();

    private Handler mShouldShowAnimationHandler;

    private final Runnable mShouldShowAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (ThreadManager.get().isActive()
                    || (mPlaybackService != null && mPlaybackService.isPreparing())
                    || ((UserCollection) CollectionManager.get()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION)).isWorking()) {
                mSmoothProgressBar.setVisibility(View.VISIBLE);
            } else {
                mSmoothProgressBar.setVisibility(View.GONE);
            }
            mShouldShowAnimationHandler.postDelayed(mShouldShowAnimationRunnable, 500);
            for (final Collection collection : CollectionManager.get().getCollections()) {
                if (collection instanceof DbCollection) {
                    ((DbCollection) collection).isInitializing().then(new DoneCallback<Boolean>() {
                        @Override
                        public void onDone(Boolean result) {
                            Boolean lastResult = mCollectionLoadingMap.get(collection);
                            mCollectionLoadingMap.put(collection, result);
                            if (lastResult == null || lastResult != result) {
                                updateDrawer();
                            }
                        }
                    });
                }
            }
        }
    };

    public static class ShowWebViewEvent {

        public String mUrl;
    }

    private ProgressHandler mProgressHandler;

    private static class ProgressHandler extends WeakReferenceHandler<TomahawkMainActivity> {

        public ProgressHandler(TomahawkMainActivity referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            TomahawkMainActivity activity = getReferencedObject();
            if (activity != null && activity.mPlaybackService != null
                    && activity.mPlaybackService.getCurrentTrack() != null) {
                PlaybackService.PlayPositionChangedEvent event
                        = new PlaybackService.PlayPositionChangedEvent();
                event.currentPosition = activity.mPlaybackService.getPosition();
                event.duration = activity.mPlaybackService.getCurrentTrack().getDuration();
                if (activity.mPlaybackPanel != null) {
                    activity.mPlaybackPanel.onPlayPositionChanged(event.duration,
                            event.currentPosition);
                }
                EventBus.getDefault().post(event);
                sendEmptyMessageDelayed(0, 500);
            }
        }
    }

    /**
     * Handles incoming broadcasts.
     */
    private class TomahawkMainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean noConnectivity =
                        intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                    InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
                }
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        /**
         * Called every time an item inside the {@link android.widget.ListView} is clicked
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this will be a view
         *                 provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HatchetAuthenticatorUtils authenticatorUtils
                    = (HatchetAuthenticatorUtils) AuthenticatorManager.get()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            TomahawkMenuAdapter.ResourceHolder holder =
                    (TomahawkMenuAdapter.ResourceHolder) mDrawerList.getAdapter().getItem(position);
            final Bundle bundle = new Bundle();
            if (holder.collection != null) {
                bundle.putString(TomahawkFragment.COLLECTION_ID, holder.collection.getId());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, CollectionPagerFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_USERPAGE)) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        bundle.putString(TomahawkFragment.USER, user.getId());
                        bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                ContentHeaderFragment.MODE_HEADER_STATIC_USER);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                FragmentUtils.replace(TomahawkMainActivity.this,
                                        UserPagerFragment.class, bundle);
                            }
                        });
                    }
                });
            } else if (holder.id.equals(HUB_ID_FEED)) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        bundle.putString(TomahawkFragment.USER, user.getId());
                        bundle.putInt(TomahawkFragment.SHOW_MODE,
                                SocialActionsFragment.SHOW_MODE_DASHBOARD);
                        bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                FragmentUtils.replace(TomahawkMainActivity.this,
                                        SocialActionsFragment.class, bundle);
                            }
                        });
                    }
                });
            } else if (holder.id.equals(HUB_ID_COLLECTION)) {
                bundle.putString(TomahawkFragment.COLLECTION_ID,
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, CollectionPagerFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_LOVEDTRACKS)) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        bundle.putInt(TomahawkFragment.SHOW_MODE,
                                PlaylistEntriesFragment.SHOW_MODE_LOVEDITEMS);
                        bundle.putString(TomahawkFragment.USER, user.getId());
                        bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                FragmentUtils.replace(TomahawkMainActivity.this,
                                        PlaylistEntriesFragment.class, bundle);
                            }
                        });
                    }
                });
            } else if (holder.id.equals(HUB_ID_PLAYLISTS)) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        bundle.putString(TomahawkFragment.USER, user.getId());
                        bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                ContentHeaderFragment.MODE_HEADER_STATIC);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                FragmentUtils.replace(TomahawkMainActivity.this,
                                        PlaylistsFragment.class, bundle);
                            }
                        });
                    }
                });
            } else if (holder.id.equals(HUB_ID_SETTINGS)) {
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC_SMALL);
                FragmentUtils.replace(TomahawkMainActivity.this, PreferencePagerFragment.class,
                        bundle);
            }
            if (mDrawerLayout != null) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }
    }

    /**
     * If the {@link PipeLine} was able to parse a given url (like a link to a spotify track for
     * example), then this method receives the result object.
     *
     * @param event the result object which contains the parsed data
     */
    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.UrlResultsEvent event) {
        final Bundle bundle = new Bundle();
        List<Query> queries;
        Query query;
        Playlist playlist;
        switch (event.mResult.type) {
            case PipeLine.URL_TYPE_ARTIST:
                bundle.putString(TomahawkFragment.ARTIST,
                        Artist.get(event.mResult.artist).getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                bundle.putLong(TomahawkFragment.CONTAINER_FRAGMENT_ID,
                        TomahawkMainActivity.getSessionUniqueId());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentUtils.replace(TomahawkMainActivity.this, ArtistPagerFragment.class,
                                bundle);
                    }
                });
                break;
            case PipeLine.URL_TYPE_ALBUM:
                Artist artist = Artist.get(event.mResult.artist);
                bundle.putString(TomahawkFragment.ALBUM,
                        Album.get(event.mResult.album, artist).getCacheKey());
                bundle.putString(
                        TomahawkFragment.COLLECTION_ID, TomahawkApp.PLUGINNAME_HATCHET);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentUtils.replace(
                                TomahawkMainActivity.this, PlaylistEntriesFragment.class, bundle);
                    }
                });
                break;
            case PipeLine.URL_TYPE_TRACK:
                queries = new ArrayList<>();
                query = Query.get(event.mResult.track, "", event.mResult.artist, false);
                queries.add(query);
                playlist =
                        Playlist.fromQueryList(getSessionUniqueStringId(), false, "", "", queries);
                playlist.setFilled(true);
                bundle.putString(TomahawkFragment.PLAYLIST, playlist.getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentUtils.replace(
                                TomahawkMainActivity.this, PlaylistEntriesFragment.class, bundle);
                    }
                });
                break;
            case PipeLine.URL_TYPE_PLAYLIST:
                queries = new ArrayList<>();
                for (ScriptResolverUrlResult track : event.mResult.tracks) {
                    query = Query.get(track.track, "", track.artist, false);
                    if (event.mResolver != null && event.mResolver.isEnabled()
                            && track.hint != null) {
                        Result result = Result.get(track.hint, query.getBasicTrack(),
                                event.mResolver);
                        float trackScore = query.howSimilar(result);
                        query.addTrackResult(result, trackScore);
                    }
                    queries.add(query);
                }
                playlist = Playlist.fromQueryList(TomahawkMainActivity.getLifetimeUniqueStringId(),
                        false, event.mResult.title, null, queries);
                playlist.setFilled(true);
                bundle.putString(TomahawkFragment.PLAYLIST, playlist.getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentUtils.replace(TomahawkMainActivity.this,
                                PlaylistEntriesFragment.class, bundle);
                    }
                });
                break;
            case PipeLine.URL_TYPE_XSPFURL:
                Playlist pl = XspfParser.parse(event.mResult.url);
                if (pl != null) {
                    bundle.putString(TomahawkFragment.PLAYLIST, pl.getCacheKey());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(TomahawkMainActivity.this,
                                    PlaylistEntriesFragment.class, bundle);
                        }
                    });
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            if (event.mInfoRequestData != null
                    && event.mInfoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
                updateDrawer();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayingTrackChangedEvent event) {
        mPlaybackPanel.update(mPlaybackService);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayStateChangedEvent event) {
        if (mPlaybackService != null && mPlaybackService.getCurrentTrack() != null
                && mPlaybackService.isPlaying()) {
            if (mProgressHandler != null) {
                mProgressHandler.sendEmptyMessage(0);
            }
            mPlaybackPanel.updatePlayPauseState(true);
        } else {
            if (mProgressHandler != null) {
                mProgressHandler.removeCallbacksAndMessages(null);
            }
            mPlaybackPanel.updatePlayPauseState(false);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.AddedEvent event) {
        updateDrawer();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(HatchetAuthenticatorUtils.UserLoginEvent event) {
        updateDrawer();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(AuthenticatorManager.ConfigTestResultEvent event) {
        if (event.mComponent instanceof HatchetAuthenticatorUtils
                && (event.mType == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS
                || event.mType == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT)) {
            onHatchetLoggedInOut(
                    event.mType == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ShowWebViewEvent event) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.URL_EXTRA, event.mUrl);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        GMusicConfigDialog.ActivityResultEvent event = new GMusicConfigDialog.ActivityResultEvent();
        event.resultCode = resultCode;
        event.requestCode = requestCode;
        EventBus.getDefault().post(event);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PipeLine.get();

        UserCollection userCollection = (UserCollection) CollectionManager.get()
                .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
        userCollection.loadMediaItems(false);

        mSavedInstanceState = savedInstanceState;

        setContentView(R.layout.tomahawk_main_activity);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mSmoothProgressBar = (SmoothProgressBar) findViewById(R.id.smoothprogressbar);

        mTitle = mDrawerTitle = getTitle().toString().toUpperCase();
        getSupportActionBar().setTitle("");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelSlideListener(this);

        mPlaybackPanel = (PlaybackPanel) findViewById(R.id.playback_panel);

        mActionBarBg = findViewById(R.id.action_bar_background);

        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open,
                    R.string.drawer_close) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    if (mSearchItem != null) {
                        MenuItemCompat.collapseActionView(mSearchItem);
                    }
                }
            };
            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        // set customization variables on the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        // Set default preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains(
                PreferenceAdvancedFragment.FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING)) {
            preferences.edit().putBoolean(
                    PreferenceAdvancedFragment.FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING, true)
                    .commit();
        }

        mRunAfterInit = new Runnable() {
            @Override
            public void run() {
                handleIntent(getIntent());
            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        mRunAfterInit = new Runnable() {
            @Override
            public void run() {
                handleIntent(intent);
            }
        };
    }

    private void handleIntent(Intent intent) {
        if (SHOW_PLAYBACKFRAGMENT_ON_STARTUP.equals(intent.getAction())) {
            // if this Activity is being shown after the user clicked the notification
            if (mSlidingUpPanelLayout != null) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        }
        if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            Bundle bundle = new Bundle();
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_SMALL);
            FragmentUtils.replace(this, PreferencePagerFragment.class, bundle);
        }

        if (intent.getData() != null) {
            final Uri data = intent.getData();
            intent.setData(null);
            List<String> pathSegments = data.getPathSegments();
            String host = data.getHost();
            String scheme = data.getScheme();
            if ((scheme != null && (scheme.equals("spotify") || scheme.equals("tomahawk")))
                    || (host != null && (host.contains("spotify.com") || host.contains("hatchet.is")
                    || host.contains("toma.hk") || host.contains("beatsmusic.com")
                    || host.contains("deezer.com") || host.contains("rdio.com")
                    || host.contains("soundcloud.com")))) {
                PipeLine.get().lookupUrl(data.toString());
            } else if ((pathSegments != null
                    && pathSegments.get(pathSegments.size() - 1).endsWith(".xspf"))
                    || (intent.getType() != null
                    && intent.getType().equals("application/xspf+xml"))) {
                TomahawkRunnable r = new TomahawkRunnable(
                        TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_HIGH) {
                    @Override
                    public void run() {
                        Playlist pl = XspfParser.parse(data);
                        if (pl != null) {
                            final Bundle bundle = new Bundle();
                            bundle.putString(TomahawkFragment.PLAYLIST, pl.getCacheKey());
                            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    FragmentUtils.replace(TomahawkMainActivity.this,
                                            PlaylistEntriesFragment.class, bundle);
                                }
                            });
                        }
                    }
                };
                ThreadManager.get().execute(r);
            } else if (pathSegments != null
                    && (pathSegments.get(pathSegments.size() - 1).endsWith(".axe")
                    || pathSegments.get(pathSegments.size() - 1).endsWith(".AXE"))) {
                InstallPluginConfigDialog dialog = new InstallPluginConfigDialog();
                Bundle args = new Bundle();
                args.putString(InstallPluginConfigDialog.PATH_TO_AXE_URI_STRING, data.toString());
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), null);
            } else {
                String albumName;
                String trackName;
                String artistName;
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(this, data);
                    albumName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    artistName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    trackName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    retriever.release();
                } catch (Exception e) {
                    Log.e(TAG, "handleIntent: " + e.getClass() + ": " + e.getLocalizedMessage());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            String msg = TomahawkApp.getContext().getString(R.string.invalid_file);
                            Toast.makeText(TomahawkApp.getContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                if (TextUtils.isEmpty(trackName) && pathSegments != null) {
                    trackName = pathSegments.get(pathSegments.size() - 1);
                }
                Query query = Query.get(trackName, albumName, artistName, false);
                Result result = Result.get(data.toString(), query.getBasicTrack(),
                        UserCollectionStubResolver.get());
                float trackScore = query.howSimilar(result);
                query.addTrackResult(result, trackScore);
                Bundle bundle = new Bundle();
                List<Query> queries = new ArrayList<>();
                queries.add(query);
                Playlist playlist = Playlist.fromQueryList(
                        TomahawkMainActivity.getSessionUniqueStringId(), false, "", "", queries);
                bundle.putString(TomahawkFragment.PLAYLIST, playlist.getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils.replace(
                        TomahawkMainActivity.this, PlaylistEntriesFragment.class, bundle);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        ACTIONBAR_HEIGHT = TomahawkApp.getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);

        if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            mPlaybackPanel.setVisibility(View.GONE);
        } else {
            mPlaybackPanel.setup(mSlidingUpPanelLayout.getPanelState()
                    == SlidingUpPanelLayout.PanelState.EXPANDED);
            mPlaybackPanel.update(mPlaybackService);
            mPlaybackPanel.setVisibility(View.VISIBLE);
            if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                onPanelSlide(mSlidingUpPanelLayout, 1f);
            } else {
                onPanelSlide(mSlidingUpPanelLayout, 0f);
            }
        }

        if (mShouldShowAnimationHandler == null) {
            mShouldShowAnimationHandler = new Handler();
            mShouldShowAnimationHandler.post(mShouldShowAnimationRunnable);
        }

        if (mProgressHandler == null) {
            mProgressHandler = new ProgressHandler(this);
            if (mPlaybackService != null && mPlaybackService.getCurrentTrack() != null
                    && mPlaybackService.isPlaying()) {
                mProgressHandler.sendEmptyMessage(0);
            }
        }

        if (mTomahawkMainReceiver == null) {
            mTomahawkMainReceiver = new TomahawkMainReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // Install listener that disables the navigation drawer and hides the actionbar whenever
        // a WelcomeFragment or ContextMenuFragment is the currently shown Fragment.
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        Fragment lastFragment = getSupportFragmentManager().findFragmentByTag(
                                FragmentUtils.FRAGMENT_TAG);
                        if (lastFragment instanceof WelcomeFragment
                                || lastFragment instanceof ContextMenuFragment) {
                            if (mDrawerLayout != null) {
                                mDrawerLayout.setDrawerLockMode(
                                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                            }
                            hideActionbar();
                        } else {
                            if (mDrawerLayout != null) {
                                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                            }
                            showActionBar(false);
                        }
                    }
                });
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //Setup UserVoice
        Config config = new Config("tomahawk.uservoice.com");
        config.setForumId(224204);
        config.setTopicId(62613);
        UserVoice.init(config, TomahawkMainActivity.this);

        //Resolve currently logged-in user
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                String requestId = InfoSystem.get().resolve(user);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        });

        //Ask for notification service access if hatchet user logged in
        HatchetAuthenticatorUtils hatchetAuthUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (hatchetAuthUtils.isLoggedIn()) {
            attemptAskAccess();
        }

        if (!mRootViewsInitialized) {
            mRootViewsInitialized = true;

            updateDrawer();

            Intent intent = new Intent(TomahawkMainActivity.this,
                    PlaybackService.class);
            startService(intent);
            bindService(intent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);

            if (mSavedInstanceState == null) {
                Bundle bundle = new Bundle();
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_PLAYBACK);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.playback_fragment_frame,
                                Fragment.instantiate(TomahawkMainActivity.this,
                                        PlaybackFragment.class.getName(), bundle),
                                null)
                        .commitAllowingStateLoss();
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(final User user) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                FragmentUtils.addRootFragment(TomahawkMainActivity.this, user);

                                SharedPreferences preferences =
                                        PreferenceManager.getDefaultSharedPreferences(
                                                TomahawkMainActivity.this);
                                if (!preferences.getBoolean(
                                        TomahawkMainActivity.COACHMARK_WELCOMEFRAGMENT_DISABLED,
                                        false)) {
                                    FragmentUtils.replace(
                                            TomahawkMainActivity.this, WelcomeFragment.class, null);
                                }
                            }
                        });
                    }
                });
            } else {
                boolean actionBarHidden = mSavedInstanceState
                        .getBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, false);
                if (actionBarHidden) {
                    hideActionbar();
                }
            }
        }

        findViewById(R.id.splash_imageview).setVisibility(View.GONE);
        if (mRunAfterInit != null) {
            mRunAfterInit.run();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mShouldShowAnimationHandler != null) {
            mShouldShowAnimationHandler.removeCallbacks(mShouldShowAnimationRunnable);
            mShouldShowAnimationHandler = null;
        }

        if (mTomahawkMainReceiver != null) {
            unregisterReceiver(mTomahawkMainReceiver);
            mTomahawkMainReceiver = null;
        }

        if (mProgressHandler != null) {
            mProgressHandler.removeCallbacksAndMessages(null);
            mProgressHandler = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, !getSupportActionBar().isShowing());
    }

    @Override
    public void onDestroy() {
        if (mPlaybackService != null) {
            unbindService(mPlaybackServiceConnection);
        }

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.tomahawk_main_menu, menu);

        // customize the searchView
        mSearchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        SearchViewStyle.on(searchView)
                .setSearchPlateDrawableId(R.drawable.edittext_background)
                .setCursorColor(getResources().getColor(R.color.tomahawk_red));
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !TextUtils.isEmpty(query)) {
                    DatabaseHelper.get().addEntryToSearchHistory(query);
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.QUERY_STRING, query);
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_STATIC);
                    FragmentUtils
                            .replace(TomahawkMainActivity.this, SearchPagerFragment.class, bundle);
                    if (mSearchItem != null) {
                        MenuItemCompat.collapseActionView(mSearchItem);
                    }
                    searchView.clearFocus();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Cursor cursor = DatabaseHelper.get().getSearchHistoryCursor(newText);
                if (cursor.getCount() != 0) {
                    String[] columns = new String[]{
                            TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY};
                    int[] columnTextId = new int[]{android.R.id.text1};

                    SuggestionSimpleCursorAdapter simple = new SuggestionSimpleCursorAdapter(
                            getBaseContext(), R.layout.searchview_dropdown_item,
                            cursor, columns, columnTextId, 0);

                    if (searchView.getSuggestionsAdapter() != null
                            && searchView.getSuggestionsAdapter().getCursor() != null) {
                        searchView.getSuggestionsAdapter().getCursor().close();
                    }
                    searchView.setSuggestionsAdapter(simple);
                    return true;
                } else {
                    cursor.close();
                    return false;
                }
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                SQLiteCursor cursor = (SQLiteCursor) searchView.getSuggestionsAdapter()
                        .getItem(position);
                int indexColumnSuggestion = cursor
                        .getColumnIndex(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY);

                searchView.setQuery(cursor.getString(indexColumnSuggestion), false);

                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                SQLiteCursor cursor = (SQLiteCursor) searchView.getSuggestionsAdapter()
                        .getItem(position);
                int indexColumnSuggestion = cursor
                        .getColumnIndex(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY);

                searchView.setQuery(cursor.getString(indexColumnSuggestion), false);

                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        EventBus.getDefault().post(new PlaybackService.ReadyEvent());
        mPlaybackPanel.update(mPlaybackService);
    }

    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    public PlaybackService getPlaybackService() {
        return mPlaybackService;
    }

    public void onHatchetLoggedInOut(boolean loggedIn) {
        if (loggedIn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                attemptAskAccess();
            }
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolve(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        }
        updateDrawer();
    }

    public void updateDrawer() {
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                HatchetAuthenticatorUtils authenticatorUtils
                        = (HatchetAuthenticatorUtils) AuthenticatorManager.get()
                        .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                // Set up the TomahawkMenuAdapter. Give it its set of menu item texts and icons to display
                mDrawerList = (StickyListHeadersListView) findViewById(R.id.left_drawer);
                final ArrayList<TomahawkMenuAdapter.ResourceHolder> holders = new ArrayList<>();
                TomahawkMenuAdapter.ResourceHolder holder
                        = new TomahawkMenuAdapter.ResourceHolder();
                if (authenticatorUtils.isLoggedIn()) {
                    holder.id = HUB_ID_USERPAGE;
                    holder.title = user.getName();
                    holder.image = user.getImage();
                    holder.user = user;
                    holders.add(holder);
                    holder = new TomahawkMenuAdapter.ResourceHolder();
                    holder.id = HUB_ID_FEED;
                    holder.title = getString(R.string.drawer_title_feed);
                    holder.iconResId = R.drawable.ic_action_dashboard;
                    holders.add(holder);
                }
                holder = new TomahawkMenuAdapter.ResourceHolder();
                holder.id = HUB_ID_COLLECTION;
                holder.title = getString(R.string.drawer_title_collection);
                holder.iconResId = R.drawable.ic_action_collection;
                Collection userCollection = CollectionManager.get().getCollection(
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Boolean isLoading = mCollectionLoadingMap.get(userCollection);
                holder.isLoading = isLoading != null && isLoading;
                holders.add(holder);
                holder = new TomahawkMenuAdapter.ResourceHolder();
                holder.id = HUB_ID_LOVEDTRACKS;
                holder.title = getString(R.string.drawer_title_lovedtracks);
                holder.iconResId = R.drawable.ic_action_favorites;
                holders.add(holder);
                holder = new TomahawkMenuAdapter.ResourceHolder();
                holder.id = HUB_ID_PLAYLISTS;
                holder.title = getString(R.string.drawer_title_playlists);
                holder.iconResId = R.drawable.ic_action_playlist;
                holders.add(holder);
                holder = new TomahawkMenuAdapter.ResourceHolder();
                holder.id = HUB_ID_SETTINGS;
                holder.title = getString(R.string.drawer_title_settings);
                holder.iconResId = R.drawable.ic_action_settings;
                holders.add(holder);
                for (Collection collection : CollectionManager.get().getCollections()) {
                    if (collection instanceof ScriptResolverCollection) {
                        ScriptResolverCollection resolverCollection
                                = (ScriptResolverCollection) collection;
                        holder = new TomahawkMenuAdapter.ResourceHolder();
                        holder.collection = resolverCollection;
                        isLoading = mCollectionLoadingMap.get(resolverCollection);
                        holder.isLoading = isLoading != null && isLoading;
                        holders.add(holder);
                    }
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (mDrawerList.getAdapter() == null) {
                            mTomahawkMenuAdapter = new TomahawkMenuAdapter(holders);
                            mDrawerList.setAdapter(mTomahawkMenuAdapter);
                        } else {
                            mTomahawkMenuAdapter.setResourceHolders(holders);
                        }
                    }
                });

                mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
            }
        });
    }

    public void closeDrawer() {
        if (mDrawerLayout != null && mDrawerList != null) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.context_menu_fragment) == null && mSlidingUpPanelLayout.isEnabled()
                && (mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.EXPANDED
                || mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
                AnimationUtils.fade(mPlaybackPanel, AnimationUtils.DURATION_CONTEXTMENU, true);
            }
            super.onBackPressed();
        }
    }

    public static long getSessionUniqueId() {
        return mSessionIdCounter++;
    }

    public static String getSessionUniqueStringId() {
        return String.valueOf(getSessionUniqueId());
    }

    public static String getLifetimeUniqueStringId() {
        return String.valueOf(System.currentTimeMillis()) + getSessionUniqueStringId();
    }

    public float getSlidingOffset() {
        return mSlidingOffset;
    }

    @Override
    public void onPanelSlide(View view, float v) {
        mSlidingOffset = v;
        if (v > 0.5f) {
            hideActionbar();
        } else if (v < 0.5f && v > 0f) {
            showActionBar(true);
        }
        final View topPanel = mSlidingUpPanelLayout.findViewById(R.id.top_buttonpanel);
        if (v > 0.15f) {
            AnimationUtils.fade(topPanel, 0f, 1f,
                    AnimationUtils.DURATION_PLAYBACKTOPPANEL, true,
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            topPanel.findViewById(R.id.imageButton_repeat)
                                    .setVisibility(View.VISIBLE);
                            topPanel.findViewById(R.id.close_button).setVisibility(View.VISIBLE);
                            topPanel.findViewById(R.id.imageButton_shuffle)
                                    .setVisibility(View.VISIBLE);
                            animation.removeListener(this);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        } else if (v < 0.15f) {
            AnimationUtils.fade(mSlidingUpPanelLayout.findViewById(R.id.top_buttonpanel), 1f, 0f,
                    AnimationUtils.DURATION_PLAYBACKTOPPANEL, false,
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            topPanel.findViewById(R.id.imageButton_repeat).setVisibility(View.GONE);
                            topPanel.findViewById(R.id.close_button).setVisibility(View.GONE);
                            topPanel.findViewById(R.id.imageButton_shuffle)
                                    .setVisibility(View.GONE);
                            animation.removeListener(this);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        }
        int position = Math.min(10000, Math.max(0, (int) ((v - 0.8f) * 10000f / (1f - 0.8f))));
        mPlaybackPanel.animate(position);
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelCollapsed(View view) {
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelExpanded(View view) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean(
                TomahawkMainActivity.COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED, true)
                .apply();
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelAnchored(View view) {
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelHidden(View view) {
        sendSlidingLayoutChangedEvent();
    }

    public void collapsePanel() {
        if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    public void showPanel() {
        if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            mPlaybackPanel.setup(mSlidingUpPanelLayout.getPanelState()
                    == SlidingUpPanelLayout.PanelState.EXPANDED);
            mPlaybackPanel.update(mPlaybackService);
            showPlaybackPanel(true);
        }
    }

    public void hidePanel() {
        if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            hidePlaybackPanel();
        }
    }

    public void showActionBar(boolean forced) {
        if (forced || mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.COLLAPSED
                || mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.HIDDEN) {
            if (!getSupportActionBar().isShowing()) {
                getSupportActionBar().show();
            }
            AnimationUtils.moveY(mActionBarBg, 0, -ACTIONBAR_HEIGHT, 250, true);
        }
    }

    public void hideActionbar() {
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
        AnimationUtils.moveY(mActionBarBg, 0, -ACTIONBAR_HEIGHT, 250, false);
    }

    public void showPlaybackPanel(boolean forced) {
        if (forced || mSlidingUpPanelLayout.getPanelState()
                != SlidingUpPanelLayout.PanelState.HIDDEN) {
            AnimationUtils.fade(mPlaybackPanel, AnimationUtils.DURATION_CONTEXTMENU, true);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!preferences.getBoolean(COACHMARK_SEEK_DISABLED, false)
                    && preferences.getLong(COACHMARK_SEEK_TIMESTAMP, 0) + 259200000
                    < System.currentTimeMillis()) {
                final View coachMark = ViewUtils.ensureInflation(mPlaybackPanel,
                        R.id.playbackpanel_seek_coachmark_stub, R.id.playbackpanel_seek_coachmark);
                coachMark.findViewById(R.id.close_button).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                coachMark.setVisibility(View.GONE);
                            }
                        });
                coachMark.setVisibility(View.VISIBLE);
                preferences.edit().putLong(COACHMARK_SEEK_TIMESTAMP, System.currentTimeMillis())
                        .apply();
            }
        }
    }

    public void hidePlaybackPanel() {
        AnimationUtils.fade(mPlaybackPanel, AnimationUtils.DURATION_CONTEXTMENU, false);
    }

    private void sendSlidingLayoutChangedEvent() {
        if (mSlidingUpPanelLayout.getPanelState() != mLastSlidingState) {
            mLastSlidingState = mSlidingUpPanelLayout.getPanelState();

            SlidingLayoutChangedEvent event = new SlidingLayoutChangedEvent();
            event.mSlideState = mSlidingUpPanelLayout.getPanelState();
            EventBus.getDefault().post(event);
        }
    }

    public PlaybackPanel getPlaybackPanel() {
        return mPlaybackPanel;
    }

    public SlidingUpPanelLayout getSlidingUpPanelLayout() {
        return mSlidingUpPanelLayout;
    }

    public void showFilledActionBar() {
        findViewById(R.id.action_bar_background).setBackgroundResource(
                R.color.primary_background_inverted);
    }

    public void showGradientActionBar() {
        findViewById(R.id.action_bar_background).setBackgroundResource(R.drawable.below_shadow);
    }

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user hasn't been asked before and is logged into hatchet
     */
    public void attemptAskAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && !PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext())
                .getBoolean(AskAccessConfigDialog.ASKED_FOR_ACCESS, false)) {
            askAccess();
        }
    }

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user is logged into Hatchet and we don't already have access
     */
    public void askAccess() {
        if (AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).isLoggedIn()) {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            preferences.edit().putBoolean(AskAccessConfigDialog.ASKED_FOR_ACCESS, true).commit();
            new AskAccessConfigDialog().show(getSupportFragmentManager(), null);
        }
    }
}
