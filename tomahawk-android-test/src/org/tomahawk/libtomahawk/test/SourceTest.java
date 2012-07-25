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
package org.tomahawk.libtomahawk.test;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.Source;

import android.test.AndroidTestCase;

public class SourceTest extends AndroidTestCase {

    private Source tstLocalSource;
    private Collection tstLocalCollection;

    public void setUp() {
        tstLocalCollection = new LocalCollection(getContext().getContentResolver());
        tstLocalSource = new org.tomahawk.libtomahawk.Source(tstLocalCollection, 1,
                "Test Collection");
    }

    public void tearDown() {
        tstLocalSource = null;

    }

    public void testIsLocal() {
        Assert.assertTrue(tstLocalSource.isLocal());
    }

    public void testGetCollection() {
        Assert.assertTrue(tstLocalSource.getCollection() == tstLocalCollection);
    }
}
