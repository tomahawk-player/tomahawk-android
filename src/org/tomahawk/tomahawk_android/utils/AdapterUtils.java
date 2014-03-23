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
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                album.getImage(), Image.IMAGE_SIZE_LARGE);
        int tracksCount = album.getQueries(isOnlyLocal).size();
        String s = album.getArtist().getName() + ", " + tracksCount + " "
                + activity.getString(R.string.content_header_track) + (tracksCount == 1 ? "" : "s");
        if (viewHolder.getTextFirstLine() != null) {
            viewHolder.getTextFirstLine().setText(s);
        }
    }

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder,
            Artist artist, boolean isOnlyLocal) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        if (viewHolder.getTextFirstLine() != null) {
            viewHolder.getTextFirstLine().setText(artist.getName());
        }
        TomahawkUtils
                .loadImageIntoImageView(activity, viewHolder.getImageView1(), artist.getImage(),
                        Image.IMAGE_SIZE_LARGE);
        int topHitsCount = artist.getArtist().getTopHits().size();
        int albumsCount = isOnlyLocal ? artist.getLocalAlbums().size() : artist.getAlbums().size();
        String s = (isOnlyLocal ? "" : (topHitsCount + " "
                + activity.getString(R.string.content_header_tophit)
                + (topHitsCount == 1 ? "" : "s") + ", ")) + albumsCount + " "
                + activity.getString(R.string.content_header_album)
                + (albumsCount == 1 ? "" : "s");
        viewHolder.getTextSecondLine().setText(s);
    }

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder,
            UserPlaylist userPlaylist, boolean isOnlyLocal) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        if (viewHolder.getTextFirstLine() != null) {
            viewHolder.getTextFirstLine().setText(userPlaylist.getName());
        }
        int tracksCount = userPlaylist.getQueries(isOnlyLocal).size();
        String s = tracksCount + " " + activity.getString(R.string.content_header_track)
                + (tracksCount == 1 ? "" : "s");
        viewHolder.getTextSecondLine().setText(s);
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

    public static void fillContentHeader(Activity activity, ViewHolder viewHolder, User user) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        if (viewHolder.getTextFirstLine() != null) {
            viewHolder.getTextFirstLine().setText(user.getName());
        }
        TomahawkUtils.loadDrawableIntoImageView(activity, viewHolder.getImageView1(),
                R.drawable.dummy_user_header);
        ImageView roundedImageView =
                (ImageView) activity.findViewById(R.id.content_header_roundedimage);
        TomahawkUtils.loadRoundedImageIntoImageView(activity, roundedImageView, user.getImage(),
                Image.IMAGE_SIZE_LARGE);
        roundedImageView.setVisibility(View.VISIBLE);
        int followersCount = user.getFollowersCount();
        int followCount = user.getFollowCount();
        String s = "Followers: " + followersCount + ", Following: " + followCount;
        viewHolder.getTextSecondLine().setText(s);
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, View rootView,
            Query query, boolean showHighlighted, boolean showAsPlaying, boolean showResolvedBy) {
        viewHolder.getTextFirstLine().setText(query.getName());
        viewHolder.getTextFourthLine().setVisibility(View.VISIBLE);
        viewHolder.getTextFourthLine().setText(query.getArtist().getName());
        viewHolder.getTextFifthLine().setVisibility(View.VISIBLE);
        if (query.getPreferredTrack().getDuration() > 0) {
            viewHolder.getTextFifthLine().setText(TomahawkUtils.durationToString(
                    (query.getPreferredTrack().getDuration())));
        } else {
            viewHolder.getTextFifthLine().setText(activity.getResources().getString(
                    R.string.playbackactivity_seekbar_completion_time_string));
        }
        setTextViewEnabled(viewHolder.getTextFirstLine(), query.isPlayable());
        viewHolder.getTextFourthLine().setVisibility(View.VISIBLE);
        setTextViewEnabled(viewHolder.getTextFourthLine(), query.isPlayable());
        setTextViewEnabled(viewHolder.getTextFifthLine(), query.isPlayable());
        if (showHighlighted) {
            rootView.setBackgroundResource(R.color.pressed_tomahawk);
            if (showAsPlaying) {
                viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadDrawableIntoImageView(activity,
                        viewHolder.getImageView1(),
                        R.drawable.ic_playlist_is_playing);
            }
        } else {
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
        viewHolder.getTextFirstLine().setText(user.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                user.getImage(), Image.IMAGE_SIZE_SMALL);
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, Artist artist) {
        viewHolder.getTextFirstLine().setText(artist.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                artist.getImage(), Image.IMAGE_SIZE_SMALL);
    }

    public static void fillView(Activity activity, ViewHolder viewHolder, Album album) {
        viewHolder.getTextFirstLine().setText(album.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(activity, viewHolder.getImageView1(),
                album.getImage(), Image.IMAGE_SIZE_SMALL);
        viewHolder.getTextFourthLine().setVisibility(View.VISIBLE);
        viewHolder.getTextFourthLine().setText(album.getArtist().getName());
    }

    public static void fillView(Activity activity, ViewHolder viewHolder,
            SocialAction socialAction) {
        Resources resources = activity.getResources();
        TomahawkListItem targetObject = socialAction.getTargetObject();
        if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE
                .equals(socialAction.getType())) {
            if (targetObject instanceof Query) {
                String phrase = socialAction.getAction() ?
                        resources.getString(R.string.socialaction_type_love_track_true)
                        : resources
                                .getString(R.string.socialaction_type_love_track_false);
                viewHolder.getTextFirstLine()
                        .setText(socialAction.getUser().getName() + " " + phrase);
                viewHolder.getTextSecondLine().setVisibility(View.VISIBLE);
                viewHolder.getTextSecondLine().setText(targetObject.getName());
                viewHolder.getTextThirdLine().setVisibility(View.VISIBLE);
                viewHolder.getTextThirdLine()
                        .setText(targetObject.getArtist().getName());
            } else if (targetObject instanceof Artist
                    || targetObject instanceof Album) {
                String firstLine = "";
                String phrase = socialAction.getAction() ?
                        resources.getString(R.string.socialaction_type_starred_true)
                        : resources.getString(R.string.socialaction_type_starred_false);
                firstLine += socialAction.getUser().getName() + " " + phrase
                        + " " + targetObject.getName();
                if (targetObject instanceof Album) {
                    firstLine += resources.getString(R.string.album_by_artist) + " "
                            + targetObject.getArtist().getName();
                }
                viewHolder.getTextFirstLine().setText(firstLine);
            }
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_follow_true);
            viewHolder.getTextFirstLine()
                    .setText(socialAction.getUser().getName() + " " + phrase
                            + " " + targetObject.getName());
        }
        String fourthLine = dateToString(resources,socialAction.getDate());
        viewHolder.getTextFourthLine().setVisibility(View.VISIBLE);
        viewHolder.getTextFourthLine().setText(fourthLine);
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
                colorResId = R.color.disabled_grey;
            }
            textView.setTextColor(textView.getResources().getColor(colorResId));
        }
        return textView;
    }
}
