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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.ScriptResolverConfigUiField;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigCheckbox;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;
import org.tomahawk.tomahawk_android.ui.widgets.StringView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

/**
 * A {@link android.support.v4.app.DialogFragment} which shows checkboxes and edittexts depending
 * on the given ScriptResolver's config. Enables the user to configure a certain ScriptResolver.
 */
public class ResolverConfigDialog extends DialogFragment {

    public final static String TAG = ResolverConfigDialog.class.getName();

    public static final String PROPERTY_CHECKED = "checked";

    public static final String PROPERTY_TEXT = "text";

    public static final String PROPERTY_VALUE = "value";

    private ScriptResolver mScriptResolver;

    private TextView mPositiveButton;

    private TextView mNegativeButton;

    private ImageView mStatusImageView;

    private ArrayList<StringView> mStringViews = new ArrayList<StringView>();

    private View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveConfig();
            hideSoftKeyboard();
            getDialog().cancel();
        }
    };

    private View.OnClickListener mNegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideSoftKeyboard();
            getDialog().cancel();
        }
    };

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY)) {
            int resolverId = getArguments().getInt(
                    TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY);
            mScriptResolver = (ScriptResolver) PipeLine.getInstance().getResolver(resolverId);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.resolver_config_dialog, null);
        LinearLayout frame = (LinearLayout) view.findViewById(R.id.resolver_config_dialog_frame);
        if (mScriptResolver.getConfigUi() != null && mScriptResolver.getConfigUi().fields != null) {
            for (ScriptResolverConfigUiField field : mScriptResolver.getConfigUi().fields) {
                Map<String, Object> config = mScriptResolver.getConfig();
                if (PROPERTY_CHECKED.equals(field.property)) {
                    LinearLayout checkboxLayout = (LinearLayout) inflater
                            .inflate(R.layout.resolver_config_checkbox, null);
                    TextView textView = (TextView) checkboxLayout
                            .findViewById(R.id.resolver_config_textview);
                    textView.setText(field.name);
                    ConfigCheckbox checkBox = (ConfigCheckbox) checkboxLayout
                            .findViewById(R.id.resolver_config_checkbox);
                    checkBox.mFieldName = field.name;
                    mStringViews.add(checkBox);
                    if (config.get(field.name) != null) {
                        checkBox.setChecked((Boolean) config.get(field.name));
                    }
                    frame.addView(checkboxLayout);
                } else if (PROPERTY_TEXT.equals(field.property)) {
                    LinearLayout textLayout = (LinearLayout) inflater
                            .inflate(R.layout.resolver_config_text, null);
                    ConfigEdittext editText = (ConfigEdittext) textLayout
                            .findViewById(R.id.resolver_config_edittext);
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
                    frame.addView(textLayout);
                    showSoftKeyboard(textLayout);
                } else if (PROPERTY_VALUE.equals(field.property)) {
                    LinearLayout numberpickerLayout = (LinearLayout) inflater
                            .inflate(R.layout.resolver_config_numberpicker, null);
                    TextView textView = (TextView) numberpickerLayout
                            .findViewById(R.id.resolver_config_textview);
                    textView.setText(field.name);
                    ConfigEdittext editText = (ConfigEdittext) numberpickerLayout
                            .findViewById(R.id.resolver_config_edittext);
                    editText.mFieldName = field.name;
                    editText.setHint(field.name);
                    mStringViews.add(editText);
                    if (config.get(field.name) != null) {
                        editText.setText(String.valueOf(config.get(field.name)));
                    }
                    frame.addView(numberpickerLayout);
                    showSoftKeyboard(numberpickerLayout);
                }
            }
        }
        TextView textView = (TextView) view
                .findViewById(R.id.resolver_config_dialog_title_textview);
        textView.setText(mScriptResolver.getName());
        CheckBox checkBox = (CheckBox) view
                .findViewById(R.id.resolver_config_dialog_enable_checkbox);
        checkBox.setChecked(mScriptResolver.isEnabled());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScriptResolver.setEnabled(isChecked);

                if (!mScriptResolver.isEnabled()) {
                    mStatusImageView.setColorFilter(TomahawkApp.getContext().getResources()
                            .getColor(R.color.disabled_resolver), PorterDuff.Mode.MULTIPLY);
                } else {
                    mStatusImageView.clearColorFilter();
                }
            }
        });

        mPositiveButton = (TextView) view.findViewById(R.id.resolver_config_dialog_ok_button);
        mPositiveButton.setOnClickListener(mPositiveButtonListener);
        mNegativeButton = (TextView) view.findViewById(R.id.resolver_config_dialog_cancel_button);
        mNegativeButton.setOnClickListener(mNegativeButtonListener);
        mStatusImageView = (ImageView) view
                .findViewById(R.id.resolver_config_dialog_status_imageview);
        mStatusImageView.setImageDrawable(mScriptResolver.getIcon());
        if (!mScriptResolver.isEnabled()) {
            mStatusImageView.setColorFilter(TomahawkApp.getContext().getResources()
                    .getColor(R.color.disabled_resolver), PorterDuff.Mode.MULTIPLY);
        } else {
            mStatusImageView.clearColorFilter();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
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
    }

    private void showSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    /**
     * Hide the soft keyboard
     */
    private void hideSoftKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}
