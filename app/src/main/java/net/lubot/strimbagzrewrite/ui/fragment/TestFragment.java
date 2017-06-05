package net.lubot.strimbagzrewrite.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.data.model.Twitch.AccessToken;
import net.lubot.strimbagzrewrite.data.model.Twitch.StreamObject;
import net.lubot.strimbagzrewrite.data.TwitchAPI;
import net.lubot.strimbagzrewrite.data.TwitchKraken;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.activity.PlayerActivity;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestFragment extends Fragment {
    private TextView status;
    private Button btnTest;
    private String channel = "luigitus";
    private ArrayList<String> followButtons = new ArrayList<>();
    private final Executor writeExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.player_fragment, null, false);

        Button loginButton = (Button) root.findViewById(R.id.loginButton);
        Button playerButton = (Button) root.findViewById(R.id.startVideo);
        btnTest = (Button) root.findViewById(R.id.btn_Test);
        status = (TextView) root.findViewById(R.id.status);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent Intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(Constants.URL_TWITCH_AUTHENTICATION));
                startActivity(Intent);
            }
        });
        playerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPlayerActivity("nightfallx");
            }
        });
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    }

    private void getStreamStatus(final String channel) {
        TwitchKraken.getService().getStreamStatus(channel).enqueue(new Callback<StreamObject>() {
            @Override
            public void onResponse(Call<StreamObject> call, Response<StreamObject> response) {
                if (response.isSuccessful()) {
                    boolean isLive = false;
                    StreamObject stream = response.body();
                    Log.d("StreamStatus", stream.toString());
                    if (stream.stream() != null) {
                        isLive = true;
                    }
                    String text = "SGDQ Stream status: ";
                    if (isLive) {
                        text += "Live";
                    } else {
                        text += "Offline";
                    }
                    final String finalText = text;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status.setText(finalText);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<StreamObject> call, Throwable t) {

            }
        });
    }

    private void startPlayerActivity(final String channel) {
        TwitchAPI.getService().getChannelToken(channel).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    AccessToken token = response.body();
                    String url = "https://usher.ttvnw.net/api/channel/hls/{channel}.m3u8";
                    url = url.replace("{channel}", channel);
                    Uri uri = Uri.parse(url)
                            .buildUpon()
                            .appendQueryParameter("allow_audio_only", "false")
                            .appendQueryParameter("token", token.token())
                            .appendQueryParameter("sig", token.sig())
                            .appendQueryParameter("allow_source", "true")
                            .appendQueryParameter("allow_spectre", "true")
                            .appendQueryParameter("p", new Random().nextInt(999999) + "")
                            .build();

                    final Intent intent = new Intent(getActivity(), PlayerActivity.class)
                            .setData(uri)
                            .putExtra("channel", channel)
                            .putExtra("quality", "source");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Intent i = new Intent(getActivity(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getActivity().finish();
            startActivity(i);
        }
    }
}
