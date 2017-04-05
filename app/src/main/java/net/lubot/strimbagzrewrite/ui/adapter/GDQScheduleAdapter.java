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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.GDQ.Run;
import net.lubot.strimbagzrewrite.data.model.GDQ.Runner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class GDQScheduleAdapter extends RecyclerView.Adapter<GDQScheduleAdapter.ViewHolder>{
    private Fragment fragment;
    private List<Run> data;
    private SimpleDateFormat dateFormat;
    private int currentRun = -1;
    private ColorStateList cardViewBG;

    private boolean hour12 = false;

    private long lastUpdated = 0;

    public GDQScheduleAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.data = new ArrayList<>();
        if (!DateFormat.is24HourFormat(fragment.getContext())) {
            hour12 = true;
        }
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void addAll(List<Run> runs) {
        this.data = runs;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticker_gdq, parent, false));
    }

    public List<Run> getData() {
        return data;
    }

    public void markCurrentRun(int i) {
        this.currentRun = i;
        notifyDataSetChanged();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Run run = data.get(position);
        String[] startTime = getDate(run.startTime());
        if (cardViewBG == null) {
            cardViewBG = holder.cardView.getCardBackgroundColor();
        }
        if (currentRun == -1 && position == 0) {
            if (hour12) {
                holder.startTime.setText("Starting at: " + startTime[1] + " " + startTime[2] + ", " + startTime[6] + " " + startTime[7]);
            } else {
                holder.startTime.setText("Starting at: " + startTime[3].substring(0, startTime[3].lastIndexOf(":")));
            }
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_next));
        } else if (position == currentRun) {
            // Current run
            holder.startTime.setText("Currently running");
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_going));
        } else if (position == currentRun + 1){
            // Upcoming run
            if (hour12) {
                holder.startTime.setText("Next run at: " + startTime[1] + " " + startTime[2] + ", " + startTime[6] + " " + startTime[7]);
            } else {
                holder.startTime.setText("Next run at: " + startTime[1] + " " + startTime[2] + ", " + startTime[3].substring(0, startTime[3].lastIndexOf(":")));
            }
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_next));
        } else if (position > currentRun) {
            if (hour12) {
                holder.startTime.setText(startTime[1] + " " + startTime[2] + ", " + startTime[6] + " " + startTime[7]);
            } else {
                holder.startTime.setText(startTime[1] + " " + startTime[2] + ", " + startTime[3].substring(0, startTime[3].lastIndexOf(":")));
            }
        } else if (position < currentRun) {
            // Run Ogre
            holder.startTime.setText("zfgRunOgre");
            holder.layout.setBackgroundResource(R.drawable.runogre);
        }
        holder.gameAndCategory.setText(run.name() + " (" + run.category() + ")");
        boolean isSetup = !run.setupTime().contentEquals("0:00:00");
        String tmp = "";
        tmp += "Length: " + run.runTime() + " - ";
        if (isSetup) {
            tmp += "Setup: " + run.setupTime() + " - ";
        }
        if (run.console() == null) {
            tmp += "Console: PC";
        } else {
            tmp += "Console: " + run.console();
        }
        holder.info.setText(tmp);
        String runners = "Runners: ";
        for (int i = 0; i < run.runners().size(); i++) {
            Runner runner = run.runners().get(i);
            runners += runner.displayName();
            if (i + 1 < run.runners().size()) {
                runners += ", ";
            }
        }
        holder.runners.setText(runners);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.layout.setBackgroundResource(0);
        holder.cardView.setCardBackgroundColor(cardViewBG);
    }

    private String[] getDate(String t) {
        try {
            Date date = dateFormat.parse(t);
            String d = DateFormat.format("hh:mm aaa", date).toString();
            String time = dateFormat.parse(t).toString() + " " + d;
            Log.d("Time AMPM", "test: " + time);
            return time.split("\\s+");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new String[5];
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layout;
        private CardView cardView;
        private TextView startTime;
        private TextView gameAndCategory;
        private TextView info;
        private TextView runners;

        public ViewHolder(View item) {
            super(item);
            layout = (LinearLayout) item.findViewById(R.id.ticker_layout);
            cardView = (CardView) item.findViewById(R.id.card_view);
            startTime = (TextView) item.findViewById(R.id.ticker_startTime);
            gameAndCategory = (TextView) item.findViewById(R.id.ticker_gameAndCategory);
            info = (TextView) item.findViewById(R.id.ticker_info);
            runners = (TextView) item.findViewById(R.id.ticker_runners);
        }

    }
}
