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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Horaro.Ticker;
import net.lubot.strimbagzrewrite.util.Utils;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TickerAdapter extends RecyclerView.Adapter<TickerAdapter.ViewHolder> {

    private Fragment fragment;
    private Ticker data;
    private List<String> columns;

    public TickerAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticker, parent, false));
    }

    public void clear() {
        this.data = null;
        notifyDataSetChanged();
    }

    public void addAll(Ticker data) {
        this.data = data;
        if (data != null) {
            columns = data.data().schedule().columns();
        }
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Ticker.Tickerticker tickers = data.data().ticker();
        if (position == 0) {
            if (tickers.current() != null) {
                setCurrentTicker(holder, tickers.current());
            } else if (tickers.next() != null) {
                setNextTicker(holder, tickers.next());
            }
        } else if (position == 1) {
            if (tickers.next() != null) {
                setNextTicker(holder, tickers.next());
            }
        }
    }

    private void setCurrentTicker(ViewHolder holder, Ticker.TickerEntry current) {
        holder.tickerState.setText("Currently showing:");
        String tmp = "";
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            String column = columns.get(i);
            String data = current.data().get(i);
            if (data.startsWith("[")) {
                data = data.substring(data.indexOf("[") + 1, data.lastIndexOf("]"));
            }
            tmp += column + ": " + data;
            if (i < columnsSize - 1) {
                tmp += "\n";
            }
        }
        holder.tickerInfo.setText(tmp);
    }

    private void setNextTicker(ViewHolder holder, Ticker.TickerEntry next) {
        Calendar currentTime = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(next.scheduled_t() * 1000);
        Map<TimeUnit, Long> timeDiff = Utils.computeTimeDiff(currentTime, time);
        long diffInHr = timeDiff.get(TimeUnit.HOURS);
        long diffInMin = timeDiff.get(TimeUnit.MINUTES);
        String timeString = "Next up";
        if (diffInHr != 0) {
            timeString += " in " + diffInHr + (diffInHr > 1 ? " hours" : " hour");
        }
        if (diffInMin != 0) {
            if (diffInHr != 0) {
                timeString += " " + diffInMin + (diffInMin > 1 ? " minutes" : " minute");
            } else {
                timeString += " in " + diffInMin + (diffInMin > 1 ? " minutes" : " minute");
            }
        }
        holder.tickerState.setText(timeString + ":");
        String tmp = "";
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            String column = columns.get(i);
            String data = next.data().get(i);
            if (data.startsWith("[")) {
                data = data.substring(data.indexOf("[") + 1, data.lastIndexOf("]"));
            }
            tmp += column + ": " + data;
            if (i < columnsSize - 1) {
                tmp += "\n";
            }
        }
        holder.tickerInfo.setText(tmp);
    }

    @Override
    public int getItemCount() {
        if (data != null) {
            Ticker.Tickerticker tickers = data.data().ticker();
            if (tickers.current() != null && tickers.next() != null) {
                return 2;
            } else if (tickers.current() != null) {
                return 1;
            } else if (tickers.next() != null) {
                return 1;
            }
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tickerState;
        private TextView tickerInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            tickerState = (TextView) itemView.findViewById(R.id.ticker_state);
            tickerInfo = (TextView) itemView.findViewById(R.id.ticker_info);
        }
    }
}
