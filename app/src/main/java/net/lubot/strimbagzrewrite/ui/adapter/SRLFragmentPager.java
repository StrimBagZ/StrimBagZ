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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import net.lubot.strimbagzrewrite.ui.fragment.LiveStreamsFragment;
import net.lubot.strimbagzrewrite.ui.fragment.RacesFragment;

public class SRLFragmentPager extends FragmentPagerAdapter {
    private int pageCount = 2;
    private String tabTitles[] = new String[] { "Streams", "Races" };

    public SRLFragmentPager(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return pageCount;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                LiveStreamsFragment fragment = new LiveStreamsFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("srl", true);
                fragment.setArguments(bundle);
                return fragment;
            case 1:
                return new RacesFragment();
            default:
                return new LiveStreamsFragment();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

}
