package se.m76.mittapi;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.android.gms.location.LocationRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import se.m76.mittapi.models.Stars;
import se.m76.mittapi.models.Trav;
import se.m76.mittapi.models.Travs;

import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        LocationProvider.LocationCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    //private LocationRequest mLocationRequest;
    private LocationProvider mLocationProvider;

    private MittApiService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Hej hopp nu kör vi");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Startar ...");

        try {
            // Loading map
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }

        mLocationProvider = new LocationProvider(this, this);

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://mittapi-158221.appspot.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();


        //MittApiService service = retrofit.create(MittApiService.class);
        service = retrofit.create(MittApiService.class);

        Call<Stars> call = service.listStars();
        call.enqueue(new Callback<Stars>() {
            @Override
            public void onResponse(Call<Stars> call, Response<Stars> response) {
                int statusCode = response.code();
                Log.i(TAG, "Fick data?");
                if(response.body()!=null){
                Log.i(TAG, String.valueOf(response.body()));
                Log.i(TAG, " " + response.body().getResult().size());
                Log.i(TAG, "Namn:   " + response.body().getResult().get(0).getName());}
                else
                {
                    Log.i(TAG,"Nej det blidee inget data");
                }
            }

            @Override
            public void onFailure(Call<Stars> call, Throwable t) {
                Log.e(TAG, "Gick inte det!!", t);
            }
        });

    }

    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initilizeMap() {

        MapFragment mapFrag = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);

        LatLng maggan = new LatLng(59.282477, 18.082992);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(maggan, 13));

        map.addMarker(new MarkerOptions()
                .title("Magganparken")
                .snippet("En park i Enskede")
                .position(maggan));


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            //Toast.makeText(this, " Permission problem! ", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, " Tack för att jag fick COARSE location ", Toast.LENGTH_LONG).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case 2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, " Tack för att jag fick FINE location ", Toast.LENGTH_LONG).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
        Log.i(TAG, " en location har kommit    --------------------------------------------");

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        googleMap.addMarker(options);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
        mLocationProvider.connect();
    }

    @Override
    public void onMapClick(LatLng point) {
        Log.i(TAG,"En click");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));

        Toast.makeText(getApplicationContext(),"En click",
                Toast.LENGTH_LONG).show();


    }

    @Override
    public void onMapLongClick(final LatLng point) {
    Log.i(TAG,"En long click");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));

        Toast.makeText(getApplicationContext(), "En long click",
                Toast.LENGTH_LONG).show();

        new AlertDialog.Builder(this)
                .setTitle("Crate new Trav")
                .setMessage("Are you sure you want to create it?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "KLICkade aj!");
                        doCreateATrav(point);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void doCreateATrav(LatLng LL){
        // Skapa en trav objekt
        Log.i(TAG, "Ska skapa en dårå");

        Travs travs = new Travs();
        Trav trav = new Trav();
        trav.setPos(LL);
        travs.addTrav(trav);
        Log.i(TAG,"So far......................");

        Call<Travs> call = service.newTrav(travs);
        call.enqueue(new Callback<Travs>() {
            @Override
            public void onResponse(Call<Travs> call, Response<Travs> response) {
                int statusCode = response.code();
                Log.i(TAG, "Fick data?");
                if(response.body()!=null){
                    Log.i(TAG, String.valueOf(response.body()));
                    Log.i(TAG, " " + response.body().getResult().size());
                    Log.i(TAG, "Id:   " + response.body().getResult().get(0).getId());}
                else
                {
                    Log.i(TAG,"Nej bbbbbbbbbbbbbbbb det blidee inget data");
                }
            }

            @Override
            public void onFailure(Call<Travs> call, Throwable t) {
                Log.e(TAG, "Gick inte det!!", t);
            }
        });
    }

}

