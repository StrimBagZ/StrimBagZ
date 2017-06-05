package net.lubot.strimbagzrewrite.data.model.Twitch;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class OAuthToken {
    public abstract String access_token();
    @Nullable
    public abstract String refresh_token();
    public abstract List<String> scope();

    public static JsonAdapter<OAuthToken> jsonAdapter(Moshi moshi) {
        return new AutoValue_OAuthToken.MoshiJsonAdapter(moshi);
    }
}
