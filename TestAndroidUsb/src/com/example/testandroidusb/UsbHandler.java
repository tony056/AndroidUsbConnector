package com.example.testandroidusb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.R.bool;
import android.R.integer;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class UsbHandler extends Handler {
	public UsbManager usbManager;
	public Context mContext;
	
	private MainActivity activity;
	private SerialInputOutputManager mSerialInputOutputManager;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private SerialInputOutputManager.Listener mListener;
	private SerialWrite serialWrite;
	private int counter = 0;
	private int len = 0;
	private int prevlen = 0;

	public static UsbSerialPort mUsbSerialPort = null;
	public boolean isWriting = false;
	private Thread writeThread;
	private final AtomicBoolean running = new AtomicBoolean(true);

	private static final int MESSAGE_REFRESH = 101;
	private static final long REFRESH_TIMEOUT_MILLIS = 5000;
	private static final int CONNECT_USB = 102;
	private static final int START_WRITE = 103;
	private static final int STOP_WRITE = 104;
	private static final int UPDATE_WRITE = 105;
	private static final int BAUD_RATE = 115200;
	private static final String TAG = UsbHandler.class.getSimpleName();

	public UsbHandler(UsbManager manager, Context context, MainActivity activity) {
		usbManager = manager;
		mContext = context;
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_REFRESH:
			refreshDevicelist();
			this.sendEmptyMessageDelayed(MESSAGE_REFRESH,
					REFRESH_TIMEOUT_MILLIS);
			break;
		case CONNECT_USB:
			startConnection();
			break;
		case START_WRITE:
			writeToUsb();
			break;
		case STOP_WRITE:
			stopWriteToUsb();
			break;
		case UPDATE_WRITE:
			updateWriteToUsb();
			break;
		default:
			super.handleMessage(msg);
			break;
		}
	}

	

	private void startConnection() {
		if (mUsbSerialPort != null && usbManager != null) {
			UsbDeviceConnection connection = this.usbManager
					.openDevice(mUsbSerialPort.getDriver().getDevice());
			if (connection == null) {
				Log.d("CON ERROR", "connection error");
				return;
			}
			try {
				mUsbSerialPort.open(connection);
				mUsbSerialPort.setParameters(BAUD_RATE,
						UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1,
						UsbSerialPort.PARITY_NONE);
				Toast.makeText(mContext, "start connection", Toast.LENGTH_SHORT).show();
				startIOManager();
				
			} catch (Exception e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				try {
					mUsbSerialPort.close();
				} catch (Exception e2) {
					//Ignore.
				}
				mUsbSerialPort = null;
				return;
			}
		}
	}

	private void startIOManager() {
		if(mUsbSerialPort != null){
			Log.d(TAG, "start io manager");
			
			mListener = new SerialInputOutputManager.Listener() {
				
				@Override
				public void onRunError(Exception arg0) {
					Log.e(TAG, "Listener error");
				}
				
				@Override
				public void onNewData(final byte[] data) {
					if(activity != null){
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								activity.updateReceivedData(data);
							}
						});
						
					}
					
				}
			};
			
			mSerialInputOutputManager = new SerialInputOutputManager(mUsbSerialPort, mListener);
			mExecutor.submit(mSerialInputOutputManager);
		}
	}

	private void refreshDevicelist() {
		new AsyncTask<Void, Void, List<UsbSerialPort>>() {
			@Override
			protected List<UsbSerialPort> doInBackground(Void... params) {
				SystemClock.sleep(1000);

				final List<UsbSerialDriver> drivers = UsbSerialProber
						.getDefaultProber().findAllDrivers(usbManager);

				final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
				for (final UsbSerialDriver driver : drivers) {
					final List<UsbSerialPort> ports = driver.getPorts();
					result.addAll(ports);
				}

				return result;
			}

			@Override
			protected void onPostExecute(List<UsbSerialPort> result) {
				if (result.size() > 0) {
					if (mUsbSerialPort == null) {
						Toast.makeText(
								mContext,
								""
										+ result.get(0).getDriver().getDevice()
												.toString(), Toast.LENGTH_SHORT)
								.show();
						mUsbSerialPort = result.get(0);
						sendEmptyMessage(CONNECT_USB);
					}
				} else {
					if (mUsbSerialPort != null) {
						mUsbSerialPort = null;
					}
				}
			}

		}.execute((Void) null);

	}
	
	public void writeToUsb(){
		serialWrite = new SerialWrite(mUsbSerialPort);
//		this.post(serialWrite);
		serialWrite.isWriting = true;
		writeThread = new Thread(serialWrite);
		writeThread.start();
	}
	
	private void stopWriteToUsb(){
		serialWrite.isWriting = false;
		writeThread.interrupt();
	}
	
	private void updateWriteToUsb() {
		counter++;
		String data = Integer.toString(counter % 2);
		serialWrite.updateSendingData(data);
	}

	public void writeToUsb(String stateString) {
		if(writeThread != null){
			serialWrite.updateSendingData(stateString);
		}
	}

}