package com.example.nanotank.bluetooth;

class BluetoothHandler {/*
    public static Handler mHandler;

    @SuppressLint("HandlerLeak")
    public void run() {
        Looper.prepare();
        mHandler = getHandler();
        Looper.loop();
    }

    @NonNull
    @SuppressLint("HandlerLeak")
    private Handler getHandler() {
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
    }*/
}