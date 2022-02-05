package ru.nikita.adb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;
import android.content.DialogInterface;
import android.content.Intent;
import ru.nikita.adb.Binary;
import ru.nikita.adb.Device;
import ru.nikita.adb.AppListActivity;
import ru.nikita.adb.ADBFileManagerActivity;
import ru.nikita.adb.FileManagerActivity;
import ru.nikita.adb.AppManagerActivity;
import ru.nikita.adb.FastbootActivity;
import ru.nikita.adb.DevicesActivity;

public class ADBActivity extends DevicesActivity {
	private static final int APP_INSTALL_FILE=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.adb_activity);
		super.init(new Binary(this,"adb"));
		super.onCreate(savedInstanceState);
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.adb_activity, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == R.id.fastboot){
			Intent intent = new Intent(this, FastbootActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	@Override
	protected Device[] getDevices(String log){
		ArrayList<Device> devices = new ArrayList<Device>();
		String lines[] = log.split("\\n");
		Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)");
		Matcher matcher;
		for(String line : lines){
			if (line.matches(pattern.pattern())) {
				matcher = pattern.matcher(line);
				if (matcher.find())
					devices.add(new Device(matcher.group(1),matcher.group(2)));
			}
		}
		return devices.toArray(new Device[0]);
	}

	public void connectDevice(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.connect_device);

		View layout = getLayoutInflater().inflate(R.layout.connect_device, null);
		builder.setView(layout);

		final EditText inputIP = (EditText)layout.findViewById(R.id.input_ip);
		final EditText inputPort = (EditText)layout.findViewById(R.id.input_port);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String ip=inputIP.getText().toString();
				String port=inputPort.getText().toString();
				new ADBTask(text,binary).connectDevice(ip, port);
				refreshDeviceList(null);
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
	public void disconnectAll(View view){
		new ADBTask(text,binary).execute("disconnect");
		refreshDeviceList(null);
	}
	public void startServer(View view){
		new ADBTask(text,binary).execute("start-server");
	}
	public void killServer(View view){
		new ADBTask(text,binary).execute("kill-server");
	}
	public void reconnect(View view){
		new ADBTask(text,binary).execute("reconnect");
		refreshDeviceList(null);
	}

	public void reboot(View view){
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.reboot);
		final String[] items = {
			"",
			"bootloader",
			"recovery",
			"sideload",
			"sideload-auto-reboot"
		};
		b.setItems(R.array.adb_reboot, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				new ADBTask(text,binary).reboot(device, items[which]);
			}
		});
		b.show();
	}
	public void installAppFromFile(View view){
		Intent intent = new Intent(this, FileManagerActivity.class);
		startActivityForResult(intent, APP_INSTALL_FILE);
	}
	public void installAppFromList(View view){
		Intent intent = new Intent(this, AppListActivity.class);
		startActivityForResult(intent, APP_INSTALL_FILE);
	}
	public void fileManager(View view){
		Intent intent = new Intent(this, ADBFileManagerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable("adb", binary);
		bundle.putSerializable("device", device);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	public void appManager(View view){
		Intent intent = new Intent(this, AppManagerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable("adb", binary);
		bundle.putSerializable("device", device);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null){
			if(requestCode == APP_INSTALL_FILE){
				String filePath = data.getData().getPath();
				new ADBTask(text,binary).installAppFromFile(device,filePath);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	public void tcpip(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.port);

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		input.setHint("5555");
		builder.setView(input);

		builder.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String port=input.getText().toString();
				if(port.trim().length()==0)port="5555";
				new ADBTask(text,binary).tcpip(device,port);
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
	public void executeCommand(View view){
		EditText command = (EditText)findViewById(R.id.command);
		new ADBTask(text,binary).execute(device, command.getText().toString());
	}
}
