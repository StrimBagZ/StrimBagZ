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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.lubot.strimbagzrewrite.util.Utils;
import net.lubot.strimbagzrewrite.data.model.Twitch.AccessToken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;

import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreamsAdapter extends RecyclerView.Adapter<StreamsAdapter.ViewHolder> {

    private MainActivity activity;
    private Context context;
    private List<Stream> data;

    public StreamsAdapter(Fragment context) {
        this.context = context.getActivity();
        this.data = new ArrayList<>();
    }

    public StreamsAdapter(MainActivity activity, Fragment context) {
        this.activity = activity;
        this.context = context.getActivity();
        this.data = new ArrayList<>();
    }

    public StreamsAdapter(Context context) {
        this.context = context;
        this.data = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stream_new, parent, false));
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Stream> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Stream stream = data.get(position);
        holder.setPosition(position);
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
        holder.setChannel(stream.channel());
        Glide.with(context)
                .load(stream.preview().medium())
                .centerCrop()
                .signature(new StringSignature(stream.channel().name() + stream.channel().updated_at()))
                .into(holder.preview);
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

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.preview);
    }

    /* Old layout
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView title;
        private TextView channel;
        private TextView game;
        private TextView viewers;
        private ImageView preview;
        private int position;

        private Channel channelData;

        public ViewHolder(View item) {
            super(item);
            title = (TextView) item.findViewById(R.id.streamTitle);
            channel = (TextView) item.findViewById(R.id.channel);
            game = (TextView) item.findViewById(R.id.game);
            viewers = (TextView) item.findViewById(R.id.viewers);
            preview = (ImageView) item.findViewById(R.id.thumbnail);
            item.setOnClickListener(this);
            item.setOnLongClickListener(this);
        }

        public void setPosition(int pos) {
            this.position = pos;
        }
        public void setChannel(Channel channel) {
            this.channelData = channel;
        }

        @Override
        public void onClick(View v) {
            //Toast.makeText(fragment.getContext(), position + ", " + channel.getText(), Toast.LENGTH_SHORT).show();
            startPlayerActivity(channelData);
              }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }
    */

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView channel;
        private TextView title;
        private TextView nowPlaying;
        private ImageView preview;
        private int position;

        private Channel channelData;

        public ViewHolder(View item) {
            super(item);
            channel = (TextView) item.findViewById(R.id.hostingTarget);
            title = (TextView) item.findViewById(R.id.streamTitle);
            nowPlaying = (TextView) item.findViewById(R.id.nowPlaying);
            preview = (ImageView) item.findViewById(R.id.previewImage);
            item.setOnClickListener(this);
            item.setOnLongClickListener(this);
        }

        public void setPosition(int pos) {
            this.position = pos;
        }
        public void setChannel(Channel channel) {
            this.channelData = channel;
        }

        @Override
        public void onClick(View v) {
            //Toast.makeText(fragment.getContext(), position + ", " + channel.getText(), Toast.LENGTH_SHORT).show();
            startPlayerActivity(channelData);
        }

        private void startPlayerActivity(final Channel channel) {
            Utils.startPlayerActivity(context, channel);
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

}
