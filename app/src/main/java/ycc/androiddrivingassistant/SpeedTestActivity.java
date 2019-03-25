package ycc.androiddrivingassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class SpeedTestActivity extends AppCompatActivity implements LocationListener {
    private static final String TAG = "SpeedTestActivity";
    LocationManager locationManager;
    Location prevLoc;
    long prevTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        setUpLocationServices();
    }

    private void setUpLocationServices() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        getVehicleSpeed(location);
    }

    private void getVehicleSpeed(Location curLoc) {
        List<String> providers = locationManager.getAllProviders();
        int i = 0;
        long curTime = System.currentTimeMillis();

        if (!providers.isEmpty()) {
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(providers.get(i));
            double speed, distance;
            try {
                distance = location.distanceTo(prevLoc) / 1000;
                long timeDiff = (curTime - prevTime) / 1000;
                speed = (distance / timeDiff) * 3600;

                Log.i(TAG, "getVehicleSpeed: SPEED  - " + speed);
                Log.i(TAG, "getVehicleSpeed: TIME SECS- " + timeDiff);
                Log.i(TAG, "getVehicleSpeed: DIST KM  - " + distance);

                prevLoc = location;
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "ERROR: No providers available OR location is not enabled.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "ERROR: No providers available.", Toast.LENGTH_SHORT).show();
        }
        prevTime = System.currentTimeMillis();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
