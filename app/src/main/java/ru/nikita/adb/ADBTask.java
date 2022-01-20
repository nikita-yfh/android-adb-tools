package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.DeviceListAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.lang.String;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;

class ADBTask extends Task{
	ADBTask(TextView text, Binary binary){
		super(text,binary);
		this.deviceList=null;
		this.context=null;
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

	private List<Device> getDeviceList(){
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
		return devices;
	}
	@Override
	protected void onPostExecute(String string){
		if(deviceList != null){
			clearDeviceList();
			List<Device> devices = getDeviceList();
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

	private Context context;
	private Spinner deviceList;
}

