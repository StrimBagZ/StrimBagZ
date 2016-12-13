package net.lubot.strimbagzrewrite.util;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;

public abstract class CallbackZ<T> implements Callback<T> {
    private static final int TOTAL_RETRIES = 6;
    private static final String TAG = CallbackZ.class.getSimpleName();

    private final Call<T> call;
    private final boolean shouldRetry;
    private int retryCount = 0;

    public CallbackZ(Call<T> call) {
        this.call = call;
        this.shouldRetry = false;
    }

    public CallbackZ(Call<T> call, boolean shouldRetry) {
        this.call = call;
        this.shouldRetry = shouldRetry;
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        Log.e(TAG, t.getLocalizedMessage());
        if (shouldRetry && retryCount < TOTAL_RETRIES) {
            Log.v(TAG, "Retrying... (" + retryCount + " out of " + TOTAL_RETRIES + ")");
            retry();
            retryCount++;
        }
    }

    private void retry() {
        call.clone().enqueue(this);
    }
}
