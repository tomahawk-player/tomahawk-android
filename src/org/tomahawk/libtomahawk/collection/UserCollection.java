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
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.tomahawk_android.R;
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
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.greenrobot.event.EventBus;

/**
 * This class represents a user's local {@link UserCollection}.
 */
public class UserCollection extends NativeCollection {

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

        FOLDER_BLACKLIST = new HashSet<>();
        for (String item : folder_blacklist) {
            FOLDER_BLACKLIST
                    .add(android.os.Environment.getExternalStorageDirectory().getPath() + item);
        }
    }

    public UserCollection() {
        super(TomahawkApp.PLUGINNAME_USERCOLLECTION,
                TomahawkApp.getContext().getString(R.string.local_collection_pretty_name), true);

        mItemList = new ArrayList<>();
        mItemListLock = new ReentrantReadWriteLock();
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {

    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s in this {@link NativeCollection}
     */
    @Override
    public Deferred<Set<Query>, String, Object> getQueries(boolean sorted) {
        Set<Query> queries;
        if (sorted) {
            queries = new TreeSet<>(new QueryComparator(QueryComparator.COMPARE_ALPHA));
        } else {
            queries = new HashSet<>();
        }
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return new ADeferredObject<Set<Query>, String, Object>().resolve(queries);
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
            Result result = Result.get(media.getLocation(), track, userCollectionResolver);
            query.addTrackResult(result, 1f);
            queries.add(query);
            if (mAlbumTimeStamps.get(query.getAlbum()) == null
                    || mAlbumTimeStamps.get(query.getAlbum()) < media.getLastModified()) {
                mAlbumTimeStamps.put(query.getAlbum(), media.getLastModified());
            }
            if (mArtistTimeStamps.get(query.getArtist()) == null
                    || mArtistTimeStamps.get(query.getArtist()) < media.getLastModified()) {
                mArtistTimeStamps.put(query.getArtist(), media.getLastModified());
            }
            mQueryTimeStamps.put(query, media.getLastModified());
        }
        return new ADeferredObject<Set<Query>, String, Object>().resolve(queries);
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link NativeCollection}
     */
    @Override
    public Deferred<Set<Artist>, String, Object> getArtists(boolean sorted) {
        HashMap<String, Artist> artistMap = new HashMap<>();
        for (MediaWrapper media : getAudioItems()) {
            if (media.getArtist() != null) {
                if (!artistMap.containsKey(media.getArtist().toLowerCase())) {
                    Artist artist = Artist.get(media.getArtist());
                    artistMap.put(media.getArtist().toLowerCase(), artist);
                    if (mArtistTimeStamps.get(artist) == null
                            || mArtistTimeStamps.get(artist) < media.getLastModified()) {
                        mArtistTimeStamps.put(artist, media.getLastModified());
                    }
                }
            }
        }
        Set<Artist> artists;
        if (sorted) {
            artists = new TreeSet<>(new AlphaComparator());
            artists.addAll(artistMap.values());
        } else {
            artists = new HashSet<>(artistMap.values());
        }
        return new ADeferredObject<Set<Artist>, String, Object>().resolve(artists);
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link NativeCollection}
     */
    @Override
    public Deferred<Set<Album>, String, Object> getAlbums(boolean sorted) {
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
                    if (mAlbumTimeStamps.get(album) == null
                            || mAlbumTimeStamps.get(album) < media.getLastModified()) {
                        mAlbumTimeStamps.put(album, media.getLastModified());
                    }
                }
            }
        }
        Set<Album> albums;
        if (sorted) {
            albums = new TreeSet<>(new AlphaComparator());
            albums.addAll(albumMap.values());
        } else {
            albums = new HashSet<>(albumMap.values());
        }
        return new ADeferredObject<Set<Album>, String, Object>().resolve(albums);
    }

    /**
     * @return A {@link java.util.List} of all {@link Album}s by the given Artist.
     */
    @Override
    public Deferred<Set<Album>, String, Object> getArtistAlbums(Artist artist, boolean sorted) {
        HashMap<String, Album> albumMap = new HashMap<>();
        for (MediaWrapper media : getAudioItems(artist.getName(), null, MODE_ARTIST)) {
            String albumName = media.getAlbum() != null ? media.getAlbum().toLowerCase() : "";
            if (!albumMap.containsKey(albumName)) {
                Album album = Album.get(albumName, artist);
                if (!TextUtils.isEmpty(media.getArtworkURL())) {
                    album.setImage(Image.get(media.getArtworkURL(), false));
                }
                albumMap.put(albumName, album);
            }
        }
        Set<Album> albums;
        if (sorted) {
            albums = new TreeSet<>(new AlphaComparator());
            albums.addAll(albumMap.values());
        } else {
            albums = new HashSet<>(albumMap.values());
        }
        return new ADeferredObject<Set<Album>, String, Object>().resolve(albums);
    }

    @Override
    public Deferred<Boolean, String, Object> hasArtistAlbums(Artist artist) {
        return new ADeferredObject<Boolean, String, Object>().resolve(
                getAudioItems(artist.getName(), null, MODE_ARTIST).size() > 0);
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Album.
     */
    @Override
    public Deferred<Set<Query>, String, Object> getAlbumTracks(Album album, boolean sorted) {
        Set<Query> queries;
        if (sorted) {
            queries = new TreeSet<>(new QueryComparator(QueryComparator.COMPARE_ALPHA));
        } else {
            queries = new HashSet<>();
        }
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return new ADeferredObject<Set<Query>, String, Object>().resolve(
                    queries);
        }
        for (MediaWrapper media : getAudioItems(album.getName(), null, MODE_ALBUM)) {
            Artist artist = Artist.get(media.getArtist());
            Track track = Track.get(media.getTitle(), album, artist);
            track.setDuration(media.getLength());
            track.setAlbumPos(media.getTrackNumber());
            Query query = Query.get(media.getTitle(), media.getAlbum(), media.getArtist(), true);
            Result result = Result.get(media.getLocation(), track, userCollectionResolver);
            query.addTrackResult(result, 1f);
            queries.add(query);
        }
        return new ADeferredObject<Set<Query>, String, Object>().resolve(queries);
    }

    @Override
    public Deferred<Boolean, String, Object> hasAlbumTracks(Album album) {
        return new ADeferredObject<Boolean, String, Object>().resolve(
                getAudioItems(album.getName(), null, MODE_ALBUM).size() > 0);
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
