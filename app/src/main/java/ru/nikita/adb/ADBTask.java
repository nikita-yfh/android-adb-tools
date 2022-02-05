package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import java.lang.String;
import android.widget.TextView;

class ADBTask extends Task{
	ADBTask(TextView text, Binary binary){
		super(text,binary);
	}
	ADBTask(Binary binary){
		this(null,binary);
	}
	public void reboot(Device device, String arg){
		execute(device,"reboot "+arg);
	}
	public void installAppFromFile(final Device device, String fileName){
		execute(device,"install '"+fileName+"'");
	}
	public void connectDevice(String ip, String port){
		execute(String.format("connect %s:%s", ip, port));
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
	public String shellNow(Device device, String args){
		return executeNow(device, String.format("shell \"%s\"", args));
	}
	public void shell(Device device, String args){
		execute(device, String.format("shell \"%s\"", args));
	}
}

