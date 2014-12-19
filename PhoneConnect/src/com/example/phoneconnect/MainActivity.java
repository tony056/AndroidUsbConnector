package com.example.phoneconnect;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import HotSpotCommander.HotSpotClientEventHandler;
import HotSpotCommander.HotSpotClientInterface;
import HotSpotCommander.HotSpotTCPClient;
import android.R.integer;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
	
	private static final int MIN_SPEED = 1000;
	private static final int MAX_SPEED = 2000;
	private static final int BALANCE_MIN = 0;
	private static final int BALANCE_MAX = 100;
	
	HotSpotClientInterface client;
	TextView wifiText;
	TextView textEvents;
	Button connectButton;
	Button disconnectButton;
	
	private Button lockButton;
	private SeekBar leftUpSeekBar;
	private SeekBar leftDownSeekBar;
	private SeekBar rightUpSeekBar;
	private SeekBar rightDownSeekBar;
	private SeekBar balanceSeekBar;
	private TextView leftUpSpeedTextView;
	private TextView leftDownSpeedTextView;
	private TextView rightUpSpeedTextView;
	private TextView rightDownSpeedTextView;
	private TextView balanceTextView;
	private RadioButton yawRadioButton;
	private RadioButton pitchRadioButton;
	private RadioButton rollRadioButton;
	private RadioGroup radioGroup;
	
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
			
			if(fromUser)
			{
				if(seekBar.getId() == R.id.balanceBar)
				{
					speeds[4] = BALANCE_MIN + progress;
					balanceTextView.setText(""+speeds[4]);
				}
				else if(isLocked){
//					setSpeedToAllMotors(progress);
//					updateTextview(0, speeds[0], true);
					int kValueIndex = 0;
					switch (seekBar.getId()) {
						case R.id.leftUp:
							setSpeedToAllMotors(progress);
							textEvents.setText(kToString() + speedToString());
							sendMessage(kToString() + speedToString());
							break;
						case R.id.leftDown:
							kValueIndex = 0;
							updateK(kValueIndex, yprIndex, progress);
							textEvents.setText(kToString() + speedToString());
							sendMessage(kToString() + speedToString());
							break;
						case R.id.rightUp:
							kValueIndex = 1;
							updateK(kValueIndex, yprIndex, progress);
							textEvents.setText(kToString() + speedToString());
							sendMessage(kToString() + speedToString());
							break;
						case R.id.rightDown:
							kValueIndex = 2;
							updateK(kValueIndex, yprIndex, progress);
							textEvents.setText(kToString() + speedToString());
							sendMessage(kToString() + speedToString());
							break;
						default:
							break;
					}
					
				}
				
			}
		}
	};
	
	private RadioGroup.OnCheckedChangeListener mRadioListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.yawRadio:
				yprIndex = 0;
				break;
			case R.id.pitchRadio:
				yprIndex = 1;
				break;
			case R.id.rollRadio:
				yprIndex = 2;
				break;
			default:
				break;
			}
			updateSeekView(yprIndex);
		}
	};
	
	private int[] speeds = {MIN_SPEED, MIN_SPEED, MIN_SPEED, MIN_SPEED,BALANCE_MIN};
	private float[] k_min = {0.0f, 0.0f, 0.0f};
	private boolean isLocked = true;
	private int yprIndex = 0;
	private int MAX_K = 10;
	private float[][] kValue = {
			{0.0f, 0.0f, 0.0f},
			{0.75f, 1.50f, 0.0f},
			{0.78f, 1.56f, 0.0f}
	};
	
	final int EVENT_COUNT = 3;
	List<String> eventList = new ArrayList<String>();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        
        wifiText = (TextView)this.findViewById(R.id.textWifi);
        textEvents = (TextView)this.findViewById(R.id.textEvent);
        connectButton = (Button)this.findViewById(R.id.buttonConnect);
        disconnectButton = (Button)this.findViewById(R.id.buttonDisconnect);
        

        String ip = getAccessPointIP();
        wifiText.setText("\n Connect to"+ip);
        
        
        leftUpSeekBar = (SeekBar) findViewById(R.id.leftUp);
		leftDownSeekBar = (SeekBar) findViewById(R.id.leftDown);
		rightUpSeekBar = (SeekBar) findViewById(R.id.rightUp);
		rightDownSeekBar = (SeekBar) findViewById(R.id.rightDown);
		balanceSeekBar = (SeekBar) findViewById(R.id.balanceBar);
		
		leftUpSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		leftDownSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		rightUpSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		rightDownSeekBar.setMax(MAX_SPEED - MIN_SPEED);
		balanceSeekBar.setMax(BALANCE_MAX - BALANCE_MIN);
		
		leftUpSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		leftDownSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		rightUpSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		rightDownSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		balanceSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		
		leftUpSpeedTextView = (TextView) findViewById(R.id.leftUpSpeedText);
		leftDownSpeedTextView = (TextView) findViewById(R.id.leftDownSpeedText);
		rightUpSpeedTextView = (TextView) findViewById(R.id.rightUpSpeedText);
		rightDownSpeedTextView = (TextView) findViewById(R.id.rightDownSpeedText);
		balanceTextView = (TextView) findViewById(R.id.balanceBarText);
		
		radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		yawRadioButton = (RadioButton) findViewById(R.id.yawRadio);
		pitchRadioButton = (RadioButton) findViewById(R.id.pitchRadio);
		rollRadioButton = (RadioButton) findViewById(R.id.rollRadio);
		radioGroup.setOnCheckedChangeListener(mRadioListener);
        
        client = new HotSpotTCPClient();
        client.RegisterHandler(new HotSpotClientEventHandler(){

			@Override
			public void OnConnected(Socket server) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
//				addEvent(server.getInetAddress()+" Connected");
			}

			@Override
			public void OnDisconnected(Socket server) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_SHORT).show();
//				addEvent(server.getInetAddress()+" Disconnected");
			}

			@Override
			public void OnReceiveMessage(Socket server, String message) {
				// TODO Auto-generated method stub
				
				addEvent(server.getInetAddress()+":"+message);
			}});
        
    	UpdateView();

    }
    
    protected void updateSeekView(int index) {
		leftDownSeekBar.setProgress((int) (kValue[index][0] * 100));
		rightUpSeekBar.setProgress((int) (kValue[index][1] * 100));
		rightDownSeekBar.setProgress((int) (kValue[index][2] * 100));
	}

	protected void updateK(int kValueIndex, int yprIndex2, int progress) {
		if(progress > 0)
			kValue[yprIndex2][kValueIndex] = 1.0f * (progress / 100.0f + k_min[kValueIndex]);
		else 
			kValue[yprIndex2][kValueIndex] = k_min[kValueIndex];
	}

	protected void sendMessage(String msg) {
    	if(client.IsConnected())
    	{
			client.SendMessage(msg);
    	}	
	}

	int eventCounter = 0;
    
    private void addEvent(String str)
    {
    	eventList.add(eventCounter+++":"+str);
    	if(eventList.size()> EVENT_COUNT)eventList.remove(0);
    	
    	UpdateView();
    }
    
    private void UpdateView()
    {
    	String eventText = "";
    	
    	for(String s : eventList)
    	{
    		eventText += s+"\n";
    	}
    	
    	this.wifiText.setText("Connected:"+this.client.IsConnected());
    	this.textEvents.setText(eventText);
    	
    	this.connectButton.setEnabled(!this.client.IsConnecting());
    	this.disconnectButton.setEnabled(this.client.IsConnecting());
    	
    }
    
    
    private String getAccessPointIP()
    {
        DhcpInfo d;
        WifiManager wifii;
        
        wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
        d=wifii.getDhcpInfo();
        String ip = intToIp(d.gateway);
        
        return ip;
    }

    public String intToIp(int i) {

    	   return ((i) & 0xFF ) + "." +
    	               ((i >> 8 ) & 0xFF) + "." +
    	               ((i >> 16 ) & 0xFF) + "." +
    	               ( i >> 24 & 0xFF) ;
    	}

    
//    public void onSendClick(View view) 
//    {	
//    	if(client.IsConnected())
//    	{
//			client.SendMessage(editText.getText().toString());
//    	}
//    	
//    	UpdateView();
//	}
    
    public void onConnectClick(View view) {

    	if(!client.IsConnected())
    	{
    	try {
			client.Connect(getAccessPointIP(),5566);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	}
    	

    	UpdateView();
	}
    
    public void onDisconnectClick(View view) {

    	if(client.IsConnecting()||client.IsConnected())
    	{
			client.Disconnect();
    	}
    	

    	UpdateView();
	}
    
	@Override
	protected void onStop() {
		super.onStop();
		
		client.Disconnect();
		addEvent("Client Disconnected");
		
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
    
    private void updateTextview(int index, int speed, boolean locked){
		if(locked){
			leftUpSpeedTextView.setText("" + speed);
//			leftDownSpeedTextView.setText("" + speed);
//			rightUpSpeedTextView.setText("" + speed);
//			rightDownSpeedTextView.setText("" + speed);
			
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
    
    protected void setSpeedToAllMotors(int progress) {
		leftUpSeekBar.setProgress(progress);
//		leftDownSeekBar.setProgress(progress);
//		rightUpSeekBar.setProgress(progress);
//		rightDownSeekBar.setProgress(progress);
		for(int i = 0;i < speeds.length; i++){
			speeds[i] = MIN_SPEED + progress;
		}
	}
    
    private String speedToString(){
		String value = "";
		
		for(int i = 0; i < speeds.length; i++){
			value += Integer.toString(speeds[i]);
			value += ',';
		}
//		value += '\n';
		return value;
	}
    
    private String kToString(){
    	String data = "" + Integer.toString(yprIndex) + ",";
    	for(int i = 0; i < 3; i++){
    		data += String.format("%.2f", kValue[yprIndex][i]);
    		data += ",";
    	}
    	return data;
    }
}
