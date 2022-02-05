package ru.nikita.adb;

import java.util.ArrayList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.content.Context;
import android.content.Intent;
import ru.nikita.adb.Binary;
import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.AppListActivity;
import ru.nikita.adb.FileManagerActivity;
import ru.nikita.adb.AppManagerActivity;
import ru.nikita.adb.FastbootActivity;
import ru.nikita.adb.DeviceListAdapter;

public abstract class DevicesActivity extends Activity{
	protected void init(Binary binary){
		getDeviceList().setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent,
					View view, int position, long id){
				device = (Device)parent.getSelectedItem();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		this.binary = binary;
		text = (TextView)findViewById(R.id.log);
		device = null;

		clearDeviceList();

		refreshDeviceList(null);
	}

	protected Spinner getDeviceList(){
		return (Spinner)findViewById(R.id.device);
	}
	protected void setDeviceList(DeviceListAdapter adapter){
		Spinner spinner = getDeviceList();
		spinner.setAdapter(adapter);
	}
	protected void clearDeviceList(){
		setDeviceList(null);
		disableEnableControls(false);
	}
	public void refreshDeviceList(View view){
		new DeviceListTask(text,binary).execute();
	}
	private void disableEnableControls(boolean enable, ViewGroup vg){
		for (int i = 0; i < vg.getChildCount(); i++){
			View child = vg.getChildAt(i);
			child.setEnabled(enable);
			if (child instanceof ViewGroup)
				disableEnableControls(enable, (ViewGroup)child);
		}
	}
	private void disableEnableControls(boolean enable){
		ViewGroup layout = (ViewGroup)findViewById(R.id.controls);
		disableEnableControls(enable, layout);
	}
	protected abstract Device[] getDevices(String log);

	private class DeviceListTask extends Task{
		public DeviceListTask(TextView text, Binary binary){
			super(text, binary);
		}
		@Override
		protected void onPostExecute(String log){
			clearDeviceList();
			Device[] devices = getDevices(log);

			disableEnableControls(devices.length > 0);

			DeviceListAdapter adapter = new DeviceListAdapter(DevicesActivity.this, devices);
			setDeviceList(adapter);

			if(device != null)
				for(int i = 0; i < devices.length; i++)
					if(devices[i].id.equals(device.id))
						getDeviceList().setSelection(i);
		}

		public void execute(){
			execute("devices");
		}
	}
	protected Device device;

	protected TextView text;
	protected Binary binary;
}
