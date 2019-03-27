package ycc.androiddrivingassistant.ui;

import android.util.Log;
import android.widget.TextView;

/* Class used to update the TextView for vehicles current speed */
public class SpeedUiRunnable implements Runnable {
    private static final String TAG = "SpeedUiRunnable";
    private TextView speedTextView;
    private double speedVal;

    public SpeedUiRunnable() {
        speedVal = 0;
    }

    public SpeedUiRunnable(TextView textView) {
        this.speedTextView = textView;
    }

    @Override
    public void run() {
        try {
            speedTextView.setText(String.format("%skm/hr", speedVal));
        } catch (Exception e) {
            Log.e(TAG, "run: ", e);
        }
    }

    public double getSpeedVal() {
        return speedVal;
    }

    public void setSpeedVal(double speedVal) {
        this.speedVal = speedVal;
    }

    public void setSpeedTextView(TextView speedTextView) {
        this.speedTextView = speedTextView;
    }

    public TextView getSpeedTextView() {
        return speedTextView;
    }
}
