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

package com.android.messaging.datamodel.data;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.BoundCursorLoader;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.FavoritesColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.binding.BindableData;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.util.Assert;
import com.android.messaging.util.Dates;
import com.android.messaging.util.LogUtil;

/**
 * A class that encapsulates all of the data for a specific favorite in a favorites.
 */
public class FavoritesData extends BindableData
        implements LoaderManager.LoaderCallbacks<Cursor> , Parcelable {
    private static final int FAVORITES_LIST_LOADER = 1;
    private static final String BINDING_ID = "bindingId";
    public static final String SORT_ORDER = FavoritesColumns.FAVORITES_SAVED_TIME + " DESC";
    private static final String TAG = FavoritesData.class.getSimpleName();

    private Context mContext;
    private LoaderManager mLoaderManager;
    private FavoritesListDataListener mListener;

    public FavoritesData(Context context, FavoritesListDataListener listener) {
        mListener = listener;
        mContext = context;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Assert.equals(FAVORITES_LIST_LOADER, id);
        final String bindingId = args.getString(BINDING_ID);
        Loader<Cursor> loader = null;
        // Check if data still bound to the requesting ui element
        if (isBound(bindingId)) {
            loader = new BoundCursorLoader(bindingId, mContext,
                    MessagingContentProvider.FAVORITES_URI,
                    FavoritesQuery.PROJECTION,
                    null,
                    null,
                    SORT_ORDER);
        } else {
            LogUtil.w(TAG, "onCreateLoader : Creating favorites loader failed");
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> generic, Cursor data) {
        final BoundCursorLoader loader = (BoundCursorLoader) generic;
        Assert.equals(FAVORITES_LIST_LOADER, loader.getId());

        // Check if data still bound to the requesting ui element
        if (isBound(loader.getBindingId())) {
            mListener.onConversationListCursorUpdated(this, data);
        } else {
            LogUtil.w(TAG, "onLoadFinished : favorites loader failed");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> generic) {
        final BoundCursorLoader loader = (BoundCursorLoader) generic;
        Assert.equals(FAVORITES_LIST_LOADER, loader.getId());
        if (isBound(loader.getBindingId())) {
            mListener.onConversationListCursorUpdated(this, null);
        } else {
            LogUtil.w(TAG, "Loader reset after unbinding list");
        }
    }

    @Override
    protected void unregisterListeners() {
        mListener = null;

        // This could be null if we bind but the caller doesn't init the BindableData
        if (mLoaderManager != null) {
            mLoaderManager.destroyLoader(FAVORITES_LIST_LOADER);
            mLoaderManager = null;
        }
    }

    public void init(final LoaderManager loaderManager,
                     final BindingBase<FavoritesData> binding) {
        final Bundle args = new Bundle();
        args.putString(BINDING_ID, binding.getBindingId());
        mLoaderManager = loaderManager;
        mLoaderManager.initLoader(FAVORITES_LIST_LOADER, args, this);
    }

    public static class FavoritesQuery {
        public static final String[] PROJECTION = new String[] {
            FavoritesColumns.FAVORITES_ID,
            FavoritesColumns.PROFILE_PHOTO_URI,
            FavoritesColumns.FULL_NAME,
            FavoritesColumns.SEND_DESTINATION,
            FavoritesColumns.FAVORITES_SAVED_TIME,
            FavoritesColumns.SENT_TIMESTAMP,
            FavoritesColumns.RECEIVED_TIMESTAMP,
            FavoritesColumns.SIM_SLOT_ID,
            FavoritesColumns.SUB_ID,
            FavoritesColumns.CONTENT,
            FavoritesColumns.CONVERSATION_ID,
            FavoritesColumns.MESSAGE_ID,
            FavoritesColumns.PARTICIPANT_CONTACT_ID,
            FavoritesColumns.PARTICIPANT_LOOKUP_KEY,

        };

        public static final int INDEX_FAVORITES_ID              = 0;
        public static final int INDEX_PROFILE_PHOTO_URI         = 1;
        public static final int INDEX_FULL_NAME                 = 2;
        public static final int INDEX_SEND_DESTINATION          = 3;
        public static final int INDEX_FAVORITES_SAVED_TIME      = 4;
        public static final int INDEX_SENT_TIMESTAMP            = 5;
        public static final int INDEX_RECEIVED_TIMESTAMP        = 6;
        public static final int INDEX_SIM_SLOT_ID               = 7;
        public static final int INDEX_SUB_ID                    = 8;
        public static final int INDEX_CONTENT                   = 9;
        public static final int INDEX_CONVERSATION_ID           = 10;
        public static final int INDEX_MESSAGE_ID                = 11;
        public static final int INDEX_PARTICIPANT_CONTACT_ID    = 12;
        public static final int INDEX_PARTICIPANT_LOOKUP_KEY    = 13;
    }

    private String mFavoriteId;
    private int mSubId;
    private int mSlotId;
    private String mSendDestination;
    private String mFullName;
    private String mProfilePhotoUri;
    private long mSentTimestamp;
    private long mReceivedTimestamp;
    private long mSavedTimestamp;
    private String mContent;
    private int mConversationId;
    private int mMessageId;
    private long mContactId;
    private String mParticipantLookupKey;

    // Don't call constructor directly
    private FavoritesData() {
    }

    public static FavoritesData getFromCursor(final Cursor cursor) {
        final FavoritesData favoritesData = new FavoritesData();
        favoritesData.mFavoriteId = cursor.getString(FavoritesQuery.INDEX_FAVORITES_ID);
        favoritesData.mSubId = cursor.getInt(FavoritesQuery.INDEX_SUB_ID);
        favoritesData.mSlotId = cursor.getInt(FavoritesQuery.INDEX_SIM_SLOT_ID);
        favoritesData.mSendDestination = cursor.getString(FavoritesQuery.INDEX_SEND_DESTINATION);
        favoritesData.mFullName = cursor.getString(FavoritesQuery.INDEX_FULL_NAME);
        favoritesData.mProfilePhotoUri = cursor.getString(FavoritesQuery.INDEX_PROFILE_PHOTO_URI);
        favoritesData.mSentTimestamp = cursor.getLong(FavoritesQuery.INDEX_SENT_TIMESTAMP);
        favoritesData.mReceivedTimestamp = cursor.getLong(FavoritesQuery.INDEX_RECEIVED_TIMESTAMP);
        favoritesData.mSavedTimestamp = cursor.getLong(FavoritesQuery.INDEX_FAVORITES_SAVED_TIME);
        favoritesData.mContent = cursor.getString(FavoritesQuery.INDEX_CONTENT);
        favoritesData.mConversationId = cursor.getInt(FavoritesQuery.INDEX_CONVERSATION_ID);
        favoritesData.mMessageId = cursor.getInt(FavoritesQuery.INDEX_MESSAGE_ID);
        favoritesData.mContactId = cursor.getLong(FavoritesQuery.INDEX_PARTICIPANT_CONTACT_ID);
        favoritesData.mParticipantLookupKey = cursor.getString(FavoritesQuery.INDEX_PARTICIPANT_LOOKUP_KEY);

        return favoritesData;
    }

    public static FavoritesData getLastFavorite(final DatabaseWrapper dbWrapper) {
        Cursor cursor = null;
        try {
            cursor = dbWrapper.query(DatabaseHelper.FAVORITES_TABLE,
                    FavoritesQuery.PROJECTION,
                    null,
                    null, null, null, null);

            if (cursor.moveToLast()) {
                return FavoritesData.getFromCursor(cursor);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getSendDestination() {
        return mSendDestination;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getDisplayName() {
        if (!TextUtils.isEmpty(mFullName)) {
            return mFullName;
        }
        if (!TextUtils.isEmpty(mSendDestination)) {
            return mSendDestination;
        }

        return Factory.get().getApplicationContext().getResources().getString(
                R.string.unknown_sender);
    }

    public String getProfilePhotoUri() {
        return mProfilePhotoUri;
    }

    public void setFullName(final String fullName) {
        mFullName = fullName;
    }
    public void setProfilePhotoUri(final String profilePhotoUri) {
        mProfilePhotoUri = profilePhotoUri;
    }


    public void setSendDestination(final String destination) {
        mSendDestination = destination;
    }

    public int getSubId() {
        return mSubId;
    }


    public boolean isActiveSubscription() {
        return mSlotId != ParticipantData.INVALID_SLOT_ID;
    }

    public boolean isDefaultSelf() {
        return mSubId == ParticipantData.DEFAULT_SELF_SUB_ID;
    }

    public int getSlotId() {
        return mSlotId;
    }

    /**
     * Slot IDs in the subscription manager is zero-based, but we want to show it
     * as 1-based in UI.
     */
    public int getDisplaySlotId() {
        return getSlotId() + 1;
    }

    public String getId() {
        return mFavoriteId;
    }

    public boolean isSelf() {
        return (mSubId != ParticipantData.OTHER_THAN_SELF_SUB_ID);
    }


    public String getContent() {
        return mContent;
    }

    public String getIcon() {
        return mProfilePhotoUri;
    }

    public String getFormattedTimestamp(long time) {
        return Dates.getConversationTimeString(time).toString();
    }

    public long getSavedTime() {
        return mSavedTimestamp;
    }

    public long getSentTimestamp() {
        return mSentTimestamp;
    }

    public long getReceivedTimestamp() {
        return mReceivedTimestamp;
    }


    public int getConversationId() {
        return mConversationId;
    }

    public int getMessageId() {
        return mMessageId;
    }

    public MessageData getMessageData(){
        final MessageData forwardedMessage = new MessageData();
        MessagePartData forwardedPart = MessagePartData.createTextMessagePart(getContent());
        forwardedMessage.addPart(forwardedPart);
        return forwardedMessage;
    }

    public long getContactId() {
        return mContactId;
    }

    public String getParticipantLookupKey() {
        return mParticipantLookupKey;
    }

    public FavoritesData(final Parcel in) {
        mFavoriteId = in.readString();
        mSubId = in.readInt();
        mSlotId = in.readInt();
        mSendDestination = in.readString();
        mFullName = in.readString();
        mProfilePhotoUri = in.readString();
        mSavedTimestamp = in.readLong();
        mSentTimestamp = in.readLong();
        mReceivedTimestamp = in.readLong();
        mContent = in.readString();
        mConversationId = in.readInt();
        mMessageId = in.readInt();
        mContactId = in.readLong();
        mParticipantLookupKey = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mFavoriteId);
        dest.writeInt(mSubId);
        dest.writeInt(mSlotId);
        dest.writeString(mSendDestination);
        dest.writeString(mFullName);
        dest.writeString(mProfilePhotoUri);
        dest.writeLong(mSavedTimestamp);
        dest.writeLong(mSentTimestamp);
        dest.writeLong(mReceivedTimestamp);
        dest.writeString(mContent);
        dest.writeInt(mConversationId);
        dest.writeInt(mMessageId);
        dest.writeLong(mContactId);
        dest.writeString(mParticipantLookupKey);
    }

    public static final Creator<FavoritesData> CREATOR
    = new Creator<FavoritesData>() {
        @Override
        public FavoritesData createFromParcel(final Parcel in) {
            return new FavoritesData(in);
        }

        @Override
        public FavoritesData[] newArray(final int size) {
            return new FavoritesData[size];
        }
    };

    public interface FavoritesListDataListener{
        void onConversationListCursorUpdated(FavoritesData data, Cursor cursor);
    }
}
