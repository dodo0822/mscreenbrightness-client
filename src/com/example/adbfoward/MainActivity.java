package com.example.adbfoward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;

public class MainActivity extends Activity {
	
	SeekBar seekBar;
	ImageView imageView;
	ListView targetList;
	Switch toggleSwitch;
	
	ScanManager scanManager;
	Thread scanThread;
	ProgressDialog mDialog;
	ArrayList<HashMap<String, String>> scanList;
	ExecutorService eSvc;
	
	Dialog listDialog;
	
	int toScan = 0;
	
	Handler handler = new Handler(){
		int scanSum = 0;
		
		@Override
		public void handleMessage(Message msg){
			Bundle data = msg.getData();
			
			switch(msg.what){
			case 0:
				String ip = data.getString("ip");
				String name = data.getString("name");
				
				HashMap<String, String> map = new HashMap<String, String>();
				
				map.put("ip", ip);
				map.put("name", name);
				map.put("select", "n");
				
				scanList.add(map);
				
				updateScanList();
				
				Log.d("MainActivity", "Received: (" + ip + "," + name + ")");
				
				break;
			case 1:
				scanSum++;
				if(scanSum == MainActivity.this.toScan){
					mDialog.dismiss();
					scanSum = 0;
				}
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		scanList = new ArrayList<HashMap<String, String>>();
		
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		imageView = (ImageView) findViewById(R.id.imageView);
		toggleSwitch = (Switch) findViewById(R.id.toggleSwitch);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				imageView.setAlpha(55 + (int) (progress * 2));
			}
		});
		toggleSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(!arg1){
					Intent stopIntent = new Intent(MainActivity.this, WiFiService.class);
					stopService(stopIntent);
					return;
				}
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				
				targetList = new ListView(MainActivity.this);
				targetList.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int pos, long arg3) {
						String ip = scanList.get(pos).get("ip");
						Intent startIntent = new Intent(MainActivity.this, WiFiService.class);
						startIntent.putExtra("ip", ip);
						startService(startIntent);
						listDialog.cancel();
					}
				});
				builder.setView(targetList);
				builder.setNegativeButton("¨ú®ø", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						toggleSwitch.setChecked(false);
					}
				});
				listDialog = builder.create();
				listDialog.show();

				mDialog = ProgressDialog.show(MainActivity.this, "Wait..", "Scanning..");
				
				eSvc = Executors.newFixedThreadPool(50);
				
		    	WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		    	DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		    	
		    	int localIp = wifiInfo.getIpAddress();
		    	String ipString = android.text.format.Formatter.formatIpAddress(localIp);
		    	Log.d("ScanActivity", "ipString=" + ipString);
		    	
		    	String[] ipArr = ipString.split("\\.");
		    	
		    	String ipPrefix = ipArr[0] + "." + ipArr[1] + "." + ipArr[2] + ".";
				
		    	Log.d("SThread", "Start scanning...");
		    	Log.d("SThread", "Local IP is " + ipPrefix);
		    	
				scanList.clear();
				updateScanList();
				
				Log.d("SThread", "Scanning only 1step");
				toScan = 254;
			
				for(int j = 1; j <= 254; ++j){
					String ip = ipPrefix + String.valueOf(j);
					scanManager = new ScanManager(handler, MainActivity.this, ip, j);
					eSvc.submit(scanManager);
				}
				
				eSvc.shutdown();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void updateScanList(){
		targetList.setAdapter(new SimpleAdapter(this, scanList, R.layout.list_description, new String[] { "name", "ip" }, new int[] { R.id.item_title, R.id.item_text }));
		return;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == 1 && resultCode == 1){
			Bundle bundle = data.getExtras();
			String ip = bundle.getString("ip");
			((EditText) findViewById(R.id.editText1)).setText(ip);
		}
	}

}
