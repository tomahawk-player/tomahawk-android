package org.tomahawk.libtomahawk.utils;

import com.google.common.collect.Multimap;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.CircularImageTransformation;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class TomahawkUtils {

    public static String TAG = TomahawkUtils.class.getName();

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
     * @param dp      A value in dp(Device independent pixels) unit. Which we need to convert into
     *                pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent Pixels equivalent to dp according to device
     */
    public static int convertDpToPixel(int dp, Context context) {
        Resources resources = context.getResources();
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
            boolean isFullTextQuery = query.isFullTextQuery();
            if (isFullTextQuery) {
                return getCacheKey(query.getFullTextQuery(), String.valueOf(query.isOnlyLocal()));
            } else {
                return getCacheKey(query.getName(), query.getAlbum().getName(),
                        query.getArtist().getName(), query.getResultHint());
            }
        }
        return "";
    }

    public static String getCacheKey(Image image) {
        return getCacheKey(image.getImagePath());
    }

    public static String getCacheKey(Result result) {
        return getCacheKey(result.getPath());
    }

    public static String httpsPost(String urlString, Multimap<String, String> params)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        return httpsPost(urlString, params, false, false);
    }

    public static String httpsPost(String urlString, Multimap<String, String> params,
            String jsonString)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        String output = null;
        URLConnection urlConnection;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = url.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
            } else {
                throw new MalformedURLException(
                        "Connection could not be cast to HttpUrlConnection");
            }

            connection = setSSLSocketFactory(connection);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-type", "application/json; charset=utf-8");
            for (String key : params.keySet()) {
                for (String value : params.get(key)) {
                    connection.setRequestProperty(key, value);
                }
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(jsonString.getBytes().length);
            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(connection.getOutputStream());
                out.write(jsonString);
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            if (connection.getResponseCode() / 100 != 2) {
                throw new IOException("HttpsURLConnection (url:'" + urlString
                        + "') didn't return with status code 2xx, instead it returned " + connection
                        .getResponseCode());
            }
            output = inputStreamToString(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return output;
    }

    public static String httpsPost(String urlString, Multimap<String, String> params,
            boolean contentTypeIsJson, boolean paramsInHeader)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        String output = null;
        URLConnection urlConnection;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = url.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
            } else {
                throw new MalformedURLException(
                        "Connection could not be cast to HttpUrlConnection");
            }

            connection = setSSLSocketFactory(connection);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            if (contentTypeIsJson) {
                connection.setRequestProperty("Content-type", "application/json; charset=utf-8");
            } else if (!paramsInHeader) {
                connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Accept", "application/json; charset=utf-8");
                connection.setDoOutput(true);
            }
            if (paramsInHeader) {
                for (String key : params.keySet()) {
                    for (String value : params.get(key)) {
                        connection.setRequestProperty(key, value);
                    }
                }
            } else {
                connection.setRequestMethod("POST");
                String paramsString = paramsListToString(params);
                connection.setFixedLengthStreamingMode(paramsString.getBytes().length);
                OutputStreamWriter out = null;
                try {
                    out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(paramsString);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }

            if (connection.getResponseCode() / 100 != 2) {
                throw new IOException("HttpsURLConnection (url:'" + urlString
                        + "') didn't return with status code 2xx, instead it returned " + connection
                        .getResponseCode());
            }
            output = inputStreamToString(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return output;
    }

    public static String httpsGet(String urlString)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        String output = null;
        URLConnection urlConnection;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = url.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
            } else {
                throw new MalformedURLException(
                        "Connection could not be cast to HttpUrlConnection");
            }

            connection = setSSLSocketFactory(connection);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setRequestProperty("Accept", "application/json; charset=utf-8");
            connection.setRequestProperty("Content-type", "application/json; charset=utf-8");

            if (connection.getResponseCode() / 100 != 2) {
                throw new IOException("HttpsURLConnection (url:'" + urlString
                        + "') didn't return with status code 2xx, instead it returned " + connection
                        .getResponseCode());
            }
            output = inputStreamToString(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return output;
    }

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

    public static String paramsListToString(Multimap<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String key : params.keySet()) {
            Collection<String> values = params.get(key);
            for (String value : values) {
                if (first) {
                    first = false;
                } else {
                    result.append("&");
                }
                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value, "UTF-8"));
            }
        }

        return result.toString();
    }

    private static String inputStreamToString(HttpURLConnection connection) throws IOException {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (FileNotFoundException e) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        }
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
     * @param width     the width in density independent pixels to scale the image down to
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
                creator.fit();
            }
            creator.into(imageView);
        } else {
            RequestCreator creator = Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder);
            if (fit) {
                creator.fit();
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
     * @param width     the width in density independent pixels to scale the image down to
     */
    public static void loadRoundedImageIntoImageView(Context context, ImageView imageView,
            Image image, int width) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            Picasso.with(context).load(TomahawkUtils.preparePathForPicasso(imagePath))
                    .transform(new CircularImageTransformation())
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder).fit().into(imageView);
        } else {
            Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .transform(new CircularImageTransformation())
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder).fit().into(imageView);
        }
    }

    /**
     * Load a {@link android.graphics.drawable.Drawable} asynchronously
     *
     * @param context       the context needed for fetching resources
     * @param imageView     the {@link android.widget.ImageView}, which will be used to show the
     *                      {@link android.graphics.drawable.Drawable}
     * @param drawableResId the resource id of the drawable to load into the imageview
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            int drawableResId) {
        Picasso.with(context).load(drawableResId).error(R.drawable.ic_action_error).into(imageView);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context the context needed for fetching resources
     * @param image   the path to load the image from
     * @param target  the Target which the loaded image will be pushed to
     * @param width   the width in density independent pixels to scale the image down to
     */
    public static void loadImageIntoBitmap(Context context, Image image, Target target, int width) {
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(context, image, width);
            Picasso.with(context).load(TomahawkUtils.preparePathForPicasso(imagePath))
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder).into(target);
        } else {
            Picasso.with(context).load(R.drawable.no_album_art_placeholder)
                    .placeholder(R.drawable.no_album_art_placeholder)
                    .error(R.drawable.no_album_art_placeholder).into(target);
        }
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
            width = convertDpToPixel(width, context);
            if (TomahawkMainActivity.sIsConnectedToWifi) {
                if (squareImageWidth > width) {
                    return image.getImagePath() + "?width=" + width;
                }
            } else if (squareImageWidth > width / 2) {
                return image.getImagePath() + "?width=" + width / 2;
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
     * @param context     Context needed to get the AccountManager
     * @param data        A Map<String, String> which contains all key value pairs to store
     * @param accountName String containing the name of the account with which to store the given
     *                    data
     * @return true if successful, otherwise false
     */
    public static boolean setUserDataForAccount(Context context, Map<String, String> data,
            String accountName) {
        final AccountManager am = AccountManager.get(context);
        Account account = getAccountByName(context, accountName);
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
     * @param context     Context needed to get the AccountManager
     * @param data        A Map<String, String> which contains all keys for which to get the
     *                    userdata
     * @param accountName String containing the name of the account from which to get the userdata
     * @return the filled map
     */
    public static Map<String, String> getUserDataForAccount(Context context,
            Map<String, String> data, String accountName) {
        final AccountManager am = AccountManager.get(context);
        Account account = getAccountByName(context, accountName);
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
     * @param context       Context needed to get the AccountManager
     * @param accountName   String containing the name of the account from which to get the auth
     *                      token
     * @param authTokenType String containing the type of the auth token to fetch
     * @return the auth token if available, otherwise null
     */
    public static String peekAuthTokenForAccount(Context context, String accountName,
            String authTokenType) {
        final AccountManager am = AccountManager.get(context);
        Account account = getAccountByName(context, accountName);
        if (am != null && account != null) {
            return am.peekAuthToken(account, authTokenType);
        }
        return null;
    }

    /**
     * Get the Account from the AccountManager that has the given account name.
     *
     * @param context     Context needed to get the AccountManager
     * @param accountName String containing the name of the account from which to get the auth
     *                    token
     * @return the account object or null if none with the given name could be found
     */
    public static Account getAccountByName(Context context, String accountName) {
        final AccountManager am = AccountManager.get(context);
        if (am != null) {
            Account[] accounts = am
                    .getAccountsByType(context.getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (accountName.equals(am.getUserData(account,
                            AuthenticatorUtils.AUTHENTICATOR_NAME))) {
                        return account;
                    }
                }
            }
        }
        return null;
    }
}
