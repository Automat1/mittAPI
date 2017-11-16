package se.m76.mittapi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.Iterator;

import ch.hsr.geohash.GeoHash;
import se.m76.mittapi.models.AddRemoveLists;
import se.m76.mittapi.models.Ball;

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

    private HashSet<Ball> ballList;

    private AddRemoveLists addRemoveList;

    public Maps(Context context) {
        mMapsCallback = (MapsCallback) context;
        mContext = context;
        mMainActivity = (MainActivity) context;
        ballList = new HashSet<Ball>();
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

//        LatLng mPosition = googleMap.getCameraPosition().target;
//        float mZoom = googleMap.getCameraPosition().zoom;
//        //private float currentZoom = -1;
//        Log.i(TAG,"Got OnCameraIdle zoom:" + mZoom);
//        if(mZoom < 14 && oldZoom >= 14){
//            hideAllPoints();
//        }
//
//        if(mZoom > 14 && oldZoom <= 14){
//            showAllPoints();
//        }
//        oldZoom = mZoom;
    }



    public void hideAllPoints(){
        for(Ball b : ballList){
            b.marker.setVisible(false);
        }
    }
    public void showAllPoints(){
        for(Ball b : ballList){
            b.marker.setVisible(true);
        }
    }


    long told;

    @Override
    public void onCameraMove() {
        long t = SystemClock.uptimeMillis(); // bör vara nåt med nanos?
        long del = t-told;
        //Log.i(TAG, " del: " + del + " told " + told);
        if(del<1000) return;
        told = t;

        Log.i(TAG, "1000ms i move!");

        upDateHashList();
    }

    boolean zoomedOut;

    private void upDateHashList(){

        // get camerapos and zoom and tell hash:
        LatLng mPosition = googleMap.getCameraPosition().target;
        float mZoom = googleMap.getCameraPosition().zoom;

        LatLngBounds curScreen = googleMap.getProjection()
                .getVisibleRegion().latLngBounds;

        if(mZoom>14) {
            //mHashStuff.setCamera(curScreen);
            new DoHashOp().execute(curScreen);
            if(zoomedOut == true) {
                zoomedOut = false;
                Log.i(TAG,"Zoom in");
            }
        }
        else{
            if(zoomedOut==false) {
                zoomedOut = true;
                Log.i(TAG,"Zoom out");
            }
        }

    }

    public void setUpdateList(boolean upd){
        updateList = upd;
    }

    public HashSet<Ball> getListOfBalls(){
        return ballList;
    }

    boolean updateList = true;

    public void updateListOnMap(){
        //if(updateList == false) return;
        // lista med de som finns, lista med de som bort och lista med de som till:
        if(updateList) {


            if (zoomedOut == false) {


                if (addRemoveList == null) return;
                if (addRemoveList.addList == null) return;
                if (addRemoveList.addList.size() == 0) return;
                Ball b = addRemoveList.addList.get(0);
                if (b != null) {

                    if (ballList.contains(b)) {
                        Log.i(TAG, "krock");
                        addRemoveList.addList.remove(0);
                        return;
                    }

                    GeoHash gh = GeoHash.fromGeohashString(b.geoHash);
                    LatLng ll = new LatLng(gh.getPoint().getLatitude(), gh.getPoint().getLongitude());

                    //Log.i(TAG, "Update: " + s + " Size: " + addRemoveList.addList.size());

                    Drawable circleDrawable;
                    switch (b.color) {
                        case 0: // yell
                            circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_yellow);
                            break;
                        case 1: //blue
                            circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_blue);
                            break;
                        case 2: //rd
                            circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_red);
                            break;
                        case 3: //green
                            circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_green);
                            break;
                        default:
                            circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_yellow);

                    }

                    BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);

                    MarkerOptions options = new MarkerOptions()
                            .position(ll)
                            .title(b.geoHash)
                            .icon(markerIcon);
                    Marker m = googleMap.addMarker(options);
                    b.marker = m;
                    ballList.add(b);
                    addRemoveList.addList.remove(0);
                }
            } else {
                if (!ballList.isEmpty()) {
                    Log.i(TAG, "ZO Ballist size : " + ballList.size());
                    Ball b = ballList.iterator().next();
                    Iterator<Ball> i = ballList.iterator();
                    b = i.next();
                    b.marker.remove();
                    i.remove();
                }
            }
        }
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
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

        LatLng maggan = new LatLng(59.282477, 18.082992);
//
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(maggan, 13));

        upDateHashList();
//
//        map.addMarker(new MarkerOptions()
//                .title("Magganparken")
//                .snippet("En park i Enskede")
//                .position(maggan));


    } // OnMapReady


    private class DoHashOp extends AsyncTask<LatLngBounds, Void, AddRemoveLists> {

        @Override
        protected void onPreExecute(){
            updateList = false;
            Log.i(TAG, "start async");
        }
        @Override
        protected AddRemoveLists doInBackground(LatLngBounds... params) {
            return mHashStuff.setCamera(params[0]);

        }

        @Override
        protected void onPostExecute(AddRemoveLists result) {
            addRemoveList = result;

            Log.i(TAG, "stop async");
            updateList = true;
        }
    }

      @Override
      public void onMapLongClick(LatLng latLng) {
//        Log.i(TAG,"En long click");
//        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//
//        Toast.makeText(mContext.getApplicationContext(), "En long click",
//                Toast.LENGTH_LONG).show();
//
//        new AlertDialog.Builder(mContext)
//                .setTitle("Crate new Trav")
//                .setMessage("Are you sure you want to create it?")
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.i(TAG, "Svarade ja!");
//                        // doCreateATrav(point);
//                    }
//                })
//                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // do nothing
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();
      }

      @Override
      public void onMapClick(LatLng latLng) {
//        Log.i(TAG,"Klick!");
//        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//        Location loc = new Location("Whut");
//        loc.setLongitude(latLng.longitude);
//        loc.setLatitude(latLng.latitude);
//        mHashStuff.updateListAtPos(loc);
//        // getTravsAtPos(latLng);
//
//
//        Toast.makeText(mContext.getApplicationContext(),"En click",
//                Toast.LENGTH_LONG).show();
//    }

//    public void handleNewLocation(Location location) {
//        Log.d(TAG, location.toString());
//
//        double currentLatitude = location.getLatitude();
//        double currentLongitude = location.getLongitude();
//        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
//
//        if(!firstPos) {
//            firstPos = true;
//            MarkerOptions options = new MarkerOptions()
//                    .position(latLng)
//                    .title("First Pos!");
//            Marker marker = googleMap.addMarker(options);
//            markerList.add(marker);
//            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,13));
//        }
//
//        //markerList.get(0).remove();
//        //Log.i(TAG, "Size markerLIst: " + markerList.size());
//        // bara för test:
//        mMapsCallback.handleSomeThing();
      }

}
