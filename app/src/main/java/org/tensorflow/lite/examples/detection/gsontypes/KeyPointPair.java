package org.tensorflow.lite.examples.detection.gsontypes;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class KeyPointPair {
    @SerializedName("part_confidence")
    float partScore;

    List<Float> locations;

    public float getPartScore() {
        return partScore;
    }

    public List<Float> getLocations() {
        return locations;
    }

    public void setLocations(ArrayList<Float> locations) {
        this.locations = locations;
    }
}
