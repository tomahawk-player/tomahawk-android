/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.audio.test;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.collection.AlbumPlaylist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.Intent;
import android.test.ServiceTestCase;

public class PlaybackServiceTest extends ServiceTestCase<PlaybackService> {

    private Track tstTrack;

    public PlaybackServiceTest() {
        super(PlaybackService.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        setupService();

        Collection coll = new LocalCollection(getContext());
        tstTrack = coll.getTracks().get(0);
        Intent startIntent = new Intent(getContext(), PlaybackService.class);

        Assert.assertNotNull(tstTrack);
        startIntent.putExtra(PlaybackFragment.PLAYLIST_EXTRA,
                AlbumPlaylist.fromAlbum(tstTrack.getAlbum(), tstTrack));
        startService(startIntent);
    }

    public void tearDown() {
        shutdownService();
    }

    public void testStartable() {
        Assert.assertTrue(getService().isRunning());
    }

    public void testGetTrack() {
        Assert.assertEquals(tstTrack, getPlaybackService().getCurrentTrack());
    }

    public void testPlayPause() {
        /*
         * I believe this fails because the media player is prepared async.
         */
        // PlaybackService service = getPlaybackService();
        // Assert.assertTrue(getPlaybackService().isPlaying());
    }

    private PlaybackService getPlaybackService() {
        return (PlaybackService) getService();
    }
}
