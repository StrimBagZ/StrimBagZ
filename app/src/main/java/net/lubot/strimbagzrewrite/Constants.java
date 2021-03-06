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
package net.lubot.strimbagzrewrite;

public class Constants {
    public static final String appName = "StrimBagZ";
    public static final String version = "2.0";
    private static final String LOGIN_SCOPES =
            "user_read" +
            "+user_follows_edit" +
            "+user_subscriptions" +
            "+chat_login";
    public static final String CALLBACK_URL = "strimbagz://lubot.net/strimbagz/redirect";
    public static final String TOKEN_URL = "https://lubot.net/sbz/token";
    public static final String REVOKE_URL = "https://lubot.net/sbz/revoke";

    // Keys
    public static final String NO_USER = "NO_USER";
    public static final String NO_TOKEN = "NO_TOKEN";
    public static final String OAUTH = "oauth_token";
    public static final String REFRESH = "refresh_token";
    public static final String TWITCH_ID = "twitch_id";
    public static final String DISPLAY_NAME = "displayName";
    public static final String LOGIN = "login";

    public static final String MARATHON_SCHEDULE_SERVICE = "marathon_schedule_service";
    public static final String MARATHON_HORARO_ID = "marathon_horaro_id";
    public static final String MARATHON_RUNNING = "marathon_running";
    public static final String MARATHON_NAME = "marathon_name";
    public static final String MARATHON_CHANNEL = "marathon_channel";
    public static final String MARATHON_CHANNEL_ID = "marathon_channel_id";

    public static final String RECREATE_SETTINGS = "recreateSettings";
    public static final String SETTING_DARK_THEME = "setting_dark_theme";
    public static final String SETTING_DEBUG = "setting_debug";
    public static final String SETTING_QUALITY = "setting_quality";
    public static final String SETTING_QUALITY_MOBILE = "setting_quality_mobile";
    public static final String SETTING_DEBUG_ADDRESS = "setting_debug_address";

    public static final String LEADERBOARDS_ENABLED = "leaderboards_enabled_communities";

    public static final int NOTIFICATION_STREAM = 14812;

    // Fragments
    public static final String FRAGMENT_FOLLOWING = "following";
    public static final String FRAGMENT_COMMUNTIES = "communities";
    public static final String FRAGMENT_SRL = "srl";
    public static final String FRAGMENT_MARATHON = "marathon";
    public static final String FRAGMENT_SETTINGS = "preferences";

    public static final int FOLLOWING_CONTEXT_MENU = 694201337;
    public static final int HOSTED_CONTEXT_MENU = 420133769;

    public static final String SETTINGS = "net.lubot.strimbagz_preferences";
    public static final int LOGGED_IN = 1337;

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36 StrimBagZ";
    public static final String URL_TWITCH_LOGIN = "https://secure.twitch.tv/login";
    public static final String URL_TWITCH_AUTHENTICATION =
            "https://api.twitch.tv/kraken/oauth2/authorize" +
                    "?response_type=code" +
                    "&client_id=" + BuildConfig.CLIENT_ID +
                    "&redirect_uri=strimbagz://lubot.net/strimbagz/redirect" +
                    "&scope=" + LOGIN_SCOPES +
                    "&force_verify=true";
    private static final String URL_TMI_HOSTS = "https://tmi.twitch.tv/hosts?include_logins=1";
    public static final String URL_TMI_CHATTERS = "https://tmi.twitch.tv/group/user/{channel}/chatters";
    public static final String URL_HOST = URL_TMI_HOSTS + "&host=";
    public static final String URL_HOST_TARGET = URL_TMI_HOSTS + "&target=";
    public static final String URL_USHER = "https://usher.ttvnw.net/api/channel/hls/{channel}.m3u8";

    public static final String URL_GDQ_SCHEDULE = "https://lubot.net/strimbagz/gdq/schedule.json";

    // Stream
    public static final String STREAM_STOPPED = "com.google.android.exoplayer.upstream.HttpDataSource$InvalidResponseCodeException: Response code: 404";
    public static final String STREAM_CONNECTION_LOST = "com.google.android.exoplayer.upstream.HttpDataSource$HttpDataSourceException: Unable to connect to";
    public static final String STREAM_ENDED = "Response code: 404";
}
