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
package net.lubot.strimbagzrewrite.data.model.Horaro;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Ticker {
    public abstract TickerData data();

    @AutoValue
    public static abstract class TickerData {
        public abstract TickerSchedule schedule();
        public abstract Tickerticker ticker();

        public static JsonAdapter<TickerData> jsonAdapter(Moshi moshi) {
            return new AutoValue_Ticker_TickerData.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class TickerSchedule {
        public abstract long start_t();
        public abstract String updated();
        public abstract List<String> columns();

        public static JsonAdapter<TickerSchedule> jsonAdapter(Moshi moshi) {
            return new AutoValue_Ticker_TickerSchedule.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class Tickerticker {
        @Nullable
        public abstract TickerEntry previous();
        @Nullable
        public abstract TickerEntry current();
        @Nullable
        public abstract TickerEntry next();

        public static JsonAdapter<Tickerticker> jsonAdapter(Moshi moshi) {
            return new AutoValue_Ticker_Tickerticker.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class TickerEntry {
        public abstract long length_t();
        public abstract long scheduled_t();
        public abstract List<String> data();

        public static JsonAdapter<TickerEntry> jsonAdapter(Moshi moshi) {
            return new AutoValue_Ticker_TickerEntry.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<Ticker> jsonAdapter(Moshi moshi) {
        return new AutoValue_Ticker.MoshiJsonAdapter(moshi);
    }
}