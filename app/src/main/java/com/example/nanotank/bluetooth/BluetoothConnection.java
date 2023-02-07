package com.example.nanotank.bluetooth;


import static com.example.nanotank.MainActivity.CONNECTING_STATUS;
import static com.example.nanotank.MainActivity.applicationOpen;
import static com.example.nanotank.MainActivity.handler;
import static com.example.nanotank.MyTestService.serviceHandler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

public class BluetoothConnection extends Thread {
    private static final String TAG ="BluetoothConnection";
    private static BluetoothAdapter bluetoothAdapter;
    private static HashMap<String,HashMap<String,Object>> connectedDevices = new HashMap<>();
    private final Context context;

    private String deviceAddress;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket mmSocket;
    private BluetoothCommunication bluetoothCommunication;

    public BluetoothConnection(String deviceAddress, Context context) {
        this.deviceAddress = deviceAddress;
        this.context = context;
    }
    public void run() {
        if(!getBluetoothAdapter()) { return; }
        enableBluetooth();
        if(isAlreadyConnected()) { return; }
        if(!getBluetoothDevice()) { return; }
        if(!createSocket()) { return; }
        if(!connectToSocket()) { return; }

        this.bluetoothCommunication = new BluetoothCommunication(mmSocket);
        this.bluetoothCommunication.run();
    }

    private boolean connectToSocket() {
        try {
            Log.d(TAG, "Trying to connect to the remote device through the socket");

            if (!this.mmSocket.isConnected() ){
                this.mmSocket.connect();
            } else{
                Log.d(TAG, "Socket is already connected");
            }
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 1,0).sendToTarget();
            }

        } catch (IOException connectException) {

            Log.e(TAG, "Socket's connect() first attempt failed", connectException);

            try {

                Method m = this.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                this.mmSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                this.mmSocket.connect();

            } catch (Exception e) {
                Log.e(TAG, "Socket's connect() second attempt failed", connectException);

                try {
                    this.mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Socket's close() method failed", closeException);
                } finally {
                    if (applicationOpen) {
                        handler.obtainMessage(CONNECTING_STATUS, 0,0).sendToTarget();
                    } else {
                        serviceHandler.obtainMessage(2).sendToTarget();
                    }
                }
                return false;
            }
        }
        if (applicationOpen) {
            handler.obtainMessage(CONNECTING_STATUS,1,0).sendToTarget();
        }

        this.addToConnected();

        return true;
    }

    private boolean getBluetoothDevice() {
        try {
            this.bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createSocket() {

        UUID uuid = UUID.nameUUIDFromBytes(deviceAddress.getBytes());//UUID.randomUUID();

        try {
            Log.d(TAG, String.format("Trying to create socket for uuid: %s", uuid.toString()));
            int sdk = Build.VERSION.SDK_INT;
            if (sdk >= 10) {
                this.mmSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } else {
                this.mmSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 0, 2).sendToTarget();
            } else {
                serviceHandler.obtainMessage(2).sendToTarget();
            }
            bluetoothAdapter.cancelDiscovery();
            return false;
        } finally {
            bluetoothAdapter.cancelDiscovery();
        }
        return true;
    }

    private boolean isAlreadyConnected() {
        HashMap<String,Object> connectedDevice = connectedDevices.get(deviceAddress);
        if(connectedDevice != null) {
            try {
                this.bluetoothDevice = (BluetoothDevice)connectedDevice.get("device");
                this.mmSocket = (BluetoothSocket)connectedDevice.get("socket");
                this.bluetoothCommunication = (BluetoothCommunication)connectedDevice.get("socket");

                if (applicationOpen) {
                    handler.obtainMessage(CONNECTING_STATUS, 1,1).sendToTarget();
                } else {
                    serviceHandler.obtainMessage(0).sendToTarget();
                }
                if (!bluetoothCommunication.isAlive()) {
                    this.bluetoothCommunication.run();
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean getBluetoothAdapter() {
        Log.d(TAG, "Trying to get bluetooth adapter");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth has not found.");
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 0,3).sendToTarget();
            }

            return false;
        }
        return true;
    }

    private void enableBluetooth() {
        if(bluetoothAdapter.isEnabled()) {
            Log.d(TAG,"Bluetooth is on");
            return;
        }

        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBTIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(enableBTIntent);
    }

    public void send(String message){
        bluetoothCommunication.write(message);
    }

    // Closes the client socket and finish thread.
    public void cancel() {
        try {
            bluetoothCommunication.cancel();
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 0, 4).sendToTarget();
            }
        } catch (Exception e) {
            if (applicationOpen) {
                handler.obtainMessage(CONNECTING_STATUS, 0,1).sendToTarget();
            }
        }
    }
    private void addToConnected() {
        Log.d(TAG, "Registering connecting bluetooth device");
        HashMap<String,Object> connectedDevice = new HashMap<>();
        connectedDevice.put("mmsocket",mmSocket);
        connectedDevice.put("communication",bluetoothCommunication);
        connectedDevice.put("device",bluetoothDevice);

        connectedDevices.put(this.bluetoothDevice.getAddress(),connectedDevice);
        Log.d(TAG, connectedDevices.toString());
    }
}

