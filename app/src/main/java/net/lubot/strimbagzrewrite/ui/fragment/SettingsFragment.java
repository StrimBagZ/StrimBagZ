/*
 * Copyright 2016 Nicola Fäßler
 *
 * This file is part of StrimBagZ.
 *
 * StrimBagZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by¹
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
package net.lubot.strimbagzrewrite.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.util.Utils;

import org.polaric.colorful.Colorful;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOGIN = "setting_login";
    private final String LOGOUT = "setting_logout";
    private final String REFRESH_TOKEN = "setting_refresh_token";
    private final String COLOR_PRIMARY = "setting_primary_color";
    private final String COLOR_ACCENT = "setting_accent_color";

    private Activity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTitle("Settings");
        }
        getPreferenceManager().setSharedPreferencesName(Constants.SETTINGS);
        if (getPreferenceManager().getSharedPreferences()
                .getString(Constants.OAUTH, Constants.NO_TOKEN).equals(Constants.NO_TOKEN)) {
            setPreferencesFromResource(R.xml.preferences, null);
        } else {
            setPreferencesFromResource(R.xml.preferences_loggedin, null);
        }
        PreferenceManager.setDefaultValues(activity, Constants.SETTINGS, 0, R.xml.preferences, false);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case LOGIN:
                loginUser();
                break;
            case LOGOUT:
                logoutUser();
                break;
            case REFRESH_TOKEN:
                break;
        }
        Log.d("Settings", "Clicked " + preference.getKey());
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals("setting_dark_theme")) {
            preferences.edit().putBoolean(Constants.RECREATE_SETTINGS, true).apply();
            boolean darkTheme = preferences.getBoolean(Constants.SETTING_DARK_THEME, false);
            Colorful.config(activity)
                    .dark(darkTheme)
                    .apply();
            Utils.restartActivity(activity);
        }
        if (key.equals("COLORFUL_PREF_KEY")) {
            preferences.edit().putBoolean(Constants.RECREATE_SETTINGS, true).apply();
            Utils.restartActivity(activity);
        }
        Log.d("onSharedChange", key);
    }

    private void loginUser() {
        Intent Intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(Constants.URL_TWITCH_AUTHENTICATION));
        startActivity(Intent);
    }

    private void logoutUser() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
        new WebView(activity.getApplicationContext()).clearCache(true);
        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
        editor.putString(Constants.LOGIN, Constants.NO_USER);
        editor.putString(Constants.DISPLAY_NAME, Constants.NO_USER);
        editor.putString(Constants.OAUTH, Constants.NO_TOKEN);
        editor.putString(Constants.TWITCH_ID, Constants.NO_USER);
        editor.apply();
        Utils.restartActivity(activity);
    }

}
