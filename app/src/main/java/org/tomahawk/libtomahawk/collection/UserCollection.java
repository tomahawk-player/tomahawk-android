/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.collection;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.database.CollectionDb;
import org.tomahawk.libtomahawk.database.CollectionDbManager;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.database.UserCollectionDb;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.UserCollectionStubResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverTrack;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.MediaWrapper;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.libvlc.util.Extensions;

import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This class represents a user's local {@link UserCollection}.
 */
public class UserCollection extends DbCollection {

    private static final String TAG = UserCollection.class.getSimpleName();

    private static final String HAS_SET_DEFAULTDIRS
            = "org.tomahawk.tomahawk_android.has_set_defaultdirs";

    private static final List<String> TYPE_WHITELIST = Arrays.asList("vfat", "exfat", "sdcardfs",
            "fuse", "ntfs", "fat32", "ext3", "ext4", "esdfs");

    private static final List<String> TYPE_BLACKLIST = Collections.singletonList("tmpfs");

    private static final String[] MOUNT_WHITELIST = {"/mnt", "/Removable", "/storage"};

    private static final String[] MOUNT_BLACKLIST = {"/mnt/secure", "/mnt/shell", "/mnt/asec",
            "/mnt/obb", "/mnt/media_rw/extSdCard", "/mnt/media_rw/sdcard", "/storage/emulated"};

    private static final String[] DEVICE_WHITELIST = {"/dev/block/vold", "/dev/fuse",
            "/mnt/media_rw"};

    public final static HashSet<String> FOLDER_BLACKLIST;

    static {
        final String[] folder_blacklist = {"/alarms", "/notifications", "/ringtones",
                "/media/alarms", "/media/notifications", "/media/ringtones", "/media/audio/alarms",
                "/media/audio/notifications", "/media/audio/ringtones", "/Android/data/"};

        FOLDER_BLACKLIST = new HashSet<>();
        for (String item : folder_blacklist) {
            FOLDER_BLACKLIST
                    .add(android.os.Environment.getExternalStorageDirectory().getPath() + item);
        }
    }

    private boolean mIsStopping = false;

    private boolean mRestart = false;

    private Thread mLoadingThread;

    private final ConcurrentHashMap<Query, Long> mQueryTimeStamps
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Artist, Long> mArtistTimeStamps
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Album, Long> mAlbumTimeStamps
            = new ConcurrentHashMap<>();

    public UserCollection() {
        super(UserCollectionStubResolver.get());
    }

    @Override
    public Promise<String, Throwable, Void> getCollectionId() {
        Deferred<String, Throwable, Void> d = new ADeferredObject<>();
        return d.resolve(TomahawkApp.PLUGINNAME_USERCOLLECTION);
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
    }

    public void loadMediaItems(boolean fullScan) {
        if (fullScan) {
            Log.d(TAG, "Executing full scan. Wiping cache...");
            DatabaseHelper.get().removeAllMedias();
        }
        if (isWorking()) {
            if (fullScan) {
                // do a clean restart if a scan is ongoing
                mRestart = true;
                mIsStopping = true;
            }
        } else {
            loadMediaItems();
        }
    }

    private void loadMediaItems() {
        if (mLoadingThread == null || mLoadingThread.getState() == Thread.State.TERMINATED) {
            mIsStopping = false;
            mLoadingThread = new Thread(new GetMediaItemsRunnable());
            mLoadingThread.start();
        }
    }

    public void stop() {
        mIsStopping = true;
    }

    public boolean isWorking() {
        return mLoadingThread != null &&
                mLoadingThread.isAlive() &&
                mLoadingThread.getState() != Thread.State.TERMINATED &&
                mLoadingThread.getState() != Thread.State.NEW;
    }

    private class GetMediaItemsRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "Scanning for local tracks...");
            long time = System.currentTimeMillis();
            Set<String> setDefaultDirs = PreferenceUtils.getStringSet(HAS_SET_DEFAULTDIRS);
            if (setDefaultDirs == null) {
                setDefaultDirs = new HashSet<>();
            }
            for (String defaultDir : getStorageDirectories()) {
                if (!setDefaultDirs.contains(defaultDir)) {
                    Log.d(TAG, "Default directory added: " + defaultDir);
                    DatabaseHelper.get().addMediaDir(defaultDir);
                    setDefaultDirs.add(defaultDir);
                }
            }
            PreferenceUtils.edit().putStringSet(HAS_SET_DEFAULTDIRS, setDefaultDirs).commit();

            List<File> mediaDirs = DatabaseHelper.get().getMediaDirs(false);
            Stack<File> directories = new Stack<>();
            directories.addAll(mediaDirs);
            for (File dir : directories) {
                Log.d(TAG, "Scanning directory: " + dir);
            }

            // get all existing media items
            HashMap<String, MediaWrapper> existingMedias = DatabaseHelper.get().getMedias();

            // list of all added files
            HashSet<String> addedLocations = new HashSet<>();

            ArrayList<File> mediaToScan = new ArrayList<>();
            try {
                long listFilesTimeBefore = System.currentTimeMillis();
                final HashSet<String> directoriesScanned = new HashSet<>();
                // Count total files, and stack them
                while (!directories.isEmpty()) {
                    File dir = directories.pop();
                    String dirPath = dir.getAbsolutePath();

                    // Skip some system folders
                    if (dirPath.startsWith("/proc/") || dirPath.startsWith("/sys/")
                            || dirPath.startsWith("/dev/")) {
                        continue;
                    }

                    // Do not scan again if same canonical path
                    try {
                        dirPath = dir.getCanonicalPath();
                    } catch (IOException e) {
                        Log.e(TAG, "GetMediaItemsRunnable#run() - " + e.getClass() + ": "
                                + e.getLocalizedMessage());
                    }
                    if (directoriesScanned.contains(dirPath)) {
                        continue;
                    } else {
                        directoriesScanned.add(dirPath);
                    }

                    // Do no scan media in .nomedia folders
                    if (new File(dirPath + "/.nomedia").exists()) {
                        continue;
                    }

                    // Filter the extensions and the folders
                    try {
                        File[] f = dir.listFiles(new MediaItemFilter());
                        if (f != null) {
                            for (File file : f) {
                                if (file.isFile()) {
                                    mediaToScan.add(file);
                                } else if (file.isDirectory()) {
                                    directories.push(file);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // listFiles can fail in OutOfMemoryError, go to the next folder
                        Log.e(TAG, "GetMediaItemsRunnable#run() - " + e.getClass() + ": "
                                + e.getLocalizedMessage());
                        continue;
                    }

                    if (mIsStopping) {
                        Log.d(TAG, "Stopping scan");
                        return;
                    }
                }
                long listFilesTime = System.currentTimeMillis() - listFilesTimeBefore;
                int parseCounter = 0;
                long parsingTimeBefore = System.currentTimeMillis();
                ArrayList<MediaWrapper> mediaWrappers = new ArrayList<>();
                // Process the stacked items
                for (File file : mediaToScan) {
                    String fileURI = AndroidUtil.FileToUri(file).toString();
                    if (existingMedias.containsKey(fileURI)) {
                        //Log.d(TAG, "File has already been scanned: " + fileURI);
                        // only add file if it is not already in the list. eg. if a user selects a
                        // subfolder as well
                        if (!addedLocations.contains(fileURI)) {
                            //Log.d(TAG, "File added to processing queue: " + fileURI);
                            // get existing media item from database
                            mediaWrappers.add(existingMedias.get(fileURI));
                            addedLocations.add(fileURI);
                        }
                    } else {
                        // create new media item
                        final Media media = new Media(VLCMediaPlayer.getLibVlcInstance(),
                                Uri.parse(fileURI));
                        media.parse();
                        parseCounter++;
                        // skip files with .mod extension and no duration
                        if ((media.getDuration() == 0 || (media.getTrackCount() != 0
                                && TextUtils.isEmpty(media.getTrack(0).codec)))
                                && fileURI.endsWith(".mod")) {
                            Log.d(TAG, "File skipped: " + fileURI);
                            continue;
                        }
                        //Log.d(TAG, "File added to database and processing queue: " + fileURI);
                        MediaWrapper mw = new MediaWrapper(media);
                        media.release();
                        mw.setLastModified(file.lastModified());
                        mediaWrappers.add(mw);
                    }
                    if (mIsStopping) {
                        Log.d(TAG, "Stopping scan");
                        return;
                    }
                }
                Log.d(TAG, "Listing files took " + listFilesTime + "ms.");
                Log.d(TAG, "Scanned " + mediaToScan.size() + " files.");
                Log.d(TAG, "Actually parsed " + parseCounter + " files.");
                Log.d(TAG,
                        "Parsing took " + (System.currentTimeMillis() - parsingTimeBefore) + "ms.");
                // Add all items to the database
                DatabaseHelper.get().addMedias(mediaWrappers);

                processMediaWrappers(mediaWrappers);
            } finally {
                // remove old files & folders from database if storage is mounted
                if (!mIsStopping && Environment.getExternalStorageState()
                        .equals(Environment.MEDIA_MOUNTED)) {
                    for (String fileURI : addedLocations) {
                        existingMedias.remove(fileURI);
                    }
                    Log.d(TAG, "Removed " + existingMedias.keySet().size()
                            + " media items from database");
                    DatabaseHelper.get().removeMedias(existingMedias.keySet());
                }

                if (mRestart) {
                    Log.d(TAG, "Restarting scan");
                    mRestart = false;
                    mRestartHandler.sendEmptyMessageDelayed(1, 200);
                }
                EventBus.getDefault().post(new CollectionManager.UpdatedEvent());
                Log.d(TAG, "Scanning process finished in " + (System.currentTimeMillis() - time)
                        + "ms");
            }
        }

        private void processMediaWrappers(List<MediaWrapper> mws) {
            Log.d(TAG, "Processing " + mws.size() + " media items...");
            Map<String, Set<String>> albumArtistsMap = new HashMap<>();
            for (MediaWrapper mw : mws) {
                if (mw.getType() == MediaWrapper.TYPE_AUDIO) {
                    String albumKey = mw.getAlbum() != null ? mw.getAlbum().toLowerCase() : "";
                    if (albumArtistsMap.get(albumKey) == null) {
                        albumArtistsMap.put(albumKey, new HashSet<String>());
                    }
                    albumArtistsMap.get(albumKey).add(mw.getArtist());
                }
            }
            List<ScriptResolverTrack> tracks = new ArrayList<>();
            for (MediaWrapper mw : mws) {
                if (mw.getType() == MediaWrapper.TYPE_AUDIO) {
                    ScriptResolverTrack track = new ScriptResolverTrack();
                    track.album = mw.getAlbum();
                    track.albumArtist = mw.getAlbumArtist();
                    track.track = mw.getTitle();
                    track.artist = mw.getArtist();
                    track.duration = mw.getLength() / 1000;
                    track.albumpos = mw.getTrackNumber();
                    track.url = mw.getLocation();
                    track.imagePath = mw.getArtworkURL();
                    track.lastModified = mw.getLastModified();
                    tracks.add(track);
                }
            }
            CollectionDb db = CollectionDbManager.get().getCollectionDb(getId());
            db.wipe();
            db.addTracks(tracks);
            Log.d(TAG, "Processed " + mws.size() + " media items. " + tracks.size()
                    + " tracks have been added to the UserCollection.");
        }
    }

    public ConcurrentHashMap<Query, Long> getQueryTimeStamps() {
        return mQueryTimeStamps;
    }

    public ConcurrentHashMap<Artist, Long> getArtistTimeStamps() {
        return mArtistTimeStamps;
    }

    public ConcurrentHashMap<Album, Long> getAlbumTimeStamps() {
        return mAlbumTimeStamps;
    }

    private final RestartHandler mRestartHandler = new RestartHandler(this);

    private static class RestartHandler extends WeakReferenceHandler<UserCollection> {

        public RestartHandler(UserCollection userCollection) {
            super(Looper.getMainLooper(), userCollection);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                getReferencedObject().loadMediaItems();
            }
        }
    }

    /**
     * Filters all irrelevant files
     */
    private static class MediaItemFilter implements FileFilter {

        @Override
        public boolean accept(File f) {
            boolean accepted = false;
            if (!f.isHidden()) {
                if (f.isDirectory()
                        && !FOLDER_BLACKLIST.contains(f.getPath().toLowerCase(Locale.ENGLISH))) {
                    accepted = true;
                } else {
                    String fileName = f.getName().toLowerCase(Locale.ENGLISH);
                    int dotIndex = fileName.lastIndexOf(".");
                    if (dotIndex != -1) {
                        String fileExt = fileName.substring(dotIndex);
                        accepted = Extensions.AUDIO.contains(fileExt);
                    }
                }
            }
            return accepted;
        }
    }

    public static ArrayList<String> getStorageDirectories() {
        BufferedReader bufReader = null;
        ArrayList<String> list = new ArrayList<>();
        list.add(Environment.getExternalStorageDirectory().getPath());

        try {
            bufReader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            while ((line = bufReader.readLine()) != null) {

                StringTokenizer tokens = new StringTokenizer(line, " ");
                String device = tokens.nextToken();
                String mountpoint = tokens.nextToken();
                String type = tokens.nextToken();

                // skip if already in list or if type/mountpoint is blacklisted
                if (list.contains(mountpoint) || TYPE_BLACKLIST.contains(type)
                        || doStringsStartWith(MOUNT_BLACKLIST, mountpoint)) {
                    continue;
                }

                // check that device is in whitelist, and either type or mountpoint is in a whitelist
                if (doStringsStartWith(DEVICE_WHITELIST, device) && (TYPE_WHITELIST.contains(type)
                        || doStringsStartWith(MOUNT_WHITELIST, mountpoint))) {
                    list.add(mountpoint);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "getStorageDirectories: " + e.getClass() + ": " + e.getLocalizedMessage());
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "getStorageDirectories: " + e.getClass() + ": "
                            + e.getLocalizedMessage());
                }
            }
        }
        return list;
    }

    private static boolean doStringsStartWith(String[] array, String text) {
        for (String item : array) {
            if (text.startsWith(item)) {
                return true;
            }
        }
        return false;
    }

    public void addLovedArtists(List<Artist> artists, List<Long> lastModifieds) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        db.addArtists(artists, lastModifieds);
    }

    public void removeLoved(Artist artist) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        db.remove(artist);
    }


    public boolean isLoved(Artist artist) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        return db.isLoved(artist);
    }


    public void addLovedAlbums(List<Album> albums, List<Long> lastModifieds) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        db.addAlbums(albums, lastModifieds);
    }

    public void removeLoved(Album album) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        db.remove(album);
    }


    public boolean isLoved(Album album) {
        UserCollectionDb db = (UserCollectionDb) CollectionDbManager.get().getCollectionDb(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        return db.isLoved(album);
    }
}
