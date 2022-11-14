package ru.nikita.adb;

import java.util.List;
import java.util.ArrayList;
import java.lang.Exception;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.os.Bundle;
import android.os.AsyncTask;
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
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.util.Log;
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

	private class FlashImageTask extends AsyncTask<Void, Integer, Exception> {
		FlashImageTask(FastbootDevice device, String filePath, String partition, boolean raw, boolean disableVerity, boolean disableVerification) {
			this.device = device;
			this.filePath = filePath;
			this.partition = partition;
			this.raw = raw;
			this.disableVerity = disableVerity;
			this.disableVerification = disableVerification;
		}
		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(FastbootActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage(getResources().getString(R.string.sending));
			pd.setCancelable(false);
			pd.show();
		}
		@Override
		protected void onProgressUpdate(Integer ... values) {
			pd.setProgress(values[0]);
			pd.setMax(values[1]);
			String message = getResources().getString(R.string.sending);
			if(values[0] == values[1])
				message = getResources().getString(R.string.writing);
			pd.setMessage(String.format("%s '%s' %d/%d", message, partition, values[2], values[3]));
		}
		@Override
		protected void onPostExecute(Exception result) {
			pd.dismiss();
			if(result != null) {
				showToast(result);
				result.printStackTrace();
			}
		}

		//private class DownloadStream extends FastbootDevice.OutputStream {
		//	DownloadStream(int length, int part, int maxParts) {
		//		this.part = part;
		//		this.maxParts = maxParts;
		//		this.bytes = 0;
		//	}

		//	@Override
		//	public synchronized void write(byte[] buffer, int offset, int count) {
		//		Log.d("ADB", Integer.toString(bytes));
		//		while(offset < count) {
		//			int bytesWritten = device.writeData(buffer, offset);
		//			offset += bytesWritten;
		//			bytes += bytesWritten;
		//			//publishProgress(bytes, 0x8000000, part, maxParts);
		//		}
		//	}

		//	@Override
		//	public synchronized void write(int b) {
		//		Log.d("ADB", Integer.toString(bytes));
		//		device.writeByte(b);
		//		bytes++;
		//		//publishProgress(bytes, 0x80000000, part, maxParts);
		//	}

		//	private int length;
		//	private int part;
		//	private int maxParts;
		//}

		private void downloadData(SparseFile file, int part, int maxParts) throws IOException {
			byte[] data = file.getBytes();
			device.downloadCommand(file.countLen());

			RandomAccessFile f = new RandomAccessFile(String.format("/sdcard/sparse%d.img", part), "rw");
			f.write(data);
			f.close();

			FastbootDevice.OutputStream stream = device.new OutputStream();
			for(int bytes = 0; bytes < data.length; bytes += stream.write(data, bytes));
			stream.close();

			device.checkOkay();
			publishProgress(1, 1, part, maxParts);
		}

		private void downloadData(RandomAccessFile file) throws IOException {
			device.downloadCommand(file.length());

			FastbootDevice.OutputStream stream = device.new OutputStream();
			for(int bytes = 0; bytes < file.length(); bytes += stream.write(file, (int) file.length(), bytes));
			stream.close();

			device.checkOkay();
			publishProgress(1, 1, 1, 1);
		}

		@Override
		protected Exception doInBackground(Void ... args) {
			try {
				RandomAccessFile file = new RandomAccessFile(filePath, "r");
				int fileLength = (int) file.length();
				int sparseLimit = device.getSparseLimit();

				if(fileLength <= sparseLimit) {
					downloadData(file);
					device.flash(partition);
				} else {
					SparseFile sparse = new SparseFile(file);
					SparseFile[] files = sparse.resparse(sparseLimit);
					device.erase(partition);
					for(int i = 0; i < files.length; i++) {
						Log.v("ADB", "Downloading");
						downloadData(files[i], i + 1, files.length);
						Log.v("ADB", "Flashing");
						device.flash(partition);
					}
				}
			} catch(Exception e) {
				return e;
			}
			return null;
		}
		private FastbootDevice device;
		private String filePath;
		private String partition;
		private boolean raw;
		private boolean disableVerity;
		private boolean disableVerification;
		private ProgressDialog pd;
	}

	public void flash(View view) {
		if(getSelectedDevice().openConnection(this)) {
			String filePath = ((EditText) findViewById(R.id.image_path)).getText().toString();
			String partition = ((Spinner) findViewById(R.id.partition)).getSelectedItem().toString();
			boolean disableVerity = ((CheckBox) findViewById(R.id.disable_verity)).isChecked();
			boolean disableVerification = ((CheckBox) findViewById(R.id.disable_verification)).isChecked();
			boolean raw = ((CheckBox) findViewById(R.id.raw)).isChecked();
			new FlashImageTask(getSelectedDevice(), filePath, partition, raw, disableVerity, disableVerification).execute();
		}
	}
}

