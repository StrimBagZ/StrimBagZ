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
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.HoraroAPI;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.data.model.Horaro.Ticker;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.TickerAdapter;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarathonFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private TickerAdapter adapter;
    private EmptyRecyclerViewAdapter emptyView;
    private RecyclerView.LayoutManager layoutManager;

    private LinearLayout linearLayout;
    private View streamView;
    private ImageView streamPreview;
    private TextView streamTitel;
    private TextView streamChannel;
    private TextView nowPlaying;

    private Context context;
    private boolean usingHoraro = true;
    private String horaroID;
    private String marathonName;
    private String marathonChannel;

    private Timer refreshTimer;
    private final long REFRESH_TIME = 90000;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marathon, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        linearLayout = (LinearLayout) view.findViewById(R.id.marathon_root);
        streamView = view.findViewById(R.id.marathon_stream);
        streamPreview = (ImageView) view.findViewById(R.id.previewImage);
        streamTitel = (TextView) view.findViewById(R.id.streamTitle);
        streamChannel = (TextView) view.findViewById(R.id.hostingTarget);
        nowPlaying = (TextView) view.findViewById(R.id.nowPlaying);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        emptyText = (TextView) view.findViewById(R.id.emptyViewText);
        layoutManager = new LinearLayoutManager(context);
        adapter = new TickerAdapter(MarathonFragment.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        swipeContainer.setEnabled(false);

        if (savedInstanceState != null) {
            usingHoraro = savedInstanceState.getBoolean("usingHoraro");
            horaroID = savedInstanceState.getString("horaroID");
            marathonName = savedInstanceState.getString("marathonName");
            marathonChannel = savedInstanceState.getString("marathonChannel");
        } else if (getActivity() instanceof MainActivity) {
            FirebaseRemoteConfig remoteConfig = ((MainActivity) getActivity()).getRemoteConfig();
            usingHoraro = remoteConfig.getString(Constants.MARATHON_SCHEDULE_SERVICE).equals("horaro");
            if (usingHoraro) {
                horaroID = remoteConfig.getString(Constants.MARATHON_HORARO_ID);
            }
            marathonName = remoteConfig.getString(Constants.MARATHON_NAME);
            marathonChannel = remoteConfig.getString(Constants.MARATHON_CHANNEL);
        }

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (usingHoraro) {
                    getData(horaroID);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTitle("Marathon: " + marathonName);
        }
        if (marathonChannel != null) {
            getSpecificStream(marathonChannel);
        }
        if (usingHoraro && horaroID != null) {
            getData(horaroID);
        }
        Log.d("onResume Marathon", "Starting refreshTimer");
        startRefreshTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("onPause Marathon", "Cancel refreshTimer");
        refreshTimer.cancel();
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isVisible()) {
                    if (usingHoraro) {
                        getData(horaroID);
                    }
                    getSpecificStream(marathonChannel);
                }
            }
        }, REFRESH_TIME, REFRESH_TIME);
    }

    private void getData(String id) {
        HoraroAPI.getService().getTicker(id).enqueue(new Callback<Ticker>() {
            @Override
            public void onResponse(Call<Ticker> call, final Response<Ticker> response) {
                if (response.code() == 200 && isVisible()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addAll(response.body());
                            Snackbar.make(linearLayout, "Successfully updated schedule", Snackbar.LENGTH_LONG);
                        }
                    });
                }
                if (isVisible() && swipeContainer.isRefreshing()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeContainer.setRefreshing(false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Ticker> call, Throwable t) {

            }
        });
    }

    private void getSpecificStream(final String channel) {
        TwitchKraken.getService().getStreams(null, channel, 0, 1).enqueue(new Callback<LiveStreams>() {
            @Override
            public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                if (response.code() == 200) {
                    if (!response.body().streams().isEmpty()) {
                        final Stream stream = response.body().streams().get(0);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                streamChannel.setText(stream.channel().displayName());
                                streamTitel.setText(stream.channel().status());
                                String tmp = "Playing " + stream.channel().game() + " for " +
                                        NumberFormat.getInstance().format(stream.viewers()) + " viewers";
                                nowPlaying.setText(tmp);
                                Glide.with(context)
                                        .load(stream.preview().large())
                                        .centerCrop()
                                        .signature(new StringSignature(stream.channel().name() + stream.channel().updated_at()))
                                        .into(streamPreview);
                                streamView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).startStream(channel);
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        getOfflineChannel(channel);
                    }
                }
            }

            @Override
            public void onFailure(Call<LiveStreams> call, Throwable t) {

            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("usingHoraro", usingHoraro);
        outState.putString("horaroID", horaroID);
        outState.putString("marathonName", marathonName);
        outState.putString("marathonChannel", marathonChannel);
    }

    private void getOfflineChannel(String channel) {
        //TODO DO IT.
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
