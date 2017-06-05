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
package net.lubot.strimbagzrewrite.data.model.Twitch;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class Chatter {
    @Json(name = "chatter_count")
    public abstract long count();
    public abstract Chatters chatters();

    @AutoValue
    public static abstract class Chatters {
        public abstract List<String> moderators();
        public abstract List<String> staff();
        public abstract List<String> admins();
        public abstract List<String> global_mods();
        public abstract List<String> viewers();

        public static JsonAdapter<Chatters> jsonAdapter(Moshi moshi) {
            return new AutoValue_Chatter_Chatters.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<Chatter> jsonAdapter(Moshi moshi) {
        return new AutoValue_Chatter.MoshiJsonAdapter(moshi);
    }

    public static Chatter createEmpty() {
        return new AutoValue_Chatter(-1, new AutoValue_Chatter_Chatters(new ArrayList<String >(),
                new ArrayList<String >(), new ArrayList<String >(), new ArrayList<String >(), new ArrayList<String >()));
    }
}
