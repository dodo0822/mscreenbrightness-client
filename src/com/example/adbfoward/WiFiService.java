package com.example.adbfoward;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class WiFiService extends Service implements SensorEventListener {

	private SensorManager mgr;
	private Sensor sensor;
	private float light;
	
	private Handler handler;
	private Runnable runnable;
	
	private boolean stopFlag;
	
	public String ip;
	
	private class SendMessageTask implements Runnable {
		String message;
		
		public SendMessageTask(String _msg){
			message = _msg;
		}
		
		public void run(){
			Socket socket;
			try {
				InetAddress serverAddr = InetAddress.getByName(ip);
				Log.d("TAG", "Connecting..");
				socket = new Socket(serverAddr, 12345);
				socket.setSoTimeout(1000);
				OutputStream stream = socket.getOutputStream();
				stream.write(message.getBytes());
				stream.flush();
				stream.close();
				Log.d("TAG", "Write complete!");
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		stopFlag = false;
		light = -1;
		mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		List<Sensor> list = this.mgr.getSensorList(Sensor.TYPE_LIGHT);
		for (Sensor s : list) {
			Log.d("TAG", "Sensor: " + s.getName());
		}
		list = mgr.getSensorList(Sensor.TYPE_LIGHT);
		sensor = list.get(0);
		
		handler = new Handler();
		runnable = new Runnable(){
			@Override
			public void run() {
				if(stopFlag) return;
				sendMessage();
				handler.postDelayed(this, 500);
			}
		};
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		ip = bundle.getString("ip");
		
		mgr.registerListener(WiFiService.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		handler.postDelayed(runnable, 0);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		mgr.unregisterListener(WiFiService.this, sensor);
		handler.removeCallbacks(runnable);
	
		try {
			Thread t = new Thread(new SendMessageTask("D"));
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		light = event.values[0];
	}
	
	synchronized private void sendMessage() {
		if(light == -1) return;
		String message = "";
		message = "L" + String.valueOf(light);
		Thread t = new Thread(new SendMessageTask(message));
		t.start();
	}

}
