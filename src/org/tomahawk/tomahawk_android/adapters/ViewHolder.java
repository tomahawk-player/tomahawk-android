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
package org.tomahawk.tomahawk_android.adapters;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.views.FancyDropDown;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ViewHolder {

    int mLayoutId;

    ImageView mUserImageView1;

    TextView mUserTextView1;

    ImageView mImageView1;

    ImageView mImageView2;

    ImageView mImageView3;

    View mConnectImageViewContainer;

    CheckBox mCheckBox1;

    CheckBox mCheckBox2;

    Spinner mSpinner1;

    FancyDropDown mFancyDropDown;

    TextView mTextView1;

    TextView mTextView2;

    TextView mTextView3;

    TextView mTextView4;

    FrameLayout mFollowButton;

    TextView mFollowButtonTextView;

    FrameLayout mMoreButton;

    View mMainClickArea;

    View mClickArea1;

    FrameLayout mProgressBarContainer;

    public ViewHolder(View rootView, int layoutId) {
        this.mLayoutId = layoutId;
        if (layoutId == R.layout.single_line_list_item) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
        } else if (layoutId == R.layout.list_item_text) {
            mTextView1 = (TextView) rootView;
        } else if (layoutId == R.layout.content_header_user_navdrawer) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mUserImageView1 = (ImageView) rootView
                    .findViewById(R.id.userimageview1);
            mUserTextView1 = (TextView) rootView
                    .findViewById(R.id.usertextview1);
        } else if (layoutId == R.layout.list_item_track) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.imageview1);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.textview3);
            mTextView4 = (TextView) rootView
                    .findViewById(R.id.textview4);
            mClickArea1 = rootView
                    .findViewById(R.id.clickarea1);
            mProgressBarContainer = (FrameLayout) rootView
                    .findViewById(R.id.progressbar_container);
        } else if (layoutId == R.layout.list_item_folder) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mCheckBox1 = (CheckBox) rootView
                    .findViewById(R.id.checkbox1);
            mCheckBox2 = (CheckBox) rootView
                    .findViewById(R.id.checkbox2);
        } else if (layoutId == R.layout.single_line_list_header) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
        } else if (layoutId == R.layout.list_header_socialaction) {
            mUserImageView1 = (ImageView) rootView
                    .findViewById(R.id.userimageview1);
            mUserTextView1 = (TextView) rootView
                    .findViewById(R.id.usertextview1);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
        } else if (layoutId == R.layout.dropdown_header) {
            mSpinner1 = (Spinner) rootView
                    .findViewById(R.id.spinner1);
        } else if (layoutId == R.layout.fake_preferences_plain) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
        } else if (layoutId == R.layout.fake_preferences_checkbox) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mCheckBox1 = (CheckBox) rootView
                    .findViewById(R.id.checkbox1);
        } else if (layoutId == R.layout.fake_preferences_spinner) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mSpinner1 = (Spinner) rootView
                    .findViewById(R.id.spinner1);
        } else if (layoutId == R.layout.fake_preferences_configauth) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.imageview1);
        } else if (layoutId == R.layout.fake_preferences_header) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
        } else if (layoutId == R.layout.grid_item || layoutId == R.layout.list_item_artistalbum) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.imageview1);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.textview3);
        } else if (layoutId == R.layout.grid_item_user || layoutId == R.layout.list_item_user) {
            mUserImageView1 = (ImageView) rootView
                    .findViewById(R.id.userimageview1);
            mUserTextView1 = (TextView) rootView
                    .findViewById(R.id.usertextview1);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.textview3);
        } else if (layoutId == R.layout.grid_item_resolver) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.imageview1);
            mImageView2 = (ImageView) rootView
                    .findViewById(R.id.imageview2);
            mConnectImageViewContainer = rootView
                    .findViewById(R.id.connect_imageview_container);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
        } else if (layoutId == R.layout.grid_item_playlist) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.imageview1);
            mImageView2 = (ImageView) rootView
                    .findViewById(R.id.imageview2);
            mImageView3 = (ImageView) rootView
                    .findViewById(R.id.imageview3);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.textview1);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.textview3);
        }
        if (mMainClickArea == null) {
            mMainClickArea = rootView;
        }
    }

    public ViewHolder(View imageFrame, View headerFrame, int layoutId) {
        mLayoutId = layoutId;
        if (layoutId == R.layout.content_header_user) {
            mTextView1 = (TextView) headerFrame
                    .findViewById(R.id.textview1);
            mUserImageView1 = (ImageView) headerFrame
                    .findViewById(R.id.userimageview1);
            mUserTextView1 = (TextView) headerFrame
                    .findViewById(R.id.usertextview1);
            mFollowButton = (FrameLayout) headerFrame
                    .findViewById(R.id.followbutton1);
            mFollowButtonTextView = (TextView) headerFrame
                    .findViewById(R.id.followbutton1_textview);
        } else if (layoutId == R.layout.content_header) {
            mMoreButton = (FrameLayout) headerFrame
                    .findViewById(R.id.morebutton1);
            mFancyDropDown = (FancyDropDown) headerFrame
                    .findViewById(R.id.fancydropdown);
        }
        if (imageFrame != null) {
            mImageView1 = (ImageView) imageFrame
                    .findViewById(R.id.imageview1);
            mImageView2 = (ImageView) imageFrame
                    .findViewById(R.id.imageview2);
            mImageView3 = (ImageView) imageFrame
                    .findViewById(R.id.imageview3);
        }
        if (mMainClickArea == null) {
            mMainClickArea = headerFrame;
        }
    }

    public void setMainClickListener(ClickListener listener) {
        mMainClickArea.setOnClickListener(listener);
        mMainClickArea.setOnLongClickListener(listener);
    }

    public void setClickArea1Listener(ClickListener listener) {
        mClickArea1.setOnClickListener(listener);
        mClickArea1.setOnLongClickListener(listener);
    }

    public void setupFancyDropDown(String text) {
        mFancyDropDown.setup(text);
    }

    public void setupFancyDropDown(int initialSelection, String text,
            List<FancyDropDown.DropDownItemInfo> dropDownItemInfos,
            FancyDropDown.DropDownListener dropDownListener) {
        mFancyDropDown.setup(initialSelection, text, dropDownItemInfos, dropDownListener);
    }

    public void fillContentHeader(final Album album, View.OnClickListener moreButtonListener) {
        mImageView1.setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                album.getImage(), Image.getLargeImageSize(), false);
        mMoreButton.setVisibility(View.VISIBLE);
        mMoreButton.setOnClickListener(moreButtonListener);
    }

    public void fillContentHeader(final Artist artist, View.OnClickListener moreButtonListener) {
        mImageView1.setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                artist.getImage(), Image.getLargeImageSize(), true);
        mMoreButton.setVisibility(View.VISIBLE);
        mMoreButton.setOnClickListener(moreButtonListener);
    }

    public void fillContentHeaderSmall(String text, User user) {
        mTextView1.setText(text.toUpperCase());
        TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(), mUserImageView1,
                user, Image.getSmallImageSize(), mUserTextView1);
        mUserImageView1.setVisibility(View.VISIBLE);
    }

    public void fillContentHeader(final User user, boolean showFollowing,
            boolean showNotFollowing, View.OnClickListener followButtonListener) {
        TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                user.getImage(), Image.getSmallImageSize(), R.color.userpage_default_background);
        TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(), mUserImageView1,
                user, Image.getSmallImageSize(), mUserTextView1);
        mTextView1.setText(user.getName().toUpperCase());
        if (showFollowing) {
            mFollowButton
                    .setBackgroundResource(R.drawable.selectable_background_button_green_filled);
            mFollowButton.setOnClickListener(followButtonListener);
            mFollowButtonTextView.setText(TomahawkApp.getContext().getString(
                    R.string.content_header_following).toUpperCase());
        } else if (showNotFollowing) {
            mFollowButton
                    .setBackgroundResource(R.drawable.selectable_background_button_green);
            mFollowButton.setOnClickListener(followButtonListener);
            mFollowButtonTextView.setText(TomahawkApp.getContext().getString(
                    R.string.content_header_follow).toUpperCase());
        } else {
            mFollowButton.setVisibility(View.GONE);
        }
    }

    public void fillContentHeader(Query query, View.OnClickListener moreButtonListener) {
        mImageView1.setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                query.getImage(), Image.getLargeImageSize(), query.hasArtistImage());
        mMoreButton.setVisibility(View.VISIBLE);
        mMoreButton.setOnClickListener(moreButtonListener);
    }

    public void fillContentHeader(Image image) {
        TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(), mImageView1, image,
                Image.getSmallImageSize(), R.drawable.album_placeholder_grid);
    }

    public void fillContentHeader(Integer integer) {
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), mImageView1, integer);
    }

    public void fillContentHeader(ColorDrawable drawable) {
        mImageView1.setImageDrawable(drawable);
    }

    public void fillView(Query query, String numerationString, boolean showAsPlaying,
            boolean showDuration, boolean hideArtistName) {
        if (!hideArtistName) {
            mTextView3.setVisibility(View.VISIBLE);
            mTextView3.setText(query.getArtist().getName());
        }
        mTextView2.setText(query.getName());
        setTextViewEnabled(mTextView2, query.isPlayable(), false);
        setTextViewEnabled(mTextView3, query.isPlayable(), false);
        if (numerationString != null) {
            if (showAsPlaying) {
                mTextView1.setVisibility(View.INVISIBLE);
                mTextView1.setText(numerationString);
                mImageView1.setVisibility(View.VISIBLE);
                Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
                resolver.loadIcon(mImageView1, false);
            } else {
                mTextView1.setVisibility(View.VISIBLE);
                mTextView1.setText(numerationString);
            }
        }
        if (showDuration) {
            mTextView4.setVisibility(View.VISIBLE);
            if (query.getPreferredTrack().getDuration() > 0) {
                mTextView4.setText(TomahawkUtils.durationToString(
                        (query.getPreferredTrack().getDuration())));
            } else {
                mTextView4.setText(PlaybackSeekBar.COMPLETION_STRING_DEFAULT);
            }
        }
    }

    public void fillView(String string) {
        mTextView1.setText(string);
    }

    public void fillView(User user) {
        mTextView1.setText(user.getName());
        TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                mUserImageView1, user, Image.getSmallImageSize(),
                mUserTextView1);
    }

    public void fillView(Artist artist) {
        mTextView1.setText(artist.getName());
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                artist.getImage(), Image.getSmallImageSize(), true);
    }

    public void fillView(Album album) {
        mTextView1.setText(album.getName());
        mTextView2.setVisibility(View.VISIBLE);
        mTextView2.setText(album.getArtist().getName());
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                album.getImage(), Image.getSmallImageSize(), false);
        int songCount = CollectionManager.getInstance().getCollection(
                TomahawkApp.PLUGINNAME_USERCOLLECTION).getAlbumTracks(album, false).size();
        if (songCount == 0) {
            songCount = CollectionManager.getInstance().getCollection(
                    TomahawkApp.PLUGINNAME_HATCHET).getAlbumTracks(album, false).size();
        }
        if (songCount > 0) {
            String songs = TomahawkApp.getContext().getResources().getString(R.string.songs);
            mTextView3.setVisibility(View.VISIBLE);
            mTextView3.setText(songCount + " " + songs);
        }
    }

    public void fillView(Resolver resolver) {
        mTextView1.setText(resolver.getPrettyName());
        mImageView1.clearColorFilter();
        resolver.loadIconBackground(mImageView1, !resolver.isEnabled());
        resolver.loadIconWhite(mImageView2);
        if (resolver.isEnabled()) {
            mConnectImageViewContainer.setVisibility(View.VISIBLE);
        } else {
            mConnectImageViewContainer.setVisibility(View.GONE);
        }
    }

    public void fillView(Playlist playlist) {
        ArrayList<Image> artistImages = new ArrayList<>();
        String topArtistsString = "";
        String[] artists = playlist.getTopArtistNames();
        if (artists != null) {
            for (int i = 0; i < artists.length && i < 3; i++) {
                Artist artist = Artist.get(artists[i]);
                topArtistsString += artists[i];
                if (i != artists.length - 1) {
                    topArtistsString += ", ";
                }
                if (artist.getImage() != null) {
                    artistImages.add(artist.getImage());
                }
            }
        }
        if (artistImages.size() > 2) {
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView1,
                    artistImages.get(0), Image.getLargeImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView2,
                    artistImages.get(1), Image.getSmallImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView3,
                    artistImages.get(2), Image.getSmallImageSize(), false);
            mImageView1.setVisibility(View.VISIBLE);
            mImageView3.setVisibility(View.VISIBLE);
        } else if (artistImages.size() > 1) {
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView2,
                    artistImages.get(0), Image.getLargeImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView3,
                    artistImages.get(1), Image.getLargeImageSize(), false);
            mImageView3.setVisibility(View.VISIBLE);
        } else if (artistImages.size() > 0) {
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), mImageView2,
                    artistImages.get(0), Image.getLargeImageSize(), false);
        } else {
            TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), mImageView2,
                    R.drawable.album_placeholder_grid);
        }
        if (mTextView1 != null) {
            mTextView1.setText(playlist.getName());
        }
        if (mTextView2 != null) {
            mTextView2.setText(topArtistsString);
        }
        if (mTextView3 != null) {
            mTextView3.setText(playlist.getCount() + " "
                    + TomahawkApp.getContext().getString(R.string.songs_with_count));
        }
    }

    private static String dateToString(Resources resources, Date date) {
        String s = "";
        if (date != null) {
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 60000) {
                s += resources.getString(R.string.time_afewseconds);
            } else if (diff < 3600000) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                if (minutes < 2) {
                    s += resources.getString(R.string.time_aminute);
                } else {
                    s += resources.getString(R.string.time_minutes, minutes);
                }
            } else if (diff < 86400000) {
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                if (hours < 2) {
                    s += resources.getString(R.string.time_anhour);
                } else {
                    s += resources.getString(R.string.time_hours, hours);
                }
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                if (days < 2) {
                    s += resources.getString(R.string.time_aday);
                } else {
                    s += resources.getString(R.string.time_days, days);
                }
            }
        }
        return s;
    }

    private static TextView setTextViewEnabled(TextView textView, boolean enabled,
            boolean isSecondary) {
        if (textView != null && textView.getResources() != null) {
            int colorResId;
            if (enabled) {
                if (isSecondary) {
                    colorResId = R.color.secondary_textcolor;
                } else {
                    colorResId = R.color.primary_textcolor;
                }
            } else {
                colorResId = R.color.disabled;
            }
            textView.setTextColor(textView.getResources().getColor(colorResId));
        }
        return textView;
    }
}
