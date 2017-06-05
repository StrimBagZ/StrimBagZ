/*
 * Copyright 2017 Nicola Fäßler
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
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Twitch.Chatter;

import java.util.List;

public class ChatterAdapter extends
        RecyclerView.Adapter<ChatterAdapter.ViewHolder> {

    private Chatter.Chatters data;
    private String broadcaster;
    private boolean addedBroadcaster = false;
    private boolean addedStaff = false;
    private boolean addedAdmins = false;
    private boolean addedGlobalMod = false;
    private boolean addedMod = false;
    private boolean addedViewers = false;
    private int size = 6;

    private long lastUpdate = 0;
    private boolean tooManyChatters = false;

    public ChatterAdapter(Chatter.Chatters chatters, String broadcaster) {
        this.data = chatters;
        this.broadcaster = broadcaster;
        setSize();
    }

    public ChatterAdapter(String broadcaster) {
        this.data = Chatter.createEmpty().chatters();
        this.broadcaster = broadcaster;
        this.tooManyChatters = true;
        setSize();
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate() {
        this.lastUpdate = System.currentTimeMillis();
    }

    public void renewList(Chatter.Chatters chatters) {
        this.data = chatters;
        this.tooManyChatters = false;
        setSize();
        notifyDataSetChanged();
    }

    public void setTooManyChatters() {
        this.data = Chatter.createEmpty().chatters();
        this.tooManyChatters = true;
        setSize();
        notifyDataSetChanged();
    }

    private void setSize() {
        addedBroadcaster = false;
        addedStaff = false;
        addedAdmins = false;
        addedGlobalMod = false;
        addedMod = false;
        addedViewers = false;
        this.size = 6;
        if (data.admins().isEmpty()) {
            size--;
            addedAdmins = true;
        }
        if (data.staff().isEmpty()) {
            size--;
            addedStaff = true;
        }
        if (data.global_mods().isEmpty()) {
            size--;
            addedGlobalMod = true;
        }
        if (data.moderators().isEmpty()) {
            size--;
            addedMod = true;
        }
        if (data.viewers().isEmpty()) {
            size--;
            addedViewers = true;
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chatter, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (tooManyChatters) {
            holder.header.setText("Viewer List is too large.");
            holder.chatter.setVisibility(View.GONE);
            return;
        }
        if (!addedBroadcaster) {
            List<String> list = data.moderators();
            boolean inList = false;
            for (int i = 0, moderatorsSize = list.size(); i < moderatorsSize; i++) {
                String name = list.get(i);
                if (name.equals(broadcaster)) {
                    inList = true;
                    list.remove(i);
                    break;
                }
            }
            if (inList) {
                holder.chatter.setText(broadcaster + "\n");
            } else {
                holder.header.setVisibility(View.GONE);
                holder.chatter.setVisibility(View.GONE);
            }
            addedBroadcaster = true;
            return;
        }
        if (!addedStaff) {
            List<String> list = data.staff();
            holder.header.setText("STAFF");
            String names = "";
            for (int i = 0, moderatorsSize = list.size(); i < moderatorsSize; i++) {
                names += list.get(i) + "\n";

            }
            holder.chatter.setText(names);
            addedStaff = true;
            return;
        }
        if (!addedAdmins) {
            List<String> list = data.admins();
            holder.header.setText("ADMINS");
            String names = "";
            for (int i = 0, size = list.size(); i < size; i++) {
                names += list.get(i) + "\n";
            }
            holder.chatter.setText(names);
            addedAdmins = true;
            return;
        }
        if (!addedGlobalMod) {
            List<String> list = data.global_mods();
            holder.header.setText("GLOBAL MODERATORS");
            String names = "";
            for (int i = 0, size = list.size(); i < size; i++) {
                names += list.get(i) + "\n";
            }
            holder.chatter.setText(names);
            addedGlobalMod = true;
            return;
        }
        if (!addedMod) {
            List<String> list = data.moderators();
            holder.header.setText("MODERATORS");
            String names = "";
            for (int i = 0, size = list.size(); i < size; i++) {
                names += list.get(i) + "\n";
            }
            holder.chatter.setText(names);
            addedMod = true;
            return;
        }
        if (!addedViewers) {
            List<String> list = data.viewers();
            holder.header.setText("VIEWERS");
            String names = "";
            for (int i = 0, size = list.size(); i < size; i++) {
                names += list.get(i) + "\n";
            }
            holder.chatter.setText(names);
            addedViewers = true;
        }
    }

    @Override
    public int getItemCount() {
        return size;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView header;
        private TextView chatter;

        public ViewHolder(View view) {
            super(view);
            header = (TextView) view.findViewById(R.id.chatterHeader);
            chatter = (TextView) view.findViewById(R.id.chatter);
        }
    }

}
