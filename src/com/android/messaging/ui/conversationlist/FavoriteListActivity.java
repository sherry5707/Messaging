package com.android.messaging.ui.conversationlist;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;

public class FavoriteListActivity extends BugleActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        invalidateActionBar();
    }

    protected void updateActionBar(ActionBar actionBar) {
        actionBar.setTitle(getString(R.string.conversation_favorites));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(
                        R.color.favorite_conversation_action_bar_background_color)));
        actionBar.show();
        super.updateActionBar(actionBar);
    }

}
