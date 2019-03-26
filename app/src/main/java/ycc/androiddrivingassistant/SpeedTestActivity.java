package ycc.androiddrivingassistant;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SpeedTestActivity extends AppCompatActivity implements LocationListener {
    private static final String TAG = "SpeedTestActivity";
    LocationManager locationManager;
    Location prevLoc = new Location("gps");
    long prevTime;
    TextView speedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        speedTextView = (TextView) findViewById(R.id.speed_text_view);
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        getVehicleSpeed(location);
    }

    private void getVehicleSpeed(Location curLoc) {
        List<String> providers = locationManager.getAllProviders();

        //check and request permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(SpeedTestActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        else {
            int i = 0;
            if (!providers.isEmpty()) {
                ActivityCompat.requestPermissions(SpeedTestActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                //get longitude and latitude is available
                Location location = locationManager.getLastKnownLocation(providers.get(i));
                long curTime = System.currentTimeMillis();
                double speed, distance;
                distance = location.distanceTo(prevLoc) / 1000;
                long timeDiff = (curTime - prevTime) / 1000;
                if (timeDiff < 1){
                    timeDiff = 1;
                }
                speed = Math.floor((distance / timeDiff) * 3600 * 10) / 10;
                prevLoc.set(location);
                speedTextView.setText(String.format("%s km/hr", speed));
                Log.i(TAG, "getVehicleSpeed: \nDIST: "+distance+"\nTIME: "+timeDiff + "\nSPEED: "+speed);
            }
            else {
                Toast.makeText(getApplicationContext(), "ERROR: No providers available.", Toast.LENGTH_SHORT).show();
            }
            prevTime = System.currentTimeMillis();
        }
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

    //stop updates to save battery
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    //restart updates when back in focus
    @Override
    protected void onResume() {
        super.onResume();

        setUpLocationServices();
    }
}
