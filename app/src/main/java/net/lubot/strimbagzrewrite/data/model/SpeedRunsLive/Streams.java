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
package net.lubot.strimbagzrewrite.data.model.SpeedRunsLive;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Streams {
    @Json(name = "_source") public abstract StreamsSource source();

    @AutoValue
    public static abstract class StreamsSource {
        public abstract List<StreamsChannels> channels();
        
        public static JsonAdapter<StreamsSource> jsonAdapter(Moshi moshi) {
            return new AutoValue_Streams_StreamsSource.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class StreamsChannels {
        public abstract String api();
        public abstract String name();
        public abstract long current_viewers();

        public static JsonAdapter<StreamsChannels> jsonAdapter(Moshi moshi) {
            return new AutoValue_Streams_StreamsChannels.MoshiJsonAdapter(moshi);
        }
    }
    
    public static JsonAdapter<Streams> jsonAdapter(Moshi moshi) {
        return new AutoValue_Streams.MoshiJsonAdapter(moshi);
    }
}
