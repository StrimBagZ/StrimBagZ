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

import android.app.Activity;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.lubot.strimbagzrewrite.util.Utils;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class StreamsAdapter extends RecyclerView.Adapter<StreamsAdapter.ViewHolder> {

    private Activity activity;
    private Fragment fragment;
    private List<Stream> data;
    private int position;
    private long lastUpdated;
    private int lastPosition = -1;

    public StreamsAdapter(Activity activity) {
        this.activity = activity;
        this.data = new ArrayList<>();
    }

    public StreamsAdapter(Activity activity, Fragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        this.data = new ArrayList<>();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stream, parent, false));
    }

    public void clear() {
        int size = data.size();
        this.data.clear();
        lastPosition = -1;
        notifyItemRangeRemoved(0, size);
    }

    public void addAll(List<Stream> data) {
        this.data.addAll(data);
        notifyItemRangeInserted(0, data.size());
    }

    public void addMore(List<Stream> data) {
        int pos = getItemCount();
        this.data.addAll(data);
        notifyItemRangeInserted(pos, data.size());
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Stream stream = data.get(position);
        holder.title.setText(stream.channel().status());
        if (stream.channel().displayName() != null) {
            holder.channel
                    .setText(checkName(stream.channel().displayName(), stream.channel().name()));
        } else {
            holder.channel.setText(stream.channel().name());
        }
        String tmp = Utils.buildString("Playing ", stream.channel().game(), " for ",
                NumberFormat.getInstance().format(stream.viewers()), " viewers");
        holder.nowPlaying.setText(tmp);
        if (fragment != null) {
            Glide.with(fragment)
                    .load(stream.preview().medium())
                    .placeholder(R.drawable.ic_channel)
                    .centerCrop()
                    .signature(new StringSignature(stream.channel().name() + stream.channel().updated_at()))
                    .into(holder.preview);
        } else {
            Glide.with(activity)
                    .load(stream.preview().medium())
                    .placeholder(R.drawable.ic_channel)
                    .centerCrop()
                    .signature(new StringSignature(stream.channel().name() + stream.channel().updated_at()))
                    .into(holder.preview);
        }
        setAnimation(holder.itemView, position);
    }

    private String checkName(String displayName, String name) {
        if (Utils.isCharCJK(displayName.charAt(0))) {
            return Utils.buildString(displayName, " (", name, ")");
        }
        return displayName;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public Channel getSelectedChannel() {
        return data.get(position).channel();
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.clearAnimation();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.preview);
    }

    private void setAnimation(View view, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left);
            view.startAnimation(animation);
            lastPosition = position;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener {
        private TextView channel;
        private TextView title;
        private TextView nowPlaying;
        private ImageView preview;

        public ViewHolder(View item) {
            super(item);
            channel = (TextView) item.findViewById(R.id.hostingTarget);
            title = (TextView) item.findViewById(R.id.streamTitle);
            nowPlaying = (TextView) item.findViewById(R.id.nowPlaying);
            preview = (ImageView) item.findViewById(R.id.previewImage);
            item.setOnClickListener(this);
            item.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            Channel channel = data.get(getAdapterPosition()).channel();
            if (channel.name().equals("gamesdonequick")) {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).showMarathonFragment();
                }
            } else {
                startPlayerActivity(data.get(getAdapterPosition()).channel());
            }
        }

        private void startPlayerActivity(Channel channel) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, channel.name());
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, channel.id() + "");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "stream");

            if (activity instanceof MainActivity) {
                ((MainActivity) activity)
                        .trackActivity(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }

            Utils.startPlayerActivity(activity, channel);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            position = getAdapterPosition();
            menu.setHeaderTitle(activity.getResources().getString(R.string.ctx_stream_options));
            MenuInflater menuInflater = new MenuInflater(activity);
            menuInflater.inflate(R.menu.ctx_stream_actions, menu);
        }

        public void clearAnimation() {
            itemView.clearAnimation();
        }

    }

}
