/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverConfigUiField;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigCheckbox;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigNumberEdittext;
import org.tomahawk.tomahawk_android.ui.widgets.StringView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

/**
 * A {@link android.support.v4.app.DialogFragment} which shows checkboxes and edittexts depending on
 * the given ScriptResolver's config. Enables the user to configure a certain ScriptResolver.
 */
public class ResolverConfigDialog extends ConfigDialog {

    public final static String TAG = ResolverConfigDialog.class.getSimpleName();

    public static final String PROPERTY_CHECKED = "checked";

    public static final String PROPERTY_TEXT = "text";

    public static final String PROPERTY_VALUE = "value";

    private ScriptResolver mScriptResolver;

    private final ArrayList<StringView> mStringViews = new ArrayList<>();

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
            String resolverId = getArguments().getString(
                    TomahawkFragment.PREFERENCEID);
            mScriptResolver = (ScriptResolver) PipeLine.getInstance().getResolver(resolverId);
        }

        EditText showKeyboardEditText = null;
        EditText lastEditText = null;
        if (mScriptResolver.getConfigUi() != null && mScriptResolver.getConfigUi().fields != null) {
            TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
            headerTextView.setText(mScriptResolver.getDescription());
            for (ScriptResolverConfigUiField field : mScriptResolver.getConfigUi().fields) {
                Map<String, Object> config = mScriptResolver.getConfig();
                if (PROPERTY_CHECKED.equals(field.property)) {
                    LinearLayout checkboxLayout =
                            (LinearLayout) addScrollingViewToFrame(R.layout.config_checkbox);
                    TextView textView = (TextView) checkboxLayout
                            .findViewById(R.id.config_textview);
                    textView.setText(field.name);
                    ConfigCheckbox checkBox = (ConfigCheckbox) checkboxLayout
                            .findViewById(R.id.config_checkbox);
                    checkBox.mFieldName = field.name;
                    mStringViews.add(checkBox);
                    if (config.get(field.name) != null) {
                        checkBox.setChecked((Boolean) config.get(field.name));
                    }
                } else if (PROPERTY_TEXT.equals(field.property)) {
                    ConfigEdittext editText =
                            (ConfigEdittext) addScrollingViewToFrame(R.layout.config_edittext);
                    editText.mFieldName = field.name;
                    editText.setHint(field.name);
                    mStringViews.add(editText);
                    if (config.get(field.name) != null) {
                        editText.setText((String) config.get(field.name));
                    }
                    if (TomahawkUtils.containsIgnoreCase(field.name, "password")) {
                        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setTransformationMethod(new PasswordTransformationMethod());
                    }
                    if (showKeyboardEditText == null) {
                        showKeyboardEditText = editText;
                    }
                    lastEditText = editText;
                } else if (PROPERTY_VALUE.equals(field.property)) {
                    LinearLayout numberpickerLayout =
                            (LinearLayout) addScrollingViewToFrame(R.layout.config_numberpicker);
                    TextView textView = (TextView) numberpickerLayout
                            .findViewById(R.id.config_textview);
                    textView.setText(field.name);
                    ConfigNumberEdittext editText = (ConfigNumberEdittext) numberpickerLayout
                            .findViewById(R.id.config_edittext);
                    editText.mFieldName = field.name;
                    editText.setHint(field.name);
                    mStringViews.add(editText);
                    if (config.get(field.name) != null) {
                        editText.setText(String.valueOf(config.get(field.name)));
                    }
                    if (showKeyboardEditText == null) {
                        showKeyboardEditText = editText;
                    }
                    lastEditText = editText;
                }
            }
        } else {
            hideNegativeButton();
        }
        if (lastEditText != null) {
            lastEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        }
        if (showKeyboardEditText != null) {
            showSoftKeyboard(showKeyboardEditText);
        }
        setDialogTitle(mScriptResolver.getName());
        if (!mScriptResolver.isConfigTestable()) {
            setConnectImageViewClickable();
        }

        setStatus(mScriptResolver);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    /**
     * Save the config.
     */
    public void saveConfig() {
        Map<String, Object> config = mScriptResolver.getConfig();
        for (StringView stringView : mStringViews) {
            config.put(stringView.getFieldName(), stringView.getValue());
        }
        mScriptResolver.setConfig(config);
        if (mScriptResolver.isConfigTestable()) {
            mScriptResolver.configTest();
        }
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        if (mScriptResolver.isEnabled() != checked) {
            mScriptResolver.setEnabled(checked);

            setStatus(mScriptResolver);
        }
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
        if (mScriptResolver == component && mScriptResolver.isConfigTestable()) {
            if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
                mScriptResolver.setEnabled(true);
                dismiss();
            } else {
                mScriptResolver.setEnabled(false);
            }
            stopLoadingAnimation();
        }
    }

    @Override
    protected void onPositiveAction() {
        saveConfig();
        if (mScriptResolver.isConfigTestable()) {
            startLoadingAnimation();
        } else {
            dismiss();
        }
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
