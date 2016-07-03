/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment implements
        StickyListHeadersListView.OnHeaderClickListener {

    private static final String TAG = SocialActionsFragment.class.getSimpleName();

    public static final int SHOW_MODE_SOCIALACTIONS = 0;

    public static final int SHOW_MODE_DASHBOARD = 1;

    public final HashSet<Date> mResolvingPages = new HashSet<>();

    private List<User> mSuggestedUsers;

    private String mRandomUsersRequestId;

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (mRandomUsersRequestId != null
                && mRandomUsersRequestId.equals(event.mInfoRequestData.getRequestId())) {
            mSuggestedUsers = event.mInfoRequestData.getResultList(User.class);
        }

        super.onEvent(event);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUser == null) {
            return;
        }

        mHideRemoveButton = true;

        if (mShowMode == SHOW_MODE_DASHBOARD) {
            if (mContainerFragmentClass == null) {
                getActivity().setTitle(getString(R.string.drawer_title_feed).toUpperCase());
            }
            for (Date date : mUser.getFriendsFeed().keySet()) {
                String requestId = InfoSystem.get().resolveFriendsFeed(mUser, date);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    if (user.getFollowCount() >= 0 && user.getFollowCount() <= 5
                            && mRandomUsersRequestId == null) {
                        mRandomUsersRequestId = InfoSystem.get().getRandomUsers(5);
                        mCorrespondingRequestIds.add(mRandomUsersRequestId);
                    }
                }
            });
        } else {
            if (mContainerFragmentClass == null) {
                getActivity().setTitle("");
            }
            for (Date date : mUser.getSocialActions().keySet()) {
                String requestId = InfoSystem.get().resolveSocialActions(mUser, date);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        }
        getListView().setOnHeaderClickListener(this);
        updateAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();

        mResolvingPages.clear();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param o    the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object o, Segment segment) {
        if (getMediaController() == null) {
            Log.e(TAG, "onItemClick failed because getMediaController() is null");
            return;
        }
        final TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        Object item = o;
        if (o instanceof SocialAction) {
            item = ((SocialAction) o).getTargetObject();
        }
        Bundle bundle = new Bundle();
        if (item instanceof User) {
            bundle.putString(TomahawkFragment.USER, ((User) item).getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_USER);
            FragmentUtils.replace(activity, UserPagerFragment.class, bundle);
        } else if (item instanceof Query && o instanceof SocialAction) {
            PlaylistEntry entry = mUser.getPlaylistEntry((SocialAction) o);
            if (entry.getQuery().isPlayable()) {
                if (getPlaybackManager().getCurrentEntry() == entry) {
                    // if the user clicked on an already playing track
                    int playState = getMediaController().getPlaybackState().getState();
                    if (playState == PlaybackStateCompat.STATE_PLAYING) {
                        getMediaController().getTransportControls().pause();
                    } else if (playState == PlaybackStateCompat.STATE_PAUSED) {
                        getMediaController().getTransportControls().play();
                    }
                } else {
                    Playlist playlist;
                    if (mShowMode == SHOW_MODE_SOCIALACTIONS) {
                        playlist = mUser.getSocialActionsPlaylist();
                    } else {
                        playlist = mUser.getFriendsFeedPlaylist();
                    }
                    getPlaybackManager().setPlaylist(playlist, entry);
                    getMediaController().getTransportControls().play();
                }
            }
        } else if (item instanceof Album) {
            bundle.putString(TomahawkFragment.ALBUM, ((Album) item).getCacheKey());
            bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils.replace(activity, PlaylistEntriesFragment.class, bundle);
        } else if (item instanceof Artist) {
            bundle.putString(TomahawkFragment.ARTIST, ((Artist) item).getCacheKey());
            bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
            bundle.putLong(CONTAINER_FRAGMENT_ID,
                    IdGenerator.getSessionUniqueId());
            FragmentUtils.replace(activity, ArtistPagerFragment.class, bundle);
        } else if (item instanceof Playlist) {
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            bundle.putString(TomahawkFragment.PLAYLIST, ((Playlist) item).getCacheKey());
            FragmentUtils.replace(activity, PlaylistEntriesFragment.class, bundle);
        }
    }

    @Override
    public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition,
            long headerId, boolean currentlySticky) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        Object item = getListAdapter().getItem(itemPosition);
        if (item instanceof List && !((List) item).isEmpty()) {
            item = ((List) item).get(0);
        }
        if (item instanceof SocialAction) {
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.USER,
                    ((SocialAction) item).getUser().getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_USER);
            FragmentUtils.replace(activity, UserPagerFragment.class, bundle);
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
        if (mShowMode == SHOW_MODE_DASHBOARD && !getResources().getBoolean(R.bool.is_landscape)) {
            setAreHeadersSticky(true);
        }
        TomahawkRunnable r = new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
            @Override
            public void run() {
                if (mUser != null) {
                    List<Segment> segments = new ArrayList<>();
                    int extraPadding = TomahawkApp.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.padding_medium)
                            + ViewUtils.convertDpToPixel(32);
                    if (mSuggestedUsers != null) {
                        List<Object> suggestions = new ArrayList<>();
                        suggestions.addAll(mSuggestedUsers);
                        Segment segment = new Segment.Builder(suggestions)
                                .headerLayout(R.layout.list_header_socialaction_fake)
                                .headerString(TomahawkApp.getContext().getString(
                                        R.string.suggest_users) + ":")
                                .leftExtraPadding(extraPadding)
                                .build();
                        segments.add(segment);
                    }
                    TreeMap<Date, List<SocialAction>> socialActionsList;
                    if (mShowMode == SHOW_MODE_DASHBOARD) {
                        socialActionsList = mUser.getFriendsFeed();
                    } else {
                        socialActionsList = mUser.getSocialActions();
                    }
                    List<List<SocialAction>> mergedActions = mergeSocialActions(socialActionsList);
                    for (List<SocialAction> actions : mergedActions) {
                        Segment segment = toSegment(actions);
                        segments.add(segment);
                    }
                    fillAdapter(segments);
                }
            }
        };
        ThreadManager.get().execute(r);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        if (mUser != null && firstVisibleItem + visibleItemCount + 5 > totalItemCount) {
            mShowMode = getArguments().getInt(SHOW_MODE);
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                if (!mResolvingPages.contains(mUser.getFriendsFeedNextDate())) {
                    mResolvingPages.add(mUser.getFriendsFeedNextDate());
                    String requestId = InfoSystem.get()
                            .resolveFriendsFeed(mUser, mUser.getFriendsFeedNextDate());
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            } else {
                if (!mResolvingPages.contains(mUser.getSocialActionsNextDate())) {
                    mResolvingPages.add(mUser.getSocialActionsNextDate());
                    String requestId = InfoSystem.get()
                            .resolveSocialActions(mUser, mUser.getSocialActionsNextDate());
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
        }
    }

    public static List<List<SocialAction>> mergeSocialActions(
            TreeMap<Date, List<SocialAction>> actions) {
        List<List<SocialAction>> mergedActionsList = new ArrayList<>();
        for (List<SocialAction> socialActions : actions.descendingMap().values()) {
            mergedActionsList.addAll(mergeSocialActions(socialActions));
        }
        return mergedActionsList;
    }

    public static List<List<SocialAction>> mergeSocialActions(List<SocialAction> actions) {
        List<List<SocialAction>> mergedActionsList = new ArrayList<>();
        Set<SocialAction> checkedActions = new HashSet<>();
        for (SocialAction socialAction : actions) {
            if (!checkedActions.contains(socialAction)
                    && shouldDisplayAction(socialAction)) {
                List<SocialAction> mergedActions = new ArrayList<>();
                mergedActions.add(socialAction);
                checkedActions.add(socialAction);
                for (SocialAction actionToCompare : actions) {
                    if (!checkedActions.contains(actionToCompare)
                            && shouldMergeAction(actionToCompare, socialAction)) {
                        mergedActions.add(actionToCompare);
                        checkedActions.add(actionToCompare);
                    }
                }
                mergedActionsList.add(mergedActions);
            }
        }
        return mergedActionsList;
    }

    private static boolean shouldDisplayAction(SocialAction socialAction) {
        boolean action = Boolean.valueOf(socialAction.getAction());
        String type = socialAction.getType();
        return HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST.equals(type)
                || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON.equals(type)
                || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW.equals(type)
                || (action && HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE.equals(type));
    }

    private static boolean shouldMergeAction(SocialAction actionToCompare,
            SocialAction socialAction) {
        return actionToCompare.getUser() == socialAction.getUser()
                && actionToCompare.getType().equals(socialAction.getType())
                && actionToCompare.getTargetObject().getClass()
                == socialAction.getTargetObject().getClass();
    }

    private Segment toSegment(List<SocialAction> actions) {
        SocialAction first = actions.get(0);
        Segment.Builder builder;
        if (first.getTargetObject() instanceof Album
                || first.getTargetObject() instanceof User
                || first.getTargetObject() instanceof Artist
                || first.getTargetObject() instanceof Playlist) {
            builder = new Segment.Builder(actions)
                    .headerLayout(R.layout.list_header_socialaction)
                    .showAsGrid(R.integer.grid_column_count_feed,
                            R.dimen.padding_superlarge, R.dimen.padding_small);
        } else {
            builder = new Segment.Builder(actions)
                    .headerLayout(R.layout.list_header_socialaction);
        }
        int extraPadding = TomahawkApp.getContext().getResources()
                .getDimensionPixelSize(R.dimen.padding_medium)
                + ViewUtils.convertDpToPixel(32);
        builder.leftExtraPadding(extraPadding);
        return builder.build();
    }
}
