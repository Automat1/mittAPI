package se.m76.mittapi.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Travs {

    private List<Trav> result = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Travs(){
        result = new ArrayList<Trav>();
    }

    public void addTrav(Trav trav){
        result.add(trav);
    }
    public List<Trav> getResult() {
        return result;
    }

    public void setResult(List<Trav> result) {
        this.result = result;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

