package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

public interface FakeContextMenu {

    public void onFakeContextItemSelected(TomahawkMainActivity tomahawkMainActivity,
            String menuItemTitle, TomahawkListItem tomahawkListItem);

}
