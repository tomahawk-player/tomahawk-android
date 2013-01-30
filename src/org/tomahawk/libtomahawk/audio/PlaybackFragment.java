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
package org.tomahawk.libtomahawk.audio;

import org.tomahawk.libtomahawk.Track;
import org.tomahawk.tomahawk_android.R;

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

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 05.01.13
 */
public class PlaybackFragment extends SherlockFragment {

    private PlaybackService mPlaybackService;
    private PlaybackActivity mPlaybackActivity;

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;
    private PlaybackSeekBar mPlaybackSeekBar;
    private Toast mToast;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PlaybackActivity) {
            mPlaybackActivity = (PlaybackActivity) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPlaybackActivity = null;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, null, false);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void init() {
        if (getView().getParent() != null) {
            ViewPager viewPager = (ViewPager) getView().findViewById(R.id.album_art_view_pager);
            mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(mPlaybackActivity, viewPager);
            mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);

            mPlaybackSeekBar = (PlaybackSeekBar) getView().findViewById(R.id.seekBar_track);
            mPlaybackSeekBar.setTextViewCurrentTime((TextView) mPlaybackActivity.findViewById(R.id.textView_currentTime));
            mPlaybackSeekBar.setTextViewCompletionTime((TextView) mPlaybackActivity.findViewById(R.id.textView_completionTime));
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
        if (mPlaybackService != null)
            mPlaybackService.playPause();
    }

    /** play the next track and set the playbutton to pause icon */
    public void onNextClicked() {
        if (mAlbumArtSwipeAdapter != null)
            mAlbumArtSwipeAdapter.setSwiped(false);
        if (mPlaybackService != null)
            mPlaybackService.next();
    }

    /** play the previous track and set the playbutton to pause icon */
    public void onPreviousClicked() {
        if (mAlbumArtSwipeAdapter != null)
            mAlbumArtSwipeAdapter.setSwiped(false);
        if (mPlaybackService != null)
            mPlaybackService.previous();
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        if (mPlaybackService != null) {
            mPlaybackService.setShuffled(!mPlaybackService.getCurrentPlaylist().isShuffled());

            if (mToast != null)
                mToast.cancel();
            if (mPlaybackService != null) {
                mToast = Toast.makeText(getSherlockActivity(),
                        getString(mPlaybackService.getCurrentPlaylist().isShuffled()
                                ? R.string.playbackactivity_toastshuffleon_string
                                : R.string.playbackactivity_toastshuffleoff_string), Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        if (mPlaybackService != null) {
            mPlaybackService.setRepeating(!mPlaybackService.getCurrentPlaylist().isRepeating());

            if (mToast != null)
                mToast.cancel();
            if (mPlaybackService != null) {
                mToast = Toast.makeText(getSherlockActivity(),
                        getString(mPlaybackService.getCurrentPlaylist().isRepeating()
                                ? R.string.playbackactivity_toastrepeaton_string
                                : R.string.playbackactivity_toastrepeatoff_string), Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
    }

    /** Called when the PlaybackService signals the current Track has changed. */
    protected void onTrackChanged() {
        refreshActivityTrackInfo();
    }

    /**
     * Called when the PlaybackService signals the current Playlist has changed.
     */
    protected void onPlaylistChanged() {
        if (mAlbumArtSwipeAdapter != null)
            mAlbumArtSwipeAdapter.updatePlaylist();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
    }

    /**
     * Called when the PlaybackService signals the current Playstate has
     * changed.
     */
    protected void onPlaystateChanged() {
        refreshPlayPauseButtonState();
        if (mPlaybackSeekBar != null)
            mPlaybackSeekBar.updateSeekBarPosition();
    }

    public void setPlaybackService(PlaybackService ps) {
        if (mPlaybackService != ps) {
            mPlaybackService = ps;
            if (mAlbumArtSwipeAdapter != null && mPlaybackActivity != null && mPlaybackSeekBar != null) {
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
     * Refresh the information in this activity to reflect that of the current
     * Track, if possible (meaning mPlaybackService is not null).
     */
    protected void refreshActivityTrackInfo() {
        if (mPlaybackService != null && mAlbumArtSwipeAdapter != null && mPlaybackActivity != null
                && mPlaybackSeekBar != null)
            refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
        else
            refreshActivityTrackInfo(null);
    }

    /**
     * Refresh the information in this activity to reflect that of the given
     * Track.
     */
    protected void refreshActivityTrackInfo(Track track) {
        if (track != null) {
            mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);
            if (!mAlbumArtSwipeAdapter.isSwiped()) {
                mAlbumArtSwipeAdapter.setByUser(false);
                if (mPlaybackService.getCurrentPlaylist().getPosition() >= 0)
                    mAlbumArtSwipeAdapter.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition(), true);
                mAlbumArtSwipeAdapter.setByUser(true);
            }
            mAlbumArtSwipeAdapter.setSwiped(false);
            final TextView artistTextView = (TextView) mPlaybackActivity.findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) mPlaybackActivity.findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) mPlaybackActivity.findViewById(R.id.textView_title);
            if (track.getArtist() != null && track.getArtist().getName() != null)
                artistTextView.setText(track.getArtist().toString());
            else
                artistTextView.setText(R.string.playbackactivity_unknown_string);
            if (track.getAlbum() != null && track.getAlbum().getName() != null)
                albumTextView.setText(track.getAlbum().toString());
            else
                albumTextView.setText(R.string.playbackactivity_unknown_string);
            if (track.getName() != null)
                titleTextView.setText(track.getName().toString());
            else
                titleTextView.setText(R.string.playbackactivity_unknown_string);
            mPlaybackActivity.findViewById(R.id.imageButton_playpause).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_next).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_previous).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_shuffle).setClickable(true);
            mPlaybackActivity.findViewById(R.id.imageButton_repeat).setClickable(true);
            mPlaybackSeekBar.setPlaybackService(mPlaybackService);
            mPlaybackSeekBar.setMax();
            mPlaybackSeekBar.setUpdateInterval();
            mPlaybackSeekBar.updateSeekBarPosition();
            mPlaybackSeekBar.updateTextViewCompleteTime();
            mPlaybackSeekBar.updateTextViewCurrentTime();
        } else {
            mPlaybackActivity.findViewById(R.id.imageButton_playpause).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_next).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_previous).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_shuffle).setClickable(false);
            mPlaybackActivity.findViewById(R.id.imageButton_repeat).setClickable(false);
            mPlaybackSeekBar.setEnabled(false);
        }
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * play/pause-button state.
     */
    protected void refreshPlayPauseButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity.findViewById(R.id.imageButton_playpause);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.isPlaying())
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_pause));
            else
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_play));
        }
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * repeatButton state.
     */
    protected void refreshRepeatButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity.findViewById(R.id.imageButton_repeat);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                    && mPlaybackService.getCurrentPlaylist().isRepeating())
                imageButton.getDrawable().setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                        PorterDuff.Mode.MULTIPLY);
            else
                imageButton.getDrawable().clearColorFilter();
        }
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * shuffleButton state.
     */
    protected void refreshShuffleButtonState() {
        ImageButton imageButton = (ImageButton) mPlaybackActivity.findViewById(R.id.imageButton_shuffle);
        if (imageButton != null) {
            if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                    && mPlaybackService.getCurrentPlaylist().isShuffled())
                imageButton.getDrawable().setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                        PorterDuff.Mode.MULTIPLY);
            else
                imageButton.getDrawable().clearColorFilter();
        }
    }
}
