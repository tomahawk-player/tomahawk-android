/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.resolver;

/**
 * The basic {@link Resolver} interface, which is implemented by every type of {@link Resolver}
 */
public interface Resolver {

    /**
     * @return Whether or not this {@link Resolver} is ready
     */
    public boolean isReady();

    /**
     * @return Whether or not this {@link Resolver} is enabled
     */
    public boolean isEnabled();

    /**
     * @return Whether or not this {@link Resolver} is currently resolving
     */
    public boolean isResolving();

    /**
     * @return the path to the icon of this {@link Resolver}
     */
    public String getIconPath();

    /**
     * @return the name of this resolver's collection
     */
    public String getCollectionName();

    /**
     * @return the resource id of the icon of this {@link Resolver}
     */
    public int getIconResId();

    /**
     * Resolve the given {@link Query}
     *
     * @return whether or not the Resolver is ready to resolve
     */
    public boolean resolve(Query query);

    /**
     * @return this {@link Resolver}'s id
     */
    public String getId();

    /**
     * @return this {@link Resolver}'s weight
     */
    public int getWeight();
}
