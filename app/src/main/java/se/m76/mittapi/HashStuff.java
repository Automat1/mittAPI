package se.m76.mittapi;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import ch.hsr.geohash.GeoHash;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import se.m76.mittapi.models.AddBallPair;
import se.m76.mittapi.models.AddUfoPair;
import se.m76.mittapi.models.Ball;
import se.m76.mittapi.models.HashTimePair;
import se.m76.mittapi.models.Trav;
import se.m76.mittapi.models.Travs;
import se.m76.mittapi.models.Ufo;
import se.m76.mittapi.models.updateResultLists;

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
    //List<HashTimePair> listOfCalculatedHashes;
    Map<String, Long> listOfCalculatedHashesBalls;
    Map<String, Long> listOfCalculatedHashesUfos;
    HashMap<Pair<String,Long>, List<Ball>> calculatedBallsMap;
    long oldUpdateTime;;
    List<String> listOfActiveHashes;

    //HashMap<String,ArrayList<GeoHash>> hoursList;

    public HashStuff(Context context, Maps map) {
        mMainActivity = (MainActivity) context;
        mContext = context;
        mMaps = map;
        listOfTravs = new ArrayList<>();
        //listOfCalculatedHashes = new ArrayList<>();
        listOfCalculatedHashesBalls = new HashMap<>();
        listOfCalculatedHashesUfos = new HashMap<>();
        calculatedBallsMap = new HashMap<>();
        listOfActiveHashes = new ArrayList<>();
    }


    private List<Ball> getAllBallsInHash(String gh6, long time){
        List<Ball> bl = new ArrayList<>();
        long t;
        //for(long h = -1; h <= 24 ; h++) {
           // t = timeNowHour + h*60*60;
            if (!calculatedBallsMap.containsKey(Pair.create(gh6, time))) {
                //Log.i(TAG,"Calc: " + gh6 + " for time +" + h);
                calculatedBallsMap.put(Pair.create(gh6, time), calcAllBallsInHash(gh6, time));
            }
                //Log.i(TAG,"Already calced " + gh6 +  " for time +" +h);
            bl.addAll(calculatedBallsMap.get(Pair.create(gh6, time)));
        //}
        //Log.i(TAG, "getAllBallsInHash reutrns :" + bl.size());

        return bl;
    }

    public List<Ball> calcAllBallsInHash(String g, long timeNowHour){
        List<Ball> balls = new ArrayList<>();

        String time = getCurrentTimeStamp(timeNowHour);
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

        //ph6 = g.toBase32().substring(0, 6);
        ph6 = g;

        utcstr = time;
        //Log.i(TAG, "Datumsträng: " + utcstr);

        for (int i = 1; i <= 1 ; i++) {  // öka till antal som ska skapas
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

            while(gp_base32Hex.length()<26)
                gp_base32Hex = "0" + gp_base32Hex;

            // String rfc4648 base32HexCharacterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

            // Ändra från base32Hex charset till base32geo charset som är ett eget charset.
            String gp1geo = "";
            for (int ii = 0; ii < gp_base32Hex.length(); ii++) {
                gp1geo = gp1geo + base32GeoHashCharacterSet.charAt(java.lang.Character.digit(gp_base32Hex.charAt(ii), 32));
            }

            // längden är 26. 18 till geo 8 kvar. Ett till färg.

            String gp1r = ph6 + gp1geo.substring(0, 18);
            Ball b = new Ball();
            b.latLng = new LatLng(GeoHash.fromGeohashString(gp1r).getPoint().getLatitude(),
                    GeoHash.fromGeohashString(gp1r).getPoint().getLongitude());
            b.geoHash = gp1r;
            b.color = b32GeoHashtable.get(gp1geo.charAt(18)) % 4;
            b.timeStart = timeNowHour + (b32GeoHashtable.get(gp1geo.charAt(19)) * b32GeoHashtable.get(gp1geo.charAt(20)) % 60)*60;
            b.timeEnd = b.timeStart + 60*60;

                // 0 yellow
                // 1 blue
                // 3 red
                // 4 green

            Integer luck = b32GeoHashtable.get(gp1geo.charAt(21));
            if(luck>=0) {
                //AddBallPair abp = new AddBallPair();
                //abp.ball=b;
                //abp.hour=time;
                balls.add(b);
            }
        } // next i..
        return balls;
    }

    public List<AddUfoPair> findAllUfosInGeoHash(GeoHash g, String time, long timeNowHour){
        List<AddUfoPair> ufos = new ArrayList<>();

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

        ph6 = g.toBase32().substring(0, 4);

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

            while(gp_base32Hex.length()<26)
                gp_base32Hex = "0" + gp_base32Hex;

            // String rfc4648 base32HexCharacterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

            // Ändra från base32Hex charset till base32geo charset som är ett eget charset.
            String gp1geo = "";
            for (int ii = 0; ii < gp_base32Hex.length(); ii++) {
                gp1geo = gp1geo + base32GeoHashCharacterSet.charAt(java.lang.Character.digit(gp_base32Hex.charAt(ii), 32));
            }

            // längden är 26. 18 till geo 8 kvar. Ett till färg.

              //  Log.i(TAG,"Length is " + gp_base32Hex.length() );
            String gp1r = ph6 + gp1geo.substring(0, 20);
            Ufo u = new Ufo();
            u.geoHash = gp1r;
            u.color = b32GeoHashtable.get(gp1geo.charAt(20)) % 4;
            //u.time = timeNowHour;
            u.time = timeNowHour + (b32GeoHashtable.get(gp1geo.charAt(21)) * b32GeoHashtable.get(gp1geo.charAt(22)) % 60)*60;
            u.direction = b32GeoHashtable.get(gp1geo.charAt(21)) * b32GeoHashtable.get(gp1geo.charAt(22)) * b32GeoHashtable.get(gp1geo.charAt(23)) % 360;

            // 0 yellow
            // 1 blue
            // 3 red
            // 4 green

            Integer luck = b32GeoHashtable.get(gp1geo.charAt(24));
            if(luck>=0) {
                AddUfoPair aup = new AddUfoPair();
                aup.hour = time;
                aup.ufo = u;
                ufos.add(aup);
            }
        } // next i..

        // Log.i(TAG, "Number o fufos in list: " + ufos.size());
        return ufos;

    }


    private LatLng getLatLngFromGeoHash(GeoHash gh) {
        LatLng ll = new LatLng(gh.getPoint().getLatitude(),gh.getPoint().getLongitude());
        return ll;
    }


    // Running as AsyncTask
    // Prepares all lists Map need to show balls and OTHER
    public updateResultLists updateHashes(LatLngBounds curScreen) {

        /*
        FÖrsök nr 5
        1. Ta position och kolla om det är ny hash (alltid första
        2. Om så skapa matris av hash-time
            Map<long, List<String>>
            time = timeNow:
            hitt första ej uträknade
            timeDiff = 0;
            List = Map.get(time + timeDiff)
            iter List: if iter not in calced MAP<long,String> do calc and add quue
                exit loop
            Loop timeDiff++
            List = Map(time + timediff
            List = Map time - timediff

        3. Kolla tid från UI.
        4. Börja gå igenom och leta efter ej uträknade hashar i den tid.
            Om tiden uträknad gå i steg om +1 och -1 i tid.
        5. Om hittar en att räkna ut gör det och sätt i queue till UI.
        6. Om hittat en starta om från 1.
        7. Om ej hittat  -> finish.

        övr.
        2 alternativ att rensa:
        - Ui får en hashlist.
        - Ui räknar avstånd i varje Ball.  < --- första att försöka tror jag.

        Ui.
        Har en Map, Times, Balls.
        Går bara igenom time-1, time och old-1time och oldtime.
        Eventuellt läggs till max X per varv.
        Det kommer funka!

         */

        /*

        while not finished
        1. Ta in hash och tid
        2. Räkna ut hasharna i lista
        3. Börja Loop: För varje hash kolla om Map har fått för timeNow.
            Om ej Map fått, kolla om den är uträknad.
            Räkna ut eller skicka färdigräknad (skippa att spara färdigräknad ..?
            Kolla om hash eller tid ändrats? exit loop
            annars loopa tills alla tider körts -- finished = true!
         */
        String lastHash = new String();
        boolean finished = false;

        while(!finished){
            String actualHash = mMaps.hashStringCenter;
            int timeAdder = mMaps.timeAdderEvenHour; //Kolla om det stämmer
            int timeNow = 0;

            int myAdder = 0;
            do {
                if (stuffIsDone(timeAdder))
                    doStuffWithMyAdder++;
                if Adderis done done = true
                else
                doStuffwithTimeAdder;

                if adder = 24 finished=true;
                   done;

                if hashDidChange
                    done

            } while !done


            // ex timeAdd = 5 : 5 6 4 7 3 8 2 9 1 10 0 11 12 13
        }
        // 1. Kolla vilken hash det är nu.
        String actualHash = mMaps.hashStringCenter;
        int actualTime = mMaps.timeAdderEvenHour; //Kolla om det stämmer

        if(!lastHash.equals(mMaps.hashStringCenter)){
            updateHashList(mMaps.hashStringCenter);
        }

        long time;
        Map<long,>

        long timeNow = System.currentTimeMillis() / 1000;
        long timeNowHour = getCurrentTimeLastHour(timeNow);
        List<GeoHash> listOfNewHashes = new ArrayList<>();
        LatLng ne;
        GeoHash gh;
        String g;


        // Create list of hashes in screen

        ne = curScreen.northeast;
        gh = GeoHash.withCharacterPrecision(ne.latitude, ne.longitude, 6);

        // 1 hitta övre högra hash
        // 2 gå söderut så länge de är i bild
        // 3 gå vänster så länge de är i bild

        // om den till vänsters högra kant är i bild så lägg till den också.
        double centerLat = curScreen.getCenter().latitude;
        double centerLong = curScreen.getCenter().longitude;

        Map<String,Long> newMapHashBalls = new HashMap<>();

        List<Ball> ballList = new ArrayList<>();
        List<String> newListOfActiveHashes = new ArrayList<>();
        do {
            g = gh.toBase32().substring(0, 6);
            //Log.i(TAG,"Found hash " + g);
            //ballList.addAll(getAllBallsInHash(g,timeNowHour));

            newListOfActiveHashes.add(g);

            GeoHash ghSouth = gh.getSouthernNeighbour();
            while (curScreen.contains(new LatLng(ghSouth.getBoundingBox().getUpperLeft().getLatitude(), centerLong))) {
                g = ghSouth.toBase32().substring(0, 6);
                //Log.i(TAG,"FOund hash " + g);
                //ballList.addAll(getAllBallsInHash(g,timeNowHour));
                newListOfActiveHashes.add(g);
                ghSouth = ghSouth.getSouthernNeighbour();
            }
            gh = gh.getWesternNeighbour();
        }
        while (curScreen.contains(new LatLng(centerLat, gh.getBoundingBox().getLowerRight().getLongitude())));
        Log.i(TAG,"New Act Hashes: " + newListOfActiveHashes.size() + " old:" + listOfActiveHashes.size());
        List<Ball> removeList = new ArrayList<>();
        List<Ball> addList = new ArrayList<>();

        long startremovetime;
        long endRemoveTime;
        long startAddTime;
        long endAddTime;

        // om finns stega från oldupdate till
        long updateTime = timeNowHour;
        for(String s : listOfActiveHashes){
            startremovetime = oldUpdateTime - 60*60;
            endAddTime = updateTime + 23*60*60;
            if(newListOfActiveHashes.contains(s)) {
                endRemoveTime = updateTime - 60*60;
                startAddTime = oldUpdateTime + 24*60*60;
            }else {
                endRemoveTime = oldUpdateTime + 23*60*60;
                startAddTime = updateTime - 60*60;
            }
            // REMOVE
            for(long t = startremovetime; t < endRemoveTime; t+=60*60) {
                //removeList.addAll(getAllBallsInHash(s,t));
                List<Ball> bl = getAllBallsInHash(s,t);
                for(Ball b : bl){
                    mMaps.addQ.add(b);
                }
            }

            // ADD
            for(long t = startAddTime; t <= endAddTime; t+=60*60) {
                //addList.addAll(getAllBallsInHash(s,t));
                List<Ball> bl = getAllBallsInHash(s,t);
                for(Ball b : bl){
                    mMaps.addQ.add(b);
                }
            }
        }
        startAddTime = updateTime;
        endAddTime = updateTime + 23*60*60;
        for(String s : newListOfActiveHashes){
            Log.i(TAG, "Adding for hash: " + s);
            if(!listOfActiveHashes.contains(s)){
                // ADD
                for(long t = startAddTime; t <= endAddTime; t+=60*60) {
                    // addList.addAll(getAllBallsInHash(s,t));
                    List<Ball> bl = getAllBallsInHash(s,t);
                    for(Ball b : bl){
                        mMaps.addQ.add(b);
                    }
                    //Log.i(TAG, "Addlit size " + addList.size());
                }
            }
        }
        oldUpdateTime = updateTime;
        listOfActiveHashes = newListOfActiveHashes;

        //updateResultLists resultLists = new updateResultLists();
        //resultLists.addBallList = addList;
        //resultLists.removeBallList = removeList;
        //
        //map:
        // lastUpdateTime is 124
        // time is 125
        // hash s1 till 124
        // hash s2 till 124
        // hash s3 ej alls

        // ta bort s1 124 -24
        // lägg till s1 125

        return null;//resultLists;
        //return Pair.create(ballList, null);

        /*    do {
                if(!listOfCalculatedHashesBalls.containsKey(gh) ||  !(listOfCalculatedHashesBalls.get(gh) > (timeNow - 60*60))){
                    listOfNewHashes.add(gh);
                    listOfCalculatedHashesBalls.put(gh,timeNow);
                }
                GeoHash ghSouth = gh.getSouthernNeighbour();
                while (curScreen.contains(new LatLng(ghSouth.getBoundingBox().getUpperLeft().getLatitude(), centerLong))) {
                    if(!listOfCalculatedHashesBalls.containsKey(ghSouth) ||  !(listOfCalculatedHashesBalls.get(ghSouth) > (timeNow - 60*60))){
                        listOfNewHashes.add(ghSouth);
                        listOfCalculatedHashesBalls.put(ghSouth,timeNow);
                    }
                    ghSouth = ghSouth.getSouthernNeighbour();
                }
                gh = gh.getWesternNeighbour();
            }
            while (curScreen.contains(new LatLng(centerLat, gh.getBoundingBox().getLowerRight().getLongitude())));
*/
           /* Log.i(TAG, "Number of new visible hashes was: " + listOfNewHashes.size());


            List<AddBallPair> theBallList = new ArrayList<>();
            for (int h = -1; h < 24; h++) {
                String time = getCurrentTimeStamp(timeNow + h * 60 * 60);
                //hs = new HashSet<>();
                for (GeoHash g : listOfNewHashes) {
                    theBallList.addAll(findAllBallsInGeoHash(g, time, timeNowHour));
                }
            }

            mMaps.setBallList(theBallList);

        //Log.i(TAG, "Number of new visible hashes was: " + listOfNewHashes.size());

        /* String base32GeoHashCharacterSet = "0123456789bcdefghjkmnpqrstuvwxyz";
        Hashtable<Character, Integer> b32GeoHashtable =
                new Hashtable<>();
        for(int i = 0;i < 32;i++){
            b32GeoHashtable.put(base32GeoHashCharacterSet.charAt(i),i);
        }*/



/*        // -----------------------------------------------------------------------------------------
        // Ufos:

        // 1. Skapa lista med alla 4 teckens hashar som är inom 20 km från skärmen
        listOfNewHashes.clear();
        ne = curScreen.getCenter();
        gh = GeoHash.withCharacterPrecision(ne.latitude, ne.longitude, 4);
        // 2. Hitta alla hashar som är inom 20 km.
        //LatLng.gh.getPoint().getLatitude()

        // för 40 km knappt ta två hashar uppåt och två neråt. tot 5 vertikalt
        // i bredd ökar antalet 5/cos(lat). Gräns vid lat 80 för att inte få för många.
        int noVertical = 3;
        int noHorizontal = (int) (3.0/Math.cos(curScreen.getCenter().latitude/360*2*3.14));

        Log.i(TAG, " No horizontal: " + noHorizontal);

        // go northeast TODO: Hantera polerna
        for(int i=0 ; i<((noVertical-1)/2);i++)
            gh=gh.getNorthernNeighbour();
        for(int i=0 ; i<((noHorizontal-1)/2);i++)
            gh=gh.getEasternNeighbour();

        int cnt = 0;
        GeoHash ghh;
        // go through all vert x horiz:
        for(int v=0 ; v<noVertical;v++) {
            ghh = gh;
            for(int h=0 ; h<noHorizontal;h++) {
                if(!listOfCalculatedHashesUfos.containsKey(gh) ||  !(listOfCalculatedHashesUfos.get(gh) > (timeNow - 60*60))){
                    listOfNewHashes.add(ghh);
                    listOfCalculatedHashesUfos.put(gh,timeNow);
                }
                cnt++;
                ghh = ghh.getWesternNeighbour();
            }
            gh = gh.getSouthernNeighbour();
        }

        Log.i(TAG,"Found 4 ch hashes:" + cnt + " new ones:" + listOfNewHashes.size()) ;


        // Find Ufos in these hashes
        List<AddUfoPair> theUfoList = new ArrayList<>();
        for (int h = -1; h < 24; h++) {
            String time = getCurrentTimeStamp(timeNow + h * 60 * 60);
            for (GeoHash g : listOfNewHashes) {
                theUfoList.addAll(findAllUfosInGeoHash(g, time, timeNowHour + h*60*60));
            }
            //Log.i(TAG, "Adding " + hs.size() + " at " + time);
        }

        Log.i(TAG,"Set list with Ufos: " + theUfoList.size());
        mMaps.setUfoList(theUfoList);



        /*float [] result = new float[1];
        result[0]=0;
        while(result[0] < 30000) {
            gh = gh.getEasternNeighbour();
            Location.distanceBetween(ne.latitude, ne.longitude, gh.getPoint().getLatitude(), ne.longitude, result);
        }
        result[0]=0;
        while(result[0] < 30000) {
            gh = gh.getWesternNeighbour();
            Location.distanceBetween(ne.latitude, ne.longitude, gh.getPoint().getLatitude(), ne.longitude, result);
        }*/


    }

    List<GeoHash> listOfGeoHashes;
    private void updateHashList(String h){
        int noVertical = 3;
        int noHorizontal = 3; // (int) (3.0/Math.cos(curScreen.getCenter().latitude/360*2*3.14));

        GeoHash gh = GeoHash.fromGeohashString(h);
        listOfGeoHashes = new ArrayList<>();

        // go northeast TODO: Hantera polerna
        for(int i=0 ; i<((noVertical-1)/2);i++)
            gh=gh.getNorthernNeighbour();
        for(int i=0 ; i<((noHorizontal-1)/2);i++)
            gh=gh.getEasternNeighbour();

        int cnt = 0;
        GeoHash ghh;
        // go through all vert x horiz:
        for(int v=0 ; v<noVertical;v++) {
            ghh = gh;
            for(int h=0 ; h<noHorizontal;h++) {
                if(!listOfCalculatedHashesUfos.containsKey(gh) ||  !(listOfCalculatedHashesUfos.get(gh) > (timeNow - 60*60))){
                    listOfNewHashes.add(ghh);
                    listOfCalculatedHashesUfos.put(gh,timeNow);
                }
                cnt++;
                ghh = ghh.getWesternNeighbour();
            }
            gh = gh.getSouthernNeighbour();
        }

        Log.i(TAG,"Found 4 ch hashes:" + cnt + " new ones:" + listOfNewHashes.size()) ;



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
    // tar utctime / 1000 (s sedan 1970, ger en datumsträng
    // Jämn timme så det blir inte exakt samma tid.
    public long getCurrentTimeLastHour(long time) {
        SimpleDateFormat utcstr;
        utcstr =  new SimpleDateFormat("ddMMyyyyHH");
        utcstr.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d = new Date();
        d.setTime(time*1000);
        String s = utcstr.format(d);
        try {
            d = utcstr.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d.getTime()/1000;
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
