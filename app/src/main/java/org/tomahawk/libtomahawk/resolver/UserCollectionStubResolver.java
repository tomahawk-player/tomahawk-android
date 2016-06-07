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

import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ColorTintTransformation;

import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;

/**
 * A stub {@link Resolver} that is associated with all local tracks.
 */
public class UserCollectionStubResolver implements Resolver {

    private static class Holder {

        private static final UserCollectionStubResolver instance = new UserCollectionStubResolver();

    }

    private UserCollectionStubResolver() {
    }

    public static UserCollectionStubResolver get() {
        return Holder.instance;
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isInitialized() {
        return false;
    }

    /**
     * @return whether or not this {@link Resolver} is currently resolving
     */
    @Override
    public boolean isResolving() {
        return false;
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hardware_smartphone,
                grayOut ? R.color.disabled_resolver : android.R.color.black);
    }

    @Override
    public void loadIconWhite(ImageView imageView, int tintColorResId) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hardware_smartphone, tintColorResId);
    }

    @Override
    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        imageView.setImageDrawable(new ColorDrawable(
                TomahawkApp.getContext().getResources()
                        .getColor(R.color.local_collection_resolver_bg)));
        if (grayOut) {
            imageView.setColorFilter(ColorTintTransformation.getColorFilter(
                    R.color.disabled_resolver));
        } else {
            imageView.clearColorFilter();
        }
    }

    @Override
    public String getPrettyName() {
        return TomahawkApp.getContext().getString(R.string.local_collection_pretty_name);
    }

    /**
     * Resolve the given {@link Query}.
     *
     * @param queryToSearchFor the {@link Query} which should be resolved
     */
    @Override
    public void resolve(final Query queryToSearchFor) {
    }

    /**
     * @return this {@link UserCollectionStubResolver}'s id
     */
    @Override
    public String getId() {
        return TomahawkApp.PLUGINNAME_USERCOLLECTION;
    }

    /**
     * @return this {@link UserCollectionStubResolver}'s weight
     */
    @Override
    public int getWeight() {
        return 110;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
