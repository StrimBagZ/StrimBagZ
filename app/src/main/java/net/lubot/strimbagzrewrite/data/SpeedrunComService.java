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
package net.lubot.strimbagzrewrite.data;

import net.lubot.strimbagzrewrite.data.model.SpeedrunCom.Record;
import net.lubot.strimbagzrewrite.data.model.SpeedrunCom.SRCGame;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpeedrunComService {
    @GET("games?max=1")
    Call<SRCGame> getGame(@Query("name") String name);
    @GET("games/{gameID}/variables")
    Call<Record> getVariables(@Path("gameID") String gameID);
    @GET("games/{gameID}/records?scope=full-game&embed=category,players")
    Call<Record> getRecords(@Path("gameID") String gameID);
}
