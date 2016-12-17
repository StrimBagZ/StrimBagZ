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

import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Entrant;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Races;
import net.lubot.strimbagzrewrite.data.SpeedRunsLive;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.RacesAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RacesFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private EmptyRecyclerViewAdapter emptyView;
    private RacesAdapter adapter;

    private final String TAG = "RacesFragment";
    private final int MAX_RACE_CACHE = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        emptyText = (TextView) view.findViewById(R.id.emptyViewText);

        adapter = new RacesAdapter(RacesFragment.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

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
        if (isVisible()) {
            getData();
        }
    }

    private void getData() {
        SpeedRunsLive.getService().getRaces().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ArrayList<Races.Race> races = prepareData(response.body().string());
                    if (races != null && !races.isEmpty()) {
                        adapter.clear();
                        adapter.addAll(races);
                    } else {
                        Log.d(TAG, "empty race list");
                        emptyView = new EmptyRecyclerViewAdapter(getContext(), R.string.races_empty);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        recyclerView.setAdapter(emptyView);
                    }
                    if (swipeContainer.isRefreshing()) {
                        swipeContainer.setRefreshing(false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("onFailure", t.getMessage());
            }
        });

    }

    private ArrayList<Races.Race> prepareData(String json) {
        ArrayList<Races.Race> races = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(json);
            JSONArray rac = object.getJSONArray("races");
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0; i < rac.length(); i++) {
                JSONObject row = rac.getJSONObject(i);
                String state = row.getString("statetext");
                long numentrants = row.getLong("numentrants");
                // We don't care about Races that are done and empty.
                if (!state.equals("Complete") && numentrants != 0) {
                    long firstPlaceTime = row.getLong("time");
                    long diffInHr = 0;
                    if (firstPlaceTime != 0) {
                        Calendar firstPlace = Calendar.getInstance();
                        firstPlace.setTimeInMillis(firstPlaceTime * 1000);
                        Map<TimeUnit, Long> timeDiff =
                                Utils.computeTimeDiff(firstPlace, currentTime);
                        diffInHr = timeDiff.get(TimeUnit.HOURS);
                    }
                    Log.d("timeDiff race", "hours: " + diffInHr);

                    // Additionally, if the first entrant is done, we remove the race if it's
                    // MAX_RACE_CACHE hours old, to keep incomplete races out
                    // (e.g. entrant(s) didn't forfeit)
                    if (diffInHr < MAX_RACE_CACHE) {
                        String id = row.getString("id");
                        Races.RaceGame game =
                                new Races.RaceGame(row.getJSONObject("game").getString("name"),
                                        row.getJSONObject("game").getString("abbrev"));
                        String goal = row.getString("goal");
                        if (goal.isEmpty()) {
                            goal = "No goal set";
                        }
                        ArrayList<Entrant> entrants = new ArrayList<>();
                        JSONObject entrantsJSON = row.getJSONObject("entrants");
                        Iterator<String> keys = entrantsJSON.keys();
                        while (keys.hasNext()) {
                            JSONObject entrant = entrantsJSON.getJSONObject(keys.next());
                            String displayName = entrant.getString("displayname");
                            long place = entrant.getLong("place");
                            long time = entrant.getLong("time");
                            String message = null;
                            if (entrant.getString("message") != null) {
                                message = entrant.getString("message");
                            }
                            String statetext = entrant.getString("statetext");
                            String twitch = entrant.getString("twitch");
                            String trueskill = entrant.getString("trueskill");
                            entrants.add(new Entrant(displayName, place, time, message, statetext,
                                    twitch, trueskill));
                        }
                        races.add(new Races.Race(id, game, goal, entrants, state));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return races;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
