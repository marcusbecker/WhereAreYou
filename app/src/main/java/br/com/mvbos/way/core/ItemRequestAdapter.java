package br.com.mvbos.way.core;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import br.com.mvbos.way.R;

/**
 * Created by Marcus Becker on 16/08/2016.
 */
public class ItemRequestAdapter extends BaseAdapter {

    private final Activity activity;
    private final List<RequestData> lst;

    public ItemRequestAdapter(Activity activity, List<RequestData> lst) {
        this.activity = activity;
        this.lst = lst;
    }

    @Override
    public int getCount() {
        return lst.size();
    }

    @Override
    public Object getItem(int position) {
        return lst.get(position);
    }

    @Override
    public long getItemId(int position) {
        return lst.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.item_request, null);
        RequestData rd = lst.get(position);

        long to = rd.isForeign() ? rd.getFromNumber() : rd.getToNumber();

        TextView v = (TextView) view.findViewById(R.id.lblName);
        v.setText(rd.getToName());

        v = (TextView) view.findViewById(R.id.lblState);
        v.setText(to + " - " + rd.getState());

        v = (TextView) view.findViewById(R.id.lblLocation);
        v.setText(rd.isForeign() ? rd.getLastUpdate().toString() : rd.getFormatedLocation());

        return view;
    }

    public void clear() {
        lst.clear();
    }

    public void add(RequestData requestData) {
        lst.add(requestData);
    }

    public void addAll(List<RequestData> requestDatas) {
        lst.addAll(requestDatas);
    }


}
