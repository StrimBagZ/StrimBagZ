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

import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedGame.FollowedGames;
import net.lubot.strimbagzrewrite.data.model.Twitch.Game;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {

    private Fragment fragment;
    private List<FollowedGames> data;

    public GamesAdapter(Fragment context) {
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

    public void addAll(List<FollowedGames> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FollowedGames tmp = data.get(position);
        holder.gameTitle.setText(tmp.game().name());
        holder.game = tmp.game();
        //holder.viewerCount.setText(tmp.viewers + "");

        Glide.with(fragment)
                .load(tmp.game().box().medium())
                .into(holder.box);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private ImageView box;
        private TextView gameTitle;
        //private TextView viewerCount;

        private Game game;

        public ViewHolder(View item) {
            super(item);
            box = (ImageView) item.findViewById(R.id.imgBox);
            gameTitle = (TextView) item.findViewById(R.id.gameTitle);
            //viewerCount = (TextView) item.findViewById(R.id.viewerCount);
            item.setOnClickListener(this);
            //item.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putString("game", game.name());
            ((MainActivity) fragment.getActivity()).showStreams(bundle);
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }
}
