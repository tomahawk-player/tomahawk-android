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
package org.tomahawk.libtomahawk.collection;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptObject;
import org.tomahawk.libtomahawk.resolver.ScriptPlugin;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverCollectionMetaData;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.widget.ImageView;

/**
 * This class represents a Collection which contains tracks/albums/artists retrieved by a
 * ScriptResolver.
 */
public class ScriptResolverCollection extends DbCollection implements ScriptPlugin {

    private final static String TAG = ScriptResolverCollection.class.getSimpleName();

    private ScriptObject mScriptObject;

    private ScriptAccount mScriptAccount;

    private ScriptResolverCollectionMetaData mMetaData;

    public ScriptResolverCollection(ScriptObject object, ScriptAccount account) {
        super(account.getScriptResolver());

        mScriptObject = object;
        mScriptAccount = account;
    }

    @Override
    public Promise<String, Throwable, Void> getCollectionId() {
        final Deferred<ScriptResolverCollectionMetaData, Throwable, Void> deferred
                = new ADeferredObject<>();
        if (mMetaData == null) {
            ScriptJob.start(mScriptObject, "settings",
                    new ScriptJob.ResultsCallback<ScriptResolverCollectionMetaData>(
                            ScriptResolverCollectionMetaData.class) {
                        @Override
                        public void onReportResults(ScriptResolverCollectionMetaData results) {
                            mMetaData = results;
                            deferred.resolve(results);
                        }
                    }, new ScriptJob.FailureCallback() {
                        @Override
                        public void onReportFailure(String errormessage) {
                            deferred.reject(new Throwable(errormessage));
                        }
                    });
            return deferred.then(
                    new DonePipe<ScriptResolverCollectionMetaData, String, Throwable, Void>() {
                        @Override
                        public Promise<String, Throwable, Void> pipeDone(
                                ScriptResolverCollectionMetaData result) {
                            final Deferred<String, Throwable, Void> deferred
                                    = new ADeferredObject<>();
                            return deferred.resolve(result.id);
                        }
                    });
        } else {
            Deferred<String, Throwable, Void> d = new ADeferredObject<>();
            return d.resolve(mMetaData.id);
        }
    }

    public Deferred<ScriptResolverCollectionMetaData, Throwable, Void> getMetaData() {
        final Deferred<ScriptResolverCollectionMetaData, Throwable, Void> deferred
                = new ADeferredObject<>();
        if (mMetaData == null) {
            ScriptJob.start(mScriptObject, "settings",
                    new ScriptJob.ResultsCallback<ScriptResolverCollectionMetaData>(
                            ScriptResolverCollectionMetaData.class) {
                        @Override
                        public void onReportResults(ScriptResolverCollectionMetaData results) {
                            mMetaData = results;
                            deferred.resolve(results);
                        }
                    }, new ScriptJob.FailureCallback() {
                        @Override
                        public void onReportFailure(String errormessage) {
                            deferred.reject(new Throwable(errormessage));
                        }
                    });
        } else {
            deferred.resolve(mMetaData);
        }
        return deferred;
    }

    @Override
    public ScriptObject getScriptObject() {
        return mScriptObject;
    }

    @Override
    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    @Override
    public void loadIcon(final ImageView imageView, final boolean grayOut) {
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String completeIconPath = mScriptAccount.getPath() + "/content/" + result.iconfile;
                ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        completeIconPath);
            }
        });
    }
}
