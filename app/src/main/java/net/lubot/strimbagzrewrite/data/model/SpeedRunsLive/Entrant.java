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
package net.lubot.strimbagzrewrite.data.model.SpeedRunsLive;

import android.os.Parcel;
import android.os.Parcelable;

public class Entrant implements Parcelable {
    public final String displayName;
    public final long place;
    public final long time;
    public final String message;
    public final String statetext;
    public final String twitch;
    public final String trueskill;

    public Entrant(String displayName, long place, long time, String message, String statetext, String twitch, String trueskill) {
        this.displayName = displayName;
        this.place = place;
        this.time = time;
        this.message = message;
        this.statetext = statetext;
        this.twitch = twitch;
        this.trueskill = trueskill;
    }

    @Override
    public String toString() {
        return "Entrant{" +
                "displayName='" + displayName + '\'' +
                ", place=" + place +
                ", time=" + time +
                ", message='" + message + '\'' +
                ", statetext='" + statetext + '\'' +
                ", twitch='" + twitch + '\'' +
                ", trueskill='" + trueskill + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flag) {
        parcel.writeString(displayName);
        parcel.writeLong(place);
        parcel.writeLong(time);
        parcel.writeString(message);
        parcel.writeString(statetext);
        parcel.writeString(twitch);
        parcel.writeString(trueskill);
    }

    protected Entrant(Parcel in) {
        displayName = in.readString();
        place = in.readLong();
        time = in.readLong();
        message = in.readString();
        statetext = in.readString();
        twitch = in.readString();
        trueskill = in.readString();
    }

    public static final Creator<Entrant> CREATOR = new Creator<Entrant>() {
        @Override
        public Entrant createFromParcel(Parcel in) {
            return new Entrant(in);
        }

        @Override
        public Entrant[] newArray(int size) {
            return new Entrant[size];
        }
    };
}
