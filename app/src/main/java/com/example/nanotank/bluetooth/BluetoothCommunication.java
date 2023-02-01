package com.example.nanotank.bluetooth;

import static com.example.nanotank.MainActivity.BLUETOOTH_MESSAGE;
import static com.example.nanotank.MainActivity.CONNECTING_STATUS;
import static com.example.nanotank.MainActivity.applicationOpen;
import static com.example.nanotank.MainActivity.handler;
import static com.example.nanotank.MyTestService.serviceHandler;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothCommunication extends Thread {
    private final String TAG ="BluetoothCommunication";
    private BluetoothSocket mmSocket;
    private InputStream mmInStream=null;
    private OutputStream mmOutStream=null;
    private String message;

    public BluetoothCommunication(BluetoothSocket socket) {
        this.mmSocket = socket;
    }

    public void run() {
        if (!socketStreamsEstablished()) {
            this.cancel();
            return;
        }

        Log.d(TAG,String.format("Started to listen bluetooth socket %s",this.mmSocket.getRemoteDevice().getAddress()));

        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes=0; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                /*
                Read from the InputStream from Arduino until termination character is reached.
                Then send the whole String message to GUI Handler.
                 */
                buffer[bytes] = (byte) mmInStream.read();
                if (buffer[bytes] == '\n'){
                    message = new String(buffer,0,bytes);
                    if (applicationOpen) {
                        handler.obtainMessage(BLUETOOTH_MESSAGE,message).sendToTarget();
                    } else {
                        serviceHandler.obtainMessage(1,message).sendToTarget();
                    }
                    Log.d(TAG,message);
                    bytes = 0;
                } else {
                    bytes++;
                }
            } catch (IOException e) {
                Log.e(TAG,"stream listening exception", e);

                if (applicationOpen) {
                    handler.obtainMessage(BLUETOOTH_MESSAGE,"stream listening").sendToTarget();
                } else {
                    serviceHandler.obtainMessage(2).sendToTarget();
                }
                break;
            }
        }
    }

    private boolean socketStreamsEstablished() {
        try {
            this.mmInStream = this.mmSocket.getInputStream();
            this.mmOutStream = this.mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG,"unable to create streams", e);
            return false;
        }
        if (applicationOpen) {
            handler.obtainMessage(CONNECTING_STATUS, 1,1).sendToTarget();
        } else {
            serviceHandler.obtainMessage(0).sendToTarget();
        }

        return true;
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String message) {
        byte[] bytes = message.getBytes(); //converts entered String into
        if (applicationOpen) {
            handler.obtainMessage(BLUETOOTH_MESSAGE,"inputs").sendToTarget();
        }
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            if (applicationOpen) {
                handler.obtainMessage(BLUETOOTH_MESSAGE,"message error").sendToTarget();
            } else {
                serviceHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmInStream.close();
        } catch (IOException e) {
            Log.e(TAG,"unable to close in stream");
        }
        try {
            mmOutStream.close();
        } catch (IOException e) {
            Log.e(TAG,"unable to close out stream");
        }
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG,"unable to close socket");
        }
    }
}