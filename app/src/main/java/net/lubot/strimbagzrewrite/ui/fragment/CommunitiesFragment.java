/*
 * Copyright 2016 Nicola Fäßler
 *
 * This file is part of StrimBagZ.
 *
 * StrimBagZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.lubot.strimbagzrewrite.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Community;
import net.lubot.strimbagzrewrite.data.model.Twitch.CommunityObject;
import net.lubot.strimbagzrewrite.data.model.Twitch.Directory;
import net.lubot.strimbagzrewrite.data.model.Twitch.DirectoryGame;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedGame;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.CommunitiesAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.GamesAdapter;
import net.lubot.strimbagzrewrite.util.AutofitRecyclerView;
import net.lubot.strimbagzrewrite.util.MarginDecoration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunitiesFragment extends Fragment {

    public final String TAG = "LiveGamesFragment";

    private Context context;
    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private EmptyRecyclerViewAdapter emptyView;
    private Parcelable listState;
    private LinearLayoutManager layoutManager;
    private GridLayoutManager internalManager;
    private MarginDecoration offsetDecoration;
    private CommunitiesAdapter adapter;

    private final int UPDATE_CYCLE = 360;

    private String cursor = "NO_CURSOR";
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int visibleItemCount = internalManager.getChildCount();
            int totalItemCount = internalManager.getItemCount();
            int firstVisibleItemPosition = internalManager.findFirstVisibleItemPosition();

            if (!isLoading && !isLastPage) {
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                    getMoreData();
                }
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable("recycler_state");
        }

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        emptyText = (TextView) view.findViewById(R.id.emptyViewText);

        if (recyclerView instanceof AutofitRecyclerView) {
            Log.d("LiveGames", "AutofitRecyclerView");
            internalManager = ((AutofitRecyclerView) recyclerView).getManager();
        }

        if (adapter == null) {
            adapter = new CommunitiesAdapter(CommunitiesFragment.this);
        }
        layoutManager = new LinearLayoutManager(context);
        offsetDecoration = new MarginDecoration(context);
        recyclerView.addItemDecoration(offsetDecoration);
        recyclerView.addOnScrollListener(onScrollListener);
        recyclerView.setAdapter(adapter);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
               updateData();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTitle("Communities");
        }
        if (listState != null && layoutManager != null) {
            layoutManager.onRestoreInstanceState(listState);
        }
        long lastUpdate = adapter.getLastUpdated();
        if (lastUpdate == 0 || adapter.getItemCount() == 0) {
            updateData();
            return;
        }
        long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastUpdate);
        Log.d("timeDiff Communities", "timeDiff: " + timeDiff + " adapter: " + adapter.getLastUpdated());
        if (timeDiff > UPDATE_CYCLE) {
            Log.d("timeDiff Communities", "update Communities");
            updateData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.removeOnScrollListener(onScrollListener);
        internalManager = null;
    }

    private void updateData() {
        forceRefreshAnimation();
        getData();
    }

    private void getData() {
        TwitchKraken.getService().getTopCommunities().enqueue(new Callback<CommunityObject>() {
            @Override
            public void onResponse(Call<CommunityObject> call, Response<CommunityObject> response) {
                if (response.code() == 200) {
                    isLoading = false;
                    isLastPage = false;
                    cursor = response.body().cursor();
                    if (adapter != null) {
                        adapter.clear();
                        adapter.addAll(response.body().communities());
                        adapter.setLastUpdated(System.currentTimeMillis());
                        recyclerView.setAdapter(adapter);
                    }
                    if (cursor.isEmpty()) {
                        isLastPage = true;
                    }
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<CommunityObject> call, Throwable t) {
                Log.d("onFailure", t.getMessage());
                if (t.getMessage().contains("Unable to resolve host")) {
                    Log.d(TAG, "Network issues");
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            forceRefreshAnimation();
                            getData();
                        }
                    };
                    emptyView = new EmptyRecyclerViewAdapter(context,
                            R.string.network_error,
                            R.string.retry_call, listener);
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(emptyView);
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
            }
        });
    }

    private void getMoreData() {
        isLoading = true;
        TwitchKraken.getService().getMoreTopCommunities(cursor).enqueue(new Callback<CommunityObject>() {
            @Override
            public void onResponse(Call<CommunityObject> call, Response<CommunityObject> response) {
                if (response.code() == 200) {
                    isLoading = false;
                    if (adapter != null) {
                        cursor = response.body().cursor();
                        adapter.addMore(response.body().communities());
                        adapter.setLastUpdated(System.currentTimeMillis());
                        if (cursor.isEmpty()) {
                            isLastPage = true;
                        }
                    }
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<CommunityObject> call, Throwable t) {
                Log.d("onFailure", t.getMessage());
                if (t.getMessage().contains("Unable to resolve host")) {
                    Log.d(TAG, "Network issues");
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            forceRefreshAnimation();
                            getData();
                        }
                    };
                    emptyView = new EmptyRecyclerViewAdapter(context,
                            R.string.network_error,
                            R.string.retry_call, listener);
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(emptyView);
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
            }
        });
    }

    private void forceRefreshAnimation() {
        swipeContainer.post(new Runnable() {
            @Override
            public void run() {
                swipeContainer.setRefreshing(true);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        listState = layoutManager.onSaveInstanceState();
        outState.putParcelable("recycler_state", listState);
    }

}
