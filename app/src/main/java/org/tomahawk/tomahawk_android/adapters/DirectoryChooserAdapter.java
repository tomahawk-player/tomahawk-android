/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.views.DirectoryChooser;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * This class populates the listview inside the navigation drawer
 */
public class DirectoryChooserAdapter extends StickyBaseAdapter {

    private final LayoutInflater mLayoutInflater;

    private boolean mIsFirstRoot;

    private List<CustomDirectory> mFolders;

    private final DirectoryChooser.DirectoryChooserListener mDirectoryChooserListener;

    public static class CustomDirectory {

        public File file;

        public boolean isWhitelisted;

        public boolean isMediaDirComplete;
    }

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.adapters.DirectoryChooserAdapter}
     *
     * @param isFirstRoot true if the currentFolderRoot is the upmost root that should be reachable
     */
    public DirectoryChooserAdapter(LayoutInflater layoutInflater, boolean isFirstRoot,
            List<CustomDirectory> folders,
            DirectoryChooser.DirectoryChooserListener directoryChooserListener) {
        mLayoutInflater = layoutInflater;
        mIsFirstRoot = isFirstRoot;
        mFolders = folders;
        mDirectoryChooserListener = directoryChooserListener;
    }

    public void update(boolean isFirstRoot, List<CustomDirectory> folders) {
        mIsFirstRoot = isFirstRoot;
        mFolders = folders;
        notifyDataSetChanged();
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        return mFolders.size();
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        return mFolders.get(position);
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get the correct {@link android.view.View} for the given position.
     *
     * @param position    The position for which to get the correct {@link android.view.View}
     * @param convertView The old {@link android.view.View}, which we might be able to recycle
     * @param parent      parental {@link android.view.ViewGroup}
     * @return the correct {@link android.view.View} for the given position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
            view = convertView;
        } else {
            view = mLayoutInflater.inflate(R.layout.list_item_folder, parent, false);
            viewHolder = new ViewHolder(view, R.layout.list_item_folder);
            view.setTag(viewHolder);
        }

        final CustomDirectory dir = (CustomDirectory) getItem(position);

        // Init checkbox
        CheckBox checkBox = (CheckBox) viewHolder.findViewById(R.id.checkbox1);
        CheckBox checkBox2 = (CheckBox) viewHolder.findViewById(R.id.checkbox2);
        if (!dir.isMediaDirComplete) {
            checkBox2.setButtonDrawable(R.drawable.abc_btn_check_to_on_mtrl_015_disabled);
            checkBox.setVisibility(View.GONE);
            checkBox2.setVisibility(View.VISIBLE);
        } else {
            checkBox.setVisibility(View.VISIBLE);
            checkBox2.setVisibility(View.GONE);
        }
        checkBox.setOnCheckedChangeListener(null);
        checkBox2.setOnCheckedChangeListener(null);
        checkBox.setChecked(dir.isWhitelisted || !dir.isMediaDirComplete);
        checkBox2.setChecked(dir.isWhitelisted || !dir.isMediaDirComplete);
        CompoundButton.OnCheckedChangeListener listener =
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mDirectoryChooserListener.onDirectoryChecked(dir.file, isChecked);
                    }
                };
        checkBox.setOnCheckedChangeListener(listener);
        checkBox2.setOnCheckedChangeListener(listener);

        // Init textviews and main click listener
        TextView textView = (TextView) viewHolder.findViewById(R.id.textview1);
        textView.setText(getVisibleName(dir.file));
        textView.setTypeface(null, Typeface.NORMAL);
        view.findViewById(R.id.browsable_indicator).setVisibility(View.INVISIBLE);
        view.setOnClickListener(null);
        if (dir.file.listFiles() != null
                && dir.file.listFiles().length > 0) {
            for (File file : dir.file.listFiles()) {
                if (file.isDirectory()) {
                    textView.setTypeface(null, Typeface.BOLD);
                    ImageView browsableIndicator =
                            (ImageView) view.findViewById(R.id.browsable_indicator);
                    ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                            browsableIndicator, R.drawable.ic_navigation_chevron_right,
                            android.R.color.black);
                    view.findViewById(R.id.browsable_indicator).setVisibility(View.VISIBLE);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mDirectoryChooserListener.onDirectoryBrowsed(dir.file);
                        }
                    });
                    break;
                }
            }
        }
        return view;
    }

    /**
     * This method is being called by the StickyListHeaders library. Get the correct header {@link
     * android.view.View} for the given position.
     *
     * @param position    The position for which to get the correct {@link android.view.View}
     * @param convertView The old {@link android.view.View}, which we might be able to recycle
     * @param parent      parental {@link android.view.ViewGroup}
     * @return the correct header {@link android.view.View} for the given position.
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (!mIsFirstRoot) {
            View view;
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            } else {
                LayoutInflater layoutInflater = LayoutInflater.from(TomahawkApp.getContext());
                view = layoutInflater.inflate(R.layout.list_item_folder_header, parent, false);
                viewHolder = new ViewHolder(view, R.layout.list_item_folder_header);
                view.setTag(viewHolder);
            }

            TextView textView = (TextView) viewHolder.findViewById(R.id.textview1);
            final CustomDirectory dir = (CustomDirectory) getItem(position);
            if (dir != null) {
                textView.setText("../" + getVisibleName(dir.file.getParentFile()));
            }

            return view;
        } else {
            return new View(TomahawkApp.getContext());
        }
    }

    /**
     * This method is being called by the StickyListHeaders library. Returns the same value for each
     * item that should be grouped under the same header.
     *
     * @param position the position of the item for which to get the header id
     * @return the same value for each item that should be grouped under the same header.
     */
    @Override
    public long getHeaderId(int position) {
        return 0;
    }

    private String getVisibleName(File file) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Show "sdcard" for the user's folder when running in multi-user
            if (file.getAbsolutePath()
                    .equals(Environment.getExternalStorageDirectory().getPath())) {
                return TomahawkApp.getContext().getString(R.string.internal_storage);
            }
        }
        return file.getName();
    }
}
