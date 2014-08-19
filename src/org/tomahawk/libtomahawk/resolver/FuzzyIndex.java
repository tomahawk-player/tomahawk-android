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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FuzzyIndex {

    private final static String TAG = FuzzyIndex.class.getSimpleName();

    private String mLucenePath;

    private IndexWriter mLuceneWriter;

    private SearcherManager mSearcherManager;

    /**
     * Tries to create a new fuzzy index
     *
     * @param fileName the path to the folder where the fuzzy index should be created
     * @param recreate whether or not to wipe any previously existing index
     * @return whether or not the creation has been successful
     */
    public boolean create(String fileName, boolean recreate) {
        synchronized (this) {
            try {
                Log.d(TAG, "create - fileName:" + fileName + ", recreate:" + recreate);
                mLucenePath = fileName;
                File indexDirFile = new File(mLucenePath);
                Directory dir = FSDirectory.open(indexDirFile);
                beginIndexing(recreate);
                endIndexing();
                mSearcherManager = new SearcherManager(dir, new SearcherFactory());
            } catch (IOException e) {
                Log.d(TAG, "FuzzyIndex<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
                return false;
            }
            return true;
        }
    }

    public void close() {
        Log.d(TAG, "close");
        endIndexing();
        try {
            if (mSearcherManager != null) {
                mSearcherManager.close();
                mSearcherManager = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "close: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public void addScriptResolverFuzzyIndexList(ScriptResolverFuzzyIndex[] indexList) {
        synchronized (this) {
            try {
                Log.d(TAG, "addScriptResolverFuzzyIndexList - count: " + indexList.length);
                if (indexList.length > 0) {
                    Log.d(TAG,
                            "addScriptResolverFuzzyIndexList - first index: id:" + indexList[0].id
                                    + ", artist:" + indexList[0].artist
                                    + ", album:" + indexList[0].album
                                    + ", track:" + indexList[0].track);
                }
                beginIndexing(true);
                for (ScriptResolverFuzzyIndex index : indexList) {
                    Document document = new Document();
                    document.add(new IntField("id", index.id, Field.Store.YES));
                    document.add(new StringField("artist", index.artist, Field.Store.YES));
                    document.add(new StringField("album", index.album, Field.Store.YES));
                    document.add(new StringField("track", index.track, Field.Store.YES));
                    mLuceneWriter.addDocument(document);
                }
                endIndexing();
            } catch (IOException e) {
                Log.e(TAG, "addScriptResolverFuzzyIndexList: " + e.getClass() + ": " + e
                        .getLocalizedMessage());
            }
        }
    }

    public void deleteIndex() {
        synchronized (this) {
            try {
                Log.d(TAG, "deleteIndex");
                TomahawkUtils.deleteRecursive(new File(mLucenePath));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "deleteIndex: " + e.getClass() + ": " + e
                        .getLocalizedMessage());
            }
        }
    }

    public double[][] search(Query query) {
        synchronized (this) {
            double[][] results = new double[][]{};
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
                    Log.d(TAG, "search - fulltext: " + escapedQuery);
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
                    Log.d(TAG, "search - non-fulltext: " + escapedArtistName + ", "
                            + escapedTrackName);
                }
                IndexSearcher searcher = mSearcherManager.acquire();
                long time = System.currentTimeMillis();
                ScoreDoc[] hits = searcher.search(qry, 50).scoreDocs;
                Log.d(TAG, "search - searching took " + (System.currentTimeMillis() - time) + "ms");
                results = new double[hits.length][2];
                for (int i = 0; i < hits.length; i++) {
                    ScoreDoc doc = hits[i];
                    Document document = searcher.doc(doc.doc);
                    results[i][0] = document.getField("id").numericValue().intValue();
                    results[i][1] = doc.score;
                }
                if (results.length > 0) {
                    Log.d(TAG, "search - first result: id:" + results[0][0]
                            + ", score: " + results[0][1]);
                }
                mSearcherManager.release(searcher);
            } catch (IOException e) {
                Log.e(TAG, "search: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            return results;
        }
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
                Log.e(TAG, "endIndexing: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }
}
