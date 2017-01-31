package se.m76.mittapi.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stars {

    private List<Star> result = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Stars(){
        result = new ArrayList<Star>();
    }

    public List<Star> getResult() {
        return result;
    }

    public void setResult(List<Star> result) {
        this.result = result;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

