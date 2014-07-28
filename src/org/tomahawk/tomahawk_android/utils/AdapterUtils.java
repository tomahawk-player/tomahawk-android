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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.fragments.UsersFragment;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AdapterUtils {

    public static void fillContentHeader(Context context, ViewHolder viewHolder,
            final Album album, Collection collection) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(album.getName());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                album.getImage(), Image.getLargeImageSize());
        int tracksCount = getAlbumTracks(album, collection).size();
        String s = album.getArtist().getName() + ", " + tracksCount + " "
                + context.getString(R.string.category_header_track) + (tracksCount == 1 ? ""
                : "s");
        viewHolder.getTextView2().setText(s);
        if (DatabaseHelper.getInstance().isItemLoved(album)) {
            viewHolder.getStarButton().setImageResource(R.drawable.ic_action_starred);
        } else {
            viewHolder.getStarButton().setImageResource(R.drawable.ic_action_notstarred);
        }
        viewHolder.getStarButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CollectionManager.getInstance().toggleLovedItem(album);
            }
        });
        viewHolder.getStarButton().setVisibility(View.VISIBLE);
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder,
            final Artist artist, Collection collection) {
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(artist.getName());
        }
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils
                .loadImageIntoImageView(context, viewHolder.getImageView1(), artist.getImage(),
                        Image.getLargeImageSize());
        int topHitsCount = 0;
        ArrayList<Query> topHits = AdapterUtils.getArtistTopHits(artist);
        if (topHits != null) {
            topHitsCount = topHits.size();
        }
        int albumsCount = getArtistAlbums(artist, collection).size();
        String s = (collection != null ? "" : (topHitsCount + " "
                + context.getString(R.string.category_header_tophit)
                + (topHitsCount == 1 ? "" : "s") + ", ")) + albumsCount + " "
                + context.getString(R.string.category_header_album)
                + (albumsCount == 1 ? "" : "s");
        viewHolder.getTextView2().setText(s);
        if (DatabaseHelper.getInstance().isItemLoved(artist)) {
            viewHolder.getStarButton().setImageResource(R.drawable.ic_action_starred);
        } else {
            viewHolder.getStarButton().setImageResource(R.drawable.ic_action_notstarred);
        }
        viewHolder.getStarButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CollectionManager.getInstance().toggleLovedItem(artist);
            }
        });
        viewHolder.getStarButton().setVisibility(View.VISIBLE);
    }

    public static void fillContentHeader(Context context, ViewHolder viewHolder,
            Playlist playlist) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        if (viewHolder.getTextView1() != null) {
            viewHolder.getTextView1().setText(playlist.getName());
        }
        int tracksCount = playlist.getQueries().size();
        String s = tracksCount + " " + context.getString(R.string.category_header_track)
                + (tracksCount == 1 ? "" : "s");
        viewHolder.getTextView2().setText(s);
        if (playlist.getContentHeaderArtists().size() > 0) {
            ArrayList<Artist> artistsWithImage = new ArrayList<Artist>();
            synchronized (playlist) {
                ArrayList<Artist> artists = playlist.getContentHeaderArtists();
                for (Artist artist : artists) {
                    if (artist.getImage() != null) {
                        artistsWithImage.add(artist);
                    }
                }
            }
            if (artistsWithImage.size() > 3) {
                viewHolder.getImageViewFrame().setVisibility(View.VISIBLE);

                TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                        artistsWithImage.get(0).getImage(), Image.getSmallImageSize());
                viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView2(),
                        artistsWithImage.get(1).getImage(), Image.getSmallImageSize());
                viewHolder.getImageView3().setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView3(),
                        artistsWithImage.get(2).getImage(), Image.getSmallImageSize());
                viewHolder.getImageView4().setVisibility(ImageView.VISIBLE);
                TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView4(),
                        artistsWithImage.get(3).getImage(), Image.getSmallImageSize());
            } else if (artistsWithImage.size() > 0) {
                TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                        artistsWithImage.get(0).getImage(), Image.getLargeImageSize());
            }
        }
    }

    public static void fillContentHeaderSmall(Context context, ViewHolder viewHolder, User user) {
        viewHolder.getTextView1().setText(user.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadDrawableIntoImageView(context, viewHolder.getImageView1(),
                R.drawable.no_album_art_placeholder);
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getRoundedImage(),
                user.getImage(), Image.getLargeImageSize());
        viewHolder.getRoundedImage().setVisibility(View.VISIBLE);
    }

    public static void fillContentHeader(final FragmentManager fragmentManager,
            final Context context, ViewHolder viewHolder, final User user) {
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadDrawableIntoImageView(context, viewHolder.getImageView1(),
                R.drawable.no_album_art_placeholder);
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getRoundedImage(),
                user.getImage(), Image.getLargeImageSize());
        viewHolder.getRoundedImage().setVisibility(View.VISIBLE);
        viewHolder.getTextView1().setText(user.getAbout());
        if (user.getNowPlaying() != null) {
            viewHolder.getTextView2().setText(context.getString(R.string.content_header_nowplaying)
                    + " " + user.getNowPlaying().getName() + " "
                    + context.getString(R.string.album_by_artist)
                    + " " + user.getNowPlaying().getArtist().getName());
        }
        viewHolder.getTextView3().setText("" + user.getTotalPlays());
        viewHolder.getTextView4().setText("" + user.getFollowCount());
        viewHolder.getTextView5().setText("" + user.getFollowersCount());
        viewHolder.getButton1().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentUtils.replace(context, fragmentManager,
                        TracksFragment.class, user.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        SocialActionsFragment.SHOW_MODE_DASHBOARD);
            }
        });
        viewHolder.getButton2().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentUtils.replace(context, fragmentManager,
                        UsersFragment.class, user.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        UsersFragment.SHOW_MODE_TYPE_FOLLOWINGS);
            }
        });
        viewHolder.getButton3().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentUtils.replace(context, fragmentManager,
                        UsersFragment.class, user.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        UsersFragment.SHOW_MODE_TYPE_FOLLOWERS);
            }
        });
    }

    public static void fillView(Context context, ViewHolder viewHolder, Query query,
            boolean showAsPlaying, boolean showResolvedBy) {
        viewHolder.getTextView1().setText(query.getName());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(query.getArtist().getName());
        viewHolder.getTextView5().setVisibility(View.VISIBLE);
        if (query.getPreferredTrack().getDuration() > 0) {
            viewHolder.getTextView5().setText(TomahawkUtils.durationToString(
                    (query.getPreferredTrack().getDuration())));
        } else {
            viewHolder.getTextView5().setText(context.getString(
                    R.string.playbackactivity_seekbar_completion_time_string));
        }
        boolean isHighlighted =
                viewHolder.getViewType() == R.id.tomahawklistadapter_viewtype_listitemhighlighted;
        setTextViewEnabled(viewHolder.getTextView1(), query.isPlayable(), false, isHighlighted);
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        setTextViewEnabled(viewHolder.getTextView4(), query.isPlayable(), true, isHighlighted);
        setTextViewEnabled(viewHolder.getTextView5(), query.isPlayable(), true, isHighlighted);
        if (showAsPlaying) {
            viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
            viewHolder.getImageView1().setBackgroundResource(R.drawable.ic_action_album_light);
            if (viewHolder.getImageView1().getAnimation() == null) {
                viewHolder.getImageView1().startAnimation(constructRotateAnimation());
            }
        } else {
            viewHolder.getImageView1().clearAnimation();
        }
        if (showResolvedBy && query.getPreferredTrackResult() != null) {
            viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
            Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
            if (resolver.getIconPath() != null) {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                        viewHolder.getImageView2(), resolver.getIconPath(), false);
            } else {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                        viewHolder.getImageView2(), resolver.getIconResId(), false);
            }
        }
    }

    public static void fillView(Context context, ViewHolder viewHolder, User user) {
        viewHolder.getTextView1().setText(user.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getImageView1(),
                user.getImage(), Image.getSmallImageSize());
    }

    public static void fillView(Context context, ViewHolder viewHolder, Artist artist) {
        viewHolder.getTextView1().setText(artist.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                artist.getImage(), Image.getSmallImageSize());
    }

    public static void fillView(Context context, ViewHolder viewHolder, Album album) {
        viewHolder.getTextView1().setText(album.getName());
        viewHolder.getImageView1().setVisibility(View.VISIBLE);
        TomahawkUtils.loadImageIntoImageView(context, viewHolder.getImageView1(),
                album.getImage(), Image.getSmallImageSize());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(album.getArtist().getName());
    }

    public static void fillView(Context context, ViewHolder viewHolder, SocialAction socialAction,
            boolean showAsPlaying, boolean showResolvedBy) {
        Resources resources = context.getResources();
        TomahawkListItem targetObject = socialAction.getTargetObject();
        viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
        TomahawkUtils.loadRoundedImageIntoImageView(context, viewHolder.getImageView1(),
                socialAction.getUser().getImage(), Image.getSmallImageSize());
        if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE
                .equals(socialAction.getType())) {
            boolean action = Boolean.valueOf(socialAction.getAction());
            if (targetObject instanceof Query) {
                Query query = (Query) targetObject;
                String phrase = action ?
                        resources.getString(R.string.socialaction_type_love_track_true)
                        : resources.getString(R.string.socialaction_type_love_track_false);
                viewHolder.getTextView1()
                        .setText(socialAction.getUser().getName() + " " + phrase);
                viewHolder.getTextView2().setVisibility(View.VISIBLE);
                viewHolder.getTextView2().setText(query.getName());
                viewHolder.getTextView3().setVisibility(View.VISIBLE);
                viewHolder.getTextView3().setText(query.getArtist().getName());
                if (showAsPlaying) {
                    viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                    viewHolder.getImageView1().setImageResource(R.drawable.ic_action_album_light);
                    if (viewHolder.getImageView1().getAnimation() == null) {
                        viewHolder.getImageView1().startAnimation(constructRotateAnimation());
                    }
                } else {
                    viewHolder.getImageView1().clearAnimation();
                }
                if (showResolvedBy && query.getPreferredTrackResult() != null) {
                    viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
                    Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
                    if (resolver.getIconPath() != null) {
                        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                viewHolder.getImageView2(), resolver.getIconPath(), false);
                    } else {
                        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                viewHolder.getImageView2(), resolver.getIconResId(), false);
                    }
                }
            } else if (targetObject instanceof Artist || targetObject instanceof Album) {
                String firstLine = "";
                String phrase = action ?
                        resources.getString(R.string.socialaction_type_starred_true)
                        : resources.getString(R.string.socialaction_type_starred_false);
                firstLine += socialAction.getUser().getName() + " " + phrase
                        + " " + targetObject.getName();
                if (targetObject instanceof Album) {
                    firstLine += " " + resources.getString(R.string.album_by_artist) + " "
                            + targetObject.getArtist().getName();
                }
                viewHolder.getTextView1().setText(firstLine);
            }
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_follow_true);
            viewHolder.getTextView1().setText(socialAction.getUser().getName() + " " + phrase
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
                if (showAsPlaying) {
                    viewHolder.getImageView1().setVisibility(ImageView.VISIBLE);
                    viewHolder.getImageView1().setImageResource(R.drawable.ic_action_album_light);
                    if (viewHolder.getImageView1().getAnimation() == null) {
                        viewHolder.getImageView1().startAnimation(constructRotateAnimation());
                    }
                } else {
                    viewHolder.getImageView1().clearAnimation();
                }
                if (showResolvedBy && query.getPreferredTrackResult() != null) {
                    viewHolder.getImageView2().setVisibility(ImageView.VISIBLE);
                    Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
                    if (resolver.getIconPath() != null) {
                        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                viewHolder.getImageView2(), resolver.getIconPath(), false);
                    } else {
                        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                viewHolder.getImageView2(), resolver.getIconResId(), false);
                    }
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
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST
                .equals(socialAction.getType())) {
            String phrase = resources.getString(R.string.socialaction_type_createplaylist);
            viewHolder.getTextView1().setText(socialAction.getUser().getName() + " " + phrase);
        } else {
            // Fallback, if no view is set yet
            viewHolder.getTextView1().setText("!FIXME! type: " + socialAction.getType()
                    + ", action: " + socialAction.getAction());
        }
        String fourthLine = dateToString(resources, socialAction.getDate());
        viewHolder.getTextView4().setVisibility(View.VISIBLE);
        viewHolder.getTextView4().setText(fourthLine);
        viewHolder.getImageView3().setVisibility(View.VISIBLE);
        viewHolder.getImageView3().setImageResource(R.drawable.ic_action_time);
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
                    s += minutes + " " + resources.getString(R.string.time_minutes);
                }
            } else if (diff < 86400000) {
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                if (hours < 2) {
                    s += resources.getString(R.string.time_anhour);
                } else {
                    s += hours + " " + resources.getString(R.string.time_hours);
                }
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                if (days < 2) {
                    s += resources.getString(R.string.time_aday);
                } else {
                    s += days + " " + resources.getString(R.string.time_days);
                }
            }
            s += " " + resources.getString(R.string.time_ago);
        }
        return s;
    }

    private static TextView setTextViewEnabled(TextView textView, boolean enabled,
            boolean isSecondary, boolean isHighlighted) {
        if (textView != null && textView.getResources() != null) {
            int colorResId;
            if (enabled) {
                if (isSecondary) {
                    if (isHighlighted) {
                        colorResId = R.color.secondary_textcolor_inverted;
                    } else {
                        colorResId = R.color.secondary_textcolor;
                    }
                } else {
                    if (isHighlighted) {
                        colorResId = R.color.primary_textcolor_inverted;
                    } else {
                        colorResId = R.color.primary_textcolor;
                    }
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
}
