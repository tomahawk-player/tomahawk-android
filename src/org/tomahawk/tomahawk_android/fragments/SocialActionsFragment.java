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

import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment {

    public static final int SHOW_MODE_SOCIALACTIONS = 0;

    public static final int SHOW_MODE_DASHBOARD = 1;

    private HashSet<User> mResolvedUsers = new HashSet<User>();

    private HashSet<Album> mResolvedAlbums = new HashSet<Album>();

    private HashSet<Artist> mResolvedArtists = new HashSet<Artist>();

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
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveFriendsFeed(mUser));
                    setActionBarOffset();
                } else {
                    if (mContainerFragmentClass == null) {
                        getActivity().setTitle("");
                    }
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveSocialActions(mUser));
                    HatchetAuthenticatorUtils authUtils = (HatchetAuthenticatorUtils)
                            AuthenticatorManager.getInstance().getAuthenticatorUtils(
                                    TomahawkApp.PLUGINNAME_HATCHET);
                    mCurrentRequestIds.add(InfoSystem.getInstance()
                            .resolveFollowings(authUtils.getLoggedInUser()));
                }
            }
        }
        getListView().setAreHeadersSticky(false);
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        if (item instanceof User) {
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    UserPagerFragment.class, ((User) item).getId(),
                    TomahawkFragment.TOMAHAWK_USER_ID);
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
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    TracksFragment.class, item.getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
        } else if (item instanceof Artist) {
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    ArtistPagerFragment.class, item.getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
        } else if (item instanceof Playlist) {
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    PlaylistEntriesFragment.class, ((Playlist) item).getId(),
                    TomahawkFragment.TOMAHAWK_PLAYLIST_KEY);
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
        if (mUser != null) {
            ArrayList<TomahawkListItem> socialActions;
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getFriendsFeed());
            } else {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getSocialActions());
            }
            List<List<TomahawkListItem>> mergedActionsList
                    = new ArrayList<List<TomahawkListItem>>();
            while (socialActions.size() > 0) {
                SocialAction socialAction = (SocialAction) socialActions.remove(0);

                boolean action = Boolean.valueOf(socialAction.getAction());
                String type = socialAction.getType();
                if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST.equals(type)
                        || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON.equals(type)
                        || HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW.equals(type)
                        || (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE.equals(type)
                        && action && (socialAction.getTargetObject() instanceof Query
                        || socialAction.getTargetObject() instanceof Album))) {
                    List<TomahawkListItem> mergedActions = new ArrayList<TomahawkListItem>();
                    mergedActions.add(socialAction);
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

            TomahawkListAdapter tomahawkListAdapter;
            List<Segment> segments = new ArrayList<Segment>();

            mShownQueries.clear();
            for (List<TomahawkListItem> mergedActions : mergedActionsList) {
                SocialAction first = (SocialAction) mergedActions.get(0);
                if (first.getTargetObject() instanceof Album
                        || first.getTargetObject() instanceof User) {
                    segments.add(new Segment(mergedActions, R.integer.grid_column_count_feed,
                            R.dimen.padding_superlarge, R.dimen.padding_small));
                } else {
                    segments.add(new Segment(mergedActions));
                }

                for (TomahawkListItem item : mergedActions) {
                    User user = ((SocialAction) item).getUser();
                    if (!mResolvedUsers.contains(user) && user.getImage() == null) {
                        mResolvedUsers.add(user);
                        mCurrentRequestIds.add(InfoSystem.getInstance().resolve(user));
                    }

                    TomahawkListItem targetObject = ((SocialAction) item).getTargetObject();
                    if (targetObject instanceof Query) {
                        mShownQueries.add((Query) targetObject);
                    } else if (targetObject instanceof Album
                            && !mResolvedAlbums.contains(targetObject)) {
                        mResolvedAlbums.add((Album) targetObject);
                        mCurrentRequestIds
                                .add(InfoSystem.getInstance().resolve((Album) targetObject));
                    } else if (targetObject instanceof User
                            && !mResolvedUsers.contains(targetObject)) {
                        mResolvedUsers.add((User) targetObject);
                        mCurrentRequestIds
                                .add(InfoSystem.getInstance().resolve((User) targetObject));
                    } else if (targetObject instanceof Artist
                            && !mResolvedArtists.contains(targetObject)) {
                        mResolvedArtists.add((Artist) targetObject);
                        mCurrentRequestIds.addAll(InfoSystem.getInstance()
                                .resolve((Artist) targetObject, true));
                    }
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
                getListAdapter().setSegments(segments);
            }

            updateShowPlaystate();
        }
    }
}
