/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.views.HatchetLoginRegisterView;
import org.tomahawk.tomahawk_android.views.SimplePagerIndicator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.greenrobot.event.EventBus;

/**
 * A {@link android.support.v4.app.Fragment} which is being shown to the user when he first opens
 * the app
 */
public class WelcomeFragment extends Fragment {

    public final static String TAG = WelcomeFragment.class.getSimpleName();

    private final static String CURRENT_PAGE = "current_page";

    private ViewPager mViewPager;

    private TextView mPositiveButton;

    private HatchetLoginRegisterView mHatchetLoginRegisterView;

    private class LoginRegisterPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            LayoutInflater inflater = activity.getLayoutInflater();
            View v = null;
            switch (position) {
                case 0:
                    v = inflater.inflate(R.layout.welcome_fragment_page_explanation, container,
                            false);
                    break;
                case 1:
                    v = inflater.inflate(R.layout.welcome_fragment_page_setup, container,
                            false);
                    FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                    ft.add(R.id.welcome_fragment_page_setup_container,
                            Fragment.instantiate(activity,
                                    PreferenceConnectFragment.class.getName(), null));
                    ft.commit();
                    break;
                case 2:
                    v = inflater.inflate(R.layout.welcome_fragment_page_hatchet, container,
                            false);
                    ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.smoothprogressbar);
                    HatchetAuthenticatorUtils authenticatorUtils = (HatchetAuthenticatorUtils)
                            AuthenticatorManager.get().getAuthenticatorUtils(
                                    TomahawkApp.PLUGINNAME_HATCHET);
                    mHatchetLoginRegisterView =
                            (HatchetLoginRegisterView) v.findViewById(R.id.hatchetloginregister);
                    mHatchetLoginRegisterView.setup(authenticatorUtils, progressBar);
                    break;
                case 3:
                    v = inflater.inflate(R.layout.welcome_fragment_page_done, container,
                            false);
                    break;
            }
            if (v != null) {
                container.addView(v);
            }
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            int lastPage = mViewPager.getAdapter().getCount() - 1;
            if (position == lastPage) {
                mPositiveButton.setText(R.string.ok);
            } else {
                mPositiveButton.setText(R.string.next_page);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(AuthenticatorManager.ConfigTestResultEvent event) {
        if (mHatchetLoginRegisterView != null) {
            mHatchetLoginRegisterView
                    .onConfigTestResult(event.mComponent, event.mType, event.mMessage);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mPositiveButton = (TextView) view.findViewById(R.id.config_dialog_positive_button);
        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int lastPage = mViewPager.getAdapter().getCount() - 1;
                if (mViewPager.getCurrentItem() == lastPage) {
                    SharedPreferences preferences =
                            PreferenceManager.getDefaultSharedPreferences(getActivity());
                    preferences.edit().putBoolean(
                            TomahawkMainActivity.COACHMARK_WELCOMEFRAGMENT_DISABLED, true).apply();
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                }
            }
        });
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new LoginRegisterPagerAdapter());
        SimplePagerIndicator indicator =
                (SimplePagerIndicator) view.findViewById(R.id.simplepagerindicator);
        indicator.setViewPager(mViewPager);
        indicator.setOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }
}
