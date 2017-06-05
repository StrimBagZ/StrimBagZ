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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.SpeedrunCom.Record;
import net.lubot.strimbagzrewrite.data.model.Twitch.Community;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private Fragment fragment;
    private ArrayList<Record.RecordData> data;

    public LeaderboardAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<Record.RecordData> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    public void addMore(ArrayList<Record.RecordData> data) {
        int position = getItemCount();
        this.data.addAll(data);
        notifyItemRangeInserted(position, data.size());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Record.RecordData data = this.data.get(position);
        holder.category.setText(data.category().data().name());
        List<Record.SRCRun> runs = data.runs();
        if (runs.isEmpty()) {
            // Empty leaderboard
            holder.layout1st.setVisibility(View.GONE);
            holder.time1st.setVisibility(View.GONE);
            holder.layout3rd.setVisibility(View.GONE);
            holder.time3rd.setVisibility(View.GONE);
            holder.time2nd.setVisibility(View.GONE);
            holder.place2nd.setVisibility(View.INVISIBLE);
            holder.name2nd.setText("Empty leaderboard");
            return;
        }
        boolean filled2nd = false;
        boolean filled3rd = false;
        for (int i = 0, runsSize = runs.size(); i < runsSize; i++) {
            Record.SRCRun run = runs.get(i);
            long place = run.place();
            Record.SRCPlayer.SRCPlayerData player = data.players().data().get(i);
            if (place == 1) {
                if (player.type().equals("user")) {
                    if (player.names().international() != null) {
                        holder.name1st.setText(player.names().international());
                    } else {
                        holder.name1st.setText(player.names().japanese());
                    }
                } else {
                    holder.name1st.setText(player.name());
                }
                setTime(holder.time1st, Math.round(run.run().times().time()));
            } else if (place == 2) {
                if (player.type().equals("user")) {
                    if (player.names().international() != null) {
                        holder.name2nd.setText(player.names().international());
                    } else {
                        holder.name2nd.setText(player.names().japanese());
                    }
                } else {
                    holder.name2nd.setText(player.name());
                }
                setTime(holder.time2nd, Math.round(run.run().times().time()));
                filled2nd = true;
            } else if (place == 3) {
                if (player.type().equals("user")) {
                    if (player.names().international() != null) {
                        holder.name3rd.setText(player.names().international());
                    } else {
                        holder.name3rd.setText(player.names().japanese());
                    }
                } else {
                    holder.name3rd.setText(player.name());
                }
                setTime(holder.time3rd, Math.round(run.run().times().time()));
                filled3rd = true;
            }
        }

        if(!filled2nd) {
            holder.layout2nd.setVisibility(View.GONE);
            holder.time2nd.setVisibility(View.GONE);
        }
        if(!filled3rd) {
            holder.layout3rd.setVisibility(View.GONE);
            holder.time3rd.setVisibility(View.GONE);
        }
    }

    private void setTime(TextView text, long seconds) {
        text.setText(String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds - (seconds / 3600) *3600 ) / 60, seconds % 60));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.layout1st.setVisibility(View.VISIBLE);
        holder.time1st.setVisibility(View.VISIBLE);
        holder.layout3rd.setVisibility(View.VISIBLE);
        holder.time3rd.setVisibility(View.VISIBLE);
        holder.time2nd.setVisibility(View.VISIBLE);
        holder.place2nd.setVisibility(View.VISIBLE);
        holder.layout2nd.setVisibility(View.VISIBLE);
        holder.time2nd.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView category;
        private RelativeLayout layout1st;
        private RelativeLayout layout2nd;
        private TextView place2nd;
        private RelativeLayout layout3rd;
        private TextView name1st;
        private TextView time1st;
        private TextView name2nd;
        private TextView time2nd;
        private TextView name3rd;
        private TextView time3rd;

        public ViewHolder(View item) {
            super(item);
            layout1st = (RelativeLayout) item.findViewById(R.id.relativeLayout2);
            layout2nd = (RelativeLayout) item.findViewById(R.id.relativeLayout3);
            place2nd = (TextView) item.findViewById(R.id.leaderboard_place_2nd);
            layout3rd = (RelativeLayout) item.findViewById(R.id.relativeLayout4);
            category = (TextView) item.findViewById(R.id.leaderboard_category_text);
            name1st = (TextView) item.findViewById(R.id.leaderboard_name_1st);
            name2nd = (TextView) item.findViewById(R.id.leaderboard_name_2nd);
            name3rd = (TextView) item.findViewById(R.id.leaderboard_name_3rd);
            time1st = (TextView) item.findViewById(R.id.leaderboard_time_1st);
            time2nd = (TextView) item.findViewById(R.id.leaderboard_time_2nd);
            time3rd = (TextView) item.findViewById(R.id.leaderboard_time_3rd);
        }
    }
}
