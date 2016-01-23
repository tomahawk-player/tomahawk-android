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
package org.tomahawk.libtomahawk.authentication;

public abstract class AuthenticatorUtils {

    private final String mPrettyName;

    private final String mId;

    protected AuthenticatorUtils(String id, String prettyName) {
        mId = id;
        mPrettyName = prettyName;
    }

    public String getPrettyName() {
        return mPrettyName;
    }

    public abstract String getDescription();

    public String getId() {
        return mId;
    }

    public abstract int getIconResourceId();

    public abstract int getUserIdEditTextHintResId();

    public abstract void register(String name, String password, String email);

    public abstract void login(String email, String password);

    public abstract void logout();

    public abstract boolean isLoggedIn();

    public abstract String getUserName();

    public abstract boolean doesAllowRegistration();
}
