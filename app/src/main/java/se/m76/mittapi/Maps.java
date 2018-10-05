package se.m76.mittapi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import ch.hsr.geohash.GeoHash;
import se.m76.mittapi.models.AddBallPair;
import se.m76.mittapi.models.AddUfoPair;
import se.m76.mittapi.models.Ball;
import se.m76.mittapi.models.Ufo;
import se.m76.mittapi.models.updateResultLists;

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
    int timeAdderEvenHour;
    int timeAdderSeconds;
    long lastHashUpdateTime;
    long startHash;

    private boolean firstPos = false;

    // Hours, Balls
    private ConcurrentHashMap<String, Set<Ball>> ballList;
    private ConcurrentHashMap<String, Set<Ufo>> ufoList;
    private List<Ball> ballsInView;
    private List<Ufo> ufosInView;
    private List<Circle> circlesInView;
    private List<Ball>  addList;
    private List<Ball> removeList;
    public Queue<Ball> addQ;
    public Queue<Ball> removeQ;
    private Map<Long, List<Ball>> ballsOnMap;
    String lastCenterHash;

    // private AddRemoveLists addRemoveList;

    public Maps(Context context) {
        mMapsCallback = (MapsCallback) context;
        mContext = context;
        mMainActivity = (MainActivity) context;
        ballList = new ConcurrentHashMap<>();
        ufoList = new ConcurrentHashMap<>();
        ballsInView = new ArrayList<>();
        ufosInView = new ArrayList<>();
        timeAdderEvenHour = new Integer(0);
        circlesInView = new ArrayList<>();
        lastHashUpdateTime = 0;
        addList = new ArrayList<>();
        removeList = new ArrayList<>();
        addQ = new ConcurrentLinkedQueue<>();
        removeQ = new ConcurrentLinkedQueue<>();
        ballsOnMap = new HashMap<>();
        lastCenterHash = new String();
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

    public float zoomLevel = 0;
    //float mZoom = 0;

    private void checkZoomLevel(){
        float mZoom = googleMap.getCameraPosition().zoom;
        zoomLevel = mZoom;
        // 13 för bollar.
        // 10 för ufon.

        //Log.i(TAG, " Zoom is :" + mZoom);
        if(mZoom>14 && (zoomedIn == false)) {
            zoomedIn = true;
            Log.i(TAG,"Zoomed In");
        }
        if(mZoom<=14 && zoomedIn == true) {
            zoomedIn = false;
            Log.i(TAG,"Zoomed Out");
            //ballList = new HashMap<>();
        }
    }

    @Override
    public void onCameraIdle() {
        Log.i(TAG, "Camera Idle");
        checkZoomLevel();
        //if(zoomedIn)
        //    upDateHashList();
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
        //long t = SystemClock.uptimeMillis(); // bör vara nåt med nanos?
        //long del = t - told;
        //Log.i(TAG, " del: " + del + " told " + told);
        //if (del < 1000) return;
        //told = t;

        //Log.i(TAG, "1000ms i move!");
        //if(zoomedIn)
        //    upDateHashList();
    }

    boolean zoomedIn;

    AsyncTask asyncTask;

    private void upDateHashList() {
        // get curscrren to see wish hashes are visible
        LatLngBounds curScreen = googleMap.getProjection()
                .getVisibleRegion().latLngBounds;

        if(asyncTask == null ||
                (asyncTask.getStatus() != AsyncTask.Status.RUNNING && asyncTask.getStatus() != AsyncTask.Status.PENDING)){
            asyncTask = new upDateBallListInBackground().execute(curScreen);
        }
    }

    public void setBallList(List<AddBallPair> abpList) {
        //if(zoomedIn) ballList = hm;
        // ballList är listan , tid, bollar som är aktuella.
        for(AddBallPair abp : abpList){
            if(!ballList.containsKey(abp.hour)){
                // trick för att skapa hashset som är concurrentmodificaion safe
                Set<Ball> s = Collections.newSetFromMap(new ConcurrentHashMap<Ball, Boolean>());
                ballList.put(abp.hour,s);
            }
            ballList.get(abp.hour).add(abp.ball);
        }
    }

    public void setUfoList(List<AddUfoPair> aupList) {
       // if(zoomedIn) ufoList = hm;
        for(AddUfoPair aup : aupList){
            if(!ufoList.containsKey(aup.hour)){
                Set<Ufo> s = Collections.newSetFromMap(new ConcurrentHashMap<Ufo, Boolean>());
                ufoList.put(aup.hour,s);
            }
            ufoList.get(aup.hour).add(aup.ufo);
        }
    }


    String hashStringCenterOld;
    LatLng centerLL;
    public String hashStringCenter;

    public void updateListOnMap() {
        //long tidstart = System.currentTimeMillis();
        long timeNow = System.currentTimeMillis() / 1000;
        //String time = mHashStuff.getCurrentTimeStamp(timeNow + timeAdderEvenHour);
        long timeAdd = timeNow + timeAdderSeconds;
        //Drawable circleDrawable;

        // Updatera hashar om mittpunkt i view har bytt hash:
        centerLL = googleMap.getProjection().getVisibleRegion().latLngBounds.getCenter();
        hashStringCenter = GeoHash.geoHashStringWithCharacterPrecision(centerLL.latitude,centerLL.longitude,6);

        //Log.i(TAG, "Z: " + zoomLevel);

        if(!hashStringCenter.equals(hashStringCenterOld) && zoomLevel > 15){
            //lastHashUpdateTime += 60*60;
            upDateHashList();
            hashStringCenterOld = hashStringCenter;
        }


        //List<String> listOfBallHashes = new ArrayList<>();

        Iterator<Ball> it = ballsInView.iterator();
        while(it.hasNext()){
            Ball b = it.next();

            /*if(!listOfBallHashes.contains(b.geoHash)) {
                b.marker.remove();
                it.remove();
                continue;
            }*/

            //if(b.marker == null) addBallToView(b);

            if(zoomLevel > 13 && timeAdd > b.timeStart && timeAdd < b.timeEnd){
                b.marker.setVisible(true);
            }
            else{
                b.marker.setVisible(false);
            }
        }

        if(addList != null && !addList.isEmpty()) {
            Ball b = addList.remove(addList.size() - 1);
            addBallToView(b);
            ballsInView.add(b);
        }else {
            if (removeList != null && !removeList.isEmpty()) {
                Ball b = removeList.remove(removeList.size() - 1);
                b.marker.remove();
                ballsInView.remove(b);
            }
        }

        Ball b = addQ.poll();
        while(b!=null){
            addBallToView(b);
            ballsInView.add(b);
            b = addQ.poll();
        }

        b = removeQ.poll();
        while(b!=null){
            b.marker.remove();
            ballsInView.remove(b);
        }



        mMainActivity.debugTxt2.setText("Balls: " + ballsInView.size());

        /*
        Iterator<Ball> it = ballsInView.iterator();
        while(it.hasNext()){
            Ball b = it.next();
            Boolean remove = false;
            if(zoomLevel<13) remove = true;
            if(!googleMap.getProjection().getVisibleRegion().latLngBounds.contains(b.latLng)) remove = true;
            if(ballList.get(time)==null) remove = true;
            else{
                if(b.time > timeNow || b.time < (timeNow - 60*60)) remove = true;
            }
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

        // 2. add new balls
        // ( what if readding a fading ball???)
        if (ballList.get(time) != null && zoomLevel > 13) {
            for (Ball b : ballList.get(time)) {
                if (!ballsInView.contains(b) && googleMap.getProjection().getVisibleRegion().latLngBounds.contains(b.latLng) &&
                        (b.time < timeNow && b.time < (timeNow + 60*60))) {
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
        // 3. go through list of ufos and move them and remove them

        Ufo u;
        // fada ut circlar:
        Iterator<Ufo> uit = ufosInView.iterator();
        while(uit.hasNext()){
            u = uit.next();
            Boolean remove = false;
            if(ufoList.get(time)==null) remove = true;
            else if(!ufoList.get(time).contains(u)) remove = true;
            if(remove) {
                //if((u.circle.getFillColor() & 0xF0000000)!=0){
                //    u.circle.setFillColor(u.circle.getFillColor()-0x08000000);
                //}   else {
                    u.circle.remove();
                    uit.remove();
                //}
            } else // if not remove then move.
            {
                // its in list, move it to pos.
                // 1. calculate the pos.
                double dir = u.direction;


                double dst = 10 * (timeAdd - u.time);  //   ( 10 m/s , 36 km / h)
                // gör ett hopp 15 s  = 150 m i taget.
                // gör ett hopp 1 s = 15 m i taget

                // test mjukare förflyttning:
                //if(dst>150) dst = 15;
                //else if (dst>15) dst = 1;
                //Log.i(TAG," Move: timeadd: " + timeAdd + " uu.time: " + u.time + " dst: " + dst);
                GeoHash gh = GeoHash.fromGeohashString(u.geoHash);
                LatLng ll = new LatLng(gh.getPoint().getLatitude(), gh.getPoint().getLongitude());
                ll = locAtBearingDistance(ll,dir,dst);

                //Log.i(TAG, "Moving 1 ufo");
                if(u.circle!=null)
                    u.circle.setCenter(ll);
                else
                    Log.i(TAG, "Null circel!");

                // howto calc new pos
            }

        }


        // 4. Remove old lines

        // 5. Add new ufos and their lines
        if (ufoList.get(time) != null)
            for (Ufo uu : ufoList.get(time)) {
                // if not yet drawn, add it
                if (!ufosInView.contains(uu)) {
                    GeoHash gh = GeoHash.fromGeohashString(uu.geoHash);
                    LatLng ll = new LatLng(gh.getPoint().getLatitude(), gh.getPoint().getLongitude());
                    int color;
                    switch (uu.color) {
                        case 0: // yell
                            color = Color.YELLOW;
                            break;
                        case 1: //blue
                            color = Color.BLUE;
                            break;
                        case 2: //rd
                            color = Color.RED;
                            break;
                        case 3: //green
                            color = Color.GREEN;
                            break;
                        default:
                            color = Color.GREEN;
                    }

                    //BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);

                    // Instantiates a new CircleOptions object and defines the center and radius
                    CircleOptions circleOptions = new CircleOptions()
                            .center(ll)
                            .radius(500)
                            .fillColor(color); // In meters

                    // Get back the mutable Circle
                    Circle circle = googleMap.addCircle(circleOptions);
                    //Log.i(TAG, "Added circle at " + ll.latitude + " " +ll.longitude);
                    //Todo bara sätt en circel en gång förstås.

                    uu.circle = circle;
                    //circlesInView.add(circle);

                    ufosInView.add(uu);
                    //u.marker = googleMap.addMarker(options);
                    //b.fade = 1.0f;
                    //ballsInView.add(b);
                    //Log.i(TAG, "Add 1 Ufo. Total: " + ufosInView.size() + "lat" + ll.latitude + " lon " + ll.longitude);


                }
            }
            */
            //Log.i(TAG,"Update map tog: " + (System.currentTimeMillis() - tidstart) + " ms ");
    }

    private void addBallToView(Ball b){
        Drawable circleDrawable;
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
                .icon(markerIcon)
                .visible(false);
        b.marker = googleMap.addMarker(options);
        b.fade = 1.0f;
    }


    // test:  var p1 = new LatLon(51.4778, -0.0015);
    //        var p2 = p1.destinationPoint(7794, 300.7); // 51.5135°N, 000.0983°W
    // avskrivet från Javascript exempel på ..:
    private LatLng locAtBearingDistance(LatLng pos, double brng, double dist){
        double radius = 6371e3;

        double distRad = dist / radius;
        double bearingRad = brng * Math.PI / 180;
        double latRad = pos.latitude * Math.PI / 180;
        double lonRad = pos.longitude * Math.PI / 180;

        double sinLat = Math.sin(latRad);
        double cosLat = Math.cos(latRad);

        double sinDist = Math.sin(distRad);
        double cosDist = Math.cos(distRad);

        double sinBrng = Math.sin(bearingRad);
        double cosBrng = Math.cos(bearingRad);

        double sinLat2 = sinLat * cosDist + cosLat * sinDist * cosBrng;

        double lat2 = Math.asin(sinLat2);

        double y = sinBrng * sinDist * cosLat;
        double x = cosDist - sinLat * sinLat2;

        double lon2 = lonRad + Math.atan2(y,x);

        LatLng ll = new LatLng(lat2 * 180 / Math.PI, (lon2 * 180 / Math.PI + 540) % 360 - 180 );

        return ll;
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
                .radius(75) // In meters
                .fillColor(0xFF00FF00);

// Get back the mutable Circle
        Circle circle = map.addCircle(circleOptions);
        circle.setFillColor(circle.getFillColor()-0xEE000000);

// Get back the mutable Polyline
        Polyline polyline = map.addPolyline(rectOptions);


        // Testa overlay:
        LatLng maggan2 = new LatLng(59.28, 18.10);

        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
                .position(maggan2, 860f, 650f);

        // Add an overlay to the map, retaining a handle to the GroundOverlay object.
        GroundOverlay imageOverlay = map.addGroundOverlay(groundOverlayOptions);


        // ----------------------------------------------------------------------------
        // Text on shape:
        Paint tPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tPaint.setColor(Color.BLUE);
        tPaint.setTextSize(50);
        tPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Canvas canvas = new Canvas();

        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        Path path = new Path();
        path.addCircle(100, 100, 40, Path.Direction.CW);

        canvas.drawTextOnPath("BLUE DOT", path, 200, 20, tPaint);
        canvas.drawTextOnPath("BLUE DOT", path, 200f-(40f*2f*3.1415f), 20, tPaint);

        //canvas.drawCircle();

        BitmapDescriptor bd = BitmapDescriptorFactory.fromBitmap(bitmap);

        // Add a drawable as marker:
        Drawable circleDrawable = ContextCompat.getDrawable(mMainActivity, R.drawable.circle_yellow);

        //BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);

        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(59.283, 18.081))
                .title("hej")
                .icon(bd);
        Marker mark = googleMap.addMarker(options);


        // test
        // test:  var p1 = new LatLon(51.4778, -0.0015);


        //        var p2 = p1.destinationPoint(7794, 300.7); // 51.5135°N, 000.0983°W

        LatLng ll = locAtBearingDistance(new LatLng(51.4778,-0.0015),300.7,7794);
        Log.i(TAG, "Disttestet: " +  ll.latitude + "  " + ll.longitude);


    } // OnMapReady


    private class upDateBallListInBackground extends AsyncTask<LatLngBounds, Void, updateResultLists> {

        @Override
        protected void onPreExecute(){
            //updateList = false;
            startHash = System.currentTimeMillis();
            Log.i(TAG, "start update Hashes");
        }

        @Override
        protected updateResultLists doInBackground(LatLngBounds... params) {
            return mHashStuff.updateHashes(params[0]);
        }

        List<Ball> listCompleteBalls;
        @Override
        protected void onPostExecute(updateResultLists result) {
            Log.i(TAG, "Running onPost hash , took: " + (System.currentTimeMillis() - startHash) + " ms");
            //addList.addAll(result.addBallList);
            //removeList.addAll(result.removeBallList);
//            Log.i(TAG, " Add: " + result.addBallList.size());
//            Log.i(TAG, " Remove: " + result.removeBallList.size());

            /*
            List<Ball> blist = result.first;
            Iterator<Ball> it = ballsInView.iterator();
            Ball b;

            int remove = 0;
            while(it.hasNext()) {
                b = it.next();
                if (!blist.contains(b)) {
                    b.marker.remove();
                    it.remove();
                    remove ++;
                }
            }
            Log.i(TAG,"hash removed " + remove);
            int added = 0;
            it = blist.iterator();
            while(it.hasNext()) {
                b = it.next();
                if (!ballsInView.contains(b)) {
                    //ballsInView.add(addBallToView(b));
                    ballsInView.add(b);
                    added++;
                }
            }
            Log.i(TAG,"hash added " + added);

            mMainActivity.debugTxt2.setText("Balls: " + ballsInView.size());
            // Fixa: ta emot och uppdatera addlista.
            lastHashUpdateTime = System.currentTimeMillis()/1000;*/
            Log.i(TAG, "finished Update Hashes, took: " + (System.currentTimeMillis() - startHash) + " ms");
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

      public void setTime(int time){
          timeAdderEvenHour = (time/60/60)*60*60; // convert hours to seconds
          timeAdderSeconds = time;
      }

}
