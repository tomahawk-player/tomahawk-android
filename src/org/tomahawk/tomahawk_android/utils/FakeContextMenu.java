package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

public interface FakeContextMenu {

    public void onFakeContextItemSelected(TomahawkMainActivity tomahawkMainActivity,
            String menuItemTitle, TomahawkBaseAdapter.TomahawkListItem tomahawkListItem);

}
