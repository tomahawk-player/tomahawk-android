package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.resolver.Query;

import java.util.ArrayList;

/**
 * This interface represents an item displayed in our {@link org.tomahawk.libtomahawk.collection.Collection}
 * list.
 */
public interface TomahawkListItem {

    /**
     * @return the corresponding name/title
     */
    public String getName();

    /**
     * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Artist}
     */
    public Artist getArtist();

    /**
     * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Album}
     */
    public Album getAlbum();

    /**
     * @return the corresponding list of {@link org.tomahawk.libtomahawk.resolver.Query}s
     */
    public ArrayList<Query> getQueries(boolean onlyLocal);

    /**
     * @return the corresponding list of {@link org.tomahawk.libtomahawk.resolver.Query}s
     */
    public ArrayList<Query> getQueries();
}