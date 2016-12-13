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

import com.squareup.moshi.Moshi;

import net.lubot.strimbagzrewrite.data.model.AdapterFactory;

import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class HoraroAPI {
    private static final String BASE_URL = "https://horaro.org/-/api/v1/";
    private static HoraroService HORARO_SERVICE;

    private HoraroAPI() {}

    public static HoraroService getService() {
        if (HORARO_SERVICE == null) {
            Moshi moshi = new Moshi.Builder().add(AdapterFactory.create()).build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build();
            HORARO_SERVICE = retrofit.create(HoraroService.class);
        }
        return HORARO_SERVICE;
    }

}
