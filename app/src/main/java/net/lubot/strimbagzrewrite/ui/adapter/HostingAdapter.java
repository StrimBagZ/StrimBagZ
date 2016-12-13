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
import android.net.Uri;
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

import net.lubot.strimbagzrewrite.util.Utils;
import net.lubot.strimbagzrewrite.data.model.Twitch.AccessToken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedHosting;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;

import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HostingAdapter extends RecyclerView.Adapter<HostingAdapter.ViewHolder> {

    private Fragment fragment;
    private List<FollowedHosting.FollowedHosts> data;

    public HostingAdapter(Fragment context) {
        this.fragment = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hosting, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<FollowedHosting.FollowedHosts> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FollowedHosting.FollowedHosts host = data.get(position);
        if (host.display_name() != null) {
            if (host.target().channel().displayName() != null) {
                String tmp = checkName(host.display_name(), host.name()) + " hosting " + checkName(
                        host.target().channel().displayName(), host.target().channel().name());
                holder.hosting.setText(tmp);
            } else {
                String tmp = checkName(host.display_name(), host.name()) + " hosting " +
                        host.target().channel().name();
                holder.hosting.setText(tmp);
            }
        } else if (host.name() != null) {
            String tmp = host.name() + " hosting " + host.target().channel().name();
            holder.hosting.setText(tmp);
        }

        // Batching hosts
        if (host.hostedBy() != null && !host.hostedBy().isEmpty()) {
            assert host.hostedBy() != null;
            int count = host.hostedBy().size();
            if (host.target().channel().displayName() != null) {
                String tmp = count + " channels are hosting " + checkName(
                        host.target().channel().displayName(), host.target().channel().name());
                holder.hosting.setText(tmp);
            } else {
                String tmp = count + " channels are hosting " + host.target().channel().name();
                holder.hosting.setText(tmp);
            }
        }

        holder.title.setText(host.target().title() != null ? host.target().title() : "");
        String game = host.target().meta_game() != null ? host.target().meta_game() : "Games";
        String tmp = "playing " + game + " for " +
                NumberFormat.getInstance().format(host.target().viewers()) + " viewers";
        holder.nowPlaying.setText(tmp);
        Glide.with(fragment)
                .load(host.target().preview())
                .signature(new StringSignature(System.currentTimeMillis() + ""))
                .centerCrop()
                .into(holder.preview);
    }

    private String checkName(String displayName, String name) {
        if (Utils.isCharCJK(displayName.charAt(0))) {
            return displayName + " (" + name + ")";
        }
        return displayName;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener {
        private TextView hosting;
        private TextView title;
        private TextView nowPlaying;
        private ImageView preview;

        public ViewHolder(View item) {
            super(item);
            hosting = (TextView) item.findViewById(R.id.hostingTarget);
            title = (TextView) item.findViewById(R.id.streamTitle);
            nowPlaying = (TextView) item.findViewById(R.id.nowPlaying);
            preview = (ImageView) item.findViewById(R.id.previewImage);
            item.setOnClickListener(this);
            item.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            Utils.startPlayerActivity(fragment.getContext(), data.get(getAdapterPosition()).target().channel());
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            FollowedHosting.FollowedHosts host = data.get(getAdapterPosition());
            if(host.hostedBy() != null && !host.hostedBy().isEmpty()) {
                menu.setHeaderTitle("Hosted by");
                List<FollowedHosting.FollowedHosts> hostedBy = host.hostedBy();
                for (FollowedHosting.FollowedHosts h: hostedBy) {
                    menu.add(h.display_name() != null ?
                            checkName(h.display_name(), h.name()) : h.name());
                }
            }
        }
    }
}
