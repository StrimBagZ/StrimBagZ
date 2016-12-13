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

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class FollowedGame {
    public abstract List<FollowedGames> follows();


    @AutoValue
    public static abstract class FollowedGames {
        public abstract long viewers();
        public abstract long channels();
        public abstract Game game();

        public static JsonAdapter<FollowedGames> jsonAdapter(Moshi moshi) {
            return new AutoValue_FollowedGame_FollowedGames.MoshiJsonAdapter(moshi);
        }
    }

    public static JsonAdapter<FollowedGame> jsonAdapter(Moshi moshi) {
        return new AutoValue_FollowedGame.MoshiJsonAdapter(moshi);
    }
}
