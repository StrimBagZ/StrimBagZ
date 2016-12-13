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

import net.lubot.strimbagzrewrite.data.model.Twitch.AccessToken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.Directory;
import net.lubot.strimbagzrewrite.data.model.Twitch.Ember;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedGame;
import net.lubot.strimbagzrewrite.data.model.Twitch.FollowedHosting;
import net.lubot.strimbagzrewrite.data.model.Twitch.Hosts;
import net.lubot.strimbagzrewrite.data.model.Twitch.KrakenBase;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.model.Twitch.Panel;
import net.lubot.strimbagzrewrite.data.model.Twitch.StreamObject;
import net.lubot.strimbagzrewrite.data.model.Twitch.TwitchUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface TwitchService {

    // Requires-Authentication is only a reference header to determine if the OAuth token should be
    // included, it will be removed from the request after inserted.
    interface TwitchKrakenService {
        @GET("/kraken")
        @Headers("Requires-Authentication: true")
        Call<KrakenBase> getBase();

        @GET("user")
        @Headers("Requires-Authentication: true")
        Call<TwitchUser> getUser();

        @GET("channels/{channel}")
        Call<Channel> getChannel(@Path("channel") String channel);

        @GET("streams/followed")
        @Headers("Requires-Authentication: true")
        Call<LiveStreams> getLiveStreams();

        @GET("streams")
        Call<LiveStreams> getStreams(@Query("game") String game, @Query("channel") String channel,
                                     @Query("offset") int offset, @Query("limit") int limit);

        @GET("streams/{channel}")
        Call<StreamObject> getStreamStatus(@Path("channel") String channel);

        @GET("games/top")
        Call<Directory> getDirectory();

        @GET
        Call<Directory> getDirectoryPagination(@Url String url);

        @GET("users/{user}/follows/channels/{target}")
        Call<Void> checkFollow(@Path("user") String user, @Path("target") String target);

        @PUT("users/{user}/follows/channels/{target}")
        @Headers("Requires-Authentication: true")
        Call<Void> followChannel(@Path("user") String user, @Path("target") String target);

        @DELETE("users/{user}/follows/channels/{target}")
        @Headers("Requires-Authentication: true")
        Call<Void> unfollowChannel(@Path("user") String user, @Path("target") String target);
    }

    interface TwitchAPIService {
        @GET("channels/{channel}/access_token")
        Call<AccessToken> getChannelToken(@Path("channel") String channel);

        @GET("users/{channel}/follows/games")
        Call<FollowedGame> getFollowedGames(@Path("channel") String channel);

        @GET("users/{channel}/follows/games/live")
        Call<FollowedGame> getFollowedLiveGames(@Path("channel") String channel);

        @GET("users/{channel}/followed/hosting")
        Call<FollowedHosting> getFollowedHosting(@Path("channel") String channel);

        @GET("channels/{channel}/panels")
        Call<List<Panel>> getChannelPanels(@Path("channel") String channel);

        @GET("channels/{channel}/ember")
        Call<Ember> getChannelEmber(@Path("channel") String channel);

        @GET("channels/{channel}/product")
        Call<Void> checkProduct(@Path("channel") String channel);

        @GET
        Call<Hosts> getHostingStatus(@Url String url);
    }

}
