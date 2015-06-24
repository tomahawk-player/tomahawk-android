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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.content.res.Resources;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ViewHolder {

    final int mLayoutId;

    private final View mRootView;

    private final Map<Integer, View> mCachedViews = new HashMap<>();

    public ViewHolder(View rootView, int layoutId) {
        mLayoutId = layoutId;
        mRootView = rootView;
    }

    public View ensureInflation(int stubResId, int inflatedId) {
        return TomahawkUtils.ensureInflation(mRootView, stubResId, inflatedId);
    }

    public View findViewById(int id) {
        if (mCachedViews.containsKey(id)) {
            return mCachedViews.get(id);
        } else {
            View view = mRootView.findViewById(id);
            if (view != null) {
                mCachedViews.put(id, view);
            }
            return view;
        }
    }

    public void setMainClickListener(ClickListener listener) {
        View mainClickArea = findViewById(R.id.mainclickarea);
        mainClickArea.setOnClickListener(listener);
        mainClickArea.setOnLongClickListener(listener);
    }

    public void fillView(Query query, String numerationString, boolean showAsPlaying,
            View.OnClickListener swipeMenuButton1Listener, boolean showAsQueued) {
        TextView trackNameTextView = (TextView) findViewById(R.id.track_textview);
        trackNameTextView.setText(query.getPrettyName());
        setTextViewEnabled(trackNameTextView, query.isPlayable(), false);

        ImageView resolverImageView = (ImageView) ensureInflation(R.id.resolver_imageview_stub,
                R.id.resolver_imageview);
        TextView numerationTextView = (TextView) findViewById(R.id.numeration_textview);
        if (showAsQueued) {
            if (numerationTextView != null) {
                numerationTextView.setVisibility(View.GONE);
            }
            if (resolverImageView != null) {
                resolverImageView.setVisibility(View.VISIBLE);
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), resolverImageView,
                        R.drawable.ic_action_queue_red);
            }
        } else if (showAsPlaying) {
            if (numerationTextView != null) {
                numerationTextView.setVisibility(View.GONE);
            }
            if (resolverImageView != null) {
                resolverImageView.setVisibility(View.VISIBLE);
                if (query.getPreferredTrackResult() != null) {
                    Resolver resolver = query.getPreferredTrackResult().getResolvedBy();
                    resolver.loadIcon(resolverImageView, false);
                }
            }
        } else if (numerationString != null) {
            if (resolverImageView != null) {
                resolverImageView.setVisibility(View.GONE);
            }
            if (numerationTextView != null) {
                numerationTextView.setVisibility(View.VISIBLE);
                numerationTextView.setText(numerationString);
                setTextViewEnabled(numerationTextView, query.isPlayable(), false);
            }
        }
        if (mLayoutId == R.layout.list_item_numeration_track_artist
                || mLayoutId == R.layout.list_item_track_artist) {
            TextView artistNameTextView = (TextView) findViewById(R.id.artist_textview);
            artistNameTextView.setText(query.getArtist().getPrettyName());
            setTextViewEnabled(artistNameTextView, query.isPlayable(), false);
        }
        if (mLayoutId == R.layout.list_item_numeration_track_duration) {
            TextView durationTextView = (TextView) findViewById(R.id.duration_textview);
            if (query.getPreferredTrack().getDuration() > 0) {
                durationTextView.setText(TomahawkUtils.durationToString(
                        (query.getPreferredTrack().getDuration())));
            } else {
                durationTextView.setText(PlaybackPanel.COMPLETION_STRING_DEFAULT);
            }
            setTextViewEnabled(durationTextView, query.isPlayable(), false);
        }
        ImageView swipeMenuButton;
        if (showAsQueued) {
            swipeMenuButton = (ImageView) ensureInflation(R.id.swipe_menu_button_dequeue_stub,
                    R.id.swipe_menu_button_dequeue);
            swipeMenuButton.setVisibility(View.VISIBLE);
            swipeMenuButton.setImageResource(R.drawable.ic_player_exit_light);
            TomahawkUtils.setTint(swipeMenuButton.getDrawable(), R.color.tomahawk_red);
            ImageView swipeMenuButtonEnqueue =
                    (ImageView) findViewById(R.id.swipe_menu_button_enqueue);
            if (swipeMenuButtonEnqueue != null) {
                swipeMenuButtonEnqueue.setVisibility(View.GONE);
            }
        } else {
            swipeMenuButton = (ImageView) ensureInflation(R.id.swipe_menu_button_enqueue_stub,
                    R.id.swipe_menu_button_enqueue);
            swipeMenuButton.setVisibility(View.VISIBLE);
            ImageView swipeMenuButtonDequeue =
                    (ImageView) findViewById(R.id.swipe_menu_button_dequeue);
            if (swipeMenuButtonDequeue != null) {
                swipeMenuButtonDequeue.setVisibility(View.GONE);
            }
        }
        swipeMenuButton.setOnClickListener(swipeMenuButton1Listener);
    }

    public void fillView(String string) {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(string);
    }

    public void fillView(User user) {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(user.getName());
        if (mLayoutId == R.layout.list_item_user) {
            TextView textView2 = (TextView) findViewById(R.id.textview2);
            textView2.setText(TomahawkApp.getContext().getString(R.string.followers_count,
                    user.getFollowersCount(), user.getFollowCount()));
        }
        TextView userTextView1 = (TextView) findViewById(R.id.usertextview1);
        ImageView userImageView1 = (ImageView) findViewById(R.id.userimageview1);
        TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                userImageView1, user, Image.getSmallImageSize(),
                userTextView1);
    }

    public void fillView(Artist artist) {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(artist.getPrettyName());
        ImageView imageView1 = (ImageView) findViewById(R.id.imageview1);
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView1,
                artist.getImage(), Image.getSmallImageSize(), true);
    }

    public void fillView(final Album album, Collection collection) {
        if (collection == null) {
            collection =
                    CollectionManager.getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        }
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(album.getPrettyName());
        TextView textView2 = (TextView) findViewById(R.id.textview2);
        textView2.setText(album.getArtist().getPrettyName());
        ImageView imageView1 = (ImageView) findViewById(R.id.imageview1);
        TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView1,
                album.getImage(), Image.getSmallImageSize(), false);
        final TextView textView3 = (TextView) findViewById(R.id.textview3);
        collection.getAlbumTracks(album).done(new DoneCallback<List<Query>>() {
            @Override
            public void onDone(List<Query> result) {
                textView3.setVisibility(View.VISIBLE);
                textView3.setText(TomahawkApp.getContext().getResources()
                        .getQuantityString(R.plurals.songs_with_count, result.size(),
                                result.size()));
            }
        });
    }

    public void fillView(Resolver resolver) {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(resolver.getPrettyName());
        ImageView imageView1 = (ImageView) findViewById(R.id.imageview1);
        imageView1.clearColorFilter();
        resolver.loadIconBackground(imageView1, !resolver.isEnabled());
        ImageView imageView2 = (ImageView) findViewById(R.id.imageview2);
        resolver.loadIconWhite(imageView2);
        View connectImageViewContainer = findViewById(R.id.connect_imageview);
        if (resolver.isEnabled()) {
            connectImageViewContainer.setVisibility(View.VISIBLE);
        } else {
            connectImageViewContainer.setVisibility(View.GONE);
        }
    }

    public void fillView(Playlist playlist) {
        if (findViewById(R.id.imageview_create_playlist) != null) {
            findViewById(R.id.imageview_create_playlist).setVisibility(View.GONE);
        }
        ArrayList<Image> artistImages = new ArrayList<>();
        String topArtistsString = "";
        String[] artists = playlist.getTopArtistNames();
        if (artists != null) {
            for (int i = 0; i < artists.length && i < 5 && artistImages.size() < 3; i++) {
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
        fillView(mRootView, artistImages, 0, false);
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        if (textView1 != null) {
            textView1.setText(playlist.getName());
        }
        TextView textView2 = (TextView) findViewById(R.id.textview2);
        if (textView2 != null) {
            textView2.setText(topArtistsString);
            textView2.setVisibility(View.VISIBLE);
        }
        TextView textView3 = (TextView) findViewById(R.id.textview3);
        if (textView3 != null) {
            textView3.setVisibility(View.VISIBLE);
            textView3.setText(TomahawkApp.getContext().getResources().getQuantityString(
                    R.plurals.songs_with_count, (int) playlist.getCount(), playlist.getCount()));
        }
    }

    public static void fillView(View view, Playlist playlist, int height, boolean isPagerFragment) {
        ArrayList<Image> artistImages = new ArrayList<>();
        String[] artists = playlist.getTopArtistNames();
        if (artists != null) {
            for (int i = 0; i < artists.length && i < 5 && artistImages.size() < 3; i++) {
                Artist artist = Artist.get(artists[i]);
                if (artist.getImage() != null) {
                    artistImages.add(artist.getImage());
                }
            }
        }
        fillView(view, artistImages, height, isPagerFragment);
    }

    private static void fillView(View view, List<Image> artistImages, int height,
            boolean isPagerFragment) {
        View v;
        int gridOneResId = isPagerFragment ? R.id.imageview_grid_one_pager
                : R.id.imageview_grid_one;
        int gridTwoResId = isPagerFragment ? R.id.imageview_grid_two_pager
                : R.id.imageview_grid_two;
        int gridThreeResId = isPagerFragment ? R.id.imageview_grid_three_pager
                : R.id.imageview_grid_three;
        int gridOneStubId = isPagerFragment ? R.id.imageview_grid_one_pager_stub
                : R.id.imageview_grid_one_stub;
        int gridTwoStubId = isPagerFragment ? R.id.imageview_grid_two_pager_stub
                : R.id.imageview_grid_two_stub;
        int gridThreeStubId = isPagerFragment ? R.id.imageview_grid_three_pager_stub
                : R.id.imageview_grid_three_stub;
        if (artistImages.size() > 2) {
            v = view.findViewById(gridOneResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = view.findViewById(gridTwoResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = TomahawkUtils.ensureInflation(view, gridThreeStubId, gridThreeResId);
            v.setVisibility(View.VISIBLE);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                    (ImageView) v.findViewById(R.id.imageview1),
                    artistImages.get(0), Image.getLargeImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                    (ImageView) v.findViewById(R.id.imageview2),
                    artistImages.get(1), Image.getSmallImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                    (ImageView) v.findViewById(R.id.imageview3),
                    artistImages.get(2), Image.getSmallImageSize(), false);
        } else if (artistImages.size() > 1) {
            v = view.findViewById(gridOneResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = view.findViewById(gridThreeResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = TomahawkUtils.ensureInflation(view, gridTwoStubId, gridTwoResId);
            v.setVisibility(View.VISIBLE);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                    (ImageView) v.findViewById(R.id.imageview1),
                    artistImages.get(0), Image.getLargeImageSize(), false);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                    (ImageView) v.findViewById(R.id.imageview2),
                    artistImages.get(1), Image.getSmallImageSize(), false);
        } else {
            v = view.findViewById(gridTwoResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = view.findViewById(gridThreeResId);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = TomahawkUtils.ensureInflation(view, gridOneStubId, gridOneResId);
            v.setVisibility(View.VISIBLE);
            if (artistImages.size() > 0) {
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(),
                        (ImageView) v.findViewById(R.id.imageview1),
                        artistImages.get(0), Image.getLargeImageSize(), false);
            } else {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                        (ImageView) v.findViewById(R.id.imageview1),
                        R.drawable.album_placeholder_grid);
            }
        }
        if (height > 0) {
            v.getLayoutParams().height = height;
        }
    }

    public void fillView(int id) {
        switch (id) {
            case PlaylistsFragment.CREATE_PLAYLIST_BUTTON_ID:
                View v = mRootView.findViewById(R.id.imageview_grid_one);
                if (v != null) {
                    v.setVisibility(View.GONE);
                }
                v = mRootView.findViewById(R.id.imageview_grid_two);
                if (v != null) {
                    v.setVisibility(View.GONE);
                }
                v = mRootView.findViewById(R.id.imageview_grid_three);
                if (v != null) {
                    v.setVisibility(View.GONE);
                }
                TomahawkUtils.ensureInflation(mRootView, R.id.imageview_create_playlist_stub,
                        R.id.imageview_create_playlist);
                findViewById(R.id.imageview_create_playlist).setVisibility(View.VISIBLE);
                TextView textView1 = (TextView) findViewById(R.id.textview1);
                textView1.setText(
                        TomahawkApp.getContext().getString(R.string.create_playlist).toUpperCase());
                TextView textView2 = (TextView) findViewById(R.id.textview2);
                textView2.setVisibility(View.GONE);
                TextView textView3 = (TextView) findViewById(R.id.textview3);
                textView3.setVisibility(View.GONE);
                break;
        }
    }

    public void fillHeaderView(ArrayList<CharSequence> spinnerItems,
            int initialSelection, AdapterView.OnItemSelectedListener listener) {
        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(TomahawkApp.getContext(),
                        R.layout.dropdown_header_textview, spinnerItems);
        adapter.setDropDownViewResource(R.layout.dropdown_header_dropdown_textview);
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        spinner.setAdapter(adapter);
        spinner.setSelection(initialSelection);
        spinner.setOnItemSelectedListener(listener);
    }

    public void fillHeaderView(String text) {
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(text);
    }

    public void fillHeaderView(SocialAction socialAction, int segmentSize) {
        ImageView userImageView1 = (ImageView) findViewById(R.id.userimageview1);
        TextView userTextView = (TextView) findViewById(R.id.usertextview1);
        TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                userImageView1, socialAction.getUser(),
                Image.getSmallImageSize(), userTextView);
        Object targetObject = socialAction.getTargetObject();
        Resources resources = TomahawkApp.getContext().getResources();
        String userName = socialAction.getUser().getName();
        String phrase = "!FIXME! type: " + socialAction.getType()
                + ", action: " + socialAction.getAction() + ", user: " + userName;
        if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE
                .equals(socialAction.getType())) {
            if (targetObject instanceof Query) {
                phrase = resources.getQuantityString(R.plurals.socialaction_type_love_track,
                        segmentSize, userName, segmentSize);
            } else if (targetObject instanceof Album) {
                phrase = resources.getQuantityString(R.plurals.socialaction_type_collected_album,
                        segmentSize, userName, segmentSize);
            } else if (targetObject instanceof Artist) {
                phrase = resources.getQuantityString(R.plurals.socialaction_type_collected_artist,
                        segmentSize, userName, segmentSize);
            }
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW
                .equals(socialAction.getType())) {
            phrase = resources.getString(R.string.socialaction_type_follow, userName);
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST
                .equals(socialAction.getType())) {
            phrase = resources.getQuantityString(R.plurals.socialaction_type_createplaylist,
                    segmentSize, userName, segmentSize);
        } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON
                .equals(socialAction.getType())) {
            phrase = resources.getQuantityString(R.plurals.socialaction_type_latchon,
                    segmentSize, userName, segmentSize);
        }
        TextView textView1 = (TextView) findViewById(R.id.textview1);
        textView1.setText(phrase + ":");
    }

    private static String dateToString(Resources resources, Date date) {
        String s = "";
        if (date != null) {
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 60000) {
                s += resources.getString(R.string.time_afewseconds);
            } else if (diff < 3600000) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                s += resources.getQuantityString(R.plurals.time_minute, (int) minutes, minutes);
            } else if (diff < 86400000) {
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                s += resources.getQuantityString(R.plurals.time_hour, (int) hours, hours);
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                s += resources.getQuantityString(R.plurals.time_day, (int) days, days);
            }
        }
        return s;
    }

    private static void setTextViewEnabled(TextView textView, boolean enabled,
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
    }
}
