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

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Hosts {
    public abstract List<Host> hosts();

    @AutoValue
    public static abstract class Host {
        public abstract long host_id();
        public abstract long target_id();
        public abstract String host_login();
        @Nullable
        public abstract String target_login();
        public abstract String host_display_name();
        @Nullable
        public abstract String target_display_name();

        public static JsonAdapter<Host> jsonAdapter(Moshi moshi) {
            return new AutoValue_Hosts_Host.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<Hosts> jsonAdapter(Moshi moshi) {
        return new AutoValue_Hosts.MoshiJsonAdapter(moshi);
    }
}
