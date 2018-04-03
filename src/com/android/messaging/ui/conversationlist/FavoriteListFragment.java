package com.android.messaging.ui.conversationlist;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;

import com.android.messaging.datamodel.data.FavoritesData;
import com.google.common.annotations.VisibleForTesting;

public class FavoriteListFragment extends Fragment implements FavoritesData.FavoritesListDataListener {
    private static final String SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY =
            "favoritesListViewState";
    @VisibleForTesting
    final Binding<FavoritesData> mListBinding = BindingBase.createBinding(this);
    private RecyclerView mRecyclerView;
    private Parcelable mListState;
    private FavoritesListAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListBinding.getData().init(getLoaderManager(), mListBinding);
        mAdapter = new FavoritesListAdapter(getActivity(), null);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mListBinding.bind(DataModel.get().createFavoritesData(activity, this));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_favorites, container, false);

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY);
        }

        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.favorites_list);
        final Activity activity = getActivity();
        final LinearLayoutManager manager = new LinearLayoutManager(activity) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListState != null) {
            outState.putParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY, mListState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    public void updateUi() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mListState = mRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onConversationListCursorUpdated(FavoritesData data, Cursor cursor) {
        mListBinding.ensureBound(data);
        final Cursor oldCursor = mAdapter.swapCursor(cursor);
        if (mListState != null && cursor != null && oldCursor == null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListBinding.unbind();
    }

}
