package com.example.testandroidusb;

import java.util.ArrayList;
import java.util.List;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;


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
	
	private static final int MESSAGE_REFRESH = 101;
	private static final long REFRESH_TIMEOUT_MILLIS = 5000;
	private static final int CONNECT_USB = 102;
	private static final int STOP_WRITE = 104;
	private static final int STOP_READ = 106;
	private static final int BAUD_RATE = 115200;
	
	public UsbManager usbManager;
	public Context mContext;
	public static UsbSerialPort mUsbSerialPort = null;
	
	private MainActivity activity;
	private SerialReadRunnable.Listener mListener;
	private SerialWriteRunnable mSerialWriteRunnable;
	private SerialReadRunnable mSerialReadRunnable;
	private Thread mWriteThread;
	private Thread mReadThread;
	private static final String TAG = UsbHandler.class.getSimpleName();

	
	
	public UsbHandler(UsbManager manager, Context context, MainActivity activity) {
		usbManager = manager;
		mContext = context;
		this.activity = activity;
	}
	
	
	public UsbHandler(UsbManager manager, Context context, SerialReadRunnable.Listener listener){
		usbManager = manager;
		mContext = context;
		mListener = listener;
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
		case STOP_WRITE:
			stopWriteToUsb();
			break;
		case STOP_READ:
			stopReadFromUsb();
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
				Toast.makeText(mContext, "start connection", Toast.LENGTH_SHORT)
						.show();
				startIOManager();

			} catch (Exception e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				try {
					mUsbSerialPort.close();
				} catch (Exception e2) {
					// Ignore.
				}
				mUsbSerialPort = null;
				return;
			}
		}
	}

	private void startIOManager() {
		if (mUsbSerialPort != null) {
			Log.d(TAG, "start io manager");
			if(mListener == null){
				mListener = new SerialReadRunnable.Listener() {
					@Override
					public void OnReceivedMessage(final String data) {
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
				
			}
			readFromUsb();
			writeToUsb();
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

	private void writeToUsb() {
		mSerialWriteRunnable = new SerialWriteRunnable(mUsbSerialPort);
		mSerialWriteRunnable.setWritingState(true);
		mWriteThread = new Thread(mSerialWriteRunnable);
		mWriteThread.start();
	}
	
	private void readFromUsb() {
		mSerialReadRunnable = new SerialReadRunnable(mUsbSerialPort, mListener);
		mSerialReadRunnable.setState(true);
		mReadThread = new Thread(mSerialReadRunnable);
		mReadThread.start();
	}

	private void stopWriteToUsb() {
		mSerialWriteRunnable.setWritingState(false);
		mWriteThread.interrupt();
		mWriteThread = null;
	}
	
	private void stopReadFromUsb(){
		mSerialReadRunnable.setState(false);
		mReadThread.interrupt();
		mReadThread = null;
	}

	public void updateSendingData(String stateString) {
		if(mWriteThread != null){
			if(stateString.indexOf('\n') < 0){
				stateString += '\n';
			}
			mSerialWriteRunnable.updateSendingData(stateString);
		}
	}
	

}