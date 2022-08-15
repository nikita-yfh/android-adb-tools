package ru.nikita.adb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
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

	public boolean openConnection(Context context) {
		if(connection == null)
			connection = manager.openDevice(device);
		if(connection != null)
			return true;
		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		manager.requestPermission(device, intent);
		return false;
	}

	private void rawCommand(String command) {
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

	private String readOkay() {
		byte[] bytes = new byte[1024];
		int length = connection.bulkTransfer(in, bytes, bytes.length, 0);
		String response = new String(bytes, 0, length);
		if(!response.startsWith("OKAY"))
			throw new FastbootException(response);
		return response.substring(4);
	}

	public void checkOkay() {
		String response = readOnce();
		if(!response.equals("OKAY"))
			throw new FastbootException(response);
	}

	public void erase(String partition) {
		rawCommand("erase:" + partition);
		readOnce();
	}

	public void reboot(String target) {
		rawCommand("reboot-" + target);
		readOnce();
	}

	public void flashingUnlock() {
		rawCommand("flashing unlock");
		while(readOnce().startsWith("INFO"));
	}

	public void flashingLock() {
		rawCommand("flashing lock");
		while(readOnce().startsWith("INFO"));
	}

	public String getVariable(String name) {
		rawCommand("getvar:" + name);
		return readOkay();
	}

	public long getSparseLimit() {
		String str = getVariable("max-download-size");
		return Long.decode(str);
	}

	public boolean hasVbmetaPartiton() {
		return !getVariable("partition-type:vbmeta").isEmpty() ||
			   !getVariable("partition-type:vbmeta_a").isEmpty() ||
			   !getVariable("partition-type:vbmeta_b").isEmpty();
	}

	public boolean isLogical(String partition) {
		return getVariable("is-logical:" + partition).equals("yes");
	}

	public FastbootVariable[] getAllVariables() {
		rawCommand("getvar:all");
		ArrayList<FastbootVariable> list = new ArrayList<FastbootVariable>();
		String response;
		do {
			response = readOnce();
			if(response.startsWith("INFO"))
				list.add(new FastbootVariable(response));
		} while(response.startsWith("INFO"));
		return list.toArray(new FastbootVariable[0]);
	}

	public void downloadCommand(int length) {
		rawCommand(String.format("download:%08x", length));
		String response = readOnce();
		if(!response.startsWith("DATA") || Integer.parseInt(response.substring(4), 16) != length)
			throw new FastbootException("Invalid response");
	}

	public int writeData(byte[] data, int offset) {
		int size = Math.min(data.length - offset, out.getMaxPacketSize());
		int bytesWritten = connection.bulkTransfer(out, Arrays.copyOfRange(data, offset, offset + size), size, 0);
		offset += bytesWritten;
		return offset;
	}

	public void flash(String partition) {
		rawCommand("flash:" + partition);
		readOnce();
	}

	private UsbManager manager;
	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint in;
	private UsbEndpoint out;
	private UsbDeviceConnection connection;

	private static final String ACTION_USB_PERMISSION = "ru.nikita.adb.USB_PERMISSION";
}
