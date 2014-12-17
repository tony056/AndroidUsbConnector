package com.example.testandroidusb;

import java.net.Socket;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.LinkedList;

import com.example.eulerangleconverter.YPREventListener;
import com.example.eulerangleconverter.YPREventListener.Listener;

import HotSpotCommander.HotSpotServerEventHandler;
import HotSpotCommander.HotSpotTCPServer;
import PIDController.PIDController;
import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.graphics.Path.Direction;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
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
	private static final int MAX_SMOOTH_COUNT = 10;

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
	private HotSpotTCPServer mHoSpotTCPServer;
	private UsbHandler mUsbHandler;
	private SensorManager mSensorManager;

	private TextView mTextView;
	private Button mButton;
	private SeekBar leftUpSeekBar;
	private SeekBar leftDownSeekBar;
	private SeekBar rightUpSeekBar;
	private SeekBar rightDownSeekBar;
	private TextView leftUpSpeedTextView;
	private TextView leftDownSpeedTextView;
	private TextView rightUpSpeedTextView;
	private TextView rightDownSpeedTextView;
	private TextView sensorDataTextView;
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
			if(fromUser){
				if (isLocked) {
					setSpeedToAllMotors(progress);
					updateTextview(0, speeds[0], true);
				} else {
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
		}
	};

	private int[] speeds = { MIN_SPEED, MIN_SPEED, MIN_SPEED, MIN_SPEED };
	private int[] outputSpeeds = { MIN_SPEED, MIN_SPEED, MIN_SPEED, MIN_SPEED};
	private PIDController yawPidController;
	private PIDController pitchPidController;
	private PIDController rollPidController;
	private YPREventListener mYprEventListener;
	private Listener yprListener = new Listener() {
		@Override
		public void onOrientationChanged(float yaw, float pitch, float roll) {
			ypr[0] = yaw;
			ypr[1] = pitch;
			ypr[2] = roll;
			calculateSpeed(yaw, pitch, roll);
			mUsbHandler.updateSendingData(speedToString());
			updateSensorTextView();
		}
	};
	private float[] ypr = new float[3];
	private boolean isLocked = false;
	private float[] accel = new float[3];
	private float[] magnet = new float[3];
	private float[] gyro = new float[3];
	private int ratio = 0;
	private Vector3 smoothAcc;
	private float[][] vectors = {
			{1, -1, 0},
			{1, 1, 0},
			{-1, -1, 0},
			{-1, 1, 0}
	};
	
	class Vector3
	{
		float x;
		float y;
		float z;
		public Vector3(float x, float y, float z){
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	LinkedList<Vector3> prevAccelDatas = new LinkedList<Vector3>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		mHoSpotTCPServer = new HotSpotTCPServer();
		mHoSpotTCPServer.RegisterHandler(new HotSpotServerEventHandler() {

			@Override
			public void OnReceiveMessage(Socket client, String message) {
				// Toast.makeText(getApplicationContext(), message,
				// Toast.LENGTH_SHORT).show();
				parseReceivedMessage(message);
//				calculateSpeed();
//				mUsbHandler.updateSendingData(speedToString());
			}

			@Override
			public void OnDisconnected(Socket client) {
				mTextView.setText("Disconnected");
			}

			@Override
			public void OnConnected(Socket client) {
				mTextView.setText("Connected");
			}
		});

		try {
			mHoSpotTCPServer.Start(5566);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), e.getMessage(),
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		mButton = (Button) findViewById(R.id.lock);
		mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				isLocked = !isLocked;
				mButton.setText("" + isLocked);
			}
		});

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
		sensorDataTextView = (TextView) findViewById(R.id.sensorData);

		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mTextView = (TextView) findViewById(R.id.showConnectInfo);
		mUsbHandler = new UsbHandler(mUsbManager, getApplicationContext(),
				mListener);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mYprEventListener = new YPREventListener(mSensorManager);
		mYprEventListener.initYPREventListener();
		mYprEventListener.startListening(yprListener);
		
		yawPidController = new PIDController(0, 4, 0.2, 1, PIDController.Direction.normal);
		pitchPidController = new PIDController(0, 4, 0.2, 1, PIDController.Direction.normal);
		rollPidController = new PIDController(0, 4, 0.2, 1, PIDController.Direction.normal);
		
		yawPidController.SetMode(true);
		pitchPidController.SetMode(true);
		rollPidController.SetMode(true);
		
		yawPidController.SetOutputLimits(-50, 50);
		pitchPidController.SetOutputLimits(-100, 100);
		rollPidController.SetOutputLimits(-100, 100);
	}
	


	protected void calculateSpeed(float yaw, float pitch, float roll) {
		
		yawPidController.Input = yaw;
		pitchPidController.Input = pitch;
		rollPidController.Input = roll;
		yawPidController.Compute();
		pitchPidController.Compute();
		rollPidController.Compute();
		
		double yawOffset = yawPidController.Output;
		double pitchOffset = pitchPidController.Output;
		double rollOffset = rollPidController.Output;
		outputSpeeds[0] = (int) (speeds[0] - pitchOffset + yawOffset);
		outputSpeeds[1] = (int) (speeds[1] - rollOffset - yawOffset);
		outputSpeeds[2] = (int) (speeds[2] + pitchOffset + yawOffset);
		outputSpeeds[3] = (int) (speeds[3] + rollOffset - yawOffset);
		
		for(int i = 0; i < speeds.length; i++){
			updateTextview(i, outputSpeeds[i], false);
		}
	}

	
	protected void parseReceivedMessage(String message) {
		String data = message.substring(0, message.length() - 1);
		String[] tokens = data.split(",");
		if (tokens.length > 0) {
			for (int i = 0; i < tokens.length; i++) {
				if(i == 4){
					ratio = Integer.parseInt(tokens[i]);
				}else{
					speeds[i] = Integer.parseInt(tokens[i]);
//					updateTextview(i, speeds[i], false);
				}
			}
		}
//		mTextView.setText(message);
	}

	protected void setSpeedToAllMotors(int progress) {
		leftUpSeekBar.setProgress(progress);
		leftDownSeekBar.setProgress(progress);
		rightUpSeekBar.setProgress(progress);
		rightDownSeekBar.setProgress(progress);
		for (int i = 0; i < speeds.length; i++) {
			speeds[i] = MIN_SPEED + progress;
			updateTextview(i, speeds[i], false);
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
	public void onResume() {
		super.onResume();
		mUsbHandler.sendEmptyMessage(MESSAGE_REFRESH);

	}

	@Override
	protected void onPause() {
		super.onPause();
		mUsbHandler.removeMessages(MESSAGE_REFRESH);
//		mSensorManager.unregisterListener(this);
	}

	public void updateReceivedData(String data) {
		mTextView.setText(data);
	}

	private void updateProgressBar(int index, int progress) {
		switch (index) {
		case 0:
			leftUpSeekBar.setProgress(progress);
			break;
		case 1:
			leftDownSeekBar.setProgress(progress);
			break;
		case 2:
			rightUpSeekBar.setProgress(progress);
			break;
		case 3:
			rightDownSeekBar.setProgress(progress);
			break;

		default:
			break;
		}
	}

	private void updateTextview(int index, int speed, boolean locked) {
		if (locked) {
			leftUpSpeedTextView.setText("" + speed);
			leftDownSpeedTextView.setText("" + speed);
			rightUpSpeedTextView.setText("" + speed);
			rightDownSpeedTextView.setText("" + speed);
		} else {
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

	private String speedToString() {
		String value = "";
		for (int i = 0; i < outputSpeeds.length; i++) {
			if(outputSpeeds[i] >= MIN_SPEED && outputSpeeds[i] <= MAX_SPEED){
				value += Integer.toString(outputSpeeds[i]);
			}else if(outputSpeeds[i] > MAX_SPEED){
				value += Integer.toString(MAX_SPEED);
			}else{
				value += Integer.toString(MIN_SPEED);
			}
			value += ',';
		}
		value += '\n';
		return value;
	}


	
	private void updateSensorTextView(){
		String data = "";
		//for(int i = 0; i < accel.length; i++){
			data += String.format("%.2f", ypr[0]);
			data += ",";
			data += String.format("%.2f", ypr[1]);
			data += ",";
			data += String.format("%.2f", ypr[2]);
			data += ",";
		//}
		data += "\n";
		for(int i = 0; i < gyro.length; i++){
			data += String.format("%.2f", gyro[i]);
			data += ",";
		}
		data += "\n";
		sensorDataTextView.setText(data);
	}
	
	public void StoreAcc(Vector3 v3)
	{
		this.prevAccelDatas.add(v3);
		
		while(this.prevAccelDatas.size()>MAX_SMOOTH_COUNT)
		{
			this.prevAccelDatas.remove(0);
		}
	}
	
	public Vector3 GetAccAverage()
	{
		Vector3 sum = new Vector3(0,0,0);
		
		for(Vector3 v3 : this.prevAccelDatas)
		{
			sum.x += v3.x;
			sum.y += v3.y;
			sum.z += v3.z;
		}
		
		sum.x /= prevAccelDatas.size();
		sum.y /= prevAccelDatas.size();
		sum.z /= prevAccelDatas.size();
		
		return sum;
	}

}
