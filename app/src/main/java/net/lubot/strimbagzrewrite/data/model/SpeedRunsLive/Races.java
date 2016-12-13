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
import android.support.annotation.Nullable;

import java.util.ArrayList;

public class Races implements Parcelable {
    public final ArrayList<Race> races;

    public Races(ArrayList<Race> races) {
        this.races = races;
    }

    public static class Race implements Parcelable {
        public final String id;
        public final RaceGame game;
        public final String goal;
        @Nullable
        public final ArrayList<Entrant> entrants;
        public final String statetext;

        public Race(String id, RaceGame game, String goal, ArrayList<Entrant> entrants, String statetext) {
            this.id = id;
            this.game = game;
            this.goal = goal;
            this.entrants = entrants;
            this.statetext = statetext;
        }

        @Override
        public String toString() {
            return "Race{" +
                    "id='" + id + '\'' +
                    ", game=" + game +
                    ", goal='" + goal + '\'' +
                    ", entrants=" + entrants +
                    ", statetext=" + statetext +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flag) {
            parcel.writeString(id);
            parcel.writeParcelable(game, flag);
            parcel.writeString(goal);
            parcel.writeTypedList(entrants);
            parcel.writeString(statetext);
        }

        protected Race(Parcel in) {
            id = in.readString();
            game = in.readParcelable(RaceGame.class.getClassLoader());
            goal = in.readString();
            entrants = in.createTypedArrayList(Entrant.CREATOR);
            statetext = in.readString();
        }

        public static final Creator<Race> CREATOR = new Creator<Race>() {
            @Override
            public Race createFromParcel(Parcel in) {
                return new Race(in);
            }

            @Override
            public Race[] newArray(int size) {
                return new Race[size];
            }
        };
    }

    public static class RaceGame implements Parcelable {
        public final String name;
        public final String abbrev;

        public RaceGame(String name, String abbrev) {
            this.name = name;
            this.abbrev = abbrev;
        }

        @Override
        public String toString() {
            return "RaceGame{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(name);
            parcel.writeString(abbrev);
        }

        protected RaceGame(Parcel in) {
            name = in.readString();
            abbrev = in.readString();
        }

        public static final Creator<RaceGame> CREATOR = new Creator<RaceGame>() {
            @Override
            public RaceGame createFromParcel(Parcel in) {
                return new RaceGame(in);
            }

            @Override
            public RaceGame[] newArray(int size) {
                return new RaceGame[size];
            }
        };
    }

    @Override
    public String toString() {
        return "Races{" +
                "races=" + races +
                '}';
    }

    protected Races(Parcel in) {
        races = in.createTypedArrayList(Race.CREATOR);
    }

    public static final Creator<Races> CREATOR = new Creator<Races>() {
        @Override
        public Races createFromParcel(Parcel in) {
            return new Races(in);
        }

        @Override
        public Races[] newArray(int size) {
            return new Races[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(races);
    }


}
