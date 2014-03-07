package com.example.adbfoward;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ScanManager implements Runnable {
	Handler mHandler;
	Context context;
	String ip;
	int i;
	int from;
	
	public ScanManager(Handler _handler, Context _context, String _ip, int _i){
		mHandler = _handler;
		context = _context;
		ip = _ip;
		i = _i;
	}
	
    public void run() {
        try {
    		Message scanMessage = mHandler.obtainMessage();
    		scanMessage.what = 1;
    		Bundle scanBundle = new Bundle();
    		scanBundle.putInt("i", i);
    		scanMessage.setData(scanBundle);
    		scanMessage.sendToTarget();
    		SocketAddress address = new InetSocketAddress(ip, 12345);
            
    		Log.d("SThread", "Connecting to IP=" + ip);
    		
            Socket socket = new Socket();
            socket.connect(address, 500);
            
            // Connected.
            String name = "";
            
            Log.d("SThread", "Connected to IP=" + ip);
            
            OutputStream bos = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            //Log.d("SThread", "Found! Greeting...");
            bos.write(new String("info").getBytes());
            bos.flush();
            
            //Log.d("SThread", "greet sent.");
            
            String line = null;
            while ((line = in.readLine()) != null) {
                Log.d("SThread", "server-recv: " + line);
                name = line;
            }

            bos.close();
            in.close();
            
            //long end = System.currentTimeMillis();
            //Log.d("SThread", String.valueOf(end-start));
            
            Message msg = mHandler.obtainMessage();
            msg.what = 0;
            Bundle data = new Bundle();
            data.putString("ip", ip);
            data.putString("name", name);
            msg.setData(data);
            msg.sendToTarget();
            socket.close();
        	
    		//Message compMessage = mHandler.obtainMessage();
    		//compMessage.what = 4;
    		//compMessage.sendToTarget();
        } catch(SocketTimeoutException e){
        	
        } catch(Exception e){
        	//e.printStackTrace();
        }
    }
}