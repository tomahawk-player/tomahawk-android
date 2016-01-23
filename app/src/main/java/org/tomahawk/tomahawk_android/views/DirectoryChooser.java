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

import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.DirectoryChooserAdapter;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class DirectoryChooser extends FrameLayout implements
        StickyListHeadersListView.OnHeaderClickListener {

    private final static String TAG = DirectoryChooser.class.getSimpleName();

    private int mDrillDownCount = 0;

    private int mLastDrillDownCount = 0;

    private File mCurrentFolderRoot;

    private DirectoryChooserAdapter mAdapter;

    public DirectoryChooser(Context context) {
        super(context);
        inflate(getContext(), R.layout.directory_chooser, this);
    }

    public DirectoryChooser(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.directory_chooser, this);
    }

    public interface DirectoryChooserListener {

        void onDirectoryChecked(File chosenSubFolder, boolean isChecked);

        void onDirectoryBrowsed(File clickedSubFolder);
    }

    public void setup() {
        mDrillDownCount = 0;
        ArrayList<String> storageDirs = new ArrayList<>();
        storageDirs.addAll(UserCollection.getStorageDirectories());
        List<File> mediaDirs = new ArrayList<>();
        for (String dir : storageDirs) {
            File f = new File(dir);
            if (f.exists()) {
                mediaDirs.add(f);
            }
        }
        setup(mediaDirs);
    }

    public void setup(File currentFolderRoot) {
        mCurrentFolderRoot = currentFolderRoot;
        ArrayList<File> folders = new ArrayList<>();
        if (mCurrentFolderRoot.listFiles() != null && mCurrentFolderRoot.listFiles().length > 0) {
            for (File file : mCurrentFolderRoot.listFiles()) {
                if (file.isDirectory() && !file.isHidden()) {
                    folders.add(file);
                }
            }
        }
        setup(folders);
    }

    public void setup(List<File> folders) {
        boolean isFirstRoot = mDrillDownCount == 0;
        ArrayList<DirectoryChooserAdapter.CustomDirectory> dirs
                = new ArrayList<>();
        for (File folder : folders) {
            DirectoryChooserAdapter.CustomDirectory dir
                    = new DirectoryChooserAdapter.CustomDirectory();
            dir.file = folder;
            try {
                dir.isWhitelisted = DatabaseHelper.get()
                        .isMediaDirWhiteListed(folder.getCanonicalPath());
                dir.isMediaDirComplete = DatabaseHelper.get()
                        .isMediaDirComplete(folder.getCanonicalPath());
            } catch (IOException e) {
                Log.e(TAG, "setup: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            int position;
            for (position = 0; position < dirs.size(); position++) {
                if (dir.file.getName().compareToIgnoreCase(dirs.get(position).file.getName()) < 0) {
                    break;
                }
            }
            dirs.add(position, dir);
        }
        if (mAdapter == null) {
            mAdapter = new DirectoryChooserAdapter(LayoutInflater.from(getContext()), isFirstRoot,
                    dirs, new DirectoryChooserListener() {
                @Override
                public void onDirectoryChecked(File chosenSubFolder, boolean isChecked) {
                    try {
                        if (isChecked) {
                            DatabaseHelper.get()
                                    .addMediaDir(chosenSubFolder.getCanonicalPath());
                        } else {
                            DatabaseHelper.get()
                                    .removeMediaDir(chosenSubFolder.getCanonicalPath());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "onDirectoryChecked: " + e.getClass() + ": "
                                + e.getLocalizedMessage());
                    }
                    if (mDrillDownCount == 0) {
                        setup();
                    } else {
                        setup(chosenSubFolder.getParentFile());
                    }
                }

                @Override
                public void onDirectoryBrowsed(File clickedSubFolder) {
                    for (File file : clickedSubFolder.listFiles()) {
                        if (file.isDirectory()) {
                            mDrillDownCount++;
                            setup(clickedSubFolder);
                            break;
                        }
                    }
                }
            });
        } else {
            mAdapter.update(isFirstRoot, dirs);
        }
        StickyListHeadersListView listView =
                (StickyListHeadersListView) findViewById(R.id.listview);
        listView.setOnHeaderClickListener(this);
        if (mDrillDownCount == mLastDrillDownCount) {
            Parcelable listState = listView.getWrappedList().onSaveInstanceState();
            listView.setAdapter(mAdapter);
            listView.getWrappedList().onRestoreInstanceState(listState);
        } else {
            listView.setAdapter(mAdapter);
        }
        mLastDrillDownCount = mDrillDownCount;
    }

    @Override
    public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition,
            long headerId, boolean currentlySticky) {
        if (mCurrentFolderRoot != null && mCurrentFolderRoot.getParentFile() != null) {
            mDrillDownCount--;
            if (mDrillDownCount > 0) {
                setup(mCurrentFolderRoot.getParentFile());
            } else {
                setup();
            }
        }
    }

}
