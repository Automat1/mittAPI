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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ch.hsr.geohash.GeoHash;
import se.m76.mittapi.models.AddBallPair;
import se.m76.mittapi.models.AddRemoveLists;
import se.m76.mittapi.models.Ball;
import se.m76.mittapi.models.Ufo;

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
    Integer timeAdder;

    private boolean firstPos = false;

    // Hours, Balls
    private HashMap<String, HashSet<Ball>> ballList;
    private HashSet<Ufo> ufoList;
    private List<Ball> ballsInView;
    // private AddRemoveLists addRemoveList;

    public Maps(Context context) {
        mMapsCallback = (MapsCallback) context;
        mContext = context;
        mMainActivity = (MainActivity) context;
        ballList = new HashMap<>();
        ufoList = new HashSet<>();
        ballsInView = new ArrayList<>();
        timeAdder = new Integer(0);
    }

    public void setHashStuff(HashStuff hashstuff) {
        mHashStuff = hashstuff;
    }

    public void mapOnResume() {
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
    //float mZoom = 0;

    private void checkZoomLevel(){
        float mZoom = googleMap.getCameraPosition().zoom;
        if(mZoom>14 && (zoomedIn == false)) {
            zoomedIn = true;
            Log.i(TAG,"Zoomed In");
        }
        if(mZoom<=14 && zoomedIn == true) {
            zoomedIn = false;
            Log.i(TAG,"Zoomed Out");
            ballList = new HashMap<>();
        }
    }

    @Override
    public void onCameraIdle() {
        Log.i(TAG, "Camera Idle");
        checkZoomLevel();
        if(zoomedIn)
            upDateHashList();
    }

    public void hideAllPoints() {
       /* for(Ball b : ballList){
            b.marker.setVisible(false);
        }*/
    }

    public void showAllPoints() {
        /*for(Ball b : ballList){
            b.marker.setVisible(true);
        }*/
    }

    long told;

    @Override
    public void onCameraMove() {
        checkZoomLevel();
        long t = SystemClock.uptimeMillis(); // bör vara nåt med nanos?
        long del = t - told;
        //Log.i(TAG, " del: " + del + " told " + told);
        if (del < 1000) return;
        told = t;

        Log.i(TAG, "1000ms i move!");
        if(zoomedIn)
            upDateHashList();
    }

    boolean zoomedIn;

    private void upDateHashList() {

        // get curscrren to see wish hashes are visible
        LatLngBounds curScreen = googleMap.getProjection()
                .getVisibleRegion().latLngBounds;

        new upDateBallListInBackground().execute(curScreen);

    }

    public void setBallList(HashMap<String, HashSet<Ball>> hm) {
        if(zoomedIn) ballList = hm;
    }

    public HashSet<Ufo> getListOfUfos() {
        return ufoList;
    }

    public void updateListOnMap() {

        long timeNow = System.currentTimeMillis() / 1000;
        String time = mHashStuff.getCurrentTimeStamp(timeNow + timeAdder);

        Drawable circleDrawable;

        Iterator<Ball> it = ballsInView.iterator();
        while(it.hasNext()){
            Ball b = it.next();
            Boolean remove = false;
            if(ballList.get(time)==null) remove = true;
            else if(!ballList.get(time).contains(b)) remove = true;
            if(remove) {
                b.fade -= 0.1f;
                //Log.i(TAG,"fading" + b.fade);
                if(b.fade <= 0) {
                    b.marker.remove();
                    it.remove();
                } else {
                    b.marker.setAlpha(b.fade);
                }
            }
        }

        if (ballList.get(time) != null)
            for (Ball b : ballList.get(time)) {
                if (!ballsInView.contains(b)) {
                    GeoHash gh = GeoHash.fromGeohashString(b.geoHash);
                    LatLng ll = new LatLng(gh.getPoint().getLatitude(), gh.getPoint().getLongitude());
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
                    b.marker = googleMap.addMarker(options);
                    b.fade = 1.0f;
                    ballsInView.add(b);
                   // Log.i(TAG, "Added 1 ball. Total: " + ballsInView.size());

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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(maggan, 13));

        //upDateHashList();

        // Testa Polylines:
        // Instantiates a new Polyline object and adds points to define a rectangle
        PolylineOptions rectOptions = new PolylineOptions()
                .add(new LatLng(59.28, 18.10))
                .add(new LatLng(59.281, 18.08));

        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(59.281, 18.08))
                .radius(75); // In meters

// Get back the mutable Circle
        Circle circle = map.addCircle(circleOptions);

// Get back the mutable Polyline
        Polyline polyline = map.addPolyline(rectOptions);


        // Testa overlay:
        LatLng maggan2 = new LatLng(59.28, 18.10);

        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
                .position(maggan2, 860f, 650f);

        // Add an overlay to the map, retaining a handle to the GroundOverlay object.
        GroundOverlay imageOverlay = map.addGroundOverlay(groundOverlayOptions);
    } // OnMapReady


    private class upDateBallListInBackground extends AsyncTask<LatLngBounds, Void, Void> {

        @Override
        protected void onPreExecute(){
            //updateList = false;
            Log.i(TAG, "start update Hashes");
        }

        @Override
        protected Void doInBackground(LatLngBounds... params) {
            mHashStuff.updateHashes(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Log.i(TAG, "finished Update Hashes");
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

      public void setTime(Integer time){
          timeAdder = time*60*60; // convert hours to seconds
      }


}
