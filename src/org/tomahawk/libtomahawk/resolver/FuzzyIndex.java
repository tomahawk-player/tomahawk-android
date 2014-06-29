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

    public FuzzyIndex(String fileName, boolean wipe) {
        synchronized (this) {
            mLucenePath = fileName;
            try {
                File indexDirFile = new File(mLucenePath);
                Directory dir = FSDirectory.open(indexDirFile);
                beginIndexing();
                endIndexing();
                mSearcherManager = new SearcherManager(dir, new SearcherFactory());
            } catch (IOException e) {
                Log.e(TAG, "FuzzyIndex<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            if (wipe) {
                deleteIndex();
            }
        }
    }

    public void close() {
        deleteIndex();
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
                beginIndexing();
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
                }
                IndexSearcher searcher = mSearcherManager.acquire();
                long time = System.currentTimeMillis();
                ScoreDoc[] hits = searcher.search(qry, 50).scoreDocs;
                Log.d(TAG, "searching took " + (System.currentTimeMillis() - time) + "ms");
                results = new double[hits.length][2];
                for (int i = 0; i < hits.length; i++) {
                    ScoreDoc doc = hits[i];
                    Document document = searcher.doc(doc.doc);
                    results[i][0] = document.getField("id").numericValue().intValue();
                    results[i][1] = doc.score;
                }
                mSearcherManager.release(searcher);
            } catch (IOException e) {
                Log.e(TAG, "search: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            return results;
        }
    }

    private void beginIndexing() {
        try {
            endIndexing();
            File indexDirFile = new File(mLucenePath);
            Directory dir = FSDirectory.open(indexDirFile);
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            mLuceneWriter = new IndexWriter(dir, iwc);
        } catch (IOException e) {
            Log.e(TAG, "beginIndexing: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    private void endIndexing() {
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
