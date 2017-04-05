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
package net.lubot.strimbagzrewrite.data.model.FrankerFaceZ;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class SRLRaceEntrant implements Parcelable {
    @Nullable
    public abstract String channel();
    @Nullable
    public abstract String comment();
    @Nullable
    @Json(name = "display_name")
    public abstract String displayName();
    public abstract long place();
    public abstract String state();
    public abstract long time();

    public static SRLRaceEntrant recreateWithModifiedPlace(SRLRaceEntrant entrant, long place) {
        return new AutoValue_SRLRaceEntrant(entrant.channel(), entrant.comment(),
                entrant.displayName(), place, entrant.state(), entrant.time());
    }

    public static JsonAdapter<SRLRaceEntrant> jsonAdapter(Moshi moshi) {
        return new AutoValue_SRLRaceEntrant.MoshiJsonAdapter(moshi);
    }
}
