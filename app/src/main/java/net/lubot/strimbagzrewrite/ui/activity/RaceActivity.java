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
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Entrant;
import net.lubot.strimbagzrewrite.data.model.SpeedRunsLive.Races;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.adapter.EmptyRecyclerViewAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.StreamsAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

import org.polaric.colorful.CActivity;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RaceActivity extends CActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private StreamsAdapter adapter;
    private EmptyRecyclerViewAdapter emptyView;
    private Races.Race race;
    private final String TAG = "RaceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_race);

        Intent intent = getIntent();
        race = intent.getParcelableExtra("race");

        Log.d(TAG, "SRCGame: " + race.game.name);
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
        Glide.with(this)
                .load("http://cdn.speedrunslive.com/images/games/" + race.game.abbrev + ".jpg")
                .placeholder(R.drawable.ic_srl)
                .centerCrop()
                .into(imageView);
        getEntrantsStreams(race.entrants);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Channel channel = adapter.getSelectedChannel();
        switch(item.getItemId()){
            case R.id.ctn_openStream_Mobile:
                Utils.startPlayerActivity(this, channel, "mobile");
                break;
            case R.id.ctn_openStream_Low:
                Utils.startPlayerActivity(this, channel, "low");
                break;
            case R.id.ctn_openStream_Medium:
                Utils.startPlayerActivity(this, channel, "medium");
                break;
            case R.id.ctn_openStream_High:
                Utils.startPlayerActivity(this, channel, "high");
                break;
            case R.id.ctn_openStream_Source:
                Utils.startPlayerActivity(this, channel, "source");
                break;
            case R.id.ctn_openChatOnly:
                Utils.startChatOnlyActivity(this, channel);
                break;
        }
        return true;
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

        TwitchKraken.getService().getStreams(null, twitchNames, 0, entrants.size(), 3)
                .enqueue(new Callback<LiveStreams>() {
                    @Override
                    public void onResponse(Call<LiveStreams> call, Response<LiveStreams> response) {
                        if (response.code() == 200) {
                            if (adapter != null && !response.body().streams().isEmpty()) {
                                adapter.clear();
                                adapter.addAll(response.body().streams());
                            }
                            if (response.body().streams().isEmpty()) {

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