package ycc.androiddrivingassistant;

import android.util.Log;
import android.widget.ImageView;

public class UiRunnable implements Runnable {
    private int signVal;
    private ImageView signImageView;

    UiRunnable() {
        setSignVal(0);
    }

    UiRunnable(int val, ImageView imageView) {
        setSignVal(val);
        setSignImageView(imageView);
    }

    public int getSignVal() {
        return signVal;
    }

    public void setSignVal(int signVal) {
        this.signVal = signVal;
    }

    public void setSignImageView(ImageView signImageView) {
        this.signImageView = signImageView;
    }

    @Override
    public void run() {
        try {
            switch (signVal) {
                case 60:
                    signImageView.setImageResource(R.drawable.sign60);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 60 --------------------");
                    break;
                case 80:
                    signImageView.setImageResource(R.drawable.sign80);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 80 --------------------");
                    break;
                case 100:
                    signImageView.setImageResource(R.drawable.sign100);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 100 --------------------");
                    break;
                case 50:
                    signImageView.setImageResource(R.drawable.sign50);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 50 --------------------");
                    break;
                case 120:
                    signImageView.setImageResource(R.drawable.sign120);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 120 --------------------");
                    break;
                case 30:
                    signImageView.setImageResource(R.drawable.sign30);
                    Log.i("ASDASDASDAS", "run: -------------------- SET 30 --------------------");
                    break;
                default:
                    signImageView.setImageResource(R.drawable.sign_stop);
                    Log.i("ASDASDASDAS", "run: -------------------- SET STOP --------------------");
                    break;
            }
        } catch (Exception e) {
            //Log.e(TAG, "onCreate: " + e);
        }
    }
}
