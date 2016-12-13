package net.lubot.strimbagzrewrite.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Entrant;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Races;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.adapter.StreamsAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RaceActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    StreamsAdapter adapter;
    Races.Race race;
    private final String TAG = "RaceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.onActivityCreateSetTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        Intent intent = getIntent();
        race = intent.getParcelableExtra("race");

        Log.d(TAG, "Game: " + race.game.name);
        Log.d(TAG, "Goal: " + race.goal);
        Log.d(TAG, "Entrants: " + race.entrants.toString());

        final CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_container);
        recyclerView = (RecyclerView) findViewById(R.id.eventRecyclerView);
        toolbarLayout.setTitle(race.game.name);

        AppBarLayout barLayout = (AppBarLayout) findViewById(R.id.appbar_container);
        ImageView imageView = (ImageView) findViewById(R.id.imgToolbar);
        /*
        barLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    toolbarLayout.setTitle(race.game.name);
                    isShow = true;
                } else if(isShow) {
                    toolbarLayout.setTitle("");
                    isShow = false;
                }
            }
        });
        */
        adapter = new StreamsAdapter(this);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        /*
        Glide.with(this)
                .load("http://cdn.speedrunslive.com/images/games/" + race.game.abbrev + ".jpg")
                .placeholder(R.drawable.ic_srl)
                .centerCrop()
                .into(imageView);
        */
        getEntrantsStreams(race.entrants);
    }

    private void getEntrantsStreams(ArrayList<Entrant> entrants) {
        String twitchNames = "";
        for (int i = 0, entrantsSize = entrants.size(); i < entrantsSize; i++) {
            Entrant entrant = entrants.get(i);
            if (!entrant.twitch.isEmpty()) {
                twitchNames += entrant.twitch.toLowerCase();
                if (i != (entrantsSize - 1)) {
                    twitchNames += ",";
                }
            }
        }

        TwitchKraken.getService().getStreams(null, twitchNames, 0, entrants.size())
                .enqueue(new Callback<LiveStreams>() {
                    @Override
                    public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                        if (response.code() == 200) {
                            if (adapter != null) {
                                adapter.clear();
                                adapter.addAll(response.body().streams());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LiveStreams> call, Throwable t) {
                        Log.d("onFailure", t.getMessage());
                    }
                });
    }

}