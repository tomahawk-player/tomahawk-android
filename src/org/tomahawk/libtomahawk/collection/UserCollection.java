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

import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.MediaWithDate;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class represents a user's local {@link UserCollection}.
 */
public class UserCollection extends Collection {

    private static final String TAG = UserCollection.class.getSimpleName();

    private static final int MODE_ARTIST = 0;

    private static final int MODE_ALBUM = 1;

    private static final int MODE_GENRE = 2;

    private final ArrayList<MediaWithDate> mItemList;

    private final ReadWriteLock mItemListLock;

    private boolean isStopping = false;

    private boolean mRestart = false;

    protected Thread mLoadingThread;

    public UserCollection() {
        super(TomahawkApp.PLUGINNAME_USERCOLLECTION,
                PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION)
                        .getCollectionName(), true);

        mItemList = new ArrayList<MediaWithDate>();
        mItemListLock = new ReentrantReadWriteLock();
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s in this {@link Collection}
     */
    @Override
    public ArrayList<Query> getQueries(boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWithDate media : getAudioItems()) {
            Artist artist = Artist.get(media.getArtist());
            Album album = Album.get(media.getAlbum(), artist);
            if (!TextUtils.isEmpty(media.getArtworkURL())) {
                album.setImage(Image.get(media.getArtworkURL(), false));
            }
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            if (media.getTrackNumber() != null) {
                track.setAlbumPos(Integer.valueOf(media.getTrackNumber()));
            }
            Query query = Query.get(media.getTitle(), media.getAlbum(), media.getArtist(), true);
            Result result = Result.get(media.getLocation(), track, userCollectionResolver,
                    query.getCacheKey());
            result.setTrackScore(1f);
            query.addTrackResult(result);
            queries.add(query);
            if (mAlbumAddedTimeStamps.get(query.getAlbum().getName()) == null
                    || mAlbumAddedTimeStamps.get(query.getAlbum().getName()) < media
                    .getDateAdded()) {
                mAlbumAddedTimeStamps.put(query.getAlbum().getName(), media.getDateAdded());
            }
            if (mArtistAddedTimeStamps.get(query.getArtist().getName()) == null
                    || mArtistAddedTimeStamps.get(query.getArtist().getName()) < media
                    .getDateAdded()) {
                mArtistAddedTimeStamps.put(query.getArtist().getName(), media.getDateAdded());
            }
            mTrackAddedTimeStamps.put(query, media.getDateAdded());
        }
        if (sorted) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    @Override
    public ArrayList<Artist> getArtists(boolean sorted) {
        HashMap<String, Artist> artistMap = new HashMap<String, Artist>();
        for (MediaWithDate media : getAudioItems()) {
            if (!artistMap.containsKey(media.getArtist().toLowerCase())) {
                Artist artist = Artist.get(media.getArtist());
                artistMap.put(media.getArtist().toLowerCase(), artist);
            }
            if (mArtistAddedTimeStamps.get(media.getArtist().toLowerCase()) == null
                    || mArtistAddedTimeStamps.get(media.getArtist().toLowerCase()) < media
                    .getDateAdded()) {
                mArtistAddedTimeStamps.put(media.getArtist().toLowerCase(), media.getDateAdded());
            }
        }
        ArrayList<Artist> artists = new ArrayList<Artist>(artistMap.values());
        if (sorted) {
            Collections.sort(artists,
                    new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        }
        return artists;
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    @Override
    public ArrayList<Album> getAlbums(boolean sorted) {
        HashMap<String, Album> albumMap = new HashMap<String, Album>();
        for (MediaWithDate media : getAudioItems()) {
            if (!albumMap.containsKey(media.getAlbum().toLowerCase())) {
                Artist artist = Artist.get(media.getArtist());
                Album album = Album.get(media.getAlbum(), artist);
                if (!TextUtils.isEmpty(media.getArtworkURL())) {
                    album.setImage(Image.get(media.getArtworkURL(), false));
                }
                albumMap.put(media.getAlbum().toLowerCase(), album);
            }
            if (mAlbumAddedTimeStamps.get(media.getAlbum().toLowerCase()) == null
                    || mAlbumAddedTimeStamps.get(media.getAlbum().toLowerCase()) < media
                    .getDateAdded()) {
                mAlbumAddedTimeStamps.put(media.getAlbum().toLowerCase(), media.getDateAdded());
            }
        }
        ArrayList<Album> albums = new ArrayList<Album>(albumMap.values());
        if (sorted) {
            Collections.sort(albums,
                    new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        }
        return albums;
    }

    /**
     * @return A {@link java.util.List} of all {@link Album}s by the given Artist.
     */
    @Override
    public ArrayList<Album> getArtistAlbums(Artist artist, boolean sorted) {
        HashMap<String, Album> albumMap = new HashMap<String, Album>();
        for (MediaWithDate media : getAudioItems(artist.getName(), null, MODE_ARTIST)) {
            if (!albumMap.containsKey(media.getAlbum().toLowerCase())) {
                Album album = Album.get(media.getAlbum(), artist);
                if (!TextUtils.isEmpty(media.getArtworkURL())) {
                    album.setImage(Image.get(media.getArtworkURL(), false));
                }
                albumMap.put(media.getAlbum().toLowerCase(), album);
            }
        }
        ArrayList<Album> albums = new ArrayList<Album>(albumMap.values());
        if (sorted) {
            Collections.sort(albums, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return albums;
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Artist.
     */
    @Override
    public ArrayList<Query> getArtistTracks(Artist artist, boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWithDate media : getAudioItems(artist.getName(), null, MODE_ARTIST)) {
            Album album = Album.get(media.getAlbum(), artist);
            if (!TextUtils.isEmpty(media.getArtworkURL())) {
                album.setImage(Image.get(media.getArtworkURL(), false));
            }
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            if (media.getTrackNumber() != null) {
                track.setAlbumPos(Integer.valueOf(media.getTrackNumber()));
            }
            Query query = Query.get(media.getTitle(), media.getAlbum(), media.getArtist(), true);
            Result result = Result.get(media.getLocation(), track, userCollectionResolver,
                    query.getCacheKey());
            result.setTrackScore(1f);
            query.addTrackResult(result);
            queries.add(query);
        }
        if (sorted) {
            Collections
                    .sort(queries, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Album.
     */
    @Override
    public ArrayList<Query> getAlbumTracks(Album album, boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWithDate media : getAudioItems(album.getName(), null, MODE_ALBUM)) {
            Artist artist = Artist.get(media.getArtist());
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            if (media.getTrackNumber() != null) {
                track.setAlbumPos(Integer.valueOf(media.getTrackNumber()));
            }
            Query query = Query.get(media.getTitle(), media.getAlbum(), media.getArtist(), true);
            Result result = Result.get(media.getLocation(), track, userCollectionResolver,
                    query.getCacheKey());
            result.setTrackScore(1f);
            query.addTrackResult(result);
            queries.add(query);
        }
        if (sorted) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        }
        return queries;
    }

    public void loadMediaItems(boolean restart) {
        if (restart && isWorking()) {
            /* do a clean restart if a scan is ongoing */
            mRestart = true;
            isStopping = true;
        } else {
            loadMediaItems();
        }
    }

    public void loadMediaItems() {
        if (mLoadingThread == null || mLoadingThread.getState() == Thread.State.TERMINATED) {
            isStopping = false;
            mLoadingThread = new Thread(new GetMediaItemsRunnable());
            mLoadingThread.start();
        }
    }

    public void stop() {
        isStopping = true;
    }

    public boolean isWorking() {
        return mLoadingThread != null &&
                mLoadingThread.isAlive() &&
                mLoadingThread.getState() != Thread.State.TERMINATED &&
                mLoadingThread.getState() != Thread.State.NEW;
    }

    public ArrayList<MediaWithDate> getAudioItems() {
        ArrayList<MediaWithDate> audioItems = new ArrayList<MediaWithDate>();
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWithDate item = mItemList.get(i);
            if (item.getType() == Media.TYPE_AUDIO) {
                audioItems.add(item);
            }
        }
        mItemListLock.readLock().unlock();
        return audioItems;
    }

    public ArrayList<MediaWithDate> getAudioItems(String name, String name2, int mode) {
        ArrayList<MediaWithDate> audioItems = new ArrayList<MediaWithDate>();
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWithDate item = mItemList.get(i);
            if (item.getType() == Media.TYPE_AUDIO) {

                boolean valid = false;
                switch (mode) {
                    case MODE_ARTIST:
                        valid = name.equalsIgnoreCase(item.getArtist()) && (name2 == null || name2
                                .equalsIgnoreCase(item.getAlbum()));
                        break;
                    case MODE_ALBUM:
                        valid = name.equalsIgnoreCase(item.getAlbum());
                        break;
                    case MODE_GENRE:
                        valid = name.equalsIgnoreCase(item.getGenre()) && (name2 == null || name2
                                .equals(item.getAlbum()));
                        break;
                    default:
                        break;
                }
                if (valid) {
                    audioItems.add(item);
                }

            }
        }
        mItemListLock.readLock().unlock();
        return audioItems;
    }

    public ArrayList<MediaWithDate> getMediaItems() {
        return mItemList;
    }

    public MediaWithDate getMediaItem(String location) {
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWithDate item = mItemList.get(i);
            if (item.getLocation().equals(location)) {
                mItemListLock.readLock().unlock();
                return item;
            }
        }
        mItemListLock.readLock().unlock();
        return null;
    }

    public ArrayList<MediaWithDate> getMediaItems(List<String> pathList) {
        ArrayList<MediaWithDate> items = new ArrayList<MediaWithDate>();
        for (int i = 0; i < pathList.size(); i++) {
            MediaWithDate item = getMediaItem(pathList.get(i));
            items.add(item);
        }
        return items;
    }

    private class GetMediaItemsRunnable implements Runnable {

        private final Stack<File> directories = new Stack<File>();

        private final HashSet<String> directoriesScanned = new HashSet<String>();

        public GetMediaItemsRunnable() {
        }

        @Override
        public void run() {
            // Use all available storage directories as our default
            ArrayList<String> storageDirs = new ArrayList<String>();
            storageDirs.addAll(Arrays.asList(getStorageDirectories()));
            storageDirs.addAll(Arrays.asList(getCustomDirectories()));
            List<File> mediaDirs = new ArrayList<File>();
            for (String dir : storageDirs) {
                File f = new File(dir);
                if (f.exists()) {
                    mediaDirs.add(f);
                }
            }
            directories.addAll(mediaDirs);

            // get all existing media items
            HashMap<String, MediaWithDate> existingMedias = DatabaseHelper.getInstance()
                    .getMedias();

            // list of all added files
            HashSet<String> addedLocations = new HashSet<String>();

            // clear all old items
            mItemListLock.writeLock().lock();
            mItemList.clear();
            mItemListLock.writeLock().unlock();

            MediaItemFilter mediaFileFilter = new MediaItemFilter();

            ArrayList<File> mediaToScan = new ArrayList<File>();
            try {
                // Count total files, and stack them
                while (!directories.isEmpty()) {
                    File dir = directories.pop();
                    String dirPath = dir.getAbsolutePath();

                    // Skip some system folders
                    if (dirPath.startsWith("/proc/") || dirPath.startsWith("/sys/") || dirPath
                            .startsWith("/dev/")) {
                        continue;
                    }

                    // Do not scan again if same canonical path
                    try {
                        dirPath = dir.getCanonicalPath();
                    } catch (IOException e) {
                        e.printStackTrace();
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
                        File[] f;
                        if ((f = dir.listFiles(mediaFileFilter)) != null) {
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
                        continue;
                    }

                    if (isStopping) {
                        Log.d(TAG, "Stopping scan");
                        return;
                    }
                }

                // Process the stacked items
                for (File file : mediaToScan) {
                    String fileURI = LibVLC.PathToURI(file.getPath());
                    if (existingMedias.containsKey(fileURI)) {
                        /**
                         * only add file if it is not already in the list. eg. if
                         * user select an subfolder as well
                         */
                        if (!addedLocations.contains(fileURI)) {
                            mItemListLock.writeLock().lock();
                            // get existing media item from database
                            mItemList.add(existingMedias.get(fileURI));
                            mItemListLock.writeLock().unlock();
                            addedLocations.add(fileURI);
                        }
                    } else {
                        mItemListLock.writeLock().lock();
                        // create new media item
                        MediaWithDate m = new MediaWithDate(VLCMediaPlayer.getLibVlcInstance(),
                                fileURI, file.lastModified());
                        mItemList.add(m);
                        // Add this item to database
                        DatabaseHelper.getInstance().addMedia(m);
                        mItemListLock.writeLock().unlock();
                    }
                    if (isStopping) {
                        Log.d(TAG, "Stopping scan");
                        return;
                    }
                }
            } finally {
                // remove old files & folders from database if storage is mounted
                if (!isStopping && Environment.getExternalStorageState()
                        .equals(Environment.MEDIA_MOUNTED)) {
                    for (String fileURI : addedLocations) {
                        existingMedias.remove(fileURI);
                    }
                    DatabaseHelper.getInstance().removeMedias(existingMedias.keySet());
                }

                if (mRestart) {
                    Log.d(TAG, "Restarting scan");
                    mRestart = false;
                    restartHandler.sendEmptyMessageDelayed(1, 200);
                }
                CollectionManager.sendCollectionUpdatedBroadcast(null, null);
            }
        }
    }

    private Handler restartHandler = new RestartHandler(this);

    private static class RestartHandler extends Handler {

        private WeakReference<UserCollection> mOwner;

        public RestartHandler(UserCollection owner) {
            super(Looper.getMainLooper());

            mOwner = new WeakReference<UserCollection>(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mOwner == null) {
                return;
            }
            mOwner.get().loadMediaItems();
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
                if (f.isDirectory() && !Media.FOLDER_BLACKLIST.contains(f.getPath().toLowerCase(
                        Locale.ENGLISH))) {
                    accepted = true;
                } else {
                    String fileName = f.getName().toLowerCase(Locale.ENGLISH);
                    int dotIndex = fileName.lastIndexOf(".");
                    if (dotIndex != -1) {
                        String fileExt = fileName.substring(dotIndex);
                        accepted = Media.AUDIO_EXTENSIONS.contains(fileExt) ||
                                Media.VIDEO_EXTENSIONS.contains(fileExt);
                    }
                }
            }
            return accepted;
        }
    }

    public static String[] getStorageDirectories() {
        String[] dirs = null;
        BufferedReader bufReader = null;
        ArrayList<String> list = new ArrayList<String>();
        list.add(Environment.getExternalStorageDirectory().getPath());

        List<String> typeWL = Arrays.asList("vfat", "exfat", "sdcardfs", "fuse");
        List<String> typeBL = Arrays.asList("tmpfs");
        String[] mountWL = {"/mnt", "/Removable"};
        String[] mountBL = {
                "/mnt/secure",
                "/mnt/shell",
                "/mnt/asec",
                "/mnt/obb",
                "/mnt/media_rw/extSdCard",
                "/mnt/media_rw/sdcard",
                "/storage/emulated"};
        String[] deviceWL = {
                "/dev/block/vold",
                "/dev/fuse",
                "/mnt/media_rw/extSdCard"};

        try {
            bufReader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            while ((line = bufReader.readLine()) != null) {

                StringTokenizer tokens = new StringTokenizer(line, " ");
                String device = tokens.nextToken();
                String mountpoint = tokens.nextToken();
                String type = tokens.nextToken();

                // skip if already in list or if type/mountpoint is blacklisted
                if (list.contains(mountpoint) || typeBL.contains(type)
                        || doStringsStartWith(mountBL, mountpoint)) {
                    continue;
                }

                // check that device is in whitelist, and either type or mountpoint is in a whitelist
                if (doStringsStartWith(deviceWL, device) && (typeWL.contains(type)
                        || doStringsStartWith(mountWL, mountpoint))) {
                    list.add(mountpoint);
                }
            }

            dirs = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                dirs[i] = list.get(i);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getStorageDirectories: " + e.getClass() + ": " + e.getLocalizedMessage());
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
        return dirs;
    }

    private static boolean doStringsStartWith(String[] array, String text) {
        for (String item : array) {
            if (text.startsWith(item)) {
                return true;
            }
        }
        return false;
    }

    public static void addCustomDirectory(String path) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());

        ArrayList<String> dirs = new ArrayList<String>(
                Arrays.asList(getCustomDirectories()));
        dirs.add(path);
        StringBuilder builder = new StringBuilder();
        builder.append(dirs.remove(0));
        for (String s : dirs) {
            builder.append(":");
            builder.append(s);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("custom_paths", builder.toString());
        editor.commit();
    }

    public static void removeCustomDirectory(String path) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        if (!preferences.getString("custom_paths", "").contains(path)) {
            return;
        }
        ArrayList<String> dirs = new ArrayList<String>(
                Arrays.asList(preferences.getString("custom_paths", "").split(
                        ":")));
        dirs.remove(path);
        String custom_path;
        if (dirs.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(dirs.remove(0));
            for (String s : dirs) {
                builder.append(":");
                builder.append(s);
            }
            custom_path = builder.toString();
        } else { // don't do unneeded extra work
            custom_path = "";
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("custom_paths", custom_path);
        editor.commit();
    }

    public static String[] getCustomDirectories() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        final String custom_paths = preferences.getString("custom_paths", "");
        if (custom_paths.equals("")) {
            return new String[0];
        } else {
            return custom_paths.split(":");
        }
    }
}
