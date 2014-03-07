package com.example.adbfoward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ScanActivity extends Activity {
	ScanManager scanManager;
	Thread scanThread;
	ProgressDialog mDialog;
	ArrayList<HashMap<String, String>> scanList;
	ExecutorService eSvc;
	int toScan;
	
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
				if(scanSum == ScanActivity.this.toScan){
					mDialog.dismiss();
				}
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		
		scanList = new ArrayList<HashMap<String, String>>();
		
		Button btn = (Button) findViewById(R.id.button1);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				scan();
			}
		});
		
		ListView lv = (ListView) findViewById(R.id.listView1);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				String ip = scanList.get(pos).get("ip");
				Intent i = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("ip", ip);
				i.putExtras(bundle);
				setResult(1, i);
				finish();
			}
		});
	}
	
	private SimpleAdapter obtainAdapter(){
		return new SimpleAdapter(this, scanList, R.layout.list_description, new String[] { "name", "ip" }, new int[] { R.id.item_title, R.id.item_text });
	}
	
	private void updateScanList(){
		ListView list = (ListView) findViewById(R.id.listView1);
		list.setAdapter(obtainAdapter());
		return;
	}
	
	private void scan(){
		eSvc = Executors.newFixedThreadPool(50);
		mDialog = ProgressDialog.show(this, "Wait..", "Scanning..");
		
    	WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	
    	int localIp = wifiInfo.getIpAddress();
    	String ipString = android.text.format.Formatter.formatIpAddress(localIp);
    	Log.d("ScanActivity", "ipString=" + ipString);
    	
    	String[] ipArr = ipString.split("\\.");
    	
    	String ipPrefix = ipArr[0] + "." + ipArr[1] + ".";
    	
    	String netmask = android.text.format.Formatter.formatIpAddress(dhcpInfo.netmask);
		
    	Log.d("SThread", "Start scanning...");
    	Log.d("SThread", "Local IP is " + ipPrefix);
    	Log.d("SThread", "Netmask is " + netmask);
    	
		scanList.clear();
		updateScanList();
		
		int n;
		int m;
		
		if(netmask.equals("255.255.255.0")){
			Log.d("SThread", "Scanning only 1step");
			n = Integer.valueOf(ipArr[2]);
			m = Integer.valueOf(ipArr[2]);
			toScan = 254;
		} else {
			Log.d("SThread", "Scanning 0~10");
			n = 0;
			m = 10;
			toScan = 2794;
		}
		
		for(int i = n; i <= m; ++i){
			for(int j = 1; j <= 254; ++j){
				String ip = ipPrefix + String.valueOf(i) + "." + String.valueOf(j);
				scanManager = new ScanManager(handler, ScanActivity.this, ip, j);
				eSvc.submit(scanManager);
			}
		}
		
		eSvc.shutdown();
	}

}
