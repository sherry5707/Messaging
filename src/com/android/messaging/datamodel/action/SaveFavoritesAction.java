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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.util.LogUtil;

/**
 * Action used to save a single message to favorites.
 */
public class SaveFavoritesAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;

    public static void saveOrDeleteFavorites(final String messageId, final boolean isDeleteAction) {
        final SaveFavoritesAction action = new SaveFavoritesAction(messageId, isDeleteAction);
        action.start();
    }

    private static final String KEY_MESSAGE_ID = "message_id";
    private static final String KEY_ACTION = "isDeleteAction";

    private SaveFavoritesAction(final String messageId, final boolean isDeleteAction) {
        super();
        actionParameters.putString(KEY_MESSAGE_ID, messageId);
        actionParameters.putBoolean(KEY_ACTION, isDeleteAction);
    }

    @Override
    protected Bundle doBackgroundWork() {
        final DatabaseWrapper db = DataModel.get().getDatabase();

        // First find the thread id for this conversation.
        final String messageId = actionParameters.getString(KEY_MESSAGE_ID);
        final boolean isDeleteAction = actionParameters.getBoolean(KEY_ACTION);

        db.beginTransaction();
        try {
            if (!TextUtils.isEmpty(messageId)) {
                // Check message still exists
                final MessageData message = BugleDatabaseOperations.readMessage(db, messageId);
                if(isDeleteAction) {
                    BugleDatabaseOperations.deleteMessageFromFavoriteDB(db, messageId);
                    if (message != null) {
                        BugleDatabaseOperations.cancelMarkMessageFavorite(db, messageId);
                        MessagingContentProvider.notifyMessagesChanged(message.getConversationId());
                    }

                    // We may have changed the conversation list
                    MessagingContentProvider.notifyConversationListChanged();

                    MessagingContentProvider.notifyFavoritesChanged();
                } else {
                    if(message == null){
                        throw new IllegalStateException("This message is null");
                    }
                    if (!message.isFavorite()) {
                        // mark favorites
                        BugleDatabaseOperations.markMessageFavorite(db, messageId);
                        // save to favorites table
                        BugleDatabaseOperations.saveFavoriteDB(db, message);

                        MessagingContentProvider.notifyMessagesChanged(message.getConversationId());
                        // We may have changed the conversation list
                        MessagingContentProvider.notifyConversationListChanged();
                    } else {
                        LogUtil.w(TAG, "SaveFavoritesAction: Message " + messageId + " no longer exists");
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return null;
    }

    /**
     * save the message to favorites.
     */
    @Override
    protected Object executeAction() {
        requestBackgroundWork();
        return null;
    }

    private SaveFavoritesAction(final Parcel in) {
        super(in);
    }

    public static final Creator<SaveFavoritesAction> CREATOR
            = new Creator<SaveFavoritesAction>() {
        @Override
        public SaveFavoritesAction createFromParcel(final Parcel in) {
            return new SaveFavoritesAction(in);
        }

        @Override
        public SaveFavoritesAction[] newArray(final int size) {
            return new SaveFavoritesAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
