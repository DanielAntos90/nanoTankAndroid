package com.example.nanotank;

import static com.example.nanotank.deviceAddress.NANOTANK;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="MainActivity";
    private TextView textDate, textTime,mDisplayBTStatus,ledBrightnessText,mLedOn,mLedOff,
            mLeDdimming;
    private Button dateButton,timeButton,timeLedOnButton,timeLedOffButton,updateTimeDateButton,
            dimmingButton, updateLedButton,scheduleButton,unscheduleButton;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private TimePickerDialog.OnTimeSetListener mTimeSetListener;
    private ImageView imgBluetoothConnected,imgBluetoothDisconnected,imgBluetoothError,
            imgLedOn,imgLedOff,imgLedUnknown;
    private int idTimeButton=0;

    private ProgressBar progressBar;
    public static Handler handler;
    private SeekBar ledBrightness;

    private BluetoothConnection bluetoothConnection;

    public final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    public final static int BLUETOOTH_MESSAGE = 2; // used in bluetooth handler to identify message update
    public final static int NOTIFICATION = 3; // used in bluetooth handler to identify message update
    public static boolean applicationOpen;

    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutInitialization();
        applicationOpen=true;

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);


        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1) {
                            case 0:
                                progressBar.setVisibility(View.GONE);
                                imgBluetoothConnected.setVisibility(View.GONE);
                                imgBluetoothDisconnected.setVisibility(View.GONE);
                                imgBluetoothError.setVisibility(View.VISIBLE);
                                switch (msg.arg2) {
                                    case 0:
                                        mDisplayBTStatus.setText("Cannot connect to device");
                                        break;
                                    case 1:
                                        mDisplayBTStatus.setText("Could not close the client socket");
                                        break;
                                    case 2:
                                        mDisplayBTStatus.setText("Socket's create() method failed");
                                        break;

                                }
                                break;
                            case 1:
                                progressBar.setVisibility(View.GONE);
                                imgBluetoothDisconnected.setVisibility(View.GONE);
                                imgBluetoothError.setVisibility(View.GONE);
                                imgBluetoothConnected.setVisibility(View.VISIBLE);
                                switch (msg.arg2) {
                                    case 0:
                                        mDisplayBTStatus.setText("Connection established");
                                        break;
                                    case 1:
                                        mDisplayBTStatus.setText("Communication established");
                                        bluetoothConnection.addToConnected();
                                        bluetoothConnection.send("inputs");
                                }
                                break;
                            case 2:
                                progressBar.setVisibility(View.GONE);

                                imgBluetoothError.setVisibility(View.GONE);
                                imgBluetoothConnected.setVisibility(View.GONE);
                                imgBluetoothDisconnected.setVisibility(View.VISIBLE);
                                switch (msg.arg2) {
                                    case 0:
                                        mDisplayBTStatus.setText("Connection closed");
                                        break;
                                    case 1:
                                        mDisplayBTStatus.setText("Could not close the client socket");
                                        break;
                                }
                                break;
                        }
                        break;
                    case BLUETOOTH_MESSAGE:{
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        if (arduinoMsg.contains("ArduinoOutputs")){
                            try{
                            String[] arduinoMsgList = arduinoMsg.split(";");
                            textTime.setText(arduinoMsgList[1]);
                            textDate.setText(arduinoMsgList[2]);
                            mLedOn.setText(arduinoMsgList[3]);
                            mLedOff.setText(arduinoMsgList[4]);
                            mLeDdimming.setText(arduinoMsgList[5]+" min.");
                            ledBrightness.setProgress(Integer.parseInt(arduinoMsgList[6]));

                            if (!updateTimeDateButton.isEnabled()){
                                updateTimeDateButton.setEnabled(true);
                                updateLedButton.setEnabled(true);
                            }

                            switch (arduinoMsgList[7]){
                            case "led on":
                                ledIsOn();
                                mDisplayBTStatus.setText("Waiting for action");
                                break;
                            case "led off":
                                ledIsOff();
                                mDisplayBTStatus.setText("Waiting for action");
                                break;}
                            }catch (Exception exception){
                                mDisplayBTStatus.setText("Unable to read data");
                            }
                        }else{switch (arduinoMsg.toLowerCase()){
                            case "led on":
                                ledIsOn();
                                mDisplayBTStatus.setText("Waiting for action");
                                break;
                            case "led off":
                                ledIsOff();
                                mDisplayBTStatus.setText("Waiting for action");
                                break;
                            case "inputs":
                                mDisplayBTStatus.setText("Request for Nanotank");
                                progressBar.setVisibility(View.VISIBLE);
                                break;
                            case "message error":
                                imgBluetoothConnected.setVisibility(View.GONE);
                                imgBluetoothDisconnected.setVisibility(View.GONE);
                                imgBluetoothError.setVisibility(View.VISIBLE);
                                mDisplayBTStatus.setText("Unable to send message");
                                break;
                            case "stream listening":
                                imgBluetoothConnected.setVisibility(View.GONE);
                                imgBluetoothDisconnected.setVisibility(View.GONE);
                                imgBluetoothError.setVisibility(View.VISIBLE);
                                mDisplayBTStatus.setText("Stream listening error");
                                break;
                            case "time changed":
                                mDisplayBTStatus.setText("Time changed");
                                progressBar.setVisibility(View.GONE);
                                break;
                            case "light changed":
                                mDisplayBTStatus.setText("Light changed");
                                progressBar.setVisibility(View.GONE);
                                break;
                            }
                            break;
                        }
                    }break;
                }
            }
        };


        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                idTimeButton=1;

                TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        mTimeSetListener, hour, minute, true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        timeLedOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int hour,minute=0;
                try {
                    hour = Integer.parseInt(mLedOn.toString().split(":")[0]);
                    minute = Integer.parseInt(mLedOn.toString().split(":")[1]);
                } catch ( IllegalArgumentException exception) {
                    hour=10;
                }

                idTimeButton=2;

                TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        mTimeSetListener, hour, minute, true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        timeLedOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int hour,minute=0;
                try {
                    hour = Integer.parseInt(mLedOff .toString().split(":")[0]);
                    minute = Integer.parseInt(mLedOff .toString().split(":")[1]);
                } catch ( IllegalArgumentException exception) {
                    hour=20;
                }

                idTimeButton=3;

                TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        mTimeSetListener, hour, minute, true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        MainActivity.this,android.R.style.Theme_Holo_Light_Dialog_NoActionBar,mDateSetListener,year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        dimmingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog d = new Dialog(MainActivity.this);
                d.setTitle("NumberPicker");
                d.setContentView(R.layout.dialog);
                Button b1 = (Button) d.findViewById(R.id.button1);
                Button b2 = (Button) d.findViewById(R.id.button2);
                final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker1);
                np.setMaxValue(60); // max value 100
                np.setMinValue(0);   // min value 0
                np.setWrapSelectorWheel(false);
                try {
                    String text= mLeDdimming.getText().toString();
                    np.setValue(Integer.parseInt(text.substring(0,text.length()-5))); // max value 100
                } catch ( IllegalArgumentException exception) {
                    np.setValue(30);
                }

                //np.setOnValueChangedListener(this);
                b1.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v) {
                        mLeDdimming.setText(String.valueOf(np.getValue()+" min.")); //set the value to textview
                        d.dismiss();
                    }
                });
                b2.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v) {
                        d.dismiss(); // dismiss the dialog
                    }
                });
                d.show();
            }

        });
        mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Log.d(TAG, "onTimeSet: Time:" + hour + ":" + minute);

                String timestr = String.format(Locale.GERMANY, "%d:%02d",hour, minute);
                switch (idTimeButton){
                    case 1: textTime.setText(timestr); break;
                    case 2: mLedOn.setText(timestr); break;
                    case 3: mLedOff.setText(timestr); break;
                }
                idTimeButton=0;
            }
        };
        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month=month+1;
                Log.d(TAG, "onDateSet: dd/mm/yyyy:" + day + "/" + month + "/" + year);

                String date = String.format(Locale.GERMANY, "%d.%d.%d", day, month, year);
                textDate.setText(date);
            }
        };
        imgLedOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothConnection.send("turn led off");
                imgLedOn.setVisibility(View.GONE);
                imgLedUnknown.setVisibility(View.VISIBLE);
            }
        });
        imgLedOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothConnection.send("turn led on");
                imgLedOff.setVisibility(View.GONE);
                imgLedUnknown.setVisibility(View.VISIBLE);
            }
        });
        updateTimeDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = textTime.getText().toString();
                String[] date = textDate.getText().toString().split("\\.");
                date[2] = date[2].substring(2);
                Log.d(TAG,String.join(".", date));
                bluetoothConnection.send("timedate;"+time+";"+String.join(".", date));
            }
        });


        updateLedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ledOn = mLedOn.getText().toString();
                String ledOff = mLedOff.getText().toString();
                String dim = mLeDdimming.getText().toString();
                int brightness = ledBrightness.getProgress();
                bluetoothConnection.send("light;"+ledOn+";"+ledOff+";"+brightness+";"+dim.replace(" min.",""));
            }
        });
        scheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSchedule();
            }
        });
        unscheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelSchedule();
            }
        });

        checkBluetoothConnection();

    }

    private void layoutInitialization(){
        textDate = (TextView) findViewById(R.id.NanoTankDate);
        textDate.setText("Unknown");
        textTime = (TextView) findViewById(R.id.NanoTankTime);
        textTime.setText("Unknown");

        imgBluetoothConnected = (ImageView)findViewById(R.id.bluetoothConnected);
        imgBluetoothConnected.setVisibility(View.INVISIBLE);
        imgBluetoothDisconnected = (ImageView)findViewById(R.id.bluetoothDisconected);
        imgBluetoothError = (ImageView)findViewById(R.id.bluetoothError);
        imgBluetoothError.setVisibility(View.INVISIBLE);
        mDisplayBTStatus = (TextView) findViewById(R.id.btth_status);

        imgLedOn = (ImageView)findViewById(R.id.imgLedOn);
        imgLedOn.setVisibility(View.GONE);
        imgLedOff = (ImageView)findViewById(R.id.imgLedOff);
        imgLedOff.setVisibility(View.GONE);
        imgLedUnknown = (ImageView)findViewById(R.id.imgLedUnknow);

        mLedOn= (TextView) findViewById(R.id.LEDon);
        mLedOff= (TextView) findViewById(R.id.LEDoff);
        mLedOn.setText("Unknown");
        mLedOff.setText("Unknown");
        mLeDdimming = (TextView) findViewById(R.id.LEDdimmingText);
        mLeDdimming.setText("Unknown");

        ledBrightness=(SeekBar)findViewById(R.id.ledBrightness);
        ledBrightness.setMax(100);
        ledBrightness.setProgress(0);
        ledBrightnessText=(TextView) findViewById(R.id.ledBrightnessText);
        ledBrightnessText.setText("LED brightness: " + ledBrightness.getProgress() + "%" );
        seekBarListenerInitialization();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        dateButton = (Button) findViewById(R.id.NewDate);
        timeButton = (Button) findViewById(R.id.NewTime);
        timeLedOnButton = (Button) findViewById(R.id.ledOn);
        timeLedOffButton = (Button) findViewById(R.id.ledOff);
        updateTimeDateButton = (Button) findViewById(R.id.updateTimeDate);
        updateTimeDateButton.setEnabled(false);
        dimmingButton = (Button) findViewById(R.id.setDimming);
        updateLedButton = (Button) findViewById(R.id.updateLED);
        updateLedButton.setEnabled(false);
        scheduleButton = (Button) findViewById(R.id.schedule);
        unscheduleButton = (Button) findViewById(R.id.unschedule);

        notificationManager = NotificationManagerCompat.from(this);
    }

    private void seekBarListenerInitialization()
    {
        ledBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        int progress = 0;
        // When Progress value changed.
        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            progress = progressValue;
            ledBrightnessText.setText("LED brightness: " + progressValue +"%");
            Log.i(TAG, "Changing seekbar's progress");
        }
        // When user has started a touch gesture.
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.i(TAG, "Started tracking seekbar");
        }
        // When user has finished a touch gesture
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            ledBrightnessText.setText("LED brightness: " + progress + "%");
            Log.i(TAG, "Stopped tracking seekbar");
        }
    });
    }

    private void ledIsOn()
    {
        imgLedOff.setVisibility(View.GONE);
        imgLedUnknown.setVisibility(View.GONE);
        imgLedOn.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
    private void ledIsOff()
    {
        imgLedOn.setVisibility(View.GONE);
        imgLedUnknown.setVisibility(View.GONE);
        imgLedOff.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void checkBluetoothConnection(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            mDisplayBTStatus.setText("Device has not bluetooth.");
            imgBluetoothDisconnected.setVisibility(View.INVISIBLE);
            imgBluetoothError.setVisibility(View.VISIBLE);

        }
        if(!mBluetoothAdapter.isEnabled()){
            mDisplayBTStatus.setText("Enable Bluetooth.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
        }
        else{
            mDisplayBTStatus.setText("Bluetooth on");
            imgBluetoothDisconnected.setVisibility(View.INVISIBLE);
            imgBluetoothConnected.setVisibility(View.VISIBLE);
            startBluetoothConnection();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                // Get the Bluetooth adapter status
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        mDisplayBTStatus.setText("Bluetooth off");
                        imgBluetoothDisconnected.setVisibility(View.VISIBLE);
                        imgBluetoothConnected.setVisibility(View.INVISIBLE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mDisplayBTStatus.setText("Turning Bluetooth off...");
                        imgBluetoothDisconnected.setVisibility(View.VISIBLE);
                        imgBluetoothConnected.setVisibility(View.INVISIBLE);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        mDisplayBTStatus.setText("Bluetooth on");
                        imgBluetoothDisconnected.setVisibility(View.INVISIBLE);
                        imgBluetoothConnected.setVisibility(View.VISIBLE);
                        startBluetoothConnection();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        mDisplayBTStatus.setText("Turning Bluetooth on...");
                        imgBluetoothDisconnected.setVisibility(View.INVISIBLE);
                        imgBluetoothConnected.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }
    };

    public void startBluetoothConnection() {
        progressBar.setVisibility(View.VISIBLE);
        mDisplayBTStatus.setText("Starting connection");
        bluetoothConnection = new BluetoothConnection(NANOTANK.address); //
        bluetoothConnection.start();
        /*
        Intent intent = new Intent(this, BluetoothConnection.class);
        intent.putExtra(BluetoothConnection.EXTRA_MAC_ADDRESS, deviceAddress.NANOTANK.address);
        startActivity(intent);
         */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothConnection.cancel();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (bluetoothConnection != null){
            bluetoothConnection.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
    private void startSchedule() {
        Log.d(TAG, "Job scheduling started");

        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pIntent);

        Log.d(TAG, "Job scheduled");
    }

    private void cancelSchedule() {
        Log.d(TAG, "Job cancel started");

        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);

        Log.d(TAG, "Job canceled");
    }

}
