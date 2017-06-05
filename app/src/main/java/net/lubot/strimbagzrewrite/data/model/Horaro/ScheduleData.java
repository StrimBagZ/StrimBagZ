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
package net.lubot.strimbagzrewrite.data.model.Horaro;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class ScheduleData implements Parcelable {
    public abstract Schedule data();

    @AutoValue
    public static abstract class Schedule implements Parcelable {
        public abstract String name();
        @Json(name = "start_t")
        public abstract long start();
        @Json(name = "setup_t")
        public abstract long setup();
        @Nullable
        public abstract List<String> columns();
        @Json(name = "items")
        public abstract List<RunData> runs();

        public static JsonAdapter<Schedule> jsonAdapter(Moshi moshi) {
            return new AutoValue_ScheduleData_Schedule.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<ScheduleData> jsonAdapter(Moshi moshi) {
        return new AutoValue_ScheduleData.MoshiJsonAdapter(moshi);
    }
}
