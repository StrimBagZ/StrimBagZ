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
import android.graphics.Bitmap;
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
        setupRemoteConfig();

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

    private void testHoraro() {
        HoraroAPI.getService().getTicker("3c11hs37lh81pg7a91").enqueue(new Callback<Ticker>() {
            @Override
            public void onResponse(Call<Ticker> call, Response<Ticker> response) {
                if (response.code() == 200) {
                    if (response.body().data().ticker().next() != null) {
                        Log.d("HoraroAPI", response.body().toString());
                        Calendar currentTime = Calendar.getInstance();
                        Calendar time = Calendar.getInstance();
                        time.setTimeInMillis(response.body().data().ticker().next().scheduled_t() * 1000);
                        Log.d("HoraroAPI", "Time: " + time);
                        if (DateFormat.is24HourFormat(MainActivity.this)) {
                            Log.d("HoraroAPI", "Next run starts at: " + DateFormat.format("HH:mm", time));
                        } else {
                            if (Utils.checkSameDay(currentTime, time)) {
                                Log.d("HoraroAPI", "Next run starts at: " + DateFormat.format("hh:mm a", time));
                            } else {
                                Log.d("HoraroAPI", "Next run starts at: " + DateFormat.format("d MMM hh:mm a", time));
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Ticker> call, Throwable t) {

            }
        });
    }

    private void testSRL() {
        SpeedRunsLive.getService().getRaces().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String json = response.body().string();
                    JSONObject object = new JSONObject(json);
                    Races races = new Races(new ArrayList<Races.Race>());
                    JSONArray rac = object.getJSONArray("races");

                    for (int i = 0; i < rac.length(); i++) {
                        JSONObject row = rac.getJSONObject(i);
                        String state = row.getString("statetext");
                        // We don't care about Races that are done.
                        if (!state.equals("Complete")) {
                            String id = row.getString("id");
                            Races.RaceGame game =
                                    new Races.RaceGame(row.getJSONObject("game").getString("name"), row.getJSONObject("game").getString("abbrev"));
                            String goal = row.getString("goal");
                            ArrayList<Entrant> entrants = new ArrayList<Entrant>();
                            JSONObject entrantsJSON = row.getJSONObject("entrants");
                            Iterator<String> keys = entrantsJSON.keys();
                            while (keys.hasNext()) {
                                JSONObject entrant = entrantsJSON.getJSONObject(keys.next());
                                String displayName = entrant.getString("displayname");
                                long place = entrant.getLong("place");
                                long time = entrant.getLong("time");
                                String message = null;
                                if (entrant.getString("message") != null) {
                                    message = entrant.getString("message");
                                }
                                String statetext = entrant.getString("statetext");
                                String twitch = entrant.getString("twitch");
                                String trueskill = entrant.getString("trueskill");
                                entrants.add(new Entrant(displayName, place, time, message, statetext, twitch, trueskill));
                            }
                            races.races.add(new Races.Race(id, game, goal, entrants, state));
                        }
                    }

                    for (Races.Race race: races.races) {
                        Log.d("Race", race.toString());
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("onFailure", t.getMessage());
            }
        });
    }

    private void testJSON() {
        List<SRLRaceEntrant> list = new ArrayList<>();
        String json = "[[\"tob3000\",\"sva16162\",\"makko9143\",\"spamminn\",\"fig02\",\"alaris_villain\",\"psymarth\",\"moosecrap\",\"flanthis\",\"phoenixfeather1\",\"cma2819\",\"mikekatz45\",\"sniping117\",\"exodus122\",\"mrjabujabu\"],{\"entrants\":{\"alaris_villain\":{\"channel\":\"alaris_villain\",\"comment\":\"row3\",\"display_name\":\"alaris_villain\",\"place\":10,\"state\":\"done\",\"time\":5388},\"cma\":{\"channel\":\"cma2819\",\"comment\":\"row3\",\"display_name\":\"Cma\",\"place\":8,\"state\":\"done\",\"time\":5064},\"exodus\":{\"channel\":\"exodus122\",\"comment\":\"col 2\",\"display_name\":\"Exodus\",\"place\":2,\"state\":\"done\",\"time\":4611},\"fig02\":{\"channel\":\"fig02\",\"display_name\":\"fig02\",\"place\":12,\"state\":\"done\",\"time\":6805},\"flanthis\":{\"channel\":\"flanthis\",\"comment\":\"bltr omg this bingo was trash\",\"display_name\":\"flanthis\",\"place\":11,\"state\":\"done\",\"time\":5685},\"makko\":{\"channel\":\"makko9143\",\"comment\":\"col1 blame blank b\",\"display_name\":\"makko\",\"place\":7,\"state\":\"done\",\"time\":5037},\"marthur\":{\"comment\":\"row2 i dont know how to get to spirit so i just quit :P\",\"display_name\":\"marthur\",\"state\":\"forfeit\"},\"mikekatz45\":{\"channel\":\"mikekatz45\",\"comment\":\"col2\",\"display_name\":\"MikeKatz45\",\"place\":1,\"state\":\"done\",\"time\":4598},\"moosecrap\":{\"channel\":\"moosecrap\",\"comment\":\"c4 routing mistakes\",\"display_name\":\"moosecrap\",\"place\":13,\"state\":\"done\",\"time\":8626},\"mrjabujabu\":{\"channel\":\"mrjabujabu\",\"display_name\":\"Mrjabujabu\",\"place\":14,\"state\":\"done\",\"time\":8741},\"niamek\":{\"comment\":\"MRJABUJABU I will redeem myself eventually. Prepare yourself!\",\"display_name\":\"niamek\",\"state\":\"forfeit\"},\"phoenixfeather\":{\"channel\":\"phoenixfeather1\",\"comment\":\"row 4\",\"display_name\":\"PhoenixFeather\",\"place\":4,\"state\":\"done\",\"time\":4834},\"psymarth\":{\"channel\":\"psymarth\",\"comment\":\"row 4; mostly child\",\"display_name\":\"PsyMarth\",\"place\":5,\"state\":\"done\",\"time\":4959},\"sniping117\":{\"channel\":\"sniping117\",\"comment\":\"tl-br, so many hearts but mine is broken\",\"display_name\":\"SNIPING117\",\"place\":9,\"state\":\"done\",\"time\":5357},\"spamminn\":{\"channel\":\"spamminn\",\"display_name\":\"spamminn\",\"place\":15,\"state\":\"done\",\"time\":9263},\"sva\":{\"channel\":\"sva16162\",\"comment\":\"col 1 no hover boots for beat deku and water, that was scary\",\"display_name\":\"sva\",\"place\":3,\"state\":\"done\",\"time\":4743},\"tob3000\":{\"channel\":\"tob3000\",\"comment\":\"row3, bad\",\"display_name\":\"tob3000\",\"place\":6,\"state\":\"done\",\"time\":5034}},\"filename\":\"true\",\"game\":\"The Legend of Zelda: Ocarina of Time\",\"goal\":\"http://www.speedrunslive.com/tools/oot-bingo?mode=normal\\u0026amp;seed=455884\",\"id\":\"nid68\",\"state\":\"done\",\"time\":1.475977149e+09}]";
        JSONObject entrants = null;
        Iterator<String> keys = null;
        try {
            JSONArray array = new JSONArray(json);
            entrants = array.getJSONObject(1).getJSONObject("entrants");
            keys = entrants.keys();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<SRLRaceEntrant> jsonAdapter = SRLRaceEntrant.jsonAdapter(moshi);
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject entrant = null;
            try {
                entrant = (JSONObject) entrants.get(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            SRLRaceEntrant test = null;
            try {
                test = jsonAdapter.fromJson(entrant.toString());
                list.add(test);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (SRLRaceEntrant e: list) {
            Log.d("Json test", e.toString());
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
        return user;
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
                        }
                        if (channel.profileBanner() != null) {
                            Glide.with(MainActivity.this)
                                    .load(channel.profileBanner())
                                    .fitCenter()
                                    .into((ImageView) navigationHeader.findViewById(R.id.headerBackground));
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
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("gameStreams");
        if (fragment == null) {
            fragment = new LiveStreamsFragment();
        }
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
