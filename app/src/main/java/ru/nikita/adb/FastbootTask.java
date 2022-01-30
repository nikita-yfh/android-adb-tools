package ru.nikita.adb;

import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import java.lang.String;
import android.widget.TextView;

class FastbootTask extends Task{
	FastbootTask(TextView text, Binary binary){
		super(text,binary);
	}
	FastbootTask(Binary binary){
		this(null,binary);
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
	public void oemUnlock(Device device, String code){
		execute(device, "oem unlock " + code);
	}
}

