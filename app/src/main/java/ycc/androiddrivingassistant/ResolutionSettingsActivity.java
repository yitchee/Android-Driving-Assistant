package ycc.androiddrivingassistant;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import ycc.androiddrivingassistant.ui.ScreenInterface;

public class ResolutionSettingsActivity extends Activity implements ScreenInterface {
    private static final String TAG = "ResolutionSettingsAct";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolution_settings);

        sharedPreferences = getSharedPreferences("Prefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.apply();

        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        getSupportedResolutions();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                RadioButton rb = (RadioButton)findViewById(checkedId);
                String resolution = rb.getText().toString();
                String[] resList = resolution.split("\\s+");
                editor.putInt("res_width", Integer.parseInt(resList[0]));
                editor.putInt("res_height", Integer.parseInt(resList[2]));
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

    private void getSupportedResolutions() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        SharedPreferences sharedPreferences = getSharedPreferences("Prefs", Context.MODE_PRIVATE);
        int imgHeight, imgWidth;
        try {
            assert manager != null;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            for (android.util.Size size : map.getOutputSizes(SurfaceTexture.class)) {
                imgHeight = size.getHeight();
                imgWidth = size.getWidth();
                if (imgWidth <= 1920) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setId(View.generateViewId());
                    radioButton.setTextColor(getColor(R.color.colorWhite));
                    radioButton.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT));
                    radioButton.setText(imgWidth + " x " + imgHeight);
                    if (sharedPreferences.getInt("res_height", 0) == imgHeight && sharedPreferences.getInt("res_width", 0) == imgWidth) {
                        radioButton.setChecked(true);
                    }
                    radioGroup.addView(radioButton);
                }
            }
        }catch (Error error) {
            Log.e(TAG, "onCreate: ", error);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
