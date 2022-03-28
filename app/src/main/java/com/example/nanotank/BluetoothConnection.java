package com.example.nanotank;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

import static com.example.nanotank.MainActivity.CONNECTING_STATUS;
import static com.example.nanotank.MainActivity.applicationOpen;
import static com.example.nanotank.MainActivity.handler;
import static com.example.nanotank.MyTestService.serviceHandler;

public class BluetoothConnection extends Thread {
    private static final String TAG ="BluetoothConnection";
    private static BluetoothAdapter bluetoothAdapter;
    private static HashMap<String,HashMap<String,Object>> connectedDevices;

    private String deviceAddress; //="98:D3:91:FD:60:CB"; //"98:D3:32:F5:A0:D3";
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket mmSocket;
    private BluetoothCommunication bluetoothCommunication;

    public BluetoothConnection(String deviceAddress) {
        this.deviceAddress = deviceAddress;
        Log.d(TAG, "Trying to get bluetooth adapter");
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            HashMap<String,Object> connectedDevice = connectedDevices.get(deviceAddress);
            if(connectedDevice != null) {
                this.bluetoothDevice = (BluetoothDevice)connectedDevice.get("device");
                this.mmSocket = (BluetoothSocket)connectedDevice.get("socket");
                this.bluetoothCommunication = (BluetoothCommunication)connectedDevice.get("socket");
                return;
            }
        } catch (NullPointerException e) {
            connectedDevices = new HashMap<>(); //not exist
        }
        this.bluetoothDevice = this.bluetoothAdapter.getRemoteDevice(deviceAddress);

        UUID uuid = this.bluetoothDevice.getUuids()[0].getUuid();
        Log.d(TAG, "Bluetooth device uuid: "+uuid.toString());
        try {
            Log.d(TAG, "Trying to create socket");
            int sdk = Build.VERSION.SDK_INT;
            if (sdk >= 10) {
                this.mmSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            }else{
                this.mmSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 0, 2).sendToTarget();
            } else {
                serviceHandler.obtainMessage(2).sendToTarget();
            }
        }
    }
    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        this.bluetoothAdapter.cancelDiscovery();
        HashMap<String,Object> connectedDevice = connectedDevices.get(deviceAddress);
        if (connectedDevice != null){
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 1,1).sendToTarget();
            } else {
                serviceHandler.obtainMessage(0).sendToTarget();
            }
            return;
        }
        Log.d(TAG, "Trying to connect to socket");
        try {
            // Connect to the remote device through the socket.
            if (!this.mmSocket.isConnected()){
                this.mmSocket.connect();
            }else{
                Log.d(TAG, "Socket is already connected");
            }
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 1,0).sendToTarget();
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e(TAG, "Socket's connect() method failed", connectException);
            try {
                Method m = this.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                this.mmSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                this.mmSocket.connect();

            } catch (Exception e) {
                Log.e(TAG, "Socket's connect() second attempt failed", connectException);
                try {
                    this.mmSocket.close();
                    if (applicationOpen) {
                        handler.obtainMessage(CONNECTING_STATUS, 0,0).sendToTarget();
                    }
                } catch (IOException closeException) {
                    Log.e(TAG, "Socket's close() method failed", closeException);
                    if (applicationOpen) {
                        handler.obtainMessage(CONNECTING_STATUS, 0,0).sendToTarget();
                    } else {
                        serviceHandler.obtainMessage(2).sendToTarget();
                    }
                }
            }
            return;
        }

        // The connection attempt succeeded. Perform communication in a separate thread.
        this.bluetoothCommunication = new BluetoothCommunication(mmSocket);
        this.bluetoothCommunication.run();
    }

    public void send(String message){
        bluetoothCommunication.write(message);
    }

    // Closes the client socket and finish thread.
    public void cancel() {
        try {
            bluetoothCommunication.cancel();
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 2, 0).sendToTarget();
            }
        } catch (Exception e) {
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 2,1).sendToTarget();
            }
        }
    }
    public void addToConnected(){
        //HashMap<String,HashMap<String,Object>>
        Log.d(TAG, "Put bluetooth device to hashmap");
        HashMap<String,Object> connectedDevice = new HashMap<>();
        connectedDevice.put("mmsocket",mmSocket);
        connectedDevice.put("communication",bluetoothCommunication);
        connectedDevice.put("device",bluetoothDevice);

        connectedDevices.put(this.bluetoothDevice.getAddress(),connectedDevice);
        Log.d(TAG, connectedDevices.toString());
    }
}
