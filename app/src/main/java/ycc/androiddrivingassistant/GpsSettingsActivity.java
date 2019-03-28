package ycc.androiddrivingassistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import ycc.androiddrivingassistant.ui.ScreenInterface;

public class GpsSettingsActivity extends Activity implements ScreenInterface {
    private static final String TAG = "GpsSettingsAct";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Switch gpsSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_settings);

        sharedPreferences = getSharedPreferences("Prefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gpsSwitch = (Switch) findViewById(R.id.gps_switch);
        final boolean status = sharedPreferences.getBoolean("gps_enabled", false);
        gpsSwitch.setChecked(status);
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (status) {
                    editor.putBoolean("gps_enabled", false);
                } else {
                    editor.putBoolean("gps_enabled", true);
                }
                editor.apply();
            }
        });
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
