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
package net.lubot.strimbagzrewrite.ui.adapter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import net.lubot.strimbagzrewrite.ui.fragment.HostedStreamsFragment;
import net.lubot.strimbagzrewrite.ui.fragment.LiveGamesFragment;
import net.lubot.strimbagzrewrite.ui.fragment.LiveStreamsFragment;
import net.lubot.strimbagzrewrite.util.CustomFragmentPagerAdapter;

public class FollowingFragmentPager extends CustomFragmentPagerAdapter {
    private int pageCount = 3;
    private String tabTitles[] = new String[] { "Live", "Hosting", "Games" };

    private LiveStreamsFragment liveFollowing;
    private HostedStreamsFragment hostedFollowing;
    private LiveGamesFragment liveGames;

    public FollowingFragmentPager(FragmentManager fm) {
        super(fm);
        tabTitles = new String[] { "Live", "Hosting", "Games" };
    }

    public FollowingFragmentPager(FragmentManager fm, int count) {
        super(fm);
        pageCount = count;
        tabTitles = new String[] { "Login required" };
    }

    @Override
    public int getCount() {
        return pageCount;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new LiveStreamsFragment();
            case 1:
                return new HostedStreamsFragment();
            case 2:
                return new LiveGamesFragment();
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        switch (position) {
            case 0:
                liveFollowing = (LiveStreamsFragment) createdFragment;
                break;
            case 1:
                hostedFollowing = (HostedStreamsFragment) createdFragment;
                break;
            case 2:
                liveGames = (LiveGamesFragment) createdFragment;
                break;
        }
        return createdFragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

}
