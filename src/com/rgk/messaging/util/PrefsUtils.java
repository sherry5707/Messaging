package com.rgk.messaging.util;

import android.content.Context;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.util.BuglePrefs;

public class PrefsUtils {
    public static final String SHOW_CONTACT_ICON_ENABLED    = "pref_show_contact_icon";

    private PrefsUtils() {
        //Don't instantiate
    }

    public static boolean isShowContactIconEnabled() {
        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();
        final Context context = Factory.get().getApplicationContext();
        final boolean defaultValue = context.getResources().getBoolean(
                R.bool.show_contacts_icon_pref_default);
        return prefs.getBoolean(SHOW_CONTACT_ICON_ENABLED, defaultValue);
    }
}
