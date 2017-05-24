package se.m76.mittapi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jakob on 2017-04-30.
 */

public class Maps implements
        OnMapReadyCallback ,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener {
    private static final String TAG = Maps.class.getSimpleName();

    public interface MapsCallback {
        void handleSomeThing();
    }

    private GoogleMap googleMap;
    private MapsCallback mMapsCallback;
    private Context mContext;
    private MainActivity mMainActivity;
    private HashStuff mHashStuff;

    private boolean firstPos = false;
    private List<Marker> markerList;

    public Maps(Context context) {
        mMapsCallback = (MapsCallback) context;
        mContext = context;
        mMainActivity = (MainActivity) context;

        markerList = new ArrayList<>();
    }

    public void setHashStuff(HashStuff hashstuff){
        mHashStuff = hashstuff;
    }

    public void mapOnResume(){
        try {
            // Loading map
            initilizeMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initilizeMap() {
        // Detta går nog att göra snyggare..
        MainActivity hepp = (MainActivity) mContext;
        MapFragment mapFrag = (MapFragment) hepp.getFragmentManager()
                .findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
    }

    float oldZoom = 0;
    @Override
    public void onCameraIdle() {
        // vet inte om jag använder denna.

        LatLng mPosition = googleMap.getCameraPosition().target;
        float mZoom = googleMap.getCameraPosition().zoom;
        //private float currentZoom = -1;
        Log.i(TAG,"Got OnCameraIdle zoom:" + mZoom);
        if(mZoom < 14 && oldZoom >= 14){
            hideAllPoints();
        }

        if(mZoom > 14 && oldZoom <= 14){
            showAllPoints();
        }
        oldZoom = mZoom;
    }

    @Override
    public void onCameraMove() {
        // get camerapos and zoom and tell hash:
        LatLng mPosition = googleMap.getCameraPosition().target;
        float mZoom = googleMap.getCameraPosition().zoom;

        LatLngBounds curScreen = googleMap.getProjection()
                .getVisibleRegion().latLngBounds;

        mHashStuff.setCamera(curScreen);
    }

    public void hideAllPoints(){
        for(Marker m : markerList){
            m.setVisible(false);
        }
    }
    public void showAllPoints(){
        for(Marker m : markerList){
            m.setVisible(true);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i(TAG,"En long click");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        Toast.makeText(mContext.getApplicationContext(), "En long click",
                Toast.LENGTH_LONG).show();

        new AlertDialog.Builder(mContext)
                .setTitle("Crate new Trav")
                .setMessage("Are you sure you want to create it?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "Svarade ja!");
                        // doCreateATrav(point);
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

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i(TAG,"Klick!");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        Location loc = new Location("Whut");
        loc.setLongitude(latLng.longitude);
        loc.setLatitude(latLng.latitude);
        mHashStuff.updateListAtPos(loc);
        // getTravsAtPos(latLng);


        Toast.makeText(mContext.getApplicationContext(),"En click",
                Toast.LENGTH_LONG).show();
    }

    public void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        if(!firstPos) {
            firstPos = true;
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("First Pos!");
            Marker marker = googleMap.addMarker(options);
            markerList.add(marker);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,13));
        }

        //markerList.get(0).remove();
        //Log.i(TAG, "Size markerLIst: " + markerList.size());
        // bara för test:
        mMapsCallback.handleSomeThing();
    }

    public void updateListOnMap(List<LatLng> lista){
        Log.i(TAG, "updatelist " + lista.size() + lista.get(0).latitude + lista.get(0).longitude);
        for(int i=0, l = lista.size();i<l;i++) {
            MarkerOptions options = new MarkerOptions()
                    .position(lista.get(i))
                    .title("Lista"+i);
            Marker m = googleMap.addMarker(options);
            markerList.add(m);
        }
    }

    public void gotLocation(){
        // när köra detta?
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnCameraMoveListener(this);

        if(mMainActivity.mGps) {
            gotLocation();
        }

        mMainActivity.mMap = true;

//        LatLng maggan = new LatLng(59.282477, 18.082992);
//
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(maggan, 13));
//
//        map.addMarker(new MarkerOptions()
//                .title("Magganparken")
//                .snippet("En park i Enskede")
//                .position(maggan));


    } // OnMapReady



}
