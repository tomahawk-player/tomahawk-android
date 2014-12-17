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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.resolver.HatchetStubResolver;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.ConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.DirectoryChooserConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.LoginConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.RedirectConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.ResolverConfigDialog;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkListFragment} which fakes the standard
 * {@link android.preference.PreferenceFragment} behaviour. We need to fake it, because the official
 * support library doesn't provide a {@link android.preference.PreferenceFragment} class
 */
public class PreferenceConnectFragment extends TomahawkListFragment
        implements MultiColumnClickListener {

    private static final String TAG = PreferenceConnectFragment.class.getSimpleName();

    private FakePreferenceFragmentReceiver mFakePreferenceFragmentReceiver;

    private class FakePreferenceFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AuthenticatorManager.CONFIG_TEST_RESULT.equals(intent.getAction())) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        getListAdapter().notifyDataSetChanged();
                    }
                });
            } else if (CollectionManager.COLLECTION_UPDATED.equals(intent.getAction())) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        getListAdapter().notifyDataSetChanged();
                    }
                });
            }
        }
    }

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.PreferenceConnectFragment}'s
     * {@link android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<Segment> segments = new ArrayList<>();

        // Add the header text item
        List textItems = new ArrayList();
        textItems.add(new ListItemString(getString(R.string.connect_headertext)));
        Segment segment = new Segment(textItems);
        segments.add(segment);

        // Add all resolver grid items
        List resolvers = new ArrayList();
        resolvers.add(PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION));
        resolvers.add(new HatchetStubResolver(HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME, null));
        resolvers.add(PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_SPOTIFY));
        for (ScriptResolver scriptResolver : PipeLine.getInstance().getScriptResolvers()) {
            if (!scriptResolver.getId().contains("-metadata")) {
                resolvers.add(scriptResolver);
            }
        }
        segment = new Segment(resolvers, R.integer.grid_column_count,
                R.dimen.padding_superlarge, R.dimen.padding_superlarge);
        segments.add(segment);

        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(
                    (TomahawkMainActivity) getActivity(), getActivity().getLayoutInflater(),
                    segments, this);
            setListAdapter(tomahawkListAdapter);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setSegments(segments, getListView());
        }

        setupNonScrollableSpacer();
    }

    /**
     * Initialize
     */
    @Override
    public void onResume() {
        super.onResume();

        getListAdapter().notifyDataSetChanged();

        if (mFakePreferenceFragmentReceiver == null) {
            mFakePreferenceFragmentReceiver = new FakePreferenceFragmentReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        getActivity().registerReceiver(mFakePreferenceFragmentReceiver,
                new IntentFilter(AuthenticatorManager.CONFIG_TEST_RESULT));
        getActivity().registerReceiver(mFakePreferenceFragmentReceiver,
                new IntentFilter(CollectionManager.COLLECTION_UPDATED));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mFakePreferenceFragmentReceiver != null) {
            getActivity().unregisterReceiver(mFakePreferenceFragmentReceiver);
            mFakePreferenceFragmentReceiver = null;
        }
    }

    @Override
    public void onItemClick(View view, Object item) {
        if (item instanceof Resolver) {
            String id = ((Resolver) item).getId();
            ConfigDialog dialog;
            switch (id) {
                case TomahawkApp.PLUGINNAME_RDIO:
                case TomahawkApp.PLUGINNAME_DEEZER:
                    dialog = new RedirectConfigDialog();
                    break;
                case TomahawkApp.PLUGINNAME_USERCOLLECTION:
                    dialog = new DirectoryChooserConfigDialog();
                    break;
                case TomahawkApp.PLUGINNAME_HATCHET:
                case TomahawkApp.PLUGINNAME_SPOTIFY:
                    dialog = new LoginConfigDialog();
                    break;
                default:
                    dialog = new ResolverConfigDialog();
                    break;
            }
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY, id);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        }
    }

    @Override
    public boolean onItemLongClick(View view, Object item) {
        return false;
    }
}
