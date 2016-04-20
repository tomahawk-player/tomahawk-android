/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Enno Gottschalk <mrmaffen@googlemail.com> in 2016
 */

package org.tomahawk.tomahawk_android.utils;

import org.apache.lucene.util.ArrayUtil;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. Maintaining
 * a visible notification (usually) guarantees that the music service won't be killed during
 * playback.
 */
public class MediaNotification {

    private static final String TAG = MediaNotification.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;

    public static final String ACTION_FAVORITE = "org.tomahawk.tomahawk_android.favorite";

    public static final String ACTION_UNFAVORITE = "org.tomahawk.tomahawk_android.unfavorite";

    public static final String ACTION_PAUSE = "org.tomahawk.tomahawk_android.pause";

    public static final String ACTION_PLAY = "org.tomahawk.tomahawk_android.play";

    public static final String ACTION_PREV = "org.tomahawk.tomahawk_android.prev";

    public static final String ACTION_NEXT = "org.tomahawk.tomahawk_android.next";

    private final PlaybackService mService;

    private MediaSessionCompat.Token mSessionToken;

    private MediaControllerCompat mController;

    private MediaControllerCompat.TransportControls mTransportControls;

    private final SparseArray<PendingIntent> mIntents = new SparseArray<>();

    private PlaybackStateCompat mPlaybackState;

    private MediaMetadataCompat mMetadata;

    private NotificationCompat.Builder mNotificationBuilder;

    private NotificationManagerCompat mNotificationManager;

    private NotificationCompat.Action mPlayPauseAction;

    private NotificationCompat.Action mFavoriteAction;

    private int mNotificationColor;

    private boolean mStarted = false;

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state");
            if (mStarted) {
                updateNotificationPlaybackState();
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            } else {
                Log.d(TAG, "Couldn't update playback state because notification is stopped");
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            Log.d(TAG, "Received new metadata ");
            if (mStarted) {
                updateNotificationMetadata();
            } else {
                Log.d(TAG, "Couldn't update playback state because notification is stopped");
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                Log.e(TAG, "Could not connect to media controller: ", e);
            }
        }
    };

    private BroadcastReceiver mActionReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Received intent with action " + action);
            if (ACTION_FAVORITE.equals(action)) {
                mTransportControls.setRating(RatingCompat.newHeartRating(true));
            } else if (ACTION_UNFAVORITE.equals(action)) {
                mTransportControls.setRating(RatingCompat.newHeartRating(false));
            } else if (ACTION_PAUSE.equals(action)) {
                mTransportControls.pause();
            } else if (ACTION_PLAY.equals(action)) {
                mTransportControls.play();
            } else if (ACTION_NEXT.equals(action)) {
                mTransportControls.skipToNext();
            } else if (ACTION_PREV.equals(action)) {
                mTransportControls.skipToPrevious();
            }
        }
    };

    public MediaNotification(PlaybackService service) throws RemoteException {
        mService = service;
        updateSessionToken();

        mNotificationColor = mService.getResources().getColor(R.color.notification_bg);

        mNotificationManager = NotificationManagerCompat.from(mService);

        String pkg = mService.getPackageName();
        mIntents.put(R.drawable.ic_action_favorites_small, PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_FAVORITE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_action_favorites_small_underlined,
                PendingIntent.getBroadcast(mService, 100, new Intent(ACTION_UNFAVORITE)
                        .setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_av_pause, PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_av_play_arrow, PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_player_previous_light, PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_player_next_light, PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
    }

    /**
     * Posts the notification and starts tracking the session to keep it updated. The notification
     * will automatically be removed if the session is destroyed before {@link #stopNotification} is
     * called.
     */
    public void startNotification() {
        if (!mStarted) {
            mController.registerCallback(mCallback, mService.getCallbackHandler());
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_FAVORITE);
            filter.addAction(ACTION_UNFAVORITE);
            filter.addAction(ACTION_NEXT);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_PREV);
            mService.registerReceiver(mActionReceiver, filter);

            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            mStarted = true;
            // The notification must be updated after setting started to true
            updateNotificationMetadata();
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session was destroyed this
     * has no effect.
     */
    public void stopNotification() {
        mStarted = false;
        mController.unregisterCallback(mCallback);
        try {
            mService.unregisterReceiver(mActionReceiver);
        } catch (IllegalArgumentException ex) {
            // ignore if the receiver is not registered.
        }
        mService.stopForeground(true);
    }

    /**
     * Update the state based on a change on the session token. Called either when we are running
     * for the first time or when the media session owner has destroyed the session (see {@link
     * android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCallback);
            }
            mSessionToken = freshToken;
            if (mSessionToken != null) {
                mController = new MediaControllerCompat(mService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCallback, mService.getCallbackHandler());
                }
            }
        }
    }

    private void updateNotificationMetadata() {
        Log.d(TAG, "updateNotificationMetadata. mMetadata=" + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        mNotificationBuilder = new NotificationCompat.Builder(mService);

        List<Integer> showInCompact = new ArrayList<>();

        updateFavoriteAction();
        showInCompact.add(mNotificationBuilder.mActions.size());
        mNotificationBuilder.addAction(mFavoriteAction);

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_player_previous_light,
                    mService.getString(R.string.playback_previous),
                    mIntents.get(R.drawable.ic_player_previous_light)).build();
            mNotificationBuilder.addAction(action);
        }

        updatePlayPauseAction();
        showInCompact.add(mNotificationBuilder.mActions.size());
        mNotificationBuilder.addAction(mPlayPauseAction);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_player_next_light,
                    mService.getString(R.string.playback_next),
                    mIntents.get(R.drawable.ic_player_next_light)).build();
            showInCompact.add(mNotificationBuilder.mActions.size());
            mNotificationBuilder.addAction(action);
        }

        MediaDescriptionCompat description = mMetadata.getDescription();

        Bitmap art = description.getIconBitmap();
        if (art == null) {
            // use a placeholder art while the remote art is being downloaded
            art = BitmapFactory.decodeResource(mService.getResources(),
                    R.drawable.album_placeholder_grid);
        }

        mNotificationBuilder
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(ArrayUtil.toIntArray(showInCompact))
                        .setMediaSession(mSessionToken)
                        .setShowCancelButton(true))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setTicker(description.getTitle() + " - " + description.getSubtitle())
                .setLargeIcon(art)
                .setOngoing(false);

        updateNotificationPlaybackState();

        mService.startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void updateFavoriteAction() {
        Log.d(TAG, "updateFavoriteAction");
        String favoriteLabel;
        int favoriteIcon;
        RatingCompat rating = mMetadata.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING);
        if (rating != null && rating.hasHeart()) {
            favoriteLabel = mService.getString(R.string.playback_unfavorite);
            favoriteIcon = R.drawable.ic_action_favorites_small_underlined;
        } else {
            favoriteLabel = mService.getString(R.string.playback_favorite);
            favoriteIcon = R.drawable.ic_action_favorites_small;
        }
        if (mFavoriteAction == null) {
            mFavoriteAction = new NotificationCompat.Action.Builder(favoriteIcon,
                    favoriteLabel, mIntents.get(favoriteIcon)).build();
        } else {
            mFavoriteAction.icon = favoriteIcon;
            mFavoriteAction.title = favoriteLabel;
            mFavoriteAction.actionIntent = mIntents.get(favoriteIcon);
        }
    }

    private void updatePlayPauseAction() {
        Log.d(TAG, "updatePlayPauseAction");
        String playPauseLabel;
        int playPauseIcon;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            playPauseLabel = mService.getString(R.string.playback_pause);
            playPauseIcon = R.drawable.ic_av_pause;
        } else {
            playPauseLabel = mService.getString(R.string.playback_play);
            playPauseIcon = R.drawable.ic_av_play_arrow;
        }
        if (mPlayPauseAction == null) {
            mPlayPauseAction = new NotificationCompat.Action.Builder(playPauseIcon,
                    playPauseLabel, mIntents.get(playPauseIcon)).build();
        } else {
            mPlayPauseAction.icon = playPauseIcon;
            mPlayPauseAction.title = playPauseLabel;
            mPlayPauseAction.actionIntent = mIntents.get(playPauseIcon);
        }
    }

    private void updateNotificationPlaybackState() {
        Log.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            Log.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (mNotificationBuilder == null) {
            Log.d(TAG, "updateNotificationPlaybackState. there is no notificationBuilder. "
                    + "Ignoring request to update state!");
            return;
        }
        if (mPlaybackState.getPosition() >= 0 &&
                mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            Log.d(TAG, "updateNotificationPlaybackState. updating playback position to " +
                    (System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000
                    + " seconds");
            mNotificationBuilder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            Log.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            mNotificationBuilder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        updatePlayPauseAction();

        // Make sure that the notification can be dismissed by the user when we are not playing:
        mNotificationBuilder
                .setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED) {
            mService.stopForeground(false);
        }
    }

}
