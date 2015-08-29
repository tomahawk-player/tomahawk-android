package org.tomahawk.libtomahawk.utils;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    public static final String HTTP_METHOD_POST = "POST";

    public static final String HTTP_METHOD_GET = "GET";

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
     * @return a HttpURLConnection
     */
    public static HttpURLConnection httpRequest(String method, String urlString,
            Map<String, String> extraHeaders, final String username, final String password,
            String data, boolean followRedirects)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        HttpURLConnection connection;

        // Establish correct HTTP/HTTPS connection
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        if (urlConnection instanceof HttpsURLConnection) {
            connection = setSSLSocketFactory((HttpsURLConnection) urlConnection);
        } else if (urlConnection instanceof HttpURLConnection) {
            connection = (HttpURLConnection) urlConnection;
        } else {
            throw new MalformedURLException(
                    "Connection could not be cast to HttpUrlConnection");
        }

        // Configure HTTP Basic Auth if available
        if (username != null && password != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        }

        // Add headers if available
        if (extraHeaders != null) {
            for (String key : extraHeaders.keySet()) {
                connection.setRequestProperty(key, extraHeaders.get(key));
            }
        }

        // Set timeout to 15 sec
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        // Set the given request method if available - default to "GET"
        if (!TextUtils.isEmpty(method) && !method.equals(HTTP_METHOD_GET)) {
            if (method.equals(HTTP_METHOD_POST)) {
                connection.setRequestMethod(HTTP_METHOD_POST);
                connection.setDoOutput(true);
            } else {
                connection.setRequestMethod(method);
            }
        } else {
            connection.setRequestMethod(HTTP_METHOD_GET);
            connection.setDoOutput(false);
        }

        // Send data string if available
        if (!TextUtils.isEmpty(data)) {
            connection.setFixedLengthStreamingMode(data.getBytes().length);
            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(connection.getOutputStream());
                out.write(data);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        // configure whether or not to follow redirects
        connection.setInstanceFollowRedirects(followRedirects);

        return connection;
    }

    private static HttpsURLConnection setSSLSocketFactory(HttpsURLConnection connection)
            throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sc;
        sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new java.security.SecureRandom());
        connection.setSSLSocketFactory(sc.getSocketFactory());
        return connection;
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
