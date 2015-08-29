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
import com.google.gson.JsonPrimitive;

import com.squareup.okhttp.Response;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverConfigUi;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverSettings;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverStreamUrlResult;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverUrlResult;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.libtomahawk.utils.NetworkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private ScriptResolverConfigUi mConfigUi;

    private boolean mEnabled;

    private boolean mReady;

    private boolean mStopped;

    private boolean mBrowsable;

    private boolean mPlaylistSync;

    private boolean mAccountFactory;

    private boolean mUrlLookup;

    private boolean mConfigTestable;

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

        if (mScriptAccount.getMetaData().staticCapabilities != null) {
            for (String capability : mScriptAccount.getMetaData().staticCapabilities) {
                if (capability.equals("configTestable")) {
                    mConfigTestable = true;
                }
            }
        }
        mReady = false;
        mStopped = true;
        mId = mScriptAccount.getMetaData().pluginName;
        if (getConfig().get(ScriptAccount.ENABLED_KEY) != null) {
            mEnabled = (Boolean) getConfig().get(ScriptAccount.ENABLED_KEY);
        } else {
            if (TomahawkApp.PLUGINNAME_JAMENDO.equals(mId)
                    || TomahawkApp.PLUGINNAME_SOUNDCLOUD.equals(mId)) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
        settings();
        init();
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isReady() {
        return mReady;
    }

    /**
     * @return whether or not this {@link ScriptResolver} is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mScriptAccount.getPath() + "/content/" + mScriptAccount.getMetaData().manifest.icon,
                grayOut ? R.color.disabled_resolver : 0);
    }

    @Override
    public void loadIconWhite(ImageView imageView) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mScriptAccount.getPath() + "/content/" + mScriptAccount
                        .getMetaData().manifest.iconWhite);
    }

    @Override
    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mScriptAccount.getPath() + "/content/" + mScriptAccount
                        .getMetaData().manifest.iconBackground,
                grayOut ? R.color.disabled_resolver : 0);
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

    @Override
    public void start(ScriptJob job) {
        mScriptAccount.startJob(job);
    }

    /**
     * This method calls the js function resolver.init().
     */
    private void init() {
        ScriptJob.start(mScriptObject, "init");
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
                        mReady = true;
                        PipeLine.get().onResolverReady(ScriptResolver.this);
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
        ScriptJob.start(mScriptObject, "getConfigUi",
                new ScriptJob.ResultsCallback<ScriptResolverConfigUi>(
                        ScriptResolverConfigUi.class) {
                    @Override
                    public void onReportResults(ScriptResolverConfigUi results) {
                        mConfigUi = results;
                    }
                });
    }

    public void lookupUrl(final String url) {
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
    }

    /**
     * Invoke the javascript to resolve the given {@link Query}.
     *
     * @param query the {@link Query} which should be resolved
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(final Query query) {
        if (mReady) {
            mStopped = false;
            mTimeOutHandler.removeCallbacksAndMessages(null);
            mTimeOutHandler.sendEmptyMessageDelayed(TIMEOUT_HANDLER_MSG, mTimeout);

            ScriptJob.ResultsArrayCallback callback = new ScriptJob.ResultsArrayCallback() {
                @Override
                public void onReportResults(JsonArray results) {
                    ArrayList<Result> parsedResults =
                            ScriptUtils.parseResultList(ScriptResolver.this, results);
                    PipeLine.get().reportResults(query, parsedResults, mId);
                    mTimeOutHandler.removeCallbacksAndMessages(null);
                    mStopped = true;
                }
            };

            if (query.isFullTextQuery()) {
                HashMap<String, Object> args = new HashMap<>();
                args.put("query", query.getFullTextQuery());
                ScriptJob.start(mScriptObject, "search", args, callback);
            } else {
                HashMap<String, Object> args = new HashMap<>();
                args.put("artist", query.getArtist().getName());
                args.put("album", query.getAlbum().getName());
                args.put("track", query.getName());
                ScriptJob.start(mScriptObject, "resolve", args, callback);
            }
        }
        return mReady;
    }

    public void getStreamUrl(final Result result) {
        if (result != null) {
            HashMap<String, Object> args = new HashMap<>();
            args.put("url", result.getPath());
            ScriptJob.start(mScriptObject, "getStreamUrl", args,
                    new ScriptJob.ResultsCallback<ScriptResolverStreamUrlResult>(
                            ScriptResolverStreamUrlResult.class) {
                        @Override
                        public void onReportResults(ScriptResolverStreamUrlResult results) {
                            try {
                                PipeLine.StreamUrlEvent event = new PipeLine.StreamUrlEvent();
                                event.mResult = result;
                                if (results.headers != null) {
                                    // If headers are given we first have to resolve the url that
                                    // the call is being redirected to
                                    Response connection = NetworkUtils.httpRequest("GET",
                                            results.url, results.headers, null, null, null, false);
                                    event.mUrl = connection.header("Location");
                                } else {
                                    event.mUrl = results.url;
                                }
                                EventBus.getDefault().post(event);
                            } catch (IOException e) {
                                Log.e(TAG, "reportStreamUrl: " + e.getClass() + ": " + e
                                        .getLocalizedMessage());
                            }
                        }
                    });
        }
    }

    public void login() {
        ScriptJob.start(mScriptObject, "login");
    }

    public void logout() {
        ScriptJob.start(mScriptObject, "logout");
    }

    public void onRedirectCallback(String url) {
        if (url != null) {
            HashMap<String, Object> args = new HashMap<>();
            args.put("url", url);
            ScriptJob.start(mScriptObject, "onRedirectCallback", args);
        }
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

    public ScriptResolverConfigUi getConfigUi() {
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
        EventBus.getDefault().post(new EnabledStateChangedEvent());
    }

    public void reportCapabilities(int in) {
        BigInteger bigInt = BigInteger.valueOf(in);
        if (bigInt.testBit(0)) {
            mBrowsable = true;
        }
        if (bigInt.testBit(1)) {
            mPlaylistSync = true;
        }
        if (bigInt.testBit(2)) {
            mAccountFactory = true;
        }
        if (bigInt.testBit(3)) {
            mUrlLookup = true;
        }
        if (bigInt.testBit(4)) {
            mConfigTestable = true;
        }
    }

    public boolean isBrowsable() {
        return mBrowsable;
    }

    public boolean isPlaylistSync() {
        return mPlaylistSync;
    }

    public boolean isAccountFactory() {
        return mAccountFactory;
    }

    public boolean hasUrlLookup() {
        return mUrlLookup;
    }

    public boolean isConfigTestable() {
        return mConfigTestable;
    }

    public void testConfig(Map<String, Object> config) {
        ScriptJob.start(mScriptObject, "testConfig", config,
                new ScriptJob.ResultsPrimitiveCallback() {
                    @Override
                    public void onReportResults(JsonPrimitive results) {
                        if (results.isString()) {
                            onConfigTestResult(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                    results.getAsString());
                        } else if (results.isNumber()
                                && results.getAsInt() > 0 && results.getAsInt() < 8) {
                            onConfigTestResult(results.getAsInt(), null);
                        }
                    }
                });
    }

    public void onConfigTestResult(final int type, final String message) {
        Log.d(TAG, getName() + ": Config test result received. type: " + type + ", message:"
                + message);
        if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
        AuthenticatorManager.ConfigTestResultEvent event
                = new AuthenticatorManager.ConfigTestResultEvent();
        event.mComponent = this;
        event.mType = type;
        event.mMessage = message;
        EventBus.getDefault().post(event);
        AuthenticatorManager.showToast(getPrettyName(), event);
    }

    public void getAccessToken(ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult> cb) {
        ScriptJob.start(mScriptObject, "getAccessToken", cb);
    }
}
