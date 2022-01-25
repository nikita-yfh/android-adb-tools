package ru.nikita.adb;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.EditText;
import android.content.Intent;
import android.content.DialogInterface;
import java.io.File;
import ru.nikita.adb.Binary;
import ru.nikita.adb.FastbootTask;

public class FastbootActivity extends Activity {
	private static final int FILE_BOOT=1;
	private static final int FILE_FLASH=2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fastboot_activity);

		text = (TextView)findViewById(R.id.log);

		fastboot = new Binary(getApplicationContext(), "fastboot");
		refreshDeviceList(null);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null){
			String filePath = data.getData().getPath();
			if(requestCode == FILE_BOOT){
				new FastbootTask(text,fastboot).boot(getSelectedDevice(),filePath);
			}else if(requestCode == FILE_FLASH){
				new FastbootTask(text,fastboot).flash(getSelectedDevice(),
						getSelectedPartition(), filePath);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	protected String getSelectedPartition(){
		Spinner spinner = (Spinner)findViewById(R.id.partition);
		return spinner.getSelectedItem().toString();
	}
	protected Spinner getDeviceList(){
		return (Spinner)findViewById(R.id.device);
	}
	protected Device getSelectedDevice(){
		return (Device)getDeviceList().getSelectedItem();
	}
	public void refreshDeviceList(View view){
		new FastbootTask(text,fastboot).listDevices(this,getDeviceList());
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
				new FastbootTask(text,fastboot).reboot(getSelectedDevice(), items[which]);
			}
		});
		b.show();
	}
	public void flash(View view){
		Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		chooseFileIntent.setType("application/x-ima");
		chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

		chooseFileIntent = Intent.createChooser(chooseFileIntent, getResources().getString(R.string.file_choose));
		startActivityForResult(chooseFileIntent, FILE_FLASH);
	}

	public void boot(View view){
		Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		chooseFileIntent.setType("application/x-ima");
		chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

		chooseFileIntent = Intent.createChooser(chooseFileIntent, getResources().getString(R.string.file_choose));
		startActivityForResult(chooseFileIntent, FILE_BOOT);
	}
	public void erase(View view){
		new FastbootTask(text,fastboot).erase(getSelectedDevice(),
				getSelectedPartition());
	}

	public void flashingUnlock(View view){
		new FastbootTask(text,fastboot).execute(getSelectedDevice(), "flashing unlock");
	}
	public void flashingLock(View view){
		new FastbootTask(text,fastboot).execute(getSelectedDevice(), "flashing lock");
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
				new FastbootTask(text,fastboot).oemUnlock(getSelectedDevice(), code);
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
		new FastbootTask(text,fastboot).execute(getSelectedDevice(), "oem lock");
	}
	public void oemDeviceInfo(View view){
		new FastbootTask(text,fastboot).execute(getSelectedDevice(), "oem device info");
	}


	private TextView text;
	private Binary fastboot;
}
