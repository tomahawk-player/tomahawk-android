package org.tomahawk.libtomahawk.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptInterface;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.GrayOutTransformation;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.CircularImageTransformation;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class TomahawkUtils {

    public static String TAG = TomahawkUtils.class.getSimpleName();

    public static String HTTP_METHOD_POST = "POST";

    public static String HTTP_METHOD_GET = "GET";

    public static String HTTP_CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    public static String HTTP_CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    public static class HttpResponse {

        public HttpResponse() {
            mResponseHeaders = new HashMap<String, List<String>>();
        }

        public String mResponseText;

        public Map<String, List<String>> mResponseHeaders;

        public int mStatus;

        public String mStatusText;
    }

    /**
     * Author: Chas Emerick (source: http://mrfoo.de/archiv/1176-Levenshtein-Distance-in-Java.html)
     *
     * This method uses the LevenstheinDistance algorithm to compute the similarity of two strings.
     *
     * @return the minimum number of single-character edits required to change one of the given
     * strings into the other
     */
    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }
        if (TextUtils.isEmpty(s)) {
            return t.length();
        } else if (TextUtils.isEmpty(t)) {
            return s.length();
        }
        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param dp A value in dp(Device independent pixels) unit. Which we need to convert into
     *           pixels
     * @return A float value to represent Pixels equivalent to dp according to device
     */
    public static int convertDpToPixel(int dp) {
        Resources resources = TomahawkApp.getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }

    /**
     * Converts a track duration int into the proper String format
     *
     * @param duration the track's duration
     * @return the formated string
     */
    public static String durationToString(long duration) {
        return String.format("%02d", (duration / 60000)) + ":" + String
                .format("%02.0f", (double) (duration / 1000) % 60);
    }

    /**
     * Parse a given String into a Date.
     */
    public static Date stringToDate(String rawDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = null;
        try {
            date = dateFormat.parse(rawDate);
        } catch (ParseException e) {
            Log.e(TAG, "stringToDate: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return date;
    }

    private static String getCacheKey(String... strings) {
        String result = "";
        for (String s : strings) {
            result += "\t\t" + s.toLowerCase();
        }
        return result;
    }

    public static String getCacheKey(TomahawkListItem tomahawkListItem) {
        if (tomahawkListItem instanceof Artist) {
            return getCacheKey(tomahawkListItem.getName());
        } else if (tomahawkListItem instanceof Album) {
            return getCacheKey(tomahawkListItem.getName(), tomahawkListItem.getArtist().getName());
        } else if (tomahawkListItem instanceof Track) {
            return getCacheKey(tomahawkListItem.getName(), tomahawkListItem.getAlbum().getName(),
                    tomahawkListItem.getArtist().getName());
        } else if (tomahawkListItem instanceof Query) {
            Query query = ((Query) tomahawkListItem);
            if (query.isFullTextQuery()) {
                return getCacheKey(query.getFullTextQuery(), String.valueOf(query.isOnlyLocal()));
            } else {
                return getCacheKey(query.getBasicTrack().getCacheKey(), query.getResultHint());
            }
        } else if (tomahawkListItem instanceof PlaylistEntry) {
            PlaylistEntry playlistEntry = ((PlaylistEntry) tomahawkListItem);
            return getCacheKey(playlistEntry.getPlaylistId(), playlistEntry.getId());
        }
        return "";
    }

    public static String getCacheKey(Image image) {
        return getCacheKey(image.getImagePath());
    }

    public static String getCacheKey(Result result, String queryKey) {
        return getCacheKey(result.getTrack().getCacheKey(), result.getAlbum().getCacheKey(),
                result.getArtist().getCacheKey(), result.getPath(), queryKey);
    }

    /**
     * Gets the URL that this request has been redirected to.
     *
     * @param method       the method that should be used ("GET" or "POST"), defaults to "GET"
     *                     (optional)
     * @param urlString    the complete url string to do the request with
     * @param extraHeaders extra headers that should be added to the request (optional)
     * @return a String containing the url that this request has been redirected to, otherwise null
     */
    public static String getRedirectedUrl(String method, String urlString,
            Map<String, String> extraHeaders)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        HttpResponse response =
                httpRequest(method, urlString, extraHeaders, null, null, null, null, false);
        List<String> responseHeaders = response.mResponseHeaders.get("Location");
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            return response.mResponseHeaders.get("Location").get(0);
        }
        return null;
    }

    /**
     * Does a HTTP or HTTPS request (convenience method)
     *
     * @param method       the method that should be used ("GET" or "POST"), defaults to "GET"
     *                     (optional)
     * @param urlString    the complete url string to do the request with
     * @param extraHeaders extra headers that should be added to the request (optional)
     * @param username     the username for HTTP Basic Auth (optional)
     * @param password     the password for HTTP Basic Auth (optional)
     * @param data         the body data included in POST requests (optional)
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    private static HttpResponse httpRequest(String method, String urlString,
            Map<String, String> extraHeaders, final String username, final String password,
            String data)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        return httpRequest(method, urlString, extraHeaders, username, password, data, null,
                true);
    }

    /**
     * Does a HTTP or HTTPS request (convenience method)
     *
     * @param method       the method that should be used ("GET" or "POST"), defaults to "GET"
     *                     (optional)
     * @param urlString    the complete url string to do the request with
     * @param extraHeaders extra headers that should be added to the request (optional)
     * @param username     the username for HTTP Basic Auth (optional)
     * @param password     the password for HTTP Basic Auth (optional)
     * @param data         the body data included in POST requests (optional)
     * @param callback     a ScriptInterface.JsCallback that should be called if this request has
     *                     been successful (optional)
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    public static HttpResponse httpRequest(String method, String urlString,
            Map<String, String> extraHeaders, final String username, final String password,
            String data, ScriptInterface.JsCallback callback)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        return httpRequest(method, urlString, extraHeaders, username, password, data, callback,
                true);
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
     * @param callback        a ScriptInterface.JsCallback that should be called if this request has
     *                        been successful (optional)
     * @param followRedirects whether or not to follow redirects (also defines what is being
     *                        returned)
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    private static HttpResponse httpRequest(String method, String urlString,
            Map<String, String> extraHeaders, final String username, final String password,
            String data, ScriptInterface.JsCallback callback, boolean followRedirects)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        HttpResponse response = new HttpResponse();
        HttpURLConnection connection = null;
        try {
            // Establish correct HTTP/HTTPS connection
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
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

            try {
                response.mResponseText = inputStreamToString(connection.getInputStream());
            } catch (IOException e) {
                InputStream stream = connection.getErrorStream();
                if (stream != null) {
                    response.mResponseText = inputStreamToString(stream);
                }
            }
            response.mResponseHeaders = connection.getHeaderFields();
            response.mStatus = connection.getResponseCode();
            response.mStatusText = connection.getResponseMessage();

            if (callback != null) {
                callback.call(response);
            }
        } finally {
            // Always disconnect connection to avoid leaks
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    /**
     * Does a HTTP/HTTPS POST request
     *
     * @param urlString    the complete url string to do the request with
     * @param extraHeaders extra headers that should be added to the request (optional)
     * @param data         the body data included in POST requests (optional)
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    public static HttpResponse httpPost(String urlString, Map<String, String> extraHeaders,
            String data, String contentType)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        if (extraHeaders == null) {
            extraHeaders = new HashMap<String, String>();
        }
        extraHeaders.put("Accept", "application/json; charset=utf-8");
        extraHeaders.put("Content-type", contentType);
        return httpRequest(HTTP_METHOD_POST, urlString, extraHeaders, null, null, data);
    }

    /**
     * Does a HTTP/HTTPS GET request
     *
     * @param urlString    the complete url string to do the request with
     * @param extraHeaders extra headers that should be added to the request (optional)
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    public static HttpResponse httpGet(String urlString, Map<String, String> extraHeaders)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        if (extraHeaders == null) {
            extraHeaders = new HashMap<String, String>();
        }
        extraHeaders.put("Accept", "application/json; charset=utf-8");
        return httpRequest(HTTP_METHOD_GET, urlString, extraHeaders, null, null, null);
    }

    /**
     * Does a HTTP/HTTPS GET request (convenience method)
     *
     * @param urlString the complete url string to do the request with
     * @return a HttpResponse containing the response (similar to a XMLHttpRequest in javascript)
     */
    public static HttpResponse httpGet(String urlString)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        return httpGet(urlString, null);
    }

    /**
     * Does a HTTP/HTTPS HEADER request (currently used to check ex.fm links)
     *
     * @param urlString the complete url string to do the request with
     * @return a String containing the response of this request
     */
    public static boolean httpHeaderRequest(String urlString) {
        URLConnection urlConnection;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = url.openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
            } else {
                throw new MalformedURLException(
                        "Connection could not be cast to HttpUrlConnection");
            }

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept-Encoding", "");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (MalformedURLException e) {
            Log.e(TAG, "httpHeaderRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "httpHeaderRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "httpHeaderRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    private static HttpsURLConnection setSSLSocketFactory(HttpsURLConnection connection)
            throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sc;
        sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new java.security.SecureRandom());
        connection.setSSLSocketFactory(sc.getSocketFactory());
        return connection;
    }

    public static String paramsListToString(Map<String, String> params) {
        Multimap<String, String> multimap = HashMultimap.create(params.size(), 1);
        for (String key : params.keySet()) {
            multimap.put(key, params.get(key));
        }
        return paramsListToString(multimap);
    }

    public static String paramsListToString(Multimap<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String key : params.keySet()) {
            if (key != null) {
                Collection<String> values = params.get(key);
                for (String value : values) {
                    if (value != null) {
                        if (first) {
                            first = false;
                        } else {
                            result.append("&");
                        }
                        try {
                            result.append(URLEncoder.encode(key, "UTF-8"));
                            result.append("=");
                            result.append(URLEncoder.encode(value, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "paramsListToString: " + e.getClass() + ": " + e
                                    .getLocalizedMessage());
                        }
                    }
                }
            }
        }

        return result.toString();
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        String text;
        InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8);
        boolean threw = true;
        try {
            text = CharStreams.toString(reader);
            threw = false;
        } finally {
            Closeables.close(reader, threw);
        }
        return text;
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link android.widget.ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in density independent pixels to scale the image down to
     */
    public static void loadImageIntoImageView(Context context, ImageView imageView, Image image,
            int width) {
        loadImageIntoImageView(context, imageView, image, width, true);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link android.widget.ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in pixels to scale the image down to
     */
    public static void loadImageIntoImageView(Context context, ImageView imageView, Image image,
            int width, boolean fit) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            RequestCreator creator = Picasso.with(context).load(
                    TomahawkUtils.preparePathForPicasso(imagePath))
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder);
            if (fit) {
                creator.resize(width, width);
            }
            creator.into(imageView);
        } else {
            RequestCreator creator = Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder);
            if (fit) {
                creator.resize(width, width);
            }
            creator.into(imageView);
        }
    }

    /**
     * Load a circle-shaped {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link android.widget.ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in pixels to scale the image down to
     */
    public static void loadRoundedImageIntoImageView(Context context, ImageView imageView,
            Image image, int width) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            Picasso.with(context).load(TomahawkUtils.preparePathForPicasso(imagePath))
                    .transform(new CircularImageTransformation())
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder)
                    .resize(width, width)
                    .into(imageView);
        } else {
            Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .transform(new CircularImageTransformation())
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder)
                    .resize(width, width)
                    .into(imageView);
        }
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link android.widget.ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param path      the path to the image
     * @param grayOut   whether or not to gray out the resolver icon
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            String path, boolean grayOut) {
        RequestCreator creator = Picasso.with(context).load(path);
        if (grayOut) {
            creator.transform(new GrayOutTransformation());
        }
        creator.error(R.drawable.no_album_art_placeholder).into(imageView);
    }

    /**
     * Load a {@link android.graphics.drawable.Drawable} asynchronously (convenience method)
     *
     * @param context       the context needed for fetching resources
     * @param imageView     the {@link android.widget.ImageView}, which will be used to show the
     *                      {@link android.graphics.drawable.Drawable}
     * @param drawableResId the resource id of the drawable to load into the imageview
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            int drawableResId) {
        loadDrawableIntoImageView(context, imageView, drawableResId, false);
    }

    /**
     * Load a {@link android.graphics.drawable.Drawable} asynchronously
     *
     * @param context       the context needed for fetching resources
     * @param imageView     the {@link android.widget.ImageView}, which will be used to show the
     *                      {@link android.graphics.drawable.Drawable}
     * @param drawableResId the resource id of the drawable to load into the imageview
     * @param grayOut       whether or not to gray out the resolver icon
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            int drawableResId, boolean grayOut) {
        RequestCreator creator = Picasso.with(context).load(drawableResId);
        if (grayOut) {
            creator.transform(new GrayOutTransformation());
        }
        creator.error(R.drawable.ic_action_error).into(imageView);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context the context needed for fetching resources
     * @param image   the path to load the image from
     * @param target  the Target which the loaded image will be pushed to
     * @param width   the width in pixels to scale the image down to
     */
    public static void loadImageIntoBitmap(Context context, Image image, Target target, int width) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            Picasso.with(context).load(TomahawkUtils.preparePathForPicasso(imagePath))
                    .resize(width, width)
                    .into(target);
        } else {
            Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .resize(width, width)
                    .into(target);
        }
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context the context needed for fetching resources
     * @param image   the path to load the image from
     * @param width   the width in pixels to scale the image down to
     */
    public static void loadImageIntoNotification(Context context, Image image,
            RemoteViews remoteViews, int viewId, int notificationId, Notification notification,
            int width) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            Picasso.with(context).load(TomahawkUtils.preparePathForPicasso(imagePath))
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder)
                    .resize(width, width)
                    .into(remoteViews, viewId, notificationId, notification);
        } else {
            Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder)
                    .resize(width, width)
                    .into(remoteViews, viewId, notificationId, notification);
        }
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context the context needed for fetching resources
     * @param resId   the resource id of the image
     */
    public static void loadDrawableIntoNotification(Context context, int resId,
            RemoteViews remoteViews, int viewId, int notificationId, Notification notification) {
        Picasso.with(context).load(resId).into(remoteViews, viewId, notificationId, notification);
    }

    public static String preparePathForPicasso(String path) {
        if (TextUtils.isEmpty(path) || path.contains("https://") || path.contains("http://")) {
            return path;
        }
        return "file:" + path;
    }

    private static String buildImagePath(Context context, Image image, int width) {
        if (image.isHatchetImage()) {
            int squareImageWidth = Math.min(image.getHeight(), image.getWidth());
            if (TomahawkMainActivity.sIsConnectedToWifi) {
                if (squareImageWidth > width) {
                    return image.getImagePath() + "?width=" + width;
                }
            } else if (squareImageWidth > width * 2 / 3) {
                return image.getImagePath() + "?width=" + width * 2 / 3;
            }
        }
        return image.getImagePath();
    }

    public static boolean containsIgnoreCase(String str1, String str2) {
        return str1.toLowerCase().contains(str2.toLowerCase());
    }

    /**
     * Set the given map of string data as the userdata of the account with the given accountname.
     *
     * @param data        A Map<String, String> which contains all key value pairs to store
     * @param accountName String containing the name of the account with which to store the given
     *                    data
     * @return true if successful, otherwise false
     */
    public static boolean setUserDataForAccount(Map<String, String> data, String accountName) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = getAccountByName(accountName);
        if (am != null && account != null) {
            for (String key : data.keySet()) {
                am.setUserData(account, key, data.get(key));
            }
            return true;
        }
        return false;
    }

    /**
     * Fill the given Map with the userdata stored with the account, which is identified by the
     * given accountname. The keys of the Map should match the keys that have been used to store the
     * userdata with the account.
     *
     * @param data        A Map<String, String> which contains all keys for which to get the
     *                    userdata
     * @param accountName String containing the name of the account from which to get the userdata
     * @return the filled map
     */
    public static Map<String, String> getUserDataForAccount(Map<String, String> data,
            String accountName) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = getAccountByName(accountName);
        if (am != null && account != null) {
            for (String key : data.keySet()) {
                data.put(key, am.getUserData(account, key));
            }
        }
        return data;
    }

    /**
     * Get the stored auth token for the account with the given accountName from the cache. Doesn't
     * refetch the authtoken if it has expired or isn't cached.
     *
     * @param accountName   String containing the name of the account from which to get the auth
     *                      token
     * @param authTokenType String containing the type of the auth token to fetch
     * @return the auth token if available, otherwise null
     */
    public static String peekAuthTokenForAccount(String accountName, String authTokenType) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = getAccountByName(accountName);
        if (am != null && account != null) {
            return am.peekAuthToken(account, authTokenType);
        }
        return null;
    }

    /**
     * Get the Account from the AccountManager that has the given account name.
     *
     * @param accountName String containing the name of the account from which to get the auth
     *                    token
     * @return the account object or null if none with the given name could be found
     */
    public static Account getAccountByName(String accountName) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            Account[] accounts = am.getAccountsByType(
                    TomahawkApp.getContext().getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (accountName.equals(am.getUserData(account,
                            AuthenticatorUtils.ACCOUNT_NAME))) {
                        return account;
                    }
                }
            }
        }
        return null;
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm". We need something
     * a little more brutual - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     * @return true iff the file and all sub files/directories have been removed
     */
    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    public static <T> ArrayList<T> constructArrayList(T... elements) {
        ArrayList<T> list = new ArrayList<T>();
        list.addAll(Arrays.asList(elements));
        return list;
    }

    public static <T> T carelessGet(List<T> list, int position) {
        if (list == null || position >= list.size()) {
            return null;
        } else {
            return list.get(position);
        }
    }

    public static <T> T carelessGet(Map<String, T> map, String key) {
        if (map == null || key == null) {
            return null;
        } else {
            return map.get(key);
        }
    }
}
