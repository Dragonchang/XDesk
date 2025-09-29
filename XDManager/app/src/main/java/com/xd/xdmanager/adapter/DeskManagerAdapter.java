package com.xd.xdmanager.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xd.xdmanager.R;
import com.xd.xdmanager.config.Desk;

import java.util.List;

public class DeskManagerAdapter  extends RecyclerView.Adapter<DeskManagerAdapter.ViewHolder>{
    private Context mContext;
    private  List<Desk> mDesks;

    public DeskManagerAdapter(Context context, List<Desk> desks) {
        mContext = context;
        mDesks = desks;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seat_config,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.i("DeskManagerAdapter", "onBindViewHolder position: " + position);
        Desk bean = mDesks.get(position);
        String item = bean.getRow()+"-"+bean.getColumn();
        holder.tv.setText(item);
    }

    @Override
    public int getItemCount() {
        return mDesks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lay;
        TextView tv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            lay = itemView.findViewById(R.id.item_background);
            tv = itemView.findViewById(R.id.item_textview);
        }
    }

}
