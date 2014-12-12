package com.example.testandroidusb;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.hoho.android.usbserial.driver.UsbSerialPort;

public class SerialWriteRunnable implements Runnable{

	private UsbSerialPort mUsbSerialPort = null;
	private static final int TIMEOUT_MILLIS = 1;
	
	public String data  = "m";
	public AtomicBoolean isWriting = new AtomicBoolean(false);
	
	public SerialWriteRunnable(UsbSerialPort port){
		mUsbSerialPort = port;
//		mUsbHandler = handler;
	}
	
	@Override
	public void run() {
		while(isWriting.get()){
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

	public synchronized void updateSendingData(String updateData){
		data = updateData;
	}
	
	public void setWritingState(boolean b){
		isWriting.set(b);
	}
}
