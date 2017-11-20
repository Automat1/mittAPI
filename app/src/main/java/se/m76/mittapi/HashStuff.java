package se.m76.mittapi;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import ch.hsr.geohash.GeoHash;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import se.m76.mittapi.models.Ball;
import se.m76.mittapi.models.HashTimePair;
import se.m76.mittapi.models.Trav;
import se.m76.mittapi.models.Travs;

/**
 * Created by Jakob on 2017-05-01.
 */

public class HashStuff {
    private static final String TAG = HashStuff.class.getSimpleName();

    private List<Trav> listOfTravs;

    MainActivity mMainActivity;
    Context mContext;
    Maps mMaps;
    LatLng mPos;
    float mZoom;
    List<HashTimePair> listOfCalculatedHashes;
    //HashMap<String,ArrayList<GeoHash>> hoursList;

    public HashStuff(Context context, Maps map) {
        mMainActivity = (MainActivity) context;
        mContext = context;
        mMaps = map;
        listOfTravs = new ArrayList<>();
        listOfCalculatedHashes = new ArrayList<>();
    }

    public HashSet<Ball> findAllBallsInGeoHash(GeoHash g, String time){
        // försök räkna ut en hash!

        HashSet<Ball> balls = new HashSet<>();

        byte[] bytesOfMessage;
        MessageDigest md;
        BigInteger bigInt;
        String ph6;
        String utcstr;

        // Prepase base32 stuff
        String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
        Hashtable<Character, Integer> b32GeoHashtable =
                new Hashtable<Character, Integer>();
        for(int i = 0;i < 32;i++){
            b32GeoHashtable.put(base32GeoHashCharacterSet.charAt(i),i);
        }

        ph6 = g.toBase32().substring(0, 6);

        utcstr = time;
        //Log.i(TAG, "Datumsträng: " + utcstr);

        for (int i = 1; i <= 1 /*10*/; i++) {
            String dstrh = utcstr + ph6 + i;

            bytesOfMessage = new byte[0];
            try {
                bytesOfMessage = dstrh.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] gp1 = md.digest(bytesOfMessage);

            //  Från bytelista till String.
            bigInt = new BigInteger(1, gp1);

            // Skapa base32 sträng, varje 5 bitar ett tecken.
            bigInt = bigInt.shiftLeft(2); // för att padda med två nollor till höger
            String gp_base32Hex = bigInt.toString(32);

            // String rfc4648 base32HexCharacterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

            // Ändra från base32Hex charset till base32geo charset som är ett eget charset.
            String gp1geo = "";
            for (int ii = 0; ii < gp_base32Hex.length(); ii++) {
                gp1geo = gp1geo + base32GeoHashCharacterSet.charAt(java.lang.Character.digit(gp_base32Hex.charAt(ii), 32));
            }

            // längden är 26. 18 till geo 8 kvar. Ett till färg.

            String gp1r = ph6 + gp1geo.substring(0, 18);
            Ball b = new Ball();
            b.geoHash = gp1r;
            b.color = b32GeoHashtable.get(gp1geo.charAt(18)) % 4;
            b.time = b32GeoHashtable.get(gp1geo.charAt(19)) * b32GeoHashtable.get(gp1geo.charAt(20)) % 60;

                // 0 yellow
                // 1 blue
                // 3 red
                // 4 green

            Integer luck = b32GeoHashtable.get(gp1geo.charAt(21));
            if(luck>=0) {
                balls.add(b);
            }
        } // next i..
        return balls;

    }

    private LatLng getLatLngFromGeoHash(GeoHash gh) {
        LatLng ll = new LatLng(gh.getPoint().getLatitude(),gh.getPoint().getLongitude());
        return ll;
    }


    // Running as AsyncTask
    // Prepares all lists Map need to show balls and OTHER
    public void updateHashes(LatLngBounds curScreen) {

        long timeNow = System.currentTimeMillis() / 1000;

        // Create list of hashes in screen
        List<GeoHash> listOfNewHashes = new ArrayList<>();
        LatLng ne = curScreen.northeast;
        GeoHash gh = GeoHash.withCharacterPrecision(ne.latitude, ne.longitude, 6);

        // 1 hitta övre högra hash
        // 2 gå söderut så länge de är i bild
        // 3 gå vänster så länge de är i bild

        // om den till vänsters högra kant är i bild så lägg till den också.
        double centerLat = curScreen.getCenter().latitude;
        double centerLong = curScreen.getCenter().longitude;

        listOfNewHashes.add(gh);
        do {
                listOfNewHashes.add(gh);
            GeoHash ghSouth = gh.getSouthernNeighbour();
            while(curScreen.contains(new LatLng(ghSouth.getBoundingBox().getUpperLeft().getLatitude(),centerLong))){
                listOfNewHashes.add(ghSouth);
                ghSouth = ghSouth.getSouthernNeighbour();
            }
            gh = gh.getWesternNeighbour();
        } while(curScreen.contains(new LatLng(centerLat , gh.getBoundingBox().getLowerRight().getLongitude())));

        Log.i(TAG, "Number of new visible hashes was: " + listOfNewHashes.size());

        HashMap<String, HashSet<Ball>> theBallList = new HashMap<>();
        HashSet<Ball> hs;
        for (int h = 0; h < 24; h++) {
            String time = getCurrentTimeStamp(timeNow + h * 60 * 60);
            hs = new HashSet<>();
            for (GeoHash g : listOfNewHashes) {
                hs.addAll(findAllBallsInGeoHash(g, time));
            }
            //Log.i(TAG, "Adding " + hs.size() + " at " + time);
            theBallList.put(time, hs);
        }

        mMaps.setBallList(theBallList);

        //Log.i(TAG, "Number of new visible hashes was: " + listOfNewHashes.size());

        /* String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
        Hashtable<Character, Integer> b32GeoHashtable =
                new Hashtable<>();
        for(int i = 0;i < 32;i++){
            b32GeoHashtable.put(base32GeoHashCharacterSet.charAt(i),i);
        }*/
    }

    // tar utctime / 1000 (s sedan 1970, ger en datumsträng
    // Jämn timme så det blir inte exakt samma tid.
    public String getCurrentTimeStamp(long time) {
        SimpleDateFormat utcstr;
        utcstr =  new SimpleDateFormat("ddMMyyyyHH");
        utcstr.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d = new Date();
        d.setTime(time*1000);
        return utcstr.format(d);
    }

    private void doCreateATrav(LatLng LL){
        // Skapa en trav objekt
        Log.i(TAG, "Skapa en trav och skickar");

        Travs travs = new Travs();
        Trav trav = new Trav();
        trav.setPos(LL);
        LatLng D = new LatLng(1,1);
        trav.setDest(D);
        travs.addTrav(trav);

        Call<Trav> call = mMainActivity.apiService.newTrav(trav);
        call.enqueue(new Callback<Trav>() {
            @Override
            public void onResponse(Call<Trav> call, Response<Trav> response) {
                int statusCode = response.code();
                Log.i(TAG, "Fick data?");
                if(response.body()!=null){
                    Log.i(TAG, String.valueOf(response.body()));
                    //Log.i(TAG, " " + response.body().getResult().size());
                    //Log.i(TAG, "Id:   " + response.body().getResult().get(0).getId());
                    Trav tr = response.body();
                    Log.i(TAG, "Fick ett id: " + tr.getId());
                    List<Trav> trs = new ArrayList<Trav>();
                    trs.add(tr);
                    addAndUpdateTravs(trs);

                }
                else
                {
                    Log.i(TAG,"Nej blev inget data");
                }
            }

            @Override
            public void onFailure(Call<Trav> call, Throwable t) {
                Log.e(TAG, "Fel i sändning", t);
            }
        });
    }

    private void addAndUpdateTravs(List<Trav> travs)
    {
        Log.i(TAG, " Lägger till ");
        for(Trav tr : travs) {
            //Log.i(TAG, "itererar: id " + tr.getId());
            if (!listOfTravs.contains(tr))
                listOfTravs.add(tr);
        }

        Log.i(TAG, "Har nu :" + listOfTravs.size());

        for(Trav tr : listOfTravs) {
            Log.i(TAG, " Hej och hå " + tr.getId() + " " + tr.getPos().latitude + " long " + tr.getPos().longitude);
            MarkerOptions options = new MarkerOptions()
                    .position(tr.getPos())
                    .title(Long.toString(tr.getId()));
            //googleMap.addMarker(options);
        }
    }

    public void updateListAtPos(Location location){
        //LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
        //List<LatLng> lista ;
        //lista = findHashesAtLocation(ll);
        //Log.i(TAG, "Lista lngd " + lista.size());
        //mMaps.updateListOfBalls(lista);
    }

    private void getTravsAtPos(LatLng LL){
        // Skapa en trav objekt
        Log.i(TAG, "Hämta travvar här");

        Call<Travs> call = mMainActivity.apiService.listTravs(LL.latitude, LL.longitude);

        call.enqueue(new Callback<Travs>() {
            @Override
            public void onResponse(Call<Travs> call, Response<Travs> response) {
                int statusCode = response.code();
                if(response.body()!=null){
                    Log.i(TAG, "Fick antal: " + response.body().getListOfTravs().size());
                    addAndUpdateTravs(response.body().getListOfTravs());
                }
                else
                {
                    Log.i(TAG,"Got no response");
                }
            }

            @Override
            public void onFailure(Call<Travs> call, Throwable t) {
                Log.e(TAG, "Failure to get travs", t);
            }
        });
    } // getTravsAtPos



}
