package se.m76.mittapi.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Travs {

    private List<Trav> travs = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Travs(){
        travs= new ArrayList<Trav>();
    }

    public void addTrav(Trav trav){
        travs.add(trav);
    }
    public List<Trav> getListOfTravs() {
        return travs;
    }

    public void setResult(List<Trav> result) {
        this.travs = result;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

