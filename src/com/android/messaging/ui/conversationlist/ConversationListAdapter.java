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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.data.FavoritesData;
import com.android.messaging.ui.CursorRecyclerAdapter;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Dates;
import com.rgk.messaging.util.PrefsUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Provides an interface to expose Conversation List Cursor data to a UI widget like a ListView.
 */
public class ConversationListAdapter
        extends CursorRecyclerAdapter<ConversationListAdapter.ConversationListViewHolder>
        implements CursorRecyclerAdapter.SectionsSetupListener{
    private SparseArray<Section> mSections = new SparseArray<>();

    private final ConversationListItemView.HostInterface mClivHostInterface;
    private FavoritesData mFavorites;
    private String mSearchString;
    private int mTodayLastIndex;
    private int mTopChatLastIndex;
    private OnSearchViewClickListener mSearchViewClickListener;
    private boolean mSearchViewVisible = true;

    public interface OnSearchViewClickListener {
        boolean onClick();
    }

    public void setSearchViewClickListener(OnSearchViewClickListener listener) {
        mSearchViewClickListener = listener;
    }

    public void setSearchviewVisible(boolean isVisible) {
        mSearchViewVisible = isVisible;
        setupSections();
        notifyDataSetChanged();
    }

    public ConversationListAdapter(final Context context, final Cursor cursor,
                                   final ConversationListItemView.HostInterface clivHostInterface) {
        super(context, cursor, 0);
        mClivHostInterface = clivHostInterface;
        setHasStableIds(false);
    }

    @Override
    protected void init(Context context, Cursor c, int flags) {
        super.init(context, c, flags);
        setSectionsSetupListener(this);
        if (mDataValid) {
            setupSections();
        }
    }

    @Override
    public void onBindViewHolder(final ConversationListViewHolder holder, final int position) {
        boolean hasFavorites = mFavorites != null;
        if (holder.mConversationCounts != null) {
            if (mCursor == null || mCursor.getCount() == 0) {
                holder.mConversationCounts.setVisibility(GONE);
            } else {
                holder.mConversationCounts.setVisibility(VISIBLE);
                holder.mConversationCounts.setText(Factory.get().getApplicationContext().getResources().
                        getString(R.string.conversations_count, mCursor.getCount()));
            }
            return;
        }
        //if the searchview is Visible,the first position is it.
        //if clicked,the searchView is Gone and position should be recaculated.
        if (position == 0 && mSearchViewVisible) {
            holder.mSearchView.setVisibility(VISIBLE);
            holder.mSearchViewEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSearchViewClickListener.onClick()) {
                        mSearchViewVisible = false;
                        holder.mSearchView.setVisibility(View.GONE);
                        setupSections();
                        notifyDataSetChanged();
                    }
                }
            });
            return;
        }
        //if there is no searchview,Favorite position == 0
        //else if there is a searchview,Favorite position == 1
        if ((mSearchViewVisible && hasFavorites && position == 1) ||
                (!mSearchViewVisible && hasFavorites && position == 0)) {
            holder.mFavoriteSnippet.setText(mFavorites.getContent());
            //topchat and favorite has a bold diliver.
            //if there is no topchat(it means mTopChatLastIndex is default value = -1),the diliver should hide.
            holder.mBoldDiliver.setVisibility(mTopChatLastIndex == -1 ? GONE : VISIBLE);
            if (PrefsUtils.isShowContactIconEnabled()) {
                holder.mFavoriteIcon.setVisibility(VISIBLE);
            } else {
                holder.mFavoriteIcon.setVisibility(GONE);
            }
            return;
        }

        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        int realPos = sectionedPositionToPosition(position);
        if (realPos != INVALID_POSITION && !mCursor.moveToPosition(realPos)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        if (realPos == INVALID_POSITION) {
            holder.mTimeText.setText(mSections.get(position).getTitle());
            //it means first section's bold diliver want to disapear must have 3 conditions:
            //1:does not have favorite items
            //2:does not have topchat items
            //3:it is mSections' first item
            if (!hasFavorites &&
                    mSections.valueAt(0).mSectionedPosition == position &&
                    mTopChatLastIndex == -1) {
                holder.mBoldDiliver.setVisibility(GONE);
            } else {
                holder.mBoldDiliver.setVisibility(VISIBLE);
            }
        } else {
            //favorite group's last item should not have diliver
            //today group's last item should not have diliver
            holder.mDiliver.setVisibility
                    ((realPos == mTodayLastIndex || realPos == mTopChatLastIndex) ? GONE : VISIBLE);
            bindViewHolder(holder, mContext, mCursor);
        }
    }

    /**
     * @see com.android.messaging.ui.CursorRecyclerAdapter#bindViewHolder(
     * android.support.v7.widget.RecyclerView.ViewHolder, android.content.Context,
     * android.database.Cursor)
     */
    @Override
    public void bindViewHolder(final ConversationListViewHolder holder, final Context context,
                               final Cursor cursor) {
        final ConversationListItemView conversationListItemView = holder.mView;
        conversationListItemView.bind(cursor, mClivHostInterface, mSearchString);

    }

    @Override
    public ConversationListViewHolder createViewHolder(final Context context,
                                                       final ViewGroup parent, final int viewType) {
        ConversationListViewHolder viewHolder;
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (viewType == ConversationItemType.TYPE_CONVERSATION) {
            final ConversationListItemView itemView =
                    (ConversationListItemView) layoutInflater.inflate(
                            R.layout.conversation_list_item_view, null);
            viewHolder = new ConversationListViewHolder(itemView, viewType, mClivHostInterface);
            viewHolder.mDiliver = itemView.findViewById(R.id.conversation_list_divider);
        } else if (viewType == ConversationItemType.TYPE_FAVORITES) {
            final View itemView = layoutInflater.inflate(
                    R.layout.conversation_list_item_favorite, null);
            viewHolder = new ConversationListViewHolder(itemView, viewType, mClivHostInterface);
            viewHolder.mBoldDiliver = itemView.findViewById(R.id.conversation_list_favorite_divider);
        } else if (viewType == ConversationItemType.TYPE_TIME) {
            final LinearLayout timeSection = (LinearLayout) layoutInflater.inflate(
                    R.layout.conversation_list_item_section, parent, false);
            viewHolder = new ConversationListViewHolder(timeSection, viewType, mClivHostInterface);
            viewHolder.mBoldDiliver = timeSection.findViewById(R.id.time_section_bold_diliver);
        } else if (viewType == ConversationItemType.TYPE_SEARCH) {
            final LinearLayout SearchSection = (LinearLayout) layoutInflater.inflate(
                    R.layout.conversation_list_search_header, parent, false);
            viewHolder = new ConversationListViewHolder(SearchSection, viewType, mClivHostInterface);
        } else {
            final LinearLayout timeSection = (LinearLayout) layoutInflater.inflate(
                    R.layout.conversation_list_item_count, null);
            viewHolder = new ConversationListViewHolder(timeSection, viewType, mClivHostInterface);
        }
        return viewHolder;
    }

    public void setFavorites(FavoritesData favoritesData) {
        mFavorites = favoritesData;
    }

    /**
     * ViewHolder that holds a ConversationListItemView.
     */
    public static class ConversationListViewHolder extends RecyclerView.ViewHolder {
        ConversationListItemView mView;
        LinearLayout mTimeSection;
        TextView mTimeText;
        View mFavoriteGroup;
        TextView mFavoriteSnippet;
        View mBoldDiliver;
        View mDiliver;
        ConversationListItemView.HostInterface mHostInterface;
        TextView mConversationCounts;
        LinearLayout mSearchView;
        TextView mSearchViewEdit;
        ImageView mFavoriteIcon;

        public ConversationListViewHolder(final View itemView, @ConversationItemType.ConversationItemTypeDef int type,
                                          ConversationListItemView.HostInterface hostInterface) {
            super(itemView);
            mHostInterface = hostInterface;
            if (type == ConversationItemType.TYPE_CONVERSATION) {
                mView = (ConversationListItemView) itemView;
            } else if (type == ConversationItemType.TYPE_TIME) {
                mTimeSection = (LinearLayout) itemView;
                mTimeText = (TextView) mTimeSection.findViewById(R.id.section_time);
            } else if (type == ConversationItemType.TYPE_FAVORITES) {
                mFavoriteGroup = itemView;
                mFavoriteSnippet = (TextView) mFavoriteGroup.findViewById(R.id.conversation_favorite_snippet);
                mFavoriteGroup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mHostInterface.isSelectionMode()) {
                            UIIntents.get().launchFavoritesActivity(v.getContext());
                        }
                    }
                });
                mFavoriteIcon = (ImageView) mFavoriteGroup.findViewById(R.id.conversation_favorite);
            } else if (type == ConversationItemType.TYPE_COUNT) {
                mConversationCounts = (TextView) itemView.findViewById(R.id.conversations_count);
            } else if (type == ConversationItemType.TYPE_SEARCH) {
                mSearchView = (LinearLayout) itemView;
                mSearchViewEdit = (TextView) mSearchView.findViewById(R.id.search_view);
            }
        }
    }

    @Override
    public void setupSections() {
        mSections.clear();
        int hasSearchView = mSearchViewVisible ? 1 : 0;//1 beacause search view
        int offset = mFavorites != null ? (1 + hasSearchView) : (0 + hasSearchView);
        long preTime = 0;
        long curItemTime;
        boolean isTopChat;
        mTopChatLastIndex = -1;
        mTodayLastIndex = -1;
        if (mCursor == null || mCursor.getCount() == 0) {
            return;
        }
        long tzRawOffset = Dates.mTimeZone.getRawOffset();
        for (int i = 0; i < mCursor.getCount(); i++) {
            mCursor.moveToPosition(i);
            //ref: @CursorRecyclerAdapter.init
            curItemTime = mCursor.getLong(mCursor.getColumnIndexOrThrow(DatabaseHelper.ConversationColumns.SORT_TIMESTAMP));
            if (Factory.get().getInSearchMode() != Factory.INSEARCH_MODE) {
                isTopChat = mCursor.getInt(mCursor.getColumnIndexOrThrow(DatabaseHelper.ConversationColumns.TOP_CHAT)) == 1;

                if (isTopChat) {
                    //it remember the last item index in topchat group
                    mTopChatLastIndex = i;
                    continue;
                }
            }

            if (Dates.isToday(curItemTime)) {
                if ((curItemTime + tzRawOffset) / Dates.SECOND_PER_DAY != (preTime + tzRawOffset) / Dates.SECOND_PER_DAY) {
                    Section section = new Section(i, i + offset, R.string.today_fmt);
                    mSections.append(section.mSectionedPosition, section);
                    preTime = curItemTime;
                    ++offset;
                }
                //it remember the last item index in today group
                mTodayLastIndex = i;
            } else {
                Section section = new Section(i, i + offset, R.string.before_fmt);
                mSections.append(section.mSectionedPosition, section);
                break;
            }

        }

    }

    /**
     * @see android.support.v7.widget.RecyclerView.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        int offset = mFavorites != null ? 1 : 0;
        int hasSearchView = mSearchViewVisible ? 1 : 0;//1 means has searchview item
        if (mDataValid && mCursor != null) {
            return mCursor.getCount() + mSections.size() + offset + hasSearchView + 1; //1 means conversations count
        } else {
            return offset;
        }
    }

    @Override
    public int getItemViewType(int position) {
        boolean hasFavorites = mFavorites != null;
        if (position == 0 && mSearchViewVisible) {
            return ConversationItemType.TYPE_SEARCH;
        }
        if ((mSearchViewVisible && hasFavorites && position == 1) ||
                !mSearchViewVisible && hasFavorites && position == 0) {
            return ConversationItemType.TYPE_FAVORITES;
        }
        if (sectionedPositionToPosition(position) == mCursor.getCount()) {
            return ConversationItemType.TYPE_COUNT;
        }
        if (sectionedPositionToPosition(position) == INVALID_POSITION) {
            return ConversationItemType.TYPE_TIME;
        } else {
            return ConversationItemType.TYPE_CONVERSATION;
        }
    }

    private int sectionedPositionToPosition(int sectionedPosition) {
        if (mSections.get(sectionedPosition) != null) {
            return INVALID_POSITION;
        }
        int hasSearchView = mSearchViewVisible ? -1 : 0;    //-1 because search view
        int offset = mFavorites != null ? (-1 + hasSearchView) : (0 + hasSearchView);
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.valueAt(i).mSectionedPosition > sectionedPosition) {
                break;
            }
            --offset;
        }
        return sectionedPosition + offset;
    }

    private static class Section {
        int mFirstPosition;
        int mSectionedPosition;
        int mTitleRes;

        public Section(int firstPosition, int sectionedPosition, int titleId) {
            this.mFirstPosition = firstPosition;
            this.mSectionedPosition = sectionedPosition;
            this.mTitleRes = titleId;
        }


        public String getTitle() {
            return Factory.get().getApplicationContext().getResources().getString(mTitleRes);
        }
    }

    public void setInSearchMode(String searchString) {
        mSearchString = searchString;
    }
}
