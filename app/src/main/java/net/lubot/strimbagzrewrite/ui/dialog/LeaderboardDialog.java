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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.SpeedrunCom.Record;
import net.lubot.strimbagzrewrite.ui.adapter.LeaderboardAdapter;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardDialog extends DialogFragment {
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private LeaderboardAdapter leaderboardAdapter;
    private Button closeButton;

    private TextView game;

    public static LeaderboardDialog newInstance(String game, List<Record.RecordData> runs) {
        ArrayList<Record.RecordData> tmp = new ArrayList<>(runs);
        Bundle bundle = new Bundle();
        bundle.putString("game", game);
        bundle.putParcelableArrayList("runs", tmp);
        LeaderboardDialog fragment = new LeaderboardDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        return inflater.inflate(R.layout.list_leaderboard_dialog, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        game = (TextView) view.findViewById(R.id.leaderboard_game);
        closeButton = (Button) view.findViewById(R.id.leaderboard_btn_close);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        leaderboardAdapter = new LeaderboardAdapter(this);
        recyclerView.setAdapter(leaderboardAdapter);
        Bundle tmp = getArguments();
        if (tmp != null) {
            game.setText(tmp.getString("game"));
            ArrayList<Record.RecordData> data = tmp.getParcelableArrayList("runs");
            leaderboardAdapter.addAll(data);
        }
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

}
