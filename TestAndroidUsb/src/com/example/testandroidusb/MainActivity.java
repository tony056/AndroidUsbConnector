package com.example.testandroidusb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.R.bool;
import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public SerialReadRunnable.Listener mListener = new SerialReadRunnable.Listener() {
		
		@Override
		public void OnReceivedMessage(final String data) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTextView.setText(data);
				}
			});
			
		}
	};
	
	private UsbManager mUsbManager;
	private TextView mTextView;
	private UsbHandler mUsbHandler;
	private Button mButton;
	private Button mLeftUpButton;
	private Button mLeftDownButton;
	private Button mRightUpButton;
	private Button mRightDownButton;
	private String stateString = "P:";
	private String speedString = "";
	private int counter = 0;
	private static final int MESSAGE_REFRESH = 101;
	private static final int START_WRITE = 103;
	private static final int STOP_WRITE = 104;
	private static final int UPDATE_WRITE = 105;
	
		
	private Button.OnClickListener btnClickListener = new Button.OnClickListener(){
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.leftUp:
				setState("1");
				break;
			case R.id.leftDown:
				setState("2");
				break;
			case R.id.rightUp:
				setState("3");
				break;
			case R.id.rightDown:
				setState("4");
				break;
			case R.id.counter:
				counter++;
				setSpeed();
			default:
				break;
			}
			Toast.makeText(getApplicationContext(), speedString, Toast.LENGTH_SHORT).show();
			mUsbHandler.updateSendingData(speedString);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mButton = (Button) findViewById(R.id.counter);
		mButton.setOnClickListener(btnClickListener);
		mLeftUpButton = (Button) findViewById(R.id.leftUp);
		mLeftUpButton.setOnClickListener(btnClickListener);
		mLeftDownButton = (Button) findViewById(R.id.leftDown);
		mLeftDownButton.setOnClickListener(btnClickListener);
		
		mRightUpButton = (Button) findViewById(R.id.rightUp);
		mRightUpButton.setOnClickListener(btnClickListener);
		mRightDownButton = (Button) findViewById(R.id.rightDown);
		mRightDownButton.setOnClickListener(btnClickListener);
		
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mTextView = (TextView) findViewById(R.id.showConnectInfo);
		mUsbHandler = new UsbHandler(mUsbManager, getApplicationContext(), mListener);
	}

	protected void setState(String string) {
		if(!(stateString.contains(string))){
			stateString += string;
		}else{
			int index = stateString.indexOf(string);
			stateString = stateString.substring(0, index) + stateString.substring(index + 1, stateString.length());
		}
		stateString += ";";
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mUsbHandler.sendEmptyMessage(MESSAGE_REFRESH);
		
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        mUsbHandler.removeMessages(MESSAGE_REFRESH);
    }
	
	public void updateReceivedData(String data){
		mTextView.setText(data);
	}
	
	private void setSpeed(){
		String speed = "low";
		speedString = "";
		if(counter % 3 == 0)
			speed = "900";
		else if(counter % 3 == 1)
			speed = "1350";
		else
			speed = "1800";
		speedString += speed;
		mButton.setText("" + counter);
	}
	
}
