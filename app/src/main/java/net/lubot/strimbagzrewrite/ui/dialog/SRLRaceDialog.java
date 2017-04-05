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
package net.lubot.strimbagzrewrite.ui.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.FrankerFaceZ.SRLRaceEntrant;
import net.lubot.strimbagzrewrite.ui.adapter.RacesEntrantsAdapter;
import net.lubot.strimbagzrewrite.ui.widget.StopwatchTextView;

import java.util.ArrayList;

public class SRLRaceDialog extends DialogFragment {
    private RecyclerView recyclerView;
    private RacesEntrantsAdapter adapter;

    private TextView game;
    private TextView goal;
    private TextView timer;
    private StopwatchTextView stopwatchTextView;

    public static SRLRaceDialog newInstance(String game, String goal, long startTime, String state,
                                            ArrayList<SRLRaceEntrant> entrants) {
        Bundle bundle = new Bundle();
        bundle.putString("game", game);
        bundle.putString("goal", goal);
        bundle.putLong("startTime", startTime);
        bundle.putString("state", state);
        bundle.putParcelableArrayList("entrants", entrants);
        SRLRaceDialog fragment = new SRLRaceDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        return inflater.inflate(R.layout.list_race_entrants, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stopwatchTextView != null) {
            stopwatchTextView.stop();
            stopwatchTextView = null;
        }
        Log.d("SRLRace", "onDestroyView");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        game = (TextView) view.findViewById(R.id.race_game);
        goal = (TextView) view.findViewById(R.id.race_goal);
        timer = (TextView) view.findViewById(R.id.race_timer);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RacesEntrantsAdapter(this);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        Bundle tmp = getArguments();
        if (tmp != null) {
            String state = tmp.getString("state");
            Log.d("SRLRACE", "State: " + state);
            game.setText(tmp.getString("game"));
            goal.setText("Goal: " + tmp.getString("goal"));
            assert state != null;
            if (state.equals("Progressing")) {
                stopwatchTextView = new StopwatchTextView(timer, 1000);
                stopwatchTextView.startWithGivenTime(tmp.getLong("startTime") * 1000);
            } else if (state.equals("Open") || state.equals("Done")) {
                timer.setText(state);
            }
            ArrayList<SRLRaceEntrant> entrants = tmp.getParcelableArrayList("entrants");
            adapter.addAll(entrants);
            Log.d("SRLRACE", "arguments not null");
        } else {
            Log.d("SRLRACE", "arguments null");
        }
    }

}
