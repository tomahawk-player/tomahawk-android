/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android;

import java.util.ArrayList;
import java.util.List;

import org.tomahawk.libtomahawk.*;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class SearchableFragment extends TomahawkFragment implements OnItemClickListener,
        CompoundButton.OnCheckedChangeListener {

    private static final String SEARCHABLEFRAGMENT_QUERY_STRING = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_QUERY_STRING";
    public static final String SEARCHABLEFRAGMENT_ARTISTCACHED = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_ARTISTCACHED";
    public static final String SEARCHABLEFRAGMENT_ALBUMCACHED = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_ALBUMCACHED";

    private SearchableFragment mSearchableFragment = this;
    private ArrayList<Track> mCurrentShownTracks;
    private ArrayList<Album> mCurrentShownAlbums;
    private ArrayList<Artist> mCurrentShownArtists;
    private String mCurrentQueryString;
    private String mCurrentQueryId;

    private PipelineBroadcastReceiver mPipelineBroadcastReceiver;

    private Collection mCollection;

    private class PipelineBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PipeLine.PIPELINE_RESULTSREPORTED)) {
                mCurrentQueryId = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                showQueryResults(mCurrentQueryId);
            }
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (inState != null && inState.containsKey(SEARCHABLEFRAGMENT_QUERY_STRING))
            mCurrentQueryString = inState.getString(SEARCHABLEFRAGMENT_QUERY_STRING);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.searchablefragment_layout, null, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        CheckBox onlineSourcesCheckBox = (CheckBox) mActivity.findViewById(R.id.searchactivity_onlinesources_checkbox);
        onlineSourcesCheckBox.setOnCheckedChangeListener(this);

        IntentFilter intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
        if (mPipelineBroadcastReceiver == null) {
            mPipelineBroadcastReceiver = new PipelineBroadcastReceiver();
            mActivity.registerReceiver(mPipelineBroadcastReceiver, intentFilter);
        }

        if (mCurrentQueryString != null)
            ((SearchableActivity) mActivity).resolveFullTextQuery(mCurrentQueryString);
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mPipelineBroadcastReceiver != null) {
            mActivity.unregisterReceiver(mPipelineBroadcastReceiver);
            mPipelineBroadcastReceiver = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString(SEARCHABLEFRAGMENT_QUERY_STRING, mCurrentQueryString);
        super.onSaveInstanceState(out);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Track) {
                long playlistId = mCollection.addPlaylist(CustomPlaylist.fromTrackList(mCurrentQueryString,
                        mCurrentShownTracks, (Track) getListAdapter().getItem(idx)));
                Bundle bundle = new Bundle();
                bundle.putLong(PlaybackActivity.PLAYLIST_PLAYLIST_ID, playlistId);
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) getListAdapter().getItem(idx)).getId());

                Intent playbackIntent = getIntent(mActivity, PlaybackActivity.class);
                playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
                startActivity(playbackIntent);
            } else if (getListAdapter().getItem(idx) instanceof Album) {
                mCollection.setCachedAlbum((Album) getListAdapter().getItem(idx));
                Bundle bundle = new Bundle();
                bundle.putBoolean(SEARCHABLEFRAGMENT_ALBUMCACHED, true);

                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.searchactivity_fragmentcontainer_framelayout,
                        android.support.v4.app.Fragment.instantiate(mActivity, TracksFragment.class.getName(), bundle));
                ft.addToBackStack(null);
                ft.commit();
            } else if (getListAdapter().getItem(idx) instanceof Artist) {
                mCollection.setCachedArtist((Artist) getListAdapter().getItem(idx));
                Bundle bundle = new Bundle();
                bundle.putBoolean(SEARCHABLEFRAGMENT_ARTISTCACHED, true);

                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.searchactivity_fragmentcontainer_framelayout,
                        android.support.v4.app.Fragment.instantiate(mActivity, TracksFragment.class.getName(), bundle));
                ft.addToBackStack(null);
                ft.commit();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ((SearchableActivity) mActivity).resolveFullTextQuery(mCurrentQueryString);
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        mCollection = coll;
    }

    public void showQueryResults(String qid) {
        PipeLine pipeLine = ((TomahawkApp) mActivity.getApplication()).getPipeLine();
        Query query = pipeLine.getQuery(qid);
        mCurrentQueryString = query.getFullTextQuery();
        List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> trackResultList = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownTracks = query.getTrackResults();
        trackResultList.addAll(mCurrentShownTracks);
        listArray.add(trackResultList);
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> artistResultList = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownArtists = query.getArtistResults();
        artistResultList.addAll(mCurrentShownArtists);
        listArray.add(artistResultList);
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> albumResultList = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownAlbums = query.getAlbumResults();
        albumResultList.addAll(mCurrentShownAlbums);
        listArray.add(albumResultList);
        //                if (mTomahawkListAdapter == null) {
        TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
        tomahawkListAdapter.setShowCategoryHeaders(true);
        //                } else
        //                    mTomahawkListAdapter.setListWithIndex(0, trackResultList);
        tomahawkListAdapter.setShowResolvedBy(true);
        setListAdapter(tomahawkListAdapter);
        getListView().setOnItemClickListener(mSearchableFragment);
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls the class which contains the activity to launch
     * @return the created intent
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
