/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

/**
 * Represents a Tomahawk ControlConnection. Used for LAN communications.
 */
public class TomahawkServerConnection {

    public static final String ACCOUNT_TYPE = "org.tomahawk";
    public static final String AUTH_TOKEN_TYPE = "org.tomahawk.auth_token";
    public static final String ACCOUNT_NAME = "Tomahawk";

    private final static String TAG = TomahawkServerConnection.class.getName();

    private HandlerThread mCollectionUpdateHandlerThread;
    private Handler mHandler;

    private String mUserId;
    private String mAuthToken;

    /**
     * Runnable that requests accessTokens to start a Connection on.
     */
    private Runnable mStartupConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            String avail = requestAccessTokens(mUserId, mAuthToken);

            // parse the access token and create a new TomahawkWebSocket here.
            Log.e(TAG, avail);
        }
    };

    /**
     * Creates a new TomahawkServerConnection for the given userid and
     * authtoken.
     * 
     * @param userid
     * @param authtoken
     */
    protected TomahawkServerConnection(String userid, String authtoken) {
        mUserId = userid;
        mAuthToken = authtoken;

        mCollectionUpdateHandlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mCollectionUpdateHandlerThread.start();
        mHandler = new Handler(mCollectionUpdateHandlerThread.getLooper());

        mHandler.postDelayed(mStartupConnectionRunnable, 1);
    }

    /**
     * Returns the TomahawkServerConnection for the given userid and auth token.
     * 
     * If a TomahawkServerConnection does not exist for the pair, then a new
     * TomahawkServerConnection is created.
     * 
     * @param authtoken
     * @return
     */
    public static TomahawkServerConnection get(String userid, String authtoken) {
        return new TomahawkServerConnection(userid, authtoken);
    }

    /**
     * Requests access tokens for the given user id and valid auth token.
     * 
     * @param userid
     * @param authToken
     * @return
     */
    private static String requestAccessTokens(String userid, String authToken) {

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", userid);
        params.put("authtoken", authToken);

        return post(new JSONObject(params));
    }

    /**
     * Authenticates the credentials against the Tomahawk server and return the
     * auth token.
     * 
     * @param username
     * @param password
     * @return auth token.
     */
    public static String authenticate(String name, String passwd) {

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", name);
        params.put("password", passwd);

        return post(new JSONObject(params));
    }

    /**
     * Post parameters to the Tomahawk server.
     * 
     * @param params
     * @return
     */
    private static String post(JSONObject params) {

        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        TomahawkHttpClient httpclient = new TomahawkHttpClient(httpParams);

        HttpPost httpost = new HttpPost("https://auth.jefferai.org/login");

        try {

            httpost.setEntity(new StringEntity(params.toString()));

            httpost.setHeader("Accept", "application/json; charset=utf-8");
            httpost.setHeader("Content-type", "application/json; charset=utf-8");
            HttpResponse httpresponse = httpclient.execute(httpost);

            BufferedReader reader = null;
            JSONObject jsonObj = null;

            reader = new BufferedReader(new InputStreamReader(httpresponse.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            jsonObj = new JSONObject(json);

            Log.d(TAG, "Tomahawk server response: " + jsonObj.toString());

            /* Test if an error occurred. */
            if (jsonObj.has("error") && jsonObj.getString("error").equals("true"))
                throw new IllegalArgumentException(jsonObj.getString("errormsg"));

            if (jsonObj.has("accesstokens"))
                return jsonObj.toString();

            return jsonObj.getString("authtoken");

        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "Uknown error authenticating against Tomahawk server.");
        return null;
    }
}
