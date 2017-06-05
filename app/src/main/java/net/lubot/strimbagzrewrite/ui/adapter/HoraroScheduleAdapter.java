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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.GDQ.Runner;
import net.lubot.strimbagzrewrite.data.model.Horaro.RunData;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HoraroScheduleAdapter extends RecyclerView.Adapter<HoraroScheduleAdapter.ViewHolder>{
    private Fragment fragment;
    private List<String> columns;
    private List<RunData> data;
    private android.text.format.DateFormat todayFormat;
    private DateFormat dateFormat;
    private int currentRun = -1;
    private ColorStateList cardViewBG;

    private boolean hour12 = false;
    private int currentDay = -1;
    private int currentMonth = -1;
    private int currentYear = -1;
    private long lastUpdated = 0;

    public HoraroScheduleAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.data = new ArrayList<>();
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Resources.getSystem().getConfiguration().locale);
        todayFormat = new android.text.format.DateFormat();
        if (!android.text.format.DateFormat.is24HourFormat(fragment.getContext())) {
            hour12 = true;
        }
    }

    public void addAll(List<String> columns, List<RunData> runs) {
        this.columns = columns;
        this.data = runs;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticker, parent, false));
    }

    public List<RunData> getData() {
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
        RunData run = data.get(position);
        long startTime = run.scheduled();
        final Calendar start = Calendar.getInstance();

        if (currentDay == -1) {
            currentDay = start.get(Calendar.DAY_OF_MONTH);
            currentMonth = start.get(Calendar.MONTH);
            currentYear = start.get(Calendar.YEAR);
            Log.d("currentDay", "current day is " + currentDay);
        }

        start.setTimeInMillis(startTime * 1000);

        if (cardViewBG == null) {
            cardViewBG = holder.cardView.getCardBackgroundColor();
        }

        if (currentRun == -1 && position == 0) {
            holder.state.setText("Starting at: " + getTimeString(start));
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_next));
        } else if (position == currentRun) {
            // Current run
            holder.state.setText("Currently running");
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_going));
        } else if (position == currentRun + 1) {
            // Upcoming run
            holder.state.setText("Starting at: " + getTimeString(start));
            holder.cardView.setCardBackgroundColor(fragment.getResources().getColor(R.color.run_next));
        } else if (position > currentRun) {
            holder.state.setText("Starting at: " + getTimeString(start));
        } else  if (position < currentRun) {
            // Run Ogre
            holder.state.setText("zfgRunOgre");
            holder.layout.setBackgroundResource(R.drawable.runogre);
        }

        String tmp = "";
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            String column = columns.get(i);
            String data = run.data().get(i);
            if (data != null) {
                if (data.startsWith("[")) {
                    data = data.substring(data.indexOf("[") + 1, data.lastIndexOf("]"));
                }
                tmp += column + ": " + data;
                if (i < columnsSize - 1) {
                    tmp += "\n";
                }
            }
        }
        holder.info.setText(tmp);
    }

    private String getTimeString(Calendar time) {
        final int runDay = time.get(Calendar.DAY_OF_MONTH);
        final int runMonth = time.get(Calendar.MONTH);
        final  int runYear = time.get(Calendar.YEAR);
        if (runDay == currentDay && runMonth == currentMonth && runYear == currentYear) {
            if (hour12) {
                return todayFormat.format("hh:mm a", time.getTime()).toString();
            } else {
                return todayFormat.format("HH:mm", time.getTime()).toString();
            }
        } else if (currentMonth != runMonth && currentYear == runYear) {
            if (hour12) {
                return todayFormat.format("MMM d, hh:mm a", time.getTime()).toString();
            } else {
                return todayFormat.format("d MMM, HH:mm", time.getTime()).toString();
            }
        } else if (currentYear != runYear) {
            if (hour12) {
                return todayFormat.format("MMM d yyyy, hh:mm a", time.getTime()).toString();
            } else {
                return todayFormat.format("d MMM yyyy, HH:mm", time.getTime()).toString();
            }
        } else {
            if (hour12) {
                return todayFormat.format("MMM d, hh:mm a", time.getTime()).toString();
            } else {
                return todayFormat.format("d MMM, HH:mm", time.getTime()).toString();
            }
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.layout.setBackgroundResource(0);
        holder.cardView.setCardBackgroundColor(cardViewBG);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layout;
        private CardView cardView;
        private TextView state;
        private TextView info;

        public ViewHolder(View item) {
            super(item);
            layout = (LinearLayout) item.findViewById(R.id.ticker_layout);
            cardView = (CardView) item.findViewById(R.id.card_view);
            state = (TextView) item.findViewById(R.id.ticker_state);
            info = (TextView) item.findViewById(R.id.ticker_info);
        }

    }
}
