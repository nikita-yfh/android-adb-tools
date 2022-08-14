package ru.nikita.adb;

import java.util.List;
import java.util.ArrayList;
import java.lang.Exception;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import android.os.Bundle;
import android.content.Context;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import ru.nikita.adb.FastbootVariablesListActivity;
import ru.nikita.adb.PartitionListActivity;
import ru.nikita.adb.FastbootDevice;
import ru.nikita.adb.FastbootVariable;
import ru.nikita.adb.FastbootException;
import ru.nikita.adb.FileManagerActivity;

import ru.nikita.adb.SparseFile;

public class FastbootActivity extends Activity {
	private static int SELECT_IMAGE = 1;
	private UsbManager usbManager;
	private List<FastbootDevice>devices;
	private FastbootDevice getSelectedDevice() {
		Spinner list = (Spinner)findViewById(R.id.device);
		return devices.get(list.getSelectedItemPosition());
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_fastboot);
		super.onCreate(savedInstanceState);

		usbManager = (UsbManager) getSystemService(USB_SERVICE);
		devices = new ArrayList<FastbootDevice>();

		BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateDeviceList();
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		TextWatcher validator = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
			@Override
			public void afterTextChanged(Editable editable) {
				findViewById(R.id.flash).setEnabled(editable.length() != 0);
			}
		};

		((EditText) findViewById(R.id.image_path)).addTextChangedListener(validator);
		updateDeviceList();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null) {
			String filePath = data.getData().getPath();
			if(requestCode == SELECT_IMAGE)
				((EditText) findViewById(R.id.image_path)).setText(filePath);
		}
	}

	private void showToast(Exception e) {
		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
	}

	private void disableEnableControls(ViewGroup vg, boolean enable) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View child = vg.getChildAt(i);
			child.setEnabled(enable);
			if (child instanceof ViewGroup)
				disableEnableControls((ViewGroup)child, enable);
		}
	}

	private void updateDeviceList() {
		devices.clear();
		for(UsbDevice usbDevice : usbManager.getDeviceList().values()) {
			FastbootDevice device = new FastbootDevice(usbManager, usbDevice);
			if(device.isOk())
				devices.add(device);
		}
		String[] deviceNames = new String[devices.size()];
		for(int i = 0; i < devices.size(); i++)
			deviceNames[i] = devices.get(i).getName();

		Spinner list = (Spinner)findViewById(R.id.device);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, deviceNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		String selected = null;
		if(list.getAdapter() != null && list.getSelectedItem() != null)
			selected = list.getSelectedItem().toString();
		list.setAdapter(adapter);
		if(selected != null)
			for(int i = 0; i < deviceNames.length; i++)
				if(deviceNames[i].equals(selected))
					list.setSelection(i);
		disableEnableControls((ViewGroup) findViewById(R.id.controls), devices.size() != 0);
		if(((EditText) findViewById(R.id.image_path)).getText().toString().isEmpty())
			findViewById(R.id.flash).setEnabled(false);
	}

	private String getPartition() {
		return ((Spinner) findViewById(R.id.partition)).getSelectedItem().toString();
	}

	public void flashPartition(View view) {
		String path = ((EditText) findViewById(R.id.image_path)).getText().toString();
		String partition = getPartition();
		boolean raw = ((CheckBox) findViewById(R.id.raw)).isSelected();
		boolean disableVerity = ((CheckBox) findViewById(R.id.disable_verity)).isSelected();
		boolean disableVerification = ((CheckBox) findViewById(R.id.disable_verification)).isSelected();
	}

	private enum FileSystem {
		NO,
		EXT4,
		EXT3,
		EXT2,
		FAT,
		EXFAT,
		F2FS
	}

	public void browseImage(View view) {
		Intent intent = new Intent(this, FileManagerActivity.class);
		startActivityForResult(intent, SELECT_IMAGE);
	}

	public void formatPartition(View view) {
		FileSystem fs = FileSystem.values()[((Spinner) findViewById(R.id.fs)).getSelectedItemPosition()];
		String partition = getPartition();
		try {
			if(getSelectedDevice().openConnection(this))
				getSelectedDevice().erase(partition);
		} catch(Exception e) {
			showToast(e);
		}
	}

	public void listVariables(View view) {
		try {
			if(getSelectedDevice().openConnection(this)) {
				FastbootVariable[] vars = getSelectedDevice().getAllVariables();
				Intent intent = new Intent(this, FastbootVariablesListActivity.class);
				intent.putExtra("variables", vars);
				startActivity(intent);
			}
		} catch(Exception e) {
			showToast(e);
		}
	}

	public void listPartitions(View view) {
		try {
			if(getSelectedDevice().openConnection(this)) {
				FastbootVariable[] vars = getSelectedDevice().getAllVariables();
				Intent intent = new Intent(this, PartitionListActivity.class);
				intent.putExtra("variables", vars);
				startActivity(intent);
			}
		} catch(Exception e) {
			showToast(e);
		}
	}

	public void reboot(View view) {
		try {
			if(getSelectedDevice().openConnection(this)) {
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle(R.string.reboot);
				final String[] items = {
					"",
					"bootloader",
					"recovery",
				};
				b.setItems(R.array.fastboot_reboot, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						getSelectedDevice().reboot(items[which]);
					}
				});
				b.show();
			}
		} catch(Exception e) {
			showToast(e);
		}
	}

	public void flash(View view) {
		try {
			FastbootDevice device = getSelectedDevice();
			if(device.openConnection(this)) {
				String filePath = ((EditText) findViewById(R.id.image_path)).getText().toString();
				String partition = ((Spinner) findViewById(R.id.partition)).getSelectedItem().toString();
				boolean disableVerity = ((CheckBox) findViewById(R.id.disable_verity)).isChecked();
				boolean disableVerification = ((CheckBox) findViewById(R.id.disable_verification)).isChecked();
				boolean raw = ((CheckBox) findViewById(R.id.raw)).isChecked();

				File file = new File(filePath);
				if (!file.exists() || !file.isFile())
					throw new FileNotFoundException("File not found");
				long fileLength = file.length();
				long sparseLimit = device.getSparseLimit();

				byte[] data = new byte[(int) file.length()];
				FileInputStream stream = new FileInputStream(file);
				stream.read(data);

				device.download(data);
				device.flash(partition);
			}
		} catch(Exception e) {
			e.printStackTrace();
			showToast(e);
		}
	}
}

