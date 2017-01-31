package se.m76.mittapi.models;
import java.util.HashMap;
import java.util.Map;

public class Star {

    private Integer distance;
    private String name;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}