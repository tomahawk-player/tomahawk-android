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
package org.tomahawk.libtomahawk.resolver;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tomahawk.libtomahawk.database.CollectionDb;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FuzzyIndex {

    private final static String TAG = FuzzyIndex.class.getSimpleName();

    private static final String LUCENE_ROOT_FOLDER =
            TomahawkApp.getContext().getFilesDir().getAbsolutePath() + File.separator + "lucene"
                    + File.separator;

    private static final String LAST_FUZZY_INDEX_UPDATE_SUFFIX = "_last_fuzzy_index_update";

    private final String mLastUpdateStorageKey;

    private CollectionDb mCollectionDb;

    private String mLucenePath;

    private IndexWriter mLuceneWriter;

    private SearcherManager mSearcherManager;

    public static class IndexResult {

        public int id;

        public float score;
    }

    public FuzzyIndex(CollectionDb collectionDb) {
        Log.d(TAG, "FuzzyIndex constructor called: " + collectionDb.getCollectionId());
        mCollectionDb = collectionDb;
        mLucenePath = LUCENE_ROOT_FOLDER + collectionDb.getCollectionId();
        mLastUpdateStorageKey = collectionDb.getCollectionId() + LAST_FUZZY_INDEX_UPDATE_SUFFIX;
        ensureIndex();
    }

    /**
     * Make sure that the FuzzyIndex contains all tracks that are stored in the CollectionDb.
     *
     * @return whether or not the process has been successful
     */
    public synchronized void ensureIndex() {
        Log.d(TAG, "addToIndex - using CollectionDb " + mCollectionDb.hashCode() + " with id "
                + mCollectionDb.getCollectionId());
        long lastDbUpdate = mCollectionDb.getLastUpdated();
        long lastIndexUpdate = PreferenceUtils.getLong(mLastUpdateStorageKey, -2);
        Log.d(TAG, "addToIndex - recreate: " + (lastDbUpdate > lastIndexUpdate));
        if (lastDbUpdate > lastIndexUpdate) {
            Cursor cursor = null;
            try {
                String[] fields = new String[]{CollectionDb.TABLE_TRACKS + "." + CollectionDb.ID,
                        CollectionDb.ARTISTS_ARTIST, CollectionDb.ALBUMS_ALBUM,
                        CollectionDb.TRACKS_TRACK};
                cursor = mCollectionDb.tracks(null, null, fields);
                beginIndexing(true);
                Log.d(TAG, "addToIndex - Adding tracks to index - count: " + cursor.getCount());
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        Document document = new Document();
                        document.add(new IntField("id", cursor.getInt(0),
                                Field.Store.YES));
                        document.add(new StringField("artist", cursor.getString(1),
                                Field.Store.YES));
                        document.add(new StringField("album", cursor.getString(2),
                                Field.Store.YES));
                        document.add(new StringField("track", cursor.getString(3),
                                Field.Store.YES));
                        mLuceneWriter.addDocument(document);
                    } while (cursor.moveToNext());
                }
                PreferenceUtils.edit().putLong(mLastUpdateStorageKey, System.currentTimeMillis())
                        .commit();
            } catch (IOException e) {
                Log.e(TAG, "addToIndex - " + e.getClass() + ": " + e.getLocalizedMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                endIndexing();
            }
        }
        updateSearcherManager();
    }

    private void updateSearcherManager() {
        Log.d(TAG, "updateSearcherManager");
        try {
            if (mSearcherManager != null) {
                mSearcherManager.close();
            }
            File indexDirFile = new File(mLucenePath);
            Directory dir = FSDirectory.open(indexDirFile);
            mSearcherManager = new SearcherManager(dir, new SearcherFactory());
        } catch (IOException e) {
            Log.e(TAG, "updateSearcherManager - " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public void close() {
        Log.d(TAG, "close");
        endIndexing();
        if (mSearcherManager != null) {
            try {
                mSearcherManager.close();
            } catch (IOException e) {
                Log.e(TAG, "close - " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            mSearcherManager = null;
        }
    }

    public synchronized List<IndexResult> searchIndex(Query query) {
        List<IndexResult> indexResults = new ArrayList<>();
        try {
            BooleanQuery qry = new BooleanQuery();
            if (query.isFullTextQuery()) {
                String escapedQuery = MultiFieldQueryParser.escape(query.getFullTextQuery());
                Term term = new Term("track", escapedQuery);
                org.apache.lucene.search.Query fqry = new FuzzyQuery(term);
                qry.add(fqry, BooleanClause.Occur.SHOULD);
                term = new Term("artist", escapedQuery);
                fqry = new FuzzyQuery(term);
                qry.add(fqry, BooleanClause.Occur.SHOULD);
                term = new Term("fulltext", escapedQuery);
                fqry = new FuzzyQuery(term);
                qry.add(fqry, BooleanClause.Occur.SHOULD);
                Log.d(TAG, "searchIndex - fulltext: " + escapedQuery);
            } else {
                String escapedTrackName = MultiFieldQueryParser
                        .escape(query.getBasicTrack().getName());
                String escapedArtistName = MultiFieldQueryParser
                        .escape(query.getArtist().getName());
                Term term = new Term("track", escapedTrackName);
                org.apache.lucene.search.Query fqry = new FuzzyQuery(term);
                qry.add(fqry, BooleanClause.Occur.MUST);
                term = new Term("artist", escapedArtistName);
                fqry = new FuzzyQuery(term);
                qry.add(fqry, BooleanClause.Occur.MUST);
                Log.d(TAG, "searchIndex - non-fulltext: " + escapedArtistName + ", "
                        + escapedTrackName);
            }
            IndexSearcher searcher = mSearcherManager.acquire();
            long time = System.currentTimeMillis();
            ScoreDoc[] hits = searcher.search(qry, 50).scoreDocs;
            Log.d(TAG,
                    "searchIndex - searching took " + (System.currentTimeMillis() - time) + "ms");
            for (ScoreDoc doc : hits) {
                Document document = searcher.doc(doc.doc);
                IndexResult indexResult = new IndexResult();
                indexResult.id = document.getField("id").numericValue().intValue();
                indexResult.score = doc.score;
                indexResults.add(indexResult);
            }
            mSearcherManager.release(searcher);
        } catch (IOException e) {
            Log.e(TAG, "searchIndex - " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return indexResults;
    }

    /**
     * Initializes the IndexWriter to be able to add entries to the index.
     *
     * @param recreate whether or not to wipe any previously existing index
     */
    private void beginIndexing(boolean recreate) throws IOException {
        Log.d(TAG, "beginIndexing - recreate: " + recreate);
        endIndexing();
        File indexDirFile = new File(mLucenePath);
        Directory dir = FSDirectory.open(indexDirFile);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        if (recreate) {
            PreferenceUtils.edit().putLong(mLastUpdateStorageKey, -2).commit();
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        }
        mLuceneWriter = new IndexWriter(dir, iwc);
    }

    private void endIndexing() {
        Log.d(TAG, "endIndexing");
        if (mLuceneWriter != null) {
            try {
                mLuceneWriter.commit();
                mLuceneWriter.close(true);
                mLuceneWriter = null;
            } catch (IOException e) {
                Log.e(TAG, "endIndexing - " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }
}
