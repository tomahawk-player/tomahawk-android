/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.tomahawk.libtomahawk.utils.GsonHelper;

import android.util.Log;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A {@link ScriptJob} is an object that is being passed to the JavaScript side to handle a certain
 * action (like a login for example). The {@link ScriptJob} provides an easy way to directly get
 * callback data whenever the JS method has returned and the data has been passed to the Java side
 * again.
 */
public class ScriptJob {

    public static final String TAG = ScriptJob.class.getSimpleName();

    private ScriptObject mScriptObject;

    private String mMethodName;

    private Map<String, Object> mArguments;

    private SuccessCallback mSuccessCallback;

    private FailureCallback mFailureCallback;

    private interface SuccessCallback {

    }

    public interface ResultsArrayCallback extends SuccessCallback {

        void onReportResults(JsonArray results);
    }

    public interface ResultsObjectCallback extends SuccessCallback {

        void onReportResults(JsonObject results);
    }

    public interface ResultsPrimitiveCallback extends SuccessCallback {

        void onReportResults(JsonPrimitive results);
    }

    public interface ResultsEmptyCallback extends SuccessCallback {

        void onReportResults();
    }

    public static abstract class ResultsCallback<T> implements SuccessCallback {

        private Class<T> type;

        public ResultsCallback(Class<T> type) {
            this.type = type;
        }

        public abstract void onReportResults(T results);

        public Class<T> getType() {
            return type;
        }
    }

    public static abstract class ResultsCollectionCallback implements SuccessCallback {

        private Type type;

        public ResultsCollectionCallback(Type type) {
            this.type = type;
        }

        public abstract void onReportResults(Object results);

        public Type getType() {
            return type;
        }
    }

    public interface FailureCallback {

        void onReportFailure(String errormessage);
    }

    /**
     * Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param arguments       The set of arguments (parameters) that is provided to the called
     *                        method.
     * @param successCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     * @param failureCallback A callback object that will get called when the request has failed.
     */
    public static void start(ScriptObject object, String methodName, Map<String, Object> arguments,
            SuccessCallback successCallback, FailureCallback failureCallback) {
        ScriptJob job = new ScriptJob(object, methodName, arguments, successCallback,
                failureCallback);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param arguments       The set of arguments (parameters) that is provided to the called
     *                        method.
     * @param successCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     */
    public static void start(ScriptObject object, String methodName, Map<String, Object> arguments,
            SuccessCallback successCallback) {
        ScriptJob job = new ScriptJob(object, methodName, arguments, successCallback, null);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param successCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     */
    public static void start(ScriptObject object, String methodName,
            SuccessCallback successCallback) {
        ScriptJob job = new ScriptJob(object, methodName, null, successCallback, null);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param successCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     * @param failureCallback A callback object that will get called when the request has failed.
     */
    public static void start(ScriptObject object, String methodName,
            SuccessCallback successCallback, FailureCallback failureCallback) {
        ScriptJob job = new ScriptJob(object, methodName, null, successCallback, failureCallback);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object     The {@link ScriptObject} that is associated with this {@link ScriptJob}.
     *                   The {@link ScriptObject} represents the Java-{@link ScriptPlugin} on the JS
     *                   side.
     * @param methodName The name of the method that will be called on the JS side.
     * @param arguments  The set of arguments (parameters) that is provided to the called method.
     */
    public static void start(ScriptObject object, String methodName,
            Map<String, Object> arguments) {
        ScriptJob job = new ScriptJob(object, methodName, arguments, null, null);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object     The {@link ScriptObject} that is associated with this {@link ScriptJob}.
     *                   The {@link ScriptObject} represents the Java-{@link ScriptPlugin} on the JS
     *                   side.
     * @param methodName The name of the method that will be called on the JS side.
     */
    public static void start(ScriptObject object, String methodName) {
        ScriptJob job = new ScriptJob(object, methodName, null, null, null);
        object.getScriptAccount().startJob(job);
    }

    private ScriptJob(ScriptObject object, String methodName, Map<String, Object> arguments,
            SuccessCallback successCallback, FailureCallback failureCallback) {
        mScriptObject = object;
        mMethodName = methodName;
        mArguments = arguments;
        mSuccessCallback = successCallback;
        if (failureCallback == null) {
            failureCallback = new FailureCallback() {
                @Override
                public void onReportFailure(String errormessage) {
                    Log.e(TAG, "ScriptJob failed - ScriptAccount: "
                            + mScriptObject.getScriptAccount().getName()
                            + ", methodName: " + mMethodName
                            + ", arguments: " + GsonHelper.get().toJson(mArguments)
                            + ", errorMessage: " + errormessage);
                }
            };
        }
        mFailureCallback = failureCallback;
    }

    public ScriptObject getScriptObject() {
        return mScriptObject;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public Map<String, Object> getArguments() {
        return mArguments;
    }

    /**
     * This method is being called if the request was successful.
     *
     * @param data The returned data.
     */
    public void reportResults(JsonElement data) {
        if (mSuccessCallback instanceof ResultsCallback) {
            ResultsCallback callback = ((ResultsCallback) mSuccessCallback);
            callback.onReportResults(GsonHelper.get().fromJson(data, callback.getType()));
        } else if (data instanceof JsonObject
                && mSuccessCallback instanceof ResultsObjectCallback) {
            ((ResultsObjectCallback) mSuccessCallback).onReportResults((JsonObject) data);
        } else if (data instanceof JsonPrimitive
                && mSuccessCallback instanceof ResultsPrimitiveCallback) {
            ((ResultsPrimitiveCallback) mSuccessCallback).onReportResults((JsonPrimitive) data);
        } else if (data instanceof JsonArray && mSuccessCallback instanceof ResultsArrayCallback) {
            ((ResultsArrayCallback) mSuccessCallback).onReportResults((JsonArray) data);
        } else if (mSuccessCallback instanceof ResultsEmptyCallback) {
            ((ResultsEmptyCallback) mSuccessCallback).onReportResults();
        } else if (mSuccessCallback != null) {
            reportFailure("Unexpected result!");
        }
    }

    /**
     * This method is being called if the request failed.
     *
     * @param errorMessage Message that describes the error that occurred.
     */
    public void reportFailure(String errorMessage) {
        mFailureCallback.onReportFailure(errorMessage);
    }

}
