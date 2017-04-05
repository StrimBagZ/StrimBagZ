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

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.FrankerFaceZ.SRLRaceEntrant;

import java.util.ArrayList;

public class RacesEntrantsAdapter extends RecyclerView.Adapter<RacesEntrantsAdapter.ViewHolder> {

    private Fragment fragment;
    private ArrayList<SRLRaceEntrant> data;

    public RacesEntrantsAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_race_entrant, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<SRLRaceEntrant> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SRLRaceEntrant entrant = data.get(position);
        holder.entrant.setText(entrant.displayName());
        long place = entrant.place();
        if (place == 9999) {
            if (entrant.state().equals("dq")) {
                holder.place.setText("DQ");
            } else {
                holder.place.setText("Forfeit");
            }
            setItemBackgroundColor(holder.item, R.color.race_forfeit);
        }
        if (place != 9998 && place != 9999) {
            Log.d("RaceEntAdapter", "place: " + place);
            String tmpPlace = place + "";
            if (place == 1) {
                tmpPlace += "st";
                setItemBackgroundColor(holder.item, R.color.race_first);
            } else if (place == 2) {
                tmpPlace += "nd";
                setItemBackgroundColor(holder.item, R.color.race_second);
            } else if (place == 3) {
                tmpPlace += "rd";
                setItemBackgroundColor(holder.item, R.color.race_third);
            } else {
                tmpPlace += "th";
            }
            holder.place.setText(tmpPlace);
            holder.endTime.setVisibility(View.VISIBLE);
            setEndTime(holder.endTime, entrant.time());
            if (entrant.comment() != null && !entrant.comment().isEmpty()) {
                holder.comment.setVisibility(View.VISIBLE);
                holder.comment.setText("Comment: " + entrant.comment());
            }
        } else {
            if (!entrant.state().isEmpty()) {
                holder.place.setText(String.format("Status: %s%s", Character
                        .toUpperCase(entrant.state().charAt(0)), entrant.state().substring(1)));
            }
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.item.setBackgroundColor(Color.TRANSPARENT);
        holder.endTime.setVisibility(View.INVISIBLE);
        holder.comment.setVisibility(View.GONE);
    }

    private void setItemBackgroundColor(View itemView, int color) {
        itemView.setBackgroundColor(fragment.getResources().getColor(color));
    }

    private void setEndTime(TextView text, long seconds) {
        text.setText(String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds - (seconds / 3600) *3600 ) / 60, seconds % 60));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout item;
        private TextView place;
        private TextView entrant;
        private TextView endTime;
        private TextView comment;

        public ViewHolder(View item) {
            super(item);
            this.item = (RelativeLayout) item.findViewById(R.id.race_item);
            this.place = (TextView) item.findViewById(R.id.race_place);
            this.entrant = (TextView) item.findViewById(R.id.race_entrant);
            this.endTime = (TextView) item.findViewById(R.id.race_endTime);
            this.comment = (TextView) item.findViewById(R.id.race_comment);
        }
    }
}
