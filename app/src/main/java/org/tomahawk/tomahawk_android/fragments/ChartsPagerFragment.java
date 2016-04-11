/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.google.gson.JsonObject;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredManager;
import org.jdeferred.multiple.MultipleResults;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsManager;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsProvider;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsResult;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ChartsPagerFragment extends PagerFragment {

    public static final String CHARTSPROVIDER_ID = "chartsprovider_id";

    public static final String CHARTSPROVIDER_COUNTRYCODE = "chartsprovider_countrycode";

    private static final String CHARTS_PLAYLIST_SUFFX = "_charts";

    public static final int SHOW_MODE_CHARTS = 10;

    /**
     * Called, when this {@link ChartsPagerFragment}'s {@link View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ScriptChartsProvider provider;
        final String countryCode;
        if (getArguments().containsKey(CHARTSPROVIDER_ID)) {
            String chartsProviderId = getArguments().getString(CHARTSPROVIDER_ID);
            provider = ScriptChartsManager.get().getScriptChartsProvider(chartsProviderId);
            if (provider == null) {
                getActivity().getSupportFragmentManager().popBackStack();
                return;
            }
        } else {
            throw new RuntimeException("No CHARTSPROVIDER_ID provided to ChartsPagerFragment");
        }
        if (getArguments().containsKey(CHARTSPROVIDER_COUNTRYCODE)) {
            countryCode = getArguments().getString(CHARTSPROVIDER_COUNTRYCODE);
            if (countryCode == null) {
                getActivity().getSupportFragmentManager().popBackStack();
                return;
            }
        } else {
            throw new RuntimeException(
                    "No CHARTSPROVIDER_COUNTRYCODE provided to ChartsPagerFragment");
        }

        showContentHeader(provider.getScriptAccount().getIconBackgroundPath());

        provider.getTypes().done(new DoneCallback<List<Pair<String, String>>>() {
            @Override
            public void onDone(List<Pair<String, String>> types) {
                List<Promise> promises = new ArrayList<>();
                for (Pair<String, String> type : types) {
                    promises.add(provider.getCharts(countryCode, type.second));
                }
                showCharts(provider, types, promises);
            }
        });
    }

    private void showCharts(final ScriptChartsProvider provider,
            final List<Pair<String, String>> types, List<Promise> promises) {
        AndroidDeferredManager deferredManager = new AndroidDeferredManager();
        deferredManager.when(promises.toArray(new Promise[promises.size()])).done(
                new DoneCallback<MultipleResults>() {
                    @Override
                    public void onDone(MultipleResults multipleResults) {
                        List<FragmentInfoList> fragmentInfoLists = new ArrayList<>();
                        for (int i = 0; i < multipleResults.size(); i++) {
                            ScriptChartsResult chartsResult =
                                    (ScriptChartsResult) multipleResults.get(i).getResult();
                            FragmentInfoList fragmentInfoList = new FragmentInfoList();
                            FragmentInfo fragmentInfo = new FragmentInfo();
                            fragmentInfo.mBundle = getChildFragmentBundle();
                            fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                                    SHOW_MODE_CHARTS);
                            fragmentInfo.mTitle = types.get(i).first;
                            if (chartsResult.contentType == PipeLine.URL_TYPE_ARTIST) {
                                fragmentInfo.mClass = ArtistsFragment.class;
                                ArrayList<String> artistKeys = new ArrayList<>();
                                for (JsonObject rawArtist : chartsResult.results) {
                                    String artistName =
                                            rawArtist.get("artist").getAsString();
                                    artistKeys.add(Artist.get(artistName).getCacheKey());
                                }
                                fragmentInfo.mBundle.putStringArrayList(
                                        TomahawkFragment.ARTISTARRAY, artistKeys);
                            } else if (chartsResult.contentType == PipeLine.URL_TYPE_ALBUM) {
                                fragmentInfo.mClass = AlbumsFragment.class;
                                ArrayList<String> albumKeys = new ArrayList<>();
                                for (JsonObject rawAlbum : chartsResult.results) {
                                    String artistName =
                                            rawAlbum.get("artist").getAsString();
                                    Artist artist = Artist.get(artistName);
                                    String albumName =
                                            rawAlbum.get("album").getAsString();
                                    albumKeys.add(
                                            Album.get(albumName, artist).getCacheKey());
                                }
                                fragmentInfo.mBundle.putStringArrayList(
                                        TomahawkFragment.ALBUMARRAY, albumKeys);
                            } else if (chartsResult.contentType == PipeLine.URL_TYPE_TRACK) {
                                fragmentInfo.mClass = PlaylistEntriesFragment.class;
                                Playlist pl = Playlist.get(
                                        provider.getScriptAccount().getMetaData().pluginName
                                                + CHARTS_PLAYLIST_SUFFX + types.get(i).second);
                                pl.setFilled(true);
                                pl.clear();
                                for (JsonObject rawTrack : chartsResult.results) {
                                    String artistName =
                                            rawTrack.get("artist").getAsString();
                                    String albumName =
                                            rawTrack.get("album").getAsString();
                                    String trackName =
                                            rawTrack.get("track").getAsString();
                                    pl.addQuery(pl.size(), Query.get(
                                            trackName, albumName, artistName, false));
                                }
                                fragmentInfo.mBundle.putString(
                                        TomahawkFragment.PLAYLIST, pl.getCacheKey());
                            }
                            fragmentInfoList.addFragmentInfo(fragmentInfo);
                            fragmentInfoLists.add(fragmentInfoList);
                        }
                        setupPager(fragmentInfoLists, 0, null, 2);
                    }
                });
    }
}
