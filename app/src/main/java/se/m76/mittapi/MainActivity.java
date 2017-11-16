package se.m76.mittapi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.abs;


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
    Integer cnt;
    double spd;
    boolean runSpd;
    Canvas canvas;
    Drawable d;
    ImageView image1;
    TextView text2;
    private VelocityTracker mVelocityTracker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Startar ...");

        spd = 0.0;
        runSpd = false;

        mMaps = new Maps(this);
        mMaps.mapOnResume();
        mHashStuff = new HashStuff(this, mMaps);
        mMaps.setHashStuff(mHashStuff);
        mLocationProvider = new LocationProvider(this, this);

        text2 = (TextView) findViewById(R.id.textView2) ;
        image1 = (ImageView) findViewById(R.id.ImageView1);
        //image1.setImageResource(R.drawable.fulpil);
        text2.setText("TEXT2");

        Bitmap drawnBitmap = Bitmap.createBitmap(2000, 200, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(drawnBitmap);
        d = getResources().getDrawable(R.drawable.fulpil);

        //Centre the drawing
        int bitMapWidthCenter = drawnBitmap.getWidth()/2;
        int bitMapheightCenter = drawnBitmap.getHeight()/2;
        for(int i=0;i<2000;i=i+300) {
            d.setBounds(0+i, 0, 200+i, 200);
            d.draw(canvas);
        }
        image1.setImageBitmap(drawnBitmap);

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

        final Handler h = new Handler();
        final int delay = 15; //milliseconds

        cnt = 0;

        final boolean b = h.postDelayed(new Runnable() {
            public void run() {
                if (runSpd) {
                    cnt = cnt + (int) (spd / 60.0);
                    spd = spd * 0.95;
                    if (abs(spd) < 10) {
                        runSpd = false;
                    }
                }
                moveArrow();
                mMaps.updateListOnMap();
                h.postDelayed(this, delay);
            }
        }, delay);


        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    //Toast.makeText(getApplicationContext(),"Something went wrong",Toast.LENGTH_SHORT).show();
                }
            });

        image1.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int X = (int) event.getRawX();
                final int Y = (int) event.getRawY();

                int index = event.getActionIndex();
                int action = event.getActionMasked();
                int pointerId = event.getPointerId(index);

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if(mVelocityTracker == null) {
                            // Retrieve a new VelocityTracker object to watch the
                            // velocity of a motion.
                            mVelocityTracker = VelocityTracker.obtain();
                        }
                        else {
                            // Reset the velocity tracker back to its initial state.
                            mVelocityTracker.clear();
                        }
                        // Add a user's movement to the tracker.
                        mVelocityTracker.addMovement(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        runSpd = true;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(event);
                        mVelocityTracker.computeCurrentVelocity(1000);
                        spd = VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                                pointerId);
                        text2.setText("X är : " + spd);
                        cnt = X;
                        break;
                }
                //_root.invalidate();
                return true;
            }

        });
    }

    public void moveArrow(){
        canvas.drawColor(Color.WHITE);
        //If you are looking for canvas transparency, you can also use this
        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        image1.postInvalidate();
        int tmp = cnt;
        tmp= tmp%300;
        for(int i=0;i<2000;i=i+300) {
            d.setBounds(0+i+tmp, 0, 200+i+tmp, 200);
            d.draw(canvas);
        }

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
        //    mMaps.handleNewLocation(location);
        }

        // Koll avstånd i stället.
        if((System.currentTimeMillis() - timeOfLastListUpdate) > 10000) {
        //    mHashStuff.updateListAtPos(location);
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

