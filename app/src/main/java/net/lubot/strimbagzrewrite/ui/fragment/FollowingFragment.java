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
package net.lubot.strimbagzrewrite.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.Twitch.Channel;
import net.lubot.strimbagzrewrite.data.model.Twitch.LiveStreams;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.FollowingFragmentPager;
import net.lubot.strimbagzrewrite.util.CustomFragmentPagerAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

public class FollowingFragment extends Fragment {

    private TabLayout tabLayout;
    private FollowingFragmentPager pagerAdapter;
    private ViewPager pager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = null;
        if (getActivity() instanceof MainActivity) {
            token = ((MainActivity) getActivity()).getToken();
        }
        if (token != null && !token.isEmpty() && !token.equals(Constants.NO_TOKEN)) {
            pagerAdapter = new FollowingFragmentPager(getChildFragmentManager());
        } else {
            pagerAdapter = new FollowingFragmentPager(getChildFragmentManager(), 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_following, container, false);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("onContextSelect", "group id: " + item.getGroupId());
        Log.d("onContextSelect", "item id: " + item.getItemId());
        int groupID = item.getGroupId();
        if (groupID == 0) {
            // If the item has no group ID we don't care about it
            return true;
        }
        Fragment fragment = pagerAdapter.getCurrentPrimaryItem();
        Channel channel  = null;
        String hostingChannel = null;
        // Since we use a ViewPager we need to make sure we get the data from the right Fragment
        boolean hostedChat = false;
        if (groupID == R.id.ctx_streams) {
            LiveStreamsFragment liveFragment = (LiveStreamsFragment) fragment;
            channel = liveFragment.getSelectedChannel();
        } else if (groupID == R.id.ctx_hosting) {
            HostedStreamsFragment hostedFragment = (HostedStreamsFragment) fragment;
            channel = hostedFragment.getSelectedChannel();
        } else if (groupID == 429691) {
            HostedStreamsFragment hostedFragment = (HostedStreamsFragment) fragment;
            channel = hostedFragment.getSelectedChannel();
            hostingChannel = hostedFragment.getClickedHostingItem();
            Log.d("Hosted Streams", "Hosted Chat items choosed");
            hostedChat = true;
        }

        if (channel == null) {
            Log.d("onContextSelect", "channel is null. This should never happen.");
            return true;
        }

        if (hostedChat) {
            Log.d("Hosted Channel", "Loading " + channel.displayName() + " with " + hostingChannel + " chat");
            if (getActivity() instanceof MainActivity) {
                Utils.startPlayerActivity(getContext(), channel, hostingChannel);
            }
            return true;
        }

        switch(item.getItemId()) {
            case R.id.ctn_openStream_Mobile:
                Utils.startPlayerActivity(getContext(), channel, "mobile");
                break;
            case R.id.ctn_openStream_Low:
                Utils.startPlayerActivity(getContext(), channel, "low");
                break;
            case R.id.ctn_openStream_Medium:
                Utils.startPlayerActivity(getContext(), channel, "medium");
                break;
            case R.id.ctn_openStream_High:
                Utils.startPlayerActivity(getContext(), channel, "high");
                break;
            case R.id.ctn_openStream_Source:
                Utils.startPlayerActivity(getContext(), channel, "source");
                break;
            case R.id.ctn_openChatOnly:
                Utils.startChatOnlyActivity(getContext(), channel);
                break;
            default:
                Utils.startPlayerActivity(getContext(), channel);
                break;
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTitle("Following");
        }
    }

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.tabLayout = (TabLayout) view.findViewById(R.id.sliding_tabs);
        this.pager = (ViewPager) view.findViewById(R.id.viewPager);
        this.pager.setOffscreenPageLimit(2);
        this.pager.setAdapter(pagerAdapter);
        this.tabLayout.setupWithViewPager(pager);

        if (savedInstanceState != null) {
            Log.d("onViewCreated", "trying to reset the current pager item");
            //Log.d("onViewCreated", "position: " + savedInstanceState.getInt("currentItem"));
            final int tmp = savedInstanceState.getInt("currentItem", 0);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                 pager.setCurrentItem(tmp, false);
                }
            });
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager != null) {
            outState.putInt("currentItem", pager.getCurrentItem());
        }
    }
}
