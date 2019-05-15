package com.example.alex.chessnoboardandroid;

import android.content.Context;
import android.provider.ContactsContract;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


class DisplayMoveItem{
    int moveNum = 1;
    String whitemove = "";
    String blackmove = "";
    String simpleString = null;
}

class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    final int ITEM_SIMPLE_STRING = 1;

    private LayoutInflater inflater;
    private List<DisplayMoveItem> phones = new ArrayList<>();

    public void setLst(List<DisplayMoveItem> lst){
        phones = lst;
    }



    DataAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        @LayoutRes int resource = R.layout.list_item_move;
        if(viewType == ITEM_SIMPLE_STRING)
            resource = R.layout.list_item_simplestring;
        View view = inflater.inflate(resource, parent, false);

        return new ViewHolder(view, viewType);
    }


    @Override
    public int getItemViewType(int position) {
        if(phones.get(position).simpleString != null)
            return ITEM_SIMPLE_STRING;
        return 0;
    }

    @Override
    public void onBindViewHolder(DataAdapter.ViewHolder holder, int position) {
        DisplayMoveItem phone = phones.get(position);

        if(holder.simpleString != null) {
            holder.simpleString.setText(phone.simpleString);
        }
        else {

            holder.tv1.setText(String.format("%s.", phone.moveNum));
            holder.tv2.setText(phone.whitemove);
            holder.tv3.setText(phone.blackmove);
        }
    }

    @Override
    public int getItemCount() {
        return phones.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv1;
        TextView tv2;
        TextView tv3;
        TextView simpleString;
        ViewHolder(View view, int viewType){
            super(view);
            if(viewType == ITEM_SIMPLE_STRING){
                simpleString = view.findViewById(R.id.textViewSimpleString);
            } else {
                tv1 = view.findViewById(R.id.textViewNumMove);
                tv2 = view.findViewById(R.id.tv2);
                tv3 = view.findViewById(R.id.tv3);
            }

        }
    }
}
