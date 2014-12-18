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
public abstract class Resolver {

    public static final String ENABLED_STATE_CHANGED
            = "org.tomahawk.tomahawk_android.enabled_state_changed";

    public interface OnResolverReadyListener {

        public void onResolverReady(Resolver resolver);
    }

    private String mPrettyName;

    private OnResolverReadyListener mOnResolverReadyListener;

    public Resolver(String prettyName, OnResolverReadyListener onResolverReadyListener) {
        mPrettyName = prettyName;
        mOnResolverReadyListener = onResolverReadyListener;
    }

    protected void onResolverReady() {
        mOnResolverReadyListener.onResolverReady(this);
    }

    /**
     * @return Whether or not this {@link Resolver} is ready
     */
    public abstract boolean isReady();

    /**
     * @return Whether or not this {@link Resolver} is enabled
     */
    public abstract boolean isEnabled();

    /**
     * @return Whether or not this {@link Resolver} is currently resolving
     */
    public abstract boolean isResolving();

    /**
     * Load this resolver's icon into the given ImageView
     */
    public abstract void loadIcon(ImageView imageView, boolean grayOut);

    /**
     * Load this resolver's white icon into the given ImageView
     */
    public abstract void loadIconWhite(ImageView imageView);

    /**
     * Load this resolver's icon background into the given ImageView
     */
    public abstract void loadIconBackground(ImageView imageView, boolean grayOut);

    /**
     * @return the pretty name of this resolver
     */
    public String getPrettyName() {
        return mPrettyName;
    }

    /**
     * @return the name of this resolver's collection
     */
    public abstract String getCollectionName();

    /**
     * Resolve the given {@link Query}
     *
     * @return whether or not the Resolver is ready to resolve
     */
    public abstract boolean resolve(Query query);

    /**
     * @return this {@link Resolver}'s id
     */
    public abstract String getId();

    /**
     * @return this {@link Resolver}'s weight
     */
    public abstract int getWeight();
}
