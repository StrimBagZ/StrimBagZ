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
package net.lubot.strimbagzrewrite.ui.activity;

import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.lubot.strimbagzrewrite.BuildConfig;
import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.HoraroAPI;
import net.lubot.strimbagzrewrite.data.model.FrankerFaceZ.SRLRaceEntrant;
import net.lubot.strimbagzrewrite.data.model.Horaro.Ticker;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Entrant;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Races;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.KrakenBase;
import net.lubot.strimbagzrewrite.data.SpeedRunsLive;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.fragment.FollowingFragment;
import net.lubot.strimbagzrewrite.ui.fragment.LiveStreamsFragment;
import net.lubot.strimbagzrewrite.ui.fragment.MarathonFragment;
import net.lubot.strimbagzrewrite.ui.fragment.SRLFragment;
import net.lubot.strimbagzrewrite.ui.fragment.SettingsFragment;
import net.lubot.strimbagzrewrite.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private Toolbar mToolbar;
    private NavigationView navigationView;
    private View navigationHeader;
    private ImageView profile_image;
    //private Fragment currentFragment;
    //private String currentFragmentTag;

    private SharedPreferences preferences;
    private String login = Constants.NO_USER;
    private String user = Constants.NO_USER;
    private String token = Constants.NO_TOKEN;

    private long reloadTimestamp = 0;

    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    //private CastContext mCastContext;
    //private CastSession mCastSession;
    //private final SessionManagerListener<CastSession> mSessionManagerListener =
    //        new MySessionManagerListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updatePreferences();
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_main);

        //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        //Log.d("Firebase Token", refreshedToken + "");

        //FirebaseMessaging.getInstance().subscribeToTopic("dev");
        if (!preferences.getBoolean("update_notification", false)) {
            FirebaseMessaging.getInstance().subscribeToTopic("updates");
            preferences.edit().putBoolean("update_notification", true).apply();
        }

        if(!preferences.getBoolean("rewrite_dialog", false)) {
            showRewriteDialog();
            preferences.edit().putBoolean("rewrite_dialog", true).apply();
        }

        //mCastContext = CastContext.getSharedInstance(this);
        //mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationHeader = navigationView.getHeaderView(0);
        profile_image = (ImageView) navigationHeader.findViewById(R.id.avatar);

        getToken();
        getLogin();
        getDisplayName();
        initNavigation();

        if (login != null && !login.equals(Constants.NO_USER)) {
            // We know the login, so we can directly get the channel information of the user
            getChannel(login);
        } else if (token != null && !token.equals(Constants.NO_TOKEN)) {
            // We have the OAuth token of the user, call the base and get the login name
            getBase();
        }

        boolean recreateSettings = preferences.getBoolean(Constants.RECREATE_SETTINGS, false);
        if (recreateSettings) {
            Fragment fragmentPref = getSupportFragmentManager()
                    .findFragmentByTag(Constants.FRAGMENT_SETTINGS);
            if (fragmentPref == null) {
                Log.d("fragmentPref", "create new Settings fragment");
                fragmentPref = new SettingsFragment();
            }
            //currentFragment = fragmentPref;
            replaceFragment(fragmentPref, Constants.FRAGMENT_SETTINGS, false);
            preferences.edit().putBoolean(Constants.RECREATE_SETTINGS, false).apply();
            return;
        }

        if (savedInstanceState != null) {
            Log.d("savedInstanceState", "is not null");
            Set<String> set = savedInstanceState.keySet();
            for (String item : set) {
                Log.d("savedInstance", item);
            }
            /*
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, "currentFragment");
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, currentFragment, currentFragmentTag);
            transaction.commit();
            */
        } else {
            //currentFragment = new FollowingFragment();
            //SRLFragment fragment = new SRLFragment();
            //LiveStreamsFragment fragment = new LiveStreamsFragment();
            //TestFragment fragment = new TestFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer,
                    new FollowingFragment(), Constants.FRAGMENT_FOLLOWING);
            transaction.commit();
        }

        if (getIntent() != null) {
            onNewIntent(getIntent());
        }

    }

    private void showWebViewDialog() {
        new AlertDialog.Builder(this)
                .setTitle("WebView not detected")
                .setMessage("It looks like WebView is not installed on your device\n" +
                        "Typically this is the case when using a Custom ROM\n" +
                        "Please install it from the Play Store in order to properly use this app")
                .setCancelable(false)
                .setPositiveButton("Open Play Store", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=com.google.android.webview"));
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void showRewriteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("StrimBagZ Reloaded")
                .setMessage("It has been a long time since StrimBagZ received an update, today it's finally time.\n" +
                        "StrimBagZ was rewritten from scratch, some features are not completely finished, but we're getting there, " +
                        "most importantly the 'Received Data Invalid' error is fixed, which I'm sorry about that it took so long to get fixed.\n" +
                        "I try to release updates more rapidly in the upcoming days to fix any other issues and to reimplement and introduce new features.\n\n" +
                        "- Luigitus")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
    }

    private void initNavigation() {
        setSupportActionBar(mToolbar);
        if (drawerLayout != null) {
            toggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.navigation_open, R.string.navigation_close);
            toggle.setDrawerIndicatorEnabled(true);
            toggle.syncState();
        }

        Menu menu = navigationView.getMenu();
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        navigationView.setCheckedItem(item.getItemId());
        switch (item.getItemId()) {
            case R.id.following:
                Fragment fragmentFollowing = getSupportFragmentManager()
                        .findFragmentByTag(Constants.FRAGMENT_FOLLOWING);
                if (fragmentFollowing == null) {
                    fragmentFollowing = new FollowingFragment();
                }
                //getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                replaceFragment(fragmentFollowing, Constants.FRAGMENT_FOLLOWING);
                break;
            case R.id.srl:
                Fragment fragmentSRL = getSupportFragmentManager()
                        .findFragmentByTag(Constants.FRAGMENT_SRL);
                if (fragmentSRL == null) {
                    fragmentSRL = new SRLFragment();
                }
                //getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                replaceFragment(fragmentSRL, Constants.FRAGMENT_SRL);
                break;
            case R.id.marathon:
                Fragment fragmentMarathon = getSupportFragmentManager()
                        .findFragmentByTag(Constants.FRAGMENT_MARATHON);
                if (fragmentMarathon == null) {
                    fragmentMarathon = new MarathonFragment();
                }
                //getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                replaceFragment(fragmentMarathon, Constants.FRAGMENT_MARATHON);
                break;
            case R.id.preferences:
                Fragment fragmentPref = getSupportFragmentManager()
                        .findFragmentByTag(Constants.FRAGMENT_SETTINGS);
                if (fragmentPref == null) {
                    fragmentPref = new SettingsFragment();
                }
                //getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                replaceFragment(fragmentPref, Constants.FRAGMENT_SETTINGS);
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //getMenuInflater().inflate(R.menu.player_cast, menu);
        //CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        Map<String, String> map = new HashMap<>();
        return true;
    }

    @Override
    protected void onResume() {
        Log.d("onResume", "called");
        fetchConfig();
        updatePreferences();

        boolean isInstalled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Since Android N the WebView can be get from Chrome too.
            isInstalled = Utils.isPackageInstalled("com.android.chrome", getPackageManager());
            Log.d("WebView", "Chrome Installed: " + isInstalled);
            if (!isInstalled) {
                // If Chrome is not installed, the normal WebView is used (if it exists).
                isInstalled = Utils.isPackageInstalled("com.google.android.webview", getPackageManager());
                Log.d("WebView", "WebView (N) Installed: " + isInstalled);
            }
        } else {
            // Older versions of Android just use the WebView package.
            isInstalled = Utils.isPackageInstalled("com.google.android.webview", getPackageManager());
            Log.d("WebView", "Legacy Installed: " + isInstalled);
        }

        if (!isInstalled) {
            showWebViewDialog();
        }
        /*
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        if (mCastSession == null) {
            mCastSession = CastContext.getSharedInstance(this).getSessionManager()
                    .getCurrentCastSession();
        }
        */
        super.onResume();
    }

    @Override
    protected void onPause() {
        //mCastContext.getSessionManager().removeSessionManagerListener(
        //        mSessionManagerListener, CastSession.class);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //getSupportFragmentManager().putFragment(outState, "currentFragment", currentFragment);
        //outState.putString("currentFragmentTag", currentFragmentTag);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onResult", "RequestCode: " + requestCode + " ResultCode: " + resultCode);
        switch (resultCode) {
            case Constants.LOGGED_IN:
                Utils.restartActivity(this);
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra("startStream", false)) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1234);
            startStream(intent.getStringExtra("channel"));
        }
    }

    private void updatePreferences() {
        preferences = getSharedPreferences(Constants.SETTINGS, MODE_PRIVATE);
    }

    private void fetchConfig() {
        if (firebaseRemoteConfig == null) {
            setupRemoteConfig();
        }
        long cacheExpiration = 3600;
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    firebaseRemoteConfig.activateFetched();
                }
                updateNavigationMenu();
            }
        });
    }

    private void updateNavigationMenu() {
        Menu menu = navigationView.getMenu();
        if (firebaseRemoteConfig.getBoolean(Constants.MARATHON_RUNNING)) {
            menu.findItem(R.id.marathon).setVisible(true);
            menu.findItem(R.id.marathon).setTitle(firebaseRemoteConfig.getString(Constants.MARATHON_NAME));
        } else {
            menu.findItem(R.id.marathon).setVisible(false);
        }
    }

    public String getToken() {
        Log.d("MainActivity", "Before Token " + token);
        boolean isPreference = preferences != null;
        if (token == null && isPreference) {
            token = preferences.getString(Constants.OAUTH, Constants.NO_TOKEN);
            return token;
        } else if ((token != null && token.equals(Constants.NO_TOKEN)) && isPreference) {
            token = preferences.getString(Constants.OAUTH, Constants.NO_TOKEN);
        } else if (!isPreference) {
            updatePreferences();
            token = preferences.getString(Constants.OAUTH, Constants.NO_TOKEN);
        }
        Log.d("MainActivity", "After Token " + token);
        return token;
    }

    public String getLogin() {
        Log.d("MainActivity", "Before Login " + login);
        boolean isPreference = preferences != null;
        if (login == null && isPreference) {
            login = preferences.getString(Constants.LOGIN, Constants.NO_USER);
            return login;
        } else if ((login != null && login.equals(Constants.NO_USER)) && isPreference) {
            login = preferences.getString(Constants.LOGIN, Constants.NO_USER);
        } else if (!isPreference) {
            updatePreferences();
            login = preferences.getString(Constants.LOGIN, Constants.NO_USER);
        }
        Log.d("MainActivity", "After Login " + login);
        return login;
    }

    public String getDisplayName() {
        Log.d("MainActivity", "Before DisplayName " + user);
        boolean isPreference = preferences != null;
        if (user == null && isPreference) {
            user = preferences.getString(Constants.DISPLAY_NAME, Constants.NO_USER);
            return user;
        } else if ((user != null && user.equals(Constants.NO_USER)) && isPreference) {
            user = preferences.getString(Constants.DISPLAY_NAME, Constants.NO_USER);
        } else if (!isPreference) {
            updatePreferences();
            user = preferences.getString(Constants.DISPLAY_NAME, Constants.NO_USER);
        }
        Log.d("MainActivity", "After DisplayName " + user);
        return user;
    }

    private void getBase() {
        TwitchKraken.getService().getBase().enqueue(new Callback<KrakenBase>() {
            @Override
            public void onResponse(Call<KrakenBase> call, Response<KrakenBase> response) {
                if (response.code() == 200) {
                    String login = response.body().token().user_name();
                    preferences.edit().putString("login", login).apply();
                    getChannel(login);
                }
            }

            @Override
            public void onFailure(Call<KrakenBase> call, Throwable t) {
                Log.d("getBase onFailure", t.getMessage());
            }
        });
    }

    public long getReloadTimestamp() {
        return reloadTimestamp;
    }

    public void getChannel(String channel) {
        TwitchKraken.getService().getChannel(channel).enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, final Response<Channel> response) {
                if (response.code() != 200) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Channel channel = response.body();

                        if (channel.logo() != null) {
                            try {
                                Glide.with(MainActivity.this)
                                        .load(channel.logo())
                                        .asBitmap()
                                        .centerCrop()
                                        .into(new BitmapImageViewTarget(profile_image) {
                                            @Override
                                            protected void setResource(Bitmap resource) {
                                                RoundedBitmapDrawable circularBitmapDrawable =
                                                        RoundedBitmapDrawableFactory
                                                                .create(MainActivity.this.getResources(), resource);
                                                circularBitmapDrawable.setCircular(true);
                                                profile_image.setImageDrawable(circularBitmapDrawable);
                                            }
                                        });
                            } catch (IllegalArgumentException ignored) {}
                        }
                        if (channel.profileBanner() != null) {
                            try {
                                Glide.with(MainActivity.this)
                                        .load(channel.profileBanner())
                                        .fitCenter()
                                        .into((ImageView) navigationHeader.findViewById(R.id.headerBackground));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        TextView user = (TextView) navigationHeader.findViewById(R.id.username);
                        if (channel.displayName() != null) {
                            user.setText(channel.displayName());
                        } else {
                            user.setText(channel.name());
                        }
                        preferences.edit()
                                .putString("displayName", channel.displayName())
                                .putString("twitch_id", channel.id() + "")
                                .apply();
                    }
                });
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {
                Log.d("getChannel onFailure", t.getMessage());
            }
        });
    }

    public FirebaseAnalytics getAnalytics() {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.this);
        }
        return firebaseAnalytics;
    }

    private void setupRemoteConfig() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        if (BuildConfig.DEBUG) {
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(true)
                    .build();
            firebaseRemoteConfig.setConfigSettings(configSettings);
        }
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
    }

    public FirebaseRemoteConfig getRemoteConfig() {
        if (firebaseRemoteConfig == null) {
            setupRemoteConfig();
        }
        return firebaseRemoteConfig;
    }

    public void trackActivity(String event ,Bundle bundle) {
        if (bundle != null) {
            firebaseAnalytics.logEvent(event, bundle);
        }
    }

    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    public void showStreams(Bundle args) {
        Fragment fragment = new LiveStreamsFragment();
        fragment.setArguments(args);
        replaceFragment(fragment, "gameStreams");
    }

    public void startStream(String channel) {
        TwitchKraken.getService().getChannel(channel).enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {
                Utils.startPlayerActivity(MainActivity.this, response.body());
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {

            }
        });
    }

    private void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag , true);
    }

    private void replaceFragment(Fragment fragment, String tag , boolean backStack) {
        if (fragment != null && !fragment.isAdded()) {
            //currentFragment = fragment;
            //currentFragmentTag = tag;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, fragment, tag);
            if (backStack) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    /*
    private class MySessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == mCastSession) {
                mCastSession = null;
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            mCastSession = session;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            invalidateOptionsMenu();

            MediaInfo media = new MediaInfo.Builder("http://video-edge-3aae48.ams01.hls.ttvnw.net/hls-5cb254/exordobiyo_22766339008_498593467/chunked/index-live.m3u8?token=id=3053870472425236595,bid=22766339008,exp=1470967814,node=video-edge-3aae48.ams01,nname=video-edge-3aae48.ams01,fmt=chunked&sig=946f8799fcff2ec9cd0d5d8ffd41fb0c5fa075e8\n")
                    .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                    .setContentType("application/vnd.apple.mpegurl")
                    .build();
            RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            remoteMediaClient.load(media);

        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }
    */
}
