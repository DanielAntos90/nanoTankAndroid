package com.example.nanotank.picker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.nanotank.R;

@SuppressLint("ValidFragment")
public class DimmingPickerFragment extends DialogFragment {

    private TextView mResultText;
    private NumberPicker np;

    private SetButton b1;
    private SetButton b2;
    private Dialog dialog;

    private class SetButton {
        Button button;
        private boolean isOkButton = false;
        private SetButton(Button button, boolean isOkButton) {
           this.button = button;
           this.isOkButton = isOkButton;
           this.button.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   if (isOkButton){
                       mResultText.setText(String.valueOf(np.getValue()+" min."));
                   }
                   dialog.dismiss();
               }
           });
        }
    }


    public DimmingPickerFragment(TextView textView) {
        mResultText = textView;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new Dialog(getActivity());
        dialog.setTitle("Number Picker");
        dialog.setContentView(R.layout.dialog);
        b1 = new SetButton((Button) dialog.findViewById(R.id.button1), true);
        b2 = new SetButton((Button) dialog.findViewById(R.id.button2), false);

        np = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
        np.setMaxValue(60);
        np.setMinValue(0);
        np.setWrapSelectorWheel(false);

        try {
            String text= mResultText.getText().toString();
            np.setValue(Integer.parseInt(text.substring(0,text.length()-5)));
        } catch ( IllegalArgumentException exception) {
            np.setValue(30);
        }
        return dialog;
    }

}