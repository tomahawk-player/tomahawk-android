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
package org.tomahawk.libtomahawk.infosystem.charts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptObject;
import org.tomahawk.libtomahawk.resolver.ScriptPlugin;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.GsonHelper;

import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptChartsProvider implements ScriptPlugin {

    public static final String TAG = ScriptChartsProvider.class.getSimpleName();

    // cache the result for a maximum of 12 hours
    private static final long CACHE_TIME = 43200000L;

    private ScriptAccount mScriptAccount;

    private ScriptObject mScriptObject;

    private Map<String, Pair<Long, ScriptChartsResult>> mCachedResults = new ConcurrentHashMap<>();

    public ScriptChartsProvider(ScriptObject scriptObject, ScriptAccount account) {
        mScriptObject = scriptObject;
        mScriptAccount = account;
    }

    public Promise<ScriptChartsCountryCodes, Throwable, Void> getCountryCodes() {
        final Deferred<ScriptChartsCountryCodes, Throwable, Void> deferred =
                new ADeferredObject<>();
        ScriptJob.start(mScriptObject, "countryCodes", new ScriptJob.ResultsObjectCallback() {
            @Override
            public void onReportResults(JsonObject results) {
                ScriptChartsCountryCodes chartsCountryCodes = new ScriptChartsCountryCodes();
                chartsCountryCodes.defaultCode = results.get("defaultCode").getAsString();
                List<Pair<String, String>> codes = new ArrayList<>();
                JsonArray rawCodes = results.getAsJsonArray("codes");
                for (JsonElement result : rawCodes) {
                    JsonObject code = (JsonObject) result;
                    for (Map.Entry<String, JsonElement> member : code.entrySet()) {
                        Pair<String, String> pair =
                                new Pair<>(member.getKey(), member.getValue().getAsString());
                        codes.add(pair);
                    }
                }
                chartsCountryCodes.codes = codes;
                deferred.resolve(chartsCountryCodes);
            }
        });
        return deferred;
    }

    public Promise<List<Pair<String, String>>, Throwable, Void> getTypes() {
        final Deferred<List<Pair<String, String>>, Throwable, Void> deferred
                = new ADeferredObject<>();
        ScriptJob.start(mScriptObject, "types", new ScriptJob.ResultsArrayCallback() {
            @Override
            public void onReportResults(JsonArray results) {
                List<Pair<String, String>> types = new ArrayList<>();
                for (JsonElement result : results) {
                    JsonObject type = (JsonObject) result;
                    for (Map.Entry<String, JsonElement> member : type.entrySet()) {
                        Pair<String, String> pair =
                                new Pair<>(member.getKey(), member.getValue().getAsString());
                        types.add(pair);
                    }
                }
                deferred.resolve(types);
            }
        });
        return deferred;
    }

    public Promise<ScriptChartsResult, Throwable, Void> getCharts(final String countryCode,
            final String type) {
        final Deferred<ScriptChartsResult, Throwable, Void> deferred = new ADeferredObject<>();

        final String cacheKey = getCacheKey(countryCode, type);
        final Pair<Long, ScriptChartsResult> pair = mCachedResults.get(cacheKey);
        if (pair != null && pair.first > System.currentTimeMillis() - CACHE_TIME) {
            Log.d(TAG, "Using cached charts for " + mScriptAccount.getName()
                    + ": countryCode=" + countryCode + ", type=" + type
                    + " - containing " + pair.second.results.size() + " results");
            deferred.resolve(pair.second);
        } else {
            Log.d(TAG, "Getting fresh charts for " + mScriptAccount.getName() + ": countryCode="
                    + countryCode + ", type=" + type);
            Map<String, Object> args = new HashMap<>();
            args.put("countryCode", countryCode);
            args.put("type", type);

            ScriptJob.start(mScriptObject, "charts", args, new ScriptJob.ResultsObjectCallback() {
                @Override
                public void onReportResults(JsonObject results) {
                    ScriptChartsResult result =
                            GsonHelper.get().fromJson(results, ScriptChartsResult.class);
                    Pair<Long, ScriptChartsResult> pair =
                            new Pair<>(System.currentTimeMillis(), result);
                    mCachedResults.put(cacheKey, pair);
                    Log.d(TAG, "Received fresh charts for " + mScriptAccount.getName()
                            + ": countryCode=" + countryCode + ", type=" + type
                            + " - containing " + result.results.size() + " results");
                    deferred.resolve(result);
                }
            });
        }
        return deferred;
    }

    private String getCacheKey(String countryCode, String type) {
        return countryCode + "\t\t" + type;
    }

    @Override
    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    @Override
    public ScriptObject getScriptObject() {
        return mScriptObject;
    }

}
