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
package net.lubot.strimbagzrewrite.ui.adapter;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Races;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.RaceActivity;

import java.util.ArrayList;

public class RacesAdapter extends RecyclerView.Adapter<RacesAdapter.ViewHolder> {

    private Fragment fragment;
    private ArrayList<Races.Race> data;

    public RacesAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_races, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<Races.Race> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Races.Race race = data.get(position);
        holder.game.setText(race.game.name);
        holder.goal.setText(race.goal);
        holder.entrantsCount.setText("Entrants: " + race.entrants.size());
        Glide.with(fragment)
                .load("http://cdn.speedrunslive.com/images/games/" + race.game.abbrev + ".jpg")
                .placeholder(R.drawable.srl_race_placeholder)
                .signature(new StringSignature(System.currentTimeMillis() + ""))
                .centerCrop()
                .into(holder.preview);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener {
        private TextView game;
        private TextView goal;
        private TextView entrantsCount;
        private ImageView preview;

        public ViewHolder(View item) {
            super(item);
            game = (TextView) item.findViewById(R.id.raceGame);
            goal = (TextView) item.findViewById(R.id.raceGoal);
            entrantsCount = (TextView) item.findViewById(R.id.raceEntrants);
            preview = (ImageView) item.findViewById(R.id.gameImage);
            item.setOnClickListener(this);
            item.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            Races.Race race = data.get(getAdapterPosition());
            Intent intent = new Intent(fragment.getActivity(), RaceActivity.class);
            intent.putExtra("race", race);
            fragment.getActivity().startActivity(intent);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo menuInfo) {

        }
    }
}
