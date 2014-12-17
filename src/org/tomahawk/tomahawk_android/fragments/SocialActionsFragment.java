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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment implements
        StickyListHeadersListView.OnHeaderClickListener {

    public static final int SHOW_MODE_SOCIALACTIONS = 0;

    public static final int SHOW_MODE_DASHBOARD = 1;

    public HashSet<Integer> mResolvingPages = new HashSet<Integer>();

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
                if (mShowMode == SHOW_MODE_DASHBOARD) {
                    if (mContainerFragmentClass == null) {
                        getActivity().setTitle(getString(R.string.drawer_title_feed).toUpperCase());
                    }
                    for (int i = 0; i < mUser.getFriendsFeed().size(); i++) {
                        mCurrentRequestIds.add(
                                InfoSystem.getInstance().resolveFriendsFeed(mUser, i));
                    }
                } else {
                    if (mContainerFragmentClass == null) {
                        getActivity().setTitle("");
                    }
                    for (int i = 0; i < mUser.getSocialActions().size(); i++) {
                        mCurrentRequestIds.add(
                                InfoSystem.getInstance().resolveSocialActions(mUser, i));
                    }
                    HatchetAuthenticatorUtils authUtils = (HatchetAuthenticatorUtils)
                            AuthenticatorManager.getInstance().getAuthenticatorUtils(
                                    TomahawkApp.PLUGINNAME_HATCHET);
                    mCurrentRequestIds.add(InfoSystem.getInstance()
                            .resolveFollowings(authUtils.getLoggedInUser()));
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
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        Bundle bundle = new Bundle();
        if (item instanceof User) {
            bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, ((User) item).getId());
            bundle.putInt(ContentHeaderFragment.MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_USER);
            FragmentUtils.replace(activity, UserPagerFragment.class, bundle);
        } else if (item instanceof Query && ((Query) item).isPlayable()) {
            Query query = (Query) item;
            ArrayList<Query> queries = new ArrayList<Query>();
            queries.addAll(mShownQueries);
            PlaybackService playbackService = activity.getPlaybackService();
            if (playbackService != null && playbackService.getCurrentQuery() == query) {
                playbackService.playPause();
            } else {
                Playlist playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME,
                        queries);
                if (playbackService != null) {
                    playbackService.setPlaylist(playlist, playlist.getEntryWithQuery(query));
                    Class clss = mContainerFragmentClass != null ? mContainerFragmentClass
                            : ((Object) this).getClass();
                    playbackService.setReturnFragment(clss, getArguments());
                    playbackService.start();
                }
            }
        } else if (item instanceof Album) {
            bundle.putString(TomahawkFragment.TOMAHAWK_ALBUM_KEY, ((Album) item).getCacheKey());
            bundle.putString(CollectionManager.COLLECTION_ID, mCollection.getId());
            bundle.putInt(ContentHeaderFragment.MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils.replace(activity, TracksFragment.class, bundle);
        } else if (item instanceof Artist) {
            bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, ((Artist) item).getCacheKey());
            bundle.putString(CollectionManager.COLLECTION_ID, mCollection.getId());
            bundle.putInt(ContentHeaderFragment.MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
            bundle.putLong(ContentHeaderFragment.CONTAINER_FRAGMENT_ID,
                    TomahawkMainActivity.getSessionUniqueId());
            FragmentUtils.replace(activity, ArtistPagerFragment.class, bundle);
        } else if (item instanceof Playlist) {
            bundle.putInt(ContentHeaderFragment.MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            bundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY, ((Playlist) item).getId());
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
            bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID,
                    ((SocialAction) item).getUser().getId());
            bundle.putInt(ContentHeaderFragment.MODE,
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

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        mShownQueries.clear();
        if (mUser != null) {
            SparseArray<List<SocialAction>> socialActionsList;
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                socialActionsList = mUser.getFriendsFeed();
            } else {
                socialActionsList = mUser.getSocialActions();
            }
            List<List<TomahawkListItem>> mergedActionsList
                    = new ArrayList<List<TomahawkListItem>>();
            int i = 0;
            while (socialActionsList.get(i) != null) {
                List<SocialAction> socialActions =
                        new ArrayList<SocialAction>(socialActionsList.get(i));
                i++;
                while (socialActions.size() > 0) {
                    SocialAction socialAction = socialActions.remove(0);

                    boolean action = Boolean.valueOf(socialAction.getAction());
                    String type = socialAction.getType();
                    if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST.equals(type)
                            || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON.equals(type)
                            || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW.equals(type)
                            || (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE.equals(type)
                            && action && (socialAction.getTargetObject() instanceof Query
                            || socialAction.getTargetObject() instanceof Album
                            || socialAction.getTargetObject() instanceof Artist))) {
                        List<TomahawkListItem> mergedActions = new ArrayList<TomahawkListItem>();
                        mergedActions.add(socialAction);
                        if (socialAction.getTargetObject() instanceof Query) {
                            mShownQueries.add((Query) socialAction.getTargetObject());
                        }
                        List<SocialAction> actionsToDelete = new ArrayList<SocialAction>();
                        for (TomahawkListItem item : socialActions) {
                            SocialAction actionToCompare = (SocialAction) item;
                            if (actionToCompare.getUser() == socialAction.getUser()
                                    && actionToCompare.getType().equals(socialAction.getType())
                                    && actionToCompare.getTargetObject().getClass()
                                    == socialAction.getTargetObject().getClass()) {
                                boolean alreadyMerged = false;
                                for (TomahawkListItem mergedItem : mergedActions) {
                                    SocialAction mergedAction = (SocialAction) mergedItem;
                                    if (mergedAction.getTargetObject()
                                            == actionToCompare.getTargetObject()) {
                                        alreadyMerged = true;
                                        break;
                                    }
                                }
                                if (!alreadyMerged) {
                                    mergedActions.add(actionToCompare);
                                    if (actionToCompare.getTargetObject() instanceof Query) {
                                        mShownQueries
                                                .add((Query) actionToCompare.getTargetObject());
                                    }
                                }
                                actionsToDelete.add(actionToCompare);
                            }
                        }
                        for (SocialAction actionToDelete : actionsToDelete) {
                            socialActions.remove(actionToDelete);
                        }
                        mergedActionsList.add(mergedActions);
                    }
                }
            }

            TomahawkListAdapter tomahawkListAdapter;
            List<Segment> segments = new ArrayList<Segment>();
            for (List mergedActions : mergedActionsList) {
                SocialAction first = (SocialAction) mergedActions.get(0);
                if (first.getTargetObject() instanceof Album
                        || first.getTargetObject() instanceof User
                        || first.getTargetObject() instanceof Artist) {
                    segments.add(new Segment(mergedActions, R.integer.grid_column_count_feed,
                            R.dimen.padding_superlarge, R.dimen.padding_small));
                } else {
                    segments.add(new Segment(mergedActions));
                }
            }
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater,
                        segments, this);
                int extraPadding = getResources().getDimensionPixelSize(R.dimen.padding_medium)
                        + TomahawkUtils.convertDpToPixel(32);
                tomahawkListAdapter.setLeftExtraPadding(extraPadding);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
            if (mShowMode == SHOW_MODE_DASHBOARD
                    && !getResources().getBoolean(R.bool.is_landscape)) {
                getListView().setAreHeadersSticky(true);
            }

            onUpdateAdapterFinished();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        if (firstVisibleItem + visibleItemCount + 5 > totalItemCount) {
            mShowMode = getArguments().getInt(SHOW_MODE);
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                if (!mResolvingPages.contains(mUser.getFriendsFeed().size())) {
                    mResolvingPages.add(mUser.getFriendsFeed().size());
                    mCurrentRequestIds.add(InfoSystem.getInstance()
                            .resolveFriendsFeed(mUser, mUser.getFriendsFeed().size()));
                }
            } else {
                if (!mResolvingPages.contains(mUser.getSocialActions().size())) {
                    mResolvingPages.add(mUser.getSocialActions().size());
                    mCurrentRequestIds.add(InfoSystem.getInstance()
                            .resolveSocialActions(mUser, mUser.getSocialActions().size()));
                }
            }
        }
    }
}
