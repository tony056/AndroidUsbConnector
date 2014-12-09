package com.example.testandroidusb;

import java.io.IOException;

import android.R.integer;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class SerialWrite implements Runnable{

	private UsbSerialPort mUsbSerialPort = null;
	private UsbHandler mUsbHandler;
	private static final int TIMEOUT_MILLIS = 1;
	
	public String data  = "0";
	public boolean isWriting = false;
	
	public SerialWrite(UsbSerialPort port){
		mUsbSerialPort = port;
//		mUsbHandler = handler;
	}
	
	@Override
	public void run() {
		while(isWriting){
			writeToUsb();
		}
	}

	private void writeToUsb() {
		if(mUsbSerialPort != null){
			byte[] dataBytes = data.getBytes();
			try {
				mUsbSerialPort.write(dataBytes, TIMEOUT_MILLIS);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	public void updateSendingData(String updateData){
		data = updateData;
	}
}
