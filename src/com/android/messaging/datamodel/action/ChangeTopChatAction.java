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

import android.os.Parcel;
import android.os.Parcelable;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;

import java.util.ArrayList;

/**
 * Action used to mark all the messages in a conversation as read
 */
public class ChangeTopChatAction extends Action implements Parcelable {

    private static final String KEY_CONVERSATION_ID = "conversation_id";
    private static final String KEY_IS_TOP_CHAT = "is_top_chat";
    private static final String KEY_IS_LIST = "is_list";


    /**
     * Action for change the conversation item's top chat status
     * @param conversationId selected conversation's id
     * @param isTopChat , true set to top chat position.
     */
    public static void markAsTopChat(final String conversationId, boolean isTopChat) {
        final ChangeTopChatAction action = new ChangeTopChatAction(conversationId, isTopChat);
        action.start();
    }

    /**
     * Action for changes conversations' top chat status
     * @param conversationIds conversations'id that need to change
     * @param isTopChat indicate the topchat status
     */
    public static void markAsTopChat(ArrayList<String> conversationIds, boolean isTopChat) {
        final ChangeTopChatAction action = new ChangeTopChatAction(conversationIds, isTopChat);
        action.start();
    }

    private ChangeTopChatAction(final String conversationId, final boolean isTopChat) {
        actionParameters.putString(KEY_CONVERSATION_ID, conversationId);
        actionParameters.putBoolean(KEY_IS_TOP_CHAT, isTopChat);
        actionParameters.putBoolean(KEY_IS_LIST, false);
    }

    private ChangeTopChatAction(ArrayList<String> conversationIds, final boolean isTopChat) {
        actionParameters.putStringArrayList(KEY_CONVERSATION_ID, conversationIds);
        actionParameters.putBoolean(KEY_IS_TOP_CHAT, isTopChat);
        actionParameters.putBoolean(KEY_IS_LIST, true);
    }

    @Override
    protected Object executeAction() {
        final boolean toTopChat = actionParameters.getBoolean(KEY_IS_TOP_CHAT);
        final boolean isList = actionParameters.getBoolean(KEY_IS_LIST);
        final DatabaseWrapper db = DataModel.get().getDatabase();
        db.beginTransaction();
        try {
            if (isList) {
                final ArrayList<String> conversationIds = actionParameters.getStringArrayList(KEY_CONVERSATION_ID);
                for (String conversationId : conversationIds) {
                    BugleDatabaseOperations.updateConversationTopChatStatusInTransaction(
                            db, conversationId, toTopChat);
                }
            } else {
                final String conversationId = actionParameters.getString(KEY_CONVERSATION_ID);
                BugleDatabaseOperations.updateConversationTopChatStatusInTransaction(
                        db, conversationId, toTopChat);
                MessagingContentProvider.notifyConversationMetadataChanged(conversationId);
            }
            db.setTransactionSuccessful();
            MessagingContentProvider.notifyConversationListChanged();
        } finally {
            db.endTransaction();
        }
        return null;
    }

    private ChangeTopChatAction(final Parcel in) {
        super(in);
    }

    public static final Creator<ChangeTopChatAction> CREATOR
            = new Creator<ChangeTopChatAction>() {
        @Override
        public ChangeTopChatAction createFromParcel(final Parcel in) {
            return new ChangeTopChatAction(in);
        }

        @Override
        public ChangeTopChatAction[] newArray(final int size) {
            return new ChangeTopChatAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
