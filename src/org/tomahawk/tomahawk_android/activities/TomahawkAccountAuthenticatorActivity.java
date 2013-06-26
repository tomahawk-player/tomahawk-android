///* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
// *
// *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
// *
// *   Tomahawk is free software: you can redistribute it and/or modify
// *   it under the terms of the GNU General Public License as published by
// *   the Free Software Foundation, either version 3 of the License, or
// *   (at your option) any later version.
// *
// *   Tomahawk is distributed in the hope that it will be useful,
// *   but WITHOUT ANY WARRANTY; without even the implied warranty of
// *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *   GNU General Public License for more details.
// *
// *   You should have received a copy of the GNU General Public License
// *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
// */
//package org.tomahawk.tomahawk_android.activities;
//
//import org.tomahawk.tomahawk_android.R;
//import org.tomahawk.tomahawk_android.TomahawkApp;
//import org.tomahawk.tomahawk_android.services.TomahawkService;
//import org.tomahawk.tomahawk_android.utils.TomahawkPreferences;
//
//import android.accounts.Account;
//import android.accounts.AccountAuthenticatorActivity;
//import android.accounts.AccountManager;
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.ProgressDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.util.Log;
//import android.util.Pair;
//import android.view.View;
//import android.view.Window;
//import android.widget.EditText;
//
//public class TomahawkAccountAuthenticatorActivity extends AccountAuthenticatorActivity {
//
//    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";
//
//    private static final String TAG = "AuthenticatorActivity";
//
//    /**
//     * Error flag when the submitted user credentials are invalid.
//     */
//    private static final int ERROR_INVALID_CREDENTIALS = 0;
//
//    private AccountManager mAccountManager;
//
//    private LoginTask mAuthTask = null;
//
//    private ProgressDialog mProgressDialog = null;
//
//    /**
//     * Asynchronous task used to create a new Tomahawk account.
//     */
//    public class LoginTask extends AsyncTask<Pair<String, String>, Void, String> {
//
//        private boolean mErrorOccurred = false;
//
//        @Override
//        protected String doInBackground(Pair<String, String>... params) {
//
//            if (params.length < 1) {
//                return null;
//            }
//
//            mErrorOccurred = false;
//            String userid = params[0].first;
//            String passwd = params[0].second;
//            try {
//                return TomahawkService.authenticate(userid, passwd);
//            } catch (IllegalArgumentException e) {
//                Log.e(TAG, e.toString());
//                mErrorOccurred = true;
//                return e.getMessage();
//            }
//        }
//
//        @Override
//        protected void onPostExecute(final String response) {
//
//            if (!mErrorOccurred) {
//                onAuthenticationResult(response);
//            } else {
//                // Check the error code here when api supports it.
//                if (response.equals("Invalid credentials")) {
//                    onAuthenticationError(ERROR_INVALID_CREDENTIALS);
//                }
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            onAuthenticationCancel();
//        }
//    }
//
//    /**
//     * Called when this Activity is created.
//     */
//    @Override
//    public void onCreate(Bundle icicle) {
//        super.onCreate(icicle);
//        requestWindowFeature(Window.FEATURE_LEFT_ICON);
//        setContentView(R.layout.login_activity);
//
//        mAccountManager = AccountManager.get(this);
//        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
//                android.R.drawable.ic_dialog_alert);
//
//        EditText user = (EditText) findViewById(R.id.username_edit);
//        user.setText(TomahawkPreferences.getUsername());
//
//        EditText passwd = (EditText) findViewById(R.id.password_edit);
//        passwd.setText(TomahawkPreferences.getPassword());
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        /** Setup account. */
//        AccountManager accountManager = AccountManager.get(this);
//        Account[] accounts = accountManager.getAccountsByType(TomahawkService.ACCOUNT_TYPE);
//
//        if (accounts.length <= 0) {
//            return;
//        }
//
//        /**
//         * 'Getting' the auth token here is asynchronous. When the
//         * AccountManager has the auth token the TomahawkMainActivity.run is
//         * called and starts the TomahawkServerConnection.
//         */
//        // if (TomahawkPreferences.goOnline())
//        accountManager.getAuthToken(accounts[0], TomahawkService.AUTH_TOKEN_TYPE, null,
//                new TomahawkAccountAuthenticatorActivity(), (TomahawkApp) getApplication(), null);
//    }
//
//    /**
//     * Called when the progress dialog is created.
//     */
//    @Override
//    protected Dialog onCreateDialog(int id, Bundle args) {
//
//        mProgressDialog = new ProgressDialog(this);
//        mProgressDialog
//                .setMessage(getResources().getString(R.string.authenticationdialog_text_string));
//        mProgressDialog.setIndeterminate(true);
//        mProgressDialog.setCancelable(true);
//
//        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            public void onCancel(DialogInterface dialog) {
//                Log.i(TAG, "user cancelling authentication");
//                if (mAuthTask != null) {
//                    mAuthTask.cancel(true);
//                }
//            }
//        });
//
//        return mProgressDialog;
//    }
//
//    /**
//     * Handle a loginSpotify request (when the loginSpotify button is pressed).
//     */
//    @SuppressWarnings({"unchecked", "deprecation"})
//    public void handleLogin(View view) {
//        showDialog(0);
//
//        EditText user = (EditText) findViewById(R.id.username_edit);
//        EditText passwd = (EditText) findViewById(R.id.password_edit);
//
//        mAuthTask = new LoginTask();
//        mAuthTask.execute(
//                new Pair<String, String>(user.getText().toString(), passwd.getText().toString()));
//    }
//
//    /**
//     * Called when the authentication is finished.
//     */
//    public void onAuthenticationResult(String authToken) {
//
//        mAuthTask = null;
//        hideProgressDialog();
//
//        if (authToken == null) {
//            return;
//        }
//
//        EditText user = (EditText) findViewById(R.id.username_edit);
//        EditText passwd = (EditText) findViewById(R.id.password_edit);
//
//        final Account account = new Account(user.getText().toString(),
//                TomahawkService.ACCOUNT_TYPE);
//
//        final Bundle result = new Bundle();
//        mAccountManager.addAccountExplicitly(account, passwd.getText().toString(), result);
//        mAccountManager.setAuthToken(account, TomahawkService.AUTH_TOKEN_TYPE, authToken);
//
//        TomahawkPreferences
//                .setUsernamePassword(user.getText().toString(), passwd.getText().toString());
//
//        final Intent intent = new Intent();
//        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, TomahawkService.HATCHET_ACCOUNT_NAME);
//        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
//                getResources().getString(R.string.accounttype_string));
//        setAccountAuthenticatorResult(intent.getExtras());
//        setResult(RESULT_OK, intent);
//        finish();
//    }
//
//    /**
//     * Called when an error ocurred during authentication.
//     */
//    public void onAuthenticationError(final int errorcode) {
//
//        mAuthTask = null;
//        hideProgressDialog();
//
//        String errormsg = null;
//        switch (errorcode) {
//
//            case ERROR_INVALID_CREDENTIALS:
//                errormsg = getResources()
//                        .getString(R.string.authenticationactivity_invalid_credentials_string);
//                break;
//        }
//
//        if (errormsg == null) {
//            return;
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(
//                TomahawkAccountAuthenticatorActivity.this);
//        builder.setMessage(errormsg).setCancelable(false)
//                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//                });
//
//        AlertDialog alert = builder.create();
//        alert.show();
//    }
//
//    /**
//     * Called when the authentication has been cancelled.
//     */
//    public void onAuthenticationCancel() {
//        mAuthTask = null;
//        hideProgressDialog();
//    }
//
//    /**
//     * Hides the progress dlg.
//     */
//    private void hideProgressDialog() {
//        if (mProgressDialog == null) {
//            return;
//        }
//
//        mProgressDialog.dismiss();
//        mProgressDialog = null;
//    }
//}
