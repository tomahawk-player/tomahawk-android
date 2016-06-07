/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ColorTintTransformation;

import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;

public class HatchetStubResolver implements Resolver {

    private static class Holder {

        private static final HatchetStubResolver instance = new HatchetStubResolver();

    }

    private HatchetStubResolver() {
    }

    public static HatchetStubResolver get() {
        return Holder.instance;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean isResolving() {
        return false;
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hatchet, grayOut ? R.color.disabled_resolver : 0);
    }

    @Override
    public void loadIconWhite(ImageView imageView, int tintColorResId) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hatchet_white, tintColorResId);
    }

    @Override
    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        imageView.setImageDrawable(new ColorDrawable(
                TomahawkApp.getContext().getResources().getColor(R.color.hatchet_resolver_bg)));
        if (grayOut) {
            imageView.setColorFilter(ColorTintTransformation.getColorFilter(
                    R.color.disabled_resolver));
        } else {
            imageView.clearColorFilter();
        }
    }

    @Override
    public String getPrettyName() {
        return HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME;
    }

    @Override
    public void resolve(final Query queryToSearchFor) {
    }

    @Override
    public String getId() {
        return TomahawkApp.PLUGINNAME_HATCHET;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).isLoggedIn();
    }
}
