package net.lubot.strimbagzrewrite.data.model;

import com.squareup.moshi.JsonAdapter;
import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;

@MoshiAdapterFactory
public abstract class AdapterFactory implements JsonAdapter.Factory {

    public static JsonAdapter.Factory create() {
        return new AutoValueMoshi_AdapterFactory();
    }

}
