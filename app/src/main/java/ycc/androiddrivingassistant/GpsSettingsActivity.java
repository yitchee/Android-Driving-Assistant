package ycc.androiddrivingassistant;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RadioGroup;

import ycc.androiddrivingassistant.ui.ScreenInterface;

public class GpsSettingsActivity extends Activity implements ScreenInterface {
    private static final String TAG = "GpsSettingsAct";
    SharedPreferences sharedPreferences;
    RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_settings);

        sharedPreferences = getSharedPreferences("Prefs", Context.MODE_PRIVATE);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int)(width*0.95), (int)(height*.9));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreen();
    }

    public void closeActivity(View v) {
        finish();
    }

    @Override
    public void setFullscreen() {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
