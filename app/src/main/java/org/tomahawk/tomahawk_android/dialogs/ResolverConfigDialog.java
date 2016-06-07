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
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigCheckbox;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigDropDown;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigFieldView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link android.support.v4.app.DialogFragment} which shows checkboxes and edittexts depending on
 * the given ScriptResolver's config. Enables the user to configure a certain ScriptResolver.
 */
public class ResolverConfigDialog extends ConfigDialog {

    public final static String TAG = ResolverConfigDialog.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    private final ArrayList<ConfigFieldView> mConfigFieldViews = new ArrayList<>();

    private View.OnClickListener mEnableButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mScriptResolver.isEnabled()) {
                saveConfig();
            } else {
                mScriptResolver.setEnabled(false);
            }
            onResolverStateUpdated(mScriptResolver);
        }
    };

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
            String resolverId = getArguments().getString(TomahawkFragment.PREFERENCEID);
            mScriptResolver = PipeLine.get().getResolver(resolverId);
        }

        EditText showKeyboardEditText = null;
        EditText lastEditText = null;
        if (mScriptResolver.getConfigUi() != null) {
            TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
            headerTextView.setText(mScriptResolver.getDescription());
            for (ScriptResolverConfigUiField field : mScriptResolver.getConfigUi()) {
                Map<String, Object> config = mScriptResolver.getConfig();
                if (ScriptResolverConfigUiField.TYPE_TEXTVIEW.equals(field.type)) {
                    TextView textView =
                            (TextView) addScrollingViewToFrame(R.layout.config_textview);
                    if (field.text.startsWith("<html>")) {
                        textView.setText(Html.fromHtml(field.text));
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        textView.setText(field.text);
                    }
                } else if (ScriptResolverConfigUiField.TYPE_CHECKBOX.equals(field.type)) {
                    LinearLayout checkboxLayout =
                            (LinearLayout) addScrollingViewToFrame(R.layout.config_checkbox);
                    TextView textView =
                            (TextView) checkboxLayout.findViewById(R.id.config_textview);
                    textView.setText(field.label);
                    ConfigCheckbox checkBox =
                            (ConfigCheckbox) checkboxLayout.findViewById(R.id.config_checkbox);
                    checkBox.mConfigFieldId = field.id;
                    mConfigFieldViews.add(checkBox);
                    if (config.get(field.id) != null) {
                        checkBox.setChecked((Boolean) config.get(field.id));
                    } else {
                        checkBox.setChecked(Boolean.valueOf(field.defaultValue));
                    }
                } else if (ScriptResolverConfigUiField.TYPE_TEXTFIELD.equals(field.type)) {
                    ConfigEdittext editText =
                            (ConfigEdittext) addScrollingViewToFrame(R.layout.config_edittext);
                    editText.mConfigFieldId = field.id;
                    editText.setHint(field.label);
                    mConfigFieldViews.add(editText);
                    if (config.get(field.id) != null) {
                        editText.setText((String) config.get(field.id));
                    } else {
                        editText.setText(field.defaultValue);
                    }
                    if (field.isPassword) {
                        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setTransformationMethod(new PasswordTransformationMethod());
                    }
                    if (showKeyboardEditText == null) {
                        showKeyboardEditText = editText;
                    }
                    lastEditText = editText;
                } else if (ScriptResolverConfigUiField.TYPE_DROPDOWN.equals(field.type)) {
                    LinearLayout numberpickerLayout =
                            (LinearLayout) addScrollingViewToFrame(R.layout.config_dropdown);
                    TextView textView =
                            (TextView) numberpickerLayout.findViewById(R.id.config_textview);
                    textView.setText(field.label);
                    ConfigDropDown dropDown =
                            (ConfigDropDown) numberpickerLayout.findViewById(R.id.config_dropdown);
                    dropDown.mConfigFieldId = field.id;
                    mConfigFieldViews.add(dropDown);
                    List<CharSequence> list = new ArrayList<>();
                    for (String item : field.items) {
                        list.add(item);
                    }
                    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                            TomahawkApp.getContext(), R.layout.spinner_textview, list);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_textview);
                    dropDown.setAdapter(adapter);
                    if (config.get(field.id) != null) {
                        dropDown.setSelection(((Double) config.get(field.id)).intValue());
                    } else {
                        dropDown.setSelection(Integer.valueOf(field.defaultValue));
                    }
                }
            }
        }
        if (mScriptResolver.getScriptAccount().isManuallyInstalled()) {
            showRemoveButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RemovePluginConfigDialog dialog = new RemovePluginConfigDialog();
                    Bundle args = new Bundle();
                    args.putString(TomahawkFragment.PREFERENCEID, mScriptResolver.getId());
                    dialog.setArguments(args);
                    dialog.show(getFragmentManager(), null);
                    dismiss();
                }
            });
        }
        if (lastEditText != null) {
            lastEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        }
        if (showKeyboardEditText != null) {
            ViewUtils.showSoftKeyboard(showKeyboardEditText);
        }
        setDialogTitle(mScriptResolver.getName());

        showEnableButton(mEnableButtonListener);
        onResolverStateUpdated(mScriptResolver);

        hideNegativeButton();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    /**
     * Save the config.
     */
    public void saveConfig() {
        Map<String, Object> config = mScriptResolver.getConfig();
        for (ConfigFieldView configFieldView : mConfigFieldViews) {
            config.put(configFieldView.getConfigFieldId(), configFieldView.getValue());
        }
        mScriptResolver.setConfig(config);
        mScriptResolver.testConfig(config);
        startLoadingAnimation();
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
        if (mScriptResolver == component) {
            mScriptResolver.setEnabled(
                    type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
            onResolverStateUpdated(mScriptResolver);
            stopLoadingAnimation();
        }
    }

    @Override
    protected void onPositiveAction() {
        dismiss();
    }
}
