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
	
	
	private UsbManager mUsbManager;
	private TextView mTextView;
	private UsbHandler mUsbHandler;
	private CheckBox mCheckBox;
	private Button mButton;
	private Button mLeftUpButton;
	private Button mLeftDownButton;
	private Button mRightUpButton;
	private Button mRightDownButton;
	private String stateString = "";
//	private SerialInputOutputManager mSerialInputOutputManager;
	
	
//	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private static final int MESSAGE_REFRESH = 101;
	private static final int START_WRITE = 103;
	private static final int STOP_WRITE = 104;
	private static final int UPDATE_WRITE = 105;
	
	private CheckBox.OnCheckedChangeListener chkListener = new CheckBox.OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if(mCheckBox.isChecked()){
//				data = "1";
				mUsbHandler.sendEmptyMessage(START_WRITE);
			}else{
				mUsbHandler.sendEmptyMessage(STOP_WRITE);
			}
			
			
		}
	};
		
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
			default:
				break;
			}
			mUsbHandler.writeToUsb(stateString);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mButton = (Button) findViewById(R.id.counter);
//		mButton.setOnClickListener(btnClickListener);
		mLeftUpButton = (Button) findViewById(R.id.leftUp);
		mLeftUpButton.setOnClickListener(btnClickListener);
		mLeftDownButton = (Button) findViewById(R.id.leftDown);
		mLeftDownButton.setOnClickListener(btnClickListener);
		
		mRightUpButton = (Button) findViewById(R.id.rightUp);
		mRightUpButton.setOnClickListener(btnClickListener);
		mRightDownButton = (Button) findViewById(R.id.rightDown);
		mRightDownButton.setOnClickListener(btnClickListener);
		
		mCheckBox = (CheckBox) findViewById(R.id.send0or1);
		mCheckBox.setOnCheckedChangeListener(chkListener);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mTextView = (TextView) findViewById(R.id.showConnectInfo);
		mUsbHandler = new UsbHandler(mUsbManager, getApplicationContext(),this);
	}

	protected void setState(String string) {
		if(!(stateString.contains(string))){
			stateString += string;
		}else{
			int index = stateString.indexOf(string);
			stateString = stateString.substring(0, index) + stateString.substring(index + 1, stateString.length());
		}
		stateString += "\n";
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
	
	public void updateReceivedData(byte[] data){
		String dataString = new String(data);
		mTextView.setText("E: " + dataString);
	}
	
	public void updateReceivedData(byte[] data, int length, int startpoint){
		byte[] subdata = new byte[length];
		for(int i = 0; i < length; i++){
			subdata[i] = data[i + startpoint];
		}
		updateReceivedData(subdata);
	}
	
	
}
