package com.example.adbfoward;

import java.util.List;
import java.util.Set;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BluetoothService extends Service implements SensorEventListener {

	private SensorManager mgr;
	private Sensor sensor;
	private float light;
	
	private Handler handler;
	private Runnable runnable;
	
	private boolean stopFlag;
	
	ConnectThread thread;
	
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
		mgr.registerListener(BluetoothService.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		BluetoothAdapter adap = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> paired = adap.getBondedDevices();
		if(paired.size() > 0){
			BluetoothDevice dev = null;
			for(BluetoothDevice device : paired){
				Log.d("DEV", device.getName() + ", addr: " + device.getAddress());
				if(device.getName().equals("DODO-U2442")) {
					dev = device;
				}
			}
			if(dev != null) {
				thread = new ConnectThread(dev);
				thread.start();
			}
		}
		handler.postDelayed(runnable, 500);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		mgr.unregisterListener(BluetoothService.this, sensor);
		stopFlag = true;
		handler.removeCallbacks(runnable);
		thread.cancel();
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
		String message = "L" + String.valueOf(light);
		thread.write(message);
	}

}
