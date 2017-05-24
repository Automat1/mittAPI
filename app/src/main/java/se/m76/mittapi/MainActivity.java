package se.m76.mittapi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements
        LocationProvider.LocationCallback,
        Maps.MapsCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private LocationProvider mLocationProvider;
    private Maps mMaps;
    public HashStuff mHashStuff;
    /*private*/public ApiService apiService; // fixa så att den kan vara private igen.
    boolean mGps;
    boolean mMap = false;
    long timeOfLastListUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Startar ...");

        mMaps = new Maps(this);
        mMaps.mapOnResume();
        mHashStuff = new HashStuff(this, mMaps);
        mMaps.setHashStuff(mHashStuff);
        mLocationProvider = new LocationProvider(this, this);

        timeOfLastListUpdate = 0;

        checkAndAskForPermisssions();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                //.baseUrl("http://mittapi-158221.appspot.com/")
                .baseUrl("http://10.0.2.2:8080/") /* local devserver */
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public void checkAndAskForPermisssions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            mGps = false;
        }
        else
        {
            mGps = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, " Got FINE location ", Toast.LENGTH_LONG).show();
                    mGps = true;


                } else {
                    Toast.makeText(this, " No location - No fun ", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    } // onRequestPermissionsResult

    @Override
    public void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        if(mMap = true) {
            mMaps.handleNewLocation(location);
        }

        // Koll avstånd i stället.
        if((System.currentTimeMillis() - timeOfLastListUpdate) > 10000) {
            mHashStuff.updateListAtPos(location);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
        mLocationProvider.connect();
    }

    @Override
    public void handleSomeThing() {
        // to be done
    }
}

