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
package org.tomahawk.libtomahawk.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.util.Log;

import javax.net.ssl.HostnameVerifier;

public class TomahawkHttpClient extends DefaultHttpClient {

    private static final String TAG = TomahawkHttpClient.class.getName();

    public TomahawkHttpClient(HttpParams params) {
        super(params);
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", getSslSocketFactory(), 443));
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        return new ThreadSafeClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory getSslSocketFactory() {
        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            InputStream in = TomahawkApp.getContext().getResources()
                    .openRawResource(R.raw.tomahawk);
            try {
                trusted.load(in, "tomahawk".toCharArray());
            } finally {
                in.close();
            }
            HostnameVerifier hostnameVerifier
                    = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
            sslSocketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
            return sslSocketFactory;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getSslSocketFactory: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (KeyStoreException e) {
            Log.e(TAG, "getSslSocketFactory: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (CertificateException e) {
            Log.e(TAG, "getSslSocketFactory: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "getSslSocketFactory: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return null;
    }
}
