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

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class SpeedRunsLive {

    private static final String BASE_URL = "http://api.speedrunslive.com/";

    private static SpeedRunsLiveService SRL_SERVICE;

    private SpeedRunsLive() {
    }

    public static SpeedRunsLiveService getService() {
        if(SRL_SERVICE == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            Moshi moshi = new Moshi.Builder()
                    .add(AdapterFactory.create())
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .client(httpClient.addInterceptor(logging).build())
                    .build();
            SRL_SERVICE = retrofit.create(SpeedRunsLiveService.class);
        }
        return SRL_SERVICE;
    }

}
