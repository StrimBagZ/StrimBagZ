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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import net.lubot.strimbagzrewrite.BuildConfig;
import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.HoraroAPI;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.data.model.GDQ.Run;
import net.lubot.strimbagzrewrite.data.model.Horaro.RunData;
import net.lubot.strimbagzrewrite.data.model.Horaro.ScheduleData;
import net.lubot.strimbagzrewrite.data.model.Horaro.Ticker;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedHosting;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.GDQScheduleAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.HoraroScheduleAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.TickerAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarathonFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private TickerAdapter tickerAdapter;
    private HoraroScheduleAdapter horaroAdapter;
    private GDQScheduleAdapter gdqAdapter;
    private EmptyRecyclerViewAdapter emptyView;
    private LinearLayoutManager layoutManager;

    private LinearLayout linearLayout;
    private View streamView;
    private ImageView streamPreview;
    private TextView streamTitel;
    private TextView streamChannel;
    private TextView nowPlaying;

    private Activity activity;
    private String scheduleService;
    private boolean usingHoraro = true;
    private String horaroID = "";
    private String marathonName;
    private String marathonChannel;
    private String marathonChannelName;
    private Stream twitchStream;
    private List<FollowedHosting.FollowedHosts> hostingChannels = new ArrayList<>();

    private String login;
    private Timer refreshTimer;
    private final long REFRESH_TIME = 90000;
    private final int UPDATE_TIME = 180;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marathon, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            login = ((MainActivity) getActivity()).getLogin();
        }

        linearLayout = (LinearLayout) view.findViewById(R.id.marathon_root);
        streamView = view.findViewById(R.id.marathon_stream);
        streamPreview = (ImageView) view.findViewById(R.id.previewImage);
        streamPreview.setTransitionName("streamPreview");
        streamTitel = (TextView) view.findViewById(R.id.streamTitle);
        streamChannel = (TextView) view.findViewById(R.id.hostingTarget);
        nowPlaying = (TextView) view.findViewById(R.id.nowPlaying);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        emptyText = (TextView) view.findViewById(R.id.emptyViewText);
        layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        swipeContainer.setEnabled(false);

        if (savedInstanceState != null) {
            usingHoraro = savedInstanceState.getBoolean("usingHoraro");
            horaroID = savedInstanceState.getString("horaroID");
            marathonName = savedInstanceState.getString("marathonName");
            marathonChannel = savedInstanceState.getString("marathonChannel");
            scheduleService = savedInstanceState.getString("scheduleService");
        } else if (getActivity() instanceof MainActivity) {
            FirebaseRemoteConfig remoteConfig = ((MainActivity) getActivity()).getRemoteConfig();
            scheduleService = remoteConfig.getString(Constants.MARATHON_SCHEDULE_SERVICE);
            usingHoraro = scheduleService.equals("horaro");
            if (usingHoraro) {
                horaroID = remoteConfig.getString(Constants.MARATHON_HORARO_ID);
            }
            marathonName = remoteConfig.getString(Constants.MARATHON_NAME);
            marathonChannel = remoteConfig.getString(Constants.MARATHON_CHANNEL_ID);
            marathonChannelName = remoteConfig.getString(Constants.MARATHON_CHANNEL);
        }

        if (usingHoraro) {
            //tickerAdapter = new TickerAdapter(MarathonFragment.this);
            horaroAdapter = new HoraroScheduleAdapter(MarathonFragment.this);
            recyclerView.setAdapter(horaroAdapter);
        }
        if (scheduleService.equals("gdq")) {
            gdqAdapter = new GDQScheduleAdapter(MarathonFragment.this);
            recyclerView.setAdapter(gdqAdapter);
        }

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (usingHoraro) {
                    getData(horaroID);
                } else {
                    getGDQData();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTitle(marathonName);
        }
        if (usingHoraro && horaroID != null) {
            getData(horaroID);
        }
        if (scheduleService.equals("gdq")) {
            long lastUpdate = gdqAdapter.getLastUpdated();
            if (lastUpdate == 0 || gdqAdapter.getItemCount() == 0) {
                Log.d("Marathon", "Loading GDQ schedule");
                getGDQData();
                getSpecificStream(marathonChannel);
                return;
            }
            long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastUpdate);
            if (timeDiff > UPDATE_TIME) {
                Log.d("Marathon", "Loading GDQ schedule");
                getGDQData();
            } else {
                scrollToCurrentTime(gdqAdapter.getData());
            }
        }
        if (marathonChannel != null) {
            getSpecificStream(marathonChannel);
        }
        Log.d("onResume Marathon", "Starting refreshTimer");
        //startRefreshTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("onPause Marathon", "Cancel refreshTimer");
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
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
                    if (scheduleService.equals("gdq")) {
                        getGDQData();
                    }
                    getSpecificStream(marathonChannel);
                }
            }
        }, REFRESH_TIME, REFRESH_TIME);
    }

    private void getData(String id) {
        HoraroAPI.getService().getSchedule(id).enqueue(new Callback<ScheduleData>() {
            @Override
            public void onResponse(Call<ScheduleData> call, final Response<ScheduleData> response) {
                if (response.code() == 200 && isVisible()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final ScheduleData.Schedule schedule = response.body().data();
                            horaroAdapter.addAll(schedule.columns(), schedule.runs());
                            scrollToCurrentTime(schedule.setup(), schedule.runs());
                            if (twitchStream == null) {
                                final Calendar start = Calendar.getInstance();
                                start.setTimeInMillis(schedule.start() * 1000);
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //streamTitel.setText(schedule.name());
                                        nowPlaying.setText("Starting at " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Resources.getSystem().getConfiguration().locale).format(start.getTime()));
                                    }
                                });
                            }
                            //Snackbar.make(linearLayout, "Successfully updated schedule", Snackbar.LENGTH_LONG);
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
            public void onFailure(Call<ScheduleData> call, Throwable t) {
                if (isVisible() && swipeContainer.isRefreshing()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeContainer.setRefreshing(false);
                        }
                    });
                }
            }
        });
    }

    private void getGDQData() {
        HoraroAPI.getService().getGDQData(Constants.URL_GDQ_SCHEDULE).enqueue(new Callback<List<Run>>() {
            @Override
            public void onResponse(Call<List<Run>> call, final Response<List<Run>> response) {
                if (response.code() == 200 && isVisible()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gdqAdapter.addAll(response.body());
                            gdqAdapter.setLastUpdated(System.currentTimeMillis());
                            scrollToCurrentTime(response.body());
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
            public void onFailure(Call<List<Run>> call, Throwable t) {
                Log.d("GDQ Failure", t.getMessage());
                if (isVisible() && swipeContainer.isRefreshing()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeContainer.setRefreshing(false);
                        }
                    });
                }
            }
        });
    }

    private void scrollToCurrentTime(List<Run> runs) {
        if (gdqAdapter != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Resources.getSystem().getConfiguration().locale);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0, runsSize = runs.size(); i < runsSize; i++) {
                Run run = runs.get(i);
                try {
                    Date date = dateFormat.parse(run.endTime());
                    Calendar endTime = Calendar.getInstance();
                    endTime.setTimeInMillis(date.getTime());
                    Map<TimeUnit, Long> timeDiff =
                            Utils.computeTimeDiff(currentTime, endTime);
                    long diffDay = timeDiff.get(TimeUnit.DAYS);
                    long diffHr = timeDiff.get(TimeUnit.HOURS);
                    long diffMin = timeDiff.get(TimeUnit.MINUTES);
                    // if diffMin is lower than 0, the Run has ended
                    // else it's still going
                    Log.d("Run diff", " diffDay: " + diffDay  + " diffHr: " + diffHr + " diffMin: " + diffMin);
                    if (diffMin >= 0 && diffHr >= 0 && diffDay >= 0) {
                        Log.d("Run diff", "found current run");
                        if (i != 0) {
                            gdqAdapter.markCurrentRun(i);
                            layoutManager.scrollToPositionWithOffset(i, 0);
                        }
                        break;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void scrollToCurrentTime(long setup, List<RunData> runs) {
        if (horaroAdapter != null) {
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0, runsSize = runs.size(); i < runsSize; i++) {
                RunData run = runs.get(i);
                Calendar endTime = Calendar.getInstance();
                endTime.setTimeInMillis((run.scheduled() + run.length() + setup) * 1000);
                Map<TimeUnit, Long> timeDiff =
                        Utils.computeTimeDiff(currentTime, endTime);
                long diffDay = timeDiff.get(TimeUnit.DAYS);
                long diffHr = timeDiff.get(TimeUnit.HOURS);
                long diffMin = timeDiff.get(TimeUnit.MINUTES);
                // if diffMin is lower than 0, the Run has ended
                // else it's still going
                Log.d("Run diff", " diffDay: " + diffDay  + " diffHr: " + diffHr + " diffMin: " + diffMin);
                if (diffMin >= 0 && diffHr >= 0 && diffDay >= 0) {
                    Log.d("Run diff", "found current run");
                    if (i != 0) {
                        horaroAdapter.markCurrentRun(i);
                        layoutManager.scrollToPositionWithOffset(i, 0);
                    }
                    break;
                }
            }
        }
    }

    private void getSpecificStream(final String channel) {
        TwitchKraken.getService().getStreams(null, channel, 0, 1, 5).enqueue(new Callback<LiveStreams>() {
            @Override
            public void onResponse(Call<LiveStreams> call, final Response<LiveStreams> response) {
                if (response.code() == 200) {
                    if (!response.body().streams().isEmpty()) {
                        final Stream stream = response.body().streams().get(0);
                        if (!isVisible()) {
                            return;
                        }
                        getHostingChannels();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                twitchStream = stream;
                                linearLayout.setClickable(true);
                                streamView.setClickable(true);
                                streamChannel.setText(stream.channel().displayName());
                                streamTitel.setText(stream.channel().status());
                                String tmp = "Playing " + stream.channel().game() + " for " +
                                        NumberFormat.getInstance().format(stream.viewers()) + " viewers";
                                nowPlaying.setText(tmp);
                                Glide.with(activity)
                                        .load(stream.preview().medium())
                                        .centerCrop()
                                        .signature(new StringSignature(twitchStream.channel().name() + twitchStream.channel().updated_at()))
                                        .into(streamPreview);
                                streamView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (!marathonChannel.equals("22510310")) {
                                            Utils.startMarathonPlayerActivity(activity, twitchStream, horaroID);
                                        } else {
                                            streamView.showContextMenu();
                                        }
                                    }
                                });
                                streamView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                                    @Override
                                    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                                        if (marathonChannel.equals("22510310")) {
                                            SubMenu restreams = menu.addSubMenu("Open Restream");
                                            restreams.setHeaderTitle("Select Restream");
                                            restreams.add("LeFrenchRestream (Français)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem menuItem) {
                                                    if (activity instanceof MainActivity) {
                                                        ((MainActivity) activity).startStream("lefrenchrestream");
                                                    }
                                                    return true;
                                                }
                                            });
                                            restreams.add("Goldensplit (Русский)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem menuItem) {
                                                    if (activity instanceof MainActivity) {
                                                        ((MainActivity) activity).startStream("goldensplit");
                                                    }
                                                    return true;
                                                }
                                            });
                                            restreams.add("GermenchRestream (Deutsch)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem menuItem) {
                                                    if (activity instanceof MainActivity) {
                                                        ((MainActivity) activity).startStream("germenchrestream");
                                                    }
                                                    return true;
                                                }
                                            });
                                            restreams.add("Japanese_Restream (日本語)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem menuItem) {
                                                    if (activity instanceof MainActivity) {
                                                        ((MainActivity) activity).startStream("japanese_restream");
                                                    }
                                                    return true;
                                                }
                                            });
                                        }
                                        if (hostingChannels != null && !hostingChannels.isEmpty()) {
                                            if (marathonChannel.equals("22510310")) {
                                                menu.setHeaderTitle("Choose non-cancerous Chat");
                                            } else {
                                                menu.setHeaderTitle("Choose hosted Chat");
                                            }
                                            for (final FollowedHosting.FollowedHosts host : hostingChannels) {
                                                menu.add(host.display_name()).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                    @Override
                                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                                        Utils.startPlayerActivity(activity, twitchStream, host.name());
                                                        return true;
                                                    }
                                                });
                                            }
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
        outState.putString("scheduleService", scheduleService);
    }

    private void getOfflineChannel(String ch) {
        TwitchKraken.getService().getChannel(ch, 5).enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {
                if (response.code() == 200) {
                    final Channel channel = response.body();
                    if (!isVisible()) {
                        return;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linearLayout.setClickable(false);
                            streamView.setClickable(false);
                            streamChannel.setText(channel.displayName());
                            if (!marathonChannel.equals("22510310")) {
                                streamTitel.setText(channel.status());
                                //streamTitel.setText(tickerAdapter.getName());
                            } else {
                                // GDQ case TODO
                                streamTitel.setText("Pre-Show (Any%)");
                            }
                            /*
                            try {
                                if (!marathonChannel.equals("gamesdonequick")) {
                                    nowPlaying.setText("Starting at " + dateFormat.parse(tickerAdapter.getStart()));
                                } else {
                                    // GDQ case TODO
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            */
                            if (channel.videoBanner() != null) {
                                Glide.with(activity)
                                        .load(channel.videoBanner())
                                        .centerCrop()
                                        .signature(new StringSignature(channel.name() + channel.updated_at()))
                                        .into(streamPreview);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {

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

    private void getHostingChannels() {
        TwitchAPI.getService().getFollowedHosting(login)
                .enqueue(new Callback<FollowedHosting>() {
                    @Override
                    public void onResponse(Call<FollowedHosting> call, final Response<FollowedHosting> response) {
                        if (response.code() == 200 && response.body().hosts() != null && !response.body().hosts().isEmpty()) {
                            prepareHostingData(response.body().hosts());
                        }
                    }

                    @Override
                    public void onFailure(Call<FollowedHosting> call, Throwable t) {
                        Log.d("onFailure", t.getMessage());
                    }
                });
    }

    private void prepareHostingData(List<FollowedHosting.FollowedHosts> hosts) {
        FollowedHosting.FollowedHosts result = null;
        for (int i = 0, hostsSize = hosts.size(); i < hostsSize; i++) {
            FollowedHosting.FollowedHosts host = hosts.get(i);
            String name = host.target().channel().name();
            if (name.equals(marathonChannelName)) {
                ArrayList<FollowedHosting.FollowedHosts> tmp = new ArrayList<>();
                tmp.add(host);
                for (int a = 0, size = hosts.size(); a < size; a++) {
                    FollowedHosting.FollowedHosts otherHost = hosts.get(a);
                    // We don't want to compare to the original object
                    if (!host.equals(otherHost)) {
                        if (host.target().equals(otherHost.target())) {
                            Log.d("prepareData", "Found pair of host who both host " + host.target().channel().name());
                            tmp.add(otherHost);
                        }
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
                    newHost = FollowedHosting.FollowedHosts.create(null,
                            null, host.target(), new ArrayList<FollowedHosting.FollowedHosts>());
                    newHost.hostedBy().add(FollowedHosting.FollowedHosts.create(host.name(), host.display_name()));
                }
                result = newHost;
                break;
            }
        }
        if (result != null && result.hostedBy() != null) {
            Log.d("hostingChannels", "hosted by: " + result.hostedBy());
            hostingChannels.clear();
            hostingChannels.addAll(result.hostedBy());
        }
    }

}
