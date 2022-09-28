package org.tensorflow.lite.examples.detection.gsontypes;

import com.google.gson.annotations.SerializedName;

public class PersonwiseResult {
    @SerializedName("pose_score")
    double poseScore;
    KeyPointEnum keys;

    public double getPoseScore() {
        return poseScore;
    }

    public KeyPointEnum getKeys() {
        return keys;
    }

    public void setKeys(KeyPointEnum keys) {
        this.keys = keys;
    }

}
