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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.HashMap;

public class AuthenticatorManager {

    public final static int CONFIG_TEST_RESULT_TYPE_OTHER = 0;

    public final static int CONFIG_TEST_RESULT_TYPE_SUCCESS = 1;

    public final static int CONFIG_TEST_RESULT_TYPE_LOGOUT = 2;

    public final static int CONFIG_TEST_RESULT_TYPE_COMMERROR = 3;

    public final static int CONFIG_TEST_RESULT_TYPE_INVALIDCREDS = 4;

    public final static int CONFIG_TEST_RESULT_TYPE_INVALIDACCOUNT = 5;

    public final static int CONFIG_TEST_RESULT_TYPE_PLAYINGELSEWHERE = 6;

    public final static int CONFIG_TEST_RESULT_TYPE_ACCOUNTEXPIRED = 7;

    private static class Holder {

        private static final AuthenticatorManager instance = new AuthenticatorManager();

    }

    public static class ConfigTestResultEvent {

        public Object mComponent;

        public int mType;

        public String mMessage;
    }

    private final HashMap<String, AuthenticatorUtils> mAuthenticatorUtils
            = new HashMap<>();

    private AuthenticatorManager() {
        HatchetAuthenticatorUtils hatchetAuthenticatorUtils = new HatchetAuthenticatorUtils();
        mAuthenticatorUtils.put(hatchetAuthenticatorUtils.getId(),
                hatchetAuthenticatorUtils);
    }

    public static AuthenticatorManager get() {
        return Holder.instance;
    }

    public AuthenticatorUtils getAuthenticatorUtils(String authenticatorId) {
        return mAuthenticatorUtils.get(authenticatorId);
    }

    public static void showToast(String componentName, ConfigTestResultEvent event) {
        String message;
        switch (event.mType) {
            case CONFIG_TEST_RESULT_TYPE_SUCCESS:
                message = TomahawkApp.getContext()
                        .getString(R.string.auth_logged_in, componentName);
                break;
            case CONFIG_TEST_RESULT_TYPE_LOGOUT:
                message = TomahawkApp.getContext()
                        .getString(R.string.auth_logged_out, componentName);
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
                message = componentName + ": " + event.mMessage;
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
