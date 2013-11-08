/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkContextMenuAdapter;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class FakeContextMenuDialog extends DialogFragment {

    private String[] mMenuItemTitles;

    private int mPosition;

    private FakeContextMenu mFakeContextMenu;

    /**
     * Construct a {@link FakeContextMenuDialog}
     *
     * @param menuItemTitles  array of {@link String} containing all menu entry texts
     * @param position        position of the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     *                        this {@link FakeContextMenuDialog} is associated with
     * @param fakeContextMenu reference to the {@link FakeContextMenu}, so that we can access its
     *                        implementation of onFakeContextItemSelected(...)
     */
    public FakeContextMenuDialog(String[] menuItemTitles, int position,
            FakeContextMenu fakeContextMenu) {
        setRetainInstance(true);
        mMenuItemTitles = menuItemTitles;
        mPosition = position;
        mFakeContextMenu = fakeContextMenu;
    }

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fake_context_menu_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.fake_context_menu_dialog_listview);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mFakeContextMenu.onFakeContextItemSelected(mMenuItemTitles[position], mPosition);
                dismiss();
            }
        });
        listView.setAdapter(
                new TomahawkContextMenuAdapter(getActivity().getLayoutInflater(), mMenuItemTitles));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }
}
