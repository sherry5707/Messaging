package com.android.messaging.ui.conversationlist;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * RGK added
 * IntDef is a way of replacing an integer enum where there's a parameter that should only accept explicit int values
 */
public class ConversationItemType {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_CONVERSATION, TYPE_TIME, TYPE_SEARCH, TYPE_FAVORITES, TYPE_COUNT})
    public @interface ConversationItemTypeDef {}

    /**
     * Normal conversation list item type
     */
    public static final int TYPE_CONVERSATION = 0;

    /**
     * Conversation time item flag
     */
    public static final int TYPE_TIME = 1;

    /**
     * Search View flag
     */
    public static final int TYPE_SEARCH = 2;

    /**
     * Favorites flag
     */
    public static final int TYPE_FAVORITES = 3;

    /**
     * Conversation count flag
     */
    public static final int TYPE_COUNT = 4;
}
