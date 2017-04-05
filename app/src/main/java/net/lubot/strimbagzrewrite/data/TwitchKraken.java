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

import android.content.Context;

import com.squareup.moshi.Moshi;

import net.lubot.strimbagzrewrite.BuildConfig;
import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.StrimBagZApplication;
import net.lubot.strimbagzrewrite.data.model.AdapterFactory;
import net.lubot.strimbagzrewrite.data.TwitchService.TwitchKrakenService;
import net.lubot.strimbagzrewrite.util.Utils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class TwitchKraken {

    private static final String BASE_URL = "https://api.twitch.tv/kraken/";

    private static TwitchKrakenService TWITCH_SERVICE;
    private static String token;

    private TwitchKraken() {
    }

    public static TwitchKrakenService getService() {
        if(TWITCH_SERVICE == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
                httpClient.addInterceptor(logging);
            }
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder request = original.newBuilder()
                            .addHeader("User-Agent", System.getProperty("http.agent"))
                            .header("Accept", "application/vnd.twitchtv.v3+json")
                            .header("Client-ID", BuildConfig.CLIENT_ID)
                            .method(original.method(), original .body());
                    if (original.header("Requires-Authentication") != null) {
                        request.removeHeader("Requires-Authentication");
                        Context ctx = StrimBagZApplication.getContext();
                        if (token == null) {
                            token = Utils.getToken(ctx);
                        } else if (token.equals(Constants.NO_TOKEN)) {
                            token = Utils.getToken(ctx);
                        }
                        request.addHeader("Authorization", Utils.token(token));
                    }
                    return chain.proceed(request.build());
                }
            });
            Moshi moshi = new Moshi.Builder().add(AdapterFactory.create()).build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .client(httpClient.build())
                    .build();
            TWITCH_SERVICE = retrofit.create(TwitchKrakenService.class);
        }
        return TWITCH_SERVICE;
    }

}
