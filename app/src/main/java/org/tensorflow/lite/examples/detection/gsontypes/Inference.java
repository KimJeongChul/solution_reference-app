package org.tensorflow.lite.examples.detection.gsontypes;

public class Inference {
    String id;
    String name;
    String userId;
    String modelId;
    String endpoint;
    String token;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getToken() {
        return token;
    }

}
