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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final int MIN_SPEED = 1000;
	private static final int MAX_SPEED = 2000;
	private static final int MESSAGE_REFRESH = 101;
	private static final int START_WRITE = 103;
	private static final int STOP_WRITE = 104;
	private static final int UPDATE_WRITE = 105;
	private static final int addSpeed = 100;
	
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
	private SeekBar leftUpSeekBar;
	private SeekBar leftDownSeekBar;
	private SeekBar rightUpSeekBar;
	private SeekBar rightDownSeekBar;
	private TextView leftUpSpeedTextView;
	private TextView leftDownSpeedTextView;
	private TextView rightUpSpeedTextView;
	private TextView rightDownSpeedTextView;
	private OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if(isLocked){
				setSpeedToAllMotors(progress);
				updateTextview(0, speeds[0], true);
			}else{
				int index = 0;
				switch (seekBar.getId()) {
				case R.id.leftUp:
					index = 0;
					speeds[index] = MIN_SPEED + progress;
					break;
				case R.id.leftDown:
					index = 1;
					speeds[index] = MIN_SPEED + progress;
					break;
				case R.id.rightUp:
					index = 2;
					speeds[index] = MIN_SPEED + progress;
					break;
				case R.id.rightDown:
					index = 3;
					speeds[index] = MIN_SPEED + progress;
					break;
				}
				updateTextview(index, speeds[index], false);
			}
			mUsbHandler.updateSendingData(speedToString());
		}
	};
	
	private int[] speeds = {MIN_SPEED, MIN_SPEED, MIN_SPEED, MIN_SPEED};
	private boolean[] speedsUp = {true, true, true, true};
	private boolean isLocked = false;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mButton = (Button) findViewById(R.id.lock);
		
		leftUpSeekBar = (SeekBar) findViewById(R.id.leftUp);
		leftDownSeekBar = (SeekBar) findViewById(R.id.leftDown);
		rightUpSeekBar = (SeekBar) findViewById(R.id.rightUp);
		rightDownSeekBar = (SeekBar) findViewById(R.id.rightDown);
		leftUpSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		leftDownSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		rightUpSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		rightDownSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		leftUpSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		leftDownSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		rightUpSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		rightDownSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		
		leftUpSpeedTextView = (TextView) findViewById(R.id.leftUpSpeedText);
		leftDownSpeedTextView = (TextView) findViewById(R.id.leftDownSpeedText);
		rightUpSpeedTextView = (TextView) findViewById(R.id.rightUpSpeedText);
		rightDownSpeedTextView = (TextView) findViewById(R.id.rightDownTextSpeed);
		
		
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mTextView = (TextView) findViewById(R.id.showConnectInfo);
		mUsbHandler = new UsbHandler(mUsbManager, getApplicationContext(), mListener);
	}

	
	protected void setSpeedToAllMotors(int progress) {
		for(int i = 0;i < speeds.length; i++){
			speeds[i] = MIN_SPEED + progress;
		}
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
	
	private void updateTextview(int index, int speed, boolean locked){
		if(locked){
			leftUpSpeedTextView.setText("" + speed);
			leftDownSpeedTextView.setText("" + speed);
			rightUpSpeedTextView.setText("" + speed);
			rightDownSpeedTextView.setText("" + speed);
		}else{
			switch (index) {
				case 0:
					leftUpSpeedTextView.setText("" + speed);
					break;
				case 1:
					leftDownSpeedTextView.setText("" + speed);
					break;
				case 2:
					rightUpSpeedTextView.setText("" + speed);
					break;
				case 3:
					rightDownSpeedTextView.setText("" + speed);
					break;

				default:
					break;
			}
		}
	}
	
	
	private String speedToString(){
		String value = "";
		for(int i = 0; i < speeds.length; i++){
			value += Integer.toString(speeds[i]);
			value += ',';
		}
		value += '\n';
		return value;
	}
	
}
