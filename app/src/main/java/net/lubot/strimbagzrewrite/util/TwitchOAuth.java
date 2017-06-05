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
package net.lubot.strimbagzrewrite.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.data.model.Twitch.OAuthToken;

import retrofit2.Call;
import retrofit2.Response;

public class TwitchOAuth {

    public static void getToken(final Activity activity, String code) {
        Call<OAuthToken> call = TwitchKraken.getService().getToken(Constants.TOKEN_URL, code);
        call.enqueue(new CallbackZ<OAuthToken>(call, true) {
            @Override
            public void onResponse(Call<OAuthToken> call, Response<OAuthToken> response) {
                if (response.isSuccessful()) {
                    SharedPreferences.Editor pref = activity.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE).edit();
                    OAuthToken token = response.body();
                    pref.putString(Constants.OAUTH, token.access_token());
                    if (token.refresh_token() != null) {
                        pref.putString(Constants.REFRESH, token.refresh_token());
                    }
                    pref.apply();
                    Utils.getTwitchUser(activity, true);
                }
            }
        });
    }

    public static void revokeToken(String token) {
        Call<Void> call = TwitchKraken.getService().revokeToken(Constants.REVOKE_URL, token);
        call.enqueue(new CallbackZ<Void>(call, true) {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("RevokeToken", "Token revoked.");
                } else {
                    Log.d("RevokeToken", "Couldnt revoke Token.");
                }
            }
        });
    }
}
