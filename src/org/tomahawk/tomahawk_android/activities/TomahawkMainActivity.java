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

import com.rdio.android.api.OAuth1WebViewActivity;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.RdioAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.database.TomahawkSQLiteHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverUrlResult;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.SuggestionSimpleCursorAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.dialogs.AskAccessConfigDialog;
import org.tomahawk.tomahawk_android.fragments.ArtistPagerFragment;
import org.tomahawk.tomahawk_android.fragments.CloudCollectionFragment;
import org.tomahawk.tomahawk_android.fragments.CollectionPagerFragment;
import org.tomahawk.tomahawk_android.fragments.ContentHeaderFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.fragments.PreferenceAdvancedFragment;
import org.tomahawk.tomahawk_android.fragments.PreferencePagerFragment;
import org.tomahawk.tomahawk_android.fragments.SearchPagerFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.fragments.UserPagerFragment;
import org.tomahawk.tomahawk_android.fragments.WelcomeFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.SearchViewStyle;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.app.Activity;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    private Handler mShouldShowAnimationHandler;

    private final Runnable mShouldShowAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (ThreadManager.getInstance().isActive()
                    || (mPlaybackService != null && mPlaybackService.isPreparing())
                    || ((UserCollection) CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION)).isWorking()) {
                mSmoothProgressBar.setVisibility(View.VISIBLE);
            } else {
                mSmoothProgressBar.setVisibility(View.GONE);
            }
            mShouldShowAnimationHandler.postDelayed(mShouldShowAnimationRunnable, 500);
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
                    AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                    InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
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
                    = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            TomahawkMenuAdapter.ResourceHolder holder =
                    (TomahawkMenuAdapter.ResourceHolder) mDrawerList.getAdapter().getItem(position);
            Bundle bundle = new Bundle();
            if (holder.isCloudCollection) {
                bundle.putString(TomahawkFragment.COLLECTION_ID, holder.id);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, CloudCollectionFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_USERPAGE)) {
                if (authenticatorUtils.getLoggedInUser() == null) {
                    return;
                }
                bundle.putString(TomahawkFragment.USER,
                        authenticatorUtils.getLoggedInUser().getId());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC_USER);
                FragmentUtils.replace(TomahawkMainActivity.this, UserPagerFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_FEED)) {
                if (authenticatorUtils.getLoggedInUser() == null) {
                    return;
                }
                bundle.putInt(TomahawkFragment.SHOW_MODE,
                        SocialActionsFragment.SHOW_MODE_DASHBOARD);
                bundle.putString(TomahawkFragment.USER,
                        authenticatorUtils.getLoggedInUser().getId());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, SocialActionsFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_COLLECTION)) {
                bundle.putString(TomahawkFragment.COLLECTION_ID,
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, CollectionPagerFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_LOVEDTRACKS)) {
                bundle.putString(PlaylistsFragment.PLAYLIST,
                        DatabaseHelper.LOVEDITEMS_PLAYLIST_ID);
                if (authenticatorUtils.getLoggedInUser() != null) {
                    bundle.putString(TomahawkFragment.USER,
                            authenticatorUtils.getLoggedInUser().getId());
                }
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils
                        .replace(TomahawkMainActivity.this, PlaylistEntriesFragment.class, bundle);
            } else if (holder.id.equals(HUB_ID_PLAYLISTS)) {
                if (authenticatorUtils.getLoggedInUser() != null) {
                    bundle.putString(TomahawkFragment.USER,
                            authenticatorUtils.getLoggedInUser().getId());
                }
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                FragmentUtils.replace(TomahawkMainActivity.this, PlaylistsFragment.class, bundle);
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

    @SuppressWarnings("unused")
    public void onEventMainThread(PipeLine.UrlResultsEvent event) {
        Bundle bundle = new Bundle();
        switch (event.mResult.type) {
            case PipeLine.URL_TYPE_ARTIST:
                bundle.putString(TomahawkFragment.ARTIST,
                        Artist.get(event.mResult.name).getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                bundle.putLong(TomahawkFragment.CONTAINER_FRAGMENT_ID,
                        TomahawkMainActivity.getSessionUniqueId());
                FragmentUtils.replace(TomahawkMainActivity.this, ArtistPagerFragment.class, bundle);
                break;
            case PipeLine.URL_TYPE_ALBUM:
                Artist artist = Artist.get(event.mResult.artist);
                bundle.putString(TomahawkFragment.ALBUM,
                        Album.get(event.mResult.name, artist).getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils.replace(TomahawkMainActivity.this, TracksFragment.class, bundle);
                break;
            case PipeLine.URL_TYPE_TRACK:
                bundle.putString(TomahawkFragment.QUERY,
                        Query.get(event.mResult.title, "", event.mResult.artist, false)
                                .getCacheKey());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils.replace(TomahawkMainActivity.this, TracksFragment.class, bundle);
                break;
            case PipeLine.URL_TYPE_PLAYLIST:
                ArrayList<Query> queries = new ArrayList<>();
                for (ScriptResolverUrlResult track : event.mResult.tracks) {
                    Query query = Query.get(track.title, "", track.artist, false);
                    if (event.mResolver != null && event.mResolver.isEnabled()
                            && track.hint != null) {
                        Result result = Result.get(track.hint, query.getBasicTrack(),
                                event.mResolver);
                        float trackScore = query.howSimilar(result);
                        query.addTrackResult(result, trackScore);
                    }
                    queries.add(query);
                }
                Playlist playlist = Playlist.fromQueryList(event.mResult.title, queries);
                playlist.setFilled(true);
                bundle.putString(TomahawkFragment.PLAYLIST, playlist.getId());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils.replace(TomahawkMainActivity.this, PlaylistEntriesFragment.class,
                        bundle);
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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserCollection userCollection = (UserCollection) CollectionManager.getInstance()
                .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
        userCollection.loadMediaItems(true);

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
                mSlidingUpPanelLayout.expandPanel();
            }
        }
        if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            Bundle bundle = new Bundle();
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_SMALL);
            FragmentUtils.replace(this, PreferencePagerFragment.class, bundle);
        }

        if (intent.getData() != null) {
            Uri data = intent.getData();
            intent.setData(null);
            List<String> pathSegments = data.getPathSegments();
            String host = data.getHost();
            String scheme = data.getScheme();
            if (scheme != null && scheme.equals("tomahawkspotifyresolver")) {
                ScriptResolver urlHandler = (ScriptResolver)
                        PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_SPOTIFY);
                urlHandler.onRedirectCallback(data.toString());
            } else if ((scheme != null && (scheme.equals("spotify") || scheme.equals("tomahawk")))
                    || host != null && (host.contains("hatchet.is") || host.contains("toma.hk")
                    || host.contains("beatsmusic.com") || host.contains("deezer.com")
                    || host.contains("rdio.com") || host.contains("soundcloud.com"))) {
                PipeLine.getInstance().lookupUrl(data.toString());
            } else {
                String albumName = null;
                String trackName = null;
                String artistName = null;
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
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "handleIntent: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                if (TextUtils.isEmpty(trackName)) {
                    trackName = pathSegments.get(pathSegments.size() - 1);
                }
                Query query = Query.get(trackName, albumName, artistName, false);
                Resolver resolver =
                        PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                if (resolver != null) {
                    Result result = Result.get(data.toString(), query.getBasicTrack(), resolver);
                    float trackScore = query.howSimilar(result);
                    query.addTrackResult(result, trackScore);
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.QUERY, query.getCacheKey());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace(TomahawkMainActivity.this, TracksFragment.class, bundle);
                }
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

        if (mSlidingUpPanelLayout.isPanelHidden()) {
            mPlaybackPanel.setVisibility(View.GONE);
        } else {
            mPlaybackPanel.setup(mSlidingUpPanelLayout.isPanelExpanded());
            mPlaybackPanel.update(mPlaybackService);
            mPlaybackPanel.setVisibility(View.VISIBLE);
            if (mSlidingUpPanelLayout.isPanelExpanded()) {
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
        HatchetAuthenticatorUtils hatchetAuthUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        String requestId =
                InfoSystem.getInstance().resolve(hatchetAuthUtils.getLoggedInUser());
        if (requestId != null) {
            mCorrespondingRequestIds.add(requestId);
        }

        //Ask for notification service access if hatchet user logged in
        if (hatchetAuthUtils.isLoggedIn()) {
            attemptAskAccess();
        }

        if (!mRootViewsInitialized) {
            mRootViewsInitialized = true;

            updateDrawer();

            //Setup our services
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
                        .commit();
                FragmentUtils.addRootFragment(TomahawkMainActivity.this,
                        hatchetAuthUtils.getLoggedInUser());

                SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                if (!preferences.getBoolean(
                        TomahawkMainActivity.COACHMARK_WELCOMEFRAGMENT_DISABLED, false)) {
                    FragmentUtils.add(this, WelcomeFragment.class, null, R.id.content_viewer_frame);
                }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            RdioAuthenticatorUtils authUtils = (RdioAuthenticatorUtils) AuthenticatorManager
                    .getInstance().getAuthenticatorUtils(TomahawkApp.PLUGINNAME_RDIO);
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Rdio access token is served and yummy");
                if (data != null) {
                    String accessToken = data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN);
                    String accessTokenSecret =
                            data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
                    authUtils.onRdioAuthorised(accessToken, accessTokenSecret);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    String errorCode = data.getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_CODE);
                    String errorDescription = data
                            .getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_DESCRIPTION);
                    authUtils.onLoginFailed(
                            AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                            "Rdio authentication cancelled");
                    Log.e(TAG, "ERROR: " + errorCode + " - " + errorDescription);
                }
            }
        }
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
                    DatabaseHelper.getInstance().addEntryToSearchHistory(query);
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
                Cursor cursor = DatabaseHelper.getInstance().getSearchHistoryCursor(newText);
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
            HatchetAuthenticatorUtils authenticatorUtils
                    = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            String requestId =
                    InfoSystem.getInstance().resolve(authenticatorUtils.getLoggedInUser());
            if (requestId != null) {
                mCorrespondingRequestIds.add(requestId);
            }
        }
        updateDrawer();
    }

    public void updateDrawer() {
        HatchetAuthenticatorUtils authenticatorUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        // Set up the TomahawkMenuAdapter. Give it its set of menu item texts and icons to display
        mDrawerList = (StickyListHeadersListView) findViewById(R.id.left_drawer);
        final ArrayList<TomahawkMenuAdapter.ResourceHolder> holders = new ArrayList<>();
        TomahawkMenuAdapter.ResourceHolder holder = new TomahawkMenuAdapter.ResourceHolder();
        if (authenticatorUtils.getLoggedInUser() != null) {
            holder.id = HUB_ID_USERPAGE;
            holder.title = authenticatorUtils.getLoggedInUser().getName();
            holder.image = authenticatorUtils.getLoggedInUser().getImage();
            holder.user = authenticatorUtils.getLoggedInUser();
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
        holders.add(holder);
        holder = new TomahawkMenuAdapter.ResourceHolder();
        holder.id = HUB_ID_LOVEDTRACKS;
        holder.title = getString(R.string.drawer_title_lovedtracks);
        holder.iconResId = R.drawable.ic_action_favorites;
        holders.add(holder);
        holder = new TomahawkMenuAdapter.ResourceHolder();
        holder.id = HUB_ID_PLAYLISTS;
        holder.title = getString(R.string.drawer_title_playlists);
        holder.iconResId = R.drawable.ic_action_playlist_light;
        holders.add(holder);
        holder = new TomahawkMenuAdapter.ResourceHolder();
        holder.id = HUB_ID_SETTINGS;
        holder.title = getString(R.string.drawer_title_settings);
        holder.iconResId = R.drawable.ic_action_settings;
        holders.add(holder);
        for (Collection collection : CollectionManager.getInstance().getCollections()) {
            if (collection instanceof ScriptResolverCollection) {
                ScriptResolverCollection resolverCollection =
                        (ScriptResolverCollection) collection;
                holder = new TomahawkMenuAdapter.ResourceHolder();
                holder.id = resolverCollection.getId();
                holder.title = resolverCollection.getName();
                holder.collection = resolverCollection;
                holder.isCloudCollection = true;
                holders.add(holder);
            }
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                TomahawkMenuAdapter slideMenuAdapter =
                        new TomahawkMenuAdapter(TomahawkMainActivity.this, holders);
                mDrawerList.setAdapter(slideMenuAdapter);
            }
        });

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    public void closeDrawer() {
        if (mDrawerLayout != null && mDrawerList != null) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.context_menu_fragment) == null && mSlidingUpPanelLayout.isEnabled()
                && (mSlidingUpPanelLayout.isPanelExpanded()
                || mSlidingUpPanelLayout.isPanelAnchored())) {
            mSlidingUpPanelLayout.collapsePanel();
        } else {
            if (!mSlidingUpPanelLayout.isPanelHidden()) {
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
        showActionBar(true);
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
        mSlidingUpPanelLayout.collapsePanel();
    }

    public void showPanel() {
        if (mSlidingUpPanelLayout.isPanelHidden()) {
            mSlidingUpPanelLayout.showPanel();
            mPlaybackPanel.setup(mSlidingUpPanelLayout.isPanelExpanded());
            mPlaybackPanel.update(mPlaybackService);
            showPlaybackPanel(true);
        }
    }

    public void hidePanel() {
        if (!mSlidingUpPanelLayout.isPanelHidden()) {
            mSlidingUpPanelLayout.hidePanel();
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
        if (forced || !mSlidingUpPanelLayout.isPanelHidden()) {
            AnimationUtils.fade(mPlaybackPanel, AnimationUtils.DURATION_CONTEXTMENU, true);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!preferences.getBoolean(COACHMARK_SEEK_DISABLED, false)
                    && preferences.getLong(COACHMARK_SEEK_TIMESTAMP, 0) + 259200000
                    < System.currentTimeMillis()) {
                final View coachMark = TomahawkUtils.ensureInflation(mPlaybackPanel,
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
        if (AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).isLoggedIn()) {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            preferences.edit().putBoolean(AskAccessConfigDialog.ASKED_FOR_ACCESS, true).commit();
            new AskAccessConfigDialog().show(getSupportFragmentManager(), null);
        }
    }
}
