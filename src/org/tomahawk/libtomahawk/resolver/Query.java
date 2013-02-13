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
package org.tomahawk.libtomahawk.resolver;

import java.util.ArrayList;
import java.util.Collections;

import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.TrackComparator;

import android.text.TextUtils;
import android.util.Log;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 18.01.13
 * 
 * This class represents a query which is passed to a resolver. It contains all the information needed to
 * enable the ScriptResolver to resolve the results.
 */
public class Query {
    public static final String TAG = Query.class.getName();

    private ArrayList<Result> mResults = new ArrayList<Result>();
    private boolean mSolved;
    private String mQid;

    private String mFullTextQuery;
    private boolean mIsFullTextQuery;

    private String mTrackName = "";
    private String mAlbumName = "";
    private String mArtistName = "";

    /**
     * Constructs a new Query with the given QueryID. ID should be generated in TomahawkApp.
     * @param qid
     */
    public Query(final String qid) {
        mQid = qid;
    }

    /**
     * Constructs a new Query with the given QueryID and a fullTextQuery String.
     * ID should be generated in TomahawkApp.
     * @param qid
     * @param fullTextQuery
     */
    public Query(final String qid, final String fullTextQuery) {
        mFullTextQuery = fullTextQuery.replace("'", "\\'");
        mIsFullTextQuery = true;
        mQid = qid;
    }

    public Query(final String qid, final String trackName, final String albumName, final String artistName) {
        mTrackName = trackName.replace("'", "\\'");
        mAlbumName = albumName.replace("'", "\\'");
        mArtistName = artistName.replace("'", "\\'");
        mQid = qid;
        mIsFullTextQuery = false;
    }

    /**
     * @return the ArrayList containing all results
     */
    public ArrayList<Result> getResults() {
        return mResults;
    }

    /**
     * Append an ArrayList<Result> to the current result list
     * @param results
     */
    public void addResults(ArrayList<Result> results) {
        mResults.addAll(results);
        mSolved = true;
    }

    /**
     * @return A ArrayList<Track> which contains all tracks in the resultList, sorted by score.
     */
    public ArrayList<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (Result r : mResults)
            tracks.add(r.getTrack());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_SCORE));
        return tracks;
    }

    public String getFullTextQuery() {
        return mFullTextQuery;
    }

    public boolean isFullTextQuery() {
        return mIsFullTextQuery;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public String getQid() {
        return mQid;
    }

    /**
     * This method determines how similar the given result is to the search string.
     * @param r
     * @return
     */
    public float howSimilar(Result r, int searchType) {
        String resultArtistName = "";
        String resultAlbumName = "";
        String resultTrackName = "";
        if (r.getArtist().getName() != null)
            resultArtistName = cleanUpString(r.getArtist().getName(), true);
        if (r.getAlbum().getName() != null)
            resultAlbumName = cleanUpString(r.getAlbum().getName(), false);
        if (r.getTrack().getName() != null)
            resultTrackName = cleanUpString(r.getTrack().getName(), false);

        int distanceArtist = TomahawkUtils.getLevenshteinDistance(mArtistName, resultArtistName);
        int distanceAlbum = TomahawkUtils.getLevenshteinDistance(mAlbumName, resultAlbumName);
        int distanceTrack = TomahawkUtils.getLevenshteinDistance(mTrackName, resultTrackName);

        int maxLengthArtist = Math.max(mArtistName.length(), resultArtistName.length());
        int maxLengthAlbum = Math.max(mAlbumName.length(), resultAlbumName.length());
        int maxLengthTrack = Math.max(mTrackName.length(), resultTrackName.length());

        float distanceScoreArtist = (float) (maxLengthArtist - distanceArtist) / maxLengthArtist;
        float distanceScoreAlbum;
        if (maxLengthAlbum > 0)
            distanceScoreAlbum = (float) (maxLengthAlbum - distanceAlbum) / maxLengthAlbum;
        else
            distanceScoreAlbum = 0F;
        float distanceScoreTrack = (float) (maxLengthTrack - distanceTrack) / maxLengthTrack;

        if (isFullTextQuery()) {
            final String searchString = cleanUpString(getFullTextQuery(), false);
            ArrayList<String> resultSearchStrings = new ArrayList<String>();
            switch (searchType) {
            case PipeLine.PIPELINE_SEARCHTYPE_ALL:
                resultSearchStrings.add(cleanUpString(resultArtistName + " " + resultTrackName, false));
                resultSearchStrings.add(cleanUpString(resultArtistName, false));
                resultSearchStrings.add(cleanUpString(resultAlbumName, false));
                resultSearchStrings.add(cleanUpString(resultTrackName, false));
                break;
            case PipeLine.PIPELINE_SEARCHTYPE_TRACKS:
                resultSearchStrings.add(cleanUpString(resultTrackName, false));
                break;
            case PipeLine.PIPELINE_SEARCHTYPE_ARTISTS:
                resultSearchStrings.add(cleanUpString(resultArtistName, false));
                break;
            case PipeLine.PIPELINE_SEARCHTYPE_ALBUMS:
                resultSearchStrings.add(cleanUpString(resultAlbumName, false));
                break;
            }

            float maxResult = 0F;
            for (String resultSearchString : resultSearchStrings) {
                int distanceArtistTrack = TomahawkUtils.getLevenshteinDistance(searchString, resultSearchString);
                int maxLengthArtistTrack = Math.max(searchString.length(), resultSearchString.length());
                float distanceScoreArtistTrack = (float) (maxLengthArtistTrack - distanceArtistTrack)
                        / maxLengthArtistTrack;

                float result = Math.max(distanceScoreArtist, distanceScoreAlbum);
                result = Math.max(result, distanceScoreArtistTrack);
                result = Math.max(result, distanceScoreTrack);
                if (resultSearchString.contains(searchString))
                    result = Math.max(result, 0.9F);
                maxResult = Math.max(result, maxResult);
                Log.d(TAG, "cleanFullTextQueryWithArticle = " + searchString + ", resultArtistName= "
                        + resultArtistName + ", resultAlbumName= " + resultAlbumName + ", resultTrackName= "
                        + resultTrackName + ", score = " + Math.max(result, distanceScoreArtistTrack));
            }
            return maxResult;
        } else {
            if (TextUtils.isEmpty(mAlbumName))
                distanceScoreAlbum = 1F;

            float combined = (distanceScoreArtist * 4 + distanceScoreAlbum + distanceScoreTrack * 5) / 10;
            return combined;
        }
    }

    /**
     * Clean up the given String.
     * @param in
     * @param replaceArticle wether or not the prefix "the " should be removed
     * @return the clean String
     */
    public String cleanUpString(String in, boolean replaceArticle) {
        String out = in.toLowerCase().trim().replaceAll("[\\s]{2,}", " ");
        if (replaceArticle && out.startsWith("the "))
            out = out.substring(4);
        return out;
    }

    public String getTrackName() {
        return mTrackName;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getArtistName() {
        return mArtistName;
    }
}
