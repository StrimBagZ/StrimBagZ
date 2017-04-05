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

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedChannels;
import net.lubot.strimbagzrewrite.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class FollowedChannelsAdapter extends RecyclerView.Adapter<FollowedChannelsAdapter.ViewHolder> {

    private Fragment fragment;
    private List<FollowedChannels.FollowedChannel> data;
    private int position;
    private int lastPosition = -1;

    public FollowedChannelsAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false));
    }

    public void clear() {
        int size = data.size();
        this.data.clear();
        lastPosition = -1;
        notifyItemRangeRemoved(0, size);
    }

    public void removeSelectedChannel() {
        this.data.remove(position);
        notifyItemRemoved(position);
    }

    public void addAll(List<FollowedChannels.FollowedChannel> data) {
        this.data.addAll(data);
        notifyItemRangeInserted(0, data.size());
    }

    public void addItem(FollowedChannels.FollowedChannel channel) {
        int size = data.size();
        this.data.add(channel);
        notifyItemInserted(size);
    }

    public void addMore(List<FollowedChannels.FollowedChannel> data) {
        int position = this.data.size();
        this.data.addAll(data);
        notifyItemRangeInserted(position + 1, data.size());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Channel tmp = data.get(position).channel();
        holder.username.setText(checkName(tmp.displayName(), tmp.name()));
        holder.followers.setText(NumberFormat.getInstance().format(tmp.followers()) + " followers");
        Glide.with(fragment)
                .load(tmp.logo())
                .error(R.drawable.ic_placeholder_avatar)
                .override(150, 150)
                .into(holder.logo);
        setAnimation(holder.itemView, position);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.clearAnimation();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.logo);
    }

    private String checkName(String displayName, String name) {
        if (Utils.isCharCJK(displayName.charAt(0))) {
            return displayName + " (" + name + ")";
        }
        return displayName;
    }

    public Channel getSelectedChannel() {
        return data.get(position).channel();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void setAnimation(View view, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(fragment.getContext(), android.R.anim.slide_in_left);
            view.startAnimation(animation);
            lastPosition = position;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnCreateContextMenuListener {
        private ImageView logo;
        private TextView username;
        private TextView followers;

        public ViewHolder(View item) {
            super(item);
            logo = (ImageView) item.findViewById(R.id.channelLogo);
            username = (TextView) item.findViewById(R.id.channelUsername);
            followers = (TextView) item.findViewById(R.id.channelFollowers);
            item.setOnClickListener(this);
            item.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            v.showContextMenu();
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            position = getAdapterPosition();
            Channel channel = data.get(position).channel();
            menu.setHeaderTitle(checkName(channel.displayName(), channel.name()));
            MenuInflater menuInflater = new MenuInflater(fragment.getContext());
            menuInflater.inflate(R.menu.ctx_channel, menu);
        }

        public void clearAnimation() {
            itemView.clearAnimation();
        }
    }
}
