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
package org.tomahawk.tomahawk_android.adapters;

import org.tomahawk.tomahawk_android.R;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class ViewHolder {

    private int mViewType;

    private ImageView mImageViewLeft;

    private ImageView mImageViewRight;

    private CheckBox mCheckBox;

    private Spinner mSpinner;

    private TextView mTextFirstLine;

    private TextView mTextSecondLine;

    private TextView mTextThirdLine;

    private TextView mTextFourthLine;

    private TextView mTextFifthLine;

    public ViewHolder(View rootView, int viewType) {
        mViewType = viewType;
        if (viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.single_line_list_textview);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
            mImageViewLeft = (ImageView) rootView
                    .findViewById(R.id.double_line_list_imageview);
            mImageViewRight = (ImageView) rootView
                    .findViewById(R.id.double_line_list_imageview2);
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview2);
            mTextThirdLine = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview3);
            mTextFourthLine = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview4);
            mTextFifthLine = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview5);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_header) {
            mImageViewLeft = (ImageView) rootView
                    .findViewById(R.id.single_line_list_header_icon_imageview);
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.single_line_list_header_textview);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_plain) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
        } else if (viewType == R.id.fake_preferences_checkbox) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mCheckBox = (CheckBox) rootView
                    .findViewById(R.id.fake_preferences_checkbox);
        } else if (viewType == R.id.fake_preferences_spinner) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mSpinner = (Spinner) rootView
                    .findViewById(R.id.fake_preferences_spinner);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_auth) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mImageViewRight = (ImageView) rootView
                    .findViewById(R.id.fake_preferences_logo);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_header) {
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.fake_preferences_header_textview);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_griditem) {
            mImageViewLeft = (ImageView) rootView
                    .findViewById(R.id.album_art_grid_image);
            mTextFirstLine = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView);
            mTextSecondLine = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView2);
        }
    }

    public int getViewType() {
        return mViewType;
    }

    public ImageView getImageViewLeft() {
        return mImageViewLeft;
    }

    public ImageView getImageViewRight() {
        return mImageViewRight;
    }

    public CheckBox getCheckBox() {
        return mCheckBox;
    }

    public Spinner getSpinner() {
        return mSpinner;
    }

    public TextView getTextFirstLine() {
        return mTextFirstLine;
    }

    public TextView getTextSecondLine() {
        return mTextSecondLine;
    }

    public TextView getTextThirdLine() {
        return mTextThirdLine;
    }

    public TextView getTextFourthLine() {
        return mTextFourthLine;
    }

    public TextView getTextFifthLine() {
        return mTextFifthLine;
    }
}
