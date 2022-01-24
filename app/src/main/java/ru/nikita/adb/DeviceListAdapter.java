package ru.nikita.adb;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import java.util.List;
import ru.nikita.adb.Device;

class DeviceListAdapter extends BaseAdapter {
	DeviceListAdapter(Context context, Device[] devices){
		inflter = (LayoutInflater.from(context));
		this.devices = devices;
	}
	@Override
	public int getCount() {
		return devices.length;
	}
	@Override
	public Object getItem(int i) {
		return devices[i];
	}
	@Override
	public long getItemId(int i) {
		return 0;
	}
	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		view = inflter.inflate(R.layout.device_list_item, null);
		TextView name = (TextView) view.findViewById(R.id.device_name);
		TextView state = (TextView) view.findViewById(R.id.device_state);
		name.setText(devices[i].id);
		state.setText(devices[i].state);
		return view;
	}
	private Device[] devices;
	private LayoutInflater inflter;
}
