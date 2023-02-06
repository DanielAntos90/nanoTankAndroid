package com.example.nanotank;

import static com.example.nanotank.App.CHANNEL_1_ID;
import static com.example.nanotank.bluetooth.deviceAddress.NANOTANK;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.nanotank.bluetooth.BluetoothConnection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MyTestService extends IntentService {
    private static String TAG ="MyTestService";

    private static int notificationCount = 1;
    private final int startTime = 18;
    @SuppressLint("HandlerLeak")
    public static Handler serviceHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 0:
                    //bluetoothConnection.addToConnected();
                    bluetoothConnection.send("inputs");
                    break;
                case 1: String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                    if (arduinoMsg.contains("ArduinoOutputs")){
                        String[] arduinoMsgList = arduinoMsg.split(";");
                        displayNotificationMessage(arduinoMsgList[0]);
                    }  break;
                //case 2: setServiceForNextDay(); break;
            }
        }
    };
    private static BluetoothConnection bluetoothConnection;
    private static boolean notified;
    private static PendingIntent pIntent;
    private static AlarmManager alarm;
    private static Context context;

    public MyTestService() {
        super("MyTestService");
    }

    public static void setContext(Context cntxt) {
        context = cntxt;
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        setContext(getApplicationContext());

        String message="";
        Log.i(TAG, "Try to connect to bluetooth device");
        bluetoothConnection = new BluetoothConnection(NANOTANK.address, getApplicationContext());
        if (!bluetoothConnection.isAlive()) {
            bluetoothConnection.start();
        }
        Log.i(TAG, "Waiting for answer");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //displayNotificationMessage("test");
    }

    private static void displayNotificationMessage(String message) {
        Log.i(TAG, "Bluetooth connection done");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_action_nanotank)
                //.setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.ic_launcher))
                .setContentTitle("Update")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();
        notificationManager.notify(1, notification);
        Log.i(TAG, "Notification send");
      //  notified = true;
        //setServiceForNextDay();
    }

    private void setServiceForNextDay(){
        //bluetoothConnection.cancel();
        int currentHour = Calendar.getInstance().getTime().getHours();
        int currentMinutes = Calendar.getInstance().getTime().getMinutes();
        if (notified || currentHour > 22) {
            notified = false;

            Log.i(TAG, "Starting next day alarm");
            alarm.cancel(pIntent);
            Calendar timeCal = Calendar.getInstance();
            timeCal.add(Calendar.DAY_OF_YEAR, 1);
            timeCal.set(Calendar.HOUR_OF_DAY, startTime);
            timeCal.set(Calendar.MINUTE, 0);

            DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            System.out.println(dateFormat.format(timeCal.getTime()));

            alarm.set(AlarmManager.RTC_WAKEUP, timeCal.getTimeInMillis(), pIntent);

        } else if (currentHour==startTime && currentMinutes<10){
            Log.i(TAG, "Starting 15 minutes repeater");
            alarm.cancel(pIntent);
            long firstMillis = System.currentTimeMillis();
            // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
            // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, pIntent);
        }
    }
}