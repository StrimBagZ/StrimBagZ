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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.lubot.strimbagzrewrite.R;

public class EmptyRecyclerViewAdapter extends
        RecyclerView.Adapter<EmptyRecyclerViewAdapter.ViewHolder> {

    private String message;
    private String btnText;
    private View.OnClickListener clickListener;

    public EmptyRecyclerViewAdapter(Context context, int textID, int btnID,
                                    View.OnClickListener listener) {
        message = context.getResources().getText(textID).toString();
        if (btnID != 0 && listener != null) {
            btnText = context.getResources().getText(btnID).toString();
            clickListener = listener;
        }
    }

    public EmptyRecyclerViewAdapter(Context context, int textID) {
        message = context.getResources().getText(textID).toString();
    }

    public EmptyRecyclerViewAdapter(String text) {
        message = text;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_empty, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.msg.setText(message);
        if (btnText != null && clickListener != null) {
            holder.btn.setText(btnText);
            holder.btn.setOnClickListener(clickListener);
        } else {
            holder.btn.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView msg;
        private Button btn;

        public ViewHolder(View view) {
            super(view);
            msg = (TextView) view.findViewById(R.id.text_empty);
            btn = (Button) view.findViewById(R.id.btn_empty);
        }
    }

}
