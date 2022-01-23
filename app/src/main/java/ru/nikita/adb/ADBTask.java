package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.DeviceListAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.lang.String;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;
import android.util.Log;

class ADBTask extends Task{
	ADBTask(TextView text, Binary binary){
		super(text,binary);
		this.deviceList=null;
		this.context=null;
	}
	ADBTask(Binary binary){
		this(null,binary);
	}
	private void clearDeviceList(){
		if(deviceList != null)
			deviceList.setAdapter(null);
	}
	public void execute(Device device, String string){
		if(device == null)
			execute(string);
		else
			execute(String.format("-s %s %s", device.id, string));
	}
	public String executeNow(Device device, String string){
		if(device == null)
			return executeNow(string);
		else
			return executeNow(String.format("-s %s %s", device.id, string));
	}

	private Device[] getDeviceList(String log){
		ArrayList<Device>devices=new ArrayList<Device>();
		String lines[] = log.split("\\n");
		Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)[\\s\\S]+device:(\\S+)[\\s\\S]+");
		Matcher matcher;
		for(String line : lines){
			if (line.matches(pattern.pattern())) {
				matcher = pattern.matcher(line);
				if (matcher.find())
					devices.add(new Device(matcher.group(1),matcher.group(3),matcher.group(2)));
			}
		}
		return devices.toArray(new Device[0]);
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
		execute("devices -l");
	}
	public void reboot(Device device, String arg){
		execute(device,"reboot "+arg);
	}
	public void installAppFromFile(final Device device, String fileName){
		execute(device,"install '"+fileName+"'");
	}
	public void connectDevice(String ip){
		execute("connect "+ip);
	}
	public void push(Device device, String src, String dest){
		execute(String.format("push '%s' '%s'",src,dest));
	}
	public void pull(Device device, String src, String dest){
		execute(String.format("pull '%s' '%s'",src,dest));
	}
	public void tcpip(Device device, String port){
		execute(device,"tcpip "+port);
	}
	public String shell(Device device, String args){
		return executeNow(device, String.format("shell \"%s\"", args));
	}

	private Context context;
	private Spinner deviceList;
}

