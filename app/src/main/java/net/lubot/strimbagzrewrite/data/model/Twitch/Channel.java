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
package net.lubot.strimbagzrewrite.data.model.Twitch;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Channel implements Parcelable {
    @Nullable
    public abstract String status();
    @Json(name = "display_name")
    public abstract String displayName();
    @Json(name = "_id")
    public abstract long id();
    @Json(name = "id")
    public abstract long awfulHostingID();
    public abstract String name();
    @Nullable
    public abstract String game();
    @Nullable
    public abstract String updated_at();
    @Nullable
    public abstract String logo();
    @Nullable
    @Json(name = "profile_banner")
    public abstract String profileBanner();
    @Nullable
    @Json(name = "video_banner")
    public abstract String videoBanner();
    public abstract boolean partner();
    public abstract long followers();

    public static Channel createEmpty() {
        return new AutoValue_Channel(null, "", 0, 0, "", null, null, null, null, null, false, 0);
    }

    public static JsonAdapter<Channel> jsonAdapter(Moshi moshi) {
        return new AutoValue_Channel.MoshiJsonAdapter(moshi);
    }
}
