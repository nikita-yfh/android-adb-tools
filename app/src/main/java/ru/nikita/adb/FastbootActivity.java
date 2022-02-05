package ru.nikita.adb;

import java.io.File;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Spinner;
import android.content.Intent;
import android.content.DialogInterface;
import ru.nikita.adb.Binary;
import ru.nikita.adb.FastbootTask;
import ru.nikita.adb.FileManagerActivity;
import ru.nikita.adb.DevicesActivity;

public class FastbootActivity extends DevicesActivity {
	private static final int FILE_BOOT=1;
	private static final int FILE_FLASH=2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.fastboot_activity);
		super.init(new Binary(this, "fastboot"));
		super.onCreate(savedInstanceState);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null){
			String filePath = data.getData().getPath();
			if(requestCode == FILE_BOOT){
				new FastbootTask(text,binary).boot(device,filePath);
			}else if(requestCode == FILE_FLASH){
				new FastbootTask(text,binary).flash(device,
						getSelectedPartition(), filePath);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	@Override
	protected Device[] getDevices(String log){
		String words[] = log.split(" |\\n");
		Device[] devices = new Device[words.length/2];

		for(int i = 0; i < devices.length; i++)
			devices[i] = new Device(words[i*2], words[i*2+1]);
		return devices;
	}
	protected String getSelectedPartition(){
		Spinner spinner = (Spinner)findViewById(R.id.partition);
		return spinner.getSelectedItem().toString();
	}
	public void reboot(View view){
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.reboot);
		final String[] items = {
			"",
			"bootloader",
			"recovery",
		};
		b.setItems(R.array.fastboot_reboot, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				new FastbootTask(text,binary).reboot(device, items[which]);
			}
		});
		b.show();
	}
	public void flash(View view){
		Intent intent = new Intent(this, FileManagerActivity.class);
		startActivityForResult(intent,FILE_FLASH);
	}

	public void boot(View view){
		Intent intent = new Intent(this, FileManagerActivity.class);
		startActivityForResult(intent, FILE_BOOT);
	}
	public void erase(View view){
		new FastbootTask(text,binary).erase(device,
				getSelectedPartition());
	}

	public void flashingUnlock(View view){
		new FastbootTask(text,binary).execute(device, "flashing unlock");
	}
	public void flashingLock(View view){
		new FastbootTask(text,binary).execute(device, "flashing lock");
	}
	public void oemUnlock(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.oem_code);

		final EditText input = new EditText(this);
		builder.setView(input);

		builder.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String code=input.getText().toString();
				new FastbootTask(text,binary).oemUnlock(device, code);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}
	public void oemLock(View view){
		new FastbootTask(text,binary).execute(device, "oem lock");
	}
	public void oemDeviceInfo(View view){
		new FastbootTask(text,binary).execute(device, "oem device-info");
	}
	public void deviceInfoAll(View view){
		new FastbootTask(text,binary).execute(device, "getvar all");
	}
	public void executeCommand(View view){
		EditText command = (EditText)findViewById(R.id.command);
		new FastbootTask(text,binary).execute(device, command.getText().toString());
	}
}
