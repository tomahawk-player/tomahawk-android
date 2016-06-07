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

import android.widget.ImageView;

/**
 * The basic {@link Resolver} interface, which is implemented by every type of {@link Resolver}
 */
public interface Resolver {

    /**
     * @return Whether or not this {@link Resolver} is ready
     */
    boolean isInitialized();

    /**
     * @return Whether or not this {@link Resolver} is enabled
     */
    boolean isEnabled();

    /**
     * @return Whether or not this {@link Resolver} is currently resolving
     */
    boolean isResolving();

    /**
     * Load this resolver's icon into the given ImageView
     */
    void loadIcon(ImageView imageView, boolean grayOut);

    /**
     * Load this resolver's white icon into the given ImageView
     */
    void loadIconWhite(ImageView imageView, int tintColorResId);

    /**
     * Load this resolver's icon background into the given ImageView
     */
    void loadIconBackground(ImageView imageView, boolean grayOut);

    /**
     * @return the pretty name of this resolver
     */
    String getPrettyName();

    /**
     * Resolve the given {@link Query}
     */
    void resolve(Query query);

    /**
     * @return this {@link Resolver}'s id
     */
    String getId();

    /**
     * @return this {@link Resolver}'s weight
     */
    int getWeight();
}
