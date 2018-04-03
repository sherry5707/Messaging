package com.android.messaging.ui.conversationlist;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.content.res.Resources;

import com.android.messaging.datamodel.action.SaveFavoritesAction;
import com.android.messaging.datamodel.data.FavoritesData;
import com.android.messaging.ui.ContactIconView;
import com.android.messaging.ui.CursorRecyclerAdapter;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.R;


public class FavoritesListAdapter extends
        CursorRecyclerAdapter<FavoritesListAdapter.FavoritesListViewHolder> {

    /**
     * Recommended constructor.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public FavoritesListAdapter(Context context, Cursor c) {
        super(context, c, 0);
        setHasStableIds(true);
    }

    @Override
    public void bindViewHolder(FavoritesListViewHolder holder, Context context, Cursor cursor) {
        FavoritesData favoritesData = FavoritesData.getFromCursor(cursor);
        setupViews(holder, favoritesData);
    }

    private void setupViews(FavoritesListViewHolder holder, FavoritesData favoritesData) {
        View rootView = holder.itemView;
        ContactIconView contactIconView = (ContactIconView) rootView.findViewById(R.id.favorite_icon);
        Uri iconUri = null;
        if (favoritesData.getIcon() != null) {
            iconUri = Uri.parse(favoritesData.getIcon());
        }
        contactIconView.setImageResourceUri(iconUri, favoritesData.getContactId()
                , favoritesData.getParticipantLookupKey(), favoritesData.getSendDestination());

        TextView name = (TextView) rootView.findViewById(R.id.favorite_name);
        name.setText(favoritesData.getFullName());

        String formattedTimestamp = favoritesData
                .getFormattedTimestamp(favoritesData.getSavedTime());

        TextView savedTime = (TextView) rootView.findViewById(R.id.favorite_saved_time);
        savedTime.setText(mContext.getString(R.string.favorite_saved_time, formattedTimestamp));

        Resources resources = rootView.getContext().getResources();
        boolean isReceived = favoritesData.getSentTimestamp() == 0;
        formattedTimestamp = favoritesData.getFormattedTimestamp(
                isReceived ? favoritesData.getReceivedTimestamp() : favoritesData.getSentTimestamp());

        String sourceStr = resources.getString(isReceived ? R.string.favorite_received : R.string.favorite_sent, formattedTimestamp);
        TextView sourceText = (TextView) rootView.findViewById(R.id.favorite_source);
        sourceText.setText(sourceStr);

        TextView contentText = (TextView) rootView.findViewById(R.id.favorite_content);
        contentText.setText(favoritesData.getContent());

        rootView.setTag(favoritesData);
    }

    @Override
    public FavoritesListViewHolder createViewHolder(Context context, ViewGroup parent, int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        return new FavoritesListViewHolder(layoutInflater.inflate(
                R.layout.favorite_list_item, null));
    }

    public static class FavoritesListViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener{

        public FavoritesListViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final FavoritesData favoritesData = (FavoritesData) v.getTag();
            AlertDialog alertDialog = new AlertDialog.Builder(v.getContext())
                    .setItems(R.array.favorite_dialog_item, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:// view raw message
                                    final String conversationId = String.valueOf(favoritesData.getConversationId());
                                    Bundle sceneTransitionAnimationOptions = null;
                                    boolean hasCustomTransitions = false;

                                    UIIntents.get().launchConversationActivity(
                                            v.getContext(), conversationId, null,
                                            sceneTransitionAnimationOptions,
                                            hasCustomTransitions);
                                    break;

                                case 1:// copy content
                                    final ClipboardManager clipboard = (ClipboardManager) v.getContext()
                                            .getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(
                                            ClipData.newPlainText(null /* label */, favoritesData.getContent()));
                                    break;

                                case 2:// forward
                                    UIIntents.get().launchForwardMessageActivity(v.getContext(),
                                            favoritesData.getMessageData());
                                    break;

                                case 3:// cancel favorite
                                    SaveFavoritesAction.saveOrDeleteFavorites(
                                            String.valueOf(favoritesData.getMessageId()), true);
                                    break;
                            }

                        }
                    }).setTitle(favoritesData.getContent()).create();
            Window window = alertDialog.getWindow();
            window.setGravity(Gravity.BOTTOM);
            alertDialog.show();
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if(newCursor == null || newCursor.getCount() == 0){
            ((Activity)mContext).finish();
        }
        return super.swapCursor(newCursor);
    }
}
