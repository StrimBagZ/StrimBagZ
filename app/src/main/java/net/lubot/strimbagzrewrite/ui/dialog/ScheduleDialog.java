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
package net.lubot.strimbagzrewrite.ui.dialog;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.data.model.GDQ.Run;
import net.lubot.strimbagzrewrite.data.model.Horaro.RunData;
import net.lubot.strimbagzrewrite.data.model.Horaro.ScheduleData;
import net.lubot.strimbagzrewrite.ui.adapter.GDQScheduleAdapter;
import net.lubot.strimbagzrewrite.ui.adapter.HoraroScheduleAdapter;
import net.lubot.strimbagzrewrite.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ScheduleDialog extends DialogFragment {
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private HoraroScheduleAdapter horaroAdapter;
    private GDQScheduleAdapter gdqAdapter;
    private Button closeButton;

    private String service = "horaro";

    private TextView marathon;
    private TextView date;

    public static ScheduleDialog newInstance(String eventName, String date, List<Run> runs) {
        ArrayList<Run> tmp = new ArrayList<>(runs);
        Bundle bundle = new Bundle();
        bundle.putString("eventName", eventName);
        bundle.putString("date", date);
        bundle.putParcelableArrayList("runs", tmp);
        ScheduleDialog fragment = new ScheduleDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static ScheduleDialog newInstance(String eventName, String date, ScheduleData scheduleData) {
        Bundle bundle = new Bundle();
        bundle.putString("eventName", eventName);
        bundle.putString("date", date);
        bundle.putParcelable("schedule", scheduleData);
        ScheduleDialog fragment = new ScheduleDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        return inflater.inflate(R.layout.list_schedule_dialog, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        marathon = (TextView) view.findViewById(R.id.schedule_marathon);
        date = (TextView) view.findViewById(R.id.schedule_date);
        closeButton = (Button) view.findViewById(R.id.schedule_btn_close);
        recyclerView = (RecyclerView) view.findViewById(R.id.listView);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        if (service.equals("gdq")) {
            gdqAdapter = new GDQScheduleAdapter(this);
            recyclerView.setAdapter(gdqAdapter);
        } else if (service.equals("horaro")) {
            horaroAdapter = new HoraroScheduleAdapter(this);
            recyclerView.setAdapter(horaroAdapter);
        }
        Bundle tmp = getArguments();
        if (tmp != null) {
            marathon.setText(tmp.getString("eventName"));
            date.setText(tmp.getString("date"));
            if (service.equals("gdq")) {
                ArrayList<Run> runs = tmp.getParcelableArrayList("runs");
                gdqAdapter.addAll(runs);
                scrollToCurrentTime(runs);
            } else if (service.equals("horaro")) {
                ScheduleData schedule = tmp.getParcelable("schedule");
                List<RunData> runs = schedule.data().runs();
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                start.setTimeInMillis(schedule.data().start() * 1000);
                RunData last = runs.get(runs.size() - 1);
                end.setTimeInMillis((last.scheduled() + last.length() + schedule.data().setup()) * 1000);
                DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, Resources.getSystem().getConfiguration().locale);
                date.setText(format.format(start.getTime()) + " - " + format.format(end.getTime()));
                horaroAdapter.addAll(schedule.data().columns(), runs);
                scrollToCurrentTime(schedule.data().setup(), runs);
            }

        }
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    private void scrollToCurrentTime(List<Run> runs) {
        if (gdqAdapter != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Resources.getSystem().getConfiguration().locale);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0, runsSize = runs.size(); i < runsSize; i++) {
                Run run = runs.get(i);
                try {
                    Date date = dateFormat.parse(run.endTime());
                    Calendar endTime = Calendar.getInstance();
                    endTime.setTimeInMillis(date.getTime());
                    Map<TimeUnit, Long> timeDiff =
                            Utils.computeTimeDiff(currentTime, endTime);
                    long diffDay = timeDiff.get(TimeUnit.DAYS);
                    long diffHr = timeDiff.get(TimeUnit.HOURS);
                    long diffMin = timeDiff.get(TimeUnit.MINUTES);
                    // if diffMin is lower than 0, the Run has ended
                    // else it's still going
                    Log.d("Run diff", " diffDay: " + diffDay  + " diffHr: " + diffHr + " diffMin: " + diffMin);
                    if (diffMin >= 0 && diffHr >= 0 && diffDay >= 0) {
                        Log.d("Run diff", "found current run");
                        if (i != 0) {
                            gdqAdapter.markCurrentRun(i);
                            layoutManager.scrollToPositionWithOffset(i, 0);
                        }
                        break;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void scrollToCurrentTime(long setup, List<RunData> runs) {
        if (horaroAdapter != null) {
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0, runsSize = runs.size(); i < runsSize; i++) {
                RunData run = runs.get(i);
                Calendar endTime = Calendar.getInstance();
                endTime.setTimeInMillis((run.scheduled() + run.length() + setup) * 1000);
                Map<TimeUnit, Long> timeDiff =
                        Utils.computeTimeDiff(currentTime, endTime);
                long diffDay = timeDiff.get(TimeUnit.DAYS);
                long diffHr = timeDiff.get(TimeUnit.HOURS);
                long diffMin = timeDiff.get(TimeUnit.MINUTES);
                // if diffMin is lower than 0, the Run has ended
                // else it's still going
                Log.d("Run diff", " diffDay: " + diffDay  + " diffHr: " + diffHr + " diffMin: " + diffMin);
                if (diffMin >= 0 && diffHr >= 0 && diffDay >= 0) {
                    Log.d("Run diff", "found current run");
                    if (i != 0) {
                        horaroAdapter.markCurrentRun(i);
                        layoutManager.scrollToPositionWithOffset(i, 0);
                    }
                    break;
                }
            }
        }
    }

}
