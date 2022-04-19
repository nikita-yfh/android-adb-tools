package ru.nikita.adb;

import java.util.HashMap;
import java.util.Iterator;
import android.app.Activity;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbConstants;
import android.util.Log;

class FastbootDevice {
	public FastbootDevice(UsbDevice device) {
		this.device = device;
		Log.v("fastboot", "Device: "+device.toString());
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

	public boolean isOk(){
		return in != null && out != null;
	}
	public String getName() {
		return String.format("%04X:%04X", device.getVendorId(), device.getProductId());
	}

	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint in;
	private UsbEndpoint out;
}
