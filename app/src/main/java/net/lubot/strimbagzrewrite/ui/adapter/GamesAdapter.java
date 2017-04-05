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

import net.lubot.strimbagzrewrite.data.model.Twitch.Directory;
import net.lubot.strimbagzrewrite.data.model.Twitch.DirectoryGame;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedGame.FollowedGames;
import net.lubot.strimbagzrewrite.data.model.Twitch.Game;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {

    private Fragment fragment;
    private boolean isTablet;
    private List<FollowedGames> data;
    private List<DirectoryGame> dataTop;

    private boolean isDirectory;
    private long lastUpdated;

    public GamesAdapter(Fragment context) {
        this.fragment = context;
        this.isTablet = fragment.getContext().getResources().getBoolean(R.bool.isTablet);
        this.data = new ArrayList<>();
    }

    public GamesAdapter(Fragment context, boolean isDirectory) {
        this.fragment = context;
        this.isTablet = fragment.getContext().getResources().getBoolean(R.bool.isTablet);
        this.isDirectory = isDirectory;
        if (!isDirectory) {
            this.data = new ArrayList<>();
        } else {
            this.dataTop = new ArrayList<>();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false));
    }

    public void clear() {
        if (!isDirectory) {
            this.data.clear();
        } else {
            this.dataTop.clear();
        }
        notifyDataSetChanged();
    }

    public void addAll(List<FollowedGames> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    public void addMore(List<FollowedGames> data) {
        int position = getItemCount();
        this.data.addAll(data);
        notifyItemRangeInserted(position, data.size());
    }

    public void addAllTop(List<DirectoryGame> data) {
        this.dataTop.addAll(data);
        notifyDataSetChanged();
    }

    public void addMoreTop(List<DirectoryGame> data) {
        int position = getItemCount();
        this.dataTop.addAll(data);
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
        if (!isDirectory) {
            FollowedGames tmp = data.get(position);
            holder.gameTitle.setText(tmp.game().name());
            holder.viewcount.setText("Viewers: "  + NumberFormat.getInstance().format(tmp.viewers()));
            if (!isTablet) {
                Glide.with(fragment)
                        .load(tmp.game().box().medium())
                        .into(holder.box);
            } else {
                Glide.with(fragment)
                        .load(tmp.game().box().large())
                        .into(holder.box);
            }
        } else {
            DirectoryGame tmp = dataTop.get(position);
            holder.gameTitle.setText(tmp.game().name());
            holder.viewcount.setText("Viewers: "  + NumberFormat.getInstance().format(tmp.viewers()));
            if (!isTablet) {
                Glide.with(fragment)
                        .load(tmp.game().box().medium())
                        .into(holder.box);
            } else {
                Glide.with(fragment)
                        .load(tmp.game().box().large())
                        .into(holder.box);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (!isDirectory) {
            return data.size();
        } else {
            return dataTop.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView box;
        private TextView gameTitle;
        private TextView viewcount;

        public ViewHolder(View item) {
            super(item);
            box = (ImageView) item.findViewById(R.id.imgBox);
            gameTitle = (TextView) item.findViewById(R.id.gameTitle);
            viewcount = (TextView) item.findViewById(R.id.viewerCount);
            item.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            if (!isDirectory) {
                bundle.putString("game", data.get(getAdapterPosition()).game().name());
            } else {
                bundle.putString("game", dataTop.get(getAdapterPosition()).game().name());
            }
            ((MainActivity) fragment.getActivity()).showStreams(bundle);
        }

    }
}
