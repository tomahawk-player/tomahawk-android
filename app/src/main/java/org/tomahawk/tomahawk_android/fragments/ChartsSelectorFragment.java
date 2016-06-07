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
package org.tomahawk.tomahawk_android.fragments;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredManager;
import org.jdeferred.multiple.MultipleResults;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsCountryCodes;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsManager;
import org.tomahawk.libtomahawk.infosystem.charts.ScriptChartsProvider;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverMetaData;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.CountryCodeSpinnerAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.views.Selector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class ChartsSelectorFragment extends Fragment {

    private MenuItem mCountryCodePicker;

    private List<FragmentInfo> mFragmentInfos = new ArrayList<>();

    private FragmentInfo mSelectedFragmentInfo;

    @SuppressWarnings("unused")
    public void onEventMainThread(ScriptChartsManager.ProviderAddedEvent event) {
        setupView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.selectorfragment_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(R.string.drawer_title_charts);

        setupView();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mCountryCodePicker = menu.findItem(R.id.action_country_code_picker);
        mCountryCodePicker.setVisible(true);

        if (mSelectedFragmentInfo != null) {
            String chartsProviderId =
                    mSelectedFragmentInfo.mBundle.getString(ChartsPagerFragment.CHARTSPROVIDER_ID);
            ScriptChartsProvider provider =
                    ScriptChartsManager.get().getScriptChartsProvider(chartsProviderId);
            populateCountryCodeSpinner(provider, true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setupView() {
        if (ScriptChartsManager.get().getAllScriptChartsProvider().size() > 0) {
            final List<ScriptChartsProvider> providers = new ArrayList<>();
            for (ScriptChartsProvider provider :
                    ScriptChartsManager.get().getAllScriptChartsProvider().values()) {
                providers.add(provider);
            }
            Collections.sort(providers, new Comparator<ScriptChartsProvider>() {
                @Override
                public int compare(ScriptChartsProvider lhs, ScriptChartsProvider rhs) {
                    return lhs.getScriptAccount().getName()
                            .compareTo(rhs.getScriptAccount().getName());
                }
            });
            List<Promise> promises = new ArrayList<>();
            for (ScriptChartsProvider provider : providers) {
                promises.add(provider.getCountryCodes());
            }
            new AndroidDeferredManager().when(promises.toArray(new Promise[promises.size()])).done(
                    new DoneCallback<MultipleResults>() {
                        @Override
                        public void onDone(MultipleResults multipleResults) {
                            mFragmentInfos.clear();
                            for (int i = 0; i < multipleResults.size(); i++) {
                                ScriptChartsCountryCodes result = (ScriptChartsCountryCodes)
                                        multipleResults.get(i).getResult();
                                FragmentInfo fragmentInfo = new FragmentInfo();
                                fragmentInfo.mClass = ChartsPagerFragment.class;
                                ScriptResolverMetaData metaData =
                                        providers.get(i).getScriptAccount().getMetaData();
                                fragmentInfo.mTitle = metaData.name;
                                fragmentInfo.mBundle = new Bundle();
                                fragmentInfo.mBundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                                        ContentHeaderFragment.MODE_HEADER_STATIC_CHARTS);
                                fragmentInfo.mBundle.putString(
                                        ChartsPagerFragment.CHARTSPROVIDER_ID, metaData.pluginName);

                                String countryCode = getStoredCountryCode(providers.get(i));
                                if (countryCode == null) {
                                    countryCode = result.defaultCode;
                                }
                                fragmentInfo.mBundle.putString(
                                        ChartsPagerFragment.CHARTSPROVIDER_COUNTRYCODE,
                                        countryCode);
                                mFragmentInfos.add(fragmentInfo);
                            }

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    setupSelector();
                                    if (getView() != null) {
                                        getView().findViewById(R.id.circularprogressview_selector)
                                                .setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                    });
        }
    }

    protected void setupSelector() {
        if (getView() != null) {
            FragmentInfo defaultFragmentInfo = mFragmentInfos.get(0);
            String lastDisplayedProviderId = getLastProviderId();
            if (lastDisplayedProviderId != null) {
                for (FragmentInfo info : mFragmentInfos) {
                    if (lastDisplayedProviderId.equals(
                            info.mBundle.getString(ChartsPagerFragment.CHARTSPROVIDER_ID))) {
                        defaultFragmentInfo = info;
                        break;
                    }
                }
            }
            showSelectedFragment(defaultFragmentInfo);

            final View selectorHeader = getView().findViewById(R.id.selectorHeader);
            selectorHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectorHeaderMode(true);

                    Selector selector = (Selector) getView().findViewById(R.id.selector);
                    Selector.SelectorListener selectorListener = new Selector.SelectorListener() {
                        @Override
                        public void onSelectorItemSelected(int position) {
                            setSelectorHeaderMode(false);

                            showSelectedFragment(mFragmentInfos.get(position));
                        }

                        @Override
                        public void onCancel() {
                            setSelectorHeaderMode(false);
                        }
                    };
                    selector.setup(mFragmentInfos, selectorListener,
                            getActivity().findViewById(R.id.sliding_layout), null);
                    selector.showSelectorList();
                }
            });
        }
    }

    private void setSelectorHeaderMode(boolean selectorShown) {
        if (getView() != null) {
            ImageView arrowTop = (ImageView) getView().findViewById(R.id.arrow_top_header);
            ImageView arrowBottom = (ImageView) getView().findViewById(R.id.arrow_bottom_header);
            View selectorHeader = getView().findViewById(R.id.selectorHeader);
            TextView textViewHeader = (TextView) getView().findViewById(R.id.textview_header);

            selectorHeader.setClickable(!selectorShown);
            arrowTop.setVisibility(selectorShown ? View.GONE : View.VISIBLE);
            arrowBottom.setVisibility(selectorShown ? View.GONE : View.VISIBLE);
            textViewHeader.setVisibility(selectorShown ? View.VISIBLE : View.GONE);
        }
    }

    private void showSelectedFragment(FragmentInfo info) {
        if (getView() != null) {
            ImageView imageView = (ImageView) getView().findViewById(R.id.imageview_header);
            TextView textView = (TextView) getView().findViewById(R.id.textview_header);

            mSelectedFragmentInfo = info;
            String chartsProviderId = info.mBundle.getString(ChartsPagerFragment.CHARTSPROVIDER_ID);
            storeLastProviderId(chartsProviderId);
            ScriptChartsProvider provider =
                    ScriptChartsManager.get().getScriptChartsProvider(chartsProviderId);
            populateCountryCodeSpinner(provider, true);
            provider.getScriptAccount().loadIconWhite(imageView, 0);
            textView.setText(info.mTitle.toUpperCase());
            textView.setVisibility(View.GONE);

            FragmentUtils.replace((TomahawkMainActivity) getActivity(), info.mClass,
                    mSelectedFragmentInfo.mBundle, R.id.content_frame);
        }
    }

    private void populateCountryCodeSpinner(final ScriptChartsProvider provider,
            final boolean isInitial) {
        if (mCountryCodePicker != null && provider != null) {
            provider.getCountryCodes().done(new DoneCallback<ScriptChartsCountryCodes>() {
                @Override
                public void onDone(final ScriptChartsCountryCodes result) {
                    final ArrayList<String> countryCodes = new ArrayList<>();
                    final ArrayList<String> displayedCountryCodes = new ArrayList<>();
                    final ArrayList<String> displayedCountryCodeNames = new ArrayList<>();
                    for (Pair<String, String> countryCode : result.codes) {
                        countryCodes.add(countryCode.second);
                        displayedCountryCodes.add(countryCode.second.toUpperCase());
                        displayedCountryCodeNames.add(countryCode.first);
                    }
                    int initialPosition = -1;
                    if (isInitial) {
                        // Must be the first call of this method. So we should set the initially
                        // stored (or default) selection of the spinner
                        String storedCountryCode =
                                ChartsSelectorFragment.this.getStoredCountryCode(provider);
                        if (storedCountryCode == null) {
                            storedCountryCode = result.defaultCode;
                        }
                        for (int i = 0; i < countryCodes.size(); i++) {
                            if (countryCodes.get(i).equalsIgnoreCase(storedCountryCode)) {
                                initialPosition = i;
                            }
                        }
                    }
                    Spinner mCountryCodePickerSpinner = (Spinner) mCountryCodePicker
                            .getActionView();
                    CountryCodeSpinnerAdapter adapter =
                            new CountryCodeSpinnerAdapter(TomahawkApp.getContext(),
                                    R.layout.spinner_textview_country_code, displayedCountryCodes,
                                    displayedCountryCodeNames);
                    adapter.setDropDownViewResource(
                            R.layout.spinner_dropdown_textview_country_code);
                    mCountryCodePickerSpinner.setAdapter(adapter);
                    mCountryCodePickerSpinner
                            .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view,
                                        int position, long id) {
                                    String selectedCountryCode = countryCodes.get(position);
                                    String storedCountryCode = ChartsSelectorFragment.this
                                            .getStoredCountryCode(provider);
                                    if (storedCountryCode == null) {
                                        storedCountryCode = result.defaultCode;
                                    }
                                    if (!storedCountryCode.equals(selectedCountryCode)) {
                                        storeCountryCode(provider, selectedCountryCode);
                                        mSelectedFragmentInfo.mBundle.putString(
                                                ChartsPagerFragment.CHARTSPROVIDER_COUNTRYCODE,
                                                selectedCountryCode);
                                        // Refresh the currently shown ChartsPagerFragment
                                        FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                                                mSelectedFragmentInfo.mClass,
                                                mSelectedFragmentInfo.mBundle, R.id.content_frame);
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                    if (initialPosition >= 0) {
                        mCountryCodePickerSpinner.setSelection(initialPosition);
                    }
                }
            });
        }
    }

    private String getCountryCodeStorageKey(ScriptChartsProvider provider) {
        return PreferenceUtils.CHARTS_COUNTRY_CODE
                + provider.getScriptAccount().getMetaData().pluginName;
    }

    private void storeCountryCode(ScriptChartsProvider provider, String countryCode) {
        PreferenceUtils.edit()
                .putString(getCountryCodeStorageKey(provider), countryCode).commit();
    }

    private String getStoredCountryCode(ScriptChartsProvider provider) {
        return PreferenceUtils.getString(getCountryCodeStorageKey(provider));
    }

    private void storeLastProviderId(String lastProviderId) {
        PreferenceUtils.edit()
                .putString(PreferenceUtils.LAST_DISPLAYED_PROVIDER_ID, lastProviderId).commit();
    }

    private String getLastProviderId() {
        return PreferenceUtils.getString(PreferenceUtils.LAST_DISPLAYED_PROVIDER_ID);
    }
}
