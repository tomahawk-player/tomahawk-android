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
package org.tomahawk.tomahawk_android.views;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class Selector extends FrameLayout {

    private List<FragmentInfo> mFragmentInfos;

    private SelectorListener mSelectorListener;

    private View mRootView;

    private String mSelectorPosStorageKey;

    private boolean mListShowing;

    public interface SelectorListener {

        void onSelectorItemSelected(int position);

        void onCancel();
    }

    public Selector(Context context) {
        super(context);
        inflate(getContext(), R.layout.selector, this);
    }

    public Selector(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.selector, this);
    }

    public void setup(List<FragmentInfo> selectorItems,
            SelectorListener selectorListener, View rootView, String selectorPosStorageKey) {
        mFragmentInfos = selectorItems;
        mSelectorListener = selectorListener;
        mRootView = rootView;
        mSelectorPosStorageKey = selectorPosStorageKey;
    }

    public void showSelectorList() {
        if (!isListShowing()) {
            setClickable(true);
            mListShowing = true;
            Bitmap bm = Bitmap.createBitmap(mRootView.getWidth(),
                    mRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            mRootView.draw(canvas);
            bm = Bitmap.createScaledBitmap(bm, bm.getWidth() / 4,
                    bm.getHeight() / 4, true);
            bm = BlurTransformation.staticTransform(bm, 25f);
            final ImageView bgImageView = (ImageView) findViewById(R.id.background);
            bgImageView.setImageBitmap(bm);

            final LinearLayout selectorFrame = (LinearLayout) findViewById(R.id.selector_frame);
            selectorFrame.removeAllViews();
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (int i = 0; i < mFragmentInfos.size(); i++) {
                LinearLayout item = (LinearLayout) inflater.inflate(R.layout.selectorfragment_item,
                        selectorFrame, false);
                final TextView textView = (TextView) item.findViewById(R.id.textview);
                textView.setText(mFragmentInfos.get(i).mTitle.toUpperCase());
                ImageView imageView = (ImageView) item.findViewById(R.id.imageview);
                imageView.setImageResource(mFragmentInfos.get(i).mIconResId);

                final int position = i;
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideSelectorList();
                        mSelectorListener.onSelectorItemSelected(position);

                        if (mSelectorPosStorageKey != null) {
                            SharedPreferences preferences = PreferenceManager
                                    .getDefaultSharedPreferences(TomahawkApp.getContext());
                            int initialPos = preferences.getInt(mSelectorPosStorageKey, 0);
                            if (initialPos != position) {
                                preferences.edit().putInt(mSelectorPosStorageKey, position).apply();
                            }
                        }
                    }
                });
                selectorFrame.addView(item);
            }
            //Set up cancel button
            LinearLayout item = (LinearLayout) inflater.inflate(R.layout.selectorfragment_item,
                    selectorFrame, false);
            final TextView textView = (TextView) item.findViewById(R.id.textview);
            textView.setText(getResources().getString(R.string.cancel).toUpperCase());
            ImageView imageView = (ImageView) item.findViewById(R.id.imageview);
            imageView.setImageResource(R.drawable.ic_player_exit_light);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSelectorList();
                    mSelectorListener.onCancel();
                }
            });
            selectorFrame.addView(item);
            AnimationUtils.fade(bgImageView, 120, true);
            AnimationUtils.fade(findViewById(R.id.darkening_background), 120, true);
            animateScale(selectorFrame, false, null);
        }
    }

    public void hideSelectorList() {
        if (isListShowing()) {
            setClickable(false);
            mListShowing = false;
            AnimationUtils.fade(findViewById(R.id.darkening_background), 120, false);
            AnimationUtils.fade(findViewById(R.id.background), 120, false);
            final LinearLayout selectorFrame = (LinearLayout) findViewById(R.id.selector_frame);
            animateScale(selectorFrame, true, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    selectorFrame.removeAllViews();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    public boolean isListShowing() {
        return mListShowing;
    }

    private void animateScale(View view, boolean reverse, Animation.AnimationListener listener) {
        ScaleAnimation animation;
        if (reverse) {
            animation = new ScaleAnimation(1f, 0.5f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0f);
        } else {
            animation = new ScaleAnimation(0.5f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0f);
        }
        animation.setDuration(120);
        view.startAnimation(animation);
        animation.setFillAfter(true);
        animation.setAnimationListener(listener);
    }
}
