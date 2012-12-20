/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
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

import java.io.IOException;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.libtomahawk.playlist.AlbumPlaylist;
import org.tomahawk.libtomahawk.playlist.ArtistPlaylist;
import org.tomahawk.libtomahawk.playlist.CollectionPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.SearchableActivity;
import org.tomahawk.tomahawk_android.SettingsActivity;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends SherlockActivity implements PlaybackServiceConnectionListener {

    private static final String TAG = PlaybackActivity.class.getName();

    private PlaybackService mPlaybackService;
    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;
    /** Allow communication to the PlaybackService. */
    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(this);

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;
    private PlaybackSeekBar mPlaybackSeekBar;
    private Toast mToast;

    /** Identifier for passing a Track as an extra in an Intent. */
    public static final String PLAYLIST_EXTRA = "playlist_extra";

    public static final String PLAYLIST_ALBUM_ID = "playlist_album_id";

    public static final String PLAYLIST_ARTIST_ID = "playlist_artist_id";

    public static final String PLAYLIST_TRACK_ID = "playlist_track_id";

    public static final String PLAYLIST_COLLECTION_ID = "playlist_collection_id";

    public static final String PLAYLIST_PLAYLIST_ID = "playlist_playlist_id";

    private class PlaybackServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK))
                onTrackChanged();
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYLISTCHANGED))
                onPlaylistChanged();
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYSTATECHANGED))
                onPlaystateChanged();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        View view = getLayoutInflater().inflate(R.layout.playback_activity, null);
        setContentView(view);

        ViewPager viewPager = (ViewPager) findViewById(R.id.album_art_view_pager);
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(getApplicationContext(), viewPager);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);

        mPlaybackSeekBar = (PlaybackSeekBar) findViewById(R.id.seekBar_track);
        mPlaybackSeekBar.setPlaybackService(mPlaybackService);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        mPlaybackSeekBar.setTextViewCurrentTime((TextView) findViewById(R.id.textView_currentTime));
        mPlaybackSeekBar.setTextViewCompletionTime((TextView) findViewById(R.id.textView_completionTime));
        if (mPlaybackServiceBroadcastReceiver == null)
            mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();

        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        startService(playbackIntent);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);

        refreshPlayPauseButtonState();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mPlaybackService != null)
            unbindService(mPlaybackServiceConnection);
        if (mPlaybackServiceBroadcastReceiver != null) {
            unregisterReceiver(mPlaybackServiceBroadcastReceiver);
            mPlaybackServiceBroadcastReceiver = null;
        }
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android
     * .view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.action_search_item) {
                Intent searchIntent = new Intent(this, SearchableActivity.class);
                startActivity(searchIntent);
                return true;
            } else if (item.getItemId() == R.id.action_settings_item) {
                Intent searchIntent = new Intent(this, SettingsActivity.class);
                startActivity(searchIntent);
                return true;
            } else if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the play/pause button is clicked.
     * 
     * @param view
     */
    public void onPlayPauseClicked(View view) {
        Log.d(TAG, "onPlayPauseClicked");

        mPlaybackService.playPause();
        refreshPlayPauseButtonState();
        mPlaybackSeekBar.updateSeekBarPosition();
    }

    /**
     * Called when the next button is clicked.
     * 
     * @param view
     */
    public void onNextClicked(View view) {
        nextTrack();
    }

    /** play the next track and set the playbutton to pause icon */
    public void nextTrack() {
        mAlbumArtSwipeAdapter.setSwiped(false);
        mPlaybackService.next();
    }

    /**
     * Called when the previous button is clicked.
     * 
     * @param view
     */
    public void onPreviousClicked(View view) {
        previousTrack();
    }

    /** play the previous track and set the playbutton to pause icon */
    public void previousTrack() {
        mAlbumArtSwipeAdapter.setSwiped(false);
        mPlaybackService.previous();
    }

    /**
     * Called when the shuffle button is clicked.
     * 
     * @param view
     */
    public void onShuffleClicked(View view) {
        mPlaybackService.getCurrentPlaylist().setShuffled(!mPlaybackService.getCurrentPlaylist().isShuffled());
        onPlaylistChanged();
        refreshShuffleButtonState();
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(getApplicationContext(), getString(mPlaybackService.getCurrentPlaylist().isShuffled()
                ? R.string.playbackactivity_toastshuffleon_string : R.string.playbackactivity_toastshuffleoff_string),
                Toast.LENGTH_SHORT);
        mToast.show();
    }

    /**
     * Called when the repeat button is clicked.
     * 
     * @param view
     */
    public void onRepeatClicked(View view) {
        mPlaybackService.getCurrentPlaylist().setRepeating(!mPlaybackService.getCurrentPlaylist().isRepeating());
        onPlaylistChanged();
        refreshRepeatButtonState();
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(getApplicationContext(), getString(mPlaybackService.getCurrentPlaylist().isRepeating()
                ? R.string.playbackactivity_toastrepeaton_string : R.string.playbackactivity_toastrepeatoff_string),
                Toast.LENGTH_SHORT);
        mToast.show();
    }

    /** Called when the PlaybackService signals the current Track has changed. */
    protected void onTrackChanged() {
        refreshActivityTrackInfo();
    }

    /**
     * Called when the PlaybackService signals the current Playlist has changed.
     */
    protected void onPlaylistChanged() {
        mAlbumArtSwipeAdapter.updatePlaylist();
    }

    /**
     * Called when the PlaybackService signals the current Playstate has
     * changed.
     */
    protected void onPlaystateChanged() {
        refreshPlayPauseButtonState();
    }

    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    /**
     * Called when the playback service is ready.
     */
    @Override
    public void onPlaybackServiceReady() {
        if (getIntent().hasExtra(PLAYLIST_EXTRA)) {

            Bundle playlistBundle = getIntent().getBundleExtra(PLAYLIST_EXTRA);
            getIntent().removeExtra(PLAYLIST_EXTRA);

            long trackid = playlistBundle.getLong(PLAYLIST_TRACK_ID);

            Playlist playlist = null;
            if (playlistBundle.containsKey(PLAYLIST_ALBUM_ID)) {
                long albumid = playlistBundle.getLong(PLAYLIST_ALBUM_ID);
                playlist = AlbumPlaylist.fromAlbum(Album.get(albumid), Track.get(trackid));
            } else if (playlistBundle.containsKey(PLAYLIST_COLLECTION_ID)) {
                int collid = playlistBundle.getInt(PLAYLIST_COLLECTION_ID);
                TomahawkApp app = (TomahawkApp) getApplication();
                playlist = CollectionPlaylist.fromCollection(app.getSourceList().getCollectionFromId(collid),
                        Track.get(trackid));
            } else if (playlistBundle.containsKey(PLAYLIST_ARTIST_ID)) {
                long artistid = playlistBundle.getLong(PLAYLIST_ARTIST_ID);
                playlist = ArtistPlaylist.fromArtist(Artist.get(artistid));
            }
            else if (playlistBundle.containsKey(PLAYLIST_PLAYLIST_ID)) {
                long playlistid = playlistBundle.getLong(PLAYLIST_PLAYLIST_ID);
                //playlist = PlaylistPlaylist.fromPlaylist(PlaylistDummy.get(playlistid));
            }
            try {
                mPlaybackService.setCurrentPlaylist(playlist);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mAlbumArtSwipeAdapter.setPlaybackService(mPlaybackService);
        refreshActivityTrackInfo();
        refreshPlayPauseButtonState();
        refreshRepeatButtonState();
        refreshShuffleButtonState();

    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * Track, if possible (meaning mPlaybackService is not null).
     */
    private void refreshActivityTrackInfo() {
        if (mPlaybackService != null)
            refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
        else
            refreshActivityTrackInfo(null);
    }

    /**
     * Refresh the information in this activity to reflect that of the given
     * Track.
     */
    private void refreshActivityTrackInfo(Track track) {
        if (track != null) {
            if (!mAlbumArtSwipeAdapter.isSwiped()) {
                mAlbumArtSwipeAdapter.setByUser(false);
                if (mPlaybackService.getCurrentPlaylist().getPosition() >= 0)
                    mAlbumArtSwipeAdapter.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition(), true);
                mAlbumArtSwipeAdapter.setByUser(true);
            }
            mAlbumArtSwipeAdapter.setSwiped(false);
            final TextView artistTextView = (TextView) findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) findViewById(R.id.textView_title);
            artistTextView.setText(track.getArtist().toString());
            albumTextView.setText(track.getAlbum().toString());
            titleTextView.setText(track.getName().toString());
            findViewById(R.id.imageButton_playpause).setClickable(true);
            findViewById(R.id.imageButton_next).setClickable(true);
            findViewById(R.id.imageButton_previous).setClickable(true);
            findViewById(R.id.imageButton_shuffle).setClickable(true);
            findViewById(R.id.imageButton_repeat).setClickable(true);
            mPlaybackSeekBar.setPlaybackService(mPlaybackService);
            mPlaybackSeekBar.setMax((int) mPlaybackService.getCurrentTrack().getDuration());
            mPlaybackSeekBar.updateSeekBarPosition();
            mPlaybackSeekBar.updateTextViewCompleteTime();
            mPlaybackSeekBar.updateTextViewCurrentTime();
            // Update the progressbar the next second
            mPlaybackSeekBar.getUiHandler().sendEmptyMessageDelayed(PlaybackSeekBar.getMsgUpdateProgress(), 1000);
        } else {
            findViewById(R.id.imageButton_playpause).setClickable(false);
            findViewById(R.id.imageButton_next).setClickable(false);
            findViewById(R.id.imageButton_previous).setClickable(false);
            findViewById(R.id.imageButton_shuffle).setClickable(false);
            findViewById(R.id.imageButton_repeat).setClickable(false);
        }
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * play/pause-button state.
     */
    private void refreshPlayPauseButtonState() {
        ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        if (mPlaybackService != null && mPlaybackService.isPlaying())
            button.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_pause));
        else
            button.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_play));
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * repeatButton state.
     */
    private void refreshRepeatButtonState() {
        ImageButton imageButton = (ImageButton) findViewById(R.id.imageButton_repeat);
        if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                && mPlaybackService.getCurrentPlaylist().isRepeating())
            imageButton.getDrawable().setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                    PorterDuff.Mode.MULTIPLY);
        else
            imageButton.getDrawable().clearColorFilter();
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * shuffleButton state.
     */
    private void refreshShuffleButtonState() {
        ImageButton imageButton = (ImageButton) findViewById(R.id.imageButton_shuffle);
        if (mPlaybackService != null && mPlaybackService.getCurrentPlaylist() != null
                && mPlaybackService.getCurrentPlaylist().isShuffled())
            imageButton.getDrawable().setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                    PorterDuff.Mode.MULTIPLY);
        else
            imageButton.getDrawable().clearColorFilter();
    }
}
