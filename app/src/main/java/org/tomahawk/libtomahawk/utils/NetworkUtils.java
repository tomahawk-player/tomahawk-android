package org.tomahawk.libtomahawk.utils;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    private static final MediaType MEDIA_TYPE_FORM =
            MediaType.parse("application/x-www-form-urlencoded");

    private static Map<String, CookieManager> sCookieManagerMap = new ConcurrentHashMap<>();

    public static CookieManager getCookieManager(String cookieContextId) {
        if (sCookieManagerMap.containsKey(cookieContextId)) {
            return sCookieManagerMap.get(cookieContextId);
        } else {
            CookieManager cookieManager = new CookieManager(
                    new PersistentCookieStore(TomahawkApp.getContext(), cookieContextId),
                    CookiePolicy.ACCEPT_ALL);
            sCookieManagerMap.put(cookieContextId, cookieManager);
            return cookieManager;
        }
    }

    /**
     * Does a HTTP or HTTPS request
     *
     * @param method          the method that should be used ("GET" or "POST"), defaults to "GET"
     *                        (optional)
     * @param urlString       the complete url string to do the request with
     * @param extraHeaders    extra headers that should be added to the request (optional)
     * @param username        the username for HTTP Basic Auth (optional)
     * @param password        the password for HTTP Basic Auth (optional)
     * @param data            the body data included in POST requests (optional)
     * @param followRedirects whether or not to follow redirects (also defines what is being
     *                        returned)
     * @param cookieManager   the {@link CookieManager} that should be used for this request
     * @return a HttpURLConnection
     */
    public static Response httpRequest(String method, String urlString,
            Map<String, String> extraHeaders, final String username, final String password,
            String data, boolean followRedirects, CookieManager cookieManager) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        client.networkInterceptors().add(loggingInterceptor);
        if (cookieManager != null) {
            client.setCookieHandler(cookieManager);
        }

        //Set time-outs
        client.setConnectTimeout(15000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(15000, TimeUnit.MILLISECONDS);

        client.setFollowRedirects(followRedirects);

        // Configure HTTP Basic Auth if available
        if (username != null && password != null) {
            client.setAuthenticator(new com.squareup.okhttp.Authenticator() {
                @Override
                public Request authenticate(Proxy proxy, Response response) throws IOException {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder().header("Authorization", credential)
                            .build();
                }

                @Override
                public Request authenticateProxy(Proxy proxy, Response response)
                        throws IOException {
                    return null;
                }
            });
        }

        // Create request for remote resource.
        Request.Builder builder = new Request.Builder().url(urlString);

        // Add headers if available
        if (extraHeaders != null) {
            for (String key : extraHeaders.keySet()) {
                builder.addHeader(key, extraHeaders.get(key));
            }
        }

        // Properly set up the request method. Default to GET
        if (method != null) {
            method = method.toUpperCase();
        }
        if (method == null || method.equals("GET")) {
            builder.get();
        } else {
            MediaType mediaType = MEDIA_TYPE_FORM;
            if (extraHeaders != null) {
                String contentType = extraHeaders.get("Content-Type");
                if (contentType != null) {
                    mediaType = MediaType.parse(contentType);
                }
            }
            RequestBody requestBody = null;
            if (data != null) {
                requestBody = RequestBody.create(mediaType, data);
            }
            builder.method(method, requestBody);
        }

        // Build and execute the request and retrieve the response.
        Request request = builder.build();
        return client.newCall(request).execute();
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                TomahawkApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isWifiAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                TomahawkApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting()
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
