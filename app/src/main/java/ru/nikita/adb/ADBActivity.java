package ru.nikita.adb;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import ru.nikita.adb.Binary;
import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.AppListActivity;

public class ADBActivity extends Activity {
	private static final int APP_INSTALL_FILE=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		text = (TextView)findViewById(R.id.log);

		adb = new Binary(getApplicationContext(), "adb");
		updateDeviceList(null);
    }
	protected Spinner getDeviceList(){
		return (Spinner)findViewById(R.id.device);
	}
	protected Device getSelectedDevice(){
		return (Device)getDeviceList().getSelectedItem();
	}
	public void updateDeviceList(View view){
		new ADBTask(text,adb).listDevices(this,getDeviceList());
	}
	public void connectDevice(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Device IP address");

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String ip=input.getText().toString();
				new ADBTask(text,adb).connectDevice(ip);
				updateDeviceList(null);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}
	public void disconnectAll(View view){
		new ADBTask(text,adb).execute("disconnect");
		updateDeviceList(null);
	}
	public void startServer(View view){
		new ADBTask(text,adb).execute("start-server");
	}
	public void killServer(View view){
		new ADBTask(text,adb).execute("kill-server");
	}
	public void reconnect(View view){
		new ADBTask(text,adb).execute("reconnect");
		updateDeviceList(null);
	}

	public void reboot(View view){
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Reboot");
		final String[] types = {
			"system",
			"bootloader",
			"recovery",
			"sideload",
			"sideload-auto-reboot"
		};
		b.setItems(types, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				String system = (which==0)?"":types[which];
				new ADBTask(text,adb).reboot(getSelectedDevice(),system);
			}
		});
		b.show();
	}
	public void installAppFromFile(View view){
		Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		chooseFileIntent.setType("application/vnd.android.package-archive");
		chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

		chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");
		startActivityForResult(chooseFileIntent, APP_INSTALL_FILE);
	}
	public void installAppFromList(View view){
		Intent intent = new Intent(this, AppListActivity.class);
		startActivityForResult(intent, APP_INSTALL_FILE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			switch (requestCode) {
			case APP_INSTALL_FILE:
				if(data != null)  {
					String filePath = data.getData().getPath();
					filePath.replace("'","\\'");
					filePath.replace("\\","\\\\");
					new ADBTask(text,adb).installAppFromFile(getSelectedDevice(),filePath);
				}
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	public void tcpip(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Input port");

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		input.setHint("5555");
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String port=input.getText().toString();
				if(port.trim().length()==0)port="5555";
				new ADBTask(text,adb).tcpip(getSelectedDevice(),port);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}



	private TextView text;
	private Binary adb;

}
