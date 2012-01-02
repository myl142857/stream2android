package org.hansdeveloper.Stream2Android;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.hansdeveloper.Stream2Android.main.*;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainListAdapter extends BaseAdapter {
	LayoutInflater mInflater = null;
    Map<String, MediaItem> mapMediaItems = null;

	public MainListAdapter(LayoutInflater inflater, Map<String, MediaItem> MediaItems)
	{
		mInflater = inflater;
		mapMediaItems = MediaItems;
	}
	@Override
	public int getCount() {
		return mapMediaItems.size();
	}

	@Override
	public Object getItem(int position) {
		Map.Entry<String, MediaItem> 
		me = (Map.Entry<String, MediaItem>)mapMediaItems.entrySet().toArray()[position];
		return me.getValue().getUrl();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		System.out.println("[Stream2Android] MainListAdapter.getView called for position " + position);
		if (convertView == null)
		{
			System.out.println("[Stream2Android] MainListAdapter.getView create new mediaserver_listitem");
			convertView = mInflater.inflate(R.layout.main_listitem, null);
		}

		@SuppressWarnings("unchecked")
		Map.Entry<String, MediaItem> 
		me = (Map.Entry<String, MediaItem>)mapMediaItems.entrySet().toArray()[position];

		TextView w = (TextView)convertView.findViewById(R.id.listtitle);
		w.setText(me.getValue().getTitle() + " (" + me.getValue().getResList().getFirst().get("duration") + ")");

		w = (TextView)convertView.findViewById(R.id.listdescription);
		w.setText(me.getValue().getResList().getFirst().get("res"));

		ImageView img = (ImageView)convertView.findViewById(R.id.imageview);
		if (me.getValue().getImage() != null)
		{
			ByteArrayInputStream is = new ByteArrayInputStream(me.getValue().getImage());
			
			Drawable drawable = Drawable.createFromStream(is, me.getValue().getTitle());
			img.setImageDrawable(drawable);
		}
		else 
			img.setImageResource(R.drawable.icon);

		return convertView;
	}
}
