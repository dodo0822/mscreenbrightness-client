package com.example.adbfoward;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class ConnectThread extends Thread {
	private final BluetoothSocket socket;
	private final BluetoothDevice device;
	
	public ConnectedThread th;
	
	public ConnectThread(BluetoothDevice dev) {
		BluetoothSocket tmp = null;
		device = dev;
		
		try {
			tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("2396be20-6879-11e3-949a-0800200c9a66"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		socket = tmp;
	}
	
	public void run() {
		try {
			socket.connect();
		} catch(Exception e) {
			e.printStackTrace();
			try {
				socket.close();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		th = new ConnectedThread(socket);
		th.start();
		
	}
	
	public void write(String line) {
		line += '\n';
		if(th != null) th.write(line.getBytes());
	}
	
	public void cancel() {
		try {
			th.write(new String("exit\n").getBytes());
			socket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
