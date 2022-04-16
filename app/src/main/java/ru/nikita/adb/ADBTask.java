package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import java.io.File;
import java.lang.String;
import java.util.Calendar;
import java.text.SimpleDateFormat;
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
	public void backup(final Device device, boolean apk, boolean userApps,
			boolean systemApps, boolean data, boolean cache) {
		new File("/sdcard/ADB").mkdir();
		String args = new String();
		if(apk)
			args += "-apk ";
		if(data)
			args += "-shared ";
		if(cache)
			args += "-obb ";
		if(userApps)
			args += "-all ";
		if(!systemApps)
			args += "-nosystem ";
		String fileName = "/sdcard/ADB/backup_"+new SimpleDateFormat("ddMMyy_hhmmss").format(Calendar.getInstance().getTime())+".ab";

		execute(device,"backup -f '"+fileName+"' "+args);
	}
	public void restore(final Device device, String fileName){
		execute(device,"restore '"+fileName+"'");
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

