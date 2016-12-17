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
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class FollowedHosting {
    public abstract List<FollowedHosts> hosts();

    @AutoValue
    public static abstract class FollowedHosts {
        @Nullable
        public abstract String name();
        @Nullable
        public abstract String display_name();
        @Nullable
        public abstract FollowedHostsTarget target();
        @Nullable
        public abstract List<FollowedHosts> hostedBy();

        public static FollowedHosts create(String name, String displayName, FollowedHostsTarget target, List<FollowedHosts> hostedBy) {
            return new AutoValue_FollowedHosting_FollowedHosts(name, displayName, target, hostedBy);
        }

        public static FollowedHosts create(String name, String displayName) {
            return new AutoValue_FollowedHosting_FollowedHosts(name, displayName, null, null);
        }

        public static JsonAdapter<FollowedHosts> jsonAdapter(Moshi moshi) {
            return new AutoValue_FollowedHosting_FollowedHosts.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class StreamPreview {
        public abstract String small();
        public abstract String medium();
        public abstract String large();
        public abstract String template();

        public static JsonAdapter<StreamPreview> jsonAdapter(Moshi moshi) {
            return new AutoValue_FollowedHosting_StreamPreview.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class FollowedHostsTarget {
        public abstract Channel channel();
        @Nullable
        public abstract String title();
        @Nullable
        public abstract String meta_game();
        public abstract long viewers();
        @Json(name = "preview_urls")
        public abstract StreamPreview preview();

        public static JsonAdapter<FollowedHostsTarget> jsonAdapter(Moshi moshi) {
            return new AutoValue_FollowedHosting_FollowedHostsTarget.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<FollowedHosting> jsonAdapter(Moshi moshi) {
        return new AutoValue_FollowedHosting.MoshiJsonAdapter(moshi);
    }
}
