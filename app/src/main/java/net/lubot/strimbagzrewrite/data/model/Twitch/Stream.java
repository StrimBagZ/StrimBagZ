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
public abstract class Stream implements Parcelable {
    public abstract long viewers();
    public abstract StreamPreview preview();
    @Nullable
    @Json(name = "community_id")
    public abstract String community();
    public abstract Channel channel();
    public abstract String created_at();

    public static JsonAdapter<Stream> jsonAdapter(Moshi moshi) {
        return new AutoValue_Stream.MoshiJsonAdapter(moshi);
    }

    public static Stream createEmpty() {
        return new AutoValue_Stream(0, StreamPreview.createEmpty(), "", Channel.createEmpty(), "2017-05-02T10:41:26Z");
    }
}
