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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedChannels;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.FollowedChannelsAdapter;
import net.lubot.strimbagzrewrite.util.AutofitRecyclerView;
import net.lubot.strimbagzrewrite.util.MarginDecoration;
import net.lubot.strimbagzrewrite.util.Utils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowedChannelsFragment extends Fragment {

    public final String TAG = "FollowedChannels";

    private Context context;
    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private EmptyRecyclerViewAdapter emptyView;
    private GridLayoutManager layoutManager;
    private MarginDecoration offsetDecoration;
    private FollowedChannelsAdapter adapter;

    public static final int PAGE_SIZE = 25;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int currentOffset = 0;
    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

            if (!isLoading && !isLastPage) {
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE) {
                    loadMoreItems();
                }
            }
        }
    };

    private String userID;

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

        if (getActivity() instanceof MainActivity) {
            userID = ((MainActivity) getActivity()).getUserID();
        }

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);

        if (recyclerView instanceof AutofitRecyclerView) {
            layoutManager = ((AutofitRecyclerView) recyclerView).getManager();
        }

        if (adapter == null) {
            adapter = new FollowedChannelsAdapter(FollowedChannelsFragment.this);
        }
        offsetDecoration = new MarginDecoration(context);
        recyclerView.addItemDecoration(offsetDecoration);
        recyclerView.addOnScrollListener(onScrollListener);
        recyclerView.setAdapter(adapter);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });
        forceRefreshAnimation();
        getData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.removeOnScrollListener(onScrollListener);
        layoutManager = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof  MainActivity) {
            ((MainActivity) getActivity()).setTitle("Followed Channels");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final Channel channel = adapter.getSelectedChannel();
        switch(item.getItemId()){
            case R.id.ctn_unfollow:
                new AlertDialog.Builder(context)
                        .setTitle("Unfollow " + channel.displayName() + "?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                unfollowChannel(channel);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing.
                            }
                        }).show();
                break;
            case R.id.ctn_openChatOnly:
                Utils.startChatOnlyActivity(context, channel);
                break;
        }
        return true;
    }

    private void getData() {
        TwitchKraken.getService().getFollowedChannels(userID, 0)
                .enqueue(new Callback<FollowedChannels>() {
                    @Override
                    public void onResponse(Call<FollowedChannels> call,
                                           Response<FollowedChannels> response) {
                        if (response.code() == 200) {
                            isLoading = false;
                            isLastPage = false;
                            currentOffset = 0;
                            if (adapter != null) {
                                adapter.clear();
                                adapter.addAll(response.body().follows());
                                recyclerView.setAdapter(adapter);
                            }
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<FollowedChannels> call, Throwable t) {
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
                            //recyclerView.removeItemDecoration(offsetDecoration);
                            recyclerView.setLayoutManager(layoutManager);
                            recyclerView.setAdapter(emptyView);
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }
                });
    }

    private void loadMoreItems() {
        isLoading = true;
        currentOffset += PAGE_SIZE;
        TwitchKraken.getService().getFollowedChannels(userID, currentOffset)
                .enqueue(new Callback<FollowedChannels>() {
                    @Override
                    public void onResponse(Call<FollowedChannels> call,
                                           Response<FollowedChannels> response) {
                        isLoading = false;
                        if (response.code() == 200) {
                            List<FollowedChannels.FollowedChannel> tmp = response.body().follows();
                            if (tmp.isEmpty()) {
                                isLastPage = true;
                            }
                            if (adapter != null) {
                                for (FollowedChannels.FollowedChannel ch: response.body().follows()) {
                                    adapter.addItem(ch);
                                }
                            }

                            if (tmp.size() >= PAGE_SIZE) {
                                // doing some work here
                            } else {
                                isLastPage = true;
                            }
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<FollowedChannels> call, Throwable t) {
                        Log.d("onFailure", t.getMessage());
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }
                });
    }

    private void unfollowChannel(final Channel channel) {
        TwitchKraken.getService().unfollowChannel(userID, channel.name())
                .enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 204) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Unfollowed " + channel.displayName(),
                                    Toast.LENGTH_SHORT).show();
                            adapter.removeSelectedChannel();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Couldn't unfollow" + channel.displayName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

}
