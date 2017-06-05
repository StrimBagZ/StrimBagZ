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

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class SRCGame {
    public abstract List<GameData> data();

    @AutoValue
    public static abstract class GameData {
        public abstract String id();

        public static JsonAdapter<GameData> jsonAdapter(Moshi moshi) {
            return new AutoValue_SRCGame_GameData.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<SRCGame> jsonAdapter(Moshi moshi) {
        return new AutoValue_SRCGame.MoshiJsonAdapter(moshi);
    }
}
