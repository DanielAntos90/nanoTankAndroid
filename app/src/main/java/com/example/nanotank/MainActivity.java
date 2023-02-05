package com.example.nanotank;

import static com.example.nanotank.bluetooth.deviceAddress.NANOTANK;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.example.nanotank.bluetooth.BluetoothConnection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="MainActivity";
    protected TextView textDate, textTime,mDisplayBTStatus,ledBrightnessText,mLedOn,mLedOff,
            mLeDdimming;
    private Button dateButton,timeButton,timeLedOnButton,timeLedOffButton,updateTimeDateButton,
            dimmingButton, updateLedButton,scheduleButton,unscheduleButton;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private TimePickerDialog.OnTimeSetListener mTimeSetListener;
    protected ImageView imgBluetoothConnected,imgBluetoothDisconnected,imgBluetoothError,
            imgLedOn,imgLedOff,imgLedUnknown;
    private int idTimeButton=0;

    private ProgressBar progressBar;
    public static Handler handler;
    private SeekBar ledBrightness;

    private BluetoothConnection bluetoothConnection;

    public final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    public final static int BLUETOOTH_MESSAGE = 2; // used in bluetooth handler to identify message update
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

        new Thread(new Runnable() {
            public void run() {
                handler = getBluetoothHandler();
            }
        }).start();



        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new TimePickerFragment(textTime);
                newFragment.show(getFragmentManager(), "timePicker");
            }
        });

        timeLedOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new TimePickerFragment(mLedOn);
                newFragment.show(getFragmentManager(), "timePickerLedOn");
            }
        });

        timeLedOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new TimePickerFragment(mLedOff);
                newFragment.show(getFragmentManager(), "timePickerLedOff");
            }
        });

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new DatePickerFragment(textDate);
                newFragment.show(getFragmentManager(), "datePicker");
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

        startBluetoothConnection();

    }

    @SuppressLint("HandlerLeak")
    @NonNull
    private Handler getBluetoothHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String message = "";
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 0:
                                setBluetoothError();

                                switch (msg.arg2) {
                                    case 0: message = "Cannot connect to device"; break;
                                    case 1: message = "Could not close the client socket"; break;
                                    case 2: message = "Socket's create() method failed"; break;
                                    case 3: message = "Device has not bluetooth"; break;
                                    case 4: message = "Connection closed"; break;
                                }
                                break;

                            case 1:
                                setBluetoothOk();

                                switch (msg.arg2) {
                                    case 0: message = "Connection established"; break;
                                    case 1: message = "Communication established";
                                            bluetoothConnection.send("inputs"); break;
                                }
                                break;

                        }
                        break;

                    case BLUETOOTH_MESSAGE: {
                        String arduinoMsg = msg.obj.toString();
                        if (arduinoMsg.contains("ArduinoOutputs")) {
                            try {
                                String[] arduinoMsgList = arduinoMsg.split(";");
                                textTime.setText(arduinoMsgList[1]);
                                textDate.setText(arduinoMsgList[2]);
                                mLedOn.setText(arduinoMsgList[3]);
                                mLedOff.setText(arduinoMsgList[4]);
                                mLeDdimming.setText(arduinoMsgList[5] + " min.");
                                ledBrightness.setProgress(Integer.parseInt(arduinoMsgList[6]));

                                if (!updateTimeDateButton.isEnabled()) {
                                    updateTimeDateButton.setEnabled(true);
                                    updateLedButton.setEnabled(true);
                                }

                                switch (arduinoMsgList[7]) {
                                    case "led on": ledIsOn(); break;
                                    case "led off": ledIsOff(); break;
                                }
                                message = "Waiting for action";
                            } catch (Exception exception) {
                                message = "Unable to obtain data";
                            }
                        } else {
                            switch (arduinoMsg.toLowerCase()) {
                                case "led on": ledIsOn(); message = "Waiting for action"; break;
                                case "led off": ledIsOff(); message = "Waiting for action"; break;
                                case "inputs": message = "Request for Nanotank";
                                               progressBar.setVisibility(View.VISIBLE); break;
                                case "message error":
                                    setBluetoothError();
                                    message ="Unable to send message";
                                    break;
                                case "stream listening":
                                    setBluetoothError();
                                    message ="Stream listening error";
                                    break;
                                case "time changed":
                                    message ="Time changed";
                                    progressBar.setVisibility(View.GONE);
                                    break;
                                case "light changed":
                                    message ="Light changed";
                                    progressBar.setVisibility(View.GONE);
                                    break;
                            }
                            break;
                        }
                    }
                    break;
                }
                mDisplayBTStatus.setText(message);
            }
        };
    }
    private void setBluetoothError() {
        progressBar.setVisibility(View.GONE);
        imgBluetoothConnected.setVisibility(View.GONE);
        imgBluetoothDisconnected.setVisibility(View.GONE);
        imgBluetoothError.setVisibility(View.VISIBLE);
    }
    private void setBluetoothOk(){
        progressBar.setVisibility(View.GONE);
        imgBluetoothError.setVisibility(View.GONE);
        imgBluetoothConnected.setVisibility(View.GONE);
        imgBluetoothDisconnected.setVisibility(View.VISIBLE);

    }

    private void layoutInitialization() {
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

    private void BluetoothOff(String text) {
        mDisplayBTStatus.setText(text);
        imgBluetoothDisconnected.setVisibility(View.VISIBLE);
        imgBluetoothConnected.setVisibility(View.INVISIBLE);
    }

    private void BluetoothOn(String text) {
        mDisplayBTStatus.setText(text);
        imgBluetoothDisconnected.setVisibility(View.INVISIBLE);
        imgBluetoothConnected.setVisibility(View.VISIBLE);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF: BluetoothOff("Bluetooth off"); break;
                    case BluetoothAdapter.STATE_TURNING_OFF: BluetoothOff("Turning Bluetooth off..."); break;
                    case BluetoothAdapter.STATE_ON: BluetoothOn("Bluetooth on"); startBluetoothConnection(); break;
                    case BluetoothAdapter.STATE_TURNING_ON:  BluetoothOn("Turning Bluetooth on..."); break;
                }
            }
        }
    };

    public void startBluetoothConnection() {
        progressBar.setVisibility(View.VISIBLE);
        mDisplayBTStatus.setText("Starting connection");
        bluetoothConnection = new BluetoothConnection(NANOTANK.address, getApplicationContext());
        bluetoothConnection.start();
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
