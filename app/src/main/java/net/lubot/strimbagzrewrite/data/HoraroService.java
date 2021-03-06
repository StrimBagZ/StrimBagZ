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
package net.lubot.strimbagzrewrite.data;

import net.lubot.strimbagzrewrite.data.model.GDQ.Run;
import net.lubot.strimbagzrewrite.data.model.Horaro.ScheduleData;
import net.lubot.strimbagzrewrite.data.model.Horaro.Ticker;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface HoraroService {
    @GET("schedules/{id}")
    Call<ScheduleData> getSchedule(@Path("id") String id);
    @GET("schedules/{id}/ticker")
    Call<Ticker> getTicker(@Path("id") String id);
    @GET
    Call<List<Run>> getGDQData(@Url String url);
}
