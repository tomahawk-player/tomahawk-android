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
import org.tomahawk.tomahawk_android.utils.MediaWrapper;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.Extensions;

import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.greenrobot.event.EventBus;

/**
 * This class represents a user's local {@link UserCollection}.
 */
public class UserCollection extends Collection {

    private static final String TAG = UserCollection.class.getSimpleName();

    private static final String HAS_SET_DEFAULTDIRS
            = "org.tomahawk.tomahawk_android.has_set_defaultdirs";

    private static final int MODE_ARTIST = 0;

    private static final int MODE_ALBUM = 1;

    private static final int MODE_GENRE = 2;

    private final ArrayList<MediaWrapper> mItemList;

    private final ReadWriteLock mItemListLock;

    private boolean isStopping = false;

    private boolean mRestart = false;

    protected Thread mLoadingThread;

    public final static HashSet<String> FOLDER_BLACKLIST;

    static {
        final String[] folder_blacklist = {
                "/alarms",
                "/notifications",
                "/ringtones",
                "/media/alarms",
                "/media/notifications",
                "/media/ringtones",
                "/media/audio/alarms",
                "/media/audio/notifications",
                "/media/audio/ringtones",
                "/Android/data/"};

        FOLDER_BLACKLIST = new HashSet<String>();
        for (String item : folder_blacklist) {
            FOLDER_BLACKLIST
                    .add(android.os.Environment.getExternalStorageDirectory().getPath() + item);
        }
    }

    public UserCollection() {
        super(TomahawkApp.PLUGINNAME_USERCOLLECTION,
                PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION)
                        .getCollectionName(), true);

        mItemList = new ArrayList<>();
        mItemListLock = new ReentrantReadWriteLock();
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s in this {@link Collection}
     */
    @Override
    public ArrayList<Query> getQueries(boolean sorted) {
        ArrayList<Query> queries = new ArrayList<>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWrapper media : getAudioItems()) {
            Artist artist = Artist.get(media.getArtist());
            Album album = Album.get(media.getAlbum(), artist);
            if (!TextUtils.isEmpty(media.getArtworkURL())) {
                album.setImage(Image.get(media.getArtworkURL(), false));
            }
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            track.setAlbumPos(media.getTrackNumber());
            Query query = Query.get(media.getTitle(), media.getAlbum(), media.getArtist(), true);
            Result result = Result.get(media.getLocation(), track, userCollectionResolver,
                    query.getCacheKey());
            result.setTrackScore(1f);
            query.addTrackResult(result);
            queries.add(query);
            if (mAlbumAddedTimeStamps.get(query.getAlbum().getName()) == null
                    || mAlbumAddedTimeStamps.get(query.getAlbum().getName()) < media
                    .getLastModified()) {
                mAlbumAddedTimeStamps.put(query.getAlbum().getName(), media.getLastModified());
            }
            if (mArtistAddedTimeStamps.get(query.getArtist().getName()) == null
                    || mArtistAddedTimeStamps.get(query.getArtist().getName()) < media
                    .getLastModified()) {
                mArtistAddedTimeStamps.put(query.getArtist().getName(), media.getLastModified());
            }
            mTrackAddedTimeStamps.put(query, media.getLastModified());
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
        HashMap<String, Artist> artistMap = new HashMap<>();
        for (MediaWrapper media : getAudioItems()) {
            if (media.getArtist() != null) {
                if (!artistMap.containsKey(media.getArtist().toLowerCase())) {
                    Artist artist = Artist.get(media.getArtist());
                    artistMap.put(media.getArtist().toLowerCase(), artist);
                }
                if (mArtistAddedTimeStamps.get(media.getArtist().toLowerCase()) == null
                        || mArtistAddedTimeStamps.get(media.getArtist().toLowerCase()) < media
                        .getLastModified()) {
                    mArtistAddedTimeStamps
                            .put(media.getArtist().toLowerCase(), media.getLastModified());
                }
            }
        }
        ArrayList<Artist> artists = new ArrayList<>(artistMap.values());
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
        HashMap<String, Album> albumMap = new HashMap<>();
        for (MediaWrapper media : getAudioItems()) {
            if (media.getAlbum() != null) {
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
                        .getLastModified()) {
                    mAlbumAddedTimeStamps
                            .put(media.getAlbum().toLowerCase(), media.getLastModified());
                }
            }
        }
        ArrayList<Album> albums = new ArrayList<>(albumMap.values());
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
        HashMap<String, Album> albumMap = new HashMap<>();
        for (MediaWrapper media : getAudioItems(artist.getName(), null, MODE_ARTIST)) {
            if (media.getAlbum() != null && !albumMap.containsKey(media.getAlbum().toLowerCase())) {
                Album album = Album.get(media.getAlbum(), artist);
                if (!TextUtils.isEmpty(media.getArtworkURL())) {
                    album.setImage(Image.get(media.getArtworkURL(), false));
                }
                albumMap.put(media.getAlbum().toLowerCase(), album);
            }
        }
        ArrayList<Album> albums = new ArrayList<>(albumMap.values());
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
        ArrayList<Query> queries = new ArrayList<>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWrapper media : getAudioItems(artist.getName(), null, MODE_ARTIST)) {
            Album album = Album.get(media.getAlbum(), artist);
            if (!TextUtils.isEmpty(media.getArtworkURL())) {
                album.setImage(Image.get(media.getArtworkURL(), false));
            }
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            track.setAlbumPos(media.getTrackNumber());
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
        ArrayList<Query> queries = new ArrayList<>();
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return queries;
        }
        for (MediaWrapper media : getAudioItems(album.getName(), null, MODE_ALBUM)) {
            Artist artist = Artist.get(media.getArtist());
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            track.setAlbumPos(media.getTrackNumber());
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

    public boolean hasAudioItems() {
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWrapper item = mItemList.get(i);
            if (item.getType() == MediaWrapper.TYPE_AUDIO) {
                mItemListLock.readLock().unlock();
                return true;
            }
        }
        mItemListLock.readLock().unlock();
        return false;
    }

    public ArrayList<MediaWrapper> getAudioItems() {
        ArrayList<MediaWrapper> audioItems = new ArrayList<>();
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWrapper item = mItemList.get(i);
            if (item.getType() == MediaWrapper.TYPE_AUDIO) {
                audioItems.add(item);
            }
        }
        mItemListLock.readLock().unlock();
        return audioItems;
    }

    public ArrayList<MediaWrapper> getAudioItems(String name, String name2, int mode) {
        ArrayList<MediaWrapper> audioItems = new ArrayList<>();
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWrapper item = mItemList.get(i);
            if (item.getType() == MediaWrapper.TYPE_AUDIO) {

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

    public ArrayList<MediaWrapper> getMediaItems() {
        return mItemList;
    }

    public MediaWrapper getMediaItem(String location) {
        mItemListLock.readLock().lock();
        for (int i = 0; i < mItemList.size(); i++) {
            MediaWrapper item = mItemList.get(i);
            if (item.getLocation().equals(location)) {
                mItemListLock.readLock().unlock();
                return item;
            }
        }
        mItemListLock.readLock().unlock();
        return null;
    }

    public ArrayList<MediaWrapper> getMediaItems(List<String> pathList) {
        ArrayList<MediaWrapper> items = new ArrayList<>();
        for (int i = 0; i < pathList.size(); i++) {
            MediaWrapper item = getMediaItem(pathList.get(i));
            items.add(item);
        }
        return items;
    }

    private class GetMediaItemsRunnable implements Runnable {

        private final Stack<File> directories = new Stack<>();

        private final HashSet<String> directoriesScanned = new HashSet<>();

        public GetMediaItemsRunnable() {
        }

        @Override
        public void run() {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            Set<String> setDefaultDirs =
                    preferences.getStringSet(HAS_SET_DEFAULTDIRS, null);
            if (setDefaultDirs == null) {
                setDefaultDirs = new HashSet<>();
            }
            for (String defaultDir : getStorageDirectories()) {
                if (!setDefaultDirs.contains(defaultDir)) {
                    DatabaseHelper.getInstance().addMediaDir(defaultDir);
                    setDefaultDirs.add(defaultDir);
                }
            }
            preferences.edit().putStringSet(HAS_SET_DEFAULTDIRS, setDefaultDirs).commit();

            List<File> mediaDirs = DatabaseHelper.getInstance().getMediaDirs(false);
            directories.addAll(mediaDirs);

            // get all existing media items
            HashMap<String, MediaWrapper> existingMedias = DatabaseHelper.getInstance()
                    .getMedias();

            // list of all added files
            HashSet<String> addedLocations = new HashSet<>();

            // clear all old items
            mItemListLock.writeLock().lock();
            mItemList.clear();
            mItemListLock.writeLock().unlock();

            MediaItemFilter mediaFileFilter = new MediaItemFilter();

            ArrayList<File> mediaToScan = new ArrayList<>();
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
                        final Media media = new Media(
                                VLCMediaPlayer.getInstance().getLibVlcInstance(), fileURI);
                        media.parse();
                        media.release();
                        /* skip files with .mod extension and no duration */
                        if ((media.getDuration() == 0 || (media.getTrackCount() != 0 && TextUtils
                                .isEmpty(media.getTrack(0).codec))) &&
                                fileURI.endsWith(".mod")) {
                            mItemListLock.writeLock().unlock();
                            continue;
                        }
                        MediaWrapper mw = new MediaWrapper(media);
                        mw.setLastModified(file.lastModified());
                        mItemList.add(mw);
                        // Add this item to database
                        DatabaseHelper.getInstance().addMedia(mw);
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
                    mRestartHandler.sendEmptyMessageDelayed(1, 200);
                }
                EventBus.getDefault().post(new CollectionManager.UpdatedEvent());
            }
        }
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
                if (f.isDirectory() && !FOLDER_BLACKLIST
                        .contains(f.getPath().toLowerCase(Locale.ENGLISH))) {
                    accepted = true;
                } else {
                    String fileName = f.getName().toLowerCase(Locale.ENGLISH);
                    int dotIndex = fileName.lastIndexOf(".");
                    if (dotIndex != -1) {
                        String fileExt = fileName.substring(dotIndex);
                        accepted = Extensions.AUDIO.contains(fileExt) ||
                                Extensions.VIDEO.contains(fileExt) ||
                                Extensions.PLAYLIST.contains(fileExt);
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

        List<String> typeWL =
                Arrays.asList("vfat", "exfat", "sdcardfs", "fuse", "ntfs", "fat32", "ext3", "ext4",
                        "esdfs");
        List<String> typeBL = Arrays.asList("tmpfs");
        String[] mountWL = {"/mnt", "/Removable", "/storage"};
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
                "/mnt/media_rw"};

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
}
