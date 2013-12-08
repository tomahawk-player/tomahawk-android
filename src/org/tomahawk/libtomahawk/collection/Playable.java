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

import org.tomahawk.libtomahawk.resolver.Query;

import java.util.Collection;

/**
 * A simple interface to represent a collection of 0 or more {@link Query}s.
 */
public interface Playable {

    /**
     * Set the {@link Query}s for this {@link Playable}Interface.
     */
    public void setQueries(Collection<Query> queries);

    /**
     * Set the current {@link Query} to {@link Query}. If {@link Query} cannot be found the current
     * {@link Query} stays the same.
     */
    public void setCurrentQuery(Query track);

    /**
     * Return the current {@link Query} for this {@link Playable}Interface.
     */
    public Query getCurrentQuery();

    /**
     * Return the next {@link Query} for this {@link Playable}Interface.
     */
    public Query getNextQuery();

    /**
     * Return the previous {@link Query} for this {@link Playable}Interface.
     */
    public Query getPreviousQuery();

    /**
     * Return the {@link Query} at pos i.
     */
    public Query getQueryAtPos(int i);

    /**
     * Return the first {@link Query} in this {@link Playable}Interface.
     */
    public Query getFirstQuery();

    /**
     * Return the last {@link Query} in this {@link Playable}Interface.
     */
    public Query getLastQuery();

    /**
     * Returns true if the {@link Playable}Interface has a next {@link Query}.
     */
    public boolean hasNextQuery();

    /**
     * Returns true if the {@link Playable}Interface has a previous {@link Query}.
     */
    public boolean hasPreviousQuery();
}
