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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import com.squareup.okhttp.Response;

import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverConfigUiField;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverSettings;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverStreamUrlResult;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverUrlResult;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.NetworkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This class represents a javascript resolver.
 */
public class ScriptResolver implements Resolver, ScriptPlugin {

    private final static String TAG = ScriptResolver.class.getSimpleName();

    public static class EnabledStateChangedEvent {

    }

    private String mId;

    private ScriptObject mScriptObject;

    private ScriptAccount mScriptAccount;

    private int mWeight;

    private int mTimeout;

    private List<ScriptResolverConfigUiField> mConfigUi;

    private boolean mEnabled;

    private boolean mInitialized;

    private boolean mStopped;

    private final Set<String> mWaitingUrlLookups =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final Set<Query> mWaitingQueries =
            Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private static final int TIMEOUT_HANDLER_MSG = 1337;

    // Handler which sets the mStopped bool to true after the timeout has occured.
    // Meaning this resolver is no longer being shown as resolving.
    private final TimeOutHandler mTimeOutHandler = new TimeOutHandler(this);

    private static class TimeOutHandler extends WeakReferenceHandler<ScriptResolver> {

        public TimeOutHandler(ScriptResolver scriptResolver) {
            super(Looper.getMainLooper(), scriptResolver);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                removeMessages(msg.what);
                getReferencedObject().mStopped = true;
            }
        }
    }

    /**
     * Construct a new {@link ScriptResolver}
     */
    public ScriptResolver(ScriptObject object, ScriptAccount account) {
        mScriptObject = object;
        mScriptAccount = account;
        mScriptAccount.setScriptResolver(this);

        mInitialized = false;
        mStopped = true;
        mId = mScriptAccount.getName();
        if (getConfig().get(ScriptAccount.ENABLED_KEY) != null) {
            mEnabled = (Boolean) getConfig().get(ScriptAccount.ENABLED_KEY);
        } else {
            // Enable soundcloud and jamendo by default
            mEnabled = TomahawkApp.PLUGINNAME_JAMENDO.equals(mId)
                    || TomahawkApp.PLUGINNAME_SOUNDCLOUD.equals(mId);
        }
        settings();
        if (mEnabled) {
            init();
        }
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * @return whether or not this {@link ScriptResolver} is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mInitialized && !mStopped;
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        mScriptAccount.loadIcon(imageView, grayOut);
    }

    @Override
    public void loadIconWhite(ImageView imageView, int tintColorResId) {
        mScriptAccount.loadIconWhite(imageView, tintColorResId);
    }

    @Override
    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        mScriptAccount.loadIconBackground(imageView, grayOut);
    }

    @Override
    public String getPrettyName() {
        return mScriptAccount.getMetaData().name;
    }

    @Override
    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    @Override
    public ScriptObject getScriptObject() {
        return mScriptObject;
    }

    /**
     * This method calls the js function resolver.init().
     */
    private void init() {
        ScriptJob.start(mScriptObject, "init", new ScriptJob.ResultsEmptyCallback() {
            @Override
            public void onReportResults() {
                mInitialized = true;
                Log.d(TAG, "ScriptResolver " + mId + " initialized successfully.");
                invokeWaitingJobs();
            }
        }, new ScriptJob.FailureCallback() {
            @Override
            public void onReportFailure(String errormessage) {
                Log.d(TAG, "ScriptResolver " + mId + " failed to initialize.");
            }
        });
    }

    private synchronized void invokeWaitingJobs() {
        Log.d(TAG, "Resolving " + mWaitingQueries.size() + " waiting queries. Looking up "
                + mWaitingUrlLookups.size() + " waiting URLs.");
        for (Query query : mWaitingQueries) {
            resolve(query);
        }
        mWaitingQueries.clear();
        for (String url : mWaitingUrlLookups) {
            lookupUrl(url);
        }
        mWaitingUrlLookups.clear();
    }

    /**
     * This method tries to get the {@link Resolver}'s settings.
     */
    private void settings() {
        ScriptJob.start(mScriptObject, "settings",
                new ScriptJob.ResultsCallback<ScriptResolverSettings>(
                        ScriptResolverSettings.class) {
                    @Override
                    public void onReportResults(ScriptResolverSettings results) {
                        mWeight = results.weight;
                        mTimeout = results.timeout * 1000;
                        resolverGetConfigUi();
                    }
                });
    }

    /**
     * This method tries to save the {@link Resolver}'s UserConfig.
     */
    public void saveUserConfig() {
        ScriptJob.start(mScriptObject, "saveUserConfig");
    }

    /**
     * This method tries to get the {@link Resolver}'s UserConfig.
     */
    private void resolverGetConfigUi() {
        ScriptJob.start(mScriptObject, "configUi",
                new ScriptJob.ResultsArrayCallback() {
                    @Override
                    public void onReportResults(JsonArray results) {
                        Type type = new TypeToken<List<ScriptResolverConfigUiField>>() {
                        }.getType();
                        mConfigUi = GsonHelper.get().fromJson(results, type);
                    }
                });
    }

    public void lookupUrl(final String url) {
        if (mInitialized) {
            HashMap<String, Object> args = new HashMap<>();
            args.put("url", url);
            ScriptJob.start(mScriptObject, "lookupUrl", args,
                    new ScriptJob.ResultsCallback<ScriptResolverUrlResult>(
                            ScriptResolverUrlResult.class) {
                        @Override
                        public void onReportResults(ScriptResolverUrlResult results) {
                            Log.d(TAG, "reportUrlResult - url: " + url);
                            PipeLine.UrlResultsEvent event = new PipeLine.UrlResultsEvent();
                            event.mResolver = ScriptResolver.this;
                            event.mResult = results;
                            EventBus.getDefault().post(event);
                            mStopped = true;
                        }
                    });
        } else {
            mWaitingUrlLookups.add(url);
        }
    }

    /**
     * Invoke the javascript to resolve the given {@link Query}.
     *
     * @param query the {@link Query} which should be resolved
     */
    @Override
    public void resolve(final Query query) {
        if (mInitialized) {
            mStopped = false;
            mTimeOutHandler.removeCallbacksAndMessages(null);
            mTimeOutHandler.sendEmptyMessageDelayed(TIMEOUT_HANDLER_MSG, mTimeout);

            ScriptJob.ResultsObjectCallback callback = new ScriptJob.ResultsObjectCallback() {
                @Override
                public void onReportResults(JsonObject results) {
                    JsonArray tracks = results.getAsJsonArray("tracks");
                    ArrayList<Result> parsedResults =
                            ScriptUtils.parseResultList(ScriptResolver.this, tracks);
                    PipeLine.get().reportResults(query, parsedResults, mId);
                    mTimeOutHandler.removeCallbacksAndMessages(null);
                    mStopped = true;
                }
            };

            if (query.isFullTextQuery()) {
                HashMap<String, Object> args = new HashMap<>();
                args.put("query", query.getFullTextQuery());
                ScriptJob.start(mScriptObject, "_adapter_search", args, callback);
            } else {
                HashMap<String, Object> args = new HashMap<>();
                args.put("artist", query.getBasicTrack().getArtist().getName());
                args.put("album", query.getBasicTrack().getAlbum().getName());
                args.put("track", query.getBasicTrack().getName());
                ScriptJob.start(mScriptObject, "_adapter_resolve", args, callback);
            }
        } else {
            mWaitingQueries.add(query);
        }
    }

    public Promise<String, Throwable, Void> getStreamUrl(final Result result) {
        final ADeferredObject<String, Throwable, Void> deferred = new ADeferredObject<>();
        if (result != null) {
            HashMap<String, Object> args = new HashMap<>();
            args.put("url", result.getPath());
            ScriptJob.start(mScriptObject, "getStreamUrl", args,
                    new ScriptJob.ResultsCallback<ScriptResolverStreamUrlResult>(
                            ScriptResolverStreamUrlResult.class) {
                        @Override
                        public void onReportResults(ScriptResolverStreamUrlResult results) {
                            Response response = null;
                            try {
                                if (results.headers != null) {
                                    // If headers are given we first have to resolve the url that
                                    // the call is being redirected to
                                    response = NetworkUtils.httpRequest("GET",
                                            results.url, results.headers, null, null, null, false,
                                            null);
                                    deferred.resolve(response.header("Location"));
                                } else {
                                    deferred.resolve(results.url);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "reportStreamUrl: " + e.getClass() + ": " + e
                                        .getLocalizedMessage());
                                deferred.reject(e);
                            } finally {
                                if (response != null) {
                                    try {
                                        response.body().close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "getStreamUrl: " + e.getClass() + ": "
                                                + e.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                    }, new ScriptJob.FailureCallback() {
                        @Override
                        public void onReportFailure(String errormessage) {
                            deferred.reject(new Throwable(errormessage));
                        }
                    });
        } else {
            deferred.reject(new Throwable("result is null"));
        }
        return deferred;
    }

    public void login() {
        ScriptJob.start(mScriptObject, "login", null, new ScriptJob.ResultsPrimitiveCallback() {
            @Override
            public void onReportResults(JsonPrimitive results) {
                onTestConfigFinished(results);
            }
        });
    }

    public void logout() {
        ScriptJob.start(mScriptObject, "logout", null, new ScriptJob.ResultsPrimitiveCallback() {
            @Override
            public void onReportResults(JsonPrimitive results) {
                onTestConfigFinished(results);
            }
        });
    }

    /**
     * @return this {@link ScriptResolver}'s id
     */
    @Override
    public String getId() {
        return mId;
    }

    public String getName() {
        return mScriptAccount.getMetaData().name;
    }

    public void setConfig(Map<String, Object> config) {
        mScriptAccount.setConfig(config);
    }

    /**
     * @return the Map<String, String> containing the Config information of this resolver
     */
    public Map<String, Object> getConfig() {
        return mScriptAccount.getConfig();
    }

    /**
     * @return this {@link ScriptResolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }

    public String getDescription() {
        return mScriptAccount.getMetaData().description;
    }

    public List<ScriptResolverConfigUiField> getConfigUi() {
        return mConfigUi;
    }

    @Override
    public boolean isEnabled() {
        AuthenticatorUtils utils = AuthenticatorManager.get().getAuthenticatorUtils(mId);
        if (utils != null) {
            return utils.isLoggedIn();
        }
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        Log.d(TAG, this.mId + " has been " + (enabled ? "enabled" : "disabled"));
        mEnabled = enabled;
        Map<String, Object> config = getConfig();
        config.put(ScriptAccount.ENABLED_KEY, enabled);
        setConfig(config);
        if (mEnabled) {
            // Re-init so that all plugins are being registered again
            settings();
            init();
        } else {
            mScriptAccount.unregisterAllPlugins();
        }
        EventBus.getDefault().post(new EnabledStateChangedEvent());
    }

    public void testConfig(Map<String, Object> config) {
        // Always wipe all cookies in the testingConfig cookie store beforehand
        mScriptAccount.getCookieManager(true).getCookieStore().removeAll();

        ScriptJob.start(mScriptObject, "_adapter_testConfig", config,
                new ScriptJob.ResultsPrimitiveCallback() {
                    @Override
                    public void onReportResults(JsonPrimitive results) {
                        onTestConfigFinished(results);
                    }
                });
    }

    private void onTestConfigFinished(JsonPrimitive results) {
        int type = -1;
        String message = null;
        if (results.isString()) {
            type = AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER;
            message = results.getAsString();
        } else if (results.isNumber()
                && results.getAsInt() > 0 && results.getAsInt() < 8) {
            type = results.getAsInt();
        }
        Log.d(TAG, getName() + ": Config test result received. type: " + type
                + ", message:" + message);
        if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
        AuthenticatorManager.ConfigTestResultEvent event
                = new AuthenticatorManager.ConfigTestResultEvent();
        event.mComponent = ScriptResolver.this;
        event.mType = type;
        event.mMessage = message;
        EventBus.getDefault().post(event);
        AuthenticatorManager.showToast(getPrettyName(), event);
    }

    public void getAccessToken(ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult> cb) {
        ScriptJob.start(mScriptObject, "getAccessToken", cb);
    }
}
