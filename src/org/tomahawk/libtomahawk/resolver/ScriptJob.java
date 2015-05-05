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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;

import android.util.Log;

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

    private ResultsCallback mResultsCallback;

    private FailureCallback mFailureCallback;

    public interface ResultsCallback {

        void onReportResults(JsonNode results);
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
     * @param resultsCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     * @param failureCallback A callback object that will get called when the request has failed.
     */
    public static void start(ScriptObject object, String methodName, Map<String, Object> arguments,
            ResultsCallback resultsCallback, FailureCallback failureCallback) {
        ScriptJob job = new ScriptJob(object, methodName, arguments, resultsCallback,
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
     * @param resultsCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     */
    public static void start(ScriptObject object, String methodName, Map<String, Object> arguments,
            ResultsCallback resultsCallback) {
        ScriptJob job = new ScriptJob(object, methodName, arguments, resultsCallback, null);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param resultsCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     */
    public static void start(ScriptObject object, String methodName,
            ResultsCallback resultsCallback) {
        ScriptJob job = new ScriptJob(object, methodName, null, resultsCallback, null);
        object.getScriptAccount().startJob(job);
    }

    /**
     * Convenience-method! Constructs and starts a new ScriptJob.
     *
     * @param object          The {@link ScriptObject} that is associated with this {@link
     *                        ScriptJob}. The {@link ScriptObject} represents the Java-{@link
     *                        ScriptPlugin} on the JS side.
     * @param methodName      The name of the method that will be called on the JS side.
     * @param resultsCallback A callback object that will get called when the request has
     *                        successfully returned from the JS side.
     * @param failureCallback A callback object that will get called when the request has failed.
     */
    public static void start(ScriptObject object, String methodName,
            ResultsCallback resultsCallback, FailureCallback failureCallback) {
        ScriptJob job = new ScriptJob(object, methodName, null, resultsCallback, failureCallback);
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
            ResultsCallback resultsCallback, FailureCallback failureCallback) {
        mScriptObject = object;
        mMethodName = methodName;
        mArguments = arguments;
        mResultsCallback = resultsCallback;
        if (failureCallback == null) {
            failureCallback = new FailureCallback() {
                @Override
                public void onReportFailure(String errormessage) {
                    String argumentsString = "not parseable";
                    try {
                        argumentsString =
                                InfoSystemUtils.getObjectMapper().writeValueAsString(mArguments);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    } finally {
                        Log.e(TAG, "ScriptJob failed - ScriptAccount: "
                                + mScriptObject.getScriptAccount().getName()
                                + ", methodName: " + mMethodName
                                + ", arguments: " + argumentsString
                                + ", errorMessage: " + errormessage);
                    }
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
    public void reportResults(JsonNode data) {
        if (mResultsCallback != null) {
            mResultsCallback.onReportResults(data);
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
