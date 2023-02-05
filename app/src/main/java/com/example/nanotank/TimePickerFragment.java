package com.example.nanotank;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint("ValidFragment")
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    TextView mResultText;

    public TimePickerFragment(TextView textView) {
        mResultText = textView;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar cal = Calendar.getInstance();
        int hour,minute=0;
        try {
            hour = Integer.parseInt(mResultText.toString().split(":")[0]);
            minute = Integer.parseInt(mResultText.toString().split(":")[1]);
        } catch ( IllegalArgumentException exception) {
            hour = cal.get(Calendar.HOUR_OF_DAY);
            minute = cal.get(Calendar.MINUTE);
        }

        Dialog dialog = new TimePickerDialog(getActivity(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                this, hour, minute,true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    public void onTimeSet(TimePicker view, int hour, int minute) {
        String time = String.format(Locale.GERMANY, "%d:%02d", hour, minute);
        mResultText.setText(time);
    }
}