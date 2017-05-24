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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import ch.hsr.geohash.GeoHash;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

    public HashStuff(Context context, Maps map) {
        mMainActivity = (MainActivity) context;
        mContext = context;
        mMaps = map;
        listOfTravs = new ArrayList<>();
    }

    public List<LatLng> findHashesAtLocation(LatLng ll){
        // försök räkna ut en hash!

        List<LatLng> lista = new ArrayList<>();

        String utcstr = getCurrentTimeStamp();
        Log.i(TAG, "Datumsträng: " + utcstr);

//        byte[] bytesOfMessage = new byte[0];
//        try {
//            bytesOfMessage = utcstr.getBytes("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//        MessageDigest md = null;
//        try {
//            md = MessageDigest.getInstance("MD5");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        byte[] hourmd5 = md.digest(bytesOfMessage);
//
//        BigInteger bigInt = new BigInteger(1,hourmd5);
//        String hashtext = bigInt.toString(16);
//        // Now we need to zero pad it if you actually want the full 32 chars.
//        while(hashtext.length() < 32 ){
//            hashtext = "0"+hashtext;
//        }
//
//        Log.i(TAG, "Md5 datum:" + hashtext);
//        // OK stämmer mot python, puh.

        LatLng pos = ll;


        String ph = GeoHash.geoHashStringWithCharacterPrecision(pos.latitude,pos.longitude,12);
        GeoHash phg6 = GeoHash.withCharacterPrecision(pos.latitude,pos.longitude,6);

        Log.i(TAG,"Poshash: " + ph); // Stämmer mot python!

        String ph4 = ph.substring(0,4);
        String ph6 = ph.substring(0,6);
        Log.i(TAG,"ph4: " + ph4);

        // Convert array to list
        List<GeoHash> listGeo = new ArrayList<>(Arrays.asList(phg6.getAdjacent()));
        //Log.i(TAG, "NULL" + listGeo.size() + " " + listGeo.get(0).toBase32());
        listGeo.add(phg6);

        Log.i(TAG,"Adjacent size: " + listGeo.size() + " " + listGeo.get(0).toBase32());

        byte[] bytesOfMessage;
        MessageDigest md;
        BigInteger bigInt;

        for(GeoHash gh : listGeo) {
            ph6 = gh.toBase32().substring(0,6);

            for (int i = 1; i <= 10; i++) {
                String dstrh = utcstr + ph6 + i;
                //Log.i(TAG, dstrh); // Ok

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

                //  Vad gör detta? Från bytelista till String.
                bigInt = new BigInteger(1, gp1);
                String gp1h = bigInt.toString(16);
                // Now we need to zero pad it if you actually want the full 32 chars.
                while (gp1h.length() < 32) {
                    gp1h = "0" + gp1h;
                }

                BigInteger bigInt2 = bigInt.shiftLeft(2); // för att padda med två nollor till höger
                String gptest = bigInt2.toString(32);

                // Log.i(TAG, "gpTest: " + gptest);
                // Log.i(TAG, "Gp1h: " + gp1h); // OK
                // Log.i(TAG, "forDigit:" + java.lang.Character.forDigit(31, 32));
                // its Triacontakaidecimal
                // String base32HexCharacterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
                String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
                // skapa b32hex map som ger int per tecken för att konvertera
                String gp1geo = "";
                for (int ii = 0; ii < gptest.length(); ii++) {
                    gp1geo = gp1geo + base32GeoHashCharacterSet.charAt(java.lang.Character.digit(gptest.charAt(ii), 32));
                }

                //Log.i(TAG, "Gp1Geo: " + gp1geo); // OK!

                String gp1r = ph6 + gp1geo.substring(0, 18);

                //Log.i(TAG, "gp1r: " + gp1r); // OK!

                double latte = GeoHash.fromGeohashString(gp1r).getPoint().getLatitude();
                double longe = GeoHash.fromGeohashString(gp1r).getPoint().getLongitude();
                Log.i(TAG, "Lat: " + latte + " Long: " + longe); // OK, (fler decimaler)

                LatLng ll1 = new LatLng(latte, longe);
                lista.add(ll1);
            }
        }
        return lista;
    }

    private LatLng getLatLngFromGeoHash(GeoHash gh) {
        LatLng ll = new LatLng(gh.getPoint().getLatitude(),gh.getPoint().getLongitude());
        return ll;
    }


    public  void setCamera(LatLngBounds curScreen){

        List<GeoHash> listOfRutor = new ArrayList<>();
        LatLng ne = curScreen.northeast;
        GeoHash gh = GeoHash.withCharacterPrecision(ne.latitude,ne.longitude,6);
        listOfRutor.add(gh);

        // hitta höger kant av hash i övre kant av bild..
        gh = gh.getWesternNeighbour();
        while (curScreen.contains(new LatLng(curScreen.getCenter().latitude,gh.getBoundingBox().getLowerRight().getLongitude()))){
            listOfRutor.add(gh);
            GeoHash ghh = gh.getSouthernNeighbour();
            while (curScreen.contains(new LatLng(ghh.getBoundingBox().getUpperLeft().getLatitude(),curScreen.getCenter().longitude))){
                listOfRutor.add(ghh);
                ghh = ghh.getSouthernNeighbour();
            }
            gh = gh.getWesternNeighbour();
        }

        Log.i(TAG, "Antal hashar i bredd: " + listOfRutor.size());


    }



    private String getCurrentTimeStamp() {
        SimpleDateFormat utcstr;
        utcstr =  new SimpleDateFormat("ddMMyyyyHH");
        utcstr.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcstr.format(new Date());
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
        LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
        List<LatLng> lista ;
        lista = findHashesAtLocation(ll);
        Log.i(TAG, "Lista lngd " + lista.size());
        mMaps.updateListOnMap(lista);

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
