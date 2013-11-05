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

import com.actionbarsherlock.app.SherlockFragment;

import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.PlaybackActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This fragment represents our Playback view in which the user can play/stop/pause. It is being
 * shown as the topmost fragment in the PlaybackActivity's listview.
 */
public class PlaybackFragment extends SherlockFragment {

    private PlaybackService mPlaybackService;

    private PlaybackActivity mPlaybackActivity;

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;

    private PlaybackSeekBar mPlaybackSeekBar;

    private Toast mToast;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof PlaybackActivity) {
            mPlaybackActivity = (PlaybackActivity) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, null, false);
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

        init();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mPlaybackActivity = null;
    }

    /**
     * All initializations are done here
     */
    public void init() {
        if (getView().getParent() != null) {
            ViewPager viewPager = (ViewPager) getView().findViewById(R.id.album_art_view_pager);
            mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(mPlaybackActivity, viewPager);
            mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);

            mPlaybackSeekBar = (PlaybackSeekBar) getView().findViewById(R.id.seekBar_track);
            mPlaybackSeekBar.setTextViewCurrentTime(
                    (TextView) mPlaybackActivity.findViewById(R.id.textView_currentTime));
            mPlaybackSeekBar.setTextViewCompletionTime(
                    (TextView) mPlaybackActivity.findViewById(R.id.textView_completionTime));
            mPlaybackSeekBar.setPlaybackService(mPlaybackService);

            refreshActivityTrackInfo();
            refreshPlayPauseButtonState();
            refreshRepeatButtonState();
            refreshShuffleButtonState();
        }
    }

    /**
     * Called when the play/pause button is clicked.
     */
    public void onPlayPauseClicked() {
        if (mPlaybackService != null) {
            mPlaybackService.playPause(true);
        }
    }

    /**
     * Called when the next button is clicked.
     */
    public void onNextClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        if (mPlaybackService != null) {
            mPlaybackService.next();
        }
    }

    /**
     * Called when the previous button is clicked.
     */
    public void onPreviousClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        if (mPlaybackService != null) {
            mPlaybackService.previous();
        }
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        if (mPlaybackService != null) {
            mPlaybackService.setShuffled(!mPlaybackService.getCurrentPlaylist().isShuffled());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getSherlockActivity(), getString(
                    mPlaybackService.getCurrentPlaylist().isShuffled()
                            ? R.string.playbackactivity_toastshuffleon_string
                            : R.string.playbackactivity_toastshuffleoff_string),
                    Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        if (mPlaybackService != null) {
            mPlaybackService.setRepeating(!mPlaybackService.getCurrentPlaylist().isRepeating());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getSherlockActivity(), getString(
                    mPlaybackService.getCurrentPlaylist().isRepeating()
                            ? R.string.playbackactivity_toastrepeaton_string
                            : R.string.playbackactivity_toastrepeatoff_string), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    public void onTrackChanged() {
        if (mPlaybackActivity != null) {
            refreshActivityTrackInfo();
        }
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    public void onPlaylistChanged() {
        if (mPlaybackActivity != null) {
            if (mAlbumArtSwipeAdapter != null) {
                mAlbumArtSwipeAdapter.updatePlaylist();
            }
            refreshRepeatButtonState();
            refreshShuffleButtonState();
        }
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackActivity received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    public void onPlaystateChanged() {
        if (mPlaybackActivity != null) {
            refreshPlayPauseButtonState();
            if (mPlaybackSeekBar != null) {
                mPlaybackSeekBar.updateSeekBarPosition();
            }
        }
    }

    public void setPlaybackService(PlaybackService ps) {
        if (mPlaybackActivity != null && mPlaybackService != ps) {
            mPlaybackService = ps;
            if (mAlbumArtSwipeAdapter != null && mPlaybackSeekBar != null) {
                mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);
                mPlaybackSeekBar.setPlaybackService(mPlaybackService);
                refreshActivityTrackInfo();
                refreshPlayPauseButtonState();
                refreshRepeatButtonState();
                refreshShuffleButtonState();
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current Track, if possible
     * (meaning mPlaybackService is not null).
     */
    protected void refreshActivityTrackInfo() {
        if (mPlaybackActivity != null) {
            if (mPlaybackService != null && mPlaybackService.getCurrentTrack() != null
                    && mAlbumArtSwipeAdapter != null && mPlaybackSeekBar != null) {
                refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
            } else {
                refreshActivityTrackInfo(null);
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the given Track.
     *
     * @param track the track to which the track info view stuff should be updated to
     */
    protected void refreshActivityTrackInfo(Track track) {
        if (track != null) {
            /*
            This logic makes sure, that if a track is being skipped by the user, it doesn't do this
            for eternity. Because a press of the next button would cause the AlbumArtSwipeAdapter
            to display a swipe to the next track, which would then cause another skipping to the
            next track. That's why we have to make a difference between a swipe by the user, and a
            programmatically called swipe.
            */
            mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);
            if (!mAlbumArtSwipeAdapter.isSwiped()) {
                mAlbumArtSwipeAdapter.setByUser(false);
                if (mPlaybackService.getCurrentPlaylist().getCurrentTrackIndex() >= 0) {
                    mAlbumArtSwipeAdapter.setCurrentItem(
                            mPlaybackService.getCurrentPlaylist().getCurrentTrackIndex(), true);
                }
                mAlbumArtSwipeAdapter.setByUser(true);
            }
            mAlbumArtSwipeAdapter.setSwiped(false);

            // Update all relevant TextViews
            final TextView artistTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_title);
            if (track.getArtist() != null && track.getArtist().getName() != null) {
                artistTextView.setText(track.getArtist().toString());
            } else {
                artistTextView.setText(R.string.playbackactivity_unknown_string);
            }
            if (track.getAlbum() != null && track.getAlbum().getName() != null) {
                albumTextView.setText(track.getAlbum().toString());
            } else {
                albumTextView.setText(R.string.playbackactivity_unknown_string);
            }
            if (track.getName() != null) {
                titleTextView.setText(track.getName());
            } else {
                titleTextView.setText(R.string.playbackactivity_unknown_string);
            }

            // Make all buttons clickable
            mPlaybackActivity.findViewById(R.id.imageButton_playpause).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_next).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_previous).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_shuffle).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_repeat).setClickable(true);

            // Update the PlaybackSeekBar
            mPlaybackSeekBar.setPlaybackService(mPlaybackService);
            mPlaybackSeekBar.setMax();
            mPlaybackSeekBar.setUpdateInterval();
            mPlaybackSeekBar.updateSeekBarPosition();
            mPlaybackSeekBar.updateTextViewCompleteTime();
        } else {
            //No track has been given, so we update the view state accordingly
            // Update all relevant TextViews
            final TextView artistTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) mPlaybackActivity
                    .findViewById(R.id.textView_title);
            artistTextView.setText("");
            albumTextView.setText("");
            titleTextView.setText(R.string.playbackactivity_no_track);

            // Make all buttons not clickable
            mPlaybackActivity.findViewById(R.id.imageButton_playpause).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_next).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_previous).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_shuffle).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_repeat).setClickable(false);

            // Update the PlaybackSeekBar
            mPlaybackSeekBar.setEnabled(false);
            mPlaybackSeekBar.updateSeekBarPosition();
            mPlaybackSeekBar.updateTextViewCompleteTime();
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current play/pause-button
     * state.
     */
    protected void refreshPlayPauseButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity
                .findViewById(R.id.imageButton_playpause);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.isPlaying()) {
                imageButton
                        .setImageDrawable(getResources().getDrawable(R.drawable.ic_player_pause));
            } else {
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_play));
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current repeatButton state.
     */
    protected void refreshRepeatButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity
                .findViewById(R.id.imageButton_repeat);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                    && mPlaybackService.getCurrentPlaylist().isRepeating()) {
                imageButton.getDrawable()
                        .setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                                PorterDuff.Mode.MULTIPLY);
            } else {
                imageButton.getDrawable().clearColorFilter();
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current shuffleButton state.
     */
    protected void refreshShuffleButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity
                .findViewById(R.id.imageButton_shuffle);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                    && mPlaybackService.getCurrentPlaylist().isShuffled()) {
                imageButton.getDrawable()
                        .setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                                PorterDuff.Mode.MULTIPLY);
            } else {
                imageButton.getDrawable().clearColorFilter();
            }
        }
    }
}
