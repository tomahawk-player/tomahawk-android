/*
 * Copyright (c) 2015 Fran Montiel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomahawk.libtomahawk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistentCookieStore implements CookieStore {

    private static final String TAG = PersistentCookieStore.class
            .getSimpleName();

    // Persistence
    private static final String SP_COOKIE_STORE_SUFFIX = "_cookieStore";

    private static final String SP_KEY_DELIMITER = "♠"; // Unusual char in URL

    private SharedPreferences sharedPreferences;

    // In memory
    private Map<URI, Set<HttpCookie>> allCookies;

    public PersistentCookieStore(Context context, String cookieContextId) {
        sharedPreferences = context.getSharedPreferences(cookieContextId + SP_COOKIE_STORE_SUFFIX,
                Context.MODE_PRIVATE);
        loadAllFromPersistence();
    }

    private void loadAllFromPersistence() {
        allCookies = new HashMap<>();

        Map<String, ?> allPairs = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allPairs.entrySet()) {
            String[] uriAndName = entry.getKey().split(SP_KEY_DELIMITER, 2);
            try {
                URI uri = new URI(uriAndName[0]);
                String encodedCookie = (String) entry.getValue();
                HttpCookie cookie = new SerializableHttpCookie().decode(encodedCookie);

                if (cookie != null) {
                    Set<HttpCookie> targetCookies = allCookies.get(uri);
                    if (targetCookies == null) {
                        targetCookies = new HashSet<>();
                        allCookies.put(uri, targetCookies);
                    }
                    // Repeated cookies cannot exist in persistence
                    // targetCookies.remove(cookie)
                    targetCookies.add(cookie);
                }
            } catch (URISyntaxException e) {
                Log.w(TAG, e);
            }
        }
    }

    @Override
    public synchronized void add(URI uri, HttpCookie cookie) {
        uri = cookieUri(uri, cookie);

        Set<HttpCookie> targetCookies = allCookies.get(uri);
        if (targetCookies == null) {
            targetCookies = new HashSet<>();
            allCookies.put(uri, targetCookies);
        }
        targetCookies.remove(cookie);
        targetCookies.add(cookie);

        saveToPersistence(uri, cookie);
    }

    /**
     * Get the real URI from the cookie "domain" and "path" attributes, if they are not set then
     * uses the URI provided (coming from the response)
     */
    private static URI cookieUri(URI uri, HttpCookie cookie) {
        URI cookieUri = uri;
        if (cookie.getDomain() != null) {
            // Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
            String domain = cookie.getDomain();
            if (domain.charAt(0) == '.') {
                domain = domain.substring(1);
            }
            try {
                cookieUri = new URI(uri.getScheme() == null ? "http" : uri.getScheme(), domain,
                        cookie.getPath() == null ? "/" : cookie.getPath(), null);
            } catch (URISyntaxException e) {
                Log.w(TAG, e);
            }
        }
        return cookieUri;
    }

    private void saveToPersistence(URI uri, HttpCookie cookie) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(uri.toString() + SP_KEY_DELIMITER + cookie.getName(),
                new SerializableHttpCookie().encode(cookie));

        editor.apply();
    }

    @Override
    public synchronized List<HttpCookie> get(URI uri) {
        return getValidCookies(uri);
    }

    @Override
    public synchronized List<HttpCookie> getCookies() {
        List<HttpCookie> allValidCookies = new ArrayList<>();
        for (URI storedUri : allCookies.keySet()) {
            allValidCookies.addAll(getValidCookies(storedUri));
        }

        return allValidCookies;
    }

    private List<HttpCookie> getValidCookies(URI uri) {
        List<HttpCookie> targetCookies = new ArrayList<>();
        // If the stored URI does not have a path then it must match any URI in
        // the same domain
        for (URI storedUri : allCookies.keySet()) {
            // Check ith the domains match according to RFC 6265
            if (checkDomainsMatch(storedUri.getHost(), uri.getHost())) {
                // Check if the paths match according to RFC 6265
                if (checkPathsMatch(storedUri.getPath(), uri.getPath())) {
                    targetCookies.addAll(allCookies.get(storedUri));
                }
            }
        }

        // Check it there are expired cookies and remove them
        if (!targetCookies.isEmpty()) {
            List<HttpCookie> cookiesToRemoveFromPersistence = new ArrayList<>();
            for (Iterator<HttpCookie> it = targetCookies.iterator(); it.hasNext(); ) {
                HttpCookie currentCookie = it.next();
                if (currentCookie.hasExpired()) {
                    cookiesToRemoveFromPersistence.add(currentCookie);
                    it.remove();
                }
            }

            if (!cookiesToRemoveFromPersistence.isEmpty()) {
                removeFromPersistence(uri, cookiesToRemoveFromPersistence);
            }
        }
        return targetCookies;
    }

   /* http://tools.ietf.org/html/rfc6265#section-5.1.3

    A string domain-matches a given domain string if at least one of the
    following conditions hold:

    o  The domain string and the string are identical.  (Note that both
    the domain string and the string will have been canonicalized to
    lower case at this point.)

    o  All of the following conditions hold:

        *  The domain string is a suffix of the string.

        *  The last character of the string that is not included in the
           domain string is a %x2E (".") character.

        *  The string is a host name (i.e., not an IP address). */

    private boolean checkDomainsMatch(String cookieHost, String requestHost) {
        return requestHost.equals(cookieHost) || requestHost.endsWith("." + cookieHost);
    }

    /*  http://tools.ietf.org/html/rfc6265#section-5.1.4

        A request-path path-matches a given cookie-path if at least one of
        the following conditions holds:

        o  The cookie-path and the request-path are identical.

        o  The cookie-path is a prefix of the request-path, and the last
        character of the cookie-path is %x2F ("/").

        o  The cookie-path is a prefix of the request-path, and the first
        character of the request-path that is not included in the cookie-
        path is a %x2F ("/") character. */

    private boolean checkPathsMatch(String cookiePath, String requestPath) {
        return requestPath.equals(cookiePath) ||
                (requestPath.startsWith(cookiePath)
                        && cookiePath.charAt(cookiePath.length() - 1) == '/') ||
                (requestPath.startsWith(cookiePath)
                        && requestPath.substring(cookiePath.length()).charAt(0) == '/');
    }

    private void removeFromPersistence(URI uri, List<HttpCookie> cookiesToRemove) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (HttpCookie cookieToRemove : cookiesToRemove) {
            editor.remove(uri.toString() + SP_KEY_DELIMITER + cookieToRemove.getName());
        }
        editor.apply();
    }

    @Override
    public synchronized List<URI> getURIs() {
        return new ArrayList<>(allCookies.keySet());
    }

    @Override
    public synchronized boolean remove(URI uri, HttpCookie cookie) {
        Set<HttpCookie> targetCookies = allCookies.get(uri);
        boolean cookieRemoved = targetCookies != null && targetCookies.remove(cookie);
        if (cookieRemoved) {
            removeFromPersistence(uri, cookie);
        }
        return cookieRemoved;
    }

    private void removeFromPersistence(URI uri, HttpCookie cookieToRemove) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(uri.toString() + SP_KEY_DELIMITER + cookieToRemove.getName());
        editor.apply();
    }

    @Override
    public synchronized boolean removeAll() {
        allCookies.clear();
        removeAllFromPersistence();
        return true;
    }

    private void removeAllFromPersistence() {
        sharedPreferences.edit().clear().apply();
    }

}