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
import net.lubot.strimbagzrewrite.data.TwitchService.TwitchAPIService;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class TwitchAPI {

    private static final String BASE_URL = "https://api.twitch.tv/api/";

    private static TwitchAPIService TWITCH_SERVICE;

    private TwitchAPI() {
    }

    public static TwitchAPIService getService() {
        if(TWITCH_SERVICE == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .addHeader("User-Agent", System.getProperty("http.agent"))
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                }
            });
            Moshi moshi = new Moshi.Builder().add(AdapterFactory.create()).build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .client(httpClient.addInterceptor(logging).build())
                    .build();
            TWITCH_SERVICE = retrofit.create(TwitchAPIService.class);
        }
        return TWITCH_SERVICE;
    }

}
