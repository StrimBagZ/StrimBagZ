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
package net.lubot.strimbagzrewrite.data.model.SpeedrunCom;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Record implements Parcelable {
    public abstract List<RecordData> data();

    @AutoValue
    public static abstract class RecordData implements Parcelable {
        public abstract Category category();
        public abstract List<SRCRun> runs();
        public abstract SRCPlayer players();

        public static JsonAdapter<RecordData> jsonAdapter(Moshi moshi) {
            return new AutoValue_Record_RecordData.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class Category implements Parcelable {
        public abstract CategoryData data();

        @AutoValue
        public static abstract class CategoryData implements Parcelable {
            public abstract String id();
            public abstract String name();

            public static JsonAdapter<CategoryData> jsonAdapter(Moshi moshi) {
                return new AutoValue_Record_Category_CategoryData.MoshiJsonAdapter(moshi);
            }
        }

        public static JsonAdapter<Category> jsonAdapter(Moshi moshi) {
            return new AutoValue_Record_Category.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class SRCRun implements Parcelable {
        public abstract long place();
        public abstract SRCRunData run();

        @AutoValue
        public static abstract class SRCRunData implements Parcelable {
            public abstract String weblink();
            public abstract Times times();

            public static JsonAdapter<SRCRunData> jsonAdapter(Moshi moshi) {
                return new AutoValue_Record_SRCRun_SRCRunData.MoshiJsonAdapter(moshi);
            }
        }

        @AutoValue
        public static abstract class Times implements Parcelable {
            @Json(name = "primary_t")
            public abstract float time();

            public static JsonAdapter<Times> jsonAdapter(Moshi moshi) {
                return new AutoValue_Record_SRCRun_Times.MoshiJsonAdapter(moshi);
            }
        }

        public static JsonAdapter<SRCRun> jsonAdapter(Moshi moshi) {
            return new AutoValue_Record_SRCRun.MoshiJsonAdapter(moshi);
        }
    }

    @AutoValue
    public static abstract class SRCPlayer implements Parcelable {
        public abstract List<SRCPlayerData> data();

        @AutoValue
        public static abstract class SRCPlayerData implements Parcelable {
            @Json(name = "rel")
            public abstract String type();
            @Nullable
            public abstract Names names();
            @Nullable
            public abstract String name();

            @AutoValue
            public static abstract class Names implements Parcelable {
                @Nullable
                public abstract String international();
                @Nullable
                public abstract String japanese();

                public static JsonAdapter<Names> jsonAdapter(Moshi moshi) {
                    return new AutoValue_Record_SRCPlayer_SRCPlayerData_Names.MoshiJsonAdapter(moshi);
                }
            }

            public static JsonAdapter<SRCPlayerData> jsonAdapter(Moshi moshi) {
                return new AutoValue_Record_SRCPlayer_SRCPlayerData.MoshiJsonAdapter(moshi);
            }
        }

        public static JsonAdapter<SRCPlayer> jsonAdapter(Moshi moshi) {
            return new AutoValue_Record_SRCPlayer.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<Record> jsonAdapter(Moshi moshi) {
        return new AutoValue_Record.MoshiJsonAdapter(moshi);
    }
}
