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
package net.lubot.strimbagzrewrite.data.model.GDQ;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Run implements Parcelable {
    @Json(name = "setup_time")
    public abstract String setupTime();
    @Nullable
    public abstract String console();
    public abstract boolean coop();
    @Json(name = "run_time")
    public abstract String runTime();
    public abstract String name();
    @Json(name = "starttime")
    public abstract String startTime();
    @Json(name = "endtime")
    public abstract String endTime();
    public abstract String category();
    public abstract List<Runner> runners();
    
    public static JsonAdapter<Run> jsonAdapter(Moshi moshi) {
        return new AutoValue_Run.MoshiJsonAdapter(moshi);
    }
}
