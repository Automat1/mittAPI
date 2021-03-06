package se.m76.mittapi.models;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Jakob on 2017-06-04.
 */

public class Ufo {
        public String geoHash;
        public Integer color;
        public Long time;
        public Integer direction;
        public Circle circle;

        @Override
        public boolean equals(Object obj){
            //Log.i("eq","eq startar");
            if (obj == null) {
                return false;
            }
            if(obj instanceof String){
                //Log.i("eq","eq: är string");
                final String s = (String) obj;
                if ((this.geoHash == null) ? (s != null) : !this.geoHash.equals(s)) {
                 //   Log.i("eq","eq: är string false :" + this.geoHash.equals(s));

                    return false;

                }
                return true;
            }
            if (!Ufo.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final Ufo other = (Ufo) obj;
            if ((this.geoHash == null) ? (other.geoHash != null) : !this.geoHash.equals(other.geoHash)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode(){
            //Log.i("eq","hashcode startar");
            return (this.geoHash != null ? this.geoHash.hashCode() : 0);
        }
}
