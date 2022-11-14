package ru.nikita.adb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import android.hardware.usb.UsbRequest;
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
		UsbRequest request = new UsbRequest();
		request.initialize(connection, out);
		request.queue(ByteBuffer.allocate(command.length()).put(command.getBytes()), command.length());
		connection.requestWait();
		request.close();
	}

	private String readOnce() {
		int bufferMaxLength = in.getMaxPacketSize();
		ByteBuffer buffer = ByteBuffer.allocate(bufferMaxLength);
		UsbRequest request = new UsbRequest();
		request.initialize(connection, in);
		request.queue(buffer, bufferMaxLength);
		connection.requestWait();
		request.close();

		Log.v("ADB", String.format("Buffer position: %d, size: %d", buffer.position(), buffer.limit()));

		String string = new String(buffer.array(), 0, buffer.position());
		if(string.startsWith("FAIL"))
			throw new FastbootException(string);
		return string;
	}

	private String readOkay() {
		String response = readOnce();
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

	public int getSparseLimit() {
		String str = getVariable("max-download-size");
		return Integer.decode(str);
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

	public void downloadCommand(long length) {
		rawCommand(String.format("download:%08x", length));
		String response = readOnce();
		if(!response.startsWith("DATA") || Integer.parseInt(response.substring(4), 16) != length)
			throw new FastbootException("Invalid response");
	}

	public void flash(String partition) {
		rawCommand("flash:" + partition);
		readOnce();
	}

	public class OutputStream {
		OutputStream() {
			request = new UsbRequest();
			request.initialize(connection, out);
		}
		public void close() {
			request.close();
		}
		public int write(byte[] data) {
			if(!request.queue(ByteBuffer.allocate(data.length).put(data), data.length))
				throw new FastbootException("Writing error");
			if(connection.requestWait() == null)
				throw new FastbootException("Writing error");
			return data.length;
		}
		public int write(byte[] array, int offset) throws IOException {
			int size = Math.min((int) array.length - offset, out.getMaxPacketSize());
			byte[] data = new byte[size];
			return write(data);
		}
		public int write(RandomAccessFile file, int count, int offset) throws IOException {
			int size = Math.min((int) file.length() - offset, out.getMaxPacketSize());
			byte[] data = new byte[size];
			file.read(data, 0, size);
			return write(data);
		}
		private UsbRequest request;
	}

	private UsbManager manager;
	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint in;
	private UsbEndpoint out;
	private UsbDeviceConnection connection;

	private static final String ACTION_USB_PERMISSION = "ru.nikita.adb.USB_PERMISSION";
}
