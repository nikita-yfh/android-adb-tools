package ru.nikita.adb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import ru.nikita.adb.FastbootVariablesListActivity;
import ru.nikita.adb.FastbootDevice;
import ru.nikita.adb.FastbootVariable;
import ru.nikita.adb.FastbootException;

public class FastbootActivity extends Activity{
	private UsbManager usbManager;
	private List<FastbootDevice>devices;
	private FastbootDevice getSelectedDevice(){
		Spinner list = (Spinner)findViewById(R.id.device);
		return devices.get(list.getSelectedItemPosition());
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_fastboot);
		super.onCreate(savedInstanceState);

		usbManager = (UsbManager) getSystemService(USB_SERVICE);
		devices = new ArrayList<FastbootDevice>();

		updateDeviceList();
	}
	private void updateDeviceList() {
		devices.clear();
		for(UsbDevice usbDevice : usbManager.getDeviceList().values()){
			FastbootDevice device = new FastbootDevice(usbManager, usbDevice);
			if(device.isOk())
				devices.add(device);
		}
		String[] deviceNames = new String[devices.size()];
		for(int i = 0; i < devices.size(); i++)
			deviceNames[i] = devices.get(i).getName();

		Spinner list = (Spinner)findViewById(R.id.device);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, deviceNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		String selected = null;
		if(list.getAdapter() != null && list.getSelectedItem() != null)
			selected = list.getSelectedItem().toString();
		list.setAdapter(adapter);
		if(selected != null)
			for(int i = 0; i < deviceNames.length; i++)
				if(deviceNames[i].equals(selected))
					list.setSelection(i);
	}
	public void updateDeviceList(View view){
		updateDeviceList();
	}
	public void deviceInfoAll(View view){
		try {
			if(getSelectedDevice().openConnection(this)) {
				FastbootVariable[] vars = getSelectedDevice().getAllVariables();
				Intent intent = new Intent(this, FastbootVariablesListActivity.class);
				intent.putExtra("variables", vars);
				startActivity(intent);
			}
		} catch(FastbootException e) {
			e.showToast(this);
		}
	}
	public void reboot(View view){
		try {
			if(getSelectedDevice().openConnection(this))
				getSelectedDevice().reboot();
		} catch(FastbootException e) {
			e.showToast(this);
		}
	}
}

