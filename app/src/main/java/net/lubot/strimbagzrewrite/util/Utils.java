package net.lubot.strimbagzrewrite.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Twitch.AccessToken;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.Stream;
import net.lubot.strimbagzrewrite.data.model.Twitch.TwitchUser;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;
import net.lubot.strimbagzrewrite.ui.activity.RaceActivity;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Utils {

    private Utils() {}

    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom rnd = new SecureRandom();

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public static String token(String token) {
        return String.format("OAuth %s", token);
    }

    public static void restartActivity(Activity activity) {
        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
        activity.overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    public static void onActivityCreateSetTheme(Activity activity) {
        boolean darkTheme = activity
                .getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(Constants.SETTING_DARK_THEME, false);
        if ((activity instanceof MainActivity || activity instanceof RaceActivity)
                && !activity.getResources().getBoolean(R.bool.isTablet)) {
            activity.setTheme(darkTheme ? R.style.AppThemeDark : R.style.AppTheme);
        } else {
            activity.setTheme(darkTheme ? R.style.AppThemeDark_StatusBar : R.style.AppTheme_StatusBar);
        }
    }

    public static void getTwitchUser(final Activity activity, final boolean login) {
        Call<TwitchUser> call = TwitchKraken.getService().getUser();
        call.enqueue(new CallbackZ<TwitchUser>(call, true) {
            @Override
            public void onResponse(Call<TwitchUser> call, Response<TwitchUser> response) {
                if (response.isSuccessful()) {
                    Log.d("getTwitchUser", response.body().toString());
                    TwitchUser user = response.body();
                    activity.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(Constants.DISPLAY_NAME, user.displayName())
                            .putString(Constants.LOGIN, user.name())
                            .putString(Constants.TWITCH_ID, user.id())
                            .apply();

                    if (login) {
                        restartActivity(activity);
                        return;
                    }

                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).getChannel(user.name());
                    }

                }
            }
        });
    }

    public static void startPlayerActivity(Context context, Channel channel, boolean loadChannel) {
        startPlayerActivity(context, channel, null, null, loadChannel, null);
    }

    public static void startPlayerActivity(Context context, Channel channel) {
        startPlayerActivity(context, channel, null, null, false, null);
    }

    public static void startPlayerActivity(Context context, Channel stream, String chatRoom) {
        startPlayerActivity(context, stream, null, chatRoom, false, null);
    }

    public static void startPlayerActivity(Context context, Stream stream, String chatRoom) {
        startPlayerActivity(context, stream, null, chatRoom, false, null);
    }

    public static void startMarathonPlayerActivity(Context context, Stream stream, String horaroID) {
        startPlayerActivity(context, stream, null, null, false, horaroID);
    }

    public static void startPlayerActivity(final Context context, final Channel channel,
                                           final String quality, final String chatRoom,
                                           final boolean loadChannel, final String horaroID) {
        TwitchAPI.getService().getChannelToken(channel.name()).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    AccessToken token = response.body();
                    String url = Constants.URL_USHER;
                    url = url.replace("{channel}", channel.name());
                    Uri uri = Uri.parse(url)
                            .buildUpon()
                            //.appendQueryParameter("allow_audio_only", "false")
                            .appendQueryParameter("token", token.token())
                            .appendQueryParameter("sig", token.sig())
                            .appendQueryParameter("allow_source", "true")
                            .appendQueryParameter("allow_spectre", "true")
                            .appendQueryParameter("p", new Random().nextInt(999999) + "")
                            .build();

                    final Intent intent = new Intent(context, PlayerActivity.class)
                            .setData(uri)
                            .putExtra("channel", channel);
                    if (quality == null) {
                        intent.putExtra("quality", getQuality(context));
                    } else {
                        intent.putExtra("quality", quality);
                    }
                    if (chatRoom != null) {
                        intent.putExtra("chatRoom", chatRoom);
                    } else {
                        intent.putExtra("chatRoom", channel.name());
                    }
                    if (loadChannel) {
                        intent.putExtra("loadChannel", true);
                    }
                    if (horaroID != null) {
                        intent.putExtra("isMarathon", true);
                        intent.putExtra("horaroID", horaroID);
                    }
                    context.startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {

            }
        });
    }

    public static void startPlayerActivity(Context context, Stream stream) {
        startPlayerActivity(context, stream, null, null, false, null);
    }

    public static void startPlayerActivity(final Context context, final Stream stream,
                                           final String quality, final String chatRoom,
                                           final boolean loadChannel, final String horaroID) {
        final Channel channel = stream.channel();
        TwitchAPI.getService().getChannelToken(channel.name()).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    AccessToken token = response.body();
                    String url = Constants.URL_USHER;
                    url = url.replace("{channel}", channel.name());
                    Uri uri = Uri.parse(url)
                            .buildUpon()
                            //.appendQueryParameter("allow_audio_only", "false")
                            .appendQueryParameter("token", token.token())
                            .appendQueryParameter("sig", token.sig())
                            .appendQueryParameter("allow_source", "true")
                            .appendQueryParameter("allow_spectre", "true")
                            .appendQueryParameter("p", new Random().nextInt(999999) + "")
                            .build();

                    final Intent intent = new Intent(context, PlayerActivity.class)
                            .setData(uri)
                            .putExtra("stream", stream);
                    if (quality == null) {
                        intent.putExtra("quality", getQuality(context));
                    } else {
                        intent.putExtra("quality", quality);
                    }
                    if (chatRoom != null) {
                        intent.putExtra("chatRoom", chatRoom);
                    } else {
                        intent.putExtra("chatRoom", channel.name());
                    }
                    if (loadChannel) {
                        intent.putExtra("loadChannel", true);
                    }
                    if (horaroID != null) {
                        intent.putExtra("isMarathon", true);
                        intent.putExtra("horaroID", horaroID);
                    }
                    context.startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {

            }
        });
    }

    public static void startChatOnlyActivity(Context context, Channel channel) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("chatOnly", true);
        intent.putExtra("chatRoom", channel.name());
        intent.putExtra("channel", channel);
        context.startActivity(intent);
    }

    private static String getQuality(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        if (isWiFi) {
            return context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE).
                    getString(Constants.SETTING_QUALITY, "medium");
        }
        return context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE).
                getString(Constants.SETTING_QUALITY_MOBILE, "mobile");
    }

    public static String getToken(Context context) {
            return context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE).
                    getString(Constants.OAUTH, Constants.NO_TOKEN);
    }

    public static String buildString(String... strings) {
        if (strings != null && strings.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string);
            }
            return sb.toString();
        }
        return "";
    }

    public static int calculateNumberOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (dpWidth / 180);
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns if a character is one of Chinese-Japanese-Korean characters.
     *
     * @param c the character to be tested
     * @return true if CJK, false otherwise
     */
    public static boolean isCharCJK(char c) {
        return (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS);
    }

    public static Map<TimeUnit ,Long> computeTimeDiff(Calendar date1, Calendar date2) {
        long diffInMillies = date2.getTimeInMillis() - date1.getTimeInMillis();
        List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);

        Map<TimeUnit, Long> result = new LinkedHashMap<TimeUnit, Long>();
        long milliesRest = diffInMillies;
        for ( TimeUnit unit : units ) {
            long diff = unit.convert(milliesRest, TimeUnit.MILLISECONDS);
            long diffInMilliesForUnit = unit.toMillis(diff);
            milliesRest = milliesRest - diffInMilliesForUnit;
            result.put(unit,diff);
        }
        return result;
    }

    public static void parseTime() {
    }

    public static boolean checkSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

}
