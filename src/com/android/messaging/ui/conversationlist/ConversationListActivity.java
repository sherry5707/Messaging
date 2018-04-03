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
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.Trace;

public class ConversationListActivity extends AbstractConversationListActivity{
    private static final String TAG = "ConversationListActivity";
    private Toolbar mToolbar;
    private TextView mToolbarTitle;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private View mToolbarSearchView;
    private CheckBox mSelectAllCheckbox;
    private SearchMsgListener mSearchListener;
    private Menu mMenu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Trace.beginSection("ConversationListActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_list_activity);
        initToolBar();
        Trace.endSection();
        invalidateActionBar();
    }

    private void initToolBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mToolbarTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
        mSelectAllCheckbox = (CheckBox) mToolbar.findViewById(R.id.select_all);
        mSelectAllCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelectAllCheckbox.isChecked()){
                    mToolbarTitle.setText(getResources().getString(R.string.selected_msgs_num,getConversationsNumber()));
                    selectAllConversations();
                }else {
                    mToolbarTitle.setText(getResources().getString(R.string.multiple_select_title));
                    exitSelectAllConversations();
                }
            }
        });

        mToolbarSearchView = mToolbar.findViewById(R.id.search_view);
/*        mToolbarSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
            }
        });*/

        setSelectedNumListener(new UpdateSelectedNumListener() {
            @Override
            public void setMultipleSize(int number) {
                if (isInConversationListSelectMode()) {
                    mToolbarTitle.setVisibility(View.VISIBLE);
                    mToolbarTitle.setText(getResources().getString(R.string.selected_msgs_num, number));
                    if (number != getConversationsNumber()) {
                        mSelectAllCheckbox.setChecked(false);
                    }
                } else {
                    mToolbarTitle.setVisibility(View.GONE);
                }
            }
        });

        setMultiSelectModeListener(new MultipleSelectModeListener() {
            @Override
            public void updateToolBar(boolean isMultiMode) {
                if (isInConversationListSelectMode()) {
                    //mToolbarSearchView.setVisibility(View.GONE);
                    mSelectAllCheckbox.setVisibility(View.VISIBLE);
                } else {
                    mToolbarTitle.setVisibility(View.GONE);
                    //mToolbarSearchView.setVisibility(View.VISIBLE);
                    mSelectAllCheckbox.setChecked(false);
                    mSelectAllCheckbox.setVisibility(View.GONE);
                }
            }
        });
        setSupportActionBar(mToolbar);
    }

    /**
     * when the searchView in adapter clicked,it callback the click event in fragment and
     * fragment call this function to use SearchItem.
     */
    public boolean startSearch() {
        if (isInConversationListSelectMode()) {
            return false;
        }
        mSearchView.setVisibility(View.VISIBLE);
        Factory.get().setInSearchMode(Factory.INSEARCH_MODE);
        onSearchRequested();
        return true;
    }

    @Override
    protected void updateActionBar(final ActionBar actionBar) {
        actionBar.setTitle(getString(R.string.app_name));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(R.color.conversation_list_diliver_favorite_color)));
        actionBar.show();

        super.updateActionBar(actionBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Invalidate the menu as items that are based on settings may have changed
        // while not in the app (e.g. Talkback enabled/disable affects new conversation
        // button)
        //supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isInConversationListSelectMode()) {
            exitMultiSelectState();
        } else {
            if (Factory.get().getInSearchMode() == Factory.BACKSEARCH_MODE) {
                mSearchListener.onExitSearch();
                Factory.get().setInSearchMode(Factory.NOT_INSEARCH_MODE);
            } else if (Factory.get().getInSearchMode() == Factory.NOT_INSEARCH_MODE) {
                super.onBackPressed();
            }
        }
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener(){

        @Override
        public boolean onQueryTextSubmit(String query) {
            mSearchListener.onSearchMsg(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            mSearchListener.onSearchMsg(newText);
            return true;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.conversation_list_fragment_menu, menu);
        mMenu = menu;
        final MenuItem item = menu.findItem(R.id.action_debug_options);
        if (item != null) {
            final boolean enableDebugItems = DebugUtils.isDebugEnabled();
            item.setVisible(enableDebugItems).setEnabled(enableDebugItems);
        }

        //add for search view init
        mSearchItem = menu.findItem(R.id.action_search);

        mSearchView = (SearchView) mSearchItem.getActionView();
        //searchview ui style
        //get search widget
        int searchFrameId = mSearchView.getContext().getResources().getIdentifier("android:id/search_edit_frame", null, null);
        View searchFrame = mSearchView.findViewById(searchFrameId);
        if (searchFrame != null) {
            //set searchview background
            searchFrame.setBackground(getDrawable(R.drawable.bg_conversation_list_search_header));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) searchFrame.getLayoutParams();
            params.height = (int) getResources().getDimension(R.dimen.search_edit_text_height);
            params.topMargin = (int) getResources().getDimension(R.dimen.search_item_top_margin);
            params.leftMargin = (int) getResources().getDimension(R.dimen.searchview_item_margin);
            //set searchview editText height and top_margin
            searchFrame.setLayoutParams(params);

            int searchTextId = searchFrame.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchFrame.findViewById(searchTextId);
            //set editText color size
            if (searchText != null) {
                searchText.setTextColor(getColor(R.color.solid_black));
                searchText.setHintTextColor(getColor(R.color.search_view_hint_color));
                searchText.setTextSize(13);
            }
        }

        int searchDrawableId = mSearchView.getContext().getResources().getIdentifier("android:id/search_button", null, null);
        View searchDrawable = mSearchView.findViewById(searchDrawableId);
        if (searchDrawable != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) searchDrawable.getLayoutParams();
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
            searchDrawable.setLayoutParams(params);
        }

        //searchview setting
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.menu_search));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.requestFocus();

        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchListener.onStartSearch();
                mSearchView.requestFocus();
                showInputMethod();
                hideMenu();
                Factory.get().setInSearchMode(Factory.INSEARCH_MODE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchListener.onExitSearch();
                mSearchView.setQuery("",false);
                mSearchView.clearFocus();
                showMenu();
                Factory.get().setInSearchMode(Factory.NOT_INSEARCH_MODE);
                return true;
            }
        });

        //end init searchview

        //for edit:
        final MenuItem editItem = menu.findItem(R.id.action_edit);
        if (item != null) {
            editItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mToolbarTitle.setVisibility(View.VISIBLE);
                    mToolbarTitle.setText(getResources().getString(R.string.selected_msgs_num,0));
                    startMultiSelectActionMode();
                    return true;
                }
            });
        }

        //for mark as read for all
/*        final MenuItem markAsRead = menu.findItem(R.id.action_mark_as_read);
        if (item != null) {
            markAsRead.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    markAsReadForAll();
                    return true;
                }
            });
        }*/

        return true;
    }

    private void showInputMethod() {
        //auto pop up inputWindow
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        //force hide inputWindow
        // inputManager.hideSoftInputFromWindow(edit.getWindowToken(),0);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_start_new_conversation:
                onActionBarStartNewConversation();
                return true;
            case R.id.action_settings:
                onActionBarSettings();
                return true;
            case R.id.action_debug_options:
                onActionBarDebug();
                return true;
            case R.id.action_show_archived:
                onActionBarArchived();
                return true;
            case R.id.action_show_blocked_contacts:
                onActionBarBlockedParticipants();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    //to hide OverflowButton in search mode
    private void hideMenu() {
        if (mMenu.findItem(R.id.action_edit) != null) {
            mMenu.findItem(R.id.action_edit).setVisible(false);
        }
        if (mMenu.findItem(R.id.action_settings) != null) {
            mMenu.findItem(R.id.action_settings).setVisible(false);
        }
    }

    //to show OverflowButton when exit search mode
    private void showMenu() {
        if (mMenu.findItem(R.id.action_edit) != null) {
            mMenu.findItem(R.id.action_edit).setVisible(true);
        }
        if (mMenu.findItem(R.id.action_settings) != null) {
            mMenu.findItem(R.id.action_settings).setVisible(true);
        }
    }

    @Override
    public void onActionBarHome() {
        exitMultiSelectState();
    }

    public void onActionBarStartNewConversation() {
        UIIntents.get().launchCreateNewConversationActivity(this, null);
    }

    public void onActionBarSettings() {
        UIIntents.get().launchSettingsActivity(this);
    }

    public void onActionBarBlockedParticipants() {
        UIIntents.get().launchBlockedParticipantsActivity(this);
    }

    public void onActionBarArchived() {
        UIIntents.get().launchArchivedConversationsActivity(this);
    }

    @Override
    public boolean isSwipeAnimatable() {
        return !isInConversationListSelectMode();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        final ConversationListFragment conversationListFragment =
                (ConversationListFragment) getFragmentManager().findFragmentById(
                        R.id.conversation_list_fragment);
        // When the screen is turned on, the last used activity gets resumed, but it gets
        // window focus only after the lock screen is unlocked.
        if (hasFocus && conversationListFragment != null) {
            conversationListFragment.setScrolledToNewestConversationIfNeeded();
        }
    }

    public interface SearchMsgListener {
        void onStartSearch();
        void onSearchMsg(String searchString);
        void onExitSearch();
    }

    public void setSearchMsgListener(SearchMsgListener listener) {
        this.mSearchListener = listener;
    }
}
