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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.DbCollection;
import org.tomahawk.libtomahawk.collection.Playlist;
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
import org.tomahawk.tomahawk_android.dialogs.AskAccessConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.GMusicConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.InstallPluginConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.WarnOldPluginDialog;
import org.tomahawk.tomahawk_android.fragments.ArtistPagerFragment;
import org.tomahawk.tomahawk_android.fragments.ContentHeaderFragment;
import org.tomahawk.tomahawk_android.fragments.ContextMenuFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.PreferenceAdvancedFragment;
import org.tomahawk.tomahawk_android.fragments.PreferencePagerFragment;
import org.tomahawk.tomahawk_android.fragments.SearchPagerFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.WelcomeFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.MenuDrawer;
import org.tomahawk.tomahawk_android.utils.PluginUtils;
import org.tomahawk.tomahawk_android.utils.SearchViewStyle;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * The main Tomahawk activity
 */
public class TomahawkMainActivity extends AppCompatActivity {

    private final static String TAG = TomahawkMainActivity.class.getSimpleName();

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

    public static class SlidingLayoutChangedEvent {

        public SlidingUpPanelLayout.PanelState mSlideState;

    }

    private float mSlidingOffset = -1f;

    protected final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    private MediaBrowserCompat mMediaBrowser;

    private int mPlaybackState = PlaybackStateCompat.STATE_NONE;

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "MediaBrowser connected");
                    try {
                        MediaControllerCompat mediaController = new MediaControllerCompat(
                                TomahawkMainActivity.this, mMediaBrowser.getSessionToken());
                        setSupportMediaController(mediaController);
                        mediaController.registerCallback(mMediaCallback);
                        mPlaybackPanel.setMediaController(mediaController);
                        mMediaCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                        ContentHeaderFragment.MediaControllerConnectedEvent event
                                = new ContentHeaderFragment.MediaControllerConnectedEvent();
                        EventBus.getDefault().post(event);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not connect media controller: ", e);
                    }
                }
            };

    private final MediaControllerCompat.Callback mMediaCallback
            = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackstate changed" + state);
            mPlaybackState = state.getState();
            mPlaybackPanel.updatePlaybackState(state);
            if (state.getState() != PlaybackStateCompat.STATE_NONE) {
                showPanel();
            } else {
                hidePanel();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                Log.d(TAG, "onMetadataChanged changed" + metadata);
                mPlaybackPanel.updateMetadata(metadata);
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
        }
    };

    private MenuItem mSearchItem;

    private MenuDrawer mMenuDrawer;

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
            if (ThreadManager.get().isActive()
                    || mPlaybackState == PlaybackStateCompat.STATE_BUFFERING
                    || CollectionManager.get().getUserCollection().isWorking()) {
                mSmoothProgressBar.setVisibility(View.VISIBLE);
            } else {
                mSmoothProgressBar.setVisibility(View.GONE);
            }
            mShouldShowAnimationHandler.postDelayed(mShouldShowAnimationRunnable, 500);
        }
    };

    public static class ShowWebViewEvent {

        public int mRequestid;

        public String mUrl;
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

    private SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener
            = new SlidingUpPanelLayout.PanelSlideListener() {

        @Override
        public void onPanelSlide(View view, float v) {
            mSlidingOffset = v;
            updateActionBarState(false);
            final View topPanel = mSlidingUpPanelLayout.findViewById(R.id.top_buttonpanel);
            if (v > 0.15f) {
                AnimationUtils.fade(topPanel, 0f, 1f,
                        AnimationUtils.DURATION_PLAYBACKTOPPANEL, true,
                        new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                topPanel.findViewById(R.id.imageButton_repeat)
                                        .setVisibility(View.VISIBLE);
                                topPanel.findViewById(R.id.close_button)
                                        .setVisibility(View.VISIBLE);
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
                AnimationUtils
                        .fade(mSlidingUpPanelLayout.findViewById(R.id.top_buttonpanel), 1f, 0f,
                                AnimationUtils.DURATION_PLAYBACKTOPPANEL, false,
                                new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        topPanel.findViewById(R.id.imageButton_repeat)
                                                .setVisibility(View.GONE);
                                        topPanel.findViewById(R.id.close_button)
                                                .setVisibility(View.GONE);
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
                    PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
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
    };

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
                        IdGenerator.getSessionUniqueId());
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
                playlist = Playlist.fromQueryList(IdGenerator.getSessionUniqueStringId(),
                        event.mResult.track + " - " + event.mResult.artist, "", queries);
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
                playlist = Playlist.fromQueryList(IdGenerator.getLifetimeUniqueStringId(),
                        event.mResult.title, null, queries);
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
    public void onEventMainThread(DbCollection.InitializedEvent event) {
        mMenuDrawer.updateDrawer(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            if (event.mInfoRequestData != null
                    && event.mInfoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
                mMenuDrawer.updateDrawer(this);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.AddedEvent event) {
        mMenuDrawer.updateDrawer(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(HatchetAuthenticatorUtils.UserLoginEvent event) {
        mMenuDrawer.updateDrawer(this);
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
        intent.putExtra(WebViewActivity.REQUESTID_EXTRA, event.mRequestid);
        startActivity(intent);
    }

    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.ResolversChangedEvent event) {
        String resolverId = event.mScriptResolver.getId();
        if ((resolverId.equals(TomahawkApp.PLUGINNAME_DEEZER)
                || resolverId.equals(TomahawkApp.PLUGINNAME_SPOTIFY))
                && event.mScriptResolver.isEnabled()
                && !PluginUtils.isPluginUpToDate(resolverId)) {
            PipeLine.get().getResolver(resolverId).setEnabled(false);
            WarnOldPluginDialog dialog = new WarnOldPluginDialog();
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.PREFERENCEID, resolverId);
            if (PluginUtils.isPluginInstalled(resolverId)) {
                args.putString(TomahawkFragment.MESSAGE, getString(R.string.warn_old_plugin));
            } else {
                args.putString(TomahawkFragment.MESSAGE, getString(R.string.warn_no_plugin));
            }
            dialog.setArguments(args);
            dialog.show(getSupportFragmentManager(), null);
        }
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

        CollectionManager.get().getUserCollection().loadMediaItems(false);

        mSavedInstanceState = savedInstanceState;

        setContentView(R.layout.tomahawk_main_activity);

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, PlaybackService.class), mConnectionCallback, null);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mSmoothProgressBar = (SmoothProgressBar) findViewById(R.id.smoothprogressbar);

        mTitle = mDrawerTitle = getTitle().toString().toUpperCase();
        getSupportActionBar().setTitle("");

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelSlideListener(mPanelSlideListener);
        hidePanel();

        mPlaybackPanel = (PlaybackPanel) findViewById(R.id.playback_panel);

        mActionBarBg = findViewById(R.id.action_bar_background);

        mMenuDrawer = (MenuDrawer) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mMenuDrawer, R.string.drawer_open,
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
        mMenuDrawer.addDrawerListener(mDrawerToggle);

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
            intent.setAction(null);
            // if this Activity is being shown after the user clicked the notification
            if (mSlidingUpPanelLayout != null) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        }
        if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            intent.removeExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
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
                        IdGenerator.getSessionUniqueStringId(), "", "", queries);
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

        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            mPlaybackPanel.setVisibility(View.GONE);
        } else {
            mPlaybackPanel.setup(mSlidingUpPanelLayout.getPanelState()
                    == SlidingUpPanelLayout.PanelState.EXPANDED);
            mPlaybackPanel.setVisibility(View.VISIBLE);
            if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                mPanelSlideListener.onPanelSlide(mSlidingUpPanelLayout, 1f);
            } else {
                mPanelSlideListener.onPanelSlide(mSlidingUpPanelLayout, 0f);
            }
        }

        if (mShouldShowAnimationHandler == null) {
            mShouldShowAnimationHandler = new Handler();
            mShouldShowAnimationHandler.post(mShouldShowAnimationRunnable);
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
                        updateActionBarState(true);
                    }
                });
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(mMediaCallback);
        }

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

            mMenuDrawer.updateDrawer(this);

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
                                            TomahawkMainActivity.this, WelcomeFragment.class,
                                            null);
                                }
                            }
                        });
                    }
                });
            }
        }
        updateActionBarState(false);

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

    public void closeDrawer() {
        mMenuDrawer.closeDrawer();
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
        mMenuDrawer.updateDrawer(this);
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

    public float getSlidingOffset() {
        return mSlidingOffset;
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
            showPlaybackPanel(true);
        }
    }

    public void hidePanel() {
        if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            hidePlaybackPanel();
        }
    }

    public void updateActionBarState(boolean checkCurrentFragment) {
        boolean hideActionBar = mSlidingOffset > 0.5f;
        boolean forced = true;
        if (checkCurrentFragment && !hideActionBar) {
            Fragment lastFragment =
                    getSupportFragmentManager().findFragmentByTag(FragmentUtils.FRAGMENT_TAG);
            hideActionBar = lastFragment instanceof WelcomeFragment
                    || lastFragment instanceof ContextMenuFragment;
            forced = false;
        }
        if (hideActionBar) {
            if (mMenuDrawer != null) {
                mMenuDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
            hideActionbar();
        } else {
            if (mMenuDrawer != null) {
                mMenuDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
            showActionBar(forced);
        }
    }

    private void showActionBar(boolean forced) {
        if (forced || mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.COLLAPSED
                || mSlidingUpPanelLayout.getPanelState()
                == SlidingUpPanelLayout.PanelState.HIDDEN) {
            if (!getSupportActionBar().isShowing()) {
                getSupportActionBar().show();
            }
            int actionBarHeight = getResources().getDimensionPixelSize(
                    R.dimen.abc_action_bar_default_height_material);
            AnimationUtils.moveY(mActionBarBg, 0, -actionBarHeight, 250, true);
        }
    }

    private void hideActionbar() {
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
        int actionBarHeight = getResources().getDimensionPixelSize(
                R.dimen.abc_action_bar_default_height_material);
        AnimationUtils.moveY(mActionBarBg, 0, -actionBarHeight, 250, false);
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
