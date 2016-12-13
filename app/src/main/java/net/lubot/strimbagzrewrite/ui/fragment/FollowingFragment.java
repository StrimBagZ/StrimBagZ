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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.lubot.strimbagzrewrite.Constants;
import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;
import net.lubot.strimbagzrewrite.ui.adapter.FollowingFragmentPager;

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
