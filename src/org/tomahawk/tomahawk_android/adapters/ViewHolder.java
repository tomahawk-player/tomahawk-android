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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class ViewHolder {

    private int mLayoutId;

    private ImageView mRoundedImage;

    private ImageView mImageView1;

    private ImageView mImageView2;

    private ImageView mImageView3;

    private ImageView mImageView4;

    private CheckBox mCheckBox;

    private Spinner mSpinner;

    private TextView mTextView1;

    private TextView mTextView2;

    private TextView mTextView3;

    private TextView mTextView4;

    private TextView mTextView5;

    private FrameLayout mButton4;

    private FrameLayout mMoreButton;

    private View mMainClickArea;

    private View mClickArea1;

    public ViewHolder(View rootView, int layoutId) {
        mLayoutId = layoutId;
        if (layoutId == R.layout.single_line_list_item) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.single_line_list_textview);
        } else if (layoutId == R.layout.list_item_text) {
            mTextView1 = (TextView) rootView;
        } else if (layoutId == R.layout.content_header_user_navdrawer) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.content_header_textview_user_navdrawer);
            mRoundedImage = (ImageView) rootView
                    .findViewById(R.id.content_header_roundedimage_user_navdrawer);
        } else if (layoutId == R.layout.list_item
                || layoutId == R.layout.list_item_highlighted) {
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
            mMainClickArea = rootView
                    .findViewById(R.id.double_line_list_container);
            mClickArea1 = rootView
                    .findViewById(R.id.double_line_list_clickarea1);
        } else if (layoutId == R.layout.single_line_list_header) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.single_line_list_header_textview);
        } else if (layoutId == R.layout.dropdown_header) {
            mSpinner = (Spinner) rootView
                    .findViewById(R.id.single_line_list_dropdown_header_spinner);
        } else if (layoutId == R.layout.fake_preferences_plain) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
        } else if (layoutId == R.layout.fake_preferences_checkbox) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mCheckBox = (CheckBox) rootView
                    .findViewById(R.id.fake_preferences_checkbox);
        } else if (layoutId == R.layout.fake_preferences_spinner) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mSpinner = (Spinner) rootView
                    .findViewById(R.id.fake_preferences_spinner);
        } else if (layoutId == R.layout.fake_preferences_configauth) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_textview2);
            mImageView2 = (ImageView) rootView
                    .findViewById(R.id.fake_preferences_logo);
        } else if (layoutId == R.layout.fake_preferences_header) {
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.fake_preferences_header_textview);
        } else if (layoutId == R.layout.grid_item) {
            mImageView1 = (ImageView) rootView
                    .findViewById(R.id.album_art_grid_image);
            mTextView1 = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView);
            mTextView2 = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView2);
            mTextView3 = (TextView) rootView
                    .findViewById(R.id.album_art_grid_textView3);
        }
        if (mMainClickArea == null) {
            mMainClickArea = rootView;
        }
    }

    public ViewHolder(View imageFrame, View headerFrame, int layoutId) {
        mLayoutId = layoutId;
        if (layoutId == R.layout.content_header
                || layoutId == R.layout.content_header_user
                || layoutId == R.layout.content_header_static) {
            mTextView1 = (TextView) headerFrame
                    .findViewById(R.id.content_header_textview);
            mRoundedImage = (ImageView) headerFrame
                    .findViewById(R.id.content_header_roundedimage);
            mImageView1 = (ImageView) imageFrame
                    .findViewById(R.id.content_header_image);
            mImageView2 = (ImageView) imageFrame
                    .findViewById(R.id.content_header_image2);
            mImageView3 = (ImageView) imageFrame
                    .findViewById(R.id.content_header_image3);
            mImageView4 = (ImageView) imageFrame
                    .findViewById(R.id.content_header_image4);
            mMoreButton = (FrameLayout) headerFrame
                    .findViewById(R.id.content_header_more_button);
            mButton4 = (FrameLayout) headerFrame
                    .findViewById(R.id.content_header_button4);
        }
        if (mMainClickArea == null) {
            mMainClickArea = headerFrame;
        }
    }

    public int getLayoutId() {
        return mLayoutId;
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

    public FrameLayout getButton4() {
        return mButton4;
    }

    public FrameLayout getMoreButton() {
        return mMoreButton;
    }

    public View getMainClickArea() {
        return mMainClickArea;
    }

    public View getClickArea1() {
        return mClickArea1;
    }
}
