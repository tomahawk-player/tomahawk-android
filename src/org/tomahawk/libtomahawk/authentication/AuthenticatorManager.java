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
package org.tomahawk.libtomahawk.authentication;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.HashMap;

public class AuthenticatorManager {

    public final static String CONFIG_TEST_RESULT
            = "org.tomahawk.tomahawk_android.config_test_result";

    public final static String CONFIG_TEST_RESULT_PLUGINNAME = "config_test_result_pluginname";

    public final static String CONFIG_TEST_RESULT_PLUGINTYPE = "config_test_result_plugintype";

    public final static int CONFIG_TEST_RESULT_PLUGINTYPE_RESOLVER = 0;

    public final static int CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS = 1;

    public final static String CONFIG_TEST_RESULT_TYPE = "config_test_result_type";

    public final static int CONFIG_TEST_RESULT_TYPE_OTHER = 0;

    public final static int CONFIG_TEST_RESULT_TYPE_SUCCESS = 1;

    public final static int CONFIG_TEST_RESULT_TYPE_LOGOUT = 2;

    public final static int CONFIG_TEST_RESULT_TYPE_COMMERROR = 3;

    public final static int CONFIG_TEST_RESULT_TYPE_INVALIDCREDS = 4;

    public final static int CONFIG_TEST_RESULT_TYPE_INVALIDACCOUNT = 5;

    public final static int CONFIG_TEST_RESULT_TYPE_PLAYINGELSEWHERE = 6;

    public final static int CONFIG_TEST_RESULT_TYPE_ACCOUNTEXPIRED = 7;

    public final static String CONFIG_TEST_RESULT_MESSAGE = "config_test_result_message";

    public final static String CONFIG_TEST_RESULT_BUNDLE = "config_test_result_bundle";

    private static AuthenticatorManager instance;

    private boolean mInitialized;

    private HashMap<String, AuthenticatorUtils> mAuthenticatorUtils
            = new HashMap<String, AuthenticatorUtils>();

    private AuthenticatorManager() {
    }

    public static AuthenticatorManager getInstance() {
        if (instance == null) {
            synchronized (AuthenticatorManager.class) {
                if (instance == null) {
                    instance = new AuthenticatorManager();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;
            SpotifyAuthenticatorUtils spotifyAuthenticatorUtils = new SpotifyAuthenticatorUtils();
            mAuthenticatorUtils.put(spotifyAuthenticatorUtils.getId(), spotifyAuthenticatorUtils);
            HatchetAuthenticatorUtils hatchetAuthenticatorUtils = new HatchetAuthenticatorUtils();
            mAuthenticatorUtils.put(hatchetAuthenticatorUtils.getId(), hatchetAuthenticatorUtils);
            RdioAuthenticatorUtils rdioAuthenticatorUtils = new RdioAuthenticatorUtils();
            mAuthenticatorUtils.put(rdioAuthenticatorUtils.getId(), rdioAuthenticatorUtils);
            DeezerAuthenticatorUtils deezerAuthenticatorUtils = new DeezerAuthenticatorUtils();
            mAuthenticatorUtils.put(deezerAuthenticatorUtils.getId(), deezerAuthenticatorUtils);
        }
    }

    public AuthenticatorUtils getAuthenticatorUtils(String authenticatorId) {
        return mAuthenticatorUtils.get(authenticatorId);
    }

    /**
     * Send a broadcast letting everybody know a certain component's (resolver or authUtils)
     * message. Also display this action to the user.
     *
     * @param componentId   the id of the component that is sending the message
     * @param componentType the type of the component
     * @param type          the type of the message (used to standardize the phrase, e.g. in case of
     *                      invalid creds)
     */
    public static void broadcastConfigTestResult(String componentId, int componentType,
            int type) {
        broadcastConfigTestResult(componentId, componentType, type, "", null);
    }

    /**
     * Send a broadcast letting everybody know a certain component's (resolver or authUtils)
     * message. Also display this action to the user.
     *
     * @param componentId   the id of the component that is sending the message
     * @param componentType the type of the component
     * @param type          the type of the message (used to standardize the phrase, e.g. in case of
     *                      invalid creds)
     * @param message       the message to send (can be empty, if type is given)
     */
    public static void broadcastConfigTestResult(String componentId, int componentType,
            int type, String message) {
        broadcastConfigTestResult(componentId, componentType, type, message, null);
    }

    /**
     * Send a broadcast letting everybody know a certain component's (resolver or authUtils)
     * message. Also display this action to the user.
     *
     * @param componentId   the id of the component that is sending the message
     * @param componentType the type of the component
     * @param type          the type of the message (used to standardize the phrase, e.g. in case of
     *                      invalid creds)
     * @param message       the message to send (can be empty, if type is given)
     * @param bundle        additional data to send with the broadcast can be put in this bundle
     */
    public static void broadcastConfigTestResult(String componentId, int componentType,
            int type, String message, Bundle bundle) {
        if (componentId != null) {
            Intent intent = new Intent(CONFIG_TEST_RESULT);
            intent.putExtra(CONFIG_TEST_RESULT_PLUGINNAME, componentId);
            intent.putExtra(CONFIG_TEST_RESULT_PLUGINTYPE, componentType);
            intent.putExtra(CONFIG_TEST_RESULT_TYPE, type);
            intent.putExtra(CONFIG_TEST_RESULT_MESSAGE, message);
            if (bundle != null) {
                intent.putExtra(CONFIG_TEST_RESULT_BUNDLE, bundle);
            }
            TomahawkApp.getContext().sendBroadcast(intent);
        }
        String componentName = "";
        switch (componentType) {
            case CONFIG_TEST_RESULT_PLUGINTYPE_RESOLVER:
                Resolver resolver = PipeLine.getInstance().getResolver(componentId);
                if (resolver != null) {
                    componentName = resolver.getPrettyName();
                }
                break;
            case CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS:
                AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                        .getAuthenticatorUtils(componentId);
                if (utils != null) {
                    componentName = utils.getPrettyName();
                }
                break;
        }
        switch (type) {
            case CONFIG_TEST_RESULT_TYPE_SUCCESS:
                message = TomahawkApp.getContext().getString(R.string.auth_logged_in) + " "
                        + componentName;
                break;
            case CONFIG_TEST_RESULT_TYPE_LOGOUT:
                message = TomahawkApp.getContext().getString(R.string.auth_logged_out) + " "
                        + componentName;
                break;
            case CONFIG_TEST_RESULT_TYPE_INVALIDCREDS:
                message = componentName + ": " + TomahawkApp.getContext().getString(
                        R.string.error_invalid_credentials);
                break;
            case CONFIG_TEST_RESULT_TYPE_INVALIDACCOUNT:
                message = componentName + ": " + TomahawkApp.getContext().getString(
                        R.string.error_invalid_account);
                break;
            case CONFIG_TEST_RESULT_TYPE_COMMERROR:
                message = componentName + ": " + TomahawkApp.getContext().getString(
                        R.string.error_communication);
                break;
            case CONFIG_TEST_RESULT_TYPE_PLAYINGELSEWHERE:
                message = componentName + ": " + TomahawkApp.getContext().getString(
                        R.string.error_simultaneous_streams);
                break;
            case CONFIG_TEST_RESULT_TYPE_ACCOUNTEXPIRED:
                message = componentName + ": " + TomahawkApp.getContext().getString(
                        R.string.error_account_expired);
                break;
            default:
                message = componentName + ": " + message;
        }
        final String msg = message;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TomahawkApp.getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
