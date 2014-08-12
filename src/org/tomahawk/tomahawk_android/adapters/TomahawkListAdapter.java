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
package org.tomahawk.tomahawk_android.adapters;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.UsersFragment;
import org.tomahawk.tomahawk_android.ui.widgets.SquareHeightRelativeLayout;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    public static final int SHOW_QUERIES_AS_TOPHITS = 0;

    public static final int SHOW_QUERIES_AS_RECENTLYPLAYED = 1;

    private TomahawkMainActivity mActivity;

    private FragmentManager mFragmentManager;

    private List<TomahawkListItem> mListItems;

    private MultiColumnClickListener mClickListener;

    private LayoutInflater mLayoutInflater;

    private boolean mShowCategoryHeaders = false;

    private int mShowQueriesAs = -1;

    private TomahawkListItem mContentHeaderTomahawkListItem;

    private RelativeLayout mContentHeaderImageFrame;

    private Collection mCollection;

    private boolean mIsLandscapeMode;

    private boolean mShowPlaystate = false;

    private PlaylistEntry mHighlightedPlaylistEntry;

    private Query mHighlightedQuery;

    private boolean mHighlightedItemIsPlaying = false;

    private boolean mShowResolvedBy = false;

    private boolean mShowArtistAsSingleLine = false;

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            List<TomahawkListItem> listItems, MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mListItems = listItems;
        mClickListener = clickListener;
    }

    /**
     * Set the complete list of lists
     */
    public void setListItems(List<TomahawkListItem> listItems) {
        mListItems = listItems;
        notifyDataSetChanged();
    }

    /**
     * Set whether or not a header should be shown above each "category". Like "Albums", "Tracks"
     * etc.
     */
    public void setShowCategoryHeaders(boolean showCategoryHeaders) {
        mShowCategoryHeaders = showCategoryHeaders;
    }

    /**
     * Set whether or not a header should be shown above each "category". Like "Albums", "Tracks"
     * etc.
     */
    public void setShowCategoryHeaders(boolean showCategoryHeaders, int showQueriesAs) {
        mShowCategoryHeaders = showCategoryHeaders;
        mShowQueriesAs = showQueriesAs;
    }

    /**
     * Show a content header. A content header provides information about the current {@link
     * TomahawkListItem} that the user has navigated to. Like an AlbumArt image with the {@link
     * Album}s name, which is shown at the top of the listview, if the user browses to a particular
     * {@link Album} in his {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param listItem   the {@link TomahawkListItem}'s information to show in the header view
     * @param collection whether or not the given listItem was given in a local context. This will
     *                   determine whether to show the total track count, or just the count of local
     *                   tracks in the contentHeader's textview.
     */
    public void showContentHeader(View rootView, TomahawkListItem listItem, Collection collection) {
        showContentHeader(null, rootView, listItem, collection);
    }

    public void showContentHeaderUser(FragmentManager fragmentManager, View rootView,
            User user, Collection collection) {
        showContentHeader(fragmentManager, rootView, user, collection);
    }

    private void showContentHeader(FragmentManager fragmentManager, View rootView,
            TomahawkListItem listItem, Collection collection) {
        mFragmentManager = fragmentManager;
        mContentHeaderTomahawkListItem = listItem;
        mCollection = collection;
        mIsLandscapeMode = mActivity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        mContentHeaderImageFrame = (RelativeLayout) rootView
                .findViewById(R.id.content_header_image_frame);
        if (mIsLandscapeMode) {
            View contentHeaderView;
            if (listItem instanceof User) {
                contentHeaderView = mLayoutInflater.inflate(R.layout.content_header_user, null);
            } else {
                contentHeaderView = mLayoutInflater.inflate(R.layout.content_header, null);
            }
            if (mContentHeaderImageFrame.findViewById(R.id.content_header) == null) {
                mContentHeaderImageFrame.addView(contentHeaderView);
            }
            updateContentHeader(rootView, listItem, collection);
        }
        notifyDataSetChanged();
    }

    private void updateContentHeader(View rootView, TomahawkListItem listItem,
            Collection collection) {
        SquareHeightRelativeLayout frame = (SquareHeightRelativeLayout)
                rootView.findViewById(R.id.content_header_image_frame);
        if (frame != null) {
            frame.setVisibility(SquareHeightRelativeLayout.VISIBLE);
        }
        ViewHolder viewHolder = new ViewHolder(rootView, R.layout.content_header);
        if (listItem instanceof Album) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (Album) listItem, collection);
        } else if (listItem instanceof Artist) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (Artist) listItem, collection);
        } else if (listItem instanceof Playlist) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (Playlist) listItem);
        } else if (listItem instanceof User) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (User) listItem);
        }
    }

    /**
     * Set whether or not to highlight the currently playing {@link TomahawkListItem} and show the
     * play/pause state
     */
    public void setShowPlaystate(boolean showPlaystate) {
        this.mShowPlaystate = showPlaystate;
    }

    public void setHighlightedItemIsPlaying(boolean highlightedItemIsPlaying) {
        mHighlightedItemIsPlaying = highlightedItemIsPlaying;
    }

    /**
     * set the position of the item, which should be highlighted
     */
    public void setHighlightedQuery(Query query) {
        mHighlightedQuery = query;
    }

    /**
     * set the position of the item, which should be highlighted
     */
    public void setHighlightedEntry(PlaylistEntry entry) {
        mHighlightedPlaylistEntry = entry;
    }

    /**
     * Set whether or not to show by which {@link org.tomahawk.libtomahawk.resolver.Resolver} the
     * {@link TomahawkListItem} has been resolved
     */
    public void setShowResolvedBy(boolean showResolvedBy) {
        this.mShowResolvedBy = showResolvedBy;
    }

    public void setShowArtistAsSingleLine(boolean showArtistAsSingleLine) {
        mShowArtistAsSingleLine = showArtistAsSingleLine;
    }

    /**
     * Get the correct {@link View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct {@link View} for the given position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        final TomahawkListItem item = (TomahawkListItem) getItem(position);

        if (item != null) {
            boolean shouldBeHighlighted = false;
            if (item instanceof SocialAction) {
                shouldBeHighlighted = mShowPlaystate
                        && ((SocialAction) item).getQuery() == mHighlightedQuery;
            } else if (item instanceof PlaylistEntry) {
                shouldBeHighlighted = mShowPlaystate && item == mHighlightedPlaylistEntry;
            } else if (item instanceof Query) {
                shouldBeHighlighted = mShowPlaystate && item == mHighlightedQuery;
            }

            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int viewType = getViewType(item, shouldBeHighlighted);
            if (viewHolder == null || viewHolder.getLayoutId() != viewType) {
                // If the viewHolder is null or the old viewType is different than the new one,
                // we need to inflate a new view and construct a new viewHolder,
                // which we set as the view's tag
                view = mLayoutInflater.inflate(viewType, parent, false);
                viewHolder = new ViewHolder(mContentHeaderImageFrame, view, viewType);
                view.setTag(viewHolder);
            } else if (viewType == R.layout.list_item
                    || viewType == R.layout.list_item_highlighted) {
                viewHolder.getImageView1().setVisibility(View.GONE);
                viewHolder.getImageView2().setVisibility(View.GONE);
                viewHolder.getTextView2().setVisibility(View.GONE);
                viewHolder.getTextView3().setVisibility(View.GONE);
                viewHolder.getTextView4().setVisibility(View.GONE);
                viewHolder.getTextView5().setVisibility(View.GONE);
            }

            // After we've setup the correct view and viewHolder, we now can fill the View's
            // components with the correct data
            if (viewHolder.getLayoutId() == R.layout.content_header
                    || viewHolder.getLayoutId() == R.layout.content_header_user) {
                if (mContentHeaderTomahawkListItem instanceof Album) {
                    AdapterUtils.fillContentHeader(mActivity, viewHolder,
                            (Album) mContentHeaderTomahawkListItem, mCollection);
                } else if (mContentHeaderTomahawkListItem instanceof Artist) {
                    AdapterUtils.fillContentHeader(mActivity, viewHolder,
                            (Artist) mContentHeaderTomahawkListItem, mCollection);
                } else if (mContentHeaderTomahawkListItem instanceof Playlist) {
                    AdapterUtils.fillContentHeader(mActivity, viewHolder,
                            (Playlist) mContentHeaderTomahawkListItem);
                } else if (mContentHeaderTomahawkListItem instanceof User) {
                    AdapterUtils.fillContentHeader(mActivity, viewHolder,
                            (User) mContentHeaderTomahawkListItem);
                }
            } else if (viewHolder.getLayoutId() == R.layout.single_line_list_item) {
                viewHolder.getTextView1().setText(item.getName());
            } else if (viewHolder.getLayoutId() == R.layout.list_item
                    || viewHolder.getLayoutId() == R.layout.list_item_highlighted) {
                if (item instanceof Query) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Query) item,
                            mHighlightedItemIsPlaying && shouldBeHighlighted, mShowResolvedBy);
                } else if (item instanceof PlaylistEntry) {
                    AdapterUtils.fillView(mActivity, viewHolder, ((PlaylistEntry) item).getQuery(),
                            mHighlightedItemIsPlaying && shouldBeHighlighted, mShowResolvedBy);
                } else if (item instanceof Album) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Album) item);
                } else if (item instanceof Artist) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Artist) item);
                } else if (item instanceof User) {
                    AdapterUtils.fillView(mActivity, viewHolder, (User) item);
                } else if (item instanceof SocialAction) {
                    AdapterUtils.fillView(mActivity, viewHolder, (SocialAction) item,
                            mHighlightedItemIsPlaying && shouldBeHighlighted, mShowResolvedBy);
                }
            }

            //Set up the click listeners
            if (viewHolder.getLayoutId() == R.layout.content_header
                    || viewHolder.getLayoutId() == R.layout.content_header_user) {
                if (mContentHeaderTomahawkListItem instanceof Album) {
                    viewHolder.getStarButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CollectionManager.getInstance().toggleLovedItem(
                                    (Album) mContentHeaderTomahawkListItem);
                        }
                    });
                } else if (mContentHeaderTomahawkListItem instanceof Artist) {
                    viewHolder.getStarButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CollectionManager.getInstance().toggleLovedItem(
                                    (Artist) mContentHeaderTomahawkListItem);
                        }
                    });
                } else if (mContentHeaderTomahawkListItem instanceof User) {
                    final User user = (User) mContentHeaderTomahawkListItem;
                    viewHolder.getButton1().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Bundle bundle = new Bundle();
                            bundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                                    user.getPlaybackLog().getCacheKey());
                            bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID,
                                    user.getCacheKey());
                            FragmentUtils.replace(mActivity, mFragmentManager,
                                    PlaylistEntriesFragment.class, bundle);
                        }
                    });
                    viewHolder.getButton2().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FragmentUtils.replace(mActivity, mFragmentManager,
                                    UsersFragment.class, user.getCacheKey(),
                                    TomahawkFragment.TOMAHAWK_USER_ID,
                                    UsersFragment.SHOW_MODE_TYPE_FOLLOWINGS);
                        }
                    });
                    viewHolder.getButton3().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FragmentUtils.replace(mActivity, mFragmentManager,
                                    UsersFragment.class, user.getCacheKey(),
                                    TomahawkFragment.TOMAHAWK_USER_ID,
                                    UsersFragment.SHOW_MODE_TYPE_FOLLOWERS);
                        }
                    });
                }
            } else if (viewHolder.getLayoutId() == R.layout.list_item
                    || viewHolder.getLayoutId() == R.layout.list_item_highlighted) {
                if (item instanceof SocialAction || item instanceof User) {
                    User user;
                    if (item instanceof SocialAction) {
                        user = ((SocialAction) item).getUser();
                    } else {
                        user = (User) item;
                    }
                    viewHolder.getClickArea1().setOnClickListener(
                            new ClickListener(user, mClickListener));
                    viewHolder.getClickArea1().setOnLongClickListener(
                            new ClickListener(user, mClickListener));
                }
            }
            viewHolder.getMainClickArea()
                    .setOnClickListener(new ClickListener(item, mClickListener));
            viewHolder.getMainClickArea()
                    .setOnLongClickListener(new ClickListener(item, mClickListener));
        }
        return view;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        int correction = 0;
        if (!mIsLandscapeMode && mContentHeaderTomahawkListItem != null) {
            correction = 1;
        }
        return mListItems.size() + correction;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        if (!mIsLandscapeMode && mContentHeaderTomahawkListItem != null) {
            position--;
        }
        if (position < 0) {
            return mContentHeaderTomahawkListItem;
        }
        return mListItems.get(position);
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * This method is being called by the StickyListHeaders library. Get the correct header {@link
     * View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct header {@link View} for the given position.
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        TomahawkListItem item = (TomahawkListItem) getItem(position);
        if (mShowCategoryHeaders && item != null && item != mContentHeaderTomahawkListItem) {
            View view = null;
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            if (viewHolder == null
                    || viewHolder.getLayoutId() != R.layout.single_line_list_header) {
                view = mLayoutInflater.inflate(R.layout.single_line_list_header, null);
                viewHolder = new ViewHolder(view, R.layout.single_line_list_header);
                view.setTag(viewHolder);
            }

            if (item instanceof Track || item instanceof Query) {
                if (mShowQueriesAs == SHOW_QUERIES_AS_TOPHITS) {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageView1(), R.drawable.ic_action_tophits);
                    viewHolder.getTextView1().setText(R.string.tophits_categoryheaders_string);
                } else if (mShowQueriesAs == SHOW_QUERIES_AS_RECENTLYPLAYED) {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageView1(), R.drawable.ic_action_time);
                    viewHolder.getTextView1()
                            .setText(R.string.recentlyplayed_categoryheaders_string);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageView1(), R.drawable.ic_action_track);
                    viewHolder.getTextView1().setText(R.string.tracksfragment_title_string);
                }
            } else if (item instanceof Artist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity,
                        viewHolder.getImageView1(), R.drawable.ic_action_artist);
                viewHolder.getTextView1().setText(R.string.artistsfragment_title_string);
            } else if (item instanceof Album) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity,
                        viewHolder.getImageView1(), R.drawable.ic_action_album);
                viewHolder.getTextView1().setText(R.string.albumsfragment_title_string);
            } else if (item instanceof Playlist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity,
                        viewHolder.getImageView1(), R.drawable.ic_action_playlist);
                viewHolder.getTextView1()
                        .setText(R.string.playlists_categoryheaders_string);
            } else if (item instanceof User) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity,
                        viewHolder.getImageView1(), R.drawable.ic_action_friends);
                viewHolder.getTextView1().setText(R.string.userfragment_title_string);
            } else if (item instanceof SocialAction) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity,
                        viewHolder.getImageView1(), R.drawable.ic_action_trending);
                viewHolder.getTextView1().setText(R.string.category_header_activityfeed);
            }
            return view;
        } else {
            return new View(mActivity);
        }
    }

    /**
     * This method is being called by the StickyListHeaders library. Returns the same value for each
     * item that should be grouped under the same header.
     *
     * @param position the position of the item for which to get the header id
     * @return the same value for each item that should be grouped under the same header.
     */
    @Override
    public long getHeaderId(int position) {
        Object item = getItem(position);
        if (item == mContentHeaderTomahawkListItem) {
            return 9;
        } else if (item instanceof Album) {
            return 1;
        } else if (item instanceof Artist) {
            return 2;
        } else if (item instanceof Query) {
            return 3;
        } else if (item instanceof SocialAction) {
            return 4;
        } else if (item instanceof User) {
            return 5;
        } else if (item instanceof Track) {
            return 6;
        } else if (item instanceof Playlist) {
            return 7;
        } else {
            return 0;
        }
    }

    /**
     * @return the {@link TomahawkListItem} shown in the content header
     */
    public TomahawkListItem getContentHeaderTomahawkListItem() {
        return mContentHeaderTomahawkListItem;
    }

    public boolean isShowingContentHeader() {
        return !mIsLandscapeMode && mContentHeaderTomahawkListItem != null;
    }

    private int getViewType(TomahawkListItem item, boolean isHighlighted) {
        if (item == mContentHeaderTomahawkListItem) {
            if (item instanceof User) {
                return R.layout.content_header_user;
            } else {
                return R.layout.content_header;
            }
        } else if (item instanceof Playlist || (item instanceof Artist
                && mShowArtistAsSingleLine)) {
            return R.layout.single_line_list_item;
        } else if (isHighlighted) {
            return R.layout.list_item_highlighted;
        } else {
            return R.layout.list_item;
        }
    }
}
