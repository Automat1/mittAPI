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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import ch.hsr.geohash.GeoHash;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import se.m76.mittapi.models.AddRemoveLists;
import se.m76.mittapi.models.Ball;
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

    public List<Ball> findHashesListOfRutor(List<GeoHash> lg){
        // försök räkna ut en hash!

        List<Ball> lista = new ArrayList<>();

        String utcstr = getCurrentTimeStamp();
        Log.i(TAG, "Datumsträng: " + utcstr);

        byte[] bytesOfMessage;
        MessageDigest md;
        BigInteger bigInt;
        String ph6;
        String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
        Hashtable<Character, Integer> b32GeoHashtable =
                new Hashtable<Character, Integer>();
        for(int i = 0;i < 32;i++){
            b32GeoHashtable.put(base32GeoHashCharacterSet.charAt(i),i);
        }

        for(GeoHash gh : lg) {
            ph6 = gh.toBase32().substring(0,6);

            for (int i = 1; i <= 10; i++) {
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
                b.color = b32GeoHashtable.get(gp1r.charAt(6))%4;
                // 0 yellow
                // 1 blue
                // 3 red
                // 4 green

                lista.add(b);
            }
        }
        return lista;
    }

    private LatLng getLatLngFromGeoHash(GeoHash gh) {
        LatLng ll = new LatLng(gh.getPoint().getLatitude(),gh.getPoint().getLongitude());
        return ll;
    }


    // Denna ska köras i annan thread!
    public AddRemoveLists setCamera(LatLngBounds curScreen){

        List<GeoHash> listOfRutor = new ArrayList<>();
        LatLng ne = curScreen.northeast;
        GeoHash gh = GeoHash.withCharacterPrecision(ne.latitude,ne.longitude,6);

        // 1 hitta övre högra hash
        // 2 gå söderut så länge de är i bild
        // 3 gå vänster så länge de är i bild

        // om den till vänsters högra kant är i bild så lägg till den också.
        double centerLat = curScreen.getCenter().latitude;
        double centerLong = curScreen.getCenter().longitude;

        do {
            listOfRutor.add(gh);
            GeoHash ghSouth = gh.getSouthernNeighbour();
            while(curScreen.contains(new LatLng(ghSouth.getBoundingBox().getUpperLeft().getLatitude(),centerLong))){
                listOfRutor.add(ghSouth);
                ghSouth = ghSouth.getSouthernNeighbour();
            }
            gh = gh.getWesternNeighbour();
        } while(curScreen.contains(new LatLng(centerLat , gh.getBoundingBox().getLowerRight().getLongitude())));

        Log.i(TAG, "Antal hashar i bild: " + listOfRutor.size());
        List<Ball> lst = findHashesListOfRutor(listOfRutor);
        HashSet<Ball> blist = mMaps.getListOfBalls();
        List<Ball> remove = new ArrayList<>();
        List<Ball> adda = new ArrayList<>();

        String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
        Hashtable<Character, Integer> b32GeoHashtable =
                new Hashtable<Character, Integer>();
        for(int i = 0;i < 32;i++){
            b32GeoHashtable.put(base32GeoHashCharacterSet.charAt(i),i);
        }


        Integer skips = new Integer(0);
        for(Ball b : lst){
            if(!blist.contains(b)){
                //Ball b = new Ball();
                //b.geoHash = s;
                //b.color = b32GeoHashtable.get(s.charAt(18));
                adda.add(b);
            }
            else{
                skips++;
                //Log.i(TAG,"Hash already in list");
            }
        }
        Log.i(TAG, "SKippade för att redan finns:" + skips);


        // skapa location mitt i bild för att ta bort bollar
        Location locScreen = new Location("");
        locScreen.setLatitude(centerLat);
        locScreen.setLongitude(centerLong);
        Location l = new Location("");

        if(!blist.isEmpty())
        for(Ball b : blist){
            gh = GeoHash.fromGeohashString(b.geoHash);
            l.setLatitude(gh.getPoint().getLatitude());
            l.setLongitude(gh.getPoint().getLongitude());
            // 10000 meters
            //Log.i(TAG, "Distance is : " + l.distanceTo(locScreen));
            if(l.distanceTo(locScreen)>10000){
                remove.add(b);
            }
        }

        Log.i(TAG, "Created add: " + adda.size() + " och remove: " + remove.size());
        AddRemoveLists arl = new AddRemoveLists();
        arl.addList = adda;
        arl.removeList = remove;
        return arl;

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
        //LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
        //List<LatLng> lista ;
        //lista = findHashesAtLocation(ll);
        //Log.i(TAG, "Lista lngd " + lista.size());
        //mMaps.updateListOnMap(lista);
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
