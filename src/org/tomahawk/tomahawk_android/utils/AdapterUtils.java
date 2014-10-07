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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AdapterUtils {

    public static void fillContentHeader(Context context, ViewHolder viewHolder,
            final Album album, View.OnClickListener listener) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setVisibility(View.VISIBLE);
            viewHolder.getTextView1().setText(album.getName().toUpperCase());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                album.getImage(), Image.getLargeImageSize(), false);
        viewHolder.getMoreButton().setVisibility(View.VISIBLE);
        viewHolder.getMoreButton().setOnClickListener(listener);
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder,
            final Artist artist, View.OnClickListener listener) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setVisibility(View.VISIBLE);
            viewHolder.getTextView1().setText(artist.getName().toUpperCase());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(), artist.getImage(),
                Image.getLargeImageSize(), true);
        viewHolder.getMoreButton().setVisibility(View.VISIBLE);
        viewHolder.getMoreButton().setOnClickListener(listener);
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder, Playlist playlist,
            ArrayList<Image> images) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setVisibility(View.VISIBLE);
            viewHolder.getTextView1().setText(playlist.getName().toUpperCase());
        }
        if (images.size() > 3) {
            TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                    images.get(0), Image.getSmallImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView2(),
                    images.get(1), Image.getSmallImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView3(),
                    images.get(2), Image.getSmallImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView4(),
                    images.get(3), Image.getSmallImageSize(), false);
        } else if (images.size() > 0) {
            TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                    images.get(0), Image.getLargeImageSize(), false);
        }
    }

    public static void fillContentHeaderSmall(Context context, ViewHolder viewHolder, User user) {
        viewHolder.getTextView1().setText(user.getName().toUpperCase());
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getRoundedImage(),
                user.getImage(), Image.getSmallImageSize(), false);
        viewHolder.getRoundedImage().setVisibility(View.VISIBLE);
    }

    public static void fillContentHeader(final Context context, ViewHolder viewHolder,
            final User user, boolean showFollowing, boolean showNotFollowing) {
        TomahawkUtils.loadBlurredImageIntoImageView(context, viewHolder.getImageView1(),
                user.getImage(), Image.getSmallImageSize(), R.drawable.album_placeholder_grid);
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getRoundedImage(),
                user.getImage(), Image.getSmallImageSize(), false);
        viewHolder.getTextView1().setText(user.getName().toUpperCase());
        if (showFollowing) {
            viewHolder.getButton4().setBackgroundResource(R.drawable.following_button_bg_filled);
            ((TextView) viewHolder.getButton4().findViewById(R.id.content_header_button4_text))
                    .setText(context.getString(R.string.content_header_following).toUpperCase());
        } else if (showNotFollowing) {
            viewHolder.getButton4().setBackgroundResource(R.drawable.following_button_bg);
        } else {
            viewHolder.getButton4().setVisibility(View.GONE);
        }
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder, Query query) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setVisibility(View.VISIBLE);
            viewHolder.getTextView1().setText(query.getName().toUpperCase());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(), query.getImage(),
                Image.getLargeImageSize(), query.hasArtistImage());
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder, Image image) {
        TomahawkUtils.loadBlurredImageIntoImageView(context, viewHolder.getImageView1(), image,
                Image.getSmallImageSize(), R.drawable.album_placeholder_grid);
    }

    public static void fillView(ViewHolder viewHolder, Query query, String numerationString,
            boolean showAsPlaying, boolean showDuration, boolean hideArtistName) {
        if (!hideArtistName) {
            viewHolder.getTextView2().setVisibility(View.VISIBLE);
            viewHolder.getTextView2().setText(query.getArtist().getName());
        }
        viewHolder.getTextView3().setText(query.getName());
        setTextViewEnabled(viewHolder.getTextView2(), query.isPlayable(), false);
        setTextViewEnabled(viewHolder.getTextView3(), query.isPlayable(), false);
        if (numerationString != null) {
            if (showAsPlaying) {
                viewHolder.getImageView1().setVisibility(View.VISIBLE);
                Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                        viewHolder.getImageView1(), resolver.getIconPath(), true);
            } else {
                viewHolder.getTextView1().setVisibility(View.VISIBLE);
                viewHolder.getTextView1().setText(numerationString);
            }
        }
        if (showDuration) {
            viewHolder.getTextView4().setVisibility(View.VISIBLE);
            if (query.getPreferredTrack().getDuration() > 0) {
                viewHolder.getTextView4().setText(TomahawkUtils.durationToString(
                        (query.getPreferredTrack().getDuration())));
            } else {
                viewHolder.getTextView4().setText(PlaybackSeekBar.COMPLETION_STRING_DEFAULT);
            }
        }
    }

    public static void fillView(Context context, ViewHolder viewHolder, ListItemString string) {
        viewHolder.getTextView1().setText(string.getName());
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

    private static RotateAnimation constructRotateAnimation() {
        final RotateAnimation animation = new RotateAnimation(0.0f, 360.0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.49f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(1500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(RotateAnimation.INFINITE);
        return animation;
    }

    public static ArrayList<Album> getArtistAlbums(Artist artist, Collection collection) {
        if (collection != null) {
            return collection.getArtistAlbums(artist, false);
        } else {
            HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                    .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
            return hatchetCollection.getArtistAlbums(artist, false);
        }
    }

    public static ArrayList<Query> getArtistTracks(Artist artist, Collection collection) {
        if (collection != null) {
            return collection.getArtistTracks(artist, false);
        } else {
            HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                    .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
            return hatchetCollection.getArtistTracks(artist, false);
        }
    }

    public static ArrayList<Query> getAlbumTracks(Album album, Collection collection) {
        if (collection != null) {
            return collection.getAlbumTracks(album, false);
        } else {
            HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                    .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
            return hatchetCollection.getAlbumTracks(album, false);
        }
    }

    public static ArrayList<Query> getArtistTopHits(Artist artist) {
        HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        return hatchetCollection.getArtistTopHits(artist);
    }

    public static boolean allFromOneArtist(ArrayList<TomahawkListItem> items) {
        if (items.size() < 2) {
            return true;
        }
        TomahawkListItem item = items.get(0);
        for (int i = 1; i < items.size(); i++) {
            TomahawkListItem itemToCompare = items.get(i);
            if (itemToCompare.getArtist() != item.getArtist()) {
                return false;
            }
            item = itemToCompare;
        }
        return true;
    }
}
