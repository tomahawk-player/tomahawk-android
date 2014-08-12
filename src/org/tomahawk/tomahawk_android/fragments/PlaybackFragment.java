/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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
package org.tomahawk.tomahawk_android.fragments;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.PlaybackPagerAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreatePlaylistDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;
import org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * This {@link android.support.v4.app.Fragment} represents our Playback view in which the user can
 * play/stop/pause. It is being shown as the topmost fragment in the {@link PlaybackFragment}'s
 * {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class PlaybackFragment extends TomahawkFragment {

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;

    private PlaybackPagerAdapter mPlaybackPagerAdapter;

    private TomahawkVerticalViewPager mTomahawkVerticalViewPager;

    private Menu mMenu;

    private PlaybackSeekBar mPlaybackSeekBar;

    private Toast mToast;

    private ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            handlePageSelect();
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    /**
     * This listener handles our button clicks
     */
    private View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageButton_shuffle:
                    onShuffleClicked();
                    break;
                case R.id.imageButton_previous:
                    onPreviousClicked();
                    break;
                case R.id.imageButton_playpause:
                    onPlayPauseClicked();
                    break;
                case R.id.imageButton_next:
                    onNextClicked();
                    break;
                case R.id.imageButton_repeat:
                    onRepeatClicked();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRestoreScrollPosition(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Set listeners on our buttons
        view.findViewById(R.id.imageButton_shuffle).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_previous).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_playpause).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_next).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_repeat).setOnClickListener(mButtonClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        onPlaylistChanged();

        getActivity().setTitle(getString(R.string.playbackfragment_title_string));

        PlaybackService playbackService = activity.getPlaybackService();
        FrameLayout viewPagerFrame = (FrameLayout) activity.getLayoutInflater()
                .inflate(R.layout.album_art_view_pager, null);
        SlidingUpPanelLayout slidingLayout =
                (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingLayout.setEnableDragViewTouchEvents(true);
        slidingLayout.setDragView(viewPagerFrame.findViewById(R.id.sliding_layout_drag_view));
        ViewPager viewPager = (ViewPager) viewPagerFrame.findViewById(R.id.album_art_view_pager);
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(activity,
                activity.getSupportFragmentManager(), activity.getLayoutInflater(), viewPager,
                this, slidingLayout);
        mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
        viewPager.setAdapter(mAlbumArtSwipeAdapter);
        viewPager.setOnPageChangeListener(mAlbumArtSwipeAdapter);

        mTomahawkVerticalViewPager = (TomahawkVerticalViewPager) getView()
                .findViewById(R.id.playback_view_pager);
        mPlaybackPagerAdapter = new PlaybackPagerAdapter(viewPagerFrame, getListView());
        mTomahawkVerticalViewPager.setAdapter(mPlaybackPagerAdapter);
        mTomahawkVerticalViewPager.setStickyListHeadersListView(getListView());
        mTomahawkVerticalViewPager.setOnPageChangeListener(mOnPageChangeListener);
        if (mMenu != null) {
            handlePageSelect();
        }

        mPlaybackSeekBar = (PlaybackSeekBar) getView().findViewById(R.id.seekBar_track);
        mPlaybackSeekBar.setTextViewCurrentTime((TextView) getView().findViewById(
                R.id.textView_currentTime));
        mPlaybackSeekBar.setTextViewCompletionTime((TextView) getView().findViewById(
                R.id.textView_completionTime));
        mPlaybackSeekBar.setPlaybackService(playbackService);

        refreshTrackInfo();
        refreshPlayPauseButtonState();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
        updateAdapter();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;

        mMenu.findItem(R.id.action_show_playlist_item).setVisible(true);
        mMenu.findItem(R.id.action_saveplaylist_item).setVisible(true);
        mMenu.findItem(R.id.action_gotoartist_item).setVisible(true);
        mMenu.findItem(R.id.action_gotoalbum_item).setVisible(true);

        onTrackChanged();

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        PlaybackService playbackService = activity.getPlaybackService();
        if (playbackService != null && playbackService.getCurrentPlaylist() != null
                && item != null) {
            if (item.getItemId() == R.id.action_saveplaylist_item) {
                Playlist playlist = Playlist.fromQueryList("",
                        playbackService.getCurrentPlaylist().getQueries());
                CreatePlaylistDialog dialog = new CreatePlaylistDialog();
                Bundle args = new Bundle();
                args.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY, playlist.getId());
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), null);
                return true;
            } else if (item.getItemId() == R.id.action_show_playlist_item) {
                if (mTomahawkVerticalViewPager.getCurrentItem() == 0) {
                    mTomahawkVerticalViewPager.setCurrentItem(1, true);
                } else {
                    mTomahawkVerticalViewPager.setCurrentItem(0, true);
                }
                return true;
            } else if (item.getItemId() == R.id.action_gotoartist_item) {
                if (playbackService.getCurrentQuery() != null) {
                    FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                            AlbumsFragment.class,
                            playbackService.getCurrentQuery().getArtist().getCacheKey(),
                            TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
                }
            } else if (item.getItemId() == R.id.action_gotoalbum_item) {
                if (playbackService.getCurrentQuery() != null) {
                    FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                            TracksFragment.class,
                            playbackService.getCurrentQuery().getAlbum().getCacheKey(),
                            TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
                }
            }
            ((TomahawkMainActivity) getActivity()).closeDrawer();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(TomahawkListItem item) {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            if (item instanceof PlaylistEntry) {
                PlaylistEntry entry = (PlaylistEntry) item;
                if (entry.getQuery().isPlayable()) {
                    // if the user clicked on an already playing track
                    if (playbackService.getCurrentEntry() == entry) {
                        playbackService.playPause();
                    } else {
                        playbackService.setCurrentEntry(entry.getId());
                    }
                }
            }
        }
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            if (mAlbumArtSwipeAdapter != null && mPlaybackSeekBar != null) {
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                mPlaybackSeekBar.setPlaybackService(playbackService);
                refreshTrackInfo();
                refreshPlayPauseButtonState();
                refreshRepeatButtonState();
                refreshShuffleButtonState();
            }
        }
        onPlaylistChanged();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    @Override
    public void onTrackChanged() {
        super.onTrackChanged();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
        if (tomahawkListAdapter != null && playbackService != null
                && playbackService.getCurrentEntry() != null) {
            if (mMenu != null) {
                handlePageSelect();
            }
        }
        refreshTrackInfo();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    @Override
    public void onPlaylistChanged() {
        super.onPlaylistChanged();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();

        if (playbackService != null
                && playbackService.getCurrentPlaylist() != null
                && playbackService.getCurrentPlaylist().getCount() > 0) {
            mShownQueries = playbackService.getCurrentPlaylist().getQueries();
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        }
        if (tomahawkListAdapter != null) {
            if (playbackService != null && playbackService.getCurrentPlaylist() != null
                    && playbackService.getCurrentPlaylist().getCount() > 0) {
                ArrayList<TomahawkListItem> tracks = new ArrayList<TomahawkListItem>();
                tracks.addAll(playbackService.getCurrentPlaylist().getQueries());
                tomahawkListAdapter.setListItems(tracks);
                tomahawkListAdapter.notifyDataSetChanged();
            }
        } else {
            updateAdapter();
        }
        refreshRepeatButtonState();
        refreshShuffleButtonState();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackFragment received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    @Override
    public void onPlaystateChanged() {
        super.onPlaystateChanged();

        refreshPlayPauseButtonState();
        if (mPlaybackSeekBar != null) {
            mPlaybackSeekBar.updateSeekBarPosition();
        }
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        PlaybackService playbackService = activity.getPlaybackService();
        if (playbackService != null && playbackService.getCurrentPlaylist() != null) {
            List<TomahawkListItem> entries = new ArrayList<TomahawkListItem>();
            entries.addAll(playbackService.getCurrentPlaylist().getEntries());
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                        layoutInflater, entries, this);
                tomahawkListAdapter.setShowPlaystate(true);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(entries);
            }
        }

        updateShowPlaystate();
    }

    private void handlePageSelect() {
        MenuItem item = mMenu.findItem(R.id.action_show_playlist_item);
        if (mTomahawkVerticalViewPager != null
                && mTomahawkVerticalViewPager.getCurrentItem() == 0) {
            item.setIcon(R.drawable.ic_action_collections_view_as_list);
            item.setTitle(R.string.menu_item_show_playlist);
            getListView().post(new Runnable() {
                @Override
                public void run() {
                    PlaybackService playbackService =
                            ((TomahawkMainActivity) getActivity()).getPlaybackService();
                    if (playbackService != null && playbackService.getCurrentPlaylist() != null
                            && getListView() != null) {
                        getListView().clearFocus();
                        getListView().setSelection(
                                playbackService.getCurrentPlaylist().getCurrentQueryIndex());
                        getListView().requestFocus();
                    }
                }
            });
        } else {
            item.setIcon(R.drawable.ic_action_up);
            item.setTitle(R.string.menu_item_show_album_cover);
        }
    }

    /**
     * Called when the play/pause button is clicked.
     */
    public void onPlayPauseClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.playPause(true);
        }
    }

    /**
     * Called when the next button is clicked.
     */
    public void onNextClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.next();
        }
    }

    /**
     * Called when the previous button is clicked.
     */
    public void onPreviousClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.previous();
        }
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.setShuffled(!playbackService.getCurrentPlaylist().isShuffled());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(
                            playbackService.getCurrentPlaylist().isShuffled()
                                    ? R.string.playbackactivity_toastshuffleon_string
                                    : R.string.playbackactivity_toastshuffleoff_string
                    ),
                    Toast.LENGTH_SHORT
            );
            mToast.show();
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.setRepeating(!playbackService.getCurrentPlaylist().isRepeating());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(
                    playbackService.getCurrentPlaylist().isRepeating()
                            ? R.string.playbackactivity_toastrepeaton_string
                            : R.string.playbackactivity_toastrepeatoff_string
            ), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current Track, if possible
     * (meaning mPlaybackService is not null).
     */
    protected void refreshTrackInfo() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            refreshTrackInfo(playbackService.getCurrentQuery());
        } else {
            refreshTrackInfo(null);
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the given Track.
     *
     * @param query the query to which the track info view stuff should be updated to
     */
    protected void refreshTrackInfo(final Query query) {
        if (getView() != null) {
            TextView artistTextView = (TextView) getView().findViewById(R.id.textView_artist);
            TextView albumTextView = (TextView) getView().findViewById(R.id.textView_album);
            TextView titleTextView = (TextView) getView().findViewById(R.id.textView_title);
            TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            final PlaybackService playbackService = activity.getPlaybackService();
            if (query != null && playbackService != null) {
                /*
                This logic makes sure, that if a track is being skipped by the user, it doesn't do this
                for eternity. Because a press of the next button would cause the AlbumArtSwipeAdapter
                to display a swipe to the next track, which would then cause another skipping to the
                next track. That's why we have to make a difference between a swipe by the user, and a
                programmatically called swipe.
                */
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                if (!mAlbumArtSwipeAdapter.isSwiped()) {
                    mAlbumArtSwipeAdapter.setByUser(false);
                    if (playbackService.getCurrentPlaylist().getCurrentQueryIndex() >= 0) {
                        mAlbumArtSwipeAdapter.setCurrentItem(
                                playbackService.getCurrentPlaylist().getCurrentQueryIndex(), true);
                    }
                    mAlbumArtSwipeAdapter.setByUser(true);
                }

                // Update the textViews, if available (in other words, if in landscape mode)
                if (artistTextView != null) {
                    if (query.getArtist() != null && query.getArtist().getName() != null) {
                        artistTextView.setText(query.getArtist().toString());
                        if (!TextUtils.isEmpty(query.getArtist().getName())) {
                            artistTextView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    FragmentUtils.replace(getActivity(),
                                            getActivity().getSupportFragmentManager(),
                                            AlbumsFragment.class, query.getArtist().getCacheKey(),
                                            TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
                                }
                            });
                        }
                    } else {
                        artistTextView.setText(R.string.playbackactivity_unknown_string);
                    }
                }
                if (albumTextView != null) {
                    if (query.getAlbum() != null && query.getAlbum().getName() != null) {
                        albumTextView.setText(query.getAlbum().toString());
                        if (!TextUtils.isEmpty(query.getAlbum().getName())) {
                            albumTextView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    FragmentUtils.replace(getActivity(),
                                            getActivity().getSupportFragmentManager(),
                                            TracksFragment.class, query.getAlbum().getCacheKey(),
                                            TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
                                }
                            });
                        }
                    } else {
                        albumTextView.setText(R.string.playbackactivity_unknown_string);
                    }
                }
                if (titleTextView != null) {
                    if (query.getName() != null) {
                        titleTextView.setText(query.getName());
                    } else {
                        titleTextView.setText(R.string.playbackactivity_unknown_string);
                    }
                }

                // Make all buttons clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(true);
                getView().findViewById(R.id.imageButton_next).setClickable(true);
                getView().findViewById(R.id.imageButton_previous).setClickable(true);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(true);
                getView().findViewById(R.id.imageButton_repeat).setClickable(true);

                // Update the PlaybackSeekBar
                mPlaybackSeekBar.setPlaybackService(playbackService);
                mPlaybackSeekBar.setMax();
                mPlaybackSeekBar.setUpdateInterval();
                mPlaybackSeekBar.updateSeekBarPosition();
                mPlaybackSeekBar.updateTextViewCompleteTime();

                ImageButton loveButton = (ImageButton) getView().findViewById(R.id.love_button);
                if (loveButton != null) {
                    if (DatabaseHelper.getInstance().isItemLoved(query)) {
                        loveButton.setImageResource(R.drawable.ic_action_loved);
                    } else {
                        loveButton.setImageResource(R.drawable.ic_action_notloved);
                    }
                    loveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CollectionManager.getInstance().toggleLovedItem(query);
                            getActivity().sendBroadcast(
                                    new Intent(PlaybackService.BROADCAST_CURRENTTRACKCHANGED));
                        }
                    });
                }
            } else {
                //No track has been given, so we update the view state accordingly

                if (artistTextView != null) {
                    artistTextView.setText("");
                    artistTextView.setOnClickListener(null);
                }
                if (albumTextView != null) {
                    albumTextView.setText("");
                    albumTextView.setOnClickListener(null);
                }
                if (titleTextView != null) {
                    titleTextView.setText(R.string.playbackactivity_no_track);
                }

                // Make all buttons not clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(false);
                getView().findViewById(R.id.imageButton_next).setClickable(false);
                getView().findViewById(R.id.imageButton_previous).setClickable(false);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(false);
                getView().findViewById(R.id.imageButton_repeat).setClickable(false);

                // Update the PlaybackSeekBar
                if (mPlaybackSeekBar != null) {
                    mPlaybackSeekBar.setEnabled(false);
                    mPlaybackSeekBar.updateSeekBarPosition();
                    mPlaybackSeekBar.updateTextViewCompleteTime();
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current play/pause-button
     * state.
     */
    protected void refreshPlayPauseButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView()
                    .findViewById(R.id.imageButton_playpause);
            if (imageButton != null) {
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isPlaying()) {
                    TomahawkUtils.loadDrawableIntoImageView(getActivity(), imageButton,
                            R.drawable.ic_player_pause);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(getActivity(), imageButton,
                            R.drawable.ic_player_play);
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current repeatButton state.
     */
    protected void refreshRepeatButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView().findViewById(R.id.imageButton_repeat);
            if (imageButton != null && imageButton.getDrawable() != null) {
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.getCurrentPlaylist() != null
                        && playbackService.getCurrentPlaylist().isRepeating()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.tomahawk_red),
                                    PorterDuff.Mode.SRC_IN);
                } else {
                    imageButton.getDrawable().clearColorFilter();
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current shuffleButton state.
     */
    protected void refreshShuffleButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView()
                    .findViewById(R.id.imageButton_shuffle);
            if (imageButton != null && imageButton.getDrawable() != null) {
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.getCurrentPlaylist() != null
                        && playbackService.getCurrentPlaylist().isShuffled()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.tomahawk_red),
                                    PorterDuff.Mode.SRC_IN);
                } else {
                    imageButton.getDrawable().clearColorFilter();
                }
            }
        }
    }
}
