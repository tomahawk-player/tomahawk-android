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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.network.TomahawkServerConnection;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class TomahawkAccountAuthenticatorActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";
    private static final String TAG = "AuthenticatorActivity";

    private AccountManager mAccountManager;
    private UserLoginTask mAuthTask = null;
    private ProgressDialog mProgressDialog = null;

    /**
     * Asynchronous task used to create a new Tomahawk account.
     */
    public class UserLoginTask extends AsyncTask<Pair<String, String>, Void, String> {

        @Override
        protected String doInBackground(Pair<String, String>... params) {

            if (params.length < 1)
                return null;

            String userid = params[0].first;
            String passwd = params[0].second;
            try {
                return TomahawkServerConnection.authenticate(userid, passwd);
            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String authToken) {
            onAuthenticationResult(authToken);
        }

        @Override
        protected void onCancelled() {
            onAuthenticationCancel();
        }
    }

    /**
     * Called when this Activity is created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);

        mAccountManager = AccountManager.get(this);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);

        /** REMOVE THESE AFTER TESTING. */
        EditText user = (EditText) findViewById(R.id.username_edit);
        user.setText("creichert");

        EditText passwd = (EditText) findViewById(R.id.password_edit);
        passwd.setText("somethingorother");
    }

    /**
     * Called when the progress dialog is created.
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getResources().getString(R.string.authenticationdialog_text_string));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "user cancelling authentication");
                if (mAuthTask != null) {
                    mAuthTask.cancel(true);
                }
            }
        });

        return mProgressDialog;
    }

    /**
     * Handle a login request (when the login button is pressed).
     * 
     * @param view
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public void handleLogin(View view) {
        showDialog(0);

        EditText user = (EditText) findViewById(R.id.username_edit);
        EditText passwd = (EditText) findViewById(R.id.password_edit);

        mAuthTask = new UserLoginTask();
        mAuthTask.execute(new Pair<String, String>(user.getText().toString(), passwd.getText().toString()));
    }

    /**
     * Called when the authentication is finished.
     * 
     * @param authToken
     */
    public void onAuthenticationResult(String authToken) {

        mAuthTask = null;
        hideProgressDialog();

        if (authToken == null)
            return;

        EditText user = (EditText) findViewById(R.id.username_edit);
        EditText passwd = (EditText) findViewById(R.id.password_edit);

        final Account account = new Account(user.getText().toString(), TomahawkServerConnection.ACCOUNT_TYPE);

        final Bundle result = new Bundle();
        mAccountManager.addAccountExplicitly(account, passwd.getText().toString(), result);
        mAccountManager.setAuthToken(account, TomahawkServerConnection.AUTH_TOKEN_TYPE, authToken);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, TomahawkServerConnection.ACCOUNT_NAME);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getResources().getString(R.string.accounttype_string));
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Called when the authentication has been cancelled.
     */
    public void onAuthenticationCancel() {
        mAuthTask = null;
        hideProgressDialog();
    }

    /**
     * Hides the progress dlg.
     */
    private void hideProgressDialog() {
        if (mProgressDialog == null)
            return;

        mProgressDialog.dismiss();
        mProgressDialog = null;
    }
}