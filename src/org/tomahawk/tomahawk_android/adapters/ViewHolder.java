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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ViewHolder {

    private int mViewType;

    private ImageView mRoundedImage;

    private ImageView mImageView1;

    private ImageView mImageView2;

    private ImageView mImageView3;

    private ImageView mImageView4;

    private LinearLayout mImageViewFrame;

    private CheckBox mCheckBox;

    private Spinner mSpinner;

    private TextView mTextView1;

    private TextView mTextView2;

    private TextView mTextView3;

    private TextView mTextView4;

    private TextView mTextView5;

    private TextView mTextView6;

    private LinearLayout mButton1;

    private LinearLayout mButton2;

    private LinearLayout mButton3;

    public ViewHolder(View rootView, int viewType) {
        this(null, rootView, viewType);
    }

    public ViewHolder(View listItemView, View rootView, int viewType) {
        mViewType = viewType;
        if (viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.single_line_list_textview);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_contentheader
                || viewType == R.id.tomahawklistadapter_viewtype_contentheader_user) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.content_header_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.content_header_textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.content_header_textview3);
            mTextView4 = (TextView) rootView
                    .findViewById(R.id.content_header_textview4);
            mTextView5 = (TextView) rootView
                    .findViewById(R.id.content_header_textview5);
            mTextView6 = (TextView) rootView
                    .findViewById(R.id.content_header_textview6);
            mRoundedImage = (ImageView) rootView
                    .findViewById(R.id.content_header_roundedimage);
            View view = rootView;
            if (listItemView != null) {
                view = listItemView;
            }
            mImageView1 = (ImageView) view
                    .findViewById(R.id.content_header_image);
            mImageView2 = (ImageView) view
                    .findViewById(R.id.content_header_image2);
            mImageView3 = (ImageView) view
                    .findViewById(R.id.content_header_image3);
            mImageView4 = (ImageView) view
                    .findViewById(R.id.content_header_image4);
            mImageViewFrame = (LinearLayout) view
                    .findViewById(R.id.content_header_image_frame2);
            mButton1 = (LinearLayout) rootView
                    .findViewById(R.id.content_header_button1);
            mButton2 = (LinearLayout) rootView
                    .findViewById(R.id.content_header_button2);
            mButton3 = (LinearLayout) rootView
                    .findViewById(R.id.content_header_button3);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_contentheader_user_navdrawer) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.content_header_textview_user_navdrawer);
            mRoundedImage = (ImageView) rootView
                    .findViewById(R.id.content_header_roundedimage_user_navdrawer);
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.content_header_image_user_navdrawer);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_listitem
                || viewType == R.id.tomahawklistadapter_viewtype_listitemhighlighted) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.double_line_list_imageview);
            mImageView2 = (ImageView) rootView
                    .findViewById(R.id.double_line_list_imageview2);
            mImageView3 = (ImageView) rootView
                    .findViewById(R.id.double_line_list_imageview3);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview3);
            mTextView4 = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview4);
            mTextView5 = (TextView) rootView
                    .findViewById(R.id.double_line_list_textview5);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_header) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.single_line_list_header_icon_imageview);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.single_line_list_header_textview);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_plain) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_checkbox) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mCheckBox = (CheckBox) rootView
                    .findViewById(R.id.fake_preferences_checkbox);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_spinner) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mSpinner = (Spinner) rootView
                    .findViewById(R.id.fake_preferences_spinner);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_auth) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mImageView2 = (ImageView) rootView
                    .findViewById(R.id.fake_preferences_logo);
        } else if (viewType == R.id.fakepreferencesadapter_viewtype_header) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_header_textview);
        } else if (viewType == R.id.tomahawklistadapter_viewtype_griditem) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.album_art_grid_image);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView2);
        }
    }

    public int getViewType() {
        return mViewType;
    }

    public ImageView getRoundedImage() {
        return mRoundedImage;
    }

    public ImageView getImageView1() {
        return mImageView1;
    }

    public ImageView getImageView2() {
        return mImageView2;
    }

    public ImageView getImageView3() {
        return mImageView3;
    }

    public ImageView getImageView4() {
        return mImageView4;
    }

    public LinearLayout getImageViewFrame() {
        return mImageViewFrame;
    }

    public CheckBox getCheckBox() {
        return mCheckBox;
    }

    public Spinner getSpinner() {
        return mSpinner;
    }

    public TextView getTextView1() {
        return mTextView1;
    }

    public TextView getTextView2() {
        return mTextView2;
    }

    public TextView getTextView3() {
        return mTextView3;
    }

    public TextView getTextView4() {
        return mTextView4;
    }

    public TextView getTextView5() {
        return mTextView5;
    }

    public TextView getTextView6() {
        return mTextView6;
    }

    public LinearLayout getButton1() {
        return mButton1;
    }

    public LinearLayout getButton2() {
        return mButton2;
    }

    public LinearLayout getButton3() {
        return mButton3;
    }
}
