/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.datamodel.action;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;

import java.util.ArrayList;

/**
 * Action used to mark all the messages in a conversation as read
 */
public class MarkAsReadAction extends Action implements Parcelable {

    private static final String KEY_CONVERSATION_ID = "conversation_id";
    private static final String KEY_IS_LIST = "is_list";

    /**
     * Mark all the messages as read for a particular conversation.
     */
    public static void markAsRead(final String conversationId) {
        final MarkAsReadAction action = new MarkAsReadAction(conversationId);
        action.start();
    }

    /**
     * mark all the messages in given conversations as read
     * @param conversationIds
     */
    public static void markAsRead(final ArrayList<String> conversationIds) {
        final MarkAsReadAction action = new MarkAsReadAction(conversationIds);
        action.start();
    }

    private MarkAsReadAction(final String conversationId) {
        actionParameters.putString(KEY_CONVERSATION_ID, conversationId);
        actionParameters.putBoolean(KEY_IS_LIST, false);
    }

    private MarkAsReadAction(final ArrayList<String> conversationIds) {
        actionParameters.putStringArrayList(KEY_CONVERSATION_ID, conversationIds);
        actionParameters.putBoolean(KEY_IS_LIST, true);
    }


    @Override
    protected Object executeAction() {
        final boolean isList = actionParameters.getBoolean(KEY_IS_LIST);
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final ContentValues values = new ContentValues();
        int count = 0;

        // TODO: Consider doing this in background service to avoid delaying other actions

        // Update local db
        db.beginTransaction();
        try {
            if (isList) {
                final ArrayList<String> conversationIds = actionParameters.getStringArrayList(KEY_CONVERSATION_ID);
                for (String conversationId : conversationIds) {

                    values.put(MessageColumns.CONVERSATION_ID, conversationId);
                    values.put(MessageColumns.READ, 1);
                    values.put(MessageColumns.SEEN, 1);     // if they read it, they saw it

                    count = db.update(DatabaseHelper.MESSAGES_TABLE, values,
                            "(" + MessageColumns.READ + " !=1 OR " +
                                    MessageColumns.SEEN + " !=1 ) AND " +
                                    MessageColumns.CONVERSATION_ID + "=?",
                            new String[]{conversationId});
                }
                if (count > 0) {
                    MessagingContentProvider.notifyConversationListChanged();
                }
            } else {
                final String conversationId = actionParameters.getString(KEY_CONVERSATION_ID);
                values.put(MessageColumns.CONVERSATION_ID, conversationId);
                values.put(MessageColumns.READ, 1);
                values.put(MessageColumns.SEEN, 1);     // if they read it, they saw it

                count = db.update(DatabaseHelper.MESSAGES_TABLE, values,
                        "(" + MessageColumns.READ + " !=1 OR " +
                                MessageColumns.SEEN + " !=1 ) AND " +
                                MessageColumns.CONVERSATION_ID + "=?",
                        new String[]{conversationId});

                BugleDatabaseOperations.updateConversationListView(db,conversationId);
                if (count > 0) {
                    MessagingContentProvider.notifyMessagesChanged(conversationId);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        //TODO: After marking messages as read, update the notifications.
        return null;
    }

    private MarkAsReadAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<MarkAsReadAction> CREATOR
            = new Parcelable.Creator<MarkAsReadAction>() {
        @Override
        public MarkAsReadAction createFromParcel(final Parcel in) {
            return new MarkAsReadAction(in);
        }

        @Override
        public MarkAsReadAction[] newArray(final int size) {
            return new MarkAsReadAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
