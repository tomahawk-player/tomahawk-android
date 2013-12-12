package org.tomahawk.libtomahawk.utils;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolution;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
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

    /**
     * Return the {@link android.content.Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls     the class which contains the activity to launch
     * @return the created intent
     */
    public static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private static String getCacheKey(String... strings) {
        String result = "";
        for (String s : strings) {
            result += "\t\t" + s.toLowerCase();
        }
        return result;
    }

    public static String getCacheKey(TomahawkBaseAdapter.TomahawkListItem tomahawkListItem) {
        if (tomahawkListItem instanceof Artist) {
            return getCacheKey(tomahawkListItem.getName());
        } else if (tomahawkListItem instanceof Album) {
            return getCacheKey(tomahawkListItem.getName(), tomahawkListItem.getArtist().getName());
        } else if (tomahawkListItem instanceof Track || tomahawkListItem instanceof Query) {
            return getCacheKey(tomahawkListItem.getName(), tomahawkListItem.getAlbum().getName(),
                    tomahawkListItem.getArtist().getName());
        }
        return "";
    }

    public static String getCacheKey(String qid, Resolver resolver) {
        return getCacheKey(qid, String.valueOf(resolver.getId()));
    }

    public static String getCacheKey(Resolution resolution) {
        return getCacheKey(resolution.getQuery().getQid(), resolution.getResolver());
    }
}
