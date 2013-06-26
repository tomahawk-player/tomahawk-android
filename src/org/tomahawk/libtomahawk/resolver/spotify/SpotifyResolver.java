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
package org.tomahawk.libtomahawk.resolver.spotify;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 18.06.13
 */
public class SpotifyResolver implements Resolver {

    private final static String TAG = SpotifyResolver.class.getName();

    private TomahawkApp mTomahawkApp;

    private int mId;

    private Drawable mIcon;

    private int mWeight;

    private boolean mReady;

    private boolean mStopped;

    private ArrayList<Result> mResults = new ArrayList<Result>();

    public SpotifyResolver(int id, TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
        mId = id;
        mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.spotify_icon);
        mWeight = 100;
        mReady = false;
        mStopped = true;
    }

    /**
     * @return whether or not this resolver is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public void resolve(Query query) {
        mStopped = false;
        if (!query.isFullTextQuery()) {
            LibSpotifyWrapper.resolve(query.getQid(),
                    query.getArtistName() + " " + query.getTrackName() + " " + query.getAlbumName(),
                    this);
        } else {
            LibSpotifyWrapper.resolve(query.getQid(), query.getFullTextQuery(), this);
        }
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public int getWeight() {
        return mWeight;
    }

    public void addResult(Result result) {
        mResults.add(result);
    }

    public void onResolved(String qid) {
        mStopped = true;
        mTomahawkApp.getPipeLine().reportResults(qid, mResults);
    }

    public boolean isReady() {
        return mReady;
    }

    public void setReady(boolean ready) {
        mReady = ready;
    }
}
