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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Streams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.data.SpeedRunsLive;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.LoginActivity;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.StreamsAdapter;
import net.lubot.strimbagzrewrite.util.AutofitRecyclerView;
import net.lubot.strimbagzrewrite.util.MarginDecoration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveStreamsFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private StreamsAdapter adapter;
    private EmptyRecyclerViewAdapter emptyView;
    private Parcelable listState;
    private GridLayoutManager internalManager;
    private LinearLayoutManager layoutManager;

    private Context context;
    private String title;
    private String game;
    private String community;
    private boolean searchMode;
    private String searchQuery;
    private int offset;
    private String token;
    private boolean showSRLStreams;
    private boolean showCommunityStreams;

    private boolean isTablet;
    private MarginDecoration offsetDecoration;

    private boolean isActive;
    private final int UPDATE_CYCLE = 120;

    public static final int PAGE_SIZE = 25;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (dy > 0) {
                int visibleItemCount;
                int totalItemCount;
                int firstVisibleItemPosition;

                if (isTablet) {
                    visibleItemCount = internalManager.getChildCount();
                    totalItemCount = internalManager.getItemCount();
                    firstVisibleItemPosition = internalManager.findFirstVisibleItemPosition();
                } else {
                    visibleItemCount = layoutManager.getChildCount();
                    totalItemCount = layoutManager.getItemCount();
                    firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                }

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        // GET MORE
                        if (game != null && !game.isEmpty()) {
                            getSpecificStreams(game, null, null);
                        }
                    }
                }
            }
        }
    };

    private final String TAG = "LiveStreamsFragment";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isTablet = context.getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            Log.d(TAG, "inflate grid");
            return inflater.inflate(R.layout.list_grid_streams, container, false);
        }
        return inflater.inflate(R.layout.list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable("recycler_state");
        }

        if (getArguments() != null) {
            if (getArguments().containsKey("search")) {
                searchMode = true;
            }
            if (getArguments().containsKey("game")) {
                game = getArguments().getString("game");
                offset = 0;
            }
            if (getArguments().containsKey("srl")) {
                showSRLStreams = true;
            }
            if (getArguments().containsKey("community")) {
                community = getArguments().getString("community");
                title = getArguments().getString("title");
                showCommunityStreams = true;
            }
        } else if (getActivity() instanceof MainActivity) {
            token = ((MainActivity) getActivity()).getToken();
        }

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        layoutManager = new LinearLayoutManager(context);
        Log.d("isTablet", "tablet: " + isTablet);

        if (isTablet) {
            offsetDecoration = new MarginDecoration(context);
            recyclerView.setHasFixedSize(true);
            recyclerView.addItemDecoration(offsetDecoration);
            if (recyclerView instanceof AutofitRecyclerView) {
                Log.d("LiveStreams", "AutofitRecyclerView");
                internalManager = ((AutofitRecyclerView) recyclerView).getManager();
            }
        } else {
            recyclerView.setLayoutManager(layoutManager);
        }
        if (adapter == null) {
            adapter = new StreamsAdapter((MainActivity) getActivity(), LiveStreamsFragment.this);
        }
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(onScrollListener);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LiveStream onResume", "resumed");
        isActive = true;
        if (listState != null && layoutManager != null) {
            layoutManager.onRestoreInstanceState(listState);
        }
        if (title != null && !title.isEmpty()) {
            ((MainActivity) getActivity()).setTitle(title);
        }
        long lastUpdate = adapter.getLastUpdated();
        if (lastUpdate == 0 || adapter.getItemCount() == 0) {
            updateData();
            return;
        }
        long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastUpdate);
        Log.d("timeDiff Streams", "timeDiff: " + timeDiff + " adapter: " + adapter.getLastUpdated());
        if (timeDiff > UPDATE_CYCLE) {
            Log.d("timeDiff Streams", "update streams");
            updateData();
        }
    }

    private void updateData() {
        offset = 0;
        isLastPage = false;
        if (searchMode) {
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    searchStreams(searchQuery);
                }
            });
            return;
        }

        forceRefreshAnimation();
        if (game != null && !game.isEmpty()) {
            getSpecificStreams(game, null, null);
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getSpecificStreams(game, null, null);
                }
            });
            return;
        }

        if (showSRLStreams) {
            getSRLStreams();
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getSRLStreams();
                }
            });
            return;
        }

        if (showCommunityStreams) {
            getCommunityStreams();
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getCommunityStreams();
                }
            });
            return;
        }

        if (token != null && !token.isEmpty() && !token.equals(Constants.NO_TOKEN)) {
            getData();
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getData();
                }
            });
        } else {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent Intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(Intent);
                }
            };
            emptyView = new EmptyRecyclerViewAdapter(context, R.string.channel_name_empty,
                    R.string.preference_login, listener);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(emptyView);
            swipeContainer.setEnabled(false);
        }
    }

    private void loadMoreData() {
        Log.d("loadMoreData", "isLastPage: " + isLastPage);
        if (game != null && !game.isEmpty()) {
            getSpecificStreams(game, null, null);
            return;
        }

        if (showCommunityStreams) {
            getCommunityStreams();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isActive = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LiveStream omDestroy", "destroying");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("LiveStream oDetach", "detaching");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.removeOnScrollListener(onScrollListener);
        if (internalManager != null) {
            internalManager = null;
        }
    }

    public Channel getSelectedChannel() {
        return adapter.getSelectedChannel();
    }

    private void getData() {
        TwitchKraken.getService().getLiveStreams()
                .enqueue(new Callback<LiveStreams>() {
                    @Override
                    public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                        if (!isActive) {
                            return;
                        }
                        if (response.code() == 200) {
                            if (adapter != null) {
                                adapter.clear();
                                List<Stream> streams = response.body().streams();
                                if (!streams.isEmpty()) {
                                    adapter.addAll(streams);
                                    adapter.setLastUpdated(System.currentTimeMillis());
                                    if (!isTablet) {
                                        recyclerView.setLayoutManager(layoutManager);
                                    }
                                    recyclerView.setAdapter(adapter);
                                } else {
                                    Log.d(TAG, "empty stream list");
                                    View.OnClickListener listener = new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            forceRefreshAnimation();
                                            getData();
                                        }
                                    };
                                    emptyView = new EmptyRecyclerViewAdapter(context,
                                            R.string.following_empty_live,
                                            R.string.refresh, listener);
                                    recyclerView.setLayoutManager(layoutManager);
                                    recyclerView.setAdapter(emptyView);
                                }
                            }
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<LiveStreams> call, Throwable t) {
                        Log.d("onFailure", t.getMessage());
                        if (!isActive) {
                            return;
                        }
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
                            if (recyclerView != null) {
                                recyclerView.setLayoutManager(layoutManager);
                                recyclerView.setAdapter(emptyView);
                            }
                        }
                        if (swipeContainer != null && swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }
                });
    }


    /*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_games, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    */

    private void getSpecificStreams(String game, String channels, String community) {
        isLoading = true;
        TwitchKraken.getService().getStreams(game, channels, offset, PAGE_SIZE, 3)
                .enqueue(new Callback<LiveStreams>() {
                    @Override
                    public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                        isLoading = false;
                        if (!isActive) {
                            return;
                        }
                        if (response.code() == 200) {
                            List<Stream> streams = response.body().streams();
                            if (streams.isEmpty()) {
                                isLastPage = true;
                                return;
                            }
                            if (streams.size() >= PAGE_SIZE) {

                            } else {
                                isLastPage = true;
                            }
                            if (adapter != null) {
                                if (offset == 0) {
                                    adapter.clear();
                                    adapter.addAll(streams);
                                } else {
                                    adapter.addMore(streams);
                                }
                                adapter.setLastUpdated(System.currentTimeMillis());
                                offset += PAGE_SIZE;
                            }
                        }
                        if (response.code() == 401) {
                            recyclerView.setVisibility(View.GONE);
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<LiveStreams> call, Throwable t) {
                        isLoading = false;
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

    private void getSRLStreams() {
        SpeedRunsLive.getService().getStreams().enqueue(new Callback<Streams>() {
            @Override
            public void onResponse(Call<Streams> call, Response<Streams> response) {
                if (!isActive) {
                    return;
                }
                if (response.code() != 200) {
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            forceRefreshAnimation();
                            getSRLStreams();
                        }
                    };
                    emptyView = new EmptyRecyclerViewAdapter(context,
                            R.string.srl_streams_error,
                            R.string.retry_call, listener);
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(emptyView);
                    if (swipeContainer.isRefreshing()) {
                        swipeContainer.setRefreshing(false);
                    }
                    return;
                }
                List<Streams.StreamsChannels> channels = response.body().source().channels();
                Collections.sort(channels, new Comparator<Streams.StreamsChannels>() {
                    @Override
                    public int compare(Streams.StreamsChannels streamsChannels, Streams.StreamsChannels t1) {
                        return streamsChannels.current_viewers() < t1.current_viewers() ? 1
                                : streamsChannels.current_viewers() > t1.current_viewers() ? -1
                                : 0;
                    }
                });
                String twitchNames = "";
                for (int i = 0, channelsSize = channels.size(); i < channelsSize; i++) {
                    Streams.StreamsChannels channel = channels.get(i);
                    if (channel.api() != null && channel.api().equals("twitch")
                            && channel.name() != null) {
                        twitchNames += channel.name();
                        if (i != (channelsSize - 1)) {
                            twitchNames += ",";
                        }
                    }
                }
                getSpecificStreams(null, twitchNames, null);
            }

            @Override
            public void onFailure(Call<Streams> call, Throwable t) {
                Log.d("onFailure", "SRL " + t.getMessage());
                if (t.getMessage().contains("Unable to resolve host")) {
                    Log.d(TAG, "Network issues");
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            forceRefreshAnimation();
                            getSRLStreams();
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

    private void getCommunityStreams() {
        isLoading = true;
        TwitchKraken.getService().getCommunityStreams(community, offset, PAGE_SIZE)
                .enqueue(new Callback<LiveStreams>() {
            @Override
            public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                if (!isActive) {
                    return;
                }
                if (response.code() == 200) {
                    if (adapter != null) {
                        List<Stream> streams = response.body().streams();
                        if (streams.isEmpty()) {
                            isLastPage = true;
                            return;
                        }
                        if (streams.size() >= PAGE_SIZE) {

                        } else {
                            isLastPage = true;
                        }
                        if (offset == 0) {
                            adapter.clear();
                            adapter.addAll(streams);
                        } else {
                            adapter.addMore(streams);
                        }
                        adapter.setLastUpdated(System.currentTimeMillis());
                        offset += PAGE_SIZE;
                    }
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
                isLoading = false;
            }

            @Override
            public void onFailure(Call<LiveStreams> call, Throwable t) {
                Log.d("onFailure", "Community " + t.getMessage());
                if (t.getMessage().contains("Unable to resolve host")) {
                    Log.d(TAG, "Network issues");
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            forceRefreshAnimation();
                            getSRLStreams();
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

    public void setSearchQuery(String query) {
        this.searchQuery = query;
    }

    public void searchStreams() {
        searchStreams(searchQuery);
    }

    private void searchStreams(String query) {
        TwitchKraken.getService().searchStreams(query).enqueue(new Callback<LiveStreams>() {
            @Override
            public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                if (response.code() == 200) {
                    if (adapter != null) {
                        adapter.clear();
                        adapter.addAll(response.body().streams());
                        adapter.setLastUpdated(System.currentTimeMillis());
                    }
                }
                if (response.code() == 401) {
                    recyclerView.setVisibility(View.GONE);
                }
                if (swipeContainer.isRefreshing()) {
                    swipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<LiveStreams> call, Throwable t) {
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
