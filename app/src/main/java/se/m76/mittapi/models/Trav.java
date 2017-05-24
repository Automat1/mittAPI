package se.m76.mittapi.models;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

public class Trav {
    private static final String TAG = Trav.class.getSimpleName();

    private long id;
    private String Key;
    private LatLng pos;
    private LatLng dest;
    private long form;
    private long color;

    //private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getKey() { return Key; }

    public void setKey(String key) { Key = key; }

    public LatLng getPos() {
        return pos;
    }

    public void setPos(LatLng pos) {
        this.pos = pos;
    }

    public LatLng getDest() {
        return dest;
    }

    public void setDest(LatLng dest) {
        this.dest = dest;
    }

    public long getForm() {
        return form;
    }

    public void setForm(long form) {
        this.form = form;
    }

    public long getColor() {
        return color;
    }

    public void setColor(long color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object other){

        if (other == null) return false;
        if (other == this) return true;
        if (getClass()!= other.getClass()) return false;

        Trav otherTrav = (Trav)other;

        return otherTrav.getId() == getId();
    }

    //public Map<String, Object> getAdditionalProperties() {
    //    return this.additionalProperties;
    //}

    //public void setAdditionalProperty(String name, Object value) {
    //    this.additionalProperties.put(name, value);
    //}

}