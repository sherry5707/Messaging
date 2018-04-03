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
package com.android.messaging.ui.conversationlist;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.annotation.VisibleForAnimation;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.media.UriImageRequestDescriptor;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.AsyncImageView;
import com.android.messaging.ui.AudioAttachmentView;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.ui.SnackBarInteraction;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.Typefaces;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;
import com.rgk.messaging.util.PrefsUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The view for a single entry in a conversation list.
 */
public class ConversationListItemView extends FrameLayout implements OnClickListener,
        OnLongClickListener, OnLayoutChangeListener {
    static final int UNREAD_SNIPPET_LINE_COUNT = 1;
    private static final String TAG = "ConversationListItemView";
    static final int NO_UNREAD_SNIPPET_LINE_COUNT = 1;
    private int mListItemReadColor;
    private int mListItemUnreadColor;
    private Typeface mListItemReadTypeface;
    private Typeface mListItemUnreadTypeface;
    private static String sPlusOneString;
    private static String sPlusNString;
    private String mSearchString;
    private TextView mUnreadCount;

    public interface HostInterface {
        boolean isConversationSelected(final String conversationId);
        void onConversationClicked(final ConversationListItemData conversationListItemData,
                boolean isLongClick, final ConversationListItemView conversationView);
        boolean isSwipeAnimatable();
        List<SnackBarInteraction> getSnackBarInteractions();
        void startFullScreenPhotoViewer(final Uri initialPhoto, final Rect initialPhotoBounds,
                final Uri photosUri);
        void startFullScreenVideoViewer(final Uri videoUri);
        boolean isSelectionMode();
    }

    private final OnClickListener fullScreenPreviewClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            final String previewType = mData.getShowDraft() ?
                    mData.getDraftPreviewContentType() : mData.getPreviewContentType();
            Assert.isTrue(ContentType.isImageType(previewType) ||
                    ContentType.isVideoType(previewType));

            final Uri previewUri = mData.getShowDraft() ?
                    mData.getDraftPreviewUri() : mData.getPreviewUri();
            if (ContentType.isImageType(previewType)) {
                final Uri imagesUri = mData.getShowDraft() ?
                        MessagingContentProvider.buildDraftImagesUri(mData.getConversationId()) :
                        MessagingContentProvider
                                .buildConversationImagesUri(mData.getConversationId());
                final Rect previewImageBounds = UiUtils.getMeasuredBoundsOnScreen(v);
                mHostInterface.startFullScreenPhotoViewer(
                        previewUri, previewImageBounds, imagesUri);
            } else {
                mHostInterface.startFullScreenVideoViewer(previewUri);
            }
        }
    };

    private final ConversationListItemData mData;

    private int mAnimatingCount;
    private ViewGroup mSwipeableContainer;
    private ViewGroup mCrossSwipeBackground;
    private ViewGroup mSwipeableContent;
    private TextViewSnippet mConversationNameView;
    private TextViewSnippet mSnippetTextView;
    private TextView mSubjectTextView;
    private TextView mTimestampTextView;
    private ContactIconView mContactIconView;
    private ImageView mContactCheckMarkView;
    private ImageView mNotificationBellView;
//    private ImageView mFailedStatusIconView;
    private ViewGroup mCrossSwipeReadStatus;
//    private ImageView mCrossSwipeDeleteRight;
//    private ImageView mCrossSwipeTopChatRight;
    private ViewGroup mCrossSwipeRightGroup;
    private AsyncImageView mImagePreviewView;
    private AudioAttachmentView mAudioAttachmentView;
    private HostInterface mHostInterface;
    private View mTopChat;
    private View mIconGroupView;
    private TextView mSnippetTip;   //display snippet and failure tips

    public ConversationListItemView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mData = new ConversationListItemData();
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContainer = (ViewGroup) findViewById(R.id.swipeableContainer);
        mCrossSwipeBackground = (ViewGroup) findViewById(R.id.crossSwipeBackground);
        mSwipeableContent = (ViewGroup) findViewById(R.id.swipeableContent);
        mConversationNameView = (TextViewSnippet) findViewById(R.id.conversation_name);
        mSnippetTextView = (TextViewSnippet) findViewById(R.id.conversation_snippet);
        mSubjectTextView = (TextView) findViewById(R.id.conversation_subject);
        mTimestampTextView = (TextView) findViewById(R.id.conversation_timestamp);
        mContactIconView = (ContactIconView) findViewById(R.id.conversation_icon);
        mContactCheckMarkView = (ImageView) findViewById(R.id.conversation_check_mark);
        mNotificationBellView = (ImageView) findViewById(R.id.conversation_notification_bell);
//        mFailedStatusIconView = (ImageView) findViewById(R.id.conversation_failed_status_icon);
        mIconGroupView = findViewById(R.id.conversation_icon_group);
        mCrossSwipeReadStatus = (ViewGroup) findViewById(R.id.crossSwipeReadStatus);
        mUnreadCount = (TextView) findViewById(R.id.conversation_unread);
        mTopChat = findViewById(R.id.conversation_topchat);
        mCrossSwipeRightGroup = (ViewGroup) findViewById(R.id.crossSwipeRightGroup);
//        mCrossSwipeDeleteRight =
//                (ImageView) findViewById(R.id.crossSwipeDelete);
//        mCrossSwipeDeleteRight.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final String conversationId = mData.getConversationId();
//                DeleteConversationAction.deleteConversation(
//                        conversationId,
//                        mData.getTimestamp());
//                UiUtils.showSnackBar(getContext(), getRootView(),
//                        getResources().getString(R.string.conversation_deleted));
//            }
//        });
//        mCrossSwipeTopChatRight =
//                (ImageView) findViewById(R.id.crossSwipeTopChat);
//        mCrossSwipeTopChatRight.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final String conversationId = mData.getConversationId();
//                ChangeTopChatAction.markAsTopChat(conversationId, !mData.getIsTopChat());
//            }
//        });

        mImagePreviewView = (AsyncImageView) findViewById(R.id.conversation_image_preview);
        mAudioAttachmentView = (AudioAttachmentView) findViewById(R.id.audio_attachment_view);
        mSnippetTip = (TextView) findViewById(R.id.conversation_snippet_tip);
        mConversationNameView.addOnLayoutChangeListener(this);
        mSnippetTextView.addOnLayoutChangeListener(this);

        final Resources resources = getContext().getResources();
        mListItemReadColor = resources.getColor(R.color.conversation_list_item_read);
        mListItemUnreadColor = resources.getColor(R.color.conversation_list_item_unread);

        mListItemReadTypeface = Typefaces.getRobotoNormal();
        mListItemUnreadTypeface = Typefaces.getRobotoBold();

        if (OsUtil.isAtLeastL()) {
            setTransitionGroup(true);
        }
    }

    @Override
    public void onLayoutChange(final View v, final int left, final int top, final int right,
            final int bottom, final int oldLeft, final int oldTop, final int oldRight,
            final int oldBottom) {
        if (v == mConversationNameView) {
            setConversationName();
        } else if (v == mSnippetTextView) {
            setSnippet();
        } else if (v == mSubjectTextView) {
            setSubject();
        }
    }

    private void setConversationName() {
//        if (mData.getIsRead() || mData.getShowDraft()) {
            //mConversationNameView.setTextColor(mListItemReadColor);
            mConversationNameView.setTypeface(mListItemReadTypeface);
//        } else {
//            mConversationNameView.setTextColor(mListItemUnreadColor);
//            mConversationNameView.setTypeface(mListItemUnreadTypeface);
//        }

        final String conversationName = mData.getName();

        // For group conversations, ellipsize the group members that do not fit
        final CharSequence ellipsizedName = UiUtils.commaEllipsize(
                conversationName,
                mConversationNameView.getPaint(),
                mConversationNameView.getMeasuredWidth(),
                getPlusOneString(),
                getPlusNString());
        // RTL : To format conversation name if it happens to be phone number.
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        final String bidiFormattedName = bidiFormatter.unicodeWrap(
                ellipsizedName.toString(),
                TextDirectionHeuristicsCompat.LTR);

        if (Factory.get().getInSearchMode() == Factory.INSEARCH_MODE && mSearchString != null) {
            mConversationNameView.setText(bidiFormattedName, mSearchString);
        } else {
            mConversationNameView.setText(bidiFormattedName);
        }
    }

    private static String getPlusOneString() {
        if (sPlusOneString == null) {
            sPlusOneString =  Factory.get().getApplicationContext().getResources()
                    .getString(R.string.plus_one);
        }
        return sPlusOneString;
    }

    private static String getPlusNString() {
        if (sPlusNString == null) {
            sPlusNString =  Factory.get().getApplicationContext().getResources()
                    .getString(R.string.plus_n);
        }
        return sPlusNString;
    }

    private void setSubject() {
        final String subjectText = mData.getShowDraft() ?
                mData.getDraftSubject() :
                    MmsUtils.cleanseMmsSubject(getContext().getResources(), mData.getSubject());
        if (!TextUtils.isEmpty(subjectText)) {
            final String subjectPrepend = getResources().getString(R.string.subject_label);
            mSubjectTextView.setText(TextUtils.concat(subjectPrepend, subjectText));
            mSubjectTextView.setVisibility(VISIBLE);
            mSnippetTextView.setVisibility(GONE);
        } else {
            mSubjectTextView.setVisibility(GONE);
            mSnippetTextView.setVisibility(VISIBLE);
        }
    }

    private void setSnippet() {
        if (Factory.get().getInSearchMode() == Factory.INSEARCH_MODE && mSearchString != null) {
            mSnippetTextView.setText(getSnippetText(), mSearchString);
        } else {
            mSnippetTextView.setText(getSnippetText());
        }
    }

    // Resource Ids of content descriptions prefixes for different message status.
    private static final int [][][] sPrimaryContentDescriptions = {
        // 1:1 conversation
        {
            // Incoming message
            {
                R.string.one_on_one_incoming_failed_message_prefix,
                R.string.one_on_one_incoming_successful_message_prefix
            },
            // Outgoing message
            {
                R.string.one_on_one_outgoing_failed_message_prefix,
                R.string.one_on_one_outgoing_successful_message_prefix,
                R.string.one_on_one_outgoing_draft_message_prefix,
                R.string.one_on_one_outgoing_sending_message_prefix,
            }
        },

        // Group conversation
        {
            // Incoming message
            {
                R.string.group_incoming_failed_message_prefix,
                R.string.group_incoming_successful_message_prefix,
            },
            // Outgoing message
            {
                R.string.group_outgoing_failed_message_prefix,
                R.string.group_outgoing_successful_message_prefix,
                R.string.group_outgoing_draft_message_prefix,
                R.string.group_outgoing_sending_message_prefix,
            }
        }
    };

    // Resource Id of the secondary part of the content description for an edge case of a message
    // which is in both draft status and failed status.
    private static final int sSecondaryContentDescription =
                                        R.string.failed_message_content_description;

    // 1:1 versus group
    private static final int CONV_TYPE_ONE_ON_ONE_INDEX = 0;
    private static final int CONV_TYPE_ONE_GROUP_INDEX = 1;
    // Direction
    private static final int DIRECTION_INCOMING_INDEX = 0;
    private static final int DIRECTION_OUTGOING_INDEX = 1;
    // Message status
    private static final int MESSAGE_STATUS_FAILED_INDEX = 0;
    private static final int MESSAGE_STATUS_SUCCESSFUL_INDEX = 1;
    private static final int MESSAGE_STATUS_DRAFT_INDEX = 2;
    private static final int MESSAGE_STATUS_SENDING_INDEX = 3;

    private static final int WIDTH_FOR_ACCESSIBLE_CONVERSATION_NAME = 600;

    public static String buildContentDescription(final Resources resources,
            final ConversationListItemData data, final TextPaint conversationNameViewPaint) {
        int messageStatusIndex;
        boolean outgoingSnippet = data.getIsMessageTypeOutgoing() || data.getShowDraft();
        if (outgoingSnippet) {
            if (data.getShowDraft()) {
                messageStatusIndex = MESSAGE_STATUS_DRAFT_INDEX;
            } else if (data.getIsSendRequested()) {
                messageStatusIndex = MESSAGE_STATUS_SENDING_INDEX;
            } else {
                messageStatusIndex = data.getIsFailedStatus() ? MESSAGE_STATUS_FAILED_INDEX
                        : MESSAGE_STATUS_SUCCESSFUL_INDEX;
            }
        } else {
            messageStatusIndex = data.getIsFailedStatus() ? MESSAGE_STATUS_FAILED_INDEX
                    : MESSAGE_STATUS_SUCCESSFUL_INDEX;
        }

        int resId = sPrimaryContentDescriptions
                [data.getIsGroup() ? CONV_TYPE_ONE_GROUP_INDEX : CONV_TYPE_ONE_ON_ONE_INDEX]
                [outgoingSnippet ? DIRECTION_OUTGOING_INDEX : DIRECTION_INCOMING_INDEX]
                [messageStatusIndex];

        final String snippetText = data.getShowDraft() ?
                data.getDraftSnippetText() : data.getSnippetText();

        final String conversationName = data.getName();
        String senderOrConvName = outgoingSnippet ? conversationName : data.getSnippetSenderName();

        String primaryContentDescription = resources.getString(resId, senderOrConvName,
                snippetText == null ? "" : snippetText,
                data.getFormattedTimestamp(),
                // This is used only for incoming group messages
                conversationName);
        String contentDescription = primaryContentDescription;

        // An edge case : for an outgoing message, it might be in both draft status and
        // failed status.
        if (outgoingSnippet && data.getShowDraft() && data.getIsFailedStatus()) {
            StringBuilder contentDescriptionBuilder = new StringBuilder();
            contentDescriptionBuilder.append(primaryContentDescription);

            String secondaryContentDescription =
                    resources.getString(sSecondaryContentDescription);
            contentDescriptionBuilder.append(" ");
            contentDescriptionBuilder.append(secondaryContentDescription);
            contentDescription = contentDescriptionBuilder.toString();
        }
        return contentDescription;
    }

    /**
     * Fills in the data associated with this view.
     *
     * @param cursor The cursor from a ConversationList that this view is in, pointing to its
     * entry.
     */
    public void bind(final Cursor cursor, final HostInterface hostInterface) {
        bind(cursor, hostInterface, null);
    }

    public void bind(final Cursor cursor, final HostInterface hostInterface, String searchString) {
        mSearchString = searchString;
        // Update our UI model
        mHostInterface = hostInterface;
        mData.bind(cursor);

        resetAnimatingState();

        mSwipeableContainer.setOnClickListener(this);
        mSwipeableContainer.setOnLongClickListener(this);

        final Resources resources = getContext().getResources();

        int color;
        final int maxLines;
        final Typeface typeface;
        final int typefaceStyle = Typeface.NORMAL;//mData.getShowDraft() ? Typeface.ITALIC : Typeface.NORMAL;
        final String snippetText = getSnippetText();

//        if (mData.getIsRead() || mData.getShowDraft()) {
            maxLines = TextUtils.isEmpty(snippetText) ? 0 : NO_UNREAD_SNIPPET_LINE_COUNT;
            color = mListItemReadColor;
            typeface = mListItemReadTypeface;
//        } else {
//            maxLines = TextUtils.isEmpty(snippetText) ? 0 : UNREAD_SNIPPET_LINE_COUNT;
//            color = mListItemUnreadColor;
//            typeface = mListItemUnreadTypeface;
//        }

        mSnippetTextView.setMaxLines(maxLines);
        mSnippetTextView.setTextColor(color);
        mSnippetTextView.setTypeface(typeface, typefaceStyle);
        mSubjectTextView.setTextColor(color);
        mSubjectTextView.setTypeface(typeface, typefaceStyle);
        setSnippet();
        setConversationName();
        setSubject();
        setContentDescription(buildContentDescription(resources, mData,
                mConversationNameView.getPaint()));

        final boolean isDefaultSmsApp = PhoneUtils.getDefault().isDefaultSmsApp();
        // don't show the error state unless we're the default sms app
        /*if (mData.getIsFailedStatus() && isDefaultSmsApp) {
            mTimestampTextView.setTextColor(resources.getColor(R.color.conversation_list_error));
            mTimestampTextView.setTypeface(mListItemReadTypeface, typefaceStyle);
            int failureMessageId = R.string.message_status_download_failed;
            if (mData.getIsMessageTypeOutgoing()) {
                failureMessageId = MmsUtils.mapRawStatusToErrorResourceId(mData.getMessageStatus(),
                        mData.getMessageRawTelephonyStatus());
            }
            mTimestampTextView.setText(resources.getString(failureMessageId));
        } else */

        //if has snippet and send failure status,mSnippetTip display snippet.
        //if has one status,display that status
        //if has no status,it gone
        int failStatusVisiblity = GONE;
        // Only show the fail icon if it is not a group conversation.!mData.getIsGroup() remove
        // And also require that we be the default sms app.
        if (mData.getIsFailedStatus() && isDefaultSmsApp) {
            failStatusVisiblity = VISIBLE;
        }
        if (Factory.get().getInSearchMode() == Factory.NOT_INSEARCH_MODE
                && (mData.getShowDraft()
                || mData.getMessageStatus() == MessageData.BUGLE_STATUS_OUTGOING_DRAFT
                // also check for unknown status which we get because sometimes the conversation
                // row is left with a latest_message_id of a no longer existing message and
                // therefore the join values come back as null (or in this case zero).
                || mData.getMessageStatus() == MessageData.BUGLE_STATUS_UNKNOWN)) {
            //mTimestampTextView.setTextColor(mListItemReadColor);
            mSnippetTip.setTypeface(mListItemReadTypeface, typefaceStyle);

            mSnippetTip.setText(resources.getString(
                    R.string.conversation_list_item_view_draft_message));
            mSnippetTip.setVisibility(View.VISIBLE);

        } else if(failStatusVisiblity == VISIBLE){
            mSnippetTip.setText(resources.getString(
                    R.string.send_fail));
            mSnippetTip.setVisibility(View.VISIBLE);
        } else {
            mSnippetTip.setVisibility(View.GONE);
        }

        //it display time
        mTimestampTextView.setTextColor(mListItemReadColor);
        mTimestampTextView.setTypeface(mListItemReadTypeface, typefaceStyle);
        final String formattedTimestamp = mData.getFormattedTimestamp();
        if (mData.getIsSendRequested()) {
            mTimestampTextView.setText(R.string.message_status_sending);
        } else {
            mTimestampTextView.setText(formattedTimestamp);
        }

        final boolean isSelected = mHostInterface.isConversationSelected(mData.getConversationId());
        final boolean isSelectionMode = mHostInterface.isSelectionMode();
        setSelected(isSelected);
        Uri iconUri = null;
        //int contactIconVisibility = GONE;
        int checkmarkVisiblity = GONE;
        if (isSelectionMode) {
            checkmarkVisiblity = VISIBLE;
        } else {
            //contactIconVisibility = VISIBLE;

        }

        if (PrefsUtils.isShowContactIconEnabled()) {
            mIconGroupView.setVisibility(VISIBLE);
            if (mData.getIcon() != null) {
                iconUri = Uri.parse(mData.getIcon());
            }
            mContactIconView.setImageResourceUri(iconUri, mData.getParticipantContactId(),
                    mData.getParticipantLookupKey(), mData.getOtherParticipantNormalizedDestination());
            //mContactIconView.setVisibility(contactIconVisibility);
            mContactIconView.setOnLongClickListener(this);
            mContactIconView.setClickable(!mHostInterface.isSelectionMode());
            mContactIconView.setLongClickable(!mHostInterface.isSelectionMode());
        } else {
            mIconGroupView.setVisibility(GONE);
        }

        mContactCheckMarkView.setVisibility(checkmarkVisiblity);
        mContactCheckMarkView.setImageResource(isSelected ?
                R.drawable.ic_check_circle : R.drawable.ic_unchecked_circle);

        final Uri previewUri = mData.getShowDraft() ?
                mData.getDraftPreviewUri() : mData.getPreviewUri();
        final String previewContentType = mData.getShowDraft() ?
                mData.getDraftPreviewContentType() : mData.getPreviewContentType();
        OnClickListener previewClickListener = null;
        Uri previewImageUri = null;
        int previewImageVisibility = GONE;
        int audioPreviewVisiblity = GONE;
        if (previewUri != null && !TextUtils.isEmpty(previewContentType)) {
            if (ContentType.isAudioType(previewContentType)) {
                boolean incoming = !(mData.getShowDraft() || mData.getIsMessageTypeOutgoing());
                mAudioAttachmentView.bind(previewUri, incoming, false);
                audioPreviewVisiblity = VISIBLE;
            } else if (ContentType.isVideoType(previewContentType)) {
                previewImageUri = UriUtil.getUriForResourceId(
                        getContext(), R.drawable.ic_preview_play);
                previewClickListener = fullScreenPreviewClickListener;
                previewImageVisibility = VISIBLE;
            } else if (ContentType.isImageType(previewContentType)) {
                previewImageUri = previewUri;
                previewClickListener = fullScreenPreviewClickListener;
                previewImageVisibility = VISIBLE;
            }
        }

        final int imageSize = resources.getDimensionPixelSize(
                R.dimen.conversation_list_image_preview_size);
        mImagePreviewView.setImageResourceId(
                new UriImageRequestDescriptor(previewImageUri, imageSize, imageSize,
                        true /* allowCompression */, false /* isStatic */, false /*cropToCircle*/,
                        ImageUtils.DEFAULT_CIRCLE_BACKGROUND_COLOR /* circleBackgroundColor */,
                        ImageUtils.DEFAULT_CIRCLE_STROKE_COLOR /* circleStrokeColor */));
        mImagePreviewView.setOnLongClickListener(this);
        mImagePreviewView.setVisibility(previewImageVisibility);
        mImagePreviewView.setOnClickListener(previewClickListener);
        mAudioAttachmentView.setOnLongClickListener(this);
        mAudioAttachmentView.setVisibility(audioPreviewVisiblity);

        final int notificationBellVisiblity = mData.getNotificationEnabled() ? GONE : VISIBLE;
        mNotificationBellView.setVisibility(notificationBellVisiblity);

/*        if (mData.getIsRead()) {
            mCrossSwipeReadStatus.setText(R.string.conversation_swipe_unread);
        } else {
            mCrossSwipeReadStatus.setText(R.string.conversation_swipe_read);
        }*/

        if (mData.getIsTopChat() && Factory.get().getInSearchMode() == Factory.NOT_INSEARCH_MODE) {
            mTopChat.setVisibility(VISIBLE);
//            mCrossSwipeTopChatRight.setImageDrawable(getResources()
//                    .getDrawable(R.drawable.ic_unpin_dark));
        } else {
//            mCrossSwipeTopChatRight.setImageDrawable(getResources()
//                    .getDrawable(R.drawable.ic_pin_to_top_dark));
            mTopChat.setVisibility(GONE);
        }

        int unreadCount = mData.getUnreadCount();
        if(unreadCount == 0){
            mUnreadCount.setVisibility(View.GONE);
        } else {
            mUnreadCount.setVisibility(View.VISIBLE);
            mUnreadCount.setText(String.valueOf(unreadCount));
        }

        //todo set SimCard Icon
    }

    public boolean isSwipeAnimatable() {
        return mHostInterface.isSwipeAnimatable();
    }

    @VisibleForAnimation
    public float getSwipeTranslationX() {
        return mSwipeableContainer.getTranslationX();
    }

    @VisibleForAnimation
    public void setSwipeTranslationX(final float translationX) {
        mSwipeableContainer.setTranslationX(translationX);
        if (translationX == 0) {
            mCrossSwipeBackground.setVisibility(View.GONE);
            mCrossSwipeReadStatus.setVisibility(GONE);
            mCrossSwipeRightGroup.setVisibility(GONE);

            mSwipeableContainer.setBackgroundColor(Color.TRANSPARENT);
        } else {
            mCrossSwipeBackground.setVisibility(View.VISIBLE);
            if (translationX > 0) {
                mCrossSwipeReadStatus.setVisibility(VISIBLE);
                mCrossSwipeRightGroup.setVisibility(GONE);
                mCrossSwipeBackground.setBackgroundColor(getContext().getColor(R.color.swipe_right_color));
            } else {
                mCrossSwipeReadStatus.setVisibility(GONE);
                mCrossSwipeRightGroup.setVisibility(VISIBLE);
                mCrossSwipeBackground.setBackgroundColor(getContext().getColor(R.color.swipe_left_color));
            }
            mSwipeableContainer.setBackgroundResource(R.drawable.swipe_shadow_drag);
            float widthPixels = getContext().getResources().getDisplayMetrics().widthPixels;
            // translationX/width is the alpha speed
            float alpha = 1 - Math.abs(translationX) / widthPixels;
            mSwipeableContainer.setAlpha(alpha);
        }
    }

    public void onSwipeComplete(int swipeDirection) {
        if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
            // TODO: figure out a good way to combine this with the implementation in
            // ConversationFragment doing similar things

            return;
        }

        final String conversationId = mData.getConversationId();
        if (swipeDirection == ConversationListSwipeHelper.SWIPE_DIRECTION_RIGHT) {
            final String phoneNumber = mData.getmSnippetSenderDisplayDestination();
            Assert.notNull(phoneNumber);
            final View targetView = this;
            final int screenLocation[] = new int[2];
            targetView.getLocationOnScreen(screenLocation);
            final int centerX = screenLocation[0] + targetView.getWidth() / 2;
            final int centerY = screenLocation[1] + targetView.getHeight() / 2;
            Point centerPoint = new Point(centerX, centerY);

            UIIntents.get().launchPhoneCallActivity(getContext(), phoneNumber, centerPoint);

            /*if (mData.getIsRead()) {
                MarkAsUnreadAction.markAsUnRead(conversationId);
                mCrossSwipeReadStatus.setText(R.string.conversation_swipe_unread);
            } else {
                MarkAsReadAction.markAsRead(conversationId);
                mCrossSwipeReadStatus.setText(R.string.conversation_swipe_read);
            }*/
        } else if (swipeDirection == ConversationListSwipeHelper.SWIPE_DIRECTION_LEFT) {
/*            DeleteConversationAction.deleteConversation(
                    conversationId,
                    mData.getTimestamp());
            UiUtils.showSnackBar(getContext(), getRootView(),
                    getResources().getString(R.string.conversation_deleted));*/

            //to conversationActivity
            Bundle sceneTransitionAnimationOptions = null;
            boolean hasCustomTransitions = false;

            UIIntents.get().launchConversationActivity(
                    getContext(), conversationId, null,
                    sceneTransitionAnimationOptions,
                    hasCustomTransitions);
        }
    }

    private void setShortAndLongClickable(final boolean clickable) {
        setClickable(clickable);
        setLongClickable(clickable);
    }

    public void resetAnimatingState() {
        mAnimatingCount = 0;
        setShortAndLongClickable(true);
        setSwipeTranslationX(0);
    }

    /**
     * Notifies this view that it is undergoing animation. This view should disable its click
     * targets.
     *
     * The animating counter is used to reset the swipe controller when the counter becomes 0. A
     * positive counter also makes the view not clickable.
     */
    public final void setAnimating(final boolean animating) {
        final int oldAnimatingCount = mAnimatingCount;
        if (animating) {
            mAnimatingCount++;
        } else {
            mAnimatingCount--;
            if (mAnimatingCount < 0) {
                mAnimatingCount = 0;
            }
        }

        if (mAnimatingCount == 0) {
            // New count is 0. All animations ended.
            setShortAndLongClickable(true);
        } else if (oldAnimatingCount == 0) {
            // New count is > 0. Waiting for some animations to end.
            setShortAndLongClickable(false);
        }
    }

    public boolean isAnimating() {
        return mAnimatingCount > 0;
    }

    /**
     * {@inheritDoc} from OnClickListener
     */
    @Override
    public void onClick(final View v) {
        processClick(v, false);
    }

    /**
     * {@inheritDoc} from OnLongClickListener
     */
    @Override
    public boolean onLongClick(final View v) {
        return processClick(v, true);
    }

    private boolean processClick(final View v, final boolean isLongClick) {
        Assert.isTrue(v == mSwipeableContainer || v == mContactIconView || v == mImagePreviewView);
        Assert.notNull(mData.getName());

        if (mHostInterface != null) {
            mHostInterface.onConversationClicked(mData, isLongClick, this);
            return true;
        }
        return false;
    }

    public View getSwipeableContent() {
        return mSwipeableContent;
    }

    public View getContactIconView() {
        return mContactIconView;
    }

    private String getSnippetText() {
        if (Factory.get().getInSearchMode() == Factory.INSEARCH_MODE) {
            return mData.getSnippetText();
        }
        String snippetText = mData.getShowDraft() ?
                mData.getDraftSnippetText() : mData.getSnippetText();
        final String previewContentType = mData.getShowDraft() ?
                mData.getDraftPreviewContentType() : mData.getPreviewContentType();
        if (TextUtils.isEmpty(snippetText)) {
            Resources resources = getResources();
            // Use the attachment type as a snippet so the preview doesn't look odd
            if (ContentType.isAudioType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_audio_clip);
            } else if (ContentType.isImageType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_picture);
            } else if (ContentType.isVideoType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_video);
            } else if (ContentType.isVCardType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_vcard);
            }
        }
        return snippetText;
    }

    public static class TextViewSnippet extends TextView {
        private static String sEllipsis = "\u2026";

        private static int sTypefaceHighlight = Typeface.BOLD;

        private String mFullText;
        private String mTargetString;
        private Pattern mPattern;

        public TextViewSnippet(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TextViewSnippet(Context context) {
            super(context);
        }

        public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        /**
         * We have to know our width before we can compute the snippet string.  Do that
         * here and then defer to super for whatever work is normally done.
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (mFullText == null) {
                return;
            }
            String fullTextLower = mFullText.toLowerCase();
            String targetStringLower = null;
            if (mTargetString != null) {
                targetStringLower = mTargetString.toLowerCase();
            }

            int startPos = 0;
            int searchStringLength = targetStringLower == null ? 0 : targetStringLower.length();
            int bodyLength = fullTextLower.length();

            Matcher m = mPattern.matcher(mFullText);
            if (m.find(0)) {
                startPos = m.start();
            }

            TextPaint tp = getPaint();

            float searchStringWidth = tp.measureText(mTargetString);
            float textFieldWidth = getWidth();
            Log.d(TAG, "onLayout startPos = " + startPos
                    + " searchStringWidth = " + searchStringWidth
                    + " textFieldWidth = " + textFieldWidth);

            /// M: google jb.mr1 patch, Modify to take Ellipsis for avoiding JE
            /// assume we'll need one on both ends @{
            float ellipsisWidth = tp.measureText(sEllipsis);
            textFieldWidth -= (2F * ellipsisWidth);
            /// @}
            String snippetString = null;
            /// M: add "=".
            if (searchStringWidth >= textFieldWidth) {
                /// M: Code analyze 006, For fix bug ALPS00280615, The tips mms
                // has stopped show and JE happen after clicking the longer
                // search suggestion. @{
                try {
                    snippetString = mFullText.substring(startPos, startPos + searchStringLength);
                } catch (Exception e) {
                    Log.e(TAG, " StringIndexOutOfBoundsException ");
                    e.printStackTrace();
                    /// M: for search je.
                    snippetString = mFullText;
                }
                /// @}
            } else {
                int offset = -1;
                int start = -1;
                int end = -1;
                /* TODO: this code could be made more efficient by only measuring the additional
                 * characters as we widen the string rather than measuring the whole new
                 * string each time.
                 */
                while (true) {
                    offset += 1;

                    int newstart = Math.max(0, startPos - offset);
                    int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                    if (newstart == start && newend == end) {
                        // if we couldn't expand out any further then we're done
                        break;
                    }
                    start = newstart;
                    end = newend;

                    // pull the candidate string out of the full text rather than body
                    // because body has been toLower()'ed
                    String candidate = mFullText.substring(start, end);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        // if the newly computed width would exceed our bounds then we're done
                        // do not use this "candidate"
                        break;
                    }

                    snippetString = String.format(
                            "%s%s%s",
                            start == 0 ? "" : sEllipsis,
                            candidate,
                            end == bodyLength ? "" : sEllipsis);
                }
            }
            if (snippetString == null) {
                if (textFieldWidth >= mFullText.length()) {
                    snippetString = mFullText;
                } else {
                    snippetString = mFullText.substring(0, (int) textFieldWidth);
                }
            }
            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;

            m = mPattern.matcher(snippetString);
            while (m.find(start)) {
                Log.d(TAG, "onLayout(): start = " + start + ", m.end() = " + m.end());
                if (start == m.end()) {
                    break;
                }
                spannable.setSpan(new StyleSpan(sTypefaceHighlight), m.start(), m.end(), 0);
                //set searchString highlight
                spannable.setSpan(new ForegroundColorSpan(
                        getResources().getColor(R.color.rgk_primary_color)), m.start(), m.end(), 0);
                start = m.end();
            }
            setText(spannable);
            // do this after the call to setText() above
            super.onLayout(changed, left, top, right, bottom);
        }

        public void setText(String fullText, String target) {
            // Use a regular expression to locate the target string
            // within the full text.  The target string must be
            // found as a word start so we use \b which matches
            // word boundaries.
            String patternString = Pattern.quote(target);
            mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

            mFullText = fullText;
            mTargetString = target;
            requestLayout();
        }
    }
}
