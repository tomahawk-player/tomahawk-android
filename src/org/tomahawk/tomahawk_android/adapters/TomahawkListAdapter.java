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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.ui.widgets.SquareHeightRelativeLayout;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends TomahawkBaseAdapter implements StickyListHeadersAdapter {

    private LayoutInflater mLayoutInflater;

    private boolean mShowCategoryHeaders = false;

    private boolean mShowQueriesAsTopHits = false;

    private TomahawkListItem mContentHeaderTomahawkListItem;

    private boolean mShowPlaystate = false;

    private boolean mShowResolvedBy = false;

    private boolean mShowArtistAsSingleLine = false;

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     *
     * @param activity  reference to whatever {@link Activity}
     * @param listArray complete set of lists containing all content which the listview should be
     *                  populated with
     */
    public TomahawkListAdapter(Activity activity, List<List<TomahawkListItem>> listArray) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        mListArray = listArray;
    }

    /**
     * Set whether or not a header should be shown above each "category". Like "Albums", "Tracks"
     * etc.
     */
    public void setShowCategoryHeaders(boolean showCategoryHeaders, boolean showQueriesAsTopHits) {
        mShowCategoryHeaders = showCategoryHeaders;
        mShowQueriesAsTopHits = showQueriesAsTopHits;
    }

    /**
     * Show a content header. A content header provides information about the current {@link
     * TomahawkListItem} that the user has navigated to. Like an AlbumArt image with the {@link
     * Album}s name, which is shown at the top of the listview, if the user browses to a particular
     * {@link Album} in his {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param list        a reference to the list, so we can set its header view
     * @param listItem    the {@link TomahawkListItem}'s information to show in the header view
     * @param isOnlyLocal whether or not the given listItem was given in a local context. This will
     *                    determine whether to show the total track count, or just the count of
     *                    local tracks in the contentHeader's textview.
     */
    public void showContentHeader(boolean landscapeMode, StickyListHeadersListView list,
            TomahawkListItem listItem, boolean isOnlyLocal) {
        mContentHeaderTomahawkListItem = listItem;
        View contentHeaderView = mLayoutInflater.inflate(R.layout.content_header, null);
        if (!landscapeMode && contentHeaderView != null && list.getHeaderViewsCount() == 0) {
            list.addHeaderView(contentHeaderView);
        }
        updateContentHeader(listItem, isOnlyLocal);
    }

    public void updateContentHeader(TomahawkListItem listItem, boolean isOnlyLocal) {
        SquareHeightRelativeLayout frame = (SquareHeightRelativeLayout)
                mActivity.findViewById(R.id.content_header_image_frame);
        if (frame != null) {
            frame.setVisibility(SquareHeightRelativeLayout.VISIBLE);
        }
        ImageView imageView = (ImageView) mActivity.findViewById(R.id.content_header_image);
        imageView.setVisibility(ImageView.VISIBLE);
        TextView textView = (TextView) mActivity.findViewById(R.id.content_header_textview);
        TextView textView2 = (TextView) mActivity.findViewById(R.id.content_header_textview2);
        if (listItem instanceof Album) {
            TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                    listItem.getImage(), Image.IMAGE_SIZE_LARGE);
            int tracksCount = listItem.getAlbum().getQueries(isOnlyLocal).size();
            String s = listItem.getArtist().getName() + ", " + tracksCount + " "
                    + mActivity.getString(R.string.content_header_track)
                    + (tracksCount == 1 ? "" : "s");
            textView2.setText(s);
        } else if (listItem instanceof Artist) {
            TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                    listItem.getImage(), Image.IMAGE_SIZE_LARGE);
            int topHitsCount = listItem.getArtist().getTopHits().size();
            int albumsCount = isOnlyLocal ? ((Artist) listItem).getLocalAlbums().size()
                    : ((Artist) listItem).getAlbums().size();
            String s = (isOnlyLocal ? "" : (topHitsCount + " "
                    + mActivity.getString(R.string.content_header_tophit)
                    + (topHitsCount == 1 ? "" : "s") + ", ")) + albumsCount + " "
                    + mActivity.getString(R.string.content_header_album)
                    + (albumsCount == 1 ? "" : "s");
            textView2.setText(s);
        } else if (listItem instanceof UserPlaylist) {
            int tracksCount = listItem.getQueries(isOnlyLocal).size();
            String s = tracksCount + " " + mActivity.getString(R.string.content_header_track)
                    + (tracksCount == 1 ? "" : "s");
            textView2.setText(s);
            ArrayList<Artist> artists = ((UserPlaylist) listItem)
                    .getContentHeaderArtists();
            ArrayList<Artist> artistsWithImage = new ArrayList<Artist>();
            for (Artist artist : artists) {
                if (artist.getImage() != null) {
                    artistsWithImage.add(artist);
                }
            }
            if (artistsWithImage.size() > 0) {
                TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                        artistsWithImage.get(0).getImage(), Image.IMAGE_SIZE_LARGE);
            }
            if (artistsWithImage.size() > 3) {
                mActivity.findViewById(R.id.content_header_image_frame2)
                        .setVisibility(View.VISIBLE);
                imageView = (ImageView) mActivity.findViewById(R.id.content_header_image2);
                imageView.setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                        artistsWithImage.get(1).getImage(), Image.IMAGE_SIZE_LARGE);
                imageView = (ImageView) mActivity.findViewById(R.id.content_header_image3);
                imageView.setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                        artistsWithImage.get(2).getImage(), Image.IMAGE_SIZE_LARGE);
                imageView = (ImageView) mActivity.findViewById(R.id.content_header_image4);
                imageView.setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(mActivity, imageView,
                        artistsWithImage.get(3).getImage(), Image.IMAGE_SIZE_LARGE);
            }
        }
        if (textView != null) {
            textView.setText(listItem.getName());
        }
    }

    /**
     * Set whether or not to highlight the currently playing {@link TomahawkListItem} and show the
     * play/pause state
     */
    public void setShowPlaystate(boolean showPlaystate) {
        this.mShowPlaystate = showPlaystate;
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
     * Set whether or not to show an AddButton, so that the user can add {@link UserPlaylist}s to
     * the database
     *
     * @param list       a reference to the list, so we can set its footer view
     * @param buttonText {@link String} containing the button's text to show
     */
    public void setShowAddButton(StickyListHeadersListView list, String buttonText) {
        View addButtonFooterView = mLayoutInflater.inflate(R.layout.add_button_layout, null);
        if (addButtonFooterView != null && list.getFooterViewsCount() == 0) {
            ((TextView) addButtonFooterView.findViewById(R.id.add_button_textview))
                    .setText(buttonText);
            list.addFooterView(addButtonFooterView);
        }
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
        TomahawkListItem item = (TomahawkListItem) getItem(position);

        if (item != null) {
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int viewType = getViewType(item);
            if (viewHolder == null || viewHolder.getViewType() != viewType) {
                // If the viewHolder is null or the old viewType is different than the new one,
                // we need to inflate a new view and construct a new viewHolder,
                // which we set as the view's tag
                if (viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                    view = mLayoutInflater.inflate(R.layout.single_line_list_item, null);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                } else if (viewType == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                    view = mLayoutInflater.inflate(R.layout.double_line_list_item, null);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                }
            } else {
                if (viewHolder.getImageViewLeft() != null) {
                    viewHolder.getImageViewLeft().setVisibility(View.GONE);
                }
                if (viewHolder.getImageViewRight() != null) {
                    viewHolder.getImageViewRight().setVisibility(View.GONE);
                }
                viewHolder.getTextThirdLine().setVisibility(View.GONE);
                viewHolder.getTextSecondLine().setVisibility(View.GONE);
            }

            // After we've setup the correct view and viewHolder, we now can fill the View's
            // components with the correct data
            if (viewHolder.getViewType() == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                viewHolder.getTextFirstLine().setText(item.getName());
            } else if (viewHolder.getViewType()
                    == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                viewHolder.getTextFirstLine().setText(item.getName());
                if (item instanceof Query) {
                    Query query = (Query) item;
                    viewHolder.getTextSecondLine().setVisibility(View.VISIBLE);
                    viewHolder.getTextSecondLine().setText(query.getArtist().getName());
                    viewHolder.getTextThirdLine().setVisibility(View.VISIBLE);
                    if (query.getPreferredTrack().getDuration() > 0) {
                        viewHolder.getTextThirdLine().setText(TomahawkUtils.durationToString(
                                (query.getPreferredTrack().getDuration())));
                    } else {
                        viewHolder.getTextThirdLine().setText(mActivity.getResources().getString(
                                R.string.playbackactivity_seekbar_completion_time_string));
                    }
                    setTextViewEnabled(viewHolder.getTextFirstLine(), query.isPlayable());
                    setTextViewEnabled(viewHolder.getTextSecondLine(), query.isPlayable());
                    setTextViewEnabled(viewHolder.getTextThirdLine(), query.isPlayable());
                    if (mShowPlaystate && position == mHighlightedItemPosition) {
                        view.setBackgroundResource(R.color.pressed_tomahawk);
                        if (mHighlightedItemIsPlaying) {
                            viewHolder.getImageViewLeft().setVisibility(ImageView.VISIBLE);
                            TomahawkUtils.loadDrawableIntoImageView(mActivity,
                                    viewHolder.getImageViewLeft(),
                                    R.drawable.ic_playlist_is_playing);
                        }
                    } else {
                        view.setBackgroundResource(
                                R.drawable.selectable_background_tomahawk_opaque);
                    }
                    if (mShowResolvedBy && query.getPreferredTrackResult() != null) {
                        viewHolder.getImageViewRight().setVisibility(ImageView.VISIBLE);
                        viewHolder.getImageViewRight().setImageDrawable(
                                query.getPreferredTrackResult().getResolvedBy().getIcon());
                    }
                } else if (item instanceof Album || item instanceof Artist
                        || item instanceof User) {
                    viewHolder.getImageViewLeft().setVisibility(View.VISIBLE);
                    TomahawkUtils.loadImageIntoImageView(mActivity, viewHolder.getImageViewLeft(),
                            item.getImage(), Image.IMAGE_SIZE_SMALL);
                    if (item instanceof Album) {
                        viewHolder.getTextSecondLine().setVisibility(View.VISIBLE);
                        viewHolder.getTextSecondLine().setText(item.getArtist().getName());
                    }
                }
            }
        }
        return view;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        if ((mFiltered ? mFilteredListArray : mListArray) == null) {
            return 0;
        }
        int displayedListArrayItemsCount = 0;
        int displayedContentHeaderCount = 0;
        for (List<TomahawkListItem> list : (mFiltered ? mFilteredListArray : mListArray)) {
            displayedListArrayItemsCount += list.size();
        }
        return displayedListArrayItemsCount + displayedContentHeaderCount;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        Object item = null;
        int offsetCounter = 0;
        for (int i = 0; i < (mFiltered ? mFilteredListArray : mListArray).size(); i++) {
            List<TomahawkListItem> list = (mFiltered ? mFilteredListArray : mListArray).get(i);
            if (position - offsetCounter < list.size()) {
                item = list.get(position - offsetCounter);
                break;
            }
            offsetCounter += list.size();
        }
        return item;
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
        if (mShowCategoryHeaders && item != null) {
            View view;
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            } else {
                view = mLayoutInflater.inflate(R.layout.single_line_list_header, null);
                viewHolder = new ViewHolder(view, R.id.tomahawklistadapter_viewtype_header);
                view.setTag(viewHolder);
            }

            if (item instanceof Track || item instanceof Query) {
                if (mShowQueriesAsTopHits) {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageViewLeft(), R.drawable.ic_action_tophits);
                    viewHolder.getTextFirstLine().setText(R.string.tophits_categoryheaders_string);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageViewLeft(), R.drawable.ic_action_track);
                    viewHolder.getTextFirstLine().setText(R.string.tracksfragment_title_string);
                }
            } else if (item instanceof Artist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageViewLeft(),
                        R.drawable.ic_action_artist);
                viewHolder.getTextFirstLine().setText(R.string.artistsfragment_title_string);
            } else if (item instanceof Album) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageViewLeft(),
                        R.drawable.ic_action_album);
                viewHolder.getTextFirstLine().setText(R.string.albumsfragment_title_string);
            } else if (item instanceof UserPlaylist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageViewLeft(),
                        R.drawable.ic_action_playlist);
                if (((UserPlaylist) item).isHatchetPlaylist()) {
                    viewHolder.getTextFirstLine()
                            .setText(R.string.hatchet_userplaylists_categoryheaders_string);
                } else {
                    viewHolder.getTextFirstLine()
                            .setText(R.string.userplaylists_categoryheaders_string);
                }
            } else if (item instanceof User) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageViewLeft(),
                        R.drawable.ic_action_friends);
                viewHolder.getTextFirstLine().setText(R.string.userfragment_title_string);
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
        long result = 0;
        int sizeSum = 0;
        for (List<TomahawkListItem> list : mFiltered ? mFilteredListArray : mListArray) {
            sizeSum += list.size();
            if (position < sizeSum) {
                break;
            } else {
                result++;
            }
        }
        return result;
    }

    /**
     * @return the {@link TomahawkListItem} shown in the content header
     */
    public TomahawkListItem getContentHeaderTomahawkListItem() {
        return mContentHeaderTomahawkListItem;
    }

    private int getViewType(TomahawkListItem item) {
        if (item instanceof UserPlaylist || (item instanceof Artist && mShowArtistAsSingleLine)) {
            return R.id.tomahawklistadapter_viewtype_singlelinelistitem;
        } else {
            return R.id.tomahawklistadapter_viewtype_doublelinelistitem;
        }
    }

    private static TextView setTextViewEnabled(TextView textView, boolean enabled) {
        if (textView != null && textView.getResources() != null) {
            int colorResId;
            if (enabled) {
                colorResId = R.color.primary_textcolor;
            } else {
                colorResId = R.color.disabled_grey;
            }
            textView.setTextColor(textView.getResources().getColor(colorResId));
        }
        return textView;
    }
}
