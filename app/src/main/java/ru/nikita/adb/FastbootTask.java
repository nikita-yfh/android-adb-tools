package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.DeviceListAdapter;
import java.util.ArrayList;
import java.lang.String;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;
import android.util.Log;

class FastbootTask extends Task{
	FastbootTask(TextView text, Binary binary){
		super(text,binary);
		this.deviceList=null;
		this.context=null;
	}
	FastbootTask(Binary binary){
		this(null,binary);
	}
	private void clearDeviceList(){
		if(deviceList != null)
			deviceList.setAdapter(null);
	}
	private Device[] getDeviceList(String log){
		String words[] = log.split(" |\\n");
		Device[] devices = new Device[words.length/2];

		for(int i = 0; i < devices.length; i++)
			devices[i] = new Device(words[i*2], words[i*2+1]);
		return devices;
	}
	@Override
	protected void onPostExecute(String log){
		if(deviceList != null){
			clearDeviceList();
			Device[] devices = getDeviceList(log);

			DeviceListAdapter adapter = new DeviceListAdapter(context, devices);
			deviceList.setAdapter(adapter);
		}
	}

	public void listDevices(Context context, Spinner deviceList){
		this.context=context;
		this.deviceList=deviceList;
		execute("devices");
	}
	public void reboot(Device device, String arg){
		execute(device, "reboot " + arg);
	}
	public void flash(Device device, String partition, String file){
		execute(device, String.format("flash %s '%s'", partition, file));
	}
	public void erase(Device device, String partition){
		execute(device, "erase " + partition);
	}
	public void boot(Device device, String file){
		execute(device, String.format("boot '%s'", file));
	}

	private Context context;
	private Spinner deviceList;
}

