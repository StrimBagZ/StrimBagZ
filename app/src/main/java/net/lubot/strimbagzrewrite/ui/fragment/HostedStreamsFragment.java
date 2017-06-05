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
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.BuildConfig;
import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedHosting;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.HostingAdapter;
import net.lubot.strimbagzrewrite.util.MarginDecoration;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HostedStreamsFragment extends Fragment {

    public final String TAG = "HostedStreamsFragment";

    private Context context;
    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private EmptyRecyclerViewAdapter emptyView;
    private HostingAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private boolean isTablet;
    private MarginDecoration offsetDecoration;

    private String login;

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

        if (getActivity() instanceof MainActivity) {
            login = ((MainActivity) getActivity()).getLogin();
        }

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        emptyText = (TextView) view.findViewById(R.id.emptyViewText);

        adapter = new HostingAdapter(HostedStreamsFragment.this);
        layoutManager = new LinearLayoutManager(getContext());

        Log.d("isTablet", "tablet: " + isTablet);

        if (isTablet) {
            offsetDecoration = new MarginDecoration(context);
            recyclerView.setHasFixedSize(true);
            recyclerView.addItemDecoration(offsetDecoration);
        } else {
            recyclerView.setLayoutManager(layoutManager);
        }
        recyclerView.setAdapter(adapter);
        registerForContextMenu(recyclerView);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!login.equals(Constants.NO_USER)) {
            forceRefreshAnimation();
            getData();
        }
    }

    public Channel getSelectedChannel() {
        return adapter.getChannel();
    }

    public String getClickedHostingItem() {
        return adapter.getClickedHostingItem();
    }

    private void getData() {
        TwitchAPI.getService().getFollowedHosting(login)
                .enqueue(new Callback<FollowedHosting>() {
                    @Override
                    public void onResponse(Call<FollowedHosting> call, final Response<FollowedHosting> response) {
                        if (response.code() == 200) {
                            if (adapter != null) {
                                prepareData(response.body().hosts());
                                //recyclerView.setLayoutManager(layoutManager);
                                recyclerView.setAdapter(adapter);
                            }
                        }
                        if (swipeContainer.isRefreshing()) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<FollowedHosting> call, Throwable t) {
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

    private void prepareData(List<FollowedHosting.FollowedHosts> hosts) {
        ArrayList<FollowedHosting.FollowedHosts> result = new ArrayList<>();

        ArrayList<String> foundHosts = new ArrayList<>();
        //ArrayList<Integer> foundHostsCounter = new ArrayList<>();
        for (int i = 0, hostsSize = hosts.size(); i < hostsSize; i++) {
            FollowedHosting.FollowedHosts host = hosts.get(i);
            String name = host.target().channel().name();
            if (!foundHosts.contains(name)) {
                ArrayList<FollowedHosting.FollowedHosts> tmp = new ArrayList<>();
                tmp.add(host);
                foundHosts.add(name);
                int hostCount = 1;
                for (int a = 0, size = hosts.size(); a < size; a++) {
                    FollowedHosting.FollowedHosts otherHost = hosts.get(a);
                    // We don't want to compare to the original object
                    if (!host.equals(otherHost)) {
                        if (host.target().equals(otherHost.target())) {
                            Log.d("prepareData", "Found pair of host who both host " + host.target().channel().name());
                            hostCount++;
                            tmp.add(otherHost);
                        }
                    } else {
                        Log.d("prepareData", "original object, not comparing");
                    }
                }
                FollowedHosting.FollowedHosts newHost;
                if (tmp.size() > 1) {
                    newHost = FollowedHosting.FollowedHosts.create(null,
                            null, host.target(), new ArrayList<FollowedHosting.FollowedHosts>());
                    for (int i1 = 0, tmpSize = tmp.size(); i1 < tmpSize; i1++) {
                        FollowedHosting.FollowedHosts h = tmp.get(i1);
                        FollowedHosting.FollowedHosts object = FollowedHosting.FollowedHosts.create(h.name(), h.display_name());
                        newHost.hostedBy().add(object);
                    }
                } else {
                    newHost = FollowedHosting.FollowedHosts.create(host.name(),
                            host.display_name(), host.target(), null);
                }
                result.add(newHost);
                //foundHostsCounter.add(hostCount);
            }
        }
        if (BuildConfig.DEBUG) {
            for (FollowedHosting.FollowedHosts test : result) {
                Log.d("prepare data", test.toString());
            }
        }
        adapter.clear();
        adapter.addAll(result);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
