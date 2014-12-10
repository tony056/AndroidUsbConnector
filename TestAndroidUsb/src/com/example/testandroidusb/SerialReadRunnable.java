package com.example.testandroidusb;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hoho.android.usbserial.driver.UsbSerialPort;



public class SerialReadRunnable implements Runnable{
	private UsbSerialPort mUsbSerialPort = null;
	private final static int TIMEOUT_MILLIS = 1;
	private final static int BUFFER_SIZE = 4096;
	private ByteBuffer mBuffer= ByteBuffer.allocate(BUFFER_SIZE);
	private Listener mListener;
	
	public AtomicBoolean isReading = new AtomicBoolean(false);
	
	public interface Listener{
		public void OnReceivedMessage(String data);
	}
	
	
	public SerialReadRunnable(UsbSerialPort port){
		this(port, null);
	}
	
	public SerialReadRunnable(UsbSerialPort port, Listener listener){
		mUsbSerialPort = port;
		mListener = listener;
	}
	
	public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(isReading.get()){
			readFromUsb();
		}
	}
	
	private void readFromUsb(){
		if(mUsbSerialPort != null){
			try {
				int len = mUsbSerialPort.read(mBuffer.array(), TIMEOUT_MILLIS);
				if(len > 0){
					Listener listener = getListener();
					if(listener != null){
						final byte[] data = new byte[len];
						mBuffer.get(data, 0, len);
						String dataString = new String(data);
						mListener.OnReceivedMessage(dataString);
					}
					mBuffer.clear();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setState(boolean b) {
		isReading.set(b);
	}
}
