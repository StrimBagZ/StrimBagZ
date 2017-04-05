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
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.util.Utils;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedHosting;
import net.lubot.strimbagzrewrite.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class HostingAdapter extends RecyclerView.Adapter<HostingAdapter.ViewHolder> {

    private Fragment fragment;
    private List<FollowedHosting.FollowedHosts> data;
    private boolean isTablet;
    private int position;
    private String clickedHostingItem = "";

    public HostingAdapter(Fragment context) {
        this.fragment = context;
        this.isTablet = fragment.getContext().getResources().getBoolean(R.bool.isTablet);
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
        if (!isTablet) {
            Glide.with(fragment)
                    .load(host.target().preview().medium())
                    .placeholder(R.drawable.ic_channel)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(holder.preview);
        } else {
            Glide.with(fragment)
                    .load(host.target().preview().medium())
                    .placeholder(R.drawable.ic_channel)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(holder.preview);
        }
    }

    @Override
    public void onViewRecycled(HostingAdapter.ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.preview);
    }

    private String checkName(String displayName, String name) {
        if (Utils.isCharCJK(displayName.charAt(0))) {
            return displayName + " (" + name + ")";
        }
        return displayName;
    }

    public int getPosition() {
        return position;
    }

    public Channel getChannel() {
        return data.get(position).target().channel();
    }

    public String getClickedHostingItem() {
        return clickedHostingItem;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener {
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
            position = getAdapterPosition();
            FollowedHosting.FollowedHosts host = data.get(getAdapterPosition());
            if (host.target().channel().name().equals("gamesdonequick")) {
                if (fragment.getActivity() instanceof MainActivity) {
                    ((MainActivity) fragment.getActivity()).showMarathonFragment();
                    return;
                }
            }
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.showContextMenu(0f, 0f);
            } else {
                v.showContextMenu();
            }
            */
            v.showContextMenu();
            //Utils.startPlayerActivity(fragment.getContext(), host.target().channel());
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            position = getAdapterPosition();
            final FollowedHosting.FollowedHosts host = data.get(getAdapterPosition());
            MenuInflater menuInflater = new MenuInflater(fragment.getContext());
            menu.add(R.id.ctx_hosting, Menu.NONE, Menu.NONE, "Open Stream");
            if(host.hostedBy() != null && !host.hostedBy().isEmpty()) {
                menu.setHeaderTitle(checkName(host.target().channel().displayName(),
                        host.target().channel().name()) + " is hosted by");
                List<FollowedHosting.FollowedHosts> hostedBy = host.hostedBy();
                for (int i = 0, hostedBySize = hostedBy.size(); i < hostedBySize; i++) {
                    FollowedHosting.FollowedHosts h = hostedBy.get(i);
                    menu.add(h.display_name() != null ?
                            checkName(h.display_name(), h.name()) : h.name())
                            .setEnabled(false).setCheckable(false);
                }
                SubMenu hostedChat = menu.addSubMenu(R.string.ctx_stream_specific_chat);
                hostedChat.setHeaderTitle("Choose Chat:");
                for (int i = 0, hostedBySize = hostedBy.size(); i < hostedBySize; i++) {
                    final FollowedHosting.FollowedHosts h = hostedBy.get(i);
                    hostedChat.add(429691, Menu.NONE, Menu.NONE, checkName(h.display_name(), h.name()))
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            clickedHostingItem = h.name();
                            return false;
                        }
                    });
                }
            } else {
                menu.setHeaderTitle(checkName(host.target().channel().displayName(),
                        host.target().channel().name()));
                menu.add(429691, Menu.NONE, Menu.NONE, "Open Stream with " + host.display_name() + "'s chat")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Log.d("HostingAdapter", "Clicked Hosting channel item");
                        clickedHostingItem = host.name();
                        return false;
                    }
                });
                //menuInflater.inflate(R.menu.ctx_stream_actions, menu);
            }
            SubMenu options = menu.addSubMenu(R.string.ctx_stream_options);
            options.setHeaderTitle(host.target().channel().displayName());
            menuInflater.inflate(R.menu.ctx_hosting_actions, options);
        }

        @Override
        public boolean onLongClick(View view) {
            position = getAdapterPosition();
            return true;
        }
    }
}
