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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Twitch.Community;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CommunitiesAdapter extends RecyclerView.Adapter<CommunitiesAdapter.ViewHolder> {

    private Fragment fragment;
    private List<Community> data;

    private long lastUpdated;

    public CommunitiesAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Community> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    public void addMore(List<Community> data) {
        int position = getItemCount();
        this.data.addAll(data);
        notifyItemRangeInserted(position, data.size());
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Community community = this.data.get(position);
        holder.name.setText(community.name());
        holder.viewcount.setText("Viewers: "  + NumberFormat.getInstance().format(community.viewers()));
        Glide.with(fragment)
                .load(community.cover())
                .into(holder.cover);
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView cover;
        private TextView name;
        private TextView viewcount;

        public ViewHolder(View item) {
            super(item);
            cover = (ImageView) item.findViewById(R.id.imgBox);
            name = (TextView) item.findViewById(R.id.gameTitle);
            viewcount = (TextView) item.findViewById(R.id.viewerCount);
            item.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putString("title", data.get(getAdapterPosition()).name());
            bundle.putString("community", data.get(getAdapterPosition()).id());
            ((MainActivity) fragment.getActivity()).showCommunityStreams(bundle);
        }

    }
}
