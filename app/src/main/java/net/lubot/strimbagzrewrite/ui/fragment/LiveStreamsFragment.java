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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;


import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.StrimBagZApplication;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Streams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.data.SpeedRunsLive;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.LoginActivity;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.StreamsAdapter;
import net.lubot.strimbagzrewrite.util.MarginDecoration;
import net.lubot.strimbagzrewrite.util.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveStreamsFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private StreamsAdapter adapter;
    private EmptyRecyclerViewAdapter emptyView;
    private RecyclerView.LayoutManager layoutManager;

    private Context context;
    private String game;
    private int offset;
    private String token;
    private boolean showSRLStreams;

    private boolean isTablet;
    private MarginDecoration offsetDecoration;

    private boolean isActive;

    private final String TAG = "LiveStreamsFragment";

    public static LiveStreamsFragment newInstance() {
        return new LiveStreamsFragment();
    }

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

        if (getArguments() != null) {
            if (getArguments().containsKey("game")) {
                game = getArguments().getString("game");
                offset = 0;
            }
            if (getArguments().containsKey("srl")) {
                showSRLStreams = true;
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
        } else {
            recyclerView.setLayoutManager(layoutManager);
        }
        adapter = new StreamsAdapter((MainActivity) getActivity(), LiveStreamsFragment.this);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LiveStream onResume", "resumed");
        isActive = true;
        if (game != null && !game.isEmpty()) {
            forceRefreshAnimation();
            getSpecificStreams(game, null, 25);
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        getSpecificStreams(game, null, 25);
                    }
                });
            return;
        }

        if (showSRLStreams) {
            forceRefreshAnimation();
            getSRLStreams();
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        getSRLStreams();
                    }
                });
            return;
        }

        if (token != null && !token.isEmpty() && !token.equals(Constants.NO_TOKEN)) {
            forceRefreshAnimation();
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
                        Intent.putExtra("url", Constants.URL_TWITCH_AUTHENTICATION);
                        startActivityForResult(Intent, 1);
                    }
                };
            emptyView = new EmptyRecyclerViewAdapter(context, R.string.channel_name_empty,
                    R.string.preference_login, listener);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(emptyView);
            swipeContainer.setEnabled(false);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        isActive = false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Channel channel = adapter.getSelectedChannel();
        switch(item.getItemId()){
            case R.id.ctn_openStream_Mobile:
                Utils.startPlayerActivity(context, channel, "mobile");
                break;
            case R.id.ctn_openStream_Low:
                Utils.startPlayerActivity(context, channel, "low");
                break;
            case R.id.ctn_openStream_Medium:
                Utils.startPlayerActivity(context, channel, "medium");
                break;
            case R.id.ctn_openStream_High:
                Utils.startPlayerActivity(context, channel, "high");
                break;
            case R.id.ctn_openStream_Source:
                Utils.startPlayerActivity(context, channel, "source");
                break;
            case R.id.ctn_openChatOnly:
                Utils.startChatOnlyActivity(context, channel);
                break;
        }
        return true;
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

    private void getSpecificStreams(String game, String channels, int limit) {
        TwitchKraken.getService().getStreams(game, channels, 0, limit)
                .enqueue(new Callback<LiveStreams>() {
                    @Override
                    public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                        if (response.code() == 200) {
                            if (adapter != null) {
                                adapter.clear();
                                adapter.addAll(response.body().streams());
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

    private void getSRLStreams() {
        SpeedRunsLive.getService().getStreams().enqueue(new Callback<Streams>() {
            @Override
            public void onResponse(Call<Streams> call, Response<Streams> response) {
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
                getSpecificStreams(null, twitchNames, 50);
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

}
