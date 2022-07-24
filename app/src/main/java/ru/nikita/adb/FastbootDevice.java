package ru.nikita.adb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;
import ru.nikita.adb.FastbootVariable;
import ru.nikita.adb.FastbootException;

class FastbootDevice {
	public FastbootDevice(UsbManager manager, UsbDevice device) {
		this.device = device;
		this.manager = manager;
		for (int i = 0; i < device.getInterfaceCount(); i++) {
			iface = device.getInterface(i);
			if(iface.getInterfaceProtocol() != 3)
				continue;
			for (int j = 0; j < iface.getEndpointCount(); j++) {
				UsbEndpoint endpoint = iface.getEndpoint(j);
				if((endpoint.getAttributes() & 0x03) != 0x02)
					continue;

				if((endpoint.getAddress() & 0x80) != 0)
					in = endpoint;
				else
					out = endpoint;
			}
			if(isOk())
				return;
		}
	}

	public boolean isOk() {
		return in != null && out != null;
	}
	public String getName() {
		return String.format("%04X:%04X", device.getVendorId(), device.getProductId());
	}

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
						if(device != null)
							connection = manager.openDevice(device);
				}
			}
		}
	};

	public boolean openConnection(Context context) {
		if(connection != null)
			return true;
		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(usbReceiver, filter);
		manager.requestPermission(device, intent);
		return false;
	}

	private void write(String command) {
		byte[] bytes = command.getBytes();
		connection.bulkTransfer(out, bytes, bytes.length, 0);
	}

	private String readOnce() {
		byte[] bytes = new byte[1024];
		int length = connection.bulkTransfer(in, bytes, bytes.length, 0);
		String response = new String(bytes, 0, length);
		if(response.startsWith("FAIL"))
			throw new FastbootException(response);
		return response;
	}

	public void reboot(String target) {
		write("reboot-" + target);
		readOnce();
	}

	public FastbootVariable[] getAllVariables() {
		write("getvar:all");
		ArrayList<FastbootVariable> list = new ArrayList<FastbootVariable>();
		String response;
		do {
			response = readOnce();
			if(response.startsWith("INFO"))
				list.add(new FastbootVariable(response));
		} while(response.startsWith("INFO"));
		return list.toArray(new FastbootVariable[0]);
	}

	private UsbManager manager;
	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint in;
	private UsbEndpoint out;
	private UsbDeviceConnection connection;

	private static final String ACTION_USB_PERMISSION = "ru.nikita.adb.USB_PERMISSION";
}
