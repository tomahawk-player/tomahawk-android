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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AdapterUtils {

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder,
            Album album, boolean isOnlyLocal) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(album.getName());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                album.getImage(), Image.IMAGE_SIZE_LARGE);
        int tracksCount = album.getQueries(isOnlyLocal).size();
        String s = album.getArtist().getName() + ", " + tracksCount + " "
                + activity.getString(R.string.category_header_track) + (tracksCount == 1 ? ""
                : "s");
        viewHolder.getTextView2().setText(s);
    }

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder,
            Artist artist, boolean isOnlyLocal) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(artist.getName());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils
                .loadImageIntoImageView(activity, viewHolder.getImageView1(), artist.getImage(),
                        Image.IMAGE_SIZE_LARGE);
        int topHitsCount = artist.getArtist().getTopHits().size();
        int albumsCount = isOnlyLocal ? artist.getLocalAlbums().size() : artist.getAlbums().size();
        String s = (isOnlyLocal ? "" : (topHitsCount + " "
                + activity.getString(R.string.category_header_tophit)
                + (topHitsCount == 1 ? "" : "s") + ", ")) + albumsCount + " "
                + activity.getString(R.string.category_header_album)
                + (albumsCount == 1 ? "" : "s");
        viewHolder.getTextView2().setText(s);
    }

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder,
            UserPlaylist userPlaylist, boolean isOnlyLocal) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(userPlaylist.getName());
        }
        int tracksCount = userPlaylist.getQueries(isOnlyLocal).size();
        String s = tracksCount + " " + activity.getString(R.string.category_header_track)
                + (tracksCount == 1 ? "" : "s");
        viewHolder.getTextView2().setText(s);
        ArrayList<Artist> artists = userPlaylist.getContentHeaderArtists();
        ArrayList<Artist> artistsWithImage = new ArrayList<Artist>();
        for (Artist artist : artists) {
            if (artist.getImage() != null) {
                artistsWithImage.add(artist);
            }
        }
        if (artistsWithImage.size() > 0) {
            TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                    artistsWithImage.get(0).getImage(), Image.IMAGE_SIZE_LARGE);
        }
        if (artistsWithImage.size() > 3) {
            activity.findViewById(R.id.content_header_image_frame2)
                    .setVisibility(View.VISIBLE);
            viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
            TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView2(),
                    artistsWithImage.get(1).getImage(), Image.IMAGE_SIZE_LARGE);
            viewHolder.getImageView3().setVisibility(ImageView.VISIBLE);
            TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView3(),
                    artistsWithImage.get(2).getImage(), Image.IMAGE_SIZE_LARGE);
            viewHolder.getImageView4().setVisibility(ImageView.VISIBLE);
            TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView4(),
                    artistsWithImage.get(3).getImage(), Image.IMAGE_SIZE_LARGE);
        }
    }

    public static void fillContentHeaderSmall(Activity activity, ViewHolder viewHolder, User user) {
        viewHolder.getTextView1().setText(user.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadDrawableIntoImageView(activity, viewHolder.getImageView1(),
                R.drawable.no_album_art_placeholder);
        TomahawkUtils.loadRoundedImageIntoImageView(activity, viewHolder.getRoundedImage(),
                user.getImage(), Image.IMAGE_SIZE_LARGE);
        viewHolder.getRoundedImage().setVisibility(View.VISIBLE);
    }

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder, User user) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadDrawableIntoImageView(activity, viewHolder.getImageView1(),
                R.drawable.no_album_art_placeholder);
        TomahawkUtils.loadRoundedImageIntoImageView(activity, viewHolder.getRoundedImage(),
                user.getImage(), Image.IMAGE_SIZE_LARGE);
        viewHolder.getRoundedImage().setVisibility(View.VISIBLE);
        viewHolder.getTextView2().setText(user.getAbout());
        viewHolder.getTextView3().setText("" + user.getTotalPlays());
        viewHolder.getTextView4().setText("" + user.getFollowCount());
        viewHolder.getTextView5().setText("" + user.getFollowersCount());
        if (user.getNowPlaying() != null) {
            viewHolder.getTextView6().setText(activity.getString(R.string.content_header_nowplaying)
                    + " " + user.getNowPlaying().getName() + " "
                    + activity.getString(R.string.album_by_artist)
                    + " " + user.getNowPlaying().getArtist().getName());
        }
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, View rootView,
            Query query, boolean showHighlighted, boolean showAsPlaying, boolean showResolvedBy) {
        viewHolder.getTextView1().setText(query.getName());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(query.getArtist().getName());
        viewHolder.getTextView5().setVisibility(View.VISIBLE);
        if (query.getPreferredTrack().getDuration() > 0) {
            viewHolder.getTextView5().setText(TomahawkUtils.durationToString(
                    (query.getPreferredTrack().getDuration())));
        } else {
            viewHolder.getTextView5().setText(activity.getResources().getString(
                    R.string.playbackactivity_seekbar_completion_time_string));
        }
        setTextViewEnabled(viewHolder.getTextView1(), query.isPlayable());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        setTextViewEnabled(viewHolder.getTextView4(), query.isPlayable());
        setTextViewEnabled(viewHolder.getTextView5(), query.isPlayable());
        Resources resources = activity.getResources();
        if (showHighlighted) {
            rootView.setBackgroundResource(R.color.tomahawk_red_transparent);
            viewHolder.getTextView1()
                    .setTextColor(resources.getColor(R.color.primary_textcolor_inverted));
            viewHolder.getTextView4()
                    .setTextColor(resources.getColor(R.color.secondary_textcolor_inverted));
            viewHolder.getTextView5()
                    .setTextColor(resources.getColor(R.color.secondary_textcolor_inverted));
            if (showAsPlaying) {
                viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadDrawableIntoImageView(activity,
                        viewHolder.getImageView1(),
                        R.drawable.ic_playlist_is_playing);
            }
        } else {
            viewHolder.getTextView1().setTextColor(resources.getColor(R.color.primary_textcolor));
            viewHolder.getTextView4().setTextColor(resources.getColor(R.color.secondary_textcolor));
            viewHolder.getTextView5().setTextColor(resources.getColor(R.color.secondary_textcolor));
            rootView.setBackgroundResource(
                    R.drawable.selectable_background_tomahawk_opaque);
        }
        if (showResolvedBy && query.getPreferredTrackResult() != null) {
            viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
            viewHolder.getImageView2().setImageDrawable(
                    query.getPreferredTrackResult().getResolvedBy().getIcon());
        }
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, User user) {
        viewHolder.getTextView1().setText(user.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                user.getImage(), Image.IMAGE_SIZE_SMALL);
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, Artist artist) {
        viewHolder.getTextView1().setText(artist.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                artist.getImage(), Image.IMAGE_SIZE_SMALL);
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, Album album) {
        viewHolder.getTextView1().setText(album.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                album.getImage(), Image.IMAGE_SIZE_SMALL);
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(album.getArtist().getName());
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, View rootView,
            SocialAction socialAction, boolean showHighlighted, boolean showAsPlaying,
            boolean showResolvedBy) {
        Resources resources = activity.getResources();
        TomahawkListItem targetObject = socialAction.getTargetObject();
        rootView.setBackgroundResource(
                R.drawable.selectable_background_tomahawk_opaque);
        if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE
                .equals(socialAction.getType())) {
            boolean action = Boolean.valueOf(socialAction.getAction());
            if (targetObject instanceof Query) {
                Query query = (Query) targetObject;
                String phrase = action ?
                        resources.getString(R.string.socialaction_type_love_track_true)
                        : resources
                                .getString(R.string.socialaction_type_love_track_false);
                viewHolder.getTextView1()
                        .setText(socialAction.getUser().getName() + " " + phrase);
                viewHolder.getTextView2().setVisibility(View.VISIBLE);
                viewHolder.getTextView2().setText(query.getName());
                viewHolder.getTextView3().setVisibility(View.VISIBLE);
                viewHolder.getTextView3()
                        .setText(query.getArtist().getName());
                if (showHighlighted) {
                    rootView.setBackgroundResource(R.color.tomahawk_red_transparent);
                    if (showAsPlaying) {
                        viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                        TomahawkUtils.loadDrawableIntoImageView(activity,
                                viewHolder.getImageView1(),
                                R.drawable.ic_playlist_is_playing);
                    }
                }
                if (showResolvedBy && query.getPreferredTrackResult() != null) {
                    viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
                    viewHolder.getImageView2().setImageDrawable(
                            query.getPreferredTrackResult().getResolvedBy().getIcon());
                }
            } else if (targetObject instanceof Artist
                    || targetObject instanceof Album) {
                String firstLine = "";
                String phrase = action ?
                        resources.getString(R.string.socialaction_type_starred_true)
                        : resources.getString(R.string.socialaction_type_starred_false);
                firstLine += socialAction.getUser().getName() + " " + phrase
                        + " " + targetObject.getName();
                if (targetObject instanceof Album) {
                    firstLine += resources.getString(R.string.album_by_artist) + " "
                            + targetObject.getArtist().getName();
                }
                viewHolder.getTextView1().setText(firstLine);
            }
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_follow_true);
            viewHolder.getTextView1()
                    .setText(socialAction.getUser().getName() + " " + phrase
                            + " " + targetObject.getName());
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATECOMMENT
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_createcomment);
            if (targetObject instanceof Query) {
                Query query = (Query) targetObject;
                viewHolder.getTextView1().setText(socialAction.getUser().getName()
                        + " " + phrase + " " + query.getName() + " "
                        + resources.getString(R.string.album_by_artist) + " "
                        + query.getArtist().getName() + ":");
                if (showHighlighted) {
                    rootView.setBackgroundResource(R.color.tomahawk_red_transparent);
                    if (showAsPlaying) {
                        viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                        TomahawkUtils.loadDrawableIntoImageView(activity,
                                viewHolder.getImageView1(),
                                R.drawable.ic_playlist_is_playing);
                    }
                }
                if (showResolvedBy && query.getPreferredTrackResult() != null) {
                    viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
                    viewHolder.getImageView2().setImageDrawable(
                            query.getPreferredTrackResult().getResolvedBy().getIcon());
                }
            } else {
                viewHolder.getTextView1().setText(socialAction.getUser().getName() + " " + phrase
                        + " " + targetObject.getName() + ":");
            }
            viewHolder.getTextView2().setVisibility(View.VISIBLE);
            viewHolder.getTextView2().setText(socialAction.getAction());
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_latchon);
            viewHolder.getTextView1().setText(socialAction.getUser().getName() + " " + phrase
                    + " " + targetObject.getName());
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHOFF
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_latchoff);
            viewHolder.getTextView1().setText(socialAction.getUser().getName() + " " + phrase
                    + " " + targetObject.getName());
        } else {
            // Fallback, if no view is set yet
            viewHolder.getTextView1().setText("!FIXME! type: " + socialAction.getType()
                    + ", action: " + socialAction.getAction());
        }
        String fourthLine = dateToString(resources, socialAction.getDate());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(fourthLine);
    }

    private static String dateToString(Resources resources, Date date) {
        String s = "";
        if (date != null) {
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 60000) {
                s += TimeUnit.MILLISECONDS.toSeconds(diff) + " "
                        + resources.getString(R.string.time_seconds);
            } else if (diff < 3600000) {
                s += TimeUnit.MILLISECONDS.toMinutes(diff) + " "
                        + resources.getString(R.string.time_minutes);
            } else if (diff < 86400000) {
                s += TimeUnit.MILLISECONDS.toHours(diff) + " "
                        + resources.getString(R.string.time_hours);
            } else {
                s += TimeUnit.MILLISECONDS.toDays(diff) + " "
                        + resources.getString(R.string.time_days);
            }
            s += " " + resources.getString(R.string.time_ago);
        }
        return s;
    }

    private static TextView setTextViewEnabled(TextView textView, boolean enabled) {
        if (textView != null && textView.getResources() != null) {
            int colorResId;
            if (enabled) {
                colorResId = R.color.primary_textcolor;
            } else {
                colorResId = R.color.disabled;
            }
            textView.setTextColor(textView.getResources().getColor(colorResId));
        }
        return textView;
    }
}
