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
package org.tomahawk.libtomahawk.collection;

import java.util.Collection;

/**
 * A simple interface to represent a collection of 0 or more {@link Track}s.
 */
public interface Playable {

    /**
     * Set the {@link Track}s for this {@link Playable}Interface.
     */
    public void setTracks(Collection<Track> tracks);

    /**
     * Set the current {@link Track} to {@link Track}. If {@link Track} cannot be found the current
     * {@link Track} stays the same.
     */
    public void setCurrentTrack(Track track);

    /**
     * Return the current {@link Track} for this {@link Playable}Interface.
     */
    public Track getCurrentTrack();

    /**
     * Return the next {@link Track} for this {@link Playable}Interface.
     */
    public Track getNextTrack();

    /**
     * Return the previous {@link Track} for this {@link Playable}Interface.
     */
    public Track getPreviousTrack();

    /**
     * Return the {@link Track} at pos i.
     */
    public Track getTrackAtPos(int i);

    /**
     * Return the first {@link Track} in this {@link Playable}Interface.
     */
    public Track getFirstTrack();

    /**
     * Return the last {@link Track} in this {@link Playable}Interface.
     */
    public Track getLastTrack();

    /**
     * Returns true if the {@link Playable}Interface has a next {@link Track}.
     */
    public boolean hasNextTrack();

    /**
     * Returns true if the {@link Playable}Interface has a previous {@link Track}.
     */
    public boolean hasPreviousTrack();
}
